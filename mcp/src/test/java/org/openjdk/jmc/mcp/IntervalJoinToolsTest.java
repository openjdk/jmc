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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jdk.jfr.Event;
import jdk.jfr.Name;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.IMCThreadGroup;
import org.openjdk.jmc.common.item.Attribute;
import org.openjdk.jmc.common.item.IAccessorKey;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.ICanonicalAccessorFactory;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemCollectionToolkit;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;

class IntervalJoinToolsTest {

	private static final TestType CHILD = new TestType("terrain");
	private static final TestType PHASE = new TestType("physics");
	private static final TestType TICK = new TestType("tick");
	private static final IMCThread THREAD = new TestThread(1L, "simulation");

	@Test
	void attachesEachParentTypeWithoutTreatingDisjointIntervalsAsOneRange() {
		IItemCollection children = items(event(CHILD, "first", 12, 13, THREAD, "run1"),
				event(CHILD, "gap", 25, 26, THREAD, "run1"), event(CHILD, "last", 32, 33, THREAD, "run1"),
				event(CHILD, "outside", 50, 51, THREAD, "run1"));
		IItemCollection parents = items(event(PHASE, "phase1", 10, 20, THREAD, "run1"),
				event(PHASE, "phase2", 30, 40, THREAD, "run1"), event(TICK, "tick1", 0, 45, THREAD, "run1"));
		Map<String, Object> result = join(children, parents, List.of("physics", "tick"), true, 0, 50);

		assertEquals(3L, result.get("matchedChildren"));
		assertEquals(2L, result.get("fullyMatchedChildren"));
		assertEquals(1L, result.get("unmatchedChildren"));
		List<Map<String, Object>> rows = rows(result);
		assertEquals("phase1", label(parent(rows.get(0), "physics")));
		assertNull(parent(rows.get(1), "physics"));
		assertEquals("tick1", label(parent(rows.get(1), "tick")));
		assertEquals("phase2", label(parent(rows.get(2), "physics")));
		assertFalse((Boolean) rows.get(3).get("matched"));
	}

	@Test
	void matchesThreadIdentityAndRunRatherThanThreadNames() {
		IMCThread otherThread = new TestThread(2L, "simulation");
		IItemCollection children = items(event(CHILD, "child", 10, 11, THREAD, "run1"));
		IItemCollection parents = items(event(PHASE, "wrongThread", 0, 20, otherThread, "run1"),
				event(PHASE, "wrongRun", 0, 20, THREAD, "run2"),
				event(PHASE, "correct", 0, 20, new TestThread(1L, "renamed"), "run1"));
		Map<String, Object> result = join(children, parents, List.of("physics"), true, 0, 50);
		Map<String, Object> row = rows(result).getFirst();
		assertEquals("correct", label(parent(row, "physics")));
		assertEquals(Map.of("physics", 1L), row.get("parentMatchCounts"));
		assertEquals(0L, result.get("ambiguousChildren"));
	}

	@Test
	void distinctJavaThreadsSharingOsIdentityDoNotJoin() {
		IMCThread childThread = new OsIdentityThread(1L, 0L);
		IMCThread wrongThread = new OsIdentityThread(2L, 0L);
		assertTrue(childThread.equals(wrongThread), "Fixture mirrors JMC's OS-only thread equality");
		IItemCollection children = items(event(CHILD, "child", 10, 11, childThread, "run1"));
		IItemCollection parents = items(event(PHASE, "wrongThread", 0, 20, wrongThread, "run1"),
				event(PHASE, "correct", 0, 20, new OsIdentityThread(1L, 0L), "run1"));
		Map<String, Object> result = join(children, parents, List.of("physics"), true, 0, 50);
		Map<String, Object> row = rows(result).getFirst();
		assertEquals("correct", label(parent(row, "physics")));
		assertEquals(Map.of("physics", 1L), row.get("parentMatchCounts"));
	}

	@Test
	void nativeThreadsWithZeroOrMissingJavaIdsUseOsIdentity() {
		for (Long javaId : Arrays.asList(null, 0L)) {
			IMCThread childThread = new OsIdentityThread(javaId, 10L);
			IItemCollection children = items(event(CHILD, "child", 10, 11, childThread, "run1"));
			IItemCollection parents = items(
					event(PHASE, "wrongThread", 0, 20, new OsIdentityThread(javaId, 20L), "run1"),
					event(PHASE, "correct", 0, 20, new OsIdentityThread(javaId, 10L), "run1"));
			Map<String, Object> result = join(children, parents, List.of("physics"), true, 0, 50);
			Map<String, Object> row = rows(result).getFirst();
			assertEquals("correct", label(parent(row, "physics")));
			assertEquals(Map.of("physics", 1L), row.get("parentMatchCounts"));
		}
	}

	@Test
	void choosesShortestNestedParentAndReportsAllCandidates() {
		IItemCollection children = items(event(CHILD, "child", 12, 13, THREAD, "run1"));
		IItemCollection parents = items(event(PHASE, "wide", 0, 100, THREAD, "run1"),
				event(PHASE, "shortFirst", 10, 20, THREAD, "run1"),
				event(PHASE, "shortSecond", 10, 20, THREAD, "run1"));
		Map<String, Object> result = join(children, parents, List.of("physics"), true, 0, 50);
		Map<String, Object> row = rows(result).getFirst();
		assertEquals("shortFirst", label(parent(row, "physics")));
		assertEquals(Map.of("physics", 3L), row.get("parentMatchCounts"));
		assertEquals(1L, result.get("ambiguousChildren"));
	}

	@Test
	void numericMatchKeysUseEquivalentUnits() {
		IItem child = event(CHILD, "child", 12, 13, THREAD, "run1");
		IItem parent = event(PHASE, "parent", 0, 20, THREAD, "run1");
		IItem other = event(PHASE, "differentContext", 0, 20, THREAD, "run1");
		((TestItem) child).values.put("context", UnitLookup.SECOND.quantity(1));
		((TestItem) parent).values.put("context", UnitLookup.MILLISECOND.quantity(1_000));
		((TestItem) other).values.put("context", UnitLookup.MILLISECOND.quantity(2_000));
		Map<String, Object> result = IntervalJoinTools.join(items(child), items(parent, other), List.of("physics"),
				List.of("context"), true, List.of(), List.of(), true, 0, 50, "test");
		assertEquals(1L, result.get("matchedChildren"));
		assertEquals("parent", label(parent(rows(result).getFirst(), "physics")));
		assertEquals(Map.of("physics", 1L), rows(result).getFirst().get("parentMatchCounts"));
	}

	@Test
	void quantityMatchKeysUseSharedNativeComparisonRepresentatives() throws Exception {
		IAttribute<?> context = Attribute.attr("context", "Context", UnitLookup.NUMBER);
		Object childContext = UnitLookup.NUMBER_UNITY.quantity(9007199254740992d);
		Object parentContext = UnitLookup.NUMBER_UNITY.quantity(9007199254740993L);
		Map<String, Object> result = joinContext(context, childContext, parentContext);
		assertEquals(1L, result.get("matchedChildren"), EventValues.json(result));
		assertEquals(Map.of("test.ContextParent", 1L), rows(result).getFirst().get("parentMatchCounts"));
	}

	@Test
	void rawNumericMatchKeysRetainTheirOriginalEquality() throws Exception {
		IAttribute<?> context = Attribute.attr("context", "Context", UnitLookup.RAW_NUMBER);
		Map<String, Object> result = joinContext(context, 1, 1L);
		assertEquals(0L, result.get("matchedChildren"), EventValues.json(result));
		assertEquals(1L, result.get("unmatchedChildren"));
	}

	private static Map<String, Object> joinContext(IAttribute<?> context, Object childContext, Object parentContext)
			throws Exception {
		List<IAttribute<?>> attributes = List.of(JfrAttributes.START_TIME, JfrAttributes.DURATION, context);
		IItemCollection children = TestItems.collection("test.ContextChild", attributes,
				List.of(Map.of("startTime", UnitLookup.EPOCH_NS.quantity(12), "duration",
						UnitLookup.NANOSECOND.quantity(1), "context", childContext)));
		IItemCollection parents = TestItems.collection("test.ContextParent", attributes,
				List.of(Map.of("startTime", UnitLookup.EPOCH_NS.quantity(0), "duration",
						UnitLookup.NANOSECOND.quantity(20), "context", parentContext)));
		IntervalJoinTools tools = new IntervalJoinTools();
		tools.recordings = TestItems
				.service(TestItems.recording(ItemCollectionToolkit.merge(() -> Stream.of(children, parents))));
		return tools.joinIntervals("test.ContextChild", "test.ContextParent", null, null, "context", false, "context",
				"context", true, null, null, null);
	}

	@Test
	void comparesNanosecondBoundariesAboveDoubleIntegerPrecision() {
		long epoch = 1_700_000_000_000_000_000L;
		IItemCollection children = items(event(CHILD, "boundary", epoch + 1, epoch + 1, THREAD, "run1"),
				event(CHILD, "outside", epoch + 2, epoch + 3, THREAD, "run1"));
		IItemCollection parents = items(event(PHASE, "oneNanosecond", epoch, epoch + 1, THREAD, "run1"));
		Map<String, Object> result = join(children, parents, List.of("physics"), true, 0, 50);
		assertEquals(1L, result.get("matchedChildren"));
		assertTrue((Boolean) rows(result).get(0).get("matched"));
		assertFalse((Boolean) rows(result).get(1).get("matched"));
		Map<String, Object> attributes = attributes(parent(rows(result).get(0), "physics"));
		Map<?, ?> start = (Map<?, ?>) attributes.get("startTime");
		assertEquals(Long.toString(epoch), start.get("exactValue"));
		assertEquals("epochns", start.get("unit"));
	}

	@Test
	void paginationAndUnmatchedPolicyKeepFullJoinCounts() {
		IItemCollection children = items(event(CHILD, "first", 1, 2, THREAD, "run1"),
				event(CHILD, "second", 3, 4, THREAD, "run1"), event(CHILD, "outside", 10, 11, THREAD, "run1"));
		IItemCollection parents = items(event(PHASE, "parent", 0, 5, THREAD, "run1"));
		Map<String, Object> result = join(children, parents, List.of("physics"), true, 1, 1);
		assertEquals(3L, result.get("totalRows"));
		assertEquals(2L, result.get("matchedChildren"));
		assertEquals(1L, result.get("unmatchedChildren"));
		assertEquals(2, result.get("nextOffset"));
		assertEquals(true, result.get("truncated"));
		assertEquals("second", label(child(rows(result).getFirst())));

		Map<String, Object> matchedOnly = join(children, parents, List.of("physics"), false, 1, 1);
		assertEquals(2L, matchedOnly.get("totalRows"));
		assertEquals(1L, matchedOnly.get("unmatchedChildren"));
		assertNull(matchedOnly.get("nextOffset"));
		assertEquals(false, matchedOnly.get("truncated"));
	}

	@Test
	void missingKeysDoNotMatchAndInvalidIntervalsRemainVisible() {
		IItemCollection children = items(event(CHILD, "missingRun", 1, 2, THREAD, null),
				event(CHILD, "backwards", 2, 1, THREAD, "run1"));
		IItemCollection parents = items(event(PHASE, "missingRunParent", 0, 5, THREAD, null));
		Map<String, Object> result = join(children, parents, List.of("physics"), true, 0, 50);
		assertEquals(0L, result.get("matchedChildren"));
		assertEquals(2L, result.get("unmatchedChildren"));
		assertEquals(1L, result.get("invalidIntervalChildren"));
		assertEquals(2, rows(result).size());
		assertThrows(IllegalArgumentException.class, () -> IntervalJoinTools.join(children, parents, List.of("physics"),
				List.of("bogus"), true, List.of(), List.of(), true, 0, 50, "test"));
	}

	@Test
	void acceptsStoredInputsAndSelectedColumns(@TempDir Path directory) throws Exception {
		Path path = directory.resolve("intervals.jfr");
		try (jdk.jfr.Recording jfr = new jdk.jfr.Recording()) {
			jfr.enable(ParentEvent.class);
			jfr.enable(ChildEvent.class);
			jfr.enable(TickEvent.class);
			jfr.start();
			TickEvent tick = new TickEvent();
			tick.run = "run1";
			tick.tickId = 17;
			tick.begin();
			ParentEvent parent = new ParentEvent();
			parent.run = "run1";
			parent.phase = "physics";
			parent.begin();
			ChildEvent child = new ChildEvent();
			child.run = "run1";
			child.vehicle = "truck";
			child.begin();
			child.end();
			child.commit();
			parent.end();
			parent.commit();
			tick.end();
			tick.commit();
			jfr.stop();
			jfr.dump(path);
		}
		RecordingService service = new RecordingService();
		RecordingService.Recording recording = service.load(path.toString());
		recording.store("children", recording.getItems().apply(ItemFilters.type("test.JoinChild")));
		recording.store("parents", recording.getItems().apply(ItemFilters.type("test.JoinParent")));
		IntervalJoinTools tools = new IntervalJoinTools();
		tools.recordings = service;
		Map<String, Object> result = tools.joinIntervals(null, null, "children", "parents", "run", true, "vehicle,run",
				"phase,run", true, null, null, null);
		assertEquals(1L, result.get("fullyMatchedChildren"), EventValues.json(result));
		Map<String, Object> row = rows(result).getFirst();
		assertEquals(Map.of("vehicle", "truck", "run", "run1"), attributes(child(row)));
		assertEquals(Map.of("phase", "physics", "run", "run1"), attributes(parent(row, "test.JoinParent")));
		assertTrue(tools.joinIntervals(null, null, "absent", "parents", null, true, null, null, true, null, null, null)
				.containsKey("error"));
		assertTrue(tools
				.joinIntervals(null, null, "children", "parents", null, true, "vehcle", null, true, null, null, null)
				.containsKey("error"));
		assertTrue(tools
				.joinIntervals(null, null, "children", "parents", null, true, null, "phaze", true, null, null, null)
				.containsKey("error"));

		Map<String, Object> both = tools.joinIntervals("test.JoinChild", "test.JoinParent,test.JoinTick", null, null,
				"run", true, "vehicle", "phase,tickId,run", true, null, null, null);
		assertEquals(1L, both.get("fullyMatchedChildren"), EventValues.json(both));
		Map<String, Object> bothRow = rows(both).getFirst();
		assertEquals("physics", attributes(parent(bothRow, "test.JoinParent")).get("phase"));
		assertNull(attributes(parent(bothRow, "test.JoinParent")).get("tickId"));
		assertNull(attributes(parent(bothRow, "test.JoinTick")).get("phase"));
		assertEquals("run1", attributes(parent(bothRow, "test.JoinTick")).get("run"));

		recording.store("empty", ItemCollectionToolkit.EMPTY);
		Map<String, Object> empty = tools.joinIntervals("test.JoinChild", "test.JoinParent", "empty", null, null, true,
				"vehicle", "phase", true, null, null, null);
		assertEquals(0, empty.get("totalChildren"), EventValues.json(empty));
		assertTrue(tools.joinIntervals("test.JoinChild", "test.JoinParent", "empty", null, null, true, "vehcle",
				"phase", true, null, null, null).containsKey("error"));
	}

	private static Map<String, Object> join(
		IItemCollection children, IItemCollection parents, List<String> types, boolean includeUnmatched, int offset,
		int limit) {
		return IntervalJoinTools.join(children, parents, types, List.of("run"), true, List.of(), List.of(),
				includeUnmatched, offset, limit, "test");
	}

	private static IItemCollection items(IItem ... items) {
		return ItemCollectionToolkit.build(Arrays.stream(items));
	}

	private static IItem event(TestType type, String label, long start, long end, IMCThread thread, String run) {
		Map<String, Object> values = new LinkedHashMap<>();
		values.put("label", label);
		values.put("startTime", UnitLookup.EPOCH_NS.quantity(start));
		values.put("(endTime)", UnitLookup.EPOCH_NS.quantity(end));
		values.put("duration", UnitLookup.NANOSECOND.quantity(end - start));
		values.put("eventThread", thread);
		values.put("run", run);
		return new TestItem(type, values);
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> rows(Map<String, Object> result) {
		return (List<Map<String, Object>>) result.get("rows");
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> child(Map<String, Object> row) {
		return (Map<String, Object>) row.get("child");
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> parent(Map<String, Object> row, String type) {
		return (Map<String, Object>) ((Map<String, Object>) row.get("parents")).get(type);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> attributes(Map<String, Object> event) {
		return (Map<String, Object>) event.get("attributes");
	}

	private static Object label(Map<String, Object> event) {
		return attributes(event).get("label");
	}

	@Name("test.JoinParent")
	static class ParentEvent extends Event {
		String run;
		String phase;
	}

	@Name("test.JoinChild")
	static class ChildEvent extends Event {
		String run;
		String vehicle;
	}

	@Name("test.JoinTick")
	static class TickEvent extends Event {
		String run;
		long tickId;
	}

	private record TestItem(TestType type, Map<String, Object> values) implements IItem {

	@Override
	public IType<?> getType() {
		return type;
	}

}

/** Mirrors the parser's OS-only equality while exposing independent Java IDs. */
private static final class OsIdentityThread implements IMCThread {
	private final Long javaId;
	private final long osId;

	OsIdentityThread(Long javaId, long osId) {
		this.javaId = javaId;
		this.osId = osId;
	}

	@Override
	public Long getThreadId() {
		return javaId;
	}

	@Override
	public String getThreadName() {
		return "simulation";
	}

	@Override
	public IMCThreadGroup getThreadGroup() {
		return null;
	}

	@Override
		public boolean equals(Object other) {
			return other instanceof OsIdentityThread thread && osId == thread.osId;
		}

	@Override
	public int hashCode() {
		return Long.hashCode(osId);
	}

	}

	private record TestThread(Long id, String name) implements IMCThread {

	@Override
	public Long getThreadId() {
		return id;
	}

	@Override
	public String getThreadName() {
		return name;
	}

	@Override
	public IMCThreadGroup getThreadGroup() {
		return null;
	}

	@Override
		public boolean equals(Object other) {
			return other instanceof TestThread thread && id.equals(thread.id);
		}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
}

private static final class TestType implements IType<IItem> {
	private final String identifier;
	private final List<IAttribute<?>> attributes = new ArrayList<>(
			List.of(JfrAttributes.START_TIME, JfrAttributes.END_TIME, JfrAttributes.DURATION,
					JfrAttributes.EVENT_THREAD, Attribute.attr("label", "Label", UnitLookup.PLAIN_TEXT),
					Attribute.attr("run", "Run", UnitLookup.PLAIN_TEXT),
					Attribute.attr("context", "Context", UnitLookup.TIMESPAN)));

	TestType(String identifier) {
		this.identifier = identifier;
	}

	@Override
	public String getIdentifier() {
		return identifier;
	}

	@Override
	public String getName() {
		return identifier;
	}

	@Override
	public String getDescription() {
		return identifier;
	}

	@Override
	public List<IAttribute<?>> getAttributes() {
		return attributes;
	}

	@Override
	public Map<IAccessorKey<?>, ? extends IDescribable> getAccessorKeys() {
		Map<IAccessorKey<?>, IDescribable> keys = new LinkedHashMap<>();
		for (IAttribute<?> attribute : attributes) {
			keys.put(attribute.getKey(), attribute);
		}
		return keys;
	}

	@Override
	public boolean hasAttribute(ICanonicalAccessorFactory<?> attribute) {
		return getAccessor(attribute.getKey()) != null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <M> IMemberAccessor<M, IItem> getAccessor(IAccessorKey<M> key) {
		if (attributes.stream().anyMatch(attribute -> attribute.getKey().equals(key))) {
			return item -> (M) ((TestItem) item).values.get(key.getIdentifier());
		}
		return null;
	}
}}
