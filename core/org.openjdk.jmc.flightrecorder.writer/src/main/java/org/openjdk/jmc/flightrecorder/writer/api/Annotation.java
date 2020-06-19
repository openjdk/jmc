package org.openjdk.jmc.flightrecorder.writer.api;

import lombok.Getter;
import lombok.ToString;
import org.openjdk.jmc.flightrecorder.writer.util.NonZeroHashCode;

import java.util.Objects;

/** A struct-like representation of a JFR annotation */
@ToString
public final class Annotation {
	public static final String ANNOTATION_SUPER_TYPE_NAME = "java.lang.annotation.Annotation";
	private int hashCode = 0;

	@Getter
	private final Type type;
	@Getter
	private final String value;

	/**
	 * Create a new {@linkplain Annotation} instance
	 *
	 * @param type
	 *            the annotation type (must have the value of
	 *            {@linkplain Annotation#ANNOTATION_SUPER_TYPE_NAME} as its super type)
	 * @param value
	 *            the annotation value or {@literal null}
	 * @throws IllegalArgumentException
	 *             if the annotation type is not having the value of
	 *             {@linkplain Annotation#ANNOTATION_SUPER_TYPE_NAME} as its super type
	 */
	public Annotation(Type type, String value) {
		if (!isAnnotationType(type)) {
			throw new IllegalArgumentException();
		}
		this.type = type;
		this.value = value;
	}

	public static boolean isAnnotationType(Type type) {
		return ANNOTATION_SUPER_TYPE_NAME.equals(type.getSupertype());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Annotation that = (Annotation) o;
		return type.equals(that.type) && Objects.equals(value, that.value);
	}

	@Override
	public int hashCode() {
		if (hashCode == 0) {
			hashCode = NonZeroHashCode.hash(type, value);
		}
		return hashCode;
	}
}
