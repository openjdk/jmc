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
package org.openjdk.jmc.common.item;

import java.util.Collections;
import java.util.Iterator;

/**
 * Toolkit methods for performing operations on items.
 */
public class ItemToolkit {

	/**
	 * @deprecated This method returns a member accessor that is not thread safe. Instead of
	 *             creating an accessor that could be used for multiple item types, items should be
	 *             iterated by type, preferably using an {@link IAggregator} which enables parallel
	 *             processing.
	 */
	// FIXME: JMC-3869 - This method should be moved to avoid it being used more than necessary.
	@Deprecated
	public static <T> IMemberAccessor<T, IItem> accessor(IAttribute<T> a) {
		return new CachingAccessor<>(a);
	}

	// FIXME: JMC-3869 - Related to the removal of ItemToolkit.accessor.
	@Deprecated
	public static Iterable<IItem> asIterable(final IItemCollection items) {
		return new Iterable<IItem>() {

			@Override
			public Iterator<IItem> iterator() {
				return new ItemIterator(items.iterator());
			}
		};
	}

	// FIXME: Can we avoid using self inferring cast like this?
	@SuppressWarnings("unchecked")
	public static <T extends IItem> IType<T> getItemType(T item) {
		return (IType<T>) item.getType();
	}

	private static class ItemIterator implements Iterator<IItem> {
		private final Iterator<? extends IItemIterable> typeIter;
		private Iterator<IItem> iterator;
		private IItem next;

		ItemIterator(Iterator<? extends IItemIterable> items) {
			typeIter = items;
			iterator = Collections.<IItem> emptyList().iterator();
			next = findNext();
		}

		@Override
		public boolean hasNext() {
			return next != null;
		}

		@Override
		public IItem next() {
			IItem tmp = next;
			if (iterator.hasNext()) {
				next = iterator.next();
			} else {
				next = findNext();
			}
			return tmp;
		}

		private IItem findNext() {
			while (true) {
				if (iterator.hasNext()) {
					return iterator.next();
				} else if (typeIter.hasNext()) {
					iterator = typeIter.next().iterator();
				} else {
					return null;
				}
			}
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Returns the value of the event first encountered with the attribute. Note that it is usually
	 * a better idea to use Aggregators.AdvancedMinAggregator to truly get the first (time-wise)
	 * matching event. This method literally returns the attribute for the first item found when
	 * iterating through the IItems, one IItemIterable at a time.
	 * <p>
	 * This method should only be used when you do not care which item you get; any matching is
	 * fine. This is commonly the case when looking up relatively static information dumped per
	 * chunk, for example environment variables. Also make sure that the collection is properly
	 * filtered down to only contain events where the attribute can be applied.
	 *
	 * @param <T>
	 *            attribute value type
	 * @param items
	 *            items to search for attribute value in
	 * @param attribute
	 *            the attribute to retrieve
	 * @return the value of the attribute
	 * @see Aggregators.AdvancedMinAggregator
	 */
	public static <T> T getFirstFound(final IItemCollection items, final IAttribute<T> attribute) {
		for (IItemIterable iterable : items) {
			IMemberAccessor<T, IItem> accessor = attribute.getAccessor(iterable.getType());
			if (accessor != null) {
				for (IItem item : iterable) {
					return accessor.getMember(item);
				}
			}
		}
		return null;
	}
}
