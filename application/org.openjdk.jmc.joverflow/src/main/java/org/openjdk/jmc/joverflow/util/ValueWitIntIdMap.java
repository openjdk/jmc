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

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A highly specialized hash map implementation, that takes only objects implementing ValueWithIntId
 * interface, i.e. objects that store a stable, unique int ID internally. The ID should be unique at
 * least within the set of objects stored in this map, because the map does not compare the objects
 * themselves - only their IDs. Since the ID is already stored in the object, there is no need to
 * store it in the map itself, which saves memory.
 * <p>
 * The internal array management depends on that the size is a power of two, since the hash code is
 * calculated using shift operations.
 */
public class ValueWitIntIdMap<V extends ValueWithIntId> {
	// Capacity MUST be power of two
	private static final int MINIMUM_CAPACITY = 4;
	private static final int MAXIMUM_CAPACITY = 1 << 29;

	private V[] values;
	private int size, capacity, threshold;

	volatile Collection<V> valuesCollectionView = null;

	private long rehashTime; // Debugging

	public ValueWitIntIdMap(int expectedMaxSize) {
		// Compute min capacity for expectedMaxSize given a load factor of 2/3
		int minCapacity = (3 * expectedMaxSize) / 2;

		// Compute the appropriate capacity
		if (minCapacity > MAXIMUM_CAPACITY || minCapacity < 0) {
			capacity = MAXIMUM_CAPACITY;
		} else {
			capacity = MINIMUM_CAPACITY;
			while (capacity < minCapacity) {
				capacity <<= 1;
			}
		}
		createTable();
	}

	public void put(V value) {
		int key = value.getId();
		int idx = hash(key);
		while (values[idx] != null) {
			if (values[idx].getId() == key) {
				values[idx] = value;
				return;
			}
			idx = (idx + 1) % capacity;
		}

		values[idx] = value;

		size++;
		if (size > threshold) {
			rehash();
		}
	}

	public V get(int key) {
		int idx = hash(key);
		while (values[idx] != null) {
			if (values[idx].getId() == key) {
				return values[idx];
			}
			idx = (idx + 1) % capacity;
		}
		return null;
	}

	public int size() {
		return size;
	}

	public int capacity() {
		return capacity;
	}

	public Collection<V> values() {
		Collection<V> vs = valuesCollectionView;
		return (vs != null ? vs : (valuesCollectionView = new Values()));
	}

	private int hash(int v) {
		return (v - (v << 7)) & (capacity - 1);
	}

	private void rehash() {
		if (capacity == MAXIMUM_CAPACITY) {
			return;
		}

		long time = System.currentTimeMillis();
		V[] oldValues = values;

		capacity <<= 1;
		createTable();

		for (int i = 0; i < oldValues.length; i++) {
			if (oldValues[i] != null) {
				put(oldValues[i]);
			}
		}

		rehashTime += System.currentTimeMillis() - time;
	}

	@SuppressWarnings("unchecked") // Just for the (V[]) cast below
	private void createTable() {
		threshold = capacity / 4 * 3;
		size = 0;
		values = (V[]) (new ValueWithIntId[capacity]);
	}

	public long getRehashTimeMillis() {
		return rehashTime;
	}

	private final class Values extends AbstractCollection<V> {

		@Override
		public Iterator<V> iterator() {
			return new ValueIterator();
		}

		@Override
		public int size() {
			return size;
		}

		@Override
		public boolean contains(Object o) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}
	}

	private final class ValueIterator implements Iterator<V> {
		private V next; // Next entry to return
		private int index; // Current slot

		ValueIterator() {
			if (size > 0) { // Advance to first entry
				V[] t = values;
				while (index < t.length && (next = t[index]) == null) {
					index++;
				}
			}
		}

		@Override
		public boolean hasNext() {
			return next != null;
		}

		@Override
		public V next() {
			V v = next;
			if (v == null) {
				throw new NoSuchElementException();
			}

			next = null;
			index++;
			V[] t = values;
			while (index < t.length && (next = t[index]) == null) {
				index++;
			}

			return v;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
