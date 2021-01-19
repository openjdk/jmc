/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, Datadog, Inc. All rights reserved.
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openjdk.jmc.flightrecorder.writer.api.Annotation;
import org.openjdk.jmc.flightrecorder.writer.api.Types;

class RecordingImplTest {
	private RecordingImpl recording;
	private ByteArrayOutputStream bos;

	@BeforeEach
	void setUp() {
		bos = new ByteArrayOutputStream();
		recording = new RecordingImpl(bos);
	}

	@AfterEach
	void tearDown() throws Exception {
		recording.close();
	}

	@Test
	void registerEventTypeNullName() {
		assertThrows(IllegalArgumentException.class, () -> recording.registerEventType(null));
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
