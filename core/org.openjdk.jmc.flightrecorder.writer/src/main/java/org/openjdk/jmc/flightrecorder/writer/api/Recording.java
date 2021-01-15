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

import org.openjdk.jmc.flightrecorder.writer.RecordingImpl;
import org.openjdk.jmc.flightrecorder.writer.TypesImpl;

import java.io.IOException;
import java.util.function.Consumer;

public abstract class Recording implements AutoCloseable {
	public abstract RecordingImpl rotateChunk();

	/**
	 * Write a custom event
	 *
	 * @param event
	 *            the event value
	 * @return {@literal this} for chaining
	 * @throws IllegalArgumentException
	 *             if the event type has not got 'jdk.jfr.Event' as its super type
	 */
	public abstract RecordingImpl writeEvent(TypedValue event);

	/**
	 * Try registering a user event type with no additional attributes. If a same-named event
	 * already exists it will be returned.
	 *
	 * @param name
	 *            the event name
	 * @return a user event type of the given name
	 */
	public abstract Type registerEventType(String name);

	/**
	 * Try registering a user event type. If a same-named event already exists it will be returned.
	 *
	 * @param name
	 *            the event name
	 * @param builderCallback
	 *            will be called with the active {@linkplain TypeStructureBuilder} when the event is
	 *            newly registered
	 * @return a user event type of the given name
	 * @throws IllegalArgumentException
	 *             if 'name' or 'builderCallback' is {@literal null}
	 */
	public abstract Type registerEventType(String name, Consumer<TypeStructureBuilder> builderCallback);

	/**
	 * Try registering a user annotation type. If a same-named annotation already exists it will be
	 * returned.
	 *
	 * @param name
	 *            the annotation name
	 * @return a user annotation type of the given name
	 * @throws IllegalArgumentException
	 *             if 'name' is {@literal null}
	 */
	public abstract Type registerAnnotationType(String name);

	/**
	 * Try registering a user annotation type. If a same-named annotation already exists it will be
	 * returned.
	 *
	 * @param name
	 *            the annotation name
	 * @param builderCallback
	 *            will be called with the active {@linkplain TypeStructureBuilder} when the
	 *            annotation is newly registered
	 * @return a user annotation type of the given name
	 * @throws IllegalArgumentException
	 *             if 'name' or 'builderCallback' is {@literal null}
	 */
	public abstract Type registerAnnotationType(String name, Consumer<TypeStructureBuilder> builderCallback);

	/**
	 * Try registering a custom type. If a same-named type already exists it will be returned.
	 *
	 * @param name
	 *            the type name
	 * @param builderCallback
	 *            will be called with the active {@linkplain TypeStructureBuilder} when the type is
	 *            newly registered
	 * @return a custom type of the given name
	 * @throws IllegalArgumentException
	 *             if 'name' or 'builderCallback' is {@literal null}
	 */
	public abstract Type registerType(String name, Consumer<TypeStructureBuilder> builderCallback);

	/**
	 * Try registering a custom type. If a same-named type already exists it will be returned.
	 *
	 * @param name
	 *            the type name
	 * @param supertype
	 *            the super type name
	 * @param builderCallback
	 *            will be called with the active {@linkplain TypeStructureBuilder} when the type is
	 *            newly registered
	 * @return a custom type of the given name
	 * @throws IllegalArgumentException
	 *             if 'name' or 'builderCallback' is {@literal null}
	 */
	public abstract Type registerType(String name, String supertype, Consumer<TypeStructureBuilder> builderCallback);

	/**
	 * A convenience method to easily get to JDK registered custom types in type-safe manner.
	 *
	 * @param type
	 *            the type
	 * @return the previously registered JDK type
	 * @throws IllegalArgumentException
	 *             if 'type' is {@literal null} or an attempt to retrieve non-registered JDK type is
	 *             made
	 */
	public abstract Type getType(TypesImpl.JDK type);

	/**
	 * Try retrieving a previously registered custom type.
	 *
	 * @param typeName
	 *            the type name
	 * @return the previously registered custom type
	 * @throws IllegalArgumentException
	 *             if 'typeName' is {@literal null} or an attempt to retrieve non-registered custom
	 *             type is made
	 */
	public abstract Type getType(String typeName);

	/**
	 * @return the associated {@linkplain Types} instance
	 */
	public abstract Types getTypes();

	@Override
	public abstract void close() throws IOException;
}
