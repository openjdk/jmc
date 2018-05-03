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
import org.openjdk.jmc.joverflow.heap.model.JavaInt;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObjectArray;
import org.openjdk.jmc.joverflow.heap.model.JavaThing;

/**
 * Descriptor for a bunch of simple array-based Collection classes that are implemented in the same
 * way: they have an int field for size and an object array field for elements. These elements
 * contain either objects that are the actually collection's "payload" (for e.g. ArrayList and
 * Vector), or intermediate $Entry objects (for e.g. HashMap and Hashtable). Note that this
 * descriptor's code currently assumes that for lists the array always points directly to the
 * payload, whereas for maps it always points to entries. Thus it does not support a map with
 * payload contained in elements array: there is currently a separate IdentityHashMapDescriptor for
 * it. ConcurrentHashMap, that has intermediate Segment objects, also uses a separate
 * ConcurrentHashMapDescriptor.
 */
public class ArrayBasedCollectionDescriptor extends AbstractArrayBasedCollectionDescriptor {

	/**
	 * Tag whether a HashMap is the new version. Specifically, for JDK8, in which the HashMap
	 * implementation has changed.
	 */
	private final boolean jdk8HashMap;

	// Since this class sometimes needs to do different things with its
	// superclasses, it needs a specific factory to hide its superclass.
	final Factory factory;

	ArrayBasedCollectionDescriptor(JavaObject col, Factory factory) {
		this(col, factory, false);
	}

	/**
	 * This constructor can be used when it is explicitly told whether the implementation of HashMap
	 * is a new one or not.
	 *
	 * @param col
	 * @param factory
	 * @param jdk8HashMap
	 */
	ArrayBasedCollectionDescriptor(JavaObject col, Factory factory, boolean jdk8HashMap) {
		super(col, factory);
		this.factory = factory;
		this.jdk8HashMap = jdk8HashMap;
	}

	@Override
	public int getNumElements() {
		return ((JavaInt) fields[factory.sizeFieldIdx]).getValue();
	}

	@Override
	public int doGetImplSize() {
		int implSize = getDirectImplSize();
		if (!factory.getClassDescriptor().isMap()) {
			return implSize;
		}

		return implSize + getMapEntriesImplSize();
	}

	/**
	 * Overrides superclass' method.
	 */
	@Override
	public int getMapEntriesImplSize() {
		return jdk8HashMap ? getMapBinsImplSize() : super.getMapEntriesImplSize();
	}

	/**
	 * Override superclass. For JDK8, HashMap implementation has changed, specifically when entries'
	 * number increase, at some point, a bucket in a HashMap could transform into a Tree from linked
	 * list. In that case, the way to traverse the map should be changed.
	 */
	@Override
	public void iterateMap(MapIteratorCallback cb) {
		if (jdk8HashMap) {
			iterateMapEntryOrTree(cb);
		} else {
			super.iterateMap(cb);
		}
	}

	/**
	 * This helper method traverses a tree as a bucket inside a HashMap and calculates the
	 * implementation size of the objects in the tree
	 */
	private int preOrderTraverseTree(JavaObject root) {
		if (root == null) {
			return 0;
		}

		int size = root.getSize();

		JavaThing leftField = root.getField(factory.leftFieldName);
		if (!(leftField instanceof JavaObject)) {
			// Probably corrupted heap
			return 0;
		}
		size += preOrderTraverseTree((JavaObject) leftField);

		JavaThing rightField = root.getField(factory.rightFieldName);
		if (!(rightField instanceof JavaObject)) {
			// Probably corrupted heap
			return 0;
		}
		size += preOrderTraverseTree((JavaObject) rightField);

		return size;
	}

	private void preOrderTraverseTree(JavaObject root, MapIteratorCallback cb) {
		if (root == null) {
			return;
		}

		JavaThing entryOrTreeNode = root.getField("entry");
		if (entryOrTreeNode == null || !(entryOrTreeNode instanceof JavaObject)) {
			return;
		}

		JavaThing keyThing = ((JavaObject) entryOrTreeNode)
				.getField(factory.getKeyFieldIdx((JavaObject) entryOrTreeNode));
		JavaHeapObject key = (keyThing instanceof JavaHeapObject) ? (JavaHeapObject) keyThing : null;

		JavaThing valueThing = ((JavaObject) entryOrTreeNode)
				.getField(factory.getValueFieldIdx((JavaObject) entryOrTreeNode));
		JavaHeapObject value = (valueThing instanceof JavaHeapObject) ? (JavaHeapObject) valueThing : null;

		if (!cb.scanMapEntry(key, value)) {
			return;
		}

		if (root.getField(factory.leftFieldName) instanceof JavaObject) {
			preOrderTraverseTree((JavaObject) root.getField(factory.leftFieldName), cb);
		}

		if (root.getField(factory.rightFieldName) instanceof JavaObject) {
			preOrderTraverseTree((JavaObject) root.getField(factory.rightFieldName), cb);
		}
	}

	/**
	 * This helper method gets the implementation size of the elements inside a HashMap, of which
	 * each element should be either an Entry<K, V> or a TreeBin
	 */
	private int getMapBinsImplSize() {
		int numEls = getNumElements();
		if (numEls == 0) {
			return 0;
		}

		JavaObjectArray binsArray = getElementsArray();
		if (binsArray == null) {
			// Unresolved
			return 0;
		}

		JavaHeapObject[] bins = binsArray.getElements();

		// Reusable fields, to reduce GC pressure
		JavaThing[] binsFields = null;

		int size = 0;
		for (JavaHeapObject binThing : bins) {
			if (binThing == null || !(binThing instanceof JavaObject)) {
				continue; // Unresolved object
			}

			JavaObject bin = (JavaObject) binThing;
			// It turns out that sometimes HashMaps etc. may be corrupted due to
			// concurrent updates by multiple threads. As a result, their entries form a cycle.
			// Thus we check each entry here if it has already been visited.

			// TODO: if (bin.getClazz().getName().equals("java.lang.Object")) {
			if (bin.getField("root") == null) {
				int nextFieldIdx = factory.getEntryNextFieldIdx(bin);
				if (nextFieldIdx == -1) {
					// corrupted heap
					return 0;
				}

				while (bin != null && !bin.isVisitedAsCollectionImpl()) {
					bin.setVisitedAsCollectionImpl();
					size += bin.getSize();

					binsFields = bin.getFields(binsFields);
					if (!(binsFields[nextFieldIdx] instanceof JavaObject)) {
						// Unresolved object
						break;
					}

					JavaObject prevBin = bin;
					bin = (JavaObject) binsFields[nextFieldIdx];
					if (bin == prevBin) {
						break;
					}
				}
			} else {
				// The bin is of TreeBin
				JavaThing treeRoot = bin.getField("root");
				if (treeRoot == null || !(treeRoot instanceof JavaObject)) {
					// Unresolved object
					continue;
				}
				// Traverse the tree to calculate size
				size += preOrderTraverseTree((JavaObject) treeRoot);
			}
		}

		return size;
	}

	/**
	 * This method is created for the new version of HashMap(s) (include and after JDK8), in which a
	 * bin of the HashMap backing array could be either a list of entry or a tree. This method won't
	 * be called if the heap dump is based on a JDK whose HashMap backing array is an array of
	 * Entry<K, V> instead of an array of Object.
	 */
	private void iterateMapEntryOrTree(MapIteratorCallback cb) {
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

			if (keyFieldIdx < 0) {
				keyFieldIdx = factory.getKeyFieldIdx(entry);
				valueFieldIdx = factory.getValueFieldIdx(entry);
				nextFieldIdx = factory.getEntryNextFieldIdx(entry);
			}

			if (entry.getField("root") == null) {
				while (entry != null) {
					if (!cb.scanImplementationObject(entry)) {
						// We get this in BFS if entry already seen
						break;
					}
					entryFields = entry.getFields(entryFields);

					JavaThing keyThing = entryFields[keyFieldIdx];
					JavaHeapObject key = (keyThing instanceof JavaHeapObject) ? (JavaHeapObject) keyThing : null;

					JavaThing valueThing = entryFields[valueFieldIdx];
					JavaHeapObject value = (valueThing instanceof JavaHeapObject) ? (JavaHeapObject) valueThing : null;

					if (!cb.scanMapEntry(key, value)) {
						break outerLoop;
					}

					if (!(entryFields[nextFieldIdx] instanceof JavaObject)) {
						break; // Unresolved object
					}
					JavaObject prevEntry = entry;
					entry = (JavaObject) entryFields[nextFieldIdx];
					if (entry == prevEntry) {
						break;
					}
				}
			} else {
				if (!cb.scanImplementationObject(entry)) {
					break;
				}
				JavaThing root = entry.getField("root");
				if (!(root instanceof JavaObject)) {
					continue;
				}
				// Traverse the tree to scan
				preOrderTraverseTree((JavaObject) root, cb);
			}
		}
	}

	static class Factory extends AbstractArrayBasedCollectionDescriptor.Factory {

		private final int sizeFieldIdx;

		/**
		 * This flag is used for letting the factory know whether the map it describes has a new
		 * implementation (see details in JDK8) or not.
		 */
		private final boolean jdk8HashMap;

		/**
		 * Name for the "left" child and "right" child when a bin inside a HashMap is a tree. In
		 * each TreeNode, there is field name like those.
		 */
		private final String leftFieldName;
		private final String rightFieldName;

		Factory(JavaClass clazz, boolean isMap, String sizeFieldName, String elsArrayFieldName, int initialCapacity,
				JavaClass[] implClasses, String[] parentColClassNames) {
			this(clazz, isMap, sizeFieldName, elsArrayFieldName, initialCapacity, implClasses, parentColClassNames,
					false);
		}

		Factory(JavaClass clazz, boolean isMap, String sizeFieldName, String elsArrayFieldName, int initialCapacity,
				JavaClass[] implClasses, String[] parentColClassNames, boolean jdk8HashMap) {
			super(clazz, isMap, elsArrayFieldName, initialCapacity, implClasses, parentColClassNames);
			sizeFieldIdx = clazz.getInstanceFieldIndex(sizeFieldName);
			this.jdk8HashMap = jdk8HashMap;
			leftFieldName = jdk8HashMap ? "left" : null;
			rightFieldName = jdk8HashMap ? "right" : null;
		}

		Factory(JavaClass clazz, AbstractArrayBasedCollectionDescriptor.Factory superclassFactory) {
			super(clazz, superclassFactory);
			this.sizeFieldIdx = ((Factory) superclassFactory).sizeFieldIdx;
			this.jdk8HashMap = ((Factory) superclassFactory).jdk8HashMap;
			this.leftFieldName = ((Factory) superclassFactory).leftFieldName;
			this.rightFieldName = ((Factory) superclassFactory).rightFieldName;
		}

		@Override
		AbstractCollectionDescriptor.Factory cloneForSubclass(JavaClass clazz) {
			return new Factory(clazz, this);
		}

		@Override
		ArrayBasedCollectionDescriptor get(JavaObject col) {
			return new ArrayBasedCollectionDescriptor(col, this, jdk8HashMap);
		}
	}
}
