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

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

class ChunkTest {
	private Chunk instance;
	private LEB128Writer writer;
	private TypesImpl types;
	private MetadataImpl metadata;

	@BeforeEach
	void setup() {
		writer = LEB128Writer.getInstance();
		instance = new Chunk();
		metadata = new MetadataImpl(new ConstantPools());
		types = new TypesImpl(metadata);
	}

	@Test
	void writeEventWrongType() {
		assertThrows(IllegalArgumentException.class,
				() -> instance.writeEvent(types.getType(TypesImpl.Builtin.STRING).asValue("value")));
	}

	@Test
	void writeNullValue() {
		assertThrows(IllegalArgumentException.class, () -> instance.writeTypedValue(writer, null));
	}

	@ParameterizedTest
	@EnumSource(TypesImpl.Builtin.class)
	void writeBuiltinValue(TypesImpl.Builtin target) {
		for (Object value : TypeUtils.getBuiltinValues(target)) {
			TypeImpl type = types.getType(target);
			TypedValueImpl tv = new TypedValueImpl(type, value, 1);
			instance.writeTypedValue(writer, tv);
			instance.writeTypedValue(writer, type.nullValue());
		}
	}

	@Test
	void writeUnknownBuiltin() {
		TypeImpl type = Mockito.mock(BaseType.class,
				Mockito.withSettings().useConstructor(1L, "unknown.Builtin", null, types));
		Mockito.when(type.isBuiltin()).thenReturn(true);
		Mockito.when(type.canAccept(ArgumentMatchers.any())).thenReturn(true);

		assertThrows(IllegalArgumentException.class,
				() -> instance.writeTypedValue(writer, new TypedValueImpl(type, "hello", 10)));
	}

	@Test
	void writeCustomNoCP() {
		TypeImpl stringType = types.getType(TypesImpl.Builtin.STRING);
		List<TypedFieldImpl> fields = Arrays.asList(new TypedFieldImpl(stringType, "string", false),
				new TypedFieldImpl(stringType, "strings", true));
		TypeStructureImpl structure = new TypeStructureImpl(fields, Collections.emptyList());

		TypeImpl type = new CompositeTypeImpl(1000, "custom.Type", null, structure, null, types);

		instance.writeTypedValue(writer, type.asValue(v -> {
			v.putField("string", "value1").putField("strings", new String[] {"value2", "value4"});
		}));
	}
}
