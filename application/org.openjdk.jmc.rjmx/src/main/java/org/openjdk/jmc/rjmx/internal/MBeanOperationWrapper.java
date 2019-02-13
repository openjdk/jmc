/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.rjmx.internal;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import javax.management.MBeanFeatureInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.openjdk.jmc.common.util.TypeHandling;
import org.openjdk.jmc.rjmx.services.IOperation;
import org.openjdk.jmc.rjmx.services.IllegalOperandException;
import org.openjdk.jmc.rjmx.services.internal.AbstractOperation;
import org.openjdk.jmc.rjmx.services.internal.Messages;
import org.openjdk.jmc.rjmx.util.internal.SimpleAttributeInfo;

final class MBeanOperationWrapper extends AbstractOperation<SimpleAttributeInfo> {
	private static final String IMPACT = ".vmImpact"; //$NON-NLS-1$
	private static final int MAX_DESCRIPTORS = 8;
	private static final int MAX_LINE_LENGTH = 100;
	private static final String ELLIPSIS_STRING = "..."; //$NON-NLS-1$

	private final MBeanServerConnection connection;
	private final ObjectName objectName;

	private MBeanOperationWrapper(MBeanServerConnection connection, ObjectName objectName, MBeanOperationInfo info) {
		super(info.getName(), convertDescription(info), info.getReturnType(), convertArguments(info),
				convertImpact(info));
		this.connection = connection;
		this.objectName = objectName;
	}

	@Override
	public Callable<?> getInvocator(final Object ... argVals) throws IllegalOperandException {
		List<SimpleAttributeInfo> params = getSignature();
		final StringBuilder argString = new StringBuilder("("); //$NON-NLS-1$
		if (argVals.length < params.size()) {
			// Argument list is shorter than the signature
			throw new IllegalOperandException(params.subList(argVals.length, params.size()));
		}
		final String[] sig = new String[params.size()];
		for (int i = 0; i < params.size(); i++) {
			sig[i] = params.get(i).getType();
			if (argVals[i] != null) {
				argString.append(' ').append(describeValue(argVals[i])).append(',');
			} else if (TypeHandling.isPrimitive(sig[i])) {
				// Argument value of primitive type is null
				IllegalOperandException ex = new IllegalOperandException(params.get(i));
				while (++i < params.size()) {
					// Check for other attributes with the same error
					if (argVals[i] == null && TypeHandling.isPrimitive(params.get(i).getType())) {
						ex.addInvalidValue(params.get(i));
					}
				}
				throw ex;
			}
		}
		if (argString.charAt(argString.length() - 1) == ',') {
			argString.deleteCharAt(argString.length() - 1);
		}
		argString.append(" )"); //$NON-NLS-1$

		return new Callable<Object>() {

			@Override
			public Object call() throws Exception {
				return connection.invoke(objectName, getName(), argVals, sig);
			}

			@Override
			public String toString() {
				return getName() + argString.toString().replace("\n", "\\n").replace("\r", "\\r"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
		};
	}

	static Collection<IOperation> createOperations(
		ObjectName objectName, MBeanOperationInfo[] operations, MBeanServerConnection connection) {
		List<IOperation> wrappedOperations = new ArrayList<>();
		for (MBeanOperationInfo info : operations) {
			wrappedOperations.add(new MBeanOperationWrapper(connection, objectName, info));
		}
		return wrappedOperations;
	}

	/**
	 * @return {@code string} if it is not {@code null} and it is not an empty or blank string,
	 *         otherwise {@code defaultString}
	 */
	private static String asNonNullNorEmptyString(String string, String defaultString) {
		return (string != null && string.trim().length() > 0) ? string : defaultString;
	}

	private static String describeValue(Object o) {
		if (o.getClass().isArray()) {
			return o.getClass().getComponentType().getName() + "[" + Array.getLength(o) + "]"; //$NON-NLS-1$ //$NON-NLS-2$
		} else if (o instanceof String) {
			return "\"" + o + "\""; //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			return o.toString();
		}
	}

	private static OperationImpact convertImpact(MBeanOperationInfo info) {
		for (String name : info.getDescriptor().getFieldNames()) {
			if (name.endsWith(IMPACT)) {
				Object impactField = info.getDescriptor().getFieldValue(name);
				if (impactField != null) {
					String impact = impactField.toString();
					if (impact.startsWith("Low")) { //$NON-NLS-1$
						return OperationImpact.IMPACT_LOW;
					}
					if (impact.startsWith("Medium")) { //$NON-NLS-1$
						return OperationImpact.IMPACT_MEDIUM;
					}
					if (impact.startsWith("High")) { //$NON-NLS-1$
						return OperationImpact.IMPACT_HIGH;
					}
				}
			}
		}
		return OperationImpact.IMPACT_UNKNOWN;
	}

	private static List<SimpleAttributeInfo> convertArguments(MBeanOperationInfo info) {
		MBeanParameterInfo[] sign = info.getSignature();
		List<SimpleAttributeInfo> signature = new ArrayList<>(sign.length);
		for (MBeanParameterInfo paramInfo : sign) {
			// Use name primarily. Use description as name only if name is null or empty.
			String name = asNonNullNorEmptyString(paramInfo.getName(),
					asNonNullNorEmptyString(paramInfo.getDescription(), "")); //$NON-NLS-1$
			signature.add(new SimpleAttributeInfo(name, paramInfo.getType(), convertDescription(paramInfo)));
		}
		return signature;
	}

	private static String convertDescription(MBeanFeatureInfo info) {
		// FIXME: Building tool tips and descriptors should be unified into a toolkit
		StringBuilder sb = new StringBuilder();
		if (info.getDescriptor() != null && info.getDescriptor().getFields() != null) {
			String[] fields = info.getDescriptor().getFields();
			if (fields.length > 0) {
				// TODO: This is a workaround to get the descriptors to UI-layer. Should be handled using some adaptive mechanism.
				sb.append(Messages.MBeanOperationsWrapper_DESCRIPTOR).append(":\n "); //$NON-NLS-1$
				for (int i = 0; i < Math.min(fields.length, MAX_DESCRIPTORS); i++) {
					String str = fields[i];
					int cur = 0;
					int newLine = 0;
					while (cur < str.length() && newLine != -1) {
						newLine = str.indexOf('\n', cur);
						if (newLine == -1) {
							sb.append(shorten(str.substring(cur))).append("\n "); //$NON-NLS-1$
						} else {
							sb.append(shorten(str.substring(cur, newLine))).append("\n "); //$NON-NLS-1$
							cur = newLine + 1;
						}
					}
				}
			}
		}
		return shorten(info.getDescription()) + "\n " + sb.toString().trim(); //$NON-NLS-1$
	}

	private static String shorten(String s) {
		if (s == null) {
			return ""; //$NON-NLS-1$
		} else if (s.length() > MAX_LINE_LENGTH) {
			return (s.subSequence(0, MAX_LINE_LENGTH - ELLIPSIS_STRING.length()) + ELLIPSIS_STRING);
		} else {
			return s;
		}
	}
}
