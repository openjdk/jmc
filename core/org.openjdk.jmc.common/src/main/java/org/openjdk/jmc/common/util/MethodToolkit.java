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

import java.io.IOException;

import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCPackage;
import org.openjdk.jmc.common.IMCType;

/**
 * Helper class to support {@link IMCType} and {@link IMCMethod} implementations and to handle
 * conversion of reference types and field descriptors according to <i>The Java Virtual Machine
 * Specification</i>, Section 4.4.1 and Section 4.3.2, respectively, into Java type formats
 * according to <i>The Java Language Specification</i>, Sections 6.7 and 13.1.
 */
// FIXME: Move MC* classes and related toolkits to a separate package?
public class MethodToolkit {
	private static final String TYPE_VOID = Void.TYPE.getName();
	private static final String TYPE_BOOLEAN = Boolean.TYPE.getName();
	private static final String TYPE_BYTE = Byte.TYPE.getName();
	private static final String TYPE_CHAR = Character.TYPE.getName();
	private static final String TYPE_SHORT = Short.TYPE.getName();
	private static final String TYPE_INTEGER = Integer.TYPE.getName();
	private static final String TYPE_LONG = Long.TYPE.getName();
	private static final String TYPE_FLOAT = Float.TYPE.getName();
	private static final String TYPE_DOUBLE = Double.TYPE.getName();
	private static final String[] PRIMITIVE_TYPES = {TYPE_BOOLEAN, TYPE_BYTE, TYPE_CHAR, TYPE_SHORT, TYPE_INTEGER,
			TYPE_LONG, TYPE_FLOAT, TYPE_DOUBLE, TYPE_VOID};

	/**
	 * Do not instantiate.
	 */
	private MethodToolkit() {
	}

	/**
	 * Check if a type name denotes a primitive type.
	 *
	 * @param typeName
	 *            type name to check
	 * @return {@code true} if the type is primitive, {@code false} if not
	 */
	public static boolean isPrimitive(String typeName) {
		if (typeName != null) {
			for (String element : PRIMITIVE_TYPES) {
				if (element.equals(typeName)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Create a fully qualified class name based on a package.
	 *
	 * @param mcPackage
	 *            package for the class
	 * @param className
	 *            class name
	 * @return a fully qualified class name
	 */
	public static String formatQualifiedName(IMCPackage mcPackage, String className) {
		return formatQualifiedName(mcPackage, className, '.');
	}

	/**
	 * Create a fully qualified class name based on a package.
	 *
	 * @param mcPackage
	 *            package for the class
	 * @param className
	 *            class name
	 * @param separator
	 *            The separator to use between elements in the class name. Typically only '.' and
	 *            '/' are useful.
	 * @return a fully qualified class name
	 */
	private static String formatQualifiedName(IMCPackage mcPackage, String className, char separator) {
		if (mcPackage == null || mcPackage.getName() == null || mcPackage.getName().isEmpty()) {
			return className;
		} else {
			return mcPackage.getName() + separator + className;
		}
	}

	/**
	 * Check if a class name denotes a nested class.
	 *
	 * @param className
	 *            class name to check
	 * @return {@code true} if the class is nested, {@code false} if not
	 */
	public static Boolean hasNestedTypes(String className) {
		if (className == null) {
			return false;
		} else {
			return className.indexOf('$') != -1;
		}
	}

	/**
	 * Get the top level type of a class. This is the class name without any nested components.
	 *
	 * @param className
	 *            class name with possible nested components
	 * @return class name without nested components
	 */
	public static String topLevelType(String className) {
		int index = className.indexOf('$');
		if (index == -1) {
			return className;
		} else {
			return className.substring(0, index);
		}
	}

	/**
	 * Get the nested type name of a class. This is the class name without the top level class name.
	 * If there are multiple nested levels then all levels will be returned.
	 *
	 * @param className
	 *            class name with possible nested components
	 * @return nested class name part if present, {@code null} if there is no nested class
	 */
	public static String nestedTypes(String className) {
		int index = className.indexOf('$');
		if (index == -1 && index + 1 < className.length()) {
			return null;
		} else {
			return className.substring(index + 1);
		}
	}

	/**
	 * Check if a type name denotes an array.
	 *
	 * @param typeName
	 *            type name to check
	 * @return {@code true} if the type is an array, {@code false} if not
	 */
	public static Boolean isArray(String typeName) {
		if (typeName == null) {
			return false;
		} else {
			return typeName.indexOf('[') > 0;
		}
	}

	/**
	 * Convert a reference type according to <i>The Java Virtual Machine Specification</i>, Section
	 * 4.4.1, into a field descriptor according to <i>The Java Virtual Machine Specification</i>,
	 * Section 4.3.2.
	 *
	 * @param refType
	 *            the reference type to convert
	 * @return the corresponding field descriptor
	 */
	public static String refTypeToFieldDescriptor(String refType) {
		return isDescOrRefArray(refType) ? refType : ('L' + refType + ';');
	}

	/**
	 * Convert a binary name to a canonical name, as defined in <i>The Java Language
	 * Specification</i>, Sections 6.7 and 13.1, respectively. These names only differ in that the
	 * former uses &quot;$&quot; as nested class separator while the latter uses &quot;.&quot;.
	 *
	 * @param binaryName
	 *            the binary name to convert
	 * @return the converted canonical name
	 */
	public static String binaryNameToCanonical(String binaryName) {
		return binaryName.replace('$', '.');
	}

	/**
	 * Convert a reference type according to <i>The Java Virtual Machine Specification</i>, Section
	 * 4.4.1, into a binary Java type name according to <i>The Java Language Specification</i>,
	 * Section 13.1.
	 *
	 * @param refType
	 *            the reference type to convert
	 * @return the converted name
	 * @throws IllegalArgumentException
	 *             if {@code refType} is not a valid reference type
	 */
	public static String refTypeToBinaryJLS(String refType) throws IllegalArgumentException {
		if (refType.length() == 0 || refType.charAt(0) != '[') {
			return refType.replace('/', '.');
		} else {
			return fieldDescToBinaryJLS(refType);
		}
	}

	/**
	 * Convert a reference type according to <i>The Java Virtual Machine Specification</i>, Section
	 * 4.4.1, into an {@link IMCType}.
	 *
	 * @param refType
	 *            the reference type to convert
	 * @return the type object
	 * @throws IllegalArgumentException
	 *             if {@code refType} is not a valid reference type
	 */
	public static IMCType typeFromReference(String refType) throws IllegalArgumentException {
		return typeFromBinaryJLS(refTypeToBinaryJLS(refType));
	}

	/**
	 * Convert a binary Java type name according to <i>The Java Language Specification</i>, Section
	 * 13.1, into an {@link IMCType}.
	 *
	 * @param jlsType
	 *            the JLS type name to convert
	 * @return the type object
	 * @throws IllegalArgumentException
	 *             if {@code refType} is not a valid reference type
	 */
	public static IMCType typeFromBinaryJLS(String jlsType) throws IllegalArgumentException {
		return new MCType(jlsType);
	}

	/**
	 * Convert a field descriptor according to <i>The Java Virtual Machine Specification</i>,
	 * Section 4.3.2, into a binary Java type name according to <i>The Java Language
	 * Specification</i>, Section 13.1.
	 *
	 * @param fieldDesc
	 *            the field descriptor (according to the JVM Specification) to convert
	 * @return the converted type name according to JLS
	 * @throws IllegalArgumentException
	 *             if {@code fieldDesc} is not a valid field descriptor
	 */
	public static String fieldDescToBinaryJLS(String fieldDesc) throws IllegalArgumentException {
		StringBuilder out = new StringBuilder();
		try {
			fieldDescToBinaryJLS(fieldDesc, 0, out);
		} catch (IOException e) {
			// This shouldn't possibly happen with StringBuilder. It's just
			// an effect of the parameter being declared as Appendable.
			throw new RuntimeException("Implementation error", e); //$NON-NLS-1$
		}
		return out.toString();
	}

	/**
	 * Convert the field descriptor, according to <i>The Java Virtual Machine Specification</i>,
	 * Section 4.3.2, starting at position {@code start} in {@code desc}, into a binary Java type
	 * name according to <i>The Java Language Specification</i>, Section 13.1. and appends it to
	 * {@code out}.
	 *
	 * @param desc
	 *            a {@link CharSequence} containing the field descriptor (according to the JVM
	 *            Specification) to convert
	 * @param start
	 *            the position in {@code desc} where the descriptor to convert starts
	 * @param out
	 *            an {@link Appendable} to which the result will be appended
	 * @return the first position in {@code desc} not converted
	 * @throws IllegalArgumentException
	 *             if {@code desc} is not a valid field descriptor
	 * @throws IOException
	 *             If an I/O error occurs when appending to {@code out}. Note that this cannot
	 *             happen when using {@link StringBuilder} or {@link StringBuffer}.
	 */
	public static int fieldDescToBinaryJLS(CharSequence desc, int start, Appendable out)
			throws IllegalArgumentException, IOException {
		int top = desc.length();
		if (start >= top) {
			throw new IllegalArgumentException("start=" + start + " > in.length()=" + top); //$NON-NLS-1$ //$NON-NLS-2$
		}

		int pos = start;
		char c = desc.charAt(pos++);
		switch (c) {
		case '[':
			pos = fieldDescToBinaryJLS(desc, pos, out);
			out.append("[]"); //$NON-NLS-1$
			break;
		case 'B':
			out.append(TYPE_BYTE);
			break;
		case 'C':
			out.append(TYPE_CHAR);
			break;
		case 'D':
			out.append(TYPE_DOUBLE);
			break;
		case 'F':
			out.append(TYPE_FLOAT);
			break;
		case 'I':
			out.append(TYPE_INTEGER);
			break;
		case 'J':
			out.append(TYPE_LONG);
			break;
		case 'S':
			out.append(TYPE_SHORT);
			break;
		case 'Z':
			out.append(TYPE_BOOLEAN);
			break;
		case 'V':
			out.append(TYPE_VOID);
			break;
		case 'L':
			while (pos < top) {
				c = desc.charAt(pos++);
				if (c == ';') {
					return pos;
				}
				out.append((c == '/') ? '.' : c);
			}
			throw new IllegalArgumentException("Class name '" + desc.subSequence(start + 1, pos) //$NON-NLS-1$
					+ "' in field descriptor not terminated with ';'."); //$NON-NLS-1$
		default:
			throw new IllegalArgumentException("The char '" + c + "' is not a valid first char of a field descriptor."); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return pos;
	}

	/**
	 * Check if a field descriptor or a reference type according to <i>The Java Virtual Machine
	 * Specification</i>, Sections 4.3.2 and 4.4.1 respectively, designates an array.
	 *
	 * @param fieldDesc
	 *            a non-null field descriptor or reference type (according to the JVM Specification)
	 * @return true iff the descriptor denotes an array type
	 */
	public static boolean isDescOrRefArray(String fieldDesc) {
		return fieldDesc.startsWith("["); //$NON-NLS-1$
	}

	/**
	 * Check if a field descriptor according to <i>The Java Virtual Machine Specification</i>,
	 * Section 4.3.2, designates a primitive type.
	 *
	 * @param fieldDesc
	 *            a non-null field descriptor (according to the JVM Specification)
	 * @return true iff the descriptor denotes a primitive type
	 */
	public static boolean isDescPrimitive(String fieldDesc) {
		// Should work as long as the descriptor is valid.
		return (fieldDesc.length() == 1);
	}
}
