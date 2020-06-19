package org.openjdk.jmc.flightrecorder.writer;

import lombok.ToString;
import org.openjdk.jmc.flightrecorder.writer.api.Annotation;
import org.openjdk.jmc.flightrecorder.writer.api.Types;
import org.openjdk.jmc.flightrecorder.writer.util.NonZeroHashCode;

import java.util.Collections;
import java.util.List;

/** A built-in type. Corresponds to a Java primitive type or {@link String String} */
@ToString(of = "builtin")
final class BuiltinType extends BaseType {
	private int hashcode = 0;

	private final Types.Builtin builtin;

	BuiltinType(long id, Types.Builtin type, ConstantPools constantPools, TypesImpl types) {
		super(id, type.getTypeName(), constantPools, types);
		this.builtin = type;
	}

	@Override
	public boolean isBuiltin() {
		return true;
	}

	@Override
	public List<TypedFieldImpl> getFields() {
		return Collections.emptyList();
	}

	@Override
	public TypedFieldImpl getField(String name) {
		throw new IllegalArgumentException();
	}

	@Override
	public List<Annotation> getAnnotations() {
		return Collections.emptyList();
	}

	@Override
	public boolean canAccept(Object value) {
		if (value == null) {
			// non-initialized built-ins will get the default value and String will be properly handled
			return true;
		}

		if (value instanceof TypedValueImpl) {
			return this == ((TypedValueImpl) value).getType();
		}
		switch (builtin) {
		case STRING: {
			return (value instanceof String);
		}
		case BYTE: {
			return value instanceof Byte;
		}
		case CHAR: {
			return value instanceof Character;
		}
		case SHORT: {
			return value instanceof Short;
		}
		case INT: {
			return value instanceof Integer;
		}
		case LONG: {
			return value instanceof Long;
		}
		case FLOAT: {
			return value instanceof Float;
		}
		case DOUBLE: {
			return value instanceof Double;
		}
		case BOOLEAN: {
			return value instanceof Boolean;
		}
		}
		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		BuiltinType that = (BuiltinType) o;
		return builtin == that.builtin;
	}

	@Override
	public int hashCode() {
		if (hashcode == 0) {
			hashcode = NonZeroHashCode.hash(super.hashCode(), builtin);
		}
		return hashcode;
	}
}
