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
package org.openjdk.jmc.agent.util;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.logging.Level;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openjdk.jmc.agent.Agent;
import org.openjdk.jmc.agent.generated_events.Dummy;

/**
 * Helper methods for doing transforms.
 */
public final class TypeUtils {
	private static final String NULL_REFERENCE_STRING = "null"; //$NON-NLS-1$
	/**
	 * The internal name of this class.
	 */
	public static final String INAME = Type.getInternalName(TypeUtils.class);
	public static final Type OBJECT_TYPE = Type.getObjectType("java/lang/Object"); //$NON-NLS-1$
	public static final Type OBJECT_ARRAY_TYPE = Type.getObjectType("[Ljava/lang/Object;"); //$NON-NLS-1$
	public static final Type STRING_TYPE = Type.getType("Ljava/lang/String;"); //$NON-NLS-1$

	public static final Object STRING_INTERNAL_NAME = "java/lang/String"; //$NON-NLS-1$

	/**
	 * The file extension for java source files (.java).
	 */
	public static final String JAVA_FILE_EXTENSION = ".java"; //$NON-NLS-1$

	private TypeUtils() {
		throw new UnsupportedOperationException("Toolkit!"); //$NON-NLS-1$
	}

	public static Object box(byte val) {
		return val;
	}

	public static Object box(short val) {
		return val;
	}

	public static Object box(char val) {
		return val;
	}

	public static Object box(int val) {
		return val;
	}

	public static Object box(long val) {
		return val;
	}

	public static Object box(float val) {
		return val;
	}

	public static Object box(double val) {
		return val;
	}

	public static String toString(Object o) {
		if (o == null) {
			return NULL_REFERENCE_STRING;
		}
		if (o.getClass().isArray()) {
			return toString(o, Array.getLength(o));
		}
		return String.valueOf(o);
	}

	public static Class<?> defineClass(
		String eventClassName, byte[] eventClass, int i, int length, ClassLoader definingClassLoader,
		ProtectionDomain protectionDomain) {
		String version = System.getProperty("java.version");
		if (Integer.parseInt(version.substring(0, version.indexOf("."))) < 9) {
			return defineClassWithReflection(eventClassName, eventClass, i, length, definingClassLoader, protectionDomain);
		}

		return defineClassWithLookup(eventClassName, eventClass, i, length, definingClassLoader, protectionDomain);
	}

	private static Class<?> defineClassWithLookup(
			String eventClassName, byte[] eventClass, int i, int length, ClassLoader definingClassLoader,
			ProtectionDomain protectionDomain) {
		try {
			Method privateLookupIn = MethodHandles.class.getMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class);
			MethodHandles.Lookup lookup = (MethodHandles.Lookup) privateLookupIn.invoke(null, Dummy.class, MethodHandles.lookup());
			byte[] bytes = Arrays.copyOfRange(eventClass, i, i + length);
			Method defineClass = MethodHandles.Lookup.class.getDeclaredMethod("defineClass", byte[].class);
			return (Class<?>) defineClass.invoke(lookup, (Object) bytes);
		} catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
			Agent.getLogger().log(Level.SEVERE, "Failed to dynamically define the class " + eventClassName, e); //$NON-NLS-1$
		}

		return null;
	}

	private static Class<?> defineClassWithReflection(
			String eventClassName, byte[] eventClass, int i, int length, ClassLoader definingClassLoader,
			ProtectionDomain protectionDomain) {
		try {
			Method defineClass = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class, ProtectionDomain.class);
			defineClass.setAccessible(true);
			return (Class<?>) defineClass.invoke(definingClassLoader, eventClassName, eventClass, i, length, protectionDomain);
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			Agent.getLogger().log(Level.SEVERE, "Failed to dynamically define the class " + eventClassName, e); //$NON-NLS-1$
		}

		return null;
	}

	/**
	 * Ensure that the operand is on the stack before calling. If type is void, this is a noop, and
	 * depending on your use case you may instead want to push Opcodes.ACONST_NULL.
	 */
	public static void visitBox(MethodVisitor mv, Type type) {
		switch (type.getSort()) {
		case Type.VOID:
			break;
		case Type.BOOLEAN:
			emitBox(mv, "(Z)Ljava/lang/Object;"); //$NON-NLS-1$
			break;
		case Type.BYTE:
			emitBox(mv, "(B)Ljava/lang/Object;"); //$NON-NLS-1$
			break;
		case Type.CHAR:
			emitBox(mv, "(C)Ljava/lang/Object;"); //$NON-NLS-1$
			break;
		case Type.SHORT:
			emitBox(mv, "(S)Ljava/lang/Object;"); //$NON-NLS-1$
			break;
		case Type.INT:
			emitBox(mv, "(I)Ljava/lang/Object;"); //$NON-NLS-1$
			break;
		case Type.LONG:
			emitBox(mv, "(J)Ljava/lang/Object;"); //$NON-NLS-1$
			break;
		case Type.FLOAT:
			emitBox(mv, "(F)Ljava/lang/Object;"); //$NON-NLS-1$
			break;
		case Type.DOUBLE:
			emitBox(mv, "(D)Ljava/lang/Object;"); //$NON-NLS-1$
			break;
		}
	}

	public static boolean isValidJavaIdentifier(String identifier) {
		if (identifier == null || identifier.length() == 0) {
			return false;
		}

		if (!Character.isJavaIdentifierStart(identifier.charAt(0))) {
			return false;
		}

		for (int i = 1; i < identifier.length(); i++) {
			if (!Character.isJavaIdentifierPart(identifier.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	public static String deriveIdentifierPart(String str) {
		StringBuilder builder = new StringBuilder();

		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (Character.isJavaIdentifierPart(c)) {
				builder.append(c);
			}
		}
		builder.setCharAt(0, Character.toUpperCase(builder.toString().charAt(0)));
		return builder.toString();
	}

	public static String getPathPart(String fqcn) {
		int lastSlashIndex = fqcn.lastIndexOf('/');
		if (lastSlashIndex >= 0) {
			return fqcn.substring(0, lastSlashIndex + 1);
		}
		return fqcn;
	}

	public static String getNamePart(String fqcn) {
		int lastSlashIndex = fqcn.lastIndexOf('/');
		if (lastSlashIndex >= 0) {
			return fqcn.substring(lastSlashIndex + 1);
		}
		return fqcn;
	}

	public static void stringify(MethodVisitor mv) {
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, INAME, "toString", //$NON-NLS-1$
				"(Ljava/lang/Object;)Ljava/lang/String;", false); //$NON-NLS-1$
	}

	public static boolean shouldStringify(Type argumentType) {
		if (argumentType.getSort() == Type.ARRAY || argumentType.getSort() == Type.OBJECT) {
			return !argumentType.getInternalName().equals(STRING_INTERNAL_NAME);
		}
		return false;
	}

	/**
	 * Transforms a FQN in internal form, so that it can be used in e.g. formal descriptors.
	 *
	 * @param className
	 *            the fully qualified class name in internal form.
	 * @return the transformed class name.
	 */
	public static String parameterize(String className) {
		return "L" + className + ";"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Type agnostic array toString() which also handles primitive arrays.
	 */
	private static String toString(Object o, int length) {
		int iMax = length - 1;
		if (iMax == -1) {
			return "[]"; //$NON-NLS-1$
		}

		StringBuilder b = new StringBuilder();
		b.append('[');
		for (int i = 0;; i++) {
			b.append(Array.get(o, i));
			if (i == iMax) {
				return b.append(']').toString();
			}
			b.append(", "); //$NON-NLS-1$
		}
	}

	private static void emitBox(MethodVisitor mv, String desc) {
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, INAME, "box", desc, false); //$NON-NLS-1$
	}
}
