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

import java.io.PrintStream;
import java.util.List;

/**
 * A statement that can be executed. Clients should instantiate
 */
public final class Statement {
	private final Command m_command;
	private final List<Value> m_parameters;

	/**
	 * Constructor for a statement. Not for clients to create
	 *
	 * @param command
	 * @param values
	 */
	public Statement(Command command, List<Value> values) {
		m_command = command;
		m_parameters = values;
	}

	/**
	 * Return the command this statement is
	 *
	 * @return the command
	 */
	public Command getCommand() {
		return m_command;
	}

	/**
	 * Return a list of values in the statement
	 *
	 * @return a list of values
	 */
	public List<Value> getValues() {
		return m_parameters;
	}

	/**
	 * Executes the statement
	 *
	 * @throws RuntimeException
	 *             if an error occurs during the execution
	 */
	void execute(PrintStream out) throws RuntimeException {
		IExecute executable = getCommand().createExecutableStatement();
		executable.execute(this, out);
	}

	/**
	 * Returns a number value
	 *
	 * @param valueName
	 *            the name of the value
	 * @return the numeric value for the parameter
	 */
	public Number getNumber(String valueName) {
		return (Number) getValue(valueName).getObject();
	}

	/**
	 * Returns a string value
	 *
	 * @param valueName
	 *            the name of the value
	 * @return the value as a string
	 */
	public String getString(String valueName) {
		return (String) getValue(valueName).getObject();
	}

	/**
	 * Returns boolean value.
	 *
	 * @param valueName
	 *            the name of the value
	 * @return Boolean.TRUE if the value is "true", false otherwise.
	 */
	public Boolean getBoolean(String valueName) {
		return (Boolean) getValue(valueName).getObject();
	}

	/**
	 * Returns a value object
	 *
	 * @param valueName
	 *            the name of the value to get
	 * @return a value object
	 */
	public Value getValue(String valueName) {
		Value value = getSafeValue(valueName);
		if (value != null) {
			return value;
		}
		throw new RuntimeException("Unknown value name " + valueName); //$NON-NLS-1$
	}

	/**
	 * Returns if a value exist
	 *
	 * @param valueName
	 *            the name of the value to check for
	 * @return true if a value with the given name exists
	 */
	public boolean hasValue(String valueName) {
		return getSafeValue(valueName) != null;
	}

	/**
	 * Returns a textual representation of a statement
	 *
	 * @return a textual representation of a statement
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getCommand().getIdentifier());
		builder.append("("); //$NON-NLS-1$
		List<Value> values = getValues();
		for (int n = 0; n < values.size(); n++) {
			builder.append(String.valueOf(values.get(n).getObject()));
			if (n < values.size() - 1) {
				builder.append(", "); //$NON-NLS-1$
			}
		}
		builder.append(")"); //$NON-NLS-1$
		return builder.toString();
	}

	private Value getSafeValue(String valueName) {
		for (Value v : getValues()) {
			if (valueName.equals(v.getParameter().getIdentifier())) {
				return v;
			}
		}
		return null;
	}
}
