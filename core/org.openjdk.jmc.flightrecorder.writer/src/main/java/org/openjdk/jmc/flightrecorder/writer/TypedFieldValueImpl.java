package org.openjdk.jmc.flightrecorder.writer;

import org.openjdk.jmc.flightrecorder.writer.api.TypedFieldValue;
import org.openjdk.jmc.flightrecorder.writer.util.NonZeroHashCode;

import java.util.Arrays;

public final class TypedFieldValueImpl implements TypedFieldValue {
	private int hashCode = 0;

	private final TypedFieldImpl field;
	private final TypedValueImpl[] values;

	public TypedFieldValueImpl(TypedFieldImpl field, TypedValueImpl value) {
		this(field, new TypedValueImpl[] {value});
	}

	public TypedFieldValueImpl(TypedFieldImpl field, TypedValueImpl[] values) {
		if (values == null) {
			values = new TypedValueImpl[0];
		}
		if (!field.isArray() && values.length > 1) {
			throw new IllegalArgumentException();
		}
		this.field = field;
		this.values = values;
	}

	/** @return the corresponding {@linkplain TypedFieldImpl} */
	public TypedFieldImpl getField() {
		return field;
	}

	/**
	 * @return the associated value
	 * @throws IllegalArgumentException
	 *             if the field is an array
	 */
	public TypedValueImpl getValue() {
		if (field.isArray()) {
			throw new IllegalArgumentException();
		}
		return values[0];
	}

	/**
	 * @return the associated values
	 * @throws IllegalArgumentException
	 *             if the field is not an array
	 */
	public TypedValueImpl[] getValues() {
		if (!field.isArray()) {
			throw new IllegalArgumentException();
		}
		return Arrays.copyOf(values, values.length);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TypedFieldValueImpl that = (TypedFieldValueImpl) o;
		return field.equals(that.field) && Arrays.equals(values, that.values);
	}

	@Override
	public int hashCode() {
		if (hashCode == 0) {
			Object[] objValues = new Object[values.length + 1];
			System.arraycopy(values, 0, objValues, 1, values.length);
			objValues[0] = field;
			hashCode = NonZeroHashCode.hash(objValues);
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return "TypedFieldValueImpl{" + "field=" + field + ", values=" + Arrays.toString(values) + '}';
	}
}
