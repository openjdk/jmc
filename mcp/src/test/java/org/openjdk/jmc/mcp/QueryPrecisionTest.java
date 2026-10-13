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
package org.openjdk.jmc.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.IMCThreadGroup;
import org.openjdk.jmc.common.item.Attribute;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.ItemCollectionToolkit;
import org.openjdk.jmc.common.unit.BinaryPrefix;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.mcp.GroupedQueryTools.Group;
import org.openjdk.jmc.mcp.QuerySupport.Predicate;
import org.openjdk.jmc.mcp.RecordingService.Recording;

class QueryPrecisionTest {

	@Test
	void largeBinaryUnitOutputUsesNativeConvertedNumbers() {
		IUnit exbibyte = UnitLookup.MEMORY.getUnit(BinaryPrefix.EXBI);
		for (IQuantity quantity : List.of(exbibyte.quantity(1), exbibyte.quantity(0.5))) {
			assertDecimal(QuerySupport.decimal(quantity.numberValueIn(UnitLookup.BYTE)),
					QuerySupport.quantityDecimal(quantity, UnitLookup.BYTE));
		}
	}

	@Test
	void inverseBinaryUnitOutputFormatsTheNativeFractionalResult() {
		IUnit exbibyte = UnitLookup.MEMORY.getUnit(BinaryPrefix.EXBI);
		IQuantity quantity = UnitLookup.BYTE.quantity(1);
		assertDecimal(QuerySupport.decimal(quantity.numberValueIn(exbibyte)),
				QuerySupport.quantityDecimal(quantity, exbibyte));
	}

	@Test
	void quantityOutputPreservesSourceIntegerAndNativeCanonicalNumber() {
		IQuantity quantity = UnitLookup.NANOSECOND.quantity(9007199254740993L);
		@SuppressWarnings("unchecked")
		Map<String, Object> output = (Map<String, Object>) EventValues.encode(quantity);
		Number canonical = quantity.numberValueIn(quantity.getType().getDefaultUnit());
		assertEquals(9007199254740993L, output.get("value"));
		assertEquals("9007199254740993", output.get("exactValue"));
		assertEquals(canonical, output.get("canonicalValue"));
		assertEquals(TestItems.plain(canonical), output.get("canonicalExactValue"));
	}

	@Test
	void namedThreadsWithoutJavaIdsRemainDistinctFromEachOtherAndMissingValues() {
		IMCThread alpha = threadWithoutId("alpha");
		IItemCollection items = TestItems.collection("test.Sample",
				List.of(JfrAttributes.EVENT_THREAD, JfrAttributes.DURATION),
				List.of(Map.of("eventThread", alpha, "duration", UnitLookup.NANOSECOND.quantity(1)),
						Map.of("eventThread", threadWithoutId("beta"), "duration", UnitLookup.NANOSECOND.quantity(1)),
						Map.of("eventThread", alpha, "duration", UnitLookup.NANOSECOND.quantity(1)),
						Map.of("duration", UnitLookup.NANOSECOND.quantity(1))));
		Map<List<Object>, Group> groups = GroupedQueryTools.computeGroups(items, List.of("eventThread"),
				List.of("duration"));
		assertEquals(3, groups.size());
		assertEquals(2L, countForThreadName(groups, "alpha"));
		assertEquals(1L, countForThreadName(groups, "beta"));
		assertEquals(1, groups.values().stream().filter(group -> group.displayKey.getFirst() == null).count());
		assertEquals(4L, groups.values().stream().mapToLong(group -> group.count).sum());
	}

	@Test
	void nativeThreadsWithZeroJavaIdsAndIdenticalNamesRemainSeparateByOsIdentity() {
		IItemCollection items = TestItems.collection("test.Sample",
				List.of(JfrAttributes.EVENT_THREAD, JfrAttributes.DURATION),
				List.of(Map.of("eventThread", new NativeThread(10L), "duration", UnitLookup.NANOSECOND.quantity(1)),
						Map.of("eventThread", new NativeThread(20L), "duration", UnitLookup.NANOSECOND.quantity(1)),
						Map.of("eventThread", new NativeThread(10L), "duration", UnitLookup.NANOSECOND.quantity(1))));
		Map<List<Object>, Group> groups = GroupedQueryTools.computeGroups(items, List.of("eventThread"),
				List.of("duration"));
		assertEquals(2, groups.size());
		assertEquals(List.of(1L, 2L), groups.values().stream().map(group -> group.count).sorted().toList());
		assertEquals(3L, countForThreadName(groups, "native worker"));
	}

	@Test
	void storedNumericSourceIgnoresUnrelatedTextMetadataWithSameIdentifier() throws Exception {
		IAttribute<Long> numeric = Attribute.attr("x", "X", UnitLookup.RAW_LONG);
		IAttribute<String> text = Attribute.attr("x", "X", UnitLookup.PLAIN_TEXT);
		IItemCollection source = TestItems.collection("test.Numeric", List.of(numeric),
				List.of(Map.of("x", 5L), Map.of("x", -2L)));
		IItemCollection unrelated = TestItems.collection("test.Text", List.of(text), List.of(Map.of("x", "text")));
		Recording recording = TestItems.recording(ItemCollectionToolkit.merge(() -> Stream.of(source, unrelated)));
		recording.store("numeric", source);
		IItemCollection selected = QuerySupport.select(recording, "numeric", null, null, null,
				List.of(new Predicate("x", "gt", "0", null)));
		assertEquals(1, JfrToolkit.countItems(selected));
		assertEquals(5L, QuerySupport.value(JfrToolkit.toItemList(selected).getFirst(), "x"));
	}

	@Test
	void storedAttributeFilterIgnoresUnmatchedTextTypeMetadata() throws Exception {
		IAttribute<Long> numeric = Attribute.attr("x", "X", UnitLookup.RAW_LONG);
		IAttribute<String> text = Attribute.attr("x", "X", UnitLookup.PLAIN_TEXT);
		IItemCollection source = TestItems.collection("test.Numeric", List.of(numeric),
				List.of(Map.of("x", 5L), Map.of("x", -2L)));
		IItemCollection unrelated = TestItems.collection("test.Text", List.of(text), List.of(Map.of("x", "text")));
		Recording recording = TestItems.recording(ItemCollectionToolkit.merge(() -> Stream.of(source, unrelated)));
		recording.store("numeric", JfrToolkit.filterByAttribute(recording.getItems(), "x", "5"));
		IItemCollection selected = QuerySupport.select(recording, "numeric", null, null, null,
				List.of(new Predicate("x", "gt", "0", null)));
		assertEquals(1, JfrToolkit.countItems(selected));
		assertEquals(5L, QuerySupport.value(JfrToolkit.toItemList(selected).getFirst(), "x"));
	}

	@Test
	void quantityFilterKeepsAdjacentIntegerOperandDistinctFromNativeDouble() {
		IQuantity quantity = UnitLookup.NUMBER_UNITY.quantity(9007199254740992d);
		assertFalse(QuerySupport.matches(quantity, new Predicate("x", "eq", "9007199254740993", null)));
		assertTrue(QuerySupport.matches(quantity, new Predicate("x", "ne", "9007199254740993", null)));
		assertTrue(QuerySupport.matches(quantity, new Predicate("x", "lt", "9007199254740993", null)));
		assertFalse(QuerySupport.matches(quantity, new Predicate("x", "gte", "9007199254740993", null)));
	}

	@Test
	void nonFiniteRawAndQuantityValuesFailEveryNumericComparison() {
		for (double value : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
			for (String operator : List.of("eq", "ne", "gt", "gte", "lt", "lte")) {
				Predicate filter = new Predicate("x", operator, "0", null);
				assertFalse(QuerySupport.matches(value, filter), operator + " must reject " + value);
				assertFalse(QuerySupport.matches(UnitLookup.NUMBER_UNITY.quantity(value), filter),
						operator + " must reject quantity " + value);
			}
			assertTrue(QuerySupport.matches(value, new Predicate("x", "exists", null, null)));
		}
	}

	@Test
	void sourceNonFiniteValuesDoNotAbortOrEnterFilteredResultSets() throws Exception {
		IAttribute<Number> numeric = Attribute.attr("x", "X", UnitLookup.RAW_NUMBER);
		IItemCollection items = TestItems.collection("test.Numeric", List.of(numeric), List.of(Map.of("x", Double.NaN),
				Map.of("x", Double.POSITIVE_INFINITY), Map.of("x", Double.NEGATIVE_INFINITY), Map.of("x", 2.0)));
		Recording recording = TestItems.recording(items);
		for (String operator : List.of("gt", "ne")) {
			IItemCollection selected = QuerySupport.select(recording, null, "test.Numeric", null, null,
					List.of(new Predicate("x", operator, "0", null)));
			assertEquals(1, JfrToolkit.countItems(selected));
			assertEquals(2.0, QuerySupport.value(JfrToolkit.toItemList(selected).getFirst(), "x"));
		}
	}

	private static IMCThread threadWithoutId(String name) {
		return new IMCThread() {
			@Override
			public Long getThreadId() {
				return null;
			}

			@Override
			public String getThreadName() {
				return name;
			}

			@Override
			public IMCThreadGroup getThreadGroup() {
				return null;
			}
		};
	}

	private static long countForThreadName(Map<List<Object>, Group> groups, String name) {
		return groups.values().stream().filter(group -> group.displayKey.getFirst() instanceof Map<?, ?> thread
				&& name.equals(thread.get("name"))).mapToLong(group -> group.count).sum();
	}

	/** Mirrors the JFR parser's thread equality for native threads with no Java ID. */
	private static final class NativeThread implements IMCThread {
		private final long osId;

		NativeThread(long osId) {
			this.osId = osId;
		}

		@Override
		public Long getThreadId() {
			return 0L;
		}

		@Override
		public String getThreadName() {
			return "native worker";
		}

		@Override
		public IMCThreadGroup getThreadGroup() {
			return null;
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof NativeThread thread && osId == thread.osId;
		}

		@Override
		public int hashCode() {
			return Long.hashCode(osId);
		}
	}

	private static void assertDecimal(BigDecimal expected, BigDecimal actual) {
		assertEquals(0, expected.compareTo(actual),
				"Expected " + expected.toPlainString() + " but got " + actual.toPlainString());
	}
}
