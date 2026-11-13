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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.Test;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemCollectionToolkit;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.selection.FlavoredSelectionBase;
import org.openjdk.jmc.flightrecorder.selection.IFlavoredSelection;
import org.openjdk.jmc.flightrecorder.selection.IItemStreamFlavor;
import org.openjdk.jmc.flightrecorder.selection.ItemBackedSelection;
import org.openjdk.jmc.flightrecorder.selection.NamedSelectionRegistry;
import org.openjdk.jmc.flightrecorder.selection.NamedSelectionRegistry.Entry;
import org.openjdk.jmc.flightrecorder.selection.SelectionToolkit;
import org.openjdk.jmc.test.mock.item.MockAttributes;
import org.openjdk.jmc.test.mock.item.MockItem;
import org.openjdk.jmc.test.mock.item.MockNumberType;

@SuppressWarnings("nls")
public class NamedSelectionRegistryTest {

	@Test
	public void testStoredItemsCanBeReusedForAggregationAndFiltering() {
		try (NamedSelectionRegistry registry = new NamedSelectionRegistry()) {
			IItemCollection source = numbers(1, 2, 3, 4);
			Entry entry = registry.put("source", source);
			assertSame(source, registry.getItems("source").get());
			assertSame(registry.get(entry.getId()).get(), registry.getByName("source").get());
			assertEquals(10, registry.getItems("source").get()
					.getAggregate(Aggregators.sum(MockAttributes.DOUBLE_VALUE)).longValue());

			registry.put("at-least-two", registry.getItems("source").get()
					.apply(ItemFilters.moreOrEqual(MockAttributes.DOUBLE_VALUE, UnitLookup.NUMBER_UNITY.quantity(2))));
			registry.put("two-to-three", registry.getItems("at-least-two").get()
					.apply(ItemFilters.lessOrEqual(MockAttributes.DOUBLE_VALUE, UnitLookup.NUMBER_UNITY.quantity(3))));
			IItemCollection narrowed = registry.getItems("two-to-three").get();
			assertEquals(2, narrowed.getAggregate(Aggregators.count()).longValue());
			assertEquals(5, narrowed.getAggregate(Aggregators.sum(MockAttributes.DOUBLE_VALUE)).longValue());
		}
	}

	@Test
	public void testSelectionReferenceIsStoredWithoutChangingItsDisplayName() {
		try (NamedSelectionRegistry registry = new NamedSelectionRegistry()) {
			IFlavoredSelection selection = new ItemBackedSelection(numbers(7), "Display name");
			Entry entry = registry.put("lookup-name", selection);
			assertEquals("lookup-name", entry.getName());
			assertSame(selection, registry.get(entry.getId()).get());
			assertEquals("Display name", registry.getByName("lookup-name").get().getName());
		}
	}

	@Test
	public void testReplacementInvalidatesOldIdAndKeepsIndexesConsistent() {
		try (NamedSelectionRegistry registry = new NamedSelectionRegistry()) {
			Entry previous = registry.put("same", numbers(1));
			IFlavoredSelection captured = registry.get(previous.getId()).get();
			Entry replacement = registry.put("same", numbers(2, 3));
			assertNotEquals(previous.getId(), replacement.getId());
			assertFalse(registry.get(previous.getId()).isPresent());
			assertSame(registry.get(replacement.getId()).get(), registry.getByName("same").get());
			assertEquals(1, registry.list().size());
			assertEquals(replacement.getId(), registry.list().get(0).getId());
			assertFalse(registry.remove(previous.getId()));
			assertEquals(2, registry.getItems("same").get().getAggregate(Aggregators.count()).longValue());
			assertEquals(1, ((ItemBackedSelection) captured).getItems().getAggregate(Aggregators.count()).longValue());
			assertTrue(registry.removeByName("same"));
			assertFalse(registry.get(replacement.getId()).isPresent());
			assertFalse(registry.getByName("same").isPresent());
		}
	}

	@Test
	public void testExactNamesAndRemovalById() {
		try (NamedSelectionRegistry registry = new NamedSelectionRegistry()) {
			Entry spaced = registry.put(" Name ", numbers(1));
			Entry differentCase = registry.put(" name ", numbers(2));
			assertEquals(" Name ", spaced.getName());
			assertFalse(registry.getByName("Name").isPresent());
			assertEquals(2, registry.list().size());
			assertTrue(registry.remove(spaced.getId()));
			assertFalse(registry.getByName(" Name ").isPresent());
			assertTrue(registry.get(differentCase.getId()).isPresent());
			assertFalse(registry.remove(spaced.getId()));
			assertFalse(registry.removeByName("missing"));
		}
	}

	@Test
	public void testEmptyItemsAreDifferentFromUnknownOrNonItemSelections() {
		try (NamedSelectionRegistry registry = new NamedSelectionRegistry()) {
			registry.put("empty", ItemCollectionToolkit.EMPTY);
			assertTrue(registry.getItems("empty").isPresent());
			assertFalse(registry.getItems("empty").get().hasItems());
			assertFalse(registry.getItems("unknown").isPresent());
			IFlavoredSelection selection = new FlavoredSelectionBase("other") {
				@Override
				public Stream<IItemStreamFlavor> getFlavors(
					IItemFilter filter, IItemCollection items, List<IAttribute<?>> attributes) {
					throw new AssertionError("Direct item retrieval must not evaluate arbitrary selections");
				}
			};
			Entry entry = registry.put("other", selection);
			assertSame(selection, registry.get(entry.getId()).get());
			assertFalse(registry.getItems("other").isPresent());
		}
	}

	@Test
	public void testMetadataSnapshotHasNoPayloadAndOutlivesRegistry() {
		NamedSelectionRegistry registry = new NamedSelectionRegistry();
		Entry first = registry.put("first", numbers(1));
		List<Entry> metadata = registry.list();
		registry.put("second", numbers(2));
		assertEquals(1, metadata.size());
		assertThrows(UnsupportedOperationException.class, () -> metadata.add(first));
		registry.close();
		assertEquals("first", metadata.get(0).getName());
		assertEquals(first.getId(), metadata.get(0).getId());
		for (Field field : Entry.class.getDeclaredFields()) {
			if (!field.isSynthetic() && !Modifier.isStatic(field.getModifiers())) {
				assertEquals("Metadata must not retain a payload or its owning registry", String.class,
						field.getType());
			}
		}
	}

	@Test
	public void testClearLeavesRegistryUsableAndIdsDoNotReturn() {
		try (NamedSelectionRegistry registry = new NamedSelectionRegistry()) {
			Entry previous = registry.put("same", numbers(1));
			registry.clear();
			assertTrue(registry.list().isEmpty());
			assertFalse(registry.get(previous.getId()).isPresent());
			assertFalse(registry.getByName("same").isPresent());
			Entry replacement = registry.put("same", numbers(2));
			assertNotEquals(previous.getId(), replacement.getId());
			assertFalse(registry.isClosed());
		}
	}

	@Test
	public void testCloseRejectsOperationsButDoesNotRevokeCapturedData() {
		NamedSelectionRegistry registry = new NamedSelectionRegistry();
		Entry entry = registry.put("source", numbers(1, 2));
		IFlavoredSelection selection = registry.get(entry.getId()).get();
		IItemCollection captured = registry.getItems("source").get();
		registry.close();
		registry.close();
		assertTrue(registry.isClosed());
		assertThrows(IllegalStateException.class, () -> registry.put("new", captured));
		assertThrows(IllegalStateException.class, () -> registry.put("new", selection));
		assertThrows(IllegalStateException.class, () -> registry.get(entry.getId()));
		assertThrows(IllegalStateException.class, () -> registry.getByName("source"));
		assertThrows(IllegalStateException.class, () -> registry.getItems("source"));
		assertThrows(IllegalStateException.class, registry::list);
		assertThrows(IllegalStateException.class, () -> registry.remove(entry.getId()));
		assertThrows(IllegalStateException.class, () -> registry.removeByName("source"));
		assertThrows(IllegalStateException.class, registry::clear);
		assertSame(captured, ((ItemBackedSelection) selection).getItems());
		assertEquals(3, captured.getAggregate(Aggregators.sum(MockAttributes.DOUBLE_VALUE)).longValue());
	}

	@Test
	public void testCloseWinsAgainstWaitingPut() throws Exception {
		NamedSelectionRegistry registry = new NamedSelectionRegistry();
		IItemCollection items = numbers(1);
		CountDownLatch started = new CountDownLatch(1);
		FutureTask<Boolean> put = new FutureTask<>(() -> {
			started.countDown();
			try {
				registry.put("racing", items);
				return false;
			} catch (IllegalStateException expected) {
				return true;
			}
		});
		Thread writer = new Thread(put, "selection-registry-test");
		try {
			synchronized (registry) {
				writer.start();
				assertTrue(started.await(5, TimeUnit.SECONDS));
				registry.close();
			}
			assertTrue(put.get(5, TimeUnit.SECONDS));
		} finally {
			registry.close();
			writer.join(5000);
		}
		assertFalse(writer.isAlive());
	}

	@Test
	public void testDerivedSelectionSurvivesSourceRemoval() {
		try (NamedSelectionRegistry registry = new NamedSelectionRegistry()) {
			Entry source = registry.put("source", numbers(1, 2, 3));
			IItemCollection all = registry.getItems("source").get();
			IItemCollection excluded = all
					.apply(ItemFilters.equals(MockAttributes.DOUBLE_VALUE, UnitLookup.NUMBER_UNITY.quantity(1)));
			registry.put("derived", SelectionToolkit.subtract(all, excluded));
			assertTrue(registry.remove(source.getId()));
			IItemCollection derived = registry.getItems("derived").get();
			assertEquals(5, derived.getAggregate(Aggregators.sum(MockAttributes.DOUBLE_VALUE)).longValue());
			assertEquals(1,
					derived.apply(ItemFilters.more(MockAttributes.DOUBLE_VALUE, UnitLookup.NUMBER_UNITY.quantity(2)))
							.getAggregate(Aggregators.count()).longValue());
		}
	}

	@Test
	public void testInvalidNamesAndNullPayloadsDoNotReplaceExistingSelection() {
		try (NamedSelectionRegistry registry = new NamedSelectionRegistry()) {
			Entry valid = registry.put("valid", numbers(1));
			assertThrows(IllegalArgumentException.class, () -> registry.put(" \t", ItemCollectionToolkit.EMPTY));
			assertThrows(NullPointerException.class, () -> registry.put(null, ItemCollectionToolkit.EMPTY));
			assertThrows(NullPointerException.class, () -> registry.put("valid", (IItemCollection) null));
			assertThrows(NullPointerException.class, () -> registry.put("valid", (IFlavoredSelection) null));
			assertTrue(registry.get(valid.getId()).isPresent());
			assertEquals(1, registry.list().size());
		}
	}

	private static IItemCollection numbers(Number ... values) {
		MockNumberType type = new MockNumberType();
		return ItemCollectionToolkit.build(Stream.of(values).map(value -> new MockItem<>(value, type, 0L)));
	}
}
