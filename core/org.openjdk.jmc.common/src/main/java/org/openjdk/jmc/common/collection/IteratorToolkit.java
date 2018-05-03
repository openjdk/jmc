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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.openjdk.jmc.common.IPredicate;

/**
 * Various methods that work with iterators.
 */
public class IteratorToolkit {

	/**
	 * Place all elements of an iterator in a list.
	 *
	 * @param <T>
	 *            input iterator type
	 * @param itr
	 *            iterator
	 * @param sizeHint
	 *            a hint of how many elements there are
	 * @return a new list with all elements from the iterator
	 */
	public static <T> List<T> toList(Iterator<T> itr, int sizeHint) {
		List<T> list = new ArrayList<>(sizeHint);
		while (itr.hasNext()) {
			list.add(itr.next());
		}
		return list;
	}

	/**
	 * Wrap an iterator in a new iterator that skips all null values.
	 *
	 * @param <T>
	 *            input iterator type
	 * @param itr
	 *            input iterator that may produce null values
	 * @return a new iterator that will never produce null values
	 */
	public static <T> Iterator<T> skipNulls(Iterator<T> itr) {
		return filter(itr, new IPredicate<T>() {

			@Override
			public boolean evaluate(T o) {
				return o != null;
			}
		});
	}

	/**
	 * Wrap an iterator in a new iterator that filters out values based on a predicate.
	 *
	 * @param <T>
	 *            input iterator type
	 * @param itr
	 *            input iterator
	 * @param filter
	 *            filter predicate
	 * @return a new iterator that only contains values where the filter evaluates to true
	 */
	public static <T> Iterator<T> filter(final Iterator<T> itr, final IPredicate<? super T> filter) {
		return new AbstractIterator<T>() {

			@Override
			protected T findNext() {
				while (itr.hasNext()) {
					T object = itr.next();
					if (filter.evaluate(object)) {
						return object;
					}
				}
				return NO_MORE_ELEMENTS;
			}
		};
	}

	/**
	 * Iterator that iterates over an array. Hopefully faster than Arrays.asList(...).iterator()
	 * since there are no concurrency checks.
	 *
	 * @param <T>
	 *            input iterator type
	 * @param elements
	 *            elements to iterate over
	 * @return an iterator
	 */
	public static <T> Iterator<T> of(T[] elements) {
		return of(elements, 0, elements.length);
	}

	/**
	 * Iterator that iterates over a part of an array. Hopefully faster than
	 * Arrays.asList(...).iterator() since there are no concurrency checks.
	 *
	 * @param <T>
	 *            input iterator type
	 * @param elements
	 *            elements to iterate over
	 * @param offset
	 *            array index to start the iterator on
	 * @param len
	 *            number of elements to iterate over
	 * @return an iterator
	 */
	static <T> Iterator<T> of(final T[] elements, final int offset, final int len) {
		if (offset < 0 || offset > len || len > elements.length) {
			throw new IllegalArgumentException("Cannnot contruct iterator with offset=" + offset + " and len=" + len //$NON-NLS-1$ //$NON-NLS-2$
					+ " for an array of length " + elements.length); //$NON-NLS-1$
		}
		return new Iterator<T>() {
			private int index = offset;

			@Override
			public boolean hasNext() {
				return index != len;
			}

			@Override
			public T next() {
				try {
					return elements[index++];
				} catch (IndexOutOfBoundsException e) {
					throw new NoSuchElementException();
				}
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("Iterator doesn't support removal."); //$NON-NLS-1$
			}
		};
	}

}
