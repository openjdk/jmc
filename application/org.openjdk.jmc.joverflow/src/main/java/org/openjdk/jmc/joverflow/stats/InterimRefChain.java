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

import org.openjdk.jmc.joverflow.descriptors.CollectionInstanceDescriptor;
import org.openjdk.jmc.joverflow.heap.model.JavaLazyReadObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.heap.model.JavaValueArray;
import org.openjdk.jmc.joverflow.heap.model.Root;
import org.openjdk.jmc.joverflow.support.Constants;
import org.openjdk.jmc.joverflow.support.ProblemRecorder;
import org.openjdk.jmc.joverflow.support.RefChainElement;
import org.openjdk.jmc.joverflow.support.RefChainElementImpl;

/**
 */
public abstract class InterimRefChain {
	private final ProblemRecorder problemRecorder;

	protected RefChainElementImpl.GCRoot curRootRefChainElement;
	protected final ArrayList<RefChainElementImpl.GCRoot> rootElements;
	protected boolean newGCRoot;
	protected RefChainElement curCondensedRefChainElement;

	public InterimRefChain(ProblemRecorder problemRecorder) {
		this.problemRecorder = problemRecorder;
		rootElements = new ArrayList<>();
	}

	void setCurrentRoot(Root currentRoot) {
		if (curRootRefChainElement != null) {
			// Converting the previous scanned subtree to final format now removes unneeded objects early
			curRootRefChainElement.switchTreeToFinalFormat();
		}

		if (currentRoot == null) {
			currentRoot = Root.UNKNOWN_ROOT;
		}
		curRootRefChainElement = new RefChainElementImpl.GCRoot(currentRoot);
		rootElements.add(curRootRefChainElement);
		resetCurrentRoot();
	}

	void resetCurrentRoot() {
		newGCRoot = true;
		curCondensedRefChainElement = curRootRefChainElement;
		onCurrentRootReset();
	}

	/**
	 * Called in the end of both setCurrentRoot() and resetCurrentRoot() methods. Should perform
	 * operations specific to the given implementation of InterimRefChain.
	 */
	protected abstract void onCurrentRootReset();

	/**
	 * For the object that is in the end of the current reference chain, returns the object that
	 * points to it. Returns null if this object is referenced directly by the GC root, or if the
	 * pointer is from an array rather than an object field.
	 */
	protected abstract JavaObject getPointingJavaObject();

	/**
	 * This operation is for internal use only (in the recordXXX() methods below). In
	 * InterimRefChainStack it's quite expensive.
	 */
	protected abstract RefChainElement getLastRefChainElement();

	protected void convertRefChainElementsToFinalRepresentation() {
		// Convert the last scanned subtree to final format
		curRootRefChainElement.switchTreeToFinalFormat();
	}

	/**
	 * Records permanently the snapshot of the current reference chain, associating it with the
	 * collection (or standalone array) object at the end of the chain, and the specified kind and
	 * value of overhead.
	 */
	void recordCurrentRefChainForColCluster(
		JavaLazyReadObject col, CollectionInstanceDescriptor colDesc, Constants.ProblemKind ovhdKind, int ovhd) {
		RefChainElement referer = getLastRefChainElement();
		problemRecorder.recordProblematicCollection(col, colDesc, ovhdKind, ovhd, referer);
	}

	/**
	 * Records permanently the snapshot of the current reference chain, associating it with a good,
	 * no-problem collection object at the end of the chain. This is used to determine which
	 * problematic collection clusters also include (many) "normal", good collections.
	 */
	void recordCurrentRefChainForGoodCollection(JavaLazyReadObject col, CollectionInstanceDescriptor colDesc) {
		RefChainElement referer = getLastRefChainElement();
		problemRecorder.recordGoodCollection(col, colDesc, referer);
	}

	/**
	 * Records permanently the snapshot of the current reference chain, associating it with the
	 * duplicated string object at the end of the chain, the specified overhead value, and whether
	 * or not there is a duplicated backing char array for this string.
	 */
	void recordCurrentRefChainForDupString(
		JavaObject stringObj, String s, int implInclusiveSize, int ovhd, boolean hasDupBackingCharArray) {
		RefChainElement referer = getLastRefChainElement();
		problemRecorder.recordDuplicateString(stringObj, s, implInclusiveSize, ovhd, hasDupBackingCharArray, referer);
	}

	/**
	 * Records permanently the snapshot of the current reference chain, associating it with the
	 * non-duplicated string object at the end of the chain. This is used in detailed analysis for
	 * duplicated strings, to determine which duplicated string clusters also include (many)
	 * "normal", nonduplicated strings.
	 */
	void recordCurrentRefChainForNonDupString(JavaObject stringObj, int implInclusiveSize) {
		RefChainElement referer = getLastRefChainElement();
		problemRecorder.recordNonDuplicateString(stringObj, implInclusiveSize, referer);
	}

	/**
	 * Records permanently the snapshot of the current reference chain, associating it with the
	 * duplicated array object at the end of the chain and the specified overhead value.
	 */
	void recordCurrentRefChainForDupArray(JavaValueArray ar, int ovhd) {
		RefChainElement referer = getLastRefChainElement();
		problemRecorder.recordDuplicateArray(ar, ovhd, referer);
	}

	/**
	 * Records permanently the snapshot of the current reference chain, associating it with the
	 * non-duplicated array object at the end of the chain. This is used in detailed analysis for
	 * duplicated arrays, to determine which duplicated array clusters also include (many) "normal",
	 * nonduplicated arrays.
	 */
	void recordCurrentRefChainForNonDupArray(JavaValueArray ar) {
		RefChainElement referer = getLastRefChainElement();
		problemRecorder.recordNonDuplicateArray(ar, referer);
	}

	/**
	 * Same as previous methods, but for an instance of WeakHashMaps that has hard references back
	 * from values to keys.
	 */
	void recordCurrentRefChainForWeakHashMapWithBackRefs(
		JavaObject col, CollectionInstanceDescriptor colDesc, int ovhd, String valueTypeAndSample) {
		RefChainElement referer = getLastRefChainElement();
		problemRecorder.recordWeakHashMapWithBackRefs(col, colDesc, ovhd, valueTypeAndSample, referer);
	}

	/**
	 * Records permanently the snapshot of the current reference chain, associating it with the good
	 * instance at the end of the chain.
	 */
	void recordCurrentRefChainForGoodInstance(JavaObject obj) {
		RefChainElement referer = getLastRefChainElement();
		problemRecorder.recordGoodInstance(obj, referer);
	}
}
