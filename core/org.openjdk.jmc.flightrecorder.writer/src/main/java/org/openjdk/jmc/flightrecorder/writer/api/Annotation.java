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
package org.openjdk.jmc.flightrecorder.writer.api;

import org.openjdk.jmc.flightrecorder.writer.util.NonZeroHashCode;

import java.util.Objects;

/** A struct-like representation of a JFR annotation */
public final class Annotation {
	public static final String ANNOTATION_SUPER_TYPE_NAME = "java.lang.annotation.Annotation";
	private int hashCode = 0;

	private final Type type;
	private final String value;

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
		this.value = value;
	}

	public static boolean isAnnotationType(Type type) {
		return ANNOTATION_SUPER_TYPE_NAME.equals(type.getSupertype());
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
		return type.equals(that.type) && Objects.equals(value, that.value);
	}

	@Override
	public int hashCode() {
		if (hashCode == 0) {
			hashCode = NonZeroHashCode.hash(type, value);
		}
		return hashCode;
	}

	public Type getType() {
		return type;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "Annotation [type=" + type + ", value=" + value + "]";
	}
}
