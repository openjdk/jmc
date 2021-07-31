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
import org.openjdk.jmc.flightrecorder.writer.api.Type;
import org.openjdk.jmc.flightrecorder.writer.api.TypedFieldBuilder;
import org.openjdk.jmc.flightrecorder.writer.api.TypedValueBuilder;
import org.openjdk.jmc.flightrecorder.writer.api.Types;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class TypedFieldBuilderImpl implements TypedFieldBuilder {
	private final TypesImpl types;
	private final List<Annotation> annotations = new ArrayList<>();
	private final TypeImpl type;
	private final String name;
	private boolean asArray;

	TypedFieldBuilderImpl(String name, TypeImpl type, TypesImpl types) {
		this.type = type;
		this.name = name;
		this.types = types;
	}

	@Override
	public TypedFieldBuilderImpl addAnnotation(Type type) {
		return addAnnotation(type, (String) null);
	}

	@Override
	public TypedFieldBuilderImpl addAnnotation(Type type, String value) {
		annotations.add(new Annotation(type, value));
		return this;
	}

	@Override
	public TypedFieldBuilderImpl addAnnotation(TypesImpl.Predefined type) {
		return addAnnotation(types.getType(type));
	}

	@Override
	public TypedFieldBuilderImpl addAnnotation(Types.Predefined type, String value) {
		return addAnnotation(types.getType(type), value);
	}

	@Override
	public TypedFieldBuilder addAnnotation(Type type, Consumer<TypedValueBuilder> builderCallback) {
		TypedValueBuilderImpl impl = new TypedValueBuilderImpl((TypeImpl) type);
		if (builderCallback != null) {
			builderCallback.accept(impl);
		}
		annotations.add(new Annotation(type, impl.build()));
		return this;
	}

	@Override
	public TypedFieldBuilder addAnnotation(Types.Predefined type, Consumer<TypedValueBuilder> builderCallback) {
		return addAnnotation(types.getType(type), builderCallback);
	}

	@Override
	public TypedFieldBuilderImpl asArray() {
		asArray = true;
		return this;
	}

	@Override
	public TypedFieldImpl build() {
		return new TypedFieldImpl(type, name, asArray, annotations);
	}
}
