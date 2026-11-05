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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.Attribute;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemCollectionToolkit;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.item.ItemIterableToolkit;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.selection.FilterBackedSelection;
import org.openjdk.jmc.flightrecorder.selection.ItemBackedSelection;
import org.openjdk.jmc.flightrecorder.selection.SelectionToolkit;
import org.openjdk.jmc.test.mock.item.MockAttributes;
import org.openjdk.jmc.test.mock.item.MockItem;
import org.openjdk.jmc.test.mock.item.MockNumberType;

@SuppressWarnings("nls")
public class SelectionToolkitTest {

	@Test
	public void testSetOperationsUseIdentityAndRemoveRepeatedReferences() {
		MockNumberType type = new MockNumberType();
		EqualItem first = new EqualItem(type, 1);
		EqualItem second = new EqualItem(type, 1);
		EqualItem third = new EqualItem(type, 1);
		assertEquals(first, second);
		IItemCollection a = items(first, second, first);
		IItemCollection b = items(second, third, second);
		assertIdentities(SelectionToolkit.intersect(a, b), second);
		assertIdentities(SelectionToolkit.union(a, b), first, second, third);
		assertIdentities(SelectionToolkit.subtract(a, b), first);
	}

	@Test
	public void testResultsCanBeFilteredAndAggregatedWithoutTheirInputs() {
		MockNumberType type = new MockNumberType();
		IItem one = new EqualItem(type, 1);
		IItem two = new EqualItem(type, 2);
		IItem three = new EqualItem(type, 3);
		IItemCollection selected = SelectionToolkit.union(items(one, two), items(two, three));
		assertEquals(6, selected.getAggregate(Aggregators.sum(MockAttributes.DOUBLE_VALUE)).longValue());
		IItemCollection narrowed = selected
				.apply(ItemFilters.more(MockAttributes.DOUBLE_VALUE, UnitLookup.NUMBER_UNITY.quantity(1)));
		assertEquals(2, narrowed.getAggregate(Aggregators.count()).longValue());
		assertEquals(5, narrowed.getAggregate(Aggregators.sum(MockAttributes.DOUBLE_VALUE)).longValue());
	}

	@Test
	public void testEmptyAndIdenticalInputs() {
		MockNumberType type = new MockNumberType();
		IItem first = new EqualItem(type, 1);
		IItem second = new EqualItem(type, 2);
		IItemCollection source = items(first, second, first);
		assertIdentities(SelectionToolkit.union(source, source), first, second);
		assertIdentities(SelectionToolkit.intersect(source, source), first, second);
		assertFalse(SelectionToolkit.subtract(source, source).hasItems());
		assertFalse(SelectionToolkit.intersect(source, ItemCollectionToolkit.EMPTY).hasItems());
		assertFalse(SelectionToolkit.intersect(ItemCollectionToolkit.EMPTY, source).hasItems());
		assertIdentities(SelectionToolkit.union(ItemCollectionToolkit.EMPTY, source), first, second);
		assertIdentities(SelectionToolkit.subtract(source, ItemCollectionToolkit.EMPTY), first, second);
		assertFalse(SelectionToolkit.subtract(ItemCollectionToolkit.EMPTY, source).hasItems());
	}

	@Test
	public void testSeparatePopulationsWithEqualValuesHaveNoIntersection() {
		MockNumberType type = new MockNumberType();
		IItem first = new EqualItem(type, 1);
		IItem separatelyCreated = new EqualItem(type, 1);
		assertFalse(SelectionToolkit.intersect(items(first), items(separatelyCreated)).hasItems());
		assertIdentities(SelectionToolkit.subtract(items(first), items(separatelyCreated)), first);
		assertIdentities(SelectionToolkit.union(items(first), items(separatelyCreated)), first, separatelyCreated);
	}

	@Test
	public void testFilterBackedSelectionsOverSameSourceComposeFilters() {
		IItemCollection source = numbers(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
		FilterBackedSelection above3 = new FilterBackedSelection(source, more(3), "above3");
		FilterBackedSelection below8 = new FilterBackedSelection(source, less(8), "below8");
		assertComposed(source, SelectionToolkit.intersect(above3, below8, "i"),
				SelectionToolkit.intersect(above3.getItems(), below8.getItems()));
		assertComposed(source, SelectionToolkit.union(above3, below8, "u"),
				SelectionToolkit.union(above3.getItems(), below8.getItems()));
		assertComposed(source, SelectionToolkit.subtract(above3, below8, "s"),
				SelectionToolkit.subtract(above3.getItems(), below8.getItems()));
		assertEquals(4, count(SelectionToolkit.intersect(above3, below8, "i").getItems()));
		assertEquals(3, count(SelectionToolkit.subtract(above3, below8, "s").getItems()));
	}

	@Test
	public void testComposedSelectionsCanBeComposedAgain() {
		IItemCollection source = numbers(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
		FilterBackedSelection low = new FilterBackedSelection(source, less(4), "low");
		FilterBackedSelection high = new FilterBackedSelection(source, more(7), "high");
		FilterBackedSelection even = new FilterBackedSelection(source,
				ItemFilters.or(equal(2), equal(4), equal(6), equal(8), equal(10)), "even");
		ItemBackedSelection chained = SelectionToolkit.subtract(SelectionToolkit.union(low, high, "edges"), even,
				"oddEdges");
		IItemCollection expected = SelectionToolkit.subtract(SelectionToolkit.union(low.getItems(), high.getItems()),
				even.getItems());
		assertComposed(source, chained, expected);
		assertEquals(13, sum(chained.getItems()));
	}

	@Test
	public void testCompositionKeepsEventsOfTypesWithoutTheFilteredAttribute() {
		IAttribute<IQuantity> value = Attribute.attr("value", "Value", UnitLookup.NUMBER);
		IAttribute<String> label = Attribute.attr("label", "Label", UnitLookup.PLAIN_TEXT);
		IType<IItem> valued = SelectionTestItems.type("valued", value);
		IType<IItem> labelled = SelectionTestItems.type("labelled", label);
		IItem one = SelectionTestItems.item(valued, value, UnitLookup.NUMBER_UNITY.quantity(1));
		IItem five = SelectionTestItems.item(valued, value, UnitLookup.NUMBER_UNITY.quantity(5));
		IItem text = SelectionTestItems.item(labelled, label, "text");
		IItemCollection source = items(one, five, text);
		FilterBackedSelection everything = new FilterBackedSelection(source, ItemFilters.all(), "all");
		FilterBackedSelection large = new FilterBackedSelection(source,
				ItemFilters.more(value, UnitLookup.NUMBER_UNITY.quantity(2)), "large");
		ItemBackedSelection notLarge = SelectionToolkit.subtract(everything, large, "notLarge");
		assertComposed(source, notLarge, SelectionToolkit.subtract(everything.getItems(), large.getItems()));
		assertEquals(Set.of(one, text), identitySet(notLarge.getItems()));
		assertEquals(Set.of(five), identitySet(SelectionToolkit.intersect(everything, large, "i").getItems()));
	}

	@Test
	public void testInputsWithoutCommonFilterSourceFallBackToIdentity() {
		IItemCollection source = numbers(1, 2, 3, 4, 5);
		FilterBackedSelection a = new FilterBackedSelection(source, more(1), "a");
		FilterBackedSelection otherSource = new FilterBackedSelection(source.apply(ItemFilters.all()), less(4),
				"otherSource");
		ItemBackedSelection itemBacked = new ItemBackedSelection(otherSource.getItems(), "itemBacked");
		for (ItemBackedSelection b : List.of(otherSource, itemBacked)) {
			ItemBackedSelection intersection = SelectionToolkit.intersect(a, b, "i");
			assertFalse(intersection instanceof FilterBackedSelection);
			assertEquals(5, sum(intersection.getItems()));
			assertEquals(15, sum(SelectionToolkit.union(a, b, "u").getItems()));
			assertEquals(9, sum(SelectionToolkit.subtract(a, b, "s").getItems()));
			assertFalse(SelectionToolkit.union(b, a, "u") instanceof FilterBackedSelection);
		}
	}

	@Test
	public void testNarrowingKeepsSourceAndComposes() {
		IItemCollection source = numbers(1, 2, 3, 4, 5, 6);
		FilterBackedSelection above1 = new FilterBackedSelection(source, more(1), "above1");
		FilterBackedSelection narrowed = above1.apply(less(5), "middle");
		assertSame(source, narrowed.getSource());
		assertEquals("middle", narrowed.getName());
		assertEquals(9, sum(narrowed.getItems()));
		assertTrue(SelectionToolkit.intersect(narrowed, above1, "i") instanceof FilterBackedSelection);
	}

	@Test
	public void testIdentityResultsKeepFirstEncounteredTypeOrder() {
		MockNumberType first = new MockNumberType();
		MockNumberType second = new MockNumberType();
		IItem a1 = new EqualItem(first, 1);
		IItem b1 = new EqualItem(second, 2);
		IItem a2 = new EqualItem(first, 3);
		IItem b2 = new EqualItem(second, 4);
		assertIdentities(SelectionToolkit.union(ordered(a1, b1), ordered(b2, a2)), a1, a2, b1, b2);
		assertIdentities(SelectionToolkit.subtract(ordered(b1, a1, b2, a2), ordered(a2)), b1, b2, a1);
	}

	private static void assertComposed(IItemCollection source, ItemBackedSelection actual, IItemCollection expected) {
		assertTrue(actual instanceof FilterBackedSelection);
		assertSame(source, ((FilterBackedSelection) actual).getSource());
		assertEquals(identitySet(expected), identitySet(actual.getItems()));
		assertEquals(count(expected), count(actual.getItems()));
	}

	private static Set<IItem> identitySet(IItemCollection items) {
		Set<IItem> result = Collections.newSetFromMap(new IdentityHashMap<>());
		items.stream().flatMap(iterable -> iterable.stream()).forEach(result::add);
		return result;
	}

	private static IItemCollection numbers(Number ... values) {
		MockNumberType type = new MockNumberType();
		return ItemCollectionToolkit.build(Stream.of(values).map(value -> new EqualItem(type, value)));
	}

	private static IItemFilter more(long value) {
		return ItemFilters.more(MockAttributes.DOUBLE_VALUE, UnitLookup.NUMBER_UNITY.quantity(value));
	}

	private static IItemFilter less(long value) {
		return ItemFilters.less(MockAttributes.DOUBLE_VALUE, UnitLookup.NUMBER_UNITY.quantity(value));
	}

	private static IItemFilter equal(long value) {
		return ItemFilters.equals(MockAttributes.DOUBLE_VALUE, UnitLookup.NUMBER_UNITY.quantity(value));
	}

	private static long count(IItemCollection items) {
		return items.getAggregate(Aggregators.count()).longValue();
	}

	private static long sum(IItemCollection items) {
		IQuantity sum = items.getAggregate(Aggregators.sum(MockAttributes.DOUBLE_VALUE));
		return sum == null ? 0 : sum.longValue();
	}

	@SuppressWarnings("unchecked")
	private static IItemCollection ordered(IItem ... items) {
		return ItemCollectionToolkit.build(() -> Stream.of(items)
				.map(item -> ItemIterableToolkit.build(() -> Stream.of(item), (IType<IItem>) item.getType())));
	}

	private static IItemCollection items(IItem ... items) {
		return ItemCollectionToolkit.build(Stream.of(items));
	}

	private static void assertIdentities(IItemCollection actual, IItem ... expected) {
		List<IItem> found = actual.stream().flatMap(iterable -> iterable.stream()).collect(Collectors.toList());
		assertEquals(expected.length, found.size());
		for (int i = 0; i < expected.length; i++) {
			assertSame(expected[i], found.get(i));
		}
	}

	private static final class EqualItem extends MockItem<Number, MockNumberType> {
		private EqualItem(MockNumberType type, Number value) {
			super(value, type, 0L);
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof EqualItem && getValue().equals(((EqualItem) other).getValue());
		}

		@Override
		public int hashCode() {
			return getValue().hashCode();
		}
	}
}
