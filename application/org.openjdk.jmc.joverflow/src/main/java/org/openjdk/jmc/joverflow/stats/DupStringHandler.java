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

import org.openjdk.jmc.joverflow.heap.model.HeapStringReader;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.heap.model.JavaValueArray;
import org.openjdk.jmc.joverflow.support.DupStringStats;
import org.openjdk.jmc.joverflow.util.ValueWitIntIdMap;

/**
 * A utility class for handling duplicated String instances during detailed analysis.
 */
public class DupStringHandler {
	private final HeapStringReader stringReader;
	private final ValueWitIntIdMap<DupStringStats.Entry> dupStrings;
	private final InterimRefChain refChain;
	private final int stringInstShallowSize;

	private JavaValueArray backingCharArray; // Backing char[] array read by last handleString() call

	DupStringHandler(HeapStringReader stringReader, List<DupStringStats.Entry> dupStringList, InterimRefChain refChain,
			int stringInstShallowSize) {
		this.stringReader = stringReader;
		dupStrings = new ValueWitIntIdMap<>(dupStringList.size());
		for (DupStringStats.Entry entry : dupStringList) {
			dupStrings.put(entry);
		}
		this.refChain = refChain;
		this.stringInstShallowSize = stringInstShallowSize;
	}

	/**
	 * Analyzes the given string for duplication. If the string is duplicated, calculates its
	 * overhead and records the reference chain for it. Returns true if the string is duplicated,
	 * false otherwise.
	 * <p>
	 * IMPORTANT: it does not read the backing char array of this string if it's not redundant, and
	 * if it's read, does not mark it as visited. Thus the caller may check its original status, and
	 * then MUST set it to visited so that further overhead calculations are done correctly.
	 */
	boolean handleString(JavaObject strObj) {
		int internalId = strObj.getInternalId();
		DupStringStats.Entry se = dupStrings.get(internalId);

		if (se == null) { // Non-duplicate string
			return false;
		}

		backingCharArray = stringReader.getCharArrayForString(strObj);
		if (backingCharArray == null) {
			// Paranoid check
			// Probably unresolved pointer in a corrupted heap dump
			return false;
		}

		int implInclusiveSize = stringInstShallowSize;
		int ovhd = se.getOvhdForNextStringCopy();
		boolean hasDupBackingCharArray = false;
		if (!backingCharArray.isVisited()) {
			implInclusiveSize += backingCharArray.getSize();
			hasDupBackingCharArray = true;
		}

		refChain.recordCurrentRefChainForDupString(strObj, se.string, implInclusiveSize, ovhd, hasDupBackingCharArray);
		return true;
	}

	/**
	 * Returns the backing char[] array read by the last handleString() call.
	 */
	JavaValueArray getLastReadBackingArray() {
		return backingCharArray;
	}
}
