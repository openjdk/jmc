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
package org.openjdk.jmc.flightrecorder.ui;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.openjdk.jmc.common.IPredicate;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemConsumer;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.util.PredicateToolkit;

/**
 * Toolkit class for working with IItemIterable instances
 */
public class ItemIterableToolkit {

	private static class StreamBackedItemIterable implements IItemIterable {

		private final Supplier<Stream<IItem>> items;
		private final IType<IItem> type;

		StreamBackedItemIterable(Supplier<Stream<IItem>> items, IType<IItem> type) {
			this.items = items;
			this.type = type;
		}

		@Override
		public IType<IItem> getType() {
			return type;
		}

		@Override
		public boolean hasItems() {
			return items.get().findAny().isPresent();
		}

		@Override
		public long getItemCount() {
			// In jdk8 the implementation of count is mapToLong(e -> 1L).sum() which may not be necessary
			long exactSizeIfKnown = spliterator().getExactSizeIfKnown();
			return exactSizeIfKnown >= 0 ? exactSizeIfKnown : items.get().count();
		}

		@Override
		public Iterator<IItem> iterator() {
			return Spliterators.iterator(spliterator());
		}

		@Override
		public Spliterator<IItem> spliterator() {
			return items.get().spliterator();
		}

		@Override
		public IItemIterable apply(IPredicate<IItem> filter) {
			return new StreamBackedItemIterable(() -> items.get().filter(filter::evaluate), getType());
		}
	}

	public static IItemIterable build(Supplier<Stream<IItem>> items, IType<IItem> type) {
		return new StreamBackedItemIterable(items, type);
	}

	public static Stream<IItemIterable> filter(Stream<? extends IItemIterable> items, IItemFilter on) {
		Function<IItemIterable, IItemIterable> streamMapper = itemStream -> {
			IPredicate<IItem> predicate = on.getPredicate(itemStream.getType());
			if (PredicateToolkit.isTrueGuaranteed(predicate)) {
				return itemStream;
			} else if (PredicateToolkit.isFalseGuaranteed(predicate)) {
				return null;
			} else {
				return itemStream.apply(predicate);
			}
		};
		return items.map(streamMapper).filter(Objects::nonNull);
	}

	public static <V, C extends IItemConsumer<C>> V aggregate(
		IAggregator<V, C> a, Stream<? extends IItemIterable> items) {
		Function<IItemIterable, C> itemsToValue = itemsStream -> ItemIterableToolkit.parallelStream(itemsStream)
				.collect(valueCollector(a, itemsStream.getType()));
		Stream<C> consumers = items.filter(is -> a.acceptType(is.getType())).map(itemsToValue);
		return a.getValue(consumers.iterator());
	}

	private static <C extends IItemConsumer<C>> Collector<IItem, C, C> valueCollector(
		IAggregator<?, C> a, IType<IItem> type) {
		return Collector.of(() -> a.newItemConsumer(type), C::consume, IItemConsumer::merge,
				Collector.Characteristics.UNORDERED);
	}

	public static <V> Stream<? extends IItem> sorted(
		IItemIterable items, IAttribute<V> onAttribute, Comparator<? super V> valueComparator) {
		IMemberAccessor<V, IItem> va = onAttribute.getAccessor(items.getType());
		return stream(items).sorted((i1, i2) -> Objects.compare(va.getMember(i1), va.getMember(i2), valueComparator));
	}

	public static Stream<IItem> stream(IItemIterable items) {
		return StreamSupport.stream(items.spliterator(), false);
	}

	public static Stream<IItem> parallelStream(IItemIterable items) {
		return StreamSupport.stream(items.spliterator(), true);
	}
}
