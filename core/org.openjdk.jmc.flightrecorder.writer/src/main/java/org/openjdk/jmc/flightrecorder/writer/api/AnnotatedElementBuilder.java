package org.openjdk.jmc.flightrecorder.writer.api;

import java.util.function.Consumer;

public interface AnnotatedElementBuilder<T extends AnnotatedElementBuilder<?>> {
	/**
	 * Add an annotation of the given type
	 *
	 * @param type
	 *            the annotation type
	 * @return a {@linkplain AnnotatedElementBuilder} instance for invocation chaining
	 */
	T addAnnotation(Type type);

	/**
	 * Add an annotation of the given type and with the given value
	 *
	 * @param type
	 *            the annotation type
	 * @param value
	 *            the annotation value
	 * @return a {@linkplain AnnotatedElementBuilder} instance for invocation chaining
	 */
	T addAnnotation(Type type, String value);

	/**
	 * Add a predefined annotation
	 *
	 * @param type
	 *            predefined annotation type
	 * @return a {@linkplain AnnotatedElementBuilder} instance for invocation chaining
	 */
	T addAnnotation(Types.Predefined type);

	/**
	 * Add a predefined annotation with a value
	 *
	 * @param type
	 *            predefined annotation type
	 * @param value
	 *            annotation value
	 * @return a {@linkplain AnnotatedElementBuilder} instance for invocation chaining
	 */
	T addAnnotation(Types.Predefined type, String value);

	/**
	 * Add an annotation of the given type and with the given values array
	 *
	 * @param type
	 *            the annotation type
	 * @param builderCallback
	 *            the annotation attributes builder callback
	 * @return a {@linkplain AnnotatedElementBuilder} instance for invocation chaining
	 */
	T addAnnotation(Type type, Consumer<TypedValueBuilder> builderCallback);

	/**
	 * Add an annotation of the given type and with the given values array
	 *
	 * @param type
	 *            the annotation type
	 * @param builderCallback
	 *            the annotation attributes builder callback
	 * @return a {@linkplain AnnotatedElementBuilder} instance for invocation chaining
	 */
	T addAnnotation(Types.Predefined type, Consumer<TypedValueBuilder> builderCallback);
}
