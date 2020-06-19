package org.openjdk.jmc.flightrecorder.writer.api;

import org.openjdk.jmc.flightrecorder.writer.TypedFieldImpl;

import java.util.List;
import java.util.function.Consumer;

/** A JFR type */
public interface Type extends NamedType {
	/** @return unique type ID */
	long getId();

	/** @return is the type built-in or a custom type */
	boolean isBuiltin();

	/**
	 * A simple type has only one field which is of a built-in type
	 *
	 * @return {@literal true} if the type is 'simple'
	 */
	boolean isSimple();

	/**
	 * @return {@literal true} if the type is fully resolved
	 */
	boolean isResolved();

	/** @return is the type using constant pool */
	boolean hasConstantPool();

	/** @return the super type - may be {@literal null} */
	String getSupertype();

	/** @return the type field structure */
	List<? extends TypedField> getFields();

	/**
	 * @param name
	 *            field name
	 * @return the {@link TypedFieldImpl field} instance
	 */
	TypedFieldImpl getField(String name);

	/**
	 * @return attached annotations
	 */
	List<Annotation> getAnnotations();

	/**
	 * Checks whether the type can accept the given value
	 *
	 * @param value
	 *            the value to check
	 * @return {@literal true} only if the type can safely hold the value
	 * @throws IllegalArgumentException
	 *             if the type can not accept the given value
	 */
	boolean canAccept(Object value);

	/**
	 * Shortcut for wrapping the given value instance as a {@linkplain TypedValue} object
	 *
	 * @param value
	 *            the value to wrap
	 * @return a {@linkplain TypedValue} object representing the typed value
	 * @throws IllegalArgumentException
	 *             if the type can not accept the given value
	 */
	TypedValue asValue(String value);

	/**
	 * Shortcut for wrapping the given value instance as a {@linkplain TypedValue} object
	 *
	 * @param value
	 *            the value to wrap
	 * @return a {@linkplain TypedValue} object representing the typed value
	 * @throws IllegalArgumentException
	 *             if the type can not accept the given value
	 */
	TypedValue asValue(byte value);

	/**
	 * Shortcut for wrapping the given value instance as a {@linkplain TypedValue} object
	 *
	 * @param value
	 *            the value to wrap
	 * @return a {@linkplain TypedValue} object representing the typed value
	 * @throws IllegalArgumentException
	 *             if the type can not accept the given value
	 */
	TypedValue asValue(char value);

	/**
	 * Shortcut for wrapping the given value instance as a {@linkplain TypedValue} object
	 *
	 * @param value
	 *            the value to wrap
	 * @return a {@linkplain TypedValue} object representing the typed value
	 * @throws IllegalArgumentException
	 *             if the type can not accept the given value
	 */
	TypedValue asValue(short value);

	/**
	 * Shortcut for wrapping the given value instance as a {@linkplain TypedValue} object
	 *
	 * @param value
	 *            the value to wrap
	 * @return a {@linkplain TypedValue} object representing the typed value
	 * @throws IllegalArgumentException
	 *             if the type can not accept the given value
	 */
	TypedValue asValue(int value);

	/**
	 * Shortcut for wrapping the given value instance as a {@linkplain TypedValue} object
	 *
	 * @param value
	 *            the value to wrap
	 * @return a {@linkplain TypedValue} object representing the typed value
	 * @throws IllegalArgumentException
	 *             if the type can not accept the given value
	 */
	TypedValue asValue(long value);

	/**
	 * Shortcut for wrapping the given value instance as a {@linkplain TypedValue} object
	 *
	 * @param value
	 *            the value to wrap
	 * @return a {@linkplain TypedValue} object representing the typed value
	 * @throws IllegalArgumentException
	 *             if the type can not accept the given value
	 */
	TypedValue asValue(float value);

	/**
	 * Shortcut for wrapping the given value instance as a {@linkplain TypedValue} object
	 *
	 * @param value
	 *            the value to wrap
	 * @return a {@linkplain TypedValue} object representing the typed value
	 * @throws IllegalArgumentException
	 *             if the type can not accept the given value
	 */
	TypedValue asValue(double value);

	/**
	 * Shortcut for wrapping the given value instance as a {@linkplain TypedValue} object
	 *
	 * @param value
	 *            the value to wrap
	 * @return a {@linkplain TypedValue} object representing the typed value
	 * @throws IllegalArgumentException
	 *             if the type can not accept the given value
	 */
	TypedValue asValue(boolean value);

	/**
	 * Shortcut for creating a new {@linkplain TypedValue} object for this type
	 *
	 * @param builderCallback
	 *            will be called when the new {@linkplain TypedValue} is being initialized
	 * @return a {@linkplain TypedValue} object representing the typed value
	 * @throws IllegalArgumentException
	 *             if the type can not be used with builder callback
	 */
	TypedValue asValue(Consumer<TypedValueBuilder> builderCallback);

	TypedValue asValue(Object value);

	/**
	 * @return a specific {@linkplain TypedValue} instance designated as the {@literal null} value
	 *         for this type
	 */
	TypedValue nullValue();

	/**
	 * @return a new {@linkplain TypedValueBuilder} instance for a value of this particular type
	 */
	TypedValueBuilder valueBuilder();

	/**
	 * Checks whether this particular type is used as a field type in the other type.
	 * 
	 * @param other
	 *            the other type
	 * @return {@literal true} if any of the fields in 'other' type are of this particular type
	 */
	boolean isUsedBy(Type other);

	/**
	 * @return the associated {@linkplain Types} helper class instance
	 */
	Types getTypes();
}
