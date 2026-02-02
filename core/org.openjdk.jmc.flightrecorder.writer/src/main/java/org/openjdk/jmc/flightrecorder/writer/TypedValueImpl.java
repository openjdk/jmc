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

import org.openjdk.jmc.flightrecorder.writer.api.Annotation;
import org.openjdk.jmc.flightrecorder.writer.api.TypedValueBuilder;
import org.openjdk.jmc.flightrecorder.writer.api.TypedValue;
import org.openjdk.jmc.flightrecorder.writer.util.NonZeroHashCode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class TypedValueImpl implements TypedValue {
	private int hashcode = 0;

	private final TypeImpl type;
	private final Object value;
	private final Map<String, TypedFieldValueImpl> fields;
	private final boolean isNull;
	private final long cpIndex;

	@SuppressWarnings("unchecked")
	TypedValueImpl(TypeImpl type, Object value, long cpIndex) {
		if (!type.canAccept(value)) {
			throw new IllegalArgumentException();
		}
		Map<String, TypedFieldValueImpl> valueMap = value instanceof Map ? (Map<String, TypedFieldValueImpl>) value
				: null;
		if (valueMap == null && type.isSimple()) {
			valueMap = wrapSimpleValueField(type, value);
		}
		this.type = type;
		this.value = valueMap == null ? value : null;
		this.fields = valueMap != null ? valueMap : Collections.emptyMap();
		this.isNull = value == null;
		this.cpIndex = cpIndex;
	}

	TypedValueImpl(TypeImpl type, Consumer<TypedValueBuilder> builderCallback) {
		this(Objects.requireNonNull(type), getFieldValues(type, Objects.requireNonNull(builderCallback)));
	}

	TypedValueImpl(TypedValueBuilder builder) {
		this((TypeImpl) builder.getType(), builder.build());
	}

	private static Map<String, TypedFieldValueImpl> getFieldValues(
		TypeImpl type, Consumer<TypedValueBuilder> builderCallback) {
		TypedValueBuilderImpl access = new TypedValueBuilderImpl(type);
		builderCallback.accept(access);
		return access.build();
	}

	public TypedValueImpl(TypeImpl type, Object value) {
		this(type, value, Long.MIN_VALUE);
	}

	protected TypedValueImpl(TypedValueImpl other, long cpIndex) {
		if (other.getType().isBuiltin()) {
			throw new IllegalArgumentException("Value of built-in types can not reside in constant pool");
		}
		this.type = other.type;
		this.value = other.value;
		this.fields = other.fields;
		this.isNull = other.isNull;
		this.hashcode = other.hashcode;
		this.cpIndex = cpIndex;
	}

	/**
	 * A factory method for properly creating an instance of {@linkplain TypedValue} holding
	 * {@literal
	 * null} value
	 *
	 * @param type
	 *            the value type
	 * @return a null {@linkplain TypedValue} instance
	 */
	static TypedValueImpl ofNull(TypeImpl type) {
		if (!type.canAccept(null)) {
			throw new IllegalArgumentException();
		}
		return new TypedValueImpl(type, (Object) null);
	}

	/** @return the type */
	public TypeImpl getType() {
		return type;
	}

	/** @return the wrapped value */
	public Object getValue() {
		return type.isSimple() ? getFieldValues().get(0).getValue().getValue() : value;
	}

	/** @return {@literal true} if this holds {@literal null} value */
	public boolean isNull() {
		return isNull;
	}

	/** @return the field values structure */
	public List<TypedFieldValueImpl> getFieldValues() {
		if (isNull) {
			throw new NullPointerException();
		}

		List<TypedFieldValueImpl> values = new ArrayList<>(fields.size());
		for (TypedFieldImpl field : type.getFields()) {
			TypedFieldValueImpl value = fields.get(field.getName());
			if (value == null) {
				value = new TypedFieldValueImpl(field, getDefaultImplicitFieldValue(field));
			}
			values.add(value);
		}
		return values;
	}

	/**
	 * Gets the default value for a field when not explicitly provided by the user.
	 * <p>
	 * For event types (jdk.jfr.Event):
	 * <ul>
	 * <li>Fields annotated with {@code @Timestamp} receive {@link System#nanoTime()} as default,
	 * providing a monotonic timestamp that will be >= the chunk's startTicks</li>
	 * <li>Other fields receive null values</li>
	 * </ul>
	 * <p>
	 * Note: JFR timestamps are stored as ticks relative to the chunk start, so the parser will
	 * convert this absolute tick value to chunk-relative during reading.
	 * <p>
	 * <strong>Tick Frequency Assumption:</strong> This implementation assumes a 1:1 tick frequency
	 * (1 tick = 1 nanosecond) as currently hardcoded in {@code RecordingImpl}. If the tick
	 * frequency becomes configurable in the future, {@link System#nanoTime()} values will need to
	 * be converted to ticks using: {@code nanoTime * ticksPerSecond / 1_000_000_000L}.
	 *
	 * @param field
	 *            the field to get default value for
	 * @return the default value for the field
	 */
	private TypedValueImpl getDefaultImplicitFieldValue(TypedFieldImpl field) {
		if (!"jdk.jfr.Event".equals(type.getSupertype())) {
			return field.getType().nullValue();
		}

		// Check if field is annotated with @Timestamp (any value means it's chunk-relative)
		if (hasTimestampAnnotation(field)) {
			// Use current nanoTime as default - will be valid and >= chunk startTicks
			// NOTE: Assumes 1:1 tick frequency (1 tick = 1 ns) as per RecordingImpl line 280
			return field.getType().asValue(System.nanoTime());
		}

		// For all other fields, return null value
		// Null builtin values are handled properly by Chunk.writeBuiltinType()
		return field.getType().nullValue();
	}

	/**
	 * Checks if a field has the {@code @Timestamp} annotation.
	 *
	 * @param field
	 *            the field to check
	 * @return true if the field is annotated with @Timestamp
	 */
	private boolean hasTimestampAnnotation(TypedFieldImpl field) {
		for (Annotation annotation : field.getAnnotations()) {
			if ("jdk.jfr.Timestamp".equals(annotation.getType().getTypeName())) {
				return true;
			}
		}
		return false;
	}

	long getConstantPoolIndex() {
		return cpIndex;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		TypedValueImpl that = (TypedValueImpl) o;
		return isNull == that.isNull && type.equals(that.type) && Objects.equals(value, that.value)
				&& fields.equals(that.fields);
	}

	@Override
	public int hashCode() {
		if (hashcode == 0) {
			hashcode = NonZeroHashCode.hash(type, value, fields, isNull);
		}
		return hashcode;
	}

	@Override
	public String toString() {
		return "TypedValueImpl{" + "type=" + type + ", value=" + value + ", fields=" + fields + ", isNull=" + isNull
				+ ", cpIndex=" + cpIndex + '}';
	}

	static TypedValueImpl wrapSimpleValueField(TypeImpl targetType, TypedValueImpl value) {
		if (value.getType().isBuiltin()) {
			TypedFieldImpl valueField = targetType.getFields().get(0);
			TypeImpl fieldType = valueField.getType();
			if (fieldType.canAccept(value)) {
				value = targetType
						.asValue(new SingleFieldMap(valueField.getName(), new TypedFieldValueImpl(valueField, value)));
			} else {
				throw new IllegalArgumentException();
			}
		}
		return value;
	}

	static Map<String, TypedFieldValueImpl> wrapSimpleValueField(TypeImpl targetType, Object value) {
		TypedFieldImpl valueField = targetType.getFields().get(0);
		TypeImpl fieldType = valueField.getType();
		if (fieldType.canAccept(value)) {
			return new SingleFieldMap(valueField.getName(),
					new TypedFieldValueImpl(valueField, fieldType.asValue(value)));
		} else {
			throw new IllegalArgumentException();
		}
	}
}
