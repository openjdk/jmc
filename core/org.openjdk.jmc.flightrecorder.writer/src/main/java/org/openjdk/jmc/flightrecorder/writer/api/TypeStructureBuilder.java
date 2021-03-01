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

import org.openjdk.jmc.flightrecorder.writer.api.Types.Predefined;

import java.util.function.Consumer;

/** A fluent API for building composite types lazily. */
public interface TypeStructureBuilder {
	/**
	 * Add a field of the given name and (predefined) type
	 *
	 * @param name
	 *            the field name
	 * @param type
	 *            the field type
	 * @return a {@linkplain TypeStructureBuilder} instance for invocation chaining
	 */
	TypeStructureBuilder addField(String name, Predefined type);

	/**
	 * Add a field of the given name and (predefined) type and with a customization callback
	 *
	 * @param name
	 *            the field name
	 * @param type
	 *            the field type
	 * @param fieldCallback
	 *            the field customization callback
	 * @return a {@linkplain TypeStructureBuilder} instance for invocation chaining
	 */
	TypeStructureBuilder addField(String name, Predefined type, Consumer<TypedFieldBuilder> fieldCallback);

	/**
	 * Add a field of the given name and type
	 *
	 * @param name
	 *            the field name
	 * @param type
	 *            the field type
	 * @return a {@linkplain TypeStructureBuilder} instance for invocation chaining
	 */
	TypeStructureBuilder addField(String name, Type type);

	/**
	 * Add a field of the given name and type and with a customization callback
	 *
	 * @param name
	 *            the field name
	 * @param type
	 *            the field type
	 * @param fieldCallback
	 *            the field customization callback
	 * @return a {@linkplain TypeStructureBuilder} instance for invocation chaining
	 */
	TypeStructureBuilder addField(String name, Type type, Consumer<TypedFieldBuilder> fieldCallback);

	/**
	 * Add a specific field.
	 *
	 * @param field
	 *            field
	 * @return a {@linkplain TypeStructureBuilder} instance for invocation chaining
	 */
	TypeStructureBuilder addField(TypedField field);

	/**
	 * Add specific fields.
	 *
	 * @param field1
	 *            first field
	 * @param field2
	 *            second field
	 * @param fields
	 *            other fields (if any)
	 * @return a {@linkplain TypeStructureBuilder} instance for invocation chaining
	 */
	TypeStructureBuilder addFields(TypedField field1, TypedField field2, TypedField ... fields);

	/**
	 * Add an annotation of the given type
	 *
	 * @param type
	 *            the annotation type
	 * @return a {@linkplain TypeStructureBuilder} instance for invocation chaining
	 */
	TypeStructureBuilder addAnnotation(Type type);

	/**
	 * Add an annotation of the given type and with the given value
	 *
	 * @param type
	 *            the annotation type
	 * @param value
	 *            the annotation value
	 * @return a {@linkplain TypeStructureBuilder} instance for invocation chaining
	 */
	TypeStructureBuilder addAnnotation(Type type, String value);

	/**
	 * A special placeholder type to refer to the type being currently built (otherwise impossible
	 * because the type is not yet ready).
	 *
	 * @return a special {@linkplain Type} denoting 'self' reflecting type
	 */
	Type selfType();

	/**
	 * @return built {@linkplain TypeStructure}
	 */
	TypeStructure build();

	/**
	 * A shortcut to build-and-register functionality
	 * 
	 * @param name
	 *            the type name
	 * @param supertype
	 *            the supertype name
	 * @return a new {@linkplain Type} instance
	 */
	Type registerAs(String name, String supertype);
}
