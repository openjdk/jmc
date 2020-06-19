package org.openjdk.jmc.flightrecorder.writer;

import lombok.ToString;
import org.openjdk.jmc.flightrecorder.writer.api.Annotation;
import org.openjdk.jmc.flightrecorder.writer.api.NamedType;
import org.openjdk.jmc.flightrecorder.writer.api.Type;
import org.openjdk.jmc.flightrecorder.writer.api.TypedValueBuilder;
import org.openjdk.jmc.flightrecorder.writer.api.TypedFieldBuilder;
import org.openjdk.jmc.flightrecorder.writer.util.NonZeroHashCode;

import java.util.List;
import java.util.function.Consumer;

@ToString
public final class ResolvableType implements TypeImpl {
	private final String typeName;
	private final MetadataImpl metadata;

	private volatile TypeImpl delegate;

	ResolvableType(String typeName, MetadataImpl metadata) {
		this.typeName = typeName;
		this.metadata = metadata;
		// self-register in metadata as 'unresolved'
		this.metadata.addUnresolved(this);
	}

	@Override
	public boolean isResolved() {
		return delegate != null;
	}

	private void checkResolved() {
		if (delegate == null) {
			throw new IllegalStateException();
		}
	}

	@Override
	public long getId() {
		checkResolved();
		return delegate.getId();
	}

	@Override
	public boolean hasConstantPool() {
		checkResolved();
		return delegate.hasConstantPool();
	}

	@Override
	public TypedValueImpl asValue(String value) {
		checkResolved();
		return delegate.asValue(value);
	}

	@Override
	public TypedValueImpl asValue(byte value) {
		checkResolved();
		return delegate.asValue(value);
	}

	@Override
	public TypedValueImpl asValue(char value) {
		checkResolved();
		return delegate.asValue(value);
	}

	@Override
	public TypedValueImpl asValue(short value) {
		checkResolved();
		return delegate.asValue(value);
	}

	@Override
	public TypedValueImpl asValue(int value) {
		checkResolved();
		return delegate.asValue(value);
	}

	@Override
	public TypedValueImpl asValue(long value) {
		checkResolved();
		return delegate.asValue(value);
	}

	@Override
	public TypedValueImpl asValue(float value) {
		checkResolved();
		return delegate.asValue(value);
	}

	@Override
	public TypedValueImpl asValue(double value) {
		checkResolved();
		return delegate.asValue(value);
	}

	@Override
	public TypedValueImpl asValue(boolean value) {
		checkResolved();
		return delegate.asValue(value);
	}

	@Override
	public TypedValueImpl asValue(Consumer<TypedValueBuilder> builderCallback) {
		checkResolved();
		return delegate.asValue(builderCallback);
	}

	@Override
	public TypedValueImpl asValue(Object value) {
		checkResolved();
		return delegate.asValue(value);
	}

	@Override
	public TypedValueImpl nullValue() {
		checkResolved();
		return delegate.nullValue();
	}

	@Override
	public boolean isBuiltin() {
		checkResolved();
		return delegate.isBuiltin();
	}

	@Override
	public boolean isSimple() {
		checkResolved();
		return delegate.isSimple();
	}

	@Override
	public String getSupertype() {
		checkResolved();
		return delegate.getSupertype();
	}

	@Override
	public List<TypedFieldImpl> getFields() {
		checkResolved();
		return delegate.getFields();
	}

	@Override
	public TypedFieldImpl getField(String name) {
		checkResolved();
		return delegate.getField(name);
	}

	@Override
	public List<Annotation> getAnnotations() {
		checkResolved();
		return delegate.getAnnotations();
	}

	@Override
	public boolean canAccept(Object value) {
		checkResolved();
		return delegate.canAccept(value);
	}

	@Override
	public String getTypeName() {
		return typeName;
	}

	@Override
	public boolean isSame(NamedType other) {
		checkResolved();
		return delegate.isSame(other);
	}

	@Override
	public boolean isUsedBy(Type other) {
		checkResolved();
		return delegate.isUsedBy(other);
	}

	@Override
	public TypesImpl getTypes() {
		return null;
	}

	@Override
	public TypedValueBuilderImpl valueBuilder() {
		checkResolved();
		return delegate.valueBuilder();
	}

	boolean resolve() {
		TypeImpl resolved = metadata.getType(typeName, false);
		if (resolved instanceof BaseType) {
			delegate = resolved;
			return true;
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
		ResolvableType that = (ResolvableType) o;
		return typeName.equals(that.typeName);
	}

	@Override
	public int hashCode() {
		return NonZeroHashCode.hash(typeName);
	}
}
