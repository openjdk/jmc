package org.openjdk.jmc.flightrecorder.writer;

import org.openjdk.jmc.flightrecorder.writer.api.Type;
import org.openjdk.jmc.flightrecorder.writer.api.TypedValueBuilder;

import java.util.List;
import java.util.function.Consumer;

public interface TypeImpl extends Type {
	@Override
	List<TypedFieldImpl> getFields();

	@Override
	TypedFieldImpl getField(String name);

	TypesImpl getTypes();

	TypedValueImpl nullValue();

	@Override
	TypedValueImpl asValue(String value);

	@Override
	TypedValueImpl asValue(byte value);

	@Override
	TypedValueImpl asValue(char value);

	@Override
	TypedValueImpl asValue(short value);

	@Override
	TypedValueImpl asValue(int value);

	@Override
	TypedValueImpl asValue(long value);

	@Override
	TypedValueImpl asValue(float value);

	@Override
	TypedValueImpl asValue(double value);

	@Override
	TypedValueImpl asValue(boolean value);

	@Override
	TypedValueImpl asValue(Consumer<TypedValueBuilder> builderCallback);

	@Override
	TypedValueImpl asValue(Object value);

	default TypedValueBuilderImpl valueBuilder() {
		return new TypedValueBuilderImpl(this);
	}
}
