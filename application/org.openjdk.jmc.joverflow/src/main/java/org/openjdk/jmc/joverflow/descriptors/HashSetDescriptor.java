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
import org.openjdk.jmc.joverflow.heap.model.JavaHeapObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.heap.model.JavaThing;
import org.openjdk.jmc.joverflow.support.Constants;
import org.openjdk.jmc.joverflow.util.ClassUtils;

/**
 * Descriptor for instances of HashSet and LinkedHashSet.
 */
public class HashSetDescriptor extends AbstractCollectionDescriptor
		implements CollectionInstanceDescriptor.CapacityDifferentFromSize, Constants {
	private final Factory factory;
	private final ArrayBasedCollectionDescriptor mapDesc;

	private HashSetDescriptor(JavaObject col, Factory factory) {
		super(col);
		this.factory = factory;
		JavaThing mapField = fields[factory.mapFieldIdx];
		if (mapField == null || !(mapField instanceof JavaObject)) {
			// Unresolved object, or collection caught in inconsistent state
			mapDesc = null;
			return;
		}

		JavaObject map = (JavaObject) mapField;
		mapDesc = factory.hashMapDescFactory.get(map);
	}

	@Override
	public int getNumElements() {
		return (mapDesc != null) ? mapDesc.getNumElements() : 0;
	}

	@Override
	public void iterateList(final ListIteratorCallback cb) {
		if (mapDesc == null) {
			return;
		}
		mapDesc.iterateMap(new MapIteratorCallback() {
			@Override
			public boolean scanMapEntry(JavaHeapObject key, JavaHeapObject value) {
				return cb.scanListElement(key);
			}

			@Override
			public boolean scanImplementationObject(JavaHeapObject implObj) {
				return cb.scanImplementationObject(implObj);
			}
		});
	}

	@Override
	public void iterateMap(MapIteratorCallback cb) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int doGetImplSize() {
		if (mapDesc == null) {
			return col.getSize();
		}
		mapDesc.col.setVisitedAsCollectionImpl();
		return col.getSize() + ((mapDesc != null) ? mapDesc.getImplSize() : 0);
	}

	@Override
	public int getSparsenessOverhead(int ptrSize) {
		if (mapDesc == null) {
			return 0;
		}
		return mapDesc.getSparsenessOverhead(ptrSize);
	}

	@Override
	public int getDefaultCapacity() {
		return mapDesc != null ? mapDesc.getDefaultCapacity() : 16;
	}

	@Override
	public int getCapacity() {
		return mapDesc != null ? mapDesc.getCapacity() : 0;
	}

	@Override
	public long getModCount() {
		return mapDesc != null ? mapDesc.getModCount() : 0;
	}

	@Override
	AbstractCollectionDescriptor.Factory getFactory() {
		return factory;
	}

	static class Factory extends AbstractCollectionDescriptor.Factory {
		// Possible names of fields for the backing HashMap in different impls of HashSet
		private static final String MAP_FIELD_NAMES = "map|backingMap";

		private final int mapFieldIdx;
		private final ArrayBasedCollectionDescriptor.Factory hashMapDescFactory;

		Factory(JavaClass hashSetClazz, JavaClass[] implClasses,
				ArrayBasedCollectionDescriptor.Factory hashMapDescFactory) {
			// We deliberately treat HashSet.map as an "extra" field that should be followed
			// during breadth-first scan. In that way, we scan HashSet->HashMap, and eventually
			// fields such as HashMap.keySet in that "dependent" HashMap. To make the code
			// treat (Linked)HashSet.map as extra field, we don't put it below in knownFieldNames
			// parameter, despite the fact that this field is quite well-known.
			super(hashSetClazz, false, implClasses, null, true, new String[] {});
			this.hashMapDescFactory = hashMapDescFactory;
			String mapFieldName = ClassUtils.getExactFieldName(MAP_FIELD_NAMES, hashSetClazz);
			mapFieldIdx = hashSetClazz.getInstanceFieldIndex(mapFieldName);
		}

		private Factory(JavaClass clazz, AbstractCollectionDescriptor.Factory superclassFactory) {
			super(clazz, superclassFactory);
			this.hashMapDescFactory = ((Factory) superclassFactory).hashMapDescFactory;
			this.mapFieldIdx = ((Factory) superclassFactory).mapFieldIdx;
		}

		@Override
		AbstractCollectionDescriptor.Factory cloneForSubclass(JavaClass clazz) {
			return new Factory(clazz, this);
		}

		@Override
		CollectionInstanceDescriptor get(JavaObject col) {
			return new HashSetDescriptor(col, this);
		}

		@Override
		protected boolean setModCountFieldIdx(JavaClass clazz) {
			// We know that modCount for HashSet can be found, though in non-standard way.
			// See HashSetDescriptor.getModCount().
			return true;
		}
	}
}
