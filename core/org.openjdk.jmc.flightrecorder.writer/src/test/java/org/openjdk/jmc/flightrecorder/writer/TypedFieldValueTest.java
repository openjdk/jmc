package org.openjdk.jmc.flightrecorder.writer;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TypedFieldValueTest {
	private TypesImpl types;

	@BeforeEach
	void setup() {
		types = new TypesImpl(new MetadataImpl(new ConstantPools()));
	}

	@Test
	void testArrayForScalarField() {
		TypeImpl type = types.getType(TypesImpl.Builtin.STRING);
		TypedFieldImpl field = new TypedFieldImpl(type, "field");
		TypedValueImpl value = type.asValue("hello");

		assertThrows(IllegalArgumentException.class,
				() -> new TypedFieldValueImpl(field, new TypedValueImpl[] {value, value}));
	}

	@Test
	void testScalarValue() {
		TypeImpl type = types.getType(TypesImpl.Builtin.STRING);
		TypedFieldImpl field = new TypedFieldImpl(type, "field");
		TypedValueImpl value = type.asValue("hello");

		TypedFieldValueImpl instance = new TypedFieldValueImpl(field, value);

		assertNotNull(instance.getValue());
		assertEquals(field, instance.getField());
		assertEquals(value, instance.getValue());
		assertThrows(IllegalArgumentException.class, instance::getValues);
	}

	@Test
	void testArrayValue() {
		TypeImpl type = types.getType(TypesImpl.Builtin.STRING);
		TypedFieldImpl field = new TypedFieldImpl(type, "field", true);
		TypedValueImpl value1 = type.asValue("hello");
		TypedValueImpl value2 = type.asValue("world");

		TypedFieldValueImpl instance = new TypedFieldValueImpl(field, new TypedValueImpl[] {value1, value2});

		assertNotNull(instance.getValues());
		assertEquals(field, instance.getField());
		assertArrayEquals(new TypedValueImpl[] {value1, value2}, instance.getValues());
		assertThrows(IllegalArgumentException.class, instance::getValue);
	}
}
