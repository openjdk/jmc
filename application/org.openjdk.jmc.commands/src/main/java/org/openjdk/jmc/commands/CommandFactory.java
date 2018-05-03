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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

/**
 * Class that creates a list of the available commands in the system.
 */
class CommandFactory {
	private static final String DESCRIPTION = "description"; //$NON-NLS-1$
	private static final String IDENTIFIER = "identifier"; //$NON-NLS-1$
	private static final String NAME = "name"; //$NON-NLS-1$
	private static final String CLASS = "class"; //$NON-NLS-1$
	private static final String CATEGORY = "category"; //$NON-NLS-1$

	private static final String EXTENSION_POINT_ID = "org.openjdk.jmc.commands.command"; //$NON-NLS-1$
	private static final String EXTENSION_NAME = "command"; //$NON-NLS-1$
	private static final String COMMAND_HELPER = "commandHelper"; //$NON-NLS-1$

	static List<Command> createFromExtensionPoints() {
		List<Command> commands = new ArrayList<>();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint exPoint = registry.getExtensionPoint(EXTENSION_POINT_ID);
		if (exPoint != null) {
			commands.addAll(createForExtensionPoint(exPoint));
		}
		return commands;
	}

	private static List<Command> createForExtensionPoint(IExtensionPoint exPoint) {
		List<Command> commands = new ArrayList<>();
		IExtension[] ext = exPoint.getExtensions();
		for (IExtension element : ext) {
			commands.addAll(createCommands(element.getConfigurationElements()));
		}
		return commands;
	}

	private static List<Command> createCommands(IConfigurationElement[] configurationElements) {
		List<Command> commands = new ArrayList<>();
		for (IConfigurationElement configurationElement : configurationElements) {
			if (EXTENSION_NAME.equalsIgnoreCase(configurationElement.getName())) {
				try {
					commands.add(createCommand(configurationElement));
				} catch (Exception e) {
					System.err.println(e.getMessage());
				}
			}
		}
		return commands;
	}

	private static Command createCommand(IConfigurationElement configElement) throws Exception {
		Command command = new Command();
		setIdentifier(command, configElement);
		setName(command, configElement);
		setDescription(command, configElement);
		setClass(command, configElement);
		setCategory(command, configElement);
		setCommandHelper(command, configElement);

		for (IConfigurationElement childElement : configElement.getChildren()) {
			try {
				command.addParameter(createParameter(childElement));
			} catch (Exception e) {
				throw new Exception("Error initializing command " + command.getName() + ' ' + e.getMessage()); //$NON-NLS-1$
			}
		}
		return command;
	}

	private static void setCommandHelper(Command command, IConfigurationElement configElement) throws Exception {
		String clazz = configElement.getAttribute(COMMAND_HELPER);
		if (clazz != null && clazz.length() > 0) {
			try {
				Object o = configElement.createExecutableExtension(COMMAND_HELPER);
				if (o instanceof ICommandHelper) {
					command.setCommandHelper((ICommandHelper) o);
				}
			} catch (CoreException e) {
				e.printStackTrace();
				throw new Exception("Error creating type completion object", e); //$NON-NLS-1$
			}
		}
	}

	private static Parameter createParameter(IConfigurationElement configElement) throws Exception {
		String type = configElement.getName();
		if (type != null) {
			Map<String, String> parameterPropertiess = new HashMap<>();
			for (String name : configElement.getAttributeNames()) {
				String value = configElement.getAttribute(name);
				parameterPropertiess.put(name, value);
			}
			return new Parameter(type, parameterPropertiess);
		}
		throw new Exception("A parameter must have a type."); //$NON-NLS-1$
	}

	private static void setClass(Command command, IConfigurationElement configElement) throws Exception {
		String clazz = configElement.getAttribute(CLASS);
		if (clazz == null) {
			throw new Exception("A command must have a class ."); //$NON-NLS-1$
		}
		command.setClassConfigurationElement(configElement);
	}

	private static void setCategory(Command command, IConfigurationElement configElement) {
		String category = configElement.getAttribute(CATEGORY);
		if (category != null) {
			command.setCategory(category);
		}
	}

	private static void setDescription(Command command, IConfigurationElement configElement) {
		String description = configElement.getAttribute(DESCRIPTION);
		if (description != null) {
			command.setDescription(description);
		}
	}

	private static void setName(Command command, IConfigurationElement configElement) {
		String name = configElement.getAttribute(NAME);
		if (name == null) {
			name = command.getIdentifier();
		}
		command.setName(name);
	}

	private static void setIdentifier(Command command, IConfigurationElement configElement) throws Exception {
		String identifier = configElement.getAttribute(IDENTIFIER);
		if (identifier == null) {
			throw new Exception("Missing command name."); //$NON-NLS-1$
		}
		if (identifier.length() == 0) {
			throw new Exception("Command must be at least one character."); //$NON-NLS-1$
		}
		ensureValidCommandIdentifier(identifier);
		command.setIdentifier(identifier);
	}

	private static void ensureValidCommandIdentifier(String identifier) throws Exception {
		for (int n = 0; n < identifier.length(); n++) {
			char c = identifier.charAt(n);
			if (!Character.isJavaIdentifierStart(c) || c == '-') {
				throw new Exception(c + " is not a valid character for a command."); //$NON-NLS-1$
			}
		}
	}
}
