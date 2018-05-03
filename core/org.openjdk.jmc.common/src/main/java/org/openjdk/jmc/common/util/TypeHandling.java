/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at http://oss.oracle.com/licenses/upl
 *
 * or the following license:
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openjdk.jmc.common.util;

import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IMCClassLoader;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.messages.internal.Messages;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;

/**
 * Helper class to provide some formatting of type names.
 */
public final class TypeHandling {

	private static final HashMap<String, String> formalPrimitiveMap = new HashMap<>();
	private static final String VALUE_COMPOSITE_DATA = "CompositeData"; //$NON-NLS-1$
	private static final String VALUE_TABULAR_DATA = "TabularData"; //$NON-NLS-1$

	static {
		formalPrimitiveMap.put("B", "byte"); //$NON-NLS-1$ //$NON-NLS-2$
		formalPrimitiveMap.put("C", "char"); //$NON-NLS-1$ //$NON-NLS-2$
		formalPrimitiveMap.put("D", "double"); //$NON-NLS-1$ //$NON-NLS-2$
		formalPrimitiveMap.put("F", "float"); //$NON-NLS-1$ //$NON-NLS-2$
		formalPrimitiveMap.put("I", "int"); //$NON-NLS-1$ //$NON-NLS-2$
		formalPrimitiveMap.put("J", "long"); //$NON-NLS-1$ //$NON-NLS-2$
		formalPrimitiveMap.put("S", "short"); //$NON-NLS-1$ //$NON-NLS-2$
		formalPrimitiveMap.put("Z", "boolean"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static HashMap<String, Class<?>> primitiveNameToClassMap = new HashMap<>();

	static {
		primitiveNameToClassMap.put(int.class.getName(), int.class);
		primitiveNameToClassMap.put(long.class.getName(), long.class);
		primitiveNameToClassMap.put(short.class.getName(), short.class);
		primitiveNameToClassMap.put(char.class.getName(), char.class);
		primitiveNameToClassMap.put(byte.class.getName(), byte.class);
		primitiveNameToClassMap.put(float.class.getName(), float.class);
		primitiveNameToClassMap.put(double.class.getName(), double.class);
		primitiveNameToClassMap.put(boolean.class.getName(), boolean.class);
	}

	private static HashMap<Class<?>, Class<?>> primitiveToObjectClassMap = new HashMap<>();

	static {
		primitiveToObjectClassMap.put(int.class, Integer.class);
		primitiveToObjectClassMap.put(long.class, Long.class);
		primitiveToObjectClassMap.put(short.class, Short.class);
		primitiveToObjectClassMap.put(char.class, Character.class);
		primitiveToObjectClassMap.put(byte.class, Byte.class);
		primitiveToObjectClassMap.put(float.class, Float.class);
		primitiveToObjectClassMap.put(double.class, Double.class);
		primitiveToObjectClassMap.put(boolean.class, Boolean.class);
	}

	private TypeHandling() {
	}

	/**
	 * Returns a simplified description of a type name. This involves transforming type names from
	 * their formal descriptors to more human friendly forms, removing package names from certain
	 * well-known classes, transforming formal array specified to square brackets etc.
	 *
	 * @param typeName
	 *            a type name
	 * @return a simplified description of the type name
	 */
	public static String simplifyType(String typeName) {
		if (typeName == null) {
			return "null"; //$NON-NLS-1$
		}
		StringBuilder arrayBuilder = new StringBuilder();
		while (typeName.startsWith("[")) { //$NON-NLS-1$
			typeName = typeName.substring(1);
			arrayBuilder.append("[]"); //$NON-NLS-1$
		}
		if (typeName.endsWith(";")) { //$NON-NLS-1$
			typeName = typeName.substring(1, typeName.length() - 1); // Remove L and ; from L<classname>;
		}
		if (typeName.equals(CompositeData.class.getName())) {
			typeName = "CompositeData"; //$NON-NLS-1$
		} else if (typeName.equals(TabularData.class.getName())) {
			typeName = "TabularData"; //$NON-NLS-1$
		} else if (typeName.equals(String.class.getName())) {
			typeName = "String"; //$NON-NLS-1$
		} else if (formalPrimitiveMap.containsKey(typeName)) {
			typeName = formalPrimitiveMap.get(typeName);
		} else if (typeName.startsWith("java.lang.") && typeName.lastIndexOf('.') == 9) { //$NON-NLS-1$
			typeName = typeName.substring(10);
		}
		return typeName + arrayBuilder.toString();
	}

	private static String createSizeString(String typeName, int size) {
		return MessageFormat.format(Messages.getString(Messages.TypeHandling_MESSAGE_SIZE), typeName,
				Integer.valueOf(size));
	}

	/**
	 * Returns the value in possible augmented way. It could be viewed as an override of toString().
	 *
	 * @param value
	 *            the value to textualize
	 * @return the value as a string
	 */
	public static String getValueString(Object value) {
		if (value != null) {
			if (value instanceof CompositeData) {
				return createSizeString(VALUE_COMPOSITE_DATA, ((CompositeData) value).values().size());
			} else if (value instanceof TabularData) {
				return createSizeString(VALUE_TABULAR_DATA, ((TabularData) value).size());
			} else if (value.getClass().isArray()) {
				String typeString = simplifyType(value.getClass().getName());
				int firstBracketIndex = typeString.indexOf('[');
				return (typeString.substring(0, firstBracketIndex + 1) + Array.getLength(value)
						+ typeString.substring(firstBracketIndex + 1));
			} else if (value instanceof Collection) {
				return createSizeString(value.getClass().getName(), ((Collection<?>) value).size());
			} else if (value instanceof Map) {
				return createSizeString(value.getClass().getName(), ((Map<?, ?>) value).size());
			} else if (isMinTimespan(value)) {
				return "-\u221e"; //$NON-NLS-1$
			} else if (isMaxTimespan(value)) {
				return "\u221e"; //$NON-NLS-1$
			} else if (value instanceof IDisplayable) {
				return ((IDisplayable) value).displayUsing(IDisplayable.AUTO);
			} else if (value instanceof IDescribable) {
				return ((IDescribable) value).getName();
			} else if (value instanceof Date) {
				return DateFormat.getDateTimeInstance().format(value);
			} else if (value instanceof IMCType) {
				return FormatToolkit.getType((IMCType) value, true);
			} else if (value instanceof IMCMethod) {
				// FIXME: Get the formatting options from MethodFormatter? Or from Preferences?
				return FormatToolkit.getHumanReadable((IMCMethod) value, true, false, true, true, true, false);
			} else if (value instanceof IMCClassLoader) {
				return FormatToolkit.getHumanReadable((IMCClassLoader) value);
			} else {
				return value.toString();
			}
		} else {
			return "null"; //$NON-NLS-1$
		}
	}

	/**
	 * Returns a possibly more verbose description of an object. This is similar to
	 * {@link #getValueString(Object)}, but can return longer texts.
	 * 
	 * @param value
	 *            the value to textualize
	 * @return the value as a string
	 */
	public static String getVerboseString(Object value) {
		if (value instanceof IDisplayable) {
			return ((IDisplayable) value).displayUsing(IDisplayable.VERBOSE);
		} else if (value instanceof IDescribable) {
			return ((IDescribable) value).getDescription();
		} else {
			return getValueString(value);
		}
	}

	private static boolean isMinTimespan(Object value) {
		if (value instanceof IQuantity) {
			IQuantity q = (IQuantity) value;
			if (q.getType() == UnitLookup.TIMESPAN && q.longValue() == Long.MIN_VALUE) {
				return true;
			}
		}
		return false;
	}

	private static boolean isMaxTimespan(Object value) {
		if (value instanceof IQuantity) {
			IQuantity q = (IQuantity) value;
			if (q.getType() == UnitLookup.TIMESPAN && q.longValue() == Long.MAX_VALUE) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Return the class object for given type name.
	 *
	 * @param name
	 *            the name of the type
	 * @return the class object of the type
	 * @throws ClassNotFoundException
	 *             if name is an invalid class name
	 */
	public static Class<?> getClassWithName(String name) throws ClassNotFoundException {
		if (name == null) {
			throw new ClassNotFoundException("The class name may not be null!"); //$NON-NLS-1$
		}
		Class<?> c = primitiveNameToClassMap.get(name);
		if (c == null) {
			return Class.forName(name);
		} else {
			return c;
		}
	}

	/**
	 * Return the non-primitive class corresponding to argument class (int -&gt; Integer).
	 *
	 * @param primitiveClass
	 *            the class to convert
	 * @return the non-primitive class or the argument class if non-primitive
	 */
	public static Class<?> toNonPrimitiveClass(Class<?> primitiveClass) {
		if (primitiveClass.isPrimitive()) {
			return primitiveToObjectClassMap.get(primitiveClass);
		}
		return primitiveClass;
	}

	/**
	 * Check if a class name represents a primitive type or not.
	 *
	 * @param className
	 *            the name to check
	 * @return {@code true} if the class name represents a primitive type, {@code false} otherwise
	 */
	public static boolean isPrimitive(String className) {
		return primitiveNameToClassMap.containsKey(className);
	}

	/**
	 * Check if an object is of a specified type and cast it to that if possible.
	 * 
	 * @param o
	 *            object to cast
	 * @param type
	 *            type to cast the object to
	 * @return the object cast to the specified type if possible, {@code null} if not
	 */
	public static <T> T cast(Object o, Class<T> type) {
		return type.isInstance(o) ? type.cast(o) : null;
	}
}
