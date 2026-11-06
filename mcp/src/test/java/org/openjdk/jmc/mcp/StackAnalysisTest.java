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

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.item.Attribute;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.ItemCollectionToolkit;
import org.openjdk.jmc.common.unit.BinaryPrefix;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.MCStackTrace;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.mcp.RecordingService.Recording;

class StackAnalysisTest {
	private static final String TYPE = "test.Sample";
	private static final IAttribute<IQuantity> WEIGHT = Attribute.attr("weight", "Weight", UnitLookup.MEMORY);
	private AnalysisTools tools;
	private Recording recording;

	@BeforeEach
	void setUp() throws Exception {
		IItemCollection items = TestItems.collection(TYPE,
				List.of(JfrAttributes.EVENT_STACKTRACE, JfrAttributes.EVENT_THREAD, JfrAttributes.DURATION, WEIGHT),
				List.of(sample("simulation", 11, "terrain", UnitLookup.MILLISECOND.quantity(1), 1024),
						sample("simulation", 11, "terrain", UnitLookup.NANOSECOND.quantity(3_000_000), 3072),
						sample("simulation", 11, "physics", UnitLookup.SECOND.quantity(0.006), 6144),
						sample("render", 12, "render", UnitLookup.MILLISECOND.quantity(10), 10240),
						Map.of("eventThread", TestItems.thread("simulation", 11), "duration",
								UnitLookup.SECOND.quantity(1), "weight", UnitLookup.BYTE.quantity(2048))));
		recording = TestItems.recording(items);
		tools = new AnalysisTools();
		tools.recordings = TestItems.service(recording);
	}

	@Test
	void threadAndFrameScopesReturnSelfInclusiveAndSelectedDenominator() {
		Map<String, Object> result = analyze(null, "simulation", "example.Simulation.terrain", null, "count");
		assertFalse(result.containsKey("error"), result.toString());
		assertEquals(2L, result.get("selectedEventCount"));
		assertEquals(2.0, number(result, "totalWeight"));
		assertEquals(2.0, number(denominator(result), "weight"));
		Map<String, Object> terrain = node(result, "example.Simulation.terrain()");
		assertEquals(2.0, number(terrain, "selfWeight"));
		assertEquals(2.0, number(terrain, "inclusiveWeight"));
		assertEquals(100.0, number(terrain, "selfPercent"));
		Map<String, Object> tick = node(result, "example.Simulation.tick()");
		assertEquals(0.0, number(tick, "selfWeight"));
		assertEquals(2.0, number(tick, "inclusiveWeight"));
		assertEquals(100.0, number(tick, "inclusivePercent"));
		assertEquals(List.of("example.Simulation.tick()"), terrain.get("callChain"));
	}

	@Test
	void sourceDenominatorRetainsOtherThreadsAndReportsMissingStacks() {
		Map<String, Object> result = analyze(null, "11", "terrain", "source", "count");
		assertEquals(2L, result.get("selectedEventCount"));
		assertEquals(5L, result.get("sourceEventCount"));
		assertEquals(5L, denominator(result).get("eventCount"));
		assertEquals(4L, denominator(result).get("stackEventCount"));
		assertEquals(4.0, number(denominator(result), "weight"));
		assertEquals(50.0, number(node(result, "example.Simulation.terrain()"), "selfPercent"));
		Map<String, Object> unfiltered = analyze(null, null, null, null, "count");
		assertEquals(1L, unfiltered.get("missingStackEventCount"));
		assertEquals(4L, unfiltered.get("stackEventCount"));
	}

	@Test
	void storedSetIsTheSourceForBothSelectionAndDenominator() {
		recording.getSelections().put("simulation",
				ItemCollectionToolkit.build(JfrToolkit.toItemList(recording.getItems()).stream().limit(3)));
		Map<String, Object> result = analyze("simulation", null, "terrain", "source", "count");
		assertEquals(3L, result.get("sourceEventCount"));
		assertEquals(3.0, number(denominator(result), "weight"));
		assertEquals(2.0 / 3 * 100, number(node(result, "example.Simulation.terrain()"), "selfPercent"), 1e-12);
		assertEquals("simulation", result.get("source"));
	}

	@Test
	void durationWeightsNormalizeMixedUnitsWithoutRounding() {
		Map<String, Object> result = analyze(null, "simulation", null, null, "duration");
		assertEquals("s", result.get("weightUnit"));
		assertEquals(0.010, number(result, "totalWeight"), 1e-15);
		assertEquals(1L, result.get("missingStackEventCount"));
		assertEquals(0.004, number(node(result, "example.Simulation.terrain()"), "selfWeight"), 1e-15);
		assertEquals(0.010, number(node(result, "example.Simulation.tick()"), "inclusiveWeight"), 1e-15);
	}

	@Test
	void memoryWeightsUseKibibytesAndSameDenominatorUnit() {
		Map<String, Object> result = analyze(null, "simulation", "terrain", "source", "weight");
		assertEquals("KiB", result.get("weightUnit"));
		assertEquals(4.0, number(result, "totalWeight"));
		assertEquals(20.0, number(denominator(result), "weight"));
		assertEquals(20.0, number(node(result, "example.Simulation.terrain()"), "selfPercent"));
	}

	@Test
	void stackConsumerMergesRepeatedTypeGroupsAndRetainsCallPaths() throws Exception {
		List<IItem> items = JfrToolkit.toItemList(recording.getItems());
		IItemCollection first = ItemCollectionToolkit.build(items.stream().limit(1));
		IItemCollection remaining = ItemCollectionToolkit.build(items.stream().skip(1));
		tools.recordings = TestItems
				.service(TestItems.recording(ItemCollectionToolkit.merge(() -> Stream.of(first, remaining))));
		Map<String, Object> result = analyze(null, "simulation", "terrain", null, "duration");
		assertFalse(result.containsKey("error"), result.toString());
		assertEquals(2L, result.get("selectedEventCount"));
		assertEquals(2, result.get("totalNodeCount"));
		assertEquals(0.004, number(result, "totalWeight"));
		Map<String, Object> terrain = node(result, "example.Simulation.terrain()");
		assertEquals(0.004, number(terrain, "selfWeight"));
		assertEquals(0.004, number(node(result, "example.Simulation.tick()"), "inclusiveWeight"));
		assertEquals(List.of("example.Simulation.tick()"), terrain.get("callChain"));
	}

	@Test
	void rawLongAttributesCanWeightStacksWithoutQuantityWrapping() throws Exception {
		IAttribute<Long> cost = Attribute.attr("cost", "Cost", UnitLookup.RAW_LONG);
		IItemCollection items = TestItems.collection(TYPE, List.of(JfrAttributes.EVENT_STACKTRACE, cost), List.of(
				Map.of("stackTrace", TestItems.stack("example.Work.first", "example.Work.parent"), "cost", 3L),
				Map.of("stackTrace", TestItems.stack("example.Work.second", "example.Work.parent"), "cost", 9L)));
		tools.recordings = TestItems.service(TestItems.recording(items));
		Map<String, Object> result = analyze(null, null, null, null, "cost");
		assertFalse(result.containsKey("error"), result.toString());
		assertEquals(12.0, number(result, "totalWeight"));
		assertEquals(3.0, number(node(result, "example.Work.first()"), "selfWeight"));
		assertEquals(25.0, number(node(result, "example.Work.first()"), "selfPercent"));
		assertEquals(12.0, number(node(result, "example.Work.parent()"), "inclusiveWeight"));
		assertEquals(100.0, number(node(result, "example.Work.parent()"), "inclusivePercent"));
	}

	@Test
	void largeRawWeightsAccumulateExactlyAndUseExactSourceDenominator() throws Exception {
		long large = 9_007_199_254_740_993L;
		IAttribute<Long> cost = Attribute.attr("cost", "Cost", UnitLookup.RAW_LONG);
		IItemCollection items = TestItems.collection(TYPE, List.of(JfrAttributes.EVENT_STACKTRACE, cost), List.of(
				Map.of("stackTrace", TestItems.stack("example.Work.main", "example.Work.parent"), "cost", large),
				Map.of("stackTrace", TestItems.stack("example.Work.main", "example.Work.parent"), "cost", 1L),
				Map.of("stackTrace", TestItems.stack("example.Work.other", "example.Work.parent"), "cost", 1L)));
		tools.recordings = TestItems.service(TestItems.recording(items));
		Map<String, Object> result = analyze(null, null, "example.Work.main", "source", "cost");
		assertFalse(result.containsKey("error"), result.toString());
		assertEquals("9007199254740994", result.get("totalWeightExactValue"));
		assertEquals("9007199254740995", denominator(result).get("weightExactValue"));
		Map<String, Object> main = node(result, "example.Work.main()");
		assertEquals("9007199254740994", main.get("selfWeightExactValue"));
		assertEquals("9007199254740994", main.get("inclusiveWeightExactValue"));
		assertEquals("9007199254740994", node(result, "example.Work.parent()").get("inclusiveWeightExactValue"));
		assertEquals(large + 1, result.get("totalWeight"));
		assertEquals(large + 2, denominator(result).get("weight"));
		double percentage = UnitLookup.NUMBER_UNITY.quantity(large + 1)
				.ratioTo(UnitLookup.NUMBER_UNITY.quantity(large + 2)) * 100;
		assertEquals(percentage, number(main, "selfPercent"));
	}

	@Test
	void largeQuantityWeightsUseNativeUnitConversionAfterAggregation() throws Exception {
		long large = 9_007_199_254_740_993L;
		IItemCollection items = TestItems.collection(TYPE,
				List.of(JfrAttributes.EVENT_STACKTRACE, JfrAttributes.DURATION, WEIGHT),
				List.of(Map.of("stackTrace", TestItems.stack("example.Work.run"), "duration",
						UnitLookup.NANOSECOND.quantity(large), "weight", UnitLookup.BYTE.quantity(large)),
						Map.of("stackTrace", TestItems.stack("example.Work.run"), "duration",
								UnitLookup.NANOSECOND.quantity(1), "weight", UnitLookup.BYTE.quantity(1))));
		tools.recordings = TestItems.service(TestItems.recording(items));
		Map<String, Object> duration = analyze(null, null, null, null, "duration");
		assertEquals("s", duration.get("weightUnit"));
		Number durationNumber = UnitLookup.NANOSECOND.quantity(large).add(UnitLookup.NANOSECOND.quantity(1))
				.numberValueIn(UnitLookup.SECOND);
		assertEquals(TestItems.plain(durationNumber), duration.get("totalWeightExactValue"));
		assertEquals(TestItems.plain(durationNumber), denominator(duration).get("weightExactValue"));
		assertEquals(TestItems.plain(durationNumber), node(duration, "example.Work.run()").get("selfWeightExactValue"));
		assertEquals(durationNumber, duration.get("totalWeight"));
		Map<String, Object> memory = analyze(null, null, null, null, "weight");
		assertEquals("KiB", memory.get("weightUnit"));
		Number memoryNumber = UnitLookup.BYTE.quantity(large).add(UnitLookup.BYTE.quantity(1))
				.numberValueIn(UnitLookup.MEMORY.getUnit(BinaryPrefix.KIBI));
		assertEquals(TestItems.plain(memoryNumber), memory.get("totalWeightExactValue"));
		assertEquals(TestItems.plain(memoryNumber), denominator(memory).get("weightExactValue"));
		assertEquals(TestItems.plain(memoryNumber), node(memory, "example.Work.run()").get("selfWeightExactValue"));
		assertEquals(memoryNumber, memory.get("totalWeight"));
	}

	@Test
	void emptySelectionStillReportsItsDenominator() {
		Map<String, Object> result = analyze(null, "absent", null, "source", "count");
		assertEquals(0L, result.get("selectedEventCount"));
		assertEquals(0.0, number(result, "totalWeight"));
		assertEquals(4.0, number(denominator(result), "weight"));
		assertEquals(List.of(), result.get("nodes"));
		assertEquals(false, result.get("truncated"));
	}

	@Test
	void missingAndZeroWeightsAreExplicitAndDoNotContribute() throws Exception {
		IItemCollection items = TestItems.collection(TYPE, List.of(JfrAttributes.EVENT_STACKTRACE, WEIGHT), List.of(
				Map.of("stackTrace", TestItems.stack("example.Work.run")),
				Map.of("stackTrace", TestItems.stack("example.Work.run"), "weight", UnitLookup.BYTE.quantity(0)),
				Map.of("stackTrace", TestItems.stack("example.Work.run"), "weight", UnitLookup.BYTE.quantity(512))));
		tools.recordings = TestItems.service(TestItems.recording(items));
		Map<String, Object> result = analyze(null, null, null, null, "weight");
		assertEquals(3L, result.get("stackEventCount"));
		assertEquals(1L, result.get("missingWeightEventCount"));
		assertEquals(1L, result.get("zeroWeightEventCount"));
		assertEquals(0.5, number(result, "totalWeight"));
		assertEquals(100.0, number(node(result, "example.Work.run()"), "selfPercent"));
	}

	@Test
	void stackDiagnosticsMergeAcrossTypesWithMissingAccessors() throws Exception {
		IItemCollection weighted = TestItems.collection(TYPE, List.of(JfrAttributes.EVENT_STACKTRACE, WEIGHT), List.of(
				Map.of("stackTrace", TestItems.stack("example.Work.run"), "weight", UnitLookup.BYTE.quantity(1024)),
				Map.of("stackTrace", TestItems.stack("example.Work.zero"), "weight", UnitLookup.BYTE.quantity(0))));
		IMCStackTrace original = TestItems.stack("example.Work.missing");
		IMCStackTrace truncated = new MCStackTrace(new java.util.ArrayList<>(original.getFrames()),
				IMCStackTrace.TruncationState.TRUNCATED);
		IItemCollection missingWeight = TestItems.collection(TYPE, List.of(JfrAttributes.EVENT_STACKTRACE),
				List.of(Map.of("stackTrace", truncated)));
		IItemCollection missingStack = TestItems.collection(TYPE, List.of(WEIGHT),
				List.of(Map.of("weight", UnitLookup.BYTE.quantity(-1))));
		tools.recordings = TestItems.service(TestItems
				.recording(ItemCollectionToolkit.merge(() -> Stream.of(weighted, missingWeight, missingStack))));
		Map<String, Object> result = analyze(null, null, null, "source", "weight");
		assertFalse(result.containsKey("error"), result.toString());
		assertEquals(4L, result.get("selectedEventCount"));
		assertEquals(3L, result.get("stackEventCount"));
		assertEquals(1L, result.get("missingStackEventCount"));
		assertEquals(1L, result.get("truncatedStackEventCount"));
		assertEquals(1L, result.get("missingWeightEventCount"));
		assertEquals(1L, result.get("zeroWeightEventCount"));
		assertEquals(1.0, number(result, "totalWeight"));
		assertEquals(4L, denominator(result).get("eventCount"));
		assertEquals(3L, denominator(result).get("stackEventCount"));
		assertEquals(1.0, number(denominator(result), "weight"));
		assertEquals(1, result.get("totalNodeCount"));
		assertEquals(1.0, number(node(result, "example.Work.run()"), "selfWeight"));
	}

	@Test
	void recursiveFrameInclusiveValuesRemainSpecificToEachCallPath() throws Exception {
		IItemCollection items = TestItems.collection(TYPE, List.of(JfrAttributes.EVENT_STACKTRACE),
				List.of(Map.of("stackTrace", TestItems.stack("example.Work.run", "example.Work.run"))));
		tools.recordings = TestItems.service(TestItems.recording(items));
		Map<String, Object> result = analyze(null, null, null, null, "count");
		assertEquals(1.0, number(result, "totalWeight"));
		assertEquals(2, result.get("totalNodeCount"));
		assertEquals(1.0, rows(result).stream().mapToDouble(row -> number(row, "selfWeight")).sum());
		assertTrue(rows(result).stream().allMatch(row -> number(row, "inclusivePercent") == 100));
	}

	@Test
	void truncatedStackPreservesEveryObservedFrameAndAddsUnknownCaller() throws Exception {
		IMCStackTrace original = TestItems.stack("example.Work.leaf", "example.Work.caller");
		IMCStackTrace truncated = new MCStackTrace(new java.util.ArrayList<>(original.getFrames()),
				IMCStackTrace.TruncationState.TRUNCATED);
		IItemCollection items = TestItems.collection(TYPE, List.of(JfrAttributes.EVENT_STACKTRACE),
				List.of(Map.of("stackTrace", truncated)));
		tools.recordings = TestItems.service(TestItems.recording(items));
		Map<String, Object> result = analyze(null, null, null, null, "count");
		assertEquals(1L, result.get("truncatedStackEventCount"));
		assertEquals(3, result.get("totalNodeCount"));
		assertEquals(1.0, number(node(result, "example.Work.leaf()"), "selfWeight"));
		assertEquals(1.0, number(node(result, "example.Work.caller()"), "inclusiveWeight"));
		assertEquals(List.of("<truncated>", "example.Work.caller()"),
				node(result, "example.Work.leaf()").get("callChain"));
	}

	@Test
	void truncatedRowsExposeCompleteTotalAndCoverage() {
		Map<String, Object> result = tools.getStackStatistics(TYPE, "count", null, null, 1, null, null, null, null,
				null);
		assertEquals(true, result.get("truncated"));
		assertEquals(1, result.get("returnedNodeCount"));
		assertEquals(4.0, number(result, "totalWeight"));
		assertTrue((Integer) result.get("totalNodeCount") > 1);
	}

	@Test
	void entirelyMissingStacksHaveZeroDenominatorAndExplicitCoverage() throws Exception {
		IItemCollection items = TestItems.collection(TYPE, List.of(JfrAttributes.EVENT_THREAD),
				List.of(Map.of("eventThread", TestItems.thread("simulation", 11))));
		tools.recordings = TestItems.service(TestItems.recording(items));
		Map<String, Object> result = analyze(null, null, null, null, "count");
		assertEquals(1L, result.get("selectedEventCount"));
		assertEquals(1L, result.get("missingStackEventCount"));
		assertEquals(0L, result.get("stackEventCount"));
		assertEquals(0.0, number(denominator(result), "weight"));
		assertEquals(List.of(), result.get("nodes"));
	}

	@Test
	void invalidWeightsDoNotProduceMisleadingPercentages() throws Exception {
		for (double weight : new double[] {-1, Double.NaN, Double.POSITIVE_INFINITY}) {
			IItemCollection items = TestItems.collection(TYPE, List.of(JfrAttributes.EVENT_STACKTRACE, WEIGHT),
					List.of(Map.of("stackTrace", TestItems.stack("example.Work.run"), "weight",
							UnitLookup.BYTE.quantity(weight))));
			tools.recordings = TestItems.service(TestItems.recording(items));
			Map<String, Object> result = analyze(null, null, null, null, "weight");
			assertEquals(1L, result.get("invalidWeightEventCount"), result.toString());
			assertEquals(0.0, number(denominator(result), "weight"));
			assertEquals(List.of(), result.get("nodes"));
		}
	}

	@Test
	void finiteWeightsThatOverflowExposeNullNumbersAndNativeExactValues() throws Exception {
		IAttribute<Number> cost = Attribute.attr("cost", "Cost", UnitLookup.RAW_NUMBER);
		IItemCollection items = TestItems.collection(TYPE,
				List.of(JfrAttributes.EVENT_STACKTRACE, JfrAttributes.EVENT_THREAD, cost),
				List.of(Map.of("stackTrace", TestItems.stack("example.Work.run", "example.Work.parent"), "eventThread",
						TestItems.thread("one", 1), "cost", Double.MAX_VALUE),
						Map.of("stackTrace", TestItems.stack("example.Work.run", "example.Work.parent"), "eventThread",
								TestItems.thread("two", 2), "cost", Double.MAX_VALUE)));
		tools.recordings = TestItems.service(TestItems.recording(items));
		Map<String, Object> result = analyze(null, null, null, null, "cost");
		assertFalse(result.containsKey("error"), result.toString());
		assertEquals(2L, result.get("stackEventCount"));
		assertNull(result.get("totalWeight"));
		assertEquals("Infinity", result.get("totalWeightExactValue"));
		assertNull(denominator(result).get("weight"));
		assertEquals("Infinity", denominator(result).get("weightExactValue"));
		Map<String, Object> run = node(result, "example.Work.run()");
		assertNull(run.get("selfWeight"));
		assertNull(run.get("inclusiveWeight"));
		assertEquals("Infinity", run.get("selfWeightExactValue"));
		assertEquals("Infinity", run.get("inclusiveWeightExactValue"));
		assertNull(run.get("selfPercent"));
		assertNull(run.get("inclusivePercent"));
		String json = EventValues.json(result);
		assertTrue(json.contains("\"totalWeight\":null"), json);
		assertTrue(json.contains("\"selfPercent\":null"), json);
		String text = tools.getStackTrace(TYPE, "cost", null, null, 20, null, null, null, null, null);
		assertTrue(text.contains("Total weight: Infinity"), text);
		assertTrue(text.contains("N/A"), text);
		Map<String, Object> scoped = analyze(null, "one", null, "source", "cost");
		assertEquals(Double.MAX_VALUE, number(scoped, "totalWeight"));
		assertNull(denominator(scoped).get("weight"));
		assertNull(node(scoped, "example.Work.run()").get("selfPercent"));
	}

	@Test
	void sourceDenominatorIncludesOnlyTheRequestedTimeRange() throws Exception {
		IItemCollection items = TestItems.collection(TYPE,
				List.of(JfrAttributes.EVENT_STACKTRACE, JfrAttributes.START_TIME, JfrAttributes.DURATION),
				List.of(Map.of("stackTrace", TestItems.stack("example.Work.before"), "startTime",
						UnitLookup.EPOCH_NS.quantity(0), "duration", UnitLookup.NANOSECOND.quantity(0)),
						Map.of("stackTrace", TestItems.stack("example.Work.selected"), "startTime",
								UnitLookup.EPOCH_NS.quantity(1_000_000_000), "duration",
								UnitLookup.NANOSECOND.quantity(0))));
		tools.recordings = TestItems.service(TestItems.recording(items));
		Map<String, Object> result = tools.getStackStatistics(TYPE, "count", 0.5, 1.5, 50, null, null, null, "source",
				null);
		assertEquals(1L, result.get("sourceEventCount"));
		assertEquals(1.0, number(denominator(result), "weight"));
		assertEquals("example.Work.selected()", rows(result).getFirst().get("method"));
	}

	@Test
	void invalidScopesReturnActionableErrors() {
		assertTrue(analyze("missing", null, null, null, "count").get("error").toString().contains("missing"));
		assertTrue(analyze(null, null, null, "all", "count").get("error").toString().contains("denominator"));
		assertTrue(
				analyze(null, null, null, null, "noSuchWeight").get("error").toString().contains("numeric attribute"));
	}

	@Test
	void invalidTimeRangesReturnErrorsBeforeFiltering() {
		assertTrue(tools.getStackStatistics(TYPE, "count", Double.NaN, null, 20, null, null, null, null, null)
				.containsKey("error"));
		assertTrue(tools.getStackStatistics(TYPE, "count", 2.0, 1.0, 20, null, null, null, null, null)
				.containsKey("error"));
	}

	@Test
	void textAndStructuredCallsUseTheSameScopeAndPreciseWeights() {
		String text = tools.getStackTrace(TYPE, "duration", null, null, 20, null, "simulation", "terrain", "source",
				null);
		assertTrue(text.contains("Total weight: 0.004"), text);
		assertTrue(text.contains("inclusive 0.004"), text);
		assertTrue(text.contains("Percentage denominator: source, weight 0.02 s"), text);
		assertTrue(text.contains("Selected events: 2"), text);
	}

	private Map<String, Object> analyze(String source, String thread, String frame, String denominator, String weight) {
		return tools.getStackStatistics(TYPE, weight, null, null, 50, source, thread, frame, denominator, null);
	}

	private static Map<String, Object> sample(
		String thread, long threadId, String method, IQuantity duration, long bytes) {
		return Map.of("eventThread", TestItems.thread(thread, threadId), "stackTrace",
				TestItems.stack("example.Simulation." + method, "example.Simulation.tick"), "duration", duration,
				"weight", UnitLookup.BYTE.quantity(bytes));
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> denominator(Map<String, Object> result) {
		return (Map<String, Object>) result.get("denominator");
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> rows(Map<String, Object> result) {
		return (List<Map<String, Object>>) result.get("nodes");
	}

	private static double number(Map<String, Object> values, String key) {
		return ((Number) values.get(key)).doubleValue();
	}

	private static Map<String, Object> node(Map<String, Object> result, String method) {
		return rows(result).stream().filter(row -> method.equals(row.get("method"))).findFirst().orElseThrow();
	}
}
