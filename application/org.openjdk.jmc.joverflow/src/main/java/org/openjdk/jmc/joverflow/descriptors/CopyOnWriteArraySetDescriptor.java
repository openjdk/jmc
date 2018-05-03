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

/**
 * Descriptor fo instances of java.util.concurrent.CopyOnWriteArraySet.
 */
public class CopyOnWriteArraySetDescriptor extends AbstractCollectionDescriptor {
	private final Factory factory;
	private final FullyUtilizedArrayListDescriptor listDesc;

	private CopyOnWriteArraySetDescriptor(JavaObject col, Factory factory) {
		super(col);
		this.factory = factory;
		JavaThing alField = fields[factory.alFieldIdx];
		if (alField == null || !(alField instanceof JavaObject)) {
			// Unresolved object, or collection caught in inconsistent state
			listDesc = null;
			return;
		}

		JavaObject list = (JavaObject) alField;
		listDesc = (FullyUtilizedArrayListDescriptor) factory.cowaListDescFactory.get(list);
	}

	@Override
	protected int doGetImplSize() {
		if (listDesc == null) {
			return col.getSize();
		}
		listDesc.col.setVisitedAsCollectionImpl();
		return col.getSize() + ((listDesc != null) ? listDesc.getImplSize() : 0);
	}

	@Override
	Factory getFactory() {
		return factory;
	}

	@Override
	public int getNumElements() {
		return listDesc != null ? listDesc.getNumElements() : 0;
	}

	@Override
	public void iterateList(final ListIteratorCallback cb) {
		if (listDesc == null) {
			return;
		}
		listDesc.iterateList(new ListIteratorCallback() {
			@Override
			public boolean scanListElement(JavaHeapObject element) {
				return cb.scanListElement(element);
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

	static class Factory extends AbstractCollectionDescriptor.Factory {
		private static final String AL_FIELD_NAME = "al";

		private final int alFieldIdx;
		private final FullyUtilizedArrayListDescriptor.Factory cowaListDescFactory;

		Factory(JavaClass cowaSetClazz, FullyUtilizedArrayListDescriptor.Factory cowaListDescFactory) {
			// Same as with HashSet, we deliberately treat CopyOnWriteArrayList al field
			// as an "extra" field that should be followed during breadth-first scan. In that
			// way, we scan COWASet -> COWAList, and eventually whatever extra fields there
			// may be in that "dependent" HashMap. To make the code treat COWASet.al as extra
			// field, we don't put it below in knownFieldNames parameter, despite the fact
			// that this field is quite well-known.
			super(cowaSetClazz, false, new JavaClass[] {}, null, true, new String[] {});
			this.cowaListDescFactory = cowaListDescFactory;
			this.alFieldIdx = cowaSetClazz.getInstanceFieldIndex(AL_FIELD_NAME);
		}

		private Factory(JavaClass clazz, AbstractCollectionDescriptor.Factory superclassFactory) {
			super(clazz, superclassFactory);
			this.cowaListDescFactory = ((Factory) superclassFactory).cowaListDescFactory;
			this.alFieldIdx = ((Factory) superclassFactory).alFieldIdx;
		}

		@Override
		AbstractCollectionDescriptor.Factory cloneForSubclass(JavaClass clazz) {
			return new Factory(clazz, this);
		}

		@Override
		CollectionInstanceDescriptor get(JavaObject col) {
			return new CopyOnWriteArraySetDescriptor(col, this);
		}
	}
}
