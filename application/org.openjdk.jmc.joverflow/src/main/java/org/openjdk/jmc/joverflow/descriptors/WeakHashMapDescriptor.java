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

import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.heap.model.JavaField;
import org.openjdk.jmc.joverflow.heap.model.JavaHeapObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObjectArray;
import org.openjdk.jmc.joverflow.heap.model.JavaThing;
import org.openjdk.jmc.joverflow.support.Constants;
import org.openjdk.jmc.joverflow.util.ClassUtils;

/**
 * So far this class exists solely to provide the getKeysAndValues() method, that we don't want
 * (because we don't need yet) to implement in other descriptors. Note that even if we define that
 * method in CollectionInstanceDescriptor, we will still need to have this class, because its keys
 * are kept in different objects/fields than in say normal HashMap.
 */
public class WeakHashMapDescriptor extends ArrayBasedCollectionDescriptor implements Constants {
	private static final JavaHeapObject[] EMPTY_ARRAY = new JavaHeapObject[0];

	private WeakHashMapDescriptor(JavaObject col, Factory factory) {
		super(col, factory);
	}

	public JavaHeapObject[][] getKeysAndValues() {
		int size = getNumElements();
		if (size <= 0) {
			// "< 0" is weird, but I've seen at least one heap dump with this value.
			// Could that be some concurrency-related inconsistency?
			return new JavaHeapObject[][] {EMPTY_ARRAY, EMPTY_ARRAY};
		}

		Factory f = (Factory) factory;

		JavaObjectArray entriesArray = getElementsArray();
		if (entriesArray == null) {
			return new JavaHeapObject[][] {EMPTY_ARRAY, EMPTY_ARRAY};
		}
		JavaHeapObject[] entries = entriesArray.getElements();
		JavaThing[] entryFields = null; // Reusable fields, to reduce GC pressure

		JavaHeapObject keys[] = new JavaHeapObject[size];
		JavaHeapObject values[] = new JavaHeapObject[size];

		int entryIdx = 0;
		outerLoop: for (JavaHeapObject entryThing : entries) {
			if (entryThing == null || !(entryThing instanceof JavaObject)) {
				continue;
			}
			JavaObject entry = (JavaObject) entryThing;
			while (true) {
				entryFields = entry.getFields(entryFields);
				JavaThing keyThing = entryFields[f.referentFieldIdx];
				JavaThing valueThing = entryFields[f.valueFieldIdx];
				// Instanceof checks below are protection against unresolved objects
				if (keyThing != null && keyThing instanceof JavaHeapObject && valueThing instanceof JavaHeapObject) {
					keys[entryIdx] = (JavaHeapObject) keyThing;
					values[entryIdx] = (JavaHeapObject) valueThing;
					entryIdx++;
					// There is a small chance that the table is caught in an inconsistent state,
					// with more entries than getSize() tells. Just ignore the excessive entry.
					if (entryIdx == keys.length) {
						break outerLoop;
					}
				}
				JavaObject prevEntry = entry;
				JavaThing entryThing1 = entryFields[f.nextFieldIdx];
				if (entryThing1 == null || !(entryThing1 instanceof JavaObject)) {
					break;
				}
				entry = (JavaObject) entryThing1;
				if (entry == prevEntry) {
					throw new RuntimeException("Problem in data: WeakHashMap$Entry.next points to itself?");
				}
			}
		}

		return new JavaHeapObject[][] {keys, values};
	}

	static class Factory extends ArrayBasedCollectionDescriptor.Factory {

		private final int referentFieldIdx, valueFieldIdx, nextFieldIdx;

		Factory(JavaClass clazz, JavaClass[] implClasses) {
			super(clazz, true, ClassUtils.getExactFieldName("size|elementCount", clazz),
					ClassUtils.getExactFieldName("table|elementData", clazz), 16, implClasses, null);
			// In WeakHashMap.Entry, we don't have the standard 'key' field, because
			// it extends WeakReference and uses its 'referent' field as a key.
			setMapKeyFieldName("referent");

			JavaClass entryClass = null;
			for (JavaClass implClass : implClasses) {
				if (implClass.getName().equals(WEAK_HASH_MAP_ENTRY)) {
					entryClass = implClass;
					break;
				}
			}
			if (entryClass == null) {
				throw new RuntimeException("Could not find class " + WEAK_HASH_MAP_ENTRY);
			}

			referentFieldIdx = entryClass.getInstanceFieldIndex("referent");
			valueFieldIdx = entryClass.getInstanceFieldIndex("value");

			int localNextFieldIdx = -1;
			// We have to be very careful with the 'next' field, because both WeakHashMap$Entry
			// has its own declared 'next' field, AND its superclass java.lang.Reference
			// has its own field with the same name! Here we depend on the fact that fields
			// in getFields() are ordered from superclass to subclass
			JavaField fieldDescs[] = entryClass.getFieldsForInstance();
			for (int i = fieldDescs.length - 1; i >= 0; i--) {
				if ("next".equals(fieldDescs[i].getName())) {
					localNextFieldIdx = i;
					break;
				}
			}
			int nDefinedFields = entryClass.getDefinedFields().length;
			if (localNextFieldIdx == -1 || localNextFieldIdx < fieldDescs.length - nDefinedFields) {
				throw new RuntimeException("Could not find field next in " + WEAK_HASH_MAP_ENTRY);
			}

			nextFieldIdx = localNextFieldIdx;
		}

		private Factory(JavaClass clazz, AbstractArrayBasedCollectionDescriptor.Factory superclassFactory) {
			super(clazz, superclassFactory);
			Factory f = (Factory) superclassFactory;
			this.referentFieldIdx = f.referentFieldIdx;
			this.valueFieldIdx = f.valueFieldIdx;
			this.nextFieldIdx = f.nextFieldIdx;
		}

		@Override
		AbstractCollectionDescriptor.Factory cloneForSubclass(JavaClass clazz) {
			return new Factory(clazz, this);
		}

		@Override
		ArrayBasedCollectionDescriptor get(JavaObject col) {
			return new WeakHashMapDescriptor(col, this);
		}
	}
}
