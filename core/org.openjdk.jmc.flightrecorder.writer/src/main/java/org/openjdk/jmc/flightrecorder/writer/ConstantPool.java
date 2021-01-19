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
import org.openjdk.jmc.flightrecorder.writer.api.TypedField;
import org.openjdk.jmc.flightrecorder.writer.api.TypedValue;

import java.util.HashMap;
import java.util.Map;

/** An in-memory map of distinct values of a certain {@linkplain Type} */
final class ConstantPool {
	private final TypeImpl type;
	private final Map<Object, TypedValueImpl> constantMap = new HashMap<>();
	private final Map<Long, TypedValueImpl> reverseMap = new HashMap<>();

	ConstantPool(TypeImpl type) {
		this.type = type;
	}

	/**
	 * Tries to add a new value
	 *
	 * @param value
	 *            the value
	 * @return the typed value representation - either created a-new or retrieved from the pool
	 */
	TypedValueImpl addOrGet(Object value) {
		if (value == null) {
			return type.nullValue();
		}
		return constantMap.computeIfAbsent(value, v -> {
			long index = constantMap.size() + 1; // index 0 is reserved for NULL encoding
			TypedValueImpl tValue;
			if (v instanceof TypedValue) {
				tValue = new TypedValueImpl((TypedValueImpl) v, index);
			} else {
				tValue = new TypedValueImpl(type, v, index);
			}
			reverseMap.put(index, tValue);
			return tValue;
		});
	}

	/**
	 * Get the value at the given index
	 *
	 * @param index
	 *            the value index
	 * @return the value or {@literal null}
	 */
	TypedValueImpl get(long index) {
		return reverseMap.get(index);
	}

	void writeTo(LEB128Writer writer) {
		writer.writeLong(type.getId()); // CP type ID
		writer.writeInt(constantMap.size()); // number of constants
		reverseMap.forEach((k, v) -> {
			writer.writeLong(k); // constant index
			writeValueType(writer, v, false);
		});
	}

	void writeValueType(LEB128Writer writer, TypedValueImpl typedValue, boolean useConstantPoolReferences) {
		if (typedValue == null) {
			throw new NullPointerException();
		}
		TypeImpl type = typedValue.getType();
		if (type.isBuiltin()) {
			writeBuiltinType(writer, typedValue, useConstantPoolReferences);
		} else {
			if (typedValue.isNull()) {
				writer.writeLong(0); // null value encoding
			} else {
				if (useConstantPoolReferences) {
					// only if cp-refs are allowed
					writer.writeLong(typedValue.getConstantPoolIndex());
				} else {
					for (TypedFieldValueImpl fieldValue : typedValue.getFieldValues()) {
						TypedField field = fieldValue.getField();
						if (field.isArray()) {
							writer.writeInt(fieldValue.getValues().length); // array length
							for (TypedValueImpl t : fieldValue.getValues()) {
								writeValueType(writer, t, t.getType().hasConstantPool());
							}
						} else {
							writeValueType(writer, fieldValue.getValue(), field.getType().hasConstantPool());
						}
					}
				}
			}
		}
	}

	void writeBuiltinType(LEB128Writer writer, TypedValueImpl typedValue, boolean useCp) {
		if (typedValue == null) {
			throw new NullPointerException();
		}

		if (!typedValue.getType().isBuiltin()) {
			throw new IllegalArgumentException();
		}

		TypeImpl type = typedValue.getType();
		Object value = typedValue.getValue();

		TypesImpl.Builtin builtin = TypesImpl.Builtin.ofType(type);

		switch (builtin) {
		case STRING: {
			if (useCp) {
				if (typedValue.isNull()) {
					writer.writeByte((byte) 0); // skip CP for NULL
				} else if (((String) typedValue.getValue()).isEmpty()) {
					writer.writeByte((byte) 1); // skip CP for empty string
				} else {
					writer.writeByte((byte) 2) // set constant-pool encoding
							.writeLong(typedValue.getConstantPoolIndex());
				}
			} else {
				writer.writeCompactUTF((String) value);
			}
			break;
		}
		case BYTE: {
			writer.writeBytes(value != null ? (byte) value : 0);
			break;
		}
		case CHAR: {
			writer.writeChar(value != null ? (char) value : 0);
			break;
		}
		case SHORT: {
			writer.writeShort(value != null ? (short) value : 0);
			break;
		}
		case INT: {
			writer.writeInt(value != null ? (int) value : 0);
			break;
		}
		case LONG: {
			writer.writeLong(value != null ? (long) value : 0);
			break;
		}
		case FLOAT: {
			writer.writeFloat(value != null ? (float) value : 0);
			break;
		}
		case DOUBLE: {
			writer.writeDouble(value != null ? (double) value : 0);
			break;
		}
		case BOOLEAN: {
			writer.writeBoolean(value != null && (boolean) value);
			break;
		}
		default: {
			throw new IllegalArgumentException("Unsupported built-in type " + type.getTypeName());
		}
		}
	}

	@Override
	public String toString() {
		return "ConstantPool [type=" + type + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((constantMap == null) ? 0 : constantMap.hashCode());
		result = prime * result + ((reverseMap == null) ? 0 : reverseMap.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConstantPool other = (ConstantPool) obj;
		if (constantMap == null) {
			if (other.constantMap != null)
				return false;
		} else if (!constantMap.equals(other.constantMap))
			return false;
		if (reverseMap == null) {
			if (other.reverseMap != null)
				return false;
		} else if (!reverseMap.equals(other.reverseMap))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}
}
