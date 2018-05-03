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
import java.util.List;

import org.openjdk.jmc.joverflow.util.IntToObjectMap;

/**
 * A histogram that groups objects (for example, strings or arrays) by their length, and for each
 * length keeps the count and total size of the respective objects.
 */
public class LengthHistogram {
	private final ArrayList<Entry> values;

	private LengthHistogram(ArrayList<Entry> values) {
		this.values = values;
	}

	/**
	 * Get a list of entries, sorted in ascending order by length, and "pruned" so that all entries
	 * with size < sizeThreshold are combined into a single entry with length ==
	 * Entry.SPECIAL_VALUE.
	 */
	public List<Entry> getPrunedAndSortedEntries(int sizeThreshold) {
		ArrayList<Entry> result = new ArrayList<>(values.size() / 2);
		Entry entryForOthers = new Entry(Entry.SPECIAL_VALUE);
		for (Entry entry : values) {
			if (entry.getSize() >= sizeThreshold) {
				result.add(entry);
			} else {
				entryForOthers.addEntry(entry);
			}
		}
		result.add(entryForOthers);

		Collections.sort(result, SPECIAL_LENGTH_COMPARATOR);
		return result;
	}

	private static final Comparator<Entry> SPECIAL_LENGTH_COMPARATOR = new Comparator<LengthHistogram.Entry>() {
		@Override
		public int compare(Entry o1, Entry o2) {
			if (o1.length == Entry.SPECIAL_VALUE) {
				return 1;
			} else if (o2.length == Entry.SPECIAL_VALUE) {
				return -1;
			} else {
				return o1.length - o2.length;
			}
		}
	};

	public static class Builder {
		private final IntToObjectMap<Entry> lenToEntry;

		public Builder(int capacity) {
			lenToEntry = new IntToObjectMap<>(capacity, false);
		}

		/** Add an object with the given length and size */
		public void addInstance(int length, int size) {
			Entry entry = lenToEntry.get(length);
			if (entry == null) {
				entry = new Entry(length);
				lenToEntry.put(length, entry);
			}
			entry.addInstance(size);
		}

		public LengthHistogram build() {
			ArrayList<Entry> result = new ArrayList<>(lenToEntry.size());
			result.addAll(lenToEntry.values());
			return new LengthHistogram(result);
		}

		public int size() {
			return lenToEntry.size();
		}
	}

	public static class Entry {
		public static final int SPECIAL_VALUE = -1;

		private final int length;
		private int count;
		private long size;

		private Entry(int length) {
			this.length = length;
		}

		private void addInstance(int instSize) {
			count++;
			size += instSize;
		}

		private void addEntry(Entry other) {
			count += other.count;
			size += other.size;
		}

		public int getLength() {
			return length;
		}

		public int getCount() {
			return count;
		}

		public long getSize() {
			return size;
		}
	}
}
