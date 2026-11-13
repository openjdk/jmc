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

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;

import jdk.jfr.Event;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openjdk.jmc.common.item.Attribute;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;

class HistogramToolsTest {
	private static final String TYPE = "test.Work";
	private static final IAttribute<Long> SIZE = Attribute.attr("size", "Size", UnitLookup.RAW_LONG);
	private static final IAttribute<IQuantity> ALLOCATED = Attribute.attr("allocated", "Allocated", UnitLookup.MEMORY);
	private static final IAttribute<String> LABEL = Attribute.attr("label", "Label", UnitLookup.PLAIN_TEXT);

	@Name(TYPE)
	@StackTrace(false)
	static class Work extends Event {
	}

	@TempDir
	Path directory;

	@Test
	void logBucketsUseMillisecondsAndKeepInteriorGaps() throws Exception {
		Map<String, Object> result = histogram(durations(UnitLookup.MILLISECOND.quantity(0.5),
				UnitLookup.NANOSECOND.quantity(1_500_000), UnitLookup.MILLISECOND.quantity(1.5),
				UnitLookup.MICROSECOND.quantity(3000), UnitLookup.MILLISECOND.quantity(120)), "duration", null, null,
				null);
		assertEquals("ms", result.get("unit"), result.toString());
		assertEquals(5, result.get("valueCount"));
		assertBuckets(result, new double[] {0.5, 1, 2, 5, 10, 20, 50, 100, 200}, new long[] {1, 2, 1, 0, 0, 0, 0, 1});
		List<Map<String, Object>> buckets = buckets(result);
		assertEquals(100.0, (double) buckets.get(buckets.size() - 1).get("cumulativePercent"), 1e-9);
		assertEquals(40.0, (double) buckets.get(1).get("percent"), 1e-9);
	}

	@Test
	void valuesOnEdgesBelongToTheUpperBucket() throws Exception {
		Map<String, Object> result = histogram(durations(UnitLookup.MILLISECOND.quantity(1),
				UnitLookup.NANOSECOND.quantity(2_000_000), UnitLookup.MICROSECOND.quantity(5000)), "duration", null,
				null, null);
		assertBuckets(result, new double[] {1, 2, 5, 10}, new long[] {1, 1, 1});
	}

	@Test
	void percentilesMatchAggregateGroupsAndCountTheTail() throws Exception {
		IQuantity[] values = LongStream.rangeClosed(1, 10).mapToObj(UnitLookup.MILLISECOND::quantity)
				.toArray(IQuantity[]::new);
		RecordingService service = TestItems.service(TestItems.recording(durations(values)));
		HistogramTools histograms = new HistogramTools();
		histograms.recordings = service;
		GroupedQueryTools grouped = new GroupedQueryTools();
		grouped.recordings = service;
		List<Double> requested = List.of(50.0, 90.0, 99.0, 100.0);
		Map<String, Object> result = histograms.getHistogram(TYPE, null, null, null, null, null, null, null, null,
				requested, null, null, null, null);
		Map<String, Object> statistics = map(map(map(list(grouped
				.aggregateGroups(TYPE, null, "duration", null, null, requested, null, null, null, null, null, null)
				.get("groups")).get(0).get("statistics")).get("duration")).get("percentiles"));
		List<Map<String, Object>> percentiles = list(result.get("percentiles"));
		long[] expectedTail = {5, 1, 1, 1};
		for (int i = 0; i < requested.size(); i++) {
			String key = TestItems.plain(requested.get(i)).replaceAll("\\.0$", "");
			double expected = ((Number) map(statistics.get(key)).get("value")).doubleValue();
			assertEquals(expected, ((Number) percentiles.get(i).get("value")).doubleValue(), 1e-9, key);
			assertEquals(expectedTail[i], ((Number) percentiles.get(i).get("countAtOrAbove")).longValue(), key);
		}
	}

	@Test
	void linearBucketsIncludeTheMaximumInTheLastBucket() throws Exception {
		List<Map<String, Object>> rows = new ArrayList<>();
		for (long i = 0; i <= 10; i++) {
			rows.add(Map.of("size", i));
		}
		IItemCollection items = TestItems.collection(TYPE, List.of(SIZE), rows);
		Map<String, Object> result = histogram(items, "size", "linear", 5, null);
		assertEquals("", result.get("unit"));
		assertBuckets(result, new double[] {0, 2, 4, 6, 8, 10}, new long[] {2, 2, 2, 2, 3});
	}

	@Test
	void zeroAndNegativeValuesGetALeadingBucket() throws Exception {
		List<Map<String, Object>> rows = new ArrayList<>();
		for (long value : new long[] {-3, 0, 0, 4, 7}) {
			rows.add(Map.of("size", value));
		}
		Map<String, Object> result = histogram(TestItems.collection(TYPE, List.of(SIZE), rows), "size", null, null,
				null);
		assertBuckets(result, new double[] {-3, 2, 5, 10}, new long[] {3, 1, 1});
	}

	@Test
	void memoryDefaultsToBytesAndUnitsCanBeChosen() throws Exception {
		IItemCollection items = TestItems.collection(TYPE, List.of(ALLOCATED),
				List.of(Map.of("allocated", UnitLookup.MEMORY.getUnit("KiB").quantity(1)),
						Map.of("allocated", UnitLookup.BYTE.quantity(3000))));
		Map<String, Object> bytes = histogram(items, "allocated", null, null, null);
		assertEquals("B", bytes.get("unit"));
		assertEquals(1024.0, bytes.get("min"));
		assertBuckets(bytes, new double[] {1000, 2000, 5000}, new long[] {1, 1});
		Map<String, Object> kibibytes = histogram(items, "allocated", null, null, "KiB");
		assertEquals("KiB", kibibytes.get("unit"));
		assertEquals(1.0, kibibytes.get("min"));
		Map<String, Object> micros = histogram(durations(UnitLookup.MILLISECOND.quantity(2)), "duration", null, null,
				"us");
		assertEquals(2000.0, micros.get("max"));
	}

	@Test
	void missingValuesAreCountedAndSelectionsCanBeStored() throws Exception {
		IItemCollection items = TestItems.collection(TYPE, List.of(JfrAttributes.DURATION, LABEL),
				List.of(Map.of("duration", UnitLookup.MILLISECOND.quantity(4), "label", "a"), Map.of("label", "b")));
		RecordingService.Recording recording = TestItems.recording(items);
		HistogramTools tools = new HistogramTools();
		tools.recordings = TestItems.service(recording);
		Map<String, Object> result = tools.getHistogram(TYPE, null, null, null, null, null, null, null, null, null,
				"work", null, null, null);
		assertEquals(1, result.get("valueCount"));
		assertEquals(1L, result.get("missingCount"));
		assertEquals("work", result.get("storedAs"));
		assertEquals(2, JfrToolkit.countItems(recording.getSelections().getItems("work").orElseThrow()));
	}

	@Test
	void percentileRangesComposeWithOtherNamedSelections() throws Exception {
		List<Map<String, Object>> rows = new ArrayList<>();
		for (long i = 1; i <= 10; i++) {
			rows.add(Map.of("duration", UnitLookup.MILLISECOND.quantity(i), "label", "monitor"));
			rows.add(Map.of("duration", UnitLookup.MILLISECOND.quantity(100 + i), "label", "other"));
		}
		RecordingService.Recording recording = TestItems
				.recording(TestItems.collection(TYPE, List.of(JfrAttributes.DURATION, LABEL), rows));
		RecordingService service = TestItems.service(recording);
		HistogramTools histograms = new HistogramTools();
		histograms.recordings = service;
		GroupedQueryTools grouped = new GroupedQueryTools();
		grouped.recordings = service;
		grouped.queryEvents(TYPE, List.of(new QuerySupport.Predicate("label", "eq", "monitor", null)), null, null, null,
				null, null, 1, "blocking", null);

		Map<String, Object> tail = histograms.getHistogram(null, null, null, "blocking", null, null, null, null, null,
				List.of(80.0), "slowBlocking", 80.0, null, null);
		assertEquals(10, tail.get("valueCount"), tail.toString());
		assertEquals(2, tail.get("storedEventCount"), tail.toString());
		assertEquals(list(tail.get("percentiles")).get(0).get("countAtOrAbove"), tail.get("storedEventCount"));
		assertEquals(
				Map.of("fromPercentile", 80.0, "toPercentile", 100.0, "from", 8.2, "to", 10.0, "toInclusive", true),
				tail.get("storedRange"));
		Map<String, Object> duration = map(map(list(grouped
				.aggregateGroups(null, null, "duration", null, "slowBlocking", null, null, null, null, null, null, null)
				.get("groups")).get(0).get("statistics")).get("duration"));
		assertEquals(2, duration.get("valueCount"), duration.toString());
		assertEquals(0.0095, ((Number) map(duration.get("avg")).get("canonicalValue")).doubleValue(), 1e-12,
				duration.toString());

		Map<String, Object> lowerHalf = histograms.getHistogram(null, null, null, "slowBlocking", null, null, null,
				null, "us", null, "lowerHalf", null, 50.0, null);
		assertEquals(
				Map.of("fromPercentile", 0.0, "toPercentile", 50.0, "from", 9000.0, "to", 9500.0, "toInclusive", false),
				lowerHalf.get("storedRange"));
		assertEquals(1, JfrToolkit.countItems(recording.getSelections().getItems("lowerHalf").orElseThrow()));
		assertEquals(2, JfrToolkit.countItems(recording.getSelections().getItems("slowBlocking").orElseThrow()));
	}

	@Test
	void percentileRangesAreValidated() throws Exception {
		HistogramTools tools = new HistogramTools();
		tools.recordings = TestItems.service(TestItems.recording(durations(UnitLookup.MILLISECOND.quantity(1))));
		assertError(
				tools.getHistogram(TYPE, null, null, null, null, null, null, null, null, null, null, 90.0, null, null),
				"require storeAs");
		assertError(
				tools.getHistogram(TYPE, null, null, null, null, null, null, null, null, null, "x", 90.0, 50.0, null),
				"must not exceed");
		assertError(
				tools.getHistogram(TYPE, null, null, null, null, null, null, null, null, null, "x", 101.0, null, null),
				"from 0 to 100");
	}

	@Test
	void invalidRequestsReturnErrors() throws Exception {
		IItemCollection items = TestItems.collection(TYPE,
				List.of(JfrAttributes.DURATION, JfrAttributes.START_TIME, SIZE, LABEL),
				List.of(Map.of("duration", UnitLookup.MILLISECOND.quantity(4), "startTime",
						UnitLookup.EPOCH_NS.quantity(1_000_000_000L), "size", 1L, "label", "a")));
		assertError(histogram(items, "duration", "cubic", null, null), "Unknown bucketing");
		assertError(histogram(items, "duration", null, 10, null), "linear bucketing");
		assertError(histogram(items, "duration", "linear", 0, null), "at least 1");
		assertError(histogram(items, "duration", null, null, "furlongs"), "Unknown unit");
		assertError(histogram(items, "size", null, null, "ms"), "raw number");
		assertError(histogram(items, "label", null, null, null), "not numeric");
		assertError(histogram(items, "startTime", null, null, null), "getTimeSeries");
	}

	@Test
	void convertedValuesAreFreeOfConversionNoise() throws Exception {
		Map<String, Object> result = histogram(durations(UnitLookup.NANOSECOND.quantity(10_011_115)), "duration", null,
				null, null);
		assertEquals(10.011115, result.get("min"));
		assertEquals(10.011115, list(result.get("percentiles")).get(0).get("value"));
	}

	@Test
	void identicalValuesFillOneInclusiveBucket() throws Exception {
		Map<String, Object> result = histogram(
				durations(UnitLookup.NANOSECOND.quantity(0), UnitLookup.NANOSECOND.quantity(0)), "duration", null, null,
				null);
		assertBuckets(result, new double[] {0, 0}, new long[] {2});
		Map<String, Object> linear = histogram(durations(UnitLookup.MILLISECOND.quantity(3)), "duration", "linear", 4,
				null);
		assertBuckets(linear, new double[] {3, 3}, new long[] {1});
	}

	@Test
	void emptySelectionsHaveNoBuckets() throws Exception {
		IItemCollection items = TestItems.collection(TYPE, List.of(JfrAttributes.DURATION, LABEL),
				List.of(Map.of("label", "a")));
		Map<String, Object> result = histogram(items, "duration", null, null, null);
		assertEquals(0, result.get("valueCount"));
		assertTrue(buckets(result).isEmpty());
		assertEquals(0, list(result.get("percentiles")).get(0).get("countAtOrAbove"));
	}

	@Test
	void durationHistogramsReportTheRecordingThreshold() throws Exception {
		Path file = directory.resolve("work.jfr");
		try (jdk.jfr.Recording recording = new jdk.jfr.Recording()) {
			recording.enable("jdk.ActiveSetting");
			recording.enable(Work.class).withThreshold(Duration.ofMillis(1));
			recording.start();
			Work work = new Work();
			work.begin();
			Thread.sleep(5);
			work.commit();
			recording.stop();
			recording.dump(file);
		}
		RecordingService service = new RecordingService();
		String recordingId = service.load(file.toString()).getId();
		HistogramTools tools = new HistogramTools();
		tools.recordings = service;
		Map<String, Object> result = tools.getHistogram(TYPE, null, null, null, null, null, null, null, null, null,
				null, null, null, recordingId);
		assertEquals(1, result.get("valueCount"), result.toString());
		assertEquals(Map.of(TYPE, 1.0), result.get("recordingThresholds"));
	}

	private static IItemCollection durations(IQuantity ... values) {
		List<Map<String, Object>> rows = new ArrayList<>();
		for (IQuantity value : values) {
			rows.add(Map.of("duration", value));
		}
		return TestItems.collection(TYPE, List.of(JfrAttributes.DURATION), rows);
	}

	private static Map<String, Object> histogram(
		IItemCollection items, String attribute, String bucketing, Integer buckets, String unit) throws Exception {
		HistogramTools tools = new HistogramTools();
		tools.recordings = TestItems.service(TestItems.recording(items));
		return tools.getHistogram(TYPE, attribute, null, null, null, null, bucketing, buckets, unit, null, null, null,
				null, null);
	}

	private static void assertBuckets(Map<String, Object> result, double[] edges, long[] counts) {
		List<Map<String, Object>> buckets = buckets(result);
		assertEquals(counts.length, buckets.size(), result.toString());
		for (int i = 0; i < counts.length; i++) {
			assertEquals(edges[i], (double) buckets.get(i).get("from"), 1e-12, "from " + i);
			assertEquals(edges[i + 1], (double) buckets.get(i).get("to"), 1e-12, "to " + i);
			assertEquals(counts[i], buckets.get(i).get("count"), "count " + i);
		}
	}

	private static void assertError(Map<String, Object> result, String fragment) {
		assertTrue(result.containsKey("error"), result.toString());
		assertTrue(String.valueOf(result.get("error")).contains(fragment), result.toString());
		assertFalse(result.containsKey("buckets"));
	}

	private static List<Map<String, Object>> buckets(Map<String, Object> result) {
		return list(result.get("buckets"));
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> list(Object value) {
		return (List<Map<String, Object>>) value;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> map(Object value) {
		return (Map<String, Object>) value;
	}
}
