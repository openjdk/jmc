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
package org.openjdk.jmc.flightrecorder.ui.selection;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.item.RangeMatchPolicy;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.QuantityRange;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;

// FIXME: Move to flightrecorder bundle, and move back to Java 7
public class FlavorToolkit {

	public static Optional<IRange<IQuantity>> getRange(IItemStreamFlavor flavor) {
		Optional<IRange<IQuantity>> range = extractTimestampRange(flavor);
		if (!range.isPresent()) {
			range = calculateTimestampRange(flavor);
		}
		return range;
	}

	public static IItemFilter getRangeAndThreadFilter(
		IItemStreamFlavor flavor, boolean showConcurrent, boolean containedIn, boolean sameThreads) {
		return getRangeAndThreadFilter(getRange(flavor), getThreads(flavor, showConcurrent, sameThreads),
				showConcurrent, containedIn, sameThreads);
	}

	public static IItemFilter getRangeAndThreadFilter(
		Optional<IRange<IQuantity>> range, Set<IMCThread> threads, boolean showConcurrent, boolean containedIn,
		boolean sameThreads) {
		IItemFilter rangeFilter = getRangeFilter(range, showConcurrent, containedIn);
		IItemFilter threadFilter = getThreadFilter(threads, sameThreads);

		// FIXME: Should this really be here, and not in the flavor calculation?
		// TODO: Would be nice to get the "time X in thread A, time Y in thread B" but probably expensive?
		if (rangeFilter != null && threadFilter != null) {
			return ItemFilters.and(rangeFilter, threadFilter);
		} else if (rangeFilter != null) {
			return rangeFilter;
		} else if (threadFilter != null) {
			return threadFilter;
		}
		return null;
	}

	private static IItemFilter getRangeFilter(
		Optional<IRange<IQuantity>> range, boolean showConcurrent, boolean containedIn) {
		if (showConcurrent && range.isPresent()) {
			RangeMatchPolicy rangePolicy;
			rangePolicy = containedIn ? RangeMatchPolicy.CONTAINED_IN_CLOSED
					: RangeMatchPolicy.CLOSED_INTERSECTS_WITH_CLOSED;
			return ItemFilters.matchRange(rangePolicy, JfrAttributes.LIFETIME, range.get());
		}
		return null;
	}

	private static IItemFilter getThreadFilter(Set<IMCThread> threads, boolean sameThreads) {
		if (sameThreads && threads != null && !threads.isEmpty()) {
			return ItemFilters.memberOf(JfrAttributes.EVENT_THREAD, threads);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static Optional<IRange<IQuantity>> extractTimestampRange(IItemStreamFlavor fromFlavor) {
		if (fromFlavor instanceof IPropertyFlavor) {
			return ((IPropertyFlavor) fromFlavor).getProperties()
					.filter(p -> p.getAttribute().getContentType().equals(UnitLookup.TIMERANGE)
							|| (p.getAttribute().getContentType().equals(UnitLookup.TIMESTAMP)
									&& p.getValue() instanceof IRange))
					.map(p -> ((IRange<IQuantity>) p.getValue())).findFirst();
		}
		return Optional.empty();
	}

	private static Optional<IRange<IQuantity>> calculateTimestampRange(IItemStreamFlavor fromFlavor) {
		if (fromFlavor != null) {
			IItemCollection items = fromFlavor.evaluate();
			IQuantity startTime = items.getAggregate(JdkAggregators.FIRST_ITEM_START);
			IQuantity endTime = items.getAggregate(JdkAggregators.LAST_ITEM_END);
			if (startTime != null) {
				if (endTime != null && startTime.compareTo(endTime) < 0) {
					return Optional.of(QuantityRange.createWithEnd(startTime, endTime));
				} else {
					return Optional.of(QuantityRange.createPoint(startTime));
				}
			}
		}
		return Optional.empty();
	}

	public static Set<IMCThread> getThreads(IItemStreamFlavor flavor, boolean showConcurrent, boolean sameThreads) {
		if (showConcurrent && sameThreads && !flavor.isEmpty()) {
			Set<IMCThread> threads = extractThreads(flavor);
			if (threads.isEmpty()) {
				threads = calculateThreads(flavor);
			}
			return threads;
		}
		return Collections.emptySet();
	}

	private static Set<IMCThread> extractThreads(IItemStreamFlavor fromFlavor) {
		if (fromFlavor instanceof IPropertyFlavor) {
			return ((IPropertyFlavor) fromFlavor).getProperties()
					.filter(p -> p.getAttribute().getContentType().equals(UnitLookup.THREAD))
					.flatMap(p -> p.getValue() instanceof Collection
							? Stream.of(((Collection<?>) p.getValue()).toArray()) : Stream.of(p.getValue()))
					.map(v -> ((IMCThread) v)).collect(Collectors.toSet());
		}
		return Collections.emptySet();
	}

	private static Set<IMCThread> calculateThreads(IItemStreamFlavor fromFlavor) {
		if (fromFlavor != null) {
			IItemCollection items = fromFlavor.evaluate();
			IAggregator<Set<IMCThread>, ?> distinctThreadsAggregator = Aggregators.distinct(JfrAttributes.EVENT_THREAD);
			return items.getAggregate(distinctThreadsAggregator);
		}
		return Collections.emptySet();
	}

	/**
	 * @param threads
	 *            Set of threads to use if non-empty.
	 * @param flavor
	 *            Flavor that might include a threadname property.
	 * @returna A set of thread names, preferring the names from the threads set if not empty,
	 *          otherwise tries to use flavor.
	 */
	public static Set<String> getThreadNames(Set<IMCThread> threads, IItemStreamFlavor flavor) {
		Set<String> threadNames = Collections.emptySet();

		if (threads != null && !threads.isEmpty()) {
			threadNames = threads.stream().map(t -> t.getThreadName()).collect(Collectors.toSet());
		} else if (flavor instanceof IPropertyFlavor
				&& ((IPropertyFlavor) flavor).getProperties().anyMatch(FlavorToolkit::threadMatcher)) {
			// FIXME: Do we care about showConcurrent and sameThreads here? probably not
			return ((IPropertyFlavor) flavor).getProperties().filter(FlavorToolkit::threadMatcher)
					.flatMap(FlavorToolkit::threadMapper).collect(Collectors.toSet());
		}
		return threadNames;
	}

	private static Stream<String> threadMapper(IPropertyFlavor.IProperty p) {
		if (p.getAttribute().equals(JdkAttributes.EVENT_THREAD_NAME)) {
			if (p.getValue() instanceof String) {
				return Stream.of((String) p.getValue());
			} else if (p.getValue() instanceof Collection) {
				return ((Collection<?>) p.getValue()).stream().map(o -> (String) o);
			}
		}
		return Stream.empty();
	}

	private static Boolean threadMatcher(IPropertyFlavor.IProperty p) {
		return p.getAttribute().equals(JdkAttributes.EVENT_THREAD_NAME);
	}
}
