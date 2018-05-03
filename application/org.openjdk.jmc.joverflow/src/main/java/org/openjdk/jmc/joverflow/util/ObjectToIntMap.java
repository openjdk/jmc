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

import java.util.Arrays;
import java.util.Comparator;

/**
 * A simple, low-memory-overhead hash map that maps an Object to a single int value. It is intended
 * to be used to count the number of instances or some other metrics associated with a given object.
 */
public class ObjectToIntMap<K> implements Cloneable {
	private K[] keys;
	private int[] values;
	private int size, capacity, threshold;
	private long rehashTime;

	public ObjectToIntMap(int initialCapacity) {
		capacity = initialCapacity | 1;
		createTable();
	}

	public void put(K key, int value) {
		int idx = hash(key) % capacity;
		while (keys[idx] != null) {
			if (keys[idx].equals(key)) {
				values[idx] = value;
				return;
			}
			idx = (idx + 1) % capacity;
		}

		keys[idx] = key;
		values[idx] = value;
		size++;
		if (size > threshold) {
			rehash();
		}
	}

	/**
	 * Returns the value for the given key. If this key is not present in the map, returns -1.
	 */
	public int get(K key) {
		int idx = hash(key) % capacity;
		while (keys[idx] != null) {
			if (keys[idx].equals(key)) {
				return values[idx];
			}
			idx = (idx + 1) % capacity;
		}
		return -1;
	}

	/**
	 * If the given key is not present in this table, this is equivalent to calling put(key, 1).
	 * Otherwise, takes the existing value for key and increments it by one.
	 */
	public void putOneOrIncrement(K key) {
		int idx = hash(key) % capacity;
		while (keys[idx] != null) {
			if (keys[idx].equals(key)) {
				values[idx]++;
				return;
			}
			idx = (idx + 1) % capacity;
		}

		keys[idx] = key;
		values[idx] = 1;
		size++;
		if (size > threshold) {
			rehash();
		}
	}

	/**
	 * If the given key is not present in this table, this is equivalent to calling put(key, value).
	 * Otherwise, takes the existing value for key and increments it by the input value.
	 */
	public void putOrIncrementBy(K key, int value) {
		int idx = hash(key) % capacity;
		while (keys[idx] != null) {
			if (keys[idx].equals(key)) {
				values[idx] += value;
				return;
			}
			idx = (idx + 1) % capacity;
		}

		keys[idx] = key;
		values[idx] = value;
		size++;
		if (size > threshold) {
			rehash();
		}
	}

	@SuppressWarnings("unchecked")
	public Entry<K>[] getEntries() {
		Entry<K>[] result = new Entry[size];
		int entryIdx = 0;
		for (int i = 0; i < capacity; i++) {
			if (keys[i] != null) {
				result[entryIdx++] = new Entry<>(keys[i], values[i]);
			}
		}

		return result;
	}

	public Entry<K>[] getEntriesSortedByValueThenKey() {
		Entry<K>[] result = getEntries();

		Arrays.sort(result, new Comparator<Entry<K>>() {
			@Override
			@SuppressWarnings("unchecked")
			public int compare(Entry<K> e1, Entry<K> e2) {
				if (e1.value > e2.value) {
					return -1;
				} else if (e1.value < e2.value) {
					return 1;
				} else {
					// Values are the same - try to compare keys to ensure order is stable
					if (e1.key instanceof Comparable) {
						return ((Comparable<K>) e1.key).compareTo(e2.key);
					} else {
						return 0;
					}
				}
			}
		});

		return result;
	}

	public static class Entry<K> {
		public K key;
		public int value;

		private Entry(K key, int value) {
			this.key = key;
			this.value = value;
		}
	}

	public int size() {
		return size;
	}

	private int hash(K key) {
		return key.hashCode() & Integer.MAX_VALUE;
	}

	private void rehash() {
		long time = System.currentTimeMillis();
		K[] oldKeys = keys;
		int[] oldValues = values;

		capacity = (capacity * 3 / 2) | 1;
		createTable();

		for (int i = 0; i < oldKeys.length; i++) {
			if (oldKeys[i] != null) {
				put(oldKeys[i], oldValues[i]);
			}
		}
		rehashTime += System.currentTimeMillis() - time;
	}

	@SuppressWarnings("unchecked") // For the (K[]) cast below
	private void createTable() {
		threshold = capacity / 4 * 3;
		size = 0;
		keys = (K[]) (new Object[capacity]);
		values = new int[capacity];
	}

	public long getRehashTimeMillis() {
		return rehashTime;
	}

	@Override
	@SuppressWarnings("unchecked")
	public ObjectToIntMap<K> clone() {
		try {
			return (ObjectToIntMap<K>) super.clone();
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}
}
