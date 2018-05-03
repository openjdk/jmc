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
 * Descriptor for linked-list style data structures, such as java.util.LinkedList.
 */
public class LinkedCollectionDescriptor extends AbstractLinkedCollectionDescriptor {
	private int cachedSize = -1;

	private LinkedCollectionDescriptor(JavaObject col, Factory factory) {
		super(col, factory);
	}

	@Override
	public int doGetImplSize() {
		int result = col.getSize();

		JavaThing rootField = col.getField(factory.rootFieldIdx);
		if (rootField == null || !(rootField instanceof JavaObject)) {
			return result;
		}

		JavaObject entry = (JavaObject) rootField;
		int nextFieldIdx = factory.getEntryNextFieldIdx(entry);
		long rootEntryObjOfsInFile = entry.getObjOfsInFile();

		// Reusable fields, to reduce GC pressure
		JavaThing[] entryFields = null;

		while (!entry.isVisitedAsCollectionImpl()) {
			// Just in case there is a corruption somewhere
			entry.setVisitedAsCollectionImpl();
			result += entry.getSize();

			entryFields = entry.getFields(entryFields);
			JavaObject prevEntry = entry;
			JavaThing entryThing = entryFields[nextFieldIdx];
			if (entryThing == null || !(entryThing instanceof JavaObject)) {
				break;
			}
			entry = (JavaObject) entryThing;
			if (entry.getObjOfsInFile() == prevEntry.getObjOfsInFile()
					|| entry.getObjOfsInFile() == rootEntryObjOfsInFile) {
				break;
			}
		}

		return result;
	}

	@Override
	public void iterateList(ListIteratorCallback cb) {
		int numElements = getNumElements();
		if (numElements == 0) {
			return;
		}

		JavaThing rootField = col.getField(factory.rootFieldIdx);
		if (rootField == null || !(rootField instanceof JavaObject)) {
			return;
		}

		JavaObject entry = (JavaObject) rootField;
		int nextFieldIdx = factory.getEntryNextFieldIdx(entry);
		int elementFieldIdx = factory.getElementFieldIdx(entry);
		long rootEntryObjOfsInFile = entry.getObjOfsInFile();
		// Reusable fields, to reduce GC pressure
		JavaThing[] entryFields = null;

		while (true) {
			if (!cb.scanImplementationObject(entry)) {
				break;
			}
			entryFields = entry.getFields(entryFields);
			JavaThing payloadThing = entryFields[elementFieldIdx];
			if (payloadThing != null && (payloadThing instanceof JavaHeapObject)) {
				JavaHeapObject payload = (JavaHeapObject) payloadThing;
				if (!cb.scanListElement(payload)) {
					break;
				}
			}

			JavaObject prevEntry = entry;
			JavaThing entryThing = entryFields[nextFieldIdx];
			if (entryThing == null || !(entryThing instanceof JavaObject)) {
				break;
			}
			entry = (JavaObject) entryThing;
			if (entry.getObjOfsInFile() == prevEntry.getObjOfsInFile()
					|| entry.getObjOfsInFile() == rootEntryObjOfsInFile) {
				break;
			}
		}
	}

	// This method is used when the descriptor is used for a collection, e.g.
	// ConcurrentLinkedQueue, that does not provide a 'size' field.
	@Override
	protected int getSizeByCountingElements() {
		if (cachedSize != -1) {
			return cachedSize;
		}

		JavaThing rootField = col.getField(factory.rootFieldIdx);
		if (rootField == null || !(rootField instanceof JavaObject)) {
			cachedSize = 0;
			return cachedSize;
		}

		int result = 0;
		JavaObject entry = (JavaObject) rootField;
		int nextFieldIdx = factory.getEntryNextFieldIdx(entry);
		long rootEntryObjOfsInFile = entry.getObjOfsInFile();
		// Reusable fields, to reduce GC pressure
		JavaThing[] entryFields = null;

		while (true) {
			result++;
			entryFields = entry.getFields(entryFields);

			JavaObject prevEntry = entry;
			JavaThing entryThing = entryFields[nextFieldIdx];
			if (entryThing == null || !(entryThing instanceof JavaObject)) {
				break;
			}
			entry = (JavaObject) entryThing;
			if (entry.getObjOfsInFile() == prevEntry.getObjOfsInFile()
					|| entry.getObjOfsInFile() == rootEntryObjOfsInFile) {
				break;
			}
		}

		cachedSize = result;
		return result;
	}

	@Override
	public void iterateMap(MapIteratorCallback cb) {
		throw new UnsupportedOperationException();
	}

	static class Factory extends AbstractLinkedCollectionDescriptor.Factory {

		Factory(JavaClass clazz, String sizeFieldName, String rootFieldName, String elementFieldName,
				JavaClass[] implClasses) {
			super(clazz, false, sizeFieldName, rootFieldName, elementFieldName, implClasses);
		}

		private Factory(JavaClass clazz, AbstractCollectionDescriptor.Factory superclassFactory) {
			super(clazz, superclassFactory);
		}

		@Override
		AbstractCollectionDescriptor.Factory cloneForSubclass(JavaClass clazz) {
			return new Factory(clazz, this);
		}

		@Override
		CollectionInstanceDescriptor get(JavaObject col) {
			return new LinkedCollectionDescriptor(col, this);
		}
	}
}
