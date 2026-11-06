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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openjdk.jmc.common.item.Attribute;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemCollectionToolkit;
import org.openjdk.jmc.common.item.ItemIterableToolkit;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.mcp.RecordingService.Recording;

class EventTypeMetadataTest {
	private static final IAttribute<String> VEHICLE = Attribute.attr("vehicle", "Vehicle", UnitLookup.PLAIN_TEXT);
	private Recording recording;
	private IItemCollection firstTicks;
	private IItemCollection secondTicks;
	private IType<IItem> otherType;
	private MetadataTools metadata;
	private RecordingTools recordingTools;
	private QueryTools query;
	private CorrelationTools correlation;

	@BeforeEach
	void setUp() throws Exception {
		List<IItem> ticks = JfrToolkit.toItemList(collection("test.Tick", List.of(VEHICLE), 5));
		firstTicks = ItemCollectionToolkit.build(ticks.stream().limit(2));
		secondTicks = ItemCollectionToolkit.build(ticks.stream().skip(2));
		List<IItem> others = JfrToolkit.toItemList(collection("test.Other", List.of(), 3));
		IItemCollection firstOthers = ItemCollectionToolkit.build(others.stream().limit(1));
		IItemCollection secondOthers = ItemCollectionToolkit.build(others.stream().skip(1));
		otherType = firstOthers.iterator().next().getType();
		IType<IItem> emptyType = collection("test.Empty", List.of(), 1).iterator().next().getType();
		IItemCollection empty = ItemCollectionToolkit
				.build(() -> Stream.of(ItemIterableToolkit.build(Stream::empty, emptyType)));
		IItemCollection items = ItemCollectionToolkit
				.merge(() -> Stream.of(firstTicks, firstOthers, secondTicks, secondOthers, empty));
		recording = TestItems.recording(items);
		RecordingService service = TestItems.service(recording);
		metadata = new MetadataTools();
		metadata.recordings = service;
		recordingTools = new RecordingTools();
		recordingTools.recordings = service;
		query = new QueryTools();
		query.recordings = service;
		correlation = new CorrelationTools();
		correlation.recordings = service;
	}

	@Test
	void eventTypeMetadataMergesCountsAcrossItemGroups() {
		String result = metadata.getEventTypes(null);
		assertTrue(result.contains("test.Tick (test.Tick): 5 events"), result);
		assertTrue(result.contains("test.Other (test.Other): 3 events"), result);
		assertEquals(1, result.lines().filter(line -> line.startsWith("  test.Tick (")).count(), result);
		assertEquals(1, result.lines().filter(line -> line.startsWith("  test.Other (")).count(), result);
		assertEquals(0, result.lines().filter(line -> line.contains("test.Empty")).count(), result);
	}

	@Test
	void recordingSummaryCountsUniquePopulatedTypes() {
		String result = recordingTools.getRecordingInfo(null);
		assertTrue(result.contains("Events: 8 across 2 event types"), result);
		IType<IItem> tickType = firstTicks.iterator().next().getType();
		assertSame(tickType, secondTicks.iterator().next().getType());
		assertEquals(Map.of(tickType, 5L, otherType, 3L), JfrToolkit.countByEventType(recording.getItems()));
	}

	@Test
	void distinctTypesWithSameIdentifierRetainSeparateCounts() {
		IItemCollection withVehicle = collection("test.Tick", List.of(VEHICLE), 2);
		IItemCollection withoutVehicle = collection("test.Tick", List.of(), 3);
		IItemCollection merged = ItemCollectionToolkit.merge(() -> Stream.of(withVehicle, withoutVehicle));
		assertEquals(
				Map.of(withVehicle.iterator().next().getType(), 2L, withoutVehicle.iterator().next().getType(), 3L),
				JfrToolkit.countByEventType(merged));
	}

	@Test
	void tableSummaryAndSkippedTypeNotesMergeCountsAcrossItemGroups() {
		String result = query.getEventTable(null, null, null, "vehicle", null, null, 100, null, null);
		assertTrue(result.contains("Matched 2 event types:"), result);
		assertEquals(1, result.lines().filter("  test.Tick: 5 events"::equals).count(), result);
		assertEquals(1, result.lines().filter("  test.Other: 3 events"::equals).count(), result);
		assertEquals(1,
				result.lines().filter(line -> line.startsWith("(test.Other: none of the requested columns")).count(),
				result);
		assertTrue(result.contains("3 events not shown"), result);
	}

	@Test
	void oneTypeAcrossItemGroupsDoesNotLookLikeMultipleTypes() {
		query.recordings = TestItems.service(recordingForTicks());
		String result = query.getEventTable(null, null, null, "vehicle", null, null, 100, null, null);
		assertEquals(0, result.lines().filter(line -> line.startsWith("Matched ")).count(), result);
	}

	@Test
	void correlationAndCombinedSetSummariesMergeItemGroupCounts() {
		recording.getSelections().put("reference",
				ItemCollectionToolkit.build(JfrToolkit.toItemList(firstTicks).stream().limit(1)));
		String related = correlation.findRelatedEvents("concurrent", "reference", null, null, null, null, null, false,
				true, null, 100, null);
		assertEquals(1, related.lines().filter("  test.Tick: 5 events"::equals).count(), related);
		assertEquals(1, related.lines().filter("  test.Other: 3 events"::equals).count(), related);
		recording.getSelections().put("first", firstTicks);
		recording.getSelections().put("second", secondTicks);
		String combined = correlation.combineResultSets("first", "second", "union", "allTicks", null);
		assertEquals(1, combined.lines().filter("  test.Tick: 5 events"::equals).count(), combined);
	}

	private Recording recordingForTicks() {
		try {
			return TestItems.recording(ItemCollectionToolkit.merge(() -> Stream.of(firstTicks, secondTicks)));
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static IItemCollection collection(String type, List<IAttribute<?>> additional, int count) {
		List<IAttribute<?>> attributes = new java.util.ArrayList<>(additional);
		attributes.add(JfrAttributes.START_TIME);
		attributes.add(JfrAttributes.DURATION);
		List<Map<String, Object>> rows = java.util.stream.IntStream.range(0, count)
				.<Map<String, Object>> mapToObj(index -> Map.of("startTime", UnitLookup.EPOCH_NS.quantity(0),
						"duration", UnitLookup.SECOND.quantity(10), "vehicle", "truck"))
				.toList();
		return TestItems.collection(type, attributes, rows);
	}
}
