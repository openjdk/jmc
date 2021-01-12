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
import org.openjdk.jmc.flightrecorder.writer.util.NonZeroHashCode;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** A representation of a typed field with a name */
public final class TypedFieldImpl implements org.openjdk.jmc.flightrecorder.writer.api.TypedField {
	private int hashCode = 0;

	private final String name;
	private final TypeImpl type;
	private final boolean isArray;
	private final List<Annotation> annotations;

	TypedFieldImpl(TypeImpl type, String name) {
		this(type, name, false, Collections.emptyList());
	}

	TypedFieldImpl(TypeImpl type, String name, boolean isArray) {
		this(type, name, isArray, Collections.emptyList());
	}

	TypedFieldImpl(TypeImpl type, String name, boolean isArray, List<Annotation> annotations) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(name);
		Objects.requireNonNull(annotations);

		this.name = name;
		this.type = type;
		this.isArray = isArray;
		this.annotations = Collections.unmodifiableList(annotations);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public TypeImpl getType() {
		return type;
	}

	@Override
	public boolean isArray() {
		return isArray;
	}

	@Override
	public List<Annotation> getAnnotations() {
		return annotations;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TypedFieldImpl that = (TypedFieldImpl) o;
		return isArray == that.isArray && name.equals(that.name) && type.equals(that.type)
				&& annotations.equals(that.annotations);
	}

	@Override
	public int hashCode() {
		if (hashCode == 0) {
			hashCode = NonZeroHashCode.hash(name, type, isArray, annotations);
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return "TypedFieldImpl [hashCode=" + hashCode + ", name=" + name + ", type=" + type + ", isArray=" + isArray
				+ ", annotations=" + annotations + "]";
	}
}
