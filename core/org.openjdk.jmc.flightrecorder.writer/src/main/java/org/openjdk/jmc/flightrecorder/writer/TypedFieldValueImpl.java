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

import org.openjdk.jmc.flightrecorder.writer.api.TypedFieldValue;
import org.openjdk.jmc.flightrecorder.writer.util.NonZeroHashCode;

import java.util.Arrays;

public final class TypedFieldValueImpl implements TypedFieldValue {
	private int hashCode = 0;

	private final TypedFieldImpl field;
	private final TypedValueImpl[] values;

	public TypedFieldValueImpl(TypedFieldImpl field, TypedValueImpl value) {
		this(field, new TypedValueImpl[] {value});
	}

	public TypedFieldValueImpl(TypedFieldImpl field, TypedValueImpl[] values) {
		if (values == null) {
			values = new TypedValueImpl[0];
		}
		if (!field.isArray() && values.length > 1) {
			throw new IllegalArgumentException();
		}
		this.field = field;
		this.values = values;
	}

	/** @return the corresponding {@linkplain TypedFieldImpl} */
	public TypedFieldImpl getField() {
		return field;
	}

	/**
	 * @return the associated value
	 * @throws IllegalArgumentException
	 *             if the field is an array
	 */
	public TypedValueImpl getValue() {
		if (field.isArray()) {
			throw new IllegalArgumentException();
		}
		return values[0];
	}

	/**
	 * @return the associated values
	 * @throws IllegalArgumentException
	 *             if the field is not an array
	 */
	public TypedValueImpl[] getValues() {
		if (!field.isArray()) {
			throw new IllegalArgumentException();
		}
		return Arrays.copyOf(values, values.length);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TypedFieldValueImpl that = (TypedFieldValueImpl) o;
		return field.equals(that.field) && Arrays.equals(values, that.values);
	}

	@Override
	public int hashCode() {
		if (hashCode == 0) {
			Object[] objValues = new Object[values.length + 1];
			System.arraycopy(values, 0, objValues, 1, values.length);
			objValues[0] = field;
			hashCode = NonZeroHashCode.hash(objValues);
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return "TypedFieldValueImpl{" + "field=" + field + ", values=" + Arrays.toString(values) + '}';
	}
}
