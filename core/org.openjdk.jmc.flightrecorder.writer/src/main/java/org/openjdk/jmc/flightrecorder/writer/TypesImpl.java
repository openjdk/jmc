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

import org.openjdk.jmc.flightrecorder.writer.api.TypeStructure;
import org.openjdk.jmc.flightrecorder.writer.api.TypeStructureBuilder;
import org.openjdk.jmc.flightrecorder.writer.api.Type;
import org.openjdk.jmc.flightrecorder.writer.api.TypedFieldBuilder;
import org.openjdk.jmc.flightrecorder.writer.api.Types;

import java.util.function.Consumer;

import static org.openjdk.jmc.flightrecorder.writer.api.Annotation.ANNOTATION_SUPER_TYPE_NAME;
import static org.openjdk.jmc.flightrecorder.writer.api.Types.Builtin.*;

/** An access class for various {@linkplain Type} related operations */
public final class TypesImpl extends Types {
	private final MetadataImpl metadata;

	TypesImpl(MetadataImpl metadata) {
		metadata.setTypes(this);
		this.metadata = metadata;

		registerBuiltins();
		registerJdkTypes();
		this.metadata.resolveTypes(); // resolve all back-referenced types
	}

	private void registerBuiltins() {
		metadata.registerBuiltin(BYTE);
		metadata.registerBuiltin(CHAR);
		metadata.registerBuiltin(SHORT);
		metadata.registerBuiltin(INT);
		metadata.registerBuiltin(LONG);
		metadata.registerBuiltin(FLOAT);
		metadata.registerBuiltin(DOUBLE);
		metadata.registerBuiltin(BOOLEAN);
		metadata.registerBuiltin(STRING);
	}

	private void registerJdkTypes() {
		TypeImpl annotationNameType = getOrAdd(JDK.ANNOTATION_NAME, ANNOTATION_SUPER_TYPE_NAME, builder -> {
			builder.addField("value", Builtin.STRING);
		});
		TypeImpl annotationLabelType = getOrAdd(JDK.ANNOTATION_LABEL, ANNOTATION_SUPER_TYPE_NAME, builder -> {
			builder.addField("value", Builtin.STRING);
		});
		TypeImpl annotationDescriptionType = getOrAdd(JDK.ANNOTATION_DESCRIPTION, ANNOTATION_SUPER_TYPE_NAME,
				builder -> {
					builder.addField("value", Builtin.STRING);
				});
		TypeImpl annotationContentTypeType = getOrAdd(JDK.ANNOTATION_CONTENT_TYPE, ANNOTATION_SUPER_TYPE_NAME,
				builder -> {
				});
		getOrAdd(JDK.ANNOTATION_TIMESTAMP, ANNOTATION_SUPER_TYPE_NAME, builder -> {
			builder.addField("value", Builtin.STRING).addAnnotation(annotationNameType, "jdk.jfr.Timestamp")
					.addAnnotation(annotationContentTypeType, null).addAnnotation(annotationLabelType, "Timestamp")
					.addAnnotation(annotationDescriptionType, "A point in time");
		});
		getOrAdd(JDK.ANNOTATION_TIMESPAN, ANNOTATION_SUPER_TYPE_NAME, builder -> {
			builder.addField("value", Builtin.STRING).addAnnotation(annotationNameType, "jdk.jfr.Timespan")
					.addAnnotation(annotationContentTypeType, null).addAnnotation(annotationLabelType, "Timespan")
					.addAnnotation(annotationDescriptionType, "A duration, measured in nanoseconds by default");
		});
		getOrAdd(JDK.ANNOTATION_UNSIGNED, ANNOTATION_SUPER_TYPE_NAME, builder -> {
			builder.addField("value", Builtin.STRING).addAnnotation(annotationNameType, "jdk.jfr.Unsigned")
					.addAnnotation(annotationContentTypeType, null).addAnnotation(annotationLabelType, "Unsigned value")
					.addAnnotation(annotationDescriptionType, "Value should be interpreted as unsigned data type");
		});

		getOrAdd(JDK.TICKSPAN, builder -> {
			builder.addField("tickSpan", Builtin.LONG);
		});
		getOrAdd(JDK.TICKS, builder -> {
			builder.addField("ticks", Builtin.LONG);
		});
		TypeImpl threadGroupType = getOrAdd(JDK.THREAD_GROUP, tgBuilder -> {
			tgBuilder.addField("parent", tgBuilder.selfType()).addField("name", getType(Builtin.STRING));
		});
		getOrAdd(JDK.THREAD, typeBuilder -> {
			typeBuilder.addField("osName", getType(Builtin.STRING)).addField("osThreadId", getType(Builtin.LONG))
					.addField("javaName", getType(Builtin.STRING)).addField("group", threadGroupType);
		});
		TypeImpl symbol = getOrAdd(JDK.SYMBOL, builder -> {
			builder.addField("string", Builtin.STRING);
		});
		TypeImpl classLoader = getOrAdd(JDK.CLASS_LOADER, builder -> {
			builder.addField("type", JDK.CLASS).addField("name", symbol);
		});
		TypeImpl moduleType = getOrAdd(JDK.MODULE, builder -> {
			builder.addField("name", symbol).addField("version", symbol).addField("location", symbol)
					.addField("classLoader", classLoader);
		});
		TypeImpl packageType = getOrAdd(JDK.PACKAGE, builder -> {
			builder.addField("name", symbol).addField("module", moduleType).addField("exported", Builtin.BOOLEAN);
		});
		TypeImpl classType = getOrAdd(JDK.CLASS, builder -> {
			builder.addField("classLoader", classLoader).addField("name", symbol).addField("package", packageType)
					.addField("modifiers", Builtin.INT).addField("hidden", Builtin.BOOLEAN);
		});
		TypeImpl methodType = getOrAdd(JDK.METHOD, builder -> {
			builder.addField("type", classType).addField("name", symbol).addField("descriptor", symbol)
					.addField("modifiers", Builtin.INT).addField("hidden", Builtin.BOOLEAN);
		});
		getOrAdd(JDK.FRAME_TYPE, builder -> {
			builder.addField("description", Builtin.STRING);
		});
		getOrAdd(JDK.STACK_FRAME, builder -> {
			builder.addField("method", methodType).addField("lineNumber", Builtin.INT)
					.addField("bytecodeIndex", Builtin.INT).addField("type", JDK.FRAME_TYPE);
		});
		getOrAdd(JDK.STACK_TRACE, builder -> {
			builder.addField("truncated", Builtin.BOOLEAN).addField("frames", JDK.STACK_FRAME,
					TypedFieldBuilder::asArray);
		});
	}

	@Override
	public TypeImpl getOrAdd(Predefined type, Consumer<TypeStructureBuilder> builderCallback) {
		return getOrAdd(type.getTypeName(), true, builderCallback);
	}

	@Override
	public TypeImpl getOrAdd(String name, boolean withConstantPool, Consumer<TypeStructureBuilder> builderCallback) {
		return getOrAdd(name, null, withConstantPool, builderCallback);
	}

	@Override
	public TypeImpl getOrAdd(Predefined type, String supertype, Consumer<TypeStructureBuilder> builderCallback) {
		return getOrAdd(type.getTypeName(), supertype, true, builderCallback);
	}

	@Override
	public TypeImpl getOrAdd(
		String name, String supertype, boolean withConstantPool, Consumer<TypeStructureBuilder> builderCallback) {
		return metadata.registerType(name, supertype, withConstantPool, () -> {
			TypeStructureBuilderImpl builder = new TypeStructureBuilderImpl(this);
			builderCallback.accept(builder);
			return (TypeStructureImpl) builder.build();
		});
	}

	@Override
	public TypeImpl getOrAdd(String name, String supertype, boolean withConstantPool, TypeStructure typeStructure) {
		return metadata.registerType(name, supertype, withConstantPool, (TypeStructureImpl) typeStructure);
	}

	@Override
	public TypeImpl getOrAdd(String name, Consumer<TypeStructureBuilder> builderCallback) {
		return getOrAdd(name, true, builderCallback);
	}

	@Override
	public TypeImpl getOrAdd(String name, String supertype, Consumer<TypeStructureBuilder> builderCallback) {
		return getOrAdd(name, supertype, true, builderCallback);
	}

	@Override
	public TypeImpl getOrAdd(String name, String supertype, TypeStructure typeStructure) {
		return getOrAdd(name, supertype, true, typeStructure);
	}

	@Override
	public TypeImpl getType(String name) {
		return getType(name, false);
	}

	@Override
	public TypeImpl getType(String name, boolean asResolvable) {
		return metadata.getType(name, asResolvable);
	}

	@Override
	public TypeImpl getType(Predefined type) {
		return getType(type.getTypeName(), true);
	}

	@Override
	public TypeStructureBuilder typeStructureBuilder() {
		return new TypeStructureBuilderImpl(this);
	}

	@Override
	public TypedFieldBuilder fieldBuilder(String fieldName, Type fieldType) {
		return new TypedFieldBuilderImpl(fieldName, (TypeImpl) fieldType, this);
	}

	@Override
	public TypedFieldBuilder fieldBuilder(String fieldName, Builtin fieldType) {
		return new TypedFieldBuilderImpl(fieldName, getType(fieldType), this);
	}

	/**
	 * Resolve all unresolved types. After this method had been called all calls to
	 * {@linkplain ResolvableType#isResolved()} will return {@literal true} if the target type was
	 * properly registered
	 */
	public void resolveAll() {
		metadata.resolveTypes();
	}
}
