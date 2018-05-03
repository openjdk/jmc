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
 * Base descriptor for Collection classes that keep their elements in an Object array, and this
 * array is not always fully utilized (in other words, collection's capacity may exceed its size).
 * Some of these classes also have a 'size' or similar field, while some don't. There are also other
 * differences in data representation (e.g. for maps the array can points to workload objects
 * directly or via intermediate "Entry" objects). Hence this descriptor has different concrete
 * subclasses.
 */
abstract class AbstractArrayBasedCollectionDescriptor extends AbstractCollectionDescriptor
		implements CollectionInstanceDescriptor.CapacityDifferentFromSize {
	protected final Factory factory;

	AbstractArrayBasedCollectionDescriptor(JavaObject col, Factory factory) {
		super(col);
		this.factory = factory;
	}

	@Override
	public int getSparsenessOverhead(int ptrSize) {
		int capacity, numNonEmptySlots;

		if (factory.classDesc.isMap()) {

			// We count the number of non-empty slots instead of simply calling
			// getNumElements(), since in collections like HashMap, which features
			// chains of Entries, we can get a sparseness problem (too many null slots)
			// even when the number of elements is more than half the capacity.
			JavaObjectArray els = getElementsArray();
			if (els == null) { // Can happen if elements is unresolved
				return -1;
			}

			capacity = els.getLength();
			numNonEmptySlots = 0;
			JavaHeapObject[] elements = els.getElements();
			for (JavaHeapObject element : elements) {
				if (element != null) {
					numNonEmptySlots++;
				}
			}
		} else {
			capacity = getCapacity();
			numNonEmptySlots = getNumElements();
		}

		if (numNonEmptySlots < capacity / 2) {
			return (capacity - numNonEmptySlots) * ptrSize;
		} else {
			return -1;
		}
	}

	@Override
	public int getDefaultCapacity() {
		return factory.initialCapacity;
	}

	@Override
	public int getCapacity() {
		JavaObjectArray els = getElementsArray();
		if (els == null) { // Can happen if elements is unresolved
			return 0;
		}

		return els.getLength();
	}

	@Override
	public void iterateList(ListIteratorCallback cb) {
		JavaObjectArray elsArray = getElementsArray();
		if (elsArray == null) {
			return; // Can happen if elements is unresolved
		}
		if (!cb.scanImplementationObject(elsArray)) {
			return;
		}

		// Iterate over all of the array elements, not just the number that size()
		// (getNumElements()) gives us. This is in case there are some references
		// there for whatever reason (inconsistency or something), and we don't want
		// the corresponding objects to end up being recorded as not reachable from
		// any GC root.
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
		JavaObjectArray entriesArray = getElementsArray();
		if (entriesArray == null) {
			return; // Can happen if entries array is unresolved
		}
		if (!cb.scanImplementationObject(entriesArray)) {
			return;
		}

		int numElements = getNumElements();
		if (numElements == 0) {
			return;
		}

		JavaHeapObject[] entries = entriesArray.getElements();
		JavaThing[] entryFields = null;
		int keyFieldIdx = -1, valueFieldIdx = -1, nextFieldIdx = -1;

		outerLoop: for (JavaHeapObject entryThing : entries) {
			if (entryThing == null || !(entryThing instanceof JavaObject)) {
				continue;
			}
			JavaObject entry = (JavaObject) entryThing;

			while (entry != null) {
				if (!cb.scanImplementationObject(entry)) {
					break; // We get this in BFS if entry already seen
				}
				if (keyFieldIdx == -1) {
					keyFieldIdx = factory.getKeyFieldIdx(entry);
					valueFieldIdx = factory.getValueFieldIdx(entry);
					nextFieldIdx = factory.getEntryNextFieldIdx(entry);
				}
				entryFields = entry.getFields(entryFields);
				JavaThing keyThing = entryFields[keyFieldIdx];
				JavaThing valueThing = entryFields[valueFieldIdx];
				JavaHeapObject key = null, value = null;
				if (keyThing instanceof JavaHeapObject) {
					key = (JavaHeapObject) keyThing;
				}
				if (valueThing instanceof JavaHeapObject) {
					value = (JavaHeapObject) valueThing;
				}
				if (!cb.scanMapEntry(key, value)) {
					break outerLoop;
				}

				JavaObject prevEntry = entry;
				if (!(entryFields[nextFieldIdx] instanceof JavaObject)) {
					break; // Unresolved object
				}
				entry = (JavaObject) entryFields[nextFieldIdx];
				if (entry == prevEntry) {
					break;
				}
			}
		}
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
	 * Returns the sum of shallow sizes of this collection object and its elements array. Marks both
	 * the head collection object and the array with
	 * {@link org.openjdk.jmc.joverflow.heap.model.JavaLazyReadObject#setVisitedAsCollectionImpl()}
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

	/**
	 * Returns additional memory size for $Entry objects used by most maps. It is very important
	 * that this method also marks these $Entry objects as setVisitedAsCollectionImpl(), so that
	 * their size is not taken into account when calculating implementation-inlcusive size for
	 * classes in DetailedStatsCalculator.handleInstance().
	 */
	protected int getMapEntriesImplSize() {
		int numEls = getNumElements();
		if (numEls == 0) {
			return 0;
		}

		JavaObjectArray entriesArray = getElementsArray();
		if (entriesArray == null) {
			return 0; // Unresolved
		}

		JavaHeapObject[] entries = entriesArray.getElements();
		int nextFieldIdx = factory.getEntryNextFieldIdx(entries);
		if (nextFieldIdx == -1) { // Array is empty - may happen with concurrent updates??
			return 0;
		}
		JavaThing[] entryFields = null; // Reusable fields, to reduce GC pressure

		int size = 0;
		for (JavaHeapObject entryThing : entries) {
			if (entryThing == null || !(entryThing instanceof JavaObject)) {
				continue; // Unresolved object
			}
			JavaObject entry = (JavaObject) entryThing;

			// It turns out that sometimes HashMaps etc. may be corrupted due to concurrent
			// updates by multiple threads. As a result, their entries form a cycle.
			// Thus we check each entry here if it has already been visited.
			while (entry != null && !entry.isVisitedAsCollectionImpl()) {
				entry.setVisitedAsCollectionImpl();
				size += entry.getSize();
				entryFields = entry.getFields(entryFields);
				JavaObject prevEntry = entry;
				if (!(entryFields[nextFieldIdx] instanceof JavaObject)) {
					break; // Unresolved object
				}
				entry = (JavaObject) entryFields[nextFieldIdx];
				if (entry == prevEntry) {
					break;
				}
			}
		}

		return size;
	}

	static abstract class Factory extends AbstractCollectionDescriptor.Factory {
		private final int elsArrayFieldIdx;
		private final int initialCapacity;

		Factory(JavaClass clazz, boolean isMap, String elsArrayFieldName, int initialCapacity, JavaClass[] implClasses,
				String[] parentColClassNames) {
			super(clazz, isMap, implClasses, parentColClassNames, false, new String[] {elsArrayFieldName});
			elsArrayFieldIdx = clazz.getInstanceFieldIndex(elsArrayFieldName);
			this.initialCapacity = initialCapacity;
		}

		Factory(JavaClass clazz, AbstractArrayBasedCollectionDescriptor.Factory superclassFactory) {
			super(clazz, superclassFactory);
			this.elsArrayFieldIdx = superclassFactory.elsArrayFieldIdx;
			this.initialCapacity = superclassFactory.initialCapacity;
		}
	}
}
