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
import java.util.Collections;
import java.util.Comparator;

import org.openjdk.jmc.joverflow.heap.model.JavaValueArray;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.support.DupArrayStats;
import org.openjdk.jmc.joverflow.util.ValueWitIntIdMap;
import org.openjdk.jmc.joverflow.util.ValueWithIntId;

/**
 * Functionality for analyzing standalone primitive arrays for duplication.
 * <p>
 * Two primitive arrays are duplicate if they have the same type, length and contents. A (unique)
 * value of the array is its contents.
 * <p>
 * Works similar to StringDuplicationMap; however to compare arrays we first use a simple hash code,
 * and in case of a match, call Arrays.compare().
 */
class PrimitiveArrayDuplicationMap {
	private final ValueWitIntIdMap<InternalEntry> table;

	private int nTotalArrays, nUniqueArrays;
	private int currentId = 1;

	private int numDifferentDupArrayValues;
	private ArrayList<DupArrayStats.Entry> dupArrays;
	private long dupArraysOvhd;

	PrimitiveArrayDuplicationMap(Snapshot snapshot) {
		int capacity = (snapshot.getNumObjects() / 10);
		table = new ValueWitIntIdMap<>(capacity);
	}

	void add(JavaValueArray array) {
		nTotalArrays++;

		byte[] bytes = array.getValue();
		int checksum = checksum(bytes);

		InternalEntry entry = table.get(checksum);
		if (entry == null) { // No possible entry for this array
			entry = new InternalEntry(checksum, array, currentId++);
			table.put(entry);
		} else { // There is an entry, but it may or may not match
			InternalEntry prevEntry = null;
			while (entry != null) {
				if (entry.firstArray.getClazz() == array.getClazz() && entry.firstArray.getLength() == array.getLength()
						&& Arrays.equals(entry.firstArray.getValue(), bytes)) {
					break; // Found matching entry
				} else {
					prevEntry = entry;
					entry = entry.next;
				}
			}
			if (entry == null) { // No matching entry found
				entry = new InternalEntry(checksum, array, currentId++);
				prevEntry.next = entry;
			}
		}

		entry.nArrayInst++;
		array.setInternalId(entry.uniqueId);
	}

	/**
	 * Must be called after all arrays are processed via {@link #add(JavaValueArray)} call, to
	 * calculate the stats returned by methods below.
	 */
	void calculateFinalStats() {
		ArrayList<DupArrayStats.Entry> result = new ArrayList<>(table.size() / 20);
		for (InternalEntry entry : table.values()) {
			while (entry != null) {
				nUniqueArrays++;
				if (entry.nArrayInst > 1) {
					int overhead = entry.firstArray.getSize() * (entry.nArrayInst - 1);
					dupArraysOvhd += overhead;

					result.add(new DupArrayStats.Entry(entry.firstArray, entry.uniqueId, entry.nArrayInst, overhead));
				}

				entry = entry.next;
			}
		}

		Collections.sort(result, new Comparator<DupArrayStats.Entry>() {
			@Override
			public int compare(DupArrayStats.Entry e1, DupArrayStats.Entry e2) {
				if (e1.overhead > e2.overhead) {
					return -1;
				} else if (e1.overhead < e2.overhead) {
					return 1;
				} else {
					return 0;
				}
			}
		});

		dupArrays = result;
		numDifferentDupArrayValues = result.size();
	}

	int getNumArrays() {
		return nTotalArrays;
	}

	int getNumUniqueArrays() {
		return nUniqueArrays;
	}

	int getNumDifferentDupArrayValues() {
		return numDifferentDupArrayValues;
	}

	long getDupArraysOverhead() {
		return dupArraysOvhd;
	}

	ArrayList<DupArrayStats.Entry> getDupArrays() {
		return dupArrays;
	}

	private static int checksum(byte[] bytes) {
		if (bytes.length == 0) {
			return 0;
		}

		int h = 0;
		for (byte b : bytes) {
			h = 31 * h + b;
		}
		return h;
	}

	/**
	 * Represents a unique array value (by value we mean contents). We use entries of this kind
	 * internally, until the final results are calculated.
	 */
	private static class InternalEntry implements ValueWithIntId {
		// Checksum of this array's contents
		final int checksum;
		// The first array with this value
		final JavaValueArray firstArray;
		// A unique id for this array
		final int uniqueId;
		// Number of instances of identical arrays
		int nArrayInst;
		// checksum collision list
		InternalEntry next;

		InternalEntry(int checksum, JavaValueArray firstArray, int uniqueId) {
			this.checksum = checksum;
			this.firstArray = firstArray;
			this.uniqueId = uniqueId;
		}

		@Override
		public int getId() {
			return checksum;
		}
	}
}
