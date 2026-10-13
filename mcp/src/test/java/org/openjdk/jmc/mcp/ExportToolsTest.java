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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jdk.jfr.Event;
import jdk.jfr.Name;
import jdk.jfr.Recording;
import jdk.jfr.Timespan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.IMCThreadGroup;
import org.openjdk.jmc.common.item.ItemCollectionToolkit;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.LinearUnit;
import org.openjdk.jmc.common.unit.UnitLookup;

class ExportToolsTest {

	private static final long LARGE_INTEGER = 9007199254740993L;
	private static final double FRACTION = 0.12345678901234568;
	private static final String EVENT_TYPE = "test.PreciseExport";

	@Name(EVENT_TYPE)
	static class ExportEvent extends Event {
		long largeInteger;
		double fraction;
		String label;

		@Timespan(Timespan.NANOSECONDS)
		long elapsed;
	}

	@Test
	void numbersKeepFullPrecision() {
		Map<?, ?> integer = (Map<?, ?>) EventValues.encode(LARGE_INTEGER);
		assertEquals(LARGE_INTEGER, integer.get("value"));
		assertEquals("9007199254740993", integer.get("exactValue"));
		assertTrue(EventValues.json(integer).contains("\"value\":9007199254740993"));
		assertEquals("", integer.get("unit"));
		assertEquals("number", integer.get("quantityKind"));
		Map<?, ?> fraction = (Map<?, ?>) EventValues.encode(FRACTION);
		assertEquals(FRACTION, fraction.get("value"));
		assertEquals(Double.toString(FRACTION), fraction.get("exactValue"));
		assertTrue(EventValues.json(fraction).contains("\"value\":" + Double.toString(FRACTION)));
		assertEquals(0.12345679f, ((Map<?, ?>) EventValues.encode(0.12345679f)).get("value"));
	}

	@Test
	void quantitiesIncludeExactNumericValueAndUnits() {
		IQuantity source = UnitLookup.NANOSECOND.quantity(LARGE_INTEGER);
		Map<?, ?> quantity = (Map<?, ?>) EventValues.encode(source);
		assertEquals(LARGE_INTEGER, quantity.get("value"));
		assertEquals("9007199254740993", quantity.get("exactValue"));
		assertEquals(UnitLookup.NANOSECOND.getIdentifier(), quantity.get("unit"));
		assertEquals(UnitLookup.TIMESPAN.getIdentifier(), quantity.get("quantityKind"));
		assertEquals(UnitLookup.SECOND.getIdentifier(), quantity.get("canonicalUnit"));
		assertEquals(source.numberValueIn(UnitLookup.SECOND), quantity.get("canonicalValue"));
		assertEquals(TestItems.plain(source.numberValueIn(UnitLookup.SECOND)), quantity.get("canonicalExactValue"));
		assertTrue(EventValues.json(quantity).contains("\"value\":9007199254740993"));
		Map<?, ?> fraction = (Map<?, ?>) EventValues.encode(UnitLookup.SECOND.quantity(FRACTION));
		assertEquals(FRACTION, fraction.get("value"));
		assertEquals(Double.toString(FRACTION), fraction.get("exactValue"));
	}

	@Test
	void recordingSpecificTickScalesHaveCanonicalValues() {
		LinearUnit ticks = UnitLookup.TIMESPAN.makeUnit("exportTicks", UnitLookup.NANOSECOND.quantity(2));
		IQuantity source = ticks.quantity(LARGE_INTEGER);
		Map<?, ?> quantity = (Map<?, ?>) EventValues.encode(source);
		assertEquals(LARGE_INTEGER, quantity.get("value"));
		assertEquals("9007199254740993", quantity.get("exactValue"));
		assertEquals("exportTicks", quantity.get("unit"));
		assertEquals("s", quantity.get("canonicalUnit"));
		assertEquals(TestItems.plain(source.numberValueIn(UnitLookup.SECOND)), quantity.get("canonicalExactValue"));
		assertEquals(source.numberValueIn(UnitLookup.SECOND), quantity.get("canonicalValue"));
		LinearUnit otherTicks = UnitLookup.TIMESPAN.makeUnit("exportTicks", UnitLookup.NANOSECOND.quantity(4));
		IQuantity otherSource = otherTicks.quantity(LARGE_INTEGER);
		Map<?, ?> other = (Map<?, ?>) EventValues.encode(otherSource);
		assertEquals(quantity.get("unit"), other.get("unit"));
		assertEquals(TestItems.plain(otherSource.numberValueIn(UnitLookup.SECOND)), other.get("canonicalExactValue"));
		assertEquals(otherSource.numberValueIn(UnitLookup.SECOND), other.get("canonicalValue"));
		Map<?, ?> statistic = EventValues.encodeNumber(FRACTION, ticks);
		IQuantity nativeStatistic = ticks.quantity(FRACTION);
		assertEquals(TestItems.plain(nativeStatistic.numberValue()), statistic.get("exactValue"));
		assertEquals(TestItems.plain(nativeStatistic.numberValueIn(UnitLookup.SECOND)),
				statistic.get("canonicalExactValue"));
		assertEquals(nativeStatistic.numberValueIn(UnitLookup.SECOND), statistic.get("canonicalValue"));
	}

	@Test
	void nullAndNonFiniteValuesHaveValidJsonRepresentations() {
		assertNull(EventValues.encode(null));
		assertEquals("null", EventValues.json(EventValues.encode(null)));
		assertEquals(true, EventValues.encode(true));
		Map<?, ?> notANumber = (Map<?, ?>) EventValues.encode(UnitLookup.NUMBER_UNITY.quantity(Double.NaN));
		assertNull(notANumber.get("value"));
		assertEquals("NaN", notANumber.get("exactValue"));
		assertNull(notANumber.get("canonicalValue"));
		assertEquals("NaN", notANumber.get("canonicalExactValue"));
		assertTrue(EventValues.json(notANumber).contains("\"value\":null"));
		Map<?, ?> infinity = (Map<?, ?>) EventValues.encode(UnitLookup.NANOSECOND.quantity(Double.POSITIVE_INFINITY));
		assertNull(infinity.get("canonicalValue"));
		assertEquals("Infinity", infinity.get("canonicalExactValue"));
		assertEquals("s", infinity.get("canonicalUnit"));
	}

	@Test
	void threadsCarryTheirStableIdAndName() {
		IMCThread thread = new IMCThread() {
			@Override
			public Long getThreadId() {
				return 42L;
			}

			@Override
			public String getThreadName() {
				return "simulation";
			}

			@Override
			public IMCThreadGroup getThreadGroup() {
				return null;
			}
		};
		assertEquals(Map.of("id", 42L, "name", "simulation"), EventValues.encode(thread));
	}

	@Test
	void explicitColumnsPreserveOrderAndRejectAmbiguousLists() {
		assertEquals(List.of(), EventValues.columns(null));
		assertEquals(List.of(), EventValues.columns(" "));
		assertEquals(List.of("fraction", "label"), EventValues.columns(" fraction, label "));
		assertThrows(IllegalArgumentException.class, () -> EventValues.columns("label,"));
		assertThrows(IllegalArgumentException.class, () -> EventValues.columns("label,,fraction"));
		assertThrows(IllegalArgumentException.class, () -> EventValues.columns("label,label"));
	}

	@Test
	void csvEscapesQuotesCommasAndNewlines() {
		assertEquals("simple", ExportTools.csvCell("simple"));
		assertEquals("\"a,b\"", ExportTools.csvCell("a,b"));
		assertEquals("\"a\"\"b\"", ExportTools.csvCell("a\"b"));
		assertEquals("\"a\nb\"", ExportTools.csvCell("a\nb"));
		assertEquals("\"a\rb\"", ExportTools.csvCell("a\rb"));
		assertEquals("", ExportTools.csvCell(null));
		assertEquals("9007199254740993", ExportTools.csvCell(LARGE_INTEGER));
		Map<String, Object> compound = new LinkedHashMap<>();
		compound.put("value", LARGE_INTEGER);
		compound.put("unit", "ns");
		assertEquals("\"{\"\"value\"\":9007199254740993,\"\"unit\"\":\"\"ns\"\"}\"", ExportTools.csvCell(compound));
	}

	@Test
	void storedSetCanBeExportedCompletelyThroughPages(@TempDir Path directory) throws Exception {
		ExportTools tools = storedRecording(directory);
		Map<String, Object> first = tools.exportResultSet("precise", "largeInteger,fraction,label,elapsed", "json", 0L,
				2, null);
		assertEquals(3L, first.get("total"));
		assertEquals(2, first.get("returned"));
		assertEquals(2L, first.get("nextOffset"));
		assertEquals(true, first.get("truncated"));
		List<?> events = (List<?>) first.get("events");
		Map<?, ?> event = (Map<?, ?>) events.get(0);
		assertEquals(EVENT_TYPE, event.get("eventType"));
		Map<?, ?> attributes = (Map<?, ?>) event.get("attributes");
		assertEquals(LARGE_INTEGER, ((Map<?, ?>) attributes.get("largeInteger")).get("value"));
		assertEquals("9007199254740993", ((Map<?, ?>) attributes.get("largeInteger")).get("exactValue"));
		assertEquals(FRACTION, ((Map<?, ?>) attributes.get("fraction")).get("value"));
		assertEquals(UnitLookup.NANOSECOND.getIdentifier(), ((Map<?, ?>) attributes.get("elapsed")).get("unit"));
		Map<String, Object> last = tools.exportResultSet("precise", "largeInteger,fraction,label,elapsed", "json",
				(Long) first.get("nextOffset"), 2, null);
		assertEquals(1, last.get("returned"));
		assertNull(last.get("nextOffset"));
		assertEquals(false, last.get("truncated"));
		Map<?, ?> lastEvent = (Map<?, ?>) ((List<?>) last.get("events")).get(0);
		assertNull(((Map<?, ?>) lastEvent.get("attributes")).get("label"));
	}

	@Test
	void csvPagesHaveStableHeadersAndExactValues(@TempDir Path directory) throws Exception {
		ExportTools tools = storedRecording(directory);
		Map<String, Object> first = tools.exportResultSet("precise", "label,largeInteger", "csv", 0L, 1, null);
		Map<String, Object> second = tools.exportResultSet("precise", "label,largeInteger", "csv", 1L, 1, null);
		String csv = (String) first.get("csv");
		assertTrue(csv.startsWith("eventType,label,largeInteger\r\n"), csv);
		assertTrue(csv.contains("9007199254740993"), csv);
		assertTrue(((String) second.get("csv")).contains("\"a,\"\"b\"\"\nc\""));
		assertFalse(first.containsKey("events"));
	}

	@Test
	void emptyAndPastEndPagesAreExplicit(@TempDir Path directory) throws Exception {
		ExportTools tools = storedRecording(directory);
		tools.recordings.get(null).store("empty", ItemCollectionToolkit.EMPTY);
		Map<String, Object> empty = tools.exportResultSet("empty", null, null, null, null, null);
		assertEquals(0L, empty.get("total"));
		assertEquals(0, empty.get("returned"));
		assertEquals(false, empty.get("truncated"));
		assertNull(empty.get("nextOffset"));
		Map<String, Object> emptyCsv = tools.exportResultSet("empty", "label,largeInteger", "csv", null, null, null);
		assertEquals("eventType,label,largeInteger\r\n", emptyCsv.get("csv"));
		Map<String, Object> pastEnd = tools.exportResultSet("precise", null, null, Long.MAX_VALUE, 1, null);
		assertEquals(3L, pastEnd.get("total"));
		assertEquals(0, pastEnd.get("returned"));
		assertNull(pastEnd.get("nextOffset"));
	}

	@Test
	void invalidExportArgumentsAreReported(@TempDir Path directory) throws Exception {
		ExportTools tools = storedRecording(directory);
		assertTrue(tools.exportResultSet("unknown", null, null, null, null, null).containsKey("error"));
		assertTrue(tools.exportResultSet("precise", "notAnAttribute", null, null, null, null).containsKey("error"));
		assertTrue(tools.exportResultSet("precise", null, "xml", null, null, null).containsKey("error"));
		assertTrue(tools.exportResultSet("precise", null, null, -1L, null, null).containsKey("error"));
		assertTrue(tools.exportResultSet("precise", null, null, null, 0, null).containsKey("error"));
		assertTrue(tools.exportResultSet(" ", null, null, null, null, null).containsKey("error"));
	}

	private static ExportTools storedRecording(Path directory) throws Exception {
		Path path = directory.resolve("precise.jfr");
		try (Recording recording = new Recording()) {
			recording.enable(ExportEvent.class).withoutStackTrace();
			recording.start();
			for (int i = 0; i < 3; i++) {
				ExportEvent event = new ExportEvent();
				event.largeInteger = LARGE_INTEGER + i;
				event.fraction = FRACTION;
				event.label = i == 0 ? "row0" : i == 1 ? "a,\"b\"\nc" : null;
				event.elapsed = i + 1;
				event.commit();
			}
			recording.stop();
			recording.dump(path);
		}
		RecordingService service = new RecordingService();
		RecordingService.Recording recording = service.load(path.toString());
		recording.store("precise", recording.getItems().apply(ItemFilters.type(EVENT_TYPE)));
		ExportTools tools = new ExportTools();
		tools.recordings = service;
		return tools;
	}
}
