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

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openjdk.jmc.flightrecorder.writer.api.TypedFieldBuilder;
import org.openjdk.jmc.flightrecorder.writer.api.TypedFieldValue;

@SuppressWarnings("unchecked")
class TypedValueBuilderImplTest {
	private static final String CUSTOM_FIELD_NAME = "custom_field";
	private static final String CUSTOM_FIELD_ARRAY_NAME = "custom_field_arr";
	private static final String SIMPLE_FIELD_VALUE = "hello";
	private static final String SIMPLE_FIELD_NAME = "field";
	private static Map<TypesImpl.Builtin, String> typeToFieldMap;
	private TypedValueBuilderImpl instance;
	private TypeImpl simpleType;
	private TypeImpl customType;
	private TypeImpl stringType;

	@BeforeAll
	static void init() {
		typeToFieldMap = new HashMap<>(TypesImpl.Builtin.values().length);
		for (TypesImpl.Builtin builtin : TypesImpl.Builtin.values()) {
			typeToFieldMap.put(builtin, builtin.name().toLowerCase() + "_field");
		}
	}

	@BeforeEach
	void setUp() {
		// not mocking here since we will need quite a number of predefined types anyway
		TypesImpl types = new TypesImpl(new MetadataImpl(new ConstantPools()));

		stringType = types.getType(TypesImpl.Builtin.STRING);

		simpleType = types.getOrAdd("custom.Simple", builder -> {
			builder.addField(SIMPLE_FIELD_NAME, TypesImpl.Builtin.STRING);
		});

		customType = types.getOrAdd("custom.Type", builder -> {
			for (TypesImpl.Builtin builtin : TypesImpl.Builtin.values()) {
				builder = builder.addField(getFieldName(builtin), builtin).addField(getArrayFieldName(builtin), builtin,
						TypedFieldBuilder::asArray);
			}
			builder.addField(CUSTOM_FIELD_NAME, simpleType).addField(CUSTOM_FIELD_ARRAY_NAME, simpleType,
					TypedFieldBuilder::asArray);
		});

		instance = new TypedValueBuilderImpl(customType);
	}

	@ParameterizedTest
	@EnumSource(TypesImpl.Builtin.class)
	void putFieldBuiltin(TypesImpl.Builtin target) {
		for (TypesImpl.Builtin type : TypesImpl.Builtin.values()) {
			if (type == target) {
				assertCorrectFieldValueBuiltinType(target, type, 1, false);
			} else {
				assertWrongFieldValueBuiltinType(target, type, 0);
			}
		}
	}

	@ParameterizedTest
	@EnumSource(TypesImpl.Builtin.class)
	void putFieldBuiltinArray(TypesImpl.Builtin target) {
		for (TypesImpl.Builtin type : TypesImpl.Builtin.values()) {
			if (type == target) {
				assertCorrectFieldValueBuiltinType(target, type, 1, true);
			} else {
				assertWrongFieldValueBuiltinType(target, type, 0);
			}
		}
	}

	@ParameterizedTest
	@EnumSource(TypesImpl.Builtin.class)
	void putFieldBuiltinArrayNonExistent(TypesImpl.Builtin target) {
		assertThrows(IllegalArgumentException.class, () -> testPutBuiltinFieldArray(target, "not a field name", 1));
	}

	@Test
	void putFieldCustom() {
		instance.putField(CUSTOM_FIELD_NAME, SIMPLE_FIELD_VALUE);

		TypedFieldValueImpl fieldValue = instance.build().get(CUSTOM_FIELD_NAME);
		assertNotNull(fieldValue);
		assertEquals(CUSTOM_FIELD_NAME, fieldValue.getField().getName());
		assertEquals(SIMPLE_FIELD_VALUE, fieldValue.getValue().getValue());
	}

	@Test
	void putFieldCustomBuilder() {
		instance.putField(CUSTOM_FIELD_NAME, v -> {
			v.putField(SIMPLE_FIELD_NAME, SIMPLE_FIELD_VALUE);
		});

		TypedFieldValueImpl fieldValue = instance.build().get(CUSTOM_FIELD_NAME);
		assertNotNull(fieldValue);
		assertEquals(CUSTOM_FIELD_NAME, fieldValue.getField().getName());
		assertEquals(SIMPLE_FIELD_VALUE, fieldValue.getValue().getValue());
	}

	@Test
	void putFieldCustomArray() {
		instance.putField(CUSTOM_FIELD_ARRAY_NAME, simpleType.asValue(v -> v.putField(SIMPLE_FIELD_NAME, "value1")),
				simpleType.asValue(v -> v.putField(SIMPLE_FIELD_NAME, "value2")));

		TypedFieldValueImpl fieldValue = instance.build().get(CUSTOM_FIELD_ARRAY_NAME);
		assertNotNull(fieldValue);
		assertEquals(CUSTOM_FIELD_ARRAY_NAME, fieldValue.getField().getName());
	}

	@Test
	void putFieldCustomArrayNonArrayField() {
		assertThrows(IllegalArgumentException.class,
				() -> instance.putField(CUSTOM_FIELD_NAME,
						simpleType.asValue(v -> v.putField(SIMPLE_FIELD_NAME, "value1")),
						simpleType.asValue(v -> v.putField(SIMPLE_FIELD_NAME, "value2"))));
	}

	@Test
	void putFieldCustomArrayNonExistingField() {
		assertThrows(IllegalArgumentException.class,
				() -> instance.putField("not a field name",
						simpleType.asValue(v -> v.putField(SIMPLE_FIELD_NAME, "value1")),
						simpleType.asValue(v -> v.putField(SIMPLE_FIELD_NAME, "value2"))));
	}

	@Test
	void putFieldCustomArrayInvalidValues() {
		assertThrows(IllegalArgumentException.class, () -> instance.putField(CUSTOM_FIELD_ARRAY_NAME,
				stringType.asValue("value1"), stringType.asValue("value2")));
	}

	@Test
	void putFieldCustomInvalid() {
		assertThrows(IllegalArgumentException.class, () -> instance.putField(CUSTOM_FIELD_NAME, 0L));
	}

	@Test
	public void putFieldCustomBuilderArray() {
		TypedFieldValue value = instance.putFields(CUSTOM_FIELD_ARRAY_NAME, fld1 -> {
			fld1.putField(SIMPLE_FIELD_NAME, SIMPLE_FIELD_VALUE);
		}, fld2 -> {
			fld2.putField(SIMPLE_FIELD_NAME, SIMPLE_FIELD_VALUE);
		}).build().get(CUSTOM_FIELD_ARRAY_NAME);

		assertEquals(CUSTOM_FIELD_ARRAY_NAME, value.getField().getName());
		assertNotNull(value);
	}

	@Test
	public void putFieldCustomBuilderArrayNonArrayField() {
		assertThrows(IllegalArgumentException.class, () -> {
			instance.putFields(CUSTOM_FIELD_NAME, fld1 -> {
				fld1.putField(SIMPLE_FIELD_NAME, SIMPLE_FIELD_VALUE);
			}, fld2 -> {
				fld2.putField(SIMPLE_FIELD_NAME, SIMPLE_FIELD_VALUE);
			}).build().get(CUSTOM_FIELD_ARRAY_NAME);
		});
	}

	@Test
	public void putFieldCustomBuilderArrayNonExistingField() {
		assertThrows(IllegalArgumentException.class, () -> {
			instance.putFields("not a field name", fld1 -> {
				fld1.putField(SIMPLE_FIELD_NAME, SIMPLE_FIELD_VALUE);
			}, fld2 -> {
				fld2.putField(SIMPLE_FIELD_NAME, SIMPLE_FIELD_VALUE);
			}).build().get(CUSTOM_FIELD_ARRAY_NAME);
		});
	}

	private void assertCorrectFieldValueBuiltinType(
		TypesImpl.Builtin target, TypesImpl.Builtin type, int value, boolean asArray) {
		if (asArray) {
			testPutBuiltinFieldArray(target, getArrayFieldName(type), value);
		} else {
			testPutBuiltinField(target, getFieldName(type), value);
		}

		String fieldName = asArray ? getArrayFieldName(type) : getFieldName(type);
		TypedFieldValueImpl fieldValue = instance.build().get(fieldName);
		assertNotNull(fieldValue);
		assertEquals(fieldName, fieldValue.getField().getName());

		Object targetValue = null;
		if (asArray) {
			Object targetValues = fieldValue.getValues();
			assertNotNull(targetValues);
			assertTrue(targetValues.getClass().isArray());
			targetValue = Array.get(targetValues, 0);
		} else {
			targetValue = fieldValue.getValue().getValue();
		}
		assertNotNull(targetValue);
		if (targetValue instanceof Number) {
			assertEquals(value, ((Number) targetValue).intValue());
		} else if (targetValue instanceof String) {
			assertEquals(String.valueOf(value), targetValue);
		} else if (targetValue instanceof Boolean) {
			assertEquals(value > 0, targetValue);
		}
	}

	private void assertWrongFieldValueBuiltinType(TypesImpl.Builtin target, TypesImpl.Builtin type, int value) {
		assertThrows(IllegalArgumentException.class, () -> testPutBuiltinField(target, getArrayFieldName(type), value));
		assertThrows(IllegalArgumentException.class, () -> testPutBuiltinFieldArray(target, getFieldName(type), value));
	}

	private void testPutBuiltinField(TypesImpl.Builtin target, String fieldName, int value) {
		switch (target) {
		case BYTE: {
			instance.putField(fieldName, (byte) value);
			break;
		}
		case CHAR: {
			instance.putField(fieldName, (char) value);
			break;
		}
		case SHORT: {
			instance.putField(fieldName, (short) value);
			break;
		}
		case INT: {
			instance.putField(fieldName, (int) value);
			break;
		}
		case LONG: {
			instance.putField(fieldName, (long) value);
			break;
		}
		case FLOAT: {
			instance.putField(fieldName, (float) value);
			break;
		}
		case DOUBLE: {
			instance.putField(fieldName, (double) value);
			break;
		}
		case BOOLEAN: {
			instance.putField(fieldName, (int) (value) > 0);
			break;
		}
		case STRING: {
			instance.putField(fieldName, String.valueOf(value));
			break;
		}
		}
	}

	private void testPutBuiltinFieldArray(TypesImpl.Builtin target, String fieldName, int value) {
		switch (target) {
		case BYTE: {
			instance.putField(fieldName, new byte[] {(byte) value});
			break;
		}
		case CHAR: {
			instance.putField(fieldName, new char[] {(char) value});
			break;
		}
		case SHORT: {
			instance.putField(fieldName, new short[] {(short) value});
			break;
		}
		case INT: {
			instance.putField(fieldName, new int[] {(int) value});
			break;
		}
		case LONG: {
			instance.putField(fieldName, new long[] {(long) value});
			break;
		}
		case FLOAT: {
			instance.putField(fieldName, new float[] {(float) value});
			break;
		}
		case DOUBLE: {
			instance.putField(fieldName, new double[] {(double) value});
			break;
		}
		case BOOLEAN: {
			instance.putField(fieldName, new boolean[] {(int) (value) > 0});
			break;
		}
		case STRING: {
			instance.putField(fieldName, new String[] {String.valueOf(value)});
			break;
		}
		}
	}

	private static String getFieldName(TypesImpl.Builtin type) {
		return typeToFieldMap.get(type);
	}

	private static String getArrayFieldName(TypesImpl.Builtin type) {
		return getFieldName(type) + "_arr";
	}
}
