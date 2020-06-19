package org.openjdk.jmc.flightrecorder.writer;

import org.openjdk.jmc.flightrecorder.writer.api.Annotation;

import java.util.List;

/**
 * A place-holder for fields of the same type as they are defined in. Keeping the default values
 * intentionally 'wrong' in order for things to break soon if this type is not replaced by the
 * concrete type counterpart correctly.
 */
final class SelfType extends BaseType {
	static final SelfType INSTANCE = new SelfType();

	private SelfType() {
		super(Long.MIN_VALUE, "", null, null);
	}

	@Override
	public boolean isBuiltin() {
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
}
