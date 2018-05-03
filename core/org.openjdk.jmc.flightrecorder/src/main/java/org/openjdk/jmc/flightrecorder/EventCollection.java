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
package org.openjdk.jmc.flightrecorder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.openjdk.jmc.common.IPredicate;
import org.openjdk.jmc.common.collection.IteratorToolkit;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemConsumer;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.util.PredicateToolkit;
import org.openjdk.jmc.flightrecorder.internal.EventArray;

/**
 * Java 1.7 based implementation of {@link IItemCollection} using {@link IItemIterable} iterators.
 * This class is only intended to be used as an IItemCollection outside of the usage in
 * {@link JfrLoaderToolkit}.
 */
class EventCollection implements IItemCollection {

	private static class EventTypeEntry implements IItemIterable {

		EventTypeEntry(EventArray events) {
			this.events = events;
			predicate = PredicateToolkit.truePredicate();
		}

		EventTypeEntry(EventArray events, IPredicate<IItem> predicate) {
			this.events = events;
			this.predicate = predicate;
		}

		final EventArray events;
		final IPredicate<IItem> predicate;

		@Override
		public IType<IItem> getType() {
			return events.getType();
		}

		@Override
		public Iterator<IItem> iterator() {
			return buildIterator(events.getEvents(), predicate);
		}

		@Override
		public boolean hasItems() {
			return iterator().hasNext();
		}

		@Override
		public long getItemCount() {
			if (isFiltered(predicate)) {
				long c = 0;
				Iterator<IItem> it = iterator();
				while (it.hasNext()) {
					it.next();
					c++;
				}
				return c;
			}
			return events.getEvents().length;
		}

		@Override
		public EventTypeEntry apply(IPredicate<IItem> filter) {
			IPredicate<IItem> newPredicate = PredicateToolkit.and(Arrays.asList(filter, predicate));
			return new EventTypeEntry(events, newPredicate);
		}

	}

	private final Set<IType<IItem>> types = new HashSet<>();
	private final ArrayList<EventTypeEntry> items;

	static IItemCollection build(EventArray[] events) {
		ArrayList<EventTypeEntry> items = new ArrayList<>(events.length);
		for (EventArray ea : events) {
			EventTypeEntry entry = new EventTypeEntry(ea);
			items.add(entry);
		}
		return new EventCollection(items);
	}

	private EventCollection(ArrayList<EventTypeEntry> items) {
		this.items = items;
		for (EventTypeEntry e : items) {
			types.add(e.events.getType());
		}
	}

	@Override
	public EventCollection apply(IItemFilter filter) {
		ArrayList<EventTypeEntry> newEntries = new ArrayList<>();
		for (EventTypeEntry e : items) {
			EventTypeEntry newEntry = e.apply(filter.getPredicate(e.events.getType()));
			if (PredicateToolkit.isTrueGuaranteed(newEntry.predicate)) {
				newEntries.add(e);
			} else if (!PredicateToolkit.isFalseGuaranteed(newEntry.predicate)) {
				newEntries.add(newEntry);
			}
		}
		return new EventCollection(newEntries);
	}

	private static Iterator<IItem> buildIterator(IItem[] array, IPredicate<? super IItem> filter) {
		if (isFiltered(filter)) {
			return IteratorToolkit.filter(IteratorToolkit.of(array), filter);
		} else {
			return IteratorToolkit.of(array);
		}
	}

	private static boolean isFiltered(IPredicate<?> filter) {
		return filter != null && !PredicateToolkit.isTrueGuaranteed(filter);
	}

	@Override
	public Iterator<IItemIterable> iterator() {
		return Collections.<IItemIterable> unmodifiableList(items).iterator();
	}

	@Override
	public boolean hasItems() {
		Iterator<? extends IItemIterable> ii = items.iterator();
		while (ii.hasNext()) {
			if (ii.next().iterator().hasNext()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public <V, C extends IItemConsumer<C>> V getAggregate(IAggregator<V, C> aggregator) {
		return aggregate(aggregator, items.iterator());
	}

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
