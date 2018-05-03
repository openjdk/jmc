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
package org.openjdk.jmc.ide.jdt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;

/**
 * Toolkit class for finding java source elements.
 */
// FIXME: Can't handle anonymous classes and static initializers.
public class JumpToSourceToolkit {
	static IMethod findMethod(IType type, String methodName, String descriptor) throws JavaModelException {
		if (methodName == null) {
			return null;
		}
		if (descriptor != null) {
			if (methodName.startsWith("<init>")) //$NON-NLS-1$
			{
				methodName = type.getElementName();
			}
			String[] params = Signature.getParameterTypes(descriptor);
			IMethod method = type.getMethod(methodName, params);
			if (method != null && method.exists()) {
				return method;

			}
		}
		IMethod[] methods = type.getMethods();
		for (IMethod method : methods) {
			if (methodName.equals(method.getElementName())) {
				return method;
			}
		}
		return null;
	}

	static List<IMember> findMethods(List<IType> types, String methodName, String descriptor)
			throws JavaModelException {
		ArrayList<IMember> results = new ArrayList<>();

		for (IType type : types) {
			IMethod method = findMethod(type, methodName, descriptor);
			if (method != null) {
				results.add(method);
			} else {
				results.add(type);
			}
		}
		return results;
	}

	private static IType[] findSourceFileTypesInWorkspace(String declaringType, IProgressMonitor monitor)
			throws CoreException {
		OpenSourceRequestor collector = new OpenSourceRequestor();

		SearchEngine engine = new SearchEngine();
		SearchParticipant searchParticipants[] = new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()};
		SearchPattern pattern = SearchPattern.createPattern(declaringType, IJavaSearchConstants.TYPE,
				IJavaSearchConstants.DECLARATIONS, SearchPattern.R_EXACT_MATCH);
		engine.search(pattern, searchParticipants, SearchEngine.createWorkspaceScope(), collector, monitor);

		return collector.getResults();
	}

	public static List<IMember> findOpenableMethodElements(
		String declaringTypeName, String methodName, String descriptor, String nestedTypes, IProgressMonitor monitor)
			throws CoreException {
		return findMethods(findTypes(findSourceFileTypesInWorkspace(declaringTypeName, monitor), nestedTypes),
				methodName, descriptor);
	}

	public static List<IType> findOpenableTypeElements(
		String declaringTypeName, String nestedTypes, IProgressMonitor monitor) throws CoreException {
		return findTypes(findSourceFileTypesInWorkspace(declaringTypeName, monitor), nestedTypes);
	}

	/**
	 * Finds the nested types in a array of IJavaElements. If the element doesn't have nested
	 * classes the type is returned.
	 *
	 * @param elements
	 * @param nestedTypes
	 * @return
	 */

	public static List<IType> findTypes(IType[] elements, String nestedTypes) {
		ArrayList<IType> list = new ArrayList<>(elements.length);
		for (IType element : elements) {
			IType type = findNestedType(element, nestedTypes);
			if (type != null) {
				list.add(type);
			}
		}
		return list;
	}

	/**
	 * Finds a nestedType in a type.
	 * <p>
	 * E.g., In the class name MainClass$InnerA$InnerB is InnerA$InnerB the nestedTypeString
	 *
	 * @param type
	 * @param nestedTypeString
	 * @return the type or null if not available
	 */
	public static IType findNestedType(IType type, String nestedTypeString) {
		if (nestedTypeString != null) {
			String nestedTypeName = getFirstNestedType(nestedTypeString);
			IType nestedType = type.getType(nestedTypeName);
			if (nestedType != null && nestedType.exists()) {
				return findNestedType(nestedType, getRemainingNestedTypes(nestedTypeString));
			}
		}
		return type;
	}

	/**
	 * Returns the first part of nestedType expression, or null if not available.
	 * <p>
	 * E.g.
	 * <ul>
	 * <li>classA$classB$classC => classA
	 * <li>classA$classB$classC => classA</li>
	 * <li>classD => classD</li>
	 * </ul>
	 *
	 * @param nestedType
	 * @return
	 */
	public static String getFirstNestedType(String nestedTypeString) {
		int index = nestedTypeString.indexOf('$');
		if (index != -1) {
			return nestedTypeString.substring(0, index);
		} else {
			return nestedTypeString;
		}
	}

	/**
	 * Returns the remaining part of nestedType expression, or null if not available.
	 * <p>
	 * E.g.
	 * <ul>
	 * <li>classA@classB@classC => classB@classC
	 * <li>classD => null
	 * </ul>
	 *
	 * @param nestedType
	 * @return
	 */
	public static String getRemainingNestedTypes(String nestedTypeString) {
		int index = nestedTypeString.indexOf('$');
		if (index != -1 && index + 1 < nestedTypeString.length()) {
			return nestedTypeString.substring(index + 1);
		} else {
			return null;
		}
	}
}
