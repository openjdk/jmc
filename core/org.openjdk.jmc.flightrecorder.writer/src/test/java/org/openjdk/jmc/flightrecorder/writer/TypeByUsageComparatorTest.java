package org.openjdk.jmc.flightrecorder.writer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TypeByUsageComparatorTest {
	private TypesImpl types;
	private org.openjdk.jmc.flightrecorder.writer.util.TypeByUsageComparator instance;

	@BeforeEach
	void setup() {
		ConstantPools constantPools = new ConstantPools();
		MetadataImpl metadata = new MetadataImpl(constantPools);
		types = new TypesImpl(metadata);

		instance = new org.openjdk.jmc.flightrecorder.writer.util.TypeByUsageComparator();
	}

	@Test
	void compare() {
		TypeImpl type1 = types.getType(TypesImpl.Builtin.STRING);
		TypeImpl type2 = types.getType(TypesImpl.JDK.CLASS);

		assertEquals(0, instance.compare(type1, type1));
		assertEquals(0, instance.compare(type2, type2));
		assertEquals(0, instance.compare(null, null));
		assertEquals(1, instance.compare(type1, null));
		assertEquals(-1, instance.compare(null, type1));
		assertEquals(-1, instance.compare(type1, type2));
		assertEquals(1, instance.compare(type2, type1));
	}
}
