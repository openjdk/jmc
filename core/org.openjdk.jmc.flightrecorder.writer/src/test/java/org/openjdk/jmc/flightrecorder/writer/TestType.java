package org.openjdk.jmc.flightrecorder.writer;

import org.openjdk.jmc.flightrecorder.writer.api.Annotation;

import java.util.List;

abstract class TestType extends BaseType {
	public TestType(long id, String name, String supertype, ConstantPools constantPools, TypesImpl types) {
		super(id, name, supertype, constantPools, types);
	}

	@Override
	public boolean isBuiltin() {
		return false;
	}

	@Override
	public List<TypedFieldImpl> getFields() {
		throw new IllegalArgumentException();
	}

	@Override
	public TypedFieldImpl getField(String name) {
		throw new IllegalArgumentException();
	}

	@Override
	public List<Annotation> getAnnotations() {
		throw new IllegalArgumentException();
	}

	@Override
	public boolean canAccept(Object value) {
		return false;
	}
}
