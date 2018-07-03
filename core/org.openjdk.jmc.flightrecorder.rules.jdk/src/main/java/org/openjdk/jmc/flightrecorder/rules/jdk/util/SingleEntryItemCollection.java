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
package org.openjdk.jmc.flightrecorder.rules.jdk.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.openjdk.jmc.common.IPredicate;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemConsumer;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IType;

/**
 * Implementation helper class for handling a singular {@link IItem} as an {@link IItemCollection}.
 */
final class SingleEntryItemCollection implements IItemCollection {
	private static final IItemCollection NULLCOLLECTION = new IItemCollection() {
		@Override
		public Iterator<IItemIterable> iterator() {
			return null;
		}

		@Override
		public boolean hasItems() {
			return false;
		}

		@Override
		public <V, C extends IItemConsumer<C>> V getAggregate(IAggregator<V, C> aggregator) {
			return null;
		}

		@Override
		public IItemCollection apply(IItemFilter filter) {
			return this;
		}
	};

	private static final IItemIterable NULLITERABLE = new IItemIterable() {
		@Override
		public Iterator<IItem> iterator() {
			return null;
		}

		@Override
		public IType<IItem> getType() {
			return null;
		}

		@Override
		public boolean hasItems() {
			return false;
		}

		@Override
		public long getItemCount() {
			return 0;
		}

		@Override
		public IItemIterable apply(IPredicate<IItem> predicate) {
			return null;
		}
	};

	private static class SingleEntryIteratorOfIterable implements Iterator<IItemIterable> {
		private final IItem item;
		private boolean picked = false;

		public SingleEntryIteratorOfIterable(IItem item) {
			this.item = item;
		}

		@Override
		public boolean hasNext() {
			return picked == false;
		}

		@Override
		public IItemIterable next() {
			if (picked == false) {
				picked = true;
				return new SingleEntryIItemIterable(item);
			}
			throw new NoSuchElementException();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private static class SingleEntryIItemIterable implements IItemIterable {
		private final List<IItem> itemList = new ArrayList<>(0);

		public SingleEntryIItemIterable(IItem item) {
			if (item == null) {
				throw new NullPointerException("Must have an item!"); //$NON-NLS-1$
			}
			itemList.add(item);
		}

		@Override
		public Iterator<IItem> iterator() {
			return itemList.iterator();
		}

		@SuppressWarnings("unchecked")
		@Override
		public IType<IItem> getType() {
			return (IType<IItem>) itemList.get(0).getType();
		}

		@Override
		public boolean hasItems() {
			return false;
		}

		@Override
		public long getItemCount() {
			return 1;
		}

		@Override
		public IItemIterable apply(IPredicate<IItem> predicate) {
			if (predicate.evaluate(itemList.get(0))) {
				return this;
			}
			return NULLITERABLE;
		}
	}

	private final IItem item;

	SingleEntryItemCollection(IItem item) {
		this.item = item;
	}

	@Override
	public Iterator<IItemIterable> iterator() {
		return new SingleEntryIteratorOfIterable(item);
	}

	@SuppressWarnings("unchecked")
	@Override
	public IItemCollection apply(IItemFilter filter) {
		return filter.getPredicate((IType<IItem>) item.getType()).evaluate(item) ? this : NULLCOLLECTION;
	}

	@Override
	public <V, C extends IItemConsumer<C>> V getAggregate(final IAggregator<V, C> aggregator) {
		return aggregator.getValue(new Iterator<C>() {
			boolean picked = false;

			@Override
			public boolean hasNext() {
				return picked == false;
			}

			@Override
			public C next() {
				if (picked == false) {
					picked = true;
					@SuppressWarnings("unchecked")
					C calc = aggregator.newItemConsumer((IType<IItem>) item.getType());
					calc.consume(item);
					return calc;
				}
				throw new NoSuchElementException();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		});
	}

	@Override
	public boolean hasItems() {
		return true;
	}
}
