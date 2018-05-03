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

import org.openjdk.jmc.joverflow.util.ValueWithIntId;

/**
 * Container various duplicate string stats, calculated by
 * {@link org.openjdk.jmc.joverflow.stats.StringStatsCollector}.
 * <p>
 * In particular, getDupStrings() returns the list of duplicate strings sorted by overhead. The
 * overhead is defined as specified in {@link org.openjdk.jmc.joverflow.stats.StringStatsCollector}, but
 * in reality we perform a bit more crude estimate, since duplicate strings may reference sub-ranges
 * of backing char[] arrays, and we don't keep length of each backing char array. The estimate is
 * conservative, i.e. the returned overhead should not be greater than the real one. We also take
 * care of the fact that some shallowly duplicate Strings with different values may point to the
 * same backing arrays. For example, a pair of objects with values "a", "b" both point to 'abc'
 * array, and another pair "a", "b" points to 'abd'. In this case, the size of both 'abc' and 'abd'
 * will count towards overhead, but only once for each (i.e. 3 + 3 chars, not 3 + 3 + 3 + 3).
 */
public class DupStringStats {
	/** Shallow size of java.lang.String instance in the analyzed heap */
	public final int stringInstShallowSize;

	/** Total number of instances of java.lang.String */
	public final int nStrings;

	/**
	 * Number of unique string values. Some of the string with these values can be duplicated, some
	 * not.
	 */
	public final int nUniqueStringValues;

	/**
	 * Number of different values of duplicated strings. In other words, this is the number of
	 * unique string values, for each of which more than one java.lang.String object exists. Since
	 * for some unique string values only one java.lang.String instance exists,
	 * nDifferentDupArrayValues is always less than nUniqueStringValues.
	 */
	public final int nUniqueDupStringValues;

	/** Number of backing char arrays for java.lang.Strings */
	public final int nBackingCharArrays;

	/** Detailed information about individual duplicate strings */
	public final ArrayList<Entry> dupStrings;

	/** Overhead due to all duplicated strings */
	public final long dupStringsOverhead;

	public DupStringStats(int stringInstShallowSize, int nStrings, int nUniqueStringValues, int nBackingCharArrays,
			ArrayList<Entry> dupStrings, long dupStringsOverhead) {
		this.stringInstShallowSize = stringInstShallowSize;
		this.nStrings = nStrings;
		this.nUniqueStringValues = nUniqueStringValues;
		this.nUniqueDupStringValues = dupStrings.size();
		this.nBackingCharArrays = nBackingCharArrays;
		this.dupStrings = dupStrings;
		this.dupStringsOverhead = dupStringsOverhead;
	}

	/**
	 * Represents a unique string value for which multiple String instances exist. They are backed
	 * by one or more char[] arrays.
	 */
	public static class Entry implements ValueWithIntId {
		/**
		 * String value (result of String.toString())
		 */
		public final String string;
		/**
		 * Internal id of all javaObjs equal to this string
		 */
		public final int internalId;
		/**
		 * Num of java.lang.String instances with this value
		 */
		public final int nStringInstances;
		/**
		 * Number of backing char[] arrays for them
		 */
		public final int nBackingArrays;
		/**
		 * Max length for these arrays, in chars
		 */
		public final int maxArrayLen;
		/**
		 * Overhead - how much space we would save if we get rid of all instances but one and all
		 * char arrays but one.
		 */
		public final int overhead;

		// These are used to calculate overhead properly for each duplicate string
		// detected during detailed analysis
		private int runningRemainingOvhd;
		private int runningRemainingNInstances;

		public Entry(String string, int internalId, int nStringInstances, int nBackingArrays, int maxArrayLen,
				int overhead) {
			this.string = string;
			this.internalId = internalId;
			this.nStringInstances = nStringInstances;
			this.nBackingArrays = nBackingArrays;
			this.maxArrayLen = maxArrayLen;
			this.overhead = overhead;

			this.runningRemainingNInstances = nStringInstances;
			this.runningRemainingOvhd = overhead;
		}

		@Override
		public int getId() {
			return internalId;
		}

		/**
		 * Returns the overhead for the next copy of the string in this Entry. We aim to report the
		 * total overhead in this entry that's evenly distributed between all copies of this string.
		 * This is consistent with the fact that the overhead can be fully eliminated only when
		 * *all* places where this string is attached to a long-lived data structure are patched
		 * with an intern() call. So ideally, this method should return the same value each time
		 * it's called. However, in practice that value would almost always be a non-integer number,
		 * like 8.43. Reporting e.g. floor(), ceil() or round() of this number is not good, because
		 * the sum of the resulting numbers won't match the total overhead. So instead this method
		 * returns a series of numbers, which in the above example will be 8s and 9s, such that in
		 * the end their sum is equal to the total overhead.
		 */
		public int getOvhdForNextStringCopy() {
			int result = runningRemainingOvhd / runningRemainingNInstances;
			runningRemainingOvhd -= result;
			runningRemainingNInstances--;
			return result;
		}
	}
}
