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
import org.openjdk.jmc.flightrecorder.writer.api.TypeStructure;
import org.openjdk.jmc.flightrecorder.writer.api.TypeStructureBuilder;
import org.openjdk.jmc.flightrecorder.writer.api.Type;
import org.openjdk.jmc.flightrecorder.writer.api.TypedField;
import org.openjdk.jmc.flightrecorder.writer.api.TypedFieldBuilder;
import org.openjdk.jmc.flightrecorder.writer.api.Types;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class TypeStructureBuilderImpl implements TypeStructureBuilder {
	private final TypesImpl types;
	private final List<TypedFieldImpl> fieldList = new ArrayList<>();
	private final List<Annotation> annotations = new ArrayList<>();

	TypeStructureBuilderImpl(TypesImpl types) {
		this.types = types;
	}

	@Override
	public TypeStructureBuilderImpl addField(String name, TypesImpl.Predefined type) {
		return addField(name, type, null);
	}

	@Override
	public TypeStructureBuilderImpl addField(
		String name, Types.Predefined type, Consumer<TypedFieldBuilder> fieldCallback) {
		return addField(name, types.getType(type), fieldCallback);
	}

	@Override
	public TypeStructureBuilderImpl addField(String name, Type type) {
		return addField(name, type, null);
	}

	@Override
	public TypeStructureBuilderImpl addField(String name, Type type, Consumer<TypedFieldBuilder> fieldCallback) {
		TypedFieldBuilderImpl annotationsBuilder = new TypedFieldBuilderImpl(name, (TypeImpl) type, types);
		if (fieldCallback != null) {
			fieldCallback.accept(annotationsBuilder);
		}
		fieldList.add(annotationsBuilder.build());
		return this;
	}

	@Override
	public TypeStructureBuilder addField(TypedField field) {
		fieldList.add((TypedFieldImpl) field);
		return this;
	}

	@Override
	public TypeStructureBuilder addFields(TypedField field1, TypedField field2, TypedField ... fields) {
		fieldList.add((TypedFieldImpl) field1);
		fieldList.add((TypedFieldImpl) field2);
		for (TypedField field : fields) {
			fieldList.add((TypedFieldImpl) field);
		}
		return this;
	}

	@Override
	public TypeStructureBuilderImpl addAnnotation(Type type) {
		return addAnnotation(type, null);
	}

	@Override
	public TypeStructureBuilderImpl addAnnotation(Type type, String value) {
		annotations.add(new Annotation(type, value));
		return this;
	}

	@Override
	public Type selfType() {
		return SelfType.INSTANCE;
	}

	@Override
	public TypeStructure build() {
		return new TypeStructureImpl(fieldList, annotations);
	}

	public Type registerAs(String name, String supertype) {
		return types.getOrAdd(name, supertype, build());
	}
}
