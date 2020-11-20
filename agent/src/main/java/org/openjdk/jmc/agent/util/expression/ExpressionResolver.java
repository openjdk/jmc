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
package org.openjdk.jmc.agent.util.expression;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.openjdk.jmc.agent.util.AccessUtils;

/* 
	Expression // a subset of Java primary expression (without array accesses)
		-> this
		 | TypeName . this
		 | FieldAccess

	TypeName
		-> TypeIdentifier
		 | PackageOrTypeName . TypeIdentifier

	PackageOrTypeName
		-> identifier
		| PackageOrTypeName . identifier

	TypeIdentifier
		 -> identifier

	FieldAccess
		-> Expression . identifier
		 | super . identifier
		 | TypeName . super . identifier
		 | FieldName

	FieldName
		 -> identifier

	identifier // terminal symbols
		 -> [A-z_]+[A-z0-9_]*
	*/
public class ExpressionResolver {
	private final Class<?> caller;
	private final String expression;

	private List<String> tokens = null;
	private Iterator<String> iterator = null;
	private ReferenceChain referenceChain = null;

	private ExpressionResolver(Class<?> caller, String expression) {
		this.caller = caller;
		this.expression = expression;
	}

	public static ReferenceChain solve(Class<?> caller, String expression) throws IllegalSyntaxException {
		ExpressionResolver resolver = new ExpressionResolver(caller, expression);
		resolver.tokens = new LinkedList<>(Arrays.asList(expression.split("\\.")));
		resolver.iterator = resolver.tokens.iterator();
		resolver.referenceChain = new ReferenceChain(caller);

		resolver.enterStartState();

		return resolver.referenceChain;
	}

	@SuppressWarnings("deprecation")
	private void enterStartState() throws IllegalSyntaxException {
		if (!iterator.hasNext()) {
			enterIllegalState(
					"Unexpected end of input: expects 'this', 'super', a field name, a class name, a package name, or a package name fragment");
		}

		String token = iterator.next(); // first identifier

		// "this"
		if (tryEnterThisState(caller, token)) {
			return;
		}

		// "super"
		if (tryEnterSuperState(caller, token)) {
			return;
		}

		// local/inherited field reference
		if (tryEnterFieldReferenceState(caller, token, false)) { // assuming accessing from a non-static context 
			return;
		}

		// nested field reference
		if (tryEntryNestedFieldReferenceState(token)) { // static class? 
			return;
		}

		// outer class reference
		if (tryEnterOuterClassState(token)) {
			return;
		}

		// inner class reference
		if (tryEnterInnerClassState(caller, token)) {
			return;
		}

		// CallerClass
		if (tryEnterSameClassState(token)) {
			return;
		}

		// ClassWithInTheSamePackage
		if (tryEnterClassState(token)) {
			return;
		}

		// com.full.qualified.pkg.ClassName
		if (tryEnterPackageState(expression)) {
			return;
		}

		// partially.qualified.pkg.ClassName
		if (tryEnterPackageState(caller.getPackage(), expression)) {
			return;
		}

		// eg. Object => java.lang.Object, Integer => java.lang.Integer
		if (tryEnterPackageState(Package.getPackage("java.lang"), expression)) {
			return;
		}

		enterIllegalState(String.format("Unrecognized symbol '%s'", token));
	}

	private boolean tryEnterThisState(Class<?> enclosingClass, String thisLiteral) throws IllegalSyntaxException {
		if (!"this".equals(thisLiteral)) {
			return false;
		}

		enterThisState(enclosingClass);
		return true;
	}

	// "^this" or "Qualified.this" expression (casting to an enclosing class)
	private void enterThisState(Class<?> enclosingClass) throws IllegalSyntaxException {
		// cast to outer class instance only when accessing non-static fields
		referenceChain.append(new ReferenceChainElement.ThisReference(caller));
		if (!caller.equals(enclosingClass)) {
			try {
				referenceChain.append(new ReferenceChainElement.QualifiedThisReference(caller, enclosingClass));
			} catch (IllegalArgumentException e) {
				enterIllegalState(e);
			}
		}

		if (!iterator.hasNext()) { // accepted state
			return;
		}
		String token = iterator.next();

		// this.prop
		if (tryEnterFieldReferenceState(enclosingClass, token, false)) {
			return;
		}

		enterIllegalState(String.format("Unrecognized symbol '%s'", token));
	}

	private boolean tryEnterSuperState(Class<?> targetClass, String superLiteral) throws IllegalSyntaxException {
		if (!"super".equals(superLiteral)) {
			return false;
		}

		enterSuperState(targetClass);
		return true;
	}

	// "^super" or "Qualified.super" expression
	private void enterSuperState(Class<?> enclosingClass) throws IllegalSyntaxException {
		referenceChain.append(new ReferenceChainElement.ThisReference(caller));
		if (!caller.equals(enclosingClass)) {
			try {
				referenceChain.append(new ReferenceChainElement.QualifiedThisReference(caller, enclosingClass));
			} catch (IllegalArgumentException e) {
				enterIllegalState(e);
			}
		}

		Class<?> superClass = enclosingClass.getSuperclass();
		if (superClass == null) { // almost would never happen, java.lang classes are not transformable
			enterIllegalState(String.format("'%s' has no super class", enclosingClass.getName()));
		}

		if (!iterator.hasNext()) { // rejected state
			enterIllegalState("unexpected end of input");
		}
		String token = iterator.next();

		// super.prop
		if (tryEnterFieldReferenceState(superClass, token, false)) {
			return;
		}

		enterIllegalState(String.format("Unrecognized symbol '%s'", token));
	}

	private boolean tryEnterFieldReferenceState(Class<?> memberingClass, String fieldName, boolean fromStaticContext)
			throws IllegalSyntaxException {
		try {
			Field field = AccessUtils.getFieldOnHierarchy(memberingClass, fieldName);
			enterFieldReferenceState(memberingClass, field, fromStaticContext);
			return true;
		} catch (NoSuchFieldException e) {
			return false;
		}
	}

	private void enterFieldReferenceState(Class<?> memberingClass, Field field, boolean fromStaticContext)
			throws IllegalSyntaxException {
		if (fromStaticContext && !Modifier.isStatic(field.getModifiers())) {
			enterIllegalState(
					String.format("Non-static field '%s' cannot be referenced from a static context", field.getName()));
		}

		if (!AccessUtils.isAccessible(memberingClass, field, caller)) {
			String access;
			if (Modifier.isPrivate(field.getModifiers())) {
				access = "private";
			} else if (Modifier.isProtected(field.getModifiers())) {
				access = "protected";
			} else {
				access = "package-private";
			}

			enterIllegalState(String.format("'%s' has %s access in '%s'", field.getName(), access,
					field.getDeclaringClass().getName()));
		}

		if (!caller.equals(memberingClass) && Modifier.isPrivate(field.getModifiers())
				&& AccessUtils.areNestMates(caller, memberingClass)) {
			enterIllegalState(
					new UnsupportedOperationException("Private member access between nestmates is not supported"));
		}

		referenceChain.append(new ReferenceChainElement.FieldReference(memberingClass, field));

		if (!iterator.hasNext()) { // accepted state
			return;
		}
		String token = iterator.next();

		// prop.prop2
		if (tryEnterFieldReferenceState(field.getType(), token, false)) {
			return;
		}

		enterIllegalState(String.format("Unrecognized symbol '%s'", token));
	}

	private boolean tryEntryNestedFieldReferenceState(String fieldName) throws IllegalSyntaxException {
		Class<?> enclosing = caller.getEnclosingClass();
		while (enclosing != null) {
			try {
				Field field = AccessUtils.getFieldOnHierarchy(enclosing, fieldName);
				enterNestedFieldReferenceState(enclosing, field);
				return true;
			} catch (NoSuchFieldException e) {
				enclosing = enclosing.getEnclosingClass();
			}
		}

		return false;
	}

	private void enterNestedFieldReferenceState(Class<?> enclosingClass, Field field) throws IllegalSyntaxException {
		if (!Modifier.isStatic(field.getModifiers())) {
			Class<?> c = caller.getEnclosingClass(); // the inner class is always static if it has a static method
			// check there is no static class in between, before reaching the enclosing class
			while (!c.equals(enclosingClass)) {
				if (Modifier.isStatic(c.getModifiers())) {
					enterIllegalState(String.format("Non-static field '%s' cannot be referenced from a static context",
							field.getName()));
				}
				c = c.getEnclosingClass();
			}
		}

		// this is syntactically allowed, but we don't support it for now
		if (Modifier.isPrivate(field.getModifiers())) {
			enterIllegalState(
					new UnsupportedOperationException("Private member access between nestmates is not supported"));
		}

		if (!Modifier.isStatic(field.getModifiers())) {
			// cast to outer class instance only when accessing non-static fields
			referenceChain.append(new ReferenceChainElement.ThisReference(caller));
			try {
				referenceChain.append(new ReferenceChainElement.QualifiedThisReference(caller, enclosingClass));
			} catch (IllegalArgumentException e) {
				enterIllegalState(e);
			}
		}

		referenceChain.append(new ReferenceChainElement.FieldReference(enclosingClass, field));

		if (!iterator.hasNext()) { // accepted state
			return;
		}
		String token = iterator.next();

		// nestedProp.prop
		if (tryEnterFieldReferenceState(field.getType(), token, false)) {
			return;
		}

		enterIllegalState(String.format("Unrecognized symbol '%s'", token));
	}

	private boolean tryEnterOuterClassState(String simpleClassName) throws IllegalSyntaxException {
		Class<?> enclosing = caller.getEnclosingClass();
		while (enclosing != null) {
			if (enclosing.getSimpleName().equals(simpleClassName)) {
				enterOuterClassState(enclosing);
				return true;
			}

			enclosing = enclosing.getEnclosingClass();
		}

		return false;
	}

	private boolean tryEnterOuterClassState(Package pkg, String className) throws IllegalSyntaxException {
		String fqcn = pkg.getName().isEmpty() ? className : pkg.getName() + "." + className;
		try {
			Class<?> clazz = caller.getClassLoader().loadClass(fqcn);
			Class<?> enclosing = caller.getEnclosingClass();
			while (enclosing != null) {
				if (enclosing.equals(clazz)) {
					enterOuterClassState(enclosing);
					return true;
				}

				enclosing = enclosing.getEnclosingClass();
			}
		} catch (ClassNotFoundException e) {
			// no op
		}

		return false;
	}

	private boolean tryEnterOuterClassState(Class<?> currentClass, String simpleClassName)
			throws IllegalSyntaxException {
		Class<?> clazz = null;
		for (Class<?> c : currentClass.getDeclaredClasses()) {
			if (c.getSimpleName().equals(simpleClassName)) {
				clazz = c;
				break;
			}
		}

		Class<?> enclosing = caller.getEnclosingClass();
		while (enclosing != null) {
			if (enclosing.equals(clazz)) {
				enterOuterClassState(enclosing);
				return true;
			}

			enclosing = enclosing.getEnclosingClass();
		}

		return false;
	}

	private void enterOuterClassState(Class<?> targetClass) throws IllegalSyntaxException {
		// static context
		if (!iterator.hasNext()) { // rejected state
			enterIllegalState(
					"Unexpected end of input: expects 'this', 'super', a static field name, or an inner class name");
		}
		String token = iterator.next();

		// OuterClass.this
		if (tryEnterThisState(targetClass, token)) {
			return;
		}

		// OuterClass.super
		if (tryEnterSuperState(targetClass, token)) {
			return;
		}

		// OuterClass.STATIC_PROP
		if (tryEnterFieldReferenceState(targetClass, token, true)) {
			return;
		}

		// OuterClass.ThisClass
		if (tryEnterSameClassState(targetClass, token)) {
			return;
		}

		// OuterMostClass.OuterClass
		if (tryEnterOuterClassState(targetClass, token)) {
			return;
		}

		// OuterClass.OtherClass
		if (tryEnterNestMateClass(targetClass, token)) {
			return;
		}

		enterIllegalState(String.format("Unrecognized symbol '%s'", token));
	}

	private boolean tryEnterInnerClassState(Class<?> currentClass, String simpleClassName)
			throws IllegalSyntaxException {
		for (Class<?> innerClass : currentClass.getDeclaredClasses()) {
			if (innerClass.getSimpleName().equals(simpleClassName)) {
				enterInnerClassState(innerClass);
				return true;
			}
		}

		return false;
	}

	private void enterInnerClassState(Class<?> targetClass) throws IllegalSyntaxException {
		// static context
		if (!iterator.hasNext()) { // rejected state
			enterIllegalState("Unexpected end of input: expects a static field name or an inner class name");
		}
		String token = iterator.next();

		// InnerClass.STATIC_PROP
		if (tryEnterFieldReferenceState(targetClass, token, true)) {
			return;
		}

		// InnerClass.InnerMoreClass
		if (tryEnterInnerClassState(targetClass, token)) {
			return;
		}

		enterIllegalState(String.format("Unrecognized symbol '%s'", token));
	}

	// target class is not a inner or outer class of the caller class, but is a classmate
	private boolean tryEnterNestMateClass(Class<?> currentClass, String simpleClassName) throws IllegalSyntaxException {
		Class<?> clazz = null;
		for (Class<?> c : currentClass.getDeclaredClasses()) {
			if (c.getSimpleName().equals(simpleClassName)) {
				clazz = c;
				break;
			}
		}

		if (clazz == null) {
			return false;
		}

		if (!AccessUtils.areNestMates(clazz, caller)) {
			return false;
		}

		// check caller is not an outer class of clazz  
		Class<?> enclosing = clazz;
		while (enclosing != null) {
			if (caller.equals(enclosing)) {
				return false;
			}
			enclosing = enclosing.getEnclosingClass();
		}

		// check clazz if not an outer class of caller
		enclosing = caller;
		while (enclosing != null) {
			if (clazz.equals(enclosing)) {
				return false;
			}
			enclosing = enclosing.getEnclosingClass();
		}

		enterNestMateClass(clazz);
		return true;
	}

	private void enterNestMateClass(Class<?> targetClass) throws IllegalSyntaxException {
		// static context
		if (!iterator.hasNext()) { // rejected state
			enterIllegalState("Unexpected end of input: expects a static field name or an inner class name");
		}
		String token = iterator.next();

		// NestMateClass.STATIC_PROP
		if (tryEnterFieldReferenceState(targetClass, token, false)) {
			return;
		}

		// NestMateClass.NestMatesInnerClass
		if (tryEnterNestMateClass(targetClass, token)) {
			return;
		}

		enterIllegalState(String.format("Unrecognized symbol '%s'", token));
	}

	private boolean tryEnterSameClassState(String simpleClassName) throws IllegalSyntaxException {
		if (caller.getSimpleName().equals(simpleClassName)) {
			enterSameClassState();
			return true;
		}

		return false;
	}

	private boolean tryEnterSameClassState(Package pkg, String simpleClassName) throws IllegalSyntaxException {
		String fqcn = pkg.getName().isEmpty() ? simpleClassName : pkg.getName() + "." + simpleClassName;
		if (caller.getName().equals(fqcn)) {
			enterSameClassState();
			return true;
		}

		return false;
	}

	private boolean tryEnterSameClassState(Class<?> currentClass, String simpleClassName)
			throws IllegalSyntaxException {
		Class<?> clazz = null;
		for (Class<?> c : currentClass.getDeclaredClasses()) {
			if (c.getSimpleName().equals(simpleClassName)) {
				clazz = c;
				break;
			}
		}

		if (caller.equals(clazz)) {
			enterSameClassState();
			return true;
		}

		return false;
	}

	private void enterSameClassState() throws IllegalSyntaxException {
		// static context
		if (!iterator.hasNext()) { // rejected state
			enterIllegalState("Unexpected end of input: expects a static field name or an inner class name");
		}
		String token = iterator.next();

		// CallerClass.this => this
		if (tryEnterThisState(caller, token)) {
			return;
		}

		// CallerClass.super => super
		if (tryEnterSuperState(caller, token)) {
			return;
		}

		// CallerClass.STATIC_PROP
		if (tryEnterFieldReferenceState(caller, token, true)) {
			return;
		}

		if (tryEnterInnerClassState(caller, token)) {
			return;
		}

		enterIllegalState(String.format("Unrecognized symbol '%s'", token));
	}

	private boolean tryEnterClassState(String simpleClassName) throws IllegalSyntaxException {
		return tryEnterClassState(caller.getPackage(), simpleClassName);
	}

	private boolean tryEnterClassState(Class<?> currentClass, String simpleClassName) throws IllegalSyntaxException {
		for (Class<?> c : currentClass.getDeclaredClasses()) {
			if (c.getSimpleName().equals(simpleClassName)) {
				enterClassState(c);
				return true;
			}
		}

		return false;
	}

	private boolean tryEnterClassState(Package pkg, String simpleClassName) throws IllegalSyntaxException {
		String fqcn = pkg.getName().isEmpty() ? simpleClassName : pkg.getName() + "." + simpleClassName;

		try {
			Class<?> c = caller.getClassLoader().loadClass(fqcn);
			enterClassState(c);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	private void enterClassState(Class<?> targetClass) throws IllegalSyntaxException {
		// static context
		if (!iterator.hasNext()) { // rejected state
			enterIllegalState("Unexpected end of input: expects a static field name or an inner class name");
		}
		String token = iterator.next();

		// ClassName.STATIC_PROP
		if (tryEnterFieldReferenceState(targetClass, token, true)) {
			return;
		}

		// ClassName.InnerClass
		if (tryEnterClassState(targetClass, token)) {
			return;
		}

		enterIllegalState(String.format("Unrecognized symbol '%s'", token));
	}

	// Full qualified package named prefixed expression
	private boolean tryEnterPackageState(String fqpnPrefixedExpression) throws IllegalSyntaxException {
		// ClassLoader.getPackage(String) or ClassLoader.getPackages() is not reliable when no class from that package is yet loaded
		int stop = 0;
		Class<?> clazz = null;
		while (stop < fqpnPrefixedExpression.length()) {
			stop = fqpnPrefixedExpression.indexOf('.', stop + 1);
			if (stop == -1) {
				break;
			}

			String fqcn = fqpnPrefixedExpression.substring(0, stop);
			try {
				clazz = caller.getClassLoader().loadClass(fqcn);
				break;
			} catch (ClassNotFoundException e) {
				// no op
			}
		}

		if (clazz == null) {
			return false;
		}

		Package pkg = clazz.getPackage();

		tokens = new LinkedList<>(Arrays.asList(fqpnPrefixedExpression.split("\\.")));
		iterator = tokens.iterator();
		int length = pkg.getName().split("\\.").length;
		for (int i = 0; i < length; i++) {
			iterator.next();
		}

		enterPackageState(pkg);
		return true;
	}

	// Partially qualified package named prefixed expression
	private boolean tryEnterPackageState(Package pkg, String pqpnPrefixedExpression) throws IllegalSyntaxException {
		String pkgPrefix = pkg.getName().isEmpty() ? "" : pkg.getName() + ".";
		return tryEnterPackageState(pkgPrefix + pqpnPrefixedExpression);
	}

	private void enterPackageState(Package pkg) throws IllegalSyntaxException {
		if (!iterator.hasNext()) { // rejected state
			enterIllegalState("Unexpected end of input: expects a class name");
		}
		String token = iterator.next();

		if (tryEnterSameClassState(pkg, token)) {
			return;
		}

		if (tryEnterOuterClassState(pkg, token)) {
			return;
		}

		if (tryEnterClassState(pkg, token)) {
			return;
		}

		enterIllegalState(String.format("Unrecognized symbol '%s'", token));
	}

	private void enterIllegalState(String msg) throws IllegalSyntaxException {
		throw new IllegalSyntaxException(msg);
	}

	private void enterIllegalState(Throwable throwable) throws IllegalSyntaxException {
		throw new IllegalSyntaxException(throwable);
	}
}
