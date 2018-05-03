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
package org.openjdk.jmc.flightrecorder.internal.parser.v0.factories;

import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.MCMethod;
import org.openjdk.jmc.flightrecorder.internal.parser.v0.model.ValueDescriptor;
import org.openjdk.jmc.flightrecorder.internal.util.CanonicalConstantMap;

/**
 * Factory that creates a {@link IMCMethod} from the method pool.
 */
final class MethodFactory implements IPoolFactory<IMCMethod> {
	private final int m_methodClass;
	private final int m_methodName;
	private final int m_methodSignatureIndex;
	private final int m_methodModifiers;

	private final CanonicalConstantMap<IMCMethod> methodMap;

	public MethodFactory(ValueDescriptor[] descriptors, CanonicalConstantMap<IMCMethod> methodMap) {
		this.methodMap = methodMap;
		m_methodClass = ValueDescriptor.getIndex(descriptors, "class"); //$NON-NLS-1$
		m_methodName = ValueDescriptor.getIndex(descriptors, "name"); //$NON-NLS-1$
		m_methodSignatureIndex = ValueDescriptor.getIndex(descriptors, "signature"); //$NON-NLS-1$
		m_methodModifiers = ValueDescriptor.getIndex(descriptors, "modifiers"); //$NON-NLS-1$
	}

	@Override
	public IMCMethod createObject(long identifier, Object source) {
		Object o[] = (Object[]) source;
		if (o != null) {
			return methodMap.canonicalize(createMethod(o));
		}
		return null;
	}

	private IMCMethod createMethod(Object[] o) {
		String formalDesc = null;
		String methodName = null;
		Integer modifier = null;
		IMCType type = null;
		if (m_methodSignatureIndex != -1) {
			formalDesc = (String) o[m_methodSignatureIndex];
		}
		if (m_methodName != -1) {
			methodName = (String) o[m_methodName];
		}
		if (m_methodModifiers != -1) {
			Number s = (Number) o[m_methodModifiers];
			modifier = (s != null ? s.intValue() : null);
		}
		if (m_methodClass != -1) {
			type = (IMCType) o[m_methodClass];
		}
		return new MCMethod(type, methodName, formalDesc, modifier, null);
	}

	@Override
	public ContentType<IMCMethod> getContentType() {
		return UnitLookup.METHOD;
	}
}
