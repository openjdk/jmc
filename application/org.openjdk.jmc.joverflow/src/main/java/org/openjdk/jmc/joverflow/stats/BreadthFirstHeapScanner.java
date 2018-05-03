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
package org.openjdk.jmc.joverflow.stats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.openjdk.jmc.joverflow.descriptors.CollectionInstanceDescriptor;
import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.heap.model.JavaHeapObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObjectArray;
import org.openjdk.jmc.joverflow.heap.model.JavaThing;
import org.openjdk.jmc.joverflow.heap.model.JavaValueArray;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.heap.parser.HprofParsingCancelledException;
import org.openjdk.jmc.joverflow.stats.InterimRefChainTree.ParentType;
import org.openjdk.jmc.joverflow.support.ProblemRecorder;
import org.openjdk.jmc.joverflow.support.RefChainElement;
import org.openjdk.jmc.joverflow.util.IntArrayList;

/**
 * The heap scanner impl-n that scans the heap in breadth-first order starting from GC roots. The
 * subtree for the first root is scanned fully, then the subtree for the second one, etc.
 * <p>
 * The implementation is heavily optimized for performance. The main goal is to scan objects in each
 * "frontier" in the strictly ascending or descending order, to minimize page swapping in the disk
 * cache. For that, the frontier is sorted. Sorting is made complicated by the fact that the
 * frontier is not a simple array of JavaHeapObjects, but consists of group of objects, where each
 * group is referenced by the same "parent" object (instance, class, array or collection). Thus we
 * have to sort objects within each group, sort groups by the first object, and then use an
 * equivalent of merging a large number of sorted arrays to fetch objects in a globally sorted order
 * from the frontier.
 * <p>
 * One additional optimization that affects performance by at least 10 per cent is alternating
 * sorting direction. It results in alternating forward and backward passes over the heap dump. This
 * results in considerable reduction in page swapping, since when we stop and then go backward
 * (rather than start from the other end of the dump again) we read from pages that are already in
 * memory.
 */
public class BreadthFirstHeapScanner extends HeapScaner {
	private final ProblemChecker objHandler;
	private final InterimRefChainTree refChain;

	private final ArrayList<ObjGroup> nextFrontier;
	private ObjGroup[] curFrontier;
	private int curFrontierEndIdx, curFrontierStartIdx;
	private final ArrayList<FieldObj> fieldBuf;
	private final IntArrayList elementBuf;
	private final FieldObjComparator fieldObjComparator = new FieldObjComparator();
	private final ObjGroupComparator objGroupComparator = new ObjGroupComparator();
	private boolean sortingDirection;

	BreadthFirstHeapScanner(Snapshot snapshot, ProblemChecker objHandler, ProblemRecorder problemRecorder) {
		super(snapshot, new InterimRefChainTree(problemRecorder));
		this.refChain = (InterimRefChainTree) getRefChain();
		this.objHandler = objHandler;
		nextFrontier = new ArrayList<>(1000);
		fieldBuf = new ArrayList<>(100);
		elementBuf = new IntArrayList(1000);
	}

	@Override
	protected void scanObjectsFromRootObj(JavaHeapObject obj) {
		while (obj != null) {
			if (obj.setVisitedIfNot()) {
				currentProcessedObjNo++;
				if (cancelled) {
					throw new HprofParsingCancelledException.Runtime();
				}

				JavaClass clazz = obj.getClazz();

				if (clazz.isString()) {
					objHandler.handleString((JavaObject) obj);
				} else {
					if (obj instanceof JavaObject) {
						JavaObject javaObj = (JavaObject) obj;
						JavaThing[] fields = javaObj.getFields();

						CollectionInstanceDescriptor colDesc = objHandler.handleInstance(javaObj, fields);

						if (fields.length > 0 && obj.getClazz().hasReferenceFields()) {
							// Nullify various fields like those for auxiliary linked list in LinkedHashMap,
							// so that scanObjectFields does not have problems with long lists, etc.
							int[] bannedFieldIndices = clazz.getBannedFieldIndices();
							if (bannedFieldIndices != null) {
								for (int bannedFieldIdx : bannedFieldIndices) {
									fields[bannedFieldIdx] = null;
								}
							}

							if (colDesc != null) {
								// obj is a collection. Add its elements ("payload") to the nextFrontier,
								// and handle its internal implementation objects immediately.
								if (colDesc.getClassDescriptor().isMap()) {
									colDesc.iterateMap(new CollectionInstanceDescriptor.MapIteratorCallback() {
										@Override
										public boolean scanMapEntry(JavaHeapObject key, JavaHeapObject value) {
											pushCollectionElement(key);
											pushCollectionElement(value);
											return true;
										}

										@Override
										public boolean scanImplementationObject(JavaHeapObject implObj) {
											if (implObj.setVisitedIfNot()) {
												currentProcessedObjNo++;
												if (implObj instanceof JavaObject) {
													JavaObject implJavaObj = (JavaObject) implObj;
													objHandler.handleInstance(implJavaObj, implJavaObj.getFields());
												} else {
													// We set elements param to null to avoid overhead. They won't be
													// used by handler anyway, because array should be marked as
													// visitedAsCollectionImpl() by this time.
													objHandler.handleObjectArray((JavaObjectArray) implObj, null);
												}
												return true;
											} else {
												// Probably corrupted data - we shouldn't see a visited object
												return false;
											}
										}
									});
								} else {
									colDesc.iterateList(new CollectionInstanceDescriptor.ListIteratorCallback() {
										@Override
										public boolean scanListElement(JavaHeapObject element) {
											pushCollectionElement(element);
											return true;
										}

										@Override
										public boolean scanImplementationObject(JavaHeapObject implObj) {
											if (implObj.setVisitedIfNot()) {
												currentProcessedObjNo++;
												if (implObj instanceof JavaObject) {
													JavaObject implJavaObj = (JavaObject) implObj;
													objHandler.handleInstance(implJavaObj, implJavaObj.getFields());
												} else {
													objHandler.handleObjectArray((JavaObjectArray) implObj, null);
												}
												return true;
											} else {
												// Probably corrupted data - we shouldn't see a visited object
												return false;
											}
										}
									});
								}
								finishCollectionPush(obj);

								if (colDesc.hasExtraObjFields()) {
									colDesc.filterExtraObjFields(fields);
									pushFields(obj, fields, ParentType.INSTANCE);
								}

							} else {
								// An ordinary, non-collection object
								pushFields(obj, fields, ParentType.INSTANCE);
							}
						}
					} else if (obj instanceof JavaClass) {
						JavaThing[] staticFields = ((JavaClass) obj).getStaticValues();

						if (staticFields.length > 0) {
							pushFields(obj, staticFields, ParentType.CLAZZ);
						}
					} else if (obj instanceof JavaObjectArray) {
						JavaObjectArray objArray = (JavaObjectArray) obj;
						JavaHeapObject[] elements = objArray.getElements();
						objHandler.handleObjectArray(objArray, elements);

						if (elements.length > 0) {
							pushArrayElements(obj, elements);
						}
					} else {
						objHandler.handleValueArray((JavaValueArray) obj);
					}
				}
			}

			// Determine the next object to scan
			obj = getNextObjFromFrontier();
		}

		curFrontier = null;
	}

	private JavaHeapObject getNextObjFromFrontier() {
		JavaHeapObject result;
		int objIdxInCurParent;
		ObjGroup curObjGroup;

		while (true) {
			if (curFrontier == null || curFrontierEndIdx < curFrontierStartIdx) {
				curFrontier = getNewFrontier();
				if (curFrontier == null) {
					return null;
				}
				curFrontierEndIdx = curFrontier.length - 1;
				curFrontierStartIdx = 0;
			}

			curObjGroup = curFrontier[curFrontierStartIdx];
			objIdxInCurParent = curObjGroup.getCurChildLogicalIndex();
			result = curObjGroup.getCurChild(snapshot);
			curObjGroup.curChildIdx++;

			// Re-sort groups within current frontier if necessary
			if (curObjGroup.hasSingleChild() || curObjGroup.curChildIdx >= curObjGroup.getChildArrayLen()) {
				// Just move to the next object group within this frontier
				curFrontier[curFrontierStartIdx] = null; // Help the GC
				curFrontierStartIdx++;
			} else {
				// If the next element in the current group is not in the right order wrt. the
				// first element in the next group, re-sort groups within current frontier to
				// get the smallest element first
				int nextObjGlobalIdx = curObjGroup.getCurChildGlobalIndex();
				// nextObjGlobalIdx > 0 below means the next object is not a class
				if (nextObjGlobalIdx > 0 && (curFrontierEndIdx - curFrontierStartIdx > 0)
						&& ordered(curFrontier[curFrontierStartIdx + 1].getCurChildGlobalIndex(), nextObjGlobalIdx)) {
					int i = findPosInCurFrontierToInsert(nextObjGlobalIdx);
					ObjGroup tmp = curFrontier[curFrontierStartIdx];
					if (i == curFrontierStartIdx + 2) { // No need to call arraycopy() for one element
						curFrontier[curFrontierStartIdx] = curFrontier[curFrontierStartIdx + 1];
					} else {
						System.arraycopy(curFrontier, curFrontierStartIdx + 1, curFrontier, curFrontierStartIdx,
								i - 1 - curFrontierStartIdx);
					}
					curFrontier[i - 1] = tmp;
				}
			}

			if (result == null) {
				continue;
			}
			if (!result.isVisited()) {
				break;
			}
		}

		refChain.setCurParent(curObjGroup.parentObj, curObjGroup.type, curObjGroup.refererToParent);
		refChain.setCurIndexInParent(objIdxInCurParent);
		return result;
	}

	private ObjGroup[] getNewFrontier() {
		if (nextFrontier.isEmpty()) {
			return null;
		}

		ObjGroup[] frontier = nextFrontier.toArray(new ObjGroup[nextFrontier.size()]);
		nextFrontier.clear();

		Arrays.sort(frontier, objGroupComparator);
		sortingDirection = !sortingDirection;
		return frontier;
	}

	/**
	 * Returns the index of element *before which* the element with global index elGlobalIdx should
	 * be inserted.
	 */
	private int findPosInCurFrontierToInsert(int elGlobalIdx) {
		int left = curFrontierStartIdx;
		int right = curFrontierEndIdx;
		if (ordered(curFrontier[right].getCurChildGlobalIndex(), elGlobalIdx)) {
			return right + 1;
		}

		int i;
		while (true) {
			i = (left + right) >>> 1; // "(left + right) / 2" but without risk of overflow
			if (ordered(curFrontier[i].getCurChildGlobalIndex(), elGlobalIdx)) {
				left = i;
				if (!ordered(curFrontier[i + 1].getCurChildGlobalIndex(), elGlobalIdx)) {
					break;
				}
			} else {
				right = i;
			}
		}
		return i;
	}

	private void pushFields(JavaHeapObject obj, JavaThing[] fields, ParentType parentType) {
		for (int i = 0; i < fields.length; i++) {
			JavaThing field = fields[i];
			if (field != null && (field instanceof JavaHeapObject) && !((JavaHeapObject) field).isVisited()) {
				fieldBuf.add(new FieldObj((JavaHeapObject) field, i));
			}
		}
		if (fieldBuf.isEmpty()) {
			return;
		}

		ObjGroup nextGroup;
		if (fieldBuf.size() > 1) {
			FieldObj[] foEls = fieldBuf.toArray(new FieldObj[fieldBuf.size()]);
			Arrays.sort(foEls, fieldObjComparator);
			nextGroup = new ObjGroupFields(refChain.getLastRefChainElement(), parentType, obj, foEls);
		} else {
			FieldObj fo = fieldBuf.get(0);
			nextGroup = new ObjGroupFields(refChain.getLastRefChainElement(), parentType, obj, fo);
		}
		nextFrontier.add(nextGroup);
		fieldBuf.clear();
	}

	private void pushArrayElements(JavaHeapObject obj, JavaHeapObject[] elements) {
		for (JavaHeapObject element : elements) {
			if (element != null && !element.isVisited()) {
				elementBuf.add(element.getGlobalObjectIndex());
			}
		}
		if (elementBuf.isEmpty()) {
			return;
		}

		ObjGroup nextGroup;
		if (elementBuf.size() > 1) {
			int[] els = elementBuf.toArray();
			sortGlobalObjectIndexesInOrder(els);
			nextGroup = new ObjGroupCol(refChain.getLastRefChainElement(), ParentType.ARRAY, obj, els);
		} else {
			nextGroup = new ObjGroupCol(refChain.getLastRefChainElement(), ParentType.ARRAY, obj, elementBuf.get(0));
		}
		nextFrontier.add(nextGroup);
		elementBuf.clear();
	}

	private void pushCollectionElement(JavaHeapObject element) {
		if (element != null && !element.isVisited()) {
			elementBuf.add(element.getGlobalObjectIndex());
		}
	}

	private void finishCollectionPush(JavaHeapObject obj) {
		if (elementBuf.isEmpty()) {
			return;
		}

		ObjGroup nextGroup;
		if (elementBuf.size() > 1) {
			int[] els = elementBuf.toArray();
			sortGlobalObjectIndexesInOrder(els);
			nextGroup = new ObjGroupCol(refChain.getLastRefChainElement(), ParentType.COLLECTION, obj, els);
		} else {
			nextGroup = new ObjGroupCol(refChain.getLastRefChainElement(), ParentType.COLLECTION, obj,
					elementBuf.get(0));
		}
		nextFrontier.add(nextGroup);
		elementBuf.clear();
	}

	/**
	 * Returns true if i1 and i2 are ordered according to sortingDirection. Note that here we use
	 * (!sortingDirection), as opposed to "direct" sortingDirection in JavaHeapObjComparator,
	 * FieldObjComparator and objGroupComparator. That's because when this method is called,
	 * sortingDirection, which is used for the next frontier, is different than for objects in the
	 * current frontier.
	 */
	private boolean ordered(int i1, int i2) {
		if (!sortingDirection) {
			return i1 < i2;
		} else {
			return i1 > i2;
		}
	}

	/**
	 * Sorts global object elements in els according to the current sortingDirection. Same is done
	 * in {@link FieldObjComparator} and {@link ObjGroupComparator}.
	 */
	private void sortGlobalObjectIndexesInOrder(int[] els) {
		Arrays.sort(els);
		if (!sortingDirection) { // Flip the order
			int len = els.length;
			for (int i = 0; i < len / 2; i++) {
				int tmp = els[i];
				int otherIdx = len - i - 1;
				els[i] = els[otherIdx];
				els[otherIdx] = tmp;
			}
		}
	}

	/**
	 * Represents a parent object and a group of its children. Children, which are JavaHeapObjects
	 * (possibly wrapped into FieldObj) are sorted by their global object index.
	 */
	private static abstract class ObjGroup {
		final RefChainElement refererToParent;
		final ParentType type;
		final JavaHeapObject parentObj;
		protected int curChildIdx;

		ObjGroup(RefChainElement refererToParent, InterimRefChainTree.ParentType type, JavaHeapObject parentObj) {
			this.refererToParent = refererToParent;
			this.type = type;
			this.parentObj = parentObj;
		}

		abstract JavaHeapObject getCurChild(Snapshot snapshot);

		abstract int getCurChildLogicalIndex();

		abstract int getCurChildGlobalIndex();

		abstract boolean hasSingleChild();

		abstract int getChildArrayLen();

		@Override
		public String toString() {
			return type + ", refererToParent = " + refererToParent;
		}
	}

	/** Objects that are fields of a parent instance or Clazz */
	private static class ObjGroupFields extends ObjGroup {

		// Either FieldObj[] for multiple children, or FieldObj for a single child
		private final Object oneOrMoreChildren;

		ObjGroupFields(RefChainElement refererToParent, InterimRefChainTree.ParentType type, JavaHeapObject parentObj,
				Object oneOrMoreChildren) {
			super(refererToParent, type, parentObj);
			this.oneOrMoreChildren = oneOrMoreChildren;
		}

		@Override
		JavaHeapObject getCurChild(Snapshot snapshot) {
			if (oneOrMoreChildren instanceof FieldObj[]) {
				return snapshot.getObjectAtGlobalIndex(((FieldObj[]) oneOrMoreChildren)[curChildIdx].objGlobalIdx);
			} else {
				return snapshot.getObjectAtGlobalIndex(((FieldObj) oneOrMoreChildren).objGlobalIdx);
			}
		}

		@Override
		int getCurChildLogicalIndex() {
			if (oneOrMoreChildren instanceof FieldObj[]) {
				return ((FieldObj[]) oneOrMoreChildren)[curChildIdx].objFieldIdx;
			} else {
				return ((FieldObj) oneOrMoreChildren).objFieldIdx;
			}
		}

		@Override
		int getCurChildGlobalIndex() {
			if (oneOrMoreChildren instanceof FieldObj[]) {
				FieldObj[] children = (FieldObj[]) oneOrMoreChildren;
				return children[curChildIdx].objGlobalIdx;
			} else {
				return ((FieldObj) oneOrMoreChildren).objGlobalIdx;
			}
		}

		@Override
		boolean hasSingleChild() {
			return oneOrMoreChildren instanceof FieldObj;
		}

		@Override
		int getChildArrayLen() {
			return ((FieldObj[]) oneOrMoreChildren).length;
		}
	}

	/** Objects that are elements of a parent collection or array */
	private static class ObjGroupCol extends ObjGroup {

		private final int[] childrenGlobalIndexes;
		private final int singleChildGlobalIndex;

		ObjGroupCol(RefChainElement refererToParent, InterimRefChainTree.ParentType type, JavaHeapObject parentObj,
				int[] childrenGlobalIndexes) {
			super(refererToParent, type, parentObj);
			this.childrenGlobalIndexes = childrenGlobalIndexes;
			this.singleChildGlobalIndex = Integer.MIN_VALUE;
		}

		ObjGroupCol(RefChainElement refererToParent, InterimRefChainTree.ParentType type, JavaHeapObject parentObj,
				int singleChildGlobalIndex) {
			super(refererToParent, type, parentObj);
			this.childrenGlobalIndexes = null;
			this.singleChildGlobalIndex = singleChildGlobalIndex;
		}

		@Override
		JavaHeapObject getCurChild(Snapshot snapshot) {
			if (childrenGlobalIndexes != null) {
				return snapshot.getObjectAtGlobalIndex(childrenGlobalIndexes[curChildIdx]);
			} else {
				if (curChildIdx != 0) {
					throw new RuntimeException();
				}
				return snapshot.getObjectAtGlobalIndex(singleChildGlobalIndex);
			}
		}

		@Override
		int getCurChildLogicalIndex() {
			return curChildIdx;
		}

		@Override
		int getCurChildGlobalIndex() {
			if (childrenGlobalIndexes != null) {
				return childrenGlobalIndexes[curChildIdx];
			} else {
				return singleChildGlobalIndex;
			}
		}

		@Override
		boolean hasSingleChild() {
			return childrenGlobalIndexes == null;
		}

		@Override
		int getChildArrayLen() {
			return childrenGlobalIndexes.length;
		}
	}

	/**
	 * A wrapper around JavaHeapObject that also encapsulate a field index for that object under its
	 * instance/Clazz parent. We have to use it to enable sorting of fields without losing logical
	 * field indices.
	 */
	private static class FieldObj {
		final int objGlobalIdx, objFieldIdx;

		FieldObj(JavaHeapObject obj, int objFieldIdx) {
			this.objGlobalIdx = obj.getGlobalObjectIndex();
			this.objFieldIdx = objFieldIdx;
		}
	}

	// A very important property of the comparator classes below is that they
	// compare global indices of two JavaHeapObjects depending on the current
	// "sorting direction". This is done to make our algorithm traverse a heap
	// dump first in forward direction, then backward, then forward again, etc.
	// This, in turn, allows the page buffer to reuse the same pages when the
	// "direction turn" is made, and ultimately to reduce page ins/page outs
	// quite considerably. Same is done in sortGlobalObjectIndexesInOrder() method.

	private class FieldObjComparator implements Comparator<FieldObj> {
		@Override
		public int compare(FieldObj o1, FieldObj o2) {
			int idx1 = o1.objGlobalIdx;
			int idx2 = o2.objGlobalIdx;
			if (sortingDirection) {
				return idx1 - idx2;
			} else {
				return idx2 - idx1;
			}
		}
	}

	private class ObjGroupComparator implements Comparator<ObjGroup> {
		@Override
		public int compare(ObjGroup o1, ObjGroup o2) {
			int firstObjIdx1 = o1.getCurChildGlobalIndex();
			int firstObjIdx2 = o2.getCurChildGlobalIndex();
			if (sortingDirection) {
				return firstObjIdx1 - firstObjIdx2;
			} else {
				return firstObjIdx2 - firstObjIdx1;
			}
		}
	}
}
