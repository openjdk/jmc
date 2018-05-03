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
package org.openjdk.jmc.joverflow.util;

import java.lang.ref.WeakReference;

/**
 * Functionality for custom interning (deduplicating) strings. The main API, internString() and
 * internStringArrayContents(), uses our own custom interning technique, which maintains a
 * relatively small, fixed-size table of strings. Unlike with String.intern(), our goal is not to
 * return the same, "canonical" String instance for every string, but rather to reduce the number of
 * duplicate strings (not necessarily eliminating all of them), and at the same time avoid CPU and
 * memory performance penalty as much as possible.
 * <p>
 * To reduce CPU overhead, synchronization in our table is kept to a minimum, which means that two
 * threads may end up writing strings with the same value to the table one after another. Also, we
 * avoid growing the table and/or finding how to evict some strings when the table is full. That can
 * be done using WeakReferences, or by always replacing the old value with the new one when there is
 * a hash collision for two strings with different values. This should ultimately help to prevent
 * keeping around for too long strings that are not used anywhere anymore.
 */
@SuppressWarnings("unchecked")
public class StringInterner {

	private static final WeakReference<String> table[];
	private static int size;
	private static final int threshold;
	private static volatile int numCalls, numResets;

	static {
		// Set maximum intern table size such that it generally does not grow over
		// more than 1 per cent of max heap size, assuming a single string in it
		// takes rougly 100 bytes.
		int length = (int) (Runtime.getRuntime().totalMemory() / 100 / 100);
		length = Integer.highestOneBit(length); // Round down to a power of 2
		table = new WeakReference[length];
		threshold = length * 3 / 4;
	}

	// The implementation based on WeakReferences, that seems to work best
	public static String internString(String s) {
		if (s == null) {
			return null;
		}

		numCalls++; // A very weak form of synchronization

		int index = s.hashCode();
		index = hash(index);
		index = index & (table.length - 1);

		int gapIdx = -1;

		WeakReference<String> entry;
		while ((entry = table[index]) != null) {
			String cachedValue = entry.get();
			if (cachedValue == null) {
				if (gapIdx == -1) {
					gapIdx = index;
				}
				index = (index + 1) & (table.length - 1);
			} else if (cachedValue.equals(s)) {
				return cachedValue;
			} else {
				index = (index + 1) & (table.length - 1);
			}
		}

		if (size > threshold && gapIdx == -1) {
			for (int i = 0; i < table.length; i++) {
				table[i] = null;
			}
			size = 0;
			numResets++;
		}

		if (gapIdx != -1) {
			index = gapIdx;
		} else {
			size++;
		}
		table[index] = new WeakReference<>(s);
		return s;
	}

	public static Object internStringInObjectRef(Object obj) {
		if (!(obj instanceof String)) {
			return obj;
		}

		return internString((String) obj);
	}

	public static String[] internStringArrayContents(String[] arr) {
		if (arr == null) {
			return null;
		}

		for (int i = 0; i < arr.length; i++) {
			String result = internString(arr[i]);
			if (result != null) { // Very limited protection from concurrent updates
				arr[i] = result;
			}
		}
		return arr;
	}

	public static Object[] internStringsInObjectArray(Object[] arr) {
		if (arr == null) {
			return null;
		}

		for (int i = 0; i < arr.length; i++) {
			Object result = internStringInObjectRef(arr[i]);
			if (result != null) { // Very limited protection from concurrent updates
				arr[i] = result;
			}
		}
		return arr;
	}

	public static void printInternStats() {
		int numNullEntries = 0;
		synchronized (table) {
			for (WeakReference<String> tableEntry : table) {
				if (tableEntry == null) {
					numNullEntries++;
				}
			}
		}
		System.out.println("Table size: " + table.length + ", null entries: " + numNullEntries);
		System.out.println("Num calls (may be off due to overflowing): " + numCalls);
		System.out.println("Num resets: " + numResets);
	}

	/**
	 * Algorithm from Thomas Wang.
	 */
	private static int hash(int h) {
		int key = (h ^ 61) ^ (h >>> 16);
		key = key + (key << 3);
		key = key ^ (key >>> 4);
		key = key * 0x27d4eb2d; // a prime or an odd constant
		key = key ^ (key >>> 15);
		return key;
	}
}
