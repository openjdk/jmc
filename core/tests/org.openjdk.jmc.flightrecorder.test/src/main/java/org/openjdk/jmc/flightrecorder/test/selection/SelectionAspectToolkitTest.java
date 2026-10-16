/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.test.selection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.openjdk.jmc.flightrecorder.test.selection.SelectionTestItems.count;
import static org.openjdk.jmc.flightrecorder.test.selection.SelectionTestItems.item;
import static org.openjdk.jmc.flightrecorder.test.selection.SelectionTestItems.items;
import static org.openjdk.jmc.flightrecorder.test.selection.SelectionTestItems.thread;
import static org.openjdk.jmc.flightrecorder.test.selection.SelectionTestItems.type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.item.Attribute;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemCollectionToolkit;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.item.RangeMatchPolicy;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.QuantityRange;
import org.openjdk.jmc.common.unit.StructContentType;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.selection.IFilterFlavor;
import org.openjdk.jmc.flightrecorder.selection.IItemStreamFlavor;
import org.openjdk.jmc.flightrecorder.selection.IPropertyFlavor;
import org.openjdk.jmc.flightrecorder.selection.ItemBackedSelection;
import org.openjdk.jmc.flightrecorder.selection.PointRangeSelection;
import org.openjdk.jmc.flightrecorder.selection.RangeSelection;
import org.openjdk.jmc.flightrecorder.selection.SelectionAspectToolkit;
import org.openjdk.jmc.flightrecorder.selection.SelectionAspectToolkit.PropertySummary;

public class SelectionAspectToolkitTest {
	private static final IAttribute<String> KEY = Attribute.attr("key", "Key", null, UnitLookup.PLAIN_TEXT);
	private static final IAttribute<String> OTHER = Attribute.attr("other", "Other", null, UnitLookup.PLAIN_TEXT);
	private static final IAttribute<IQuantity> NUMBER = Attribute.attr("number", "Number", null, UnitLookup.NUMBER);

	@Test
	public void testPropertyAspectsUseFullRecordingUniverse() {
		StructContentType<IItem> sourceType = type("source", KEY);
		StructContentType<IItem> destinationType = type("destination", KEY);
		IItem selected = item(sourceType, KEY, "shared");
		IItem destination = item(destinationType, KEY, "shared");
		IItem unrelatedType = item(type("third", KEY), KEY, "shared");
		IItemCollection universe = items(selected, destination, unrelatedType);
		List<IFilterFlavor> aspects = SelectionAspectToolkit.calculatePersistableFilterFlavors(items(selected),
				items(destination), universe, Collections.singletonList(KEY)).collect(Collectors.toList());
		assertEquals(1, aspects.size());
		assertEquals(3, count(aspects.get(0).evaluate()));
	}

	@Test
	public void testCommonAttributesAndDestinationRestriction() {
		IItemCollection source = items(item(type("first", KEY, OTHER), KEY, "shared", OTHER, "source-only"));
		IItemCollection destination = items(item(type("second", KEY), KEY, "shared"));
		assertEquals(Collections.singletonList(KEY),
				SelectionAspectToolkit
						.getCommonAttributes(Stream.concat(source.stream(), destination.stream()).iterator())
						.collect(Collectors.toList()));
		assertEquals(1, SelectionAspectToolkit.calculatePersistableFilterFlavors(source, destination,
				ItemCollectionToolkit.merge(() -> Stream.of(source, destination)), null).count());
		assertEquals(0, SelectionAspectToolkit.calculatePersistableFilterFlavors(source, destination, source,
				Collections.singletonList(OTHER), attribute -> !attribute.equals(OTHER)).count());
	}

	@Test
	public void testOrderingAndQuantityRangeSummary() {
		StructContentType<IItem> eventType = type("ordered", NUMBER, OTHER, KEY, JfrAttributes.START_TIME);
		IItemCollection source = items(
				item(eventType, NUMBER, UnitLookup.NUMBER_UNITY.quantity(9), OTHER, "b", KEY, "a",
						JfrAttributes.START_TIME, UnitLookup.EPOCH_NS.quantity(200)),
				item(eventType, NUMBER, UnitLookup.NUMBER_UNITY.quantity(2), OTHER, "b", KEY, "a",
						JfrAttributes.START_TIME, UnitLookup.EPOCH_NS.quantity(100)));
		List<IPropertyFlavor> aspects = SelectionAspectToolkit
				.calculatePersistableFilterFlavors(source, source, source, null).map(IPropertyFlavor.class::cast)
				.collect(Collectors.toList());
		assertEquals(Arrays.asList(JfrAttributes.START_TIME, KEY, OTHER, NUMBER), aspects.stream()
				.map(aspect -> aspect.getProperties().findFirst().get().getAttribute()).collect(Collectors.toList()));
		IRange<?> numbers = (IRange<?>) aspects.get(3).getProperties().findFirst().get().getValue();
		assertEquals(UnitLookup.NUMBER_UNITY.quantity(2), numbers.getStart());
		assertEquals(UnitLookup.NUMBER_UNITY.quantity(9), numbers.getEnd());
		assertEquals(2, count(aspects.get(3).evaluate()));
	}

	@Test
	public void testDistinctLimitPreservesExistingLookAhead() {
		StructContentType<IItem> eventType = type("distinct", KEY);
		List<IItem> events = new ArrayList<>();
		for (int index = 0; index < 11; index++) {
			events.add(item(eventType, KEY, "value-" + index));
		}
		IItemCollection eleven = ItemCollectionToolkit.build(events.stream());
		assertEquals(1, SelectionAspectToolkit.calculatePersistableFilterFlavors(eleven, eleven, eleven, null).count());
		events.add(item(eventType, KEY, "last"));
		IItemCollection twelve = ItemCollectionToolkit.build(events.stream());
		assertEquals(0, SelectionAspectToolkit.calculatePersistableFilterFlavors(twelve, twelve, twelve, null).count());
		PropertySummary summary = SelectionAspectToolkit.summarizeProperty(KEY, twelve.iterator(), 10);
		assertTrue(summary.isTooManyValues());
		assertNull(summary.getValue());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testPropertySummaryValueSetIsImmutable() {
		StructContentType<IItem> eventType = type("summary", KEY);
		IItemCollection source = items(item(eventType, KEY, "first"), item(eventType, KEY, "second"));
		PropertySummary summary = SelectionAspectToolkit.summarizeProperty(KEY, source.iterator(), 10);
		Set<String> values = (Set<String>) summary.getValue();
		assertEquals(Set.of("first", "second"), values);
		try {
			values.add("third");
			fail("Property summary values must be immutable");
		} catch (UnsupportedOperationException expected) {
			// Expected: callers must not mutate the values used to construct property aspects.
		}
		assertEquals(Set.of("first", "second"), summary.getValue());
	}

	@Test
	public void testDerivedPersistableAttributes() {
		List<IAttribute<?>> attributes = SelectionAspectToolkit
				.getPersistableAttributes(Stream.of(JfrAttributes.EVENT_THREAD, JfrAttributes.EVENT_STACKTRACE,
						JdkAttributes.COMPILER_METHOD, JdkAttributes.REC_SETTING_FOR,
						JdkAttributes.CLASS_DEFINING_CLASSLOADER, JdkAttributes.CLASS_INITIATING_CLASSLOADER,
						JdkAttributes.PARENT_CLASSLOADER, JdkAttributes.CLASSLOADER, JfrAttributes.EVENT_THREAD))
				.collect(Collectors.toList());
		assertEquals(Arrays.asList(JdkAttributes.EVENT_THREAD_NAME, JdkAttributes.STACK_TRACE_STRING,
				JdkAttributes.STACK_TRACE_TOP_METHOD_STRING, JdkAttributes.STACK_TRACE_TOP_CLASS_STRING,
				JdkAttributes.STACK_TRACE_TOP_PACKAGE, JdkAttributes.STACK_TRACE_BOTTOM_METHOD_STRING,
				JdkAttributes.COMPILER_METHOD_STRING, JdkAttributes.REC_SETTING_FOR_NAME,
				JdkAttributes.CLASS_DEFINING_CLASSLOADER_STRING, JdkAttributes.CLASS_INITIATING_CLASSLOADER_STRING,
				JdkAttributes.PARENT_CLASSLOADER_STRING, JdkAttributes.CLASSLOADER_STRING), attributes);
	}

	@Test
	public void testQuantityAndRangeSummariesIgnoreNullValues() {
		IAttribute<IRange<IQuantity>> ranges = Attribute.attr("range", "Range", null, UnitLookup.TIMERANGE);
		StructContentType<IItem> eventType = type("ranges", NUMBER, ranges);
		IItemCollection source = items(item(eventType, NUMBER, null, ranges, null),
				item(eventType, NUMBER, UnitLookup.NUMBER_UNITY.quantity(3), ranges,
						QuantityRange.createWithEnd(UnitLookup.EPOCH_NS.quantity(100),
								UnitLookup.EPOCH_NS.quantity(200))),
				item(eventType, NUMBER, UnitLookup.NUMBER_UNITY.quantity(1), ranges, QuantityRange
						.createWithEnd(UnitLookup.EPOCH_NS.quantity(50), UnitLookup.EPOCH_NS.quantity(400))));
		PropertySummary summary = SelectionAspectToolkit.summarizeProperty(ranges, source.iterator(), 10);
		assertFalse(summary.isTooManyValues());
		IRange<?> range = (IRange<?>) summary.getValue();
		assertEquals(UnitLookup.EPOCH_NS.quantity(50), range.getStart());
		assertEquals(UnitLookup.EPOCH_NS.quantity(400), range.getEnd());
		assertNull(SelectionAspectToolkit.summarizeProperty(NUMBER, ItemCollectionToolkit.EMPTY.iterator(), 10));
	}

	@Test
	public void testRangePrefersExplicitPropertyAndOtherwiseAggregatesEvents() {
		StructContentType<IItem> eventType = type("timed", JfrAttributes.START_TIME, JfrAttributes.DURATION);
		IItemCollection source = items(timed(eventType, 100, 10), timed(eventType, 50, 200));
		IRange<IQuantity> aggregate = SelectionAspectToolkit.getRange(IItemStreamFlavor.build("events", source)).get();
		assertEquals(UnitLookup.EPOCH_NS.quantity(50), aggregate.getStart());
		assertEquals(UnitLookup.EPOCH_NS.quantity(250), aggregate.getEnd());
		IRange<IQuantity> explicit = QuantityRange.createWithEnd(UnitLookup.EPOCH_NS.quantity(0),
				UnitLookup.EPOCH_NS.quantity(1000));
		assertEquals(explicit, SelectionAspectToolkit
				.getRange(IPropertyFlavor.buildPointRange(JfrAttributes.START_TIME, explicit, source)).get());
		assertFalse(SelectionAspectToolkit.getRange(null).isPresent());
		assertFalse(SelectionAspectToolkit.getRange(IItemStreamFlavor.build(ItemCollectionToolkit.EMPTY)).isPresent());
	}

	@Test
	public void testPointRangeAndConcurrentContainmentPolicies() {
		StructContentType<IItem> eventType = type("timed", JfrAttributes.START_TIME, JfrAttributes.DURATION,
				JfrAttributes.EVENT_THREAD);
		IMCThread selectedThread = thread(1, "selected");
		IMCThread otherThread = thread(2, "other");
		IItem selected = item(eventType, JfrAttributes.START_TIME, UnitLookup.EPOCH_NS.quantity(10),
				JfrAttributes.DURATION, UnitLookup.NANOSECOND.quantity(10), JfrAttributes.EVENT_THREAD, selectedThread);
		IItem enclosing = item(eventType, JfrAttributes.START_TIME, UnitLookup.EPOCH_NS.quantity(0),
				JfrAttributes.DURATION, UnitLookup.NANOSECOND.quantity(100), JfrAttributes.EVENT_THREAD,
				selectedThread);
		IItem other = item(eventType, JfrAttributes.START_TIME, UnitLookup.EPOCH_NS.quantity(12),
				JfrAttributes.DURATION, UnitLookup.NANOSECOND.quantity(2), JfrAttributes.EVENT_THREAD, otherThread);
		IItemCollection universe = items(selected, enclosing, other);
		IItemStreamFlavor reference = IItemStreamFlavor.build("reference", items(selected));
		IItemFilter concurrent = SelectionAspectToolkit.getRangeAndThreadFilter(reference, true, false, true);
		IItemFilter contained = SelectionAspectToolkit.getRangeAndThreadFilter(reference, true, true, true);
		assertEquals(2, count(universe.apply(concurrent)));
		assertEquals(1, count(universe.apply(contained)));
		assertEquals(3,
				count(universe.apply(SelectionAspectToolkit.getRangeAndThreadFilter(reference, true, false, false))));
		assertNull(SelectionAspectToolkit.getRangeAndThreadFilter(reference, false, false, false));
		IRange<IQuantity> point = SelectionAspectToolkit
				.getRange(IItemStreamFlavor.build("point", items(timed(eventType, 7, 0)))).get();
		assertEquals(point.getStart(), point.getEnd());
	}

	@Test
	public void testThreadsSentinelAndThreadNames() {
		IMCThread selectedThread = thread(1, "selected");
		StructContentType<IItem> eventType = type("threads", JfrAttributes.EVENT_THREAD);
		IItemCollection source = items(item(eventType, JfrAttributes.EVENT_THREAD, selectedThread));
		IItemStreamFlavor reference = IItemStreamFlavor.build("reference", source);
		assertEquals(Collections.singleton(selectedThread), SelectionAspectToolkit.getThreads(reference, true, true));
		assertTrue(SelectionAspectToolkit.getThreads(reference, false, true).isEmpty());
		assertTrue(
				SelectionAspectToolkit.getThreads(IItemStreamFlavor.build("none", true, source), true, true).isEmpty());
		assertTrue(SelectionAspectToolkit.getThreads(null, true, true).isEmpty());
		IPropertyFlavor names = IPropertyFlavor.build(JdkAttributes.EVENT_THREAD_NAME, Set.of("one", "two"), source);
		assertEquals(Set.of("one", "two"), SelectionAspectToolkit.getThreadNames(Collections.emptySet(), names));
		assertEquals(Collections.singleton("selected"),
				SelectionAspectToolkit.getThreadNames(Collections.singleton(selectedThread), names));
		IPropertyFlavor threads = IPropertyFlavor.build(JfrAttributes.EVENT_THREAD, selectedThread, source);
		assertEquals(Collections.singleton(selectedThread), SelectionAspectToolkit.getThreads(threads, true, true));
	}

	@Test
	public void testNullRangeAndThreadPropertiesAreIgnored() {
		IPropertyFlavor flavor = new IPropertyFlavor() {
			@Override
			public String getIdentifier() {
				return "missing-properties";
			}

			@Override
			public String getName() {
				return "Missing properties";
			}

			@Override
			public IItemCollection evaluate() {
				return ItemCollectionToolkit.EMPTY;
			}

			@Override
			public IItemFilter getFilter() {
				return ItemFilters.type("missing");
			}

			@Override
			public Stream<IProperty> getProperties() {
				return Stream.of(null, IPropertyFlavor.buildProperty(JfrAttributes.LIFETIME, null),
						IPropertyFlavor.buildProperty(JfrAttributes.EVENT_THREAD, null),
						IPropertyFlavor.buildProperty(JdkAttributes.EVENT_THREAD_NAME, null));
			}
		};
		assertFalse(SelectionAspectToolkit.getRange(flavor).isPresent());
		assertTrue(SelectionAspectToolkit.getThreads(flavor, true, true).isEmpty());
		assertTrue(SelectionAspectToolkit.getThreadNames(Collections.emptySet(), flavor).isEmpty());
		assertNull(SelectionAspectToolkit.getRangeAndThreadFilter(flavor, true, false, true));
		assertNull(SelectionAspectToolkit.getRangeAndThreadFilter(Optional.empty(), Collections.singleton(null), false,
				false, true));
	}

	@Test
	public void testNullPropertyValuesFallBackToPopulatedEvents() {
		IMCThread selectedThread = thread(1, "selected");
		StructContentType<IItem> eventType = type("populated", JfrAttributes.START_TIME, JfrAttributes.DURATION,
				JfrAttributes.EVENT_THREAD);
		IItemCollection source = items(
				item(eventType, JfrAttributes.START_TIME, UnitLookup.EPOCH_NS.quantity(10), JfrAttributes.DURATION,
						UnitLookup.NANOSECOND.quantity(20), JfrAttributes.EVENT_THREAD, selectedThread),
				item(eventType, JfrAttributes.START_TIME, UnitLookup.EPOCH_NS.quantity(40), JfrAttributes.DURATION,
						UnitLookup.NANOSECOND.quantity(5), JfrAttributes.EVENT_THREAD, null));
		IPropertyFlavor range = IPropertyFlavor.build(ItemFilters.type("populated"), source, JfrAttributes.LIFETIME,
				null, "Missing lifetime");
		IRange<IQuantity> fallback = SelectionAspectToolkit.getRange(range).get();
		assertEquals(UnitLookup.EPOCH_NS.quantity(10), fallback.getStart());
		assertEquals(UnitLookup.EPOCH_NS.quantity(45), fallback.getEnd());
		IPropertyFlavor threads = IPropertyFlavor.build(ItemFilters.type("populated"), source,
				JfrAttributes.EVENT_THREAD, null, "Missing thread");
		assertEquals(Collections.singleton(selectedThread), SelectionAspectToolkit.getThreads(threads, true, true));
		IItemCollection nullThreads = items(item(eventType, JfrAttributes.EVENT_THREAD, null));
		assertTrue(SelectionAspectToolkit.getThreads(IItemStreamFlavor.build(nullThreads), true, true).isEmpty());
	}

	@Test
	public void testNullThreadCollectionElementsAndNamesAreIgnored() {
		IMCThread selectedThread = thread(1, "selected");
		IPropertyFlavor threads = IPropertyFlavor.build(ItemFilters.type("missing"), ItemCollectionToolkit.EMPTY,
				JfrAttributes.EVENT_THREAD, Arrays.asList(null, selectedThread), "Thread collection");
		assertEquals(Collections.singleton(selectedThread), SelectionAspectToolkit.getThreads(threads, true, true));
		Set<IMCThread> threadSet = new HashSet<>(Arrays.asList(null, selectedThread, thread(2, null)));
		assertEquals(Collections.singleton("selected"), SelectionAspectToolkit.getThreadNames(threadSet, threads));
		IPropertyFlavor names = IPropertyFlavor.build(ItemFilters.type("missing"), ItemCollectionToolkit.EMPTY,
				JdkAttributes.EVENT_THREAD_NAME, Arrays.asList(null, "selected"), "Thread names");
		assertEquals(Collections.singleton("selected"),
				SelectionAspectToolkit.getThreadNames(Collections.emptySet(), names));
	}

	@Test
	public void testAspectIdentifiersSurviveDisplayNamesAndDestinationOrdering() {
		StructContentType<IItem> sourceType = type("source", KEY);
		StructContentType<IItem> destinationType = type("destination", KEY);
		IItemCollection selected = items(item(sourceType, KEY, "shared"));
		IItemCollection universe = ItemCollectionToolkit
				.merge(() -> Stream.of(selected, items(item(destinationType, KEY, "shared"))));
		IItemStreamFlavor direct = IItemStreamFlavor.build("first display name", selected);
		assertSame(selected, direct.evaluate());
		assertEquals(direct.getIdentifier(), IItemStreamFlavor.build("another display name", selected).getIdentifier());
		IPropertyFlavor property = IPropertyFlavor.build(KEY, "shared", universe);
		IPropertyFlavor renamed = IPropertyFlavor.build(ItemFilters.equals(KEY, "shared"), universe, KEY, "shared",
				"different display name");
		assertEquals(property.getIdentifier(), renamed.getIdentifier());
		ItemBackedSelection selection = new ItemBackedSelection(selected, "selection");
		List<IItemStreamFlavor> sourceAspects = SelectionAspectToolkit.listAspects(selection,
				ItemFilters.type("source"), universe, Collections.singletonList(KEY));
		List<IItemStreamFlavor> destinationAspects = SelectionAspectToolkit.listAspects(selection,
				ItemFilters.type("destination"), universe, Collections.singletonList(KEY));
		assertEquals("events", sourceAspects.get(0).getIdentifier());
		assertEquals("events", destinationAspects.get(destinationAspects.size() - 1).getIdentifier());
		IItemStreamFlavor sourceProperty = sourceAspects.stream().filter(IPropertyFlavor.class::isInstance).findFirst()
				.get();
		IItemStreamFlavor destinationProperty = destinationAspects.stream().filter(IPropertyFlavor.class::isInstance)
				.findFirst().get();
		assertEquals(sourceProperty.getIdentifier(), destinationProperty.getIdentifier());
		assertEquals(2, count(destinationProperty.evaluate()));
		assertSame(selected, sourceAspects.get(0).evaluate());
		try {
			destinationAspects.clear();
			fail("Aspect lists must be immutable");
		} catch (UnsupportedOperationException expected) {
			// Expected: the list is a snapshot, while the flavors borrow their source collections.
		}
	}

	@Test
	public void testPointRangeSelectionIncludesBothPointBoundaries() {
		StructContentType<IItem> eventType = type("points", JfrAttributes.START_TIME, JfrAttributes.DURATION);
		IItemCollection universe = items(timed(eventType, 1, 0), timed(eventType, 2, 0), timed(eventType, 3, 0));
		PointRangeSelection selection = new PointRangeSelection("points", universe, UnitLookup.EPOCH_NS.quantity(1),
				UnitLookup.EPOCH_NS.quantity(2), JfrAttributes.START_TIME);
		List<IItemStreamFlavor> aspects = SelectionAspectToolkit.listAspects(selection, null, universe,
				Collections.emptyList());
		assertEquals(2, count(aspects.get(0).evaluate()));
		IPropertyFlavor range = aspects.stream().filter(IPropertyFlavor.class::isInstance)
				.map(IPropertyFlavor.class::cast).filter(aspect -> aspect.getProperties()
						.anyMatch(p -> p.getAttribute().equals(JfrAttributes.START_TIME)))
				.findFirst().get();
		assertEquals(2, count(range.evaluate()));
	}

	@Test
	public void testRangeSelectionPreservesSelectionAndAspectPolicies() {
		StructContentType<IItem> eventType = type("ranges", JfrAttributes.START_TIME, JfrAttributes.DURATION);
		IItemCollection universe = items(timed(eventType, 0, 3), timed(eventType, 1, 0), timed(eventType, 2, 0),
				timed(eventType, 3, 0));
		IRange<IQuantity> selectedRange = QuantityRange.createWithEnd(UnitLookup.EPOCH_NS.quantity(1),
				UnitLookup.EPOCH_NS.quantity(2));
		RangeSelection selection = new RangeSelection("range", universe, selectedRange,
				RangeMatchPolicy.CLOSED_INTERSECTS_WITH_CLOSED, JfrAttributes.LIFETIME);
		List<IItemStreamFlavor> aspects = SelectionAspectToolkit.listAspects(selection, null, universe,
				Collections.emptyList());
		assertEquals(3, count(aspects.get(0).evaluate()));
		IPropertyFlavor range = aspects.stream().filter(IPropertyFlavor.class::isInstance)
				.map(IPropertyFlavor.class::cast)
				.filter(aspect -> aspect.getProperties().anyMatch(p -> p.getAttribute().equals(JfrAttributes.LIFETIME)))
				.findFirst().get();
		assertEquals(2, count(range.evaluate()));
	}

	private static IItem timed(StructContentType<IItem> eventType, long start, long duration) {
		return item(eventType, JfrAttributes.START_TIME, UnitLookup.EPOCH_NS.quantity(start), JfrAttributes.DURATION,
				UnitLookup.NANOSECOND.quantity(duration));
	}
}
