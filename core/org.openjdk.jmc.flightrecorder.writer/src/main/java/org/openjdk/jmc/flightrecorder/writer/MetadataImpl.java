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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.openjdk.jmc.flightrecorder.writer.api.Annotation;
import org.openjdk.jmc.flightrecorder.writer.api.NamedType;
import org.openjdk.jmc.flightrecorder.writer.api.Types;

/** JFR type repository class. */
final class MetadataImpl {
	private static final String CLASS_KEY = "class";
	private static final String FIELD_KEY = "field";
	private static final String NAME_KEY = "name";
	private static final String ID_KEY = "id";
	private static final String VALUE_KEY = "value";
	private static final String SUPER_TYPE_KEY = "superType";
	private static final String CONSTANT_POOL_KEY = "constantPool";
	private static final String SIMPLE_TYPE_KEY = "simpleType";
	private static final String ROOT_KEY = "root";
	private static final String METADATA_KEY = "metadata";
	private static final String TRUE_VALUE = "true";
	private static final String REGION_KEY = "region";
	private static final String DIMENSION_KEY = "dimension";
	private static final String ANNOTATION_KEY = "annotation";
	private static final String VAL_1_VALUE = "1";

	private final AtomicLong typeCounter = new AtomicLong(1);
	private final ConstantPools constantPools;
	private final Map<String, TypeImpl> metadata = new ConcurrentHashMap<>();
	private final Map<String, Integer> stringTable = new ConcurrentHashMap<>();
	private final Map<Integer, String> reverseStringTable = new ConcurrentSkipListMap<>();
	private final Set<ResolvableType> unresolvedTypes = new CopyOnWriteArraySet<>();

	private volatile TypesImpl types = null;

	MetadataImpl(ConstantPools constantPools) {
		this.constantPools = constantPools;
		fillStrings();
	}

	void setTypes(TypesImpl types) {
		this.types = types;
	}

	ConstantPools getConstantPools() {
		return constantPools;
	}

	/** Pre-fill the string constant pool with all used constant strings */
	private void fillStrings() {
		storeString(VAL_1_VALUE);
		storeString(CLASS_KEY);
		storeString(FIELD_KEY);
		storeString(NAME_KEY);
		storeString(ID_KEY);
		storeString(VALUE_KEY);
		storeString(SUPER_TYPE_KEY);
		storeString(CONSTANT_POOL_KEY);
		storeString(SIMPLE_TYPE_KEY);
		storeString(ROOT_KEY);
		storeString(METADATA_KEY);
		storeString(TRUE_VALUE);
		storeString(REGION_KEY);
		storeString(DIMENSION_KEY);
		storeString(ANNOTATION_KEY);
	}

	/**
	 * Register a built-in type
	 *
	 * @param typeDef
	 *            a {@link org.openjdk.jmc.flightrecorder.writer.api.Types.Builtin built-in} type
	 */
	public void registerBuiltin(Types.Builtin typeDef) {
		TypeImpl type = metadata.computeIfAbsent(typeDef.getTypeName(), this::createBuiltinType);
		storeTypeStrings(type);
	}

	/**
	 * Register a {@linkplain org.openjdk.jmc.flightrecorder.writer.api.Type} instance with values
	 * stored in an associated constant pool.
	 *
	 * @param typeName
	 *            the type name
	 * @param supertype
	 *            super type; may be {@literal null}
	 * @param typeStructureProvider
	 *            type structure provider to be called lazily when a new type is created
	 * @return registered type - either a new type or or a previously registered with the same name
	 */
	TypeImpl registerType(String typeName, String supertype, Supplier<TypeStructureImpl> typeStructureProvider) {
		return registerType(typeName, supertype, true,
				typeStructureProvider != null ? typeStructureProvider.get() : TypeStructureImpl.EMPTY);
	}

	/**
	 * Register a {@linkplain org.openjdk.jmc.flightrecorder.writer.api.Type} instance
	 *
	 * @param typeName
	 *            the type name
	 * @param supertype
	 *            super type; may be {@literal null}
	 * @param withConstantPool
	 *            store the values in an associated constant pool
	 * @param typeStructureProvider
	 *            type structure provider to be called lazily when a new type is created
	 * @return registered type - either a new type or or a previously registered with the same name
	 */
	TypeImpl registerType(
		String typeName, String supertype, boolean withConstantPool,
		Supplier<TypeStructureImpl> typeStructureProvider) {
		return registerType(typeName, supertype, withConstantPool,
				typeStructureProvider != null ? typeStructureProvider.get() : TypeStructureImpl.EMPTY);
	}

	/**
	 * Register a {@linkplain org.openjdk.jmc.flightrecorder.writer.api.Type} instance with values
	 * stored in an associated constant pool.
	 *
	 * @param typeName
	 *            the type name
	 * @param supertype
	 *            super type; may be {@literal null}
	 * @param compositeType
	 *            the composite type structure description
	 * @return registered type - either a new type or or a previously registered with the same name
	 */
	TypeImpl registerType(String typeName, String supertype, TypeStructureImpl compositeType) {
		return registerType(typeName, supertype, true, compositeType);
	}

	/**
	 * Register a {@linkplain org.openjdk.jmc.flightrecorder.writer.api.Type} instance
	 *
	 * @param typeName
	 *            the type name
	 * @param supertype
	 *            super type; may be {@literal null}
	 * @param withConstantPool
	 *            store the values in an associated constant pool
	 * @param compositeType
	 *            the composite type structure description
	 * @return registered type - either a new type or or a previously registered with the same name
	 */
	TypeImpl registerType(
		String typeName, String supertype, boolean withConstantPool, TypeStructureImpl compositeType) {
		return metadata.computeIfAbsent(typeName, name -> {
			TypeImpl t = createCustomType(name, supertype, compositeType, withConstantPool);
			storeTypeStrings(t);
			return t;
		});
	}

	/**
	 * Retrieve a type with the given name.
	 *
	 * @param name
	 *            the type name
	 * @param asResolvable
	 *            if the type is not found to be registered should a {@link ResolvableType
	 *            resolvable} wrapper be returned instead?
	 * @return the type of the given name
	 */
	TypeImpl getType(String name, boolean asResolvable) {
		TypeImpl found = metadata.get(name);
		if (found == null) {
			if (asResolvable) {
				found = new ResolvableType(name, this);
			}
		}
		return found;
	}

	/**
	 * Create a new built-in type of the given name. !Package visibility only because of unit
	 * testing!
	 *
	 * @param name
	 *            the type name
	 * @return new built-in type
	 * @throws IllegalArgumentException
	 *             if a the type name is not representing a built-in
	 */
	TypeImpl createBuiltinType(String name) {
		if (!Types.Builtin.hasType(name)) {
			throw new IllegalArgumentException();
		}
		Types.Builtin type = Types.Builtin.ofName(name);
		return new BuiltinType(typeCounter.getAndIncrement(), type, type == Types.Builtin.STRING ? constantPools : null,
				types);
	}

	/**
	 * Create a new custom type of the given name and structure. !Package visibility only because of
	 * unit testing!
	 *
	 * @param name
	 *            the type name
	 * @param supertype
	 *            the super type name - may be {@literal null}
	 * @param structure
	 *            the type structure - fields, annotations
	 * @return new custom type
	 * @throws IllegalArgumentException
	 *             if the name belongs to one of the built-in types
	 */
	TypeImpl createCustomType(String name, String supertype, TypeStructureImpl structure, boolean withConstantPool) {
		if (Types.Builtin.hasType(name)) {
			throw new IllegalArgumentException();
		}
		return new CompositeTypeImpl(typeCounter.getAndIncrement(), name, supertype, structure,
				// TODO hack for event types not to go to constant pool
				withConstantPool && !"jdk.jfr.Event".equals(supertype) ? constantPools : null, types);
	}

	/**
	 * Retrieve a type from the metadata storage
	 *
	 * @param type
	 *            the (enumerated) type to retrieve from the metadata storage
	 * @param asResolvable
	 *            should a {@linkplain ResolvableType} wrapper be returned if the requested type is
	 *            not present in the metadata storage yet?
	 * @return the specified {@linkplain org.openjdk.jmc.flightrecorder.writer.api.Type} instance or
	 *         {@linkplain null} if that type is not in the metadata storage yet and 'asResolvable'
	 *         was {@literal false}
	 */
	TypeImpl getType(NamedType type, boolean asResolvable) {
		return getType(type.getTypeName(), asResolvable);
	}

	/**
	 * Add a new unresolved {@linkplain ResolvableType} instance.
	 *
	 * @param type
	 *            unresolved type
	 */
	void addUnresolved(ResolvableType type) {
		unresolvedTypes.add(type);
	}

	/**
	 * Resolve all dangling unresolved {@link ResolvableType resolvable types}. This needs to be
	 * done if some of the type definitions are using forward references to not yet registered
	 * types.
	 */
	void resolveTypes() {
		unresolvedTypes.removeIf(ResolvableType::resolve);
	}

	private void storeTypeStrings(TypeImpl type) {
		storeString(type.getTypeName());
		if (type.getSupertype() != null) {
			storeString(type.getSupertype());
		}
		storeString(String.valueOf(type.getId()));
		for (TypedFieldImpl field : type.getFields()) {
			storeString(field.getName());
			storeAnnotationStrings(field.getAnnotations());
		}
		storeAnnotationStrings(type.getAnnotations());
	}

	private void storeAnnotationStrings(List<Annotation> annotations) {
		for (Annotation annotation : annotations) {
			if (annotation.getValue() != null) {
				storeString(annotation.getValue());
			}
		}
	}

	private void storeString(String value) {
		stringTable.computeIfAbsent(value, k -> {
			int pointer = stringTable.size();
			reverseStringTable.put(pointer, k);
			return pointer;
		});
	}

	int stringIndex(String value) {
		Objects.requireNonNull(value);
		return stringTable.get(value);
	}

	void writeMetaEvent(LEB128Writer writer, long startTs, long duration) {
		LEB128Writer metaWriter = LEB128Writer.getInstance();
		writeMetadataHeader(startTs, duration, metaWriter);

		writeStringConstants(metaWriter);
		writeTypes(metaWriter);
		writeRegion(metaWriter);

		writeMetaEventWithSize(metaWriter, writer);
	}

	private void writeMetaEventWithSize(LEB128Writer metaWriter, LEB128Writer writer) {
		int len = metaWriter.length();
		writer.writeInt(len);
		writer.writeBytes(metaWriter.export());
	}

	private void writeRegion(LEB128Writer metaWriter) {
		metaWriter.writeInt(stringIndex(REGION_KEY)).writeInt(0) // 0 attributes
				.writeInt(0); // 0 elements
	}

	private void writeTypes(LEB128Writer metaWriter) {
		metaWriter.writeInt(stringIndex(ROOT_KEY)).writeInt(0) // 0 attributes
				.writeInt(2) // 1 element
				.writeInt(stringIndex(METADATA_KEY)).writeInt(0) // 0 attributes
				.writeInt(metadata.size()); // metadata.size() elements
		for (TypeImpl type : metadata.values()) {
			writeType(metaWriter, type);
		}
	}

	private void writeStringConstants(LEB128Writer metaWriter) {
		for (String text : reverseStringTable.values()) {
			metaWriter.writeCompactUTF(text);
		}
	}

	private void writeMetadataHeader(long startTs, long duration, LEB128Writer metaWriter) {
		metaWriter.writeLong(0L) // metadata event id
				.writeLong(startTs).writeLong(duration).writeLong(0L).writeInt(stringTable.size());
	}

	private void writeType(LEB128Writer writer, TypeImpl type) {
		int attributes = 2;
		if (type.getSupertype() != null) {
			attributes++;
		}
		if (type.isSimple()) {
			attributes++;
		}
		writer.writeInt(stringIndex(CLASS_KEY)).writeInt(attributes).writeInt(stringIndex(NAME_KEY))
				.writeInt(stringIndex(type.getTypeName())).writeInt(stringIndex(ID_KEY))
				.writeInt(stringIndex(String.valueOf(type.getId())));
		if (type.getSupertype() != null) {
			writer.writeInt(stringIndex(SUPER_TYPE_KEY)).writeInt(stringIndex(type.getSupertype()));
		}
		if (type.isSimple()) {
			writer.writeInt(stringIndex(SIMPLE_TYPE_KEY)).writeInt(stringIndex(TRUE_VALUE));
		}
		writer.writeInt(type.getFields().size() + type.getAnnotations().size());
		writeTypeFields(writer, type);
		writeTypeAnnotations(writer, type);
	}

	private void writeTypeFields(LEB128Writer writer, TypeImpl type) {
		for (TypedFieldImpl field : type.getFields()) {
			writeField(writer, field);
		}
	}

	private void writeTypeAnnotations(LEB128Writer writer, TypeImpl type) {
		for (Annotation annotation : type.getAnnotations()) {
			writeAnnotation(writer, annotation);
		}
	}

	private void writeField(LEB128Writer writer, TypedFieldImpl field) {
		writer.writeInt(stringIndex(FIELD_KEY));
		int attrCount = 2;

		// java.lang.String is special - it is using constant pool but is not marked as such
		boolean withConstantPool = !field.getType().isSame(Types.Builtin.STRING) && field.getType().hasConstantPool();
		if (withConstantPool) {
			attrCount++;
		}
		if (field.isArray()) {
			attrCount++;
		}
		writer.writeInt(attrCount).writeInt(stringIndex(NAME_KEY)).writeInt(stringIndex(field.getName()))
				.writeInt(stringIndex(CLASS_KEY)).writeInt(stringIndex(String.valueOf(field.getType().getId())));
		if (field.isArray()) {
			writer.writeInt(stringIndex(DIMENSION_KEY)).writeInt(stringIndex(VAL_1_VALUE));
		}
		if (withConstantPool) {
			writer.writeInt(stringIndex(CONSTANT_POOL_KEY)).writeInt(stringIndex(TRUE_VALUE));
		}
		writeFieldAnnotations(writer, field);
	}

	private void writeFieldAnnotations(LEB128Writer writer, TypedFieldImpl field) {
		writer.writeInt(field.getAnnotations().size()); // annotations are the only sub-elements
		for (Annotation annotation : field.getAnnotations()) {
			writeAnnotation(writer, annotation);
		}
	}

	private void writeAnnotation(LEB128Writer writer, Annotation annotation) {
		writer.writeInt(stringIndex(ANNOTATION_KEY));

		writer.writeInt(annotation.getValue() != null ? 2 : 1) // number of attributes
				.writeInt(stringIndex(CLASS_KEY)).writeInt(stringIndex(String.valueOf(annotation.getType().getId())));
		if (annotation.getValue() != null) {
			writer.writeInt(stringIndex(VALUE_KEY)).writeInt(stringIndex(annotation.getValue()));
		}
		writer.writeInt(0); // no sub-elements
	}
}
