/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * A map from long to T. Gives O(1) access to indexes that are between 0 and
 * {@code pageSize*maxPageCount} at the cost of high memory use. Values are kept in dynamically
 * allocated pages, each of a fixed size.
 * <p>
 * It can be thought of as a big array that is split up into pages of a fixed size. This is useful
 * if you want to use a sparse set of indexes since you don't have to allocate the full array
 * immediately. If you are going to fill all indexes then it is more practical to use a normal array
 * or list.
 * <p>
 * If you try to access an index outside of the max page count then an overflow hash map is used as
 * a fallback mechanism. In that case access will be slower than O(1).
 *
 * @param <T>
 *            type of objects to store in this map
 */
public class FastAccessNumberMap<T> implements Iterable<T> {

	private final int pagesUpperLimit;
	private Page[] pages;
	private Map<Long, T> overflow;

	/**
	 * Constructs a map with O(1) access up to approximately index 5000.
	 */
	public FastAccessNumberMap() {
		this(5000);
	}

	/**
	 * Constructs a map with O(1) access up to index {@code expectedSize}.
	 *
	 * @param expectedSize
	 *            - the maximum number of elements expected to be inserted into the map before
	 *            overflowing to slower storage.
	 */
	public FastAccessNumberMap(int expectedSize) {
		// round up to the next multiple of Page.SIZE
		this.pagesUpperLimit = (expectedSize + Page.SIZE - 1) & -Page.SIZE;
		this.pages = new Page[1];
	}

	/**
	 * Constructs a map with O(1) access up to index {@code pageSize*maxPageCount}.
	 *
	 * @param pageSize
	 *            page size
	 * @param maxPageCount
	 *            max page count
	 */
	public FastAccessNumberMap(int pageSize, int maxPageCount) {
		this(pageSize * maxPageCount);
	}

	private Page getPage(int pageIndex) {
		if (pages.length <= pageIndex) {
			pages = Arrays.copyOf(pages, pageIndex + 1);
		}
		Page page = pages[pageIndex];
		if (page == null) {
			page = new Page();
			pages[pageIndex] = page;
		}
		return page;

	}

	private T getLow(int index) {
		// masking by Page.SIZE - 1 is equivalent to % Page.SIZE
		Object value = getPage(index / Page.SIZE).get(index & (Page.SIZE - 1));
		@SuppressWarnings("unchecked")
		T tValue = (T) value;
		return tValue;
	}

	private void putLow(int index, T object) {
		// masking by Page.SIZE - 1 is equivalent to % Page.SIZE
		getPage(index / Page.SIZE).set(index & (Page.SIZE - 1), object);
	}

	/**
	 * Get the value at an index.
	 *
	 * @param index
	 *            value index
	 * @return value at index
	 */
	public T get(long index) {
		if (index >= 0 && index < pagesUpperLimit) {
			return getLow((int) index);
		} else {
			return getOverflowMap().get(index);
		}
	}

	/**
	 * Store a value at an index.
	 *
	 * @param index
	 *            value index
	 * @param value
	 *            value to store
	 */
	public void put(long index, T value) {
		if (index >= 0 && index < pagesUpperLimit) {
			putLow((int) index, value);
		} else {
			getOverflowMap().put(index, value);
		}
	}

	private Map<Long, T> getOverflowMap() {
		if (overflow == null) {
			overflow = new HashMap<>();
		}
		return overflow;
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			T next;
			Iterator<Page> pageIterator = IteratorToolkit.of(pages);
			PageIteratorFlyweight elementIterator = new PageIteratorFlyweight();
			Iterator<T> highIterator = overflow == null ? Collections.<T> emptyList().iterator()
					: overflow.values().iterator();

			@Override
			public boolean hasNext() {
				while (next == null) {
					if (!elementIterator.hasNext()) {
						if (pageIterator.hasNext()) {
							Page nextPage = pageIterator.next();
							if (nextPage != null) {
								elementIterator.wrap(nextPage);
							}
						} else if (highIterator.hasNext()) {
							next = highIterator.next();
						} else {
							return false;
						}
					} else {
						@SuppressWarnings("unchecked")
						T value = (T) elementIterator.next();
						next = value;
					}
				}
				return true;
			}

			@Override
			public T next() {
				if (hasNext()) {
					T tmp = next;
					next = null;
					return tmp;
				}
				throw new NoSuchElementException();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	private static final class Page {
		// choose a fixed power of 2 so that divisions and mods can be replaced by shifts and masks
		public static final int SIZE = 64;
		private long mask;
		private final Object[] values;

		Page() {
			this.values = new Object[SIZE];
		}

		public Object get(int index) {
			return values[index];
		}

		public void set(int index, Object value) {
			values[index] = value;
			// mark presence of the element so iteration can skip nulls quickly
			mask |= (1L << index);
		}
	}

	private static final class PageIteratorFlyweight implements Iterator<Object> {
		private long mask;
		private Object[] values;

		public void wrap(Page page) {
			this.mask = page.mask;
			this.values = page.values;
		}

		@Override
		public boolean hasNext() {
			// when there are no bits left, the page has been iterated entirely
			return mask != 0;
		}

		@Override
		public Object next() {
			// get the index of the lowest bit, then switch it off
			int index = Long.numberOfTrailingZeros(mask);
			mask &= (mask - 1);
			return values[index];
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
