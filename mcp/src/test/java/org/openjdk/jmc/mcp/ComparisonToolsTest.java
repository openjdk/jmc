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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.Attribute;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.mcp.QuerySupport.Predicate;
import org.openjdk.jmc.mcp.RecordingService.Recording;

class ComparisonToolsTest {

	private static final long EPOCH = 1_700_000_000_000_000_000L;
	private static final IAttribute<String> VEHICLE = Attribute.attr("vehicle", "Vehicle", UnitLookup.PLAIN_TEXT);
	private static final IAttribute<IQuantity> HP = Attribute.attr("hpAtStart", "HP", UnitLookup.NUMBER);
	private static final IAttribute<IQuantity> ITERATIONS = Attribute.attr("iterations", "Iterations",
			UnitLookup.NUMBER);
	private static final IAttribute<IQuantity> REMAINING = Attribute.attr("remainingSeconds", "Remaining",
			UnitLookup.NUMBER);
	private static final IAttribute<IQuantity> CONTEXT = Attribute.attr("context", "Context", UnitLookup.TIMESPAN);
	private static final List<IAttribute<?>> ATTRIBUTES = List.of(JfrAttributes.START_TIME, JfrAttributes.DURATION,
			VEHICLE, HP, ITERATIONS, REMAINING, CONTEXT);
	private static final List<Predicate> FILTERS = List.of(new Predicate("hpAtStart", "eq", "0", null),
			new Predicate("iterations", "eq", "6", null), new Predicate("remainingSeconds", "gt", "0", null));

	@Test
	void appliesIdenticalCompoundFiltersAndNormalizesDeltaUnits() throws Exception {
		ComparisonTools tools = tools(
				List.of(row("truck", 0, 6, 5, UnitLookup.SECOND.quantity(1), 0),
						row("truck", 0, 6, 7, UnitLookup.SECOND.quantity(3), 1),
						row("car", 0, 6, 1, UnitLookup.SECOND.quantity(2), 2),
						row("excluded", 1, 6, 20, UnitLookup.SECOND.quantity(100), 3)),
				List.of(row("truck", 0, 6, 9, UnitLookup.MILLISECOND.quantity(3_000), 0),
						row("truck", 0, 6, 11, UnitLookup.MILLISECOND.quantity(5_000), 1),
						row("bike", 0, 6, 2, UnitLookup.MILLISECOND.quantity(1_000), 2),
						row("excluded", 0, 5, 20, UnitLookup.MILLISECOND.quantity(100_000), 3)));
		Map<String, Object> result = tools.compareRecordings("baseline", "candidate", "terrain", "vehicle",
				"duration,remainingSeconds", FILTERS, List.of(50.0, 90.0), null, null, null, null);
		assertFalse(result.containsKey("error"), EventValues.json(result));
		assertEquals(3L, result.get("baselineEvents"));
		assertEquals(3L, result.get("candidateEvents"));
		assertEquals(1L, result.get("bothGroups"));
		assertEquals(1L, result.get("baselineOnlyGroups"));
		assertEquals(1L, result.get("candidateOnlyGroups"));
		assertEquals("absolute event counts; no recording-length normalization", result.get("countNormalization"));
		Map<String, Object> truck = group(result, "vehicle", "truck");
		assertEquals("both", truck.get("presence"));
		assertEquals(0L, truck.get("countDelta"));
		assertDecimal("0", truck.get("countChangePercent"));
		Map<String, Object> duration = map(map(truck.get("changes")).get("duration"));
		assertDecimal("4", map(map(duration.get("sum")).get("delta")).get("exactValue"));
		assertEquals("s", map(map(duration.get("sum")).get("delta")).get("unit"));
		assertDecimal("100", map(duration.get("sum")).get("changePercent"));
		assertDecimal("2", map(map(duration.get("median")).get("delta")).get("exactValue"));
		Map<String, Object> percentile = map(map(duration.get("percentiles")).get("90"));
		IQuantity baselinePercentile = UnitLookup.SECOND.quantity(1).add(UnitLookup.SECOND.quantity(2).multiply(0.9));
		IQuantity candidatePercentile = UnitLookup.MILLISECOND.quantity(3_000)
				.add(UnitLookup.MILLISECOND.quantity(2_000).multiply(0.9));
		assertEquals(candidatePercentile.subtract(baselinePercentile).numberValueIn(UnitLookup.SECOND).toString(),
				map(percentile.get("delta")).get("exactValue"));

		Map<String, Object> car = group(result, "vehicle", "car");
		assertEquals("baselineOnly", car.get("presence"));
		assertNull(car.get("candidate"));
		assertNull(car.get("countDelta"));
		assertNull(car.get("changes"));
		Map<String, Object> bike = group(result, "vehicle", "bike");
		assertEquals("candidateOnly", bike.get("presence"));
		assertNull(bike.get("baseline"));
		assertNull(bike.get("countChangePercent"));
		assertNull(bike.get("changes"));
	}

	@Test
	void zeroBaselineAndMissingNumericValuesHaveNullPercentages() throws Exception {
		Map<String, Object> missing = row("missing", 0, 6, 1, UnitLookup.NANOSECOND.quantity(1), 1);
		missing.put("remainingSeconds", null);
		ComparisonTools tools = tools(List.of(row("zero", 0, 6, 0, UnitLookup.NANOSECOND.quantity(0), 0), missing),
				List.of(row("zero", 0, 6, 4, UnitLookup.NANOSECOND.quantity(4), 0),
						row("missing", 0, 6, 5, UnitLookup.NANOSECOND.quantity(1), 1)));
		Map<String, Object> result = tools.compareRecordings("baseline", "candidate", "terrain", "vehicle",
				"remainingSeconds", null, List.of(), null, null, null, null);
		Map<String, Object> zero = map(map(group(result, "vehicle", "zero").get("changes")).get("remainingSeconds"));
		assertNull(map(zero.get("avg")).get("changePercent"));
		assertDecimal("4", map(map(zero.get("avg")).get("delta")).get("exactValue"));
		Map<String, Object> missingChanges = map(
				map(group(result, "vehicle", "missing").get("changes")).get("remainingSeconds"));
		assertNull(map(missingChanges.get("avg")).get("delta"));
		assertNull(map(missingChanges.get("avg")).get("changePercent"));
	}

	@Test
	void usesNativeAggregatePrecisionAndPreservesIntegerExtrema() throws Exception {
		long large = 9_007_199_254_740_993L;
		List<Map<String, Object>> baseline = List.of(row("truck", 0, 6, large, UnitLookup.NANOSECOND.quantity(1), 0));
		List<Map<String, Object>> candidate = List
				.of(row("truck", 0, 6, large + 1, UnitLookup.NANOSECOND.quantity(1), 0));
		IQuantity baselineSum = TestItems.collection("terrain", ATTRIBUTES, baseline)
				.getAggregate(Aggregators.sum(REMAINING));
		IQuantity candidateSum = TestItems.collection("terrain", ATTRIBUTES, candidate)
				.getAggregate(Aggregators.sum(REMAINING));
		ComparisonTools tools = tools(baseline, candidate);
		Map<String, Object> result = tools.compareRecordings("baseline", "candidate", "terrain", "vehicle",
				"remainingSeconds", null, null, null, null, null, null);
		Map<String, Object> truck = group(result, "vehicle", "truck");
		Map<String, Object> left = map(map(map(truck.get("baseline")).get("statistics")).get("remainingSeconds"));
		Map<String, Object> right = map(map(map(truck.get("candidate")).get("statistics")).get("remainingSeconds"));
		assertEquals(TestItems.plain(baselineSum.numberValue()), map(left.get("sum")).get("exactValue"));
		assertEquals(TestItems.plain(candidateSum.numberValue()), map(right.get("sum")).get("exactValue"));
		assertEquals(Long.toString(large), map(left.get("min")).get("exactValue"));
		assertEquals(Long.toString(large + 1), map(right.get("min")).get("exactValue"));
		Map<String, Object> changes = map(map(truck.get("changes")).get("remainingSeconds"));
		assertEquals(candidateSum.subtract(baselineSum).numberValue().toString(),
				map(map(changes.get("sum")).get("delta")).get("exactValue"));
		assertEquals(candidateSum.subtract(baselineSum).ratioTo(baselineSum) * 100,
				map(changes.get("sum")).get("changePercent"));
		for (String metric : List.of("min", "max", "median")) {
			assertEquals("1", map(map(changes.get(metric)).get("delta")).get("exactValue"));
		}
	}

	@Test
	void groupsEquivalentNumericKeysAndPaginatesTheUnion() throws Exception {
		Map<String, Object> left = row("baselineName", 0, 6, 1, UnitLookup.NANOSECOND.quantity(1), 0);
		left.put("context", UnitLookup.SECOND.quantity(1));
		Map<String, Object> right = row("candidateName", 0, 6, 1, UnitLookup.NANOSECOND.quantity(1), 0);
		right.put("context", UnitLookup.MILLISECOND.quantity(1_000));
		ComparisonTools tools = tools(List.of(left), List.of(right));
		Map<String, Object> result = tools.compareRecordings("baseline", "candidate", "terrain", "context", null, null,
				null, null, null, null, null);
		assertEquals(1, result.get("totalGroups"));
		assertEquals(1L, result.get("bothGroups"));
		assertEquals("both", groups(result).getFirst().get("presence"));

		ComparisonTools paged = tools(List.of(row("a", 0, 6, 1, UnitLookup.NANOSECOND.quantity(1), 0)),
				List.of(row("b", 0, 6, 1, UnitLookup.NANOSECOND.quantity(1), 0),
						row("c", 0, 6, 1, UnitLookup.NANOSECOND.quantity(1), 1)));
		Map<String, Object> page = paged.compareRecordings("baseline", "candidate", "terrain", "vehicle", null, null,
				null, null, null, 1, 1);
		assertEquals(3, page.get("totalGroups"));
		assertEquals(1L, page.get("baselineEvents"));
		assertEquals(2L, page.get("candidateEvents"));
		assertEquals(1L, page.get("countDelta"));
		assertDecimal("100", page.get("countChangePercent"));
		assertEquals(2L, page.get("nextOffset"));
		assertEquals(true, page.get("truncated"));
		assertEquals("b", map(groups(page).getFirst().get("key")).get("vehicle"));
	}

	@Test
	void timeWindowIsRelativeToEachRecordingStartAndEmptyMatchesStayValid() throws Exception {
		List<Map<String, Object>> baseline = List.of(row("anchor", 1, 6, 1, UnitLookup.NANOSECOND.quantity(0), 0),
				row("inside", 0, 6, 1, UnitLookup.NANOSECOND.quantity(0), 2),
				row("outside", 0, 6, 1, UnitLookup.NANOSECOND.quantity(0), 10));
		List<Map<String, Object>> candidate = List.of(row("anchor", 1, 6, 1, UnitLookup.NANOSECOND.quantity(0), 0),
				row("inside", 0, 6, 1, UnitLookup.NANOSECOND.quantity(0), 2),
				row("outside", 0, 6, 1, UnitLookup.NANOSECOND.quantity(0), 10));
		for (Map<String, Object> row : candidate) {
			IQuantity start = (IQuantity) row.get("startTime");
			row.put("startTime", start.add(UnitLookup.SECOND.quantity(10_000)));
		}
		ComparisonTools tools = tools(baseline, candidate);
		Map<String, Object> result = tools.compareRecordings("baseline", "candidate", "terrain", "vehicle", null,
				FILTERS, null, 1.0, 3.0, null, null);
		assertEquals(1L, result.get("baselineEvents"));
		assertEquals(1L, result.get("candidateEvents"));
		assertEquals("both", group(result, "vehicle", "inside").get("presence"));
		Map<String, Object> empty = tools.compareRecordings("baseline", "candidate", "terrain", "vehicle", null,
				List.of(new Predicate("hpAtStart", "eq", "9", null)), null, null, null, null, null);
		assertEquals(0, empty.get("totalGroups"));
		assertEquals(0L, empty.get("baselineEvents"));
		assertNull(empty.get("countChangePercent"));
	}

	@Test
	void threadGroupsAreMatchedByNameAcrossRecordings() throws Exception {
		List<IAttribute<?>> attributes = List.of(JfrAttributes.START_TIME, JfrAttributes.DURATION,
				JfrAttributes.EVENT_THREAD);
		ComparisonTools tools = tools(
				List.of(threadRow("worker", 27, 0), threadRow("worker", 27, 1), threadRow("pool", 28, 2)),
				attributes, List.of(threadRow("worker", 40, 0), threadRow("GC Thread#2", 27, 1)), attributes);
		Map<String, Object> result = tools.compareRecordings("baseline", "candidate", "terrain", "eventThread",
				"duration", null, null, null, null, null, null);
		assertFalse(result.containsKey("error"), EventValues.json(result));
		assertEquals(3, result.get("totalGroups"));
		assertEquals(1L, result.get("bothGroups"));
		assertEquals(1L, result.get("baselineOnlyGroups"));
		assertEquals(1L, result.get("candidateOnlyGroups"));
		for (Map<String, Object> group : groups(result)) {
			String name = (String) map(map(group.get("key")).get("eventThread")).get("name");
			String expected = switch (name) {
			case "worker" -> "both";
			case "pool" -> "baselineOnly";
			case "GC Thread#2" -> "candidateOnly";
			default -> throw new AssertionError(name);
			};
			assertEquals(expected, group.get("presence"), name);
		}
		Map<String, Object> worker = groups(result).stream()
				.filter(group -> "worker".equals(map(map(group.get("key")).get("eventThread")).get("name")))
				.findFirst().orElseThrow();
		assertEquals(-1L, worker.get("countDelta"));
	}

	private static Map<String, Object> threadRow(String thread, long id, long startSeconds) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("eventThread", TestItems.thread(thread, id));
		row.put("duration", UnitLookup.SECOND.quantity(1));
		row.put("startTime", UnitLookup.EPOCH_NS.quantity(EPOCH + startSeconds * 1_000_000_000L));
		return row;
	}

	@Test
	void rejectsMissingIdsInvalidQueriesAndIncompatibleSchemas() throws Exception {
		List<Map<String, Object>> rows = List.of(row("truck", 0, 6, 1, UnitLookup.NANOSECOND.quantity(1), 0));
		ComparisonTools tools = tools(rows, rows);
		assertTrue(tools.compareRecordings(null, "candidate", "terrain", null, null, null, null, null, null, null, null)
				.containsKey("error"));
		assertTrue(tools.compareRecordings("baseline", "candidate", "terrain", "vehcle", null, null, null, null, null,
				null, null).containsKey("error"));
		assertTrue(tools.compareRecordings("baseline", "candidate", "terrain", null, "vehicle", null, null, null, null,
				null, null).containsKey("error"));
		assertTrue(tools.compareRecordings("baseline", "candidate", "terrain", null, null, null, List.of(101.0), null,
				null, null, null).containsKey("error"));
		assertTrue(tools
				.compareRecordings("baseline", "candidate", "terrain", null, null,
						List.of(new Predicate("hpAtStart", "eq", "invalid", null)), null, null, null, null, null)
				.containsKey("error"));
		IAttribute<IQuantity> frequency = Attribute.attr("remainingSeconds", "Remaining", UnitLookup.FREQUENCY);
		List<IAttribute<?>> changed = List.of(JfrAttributes.START_TIME, JfrAttributes.DURATION, VEHICLE, HP, ITERATIONS,
				frequency, CONTEXT);
		ComparisonTools incompatible = tools(rows, rows, changed);
		Map<String, Object> result = incompatible.compareRecordings("baseline", "candidate", "terrain", "vehicle",
				"remainingSeconds", null, null, null, null, null, null);
		assertTrue(result.containsKey("error"));
		assertTrue(result.get("error").toString().contains("incompatible"));
	}

	private static ComparisonTools tools(List<Map<String, Object>> baseline, List<Map<String, Object>> candidate)
			throws Exception {
		return tools(baseline, candidate, ATTRIBUTES);
	}

	private static ComparisonTools tools(
		List<Map<String, Object>> baselineRows, List<Map<String, Object>> candidateRows,
		List<IAttribute<?>> candidateAttributes) throws Exception {
		return tools(baselineRows, ATTRIBUTES, candidateRows, candidateAttributes);
	}

	private static ComparisonTools tools(
		List<Map<String, Object>> baselineRows, List<IAttribute<?>> baselineAttributes,
		List<Map<String, Object>> candidateRows, List<IAttribute<?>> candidateAttributes) throws Exception {
		Recording baseline = TestItems.recording(TestItems.collection("terrain", baselineAttributes, baselineRows));
		Recording candidate = TestItems.recording(TestItems.collection("terrain", candidateAttributes, candidateRows));
		ComparisonTools tools = new ComparisonTools();
		tools.recordings = new RecordingService() {
			@Override
			public Recording get(String recordingId) {
				return switch (recordingId) {
				case "baseline" -> baseline;
				case "candidate" -> candidate;
				default -> throw new IllegalArgumentException("Unknown test recording " + recordingId);
				};
			}
		};
		return tools;
	}

	private static Map<String, Object> row(
		String vehicle, long hp, long iterations, long remaining, IQuantity duration, long startSeconds) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("vehicle", vehicle);
		row.put("hpAtStart", UnitLookup.NUMBER_UNITY.quantity(hp));
		row.put("iterations", UnitLookup.NUMBER_UNITY.quantity(iterations));
		row.put("remainingSeconds", UnitLookup.NUMBER_UNITY.quantity(remaining));
		row.put("duration", duration);
		row.put("startTime", UnitLookup.EPOCH_NS.quantity(EPOCH + startSeconds * 1_000_000_000L));
		row.put("context", UnitLookup.SECOND.quantity(1));
		return row;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> map(Object value) {
		return (Map<String, Object>) value;
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> groups(Map<String, Object> result) {
		assertFalse(result.containsKey("error"), EventValues.json(result));
		return (List<Map<String, Object>>) result.get("groups");
	}

	private static Map<String, Object> group(Map<String, Object> result, String key, Object value) {
		return groups(result).stream().filter(group -> value.equals(map(group.get("key")).get(key))).findFirst()
				.orElseThrow();
	}

	private static void assertDecimal(String expected, Object actual) {
		assertEquals(0, new BigDecimal(expected).compareTo(new BigDecimal(actual.toString())));
	}
}
