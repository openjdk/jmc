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
 * Superclass for IntToObjectMap and LongToObjectMap, with their common functionality.
 */
public abstract class NumberToObjectMap<V> {

	protected V[] values;

	protected int size, capacity, threshold;

	// Element linking support
	protected final boolean linked;
	protected int[] nextElement;
	protected int firstElementIdx, prevAddedElementIdx;

	private volatile Collection<V> valuesCollectionView = null;

	// Debugging
	protected long rehashTime, numRehashes;

	protected NumberToObjectMap(int initialCapacity, boolean linked) {
		if (initialCapacity < 11) {
			initialCapacity = 11; // Protect ourselves from stupidly small capacity
		}
		capacity = initialCapacity | 1;
		this.linked = linked;
		createTable();
	}

	public abstract void put(long key, V value);

	public abstract V get(long key);

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

	protected void finishPut(int idx) {
		if (linked) {
			if (prevAddedElementIdx == -1) {
				firstElementIdx = idx;
			} else {
				nextElement[prevAddedElementIdx] = idx;
				nextElement[idx] = -1;
			}
			prevAddedElementIdx = idx;
		}

		size++;
		if (size > threshold) {
			rehash();
		}
	}

	protected abstract void rehash();

	protected abstract void createTable();

	public long getRehashTimeMillis() {
		return rehashTime;
	}

	public long getNumRehashes() {
		return numRehashes;
	}

	private final class Values extends AbstractCollection<V> {

		@Override
		public Iterator<V> iterator() {
			return linked ? new LinkedValueIterator() : new ValueIterator();
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

	private final class LinkedValueIterator implements Iterator<V> {

		private int index; // Current slot

		LinkedValueIterator() {
			index = firstElementIdx;
		}

		@Override
		public boolean hasNext() {
			return index != -1;
		}

		@Override
		public V next() {
			if (index == -1) {
				throw new NoSuchElementException();
			}

			V v = values[index];
			index = nextElement[index];

			return v;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

}
