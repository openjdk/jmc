package org.openjdk.jmc.flightrecorder.writer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openjdk.jmc.flightrecorder.writer.api.Annotation;

import java.io.ByteArrayOutputStream;

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
	}

	@Test
	void registerEventTypeNew() {
		String name = "custom.Event";
		TypeImpl eventType = recording.registerEventType(name);
		assertNotNull(eventType);
		assertEquals(name, eventType.getTypeName());
		assertEquals("jdk.jfr.Event", eventType.getSupertype());
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
	}

	@Test
	void registerAnnotationTypeNew() {
		String name = "custom.Annotation";
		TypeImpl annotationType = recording.registerAnnotationType(name);
		assertNotNull(annotationType);
		assertEquals(name, annotationType.getTypeName());
		assertEquals(Annotation.ANNOTATION_SUPER_TYPE_NAME, annotationType.getSupertype());
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
