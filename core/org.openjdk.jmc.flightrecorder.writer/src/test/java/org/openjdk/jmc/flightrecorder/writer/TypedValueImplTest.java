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

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TypedValueImplTest {
	private ConstantPools constantPools;
	private MetadataImpl metadata;
	private TypesImpl types;

	@BeforeEach
	void setup() {
		constantPools = new ConstantPools();
		metadata = new MetadataImpl(constantPools);
		types = new TypesImpl(metadata);
	}

	@Test
	void invalidValue() {
		assertThrows(IllegalArgumentException.class, () -> types.getType(TypesImpl.Builtin.STRING).asValue(1));
	}

	@Test
	void ofBuiltinNonCp() {
		TypeImpl type = types.getType(TypesImpl.Builtin.INT);
		TypedValueImpl value = type.asValue(1);
		assertNotNull(value);
		assertEquals(type, value.getType());
		assertEquals(1, value.getValue());
		assertEquals(Long.MIN_VALUE, value.getConstantPoolIndex());
	}

	@Test
	void ofBuiltinCp() {
		TypeImpl type = types.getType(TypesImpl.Builtin.STRING);
		String targetValue = "hello";
		TypedValueImpl value = type.asValue(targetValue);
		assertNotNull(value);
		assertFalse(value.isNull());
		assertEquals(type, value.getType());
		assertEquals(targetValue, value.getValue());
		assertNotEquals(Long.MIN_VALUE, value.getConstantPoolIndex());
	}

	@Test
	void ofCustom() {
		String targetValue = "hello";
		TypeImpl type = types.getOrAdd("type.Custom", t -> {
			t.addField("field", types.getType(TypesImpl.Builtin.STRING));
		});

		TypedValueImpl value = type.asValue(v -> {
			v.putField("field", targetValue);
		});
		assertNotNull(value);
		assertFalse(value.isNull());
		assertEquals(type, value.getType());
		assertEquals(targetValue, value.getValue());
		assertNotEquals(Long.MIN_VALUE, value.getConstantPoolIndex());
	}

	@Test
	void ofCustomNoCP() {
		TypeStructureImpl structure = new TypeStructureImpl(
				Collections.singletonList(new TypedFieldImpl(types.getType(TypesImpl.Builtin.STRING), "field")),
				Collections.emptyList());
		CompositeTypeImpl nonCpType = new CompositeTypeImpl(1234, "test.Type", null, structure, null, types);

		TypedValueImpl typedValue = nonCpType.asValue(t -> {
			t.putField("field", "Ho!");
		});
		assertNotNull(typedValue);
	}

	@ParameterizedTest
	@EnumSource(TypesImpl.Builtin.class)
	void ofNull(TypesImpl.Builtin type) {
		TypedValueImpl nullValue = TypedValueImpl.ofNull(types.getType(type));
		assertNotNull(nullValue);
		assertTrue(nullValue.isNull());
		assertThrows(NullPointerException.class, nullValue::getFieldValues);
	}

	@Test
	void ofNullInvalid() {
		TestType type1 = new TestType(1234, "test.Type", null, constantPools, types) {
			@Override
			public boolean canAccept(Object value) {
				// disallow null values
				return value != null;
			}
		};
		assertThrows(IllegalArgumentException.class, () -> TypedValueImpl.ofNull(type1));
	}

	@Test
	void ofNullCustom() {
		TypeImpl type = types.getOrAdd("type.Custom", t -> {
			t.addField("field", types.getType(TypesImpl.Builtin.STRING));
		});
		assertNotNull(TypedValueImpl.ofNull(type));
	}

	@Test
	void copyBuiltinWithCp() {
		int newCpIndex = 10;
		TypeImpl type = types.getType(TypesImpl.Builtin.STRING);
		String targetValue = "hello";
		TypedValueImpl value = new TypedValueImpl(type, targetValue, -1);

		assertEquals(-1, value.getConstantPoolIndex());
		assertThrows(IllegalArgumentException.class, () -> new TypedValueImpl(value, newCpIndex));
	}

	@Test
	void copyCustomWithCp() {
		int newCpIndex = 10;
		String targetValue = "hello";

		TypeImpl type = types.getOrAdd("type.Custom", t -> {
			t.addField("field", types.getType(TypesImpl.Builtin.STRING));
		});

		TypedValueImpl value = new TypedValueImpl(type, Collections.singletonMap("field",
				new TypedFieldValueImpl(type.getField("field"), type.getField("field").getType().asValue(targetValue))),
				-1);

		assertEquals(-1, value.getConstantPoolIndex());
		assertEquals(newCpIndex, new TypedValueImpl(value, newCpIndex).getConstantPoolIndex());
	}

	@Test
	void getFieldValues() {
		TypeImpl type = types.getOrAdd("type.Custom", t -> {
			t.addField("field1", types.getType(TypesImpl.Builtin.STRING)).addField("field2",
					types.getType(TypesImpl.Builtin.STRING));
		});

		TypedValueImpl typedValue = type.asValue(v -> {
			v.putField("field1", "value1");
		});

		List<TypedFieldValueImpl> fields = typedValue.getFieldValues();
		assertEquals(2, fields.size());
		for (TypedFieldValueImpl tValue : fields) {
			assertNotNull(tValue);
			if (tValue.getValue().isNull()) {
				assertNull(tValue.getValue().getValue());
			} else {
				assertEquals("value1", tValue.getValue().getValue());
			}
		}
	}

	@Test
	void testEquality() {
		TypeImpl type1 = types.getType(TypesImpl.Builtin.STRING);
		TypeImpl type2 = types.getOrAdd("type.Custom", t -> {
			t.addField("field1", types.getType(TypesImpl.Builtin.STRING)).addField("field2",
					types.getType(TypesImpl.Builtin.STRING));
		});

		TypedValueImpl value1_1 = type1.asValue("hello");
		TypedValueImpl value1_2 = type1.asValue("world");
		TypedValueImpl value2_1 = type2.asValue(v -> {
			v.putField("field1", "hello");
		});
		TypedValueImpl value2_2 = type2.asValue(v -> {
			v.putField("field2", "world");
		});

		assertEquals(value1_1, value1_1);
		assertEquals(value1_2, value1_2);
		assertEquals(value2_1, value2_1);
		assertEquals(value2_2, value2_2);

		assertNotEquals(null, value1_1);
		assertNotEquals(null, value1_2);
		assertNotEquals(null, value2_1);
		assertNotEquals(null, value2_2);

		assertNotEquals(value1_1, null);
		assertNotEquals(value1_2, null);
		assertNotEquals(value2_1, null);
		assertNotEquals(value2_2, null);

		assertNotEquals(1, value1_1);
		assertNotEquals(1, value1_2);
		assertNotEquals(1, value2_1);
		assertNotEquals(1, value2_2);

		assertNotEquals(value1_1, 1);
		assertNotEquals(value1_2, 1);
		assertNotEquals(value2_1, 1);
		assertNotEquals(value2_2, 1);

		assertNotEquals(value1_1, value1_2);
		assertNotEquals(value1_1, value2_1);
		assertNotEquals(value1_1, value2_2);
		assertNotEquals(value1_2, value1_1);
		assertNotEquals(value1_2, value2_1);
		assertNotEquals(value1_2, value2_2);
		assertNotEquals(value2_1, value1_1);
		assertNotEquals(value2_1, value1_2);
		assertNotEquals(value2_1, value2_2);
		assertNotEquals(value2_2, value1_1);
		assertNotEquals(value2_2, value1_2);
		assertNotEquals(value2_2, value2_1);
	}
}
