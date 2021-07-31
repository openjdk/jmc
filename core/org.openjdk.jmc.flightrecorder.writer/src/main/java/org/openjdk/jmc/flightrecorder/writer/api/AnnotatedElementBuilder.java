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

import java.util.function.Consumer;

public interface AnnotatedElementBuilder<T extends AnnotatedElementBuilder<?>> {
	/**
	 * Add an annotation of the given type
	 *
	 * @param type
	 *            the annotation type
	 * @return a {@linkplain AnnotatedElementBuilder} instance for invocation chaining
	 */
	T addAnnotation(Type type);

	/**
	 * Add an annotation of the given type and with the given value
	 *
	 * @param type
	 *            the annotation type
	 * @param value
	 *            the annotation value
	 * @return a {@linkplain AnnotatedElementBuilder} instance for invocation chaining
	 */
	T addAnnotation(Type type, String value);

	/**
	 * Add a predefined annotation
	 *
	 * @param type
	 *            predefined annotation type
	 * @return a {@linkplain AnnotatedElementBuilder} instance for invocation chaining
	 */
	T addAnnotation(Types.Predefined type);

	/**
	 * Add a predefined annotation with a value
	 *
	 * @param type
	 *            predefined annotation type
	 * @param value
	 *            annotation value
	 * @return a {@linkplain AnnotatedElementBuilder} instance for invocation chaining
	 */
	T addAnnotation(Types.Predefined type, String value);

	/**
	 * Add an annotation of the given type and with the given values array
	 *
	 * @param type
	 *            the annotation type
	 * @param builderCallback
	 *            the annotation attributes builder callback
	 * @return a {@linkplain AnnotatedElementBuilder} instance for invocation chaining
	 */
	T addAnnotation(Type type, Consumer<TypedValueBuilder> builderCallback);

	/**
	 * Add an annotation of the given type and with the given values array
	 *
	 * @param type
	 *            the annotation type
	 * @param builderCallback
	 *            the annotation attributes builder callback
	 * @return a {@linkplain AnnotatedElementBuilder} instance for invocation chaining
	 */
	T addAnnotation(Types.Predefined type, Consumer<TypedValueBuilder> builderCallback);
}
