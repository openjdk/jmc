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

import org.openjdk.jmc.flightrecorder.writer.ResolvableType;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class Types {
	/**
	 * Retrieve the given type or create it a-new if it hasn't been added yet. The type values will
	 * be stored in an associated constant pool.
	 *
	 * @param type
	 *            the type to retrieve
	 * @param builderCallback
	 *            will be called lazily when the type is about to be initialized
	 * @return the corresponding {@link Type type} instance
	 */
	public abstract Type getOrAdd(Predefined type, Consumer<TypeStructureBuilder> builderCallback);

	/**
	 * Retrieve the given type or create it a-new if it hasn't been added yet.
	 *
	 * @param name
	 *            the name of the type to retrieve
	 * @param builderCallback
	 *            will be called lazily when the type is about to be initialized
	 * @return the corresponding {@link Type type} instance
	 */
	public abstract Type getOrAdd(String name, Consumer<TypeStructureBuilder> builderCallback);

	/**
	 * Retrieve the given type or create it a-new if it hasn't been added yet.
	 *
	 * @param name
	 *            the name of the type to retrieve
	 * @param withConstantPool
	 *            should the type values use an associated constant pool
	 * @param builderCallback
	 *            will be called lazily when the type is about to be initialized
	 * @return the corresponding {@link Type type} instance
	 */
	public abstract Type getOrAdd(
		String name, boolean withConstantPool, Consumer<TypeStructureBuilder> builderCallback);

	/**
	 * Retrieve the given type or create it a-new if it hasn't been added yet.
	 *
	 * @param type
	 *            the type to retrieve
	 * @param supertype
	 *            the super type name
	 * @param builderCallback
	 *            will be called lazily when the type is about to be initialized
	 * @return the corresponding {@link Type type} instance
	 */
	public abstract Type getOrAdd(Predefined type, String supertype, Consumer<TypeStructureBuilder> builderCallback);

	/**
	 * Retrieve the given type or create it a-new if it hasn't been added yet. The type values will
	 * be stored in an associated constant pool.
	 *
	 * @param name
	 *            the name of the type to retrieve
	 * @param supertype
	 *            the super type name
	 * @param builderCallback
	 *            will be called lazily when the type is about to be initialized
	 * @return the corresponding {@link Type type} instance
	 */
	public abstract Type getOrAdd(String name, String supertype, Consumer<TypeStructureBuilder> builderCallback);

	/**
	 * Retrieve the given type or create it a-new if it hasn't been added yet.
	 *
	 * @param name
	 *            the name of the type to retrieve
	 * @param supertype
	 *            the super type name
	 * @param withConstantPool
	 *            should the type values use an associated constant pool
	 * @param builderCallback
	 *            will be called lazily when the type is about to be initialized
	 * @return the corresponding {@link Type type} instance
	 */
	public abstract Type getOrAdd(
		String name, String supertype, boolean withConstantPool, Consumer<TypeStructureBuilder> builderCallback);

	/**
	 * Retrieve the given type or create it a-new if it hasn't been added yet. The type values will
	 * be stored in an associated constant pool.
	 *
	 * @param name
	 *            the name of the type to retrieve
	 * @param supertype
	 *            the super type name
	 * @param typeStructure
	 *            the type structure definition
	 * @return the corresponding {@link Type type} instance
	 */
	public abstract Type getOrAdd(String name, String supertype, TypeStructure typeStructure);

	/**
	 * Retrieve the given type or create it a-new if it hasn't been added yet.
	 *
	 * @param name
	 *            the name of the type to retrieve
	 * @param supertype
	 *            the super type name
	 * @param withConstantPool
	 *            should the type values use an associated constant pool
	 * @param typeStructure
	 *            the type structure definition
	 * @return the corresponding {@link Type type} instance
	 */
	public abstract Type getOrAdd(String name, String supertype, boolean withConstantPool, TypeStructure typeStructure);

	/**
	 * Retrieve the type by its name.
	 *
	 * @param name
	 *            the type name
	 * @return the registered {@link Type type} or {@literal null}
	 */
	public abstract Type getType(String name);

	/**
	 * Retrieve the type by its name. If the type hasn't been added yet a
	 * {@linkplain ResolvableType} wrapper may be returned.
	 *
	 * @param name
	 *            the type name
	 * @param asResolvable
	 *            if {@literal true} a {@linkplain ResolvableType} wrapper will be returned instead
	 *            of {@literal null} for non-existent type
	 * @return an existing {@link Type} type, {@literal null} or a {@linkplain ResolvableType}
	 *         wrapper
	 */
	public abstract Type getType(String name, boolean asResolvable);

	/**
	 * A convenience shortcut to get a {@linkplain Type} instance corresponding to the
	 * {@linkplain Predefined} type
	 *
	 * @param type
	 *            the predefined/enum type
	 * @return the registered {@linkplain Type} instance or a {@linkplain ResolvableType} wrapper
	 */
	public abstract Type getType(Predefined type);

	/**
	 * A convenience accessor to {@linkplain TypeStructureBuilder} instance outside of the type
	 * configuration callback
	 * 
	 * @return a new {@linkplain TypeStructureBuilder} instance
	 */
	public abstract TypeStructureBuilder typeStructureBuilder();

	/**
	 * @param fieldName
	 *            field name
	 * @param fieldType
	 *            field type
	 * @return a new {@linkplain TypedFieldBuilder} instance for a named field of the given type
	 */
	public abstract TypedFieldBuilder fieldBuilder(String fieldName, Type fieldType);

	/**
	 * @param fieldName
	 *            field name
	 * @param fieldType
	 *            field type
	 * @return a new {@linkplain TypedFieldBuilder} instance for a named field of the given type
	 */
	public abstract TypedFieldBuilder fieldBuilder(String fieldName, Builtin fieldType);

	/** A {@link Type type} predefined by the writer */
	public interface Predefined extends NamedType {
	}

	/** Built-in types */
	public enum Builtin implements Predefined {
		BYTE("byte", byte.class),
		CHAR("char", char.class),
		SHORT("short", short.class),
		INT("int", int.class),
		LONG("long", long.class),
		FLOAT("float", float.class),
		DOUBLE("double", double.class),
		BOOLEAN("boolean", boolean.class),
		STRING("java.lang.String", String.class);

		private static Map<String, Builtin> NAME_MAP;
		private final String typeName;
		private final Class<?> type;

		Builtin(String name, Class<?> type) {
			addName(name);
			this.typeName = name;
			this.type = type;
		}

		private static Map<String, Builtin> getNameMap() {
			if (NAME_MAP == null) {
				NAME_MAP = new HashMap<>();
			}
			return NAME_MAP;
		}

		private void addName(String name) {
			getNameMap().put(name, this);
		}

		public static boolean hasType(String name) {
			return getNameMap().containsKey(name);
		}

		public static Builtin ofName(String name) {
			return getNameMap().get(name);
		}

		public static Builtin ofType(Type type) {
			return ofName(type.getTypeName());
		}

		@Override
		public String getTypeName() {
			return typeName;
		}

		public Class<?> getTypeClass() {
			return type;
		}
	}

	/** Types (subset of) defined by the JFR JVM implementation */
	public enum JDK implements Predefined {
		TICKSPAN("jdk.type.Tickspan"),
		TICKS("jdk.type.Ticks"),
		THREAD_GROUP("jdk.types.ThreadGroup"),
		THREAD("java.lang.Thread"),
		STACK_TRACE("jdk.types.StackTrace"),
		STACK_FRAME("jdk.types.StackFrame"),
		METHOD("jdk.types.Method"),
		FRAME_TYPE("jdk.types.FrameType"),
		CLASS("java.lang.Class"),
		SYMBOL("jdk.types.Symbol"),
		CLASS_LOADER("jdk.type.ClassLoader"),
		PACKAGE("jdk.types.Package"),
		MODULE("jdk.types.Module"),
		ANNOTATION_LABEL("jdk.jfr.Label"),
		ANNOTATION_CONTENT_TYPE("jdk.jfr.ContentType"),
		ANNOTATION_NAME("jdk.jfr.Name"),
		ANNOTATION_DESCRIPTION("jdk.jfr.Description"),
		ANNOTATION_TIMESTAMP("jdk.jfr.Timestamp"),
		ANNOTATION_TIMESPAN("jdk.jfr.Timespan"),
		ANNOTATION_UNSIGNED("jdk.jfr.Unsigned");

		private final String typeName;

		JDK(String name) {
			this.typeName = name;
		}

		@Override
		public String getTypeName() {
			return typeName;
		}
	}
}
