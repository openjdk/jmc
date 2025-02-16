/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, 2025, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.writer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import jdk.jfr.Event;
import jdk.jfr.Enabled;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Registered;
import jdk.jfr.StackTrace;
import jdk.jfr.Timestamp;
import jdk.jfr.Timespan;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.openjdk.jmc.common.item.Attribute;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.testutils.parser.ChunkHeader;
import org.openjdk.jmc.flightrecorder.testutils.parser.ChunkParserListener;
import org.openjdk.jmc.flightrecorder.testutils.parser.StreamingChunkParser;
import org.openjdk.jmc.flightrecorder.writer.api.Annotation;
import org.openjdk.jmc.flightrecorder.writer.api.RecordingSettings;
import org.openjdk.jmc.flightrecorder.writer.api.TypedField;
import org.openjdk.jmc.flightrecorder.writer.api.Types;

class RecordingImplTest {
	@Label("Simple Test Event")
	@Registered(true)
	@Enabled(false)
	public static final class SimpleTestEvent extends Event {
		@Label("field")
		public String fld;

		@Timestamp
		public long timestamp;

		@Timespan
		public long timespan;

		@Label("static")
		public static int staticFld;
		@Label("transient")
		public transient char transientFld;
	}

	@StackTrace(true)
	public static final class EventWithStackTrace extends Event {

	}

	@Label("Named Test Event")
	@Name("jmc.TestEvent")
	public static final class TestEventWithName extends Event {
		@Label("field")
		public String fld;
	}

	public static final class EventWithOverrides extends Event {
		@Name("startTime")
		@Label("overridden timestamp")
		@Timestamp
		public long timestamp;

		@Label("overridden thread")
		public Thread eventThread;
	}

	private RecordingImpl recording;
	private ByteArrayOutputStream bos;

	@BeforeEach
	void setUp() {
		bos = new ByteArrayOutputStream();
		recording = new RecordingImpl(bos, new RecordingSettings());
	}

	@AfterEach
	void tearDown() throws Exception {
		recording.close();
	}

	@ParameterizedTest
	@MethodSource("recordingSettings")
	void testChunkHeaderTimestamps(RecordingSettings settings) throws IOException {
		StreamingChunkParser parser = new StreamingChunkParser();
		ByteArrayOutputStream bos1 = new ByteArrayOutputStream();
		RecordingImpl recording1 = new RecordingImpl(bos1, settings);
		recording1.close();

		parser.parse(new ByteArrayInputStream(bos1.toByteArray()), new ChunkParserListener() {
			@Override
			public boolean onChunkStart(int chunkIndex, ChunkHeader header) {
				assertTrue(header.startNanos > -1);
				assertTrue(header.startTicks > -1);
				assertTrue(header.duration > -1);

				assertTrue(settings.getStartTimestamp() == -1 || settings.getStartTimestamp() == header.startNanos);
				assertTrue(settings.getStartTicks() == -1 || settings.getStartTicks() == header.startTicks);
				assertTrue(settings.getDuration() == -1 || settings.getDuration() == header.duration);
				// skip the rest
				return false;
			}
		});
	}

	private static Stream<Arguments> recordingSettings() {
		return Stream.of(Arguments.of(new RecordingSettings()),
				Arguments.of(new RecordingSettings(0, 0, 500_000_000L, true)));
	}

	@Test
	void registerEventTypeNullName() {
		assertThrows(IllegalArgumentException.class, () -> recording.registerEventType((String) null));
	}

	@Test
	void registerEventTypeNullCallback() {
		assertThrows(IllegalArgumentException.class, () -> recording.registerEventType("name", null));
	}

	@Test
	void registerEventCallaback() {
		String name = "custom.Event";
		String fieldName = "field";
		TypeImpl eventType = recording.registerEventType(name, t -> {
			t.addField(fieldName, TypesImpl.Builtin.STRING);
		});
		assertNotNull(eventType);
		assertEquals(name, eventType.getTypeName());
		assertEquals("jdk.jfr.Event", eventType.getSupertype());
		assertNotNull(eventType.getField(fieldName));
		assertFalse(eventType.hasConstantPool());
	}

	@Test
	void registerEventTypeNew() {
		String name = "custom.Event";
		TypeImpl eventType = recording.registerEventType(name);
		assertNotNull(eventType);
		assertEquals(name, eventType.getTypeName());
		assertEquals("jdk.jfr.Event", eventType.getSupertype());
		assertFalse(eventType.hasConstantPool());
	}

	@Test
	void registerEventTypeExisting() {
		String name = "custom.Event";
		TypeImpl eventType = recording.registerEventType(name);

		TypeImpl eventType1 = recording.registerEventType(name);

		assertEquals(eventType, eventType1);
	}

	@Test
	void registerSimpleJfrEvent() {
		TypeImpl eventType = recording.registerEventType(SimpleTestEvent.class);
		assertNotNull(eventType);
		assertEquals(SimpleTestEvent.class.getSimpleName(), eventType.getTypeName());

		// make sure the following two fields are implicitly added and properly labelled
		assertFieldAnnotatedBy(eventType, "startTime", Timestamp.class.getName());
		assertNotNull(eventType.getField("eventThread"));

		assertFieldAnnotatedBy(eventType, "fld", Label.class.getName());
		assertFieldAnnotatedBy(eventType, "timestamp", Timestamp.class.getName());
		assertFieldAnnotatedBy(eventType, "timespan", Timespan.class.getName());

		// make sure that static and transient fields are ignored
		assertNull(eventType.getField("staticFld"));
		assertNull(eventType.getField("transientFld"));
	}

	@Test
	void registerEventWithStackTrace() {
		TypeImpl eventType = recording.registerEventType(EventWithStackTrace.class);
		assertNotNull(eventType);
		assertEquals(EventWithStackTrace.class.getSimpleName(), eventType.getTypeName());

		TypedField stackTrace = eventType.getField("stackTrace");
		assertNotNull(stackTrace);
		assertEquals(Types.JDK.STACK_TRACE.getTypeName(), stackTrace.getType().getTypeName());
	}

	@Test
	void registerEventWithOverrides() {
		TypeImpl eventType = recording.registerEventType(EventWithOverrides.class);
		assertNotNull(eventType);

		// make sure the @Timestamp annotation is preserved here
		assertFieldAnnotatedBy(eventType, "startTime", Timestamp.class.getName());

		// the overridden fields have extra @Label annotation
		assertFieldAnnotatedBy(eventType, "startTime", Label.class.getName());
		assertFieldAnnotatedBy(eventType, "eventThread", Label.class.getName());

	}

	@Test
	void registerJfrEventTypeWithCustomName() {
		TypeImpl eventType = recording.registerEventType(TestEventWithName.class);
		assertNotNull(eventType);
		assertEquals("jmc.TestEvent", eventType.getTypeName());
		List<Annotation> annotations = eventType.getAnnotations();
		assertNotNull(annotations);

		assertTypeAnnotatedBy(eventType, Label.class.getName());
		assertTypeNotAnnotatedBy(eventType, Name.class.getName());
	}

	private static void assertTypeAnnotatedBy(TypeImpl eventType, String expectedAnnotationType) {
		List<Annotation> annotations = eventType.getAnnotations();
		assertNotNull(annotations);
		assertTrue(annotations.stream().anyMatch(a -> a.getType().getTypeName().equals(expectedAnnotationType)),
				"Type '" + eventType.getTypeName() + "' is missing the expected @" + expectedAnnotationType
						+ " annotation");
	}

	private static void assertTypeNotAnnotatedBy(TypeImpl eventType, String expectedAnnotationType) {
		List<Annotation> annotations = eventType.getAnnotations();
		assertNotNull(annotations);
		assertTrue(annotations.stream().noneMatch(a -> a.getType().getTypeName().equals(expectedAnnotationType)),
				"Type '" + eventType.getTypeName() + "' is having unexpected @" + expectedAnnotationType
						+ " annotation");
	}

	private static void assertFieldAnnotatedBy(TypeImpl eventType, String fldName, String expectedAnnotationType) {
		TypedField fld = eventType.getField(fldName);
		assertNotNull(fld);
		List<Annotation> annotations = fld.getAnnotations();
		assertNotNull(annotations);
		assertTrue(annotations.stream().anyMatch(a -> a.getType().getTypeName().equals(expectedAnnotationType)),
				"Field '" + eventType.getTypeName() + "#" + fldName + "' is missing the expected @"
						+ expectedAnnotationType + " annotation");
	}

	@Test
	void writeJfrEvent() throws Exception {
		// attempt to write a simple user defined event without registering its type first
		SimpleTestEvent e = new SimpleTestEvent();
		e.fld = "hello";
		e.timespan = 10_000_000L;
		e.timestamp = 60_000_000L;
		e.transientFld = 'x';

		recording.writeEvent(e);
		recording.close();

		try (ByteArrayInputStream is = new ByteArrayInputStream(bos.toByteArray())) {
			IItemCollection events = JfrLoaderToolkit.loadEvents(is);
			assertNotNull(events);

			IAttribute<String> fldAttr = Attribute.attr("fld", "fld", UnitLookup.PLAIN_TEXT);
			IAttribute<IQuantity> timestampAttr = Attribute.attr("timestamp", "timestamp", UnitLookup.TIMESTAMP);
			IAttribute<IQuantity> timespanAttr = Attribute.attr("timespan", "timespan", UnitLookup.TIMESPAN);
			boolean eventTypeFound = false;
			for (IItemIterable lane : events) {
				IType<IItem> type = lane.getType();
				if (type.getIdentifier().equals(SimpleTestEvent.class.getSimpleName())) {
					var fldAccessor = fldAttr.getAccessor(type);
					var timestampAccessor = timestampAttr.getAccessor(type);
					var timespanAccessor = timespanAttr.getAccessor(type);

					assertNotNull(fldAccessor);
					assertNotNull(timestampAccessor);
					assertNotNull(timespanAccessor);

					eventTypeFound = true;
					assertEquals(1, lane.getItemCount());
					for (IItem event : lane) {
						assertEquals(e.fld, fldAccessor.getMember(event));
						assertEquals(e.timestamp, timestampAccessor.getMember(event).longValue());
						assertEquals(e.timespan, timespanAccessor.getMember(event).longValue());
					}
				}
			}
			assertTrue(eventTypeFound,
					"The event type " + SimpleTestEvent.class.getSimpleName() + " was not found in the recording");
		}

	}

	@Test
	void registerAnnotationTypeNullName() {
		assertThrows(IllegalArgumentException.class, () -> recording.registerAnnotationType(null));
	}

	@Test
	void registerAnnotationTypeNullCallback() {
		assertThrows(IllegalArgumentException.class, () -> recording.registerAnnotationType("name", null));
	}

	@Test
	void registerAnnotationTypeWithCallback() {
		String name = "custom.Annotation";
		String fieldName = "field";
		TypeImpl annotationType = recording.registerAnnotationType(name, t -> {
			t.addField(fieldName, TypesImpl.Builtin.STRING);
		});
		assertNotNull(annotationType);
		assertEquals(name, annotationType.getTypeName());
		assertEquals(Annotation.ANNOTATION_SUPER_TYPE_NAME, annotationType.getSupertype());
		assertEquals(1, annotationType.getFields().size());
		assertNotNull(annotationType.getField(fieldName));
		assertTrue(annotationType.hasConstantPool());
	}

	@Test
	void registerAnnotationTypeNew() {
		String name = "custom.Annotation";
		TypeImpl annotationType = recording.registerAnnotationType(name);
		assertNotNull(annotationType);
		assertEquals(name, annotationType.getTypeName());
		assertEquals(Annotation.ANNOTATION_SUPER_TYPE_NAME, annotationType.getSupertype());
		assertTrue(annotationType.hasConstantPool());
	}

	@Test
	void registerAnnotationTypeExisting() {
		String name = "custom.Annotation";
		TypeImpl annotationType = recording.registerAnnotationType(name);

		TypeImpl annotationType1 = recording.registerAnnotationType(name);

		assertEquals(annotationType, annotationType1);
	}

	@Test
	void registerTypeNulls() {
		assertThrows(IllegalArgumentException.class, () -> recording.registerType(null, builder -> {
		}));
		assertThrows(IllegalArgumentException.class, () -> recording.registerType("name", null));
		assertThrows(IllegalArgumentException.class, () -> recording.registerType(null, null));
		assertThrows(IllegalArgumentException.class, () -> recording.registerType(null, "super", builder -> {
		}));
		assertThrows(IllegalArgumentException.class, () -> recording.registerType("name", "super", null));
		assertThrows(IllegalArgumentException.class, () -> recording.registerType(null, "super", null));
		assertThrows(IllegalArgumentException.class, () -> recording.registerType(null, null, builder -> {
		}));
		assertThrows(IllegalArgumentException.class, () -> recording.registerType("name", null, null));
		assertThrows(IllegalArgumentException.class, () -> recording.registerType(null, null, null));
	}

	@ParameterizedTest
	@EnumSource(TypesImpl.JDK.class)
	void getBuiltinJDKType(TypesImpl.JDK target) {
		TypeImpl type = recording.getType(target);
		assertNotNull(type);
		assertTrue(type.hasConstantPool());
	}

	@ParameterizedTest
	@EnumSource(Types.Builtin.class)
	void getBuiltinType(Types.Builtin target) {
		TypeImpl type = recording.getType(target.getTypeName());
		assertNotNull(type);
		// only String 'primitive' values have constant pool associated with them
		assertEquals(target.isSame(Types.Builtin.STRING), type.hasConstantPool());
	}

	@Test
	void getNullType() {
		assertThrows(IllegalArgumentException.class, () -> recording.getType((TypesImpl.JDK) null));
		assertThrows(IllegalArgumentException.class, () -> recording.getType((String) null));
	}

	@Test
	void getInvalidType() {
		assertThrows(IllegalArgumentException.class, () -> recording.getType("Invalid type"));
	}

	@Test
	void getRegisteredType() {
		String typeName = "custom.Type";
		TypeImpl type = recording.registerType(typeName, builder -> {
		});

		TypeImpl type1 = recording.getType(typeName);
		assertNotNull(type1);
		assertEquals(type, type1);
	}
}
