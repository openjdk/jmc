package org.openjdk.jmc.flightrecorder.writer;

import lombok.ToString;
import org.openjdk.jmc.flightrecorder.writer.api.Annotation;
import org.openjdk.jmc.flightrecorder.writer.util.NonZeroHashCode;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** A representation of a typed field with a name */
@ToString
public final class TypedFieldImpl implements org.openjdk.jmc.flightrecorder.writer.api.TypedField {
	private int hashCode = 0;

	private final String name;
	private final TypeImpl type;
	private final boolean isArray;
	private final List<Annotation> annotations;

	TypedFieldImpl(TypeImpl type, String name) {
		this(type, name, false, Collections.emptyList());
	}

	TypedFieldImpl(TypeImpl type, String name, boolean isArray) {
		this(type, name, isArray, Collections.emptyList());
	}

	TypedFieldImpl(TypeImpl type, String name, boolean isArray, List<Annotation> annotations) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(name);
		Objects.requireNonNull(annotations);

		this.name = name;
		this.type = type;
		this.isArray = isArray;
		this.annotations = Collections.unmodifiableList(annotations);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public TypeImpl getType() {
		return type;
	}

	@Override
	public boolean isArray() {
		return isArray;
	}

	@Override
	public List<Annotation> getAnnotations() {
		return annotations;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TypedFieldImpl that = (TypedFieldImpl) o;
		return isArray == that.isArray && name.equals(that.name) && type.equals(that.type)
				&& annotations.equals(that.annotations);
	}

	@Override
	public int hashCode() {
		if (hashCode == 0) {
			hashCode = NonZeroHashCode.hash(name, type, isArray, annotations);
		}
		return hashCode;
	}
}
