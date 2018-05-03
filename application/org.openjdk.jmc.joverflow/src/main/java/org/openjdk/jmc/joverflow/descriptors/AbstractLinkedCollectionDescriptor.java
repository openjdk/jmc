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
import org.openjdk.jmc.joverflow.heap.model.JavaInt;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.support.Constants;
import org.openjdk.jmc.joverflow.util.ClassUtils;

/**
 * Base descriptor for collection classes that keep their elements in a linked structure, such as
 * LinkedList, TreeMap, ConcurrentLinkedQueue etc. Such classes may or may not have the 'size'
 * field, so this descriptor supports different methods of determining collection size.
 */
public abstract class AbstractLinkedCollectionDescriptor extends AbstractCollectionDescriptor implements Constants {
	protected final Factory factory;

	protected AbstractLinkedCollectionDescriptor(JavaObject col, Factory factory) {
		super(col);
		this.factory = factory;
	}

	@Override
	public int getNumElements() {
		if (factory.sizeFieldIdx != -1) {
			return ((JavaInt) fields[factory.sizeFieldIdx]).getValue();
		} else {
			return getSizeByCountingElements();
		}
	}

	@Override
	AbstractCollectionDescriptor.Factory getFactory() {
		return factory;
	}

	/**
	 * Returns the size of the described collection determined by counting its elements. The
	 * implementation may, of course, cache the value and return it on subsequent invocations.
	 */
	protected abstract int getSizeByCountingElements();

	static abstract class Factory extends AbstractCollectionDescriptor.Factory {
		protected final int sizeFieldIdx, rootFieldIdx;
		private final String elementFieldName;
		private int elementFieldIdx = -1;

		/**
		 * Note that sizeFieldName parameter may be null, which means that the described class does
		 * not have 'size' field. In that case, the descriptor subclass should provide the
		 * implementation of {@link AbstractLinkedCollectionDescriptor#getSizeByCountingElements()}
		 * method.
		 */
		Factory(JavaClass clazz, boolean isMap, String sizeFieldName, String rootFieldName, String elementFieldName,
				JavaClass[] implClasses) {
			super(clazz, isMap, implClasses, null, false, new String[] {rootFieldName});
			sizeFieldIdx = sizeFieldName != null ? clazz.getInstanceFieldIndex(sizeFieldName) : -1;
			rootFieldIdx = clazz.getInstanceFieldIndex(rootFieldName);
			this.elementFieldName = elementFieldName;
		}

		protected Factory(JavaClass clazz, AbstractCollectionDescriptor.Factory superclassFactory) {
			super(clazz, superclassFactory);
			this.sizeFieldIdx = ((Factory) superclassFactory).sizeFieldIdx;
			this.rootFieldIdx = ((Factory) superclassFactory).rootFieldIdx;
			this.elementFieldName = ((Factory) superclassFactory).elementFieldName;
		}

		protected int getElementFieldIdx(JavaObject entry) {
			if (elementFieldIdx == -1) {
				JavaClass entryClass = entry.getClazz();
				String elFieldName = ClassUtils.getExactFieldName(elementFieldName, entryClass);
				elementFieldIdx = entry.getClazz().getInstanceFieldIndex(elFieldName);
			}
			return elementFieldIdx;
		}
	}
}
