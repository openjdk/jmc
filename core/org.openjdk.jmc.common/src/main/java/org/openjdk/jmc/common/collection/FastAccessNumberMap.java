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
	private final int pageSize;
	private Object[][] pages;
	private Map<Long, T> overflow;

	/**
	 * Constructs a map with O(1) access up to index 5000.
	 */
	public FastAccessNumberMap() {
		this(100, 50);
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
		this.pagesUpperLimit = pageSize * maxPageCount;
		this.pageSize = pageSize;
		this.pages = new Object[1][];
	}

	private Object[] getPage(int pageIndex) {
		if (pages.length <= pageIndex) {
			pages = Arrays.copyOf(pages, pageIndex + 1);
		}
		Object[] page = pages[pageIndex];
		if (page == null) {
			page = new Object[pageSize];
			pages[pageIndex] = page;
		}
		return page;

	}

	private T getLow(int index) {
		Object value = getPage(index / pageSize)[index % pageSize];
		@SuppressWarnings("unchecked")
		T tValue = (T) value;
		return tValue;
	}

	private void putLow(int index, T object) {
		getPage(index / pageSize)[index % pageSize] = object;
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
			Iterator<Object[]> pageIterator = IteratorToolkit.of(pages);
			Iterator<Object> elementIterator;
			Iterator<T> highIterator = overflow == null ? Collections.<T> emptyList().iterator()
					: overflow.values().iterator();

			@Override
			public boolean hasNext() {
				while (next == null) {
					if (elementIterator == null || !elementIterator.hasNext()) {
						if (pageIterator.hasNext()) {
							Object[] nextPage = pageIterator.next();
							if (nextPage != null) {
								elementIterator = IteratorToolkit.of(nextPage);
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
}
