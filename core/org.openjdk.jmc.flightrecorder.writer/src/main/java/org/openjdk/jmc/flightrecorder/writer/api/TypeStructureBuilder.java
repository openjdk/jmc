package org.openjdk.jmc.flightrecorder.writer.api;

import org.openjdk.jmc.flightrecorder.writer.api.Types.Predefined;

import java.util.function.Consumer;

/** A fluent API for building composite types lazily. */
public interface TypeStructureBuilder {
	/**
	 * Add a field of the given name and (predefined) type
	 *
	 * @param name
	 *            the field name
	 * @param type
	 *            the field type
	 * @return a {@linkplain TypeStructureBuilder} instance for invocation chaining
	 */
	TypeStructureBuilder addField(String name, Predefined type);

	/**
	 * Add a field of the given name and (predefined) type and with a customization callback
	 *
	 * @param name
	 *            the field name
	 * @param type
	 *            the field type
	 * @param fieldCallback
	 *            the field customization callback
	 * @return a {@linkplain TypeStructureBuilder} instance for invocation chaining
	 */
	TypeStructureBuilder addField(String name, Predefined type, Consumer<TypedFieldBuilder> fieldCallback);

	/**
	 * Add a field of the given name and type
	 *
	 * @param name
	 *            the field name
	 * @param type
	 *            the field type
	 * @return a {@linkplain TypeStructureBuilder} instance for invocation chaining
	 */
	TypeStructureBuilder addField(String name, Type type);

	/**
	 * Add a field of the given name and type and with a customization callback
	 *
	 * @param name
	 *            the field name
	 * @param type
	 *            the field type
	 * @param fieldCallback
	 *            the field customization callback
	 * @return a {@linkplain TypeStructureBuilder} instance for invocation chaining
	 */
	TypeStructureBuilder addField(String name, Type type, Consumer<TypedFieldBuilder> fieldCallback);

	/**
	 * Add a specific field.
	 *
	 * @param field
	 *            field
	 * @return a {@linkplain TypeStructureBuilder} instance for invocation chaining
	 */
	TypeStructureBuilder addField(TypedField field);

	/**
	 * Add specific fields.
	 *
	 * @param field1
	 *            first field
	 * @param field2
	 *            second field
	 * @param fields
	 *            other fields (if any)
	 * @return a {@linkplain TypeStructureBuilder} instance for invocation chaining
	 */
	TypeStructureBuilder addFields(TypedField field1, TypedField field2, TypedField ... fields);

	/**
	 * Add an annotation of the given type
	 *
	 * @param type
	 *            the annotation type
	 * @return a {@linkplain TypeStructureBuilder} instance for invocation chaining
	 */
	TypeStructureBuilder addAnnotation(Type type);

	/**
	 * Add an annotation of the given type and with the given value
	 *
	 * @param type
	 *            the annotation type
	 * @param value
	 *            the annotation value
	 * @return a {@linkplain TypeStructureBuilder} instance for invocation chaining
	 */
	TypeStructureBuilder addAnnotation(Type type, String value);

	/**
	 * A special placeholder type to refer to the type being currently built (otherwise impossible
	 * because the type is not yet ready).
	 *
	 * @return a special {@linkplain Type} denoting 'self' reflecting type
	 */
	Type selfType();

	/**
	 * @return
	 */
	TypeStructure build();

	Type registerAs(String name, String supertype);
}
