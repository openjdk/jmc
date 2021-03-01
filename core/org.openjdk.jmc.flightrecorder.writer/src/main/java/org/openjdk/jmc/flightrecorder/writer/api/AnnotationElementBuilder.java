package org.openjdk.jmc.flightrecorder.writer.api;

import java.util.function.Consumer;

/**
 * Builder for {@linkplain Annotation} instances
 */
public interface AnnotationElementBuilder {
	/**
	 * Add annotation argument
	 * 
	 * @param name
	 *            the argument name
	 * @param value
	 *            the argument value
	 * @return this instance for chaining
	 */
	AnnotationElementBuilder addArgument(String name, TypedValue value);

	/**
	 * Add annotation argument
	 * 
	 * @param name
	 *            the argument name
	 * @param builderCallback
	 *            the argument value customization callback
	 * @return this instance for chaining
	 */
	AnnotationElementBuilder addArgument(String name, Consumer<TypedValueBuilder> builderCallback);

	/**
	 * Build a new {@linkplain Annotation} instance
	 * 
	 * @return {@linkplain Annotation} instance
	 */
	Annotation build();
}
