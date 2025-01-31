/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, 2025, Datadog, Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openjdk.jmc.flightrecorder.writer.api.Annotation;
import org.openjdk.jmc.flightrecorder.writer.api.Types;

class AnnotationTest {
	private TypesImpl types;

	@BeforeEach
	void setup() {
		ConstantPools constantPools = new ConstantPools();
		MetadataImpl metadata = new MetadataImpl(constantPools);
		types = new TypesImpl(metadata);
	}

	@Test
	void validAnnotationTypeNullValue() {
		TypeImpl type = types.getType(TypesImpl.JDK.ANNOTATION_LABEL);
		Annotation annotation = new Annotation(type);
		assertNull(annotation.getValue());
		assertEquals(type, annotation.getType());
	}

	@Test
	void validAnnotationTypeWithValue() {
		String value = "value";
		TypeImpl type = types.getType(TypesImpl.JDK.ANNOTATION_LABEL);
		Annotation annotation = new Annotation(type, value);
		assertEquals(type, annotation.getType());
		assertEquals(value, annotation.getValue());
	}

	@Test
	void validAnnotationWithArrayValue() {
		String[] values = new String[] {"value1", "value2"};
		TypeImpl type = types.getType(Types.JDK.ANNOTATION_CATEGORY);
		Annotation annotation = new Annotation(type, access -> {
			access.putField("value", values);
		});
		assertEquals(type, annotation.getType());
		assertArrayEquals(values, annotation.getValue(String[].class, "value"));
	}

	@Test
	void invalidAnnotationType() {
		TypeImpl type = types.getType(TypesImpl.Builtin.STRING);
		assertThrows(IllegalArgumentException.class, () -> new Annotation(type));
	}
}
