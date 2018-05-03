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
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.heap.model.JavaThing;
import org.openjdk.jmc.joverflow.support.Constants;

/**
 * Descriptor for LinkedHashMap. The main reason it exists is that in addition to the stuff standard
 * for all array-based maps, each LinkedHashMap contains the non-null 'Entry header' field, which
 * does not contain any workload, but is used just as an implementation convenience to manage the
 * linked list.
 */
public class LinkedHashMapDescriptor extends ArrayBasedCollectionDescriptor {

	private LinkedHashMapDescriptor(JavaObject col, Factory factory) {
		super(col, factory);
	}

	@Override
	public int getImplSize() {
		int result = super.getImplSize();
		int idx = ((Factory) factory).headerFieldIdx;
		JavaThing headerThing = idx != -1 ? col.getField(idx) : null;
		if (headerThing == null || !(headerThing instanceof JavaObject)) {
			// Heap dump partly corrupted, or object caught in inconsistent state
			return result;
		}
		JavaObject header = (JavaObject) headerThing;
		header.setVisitedAsCollectionImpl();
		result += header.getSize();
		return result;
	}

	static class Factory extends ArrayBasedCollectionDescriptor.Factory {
		private final int headerFieldIdx;

		Factory(JavaClass clazz, JavaClass[] implClasses, boolean jdk8HashMap) {
			super(clazz, true, "size", "table", 16, implClasses, new String[] {Constants.LINKED_HASH_SET}, jdk8HashMap);
			headerFieldIdx = jdk8HashMap ? clazz.getInstanceFieldIndexOrMinusOne("head")
					: clazz.getInstanceFieldIndexOrMinusOne("header");
		}

		Factory(JavaClass clazz, AbstractArrayBasedCollectionDescriptor.Factory superclassFactory) {
			super(clazz, superclassFactory);
			this.headerFieldIdx = ((Factory) superclassFactory).headerFieldIdx;
		}

		@Override
		AbstractCollectionDescriptor.Factory cloneForSubclass(JavaClass clazz) {
			return new Factory(clazz, this);
		}

		@Override
		ArrayBasedCollectionDescriptor get(JavaObject col) {
			return new LinkedHashMapDescriptor(col, this);
		}
	}
}
