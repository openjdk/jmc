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
import org.openjdk.jmc.joverflow.heap.model.JavaObjectArray;
import org.openjdk.jmc.joverflow.heap.model.JavaThing;

/**
 * Descriptor for List Collection classes that keep their elements in an object array, and this
 * array is fully utilized (collection's capacity is always the same as its size).
 */
class FullyUtilizedArrayListDescriptor extends AbstractCollectionDescriptor {
	protected final Factory factory;

	FullyUtilizedArrayListDescriptor(JavaObject col, Factory factory) {
		super(col);
		this.factory = factory;
	}

	@Override
	public int getNumElements() {
		JavaObjectArray elsArray = getElementsArray();
		if (elsArray == null) {
			// Can happen if elements is unresolved
			return 0;
		}
		return elsArray.getLength();
	}

	@Override
	public void iterateList(ListIteratorCallback cb) {
		JavaObjectArray elsArray = getElementsArray();
		if (elsArray == null) {
			// Can happen if elements is unresolved
			return;
		}
		if (!cb.scanImplementationObject(elsArray)) {
			return;
		}

		JavaHeapObject[] elements = elsArray.getElements();
		for (JavaHeapObject element : elements) {
			if (element == null) {
				continue;
			}
			if (!cb.scanListElement(element)) {
				break;
			}
		}
	}

	@Override
	public void iterateMap(MapIteratorCallback cb) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected int doGetImplSize() {
		return getDirectImplSize();
	}

	@Override
	AbstractCollectionDescriptor.Factory getFactory() {
		return factory;
	}

	public JavaObjectArray getElementsArray() {
		JavaThing els = fields[factory.elsArrayFieldIdx]; // May be null or UnresolvedObject
		return els instanceof JavaObjectArray ? (JavaObjectArray) els : null;
	}

	/**
	 * Returns the sum of shallow sizes of this collection object and its elements array.
	 */
	protected int getDirectImplSize() {
		col.setVisitedAsCollectionImpl();
		int colSize = col.getSize();
		JavaObjectArray els = getElementsArray();
		if (els == null) {
			return colSize;
		} else {
			els.setVisitedAsCollectionImpl();
			return colSize + els.getSize();
		}
	}

	static class Factory extends AbstractCollectionDescriptor.Factory {

		private final int elsArrayFieldIdx;

		Factory(JavaClass clazz, String elsArrayFieldName, JavaClass[] implClasses, String[] parentColClassNames) {
			super(clazz, false, implClasses, parentColClassNames, false, new String[] {elsArrayFieldName});
			elsArrayFieldIdx = clazz.getInstanceFieldIndex(elsArrayFieldName);
		}

		Factory(JavaClass clazz, FullyUtilizedArrayListDescriptor.Factory superclassFactory) {
			super(clazz, superclassFactory);
			this.elsArrayFieldIdx = superclassFactory.elsArrayFieldIdx;
		}

		@Override
		CollectionInstanceDescriptor get(JavaObject col) {
			return new FullyUtilizedArrayListDescriptor(col, this);
		}

		@Override
		AbstractCollectionDescriptor.Factory cloneForSubclass(JavaClass clazz) {
			return new Factory(clazz, this);
		}
	}
}
