package org.openjdk.jmc.flightrecorder.writer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openjdk.jmc.flightrecorder.writer.api.Annotation;

class AnnotationTest {
	private TypesImpl types;

	@BeforeEach
	void setup() {
		ConstantPools constantPools = new ConstantPools();
		MetadataImpl metadata = new MetadataImpl(constantPools);
		types = new TypesImpl(metadata);
	}

	@Test
	void validAnnotationTypeNullValue() {
		TypeImpl type = types.getType(TypesImpl.JDK.ANNOTATION_LABEL);
		Annotation annotation = new Annotation(type, null);
		assertNotNull(annotation);
		assertNull(annotation.getValue());
		assertEquals(type, annotation.getType());
	}

	@Test
	void validAnnotationTypeWithValue() {
		String value = "value";
		TypeImpl type = types.getType(TypesImpl.JDK.ANNOTATION_LABEL);
		Annotation annotation = new Annotation(type, value);
		assertNotNull(annotation);
		assertEquals(value, annotation.getValue());
		assertEquals(type, annotation.getType());
	}

	@Test
	void invalidAnnotationType() {
		TypeImpl type = types.getType(TypesImpl.Builtin.STRING);
		assertThrows(IllegalArgumentException.class, () -> new Annotation(type, null));
	}
}
