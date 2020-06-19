package org.openjdk.jmc.flightrecorder.writer;

import org.openjdk.jmc.flightrecorder.writer.api.Annotation;
import org.openjdk.jmc.flightrecorder.writer.api.NamedType;
import org.openjdk.jmc.flightrecorder.writer.api.Type;
import org.openjdk.jmc.flightrecorder.writer.api.TypedValueBuilder;
import org.openjdk.jmc.flightrecorder.writer.api.TypedFieldBuilder;

import java.util.List;
import java.util.function.Consumer;

final class InvalidType implements TypeImpl {
	@Override
	public long getId() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isBuiltin() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isSimple() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isResolved() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasConstantPool() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getSupertype() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<TypedFieldImpl> getFields() {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypedFieldImpl getField(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Annotation> getAnnotations() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean canAccept(Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypedValueImpl asValue(String value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypedValueImpl asValue(byte value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypedValueImpl asValue(char value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypedValueImpl asValue(short value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypedValueImpl asValue(int value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypedValueImpl asValue(long value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypedValueImpl asValue(float value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypedValueImpl asValue(double value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypedValueImpl asValue(boolean value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypedValueImpl asValue(Consumer<TypedValueBuilder> builderCallback) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypedValueImpl asValue(Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypedValueImpl nullValue() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isUsedBy(Type other) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getTypeName() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isSame(NamedType other) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypesImpl getTypes() {
		return null;
	}
}
