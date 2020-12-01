/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.agent.impl;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;

import org.objectweb.asm.Type;
import org.openjdk.jmc.agent.Convertable;

public final class ResolvedConvertable extends AbstractConvertable implements Convertable {
	public static final String DEFAULT_CONVERTER_METHOD = "convert";
	private final Method converterMethod;

	public ResolvedConvertable(String converterDefinition, Class<?> typeToConvert) throws MalformedConverterException {
		super(converterDefinition);
		if (typeToConvert == null) {
			throw new MalformedConverterException("Type to convert cannot be null!");
		}
		String className = resolveClassName(converterDefinition);
		Class<?> tmpClass = null;
		try {
			if (converterDefinition != null) {
				tmpClass = Class.forName(className);
			}
		} catch (ClassNotFoundException e) {
			throw new MalformedConverterException("Converter must convert to an existing class!", e);
		}
		this.converterMethod = getConvertMethod(tmpClass, converterDefinition, typeToConvert);
	}

	public ResolvedConvertable(String converterDefinition, Type type) throws MalformedConverterException {
		this(converterDefinition, getClassFromType(type));
	}

	public Class<?> getConverterClass() {
		return converterMethod.getDeclaringClass();
	}

	public Method getConverterMethod() {
		return converterMethod;
	}

	private static Method getConvertMethod(Class<?> converterClass, String converterDefinition, Class<?> originalType)
			throws MalformedConverterException {
		String methodName = resolveMethodName(converterDefinition);

		if (converterClass == null) {
			return null;
		}
		for (Method m : converterClass.getDeclaredMethods()) {
			if (methodName.equals(m.getName())) {
				if (!isValidMethod(m)) {
					continue;
				}
				if (parameterIsAssignableType(m.getParameters()[0], originalType)) {
					return m;
				}
			}
		}
		throw new MalformedConverterException("Could not find the convert method to use in " + converterDefinition
				+ " to convert " + originalType.getName());
	}

	private static boolean isValidMethod(Method m) {
		return Modifier.isStatic(m.getModifiers())
				&& !(Modifier.isAbstract(m.getModifiers()) || Modifier.isInterface(m.getModifiers()))
				&& m.getParameterCount() == 1;
	}

	private static boolean parameterIsAssignableType(Parameter p, Class<?> originalType) {
		if (p.getType().isAssignableFrom(originalType)) {
			return true;
		}
		return false;
	}

	private static String resolveClassName(String converterDefinition) throws MalformedConverterException {
		if (!converterDefinition.contains("(")) {
			return converterDefinition;
		}
		int lastDotIndex = converterDefinition.lastIndexOf('.');
		if (lastDotIndex == -1) {
			throw new MalformedConverterException(
					"Converter with method declaration must contain method: " + converterDefinition);
		}
		return converterDefinition.substring(0, lastDotIndex);
	}

	private static String resolveMethodName(String converterDefinition) throws MalformedConverterException {
		if (!converterDefinition.contains("(")) {
			return DEFAULT_CONVERTER_METHOD;
		}
		int lastDotIndex = converterDefinition.lastIndexOf('.');
		if (lastDotIndex == -1) {
			throw new MalformedConverterException(
					"Converter with method declaration must contain method: " + converterDefinition);
		}
		int firstParenIndex = converterDefinition.lastIndexOf('(');
		if (firstParenIndex < lastDotIndex) {
			throw new MalformedConverterException("No dots in the formal descriptor allowed: " + converterDefinition);
		}
		return converterDefinition.substring(lastDotIndex + 1, firstParenIndex);
	}

	private static Class<?> getClassFromType(Type type) throws MalformedConverterException {
		try {
			return Class.forName(type.getClassName());
		} catch (ClassNotFoundException e) {
			throw new MalformedConverterException("The type to transform could not be found", e);
		}
	}

	@Override
	public String toString() {
		return "Resolved " + getConverterDefinition() + ":\nClass: " + getConverterClass() + "\nMethod: "
				+ getConverterMethod();
	}

}
