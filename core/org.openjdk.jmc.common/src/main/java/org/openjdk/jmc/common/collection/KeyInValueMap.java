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
package org.openjdk.jmc.common.collection;

import java.util.Iterator;

/**
 * A map for values that has the key inside the value, so only storing the value directly without a
 * wrapping map entry is sufficient. It can also compute absent values which is not available in
 * Java 7 maps.
 * <p>
 * Does not support null keys or values. Not thread safe.
 *
 * @param <K>
 *            key type
 * @param <V>
 *            value type
 */
public abstract class KeyInValueMap<K, V> implements Iterable<V> {

	private Object[] values;
	private int size;
	private int capacity;
	private int threshold;
	final private float loadFactor;

	/**
	 * Create a new map.
	 * 
	 * @param initialCapacity
	 *            initial storage capacity
	 * @param loadFactor
	 *            load factor at which to increase the internal storage capacity
	 */
	public KeyInValueMap(int initialCapacity, float loadFactor) {
		this.loadFactor = loadFactor;
		createTable(initialCapacity);
	}

	/**
	 * Get the value for a key and optionally compute a new value if it is not already present in
	 * the map. Automatic value computation is done with {@link #computeValue(Object)} which must be
	 * implemented by subclasses.
	 *
	 * @param key
	 *            key
	 * @param computeIfAbsent
	 *            If a value is not found and this is set to {@code true}, then compute and add a
	 *            new value using {@link #computeValue(Object)}.
	 * @return The value for the key. If computeIfAbsent is {@code false} and no matching value
	 *         exists, then {@code null} is returned.
	 */
	public V get(K key, boolean computeIfAbsent) {
		int idx = getIndex(hashKey(key));
		while (values[idx] != null) {
			V e = getValue(idx);
			if (isKeyFor(key, e)) {
				return e;
			}
			idx = (idx + 1) % capacity;
		}
		if (computeIfAbsent) {
			V entry = computeValue(key);
			values[idx] = entry;
			size++;
			if (size > threshold) {
				rehash();
			}
			return entry;
		} else {
			return null;
		}
	}

	public int size() {
		return size;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public Iterator<V> iterator() {
		return (Iterator) IteratorToolkit.skipNulls(IteratorToolkit.of(values));
	}

	private void createTable(int newCapacity) {
		capacity = newCapacity | 1;
		threshold = (int) (capacity * loadFactor);
		values = new Object[capacity];
	}

	@SuppressWarnings("unchecked")
	private V getValue(int index) {
		return (V) values[index];
	}

	private void rehash() {
		Object[] oldEntries = values;
		createTable(capacity * 2);
		for (Object oldEntry : oldEntries) {
			if (oldEntry != null) {
				@SuppressWarnings("unchecked")
				V e = (V) oldEntry;
				int idx = getIndex(hashFromValue(e));
				while (values[idx] != null) {
					idx = (idx + 1) % capacity;
				}
				values[idx] = oldEntry;
			}

		}
	}

	private int getIndex(int hash) {
		return (hash & Integer.MAX_VALUE) % capacity;
	}

	/**
	 * This method must be overridden in subclasses so that values can be checked if they match a
	 * given key. Called by {@link #get(Object, boolean)} when there are multiple values that share
	 * the same key hash.
	 * 
	 * @param key
	 *            key to check
	 * @param value
	 *            value to check
	 * @return {@code true} if {@code key} is the key for {@code value}, {@code false} otherwise
	 */
	private boolean isKeyFor(K key, V value) {
		return getKey(value).equals(key);
	}

	/**
	 * This method must be overridden in subclasses so that values can be computed for missing keys.
	 * Called by {@link #get(Object, boolean)} when the requested key is missing in the map.
	 * 
	 * @param key
	 *            key to calculate value for
	 * @return calculated value
	 */
	protected abstract V computeValue(K key);

	/**
	 * Calculate the hash for a key. May be overridden by subclasses.
	 * 
	 * @param key
	 *            key to calculate hash for
	 * @return hash for key
	 */
	protected int hashKey(K key) {
		return key.hashCode();
	}

	private int hashFromValue(V value) {
		return hashKey(getKey(value));
	};

	/**
	 * Get the key for a value.
	 * 
	 * @param value
	 *            value to get key for
	 * @return key for value
	 */
	protected abstract K getKey(V value);
}
