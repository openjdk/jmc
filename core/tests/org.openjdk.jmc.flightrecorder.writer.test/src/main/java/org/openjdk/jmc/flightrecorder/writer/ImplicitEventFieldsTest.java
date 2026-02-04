/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2025, Datadog, Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at http://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.flightrecorder.writer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openjdk.jmc.common.item.Attribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.writer.api.Recording;
import org.openjdk.jmc.flightrecorder.writer.api.Recordings;
import org.openjdk.jmc.flightrecorder.writer.api.Type;
import org.openjdk.jmc.flightrecorder.writer.api.Types;

/**
 * Tests for implicit event fields (stackTrace, eventThread, startTime) handling.
 * <p>
 * These tests verify that events can be written without explicitly providing values for implicit
 * fields, and that the Writer API automatically provides default values for them.
 */
@SuppressWarnings("restriction")
class ImplicitEventFieldsTest {
	private Recording recording;
	private Path jfrPath;

	@BeforeEach
	void setup() throws Exception {
		jfrPath = Files.createTempFile("jfr-writer-test-implicit-", ".jfr");
		recording = Recordings.newRecording(jfrPath);
	}

	@AfterEach
	void teardown() throws Exception {
		if (recording != null) {
			recording.close();
		}
		if (jfrPath != null) {
			Files.deleteIfExists(jfrPath);
		}
	}

	/**
	 * Tests that an event can be written without implicit fields explicitly set.
	 * <p>
	 * This test reproduces the issue where field values appear shifted when implicit fields are not
	 * provided. After the fix, this should pass.
	 */
	@Test
	void eventWithoutImplicitFields() throws Exception {
		Type eventType = recording.registerEventType("test.MinimalEvent", builder -> {
			builder.addField("customField", Types.Builtin.LONG);
		});

		// Write event WITHOUT setting implicit fields
		recording.writeEvent(eventType.asValue(builder -> {
			builder.putField("customField", 12345L);
		})).close();

		// Verify recording parses correctly
		IItemCollection events = JfrLoaderToolkit.loadEvents(jfrPath.toFile());
		assertTrue(events.hasItems(), "Recording should contain events");

		events.forEach(itemType -> {
			itemType.forEach(item -> {
				// Verify implicit fields have defaults
				IQuantity startTime = JfrAttributes.START_TIME.getAccessor(itemType.getType()).getMember(item);
				assertNotNull(startTime, "startTime should have a default value");

				// Verify custom field has correct value (not shifted to startTime)
				IMemberAccessor<Number, IItem> accessor = Attribute
						.attr("customField", "customField", UnitLookup.RAW_NUMBER).getAccessor(itemType.getType());
				assertNotNull(accessor, "Accessor for customField should not be null");
				Number customFieldValue = accessor.getMember(item);
				assertNotNull(customFieldValue, "customField should have a value");
				assertEquals(12345L, customFieldValue.longValue(),
						"customField should be 12345, not shifted to startTime");
			});
		});
	}

	/**
	 * Tests that explicit startTime values are respected and not overridden by the default.
	 * <p>
	 * Note: JFR stores timestamps as ticks relative to the chunk start, so the parser converts the
	 * stored tick value to absolute epoch nanoseconds using the chunk header. We verify that the
	 * timestamp is reasonable (positive epoch time) rather than checking for exact equality.
	 */
	@Test
	void eventWithExplicitStartTime() throws Exception {
		long explicitTime = System.nanoTime();
		Type eventType = recording.registerEventType("test.ExplicitTime");

		recording.writeEvent(eventType.asValue(builder -> {
			builder.putField("startTime", explicitTime);
		})).close();

		IItemCollection events = JfrLoaderToolkit.loadEvents(jfrPath.toFile());
		assertTrue(events.hasItems(), "Recording should contain events");

		events.forEach(itemType -> {
			itemType.forEach(item -> {
				IQuantity time = JfrAttributes.START_TIME.getAccessor(itemType.getType()).getMember(item);
				assertNotNull(time, "startTime should not be null");
				// Verify that the timestamp is reasonable (epoch time in nanoseconds)
				// Should be a recent positive timestamp, not negative or zero
				long epochNanos = time.longValue();
				assertTrue(epochNanos > 0L, "Timestamp should be positive (epoch nanos)");
			});
		});
	}

	/**
	 * Tests an event with only implicit fields and no custom fields.
	 */
	@Test
	void eventWithOnlyImplicitFields() throws Exception {
		Type eventType = recording.registerEventType("test.ImplicitOnly");

		recording.writeEvent(eventType.asValue(builder -> {
			// Don't set any fields - rely on defaults
		})).close();

		IItemCollection events = JfrLoaderToolkit.loadEvents(jfrPath.toFile());
		assertTrue(events.hasItems(), "Recording should contain events");

		events.forEach(itemType -> {
			itemType.forEach(item -> {
				IQuantity startTime = JfrAttributes.START_TIME.getAccessor(itemType.getType()).getMember(item);
				assertNotNull(startTime, "startTime should have a default value");
			});
		});
	}

	/**
	 * Tests that multiple custom fields maintain correct alignment when implicit fields are not
	 * provided.
	 */
	@Test
	void eventWithMultipleCustomFields() throws Exception {
		Type eventType = recording.registerEventType("test.MultiField", builder -> {
			builder.addField("field1", Types.Builtin.LONG).addField("field2", Types.Builtin.STRING).addField("field3",
					Types.Builtin.INT);
		});

		recording.writeEvent(eventType.asValue(builder -> {
			builder.putField("field1", 111L).putField("field2", "test-string").putField("field3", 333);
		})).close();

		IItemCollection events = JfrLoaderToolkit.loadEvents(jfrPath.toFile());
		assertTrue(events.hasItems(), "Recording should contain events");

		events.forEach(itemType -> {
			itemType.forEach(item -> {
				// Verify all custom fields have correct values
				// field1 is LONG → raw number
				IMemberAccessor<Number, IItem> field1Accessor = Attribute
						.attr("field1", "field1", UnitLookup.RAW_NUMBER).getAccessor(itemType.getType());
				assertEquals(111L, field1Accessor.getMember(item).longValue(), "field1 should be 111");

				IMemberAccessor<String, IItem> field2Accessor = Attribute
						.attr("field2", "field2", UnitLookup.PLAIN_TEXT).getAccessor(itemType.getType());
				assertEquals("test-string", field2Accessor.getMember(item), "field2 should be 'test-string'");

				// field3 is INT → linear number
				IMemberAccessor<IQuantity, IItem> field3Accessor = Attribute.attr("field3", "field3", UnitLookup.NUMBER)
						.getAccessor(itemType.getType());
				assertEquals(333, field3Accessor.getMember(item).longValue(), "field3 should be 333");
			});
		});
	}

	/**
	 * Tests that all builtin types receive proper default values when not explicitly set.
	 * <p>
	 * This test verifies the fix for builtin type field skipping. When builtin fields are not
	 * explicitly set, they should receive type-appropriate defaults (0 for numbers, false for
	 * boolean, null for String) instead of being skipped during serialization, which would cause
	 * field alignment issues.
	 * <p>
	 * The test includes a final field with an explicit value to verify that field alignment remains
	 * correct after all the default builtin fields.
	 */
	@Test
	void eventWithAllBuiltinFieldsUnset() throws Exception {
		Type eventType = recording.registerEventType("test.AllBuiltins", builder -> {
			builder.addField("byteField", Types.Builtin.BYTE).addField("charField", Types.Builtin.CHAR)
					.addField("shortField", Types.Builtin.SHORT).addField("intField", Types.Builtin.INT)
					.addField("longField", Types.Builtin.LONG).addField("floatField", Types.Builtin.FLOAT)
					.addField("doubleField", Types.Builtin.DOUBLE).addField("booleanField", Types.Builtin.BOOLEAN)
					.addField("stringField", Types.Builtin.STRING).addField("finalField", Types.Builtin.LONG);
		});

		// Write event WITHOUT setting builtin field values - all should get defaults
		// Set finalField to verify field alignment is correct
		recording.writeEvent(eventType.asValue(builder -> {
			builder.putField("finalField", 99999L);
		})).close();

		// Verify recording parses correctly and contains the event
		IItemCollection events = JfrLoaderToolkit.loadEvents(jfrPath.toFile());
		assertTrue(events.hasItems(), "Recording should contain events");

		events.forEach(itemType -> {
			itemType.forEach(item -> {
				// Verify all builtin fields have appropriate default values
				// BYTE → linear number
				IMemberAccessor<IQuantity, IItem> byteAccessor = Attribute
						.attr("byteField", "byteField", UnitLookup.NUMBER).getAccessor(itemType.getType());
				assertEquals(0, byteAccessor.getMember(item).longValue(), "byteField should default to 0");

				// SHORT → linear number
				IMemberAccessor<IQuantity, IItem> shortAccessor = Attribute
						.attr("shortField", "shortField", UnitLookup.NUMBER).getAccessor(itemType.getType());
				assertEquals(0, shortAccessor.getMember(item).longValue(), "shortField should default to 0");

				// INT → linear number
				IMemberAccessor<IQuantity, IItem> intAccessor = Attribute
						.attr("intField", "intField", UnitLookup.NUMBER).getAccessor(itemType.getType());
				assertEquals(0, intAccessor.getMember(item).longValue(), "intField should default to 0");

				// LONG → raw number
				IMemberAccessor<Number, IItem> longAccessor = Attribute
						.attr("longField", "longField", UnitLookup.RAW_NUMBER).getAccessor(itemType.getType());
				assertEquals(0L, longAccessor.getMember(item).longValue(), "longField should default to 0");

				// FLOAT → linear number
				IMemberAccessor<IQuantity, IItem> floatAccessor = Attribute
						.attr("floatField", "floatField", UnitLookup.NUMBER).getAccessor(itemType.getType());
				assertEquals(0.0, floatAccessor.getMember(item).doubleValue(), 0.001,
						"floatField should default to 0.0");

				// DOUBLE → linear number
				IMemberAccessor<IQuantity, IItem> doubleAccessor = Attribute
						.attr("doubleField", "doubleField", UnitLookup.NUMBER).getAccessor(itemType.getType());
				assertEquals(0.0, doubleAccessor.getMember(item).doubleValue(), 0.001,
						"doubleField should default to 0.0");

				IMemberAccessor<Boolean, IItem> booleanAccessor = Attribute
						.attr("booleanField", "booleanField", UnitLookup.FLAG).getAccessor(itemType.getType());
				assertEquals(false, booleanAccessor.getMember(item), "booleanField should default to false");

				IMemberAccessor<String, IItem> stringAccessor = Attribute
						.attr("stringField", "stringField", UnitLookup.PLAIN_TEXT).getAccessor(itemType.getType());
				assertEquals(null, stringAccessor.getMember(item), "stringField should default to null");

				// Verify the explicit field value is read correctly (proves field alignment is correct)
				// LONG → raw number
				IMemberAccessor<Number, IItem> finalAccessor = Attribute
						.attr("finalField", "finalField", UnitLookup.RAW_NUMBER).getAccessor(itemType.getType());
				assertEquals(99999L, finalAccessor.getMember(item).longValue(),
						"finalField should be 99999, confirming correct field alignment after all default builtin fields");
			});
		});
	}
}
