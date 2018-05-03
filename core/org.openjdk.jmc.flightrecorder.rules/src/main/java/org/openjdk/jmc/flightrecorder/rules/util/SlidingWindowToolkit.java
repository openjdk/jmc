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
package org.openjdk.jmc.flightrecorder.rules.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.FutureTask;

import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.QuantityRange;
import org.openjdk.jmc.common.util.Pair;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.rules.Result;

/**
 * Utility functions and interfaces for doing sliding window calculations.
 */
public class SlidingWindowToolkit {

	/**
	 * Visitor interface used when calling {@link SlidingWindowToolkit#slidingWindowOrdered}
	 */
	public interface IOrderedWindowVisitor {
		void visitWindow(Iterator<IItem> items);

		boolean shouldContinue();
	}

	/**
	 * Runs a sliding window through all items, looping through items once, removing from the start
	 * and adding and the end of a windowItem set to match the current time window. items.
	 * <p>
	 * Suitable if the items are guaranteed to be ordered.
	 *
	 * @param callback
	 *            method that can do calculations on the items in the window
	 * @param items
	 *            input items
	 * @param posAccessor
	 *            an accessor that should give a position value that is used with windowsSize and
	 *            slideSize
	 * @param windowSize
	 *            size for the sliding window
	 * @param slideSize
	 *            how big the slide should be, if slideSize is {@code null}, it will slide one item
	 *            at a time
	 */
	public static void slidingWindowOrdered(
		IOrderedWindowVisitor callback, Iterator<IItem> items, IMemberAccessor<IQuantity, IItem> posAccessor,
		IQuantity windowSize, IQuantity slideSize) {

		IQuantity windowStart = null;

		List<IItem> windowItems = new ArrayList<>();

		for (Iterator<IItem> iterator = items; iterator.hasNext() && callback.shouldContinue();) {
			IItem item = iterator.next();
			if (windowItems.isEmpty()) {
				windowStart = posAccessor.getMember(item);
			} else {
				windowStart = posAccessor.getMember(windowItems.get(0));
			}
			windowItems.add(item);
			IQuantity windowEnd = posAccessor.getMember(item);
			while (iterator.hasNext() && windowEnd.subtract(windowSize).compareTo(windowStart) < 0
					&& callback.shouldContinue()) {
				IItem next = iterator.next();
				windowEnd = posAccessor.getMember(next);
				windowItems.add(next);
			}

			callback.visitWindow(windowItems.iterator());

			// FIXME: What should we do about empty time intervals?
			if (slideSize == null) {
				if (windowItems.size() > 1) {
					windowItems.remove(0);
				}
			} else {
				IQuantity newStart = windowStart.add(slideSize);
				for (Iterator<IItem> it = windowItems.iterator(); it.hasNext() && callback.shouldContinue();) {
					IItem wi = it.next();
					if (posAccessor.getMember(wi).compareTo(newStart) < 0) {
						it.remove();
					} else {
						break;
					}
				}
			}
		}
	}

	/**
	 * Visitor interface used when calling {@link SlidingWindowToolkit#slidingWindowUnordered}
	 */
	public interface IUnorderedWindowVisitor {
		void visitWindow(IItemCollection items, IQuantity startTime, IQuantity endTime);

		boolean shouldContinue();
	}

	/**
	 * Runs a sliding window through all items, by calculating the window start and end and filter
	 * through all the items. Suitable if the items are not guaranteed to be ordered, but is slower
	 * than the ordered version.
	 *
	 * @param callback
	 *            method that can do calculations on the items in the window.
	 * @param items
	 *            input items
	 * @param windowSize
	 *            size for the sliding window
	 * @param slideSize
	 *            how big the slide should be
	 */
	public static void slidingWindowUnordered(
		IUnorderedWindowVisitor callback, IItemCollection items, IQuantity windowSize, IQuantity slideSize) {
		slidingWindowUnordered(callback, items, windowSize, slideSize, false);
	}

	/**
	 * Runs a sliding window through all items, by calculating the window start and end and filter
	 * through all the items. Suitable if the items are not guaranteed to be ordered, but is slower
	 * than the ordered version.
	 *
	 * @param callback
	 *            method that can do calculations on the items in the window.
	 * @param items
	 *            input items
	 * @param windowSize
	 *            size for the sliding window
	 * @param slideSize
	 *            how big the slide should be
	 * @param includeIntersecting
	 *            if the window filter should include events intersecting the window, or just those
	 *            with end time in the window. It's up to the caller to take this into account and
	 *            for example cap event duration to the window timespan.
	 */
	public static void slidingWindowUnordered(
		IUnorderedWindowVisitor callback, IItemCollection items, IQuantity windowSize, IQuantity slideSize,
		boolean includeIntersecting) {
		IQuantity first = includeIntersecting ? items.getAggregate(JdkAggregators.FIRST_ITEM_START)
				: items.getAggregate(JdkAggregators.FIRST_ITEM_END);
		IQuantity last = items.getAggregate(JdkAggregators.LAST_ITEM_END);

		if (first == null) {
			return;
		}

		IQuantity windowStart = first;
		IQuantity windowEnd = windowStart.add(windowSize);
		do {
			IItemFilter window = includeIntersecting
					? ItemFilters.rangeIntersects(JfrAttributes.LIFETIME,
							QuantityRange.createWithEnd(windowStart, windowEnd))
					: ItemFilters.interval(JfrAttributes.END_TIME, windowStart, true, windowEnd, true);

			IItemCollection windowItems = items.apply(window);

			callback.visitWindow(windowItems, windowStart, windowEnd);

			windowStart = windowStart.add(slideSize);
			windowEnd = windowEnd.add(slideSize);

		} while (windowStart.compareTo(last) < 0 && callback.shouldContinue());
	}

	/**
	 * Value function used when calling
	 * {@link SlidingWindowToolkit#slidingWindowUnorderedMinMaxValue}
	 */
	public interface IUnorderedWindowValueFunction<V> {
		V getValue(IItemCollection items, IQuantity startTime, IQuantity endTime);
	}

	/**
	 * Calculates max/min window quantity value of items.
	 *
	 * @param items
	 *            items to use for evaluation
	 * @param windowSize
	 *            window size
	 * @param cancellationSupplier
	 *            if the evaluation should be cancelled
	 * @param valueFunction
	 *            provides the window value for items
	 * @param max
	 *            true to get the max value, false to get min value
	 * @param includeIntersecting
	 *            true to include also intersecting items, false to only include contained items. If
	 *            set to true, it's up to the valueFunction to only use the duration events that is
	 *            actually included in the window.
	 * @return min/max window value and range
	 */
	public static Pair<IQuantity, IRange<IQuantity>> slidingWindowUnorderedMinMaxValue(
		IItemCollection items, IQuantity windowSize, final FutureTask<Result> cancellationSupplier,
		final IUnorderedWindowValueFunction<IQuantity> valueFunction, boolean max, boolean includeIntersecting) {
		IQuantity slideSize = windowSize.getUnit().quantity(windowSize.ratioTo(windowSize.getUnit().quantity(2)));

		return slidingWindowUnorderedMinMaxValue(items, windowSize, slideSize, cancellationSupplier, valueFunction, max,
				includeIntersecting);
	}

	/**
	 * Calculates max/min window quantity value of items.
	 *
	 * @param items
	 *            items to use for evaluation
	 * @param windowSize
	 *            window size
	 * @param slideSize
	 *            window slide size
	 * @param cancellationSupplier
	 *            if the evaluation should be cancelled
	 * @param valueFunction
	 *            provides the window value for items
	 * @param max
	 *            true to get the max value, false to get min value
	 * @param includeIntersecting
	 *            true to include also intersecting items, false to only include contained items. If
	 *            set to true, it's up to the valueFunction to only use the duration events that is
	 *            actually included in the window.
	 * @return min/max window value and range
	 */
	public static Pair<IQuantity, IRange<IQuantity>> slidingWindowUnorderedMinMaxValue(
		IItemCollection items, IQuantity windowSize, IQuantity slideSize, final FutureTask<Result> cancellationSupplier,
		final IUnorderedWindowValueFunction<IQuantity> valueFunction, boolean max, boolean includeIntersecting) {
		return slidingWindowUnorderedMinMaxValue(items, windowSize, slideSize, cancellationSupplier, valueFunction,
				QUANTITY_COMPARATOR, max, includeIntersecting);
	}

	private static final Comparator<IQuantity> QUANTITY_COMPARATOR = new Comparator<IQuantity>() {

		@Override
		public int compare(IQuantity o1, IQuantity o2) {
			return o1.compareTo(o2);
		}
	};

	/**
	 * Calculates max/min window value of items.
	 *
	 * @param items
	 *            items to use for evaluation
	 * @param windowSize
	 *            window size
	 * @param cancellationSupplier
	 *            if the evaluation should be cancelled
	 * @param valueFunction
	 *            provides the window value for items
	 * @param valueComparator
	 *            compares values
	 * @param max
	 *            true to get the max value, false to get min value
	 * @param includeIntersecting
	 *            true to include also intersecting items, false to only include contained items. If
	 *            set to true, it's up to the valueFunction to only use the duration events that is
	 *            actually included in the window.
	 * @return min/max window value and range
	 */
	public static <V> Pair<V, IRange<IQuantity>> slidingWindowUnorderedMinMaxValue(
		IItemCollection items, IQuantity windowSize, final FutureTask<Result> cancellationSupplier,
		final IUnorderedWindowValueFunction<V> valueFunction, final Comparator<V> valueComparator, boolean max,
		boolean includeIntersecting) {
		IQuantity slideSize = windowSize.getUnit().quantity(windowSize.ratioTo(windowSize.getUnit().quantity(2)));
		return slidingWindowUnorderedMinMaxValue(items, windowSize, slideSize, cancellationSupplier, valueFunction,
				valueComparator, max, includeIntersecting);
	}

	/**
	 * Calculates max/min window value of items.
	 *
	 * @param items
	 *            items to use for evaluation
	 * @param windowSize
	 *            window size
	 * @param slideSize
	 *            window slide size
	 * @param cancellationSupplier
	 *            if the evaluation should be cancelled
	 * @param valueFunction
	 *            provides the window value for items
	 * @param valueComparator
	 *            compares values
	 * @param max
	 *            true to get the max value, false to get min value
	 * @param includeIntersecting
	 *            true to include also intersecting items, false to only include contained items. If
	 *            set to true, it's up to the valueFunction to only use the duration events that is
	 *            actually included in the window.
	 * @return min/max window value and range
	 */
	public static <V> Pair<V, IRange<IQuantity>> slidingWindowUnorderedMinMaxValue(
		IItemCollection items, IQuantity windowSize, IQuantity slideSize, final FutureTask<Result> cancellationSupplier,
		final IUnorderedWindowValueFunction<V> valueFunction, final Comparator<V> valueComparator, boolean max,
		boolean includeIntersecting) {

		final List<Pair<V, IRange<IQuantity>>> valueList = new ArrayList<>();

		slidingWindowUnordered(new IUnorderedWindowVisitor() {
			@Override
			public void visitWindow(IItemCollection items, IQuantity startTime, IQuantity endTime) {
				V value = valueFunction.getValue(items, startTime, endTime);
				if (value != null) {
					valueList.add(new Pair<>(value, QuantityRange.createWithEnd(startTime, endTime)));
				}
			}

			@Override
			public boolean shouldContinue() {
				return !cancellationSupplier.isCancelled();
			}
		}, items, windowSize, slideSize, includeIntersecting);
		Comparator<Pair<V, IRange<IQuantity>>> comparator = new Comparator<Pair<V, IRange<IQuantity>>>() {
			@Override
			public int compare(Pair<V, IRange<IQuantity>> o1, Pair<V, IRange<IQuantity>> o2) {
				return valueComparator.compare(o1.left, o2.left);
			}
		};
		if (valueList.isEmpty()) {
			return null;
		}
		Pair<V, IRange<IQuantity>> minMaxWindow = max ? Collections.max(valueList, comparator)
				: Collections.min(valueList, comparator);
		return minMaxWindow;
	}
}
