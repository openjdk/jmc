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
import org.openjdk.jmc.joverflow.heap.model.JavaLazyReadObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObjectArray;
import org.openjdk.jmc.joverflow.heap.model.JavaThing;
import org.openjdk.jmc.joverflow.heap.model.UnresolvedObject;
import org.openjdk.jmc.joverflow.support.Constants;

// @SuppressWarnings("unused")
public class ConcurrentHashMapDescriptorForJdk8 extends AbstractCollectionDescriptor
		implements CollectionInstanceDescriptor.CapacityDifferentFromSize, Constants {

	private final Factory factory;
	private int cachedNumElements = -1;
	private int cachedTotalCapacity = -1;

	private ConcurrentHashMapDescriptorForJdk8(JavaObject col, Factory factory) {
		super(col);
		this.factory = factory;
	}

	/**
	 * Assume: table is not null
	 */
	private int getNumElementsForOneTable(JavaObjectArray table) {
		int result = 0;
		JavaHeapObject[] tableElems = table.getElements();
		for (JavaThing tabThing : tableElems) {
			// Can be null in JDK7/8, where individual Nodes are created lazily.
			if (tabThing == null || !(tabThing instanceof JavaObject)) {
				continue;
			}
			result += 1;
		}
		return result;
	}

	@Override
	public int getNumElements() {
		if (cachedNumElements != -1) {
			return cachedNumElements;
		}
		// IMPORTANT: The following part is added because in JDK8, the
		// ConcurrentHashMap implementation has changed a lot, e.g. there
		// are two tables in it, of which the second is a field called
		// "nextTable". Thus for the sake of safety, we also need to calculate the
		// sizes of the second table, although it is probably not inflated yet.
		int result = 0;
		int[] index = {factory.tableFieldIdx, factory.nextTableFieldIdx};
		for (int i = 0; i < 2; i++) {
			JavaThing tableField = fields[index[i]];
			if (tableField != null && tableField instanceof JavaObjectArray) {
				result += getNumElementsForOneTable((JavaObjectArray) tableField);
			}
		}
		cachedNumElements = result;
		return result;
	}

	@Override
	public void iterateList(ListIteratorCallback cb) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Helper method. Iterate a table inside a ConcurrentHashMap.
	 *
	 * @param table
	 * @param cb
	 */
	private void iterateOneTable(JavaHeapObject[] table, MapIteratorCallback cb) {
		int keyFieldIdx = factory.nodeKeyFieldIdx;
		int valFieldIdx = factory.nodeValFieldIdx;
		int nextFieldIdx = factory.nodeNextFieldIdx;
		JavaThing[] nodeFields = null;

		outerLoop: for (JavaHeapObject nodeThing : table) {
			if (nodeThing == null || !(nodeThing instanceof JavaObject)) {
				continue;
			}
			JavaObject node = (JavaObject) nodeThing;

			while (true) {
				if (!cb.scanImplementationObject(node)) {
					break;
				}

				// array may be resized if node switches from
				// Node to TreeBin or vice versa
				nodeFields = node.getFields(nodeFields);

				JavaHeapObject key = null;
				JavaThing keyThing = nodeFields[keyFieldIdx];
				if (keyThing instanceof JavaHeapObject) {
					key = (JavaHeapObject) keyThing;
				}

				JavaHeapObject val = null;
				JavaThing valThing = nodeFields[valFieldIdx];
				if (valThing instanceof JavaHeapObject) {
					val = (JavaHeapObject) valThing;
				}

				if (!cb.scanMapEntry(key, val)) {
					break outerLoop;
				}

				JavaObject prevNode = node;
				JavaThing nextNode = nodeFields[nextFieldIdx];
				if (nextNode == null || !(nextNode instanceof JavaObject)) {
					break;
				}
				node = (JavaObject) nextNode;
				if (node == prevNode) {
					break;
				}
			}
		}
	}

	/**
	 * In new versions of ConcurrentHashMap(from JDK8), there are two tables there. This method
	 * overrides its superclass. It iterates one table first, and iterates the second one.
	 */
	@Override
	public void iterateMap(MapIteratorCallback cb) {
		int[] index = {factory.tableFieldIdx, factory.nextTableFieldIdx};
		for (int i = 0; i < 2; i++) {
			JavaThing tableThing = fields[index[i]];
			JavaObjectArray table;
			if (tableThing != null && tableThing instanceof JavaObjectArray) {
				table = (JavaObjectArray) tableThing;
				if (cb.scanImplementationObject(table)) {
					JavaHeapObject[] tableElems = table.getElements();
					int numElements = getNumElements();
					if (numElements != 0) {
						iterateOneTable(tableElems, cb);
					}
				}
			}
		}
	}

	/**
	 * Overrides superclass method. Get the capacity of each of the two tables inside the
	 * ConcurrentHashMap and add them. This might not be revised in the future, according to
	 * how the ConcurrentHashMap works
	 */
	@Override
	public int getCapacity() {
		if (cachedTotalCapacity != -1) {
			return cachedTotalCapacity;
		}

		int totalCapacity = 0;
		int[] index = {factory.tableFieldIdx, factory.nextTableFieldIdx};
		for (int i = 0; i < 2; i++) {
			JavaThing tableThing = fields[index[i]];
			if (tableThing != null && tableThing instanceof JavaObjectArray) {
				JavaObjectArray table = (JavaObjectArray) tableThing;
				totalCapacity += table.getLength();
			}
		}

		cachedTotalCapacity = totalCapacity;
		return totalCapacity;
	}

	/**
	 * Helper method, used for iterating over one table inside the ConcurrentHashMap.
	 */
	private int getSizeOfOneTable(JavaObjectArray table) {
		int result = 0;
		for (JavaThing nodeThing : table.getElements()) {
			// Can be null in JDK7/8, where individual Segments are created lazily.
			if (nodeThing == null || !(nodeThing instanceof JavaObject)) {
				continue;
			}
			JavaObject node = (JavaObject) nodeThing;
			while (true) {
				node.setVisitedAsCollectionImpl();
				result += node.getSize();
				JavaThing nodeKeyField = node.getField(factory.nodeKeyFieldIdx);
				if (!(nodeKeyField == null || nodeKeyField instanceof UnresolvedObject)) {
					if (nodeKeyField instanceof JavaLazyReadObject) {
						JavaLazyReadObject key = (JavaLazyReadObject) nodeKeyField;
						key.setVisitedAsCollectionImpl();
						result += key.getSize();
					} else if (nodeKeyField instanceof JavaClass) {
						JavaClass key = (JavaClass) nodeKeyField;
						result += key.getSize();
					} else {
						// I don't think it can be anything else
						System.err.println("Unexpected nodeKeyField: " + nodeKeyField.getClass().getName());
					}
				}
				JavaThing nodeValField = node.getField(factory.nodeValFieldIdx);
				if (!(nodeValField == null || nodeValField instanceof UnresolvedObject)) {
					if (nodeValField instanceof JavaLazyReadObject) {
						JavaLazyReadObject val = (JavaLazyReadObject) nodeValField;
						val.setVisitedAsCollectionImpl();
						result += val.getSize();
					} else if (nodeValField instanceof JavaClass) {
						JavaClass val = (JavaClass) nodeValField;
						result += val.getSize();
					} else {
						System.err.println("Unexpected nodeValField: " + nodeValField.getClass().getName());
					}
				}
				JavaThing nextNodeThing = node.getField(factory.nodeNextFieldIdx);
				if (nextNodeThing == null || !(nextNodeThing instanceof JavaObject)) {
					// Unresolved
					break;
				}
				JavaObject prev = node;
				node = (JavaObject) nextNodeThing;
				if (node == prev) {
					break;
				}
			}
		}
		return result;
	}

	@Override
	protected int doGetImplSize() {
		// TODO: shall we also look at views here, like keySet etc.?
		col.setVisitedAsCollectionImpl();
		int result = col.getSize();
		int[] index = {factory.tableFieldIdx, factory.nextTableFieldIdx};
		for (int i = 0; i < 2; i++) {
			JavaThing tableThing = fields[index[i]];
			if (tableThing == null || !(tableThing instanceof JavaObjectArray)) {
				// Likely unresolved object in a corrupted heap dump
				return result;
			}
			JavaObjectArray table = (JavaObjectArray) tableThing;
			table.setVisitedAsCollectionImpl();
			result += table.getSize();
			result += getSizeOfOneTable(table);
		}

		return result;
	}

	@Override
	Factory getFactory() {
		return factory;
	}

	/**
	 * Override superclass. Calculate total length of two tables, and total elements number in two
	 * tables.
	 */
	// FIXME: the way of seeing whether it is sparseness might not be accurate because there are two tables.
	@Override
	public int getSparsenessOverhead(int ptrSize) {
		int totalEls = 0;
		int totalCapacity = 0;
		int emptyTableOverhead = 0;
		int[] index = {factory.tableFieldIdx, factory.nextTableFieldIdx};
		for (int i = 0; i < 2; i++) {
			JavaThing tableThing = fields[index[i]];
			if (tableThing != null && !(tableThing instanceof JavaObjectArray)) {
				JavaObjectArray table = (JavaObjectArray) tableThing;
				int nElsInTab = getNumElementsForOneTable(table);
				totalEls += nElsInTab;
				totalCapacity += table.getLength();
				if (nElsInTab == 0) {
					emptyTableOverhead += table.getSize();
				}
			}
		}

		cachedTotalCapacity = totalCapacity;

		if (totalEls >= totalCapacity / 2) {
			return -1;
		}
		return (totalCapacity - totalEls) * ptrSize + emptyTableOverhead;
	}

	@Override
	public int getDefaultCapacity() {
		return 16;
	}

	@Override
	public long getModCount() {
		return 0;
	}

	static class Factory extends AbstractCollectionDescriptor.Factory {
		private static final String TABLE_NAME = "table";
		private static final String NEXT_TABLE_NAME = "nextTable";
		private final int tableFieldIdx, nextTableFieldIdx, nodeKeyFieldIdx, nodeValFieldIdx, nodeNextFieldIdx;

		Factory(JavaClass mapClazz, JavaClass nodeClazz, JavaClass[] allImplClasses) {
			super(mapClazz, true, allImplClasses, null, false, new String[] {TABLE_NAME, NEXT_TABLE_NAME});

			tableFieldIdx = mapClazz.getInstanceFieldIndex(TABLE_NAME);
			nextTableFieldIdx = mapClazz.getInstanceFieldIndex(NEXT_TABLE_NAME);
			nodeKeyFieldIdx = nodeClazz.getInstanceFieldIndex("key");
			nodeValFieldIdx = nodeClazz.getInstanceFieldIndex("val");
			nodeNextFieldIdx = nodeClazz.getInstanceFieldIndex("next");

		}

		private Factory(JavaClass clazz, AbstractCollectionDescriptor.Factory superclassFactory) {
			super(clazz, superclassFactory);
			tableFieldIdx = ((Factory) superclassFactory).tableFieldIdx;
			nextTableFieldIdx = ((Factory) superclassFactory).nextTableFieldIdx;
			nodeKeyFieldIdx = ((Factory) superclassFactory).nodeKeyFieldIdx;
			nodeValFieldIdx = ((Factory) superclassFactory).nodeValFieldIdx;
			nodeNextFieldIdx = ((Factory) superclassFactory).nodeNextFieldIdx;
		}

		@Override
		CollectionInstanceDescriptor get(JavaObject col) {
			return new ConcurrentHashMapDescriptorForJdk8(col, this);
		}

		@Override
		org.openjdk.jmc.joverflow.descriptors.AbstractCollectionDescriptor.Factory cloneForSubclass(JavaClass clazz) {
			return new Factory(clazz, this);
		}

		// FIXME: this is a problem in new version of ConcurrentHashMap because it doesn't have modCount field any more
		@Override
		protected boolean setModCountFieldIdx(JavaClass clazz) {
			return false;
		}
	}
}
