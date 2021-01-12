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

import org.openjdk.jmc.flightrecorder.writer.TypedFieldImpl;

/** A fluent API for building typed fields lazily. */
public interface TypedFieldBuilder {
	/**
	 * Add an annotation
	 *
	 * @param type
	 *            annotation type
	 * @return a {@linkplain TypedFieldBuilder} instance for invocation chaining
	 */
	TypedFieldBuilder addAnnotation(Type type);

	/**
	 * Add an annotation with a value
	 *
	 * @param type
	 *            annotation type
	 * @param value
	 *            annotation value
	 * @return a {@linkplain TypedFieldBuilder} instance for invocation chaining
	 */
	TypedFieldBuilder addAnnotation(Type type, String value);

	/**
	 * Add a predefined annotation
	 *
	 * @param type
	 *            predefined annotation type
	 * @return a {@linkplain TypedFieldBuilder} instance for invocation chaining
	 */
	TypedFieldBuilder addAnnotation(Types.Predefined type);

	/**
	 * Add a predefined annotation with a value
	 *
	 * @param type
	 *            predefined annotation type
	 * @param value
	 *            annotation value
	 * @return a {@linkplain TypedFieldBuilder} instance for invocation chaining
	 */
	TypedFieldBuilder addAnnotation(Types.Predefined type, String value);

	/**
	 * Mark the field as holding an array value
	 *
	 * @return a {@linkplain TypedFieldBuilder} instance for invocation chaining
	 */
	TypedFieldBuilder asArray();

	TypedFieldImpl build();
}
