package org.openjdk.jmc.flightrecorder.writer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openjdk.jmc.flightrecorder.writer.api.Annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypedFieldImplTest {
	private static final String FIELD_NAME = "instance";

	private TypesImpl types;

	@BeforeEach
	void setUp() {
		ConstantPools constantPools = new ConstantPools();
		MetadataImpl metadata = new MetadataImpl(constantPools);
		types = new TypesImpl(metadata);
	}

	@Test
	void getCustomTypeNoAnnotations() {
		TypeImpl type = types.getOrAdd("custom.Type", t -> {
			t.addField("field", TypesImpl.Builtin.STRING).addAnnotation(types.getType(TypesImpl.JDK.ANNOTATION_LABEL),
					"custom.Type");
		});
		TypedFieldImpl instance = new TypedFieldImpl(type, FIELD_NAME);

		assertEquals(FIELD_NAME, instance.getName());
		assertEquals(type, instance.getType());
		assertEquals(0, instance.getAnnotations().size());
		assertFalse(instance.isArray());
	}

	@Test
	void getCustomTypeAnnotations() {
		TypeImpl type = types.getOrAdd("custom.Type", t -> {
			t.addField("field", TypesImpl.Builtin.STRING).addAnnotation(types.getType(TypesImpl.JDK.ANNOTATION_LABEL),
					"custom.Type");
		});
		TypedFieldImpl instance = new TypedFieldImpl(type, FIELD_NAME, false,
				Collections.singletonList(new Annotation(types.getType(TypesImpl.JDK.ANNOTATION_LABEL), "field")));

		assertEquals(FIELD_NAME, instance.getName());
		assertEquals(type, instance.getType());
		assertEquals(1, instance.getAnnotations().size());
		assertFalse(instance.isArray());
	}

	@Test
	void getCustomTypeArrayNoAnnotations() {
		TypeImpl type = types.getOrAdd("custom.Type", t -> {
			t.addField("field", TypesImpl.Builtin.STRING).addAnnotation(types.getType(TypesImpl.JDK.ANNOTATION_LABEL),
					"custom.Type");
		});
		TypedFieldImpl instance = new TypedFieldImpl(type, FIELD_NAME, true);

		assertEquals(FIELD_NAME, instance.getName());
		assertEquals(type, instance.getType());
		assertEquals(0, instance.getAnnotations().size());
		assertTrue(instance.isArray());
	}

	@Test
	void getCustomTypeArrayAnnotations() {
		TypeImpl type = types.getOrAdd("custom.Type", t -> {
			t.addField("field", TypesImpl.Builtin.STRING).addAnnotation(types.getType(TypesImpl.JDK.ANNOTATION_LABEL),
					"custom.Type");
		});
		TypedFieldImpl instance = new TypedFieldImpl(type, FIELD_NAME, true,
				Collections.singletonList(new Annotation(types.getType(TypesImpl.JDK.ANNOTATION_LABEL), "field")));

		assertEquals(FIELD_NAME, instance.getName());
		assertEquals(type, instance.getType());
		assertEquals(1, instance.getAnnotations().size());
		assertTrue(instance.isArray());
	}

	@Test
	void equality() {
		TypeImpl[] fieldTypes = new TypeImpl[] {types.getOrAdd("custom.Type", t -> {
			t.addField("field", TypesImpl.Builtin.STRING).addAnnotation(types.getType(TypesImpl.JDK.ANNOTATION_LABEL),
					"custom.Type");
		}), types.getType(TypesImpl.Builtin.STRING)};

		String[] fieldNames = new String[] {"field1", "field2"};
		boolean[] arrayFlags = new boolean[] {true, false};
		List<List<Annotation>> annotations = Arrays.asList(Collections.emptyList(),
				Collections.singletonList(new Annotation(types.getType(TypesImpl.JDK.ANNOTATION_LABEL), "field")));

		List<TypedFieldImpl> fields = new ArrayList<>();
		for (TypeImpl fieldType : fieldTypes) {
			for (String fieldName : fieldNames) {
				for (boolean arrayFlag : arrayFlags) {
					for (List<Annotation> annotationList : annotations) {
						fields.add(new TypedFieldImpl(fieldType, fieldName, arrayFlag, annotationList));
					}
				}
			}
		}

		for (TypedFieldImpl field1 : fields) {
			// make the code coverage check happy
			assertFalse(field1.equals(10));

			assertFalse(field1.equals(null));
			for (TypedFieldImpl field2 : fields) {
				assertEquals(field1 == field2, field1.equals(field2));

				// keep the hashCode-equals contract
				assertTrue(field1.hashCode() != field2.hashCode() || field1.equals(field2));
			}
		}
	}
}
