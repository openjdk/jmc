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
import org.openjdk.jmc.flightrecorder.writer.util.NonZeroHashCode;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Common JFR type super-class
 */
abstract class BaseType implements TypeImpl {
	int hashcode = 0;

	private final long id;
	private final String name;
	private final String supertype;
	private final ConstantPools constantPools;
	private final TypesImpl types;
	private final AtomicReference<TypedValueImpl> nullValue = new AtomicReference<>();

	BaseType(long id, String name, String supertype, ConstantPools constantPools, TypesImpl types) {
		this.id = id;
		this.name = name;
		this.supertype = supertype;
		this.constantPools = constantPools;
		this.types = types;
	}

	BaseType(long id, String name, ConstantPools constantPools, TypesImpl types) {
		this(id, name, null, constantPools, types);
	}

	@Override
	public long getId() {
		return id;
	}

	@Override
	public TypesImpl getTypes() {
		return types;
	}

	@Override
	public final boolean isSimple() {
		List<TypedFieldImpl> fields = getFields();
		if (fields.size() == 1) {
			TypedFieldImpl field = fields.get(0);
			return field.getType().isBuiltin() && !field.isArray();
		}
		return false;
	}

	@Override
	public boolean isResolved() {
		return true;
	}

	@Override
	public final String getTypeName() {
		return name;
	}

	@Override
	public boolean hasConstantPool() {
		return constantPools != null;
	}

	@Override
	public final String getSupertype() {
		return supertype;
	}

	@Override
	public TypedValueImpl asValue(String value) {
		if (hasConstantPool()) {
			return constantPools.forType(this).addOrGet(value);
		}
		return new TypedValueImpl(this, value);
	}

	@Override
	public TypedValueImpl asValue(byte value) {
		if (hasConstantPool()) {
			return constantPools.forType(this).addOrGet(value);
		}
		return new TypedValueImpl(this, value);
	}

	@Override
	public TypedValueImpl asValue(char value) {
		if (hasConstantPool()) {
			return constantPools.forType(this).addOrGet(value);
		}
		return new TypedValueImpl(this, value);
	}

	@Override
	public TypedValueImpl asValue(short value) {
		if (hasConstantPool()) {
			return constantPools.forType(this).addOrGet(value);
		}
		return new TypedValueImpl(this, value);
	}

	@Override
	public TypedValueImpl asValue(int value) {
		if (hasConstantPool()) {
			return constantPools.forType(this).addOrGet(value);
		}
		return new TypedValueImpl(this, value);
	}

	@Override
	public TypedValueImpl asValue(long value) {
		if (hasConstantPool()) {
			return constantPools.forType(this).addOrGet(value);
		}
		return new TypedValueImpl(this, value);
	}

	@Override
	public TypedValueImpl asValue(float value) {
		if (hasConstantPool()) {
			return constantPools.forType(this).addOrGet(value);
		}
		return new TypedValueImpl(this, value);
	}

	@Override
	public TypedValueImpl asValue(double value) {
		if (hasConstantPool()) {
			return constantPools.forType(this).addOrGet(value);
		}
		return new TypedValueImpl(this, value);
	}

	@Override
	public TypedValueImpl asValue(boolean value) {
		if (hasConstantPool()) {
			return constantPools.forType(this).addOrGet(value);
		}
		return new TypedValueImpl(this, value);
	}

	@Override
	public TypedValueImpl asValue(Consumer<TypedValueBuilder> builderCallback) {
		if (isBuiltin()) {
			throw new IllegalArgumentException();
		}
		TypedValueImpl checkValue = new TypedValueImpl(this, builderCallback);
		if (hasConstantPool()) {
			return constantPools.forType(this).addOrGet(checkValue);
		}
		return checkValue;
	}

	@Override
	public TypedValueImpl asValue(Object value) {
		if (hasConstantPool()) {
			return constantPools.forType(this).addOrGet(value);
		}
		return new TypedValueImpl(this, value);
	}

	@Override
	public TypedValueImpl nullValue() {
		return nullValue.updateAndGet(v -> (v == null ? TypedValueImpl.ofNull(this) : v));
	}

	@Override
	public boolean isUsedBy(Type other) {
		if (other == null) {
			return false;
		}
		return isUsedBy(this, (TypeImpl) other, new HashSet<>());
	}

	private static boolean isUsedBy(TypeImpl type1, TypeImpl type2, HashSet<TypeImpl> track) {
		if (!track.add(type2)) {
			return false;
		}
		for (TypedFieldImpl typedField : type2.getFields()) {
			if (typedField.getType().equals(type1)) {
				return true;
			}
			if (isUsedBy(type1, typedField.getType(), track)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		BaseType baseType = (BaseType) o;
		return id == baseType.id && name.equals(baseType.name) && Objects.equals(supertype, baseType.supertype);
	}

	@Override
	public int hashCode() {
		if (hashcode == 0) {
			hashcode = NonZeroHashCode.hash(id, name, supertype);
		}
		return hashcode;
	}

	@Override
	public String toString() {
		return "BaseType [id=" + id + ", name=" + name + ", supertype=" + supertype + "]";
	}
}
