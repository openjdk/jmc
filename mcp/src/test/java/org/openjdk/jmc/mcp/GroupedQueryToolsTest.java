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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jdk.jfr.Event;
import jdk.jfr.Name;
import jdk.jfr.Recording;
import jdk.jfr.StackTrace;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openjdk.jmc.common.item.Attribute;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.ItemCollectionToolkit;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.mcp.QuerySupport.Predicate;

class GroupedQueryToolsTest {

	@Name("test.Terrain")
	@StackTrace(false)
	static class Terrain extends Event {
		String vehicle;
		int hpAtStart;
		int iterations;
		double remainingSeconds;
		long exactId;
	}

	@TempDir
	Path directory;

	private GroupedQueryTools tools;
	private RecordingService service;
	private String recordingId;

	@BeforeEach
	void recording() throws Exception {
		Path file = directory.resolve("terrain.jfr");
		try (Recording recording = new Recording()) {
			recording.enable(Terrain.class);
			recording.start();
			terrain("car", 0, 6, 1, 9007199254740992L);
			terrain("car", 0, 6, 3, 9007199254740993L);
			terrain("truck", 0, 6, 7, 9007199254740994L);
			terrain("car", 1, 6, 100, 2);
			terrain("car", 0, 5, 200, 3);
			terrain("car", 0, 6, 0, 4);
			terrain("car", 0, 6, -1, 5);
			recording.stop();
			recording.dump(file);
		}
		service = new RecordingService();
		recordingId = service.load(file.toString()).getId();
		tools = new GroupedQueryTools();
		tools.recordings = service;
	}

	private static void terrain(String vehicle, int hp, int iterations, double seconds, long id) {
		Terrain event = new Terrain();
		event.vehicle = vehicle;
		event.hpAtStart = hp;
		event.iterations = iterations;
		event.remainingSeconds = seconds;
		event.exactId = id;
		event.commit();
	}

	private static List<Predicate> filters() {
		return List.of(new Predicate("hpAtStart", "eq", "0", null), new Predicate("iterations", "eq", "6", null),
				new Predicate("remainingSeconds", "gt", "0", null));
	}

	@Test
	void groupedQuestionUsesAllPredicatesAndInterpolatesPercentiles() {
		Map<String, Object> result = tools.aggregateGroups("test.Terrain", "vehicle", "duration,remainingSeconds",
				filters(), null, List.of(0.0, 50.0, 90.0, 100.0), null, null, null, null, "terrain", recordingId);
		assertFalse(result.containsKey("error"), result.toString());
		assertEquals(3L, result.get("totalEvents"));
		assertEquals(2, result.get("totalGroups"));
		Map<String, Object> car = groups(result).stream()
				.filter(group -> map(group.get("key")).get("vehicle").equals("car")).findFirst().orElseThrow();
		assertEquals(2L, car.get("count"));
		Map<String, Object> stats = map(map(car.get("statistics")).get("remainingSeconds"));
		assertDecimal("4", map(stats.get("sum")).get("exactValue"));
		assertDecimal("2", map(stats.get("median")).get("exactValue"));
		assertDecimal("1", map(stats.get("stddev")).get("exactValue"));
		Map<String, Object> percentiles = map(stats.get("percentiles"));
		assertDecimal("2.8", map(percentiles.get("90")).get("exactValue"));
		assertDecimal("1", map(percentiles.get("0")).get("exactValue"));
		assertDecimal("3", map(percentiles.get("100")).get("exactValue"));
		assertNotNull(map(map(car.get("statistics")).get("duration")).get("sum"));
		assertEquals(3, JfrToolkit.countItems(service.get(recordingId).getStored("terrain")));
	}

	@Test
	void typedPredicatesDistinguishAdjacentLargeIntegers() {
		Map<String, Object> result = tools.queryEvents("test.Terrain",
				List.of(new Predicate("exactId", "eq", "9007199254740993", null)), null, "exactId", null, null, null,
				null, null, recordingId);
		assertEquals(1L, result.get("total"), result.toString());
		Map<String, Object> value = map(map(rows(result).getFirst().get("attributes")).get("exactId"));
		assertEquals("9007199254740993", value.get("exactValue"));
	}

	@Test
	void paginationNeverChangesStatisticsOrStoredSelection() {
		Map<String, Object> result = tools.aggregateGroups("test.Terrain", "vehicle", "remainingSeconds", filters(),
				null, null, null, null, 0, 1, "all-matches", recordingId);
		assertEquals(3L, result.get("totalEvents"));
		assertEquals(2, result.get("totalGroups"));
		assertEquals(1, groups(result).size());
		assertTrue((Boolean) result.get("truncated"));
		assertEquals(1L, result.get("nextOffset"));
		Map<String, Object> page = tools.queryEvents(null, null, "all-matches", "vehicle,remainingSeconds", null, null,
				1, 1, null, recordingId);
		assertEquals(3L, page.get("total"));
		assertEquals(1, rows(page).size());
	}

	@Test
	void rejectsInvalidQueriesWithoutStoringResults() {
		assertError(tools.aggregateGroups("test.Terrain", "bogus", "remainingSeconds", null, null, null, null, null,
				null, null, "invalid", recordingId), "Unknown attribute");
		assertError(tools.aggregateGroups("test.Terrain", "vehicle", "vehicle", null, null, null, null, null, null,
				null, "invalid", recordingId), "not numeric");
		assertError(tools.aggregateGroups("test.Terrain", null, "duration", null, null, List.of(101.0), null, null,
				null, null, "invalid", recordingId), "Percentiles");
		assertError(tools.queryEvents("test.Terrain", List.of(new Predicate("hpAtStart", "gt", "oops", null)), null,
				null, null, null, null, null, "invalid", recordingId), "numeric");
		assertError(
				tools.queryEvents("test.Terrain", filters(), null, null, 2.0, 1.0, null, null, "invalid", recordingId),
				"Time bounds");
		assertFalse(service.get(recordingId).getStoredNames().contains("invalid"));
	}

	@Test
	void emptySelectionProducesExplicitEmptyResult() {
		Map<String, Object> result = tools.aggregateGroups("test.Terrain", "vehicle", "remainingSeconds",
				List.of(new Predicate("hpAtStart", "gt", "1000", null)), null, null, null, null, null, null, "empty",
				recordingId);
		assertEquals(0L, result.get("totalEvents"));
		assertEquals(0, result.get("totalGroups"));
		assertEquals(List.of(), result.get("groups"));
		assertNotNull(service.get(recordingId).getStored("empty"));
	}

	@Test
	void quantityComparisonsAreUnitAwareAndMissingValuesFailComparisons() {
		assertTrue(QuerySupport.matches(UnitLookup.NANOSECOND.quantity(1_001_000),
				new Predicate("duration", "gt", "1", "ms")));
		assertFalse(QuerySupport.matches(UnitLookup.NANOSECOND.quantity(1_001_000),
				new Predicate("duration", "gt", "1", "s")));
		assertFalse(QuerySupport.matches(null, new Predicate("missing", "ne", "0", null)));
		assertTrue(QuerySupport.matches(null, new Predicate("missing", "notExists", null, null)));
		assertEquals(-1, QuerySupport.compareQuantities(UnitLookup.EPOCH_NS.quantity(9007199254740992L),
				UnitLookup.EPOCH_NS.quantity(9007199254740993L)));
		assertTrue(QuerySupport.matches(UnitLookup.NANOSECOND.quantity(9007199254740993L),
				new Predicate("duration", "eq", "9007199.254740993", "s")));
	}

	@Test
	void threadsWithTheSameNameStaySeparateWithinOneRecording() {
		IItemCollection items = TestItems.collection("test.Work", List.of(JfrAttributes.EVENT_THREAD),
				List.of(Map.of("eventThread", TestItems.thread("worker", 1)),
						Map.of("eventThread", TestItems.thread("worker", 2))));
		assertEquals(2, GroupedQueryTools.computeGroups(items, List.of("eventThread"), List.of()).size());
	}

	@Test
	void blankUnitMeansDefaultUnit() {
		assertEquals(null, new Predicate("machineTotal", "gt", "0.5", " ").unit());
		assertTrue(QuerySupport.matches(UnitLookup.PERCENT_UNITY.quantity(0.3),
				new Predicate("machineTotal", "gt", "0.5", "")));
		assertTrue(QuerySupport.matches(UnitLookup.NANOSECOND.quantity(2_000_000_000L),
				new Predicate("duration", "gt", "1", "")));
	}

	@Test
	void groupsEquivalentUnitsAndReportsMissingAndNonfiniteStatistics() throws Exception {
		IAttribute<IQuantity> bucket = Attribute.attr("bucket", "Bucket", UnitLookup.TIMESPAN);
		IAttribute<IQuantity> value = Attribute.attr("value", "Value", UnitLookup.NUMBER);
		var items = TestItems.collection("test.Numeric", List.of(bucket, value), List.of(
				Map.of("bucket", UnitLookup.MILLISECOND.quantity(1), "value", UnitLookup.NUMBER_UNITY.quantity(2)),
				Map.of("bucket", UnitLookup.NANOSECOND.quantity(1_000_000), "value",
						UnitLookup.NUMBER_UNITY.quantity(4)),
				Map.of("bucket", UnitLookup.MILLISECOND.quantity(1), "value",
						UnitLookup.NUMBER_UNITY.quantity(Double.NaN)),
				Map.of("bucket", UnitLookup.MILLISECOND.quantity(1))));
		tools.recordings = TestItems.service(TestItems.recording(items));
		Map<String, Object> result = tools.aggregateGroups("test.Numeric", "bucket", "value", null, null, null, null,
				null, null, null, null, null);
		assertFalse(result.containsKey("error"), result.toString());
		assertEquals(1, result.get("totalGroups"));
		assertEquals(4L, groups(result).getFirst().get("count"));
		Map<String, Object> stats = map(map(groups(result).getFirst().get("statistics")).get("value"));
		assertEquals(2, stats.get("valueCount"));
		assertEquals(1L, stats.get("missingCount"));
		assertEquals(1L, stats.get("nonFiniteCount"));
		assertDecimal("3", map(stats.get("median")).get("exactValue"));
	}

	@Test
	void groupedConsumersMergeNativeStatisticsAcrossTypesAndUnits() throws Exception {
		IAttribute<String> vehicle = Attribute.attr("vehicle", "Vehicle", UnitLookup.PLAIN_TEXT);
		IAttribute<IQuantity> value = Attribute.attr("value", "Value", UnitLookup.TIMESPAN);
		var first = TestItems.collection("test.First", List.of(vehicle, value),
				List.of(Map.of("vehicle", "car", "value", UnitLookup.SECOND.quantity(1))));
		var second = TestItems.collection("test.Second", List.of(vehicle, value),
				List.of(Map.of("vehicle", "car", "value", UnitLookup.MILLISECOND.quantity(3000))));
		var items = ItemCollectionToolkit.merge(() -> Stream.of(first, second));
		tools.recordings = TestItems.service(TestItems.recording(items));
		Map<String, Object> result = tools.aggregateGroups(null, "vehicle", "value", null, null, null, null, null, null,
				null, null, null);
		assertFalse(result.containsKey("error"), result.toString());
		assertEquals(1, result.get("totalGroups"));
		Map<String, Object> group = groups(result).getFirst();
		assertEquals(2L, group.get("count"));
		Map<String, Object> statistics = map(map(group.get("statistics")).get("value"));
		assertEquals(EventValues.encode(items.getAggregate(Aggregators.sum(value))), statistics.get("sum"));
		assertEquals(EventValues.encode(items.getAggregate(Aggregators.avg(value))), statistics.get("avg"));
		assertEquals(EventValues.encode(items.getAggregate(Aggregators.stddevp(value))), statistics.get("stddev"));
		assertDecimal("2", map(statistics.get("median")).get("exactValue"));
	}

	@Test
	void groupingRetainsDistinctSubsecondQuantityKeys() throws Exception {
		IAttribute<IQuantity> value = Attribute.attr("value", "Value", UnitLookup.TIMESPAN);
		Map<String, Object> result = aggregateQuantityKeys(value,
				List.of(UnitLookup.NANOSECOND.quantity(1), UnitLookup.NANOSECOND.quantity(2)));
		assertEquals(2, result.get("totalGroups"));
		assertEquals(List.of("1", "2"), groups(result).stream()
				.map(group -> map(map(group.get("key")).get("value")).get("exactValue").toString()).sorted().toList());
		groups(result).forEach(group -> assertEquals(1L, group.get("count")));
	}

	@Test
	void groupingMergesNumericallyEqualLongAndDoubleQuantities() throws Exception {
		IAttribute<IQuantity> value = Attribute.attr("value", "Value", UnitLookup.NUMBER);
		Map<String, Object> result = aggregateQuantityKeys(value,
				List.of(UnitLookup.NUMBER_UNITY.quantity(1L), UnitLookup.NUMBER_UNITY.quantity(1.0)));
		assertEquals(1, result.get("totalGroups"));
		assertEquals(2L, groups(result).getFirst().get("count"));
	}

	@Test
	void groupingRetainsAdjacentLongKeysAboveDoublePrecision() throws Exception {
		long first = 9_007_199_254_740_992L;
		IAttribute<IQuantity> value = Attribute.attr("value", "Value", UnitLookup.TIMESPAN);
		Map<String, Object> result = aggregateQuantityKeys(value,
				List.of(UnitLookup.NANOSECOND.quantity(first), UnitLookup.NANOSECOND.quantity(first + 1)));
		assertEquals(2, result.get("totalGroups"));
		assertEquals(List.of(Long.toString(first), Long.toString(first + 1)), groups(result).stream()
				.map(group -> map(map(group.get("key")).get("value")).get("exactValue").toString()).sorted().toList());
		groups(result).forEach(group -> assertEquals(1L, group.get("count")));
	}

	private Map<String, Object> aggregateQuantityKeys(IAttribute<IQuantity> value, List<IQuantity> quantities)
			throws Exception {
		List<Map<String, Object>> rows = quantities.stream().map(quantity -> Map.<String, Object> of("value", quantity))
				.toList();
		tools.recordings = TestItems
				.service(TestItems.recording(TestItems.collection("test.Keys", List.of(value), rows)));
		Map<String, Object> result = tools.aggregateGroups(null, "value", "value", null, null, null, null, null, null,
				null, null, null);
		assertFalse(result.containsKey("error"), result.toString());
		return result;
	}

	@Test
	void validatesOperandsEvenWhenStoredSelectionIsEmpty() {
		tools.queryEvents("test.Terrain", List.of(new Predicate("hpAtStart", "gt", "1000", null)), null, null, null,
				null, null, null, "empty", recordingId);
		assertError(tools.queryEvents("test.Terrain", List.of(new Predicate("duration", "gt", "1", "bogus")), "empty",
				null, null, null, null, null, null, recordingId), "Unknown unit");
		assertError(tools.queryEvents("test.Terrain", List.of(new Predicate("vehicle", "gt", "car", null)), "empty",
				null, null, null, null, null, null, recordingId), "numeric");
	}

	@Test
	void rejectsMixingRawNumbersWithUnitBearingValuesAcrossTypes() throws Exception {
		IAttribute<IQuantity> measured = Attribute.attr("value", "Value", UnitLookup.TIMESPAN);
		IAttribute<Number> raw = Attribute.attr("value", "Value", UnitLookup.RAW_NUMBER);
		var first = TestItems.collection("test.Measured", List.of(measured),
				List.of(Map.of("value", UnitLookup.MILLISECOND.quantity(5))));
		var second = TestItems.collection("test.Raw", List.of(raw), List.of(Map.of("value", 10)));
		tools.recordings = TestItems
				.service(TestItems.recording(ItemCollectionToolkit.merge(() -> Stream.of(first, second))));
		assertError(tools.aggregateGroups(null, null, "value", null, null, null, null, null, null, null, null, null),
				"mixes raw numbers");
	}

	private static void assertDecimal(String expected, Object actual) {
		assertEquals(0, new BigDecimal(expected).compareTo(new BigDecimal(actual.toString())));
	}

	private static void assertError(Map<String, Object> result, String message) {
		assertTrue(result.containsKey("error"), result.toString());
		assertTrue(result.get("error").toString().contains(message), result.toString());
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> map(Object value) {
		return (Map<String, Object>) value;
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> groups(Map<String, Object> result) {
		return (List<Map<String, Object>>) result.get("groups");
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> rows(Map<String, Object> result) {
		return (List<Map<String, Object>>) result.get("rows");
	}
}
