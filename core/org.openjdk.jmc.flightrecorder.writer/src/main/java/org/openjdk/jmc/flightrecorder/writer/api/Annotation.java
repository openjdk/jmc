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
package org.openjdk.jmc.flightrecorder.writer.api;

import org.openjdk.jmc.flightrecorder.writer.TypeImpl;
import org.openjdk.jmc.flightrecorder.writer.TypedFieldImpl;
import org.openjdk.jmc.flightrecorder.writer.TypedFieldValueImpl;
import org.openjdk.jmc.flightrecorder.writer.TypedValueBuilderImpl;
import org.openjdk.jmc.flightrecorder.writer.TypedValueImpl;
import org.openjdk.jmc.flightrecorder.writer.util.NonZeroHashCode;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/** A struct-like representation of a JFR annotation */
public final class Annotation {
	private static final String VALUE_KEY = "value";

	public static final String ANNOTATION_SUPER_TYPE_NAME = "java.lang.annotation.Annotation";

	private int hashCode = 0;

	private final Type type;
	private final Map<String, ? extends TypedFieldValue> attributes;
	private final List<Annotation> annotations = new ArrayList<>();

	/**
	 * Create a new {@linkplain Annotation} instance
	 *
	 * @param type
	 *            the annotation type (must have the value of
	 *            {@linkplain Annotation#ANNOTATION_SUPER_TYPE_NAME} as its super type)
	 * @throws IllegalArgumentException
	 *             if the annotation type is not having the value of
	 *             {@linkplain Annotation#ANNOTATION_SUPER_TYPE_NAME} as its super type
	 */
	public Annotation(Type type) {
		this(type, (String) null);
	}

	/**
	 * Create a new {@linkplain Annotation} instance
	 *
	 * @param type
	 *            the annotation type (must have the value of
	 *            {@linkplain Annotation#ANNOTATION_SUPER_TYPE_NAME} as its super type)
	 * @param value
	 *            the annotation value or {@literal null}
	 * @throws IllegalArgumentException
	 *             if the annotation type is not having the value of
	 *             {@linkplain Annotation#ANNOTATION_SUPER_TYPE_NAME} as its super type
	 */
	public Annotation(Type type, String value) {
		if (!isAnnotationType(type)) {
			throw new IllegalArgumentException();
		}
		this.type = type;
		this.attributes = value != null ? singleValue(value) : Collections.emptyMap();
	}

	/**
	 * Create a new {@linkplain Annotation} instance
	 *
	 * @param type
	 *            the annotation type (must have the value of
	 *            {@linkplain Annotation#ANNOTATION_SUPER_TYPE_NAME} as its super type)
	 * @param value
	 *            the annotation value or {@literal null}
	 * @param annotations
	 *            the annotations list attached to this annotation
	 * @throws IllegalArgumentException
	 *             if the annotation type is not having the value of
	 *             {@linkplain Annotation#ANNOTATION_SUPER_TYPE_NAME} as its super type
	 */
	public Annotation(Type type, String value, Annotation ... annotations) {
		if (!isAnnotationType(type)) {
			throw new IllegalArgumentException();
		}
		this.type = type;
		this.attributes = value != null ? singleValue(value) : Collections.emptyMap();
		this.annotations.addAll(Arrays.asList(annotations));
	}

	/**
	 * Create a new {@linkplain Annotation} instance
	 *
	 * @param type
	 *            the annotation type (must have the value of
	 *            {@linkplain Annotation#ANNOTATION_SUPER_TYPE_NAME} as its super type)
	 * @param attributes
	 *            the annotation attributes or {@literal null}
	 * @param annotations
	 *            the annotations list attached to this annotation
	 * @throws IllegalArgumentException
	 *             if the annotation type is not having the value of
	 *             {@linkplain Annotation#ANNOTATION_SUPER_TYPE_NAME} as its super type
	 */
	public Annotation(Type type, Map<String, ? extends TypedFieldValue> attributes, Annotation ... annotations) {
		if (!isAnnotationType(type)) {
			throw new IllegalArgumentException();
		}
		this.type = type;
		this.attributes = attributes;
	}

	/**
	 * Create a new {@linkplain Annotation} instance
	 *
	 * @param type
	 *            the annotation type (must have the value of
	 *            {@linkplain Annotation#ANNOTATION_SUPER_TYPE_NAME} as its super type)
	 * @param builderCallback
	 *            the annotation attributes builder callback or {@literal null}
	 * @param annotations
	 *            the annotations list attached to this annotation
	 * @throws IllegalArgumentException
	 *             if the annotation type is not having the value of
	 *             {@linkplain Annotation#ANNOTATION_SUPER_TYPE_NAME} as its super type
	 */
	public Annotation(Type type, Consumer<TypedValueBuilder> builderCallback, Annotation ... annotations) {
		if (!isAnnotationType(type)) {
			throw new IllegalArgumentException();
		}
		this.type = type;
		TypedValueBuilderImpl builder = new TypedValueBuilderImpl((TypeImpl) type);
		if (builderCallback != null) {
			builderCallback.accept(builder);
		}
		this.attributes = builder.build();
	}

	/**
	 * Check whether a particular {@linkplain Type} is an annotation type
	 *
	 * @param type
	 *            {@linkplain Type} to check
	 * @return true if the type extends {@linkplain java.lang.annotation.Annotation} type
	 */
	public static boolean isAnnotationType(Type type) {
		return ANNOTATION_SUPER_TYPE_NAME.equals(type.getSupertype());
	}

	/**
	 * Get the list of the associated {@link Annotation annotations}
	 *
	 * @return the associated {@link Annotation annotations}
	 */
	public List<Annotation> getAnnotations() {
		return annotations;
	}

	/**
	 * @return the annotation type
	 */
	public Type getType() {
		return type;
	}

	/**
	 * @return the annotation's attribute value or {@literal null} if the attribute is an array
	 */
	public String getValue() {
		return getValue(String.class, VALUE_KEY);
	}

	/**
	 * Get the attribute value by its name and type
	 *
	 * @param valueType
	 *            the expected value type
	 * @param name
	 *            the attribute name
	 * @param <T>
	 *            the expected value type
	 * @return the attribute value or {@literal null}
	 * @throws IllegalArgumentException
	 *             when the attribute's type is not assignable to the requested value type
	 */
	@SuppressWarnings("unchecked")
	public <T> T getValue(Class<T> valueType, String name) {
		TypedFieldValue fieldValue = attributes.get(name);
		if (fieldValue == null) {
			return null;
		}
		Object val = null;
		if (fieldValue.getField().isArray()) {
			TypedValue[] values = fieldValue.getValues();
			Object valArray = Array.newInstance(valueType.getComponentType(), values.length);
			for (int i = 0; i < values.length; i++) {
				Array.set(valArray, i, values[i].getValue());
			}
			val = valArray;
		} else {
			TypedValue typedValue = fieldValue.getValue();
			val = typedValue != null ? typedValue.getValue() : null;
		}

		if (val != null && !valueType.isAssignableFrom(val.getClass())) {
			throw new IllegalArgumentException();
		}
		return (T) val;
	}

	/**
	 * @return the annotation's attribute array value or {@literal null} if the attribute is not an
	 *         array
	 */
	public Map<String, ? extends TypedFieldValue> getAttributes() {
		return attributes;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Annotation that = (Annotation) o;
		return type.equals(that.type) && Objects.equals(attributes, that.attributes);
	}

	@Override
	public int hashCode() {
		if (hashCode == 0) {
			hashCode = NonZeroHashCode.hash(type, attributes);
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return "Annotation [type=" + type + ", attributes=" + attributes + "]";
	}

	private Map<String, ? extends TypedFieldValue> singleValue(String value) {
		TypeImpl targetType = (TypeImpl) type.getTypes().getType(Types.Builtin.STRING);
		TypedValueImpl typedValue = targetType.asValue(value);
		TypedFieldImpl typedField = new TypedFieldImpl(targetType, VALUE_KEY);
		return Collections.singletonMap(VALUE_KEY, new TypedFieldValueImpl(typedField, typedValue));
	}
}
