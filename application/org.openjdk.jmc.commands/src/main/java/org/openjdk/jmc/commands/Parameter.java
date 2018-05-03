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

import java.util.Map;

/**
 * Parameter for an executable statement.
 */
public final class Parameter {
	private final static String NAME = "name"; //$NON-NLS-1$
	private final static String DESCRIPTION = "description"; //$NON-NLS-1$
	private final static String IDENTIFIER = "identifier"; //$NON-NLS-1$
	private final static String OPTIONAL = "optional"; //$NON-NLS-1$
	private final static String EXAMPLE = "exampleValue"; //$NON-NLS-1$

	private final String m_type;
	private final Map<String, String> m_properties;

	/**
	 * Constructor for a parameter.
	 *
	 * @param type
	 *            the type of parameter it is ("string", "number" or "boolean").
	 * @param properties
	 *            the set of properties that describes the command. e.g. "name".
	 */
	Parameter(String type, Map<String, String> properties) {
		m_type = type;
		m_properties = properties;
	}

	/**
	 * Returns the human readable name of the parameter, or null if missing
	 *
	 * @return teh human readable name.
	 */
	public String getName() {
		return m_properties.get(NAME);
	}

	/**
	 * Returns a description of the parameter.
	 *
	 * @return the description of the parameter, or null if missing
	 */
	public String getDescription() {
		return m_properties.get(DESCRIPTION);
	}

	/**
	 * Return if the parameter is optional.
	 *
	 * @return true if the parameter is optional, false otherwise.
	 */
	public boolean isOptional() {
		return Boolean.parseBoolean(m_properties.get(OPTIONAL));
	}

	/**
	 * The identifier, or the keyword, for the command
	 *
	 * @return the identifier
	 */
	public String getIdentifier() {
		return m_properties.get(IDENTIFIER);
	}

	/**
	 * Returns a value that can be passed to the parameter. Used by the help system.
	 *
	 * @return an example value.
	 */
	public String getExampleValue() {
		return m_properties.get(EXAMPLE);
	}

	/**
	 * The type the parameter can be. Valid types are "string", "number" and "boolean".
	 *
	 * @return the parameter type
	 */
	public String getType() {
		return m_type;
	}

	/**
	 * Return the properties that can be used for this parameter.
	 *
	 * @return the properties
	 */
	public Map<String, String> getProperties() {
		return m_properties;
	}

	/**
	 * Returns a textual representation for the parameter.
	 *
	 * @return a textual representation of the parameter.
	 */
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(getType());
		result.append(getProperties());
		return result.toString();
	}
}
