package org.openjdk.jmc.flightrecorder.writer.api;

import java.util.List;

public interface TypedField {
	/** @return field name */
	String getName();

	/** @return field type */
	Type getType();

	/** @return is the field content an array */
	boolean isArray();

	/**
	 * @return the associate {@link Annotation annotations}
	 */
	List<Annotation> getAnnotations();
}
