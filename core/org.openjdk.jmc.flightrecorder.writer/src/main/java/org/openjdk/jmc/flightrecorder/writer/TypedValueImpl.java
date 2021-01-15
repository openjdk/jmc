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

import org.openjdk.jmc.flightrecorder.writer.api.TypedValueBuilder;
import org.openjdk.jmc.flightrecorder.writer.api.TypedValue;
import org.openjdk.jmc.flightrecorder.writer.util.NonZeroHashCode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

final class TypedValueImpl implements TypedValue {
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
		this.type = type;
		this.value = value instanceof Map ? null : value;
		this.fields = value instanceof Map ? (Map<String, TypedFieldValueImpl>) value : Collections.emptyMap();
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
		this.fields = Collections.unmodifiableMap(other.fields);
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
				value = new TypedFieldValueImpl(field, field.getType().nullValue());
			}
			values.add(value);
		}
		return values;
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
}
