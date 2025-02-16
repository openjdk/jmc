/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
import org.openjdk.jmc.joverflow.heap.model.JavaInt;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObjectArray;

/**
 * Descriptor for java.util.ArrayDeque.
 */
public class ArrayDequeDescriptor extends AbstractArrayBasedCollectionDescriptor {
	private int numElements = -1; // Cached number of elements

	ArrayDequeDescriptor(JavaObject col, Factory factory) {
		super(col, factory);
	}

	@Override
	public int getNumElements() {
		if (numElements != -1) {
			return numElements;
		}
		JavaObjectArray elsArray = getElementsArray();
		if (elsArray == null) {
			// Unresolved in corrupted heap dump
			numElements = 0;
			return numElements;
		}

		Factory f = (Factory) factory;
		int tail = ((JavaInt) fields[f.tailFieldIdx]).getValue();
		int head = ((JavaInt) fields[f.headFieldIdx]).getValue();
		numElements = (tail - head) & (elsArray.getLength() - 1);
		return numElements;
	}

	@Override
	public int doGetImplSize() {
		return getDirectImplSize();
	}

	static class Factory extends AbstractArrayBasedCollectionDescriptor.Factory {

		private final int headFieldIdx, tailFieldIdx;

		Factory(JavaClass clazz) {
			super(clazz, false, "elements", 16, new JavaClass[] {}, null);
			headFieldIdx = clazz.getInstanceFieldIndex("head");
			tailFieldIdx = clazz.getInstanceFieldIndex("tail");
		}

		private Factory(JavaClass clazz, AbstractArrayBasedCollectionDescriptor.Factory superclassFactory) {
			super(clazz, superclassFactory);
			this.headFieldIdx = ((Factory) superclassFactory).headFieldIdx;
			this.tailFieldIdx = ((Factory) superclassFactory).tailFieldIdx;
		}

		@Override
		AbstractCollectionDescriptor.Factory cloneForSubclass(JavaClass clazz) {
			return new Factory(clazz, this);
		}

		@Override
		CollectionInstanceDescriptor get(JavaObject col) {
			return new ArrayDequeDescriptor(col, this);
		}
	}
}
