package org.openjdk.jmc.flightrecorder.writer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.openjdk.jmc.flightrecorder.writer.api.Annotation;

class TypedFieldBuilderImplTest {
	private static final String FIELD_NAME = "field";
	private static final String CUSTOM_TYPE_NAME = "test.Type";
	private static final String ANNOTATION_TYPE_NAME = "jdk.jfr.Label";
	private static final String ANNOTATION_LABEL = "test.Label";

	private TypedFieldBuilderImpl instance;
	private TypeImpl stringType;
	private TypeImpl customType;
	private TypeImpl annotationType;

	@BeforeEach
	void setUp() {
		TypesImpl types = Mockito.mock(TypesImpl.class);
		ConstantPools constantPools = Mockito.mock(ConstantPools.class);

		stringType = new BuiltinType(1, TypesImpl.Builtin.STRING, constantPools, types);

		List<TypedFieldImpl> customTypeFields = Collections.singletonList(new TypedFieldImpl(stringType, "item"));
		customType = new CompositeTypeImpl(2, CUSTOM_TYPE_NAME, null,
				new TypeStructureImpl(customTypeFields, Collections.emptyList()), constantPools, types);

		annotationType = new CompositeTypeImpl(3, ANNOTATION_TYPE_NAME, Annotation.ANNOTATION_SUPER_TYPE_NAME,
				new TypeStructureImpl(Collections.singletonList(new TypedFieldImpl(stringType, "value")),
						Collections.emptyList()),
				constantPools, types);

		Mockito.when(types.getType(ArgumentMatchers.any(TypesImpl.Predefined.class)))
				.thenAnswer(i -> ((TypesImpl.Predefined) i.getArgument(0)).getTypeName().equals(ANNOTATION_TYPE_NAME)
						? annotationType : stringType);
		Mockito.when(types.getType(ArgumentMatchers.matches(CUSTOM_TYPE_NAME.replace(".", "\\."))))
				.thenReturn(customType);
		Mockito.when(types.getType(ArgumentMatchers.matches(ANNOTATION_TYPE_NAME.replace(".", "\\."))))
				.thenReturn(annotationType);

		instance = new TypedFieldBuilderImpl(FIELD_NAME, customType, types);
	}

	@Test
	void addAnnotationNullValue() {
		TypedFieldImpl field = instance.addAnnotation(annotationType).build();

		assertNotNull(field);
		assertEquals(FIELD_NAME, field.getName());
		assertEquals(customType, field.getType());

		assertEquals(1, field.getAnnotations().size());
		Annotation annotation = field.getAnnotations().get(0);
		assertEquals(annotationType, annotation.getType());
		assertNull(annotation.getValue());
	}

	@Test
	void addPredefinedAnnotationNullValue() {
		TypedFieldImpl field = instance.addAnnotation(TypesImpl.JDK.ANNOTATION_LABEL).build();

		assertNotNull(field);
		assertEquals(FIELD_NAME, field.getName());
		assertEquals(customType, field.getType());

		assertEquals(1, field.getAnnotations().size());
		Annotation annotation = field.getAnnotations().get(0);
		assertEquals(annotationType, annotation.getType());
		assertNull(annotation.getValue());
	}

	@Test
	void addAnnotationValue() {
		TypedFieldImpl field = instance.addAnnotation(annotationType, ANNOTATION_LABEL).build();

		assertNotNull(field);
		assertEquals(FIELD_NAME, field.getName());
		assertEquals(customType, field.getType());

		assertEquals(1, field.getAnnotations().size());
		Annotation annotation = field.getAnnotations().get(0);
		assertEquals(annotationType, annotation.getType());
		assertEquals(ANNOTATION_LABEL, annotation.getValue());
	}

	@Test
	void addPredefinedAnnotationValue() {
		TypedFieldImpl field = instance.addAnnotation(TypesImpl.JDK.ANNOTATION_LABEL, ANNOTATION_LABEL).build();

		assertNotNull(field);
		assertEquals(FIELD_NAME, field.getName());
		assertEquals(customType, field.getType());

		assertEquals(1, field.getAnnotations().size());
		Annotation annotation = field.getAnnotations().get(0);
		assertEquals(annotationType, annotation.getType());
		assertEquals(ANNOTATION_LABEL, annotation.getValue());
	}

	@Test
	void asArray() {
		TypedFieldImpl field = instance.asArray().build();

		assertNotNull(field);
		assertTrue(field.isArray());
	}
}
