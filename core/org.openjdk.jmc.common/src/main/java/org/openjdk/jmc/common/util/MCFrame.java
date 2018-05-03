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

import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCMethod;

/**
 * Base implementation of the {@link IMCFrame} interface.
 */
// FIXME: Move MC* classes and related toolkits to a separate package?
public class MCFrame implements IMCFrame {
	private final IMCMethod m_method;
	private final Integer m_bci;
	private final Integer m_frameLineNumber;
	private final Type m_type;

	/**
	 * Create a new frame instance.
	 * 
	 * @param method
	 *            method for the frame, see {@link IMCFrame#getMethod()}
	 * @param bci
	 *            byte code index for the frame, see {@link IMCFrame#getBCI()}
	 * @param frameLineNumber
	 *            frame line number, see {@link IMCFrame#getFrameLineNumber()}
	 * @param type
	 *            frame compilation type
	 */
	public MCFrame(IMCMethod method, Integer bci, Integer frameLineNumber, Type type) {
		m_method = method == null ? new MCMethod(null, "", "()V;", null, false) : method; //$NON-NLS-1$ //$NON-NLS-2$
		m_bci = bci;
		m_frameLineNumber = frameLineNumber;
		m_type = type;
	}

	@Override
	final public Integer getBCI() {
		return m_bci;
	}

	@Override
	final public IMCMethod getMethod() {
		return m_method;
	}

	@Override
	final public Integer getFrameLineNumber() {
		return m_frameLineNumber;
	}

	@Override
	final public Type getType() {
		return m_type;
	}

	@Override
	public String toString() {
		return m_method + " " + m_type + " " + m_frameLineNumber + " " + m_type; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((m_bci == null) ? 0 : m_bci.hashCode());
		result = prime * result + ((m_frameLineNumber == null) ? 0 : m_frameLineNumber.hashCode());
		result = prime * result + ((m_method == null) ? 0 : m_method.hashCode());
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
		MCFrame other = (MCFrame) obj;
		if (m_bci == null) {
			if (other.m_bci != null) {
				return false;
			}
		} else if (!m_bci.equals(other.m_bci)) {
			return false;
		}
		if (m_frameLineNumber == null) {
			if (other.m_frameLineNumber != null) {
				return false;
			}
		} else if (!m_frameLineNumber.equals(other.m_frameLineNumber)) {
			return false;
		}
		if (m_method == null) {
			if (other.m_method != null) {
				return false;
			}
		} else if (!m_method.equals(other.m_method)) {
			return false;
		}
		if (m_type != other.m_type) {
			return false;
		}
		return true;
	}

}
