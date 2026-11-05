/*
 * Copyright (c) 2018, 2026, Oracle and/or its affiliates. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.flightrecorder.selection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.item.RangeMatchPolicy;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.KindOfQuantity;
import org.openjdk.jmc.common.unit.QuantitiesToolkit;
import org.openjdk.jmc.common.unit.QuantityRange;
import org.openjdk.jmc.common.unit.RangeContentType;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;

/**
 * Headless discovery and evaluation helpers for the aspects of a recording selection.
 * <p>
 * The destination collection and attributes guide discovery. Returned flavors retain their source
 * collections and should be used only within the lifetime of the recording that supplies them.
 */
public final class SelectionAspectToolkit {
	private static final Object TOO_MANY_VALUES = new Object();

	/**
	 * Summary of values for an attribute. Values may include shared parsed objects and retain their
	 * referenced data; this is not a detached metadata descriptor.
	 */
	public static final class PropertySummary {
		final IAttribute<?> attribute;
		final Object value;

		PropertySummary(IAttribute<?> attribute, Object value) {
			this.attribute = attribute;
			if (value instanceof Set<?>) {
				this.value = Collections.unmodifiableSet(new LinkedHashSet<>((Set<?>) value));
			} else if (value instanceof Collection<?>) {
				this.value = Collections.unmodifiableList(new ArrayList<>((Collection<?>) value));
			} else {
				this.value = value;
			}
		}

		public IAttribute<?> getAttribute() {
			return attribute;
		}

		public Object getValue() {
			return isTooManyValues() ? null : value;
		}

		public boolean isTooManyValues() {
			return value == TOO_MANY_VALUES;
		}
	}

	private SelectionAspectToolkit() {
	}

	/**
	 * Lists the aspects for a destination context in their existing relevance order.
	 * <p>
	 * The list is immutable, but its flavors retain live collection references. It must not be used
	 * as a detached descriptor cache beyond the lifetime of the recording.
	 */
	public static List<IItemStreamFlavor> listAspects(
		IFlavoredSelection selection, IItemFilter destinationFilter, IItemCollection recordingItems,
		List<IAttribute<?>> destinationAttributes) {
		Objects.requireNonNull(selection, "selection"); //$NON-NLS-1$
		Objects.requireNonNull(recordingItems, "recordingItems"); //$NON-NLS-1$
		return List.copyOf(selection.getFlavors(destinationFilter, recordingItems, destinationAttributes)
				.collect(Collectors.toList()));
	}

	public static Stream<IAttribute<?>> getCommonAttributes(Iterator<? extends IItemIterable> iterables)
			throws IllegalArgumentException {
		// FIXME: List of attributes for the item collection should be provided from elsewhere
		if (!iterables.hasNext()) {
			return Stream.empty();
		} else {
			IItemIterable single = iterables.next();
			@SuppressWarnings("deprecation")
			List<IAttribute<?>> attributes = single.getType().getAttributes();
			if (iterables.hasNext()) {
				attributes = new ArrayList<>(attributes); // modifiable copy
				while (iterables.hasNext()) {
					IType<?> otherType = iterables.next().getType();
					// FIXME: Use a Set<IType<?>> to avoid going through any type more than once.
					Iterator<IAttribute<?>> aIterator = attributes.iterator();
					while (aIterator.hasNext()) {
						if (!otherType.hasAttribute(aIterator.next())) {
							aIterator.remove();
						}
					}
				}
			}
			// FIXME: Possible remove this filter if we convert this to persistable attributes.
			return attributes.stream().filter(a -> a.getContentType() != UnitLookup.STACKTRACE);
		}
	}

	public static Stream<IFilterFlavor> calculatePersistableFilterFlavors(
		IItemCollection srcItems, IItemCollection dstItems, IItemCollection allItems,
		List<IAttribute<?>> dstAttributes) {
		return calculatePersistableFilterFlavors(srcItems, dstItems, allItems, dstAttributes, a -> true);
	}

	public static Stream<IFilterFlavor> calculatePersistableFilterFlavors(
		IItemCollection srcItems, IItemCollection dstItems, IItemCollection allItems, List<IAttribute<?>> dstAttributes,
		Predicate<IAttribute<?>> include) {
		// FIXME: Calculate common content types from the dstItems, and see if any of the srcItems can deliver them?
		Stream<IAttribute<?>> commonAttributes = null;
		if (dstAttributes != null && !dstAttributes.isEmpty()) {
			commonAttributes = getCommonAttributes(srcItems.iterator()).filter(a -> dstAttributes.contains(a));
		} else {
			Stream<? extends IItemIterable> items = Stream.concat(srcItems.stream(), dstItems.stream());
			commonAttributes = getCommonAttributes(items.iterator());
		}
		Stream<IAttribute<?>> persistableAttributes = getPersistableAttributes(commonAttributes).filter(include::test);
		// FIXME: Add combinations here as well, similar to PropertySelection.getFlavors
		// FIXME: Can we get construct a life time filter from start and end times?
		return persistableAttributes.map(attr -> summarizeProperty(attr, srcItems.iterator(), MAX_DISTINCT_VALUES))
				.filter(p -> p != null && p.value != TOO_MANY_VALUES).sorted(RELEVANCE_ORDER)
				.map(p -> IPropertyFlavor.build(p.attribute, p.value, allItems));
	}

	private static final int MAX_DISTINCT_VALUES = 10;

	// FIXME: How to order? (currently quantity attributes last). Should we involve relational key attributes?
	private static final Comparator<PropertySummary> RELEVANCE_ORDER = new Comparator<PropertySummary>() {

		@Override
		public int compare(PropertySummary o1, PropertySummary o2) {
			int a1c = getAttributeCategory(o1.getAttribute());
			int a2c = getAttributeCategory(o2.getAttribute());
			if (a1c == a2c) {
				return o1.getAttribute().getIdentifier().compareTo(o2.getAttribute().getIdentifier());
			}
			return Integer.compare(a1c, a2c);
		}

		private int getAttributeCategory(IAttribute<?> attr) {
			ContentType<?> ct = attr.getContentType();
			if (ct.equals(UnitLookup.TIMESTAMP)) {
				return 0;
			} else if (ct instanceof KindOfQuantity) {
				return 2;
			}
			return 1;
		}

	};

	public static <M> PropertySummary summarizeProperty(
		IAttribute<M> attribute, Iterator<? extends IItemIterable> iterables, int maxDistinct) {
		ContentType<M> contentType = attribute.getContentType();
		if (contentType instanceof KindOfQuantity) {
			@SuppressWarnings("unchecked")
			IAttribute<IQuantity> qAttribute = (IAttribute<IQuantity>) attribute;
			IQuantity minValue = null;
			IQuantity maxValue = null;
			while (iterables.hasNext()) {
				IItemIterable ii = iterables.next();
				IMemberAccessor<IQuantity, IItem> accessor = qAttribute.getAccessor(ii.getType());
				Iterator<? extends IItem> items = ii.iterator();
				while (items.hasNext()) {
					IQuantity val = accessor.getMember(items.next());
					if (val == null) {
						// FIXME: Should null values be expected/accepted?
					} else if (minValue == null) {
						minValue = maxValue = val;
					} else {
						minValue = QuantitiesToolkit.min(val, minValue);
						maxValue = QuantitiesToolkit.max(val, maxValue);
					}
				}
			}

			if (minValue != null) {
				if (minValue == maxValue) {
					return new PropertySummary(qAttribute, minValue);
				} else {
					return new PropertySummary(qAttribute, QuantityRange.createWithEnd(minValue, maxValue));
				}
			}
		} else if (contentType instanceof RangeContentType) {
			if (((RangeContentType<?>) contentType).getEndPointContentType() instanceof KindOfQuantity) {
				@SuppressWarnings("unchecked")
				IAttribute<IRange<IQuantity>> rangeAttribute = (IAttribute<IRange<IQuantity>>) attribute;
				IQuantity minValue = null;
				IQuantity maxValue = null;
				while (iterables.hasNext()) {
					IItemIterable ii = iterables.next();
					IMemberAccessor<IRange<IQuantity>, IItem> accessor = rangeAttribute.getAccessor(ii.getType());
					Iterator<? extends IItem> items = ii.iterator();
					while (items.hasNext()) {
						IRange<IQuantity> range = accessor.getMember(items.next());
						if (range == null) {
							// FIXME: Should null values be expected/accepted?
						} else if (minValue == null) {
							minValue = range.getStart();
							maxValue = range.getEnd();
						} else {
							minValue = QuantitiesToolkit.min(range.getStart(), minValue);
							maxValue = QuantitiesToolkit.max(range.getEnd(), maxValue);
						}
					}
				}

				if (minValue != null) {
					if (minValue == maxValue) {
						return new PropertySummary(rangeAttribute, minValue);
					} else {
						return new PropertySummary(rangeAttribute, QuantityRange.createWithEnd(minValue, maxValue));
					}
				}
			}
		}

		Set<M> keys = new HashSet<>();
		while (iterables.hasNext()) {
			IItemIterable ii = iterables.next();
			IMemberAccessor<M, IItem> accessor = attribute.getAccessor(ii.getType());
			Iterator<? extends IItem> items = ii.iterator();
			while (items.hasNext()) {
				if (keys.size() > maxDistinct) {
					return new PropertySummary(attribute, TOO_MANY_VALUES);
				}
				// FIXME: Add more limitations if there are a lot of items?
				keys.add(accessor.getMember(items.next()));
			}
		}
		if (keys.size() == 0) {
			return null;
		} else if (keys.size() == 1) {
			return new PropertySummary(attribute, keys.iterator().next());
		} else {
			return new PropertySummary(attribute, keys);
		}

	}

	public static Stream<IAttribute<?>> getPersistableAttributes(Stream<IAttribute<?>> attributes) {
		// FIXME: Would like to always be able to persist a string representation of the attribute, because this is usable by filters.

		// FIXME: Should we always include event type? Does it make any sense, except on the custom pages?

		// FIXME: Transform both START_TIME and END_TIME to LIFETIME?
		// FIXME: Add derived attributes, like a conversion of any THREAD or CLASS attribute? Thread group?
		/*
		 * Make sure to do the conversions in the right order, so for example a stack trace can be
		 * converted to a top method, which then is converted to a method string.
		 */
		return attributes.map(a -> a.equals(JfrAttributes.EVENT_THREAD) ? JdkAttributes.EVENT_THREAD_NAME : a)
				.flatMap(a -> a.equals(JfrAttributes.EVENT_STACKTRACE) ? Stream.of(JdkAttributes.STACK_TRACE_STRING,
						JdkAttributes.STACK_TRACE_TOP_METHOD_STRING, JdkAttributes.STACK_TRACE_TOP_CLASS_STRING,
						JdkAttributes.STACK_TRACE_TOP_PACKAGE, JdkAttributes.STACK_TRACE_BOTTOM_METHOD_STRING)
						: Stream.of(a))
				.map(a -> a.equals(JdkAttributes.COMPILER_METHOD) ? JdkAttributes.COMPILER_METHOD_STRING : a)
				// FIXME: String or id?
				.map(a -> a.equals(JdkAttributes.REC_SETTING_FOR) ? JdkAttributes.REC_SETTING_FOR_NAME : a)
				.map(a -> a.equals(JdkAttributes.CLASS_DEFINING_CLASSLOADER)
						? JdkAttributes.CLASS_DEFINING_CLASSLOADER_STRING : a)
				.map(a -> a.equals(JdkAttributes.CLASS_INITIATING_CLASSLOADER)
						? JdkAttributes.CLASS_INITIATING_CLASSLOADER_STRING : a)
				.map(a -> a.equals(JdkAttributes.PARENT_CLASSLOADER) ? JdkAttributes.PARENT_CLASSLOADER_STRING : a)
				.map(a -> a.equals(JdkAttributes.CLASSLOADER) ? JdkAttributes.CLASSLOADER_STRING : a)
				.filter(a -> a.equals(JfrAttributes.EVENT_TYPE) || (a.getContentType() instanceof RangeContentType)
						|| (a.getContentType().getPersister() != null))
				.distinct();
	}

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
		if (sameThreads && threads != null) {
			Set<IMCThread> presentThreads = threads.stream().filter(Objects::nonNull).collect(Collectors.toSet());
			if (!presentThreads.isEmpty()) {
				return ItemFilters.memberOf(JfrAttributes.EVENT_THREAD, presentThreads);
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static Optional<IRange<IQuantity>> extractTimestampRange(IItemStreamFlavor fromFlavor) {
		if (fromFlavor instanceof IPropertyFlavor) {
			return ((IPropertyFlavor) fromFlavor).getProperties().filter(Objects::nonNull)
					.filter(p -> p.getValue() instanceof IRange
							&& (p.getAttribute().getContentType().equals(UnitLookup.TIMERANGE)
									|| p.getAttribute().getContentType().equals(UnitLookup.TIMESTAMP)))
					.map(p -> ((IRange<IQuantity>) p.getValue())).findFirst();
		}
		return Optional.empty();
	}

	private static Optional<IRange<IQuantity>> calculateTimestampRange(IItemStreamFlavor fromFlavor) {
		if (fromFlavor != null) {
			IItemCollection items = fromFlavor.evaluate();
			IAggregator<IQuantity, ?> startTimeAggregator = Aggregators.min(JfrAttributes.START_TIME);
			IAggregator<IQuantity, ?> endTimeAggregator = Aggregators.max(JfrAttributes.END_TIME);
			IQuantity startTime = items.getAggregate(startTimeAggregator);
			IQuantity endTime = items.getAggregate(endTimeAggregator);
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
		if (showConcurrent && sameThreads && flavor != null && !flavor.isEmpty()) {
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
			return ((IPropertyFlavor) fromFlavor).getProperties().filter(Objects::nonNull)
					.filter(p -> p.getAttribute().getContentType().equals(UnitLookup.THREAD))
					.flatMap(p -> p.getValue() instanceof Collection
							? Stream.of(((Collection<?>) p.getValue()).toArray()) : Stream.of(p.getValue()))
					.filter(Objects::nonNull).map(v -> ((IMCThread) v)).collect(Collectors.toSet());
		}
		return Collections.emptySet();
	}

	private static Set<IMCThread> calculateThreads(IItemStreamFlavor fromFlavor) {
		if (fromFlavor != null) {
			IItemCollection items = fromFlavor.evaluate();
			IAggregator<Set<IMCThread>, ?> distinctThreadsAggregator = Aggregators.distinct(JfrAttributes.EVENT_THREAD);
			Set<IMCThread> threads = items.getAggregate(distinctThreadsAggregator);
			return threads == null ? Collections.emptySet()
					: threads.stream().filter(Objects::nonNull).collect(Collectors.toSet());
		}
		return Collections.emptySet();
	}

	/**
	 * @param threads
	 *            Set of threads to use if non-empty.
	 * @param flavor
	 *            Flavor that might include a threadname property.
	 * @return a set of thread names, preferring the names from the threads set if not empty,
	 *         otherwise tries to use flavor.
	 */
	public static Set<String> getThreadNames(Set<IMCThread> threads, IItemStreamFlavor flavor) {
		Set<String> threadNames = Collections.emptySet();

		if (threads != null && !threads.isEmpty()) {
			threadNames = threads.stream().filter(Objects::nonNull).map(IMCThread::getThreadName)
					.filter(Objects::nonNull).collect(Collectors.toSet());
		} else if (flavor instanceof IPropertyFlavor
				&& ((IPropertyFlavor) flavor).getProperties().anyMatch(SelectionAspectToolkit::threadMatcher)) {
			// FIXME: Do we care about showConcurrent and sameThreads here? probably not
			return ((IPropertyFlavor) flavor).getProperties().filter(SelectionAspectToolkit::threadMatcher)
					.flatMap(SelectionAspectToolkit::threadMapper).collect(Collectors.toSet());
		}
		return threadNames;
	}

	private static Stream<String> threadMapper(IPropertyFlavor.IProperty p) {
		if (p.getAttribute().equals(JdkAttributes.EVENT_THREAD_NAME)) {
			if (p.getValue() instanceof String) {
				return Stream.of((String) p.getValue());
			} else if (p.getValue() instanceof Collection) {
				return ((Collection<?>) p.getValue()).stream().filter(Objects::nonNull).map(o -> (String) o);
			}
		}
		return Stream.empty();
	}

	private static Boolean threadMatcher(IPropertyFlavor.IProperty p) {
		return p != null && p.getAttribute().equals(JdkAttributes.EVENT_THREAD_NAME);
	}
}
