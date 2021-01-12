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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

class BuiltinTypeTest {
	private static final long TYPE_ID = 1L;
	private BuiltinType instance;
	private TypesImpl types;

	@BeforeEach
	void setUp() {
		MetadataImpl metadata = Mockito.mock(MetadataImpl.class);
		types = new TypesImpl(metadata);
		instance = new BuiltinType(TYPE_ID, TypesImpl.Builtin.INT, Mockito.mock(ConstantPools.class), types);
	}

	@Test
	void isBuiltin() {
		assertTrue(instance.isBuiltin());
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
	void getFields() {
		assertTrue(instance.getFields().isEmpty());
	}

	@Test
	void getField() {
		assertThrows(IllegalArgumentException.class, () -> instance.getField("field"));
	}

	@Test
	void getAnnotations() {
		assertTrue(instance.getAnnotations().isEmpty());
	}

	@Test
	void getId() {
		assertEquals(TYPE_ID, instance.getId());
	}

	@Test
	void getTypeName() {
		assertEquals(TypesImpl.Builtin.INT.getTypeName(), instance.getTypeName());
	}

	@Test
	void getSupertype() {
		assertNull(instance.getSupertype());
	}

	@Test
	void isUsedBy() {
		TypeImpl other = new BuiltinType(TYPE_ID, TypesImpl.Builtin.STRING, Mockito.mock(ConstantPools.class), types);

		assertFalse(instance.isUsedBy(null));
		assertFalse(instance.isUsedBy(instance));
		assertFalse(instance.isUsedBy(other));
	}

	@ParameterizedTest
	@EnumSource(TypesImpl.Builtin.class)
	void canAccept(TypesImpl.Builtin target) {
		BaseType type = new BuiltinType(1, target, Mockito.mock(ConstantPools.class), types);
		for (TypesImpl.Builtin builtin : TypesImpl.Builtin.values()) {
			for (Object value : TypeUtils.getBuiltinValues(builtin)) {
				assertEquals(target == builtin || value == null, type.canAccept(value)); // null is generally accepted
			}
		}
	}

	@ParameterizedTest
	@EnumSource(TypesImpl.Builtin.class)
	void equality(TypesImpl.Builtin target) {
		BaseType type1 = new BuiltinType(1, target, Mockito.mock(ConstantPools.class), types);
		for (TypesImpl.Builtin builtin : TypesImpl.Builtin.values()) {
			BaseType type2 = new BuiltinType(1, builtin, Mockito.mock(ConstantPools.class), types);

			assertFalse(type1.equals(null));
			assertTrue(type1.equals(type1));
			assertEquals(target == builtin, type1.equals(type2));
			assertEquals(target == builtin, type2.equals(type1));
		}
	}

	@ParameterizedTest
	@EnumSource(TypesImpl.Builtin.class)
	void asValue(TypesImpl.Builtin target) throws Exception {
		// STRING builtin needs a constant pool; other builtins must not have a constant pool
		ConstantPools constantPools = Mockito.mock(ConstantPools.class);
		Mockito.when(constantPools.forType(ArgumentMatchers.any(TypeImpl.class)))
				.thenAnswer(i -> new ConstantPool(i.getArgument(0)));

		BaseType type = new BuiltinType(1, target, target == TypesImpl.Builtin.STRING ? constantPools : null, types);
		for (TypesImpl.Builtin builtin : TypesImpl.Builtin.values()) {
			for (Object fromValue : TypeUtils.getBuiltinValues(builtin)) {
				Method asValueMethod = getAsValueMethod(builtin);

				if (target == builtin) {
					TypedValueImpl typedValue = (TypedValueImpl) asValueMethod.invoke(type, fromValue);

					assertNotNull(typedValue);
					assertEquals(fromValue, typedValue.getValue());
				} else {
					try {
						asValueMethod.invoke(type, fromValue);
						// for 'null' values there is no extra type info so really can't assert anything
						if (fromValue != null) {
							fail("Attempted conversion of a value of type '" + builtin.getTypeClass() + "' to '"
									+ target.getTypeClass() + "'");
						}
					} catch (InvocationTargetException e) {
						if (!(e.getCause() instanceof IllegalArgumentException)) {
							fail(e);
						}
					}
				}
			}
		}

		assertThrows(IllegalArgumentException.class, () -> type.asValue(v -> {
			v.putField("f1", "no value");
		}));
	}

	private static Method getAsValueMethod(TypesImpl.Builtin type) throws Exception {
		return BaseType.class.getMethod("asValue", type.getTypeClass());
	}
}
