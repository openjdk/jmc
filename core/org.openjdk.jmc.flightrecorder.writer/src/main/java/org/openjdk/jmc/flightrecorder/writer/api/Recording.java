package org.openjdk.jmc.flightrecorder.writer.api;

import org.openjdk.jmc.flightrecorder.writer.RecordingImpl;
import org.openjdk.jmc.flightrecorder.writer.TypesImpl;

import java.util.function.Consumer;

public abstract class Recording implements AutoCloseable {
	public abstract RecordingImpl rotateChunk();

	/**
	 * Write a custom event
	 *
	 * @param event
	 *            the event value
	 * @return {@literal this} for chaining
	 * @throws IllegalArgumentException
	 *             if the event type has not got 'jdk.jfr.Event' as its super type
	 */
	public abstract RecordingImpl writeEvent(TypedValue event);

	/**
	 * Try registering a user event type with no additional attributes. If a same-named event
	 * already exists it will be returned.
	 *
	 * @param name
	 *            the event name
	 * @return a user event type of the given name
	 */
	public abstract Type registerEventType(String name);

	/**
	 * Try registering a user event type. If a same-named event already exists it will be returned.
	 *
	 * @param name
	 *            the event name
	 * @param builderCallback
	 *            will be called with the active {@linkplain TypeStructureBuilder} when the event is
	 *            newly registered
	 * @return a user event type of the given name
	 * @throws IllegalArgumentException
	 *             if 'name' or 'builderCallback' is {@literal null}
	 */
	public abstract Type registerEventType(String name, Consumer<TypeStructureBuilder> builderCallback);

	/**
	 * Try registering a user annotation type. If a same-named annotation already exists it will be
	 * returned.
	 *
	 * @param name
	 *            the annotation name
	 * @return a user annotation type of the given name
	 * @throws IllegalArgumentException
	 *             if 'name' is {@literal null}
	 */
	public abstract Type registerAnnotationType(String name);

	/**
	 * Try registering a user annotation type. If a same-named annotation already exists it will be
	 * returned.
	 *
	 * @param name
	 *            the annotation name
	 * @param builderCallback
	 *            will be called with the active {@linkplain TypeStructureBuilder} when the
	 *            annotation is newly registered
	 * @return a user annotation type of the given name
	 * @throws IllegalArgumentException
	 *             if 'name' or 'builderCallback' is {@literal null}
	 */
	public abstract Type registerAnnotationType(String name, Consumer<TypeStructureBuilder> builderCallback);

	/**
	 * Try registering a custom type. If a same-named type already exists it will be returned.
	 *
	 * @param name
	 *            the type name
	 * @param builderCallback
	 *            will be called with the active {@linkplain TypeStructureBuilder} when the type is
	 *            newly registered
	 * @return a custom type of the given name
	 * @throws IllegalArgumentException
	 *             if 'name' or 'builderCallback' is {@literal null}
	 */
	public abstract Type registerType(String name, Consumer<TypeStructureBuilder> builderCallback);

	/**
	 * Try registering a custom type. If a same-named type already exists it will be returned.
	 *
	 * @param name
	 *            the type name
	 * @param supertype
	 *            the super type name
	 * @param builderCallback
	 *            will be called with the active {@linkplain TypeStructureBuilder} when the type is
	 *            newly registered
	 * @return a custom type of the given name
	 * @throws IllegalArgumentException
	 *             if 'name' or 'builderCallback' is {@literal null}
	 */
	public abstract Type registerType(String name, String supertype, Consumer<TypeStructureBuilder> builderCallback);

	/**
	 * A convenience method to easily get to JDK registered custom types in type-safe manner.
	 *
	 * @param type
	 *            the type
	 * @return the previously registered JDK type
	 * @throws IllegalArgumentException
	 *             if 'type' is {@literal null} or an attempt to retrieve non-registered JDK type is
	 *             made
	 */
	public abstract Type getType(TypesImpl.JDK type);

	/**
	 * Try retrieving a previously registered custom type.
	 *
	 * @param typeName
	 *            the type name
	 * @return the previously registered custom type
	 * @throws IllegalArgumentException
	 *             if 'typeName' is {@literal null} or an attempt to retrieve non-registered custom
	 *             type is made
	 */
	public abstract Type getType(String typeName);

	/**
	 * @return the associated {@linkplain Types} instance
	 */
	public abstract Types getTypes();
}
