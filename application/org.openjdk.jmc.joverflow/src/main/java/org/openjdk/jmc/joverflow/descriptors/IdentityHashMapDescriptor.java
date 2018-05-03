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
import org.openjdk.jmc.joverflow.util.ClassUtils;

/**
 * Descriptor for IdentityHashMap. Unlike other maps, IdentityHashMap is implemented as a single
 * array of contained objects, thus it needs a special implementation of content sampling.
 */
public class IdentityHashMapDescriptor extends ArrayBasedCollectionDescriptor {

	private IdentityHashMapDescriptor(JavaObject col, Factory factory) {
		super(col, factory);
	}

	@Override
	public int getImplSize() {
		return getDirectImplSize();
	}

	@Override
	public void iterateMap(MapIteratorCallback cb) {
		JavaObjectArray elsArray = getElementsArray();
		if (elsArray == null) {
			// Unresolved in corrupted heap dump
			return;
		}
		if (!cb.scanImplementationObject(elsArray)) {
			return;
		}

		int numElements = getNumElements();
		if (numElements == 0) {
			return;
		}

		JavaHeapObject[] elements = elsArray.getElements();

		for (int idx = 0; idx < elements.length; idx++) {
			JavaHeapObject key = elements[idx++];
			JavaHeapObject value = elements[idx];
			if (key == null && value == null) {
				continue;
			}
			if (!cb.scanMapEntry(key, value)) {
				break;
			}
		}
	}

	static class Factory extends ArrayBasedCollectionDescriptor.Factory {

		Factory(JavaClass clazz) {
			super(clazz, true, "size", ClassUtils.getExactFieldName("table|elementData", clazz), 32, new JavaClass[] {},
					null);
		}

		Factory(JavaClass clazz, AbstractArrayBasedCollectionDescriptor.Factory superclassFactory) {
			super(clazz, superclassFactory);
		}

		@Override
		AbstractCollectionDescriptor.Factory cloneForSubclass(JavaClass clazz) {
			return new Factory(clazz, this);
		}

		@Override
		ArrayBasedCollectionDescriptor get(JavaObject col) {
			return new IdentityHashMapDescriptor(col, this);
		}
	}
}
