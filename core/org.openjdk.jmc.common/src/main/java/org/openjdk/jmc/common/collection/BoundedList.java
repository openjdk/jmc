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
import java.util.NoSuchElementException;

/**
 * Ordered bounded list that implements {@link java.lang.Iterable}. It is technically not a list,
 * since it does not implement the {@link java.util.List} interface, but is rather a bounded
 * Iterable.
 * <p>
 * The list has a fixed max size. If more elements are added to it, then the oldest elements will be
 * dropped from it.
 *
 * @param <T>
 *            type of the stored elements
 */
public class BoundedList<T> implements Iterable<T> {
	private int maxSize;
	private int size;
	private INode<T> first;
	private INode<T> last;

	/**
	 * The list elements are stored in nodes that takes care of the actual linking. This interface
	 * can be implemented by a class that is to be stored in a BoundedList in order to avoid
	 * wrapping the values.
	 * <p>
	 * The most obvious implementation of these methods by a class is like this:
	 *
	 * <pre>
	 * <code>
	 * public INode&lt;MyValue&gt; getNext() {
	 *     return next;
	 * }
	 * public void setNext(INode&lt;MyValue&gt; next) {
	 *     this.next = next;
	 * }
	 * public MyValue getValue() {
	 *     return this;
	 * }
	 * </code>
	 * </pre>
	 *
	 * @param <T>
	 *            type of the stored elements
	 */
	public interface INode<T> {
		/**
		 * Get the next node in the list.
		 *
		 * @return the next node
		 */
		INode<T> getNext();

		/**
		 * Set the next node in the list.
		 *
		 * @param next
		 *            the next node
		 */
		void setNext(INode<T> next);

		/**
		 * Get the value of this node.
		 *
		 * @return the node value
		 */
		T getValue();
	}

	/**
	 * Private class used to wrap values as nodes.
	 */
	private static class Node<T> implements INode<T> {
		private final T value;
		private INode<T> next;

		public Node(T value) {
			this.value = value;
		}

		@Override
		public Node<T> getNext() {
			return (Node<T>) next;
		}

		@Override
		public void setNext(INode<T> next) {
			this.next = next;
		}

		@Override
		public T getValue() {
			return value;
		}
	}

	/**
	 * The actual iterator. We assume that an iterator instance will not be shared between threads
	 * and that elements will not be added to the list after the iterator is created.
	 */
	private class BoundedIterator implements Iterator<T>, Iterable<T> {
		private final int size;
		private INode<T> current;
		private final INode<T> last;

		BoundedIterator(int size, INode<T> first, INode<T> last) {
			current = first;
			this.last = last;
			this.size = size;
		}

		@Override
		public boolean hasNext() {
			return current != null && current != last.getNext();
		}

		@Override
		public T next() {
			if (!hasNext()) {
				throw new NoSuchElementException("No more elements!"); //$NON-NLS-1$
			}
			T value = current.getValue();
			current = current.getNext();
			return value;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Iterator<T> iterator() {
			return this;
		}

		@Override
		public String toString() {
			return String.format("BoundedIterator size=%d, maxSize=%d, current=%d, last=%d", size, getMaxSize(), //$NON-NLS-1$
					current.getValue(), last.getValue());
		}
	}

	/**
	 * Create a new bounded list.
	 *
	 * @param maxSize
	 *            maximum number of elements to keep
	 */
	public BoundedList(int maxSize) {
		setMaxSize(maxSize);
	}

	/**
	 * Adds a value to this list. If the list is at max capacity then the oldest element will be
	 * dropped.
	 *
	 * @param t
	 *            the value to add
	 */
	public synchronized void add(T t) {
		if (t instanceof INode) {
			@SuppressWarnings("unchecked")
			INode<T> node = (INode<T>) t;
			addNode(node);
		} else {
			addNode(new Node<>(t));
		}
	}

	private void addNode(INode<T> t) {
		if (first == null) {
			first = t;
			last = t;
		} else {
			last.setNext(t);
			last = t;
		}
		size++;
		if (size > maxSize) {
			first = first.getNext();
			size--;
		}
	}

	/**
	 * Get an iterator from the first available to the last available element at the time the
	 * iterator was created. Keeping a reference to an iterator for longer than necessary may keep
	 * memory from properly being reclaimed.
	 *
	 * @return an iterator over the list elements
	 */
	@Override
	public synchronized Iterator<T> iterator() {
		BoundedIterator iter = new BoundedIterator(size, first, last);
		return iter;
	}

	/**
	 * Get the first element in the list.
	 *
	 * @return the first element
	 */
	public synchronized T getFirst() {
		return first == null ? null : first.getValue();
	}

	/**
	 * Get the last element in the list.
	 *
	 * @return the last element
	 */
	public synchronized T getLast() {
		return last == null ? null : last.getValue();
	}

	/**
	 * Get the number of elements in this list.
	 *
	 * @return the size of the list
	 */
	public synchronized int getSize() {
		return size;
	}

	/**
	 * Get the maximum number of elements to retain in this list.
	 *
	 * @return the maximum size of the list
	 */
	public synchronized int getMaxSize() {
		return maxSize;
	}

	/**
	 * Set the maximum number of elements to retain in this list.
	 *
	 * @param maxSize
	 *            the maximum size of the list
	 */
	public void setMaxSize(int maxSize) {
		if (maxSize < 1) {
			throw new IllegalArgumentException("The maximum size must be at least 1!"); //$NON-NLS-1$
		}
		synchronized (this) {
			this.maxSize = maxSize;
			while (size > maxSize) {
				first = first.getNext();
				size -= 1;
			}
		}
	}

	/**
	 * Use only for debugging purposes!
	 */
	@Override
	public String toString() {
		return iterator().toString();
	}
}
