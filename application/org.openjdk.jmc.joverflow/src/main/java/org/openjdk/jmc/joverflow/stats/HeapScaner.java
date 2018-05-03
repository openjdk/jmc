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

import java.util.List;

import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.heap.model.JavaHeapObject;
import org.openjdk.jmc.joverflow.heap.model.JavaLazyReadObject;
import org.openjdk.jmc.joverflow.heap.model.JavaValueArray;
import org.openjdk.jmc.joverflow.heap.model.Root;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.heap.parser.HprofParsingCancelledException;

/**
 * Base functionality that's common for all heap scaners.
 */
abstract class HeapScaner {

	protected final Snapshot snapshot;
	private final InterimRefChain refChain;

	// Reporting progress
	protected int currentProcessedObjNo;
	private final int nTotalObjects;
	protected boolean cancelled;

	// Debugging
	private static final boolean REPORT_UNVISITED = false;

	protected HeapScaner(Snapshot snapshot, InterimRefChain refChain) {
		this.snapshot = snapshot;
		this.refChain = refChain;
		nTotalObjects = snapshot.getNumObjects();
	}

	protected abstract void scanObjectsFromRootObj(JavaHeapObject rootObj);

	/**
	 * Analyzes the heap for anti-patterns starting from the set of heap roots. This has the
	 * advantage that when a problematic data structure is found, we have a trace to it from a root
	 * readily available.
	 */
	protected void analyzeViaRoots() throws HprofParsingCancelledException {
		List<Root> roots = snapshot.getRoots();

		for (Root root : roots) {
			refChain.setCurrentRoot(root);
			JavaHeapObject rootObj = snapshot.getObjectForId(root.getId());
			if (rootObj == null) {
				continue;
			}
			if (rootObj instanceof JavaValueArray) {
				continue;
			}
			scanObjectsFromRootObj(rootObj);
		}

		refChain.setCurrentRoot(null);
	}

	/**
	 * Analyzes the heap by scanning all of the (not yet scanned) objects in it. We have this method
	 * because it seems that some live objects may not always be reachable from the root set. I am
	 * not sure why is that.
	 */
	protected void analyzeViaAllObjectsEnum() throws HprofParsingCancelledException {
		int nObjsBefore = currentProcessedObjNo;
		refChain.setCurrentRoot(Root.UNKNOWN_ROOT);
		if (cancelled) {
			throw new HprofParsingCancelledException();
		}

		// First, scan all collections that contain other collections in their
		// implementation, to avoid counting the latter as first-class collections
		for (JavaLazyReadObject javaHeapObj : snapshot.getUnvisitedObjects()) {
			if (javaHeapObj.getClazz().isCollectionWithOtherCollectionInImpl()) {
				refChain.resetCurrentRoot();
				scanObjectsFromRootObj(javaHeapObj);
			}
		}
		if (cancelled) {
			throw new HprofParsingCancelledException();
		}

		// Next, scan all collections, to avoid counting as standalone e.g. Object[]
		// arrays that belong to collections. Same with Strings.
		for (JavaHeapObject javaHeapObj : snapshot.getUnvisitedObjects()) {
			JavaClass objClazz = javaHeapObj.getClazz();
			if (objClazz.isCollection() || objClazz.isString()) {
				refChain.resetCurrentRoot();
				scanObjectsFromRootObj(javaHeapObj);
			}
		}
		if (cancelled) {
			throw new HprofParsingCancelledException();
		}

		// Finally, scan all remaining objects
		for (JavaHeapObject javaHeapObj : snapshot.getUnvisitedObjects()) {
			refChain.resetCurrentRoot();
			scanObjectsFromRootObj(javaHeapObj);
		}
		if (cancelled) {
			throw new HprofParsingCancelledException();
		}

		if (REPORT_UNVISITED) {
			System.out.println("\rDebug info:");
			System.out.println("Objects unreachable from GC roots: " + (currentProcessedObjNo - nObjsBefore));
		}
	}

	/** Should be necessarily called after all objects have been scanned */
	protected void done() {
		refChain.convertRefChainElementsToFinalRepresentation();
	}

	protected InterimRefChain getRefChain() {
		return refChain;
	}

	/** Used for providing user an estimate of progress made. */
	public synchronized int getProgressPercentage() {
		return (int) (((long) currentProcessedObjNo) * 100 / nTotalObjects);
	}

	public synchronized void cancelCalculation() {
		cancelled = true;
	}

	void incrementCurrentProcessedObjNo() {
		currentProcessedObjNo++;
	}

}
