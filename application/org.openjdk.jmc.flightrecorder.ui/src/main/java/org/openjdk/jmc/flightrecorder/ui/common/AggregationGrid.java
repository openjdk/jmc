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
package org.openjdk.jmc.flightrecorder.ui.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openjdk.jmc.common.item.IAccessorFactory;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.ItemToolkit;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.ui.ItemCollectionToolkit;
import org.openjdk.jmc.flightrecorder.ui.ItemIterableToolkit;

public class AggregationGrid {

	private static class AggregationModel {
		final Object[][] cellData;
		AggregateRow[] aggregateItems;
		int itemsCount;

		AggregationModel(int columnCount, int rowCount) {
			cellData = new Object[columnCount][];
			aggregateItems = new AggregateRow[rowCount];
		}

		void addRow(Object key, List<IItem[]> items, int rowIndex) {
			AggregateRow ai = new AggregateRow(this, key, items, rowIndex);
			aggregateItems[rowIndex] = ai;
			itemsCount += ai.count.longValue();
		}
	}

	public static class AggregateRow {
		final int index;
		final IItemCollection items;
		final Object key;
		final IQuantity count;
		final AggregationModel model;

		AggregateRow(AggregationModel model, Object key, List<IItem[]> itemsByType, int rowIndex) {
			this.model = model;
			this.key = key;
			items = buildItemCollection(itemsByType);
			count = UnitLookup.NUMBER_UNITY.quantity(itemsByType.stream().mapToInt(ia -> ia.length).sum());
			index = rowIndex;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(key);
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof AggregateRow && Objects.equals(key, ((AggregateRow) obj).key);
		}

	}

	private static class AggregateColumn implements IMemberAccessor<Object, Object> {

		private final Function<IItemCollection, ?> valueFunction;
		private final int columnIndex;

		AggregateColumn(Function<IItemCollection, ?> valueFunction, int columnIndex) {
			this.valueFunction = valueFunction;
			this.columnIndex = columnIndex;
		}

		@Override
		public Object getMember(Object inObject) {
			if (inObject instanceof AggregateRow) {
				AggregateRow ai = ((AggregateRow) inObject);
				if (ai.model.cellData[columnIndex] == null) {
					ai.model.cellData[columnIndex] = Arrays.stream(ai.model.aggregateItems).parallel()
							.map(this::calculateValue).toArray();
				}
				return ai.model.cellData[columnIndex][((AggregateRow) inObject).index];
			}
			return null;
		}

		private Object calculateValue(AggregateRow row) {
			return valueFunction.apply(row.items);
		}
	}

	private int createdColumns;

	public static Object getKey(Object row) {
		return (row instanceof AggregateRow) ? ((AggregateRow) row).key : null;
	}

	public static IQuantity getCount(Object row) {
		return (row instanceof AggregateRow) ? ((AggregateRow) row).count : null;
	}

	public static IItemCollection getItems(Object row) {
		return ((AggregateRow) row).items;
	}

	public static double getCountFraction(Object row) {
		if ((row instanceof AggregateRow)) {
			AggregateRow ai = ((AggregateRow) row);
			if (ai.model.itemsCount > 0) {
				return ai.count.doubleValue() / ai.model.itemsCount;
			}
		}
		return 0;
	}

	// All rows built before the column was added will not have the extra column
	public IMemberAccessor<?, Object> addColumn(Function<IItemCollection, ?> valueFunction) {
		return new AggregateColumn(valueFunction, createdColumns++);
	}

	private static <T> void addStream(HashMap<T, List<IItem[]>> map, KeyedStream<T, IItem> ks) {
		map.computeIfAbsent(ks.getKey(), k -> new ArrayList<>()).add(ks.getStream().toArray(IItem[]::new));
	}

	private static <T, U extends HashMap<T, List<IItem[]>>> U merge(U map1, U map2) {
		for (Map.Entry<T, List<IItem[]>> e : map2.entrySet()) {
			map1.merge(e.getKey(), e.getValue(), (l1, l2) -> {
				l1.addAll(l2);
				return l1;
			});
		}
		return map1;
	}

	private static <T> Map<T, List<IItem[]>> mapItems(Stream<IItemIterable> items, IAccessorFactory<T> classifier) {
		Stream<KeyedStream<T, IItem>> flatMap = items.parallel().flatMap(is -> {
			IMemberAccessor<? extends T, IItem> accessor = classifier.getAccessor(is.getType());
			if (accessor == null) {
				throw new IllegalArgumentException(
						"Cannot fetch accessor from " + classifier + " for type " + is.getType().getIdentifier()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			// It seems Eclipse 4.5 has trouble inferring the correct type of this function ...
			Function<IItem, T> getMemberFunc = accessor::getMember;
			return ItemIterableToolkit.parallelStream(is).collect(KeyedStream.collector(getMemberFunc));
		});
		return flatMap.collect(Collector.of(HashMap<T, List<IItem[]>>::new, AggregationGrid::addStream,
				AggregationGrid::merge, Characteristics.UNORDERED));
	}

	public static <T, U> Stream<U> mapItems(
		Stream<IItemIterable> items, IAccessorFactory<T> classifier, BiFunction<T, IItemCollection, U> rowBuilder) {
		return mapItems(items, classifier).entrySet().stream()
				.map(e -> rowBuilder.apply(e.getKey(), buildItemCollection(e.getValue())));
	}

	private static final Function<IItem[], IItemIterable> ITEMS_BY_TYPE_CONSTRUCTOR = ia -> ItemIterableToolkit
			.build(() -> Stream.of(ia), ItemToolkit.getItemType(ia[0]));

	private static IItemCollection buildItemCollection(List<IItem[]> items) {
		return ItemCollectionToolkit
				.build(items.stream().map(ITEMS_BY_TYPE_CONSTRUCTOR).collect(Collectors.toList())::stream);
	}

	public <T> Object[] buildRows(Stream<IItemIterable> items, IAccessorFactory<T> classifier) {
		Map<T, List<IItem[]>> itemsMap = mapItems(items, classifier);
		AggregationModel model = new AggregationModel(createdColumns, itemsMap.size());
		int index = 0;
		for (Entry<T, List<IItem[]>> e : itemsMap.entrySet()) {
			model.addRow(e.getKey(), e.getValue(), index++);
		}
		return model.aggregateItems;
	}
}
