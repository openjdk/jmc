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
package org.openjdk.jmc.joverflow.util;

import java.util.HashMap;

import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.heap.model.JavaField;
import org.openjdk.jmc.joverflow.support.Constants;

/**
 * Various utility methods related to analyzed Java classes/objects.
 */
public class ClassUtils implements Constants {

	private static final HashMap<String, String> POPULAR_CLASS_SHORT_NAMES = new HashMap<>();
	private static final String[] POPULAR_PACKAGES = new String[] {"java.lang.", "java.util.", "java.util.concurrent.",
			"java.lang.ref."};

	/**
	 * Some fields may be called differently in different implementations of Java libraries. For
	 * example, ArrayList.elementData in the standard JDK vs. ArrayList.array in Android. Given a
	 * string like "elementData|array", this method returns the name of the field that exists in the
	 * given class in this heap dump.
	 */
	public static String getExactFieldName(String oneOrMoreFieldNames, JavaClass clazz) {
		int splitIdx = oneOrMoreFieldNames.indexOf('|');
		if (splitIdx != -1) {
			int startIdx = 0;
			while (true) {
				String fieldName = oneOrMoreFieldNames.substring(startIdx, splitIdx);
				if (clazz.getDeclaringClassForField(fieldName) != null) {
					return fieldName;
				}
				startIdx = splitIdx + 1;
				if (startIdx >= oneOrMoreFieldNames.length()) {
					break;
				}
				splitIdx = oneOrMoreFieldNames.indexOf('|', startIdx);
				if (splitIdx == -1) {
					splitIdx = oneOrMoreFieldNames.length();
				}
			}
			throw new RuntimeException(ClassUtils.getMessageForMissingField(clazz, oneOrMoreFieldNames));
		} else {
			if (clazz.getDeclaringClassForField(oneOrMoreFieldNames) == null) {
				throw new RuntimeException(ClassUtils.getMessageForMissingField(clazz, oneOrMoreFieldNames));
			}
			return oneOrMoreFieldNames;
		}
	}

	public static String getMessageForMissingField(JavaClass clazz, String fieldName) {
		JavaField fieldDescs[] = clazz.getFieldsForInstance();
		StringBuilder msg = new StringBuilder();
		msg.append(clazz.getName()).append(": field ").append(fieldName).append(" not found.\n");
		msg.append("Existing fields:\n");
		for (JavaField fieldDesc : fieldDescs) {
			msg.append(fieldDesc.getTypeId()).append(' ').append(fieldDesc.getName()).append('\n');
		}
		return msg.toString();
	}

	/**
	 * If the given class name is "popular" (it belongs to one of POPULAR_PACKAGES above), returns
	 * the short class name. Otherwise, returns the class name unchanged.
	 */
	public static String getShortNameForPopularClass(String className) {
		String shortName = POPULAR_CLASS_SHORT_NAMES.get(className);
		if (shortName != null) {
			return shortName;
		}

		for (String pkg : POPULAR_PACKAGES) {
			if (!className.startsWith(pkg)) {
				continue;
			}
			int lastDotIdx = className.lastIndexOf('.');
			if (lastDotIdx != pkg.length() - 1) {
				continue;
			}
			shortName = className.substring(lastDotIdx + 1);
			POPULAR_CLASS_SHORT_NAMES.put(className, shortName);
			return shortName;
		}

		return className;
	}

	public static boolean isAnonymousInnerClass(String className) {
		if (!Character.isDigit(className.charAt(className.length() - 1))) {
			return false;
		}
		int dollarIdx = className.lastIndexOf('$');
		if (dollarIdx == -1) {
			return false;
		}
		return (Character.isDigit(className.charAt(dollarIdx + 1)));
	}

	public static String arrayOf(String className) {
		return "[L" + className + ';';
	}
}
