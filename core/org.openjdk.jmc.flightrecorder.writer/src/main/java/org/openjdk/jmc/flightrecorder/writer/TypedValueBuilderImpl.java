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

import org.openjdk.jmc.flightrecorder.writer.api.Type;
import org.openjdk.jmc.flightrecorder.writer.api.TypedValueBuilder;
import org.openjdk.jmc.flightrecorder.writer.api.TypedValue;
import org.openjdk.jmc.flightrecorder.writer.api.Types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class TypedValueBuilderImpl implements TypedValueBuilder {
	private final Type type;
	private final TypesImpl types;
	private final Map<String, TypedFieldImpl> fieldMap;
	private final Map<String, TypedFieldValueImpl> fieldValueMap;

	public TypedValueBuilderImpl(TypeImpl type) {
		this.type = type;
		this.types = type.getTypes();
		fieldMap = type.getFields().stream().collect(Collectors.toMap(TypedFieldImpl::getName, typeField -> typeField));
		fieldValueMap = new HashMap<>();
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public TypedValueBuilder putField(String name, byte value) {
		return putField(name, types.getType(Types.Builtin.BYTE).asValue(value));
	}

	@Override
	public TypedValueBuilder putField(String name, byte[] values) {
		putArrayField(name, () -> {
			TypeImpl type = types.getType(TypesImpl.Builtin.BYTE);
			TypedValueImpl[] typedValues = new TypedValueImpl[values.length];
			for (int i = 0; i < values.length; i++) {
				typedValues[i] = type.asValue(values[i]);
			}
			return typedValues;
		});

		return this;
	}

	@Override
	public TypedValueBuilder putField(String name, char value) {
		return putField(name, types.getType(Types.Builtin.CHAR).asValue(value));
	}

	@Override
	public TypedValueBuilder putField(String name, char[] values) {
		putArrayField(name, () -> {
			TypeImpl type = types.getType(TypesImpl.Builtin.CHAR);
			TypedValueImpl[] typedValues = new TypedValueImpl[values.length];
			for (int i = 0; i < values.length; i++) {
				typedValues[i] = type.asValue(values[i]);
			}
			return typedValues;
		});

		return this;
	}

	@Override
	public TypedValueBuilder putField(String name, short value) {
		return putField(name, types.getType(TypesImpl.Builtin.SHORT).asValue(value));
	}

	@Override
	public TypedValueBuilder putField(String name, short[] values) {
		putArrayField(name, () -> {
			TypeImpl type = types.getType(TypesImpl.Builtin.SHORT);
			TypedValueImpl[] typedValues = new TypedValueImpl[values.length];
			for (int i = 0; i < values.length; i++) {
				typedValues[i] = type.asValue(values[i]);
			}
			return typedValues;
		});

		return this;
	}

	@Override
	public TypedValueBuilder putField(String name, int value) {
		return putField(name, types.getType(TypesImpl.Builtin.INT).asValue(value));
	}

	@Override
	public TypedValueBuilder putField(String name, int[] values) {
		putArrayField(name, () -> {
			TypeImpl type = types.getType(TypesImpl.Builtin.INT);
			TypedValueImpl[] typedValues = new TypedValueImpl[values.length];
			for (int i = 0; i < values.length; i++) {
				typedValues[i] = type.asValue(values[i]);
			}
			return typedValues;
		});

		return this;
	}

	@Override
	public TypedValueBuilder putField(String name, long value) {
		return putField(name, types.getType(TypesImpl.Builtin.LONG).asValue(value));
	}

	@Override
	public TypedValueBuilder putField(String name, long[] values) {
		putArrayField(name, () -> {
			TypeImpl type = types.getType(TypesImpl.Builtin.LONG);
			TypedValueImpl[] typedValues = new TypedValueImpl[values.length];
			for (int i = 0; i < values.length; i++) {
				typedValues[i] = type.asValue(values[i]);
			}
			return typedValues;
		});

		return this;
	}

	@Override
	public TypedValueBuilder putField(String name, float value) {
		return putField(name, types.getType(TypesImpl.Builtin.FLOAT).asValue(value));
	}

	@Override
	public TypedValueBuilder putField(String name, float[] values) {
		putArrayField(name, () -> {
			TypeImpl type = types.getType(TypesImpl.Builtin.FLOAT);
			TypedValueImpl[] typedValues = new TypedValueImpl[values.length];
			for (int i = 0; i < values.length; i++) {
				typedValues[i] = type.asValue(values[i]);
			}
			return typedValues;
		});

		return this;
	}

	@Override
	public TypedValueBuilder putField(String name, double value) {
		return putField(name, types.getType(TypesImpl.Builtin.DOUBLE).asValue(value));
	}

	@Override
	public TypedValueBuilder putField(String name, double[] values) {
		putArrayField(name, () -> {
			TypeImpl type = types.getType(TypesImpl.Builtin.DOUBLE);
			TypedValueImpl[] typedValues = new TypedValueImpl[values.length];
			for (int i = 0; i < values.length; i++) {
				typedValues[i] = type.asValue(values[i]);
			}
			return typedValues;
		});

		return this;
	}

	@Override
	public TypedValueBuilder putField(String name, boolean value) {
		return putField(name, types.getType(TypesImpl.Builtin.BOOLEAN).asValue(value));
	}

	@Override
	public TypedValueBuilderImpl putField(String name, boolean[] values) {
		putArrayField(name, () -> {
			TypeImpl type = types.getType(TypesImpl.Builtin.BOOLEAN);
			TypedValueImpl[] typedValues = new TypedValueImpl[values.length];
			for (int i = 0; i < values.length; i++) {
				typedValues[i] = type.asValue(values[i]);
			}
			return typedValues;
		});

		return this;
	}

	@Override
	public TypedValueBuilder putField(String name, String value) {
		return putField(name, types.getType(TypesImpl.Builtin.STRING).asValue(value));
	}

	@Override
	public TypedValueBuilder putField(String name, String[] values) {
		putArrayField(name, () -> {
			TypeImpl type = types.getType(TypesImpl.Builtin.STRING);
			TypedValueImpl[] typedValues = new TypedValueImpl[values.length];
			for (int i = 0; i < values.length; i++) {
				typedValues[i] = type.asValue(values[i]);
			}
			return typedValues;
		});

		return this;
	}

	@Override
	public TypedValueBuilder putField(String name, TypedValueBuilder valueBuilder) {
		return putField(name, new TypedValueImpl(valueBuilder));
	}

	@Override
	public TypedValueBuilder putField(String name, TypedValue ... values) {
		if (values.length > 0) {
			TypedValueImpl[] typedValues = new TypedValueImpl[values.length];
			System.arraycopy(values, 0, typedValues, 0, values.length);
			putArrayField(name, typedValues);
		}
		return this;
	}

	@Override
	public TypedValueBuilder putField(String name, TypedValue value) {
		TypedFieldImpl field = fieldMap.get(name);
		TypedValueImpl typedValue = (TypedValueImpl) value;
		if (field != null) {
			TypeImpl type = field.getType();
			if (type.isSimple()) {
				typedValue = wrapSimpleValue(type, typedValue);
			}
			if (field.getType().canAccept(typedValue)) {
				fieldValueMap.put(name, new TypedFieldValueImpl(field, typedValue));
			} else {
				throw new IllegalArgumentException();
			}
		}
		return this;
	}

	private TypedValueImpl wrapSimpleValue(TypeImpl targetType, TypedValueImpl value) {
		TypedFieldImpl valueField = targetType.getFields().get(0);
		TypeImpl fieldType = valueField.getType();
		if (fieldType.canAccept(value)) {
			value = targetType
					.asValue(new SingleFieldMap(valueField.getName(), new TypedFieldValueImpl(valueField, value)));
		} else {
			throw new IllegalArgumentException();
		}
		return value;
	}

	@Override
	public TypedValueBuilder putField(String name, Consumer<TypedValueBuilder> fieldValueCallback) {
		TypedFieldImpl field = fieldMap.get(name);
		if (field != null) {
			fieldValueMap.put(name, new TypedFieldValueImpl(field, field.getType().asValue(fieldValueCallback)));
		}
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public TypedValueBuilder putFields(
		String name, Consumer<TypedValueBuilder> callback1, Consumer<TypedValueBuilder> callback2,
		Consumer<TypedValueBuilder> ... otherCallbacks) {
		buildArrayField(name, () -> {
			List<Consumer<TypedValueBuilder>> callbacks = new ArrayList<>(2 + otherCallbacks.length);
			callbacks.add(callback1);
			callbacks.add(callback2);
			callbacks.addAll(Arrays.asList(otherCallbacks));
			return callbacks.toArray(new Consumer[0]);
		});
		return this;
	}

	@Override
	public Map<String, TypedFieldValueImpl> build() {
		return Collections.unmodifiableMap(fieldValueMap);
	}

	private void putArrayField(String name, TypedValueImpl[] values) {
		TypedFieldImpl field = fieldMap.get(name);
		if (field != null) {
			putArrayField(field, values);
		} else {
			throw new IllegalArgumentException();
		}
	}

	private void putArrayField(TypedFieldImpl field, TypedValueImpl[] values) {
		TypeImpl fieldType = field.getType();
		for (TypedValueImpl value : values) {
			if (!fieldType.canAccept(value)) {
				throw new IllegalArgumentException();
			}
		}
		fieldValueMap.put(field.getName(), new TypedFieldValueImpl(field, values));
	}

	private void putArrayField(String name, Supplier<TypedValueImpl[]> valueSupplier) {
		TypedFieldImpl field = fieldMap.get(name);
		if (field != null) {
			if (!field.isArray()) {
				throw new IllegalArgumentException();
			}
			putArrayField(field, valueSupplier.get());
		} else {
			throw new IllegalArgumentException();
		}
	}

	private void buildArrayField(String name, Supplier<Consumer<TypedValueBuilder>[]> builderSupplier) {
		TypedFieldImpl field = fieldMap.get(name);
		if (field != null) {
			if (field.isArray()) {
				Consumer<TypedValueBuilder>[] builders = builderSupplier.get();
				TypedValueImpl[] values = new TypedValueImpl[builders.length];
				TypeImpl fieldType = field.getType();
				for (int i = 0; i < builders.length; i++) {
					values[i] = fieldType.asValue(builders[i]);
				}
				fieldValueMap.put(field.getName(), new TypedFieldValueImpl(field, values));
			} else {
				throw new IllegalArgumentException();
			}
		} else {
			throw new IllegalArgumentException();
		}
	}
}
