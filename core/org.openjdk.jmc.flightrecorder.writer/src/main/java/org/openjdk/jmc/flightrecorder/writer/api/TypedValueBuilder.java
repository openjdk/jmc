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

import java.util.Map;
import java.util.function.Consumer;

/** A fluent API for lazy initialization of a composite type value */
public interface TypedValueBuilder {
	Type getType();

	/**
	 * Put a named field value
	 *
	 * @param name
	 *            field name
	 * @param value
	 *            field value
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, byte value);

	/**
	 * Put a named field array of values
	 *
	 * @param name
	 *            field name
	 * @param values
	 *            field values
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, byte[] values);

	/**
	 * Put a named field value
	 *
	 * @param name
	 *            field name
	 * @param value
	 *            field value
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, char value);

	/**
	 * Put a named field array of values
	 *
	 * @param name
	 *            field name
	 * @param values
	 *            field values
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, char[] values);

	/**
	 * Put a named field value
	 *
	 * @param name
	 *            field name
	 * @param value
	 *            field value
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, short value);

	/**
	 * Put a named field array of values
	 *
	 * @param name
	 *            field name
	 * @param values
	 *            field values
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, short[] values);

	/**
	 * Put a named field value
	 *
	 * @param name
	 *            field name
	 * @param value
	 *            field value
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, int value);

	/**
	 * Put a named field array of values
	 *
	 * @param name
	 *            field name
	 * @param values
	 *            field values
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, int[] values);

	/**
	 * Put a named field value
	 *
	 * @param name
	 *            field name
	 * @param value
	 *            field value
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, long value);

	/**
	 * Put a named field array of values
	 *
	 * @param name
	 *            field name
	 * @param values
	 *            field values
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, long[] values);

	/**
	 * Put a named field value
	 *
	 * @param name
	 *            field name
	 * @param value
	 *            field value
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, float value);

	/**
	 * Put a named field array of values
	 *
	 * @param name
	 *            field name
	 * @param values
	 *            field values
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, float[] values);

	/**
	 * Put a named field value
	 *
	 * @param name
	 *            field name
	 * @param value
	 *            field value
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, double value);

	/**
	 * Put a named field array of values
	 *
	 * @param name
	 *            field name
	 * @param values
	 *            field values
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, double[] values);

	/**
	 * Put a named field value
	 *
	 * @param name
	 *            field name
	 * @param value
	 *            field value
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, boolean value);

	/**
	 * Put a named field array of values
	 *
	 * @param name
	 *            field name
	 * @param values
	 *            field values
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, boolean[] values);

	/**
	 * Put a named field value
	 *
	 * @param name
	 *            field name
	 * @param value
	 *            field value
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, String value);

	/**
	 * Put a named field array of values
	 *
	 * @param name
	 *            field name
	 * @param values
	 *            field values
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, String[] values);

	/**
	 * Put a named field value
	 *
	 * @param name
	 *            field name
	 * @param valueBuilder
	 *            field value builder
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, TypedValueBuilder valueBuilder);

	/**
	 * Put a named field value
	 *
	 * @param name
	 *            field name
	 * @param value
	 *            field value
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, TypedValue value);

	/**
	 * Put a named field array of values
	 *
	 * @param name
	 *            field name
	 * @param values
	 *            field values
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, TypedValue ... values);

	/**
	 * Put a named field lazily evaluated value
	 *
	 * @param name
	 *            field name
	 * @param fieldValueCallback
	 *            field value builder
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, Consumer<TypedValueBuilder> fieldValueCallback);

	/**
	 * Put a named field array of lazily evaluated values
	 *
	 * @param name
	 *            field name
	 * @param callback1
	 *            first field value builder callback
	 * @param callback2
	 *            second field value builder callback
	 * @param otherCallbacks
	 *            other field value builder callbacks field value builders
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putFields(
		String name, Consumer<TypedValueBuilder> callback1, Consumer<TypedValueBuilder> callback2,
		Consumer<TypedValueBuilder> ... otherCallbacks);

	Map<String, ? extends TypedFieldValue> build();
}
