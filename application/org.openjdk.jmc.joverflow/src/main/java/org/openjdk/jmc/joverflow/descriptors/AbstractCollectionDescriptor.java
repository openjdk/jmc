/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at http://oss.oracle.com/licenses/upl
 *
 * or the following license:
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openjdk.jmc.joverflow.descriptors;

import org.openjdk.jmc.joverflow.heap.model.ImplInclusiveSizeCalculator;
import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.heap.model.JavaField;
import org.openjdk.jmc.joverflow.heap.model.JavaHeapObject;
import org.openjdk.jmc.joverflow.heap.model.JavaInt;
import org.openjdk.jmc.joverflow.heap.model.JavaLong;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.heap.model.JavaThing;
import org.openjdk.jmc.joverflow.util.IntArrayList;

/**
 * Implementation of common class-level functionality for all descriptors.
 */
abstract class AbstractCollectionDescriptor implements CollectionInstanceDescriptor {
	protected final JavaObject col;
	protected final JavaThing[] fields;
	private int implInclusiveSize = -1;

	AbstractCollectionDescriptor(JavaObject col) {
		this.col = col;
		fields = col.getFields();
	}

	@Override
	public CollectionClassDescriptor getClassDescriptor() {
		return getFactory().classDesc;
	}

	@Override
	public int getImplSize() {
		if (implInclusiveSize == -1) {
			implInclusiveSize = doGetImplSize();
		}
		return implInclusiveSize;
	}

	/**
	 * Actually calculates the implementation-inclusive size for this collection. See
	 * {@link #getImplSize()}. Since this calculation may be expensive, this method is called only
	 * once by {@link #getImplSize()}, and the result is cached.
	 */
	protected abstract int doGetImplSize();

	abstract Factory getFactory();

	@Override
	public long getModCount() {
		Factory factory = getFactory();
		int modCountFieldIdx = factory.modCountFieldIdx;
		if (modCountFieldIdx == -1) {
			throw new RuntimeException("There is no modCount field in class " + factory.classDesc.getClassName());
		}
		JavaThing modCountField = fields[modCountFieldIdx];
		if (modCountField instanceof JavaLong) {
			return ((JavaLong) modCountField).getValue();
		} else {
			return ((JavaInt) modCountField).getValue();
		}
	}

	@Override
	public final JavaHeapObject getSampleElement() {
		class ListSampler implements ListIteratorCallback {
			JavaHeapObject sampleElement;
			private int counter;

			@Override
			public boolean scanListElement(JavaHeapObject element) {
				counter++;
				if (element == null) {
					return (counter < 3);
				}
				if (counter == 1) {
					sampleElement = element;
				} else {
					if (sampleElement != null && element.getClazz() == sampleElement.getClazz()) {
						sampleElement = element;
					} else {
						sampleElement = null;
						return false;
					}
				}
				return (counter < 3);
			}

			@Override
			public boolean scanImplementationObject(JavaHeapObject implObj) {
				return true;
			}
		}

		ListSampler listSampler = new ListSampler();
		iterateList(listSampler);
		return listSampler.sampleElement;
	}

	@Override
	public final JavaHeapObject[] getSampleKeyAndValue() {
		class MapSampler implements MapIteratorCallback {
			JavaHeapObject sampleKey, sampleValue;
			private int counter;

			@Override
			public boolean scanMapEntry(JavaHeapObject key, JavaHeapObject value) {
				counter++;
				if (counter == 1) {
					sampleKey = key;
					sampleValue = value;
				} else {
					if (key != null) {
						if (sampleKey != null && key.getClazz() == sampleKey.getClazz()) {
							sampleKey = key;
						} else {
							sampleKey = null;
						}
					}
					if (value != null) {
						if (sampleValue != null && value.getClazz() == sampleValue.getClazz()) {
							sampleValue = value;
						} else {
							sampleValue = null;
						}
					}
				}
				return (counter < 3);
			}

			@Override
			public boolean scanImplementationObject(JavaHeapObject implObj) {
				return true;
			}
		}

		MapSampler cb = new MapSampler();
		iterateMap(cb);
		return new JavaHeapObject[] {cb.sampleKey, cb.sampleValue};
	}

	@Override
	public final boolean hasExtraObjFields() {
		return getFactory().knownAndPrimitiveFieldIndices != null;
	}

	@Override
	public final void filterExtraObjFields(JavaThing[] fields) {
		int[] knownAndPrimitiveFieldIndices = getFactory().knownAndPrimitiveFieldIndices;
		for (int idx : knownAndPrimitiveFieldIndices) {
			fields[idx] = null;
		}
	}

	/**
	 * An instance of Factory is created for every known collection or standalone array type. Its
	 * role is to generate a new CollectionInstanceDescriptor, with all the internals initialized
	 * properly, for each JavaObject (that should be of this Factory's associated type) given to it.
	 * <p>
	 * The abstract Factory class provides data and functionality common to all factories. Its
	 * concrete subclasses are associated with concrete subclasses of AbstractCollectionDescriptor.
	 */
	abstract static class Factory implements ImplInclusiveSizeCalculator {

		protected final CollectionClassDescriptor classDesc;
		private String keyFieldName = "key";
		private String valueFieldName = "value";
		private int keyFieldIdx = -1, valueFieldIdx = -1;
		private int modCountFieldIdx = -1;
		private int entryNextFieldIdx = -1;
		protected final int[] knownAndPrimitiveFieldIndices;

		/**
		 * Creates and returns an instance of CollectionInstanceDescriptor for a JavaObject that
		 * represents a collection or array of type associated with this Factory.
		 */
		abstract CollectionInstanceDescriptor get(JavaObject col);

		CollectionClassDescriptor getClassDescriptor() {
			return classDesc;
		}

		/**
		 * Creates a factory, that in turn would create an instance of CollectionInstanceDescriptor
		 * for any JavaObject that has the same class as clazz below. Most parameters are
		 * self-explanatory. The knownObjFieldNames parameter is needed to properly scan collections
		 * in breadth-first mode. In that mode we need to take special measures for JOverflow to
		 * properly follow references from collections to objects such as HashMap.KeySet etc., that
		 * we consider separate from "collection implementation" fields such as HashMap.table. For
		 * more information, see {@link CollectionInstanceDescriptor#hasExtraObjFields()}) and
		 * {@link #getKnownAndPrimitiveFieldIndices(JavaClass, String[])}.
		 */
		Factory(JavaClass clazz, boolean isMap, JavaClass[] implClasses, String[] parentColClassNames,
				boolean hasOtherCollectionInImpl, String[] knownObjFieldNames) {
			boolean canDetermineModCount = setModCountFieldIdx(clazz);
			knownAndPrimitiveFieldIndices = knownObjFieldNames != null
					? getKnownAndPrimitiveFieldIndices(clazz, knownObjFieldNames) : null;
			classDesc = new CollectionClassDescriptor(clazz, isMap, canDetermineModCount, implClasses,
					parentColClassNames, hasOtherCollectionInImpl);
			clazz.setImplInclusiveSizeCalculator(this);
		}

		/**
		 * Creates a new Factory, where all properties are the same as in superClassFactory except
		 * for the class, which is supplied via clazz. It is intended to be used for generating
		 * factories of descriptors for subclasses of known collection classes, where the
		 * superclass' collection implementation is reused. Should not be called directly - only by
		 * implementations of cloneForSubclass(JavaClass) below.
		 */
		Factory(JavaClass clazz, Factory superclassFactory) {
			classDesc = superclassFactory.classDesc.cloneForSubclass(clazz);
			this.keyFieldName = superclassFactory.keyFieldName;
			this.keyFieldIdx = superclassFactory.keyFieldIdx;
			this.valueFieldIdx = superclassFactory.valueFieldIdx;
			this.modCountFieldIdx = superclassFactory.modCountFieldIdx;
			this.knownAndPrimitiveFieldIndices = superclassFactory.knownAndPrimitiveFieldIndices;
			clazz.setImplInclusiveSizeCalculator(this);
		}

		/**
		 * Calls the above clone constructor (that should be properly re-implemented in each
		 * subclass of Factory).
		 */
		abstract Factory cloneForSubclass(JavaClass clazz);

		@Override
		public int calculateImplInclusiveSize(JavaObject javaObj) {
			CollectionInstanceDescriptor colDesc = get(javaObj);
			return colDesc.getImplSize();
		}

		protected int getEntryNextFieldIdx(JavaThing[] entries) {
			if (entryNextFieldIdx == -1) {
				for (JavaThing entry : entries) {
					if (entry != null && entry instanceof JavaObject) {
						entryNextFieldIdx = getEntryNextFieldIdx((JavaObject) entry);
						break;
					}
				}
			}

			if (entryNextFieldIdx == -1) {
				// Couldn't find any proper elements in this entries array.
				// Perhaps it's really empty (and size field is not null due to e.g.
				// concurrent updates). Or perhaps the dump is corrupted a bit, and
				// none of the elements is a resolved object.
				return -1;
			}

			return entryNextFieldIdx;
		}

		protected int getEntryNextFieldIdx(JavaObject entry) {
			if (entryNextFieldIdx == -1) {
				JavaClass entryClazz = entry.getClazz();
				// We have to be very careful with the 'next' field, because in some classes,
				// e.g. WeakHashMap$Entry, a superclass may have its own field with the same
				// name. Here we rely on the fact that fields in getFields() are ordered from
				// the superclass to subclass.
				JavaField fieldDescs[] = entryClazz.getFieldsForInstance();
				for (int i = fieldDescs.length - 1; i >= 0; i--) {
					if ("next".equals(fieldDescs[i].getName())) {
						entryNextFieldIdx = i;
						break;
					}
				}

			}
			return entryNextFieldIdx;
		}

		/**
		 * Sets the non-standard name for the field that in most maps is called "key" (in
		 * WhateverMap$Entry class)
		 */
		void setMapKeyFieldName(String keyFieldName) {
			this.keyFieldName = keyFieldName;
		}

		/**
		 * Sets the non-standard name for the field that in most maps is called "value" (in
		 * WhateverMap$Entry class)
		 */
		void setValueFieldName(String valueFieldName) {
			this.valueFieldName = valueFieldName;
		}

		protected int getKeyFieldIdx(JavaObject entry) {
			if (keyFieldIdx == -1) {
				keyFieldIdx = entry.getClazz().getInstanceFieldIndex(keyFieldName);
			}
			return keyFieldIdx;
		}

		protected int getValueFieldIdx(JavaObject entry) {
			if (valueFieldIdx == -1) {
				valueFieldIdx = entry.getClazz().getInstanceFieldIndex(valueFieldName);
			}
			return valueFieldIdx;
		}

		protected boolean setModCountFieldIdx(JavaClass clazz) {
			modCountFieldIdx = clazz.getInstanceFieldIndexOrMinusOne("modCount");
			if (modCountFieldIdx != -1) {
				JavaField modCountField = clazz.getFieldForInstance(modCountFieldIdx);
				if (modCountField != null) {
					char modCountFieldType = modCountField.getTypeId();
					if (modCountFieldType != 'J' && modCountFieldType != 'I') {
						// modCount is neither long or int - probably something wrong
						modCountFieldIdx = -1;
					}
				} else {
					modCountFieldIdx = -1; // Paranoia
				}
			}
			return modCountFieldIdx != -1;
		}
	}

	/**
	 * Returns a list of field indices for fields that are either: - "known" to us in advance in
	 * this collection class (that is, used for standard metrics calculation, e.g. HashMap.table) -
	 * primitive fields - "banned" fields, per JavaClass.getBannedFieldIndices()
	 * <p>
	 * The common thing for all these fields is that when they are filtered out, what remains is
	 * "extra" object fields (see {@link CollectionInstanceDescriptor#hasExtraObjFields()}).
	 * However, if it turns out that all of this class's fields fall into one of the above
	 * categories, i.e. there will be no "extra" fields, null is returned.
	 */
	private static int[] getKnownAndPrimitiveFieldIndices(JavaClass clazz, String[] knownObjFieldNames) {
		JavaField[] fields = clazz.getFieldsForInstance();
		int[] bannedFieldIndices = clazz.getBannedFieldIndices();
		// Set banned fields to null
		if (bannedFieldIndices != null) {
			for (int bannedFieldIdx : bannedFieldIndices) {
				fields[bannedFieldIdx] = null;
			}
		}
		// Set known fields to null
		for (String knownFieldName : knownObjFieldNames) {
			// Go from fields of this class to superclass fields, since any name in
			// knownObjFieldNames is more likely to refer to a field defined in this
			// class rather than in some superclass. This is especially important in
			// a situation when both subclass and superclass have fields with the same
			// name.
			for (int i = fields.length - 1; i >= 0; i--) {
				if (fields[i] != null && fields[i].getName().equals(knownFieldName)) {
					fields[i] = null;
					break;
				}
			}
		}
		// Set primitive fields to null
		for (int i = 0; i < fields.length; i++) {
			if (fields[i] != null && !fields[i].isReference()) {
				fields[i] = null;
			}
		}

		IntArrayList ia = new IntArrayList(fields.length);
		for (int i = 0; i < fields.length; i++) {
			if (fields[i] == null) {
				ia.add(i);
			}
		}

		if (ia.size() == fields.length) {
			return null;
		}
		return ia.toArray();
	}
}
