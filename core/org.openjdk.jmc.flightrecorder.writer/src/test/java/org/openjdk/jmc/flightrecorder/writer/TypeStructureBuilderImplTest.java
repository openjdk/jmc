package org.openjdk.jmc.flightrecorder.writer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.openjdk.jmc.flightrecorder.writer.api.Annotation;
import org.openjdk.jmc.flightrecorder.writer.api.TypeStructure;
import org.openjdk.jmc.flightrecorder.writer.api.TypedField;

class TypeStructureBuilderImplTest {
	private static final String FIELD_NAME = "field";
	private static final String CUSTOM_TYPE_NAME = "test.Type";
	private static final String ANNOTATION_TYPE_NAME = "jdk.jfr.Label";
	private static final String ANNOTATION_LABEL = "test.Label";

	private TypeStructureBuilderImpl instance;
	private TypeImpl stringType;
	private TypeImpl customType;
	private TypeImpl annotationType;

	@BeforeEach
	void setUp() {
		ConstantPools constantPools = Mockito.mock(ConstantPools.class);
		TypesImpl types = Mockito.mock(TypesImpl.class);

		stringType = new BuiltinType(1, TypesImpl.Builtin.STRING, constantPools, types);

		List<TypedFieldImpl> customTypeFields = Collections.singletonList(new TypedFieldImpl(stringType, "item"));
		customType = new CompositeTypeImpl(2, CUSTOM_TYPE_NAME, null,
				new TypeStructureImpl(customTypeFields, Collections.emptyList()), constantPools, types);

		annotationType = new CompositeTypeImpl(3, ANNOTATION_TYPE_NAME, Annotation.ANNOTATION_SUPER_TYPE_NAME,
				new TypeStructureImpl(Collections.singletonList(new TypedFieldImpl(stringType, "value")),
						Collections.emptyList()),
				constantPools, types);

		Mockito.when(types.getType(ArgumentMatchers.any(TypesImpl.Predefined.class))).thenReturn(stringType);
		Mockito.when(types.getType(ArgumentMatchers.matches(CUSTOM_TYPE_NAME.replace(".", "\\."))))
				.thenReturn(customType);
		Mockito.when(types.getType(ArgumentMatchers.matches(ANNOTATION_TYPE_NAME.replace(".", "\\."))))
				.thenReturn(annotationType);

		instance = new TypeStructureBuilderImpl(types);
	}

	@Test
	void addFieldPredefined() {
		TypeStructure structure = instance.addField(FIELD_NAME, TypesImpl.Builtin.STRING).build();
		assertEquals(1, structure.getFields().size());
		assertEquals(0, structure.getAnnotations().size());

		TypedField field = structure.getFields().get(0);
		assertEquals(FIELD_NAME, field.getName());
		assertEquals(stringType, field.getType());
	}

	@Test
	void testAddFieldCustom() {
		TypeStructure structure = instance.addField(FIELD_NAME, customType).build();
		assertEquals(1, structure.getFields().size());

		TypedField field = structure.getFields().get(0);
		assertEquals(FIELD_NAME, field.getName());
		assertEquals(customType, field.getType());
	}

	@Test
	void addFieldNullName() {
		assertThrows(NullPointerException.class, () -> instance.addField(null, TypesImpl.Builtin.STRING));
	}

	@Test
	void addFieldNullType() {
		assertThrows(NullPointerException.class, () -> instance.addField(FIELD_NAME, (TypesImpl.Predefined) null));
		assertThrows(NullPointerException.class, () -> instance.addField(FIELD_NAME, (TypeImpl) null));
	}

	@Test
	void addAnnotation() {
		TypeStructure structure = instance.addAnnotation(annotationType, ANNOTATION_LABEL).build();
		assertEquals(0, structure.getFields().size());
		assertEquals(1, structure.getAnnotations().size());

		Annotation annotation = structure.getAnnotations().get(0);
		assertEquals(annotationType, annotation.getType());
		assertEquals(ANNOTATION_LABEL, annotation.getValue());
	}

	@Test
	void addAnnotationNullValue() {
		TypeStructure structure = instance.addAnnotation(annotationType).build();
		assertEquals(0, structure.getFields().size());
		assertEquals(1, structure.getAnnotations().size());

		Annotation annotation = structure.getAnnotations().get(0);
		assertEquals(annotationType, annotation.getType());
		assertNull(annotation.getValue());
	}
}
