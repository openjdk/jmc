/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Queue;

/**
 * Helper methods for checking accessibility, implied as modifiers, from various contexts.
 */
public final class AccessUtils {
	private AccessUtils() {
		throw new UnsupportedOperationException("Toolkit!"); //$NON-NLS-1$
	}

	/**
	 * Like Class.getDeclaredField, but also gets fields declared by ancestors and interfaces.
	 *
	 * @param clazz
	 *            the class to lookup from
	 * @param name
	 *            the name of the field
	 * @return the {@code Field} object for the specified field in this class
	 * @throws NoSuchFieldException
	 *             if a field with the specified name is not found.
	 */
	public static Field getFieldOnHierarchy(Class<?> clazz, String name) throws NoSuchFieldException {
		Queue<Class<?>> q = new LinkedList<>();
		q.add(clazz);

		while (!q.isEmpty()) {
			Class<?> targetClass = q.remove();
			try {
				return targetClass.getDeclaredField(name);
			} catch (NoSuchFieldException e) {
				// ignore
			}

			q.addAll(Arrays.asList(targetClass.getInterfaces()));
			Class<?> superClass = targetClass.getSuperclass();
			if (superClass != null) {
				q.add(targetClass.getSuperclass());
			}
		}

		throw new NoSuchFieldException(String.format("cannot find field %s in class %s", name, clazz.getName()));
	}

	/**
	 * Checks whether a field can be accessed from a caller context.
	 *
	 * @param targetClass
	 *            the class being referenced
	 * @param field
	 *            the field being accessed
	 * @param currentClass
	 *            the caller class
	 * @return whether the field is accessible from given context
	 */
	public static boolean isAccessible(Class<?> targetClass, Field field, Class<?> currentClass) {
		int modifiers = field.getModifiers();

		Class<?> memberClass = field.getDeclaringClass();
		if (Modifier.isStatic(modifiers)) {
			targetClass = null;
		}

		return verifyMemberAccess(targetClass, memberClass, currentClass, modifiers);
	}

	/**
	 * Checks whether the field/method/inner class modifier allows access from a caller context
	 *
	 * @param targetClass
	 *            the class being referenced
	 * @param memberClass
	 *            the class declaring the field/method/inner class
	 * @param currentClass
	 *            the caller class
	 * @param modifiers
	 *            member access modifiers in bit flags as a integer
	 * @return
	 */
	public static boolean verifyMemberAccess(
		Class<?> targetClass, Class<?> memberClass, Class<?> currentClass, int modifiers) {
		if (currentClass == memberClass) {
			return true;
		}

		if (!verifyModuleAccess(memberClass, currentClass)) {
			return false;
		}

		boolean gotIsSameClassPackage = false;
		boolean isSameClassPackage = false;

		if (!Modifier.isPublic(getClassAccessFlags(memberClass))) {
			isSameClassPackage = isSameClassPackage(currentClass, memberClass);
			gotIsSameClassPackage = true;
			if (!isSameClassPackage) {
				return false;
			}
		}

		// At this point we know that currentClass can access memberClass.

		if (Modifier.isPublic(modifiers)) {
			return true;
		}

		// Check for nestmate access if member is private
		if (Modifier.isPrivate(modifiers)) {
			// Note: targetClass may be outside the nest, but that is okay
			//       as long as memberClass is in the nest.
			if (areNestMates(currentClass, memberClass)) {
				return true;
			}
		}

		boolean successSoFar = false;

		if (Modifier.isProtected(modifiers)) {
			// See if currentClass is a subclass of memberClass
			if (isSubclassOf(currentClass, memberClass)) {
				successSoFar = true;
			}
		}

		if (!successSoFar && !Modifier.isPrivate(modifiers)) {
			if (!gotIsSameClassPackage) {
				isSameClassPackage = isSameClassPackage(currentClass, memberClass);
				gotIsSameClassPackage = true;
			}

			if (isSameClassPackage) {
				successSoFar = true;
			}
		}

		if (!successSoFar) {
			return false;
		}

		// Additional test for protected instance members
		// and protected constructors: JLS 6.6.2
		if (targetClass != null && Modifier.isProtected(modifiers) && targetClass != currentClass) {
			if (!gotIsSameClassPackage) {
				isSameClassPackage = isSameClassPackage(currentClass, memberClass);
			}
			if (!isSameClassPackage) {
				return isSubclassOf(targetClass, currentClass);
			}
		}

		return true;
	}

	/**
	 * Check whether the module has the class exported for the caller to access. For Pre-9 Java
	 * runtime, this function always returns <code>true</code>.
	 *
	 * @param targetClass
	 *            the class being accessed
	 * @param callerClass
	 *            the caller class
	 * @return whether the class is accessible
	 */
	public static boolean verifyModuleAccess(Class<?> targetClass, Class<?> callerClass) {
		OptionalInt featureVersion = VersionUtils.getFeatureVersion();
		if (featureVersion.isPresent() && featureVersion.getAsInt() < 9) {
			return true; // There is no module for pre-java 9
		}

		Object targetModule;
		Object callerModule;
		try {
			Method getModuleMethod = Class.class.getDeclaredMethod("getModule");
			targetModule = getModuleMethod.invoke(targetClass);
			callerModule = getModuleMethod.invoke(callerClass);
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e); // this should not happen
		}

		if (targetModule == callerModule) {
			return true;
		}

		String pkg = getPackageName(targetClass);
		try {
			Method isExportedMethod = targetModule.getClass().getDeclaredMethod("isExported", String.class,
					Class.forName("java.lang.Module"));
			return (boolean) isExportedMethod.invoke(targetModule, pkg, callerModule);
		} catch (ClassNotFoundException
				| NoSuchMethodException
				| IllegalAccessException
				| InvocationTargetException e) {
			throw new RuntimeException(e); // this should not happen
		}
	}

	/**
	 * polyfill for <code>Class.getPackageName(Class<?>)</code> for pre-9 Java.
	 *
	 * @param clazz
	 *            the class to lookup the package name against
	 * @return the name of the package containing the class
	 */
	public static String getPackageName(Class<?> clazz) {
		return clazz.getPackage().getName();

	}

	/**
	 * Polyfill for <code>Reflection.getClassAccessFlags(Class<?>)</code> as
	 * <code>jdk.internal.reflect.Reflection</code> is not exported.
	 *
	 * @param c
	 *            the class being inspected
	 * @return the access flags written to the class file
	 */
	public static int getClassAccessFlags(Class<?> c) {
		return c.getModifiers();
	}

	/**
	 * Check whether the two classes exist in the same package
	 *
	 * @param lhs
	 *            the first class
	 * @param rhs
	 *            the second class
	 * @return whether the given classes exist in the same package
	 */
	public static boolean isSameClassPackage(Class<?> lhs, Class<?> rhs) {
		if (lhs.getClassLoader() != rhs.getClassLoader())
			return false;
		return getPackageName(lhs).equals(getPackageName(rhs));
	}

	/**
	 * Check whether a class is a subclass of the other
	 *
	 * @param queryClass
	 *            the subclass
	 * @param ofClass
	 *            the superclass
	 * @return whether it's a subclass-superclass relationship
	 */
	public static boolean isSubclassOf(Class<?> queryClass, Class<?> ofClass) {
		while (queryClass != null) {
			if (queryClass == ofClass) {
				return true;
			}
			queryClass = queryClass.getSuperclass();
		}
		return false;
	}

	/**
	 * Polyfill Class.getNestMembers() for pre-11 runtime. This function does not fully respect the
	 * definition of nesting from JVM's perspective. It's only used for validating access.
	 *
	 * @param clazz
	 *            the class to inspect against
	 * @return an array of all nest members
	 */
	public static Class<?>[] getNestMembers(Class<?> clazz) {
		List<Class<?>> classes = new ArrayList<>();
		classes.add(getNestHost(clazz));
		int i = 0;
		while (i < classes.size()) {
			classes.addAll(Arrays.asList(classes.get(i).getDeclaredClasses()));
			i++;
		}

		return classes.toArray(new Class[0]);
	}

	/**
	 * Polyfill Class.isNestMateOf() for pre-11 runtime. This function does not fully respect the
	 * definition of nesting from JVM's perspective. It's only used for validating access.
	 *
	 * @param lhs
	 *            the first class
	 * @param rhs
	 *            the second class
	 * @return whether the given classes are nestmates
	 */
	public static boolean areNestMates(Class<?> lhs, Class<?> rhs) {
		return getNestHost(lhs).equals(getNestHost(rhs));
	}

	/**
	 * Polyfill Class.getNestHost() for pre-11 runtime. This function does not fully respect the
	 * definition of nesting from JVM's perspective. It's only used for validating access.
	 *
	 * @param clazz
	 *            the class the inspect against
	 * @return the nesthost of the class
	 */
	public static Class<?> getNestHost(Class<?> clazz) {
		// array types, primitive types, and void belong to the nests consisting only of theme, and are the nest hosts.
		if (clazz.isArray()) {
			return clazz;
		}

		if (clazz.isPrimitive()) {
			return clazz;
		}

		if (Void.class.equals(clazz)) {
			return clazz;
		}

		while (true) {
			if (clazz.getEnclosingClass() == null) {
				return clazz;
			}

			clazz = clazz.getEnclosingClass();
		}
	}
}
