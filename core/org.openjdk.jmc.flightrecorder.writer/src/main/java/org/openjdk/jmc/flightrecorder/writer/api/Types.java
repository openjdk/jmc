package org.openjdk.jmc.flightrecorder.writer.api;

import org.openjdk.jmc.flightrecorder.writer.ResolvableType;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class Types {
	/**
	 * Retrieve the given type or create it a-new if it hasn't been added yet.
	 *
	 * @param type
	 *            the type to retrieve
	 * @param builderCallback
	 *            will be called lazily when the type is about to be initialized
	 * @return the corresponding {@link Type type} instance
	 */
	public abstract Type getOrAdd(Predefined type, Consumer<TypeStructureBuilder> builderCallback);

	/**
	 * Retrieve the given type or create it a-new if it hasn't been added yet.
	 *
	 * @param name
	 *            the name of the type to retrieve
	 * @param builderCallback
	 *            will be called lazily when the type is about to be initialized
	 * @return the corresponding {@link Type type} instance
	 */
	public abstract Type getOrAdd(String name, Consumer<TypeStructureBuilder> builderCallback);

	/**
	 * Retrieve the given type or create it a-new if it hasn't been added yet.
	 *
	 * @param type
	 *            the type to retrieve
	 * @param supertype
	 *            the super type name
	 * @param builderCallback
	 *            will be called lazily when the type is about to be initialized
	 * @return the corresponding {@link Type type} instance
	 */
	public abstract Type getOrAdd(Predefined type, String supertype, Consumer<TypeStructureBuilder> builderCallback);

	/**
	 * Retrieve the given type or create it a-new if it hasn't been added yet.
	 *
	 * @param name
	 *            the name of the type to retrieve
	 * @param supertype
	 *            the super type name
	 * @param builderCallback
	 *            will be called lazily when the type is about to be initialized
	 * @return the corresponding {@link Type type} instance
	 */
	public abstract Type getOrAdd(String name, String supertype, Consumer<TypeStructureBuilder> builderCallback);

	public abstract Type getOrAdd(String name, String supertype, TypeStructure typeStructure);

	/**
	 * Retrieve the type by its name.
	 *
	 * @param name
	 *            the type name
	 * @return the registered {@link Type type} or {@literal null}
	 */
	public abstract Type getType(String name);

	/**
	 * Retrieve the type by its name. If the type hasn't been added yet a
	 * {@linkplain ResolvableType} wrapper may be returned.
	 *
	 * @param name
	 *            the type name
	 * @param asResolvable
	 *            if {@literal true} a {@linkplain ResolvableType} wrapper will be returned instead
	 *            of {@literal null} for non-existent type
	 * @return an existing {@link Type} type, {@literal null} or a {@linkplain ResolvableType}
	 *         wrapper
	 */
	public abstract Type getType(String name, boolean asResolvable);

	/**
	 * A convenience shortcut to get a {@linkplain Type} instance corresponding to the
	 * {@linkplain Predefined} type
	 *
	 * @param type
	 *            the predefined/enum type
	 * @return the registered {@linkplain Type} instance or a {@linkplain ResolvableType} wrapper
	 */
	public abstract Type getType(Predefined type);

	public abstract TypeStructureBuilder typeStructureBuilder();

	/**
	 * @param fieldName
	 *            field name
	 * @param fieldType
	 *            field type
	 * @return a new {@linkplain TypedFieldBuilder} instance for a named field of the given type
	 */
	public abstract TypedFieldBuilder fieldBuilder(String fieldName, Type fieldType);

	/**
	 * @param fieldName
	 *            field name
	 * @param fieldType
	 *            field type
	 * @return a new {@linkplain TypedFieldBuilder} instance for a named field of the given type
	 */
	public abstract TypedFieldBuilder fieldBuilder(String fieldName, Builtin fieldType);

	/** A {@link Type type} predefined by the writer */
	public interface Predefined extends NamedType {
	}

	/** Built-in types */
	public enum Builtin implements Predefined {
		BYTE("byte", byte.class),
		CHAR("char", char.class),
		SHORT("short", short.class),
		INT("int", int.class),
		LONG("long", long.class),
		FLOAT("float", float.class),
		DOUBLE("double", double.class),
		BOOLEAN("boolean", boolean.class),
		STRING("java.lang.String", String.class);

		private static Map<String, Builtin> NAME_MAP;
		private final String typeName;
		private final Class<?> type;

		Builtin(String name, Class<?> type) {
			addName(name);
			this.typeName = name;
			this.type = type;
		}

		private static Map<String, Builtin> getNameMap() {
			if (NAME_MAP == null) {
				NAME_MAP = new HashMap<>();
			}
			return NAME_MAP;
		}

		private void addName(String name) {
			getNameMap().put(name, this);
		}

		public static boolean hasType(String name) {
			return getNameMap().containsKey(name);
		}

		public static Builtin ofName(String name) {
			return getNameMap().get(name);
		}

		public static Builtin ofType(Type type) {
			return ofName(type.getTypeName());
		}

		@Override
		public String getTypeName() {
			return typeName;
		}

		public Class<?> getTypeClass() {
			return type;
		}
	}

	/** Types (subset of) defined by the JFR JVM implementation */
	public enum JDK implements Predefined {
		TICKSPAN("jdk.type.Tickspan"),
		TICKS("jdk.type.Ticks"),
		THREAD_GROUP("jdk.types.ThreadGroup"),
		THREAD("java.lang.Thread"),
		STACK_TRACE("jdk.types.StackTrace"),
		STACK_FRAME("jdk.types.StackFrame"),
		METHOD("jdk.types.Method"),
		FRAME_TYPE("jdk.types.FrameType"),
		CLASS("java.lang.Class"),
		SYMBOL("jdk.types.Symbol"),
		CLASS_LOADER("jdk.type.ClassLoader"),
		PACKAGE("jdk.types.Package"),
		MODULE("jdk.types.Module"),
		ANNOTATION_LABEL("jdk.jfr.Label"),
		ANNOTATION_CONTENT_TYPE("jdk.jfr.ContentType"),
		ANNOTATION_NAME("jdk.jfr.Name"),
		ANNOTATION_DESCRIPTION("jdk.jfr.Description"),
		ANNOTATION_TIMESTAMP("jdk.jfr.Timestamp"),
		ANNOTATION_TIMESPAN("jdk.jfr.Timespan"),
		ANNOTATION_UNSIGNED("jdk.jfr.Unsigned");

		private final String typeName;

		JDK(String name) {
			this.typeName = name;
		}

		@Override
		public String getTypeName() {
			return typeName;
		}
	}
}
