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
package org.openjdk.jmc.common.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import org.openjdk.jmc.common.collection.SimpleArray;

/**
 * A collection of items that are divided in a sorted head and an unsorted tail. Keeps the head
 * sorted according to comparator. Other objects are unsorted in tail.
 *
 * @param <T>
 *            type of items to hold in the collection
 */
public class SortedHead<T> {

	private final T[] head;
	private final T[] candidates;
	private int candidateCount;
	private T limit;
	private final Comparator<? super T> comparator;
	private final SimpleArray<T> tail;

	private SortedHead(T[] head, SimpleArray<T> tail, Comparator<? super T> comparator) {
		if (head.length < 1) {
			throw new IllegalArgumentException("Cannot use SortedHead with 0 size"); //$NON-NLS-1$
		} else if (comparator == null) {
			throw new IllegalArgumentException("Comparator is null"); //$NON-NLS-1$
		}
		Arrays.sort(head, comparator);
		candidates = head.clone();
		limit = head[head.length - 1];
		this.tail = tail;
		this.head = head;
		this.comparator = comparator;
	}

	/**
	 * Create a new collection with an initial head.
	 * 
	 * @param head
	 *            Head items. The array must contain at least one item but does not need to be
	 *            sorted. This array will be sorted every time the collection is added to with
	 *            {@link #addObject(Object)}.
	 * @param comparator
	 *            comparator to use when sorting
	 */
	public SortedHead(T[] head, Comparator<? super T> comparator) {
		this(head, new SimpleArray<>(head.clone()), comparator);
	}

	/**
	 * Get the tail items.
	 * 
	 * @return an array with the tail items
	 */
	public T[] getTail() {
		updateHead();
		return tail.elements();
	}

	/**
	 * Add an item to the collection.
	 * 
	 * @param o
	 *            item to add
	 */
	public void addObject(T o) {
		boolean isBeforeLimit = comparator.compare(o, limit) < 0;
		if (isBeforeLimit) {
			if (candidateCount >= candidates.length) {
				updateHead();
				isBeforeLimit = comparator.compare(o, limit) < 0;
			}
		}
		if (isBeforeLimit) {
			candidates[candidateCount++] = o;
		} else {
			tail.add(o);
		}

	}

	private void updateHead() {
		Arrays.sort(candidates, 0, candidateCount, comparator);
		T[] tmp = head.clone();
		int p = 0;
		int q = 0;
		for (int i = 0; i < head.length; i++) {
			if (p < candidateCount) {
				T c = candidates[p];
				T s = tmp[q];
				if (comparator.compare(c, s) < 0) {
					head[i] = c;
					p++;
				} else {
					head[i] = s;
					q++;
				}
			} else {
				int left = head.length - i;
				System.arraycopy(tmp, q, head, i, left);
				q += left;
				break;
			}
		}
		for (int i = p; i < candidateCount; i++) {
			tail.add(candidates[i]);
		}
		for (int i = q; i < head.length; i++) {
			tail.add(tmp[i]);
		}
		limit = head[head.length - 1];
		candidateCount = 0;
	}

	/**
	 * Convenience method for sorting items into a head of fixed size and a growable tail. There is
	 * no return value from the method, the result is instead located in the head and tail arguments
	 * that are modified by the call.
	 * 
	 * @param sourceIterator
	 *            iterator that gives items to sort
	 * @param fullHead
	 *            An array with current head items. The array may be modified by this method.
	 * @param tail
	 *            A growable tail. It may contain items at the start of the method call and items
	 *            that do not fit in the head are placed here.
	 * @param comparator
	 *            comparator to use when sorting
	 */
	public static <T> void addSorted(
		Iterator<? extends T> sourceIterator, T[] fullHead, SimpleArray<T> tail, Comparator<? super T> comparator) {
		SortedHead<T> sorter = new SortedHead<>(fullHead, tail, comparator);
		while (sourceIterator.hasNext()) {
			sorter.addObject(sourceIterator.next());
		}
		sorter.updateHead();
	}
}
