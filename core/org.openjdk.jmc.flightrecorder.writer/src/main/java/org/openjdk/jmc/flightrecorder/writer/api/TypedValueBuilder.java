package org.openjdk.jmc.flightrecorder.writer.api;

import java.util.Map;
import java.util.function.Consumer;

/** A fluent API for lazy initialization of a composite type value */
public interface TypedValueBuilder {
	Type getType();

	/**
	 * Put a named field value
	 *
	 * @param name
	 *            field name
	 * @param value
	 *            field value
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, byte value);

	/**
	 * Put a named field array of values
	 *
	 * @param name
	 *            field name
	 * @param values
	 *            field values
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, byte[] values);

	/**
	 * Put a named field value
	 *
	 * @param name
	 *            field name
	 * @param value
	 *            field value
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, char value);

	/**
	 * Put a named field array of values
	 *
	 * @param name
	 *            field name
	 * @param values
	 *            field values
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, char[] values);

	/**
	 * Put a named field value
	 *
	 * @param name
	 *            field name
	 * @param value
	 *            field value
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, short value);

	/**
	 * Put a named field array of values
	 *
	 * @param name
	 *            field name
	 * @param values
	 *            field values
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, short[] values);

	/**
	 * Put a named field value
	 *
	 * @param name
	 *            field name
	 * @param value
	 *            field value
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, int value);

	/**
	 * Put a named field array of values
	 *
	 * @param name
	 *            field name
	 * @param values
	 *            field values
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, int[] values);

	/**
	 * Put a named field value
	 *
	 * @param name
	 *            field name
	 * @param value
	 *            field value
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, long value);

	/**
	 * Put a named field array of values
	 *
	 * @param name
	 *            field name
	 * @param values
	 *            field values
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, long[] values);

	/**
	 * Put a named field value
	 *
	 * @param name
	 *            field name
	 * @param value
	 *            field value
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, float value);

	/**
	 * Put a named field array of values
	 *
	 * @param name
	 *            field name
	 * @param values
	 *            field values
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, float[] values);

	/**
	 * Put a named field value
	 *
	 * @param name
	 *            field name
	 * @param value
	 *            field value
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, double value);

	/**
	 * Put a named field array of values
	 *
	 * @param name
	 *            field name
	 * @param values
	 *            field values
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, double[] values);

	/**
	 * Put a named field value
	 *
	 * @param name
	 *            field name
	 * @param value
	 *            field value
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, boolean value);

	/**
	 * Put a named field array of values
	 *
	 * @param name
	 *            field name
	 * @param values
	 *            field values
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, boolean[] values);

	/**
	 * Put a named field value
	 *
	 * @param name
	 *            field name
	 * @param value
	 *            field value
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, String value);

	/**
	 * Put a named field array of values
	 *
	 * @param name
	 *            field name
	 * @param values
	 *            field values
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, String[] values);

	/**
	 * Put a named field value
	 *
	 * @param name
	 *            field name
	 * @param valueBuilder
	 *            field value builder
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, TypedValueBuilder valueBuilder);

	/**
	 * Put a named field value
	 *
	 * @param name
	 *            field name
	 * @param value
	 *            field value
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, TypedValue value);

	/**
	 * Put a named field array of values
	 *
	 * @param name
	 *            field name
	 * @param values
	 *            field values
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, TypedValue ... values);

	/**
	 * Put a named field lazily evaluated value
	 *
	 * @param name
	 *            field name
	 * @param fieldValueCallback
	 *            field value builder
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putField(String name, Consumer<TypedValueBuilder> fieldValueCallback);

	/**
	 * Put a named field array of lazily evaluated values
	 *
	 * @param name
	 *            field name
	 * @param callback1
	 *            first field value builder callback
	 * @param callback2
	 *            second field value builder callback
	 * @param otherCallbacks
	 *            other field value builder callbacks field value builders
	 * @return a {@linkplain TypedValueBuilder} instance for invocation chaining
	 */
	TypedValueBuilder putFields(
		String name, Consumer<TypedValueBuilder> callback1, Consumer<TypedValueBuilder> callback2,
		Consumer<TypedValueBuilder> ... otherCallbacks);

	Map<String, ? extends TypedFieldValue> build();
}
