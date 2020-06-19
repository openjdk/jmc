package org.openjdk.jmc.flightrecorder.writer;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openjdk.jmc.flightrecorder.writer.api.Annotation;

class CompositeTypeImplTest {
	private static final String TYPE_NAME = "test.Type";
	private static final String FIELD_NAME = "field1";
	private static final String PARENT_FIELD_NAME = "parent";
	private static final String FIELD_VALUE = "hello";
	public static final int TYPE_ID = 1;
	private CompositeTypeImpl instance;
	private TypesImpl types;

	@BeforeEach
	void setUp() {
		ConstantPools constantPools = new ConstantPools();
		MetadataImpl metadata = new MetadataImpl(constantPools);
		types = new TypesImpl(metadata);

		List<TypedFieldImpl> fields = new ArrayList<>();
		List<Annotation> annotations = new ArrayList<>();

		fields.add(new TypedFieldImpl(types.getType(TypesImpl.Builtin.STRING), FIELD_NAME));
		fields.add(new TypedFieldImpl(SelfType.INSTANCE, PARENT_FIELD_NAME));
		annotations.add(new Annotation(types.getType(TypesImpl.JDK.ANNOTATION_NAME), "test.Type"));

		TypeStructureImpl structure = new TypeStructureImpl(fields, annotations);
		instance = new CompositeTypeImpl(TYPE_ID, TYPE_NAME, null, structure, constantPools, types);
	}

	@Test
	void typeSelfReferenceResolved() {
		for (TypedFieldImpl field : instance.getFields()) {
			if (field.getName().equals("parent")) {
				assertNotEquals(SelfType.INSTANCE, field.getType());
			}
		}
	}

	@Test
	void isBuiltin() {
		assertFalse(instance.isBuiltin());
	}

	@Test
	void getFields() {
		assertEquals(2, instance.getFields().size());
	}

	@Test
	void getField() {
		assertNotNull(instance.getField(FIELD_NAME));
		assertNotNull(instance.getField(PARENT_FIELD_NAME));
	}

	@Test
	void isResolved() {
		assertTrue(instance.isResolved());
	}

	@Test
	void nullValue() {
		TypedValueImpl value = instance.nullValue();
		assertNotNull(value);
		assertTrue(value.isNull());
		assertEquals(value, instance.nullValue());
	}

	@Test
	void getAnnotations() {
		assertEquals(1, instance.getAnnotations().size());
	}

	@Test
	void canAccept() {
		for (TypesImpl.Builtin builtin : TypesImpl.Builtin.values()) {
			for (Object builtinVal : TypeUtils.getBuiltinValues(builtin)) {
				// null is generally accepted
				assertEquals(builtinVal == null, instance.canAccept(builtinVal));
			}
		}
		TypedValueImpl value = instance.asValue(builder -> {
			builder.putField(FIELD_NAME, FIELD_VALUE);
		});
		assertTrue(instance.canAccept(value));
	}

	@Test
	void getId() {
		assertEquals(TYPE_ID, instance.getId());
	}

	@Test
	void getTypeName() {
		assertEquals(TYPE_NAME, instance.getTypeName());
	}

	@Test
	void getSupertype() {
		assertNull(instance.getSupertype());
	}

	@Test
	void isUsedBySimple() {
		TypeImpl other = types.getType(TypesImpl.Builtin.STRING);

		// has a self-referenced field
		assertTrue(instance.isUsedBy(instance));
		assertFalse(instance.isUsedBy(other));
		assertTrue(other.isUsedBy(instance));
	}

	@Test
	void isUsedByComplex() {
		TypeImpl target = types.getType(TypesImpl.Builtin.INT);

		TypeImpl main = types.getOrAdd("custom.Main", type -> {
			type.addField("parent", type.selfType()).addField("field", TypesImpl.Builtin.STRING).addField("other",
					types.getType("custom.Other", true));
		});

		TypeImpl other = types.getOrAdd("custom.Other", type -> {
			type.addField("loopback", main).addField("field", TypesImpl.Builtin.INT);
		});
		types.resolveAll();

		assertFalse(instance.isUsedBy(null));
		// has a self-referenced field
		assertTrue(main.isUsedBy(main));
		assertTrue(main.isUsedBy(other));
		assertTrue(target.isUsedBy(main));
	}

	@Test
	void testEquality() {
		TypeImpl type1 = types.getType(TypesImpl.Builtin.STRING);
		TypeImpl type2 = types.getType(TypesImpl.Builtin.INT);
		TypeImpl type3 = types.getOrAdd("type.Custom", t -> {
			t.addField("field1", types.getType(TypesImpl.Builtin.STRING)).addField("field2",
					types.getType(TypesImpl.Builtin.STRING));
		});

		assertEquals(type1, type1);
		assertEquals(type2, type2);
		assertEquals(type3, type3);
		assertNotEquals(type1, type2);
		assertNotEquals(type1, type3);
		assertNotEquals(type2, type1);
		assertNotEquals(type2, type3);
		assertNotEquals(type3, type1);
		assertNotEquals(type3, type2);
	}
}
