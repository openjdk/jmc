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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MetadataImplTest {
	private static final String TYPE_NAME = "dummy.Type";

	private MetadataImpl instance;

	@BeforeEach
	public void setup() throws Exception {
		instance = new MetadataImpl(new ConstantPools());
	}

	@Test
	public void registerBuiltinNull() {
		assertThrows(NullPointerException.class, () -> instance.registerBuiltin(null));
	}

	@Test
	public void registerBuiltin() {
		for (TypesImpl.Builtin builtin : TypesImpl.Builtin.values()) {
			instance.registerBuiltin(builtin);
			// the builtin must be immediately available
			assertNotNull(instance.getType(builtin.getTypeName(), false));
		}
	}

	@Test
	public void resolveTypes() {
		// try to get a non-existing type
		TypeImpl resolvable = instance.getType(TYPE_NAME, true);
		assertFalse(resolvable.isResolved());
		assertEquals(TYPE_NAME, resolvable.getTypeName());

		// register the type in metadata
		instance.registerType(TYPE_NAME, null,
				() -> new TypeStructureImpl(Collections.emptyList(), Collections.emptyList()));
		// and resolve the resolvable wrapper
		instance.resolveTypes();

		assertTrue(resolvable.isResolved());
		assertEquals(TYPE_NAME, resolvable.getTypeName());
	}

	@Test
	public void registerTypeNullStructureProvider() {
		assertNotNull(instance.registerType(TYPE_NAME, "type.Super", (Supplier<TypeStructureImpl>) null));
	}

	@Test
	public void registerType() {
		TypeImpl type = instance.registerType(TYPE_NAME, "type.Super", () -> TypeStructureImpl.EMPTY);
		assertNotNull(type);
		assertNotNull(type.getFields());
		assertNotNull(type.getAnnotations());
	}

	@Test
	public void getTypeEmptyMetadata() {
		assertNull(instance.getType("dummy.Type", false));
	}

	@Test
	public void getResolvableTypeEmptyMetadata() {
		assertNotNull(instance.getType("dummy.Type", true));
	}

	@Test
	void createInvalidBuiltinType() {
		String typeName = "invalid";
		assertThrows(IllegalArgumentException.class, () -> instance.createBuiltinType(typeName));
	}

	@Test
	void createValidBuiltinType() {
		String typeName = TypesImpl.Builtin.BYTE.getTypeName();
		TypeImpl type = instance.createBuiltinType(typeName);
		assertNotNull(type);
		assertTrue(type.isBuiltin());
		assertEquals(typeName, type.getTypeName());
	}

	@Test
	void createDuplicateBuiltinType() {
		String typeName = TypesImpl.Builtin.BYTE.getTypeName();
		TypeImpl type1 = instance.createBuiltinType(typeName);
		assertNotNull(type1);
		assertTrue(type1.isBuiltin());

		TypeImpl type2 = instance.createBuiltinType(typeName);
		assertNotNull(type2);
		assertTrue(type2.isBuiltin());
		assertEquals(type1.getTypeName(), type2.getTypeName());
		assertNotEquals(type1, type2);
	}

	@Test
	void createCustomTypeForBuiltin() {
		String typeName = TypesImpl.Builtin.BYTE.getTypeName();
		assertThrows(IllegalArgumentException.class,
				() -> instance.createCustomType(typeName, null, TypeStructureImpl.EMPTY, true));
	}

	@Test
	void createCustomTypeNullStructure() {
		String typeName = "dummy.Test";
		TypeImpl type = instance.createCustomType(typeName, null, null, true);
		assertNotNull(type);
		assertFalse(type.isBuiltin());
		assertEquals(typeName, type.getTypeName());
		assertNotNull(type.getFields());
		assertNotNull(type.getAnnotations());
	}

	@Test
	void createCustomType() {
		String typeName = "dummy.Test";
		String superName = "super.Type";
		for (boolean withCp : new boolean[] {true, false}) {
			TypeImpl type = instance.createCustomType(typeName, superName, TypeStructureImpl.EMPTY, withCp);
			assertNotNull(type);
			assertFalse(type.isBuiltin());
			assertEquals(typeName, type.getTypeName());
			assertNotNull(type.getFields());
			assertNotNull(type.getAnnotations());
			assertEquals(superName, type.getSupertype());
			assertEquals(withCp, type.hasConstantPool());
		}
	}

	@Test
	void createDuplicateCustomType() {
		String typeName = "dummy.Test";
		String superName = "super.Type";

		TypeImpl type1 = instance.createCustomType(typeName, superName, TypeStructureImpl.EMPTY, true);
		assertNotNull(type1);
		assertFalse(type1.isBuiltin());

		TypeImpl type2 = instance.createCustomType(typeName, superName, TypeStructureImpl.EMPTY, true);
		assertNotNull(type2);
		assertFalse(type2.isBuiltin());
		assertEquals(type1.getTypeName(), type2.getTypeName());
		assertNotEquals(type1, type2);
	}

	@Test
	void stringIndexNull() {
		assertThrows(NullPointerException.class, () -> instance.stringIndex(null));
	}
}
