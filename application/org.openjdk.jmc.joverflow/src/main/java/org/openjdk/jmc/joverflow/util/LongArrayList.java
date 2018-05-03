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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A simple growable array of longs.
 */
public class LongArrayList implements Iterable<Long> {
	private static final long[] EMPTY_ARRAY = new long[0];

	private long[] array;
	private int size;

	public LongArrayList(int capacity) {
		array = new long[capacity];
	}

	public long get(int idx) {
		return array[idx];
	}

	public int size() {
		return size;
	}

	public void add(long value) {
		if (size == array.length) {
			long[] oldArray = array;
			array = new long[oldArray.length * 2];
			System.arraycopy(oldArray, 0, array, 0, oldArray.length);
		}
		array[size++] = value;
	}

	public boolean contains(long v) {
		for (int i = 0; i < size; i++) {
			if (array[i] == v) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the internal array, with size unchanged. Useful for quick inspection of contents, if
	 * we don't want to store them long-term.
	 */
	public long[] internalArray() {
		return array;
	}

	/** Returns a copy of the contents of the internal array, with exact size. */
	public long[] toArray() {
		if (size == 0) {
			return EMPTY_ARRAY;
		}

		long[] result = new long[size];
		System.arraycopy(array, 0, result, 0, size);
		return result;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public void clear() {
		size = 0;
	}

	@Override
	public LongArrayListIterator iterator() {
		return new LongArrayListIterator(this);
	}

	/** Iterator for iterating over an IntArrayList */
	public static class LongArrayListIterator implements Iterator<Long> {
		private final LongArrayList list;
		private int index;

		public LongArrayListIterator(LongArrayList list) {
			this.list = list;
		}

		@Override
		public boolean hasNext() {
			return index >= 0 && index < list.size();
		}

		@Override
		public Long next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			return list.get(index++);
		}

		/** Unsupported on LongArrayList */
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
