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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.openjdk.jmc.joverflow.heap.model.HeapStringReader;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.heap.model.JavaValueArray;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.support.CompressibleStringStats;
import org.openjdk.jmc.joverflow.support.DupStringStats;
import org.openjdk.jmc.joverflow.support.NumberEncodingStringStats;
import org.openjdk.jmc.joverflow.support.ShortArrayStats;

/**
 * Functionality for analyzing Strings for duplication.
 * <p>
 * For JDK versions prior to JDK7u?, there are two ways in which two (or more) java.lang.String
 * objects can be duplicate: instances with same value can either point to the same backing char
 * array, or point to different char arrays. In the first case the overhead is the shallow size of
 * the second and subsequent String objects with the same value. In the second case it's as above
 * plus the size of the second and subsequent char arrays.
 * <p>
 * This class maintains a hash map that maps a unique string value to the record containing the
 * number of separate String objects with this value, number of backing char arrays, etc. When all
 * the strings are processed by add() calls, getDuplicationStats() calculates and returns the final
 * results.
 */
class StringStatsCollector {

	private final HeapStringReader stringReader;

	private int stringInstShallowSize; // Shallow size of a String instance

	// Duplicate strings management
	private int nTotalStrings, numBackingCharArrays;
	private final HashMap<String, InternalEntry> table;
	private int currentId = 1;

	// Short strings management
	private int nZeroLengthStrings, n1Strings, n4Strings, n8Strings;

	// Potentially compressible string management
	private int nCompressedStrings, nAsciiCharBackedStrings;
	private long usedBackingArrayBytes, compressedBackingArrayBytes, asciiCharBackingArrayBytes;

	// Management of strings that encode numbers
	private int nStringsEncodingInts;
	private long stringsEncodingIntsOvhd;

	// String length histogram builder
	private LengthHistogram.Builder lenHistoBuilder;

	StringStatsCollector(Snapshot snapshot) {
		stringReader = snapshot.getStringReader();

		// It looks like generally about 1/4..1/3 of objects are Strings
		int capacity = (snapshot.getNumObjects() / 4);
		table = new HashMap<>(capacity);
		lenHistoBuilder = new LengthHistogram.Builder(capacity);
	}

	/**
	 * Add the information for the given string heap object to the table and analyze it for
	 * duplication. Regardless of whether this string is duplicated or not, returns the String equal
	 * to the value of the analyzed string object.
	 */
	String add(JavaObject strObj) {
		nTotalStrings++;
		if (stringInstShallowSize == 0) {
			stringInstShallowSize = strObj.getSize();
		}

		String strVal = stringReader.readString(strObj);
		if (strVal == null) {
			return null;
		}

		InternalEntry entry = table.get(strVal);
		if (entry == null) {
			entry = new InternalEntry(currentId++);
			table.put(strVal, entry);
		}
		strObj.setInternalId(entry.uniqueId);

		entry.nStringInst++;

		// Check if its backing array has been seen before
		JavaValueArray backingArray = stringReader.getLastReadBackingArray();
		boolean arrayNotSeenBefore = !backingArray.isVisitedAsCollectionImpl();
		int backingArraySize = 0; // Will remain zero if backing array already seen
		if (arrayNotSeenBefore) {
			backingArray.setVisitedAsCollectionImpl();
			numBackingCharArrays++;
			entry.nBackingArrs++;
			backingArraySize = backingArray.getSize();
			entry.totalBackingArrSize += backingArraySize;
			int arrayLen = backingArray.getLength();
			if (arrayLen > entry.maxArrayLen) {
				entry.maxArrayLen = arrayLen;
			}
		}

		// Check for potentially compressible strings
		int strLen = strVal.length();
		if (backingArray.getClazz().isByteArray()) {
			nCompressedStrings++;
			if (arrayNotSeenBefore) {
				usedBackingArrayBytes += strLen;
				compressedBackingArrayBytes += strLen;
			}
		} else {
			if (arrayNotSeenBefore) {
				usedBackingArrayBytes += strLen * 2;
			}
			boolean compressible = true;
			for (int i = 0; i < strVal.length(); i++) {
				if (strVal.charAt(i) > 255) {
					compressible = false;
					break;
				}
			}
			if (compressible) {
				nAsciiCharBackedStrings++;
				if (arrayNotSeenBefore) {
					asciiCharBackingArrayBytes += strLen * 2;
				}
			}
		}

		checkForShortString(strVal);

		if (isEncodedIntNumber(strVal)) {
			nStringsEncodingInts++;
			// Below we rely on the fact that backingArraySize is 0 if the backing
			// array has already been seen before
			stringsEncodingIntsOvhd += stringInstShallowSize + backingArraySize - 4;
		}

		lenHistoBuilder.addInstance(strLen, stringInstShallowSize + backingArraySize);

		return strVal;
	}

	public DupStringStats getDuplicationStats() {
		ArrayList<DupStringStats.Entry> dupStringList = new ArrayList<>(table.size() / 20);
		long dupStringsOvhd = 0;
		for (Map.Entry<String, InternalEntry> tableEntry : table.entrySet()) {
			InternalEntry ie = tableEntry.getValue();
			// If there is just one instance of this String, there is no redundancy
			if (ie == null || ie.nStringInst == 1) {
				tableEntry.setValue(null); // Help the GC
				continue;
			}

			int overhead = (ie.nStringInst - 1) * stringInstShallowSize;
			if (ie.nBackingArrs > 1) {
				// If nBackingArrs == 0, it means that we have several String instances with the
				// same value, all pointing to the same char[] array (or bunch of arrays) already
				// claimed by other String(s).
				// If nBackingArrs == 1, we have several String instances with the same value,
				// all pointing at the same array.
				// In both cases, the overhead is only caused by redundant String instances -
				// there are no redundant backing char[] arrays.
				// Need (long) below to avoid overflow
				overhead += (int) (((long) ie.totalBackingArrSize) * (ie.nBackingArrs - 1) / ie.nBackingArrs);
			}

			dupStringList.add(new DupStringStats.Entry(tableEntry.getKey(), ie.uniqueId, ie.nStringInst,
					ie.nBackingArrs, ie.maxArrayLen, overhead));
			dupStringsOvhd += overhead;
		}

		Collections.sort(dupStringList, new Comparator<DupStringStats.Entry>() {
			@Override
			public int compare(DupStringStats.Entry e1, DupStringStats.Entry e2) {
				return e2.overhead - e1.overhead;
			}
		});

		return new DupStringStats(stringInstShallowSize, nTotalStrings, table.size(), numBackingCharArrays,
				dupStringList, dupStringsOvhd);
	}

	public ShortArrayStats getShortStringStats() {
		long strShallowSize = stringInstShallowSize; // Made long to correctly calculate long values
		return new ShortArrayStats(nZeroLengthStrings, strShallowSize * nZeroLengthStrings, n1Strings,
				strShallowSize * n1Strings, n4Strings, strShallowSize * n4Strings, n8Strings,
				strShallowSize * n8Strings);
	}

	public CompressibleStringStats getCompressibleStringStats() {
		return new CompressibleStringStats(nTotalStrings, numBackingCharArrays, usedBackingArrayBytes,
				nCompressedStrings, compressedBackingArrayBytes, nAsciiCharBackedStrings, asciiCharBackingArrayBytes);
	}

	public NumberEncodingStringStats getNumberEncodingStringStats() {
		return new NumberEncodingStringStats(nStringsEncodingInts, stringsEncodingIntsOvhd);
	}

	public LengthHistogram getLengthHistogram() {
		return lenHistoBuilder.build();
	}

	private void checkForShortString(String string) {
		int strLen = string.length();
		if (strLen == 0) {
			nZeroLengthStrings++;
		} else if (strLen == 1) {
			n1Strings++;
		} else if (strLen <= 4) {
			n4Strings++;
		} else if (strLen <= 8) {
			n8Strings++;
		}
	}

	/**
	 * Checks whether the given string encodes an integer number with radix 10. The code is adapted
	 * from Integer.parseInt(). We didnt' want to use that method directly because it throws an
	 * exception if the string is wrong, which can be very costly. It also doesn't make sense for us
	 * to use a radix other than 10 - and calculations that use non-custom radix likely incur
	 * additional overhead.
	 */
	private boolean isEncodedIntNumber(String s) {
		if (s.isEmpty()) {
			return false;
		}

		int result = 0;
//		boolean negative = false;
		int i = 0, len = s.length();
		int limit = -Integer.MAX_VALUE;

		char firstChar = s.charAt(0);
		if (firstChar < '0') { // Possible leading "+" or "-"
			if (firstChar == '-') {
//				negative = true;
				limit = Integer.MIN_VALUE;
			} else if (firstChar != '+') {
				return false;
			}

			if (len == 1) {
				return false; // Cannot have lone "+" or "-"
			}
			i++;
		}

		int multmin = limit / 10;
		while (i < len) {
			// Accumulating negatively avoids surprises near MAX_VALUE
			int digit = getDigitFromChar(s.charAt(i++));
			if (digit < 0) {
				return false;
			}
			if (result < multmin) {
				return false;
			}
			result *= 10;
			if (result < limit + digit) {
				return false;
			}
			result -= digit;
		}

		// Real result: negative ? result : -result;
		return true;
	}

	private static int getDigitFromChar(char c) {
		if (c >= '0' && c <= '9') {
			return (c - '0');
		} else {
			return -1;
		}
	}

	/**
	 * Represents a unique string value. We use entries of this kind internally, until the final
	 * results are calculated.
	 */
	private static class InternalEntry {
		// A unique id for this string
		final int uniqueId;
		// Number of instances of this string
		int nStringInst;
		// Number of backing char arrays for this string
		int nBackingArrs;
		// Total size, in bytes, of all backing arrays for this string.
		int totalBackingArrSize;
		// Max backing array length, in chars, for this string
		private int maxArrayLen;

		InternalEntry(int uniqueId) {
			this.uniqueId = uniqueId;
		}
	}
}
