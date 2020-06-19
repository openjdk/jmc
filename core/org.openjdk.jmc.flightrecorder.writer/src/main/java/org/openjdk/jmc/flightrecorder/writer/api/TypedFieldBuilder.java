package org.openjdk.jmc.flightrecorder.writer.api;

import org.openjdk.jmc.flightrecorder.writer.TypedFieldImpl;

/** A fluent API for building typed fields lazily. */
public interface TypedFieldBuilder {
	/**
	 * Add an annotation
	 *
	 * @param type
	 *            annotation type
	 * @return a {@linkplain TypedFieldBuilder} instance for invocation chaining
	 */
	TypedFieldBuilder addAnnotation(Type type);

	/**
	 * Add an annotation with a value
	 *
	 * @param type
	 *            annotation type
	 * @param value
	 *            annotation value
	 * @return a {@linkplain TypedFieldBuilder} instance for invocation chaining
	 */
	TypedFieldBuilder addAnnotation(Type type, String value);

	/**
	 * Add a predefined annotation
	 *
	 * @param type
	 *            predefined annotation type
	 * @return a {@linkplain TypedFieldBuilder} instance for invocation chaining
	 */
	TypedFieldBuilder addAnnotation(Types.Predefined type);

	/**
	 * Add a predefined annotation with a value
	 *
	 * @param type
	 *            predefined annotation type
	 * @param value
	 *            annotation value
	 * @return a {@linkplain TypedFieldBuilder} instance for invocation chaining
	 */
	TypedFieldBuilder addAnnotation(Types.Predefined type, String value);

	/**
	 * Mark the field as holding an array value
	 *
	 * @return a {@linkplain TypedFieldBuilder} instance for invocation chaining
	 */
	TypedFieldBuilder asArray();

	TypedFieldImpl build();
}
