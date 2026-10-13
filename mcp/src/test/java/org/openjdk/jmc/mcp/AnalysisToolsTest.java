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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.openjdk.jmc.common.item.Attribute;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.mcp.RecordingService.Recording;

class AnalysisToolsTest {
	private static final String TYPE = "test.Tick";
	private static final IAttribute<String> ID = Attribute.attr("id", "Identifier", UnitLookup.PLAIN_TEXT);

	@Test
	void minimumAndMaximumDetailsAndStoredEventsMatchExactDuration() throws Exception {
		IItemCollection items = TestItems.collection(TYPE, List.of(JfrAttributes.DURATION, ID),
				List.of(Map.of("duration", UnitLookup.MILLISECOND.quantity(49.806), "id", "long"),
						Map.of("duration", UnitLookup.NANOSECOND.quantity(1_001_000), "id", "short"),
						Map.of("duration", UnitLookup.MILLISECOND.quantity(10), "id", "middle")));
		Recording recording = TestItems.recording(items);
		AnalysisTools tools = new AnalysisTools();
		tools.recordings = TestItems.service(recording);
		String result = tools.aggregateEvents(TYPE, "all", null, null, null);
		assertFalse(result.startsWith("Error:"), result);
		String minimum = result.substring(result.indexOf("Min event details:"), result.indexOf("  Max(duration):"));
		String maximum = result.substring(result.indexOf("Max event details:"));
		assertTrue(minimum.contains("Identifier: short"), result);
		assertFalse(minimum.contains("Identifier: long"), result);
		assertTrue(maximum.contains("Identifier: long"), result);
		IItem min = JfrToolkit.toItemList(recording.getStored(TYPE + ".min")).getFirst();
		IItem max = JfrToolkit.toItemList(recording.getStored(TYPE + ".max")).getFirst();
		assertEquals("short", ID.getAccessor(type(min)).getMember(min));
		assertEquals("long", ID.getAccessor(type(max)).getMember(max));
		assertEquals(1_001_000, duration(min).longValueIn(UnitLookup.NANOSECOND));
		assertEquals(49_806_000, duration(max).longValueIn(UnitLookup.NANOSECOND));
		assertEquals(1, JfrToolkit.countItems(recording.getStored(TYPE + ".min")));
		assertSame(JfrToolkit.toItemList(items).get(1), min);
	}

	@Test
	void minimumTiesKeepOneMatchingEventAndMissingDurationsAreSkipped() throws Exception {
		IItemCollection items = TestItems.collection(TYPE, List.of(JfrAttributes.DURATION, ID),
				List.of(Map.of("id", "missing"), Map.of("duration", UnitLookup.MILLISECOND.quantity(1), "id", "first"),
						Map.of("duration", UnitLookup.NANOSECOND.quantity(1_000_000), "id", "tied")));
		Recording recording = TestItems.recording(items);
		AnalysisTools tools = new AnalysisTools();
		tools.recordings = TestItems.service(recording);
		String result = tools.aggregateEvents(TYPE, "min", null, null, null);
		assertEquals(1, JfrToolkit.countItems(recording.getStored(TYPE + ".min")));
		IItem minimum = JfrToolkit.toItemList(recording.getStored(TYPE + ".min")).getFirst();
		assertEquals(1_000_000, duration(minimum).longValueIn(UnitLookup.NANOSECOND));
		assertTrue(result.contains("Identifier: " + ID.getAccessor(type(minimum)).getMember(minimum)), result);
	}

	@SuppressWarnings("unchecked")
	private static org.openjdk.jmc.common.item.IType<IItem> type(IItem item) {
		return (org.openjdk.jmc.common.item.IType<IItem>) item.getType();
	}

	private static IQuantity duration(IItem item) {
		return JfrAttributes.DURATION.getAccessor(type(item)).getMember(item);
	}
}
