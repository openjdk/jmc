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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemConsumer;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.item.ItemToolkit;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;

/**
 * Toolkit class for working with IItemCollection instances
 */
public class ItemCollectionToolkit {

	private static class StreamBackedItemCollection implements IItemCollection {

		private final Supplier<Stream<IItemIterable>> items;

		StreamBackedItemCollection(Supplier<Stream<IItemIterable>> items) {
			this.items = items;
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
			return new StreamBackedItemCollection(() -> ItemIterableToolkit.filter(items.get(), filter));
		}

		@Override
		public <V, C extends IItemConsumer<C>> V getAggregate(IAggregator<V, C> aggregator) {
			return ItemIterableToolkit.aggregate(aggregator, items.get());
		}

		@Override
		public boolean hasItems() {
			return items.get().anyMatch(IItemIterable::hasItems);
		}

	}

	public static final IItemCollection EMPTY = new StreamBackedItemCollection(() -> Stream.empty());

	public static IItemCollection build(Stream<? extends IItem> items) {
		Map<IType<IItem>, List<IItem>> byTypeMap = items.collect(Collectors.groupingBy(ItemToolkit::getItemType));
		List<Entry<IType<IItem>, List<IItem>>> entryList = new ArrayList<>(byTypeMap.entrySet());
		return ItemCollectionToolkit
				.build(() -> entryList.stream().map(e -> ItemIterableToolkit.build(e.getValue()::stream, e.getKey())));
	}

	public static IItemCollection build(Supplier<Stream<IItemIterable>> items) {
		return new StreamBackedItemCollection(items);
	}

	public static IItemCollection merge(Supplier<Stream<IItemCollection>> items) {
		return ItemCollectionToolkit.build(() -> items.get().flatMap(ItemCollectionToolkit::stream));
	}

	public static <V> Optional<IItemIterable> join(IItemCollection items, String withTypeId) {
		IItemCollection itemsWithType = items.apply(ItemFilters.type(withTypeId));
		return ItemCollectionToolkit.stream(itemsWithType).findAny()
				.map(s -> ItemIterableToolkit.build(
						() -> ItemCollectionToolkit.stream(itemsWithType).flatMap(ItemIterableToolkit::stream),
						s.getType()));
	}

	public static <T> Supplier<Stream<T>> values(IItemCollection items, IAttribute<T> attribute) {
		return () -> ItemCollectionToolkit.stream(items).flatMap(itemStream -> {
			IMemberAccessor<T, IItem> accessor = attribute.getAccessor(itemStream.getType());
			if (accessor != null) {
				return ItemIterableToolkit.stream(itemStream).map(accessor::getMember);
			} else {
				return Stream.empty();
			}
		});
	}

	public static String getDescription(IItemCollection items) {
		Map<IType<?>, Long> itemCountByType = ItemCollectionToolkit.stream(items).filter(IItemIterable::hasItems)
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

	public static Stream<IItemIterable> stream(IItemCollection items) {
		return StreamSupport.stream(items.spliterator(), false);
	}

	public static Stream<IItemIterable> parallelStream(IItemCollection items) {
		return StreamSupport.stream(items.spliterator(), true);
	}

}
