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
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.openjdk.jmc.flightrecorder.writer.api.TypedValue;

class ResolvableTypeTest {
	private static final String FIELD_NAME = "field";

	private ResolvableType resolvableType;
	private final String targetTypeName = "custom.Type";
	private MetadataImpl metadata;
	private TypesImpl types;
	private TypeImpl targetType;

	@BeforeEach
	void setUp() {
		ConstantPools constantPools = new ConstantPools();
		metadata = new MetadataImpl(constantPools);
		types = new TypesImpl(metadata);
		resolvableType = new ResolvableType(targetTypeName, metadata);
	}

	@Test
	void isResolved() {
		assertFalse(resolvableType.isResolved());
		resolve();
		assertTrue(resolvableType.isResolved());
	}

	@Test
	void getId() {
		assertThrows(IllegalStateException.class, () -> resolvableType.getId());
		resolve();
		assertEquals(targetType.getId(), resolvableType.getId());
	}

	@Test
	void hasConstantPool() {
		assertThrows(IllegalStateException.class, () -> resolvableType.hasConstantPool());
		resolve();
		assertEquals(targetType.hasConstantPool(), resolvableType.hasConstantPool());
	}

	@Test
	void asValueByte() {
		assertThrows(IllegalStateException.class, () -> resolvableType.asValue((byte) 1));
		resolve();
		// a resolvable type is always a complex/custom type so `asValue(byte)` will always fail
		assertThrows(IllegalArgumentException.class, () -> resolvableType.asValue((byte) 1));
	}

	@Test
	void asValueChar() {
		assertThrows(IllegalStateException.class, () -> resolvableType.asValue((char) 1));
		resolve();
		// a resolvable type is always a complex/custom type so `asValue(char)` will always fail
		assertThrows(IllegalArgumentException.class, () -> resolvableType.asValue((char) 1));
	}

	@Test
	void asValueShort() {
		assertThrows(IllegalStateException.class, () -> resolvableType.asValue((short) 1));
		resolve();
		// a resolvable type is always a complex/custom type so `asValue(short)` will always fail
		assertThrows(IllegalArgumentException.class, () -> resolvableType.asValue((short) 1));
	}

	@Test
	void asValueInt() {
		assertThrows(IllegalStateException.class, () -> resolvableType.asValue((int) 1));
		resolve();
		// a resolvable type is always a complex/custom type so `asValue(int)` will always fail
		assertThrows(IllegalArgumentException.class, () -> resolvableType.asValue((int) 1));
	}

	@Test
	void asValueLong() {
		assertThrows(IllegalStateException.class, () -> resolvableType.asValue((long) 1));
		resolve();
		// a resolvable type is always a complex/custom type so `asValue(long)` will always fail
		assertThrows(IllegalArgumentException.class, () -> resolvableType.asValue((long) 1));
	}

	@Test
	void asValueFloat() {
		assertThrows(IllegalStateException.class, () -> resolvableType.asValue((float) 1));
		resolve();
		// a resolvable type is always a complex/custom type so `asValue(float)` will always fail
		assertThrows(IllegalArgumentException.class, () -> resolvableType.asValue((float) 1));
	}

	@Test
	void asValueDouble() {
		assertThrows(IllegalStateException.class, () -> resolvableType.asValue((double) 1));
		resolve();
		// a resolvable type is always a complex/custom type so `asValue(double)` will always fail
		assertThrows(IllegalArgumentException.class, () -> resolvableType.asValue((double) 1));
	}

	@Test
	void asValueBoolean() {
		assertThrows(IllegalStateException.class, () -> resolvableType.asValue(true));
		resolve();
		// a resolvable type is always a complex/custom type so `asValue(boolean)` will always fail
		assertThrows(IllegalArgumentException.class, () -> resolvableType.asValue(true));
	}

	@Test
	void asValueString() {
		assertThrows(IllegalStateException.class, () -> resolvableType.asValue("1"));
		resolve();
		// a resolvable type is always a complex/custom type so `asValue(String)` will always fail
		assertThrows(IllegalArgumentException.class, () -> resolvableType.asValue("1"));
	}

	@Test
	void asValueCustom() {
		assertThrows(IllegalStateException.class, () -> resolvableType.asValue(v -> {
		}));
		resolve();
		TypedValue value1 = targetType.asValue(v -> {
		});
		TypedValue value2 = resolvableType.asValue(v -> {
		});
		assertEquals(value1, value2);
	}

	@Test
	void nullValue() {
		assertThrows(IllegalStateException.class, () -> resolvableType.nullValue());
		resolve();
		assertEquals(targetType.nullValue(), resolvableType.nullValue());
	}

	@Test
	void isBuiltin() {
		assertThrows(IllegalStateException.class, () -> resolvableType.isBuiltin());
		resolve();
		assertEquals(targetType.isBuiltin(), resolvableType.isBuiltin());
	}

	@Test
	void isSimple() {
		assertThrows(IllegalStateException.class, () -> resolvableType.isSimple());
		resolve();
		assertEquals(targetType.isSimple(), resolvableType.isSimple());
	}

	@Test
	void getSupertype() {
		assertThrows(IllegalStateException.class, () -> resolvableType.getSupertype());
		resolve();
		assertEquals(targetType.getSupertype(), resolvableType.getSupertype());
	}

	@Test
	void getFields() {
		assertThrows(IllegalStateException.class, () -> resolvableType.getFields());
		resolve();
		assertEquals(targetType.getFields(), resolvableType.getFields());
	}

	@Test
	void getField() {
		assertThrows(IllegalStateException.class, () -> resolvableType.getField(FIELD_NAME));
		resolve();
		assertEquals(targetType.getField(FIELD_NAME), resolvableType.getField(FIELD_NAME));
	}

	@Test
	void getAnnotations() {
		assertThrows(IllegalStateException.class, () -> resolvableType.getAnnotations());
		resolve();
		assertEquals(targetType.getAnnotations(), resolvableType.getAnnotations());
	}

	@Test
	void canAccept() {
		assertThrows(IllegalStateException.class, () -> resolvableType.canAccept("value"));
		resolve();
		assertEquals(targetType.canAccept("value"), resolvableType.canAccept("value"));
	}

	@Test
	void getTypeName() {
		assertEquals(targetTypeName, resolvableType.getTypeName());
		resolve();
		assertEquals(targetType.getFields(), resolvableType.getFields());
	}

	@Test
	void isSame() {
		assertThrows(IllegalStateException.class, () -> resolvableType.isSame(TypesImpl.JDK.CLASS_LOADER));
		resolve();
		assertEquals(targetType.isSame(TypesImpl.JDK.CLASS_LOADER), resolvableType.isSame(TypesImpl.JDK.CLASS_LOADER));
	}

	@Test
	void isUsedBy() {
		TypeImpl otherType = types.getType(TypesImpl.JDK.CLASS_LOADER);
		assertThrows(IllegalStateException.class, () -> resolvableType.isUsedBy(otherType));
		resolve();
		assertEquals(targetType.isUsedBy(otherType), resolvableType.isUsedBy(otherType));
	}

	/**
	 * Resolvable type is supposed to work with {@linkplain BaseType} subclasses only. This test
	 * asserts that a resolvable will not be resolved to anything that is not a
	 * {@linkplain BaseType} subclass.
	 */
	@Test
	void wrongTypeResolve() {
		MetadataImpl mockedMetadata = Mockito.mock(MetadataImpl.class);
		Mockito.when(mockedMetadata.getType(ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean()))
				.thenReturn(new InvalidType());

		assertFalse(resolvableType.resolve());
	}

	private void resolve() {
		List<TypedFieldImpl> fields = Collections
				.singletonList(new TypedFieldImpl(types.getType(TypesImpl.Builtin.STRING), FIELD_NAME));

		targetType = metadata.registerType(targetTypeName, null,
				() -> new TypeStructureImpl(fields, Collections.emptyList()));
		metadata.resolveTypes();
	}
}
