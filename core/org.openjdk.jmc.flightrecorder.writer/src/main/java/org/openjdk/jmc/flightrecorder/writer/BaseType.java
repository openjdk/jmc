package org.openjdk.jmc.flightrecorder.writer;

import lombok.ToString;
import org.openjdk.jmc.flightrecorder.writer.api.Type;
import org.openjdk.jmc.flightrecorder.writer.api.TypedValueBuilder;
import org.openjdk.jmc.flightrecorder.writer.api.TypedFieldBuilder;
import org.openjdk.jmc.flightrecorder.writer.util.NonZeroHashCode;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Common JFR type super-class
 */
@ToString(of = {"id", "name", "supertype"})
abstract class BaseType implements TypeImpl {
	int hashcode = 0;

	private final long id;
	private final String name;
	private final String supertype;
	private final ConstantPools constantPools;
	private final TypesImpl types;
	private final AtomicReference<TypedValueImpl> nullValue = new AtomicReference<>();

	BaseType(long id, String name, String supertype, ConstantPools constantPools, TypesImpl types) {
		this.id = id;
		this.name = name;
		this.supertype = supertype;
		this.constantPools = constantPools;
		this.types = types;
	}

	BaseType(long id, String name, ConstantPools constantPools, TypesImpl types) {
		this(id, name, null, constantPools, types);
	}

	@Override
	public long getId() {
		return id;
	}

	@Override
	public TypesImpl getTypes() {
		return types;
	}

	@Override
	public final boolean isSimple() {
		List<TypedFieldImpl> fields = getFields();
		if (fields.size() == 1) {
			TypedFieldImpl field = fields.get(0);
			return field.getType().isBuiltin() && !field.isArray();
		}
		return false;
	}

	@Override
	public boolean isResolved() {
		return true;
	}

	@Override
	public final String getTypeName() {
		return name;
	}

	@Override
	public boolean hasConstantPool() {
		return constantPools != null;
	}

	@Override
	public final String getSupertype() {
		return supertype;
	}

	@Override
	public TypedValueImpl asValue(String value) {
		if (hasConstantPool()) {
			return constantPools.forType(this).addOrGet(value);
		}
		return new TypedValueImpl(this, value);
	}

	@Override
	public TypedValueImpl asValue(byte value) {
		if (hasConstantPool()) {
			return constantPools.forType(this).addOrGet(value);
		}
		return new TypedValueImpl(this, value);
	}

	@Override
	public TypedValueImpl asValue(char value) {
		if (hasConstantPool()) {
			return constantPools.forType(this).addOrGet(value);
		}
		return new TypedValueImpl(this, value);
	}

	@Override
	public TypedValueImpl asValue(short value) {
		if (hasConstantPool()) {
			return constantPools.forType(this).addOrGet(value);
		}
		return new TypedValueImpl(this, value);
	}

	@Override
	public TypedValueImpl asValue(int value) {
		if (hasConstantPool()) {
			return constantPools.forType(this).addOrGet(value);
		}
		return new TypedValueImpl(this, value);
	}

	@Override
	public TypedValueImpl asValue(long value) {
		if (hasConstantPool()) {
			return constantPools.forType(this).addOrGet(value);
		}
		return new TypedValueImpl(this, value);
	}

	@Override
	public TypedValueImpl asValue(float value) {
		if (hasConstantPool()) {
			return constantPools.forType(this).addOrGet(value);
		}
		return new TypedValueImpl(this, value);
	}

	@Override
	public TypedValueImpl asValue(double value) {
		if (hasConstantPool()) {
			return constantPools.forType(this).addOrGet(value);
		}
		return new TypedValueImpl(this, value);
	}

	@Override
	public TypedValueImpl asValue(boolean value) {
		if (hasConstantPool()) {
			return constantPools.forType(this).addOrGet(value);
		}
		return new TypedValueImpl(this, value);
	}

	@Override
	public TypedValueImpl asValue(Consumer<TypedValueBuilder> builderCallback) {
		if (isBuiltin()) {
			throw new IllegalArgumentException();
		}
		TypedValueImpl checkValue = new TypedValueImpl(this, builderCallback);
		if (hasConstantPool()) {
			return constantPools.forType(this).addOrGet(checkValue);
		}
		return checkValue;
	}

	@Override
	public TypedValueImpl asValue(Object value) {
		if (hasConstantPool()) {
			return constantPools.forType(this).addOrGet(value);
		}
		return new TypedValueImpl(this, value);
	}

	@Override
	public TypedValueImpl nullValue() {
		return nullValue.updateAndGet(v -> (v == null ? TypedValueImpl.ofNull(this) : v));
	}

	@Override
	public boolean isUsedBy(Type other) {
		if (other == null) {
			return false;
		}
		return isUsedBy(this, (TypeImpl) other, new HashSet<>());
	}

	private static boolean isUsedBy(TypeImpl type1, TypeImpl type2, HashSet<TypeImpl> track) {
		if (!track.add(type2)) {
			return false;
		}
		for (TypedFieldImpl typedField : type2.getFields()) {
			if (typedField.getType().equals(type1)) {
				return true;
			}
			if (isUsedBy(type1, typedField.getType(), track)) {
				return true;
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
		BaseType baseType = (BaseType) o;
		return id == baseType.id && name.equals(baseType.name) && Objects.equals(supertype, baseType.supertype);
	}

	@Override
	public int hashCode() {
		if (hashcode == 0) {
			hashcode = NonZeroHashCode.hash(id, name, supertype);
		}
		return hashcode;
	}
}
