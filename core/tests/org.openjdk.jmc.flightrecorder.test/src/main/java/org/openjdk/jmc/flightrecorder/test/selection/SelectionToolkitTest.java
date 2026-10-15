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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.ItemCollectionToolkit;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.UnitLookup;
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
