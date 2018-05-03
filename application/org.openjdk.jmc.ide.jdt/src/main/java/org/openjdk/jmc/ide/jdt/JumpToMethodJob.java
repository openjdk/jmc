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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;

import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.util.MethodToolkit;

/**
 * Job looking for a Java method.
 */
public class JumpToMethodJob extends JumpToSourceJob {
	protected final String m_methodName;
	protected final String m_descriptor;

	public static Job createJob(IMCMethod method, Integer lineNumber) {
		String className = method.getType().getTypeName();
		String topLevelType = MethodToolkit.topLevelType(className);
		String nestedTypes = MethodToolkit.nestedTypes(className);
		String qualifiedClassName = MethodToolkit.formatQualifiedName(method.getType().getPackage(), topLevelType);
		String meth = method.getMethodName();
		String desc = method.getFormalDescriptor();
		return new JumpToMethodJob(qualifiedClassName, meth, desc, nestedTypes, lineNumber);
	}

	public JumpToMethodJob(String className, String methodName, String descriptor, String nestedTypes,
			Integer lineNumber) {
		super(className, nestedTypes, lineNumber);
		m_methodName = methodName;
		m_descriptor = descriptor;
	}

	@Override
	protected Map<IType, IMember> createTypeToJavaElementMap(IProgressMonitor monitor) throws CoreException {
		List<IMember> methods = JumpToSourceToolkit.findOpenableMethodElements(m_className, m_methodName, m_descriptor,
				m_nestedTypes, monitor);
		HashMap<IType, IMember> map = new HashMap<>();
		for (IMember element : methods) {
			if (element instanceof IType) {
				map.put((IType) element, element);
			} else {
				map.put(element.getDeclaringType(), element);
			}
		}
		return map;
	}

	@Override
	protected String getTitle() {
		return "Open type for method: " + m_methodName; //$NON-NLS-1$
	}
}
