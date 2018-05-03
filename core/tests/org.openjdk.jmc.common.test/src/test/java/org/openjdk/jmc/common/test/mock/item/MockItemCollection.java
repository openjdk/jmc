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
package org.openjdk.jmc.common.test.mock.item;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openjdk.jmc.common.collection.IteratorToolkit;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemConsumer;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IType;

public class MockItemCollection<T, CT extends IType<?>> implements IItemCollection {
	private List<IItem> items = new ArrayList<>();
	private IType<?> type;

	public MockItemCollection(T[] values, IType<?> type) {
		build(values, type);
		this.type = type;
	}

	private void build(T[] values, IType<?> type) {
		long index = 0;
		for (T value : values) {
			items.add(new MockItem<T, CT>(value, type, index++));
		}
	}

	@Override
	public IItemCollection apply(IItemFilter filter) {
		// Filtering not supported in this implementation
		return null;
	}

	@Override
	public Iterator<IItemIterable> iterator() {
		return IteratorToolkit.of(new IItemIterable[] {new MockItemIterable<CT>(items, type)});
	}

	@Override
	public <V, C extends IItemConsumer<C>> V getAggregate(IAggregator<V, C> aggregator) {
		return aggregate(aggregator, iterator());
	}

	@Override
	public boolean hasItems() {
		return !items.isEmpty();
	}

	// Copied from EventCollection
	private static <V, C extends IItemConsumer<C>> V aggregate(
		final IAggregator<V, C> aggregator, final Iterator<? extends IItemIterable> items) {
		return aggregator.getValue(new Iterator<C>() {

			IItemIterable next = findNext();

			@Override
			public boolean hasNext() {
				return next != null;
			}

			@Override
			public C next() {
				C calc = aggregator.newItemConsumer(next.getType());
				Iterator<? extends IItem> iterator = next.iterator();
				while (iterator.hasNext()) {
					calc.consume(iterator.next());
				}
				next = findNext();
				return calc;
			}

			IItemIterable findNext() {
				while (items.hasNext()) {
					IItemIterable ii = items.next();
					if (aggregator.acceptType(ii.getType())) {
						return ii;
					}
				}
				return null;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		});
	}
}
