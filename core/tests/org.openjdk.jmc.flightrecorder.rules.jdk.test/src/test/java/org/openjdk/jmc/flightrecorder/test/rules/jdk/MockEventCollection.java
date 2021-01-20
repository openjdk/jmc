/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.test.rules.jdk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemConsumer;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;

public class MockEventCollection implements IItemCollection {
	private final List<TestEventItem> items = new ArrayList<>();

	public MockEventCollection(TestEvent[] values) {
		for (TestEvent value : values) {
			items.add(new TestEventItem(value));
		}
	}

	@Override
	public IItemCollection apply(IItemFilter filter) {
		ArrayList<TestEvent> newEntries = new ArrayList<>();
		for (TestEventItem item : items) {
			if (filter.getPredicate(item.getType()).test(item)) {
				newEntries.add(item.getEvent());
			}
		}
		return new MockEventCollection(newEntries.toArray(new TestEvent[0]));
	}

	@Override
	public Iterator<IItemIterable> iterator() {
		return Collections.<IItemIterable> unmodifiableList(items).iterator();
	}

	@Override
	public <V, C extends IItemConsumer<C>> V getAggregate(IAggregator<V, C> aggregator) {
		return aggregate(aggregator, items.iterator());
	}

	private static <V, C extends IItemConsumer<C>> V aggregate(
		final IAggregator<V, C> aggregator, final Iterator<? extends IItem> items) {
		return aggregator.getValue(new Iterator<C>() {

			TestEventItem next = findNext();

			@Override
			public boolean hasNext() {
				return next != null;
			}

			@Override
			public C next() {
				C calc = aggregator.newItemConsumer(next.getType());
				calc.consume(next);
				next = findNext();
				return calc;
			}

			TestEventItem findNext() {
				while (items.hasNext()) {
					TestEventItem ii = (TestEventItem) items.next();
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

	@Override
	public boolean hasItems() {
		return !items.isEmpty();
	}

	@Override
	public Set<IRange<IQuantity>> getUnfilteredTimeRanges() {
		return null;
	}
}
