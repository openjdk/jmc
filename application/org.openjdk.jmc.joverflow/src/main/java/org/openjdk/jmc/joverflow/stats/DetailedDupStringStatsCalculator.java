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

import java.util.Collection;

import org.openjdk.jmc.joverflow.descriptors.CollectionDescriptors;
import org.openjdk.jmc.joverflow.descriptors.CollectionInstanceDescriptor;
import org.openjdk.jmc.joverflow.heap.model.HeapStringReader;
import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.heap.model.JavaHeapObject;
import org.openjdk.jmc.joverflow.heap.model.JavaLazyReadObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObjectArray;
import org.openjdk.jmc.joverflow.heap.model.JavaThing;
import org.openjdk.jmc.joverflow.heap.model.JavaValueArray;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.heap.parser.HprofParsingCancelledException;
import org.openjdk.jmc.joverflow.support.DupStringStats;
import org.openjdk.jmc.joverflow.support.ProblemRecorder;

/**
 * Calculates stats for all duplicated strings in the heap, without exception - unlike the standard
 * DetailedStatsCalculator that calculates all sorts of metrics, but takes into account at most 100
 * (we may change the number) top duplicated strings. Still, after calculating aggregated data, this
 * calculator returns information only about the string clusters with the overhead exceeding the
 * specified minimum.
 */
class DetailedDupStringStatsCalculator implements ProblemChecker {
	private final Snapshot snapshot;
	private final InterimRefChain refChain;
	private final DepthFirstHeapScaner scaner;

	private final HeapStringReader stringReader;
	private DupStringHandler dupStringHandler;

	public DetailedDupStringStatsCalculator(Snapshot snapshot, ProblemRecorder recorder) {
		this.snapshot = snapshot;
		CollectionDescriptors colDescriptors = new CollectionDescriptors(snapshot);
		scaner = new DepthFirstHeapScaner(snapshot, this, recorder, colDescriptors);
		refChain = scaner.getRefChain();

		stringReader = snapshot.getStringReader();
	}

	public DupStringStats calculate() throws HprofParsingCancelledException {
		// Iterate over the heap dump sequentially, and find all duplicated strings
		DupStringStats dss = findDupStrings();

		// Using info from the previous step, collect aggregated info about clusters
		// of duplicated strings
		findRefsToDupStrings(dss);
		return dss;
	}

	/**
	 * Iterates the heap dump sequentially, and finds all the duplicated strings in it.
	 */
	private DupStringStats findDupStrings() {
		Collection<JavaLazyReadObject> allObjects = snapshot.getObjects();
		StringStatsCollector stringDupMap = new StringStatsCollector(snapshot);

		for (JavaLazyReadObject obj : allObjects) {
			JavaClass clazz = obj.getClazz();
			if (!clazz.isString()) {
				continue;
			}

			stringDupMap.add((JavaObject) obj);
		}

		return stringDupMap.getDuplicationStats();
	}

	/**
	 * Walks the heap dumps depth-first, and collects the information about clusters of duplicated
	 * strings.
	 */
	private void findRefsToDupStrings(DupStringStats dss) throws HprofParsingCancelledException {
		dupStringHandler = new DupStringHandler(stringReader, dss.dupStrings, refChain, dss.stringInstShallowSize);

		scaner.analyzeViaRoots();
	}

	@Override
	public CollectionInstanceDescriptor handleInstance(JavaObject obj, JavaThing[] fields) {
		return null;
	}

	@Override
	public void handleObjectArray(JavaObjectArray array, JavaHeapObject[] elements) {
	}

	@Override
	public void handleValueArray(JavaValueArray array) {
	}

	@Override
	public void handleString(JavaObject strObj) {
		boolean isDuplicated = dupStringHandler.handleString(strObj);

		JavaValueArray backingCharArray = dupStringHandler.getLastReadBackingArray();
		if (backingCharArray != null) {
			backingCharArray.setVisited();
		}

		if (!isDuplicated) {
			// Normal, non-duplicated string. Record it, so that eventually for fields
			// pointing at duplicated strings we also know how many random strings they
			// also point to.
			refChain.recordCurrentRefChainForNonDupString(strObj, strObj.getSize() + backingCharArray.getSize());
		}
	}

	public int getProgressPercentage() {
		return scaner.getProgressPercentage();
	}
}
