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

import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCType;

/**
 * Base implementation of the {@link IMCMethod} interface.
 * <p>
 * Methods in this class should not be overridden. If you want to override anything, then implement
 * the {@link IMCMethod} interface instead and optionally delegate calls to this class.
 * <p>
 * Please do not add utility methods to this class. Use the helper class {@link MethodToolkit} if
 * you want to do common utility stuff.
 */
// FIXME: Move MC* classes and related toolkits to a separate package?
public class MCMethod implements IMCMethod {
	private final IMCType m_type;
	private final String m_methodName;
	private final String m_formalDescriptor;
	private final Integer m_modifier;
	private final Boolean m_isNative;

	/**
	 * Create a new instance.
	 * 
	 * @param type
	 *            the class that this method is declared in
	 * @param methodName
	 *            the method name
	 * @param formalDescriptor
	 *            the formal descriptor, see {@link IMCMethod#getFormalDescriptor()}
	 * @param modifier
	 *            method modifier bit pattern, see {@link IMCMethod#getModifier()}
	 * @param isNative
	 *            whether the method is native, see {@link IMCMethod#isNative()}
	 */
	public MCMethod(IMCType type, String methodName, String formalDescriptor, Integer modifier, Boolean isNative) {
		m_type = type == null ? MCType.UNKNOWN : type;
		m_methodName = methodName;
		m_formalDescriptor = formalDescriptor;
		m_modifier = modifier;
		m_isNative = isNative;
	}

	@Override
	public final IMCType getType() {
		return m_type;
	}

	@Override
	public final String getMethodName() {
		return m_methodName;
	}

	@Override
	public final String getFormalDescriptor() {
		return m_formalDescriptor;
	}

	@Override
	public final Integer getModifier() {
		return m_modifier;
	}

	@Override
	public final Boolean isNative() {
		return m_isNative;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((m_formalDescriptor == null) ? 0 : m_formalDescriptor.hashCode());
		result = prime * result + ((m_methodName == null) ? 0 : m_methodName.hashCode());
		result = prime * result + ((m_type == null) ? 0 : m_type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		MCMethod other = (MCMethod) obj;
		if (m_formalDescriptor == null) {
			if (other.m_formalDescriptor != null) {
				return false;
			}
		} else if (!m_formalDescriptor.equals(other.m_formalDescriptor)) {
			return false;
		}
		if (m_methodName == null) {
			if (other.m_methodName != null) {
				return false;
			}
		} else if (!m_methodName.equals(other.m_methodName)) {
			return false;
		}
		if (m_type == null) {
			if (other.m_type != null) {
				return false;
			}
		} else if (!m_type.equals(other.m_type)) {
			return false;
		}
		return true;
	}
}
