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
import java.util.Iterator;

/**
 * An array that can be iterated over.
 * <p>
 * This class is useful in a few cases:
 * <ul>
 * <li>You expect that there are a large, but unknown, number of elements that will be added. This
 * class grows its internal storage array more aggressively to avoid multiple array copying. The
 * back side to this is that it may waste more memory.
 * <li>You want to reuse a preallocated array.
 * </ul>
 * If you don't have any of those needs, then you might as well use a standard ArrayList instead.
 *
 * @param <T>
 *            type of elements in the array
 */
public class SimpleArray<T> implements Iterable<T> {

	private T[] elements;
	private int size;

	/**
	 * Create an instance backed by an array. The array content will be overwritten with elements
	 * that are subsequently added.
	 *
	 * @param initial
	 *            array to use for storage
	 */
	public SimpleArray(T[] initial) {
		this(initial, 0);
	}

	/**
	 * Create an instance backed by an array with a specified number of preallocated elements. The
	 * array content after the preallocated elements will be overwritten with elements that are
	 * subsequently added.
	 *
	 * @param initial
	 *            array to use for storage
	 * @param size
	 *            number of preallocated elements
	 */
	public SimpleArray(T[] initial, int size) {
		elements = initial;
		this.size = size;
		if (initial.length < size || size < 0) {
			throw new IllegalArgumentException("Invalid number of preallocated elements (" + size //$NON-NLS-1$
					+ "). Must be between 0 and the initial array length (" + initial.length + ")."); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * Get the element at the specified index.
	 *
	 * @param index
	 *            element index
	 * @return element at index
	 */
	public T get(int index) {
		if (index >= size || index < 0) {
			throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return elements[index];
	}

	/**
	 * Add an element to the end of the list.
	 *
	 * @param element
	 *            element to add
	 */
	public void add(T element) {
		if (size + 1 > elements.length) {
			elements = Arrays.copyOf(elements, Math.max(calculateNewCapacity(elements.length), 3));
		}
		elements[size++] = element;
	}

	/**
	 * Add all elements from an array.
	 *
	 * @param src
	 *            array containing elements to add
	 */
	public void addAll(T[] src) {
		addAll(src, 0, src.length);
	}

	/**
	 * Add a portion of elements from an array.
	 *
	 * @param src
	 *            array containing elements to add
	 * @param offset
	 *            offset in array to start adding elements from
	 * @param len
	 *            number of elements from the array to add
	 */
	private void addAll(T[] src, int offset, int len) {
		if (size + len > elements.length) {
			int newCapacity = Math.max(calculateNewCapacity(elements.length), (size + len) * 2);
			elements = Arrays.copyOf(elements, newCapacity);
		}
		System.arraycopy(src, offset, elements, size, len);
		size += len;
	}

	/**
	 * Copy all elements from the backing array to another array. The destination array must be
	 * large enough to fit all elements.
	 *
	 * @param dst
	 *            array to copy elements to
	 * @param offset
	 *            starting position in the destination array
	 */
	public void copyTo(T[] dst, int offset) {
		System.arraycopy(elements, 0, dst, offset, size);
	}

	protected int calculateNewCapacity(int currentCapacity) {
		// Grow aggressively to avoid a lot of array copy
		return currentCapacity * 10;
	}

	/**
	 * Get the number of stored elements. Note that this is not the same as the size of the backing
	 * array.
	 *
	 * @return the number of stored elements
	 */
	public int size() {
		return size;
	}

	/**
	 * Clear all elements. Note that the backing array is not cleared.
	 */
	public void clear() {
		size = 0;
	}

	/**
	 * Get an array with all elements. The array length will be equal to the number of elements.
	 *
	 * @return an array with all elements
	 */
	public T[] elements() {
		if (size < elements.length) {
			elements = Arrays.copyOf(elements, size); // trim to actual size
		}
		return elements;
	}

	@Override
	public Iterator<T> iterator() {
		return IteratorToolkit.of(elements, 0, size);
	}
}
