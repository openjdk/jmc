package org.openjdk.jmc.flightrecorder.writer;

import org.openjdk.jmc.flightrecorder.writer.api.Annotation;
import org.openjdk.jmc.flightrecorder.writer.api.AnnotationElementBuilder;
import org.openjdk.jmc.flightrecorder.writer.api.Type;
import org.openjdk.jmc.flightrecorder.writer.api.TypedValue;
import org.openjdk.jmc.flightrecorder.writer.api.TypedValueBuilder;

import java.util.function.Consumer;

final class AnnotationElementBuilderImpl implements AnnotationElementBuilder {
	private final Type type;

	AnnotationElementBuilderImpl(Type type) {
		this.type = type;
	}

	@Override
	public AnnotationElementBuilder addArgument(String name, TypedValue value) {
		return this;
	}

	@Override
	public AnnotationElementBuilder addArgument(String name, Consumer<TypedValueBuilder> builderCallback) {
		return this;
	}

	@Override
	public Annotation build() {
		return new Annotation(type, (String) null);
	}
}
