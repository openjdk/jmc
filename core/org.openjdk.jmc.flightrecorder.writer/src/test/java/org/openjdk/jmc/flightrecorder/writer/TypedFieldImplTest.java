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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openjdk.jmc.flightrecorder.writer.api.Annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypedFieldImplTest {
	private static final String FIELD_NAME = "instance";

	private TypesImpl types;

	@BeforeEach
	void setUp() {
		ConstantPools constantPools = new ConstantPools();
		MetadataImpl metadata = new MetadataImpl(constantPools);
		types = new TypesImpl(metadata);
	}

	@Test
	void getCustomTypeNoAnnotations() {
		TypeImpl type = types.getOrAdd("custom.Type", t -> {
			t.addField("field", TypesImpl.Builtin.STRING).addAnnotation(types.getType(TypesImpl.JDK.ANNOTATION_LABEL),
					"custom.Type");
		});
		TypedFieldImpl instance = new TypedFieldImpl(type, FIELD_NAME);

		assertEquals(FIELD_NAME, instance.getName());
		assertEquals(type, instance.getType());
		assertEquals(0, instance.getAnnotations().size());
		assertFalse(instance.isArray());
	}

	@Test
	void getCustomTypeAnnotations() {
		TypeImpl type = types.getOrAdd("custom.Type", t -> {
			t.addField("field", TypesImpl.Builtin.STRING).addAnnotation(types.getType(TypesImpl.JDK.ANNOTATION_LABEL),
					"custom.Type");
		});
		TypedFieldImpl instance = new TypedFieldImpl(type, FIELD_NAME, false,
				Collections.singletonList(new Annotation(types.getType(TypesImpl.JDK.ANNOTATION_LABEL), "field")));

		assertEquals(FIELD_NAME, instance.getName());
		assertEquals(type, instance.getType());
		assertEquals(1, instance.getAnnotations().size());
		assertFalse(instance.isArray());
	}

	@Test
	void getCustomTypeArrayNoAnnotations() {
		TypeImpl type = types.getOrAdd("custom.Type", t -> {
			t.addField("field", TypesImpl.Builtin.STRING).addAnnotation(types.getType(TypesImpl.JDK.ANNOTATION_LABEL),
					"custom.Type");
		});
		TypedFieldImpl instance = new TypedFieldImpl(type, FIELD_NAME, true);

		assertEquals(FIELD_NAME, instance.getName());
		assertEquals(type, instance.getType());
		assertEquals(0, instance.getAnnotations().size());
		assertTrue(instance.isArray());
	}

	@Test
	void getCustomTypeArrayAnnotations() {
		TypeImpl type = types.getOrAdd("custom.Type", t -> {
			t.addField("field", TypesImpl.Builtin.STRING).addAnnotation(types.getType(TypesImpl.JDK.ANNOTATION_LABEL),
					"custom.Type");
		});
		TypedFieldImpl instance = new TypedFieldImpl(type, FIELD_NAME, true,
				Collections.singletonList(new Annotation(types.getType(TypesImpl.JDK.ANNOTATION_LABEL), "field")));

		assertEquals(FIELD_NAME, instance.getName());
		assertEquals(type, instance.getType());
		assertEquals(1, instance.getAnnotations().size());
		assertTrue(instance.isArray());
	}

	@Test
	void equality() {
		TypeImpl[] fieldTypes = new TypeImpl[] {types.getOrAdd("custom.Type", t -> {
			t.addField("field", TypesImpl.Builtin.STRING).addAnnotation(types.getType(TypesImpl.JDK.ANNOTATION_LABEL),
					"custom.Type");
		}), types.getType(TypesImpl.Builtin.STRING)};

		String[] fieldNames = new String[] {"field1", "field2"};
		boolean[] arrayFlags = new boolean[] {true, false};
		List<List<Annotation>> annotations = Arrays.asList(Collections.emptyList(),
				Collections.singletonList(new Annotation(types.getType(TypesImpl.JDK.ANNOTATION_LABEL), "field")));

		List<TypedFieldImpl> fields = new ArrayList<>();
		for (TypeImpl fieldType : fieldTypes) {
			for (String fieldName : fieldNames) {
				for (boolean arrayFlag : arrayFlags) {
					for (List<Annotation> annotationList : annotations) {
						fields.add(new TypedFieldImpl(fieldType, fieldName, arrayFlag, annotationList));
					}
				}
			}
		}

		for (TypedFieldImpl field1 : fields) {
			// make the code coverage check happy
			assertFalse(field1.equals(10));

			assertFalse(field1.equals(null));
			for (TypedFieldImpl field2 : fields) {
				assertEquals(field1 == field2, field1.equals(field2));

				// keep the hashCode-equals contract
				assertTrue(field1.hashCode() != field2.hashCode() || field1.equals(field2));
			}
		}
	}
}
