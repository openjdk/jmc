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

import org.openjdk.jmc.flightrecorder.writer.api.Annotation;
import org.openjdk.jmc.flightrecorder.writer.api.Types;
import org.openjdk.jmc.flightrecorder.writer.util.NonZeroHashCode;

import java.util.Collections;
import java.util.List;

/** A built-in type. Corresponds to a Java primitive type or {@link String String} */
final class BuiltinType extends BaseType {
	private int hashcode = 0;

	private final Types.Builtin builtin;

	BuiltinType(long id, Types.Builtin type, ConstantPools constantPools, TypesImpl types) {
		super(id, type.getTypeName(), constantPools, types);
		this.builtin = type;
	}

	@Override
	public boolean isBuiltin() {
		return true;
	}

	@Override
	public List<TypedFieldImpl> getFields() {
		return Collections.emptyList();
	}

	@Override
	public TypedFieldImpl getField(String name) {
		throw new IllegalArgumentException();
	}

	@Override
	public List<Annotation> getAnnotations() {
		return Collections.emptyList();
	}

	@Override
	public boolean canAccept(Object value) {
		if (value == null) {
			// non-initialized built-ins will get the default value and String will be properly handled
			return true;
		}

		if (value instanceof TypedValueImpl) {
			return this == ((TypedValueImpl) value).getType();
		}
		switch (builtin) {
		case STRING: {
			return (value instanceof String);
		}
		case BYTE: {
			return value instanceof Byte;
		}
		case CHAR: {
			return value instanceof Character;
		}
		case SHORT: {
			return value instanceof Short;
		}
		case INT: {
			return value instanceof Integer;
		}
		case LONG: {
			return value instanceof Long;
		}
		case FLOAT: {
			return value instanceof Float;
		}
		case DOUBLE: {
			return value instanceof Double;
		}
		case BOOLEAN: {
			return value instanceof Boolean;
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
		if (!super.equals(o)) {
			return false;
		}
		BuiltinType that = (BuiltinType) o;
		return builtin == that.builtin;
	}

	@Override
	public int hashCode() {
		if (hashcode == 0) {
			hashcode = NonZeroHashCode.hash(super.hashCode(), builtin);
		}
		return hashcode;
	}

	@Override
	public String toString() {
		return "BuiltinType [builtin=" + builtin + "]";
	}
}
