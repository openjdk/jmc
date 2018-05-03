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

import org.openjdk.jmc.joverflow.descriptors.CollectionClassDescriptor;
import org.openjdk.jmc.joverflow.descriptors.CollectionDescriptors;
import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.heap.model.JavaField;
import org.openjdk.jmc.joverflow.heap.model.JavaHeapObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObjectArray;
import org.openjdk.jmc.joverflow.support.Constants;
import org.openjdk.jmc.joverflow.support.ProblemRecorder;
import org.openjdk.jmc.joverflow.support.RefChainElement;
import org.openjdk.jmc.joverflow.support.RefChainElementImpl;
import org.openjdk.jmc.joverflow.util.FastStack;
import org.openjdk.jmc.joverflow.util.IndexContainer;

/**
 * Functionality for maintaining the internal reference chain leading from the current root to the
 * currently scanned object, and recording its snapshot when we come across an interesting object.
 * This reference chain representation is internal and interim; the permanent ones, that are
 * accumulated while JOverflow is running, are eventually returned by it as ReferenceChain objects.
 */
class InterimRefChainStack extends InterimRefChain {

	private final CollectionDescriptors colDescriptors;

	// Contains alternating objects representing a JavaHeapObject and an
	// integer index (of field or array element) in the form of IndexContainer.
	private final FastStack<Object> refChain = new FastStack<>(512);
	private final ArrayList<RefChainElement> condensedRefChain;

	// Management of conversion from the concrete to condensed (abstract)
	// reference chain
	private int numCommonRefChainEls;
	private int lastRecordedRefTreeIdx;
	private int[] refChainToRefTreeIdx = new int[16000];

	InterimRefChainStack(ProblemRecorder problemRecorder, CollectionDescriptors colDescriptors) {
		super(problemRecorder);
		this.colDescriptors = colDescriptors;
		condensedRefChain = new ArrayList<>(256);
	}

	@Override
	protected void onCurrentRootReset() {
		condensedRefChain.clear();
		condensedRefChain.add(curCondensedRefChainElement);
	}

	void push(JavaHeapObject javaHeapObj) {
		refChain.push(javaHeapObj);
	}

	void pushIndexContainer(IndexContainer idx) {
		// Note that I tried to use a pool of reusable IndexContainers instead of creating a new one
		// here each time, but looks like it didn't improve performance, and might even have made it
		// slightly worse.
		refChain.push(idx);
	}

	void setCurrentFieldOrArrayIndex(int idx) {
		((IndexContainer) refChain.peek()).set(idx);
	}

	IndexContainer getCurrentIndexContainer() {
		return (IndexContainer) refChain.peek();
	}

	void pop() {
		refChain.pop();

		if (refChain.size() < numCommonRefChainEls) {
			numCommonRefChainEls = refChain.size();
		}
	}

	void pop2() {
		refChain.pop();
		refChain.pop();

		if (refChain.size() < numCommonRefChainEls) {
			numCommonRefChainEls = refChain.size();
		}
	}

	@Override
	protected JavaObject getPointingJavaObject() {
		if (refChain.size() < 2) {
			return null;
		}

		Object obj = refChain.get(refChain.size() - 3);
		if (!(obj instanceof JavaObject)) {
			return null;
		}
		return (JavaObject) obj;
	}

	int size() {
		return refChain.size();
	}

	JavaHeapObject peekJavaHeapObject(int idxFromBack) {
		return (JavaHeapObject) refChain.get(refChain.size() - 1 - idxFromBack);
	}

	int peekIndex(int idxFromBack) {
		return ((IndexContainer) refChain.get(refChain.size() - 1 - idxFromBack)).get();
	}

	/**
	 * Takes the current concrete reference chain and, using information about the previous recorded
	 * ref chain for the same root, generates the elements of the condensed reference chain. The
	 * elements will hang off either a new GC root, or will be attached to some existing
	 * RefChainElement (adding an incremental condensed reference chain). In the condensed ref
	 * chain, multiple objects representing implementation details of known collections and
	 * easy-to-discover linked lists are collapsed into special individual RefChainElements. Returns
	 * the last (leaf) element of the condensed ref chain.
	 */
	@Override
	protected RefChainElement getLastRefChainElement() {
		if (refChainToRefTreeIdx.length < refChain.size()) {
			int[] old = refChainToRefTreeIdx;
			refChainToRefTreeIdx = new int[refChain.size() * 5 / 4];
			System.arraycopy(old, 0, refChainToRefTreeIdx, 0, old.length);
		}
		int chainSizeMinusOne = refChain.size() - 1;
		RefChainElement curRefChainElement = curCondensedRefChainElement;

		if (newGCRoot || numCommonRefChainEls == 0) { // Build non-incremental condensed ref chain
			newGCRoot = false;
			int refTreeElementIdx = -1; // Proper initial value given loop code below
			for (int i = 0; i < chainSizeMinusOne; i++) {
				refTreeElementIdx++;
				CollapsedObj clpsObj = collapseLinkedListImpl(i, curRefChainElement);
				if (clpsObj == null) {
					clpsObj = collapseCollectionImpl(i, curRefChainElement);
				}

				if (clpsObj != null) { // We went through a collection or linked list implementation
					curRefChainElement = clpsObj.desc;
					for (int j = i; j <= clpsObj.endIdx; j++) {
						refChainToRefTreeIdx[j] = refTreeElementIdx;
					}
					i = clpsObj.endIdx;
				} else { // index i points at a regular object in the reference chain
					refChainToRefTreeIdx[i] = refChainToRefTreeIdx[i + 1] = refTreeElementIdx;
					JavaHeapObject javaHeapObj = (JavaHeapObject) refChain.get(i++);
					int fieldOrArrIdx = ((IndexContainer) refChain.get(i)).get();
					curRefChainElement = getLinkDesc(javaHeapObj, fieldOrArrIdx, curRefChainElement);
				}
				condensedRefChain.add(curRefChainElement);
			}
			lastRecordedRefTreeIdx = refTreeElementIdx;
			numCommonRefChainEls = chainSizeMinusOne;
			curCondensedRefChainElement = curRefChainElement;
			return curRefChainElement;
		}

		// Otherwise, need to build an incremental condensedRefChain.
		int lastCommonRefChainIdx = numCommonRefChainEls - 1;
		if ((lastCommonRefChainIdx & 1) == 1) {
			// Need lastCommonRefChainIdx to point at an object, not at field after it.
			// For that, the number should be even.
			lastCommonRefChainIdx--;
		}

		// Incremental compressed ref chain cannot be constructed properly if
		// lastCommonRefChainIdx points into the middle of compressible data structure
		lastCommonRefChainIdx = skipBackPotentialCollectionImplOrLinkedList(lastCommonRefChainIdx);

		// This gets lastCommonRefChainIdx above a compressible data structure
		// or another object that will be the start of the incremental chain
		lastCommonRefChainIdx -= 2;
		// This points at the object that starts the incremental chain
		// +2 is essentially a performance optimization; don't use it when in doubt.
		int startIdx = lastCommonRefChainIdx == 0 ? 0 : lastCommonRefChainIdx + 2;

		// If startIdx == 0, we have to attach the new chain directly to the GC root.
		// That does not allow us to use normal indexing machinery below.
		int nStepsBack = startIdx > 0 ? lastRecordedRefTreeIdx - refChainToRefTreeIdx[lastCommonRefChainIdx]
				: lastRecordedRefTreeIdx + 1;
		int refTreeElementIdx = startIdx > 0 ? refChainToRefTreeIdx[lastCommonRefChainIdx] : -1;

		int lastIndex = condensedRefChain.size() - 1;
		for (int i = 0; i < nStepsBack; i++, lastIndex--) {
			condensedRefChain.remove(lastIndex);
		}
		curRefChainElement = condensedRefChain.get(lastIndex);

		for (int i = startIdx; i < chainSizeMinusOne; i++) {
			refTreeElementIdx++;
			CollapsedObj clpsObj = collapseLinkedListImpl(i, curRefChainElement);
			if (clpsObj == null) {
				clpsObj = collapseCollectionImpl(i, curRefChainElement);
			}

			boolean stitchingLinkedListParts = false;
			if (clpsObj != null) { // We went through a collection or linked list implementation
				if (curRefChainElement == clpsObj.desc) {
					// May happen for a linked list - we have two entities representing compressed
					// parts of the same list. We need to, in effect, merge them together.
					refTreeElementIdx--;
					stitchingLinkedListParts = true;
				} else {
					curRefChainElement = clpsObj.desc;
				}
				for (int j = i; j <= clpsObj.endIdx; j++) {
					refChainToRefTreeIdx[j] = refTreeElementIdx;
				}
				i = clpsObj.endIdx;
			} else { // index i points at a regular object in the reference chain
				refChainToRefTreeIdx[i] = refChainToRefTreeIdx[i + 1] = refTreeElementIdx;
				JavaHeapObject javaHeapObj = (JavaHeapObject) refChain.get(i++);
				int idx = ((IndexContainer) refChain.get(i)).get();
				curRefChainElement = getLinkDesc(javaHeapObj, idx, curRefChainElement);
			}
			if (!stitchingLinkedListParts) {
				condensedRefChain.add(curRefChainElement);
			}
		}

		lastRecordedRefTreeIdx = refTreeElementIdx;
		numCommonRefChainEls = chainSizeMinusOne;
		curCondensedRefChainElement = curRefChainElement;
		return curRefChainElement;
	}

	/**
	 * Starting from the reference chain element at the specified index, skips back as far as needed
	 * so that the resulting object is not an array or an instance of an inner class. This is a
	 * rough way to skip chain elements that are potentially in the implementation of some
	 * collection. Also takes some measures if we happen to be inside a linked list.
	 */
	private int skipBackPotentialCollectionImplOrLinkedList(int idx) {
		int llStepsBack = 0;
		while (true) {
			if (idx <= 2) {
				return idx;
			}

			IndexContainer idx1 = (IndexContainer) refChain.get(idx + 1);
			IndexContainer idx0 = (IndexContainer) refChain.get(idx - 1);
			JavaHeapObject obj1 = (JavaHeapObject) refChain.get(idx);
			JavaHeapObject obj0 = (JavaHeapObject) refChain.get(idx - 2);
			if ((idx0.get() == idx1.get()) && (obj1.getClazz() == obj0.getClazz())) {
				// We are in a linked list. Walk back a little, so that the new
				// part of LL is not too short, and the old and the new parts can
				// later be "stitched" together.
				idx -= 2;
				llStepsBack++;
				if (llStepsBack > 2) {
					return idx;
				}
			} else {
				String className = obj1.getClazz().getName();
				if (className.charAt(0) == '[' || className.contains("$")) {
					idx -= 2;
				} else {
					CollectionClassDescriptor colDesc = colDescriptors.getClassDescriptor(className);
					if (colDesc != null && colDesc.isInImplementationOf(obj0.getClazz().getName())) {
						idx -= 2;
					} else {
						return idx;
					}
				}
			}
		}
	}

	private CollapsedObj collapseCollectionImpl(int startIdx, RefChainElement referer) {
		JavaHeapObject javaHeapObj = (JavaHeapObject) refChain.get(startIdx);
		if (!(javaHeapObj instanceof JavaObject)) {
			return null;
		}

		JavaObject javaObj = (JavaObject) javaHeapObj;
		CollectionClassDescriptor colDesc = colDescriptors.getClassDescriptor(javaObj);
		if (colDesc == null) {
			return null;
		}

		int refChainIdx = startIdx + 2;
		if (refChainIdx >= refChain.size()) {
			return null;
		}
		JavaHeapObject obj = (JavaHeapObject) refChain.get(refChainIdx);
		String objClassName = obj.getClazz().getName();
		if (!(colDesc.isImplClassName(objClassName) || objClassName.equals(Constants.OBJECT_ARRAY))) {
			return null;
		}

		// Collections never point to their elements directly.
		// We skip the field index (e.g. HashMap.table), the object that the field points at
		// (e.g. array of HashMap$Entry objects), the field or array index following that object
		// (e.g. index into array of entries), and look at the next object
		int refChainSize = refChain.size();
		refChainIdx = startIdx + 4;
		if (refChainIdx >= refChainSize) {
			// Can currently happen for e.g. ConcurrentHashMap in JDK7, where Segments are allocated lazily
			// So we can have CHM -> idx of 'segments' field -> Segment[] -> null
			return new CollapsedObj(javaObj.getClazz(), refChainSize - 1, referer);
		}

		obj = (JavaHeapObject) refChain.get(refChainIdx);
		objClassName = obj.getClazz().getName();

		while (colDesc.isImplClassName(objClassName)) {
			refChainIdx += 2;
			if (refChainIdx >= refChainSize) {
				break;
			}
			objClassName = ((JavaHeapObject) refChain.get(refChainIdx)).getClazz().getName();
		}

		return new CollapsedObj(javaObj.getClazz(), refChainIdx - 1, referer);
	}

	private CollapsedObj collapseLinkedListImpl(int startIdx, RefChainElement referer) {
		JavaHeapObject javaHeapObj = (JavaHeapObject) refChain.get(startIdx);
		if (!(javaHeapObj instanceof JavaObject)) {
			return null;
		}

		int refChainLastIdx = refChain.size() - 1;
		if (startIdx + 3 >= refChainLastIdx) {
			return null;
		}

		int refChainIdx = startIdx;
		int fieldIdx = ((IndexContainer) refChain.get(startIdx + 1)).get();
		JavaClass elementClass = javaHeapObj.getClazz();
		JavaClass llDefiningClass = elementClass.getDeclaringClassForField(fieldIdx);

		while (true) {
			int fieldIdx1 = ((IndexContainer) refChain.get(refChainIdx + 3)).get();
			if (fieldIdx1 != fieldIdx) {
				break;
			}
			javaHeapObj = (JavaHeapObject) refChain.get(refChainIdx + 2);
			JavaClass elementClass1 = javaHeapObj.getClazz();
			if (elementClass1.getDeclaringClassForField(fieldIdx) != llDefiningClass) {
				break;
			}
			if (elementClass != null && elementClass1 != elementClass) {
				elementClass = null;
			}

			refChainIdx += 2;
			if (refChainIdx + 3 > refChainLastIdx) {
				break;
			}
		}

		if (refChainIdx == startIdx) {
			return null;
		}

		JavaClass llClazz = elementClass != null ? elementClass : llDefiningClass;

		// Check if we actually look at the same linked list that's already been there
		if (referer instanceof RefChainElementImpl.InstanceFieldOrLinkedList) {
			RefChainElementImpl.InstanceFieldOrLinkedList otherList = (RefChainElementImpl.InstanceFieldOrLinkedList) referer;
			if (!otherList.isInstanceField() && otherList.getJavaClass() == llClazz
					&& otherList.getFieldIdx() == fieldIdx) {
				return new CollapsedObj(refChainIdx + 1, referer);
			}
		} else if (referer instanceof RefChainElementImpl.Collection) {
			// This is intended to help stitch together things like java.util.LinkedList,
			// which we first recognize as a collection, and then, when we see it from the
			// middle, as a noncategorized linked list.
			RefChainElementImpl.Collection otherCol = (RefChainElementImpl.Collection) referer;
			CollectionClassDescriptor colDesc = colDescriptors.getClassDescriptor(otherCol.getJavaClass().getName());
			if (colDesc != null && colDesc.isImplClassName(llClazz.getName())) {
				// Let's now take a look at the last element - maybe it also belongs to this collection
				// (though not technically to the linked list), like LinkedList$Entry.element
				JavaHeapObject lastObj = (JavaHeapObject) refChain.get(refChainIdx + 2);
				if (colDesc.isImplClassName(lastObj.getClazz().getName())) {
					refChainIdx += 2;
				}
				int endIdx = refChainIdx + 1 < refChain.size() ? refChainIdx + 1 : refChainIdx;
				return new CollapsedObj(endIdx, referer);
			}
		}

		return new CollapsedObj(llClazz, fieldIdx, refChainIdx + 1, referer);
	}

	private String getFullLinkDesc(JavaHeapObject javaHeapObj, int idx) {
		if (javaHeapObj instanceof JavaObject) {
			JavaObject javaObj = (JavaObject) javaHeapObj;
			return javaObj.getClazz().getName() + '.' + getFieldDesc(javaObj, idx);
		} else if (javaHeapObj instanceof JavaClass) {
			JavaClass clazz = (JavaClass) javaHeapObj;
			return clazz.getName() + ':' + getStaticFieldDesc(clazz, idx);
		} else if (javaHeapObj instanceof JavaObjectArray) {
			JavaObjectArray arrayObj = (JavaObjectArray) javaHeapObj;
			return arrayObj.getClazz().getName() + '[' + idx + ']';
		} else {
			return null;
		}
	}

	private RefChainElement getLinkDesc(JavaHeapObject javaHeapObj, int idx, RefChainElement referer) {
		if (javaHeapObj instanceof JavaObject) {
			return RefChainElementImpl.getInstanceFieldElement(javaHeapObj.getClazz(), idx, referer);
		} else if (javaHeapObj instanceof JavaClass) {
			JavaClass clazz = (JavaClass) javaHeapObj;
			return RefChainElementImpl.getStaticFieldElement(clazz, idx, referer);
		} else if (javaHeapObj instanceof JavaObjectArray) {
			return RefChainElementImpl.getCompoundArrayElement(javaHeapObj.getClazz(), referer);
		} else {
			throw new RuntimeException("JavaHeapObject of wrong type is supplied: " + javaHeapObj);
		}
	}

	private String getFieldDesc(JavaObject javaObj, int idx) {
		JavaClass clazz = javaObj.getClazz();
		return clazz.getFieldForInstance(idx).getName();
	}

	private String getStaticFieldDesc(JavaClass clazz, int idx) {
		JavaField[] statics = clazz.getStaticFields();
		return statics[idx].getName();
	}

	/**
	 * Used for passing back information about a collapsed series of reference chain elements -
	 * those that belong to a collection implementation or to the linked list.
	 */
	private static class CollapsedObj {
		RefChainElement desc;
		int endIdx; // The index of the last element in this object

		CollapsedObj(JavaClass clazz, int endIdx, RefChainElement referer) {
			this.desc = RefChainElementImpl.getCompoundCollectionElement(clazz, referer);
			this.endIdx = endIdx;
		}

		CollapsedObj(JavaClass clazz, int fieldIdx, int endIdx, RefChainElement referer) {
			this.desc = RefChainElementImpl.getCompoundLinkedListElement(clazz, fieldIdx, referer);
			this.endIdx = endIdx;
		}

		CollapsedObj(int endIdx, RefChainElement desc) {
			this.desc = desc;
			this.endIdx = endIdx;
		}
	}

	// Debugging

	/** Debugging: print all the elements of the current reference chain as-is. */
	public void printFullRefChain() {
		System.out.print(curRootRefChainElement.getRoot().getTypeName());
		System.out.print("  ");

		int chainSizeMinusOne = refChain.size() - 1;
		for (int i = 0; i < chainSizeMinusOne; i++) {
			JavaHeapObject javaHeapObj = (JavaHeapObject) refChain.get(i++);
			int idx = ((IndexContainer) refChain.get(i)).get();
			System.out.print("-->" + getFullLinkDesc(javaHeapObj, idx));
		}
		System.out.println();
	}

	/**
	 * Debugging: returns the current full reference chain, condensing elements that are
	 * implementation details of known collection classes.
	 */
	StringBuilder getPrintableCondensedRefChain() {
		StringBuilder result = new StringBuilder(160);
		result.append(curRootRefChainElement.getRoot().getTypeName()).append('@')
				.append(curRootRefChainElement.getRoot().getId());
		result.append(" ");

		int chainSizeMinusOne = refChain.size() - 1;
		int startIdx = chainSizeMinusOne - 2 * 8;
		if (startIdx <= 0) {
			startIdx = 0;
		} else {
			// Avoid starting from e.g. HashMap$Entry
			while (((JavaHeapObject) refChain.get(startIdx)).getClazz().getName().indexOf('$') > 0) {
				startIdx -= 2;
				if (startIdx == 0) {
					break;
				}
			}
			if (startIdx > 0) {
				result.append("-->...");
			}
		}

		for (int i = startIdx; i < chainSizeMinusOne; i++) {
			CollapsedObj clpsObj = collapseLinkedListImpl(i, null);
			if (clpsObj == null) {
				clpsObj = collapseCollectionImpl(i, null);
			}

			if (clpsObj != null) {
				result.append(clpsObj.desc.toString());
				i = clpsObj.endIdx;
			} else {
				JavaHeapObject javaHeapObj = (JavaHeapObject) refChain.get(i++);
				int idx = ((IndexContainer) refChain.get(i)).get();
				result.append("-->");
				result.append(getLinkDesc(javaHeapObj, idx, null));
			}
		}

		JavaHeapObject lastObj = (JavaHeapObject) refChain.get(chainSizeMinusOne);
		String shortClassName = lastObj.getClazz().getHumanFriendlyName();
		result.append("->>").append(shortClassName);
		return result;
	}

}
