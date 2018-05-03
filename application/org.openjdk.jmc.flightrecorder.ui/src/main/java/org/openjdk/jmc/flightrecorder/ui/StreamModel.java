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

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openjdk.jmc.common.IPredicate;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.util.PredicateToolkit;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.internal.EventArray;
import org.openjdk.jmc.flightrecorder.ui.EventTypeFolderNode.TypeWithCategory;

public class StreamModel {

	private final EventArray[] eventsByType;

	StreamModel(EventArray[] eventsByType) {
		this.eventsByType = eventsByType;
	}

	public IItemCollection getItems(IRange<IQuantity> range, IItemFilter filter) {
		IItemIterable[] rangedStreams = Stream.of(eventsByType).map(ea -> {
			IType<IItem> eventType = ea.getType();
			IPredicate<IItem> predicate = filter.getPredicate(eventType);
			if (PredicateToolkit.isTrueGuaranteed(predicate)) {
				return ItemIterableToolkit.build(itemSupplier(ea.getEvents(), eventType, range), eventType);
			} else if (PredicateToolkit.isFalseGuaranteed(predicate)) {
				return null;
			} else {
				return ItemIterableToolkit.build(itemSupplier(ea.getEvents(), eventType, range, predicate::evaluate),
						eventType);
			}
		}).filter(Objects::nonNull).toArray(IItemIterable[]::new);
		return ItemCollectionToolkit.build(() -> Arrays.stream(rangedStreams));
	}

	public IItemCollection getItems(IRange<IQuantity> range) {
		return ItemCollectionToolkit.build(() -> Arrays.stream(eventsByType).map(ea -> ItemIterableToolkit
				.build(() -> itemSupplier(ea.getEvents(), ea.getType(), range).get(), ea.getType())));
	}

	public IItemCollection getItems() {
		return ItemCollectionToolkit.build(() -> Arrays.stream(eventsByType)
				.map(ea -> ItemIterableToolkit.build(() -> Arrays.stream(ea.getEvents()), ea.getType())));
	}

	private static Supplier<Stream<IItem>> itemSupplier(IItem[] events, IType<IItem> ofType, IRange<IQuantity> range) {
		int start = findStart(events, ofType, range.getStart());
		int end = findEnd(events, ofType, range.getEnd());
		return () -> Arrays.stream(events, start, end);
	}

	private static Supplier<Stream<IItem>> itemSupplier(
		IItem[] events, IType<IItem> ofType, IRange<IQuantity> range, Predicate<? super IItem> predicate) {
		int start = findStart(events, ofType, range.getStart());
		int end = findEnd(events, ofType, range.getEnd());
		return () -> Arrays.stream(events, start, end).filter(predicate);
	}

	private static int findStart(IItem[] events, IType<IItem> ofType, IQuantity boundary) {
		IMemberAccessor<IQuantity, IItem> accessor = JfrAttributes.END_TIME.getAccessor(ofType);
		int index = binarySearch(events, accessor, boundary);
		while (index > 0 && accessor.getMember(events[index - 1]).compareTo(boundary) == 0) {
			index--;
		}
		return index;
	}

	private static int findEnd(IItem[] events, IType<IItem> ofType, IQuantity boundary) {
		IMemberAccessor<IQuantity, IItem> accessor = JfrAttributes.START_TIME.getAccessor(ofType);
		int index = binarySearch(events, accessor, boundary);
		while (index < events.length && accessor.getMember(events[index]).compareTo(boundary) == 0) {
			index++;
		}
		return index;
	}

	/**
	 * @param events
	 * @param accessor
	 * @param key
	 * @return The insertion point in the sorted array {@code events} if {@code key} was not found,
	 *         or an index of any item the {@code boundary} value if it was found.
	 */
	private static int binarySearch(IItem[] events, IMemberAccessor<IQuantity, IItem> accessor, IQuantity key) {
		int low = 0;
		int high = events.length - 1;
		while (low <= high) {
			int middle = (low + high) >>> 1;
			int comparison = key.compareTo(accessor.getMember(events[middle]));
			if (comparison == 0) {
				return middle;
			} else if (comparison > 0) {
				low = middle + 1;
			} else {
				high = middle - 1;
			}
		}
		return low;
	}

	public EventTypeFolderNode getTypeTree(Stream<IItemIterable> items) {
		Map<IType<IItem>, Long> itemCountByType = items
				.collect(Collectors.toMap(IItemIterable::getType, is -> is.getItemCount(), Long::sum));
		Function<EventArray, TypeWithCategory> eventArrayToTypeWithCategoryMapper = ea -> {
			Long count = itemCountByType.remove(ea.getType());
			return count == null ? null : new TypeWithCategory(ea.getType(), ea.getTypeCategory(), count);
		};
		return EventTypeFolderNode
				.buildRoot(Stream.of(eventsByType).map(eventArrayToTypeWithCategoryMapper).filter(Objects::nonNull));
	}

	public EventTypeFolderNode getTypeTree() {
		return getTypeTree(ItemCollectionToolkit.stream(getItems()));
	}
}
