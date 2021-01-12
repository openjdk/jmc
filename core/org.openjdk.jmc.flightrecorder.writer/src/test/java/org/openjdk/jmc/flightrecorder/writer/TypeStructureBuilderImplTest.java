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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.openjdk.jmc.flightrecorder.writer.api.Annotation;
import org.openjdk.jmc.flightrecorder.writer.api.TypeStructure;
import org.openjdk.jmc.flightrecorder.writer.api.TypedField;

class TypeStructureBuilderImplTest {
	private static final String FIELD_NAME = "field";
	private static final String CUSTOM_TYPE_NAME = "test.Type";
	private static final String ANNOTATION_TYPE_NAME = "jdk.jfr.Label";
	private static final String ANNOTATION_LABEL = "test.Label";

	private TypeStructureBuilderImpl instance;
	private TypeImpl stringType;
	private TypeImpl customType;
	private TypeImpl annotationType;

	@BeforeEach
	void setUp() {
		ConstantPools constantPools = Mockito.mock(ConstantPools.class);
		TypesImpl types = Mockito.mock(TypesImpl.class);

		stringType = new BuiltinType(1, TypesImpl.Builtin.STRING, constantPools, types);

		List<TypedFieldImpl> customTypeFields = Collections.singletonList(new TypedFieldImpl(stringType, "item"));
		customType = new CompositeTypeImpl(2, CUSTOM_TYPE_NAME, null,
				new TypeStructureImpl(customTypeFields, Collections.emptyList()), constantPools, types);

		annotationType = new CompositeTypeImpl(3, ANNOTATION_TYPE_NAME, Annotation.ANNOTATION_SUPER_TYPE_NAME,
				new TypeStructureImpl(Collections.singletonList(new TypedFieldImpl(stringType, "value")),
						Collections.emptyList()),
				constantPools, types);

		Mockito.when(types.getType(ArgumentMatchers.any(TypesImpl.Predefined.class))).thenReturn(stringType);
		Mockito.when(types.getType(ArgumentMatchers.matches(CUSTOM_TYPE_NAME.replace(".", "\\."))))
				.thenReturn(customType);
		Mockito.when(types.getType(ArgumentMatchers.matches(ANNOTATION_TYPE_NAME.replace(".", "\\."))))
				.thenReturn(annotationType);

		instance = new TypeStructureBuilderImpl(types);
	}

	@Test
	void addFieldPredefined() {
		TypeStructure structure = instance.addField(FIELD_NAME, TypesImpl.Builtin.STRING).build();
		assertEquals(1, structure.getFields().size());
		assertEquals(0, structure.getAnnotations().size());

		TypedField field = structure.getFields().get(0);
		assertEquals(FIELD_NAME, field.getName());
		assertEquals(stringType, field.getType());
	}

	@Test
	void testAddFieldCustom() {
		TypeStructure structure = instance.addField(FIELD_NAME, customType).build();
		assertEquals(1, structure.getFields().size());

		TypedField field = structure.getFields().get(0);
		assertEquals(FIELD_NAME, field.getName());
		assertEquals(customType, field.getType());
	}

	@Test
	void addFieldNullName() {
		assertThrows(NullPointerException.class, () -> instance.addField(null, TypesImpl.Builtin.STRING));
	}

	@Test
	void addFieldNullType() {
		assertThrows(NullPointerException.class, () -> instance.addField(FIELD_NAME, (TypesImpl.Predefined) null));
		assertThrows(NullPointerException.class, () -> instance.addField(FIELD_NAME, (TypeImpl) null));
	}

	@Test
	void addAnnotation() {
		TypeStructure structure = instance.addAnnotation(annotationType, ANNOTATION_LABEL).build();
		assertEquals(0, structure.getFields().size());
		assertEquals(1, structure.getAnnotations().size());

		Annotation annotation = structure.getAnnotations().get(0);
		assertEquals(annotationType, annotation.getType());
		assertEquals(ANNOTATION_LABEL, annotation.getValue());
	}

	@Test
	void addAnnotationNullValue() {
		TypeStructure structure = instance.addAnnotation(annotationType).build();
		assertEquals(0, structure.getFields().size());
		assertEquals(1, structure.getAnnotations().size());

		Annotation annotation = structure.getAnnotations().get(0);
		assertEquals(annotationType, annotation.getType());
		assertNull(annotation.getValue());
	}
}
