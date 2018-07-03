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

import org.openjdk.jmc.common.IMCClassLoader;
import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCPackage;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.messages.internal.Messages;

/**
 * Methods for formatting IMC* instances.
 */
// FIXME: Avoid exposing if possible. Could be moved to another package together with IMCMethod etc.
public class FormatToolkit {
	private static final char LP = '(';
	private static final char RP = ')';
	private static final char LB = '[';
	private static final char CLASS_SUFFIX = ';';
	private static final char CLASS_PREFIX = 'L';
	private static final char PACKAGE_SEPARATOR = '.';
	private static final String COMMA_SEPARATOR = ", "; //$NON-NLS-1$
	private static final String ARRAY = "[]"; //$NON-NLS-1$

	/**
	 * Get a human readable string representing a method, displays all available information
	 *
	 * @return a human readable string representing the method
	 */
	public static String getHumanReadable(IMCMethod method) {
		return getHumanReadable(method, true, true, true, true, true, true);
	}

	/**
	 * Get a human readable string representing a method.
	 *
	 * @param method
	 *            the method to get a string for
	 * @param showReturnValue
	 *            {@code true} if the return value type should be included
	 * @param showReturnValuePackage
	 *            {@code true} if the package name of the return value type should be included. Only
	 *            relevant if {@code showReturnValue} is {@code true}.
	 * @param showClassName
	 *            {@code true} if the class name for the method should be included
	 * @param showClassPackageName
	 *            {@code true} if the package name of the class for the method should be included.
	 *            Only relevant if {@code showClassName} is {@code true}.
	 * @param showArguments
	 *            {@code true} if the class names for the method arguments should be included
	 * @param showArgumentsPackage
	 *            {@code true} if the package names of the classes for the method arguments should
	 *            be included. Only relevant if {@code showArguments} is {@code true}.
	 * @return a human readable string representing the method
	 */
	public static String getHumanReadable(
		IMCMethod method, boolean showReturnValue, boolean showReturnValuePackage, boolean showClassName,
		boolean showClassPackageName, boolean showArguments, boolean showArgumentsPackage) {
		String ret = ""; //$NON-NLS-1$

		try {
			if (showReturnValue) {
				ret += getReturnType(method.getFormalDescriptor(), showReturnValuePackage) + ' ';
			}
			if (showClassName) {
				ret += getType(method.getType(), showClassPackageName) + "."; //$NON-NLS-1$
			}
			ret += method.getMethodName();
			String arguments = getParameters(method.getFormalDescriptor(), showArgumentsPackage);

			if (!"()".equals(arguments) && !showArguments) { //$NON-NLS-1$
				ret += "(...)"; //$NON-NLS-1$
			} else {
				ret += arguments;
			}

		} catch (Exception e) {
			return null;
		}
		return ret;
	}

	/**
	 * Get the package name as a human readable string. If it is the default package (the empty
	 * string), then get a describing text for that.
	 *
	 * @param mcPackage
	 *            package instance to format
	 * @return the package name
	 */
	public static String getPackage(IMCPackage mcPackage) {
		if (mcPackage == null) {
			return "null"; //$NON-NLS-1$
		}
		String packageName = mcPackage.getName();
		if (packageName == null) {
			return "null"; //$NON-NLS-1$
		} else if (packageName.length() == 0) {
			return Messages.getString(Messages.FormatToolkit_DEFAULT_PACKAGE);
		} else {
			return packageName;
		}
	}

	/**
	 * Get the type name as a human readable string.
	 *
	 * @param type
	 *            type instance to format
	 * @param qualified
	 *            {@code true} if the returned string should be fully qualified
	 * @return the type name, fully qualified if requested so
	 */
	public static String getType(IMCType type, boolean qualified) {
		return qualified ? MethodToolkit.formatQualifiedName(type.getPackage(), type.getTypeName())
				: type.getTypeName();
	}

	private static String getReturnType(String descriptor, boolean qualified) throws Exception {
		StringBuffer output = new StringBuffer();
		parseToken(output, descriptor.substring(descriptor.indexOf(RP) + 1), 0, qualified);
		return output.toString();
	}

	private static String getParameters(String descriptor, boolean qualified) throws Exception {
		String input = descriptor.substring(descriptor.indexOf(LP) + 1, descriptor.lastIndexOf(RP));
		StringBuffer output = new StringBuffer(Character.toString(LP));
		int next = -1;
		for (int current = 0; current < input.length(); current = next) {
			next = parseToken(output, input, current, qualified);
			if (next == current) {
				break;
			}
			if (next < input.length()) {
				output.append(COMMA_SEPARATOR);
			}
		}
		return output.append(RP).toString();
	}

	private static int parseToken(StringBuffer output, String input, int position, boolean qualified) throws Exception {
		if (input.charAt(position) == LB) {
			return readArray(output, input, position, qualified);
		}
		if (input.charAt(position) == CLASS_PREFIX) {
			return readComponentType(output, input, position, qualified);
		}
		return readPrimitive(output, input, position, qualified);
	}

	private static int readArray(StringBuffer output, String input, int position, boolean qualified) throws Exception {
		return write(output, ARRAY, parseToken(output, input, position + 1, qualified));
	}

	private static int readPrimitive(StringBuffer output, String input, int position, boolean qualified)
			throws Exception {
		return write(output, getPrimitiveType(input.charAt(position)), position + 1);
	}

	private static int write(StringBuffer output, String string, int next_position) {
		output.append(string);
		return next_position;
	}

	private static int readComponentType(StringBuffer output, String input, int position, boolean qualified) {
		int endIndex = input.indexOf(CLASS_SUFFIX, position);
		return write(output, getClass(input.substring(position + 1, endIndex).replace('/', '.'), qualified),
				endIndex + 1);
	}

	private static String getClass(String clazz, boolean qualified) {
		return (qualified) ? clazz : clazz.substring(clazz.lastIndexOf(PACKAGE_SEPARATOR) + 1);
	}

	private static String getPrimitiveType(char ch) {
		if (ch == 'Z') {
			return Boolean.TYPE.toString();
		}
		if (ch == 'B') {
			return Byte.TYPE.toString();
		}
		if (ch == 'S') {
			return Short.TYPE.toString();
		}
		if (ch == 'I') {
			return Integer.TYPE.toString();
		}
		if (ch == 'C') {
			return Character.TYPE.toString();
		}
		if (ch == 'J') {
			return Long.TYPE.toString();
		}
		if (ch == 'F') {
			return Float.TYPE.toString();
		}
		if (ch == 'D') {
			return Double.TYPE.toString();
		}
		if (ch == 'V') {
			return Void.TYPE.toString();
		}

		return null;
	}

	/**
	 * Get a human readable string representing a stack trace, displays all available information.
	 *
	 * @param trace
	 *            the stack trace to get a string for
	 * @return a human readable string representing the stack trace
	 */
	public static String getHumanReadable(IMCStackTrace trace) {
		return getHumanReadable(trace, true, true, true, true, true, true, Integer.MAX_VALUE, null, null, null);
	}

	/**
	 * Get a human readable string representing a stack trace, displays all available information.
	 *
	 * @param trace
	 *            the stack trace to get a string for
	 * @param indent
	 *            string to use for indentation, defaults to four spaces if parameter is null
	 * @param linePrefix
	 *            string to use as a line prefix, defaults to "at " if parameter is null string to
	 *            use for indentation
	 * @param lineSeparator
	 *            string to use as line separator, defaults to line separator property if parameter
	 *            is null
	 * @return a human readable string representing the stack trace
	 */
	public static String getHumanReadable(IMCStackTrace trace, String indent, String linePrefix, String lineSeparator) {
		return getHumanReadable(trace, true, true, true, true, true, true, Integer.MAX_VALUE, indent, linePrefix,
				lineSeparator);
	}

	/**
	 * Get a human readable string representing a stack trace.
	 *
	 * @param trace
	 *            the stack trace to get a string for
	 * @param showReturnValue
	 *            {@code true} if the return value type should be included
	 * @param showReturnValuePackage
	 *            {@code true} if the package name of the return value type should be included. Only
	 *            relevant if {@code showReturnValue} is {@code true}.
	 * @param showClassName
	 *            {@code true} if the class name for the method should be included
	 * @param showClassPackageName
	 *            {@code true} if the package name of the class for the method should be included.
	 *            Only relevant if {@code showClassName} is {@code true}.
	 * @param showArguments
	 *            {@code true} if the class names for the method arguments should be included
	 * @param showArgumentsPackage
	 *            {@code true} if the package names of the classes for the method arguments should
	 *            be included. Only relevant if {@code showArguments} is {@code true}.
	 * @param indent
	 *            string to use for indentation, defaults to four spaces if parameter is null
	 * @param linePrefix
	 *            string to use as a line prefix, defaults to "at " if parameter is null string to
	 *            use for indentation
	 * @param lineSeparator
	 *            string to use as line separator, defaults to line separator property if parameter
	 *            is null
	 * @return a human readable string representing the stack trace
	 */
	public static String getHumanReadable(
		IMCStackTrace trace, boolean showReturnValue, boolean showReturnValuePackage, boolean showClassName,
		boolean showClassPackageName, boolean showArguments, boolean showArgumentsPackage,
		int maximumVisibleStackTraceElements, String indent, String linePrefix, String lineSeparator) {
		indent = indent != null ? indent : "    "; //$NON-NLS-1$
		linePrefix = linePrefix != null ? linePrefix : "at "; //$NON-NLS-1$
		lineSeparator = lineSeparator != null ? lineSeparator : System.getProperty("line.separator"); //$NON-NLS-1$

		StringBuilder sb = new StringBuilder();
		if (trace.getFrames() != null && trace.getFrames().size() > 0) {
			int rowIndex = 0;
			int count = trace.getFrames().size();
			for (IMCFrame frame : trace.getFrames()) {
				IMCMethod method = frame.getMethod();
				String methodText = getHumanReadable(method, showReturnValue, showReturnValuePackage, showClassName,
						showClassPackageName, showArguments, showArgumentsPackage);

				sb.append(indent).append(linePrefix).append(methodText).append(lineSeparator);

				if (rowIndex == maximumVisibleStackTraceElements && rowIndex != count - 1) {
					sb.append(indent).append("..." + lineSeparator); //$NON-NLS-1$
					return sb.toString();
				}
				rowIndex++;
			}
		}
		return sb.toString();
	}

	/**
	 * Get a human readable string representing a classloader.
	 *
	 * @param classLoader
	 *            the classloader to get a string for
	 * @return a human readable string representing the classloader
	 */
	public static String getHumanReadable(IMCClassLoader classLoader) {
		return classLoader == null ? null
				: classLoader.getType() + (classLoader.getName() != null && !classLoader.getName().isEmpty()
						? " (\"" + classLoader.getName() + "\")" : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
