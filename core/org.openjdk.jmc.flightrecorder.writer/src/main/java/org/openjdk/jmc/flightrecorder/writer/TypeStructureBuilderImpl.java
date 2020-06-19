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
