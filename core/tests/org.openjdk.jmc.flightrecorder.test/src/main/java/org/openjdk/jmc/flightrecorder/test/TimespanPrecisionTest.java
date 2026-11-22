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
package org.openjdk.jmc.flightrecorder.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.openjdk.jmc.common.item.Attribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.test.util.RecordingToolkit;

import jdk.jfr.Event;
import jdk.jfr.Name;
import jdk.jfr.Recording;
import jdk.jfr.StackTrace;
import jdk.jfr.Timespan;

/**
 * Timespans in ticks are converted to nanoseconds when parsed, like tick timestamps, so that end
 * times (start time plus duration) are exact and events of zero duration end where they start.
 */
public class TimespanPrecisionTest {

	private static final String[] RECORDINGS = {"7u40.jfr", "7u60.jfr", "7u76.jfr", "8u0.jfr", "8u20.jfr", "8u40.jfr",
			"8u60.jfr", "9u0.jfr", "char_attribute.jfr", "overlap.jfr"};

	private static final String OUTER_TYPE = "org.openjdk.jmc.test.TicksOuter";
	private static final String INNER_TYPE = "org.openjdk.jmc.test.TicksInner";
	private static final String FRACTIONAL_TYPE = "org.openjdk.jmc.test.FractionalTicks";
	private static final int OUTERS = 20;
	private static final int INNERS_PER_OUTER = 50;

	// Offsets in a JFR chunk header (format 1.0 and later)
	private static final int CHUNK_MAGIC = 0x464C5200; // "FLR\0"
	private static final int CHUNK_SIZE_OFFSET = 8;
	private static final int TICKS_PER_SECOND_OFFSET = 56;

	// JFR v0 constants used to build a minimal synthetic chunk for testing v0 tick timespans
	private static final int V0_EVENT_TYPE_ID = 10;
	private static final int V0_CONTENT_TYPE_TICKS = 5;
	private static final byte V0_DATA_TYPE_LONG = 7;
	private static final byte V0_DATA_TYPE_FLOAT = 9;
	private static final byte V0_DATA_TYPE_DOUBLE = 10;
	private static final String V0_TEST_TYPE = "http://test/test/v0ticks";

	// Indexes into the per event times collected by times()
	private static final int START = 0;
	private static final int END = 1;
	private static final int DURATION = 2;
	private static final int OUTER = 3;

	@Name(OUTER_TYPE)
	@StackTrace(false)
	static class Outer extends Event {
		int index;
	}

	@Name(INNER_TYPE)
	@StackTrace(false)
	static class Inner extends Event {
		int index;
		int outer;
	}

	@Name(FRACTIONAL_TYPE)
	@StackTrace(false)
	static class FractionalTicks extends Event {
		@Timespan(Timespan.TICKS)
		double doubleTicks;

		@Timespan(Timespan.TICKS)
		float floatTicks;
	}

	@Test
	public void testEndTimeIsStartTimePlusDuration() throws IOException, CouldNotLoadRecordingException {
		for (String name : RECORDINGS) {
			long checked = assertEndTimesExact(name, RecordingToolkit.getNamedRecording(name));
			assertTrue("No events with a duration in " + name, checked > 0);
		}
	}

	@Test
	public void testTickFrequencies() throws IOException, CouldNotLoadRecordingException {
		byte[] recording = recordNestedEvents();
		// One tick per nanosecond, so the parsed values are the raw ticks
		Map<Integer, long[]> rawInner = parseAndCheck(recording, 1_000_000_000L, 0);
		assertEquals(OUTERS * INNERS_PER_OUTER * 2, rawInner.size());
		int zeroDurations = 0;
		int nonZeroDurations = 0;
		for (Map.Entry<Integer, long[]> entry : rawInner.entrySet()) {
			long duration = entry.getValue()[DURATION];
			if (entry.getKey() % 2 == 0) {
				assertEquals("Instant event should have zero duration", 0L, duration);
				zeroDurations++;
			} else if (duration > 0) {
				nonZeroDurations++;
			}
		}
		assertEquals(OUTERS * INNERS_PER_OUTER, zeroDurations);
		assertTrue("Expected timed events with non-zero duration", nonZeroDurations > 0);

		Map<Integer, long[]> tenMegahertz = parseAndCheck(recording, 10_000_000L, 0);
		for (Map.Entry<Integer, long[]> entry : rawInner.entrySet()) {
			assertEquals(entry.getValue()[DURATION] * 100, tenMegahertz.get(entry.getKey())[DURATION]);
		}

		// Non-whole-nanosecond tick periods are truncated separately for timestamps and timespans,
		// so a nested end time can exceed the end time of its parent by at most one nanosecond.
		for (long ticksPerSecond : new long[] {24_000_000L, 2_903_998_123L}) {
			Map<Integer, long[]> converted = parseAndCheck(recording, ticksPerSecond, 1);
			for (Map.Entry<Integer, long[]> entry : rawInner.entrySet()) {
				long expected = (long) (entry.getValue()[DURATION] / (ticksPerSecond / 1_000_000_000.0));
				assertEquals(expected, converted.get(entry.getKey())[DURATION]);
			}
		}
	}

	@Test
	public void testV0TicksTimespan() throws IOException, CouldNotLoadRecordingException {
		assertV0TicksTimespan(10_000_000L, 71_974L, 7_197_400L, 42.9, 4_290L);
		long subNsFreq = 2_903_998_123L;
		long ticks = 123_456_789L;
		assertV0TicksTimespan(subNsFreq, ticks, (long) (ticks / (subNsFreq / 1_000_000_000.0)), 2.95, 1L);
	}

	@Test
	public void testV1FractionalAndNaNTicksTimespan() throws IOException, CouldNotLoadRecordingException {
		byte[] recording = recordFractionalTicks(42.9, Float.NaN);
		IItemCollection items = JfrLoaderToolkit
				.loadEvents(new ByteArrayInputStream(withTicksPerSecond(recording, 10_000_000L)));
		int count = 0;
		for (IItemIterable iterable : items.apply(ItemFilters.type(FRACTIONAL_TYPE))) {
			IType<IItem> type = iterable.getType();
			IMemberAccessor<IQuantity, IItem> doubleAttr = Attribute
					.attr("doubleTicks", "doubleTicks", UnitLookup.TIMESPAN).getAccessor(type);
			IMemberAccessor<IQuantity, IItem> floatAttr = Attribute
					.attr("floatTicks", "floatTicks", UnitLookup.TIMESPAN).getAccessor(type);
			for (IItem item : iterable) {
				IQuantity doubleVal = doubleAttr.getMember(item);
				assertEquals(UnitLookup.NANOSECOND, doubleVal.getUnit());
				assertEquals(4_290L, doubleVal.longValue());

				IQuantity floatVal = floatAttr.getMember(item);
				assertEquals(UnitLookup.NANOSECOND, floatVal.getUnit());
				assertTrue("Expected NaN to be preserved", Double.isNaN(floatVal.doubleValue()));
				count++;
			}
		}
		assertEquals(1, count);

		byte[] nanDoubleRecording = recordFractionalTicks(Double.NaN, 2.95f);
		IItemCollection subNsItems = JfrLoaderToolkit
				.loadEvents(new ByteArrayInputStream(withTicksPerSecond(nanDoubleRecording, 2_903_998_123L)));
		int subNsCount = 0;
		for (IItemIterable iterable : subNsItems.apply(ItemFilters.type(FRACTIONAL_TYPE))) {
			IType<IItem> type = iterable.getType();
			IMemberAccessor<IQuantity, IItem> doubleAttr = Attribute
					.attr("doubleTicks", "doubleTicks", UnitLookup.TIMESPAN).getAccessor(type);
			IMemberAccessor<IQuantity, IItem> floatAttr = Attribute
					.attr("floatTicks", "floatTicks", UnitLookup.TIMESPAN).getAccessor(type);
			for (IItem item : iterable) {
				IQuantity doubleVal = doubleAttr.getMember(item);
				assertEquals(UnitLookup.NANOSECOND, doubleVal.getUnit());
				assertTrue("Expected NaN to be preserved", Double.isNaN(doubleVal.doubleValue()));

				IQuantity floatVal = floatAttr.getMember(item);
				assertEquals(UnitLookup.NANOSECOND, floatVal.getUnit());
				assertEquals(1L, floatVal.longValue());
				subNsCount++;
			}
		}
		assertEquals(1, subNsCount);
	}

	private static void assertV0TicksTimespan(
		long ticksPerSecond, long longTicks, long expectedLongNanos, double doubleTicks, long expectedDoubleNanos)
			throws IOException, CouldNotLoadRecordingException {
		byte[] chunk = buildV0Recording(ticksPerSecond, longTicks, doubleTicks, Float.NaN);
		IItemCollection items = JfrLoaderToolkit.loadEvents(new ByteArrayInputStream(chunk));
		IItemCollection v0Events = items.apply(ItemFilters.type(V0_TEST_TYPE));
		assertEndTimesExact("v0 " + ticksPerSecond + " ticks/s", v0Events);
		int count = 0;
		for (IItemIterable iterable : v0Events) {
			IType<IItem> type = iterable.getType();
			IMemberAccessor<IQuantity, IItem> longAttr = Attribute.attr("longTicks", "Long Ticks", UnitLookup.TIMESPAN)
					.getAccessor(type);
			IMemberAccessor<IQuantity, IItem> doubleAttr = Attribute
					.attr("doubleTicks", "Double Ticks", UnitLookup.TIMESPAN).getAccessor(type);
			IMemberAccessor<IQuantity, IItem> nanAttr = Attribute.attr("nanTicks", "NaN Ticks", UnitLookup.TIMESPAN)
					.getAccessor(type);
			for (IItem item : iterable) {
				IQuantity longVal = longAttr.getMember(item);
				assertEquals(UnitLookup.NANOSECOND, longVal.getUnit());
				assertEquals(expectedLongNanos, longVal.longValue());

				IQuantity doubleVal = doubleAttr.getMember(item);
				assertEquals(UnitLookup.NANOSECOND, doubleVal.getUnit());
				assertEquals(expectedDoubleNanos, doubleVal.longValue());

				assertNull(nanAttr.getMember(item));
				count++;
			}
		}
		assertEquals(1, count);
	}

	/**
	 * Parses the recording with its tick frequency replaced, checks end times and nesting, and
	 * returns the inner events by index.
	 */
	private static Map<Integer, long[]> parseAndCheck(byte[] recording, long ticksPerSecond, long tolerance)
			throws IOException, CouldNotLoadRecordingException {
		IItemCollection items = JfrLoaderToolkit
				.loadEvents(new ByteArrayInputStream(withTicksPerSecond(recording, ticksPerSecond)));
		String context = ticksPerSecond + " ticks/s";
		assertEndTimesExact(context, items);
		Map<Integer, long[]> outers = times(items.apply(ItemFilters.type(OUTER_TYPE)));
		Map<Integer, long[]> inners = times(items.apply(ItemFilters.type(INNER_TYPE)));
		assertEquals(context, OUTERS, outers.size());
		for (Map.Entry<Integer, long[]> entry : inners.entrySet()) {
			long[] inner = entry.getValue();
			long[] outer = outers.get((int) inner[OUTER]);
			String event = context + ", inner event " + entry.getKey();
			assertTrue(event + " starts before its parent", outer[START] <= inner[START]);
			assertTrue(event + " ends after its parent", inner[END] <= outer[END] + tolerance);
		}
		return inners;
	}

	private static Map<Integer, long[]> times(IItemCollection items) {
		Map<Integer, long[]> result = new HashMap<>();
		for (IItemIterable iterable : items) {
			IType<IItem> type = iterable.getType();
			IMemberAccessor<IQuantity, IItem> start = JfrAttributes.START_TIME.getAccessor(type);
			IMemberAccessor<IQuantity, IItem> end = JfrAttributes.END_TIME.getAccessor(type);
			IMemberAccessor<IQuantity, IItem> duration = JfrAttributes.DURATION.getAccessor(type);
			IMemberAccessor<IQuantity, IItem> index = Attribute.attr("index", "index", UnitLookup.NUMBER)
					.getAccessor(type);
			IMemberAccessor<IQuantity, IItem> outer = Attribute.attr("outer", "outer", UnitLookup.NUMBER)
					.getAccessor(type);
			for (IItem item : iterable) {
				IQuantity startTime = start.getMember(item);
				IQuantity endTime = end.getMember(item);
				IQuantity dur = duration.getMember(item);
				assertEquals(UnitLookup.EPOCH_NS, startTime.getUnit());
				assertEquals(UnitLookup.EPOCH_NS, endTime.getUnit());
				assertEquals(UnitLookup.NANOSECOND, dur.getUnit());
				long[] values = new long[] {nanos(startTime), nanos(endTime),
						dur.clampedLongValueIn(UnitLookup.NANOSECOND),
						outer == null ? -1 : outer.getMember(item).longValue()};
				result.put((int) index.getMember(item).longValue(), values);
			}
		}
		return result;
	}

	private static long nanos(IQuantity timestamp) {
		return timestamp.clampedLongValueIn(UnitLookup.EPOCH_NS);
	}

	/**
	 * Checks that each event's end time minus its start time is exactly its duration, which also
	 * means that events of zero duration end where they start.
	 *
	 * @return the number of events checked
	 */
	private static long assertEndTimesExact(String context, IItemCollection items) {
		long checked = 0;
		for (IItemIterable iterable : items) {
			IType<IItem> type = iterable.getType();
			if (type.getAccessor(JfrAttributes.DURATION.getKey()) == null
					&& type.getAccessor(JfrAttributes.END_TIME.getKey()) == null) {
				continue;
			}
			IMemberAccessor<IQuantity, IItem> start = JfrAttributes.START_TIME.getAccessor(type);
			IMemberAccessor<IQuantity, IItem> end = JfrAttributes.END_TIME.getAccessor(type);
			IMemberAccessor<IQuantity, IItem> duration = JfrAttributes.DURATION.getAccessor(type);
			if (start == null || end == null || duration == null) {
				continue;
			}
			for (IItem item : iterable) {
				IQuantity startTime = start.getMember(item);
				IQuantity endTime = end.getMember(item);
				IQuantity timespan = duration.getMember(item);
				if (startTime == null || endTime == null || timespan == null) {
					continue;
				}
				assertEquals(context + ", " + type.getIdentifier(), timespan.clampedLongValueIn(UnitLookup.NANOSECOND),
						endTime.subtract(startTime).clampedLongValueIn(UnitLookup.NANOSECOND));
				checked++;
			}
		}
		return checked;
	}

	/**
	 * Records events of zero duration and timed events, all nested in enclosing events.
	 */
	private static byte[] recordNestedEvents() throws IOException {
		Path file = Files.createTempFile("ticks", ".jfr");
		try {
			try (Recording recording = new Recording()) {
				recording.enable(Outer.class);
				recording.enable(Inner.class);
				recording.start();
				int index = 0;
				for (int o = 0; o < OUTERS; o++) {
					Outer outer = new Outer();
					outer.index = o;
					outer.begin();
					for (int i = 0; i < INNERS_PER_OUTER; i++) {
						Inner instant = new Inner();
						instant.index = index++;
						instant.outer = o;
						instant.commit();
						Inner timed = new Inner();
						timed.index = index++;
						timed.outer = o;
						timed.begin();
						spin();
						timed.end();
						timed.commit();
					}
					outer.end();
					outer.commit();
				}
				recording.stop();
				recording.dump(file);
			}
			return Files.readAllBytes(file);
		} finally {
			Files.deleteIfExists(file);
		}
	}

	private static byte[] recordFractionalTicks(double doubleTicks, float floatTicks) throws IOException {
		Path file = Files.createTempFile("ticks-fractional", ".jfr");
		try {
			try (Recording recording = new Recording()) {
				recording.enable(FractionalTicks.class);
				recording.start();
				FractionalTicks event = new FractionalTicks();
				event.doubleTicks = doubleTicks;
				event.floatTicks = floatTicks;
				event.commit();
				recording.stop();
				recording.dump(file);
			}
			return Files.readAllBytes(file);
		} finally {
			Files.deleteIfExists(file);
		}
	}

	private static void spin() {
		long start = System.nanoTime();
		while (System.nanoTime() - start < 2_000) {
			Thread.onSpinWait();
		}
	}

	/**
	 * Returns a copy of the recording with the tick frequency of every chunk replaced, so that all
	 * tick values are interpreted at that frequency.
	 */
	private static byte[] withTicksPerSecond(byte[] recording, long ticksPerSecond) {
		byte[] patched = recording.clone();
		ByteBuffer buffer = ByteBuffer.wrap(patched);
		int chunkStart = 0;
		while (chunkStart < patched.length) {
			assertEquals("Not a chunk header", CHUNK_MAGIC, buffer.getInt(chunkStart));
			buffer.putLong(chunkStart + TICKS_PER_SECOND_OFFSET, ticksPerSecond);
			chunkStart += (int) buffer.getLong(chunkStart + CHUNK_SIZE_OFFSET);
		}
		return patched;
	}

	private static byte[] buildV0Recording(long ticksPerSecond, long longTicks, double doubleTicks, float floatTicks)
			throws IOException {
		ByteArrayOutputStream eventBody = new ByteArrayOutputStream();
		try (DataOutputStream out = new DataOutputStream(eventBody)) {
			out.writeInt(V0_EVENT_TYPE_ID);
			out.writeLong(1_500L); // endTime ticks
			out.writeLong(1_100L); // startTime ticks
			out.writeLong(longTicks);
			out.writeDouble(doubleTicks);
			out.writeFloat(floatTicks);
		}
		byte[] eventBytes = eventBody.toByteArray();
		int eventSize = Integer.BYTES + eventBytes.length;

		ByteArrayOutputStream metaBody = new ByteArrayOutputStream();
		try (DataOutputStream out = new DataOutputStream(metaBody)) {
			out.writeInt(0); // metadata event type
			out.writeInt(1); // producer count
			out.writeInt(1); // producer id
			writeUtf(out, "test");
			writeUtf(out, "");
			writeUtf(out, "http://test/");
			out.writeInt(0); // relations count
			out.writeInt(1); // data structures count
			out.writeInt(3); // fields count
			writeV0Field(out, "longTicks", "Long Ticks", V0_DATA_TYPE_LONG);
			writeV0Field(out, "doubleTicks", "Double Ticks", V0_DATA_TYPE_DOUBLE);
			writeV0Field(out, "nanTicks", "NaN Ticks", V0_DATA_TYPE_FLOAT);
			out.writeInt(1); // event types count
			out.writeInt(V0_EVENT_TYPE_ID);
			writeUtf(out, "V0 Ticks");
			writeUtf(out, "");
			writeUtf(out, "test/v0ticks");
			out.writeByte(1); // hasStartTime
			out.writeByte(0); // hasThread
			out.writeByte(0); // canHaveStacktrace
			out.writeByte(0); // isRequestable
			out.writeInt(0); // dataStructureIndex
			out.writeInt(0); // expansion
			out.writeInt(0); // content types count
			out.writeLong(1_700_000_000_000L); // startTimeMillis
			out.writeLong(1_700_000_001_000L); // endTimeMillis
			out.writeLong(1_000L); // startTicks
			out.writeLong(ticksPerSecond);
			out.writeLong(0L); // previousCheckPoint
		}
		byte[] metaBytes = metaBody.toByteArray();
		int metaSize = Integer.BYTES + metaBytes.length;
		long metadataOffset = 16L + eventSize;

		ByteArrayOutputStream chunk = new ByteArrayOutputStream();
		try (DataOutputStream out = new DataOutputStream(chunk)) {
			out.writeInt(CHUNK_MAGIC);
			out.writeShort(0); // major version 0
			out.writeShort(9); // minor version 9
			out.writeLong(metadataOffset);
			out.writeInt(eventSize);
			out.write(eventBytes);
			out.writeInt(metaSize);
			out.write(metaBytes);
		}
		return chunk.toByteArray();
	}

	private static void writeV0Field(DataOutputStream out, String id, String name, byte dataType) throws IOException {
		writeUtf(out, id);
		writeUtf(out, name);
		writeUtf(out, "");
		out.writeByte(0); // Transition.None
		out.writeByte(dataType);
		out.writeInt(V0_CONTENT_TYPE_TICKS);
		out.writeInt(0); // dataStructIndex
		out.writeInt(0); // expansion
	}

	private static void writeUtf(DataOutputStream out, String value) throws IOException {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		out.writeShort(bytes.length);
		out.write(bytes);
	}
}
