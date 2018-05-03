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
package org.openjdk.jmc.commands;

/**
 * Class that hold a parameter value for a statement.
 * <p>
 * Clients not should instantiate.
 */
final public class Value {
	private final Parameter m_parameter;
	private final Object m_valueObject;
	private final int m_position;

	public Value(Parameter parameter, Object valueObject, int position) {
		m_parameter = parameter;
		m_valueObject = valueObject;
		m_position = position;
	}

	static Object resolve(Object valueName) {
		if (valueName instanceof String && ((String) valueName).startsWith("$")) { //$NON-NLS-1$
			return CommandsPlugin.getDefault().getEnvironmentVariable((String) valueName);
		}
		return valueName;
	}

	/**
	 * Returns the parameter for the value
	 *
	 * @return the parameter value
	 */
	public Parameter getParameter() {
		return m_parameter;
	}

	/**
	 * Returns the object value
	 *
	 * @return the value object
	 */
	public Object getObject() {
		return resolve(m_valueObject);
	}

	/**
	 * Returns the position (column) that the token for the value was found.
	 *
	 * @return the value object
	 */
	public int getPosition() {
		return m_position;
	}

	/**
	 * Returns textual representation of the value.
	 *
	 * @return textual representation of the value
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getParameter().getIdentifier());
		builder.append("="); //$NON-NLS-1$
		builder.append(String.valueOf(getObject()));
		return builder.toString();
	}
}
