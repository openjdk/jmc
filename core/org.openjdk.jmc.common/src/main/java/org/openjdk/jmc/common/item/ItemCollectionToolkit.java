/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openjdk.jmc.common.messages.internal.Messages;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;

/**
 * Toolkit class for working with IItemCollection instances
 */
public class ItemCollectionToolkit {
	public static final IItemCollection EMPTY = new StreamBackedItemCollection(() -> Stream.empty(),
			Collections.emptySet());

	private static class StreamBackedItemCollection implements IItemCollection {

		private final Supplier<Stream<IItemIterable>> items;
		private final Set<IRange<IQuantity>> chunkRanges;

		StreamBackedItemCollection(Supplier<Stream<IItemIterable>> items, Set<IRange<IQuantity>> chunkRanges) {
			this.items = items;
			this.chunkRanges = chunkRanges;
		}

		@Override
		public Iterator<IItemIterable> iterator() {
			return items.get().iterator();
		}

		@Override
		public Spliterator<IItemIterable> spliterator() {
			return items.get().spliterator();
		}

		@Override
		public StreamBackedItemCollection apply(IItemFilter filter) {
			return new StreamBackedItemCollection(() -> ItemIterableToolkit.filter(items.get(), filter), chunkRanges);
		}

		@Override
		public <V, C extends IItemConsumer<C>> V getAggregate(IAggregator<V, C> aggregator) {
			return ItemIterableToolkit.aggregate(aggregator, items.get());
		}

		@Override
		public boolean hasItems() {
			return items.get().anyMatch(IItemIterable::hasItems);
		}

		@Override
		public Set<IRange<IQuantity>> getUnfilteredTimeRanges() {
			return chunkRanges;
		}

	}

	static IItemCollection build(Stream<? extends IItem> items, Set<IRange<IQuantity>> chunkRanges) {
		Map<IType<IItem>, List<IItem>> byTypeMap = items.collect(Collectors.groupingBy(ItemToolkit::getItemType));
		List<Entry<IType<IItem>, List<IItem>>> entryList = new ArrayList<>(byTypeMap.entrySet());
		return ItemCollectionToolkit
				.build(() -> entryList.stream().map(e -> ItemIterableToolkit.build(e.getValue()::stream, e.getKey())));
	}

	public static IItemCollection build(Stream<? extends IItem> items) {
		return build(items, Collections.emptySet());
	}

	public static IItemCollection build(Supplier<Stream<IItemIterable>> items, Set<IRange<IQuantity>> chunkRanges) {
		return new StreamBackedItemCollection(items, Collections.emptySet());
	}

	public static IItemCollection build(Supplier<Stream<IItemIterable>> items) {
		return build(items, Collections.emptySet());
	}

	public static IItemCollection merge(Supplier<Stream<IItemCollection>> items) {
		Set<IRange<IQuantity>> chunkRanges = items.get().flatMap(i -> i.getUnfilteredTimeRanges().stream())
				.collect(Collectors.toSet());
		return ItemCollectionToolkit.build(() -> items.get().flatMap(i -> i.stream()), chunkRanges);
	}

	public static <V> Optional<IItemIterable> join(IItemCollection items, String withTypeId) {
		IItemCollection itemsWithType = items.apply(ItemFilters.type(withTypeId));
		return itemsWithType.stream().findAny().map(
				s -> ItemIterableToolkit.build(() -> itemsWithType.stream().flatMap(i -> i.stream()), s.getType()));
	}

	public static String getDescription(IItemCollection items) {
		Map<IType<?>, Long> itemCountByType = items.stream().filter(IItemIterable::hasItems)
				.collect(Collectors.toMap(IItemIterable::getType, IItemIterable::getItemCount, Long::sum));
		if (itemCountByType.size() < 4) {
			return itemCountByType.entrySet().stream().map(e -> e.getValue() + " " + e.getKey().getName()).sorted() //$NON-NLS-1$
					.collect(Collectors.joining(", ")); //$NON-NLS-1$
		}
		return MessageFormat.format(Messages.ITEM_COLLECTION_DESC,
				itemCountByType.values().stream().mapToLong(Long::longValue).sum(), itemCountByType.size());
	}

	public static IItemCollection filterIfNotNull(IItemCollection items, IItemFilter filter) {
		return filter == null ? items : items.apply(filter);
	}
}
