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
package org.openjdk.jmc.joverflow.support;

import java.util.ArrayList;

import org.openjdk.jmc.joverflow.heap.model.JavaValueArray;
import org.openjdk.jmc.joverflow.util.ValueWithIntId;

/**
 * Container for stats on duplicated primitive arrays.
 */
public class DupArrayStats {
	/** Total number of standalone primitive array objects */
	public final int nArrays;

	/**
	 * Number of unique array values. Some of the arrays with these values can be duplicated, some
	 * not.
	 */
	public final int nUniqueArrays;

	/**
	 * Number of different values of duplicated arrays. In other words, this is the number of unique
	 * array values, for each of which more than one separate array object exists. Since for some
	 * unique array values only one array object exists, nDifferentDupArrayValues is always less
	 * than nUniqueArrays.
	 */
	public final int nDifferentDupArrayValues;

	/** Detailed information about individual duplicated arrays */
	public final ArrayList<Entry> dupArrays;

	/** Overhead due to all duplicated arrays */
	public final long dupArraysOverhead;

	/** Sets the overall duplicated string stats */
	public DupArrayStats(int nArrays, int nUniqueArrays, int nDifferentDupArrayValues, ArrayList<Entry> dupArrays,
			long dupArraysOverhead) {
		this.nArrays = nArrays;
		this.nUniqueArrays = nUniqueArrays;
		this.nDifferentDupArrayValues = nDifferentDupArrayValues;
		this.dupArrays = dupArrays;
		this.dupArraysOverhead = dupArraysOverhead;
	}

	/**
	 * Represents a unique array value (unique contents), for which multiple array objects exist.
	 */
	public static class Entry implements ValueWithIntId {
		public final JavaValueArray firstArray;
		public final int internalId;
		public final int nArrayInstances;
		public final int overhead;

		// These are used to calculate overhead properly for each individual
		// duplicate array instance detected during detailed analysis
		private int runningRemainingOvhd;
		private int runningRemainingNInstances;

		public Entry(JavaValueArray firstArray, int internalId, int nArrayInstances, int overhead) {
			this.firstArray = firstArray;
			this.internalId = internalId;
			this.nArrayInstances = nArrayInstances;
			this.overhead = overhead;

			this.runningRemainingNInstances = nArrayInstances;
			this.runningRemainingOvhd = overhead;
		}

		@Override
		public int getId() {
			return internalId;
		}

		/**
		 * Returns the overhead for the next copy of the array in this Entry. We aim to report the
		 * total overhead in this entry that's evenly distributed between all copies of this array.
		 * This is consistent with the fact that the overhead can be fully eliminated only when
		 * <strong>all</strong> places where this array is attached to a long-lived data structure
		 * are somehow patched to get rid of duplication. So ideally, this method should return the
		 * same value each time it's called. However, in practice that value would almost always be
		 * a non-integer number, like 8.43. Reporting e.g. floor(), ceil() or round() of this number
		 * is not good, because the sum of the resulting numbers won't match the total overhead. So
		 * instead this method returns a series of numbers, which in the above example will be 8s
		 * and 9s, such that in the end their sum is equal to the total overhead.
		 */
		public int getOvhdForNextArrayCopy() {
			int result = runningRemainingOvhd / runningRemainingNInstances;
			runningRemainingOvhd -= result;
			runningRemainingNInstances--;
			return result;
		}
	}
}
