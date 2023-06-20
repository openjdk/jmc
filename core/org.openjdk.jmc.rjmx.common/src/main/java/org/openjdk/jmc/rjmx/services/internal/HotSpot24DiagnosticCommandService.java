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
package org.openjdk.jmc.rjmx.services.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.management.Descriptor;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.openjdk.jmc.rjmx.ConnectionToolkit;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.ServiceNotAvailableException;
import org.openjdk.jmc.rjmx.services.IDiagnosticCommandService;
import org.openjdk.jmc.rjmx.services.IOperation.OperationImpact;
import org.openjdk.jmc.rjmx.services.IllegalOperandException;
import org.openjdk.jmc.rjmx.util.internal.SimpleAttributeInfo;

public class HotSpot24DiagnosticCommandService implements IDiagnosticCommandService {

	private static final ObjectName DIAGNOSTIC_BEAN = ConnectionToolkit
			.createObjectName("com.sun.management:type=DiagnosticCommand"); //$NON-NLS-1$
	private static final String OPERATION_UPDATE = "update"; //$NON-NLS-1$
	private final MBeanServerConnection m_mbeanServer;
	private final Map<String, String> commandNameToOperation = new HashMap<>();
	private Collection<DiagnosticCommand> operations;

	private static final String IMPACT = "dcmd.vmImpact"; //$NON-NLS-1$
	private static final String NAME = "dcmd.name"; //$NON-NLS-1$
	private static final String DESCRIPTION = "dcmd.description"; //$NON-NLS-1$
//	private final static String HELP = "dcmd.help"; //$NON-NLS-1$
	private static final String ARGUMENTS = "dcmd.arguments"; //$NON-NLS-1$
	private static final String ARGUMENT_NAME = "dcmd.arg.name"; //$NON-NLS-1$
	private static final String ARGUMENT_DESCRIPTION = "dcmd.arg.description"; //$NON-NLS-1$
	private static final String ARGUMENT_MANDATORY = "dcmd.arg.isMandatory"; //$NON-NLS-1$
	private static final String ARGUMENT_TYPE = "dcmd.arg.type"; //$NON-NLS-1$
	private static final String ARGUMENT_OPTION = "dcmd.arg.isOption"; //$NON-NLS-1$
	private static final String ARGUMENT_MULITPLE = "dcmd.arg.isMultiple"; //$NON-NLS-1$

	private static List<DiagnosticCommandParameter> extractSignature(Descriptor args) {
		if (args != null) {
			String[] argNames = args.getFieldNames();
			List<DiagnosticCommandParameter> parameters = new ArrayList<>(argNames.length);
			for (String argName : argNames) {
				Descriptor arg = (Descriptor) args.getFieldValue(argName);
				parameters.add(new DiagnosticCommandParameter(arg));
			}
			return parameters;
		} else {
			return Collections.emptyList();
		}
	}

	private static OperationImpact extractImpact(Descriptor d) {
		String impact = d.getFieldValue(IMPACT).toString();
		if (impact.startsWith("Low")) { //$NON-NLS-1$
			return OperationImpact.IMPACT_LOW;
		}
		if (impact.startsWith("Medium")) { //$NON-NLS-1$
			return OperationImpact.IMPACT_MEDIUM;
		}
		if (impact.startsWith("High")) { //$NON-NLS-1$
			return OperationImpact.IMPACT_HIGH;
		}
		return OperationImpact.IMPACT_UNKNOWN;
	}

	private static String extractType(Descriptor d) {
		boolean isMultiple = Boolean.parseBoolean(d.getFieldValue(ARGUMENT_MULITPLE).toString());
		String typeName = d.getFieldValue(ARGUMENT_TYPE).toString();
		if (isMultiple) {
			if (typeName.equals("STRING SET")) { //$NON-NLS-1$
				return String[].class.getName();
			} else {
				return typeName.toLowerCase().replace(' ', '_') + '*';
			}
		}
		if (typeName.equals("BOOLEAN")) { //$NON-NLS-1$
			return Boolean.class.getName();
		} else if (typeName.equals("STRING") || typeName.equals("NANOTIME") || typeName.equals("MEMORY SIZE")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			return String.class.getName();
		} else if (typeName.equals("JLONG")) { //$NON-NLS-1$
			return Long.class.getName();
		} else {
			return typeName.toLowerCase().replace(' ', '_');
		}
	}

	private static String extractDescription(Descriptor d) {
		// FIXME: Argument descriptions for JFR operations contains \" that should be ". Workaround for now.
		String desc = d.getFieldValue(ARGUMENT_DESCRIPTION).toString().trim().replaceAll("\\\\\"", "\""); //$NON-NLS-1$ //$NON-NLS-2$
		return desc.length() > 0 ? desc : d.getFieldValue(ARGUMENT_NAME).toString().trim();
	}

	private static class ArgumentBuilder {
		private final List<String> arguments = new ArrayList<>();

		public void appendArgument(Object value, DiagnosticCommandParameter parameterInfo)
				throws IllegalOperandException {
			if (parameterInfo.isMultiple) {
				if (value.getClass().isArray()) {
					for (Object o : ((Object[]) value)) {
						appendValue(o, parameterInfo);
					}
				} else {
					throw new IllegalOperandException(parameterInfo);
				}
			} else {
				appendValue(value, parameterInfo);
			}
		}

		private void appendValue(Object value, DiagnosticCommandParameter parameterInfo)
				throws IllegalOperandException {
			StringBuilder sb = new StringBuilder();
			if (parameterInfo.isOption) {
				sb.append(parameterInfo.parameterName).append('=');
			}
			String stringValue = String.valueOf(value);
			if (stringValue.indexOf('"') >= 0) {
				throw new IllegalOperandException(parameterInfo);
			} else if (stringValue.indexOf(' ') >= 0) {
				sb.append('"').append(stringValue).append('"');
			} else {
				sb.append(stringValue);
			}
			arguments.add(sb.toString());
		}

		public String[] asArray() {
			return arguments.toArray(new String[arguments.size()]);
		}
	}

	private static class DiagnosticCommandParameter extends SimpleAttributeInfo {
		private final boolean isOption;
		private final boolean isMultiple;
		private final boolean isRequired;
		private final String parameterName;

		public DiagnosticCommandParameter(Descriptor d) {
			super(d.getFieldValue(ARGUMENT_NAME).toString(), extractType(d), extractDescription(d));
			parameterName = d.getFieldValue(ARGUMENT_NAME).toString();
			isOption = Boolean.parseBoolean(d.getFieldValue(ARGUMENT_OPTION).toString());
			isMultiple = Boolean.parseBoolean(d.getFieldValue(ARGUMENT_MULITPLE).toString());
			isRequired = Boolean.parseBoolean(d.getFieldValue(ARGUMENT_MANDATORY).toString());
			RJMXPlugin.getDefault().getLogger()
					.finest("DiagnosticCommandArg created: " + getType() + ' ' + getName() + ' ' + getDescription() //$NON-NLS-1$
							+ (isRequired ? " isRequired" : "") //$NON-NLS-1$ //$NON-NLS-2$
							+ (isOption ? " isOption" : "") + (isMultiple ? " isMultiple" : "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}

	}

	private class DiagnosticCommand extends AbstractOperation<DiagnosticCommandParameter> {

		public DiagnosticCommand(Descriptor d, String returnType) {
			super(d.getFieldValue(NAME).toString(), d.getFieldValue(DESCRIPTION).toString(), returnType,
					extractSignature((Descriptor) d.getFieldValue(ARGUMENTS)), extractImpact(d));
			RJMXPlugin.getDefault().getLogger()
					.finest("DiagnosticCommand created: " + getName() + ' ' + getReturnType() + ' ' + getImpact()); //$NON-NLS-1$
		}

		@Override
		public Callable<?> getInvocator(Object ... argValues) throws IllegalOperandException {
			ArgumentBuilder ab = new ArgumentBuilder();
			List<DiagnosticCommandParameter> args = getSignature();
			for (int i = 0; i < args.size(); i++) {
				if (i >= argValues.length || argValues[i] == null) {
					if (args.get(i).isRequired) {
						// Argument value is required but not provided
						IllegalOperandException ex = new IllegalOperandException(args.get(i));
						while (++i < args.size()) {
							// Check for other attributes with the same error
							if (args.get(i).isRequired) {
								ex.addInvalidValue(args.get(i));
							}
						}
						throw ex;
					} else {
						continue;
					}
				}
				ab.appendArgument(argValues[i], args.get(i));
			}
			final String[] arguments = ab.asArray();
			return new Callable<Object>() {

				@Override
				public Object call() throws Exception {
					return execute(arguments);
				}

				@Override
				public String toString() {
					return getName() + ' ' + asString(arguments);
				}
			};
		}

		private String asString(String[] array) {
			StringBuilder sb = new StringBuilder();
			if (array != null) {
				for (int i = 0; i < array.length; i += 1) {
					sb.append(array[i]);
					if (i + 1 < array.length) {
						sb.append(' ');
					}
				}
			}
			return sb.toString();
		}

		private String execute(String[] arguments)
				throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
			String operation = commandNameToOperation.get(getName());
			RJMXPlugin.getDefault().getLogger()
					.fine("Running " + getName() + " |" + operation + '|' + asString(arguments) + '|'); //$NON-NLS-1$ //$NON-NLS-2$
			if (operation == null) {
				throw new RuntimeException("Unavailable diagnostic command " + getName() + '!'); //$NON-NLS-1$
			}
			if (getSignature().size() > 0) {
				return (String) m_mbeanServer.invoke(DIAGNOSTIC_BEAN, operation, new Object[] {arguments},
						new String[] {String[].class.getName()});
			} else {
				return (String) m_mbeanServer.invoke(DIAGNOSTIC_BEAN, operation, new Object[0], new String[0]);
			}
		}

	}

	public HotSpot24DiagnosticCommandService(MBeanServerConnection server) throws ServiceNotAvailableException {
		m_mbeanServer = server;
		try {
			refreshOperations();
		} catch (Exception e) {
			throw new ServiceNotAvailableException("Unable to retrieve diagnostic commands!"); //$NON-NLS-1$
		}
	}

	@Override
	public synchronized Collection<DiagnosticCommand> getOperations() throws Exception {
		refreshOperations();
		return operations;
	}

	private void refreshOperations() throws Exception {
		RJMXPlugin.getDefault().getLogger().finer("Refreshing diagnostic operations"); //$NON-NLS-1$
		MBeanInfo info = m_mbeanServer.getMBeanInfo(DIAGNOSTIC_BEAN);
		operations = new ArrayList<>(info.getOperations().length);
		commandNameToOperation.clear();
		for (MBeanOperationInfo oper : info.getOperations()) {
			if (!oper.getName().equals(OPERATION_UPDATE)) {
				Descriptor descriptor = oper.getDescriptor();
				DiagnosticCommand c = new DiagnosticCommand(descriptor, oper.getReturnType());
				operations.add(c);
				commandNameToOperation.put(c.getName(), oper.getName());
			}
		}
	}

	@Override
	public String runCtrlBreakHandlerWithResult(String command) throws Exception {
		int index = command.indexOf(' ');
		if (index > 0) {
			String operationName = command.substring(0, index);
			return findDiagnosticCommand(operationName).execute(splitArguments(command.substring(index + 1).trim()));
		} else {
			return findDiagnosticCommand(command).execute(null);
		}
	}

	private String[] splitArguments(String commandArguments) {
		List<String> arguments = new ArrayList<>();
		StringBuilder argument = new StringBuilder();
		boolean inCitation = false;
		for (char c : commandArguments.toCharArray()) {
			if (inCitation) {
				if (c == '"') {
					inCitation = false;
				}
				argument.append(c);
			} else {
				if (Character.isWhitespace(c)) {
					if (argument.length() > 0) {
						arguments.add(argument.toString());
						argument = new StringBuilder();
					}
				} else {
					if (c == '"') {
						inCitation = true;
					}
					argument.append(c);
				}
			}
		}
		if (argument.length() > 0) {
			arguments.add(argument.toString());
		}
		return arguments.toArray(new String[arguments.size()]);
	}

	private synchronized DiagnosticCommand findDiagnosticCommand(String operationName) throws Exception {
		for (DiagnosticCommand op : operations) {
			if (op.getName().equals(operationName)) {
				return op;
			}
		}
		throw new IllegalArgumentException("Unavailable diagnostic command " + operationName + '!'); //$NON-NLS-1$
	}

}
