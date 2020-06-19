package org.openjdk.jmc.flightrecorder.writer.api;

/** A generic 'named' type */
public interface NamedType {
	/** @return the type name */
	String getTypeName();

	/**
	 * @param other
	 *            other type
	 * @return {@literal true} if the two types are the same (have the same name)
	 */
	default boolean isSame(NamedType other) {
		return getTypeName().equals(other.getTypeName());
	}
}
