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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.InvalidRegistryObjectException;

/**
 * A command that can be executed.
 */
public final class Command implements Comparable<Command> {
	private final List<Parameter> m_parameters = new ArrayList<>();

	private String m_identifier;
	private String m_description;
	private String m_name;
	private IConfigurationElement m_classElement;
	private ICommandHelper m_commandHelper;
	private String m_category = ""; //$NON-NLS-1$

	/**
	 * Constructor for a command
	 */
	Command() {
		// prevent instantiation outside package
	}

	/**
	 * Returns a human readable name of the command
	 *
	 * @return the name
	 */
	public String getName() {
		return m_name;
	}

	/**
	 * Returns a human readable description of the command.
	 *
	 * @return a human readable description
	 */
	public String getDesciption() {
		return m_description;
	}

	/**
	 * Returns an interface that can look up parameter values
	 *
	 * @return a type completion object or null if not available
	 */
	public ICommandHelper getCommandHelp() {
		return m_commandHelper;
	}

	/**
	 * REturn the name of the command
	 *
	 * @return the command
	 */
	public String getIdentifier() {
		return m_identifier;
	}

	/**
	 * Returns the parameters for the command
	 *
	 * @return a list of parameters.
	 */
	public List<Parameter> getParameters() {
		return m_parameters;
	}

	/**
	 * The human readable name of the command
	 *
	 * @param name
	 */
	void setName(String name) {
		m_name = name;
	}

	/**
	 * The description of the command
	 *
	 * @param description
	 */
	void setDescription(String description) {
		m_description = description;
	}

	/**
	 * Returns the category the command belongs to.
	 *
	 * @return the category
	 */
	public String getCategory() {
		return m_category;
	}

	/**
	 * Sets the category
	 *
	 * @param category
	 *            the category
	 */
	void setCategory(String category) {
		m_category = category;
	}

	/**
	 * sets the identifier for this commands
	 *
	 * @param identifier
	 *            the identifier
	 */
	void setIdentifier(String identifier) {
		m_identifier = identifier;
	}

	/**
	 * @param parameter
	 */
	void addParameter(Parameter parameter) {
		m_parameters.add(parameter);
	}

	/**
	 * Sets a type completion interface that can be used to look up parameter values.
	 *
	 * @param typeCompletion
	 *            an interface that can return values or null if not available
	 */
	void setCommandHelper(ICommandHelper typeCompletion) {
		m_commandHelper = typeCompletion;
	}

	// NOTE: It's pretty ugly to add extension point things into this class, but this is done because we need need to create the class using createExecutableExtension in createExecutableStatement below
	/**
	 * The configuration element that should be executed.
	 *
	 * @param element
	 */
	void setClassConfigurationElement(IConfigurationElement element) {
		m_classElement = element;
	}

	IExecute createExecutableStatement() {
		if (m_classElement == null) {
			throw new RuntimeException("No registered IExcutable for statement"); //$NON-NLS-1$
		}
		try {
			return (IExecute) m_classElement.createExecutableExtension("class"); //$NON-NLS-1$
		} catch (InvalidRegistryObjectException ie) {
			throw new RuntimeException("Could not execute " + getIdentifier(), ie); //$NON-NLS-1$
		} catch (CoreException e) {
			throw new RuntimeException("Could not execute " + getIdentifier(), e); //$NON-NLS-1$
		}
	}

	/**
	 * Returns a textual representation for the command.
	 *
	 * @return a textual representation of the command.
	 */
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(m_identifier);
		result.append("("); //$NON-NLS-1$
		List<Parameter> parameters = getParameters();
		for (int n = 0; n < parameters.size(); n++) {
			result.append(parameters.get(n).getIdentifier());
			if (n != parameters.size() - 1) {
				result.append(", "); //$NON-NLS-1$
			}
		}
		result.append(")"); //$NON-NLS-1$
		return result.toString();
	}

	/**
	 * Compare this command with another command. The order is determined by comparing the
	 * identifier alphabetically.
	 */
	@Override
	public int compareTo(Command command) {
		return getIdentifier().compareTo(command.getIdentifier());
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof Command) {
			return getIdentifier().equals(((Command) object).getIdentifier());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return getIdentifier().hashCode();
	}

	public int getNonOptionalParameterCount() {
		int count = 0;
		for (Parameter p : getParameters()) {
			if (!p.isOptional()) {
				count++;
			}
		}
		return count;
	}

}
