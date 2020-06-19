package org.openjdk.jmc.flightrecorder.writer;

import org.openjdk.jmc.flightrecorder.writer.api.Annotation;
import org.openjdk.jmc.flightrecorder.writer.api.Type;
import org.openjdk.jmc.flightrecorder.writer.api.TypedFieldBuilder;

import java.util.ArrayList;
import java.util.List;

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
		return addAnnotation(type, null);
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
	public TypedFieldBuilderImpl addAnnotation(TypesImpl.Predefined type, String value) {
		return addAnnotation(types.getType(type), value);
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
