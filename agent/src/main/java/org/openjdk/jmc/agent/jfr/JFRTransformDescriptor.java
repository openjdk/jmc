/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.agent.jfr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import org.objectweb.asm.Type;
import org.openjdk.jmc.agent.Convertable;
import org.openjdk.jmc.agent.Field;
import org.openjdk.jmc.agent.Method;
import org.openjdk.jmc.agent.Parameter;
import org.openjdk.jmc.agent.ReturnValue;
import org.openjdk.jmc.agent.TransformDescriptor;
import org.openjdk.jmc.agent.util.TypeUtils;

public class JFRTransformDescriptor extends TransformDescriptor {
	private static final String ATTRIBUTE_EVENT_LABEL = "label"; //$NON-NLS-1$
	private static final String ATTRIBUTE_JFR_EVENT_DESCRIPTION = "description"; //$NON-NLS-1$
	private static final String ATTRIBUTE_JFR_EVENT_PATH = "path"; //$NON-NLS-1$
	private static final String ATTRIBUTE_STACK_TRACE = "stacktrace"; //$NON-NLS-1$
	private static final String ATTRIBUTE_RETHROW = "rethrow"; //$NON-NLS-1$

	private final String classPrefix;
	private final String eventDescription;
	private final String eventClassName;
	private final String eventLabel;
	private final String eventPath;
	private final boolean recordStackTrace;
	private final boolean useRethrow;
	private final boolean allowToString;
	private final boolean allowConverter;
	private final boolean emitOnException;
	private boolean matchFound;
	private final List<Parameter> parameters;
	private final ReturnValue returnValue;
	private final List<Field> fields;

	public JFRTransformDescriptor(String id, String className, Method method,
			Map<String, String> transformationAttributes, List<Parameter> parameters, ReturnValue returnValue,
			List<Field> fields) {
		super(id, className, method, transformationAttributes);
		classPrefix = initializeClassPrefix();
		eventLabel = initializeEventLabel();
		eventClassName = initializeEventClassName();
		eventPath = initializeEventPath();
		eventDescription = initializeEventDescription();
		recordStackTrace = getBoolean(ATTRIBUTE_STACK_TRACE, true);
		useRethrow = getBoolean(ATTRIBUTE_RETHROW, false);
		allowToString = getBoolean(ATTRIBUTE_ALLOW_TO_STRING, false);
		allowConverter = getBoolean(ATTRIBUTE_ALLOW_CONVERTER, false);
		emitOnException = getBoolean(ATTRIBUTE_EMIT_ON_EXCEPTION, false);
		this.parameters = parameters;
		this.fields = fields;
		this.returnValue = returnValue;
	}

	public static JFRTransformDescriptor from(CompositeData cd) {
		List<Parameter> params = new ArrayList<>();
		CompositeData[] cdParams = (CompositeData[]) cd.get("parameters");
		for (CompositeData cdParam : cdParams) {
			params.add(Parameter.from(cdParam));
		}

		List<Field> fields = new ArrayList<>();
		CompositeData[] cdFields = (CompositeData[]) cd.get("fields");
		for (CompositeData cdField : cdFields) {
			fields.add(Field.from(cdField));
		}

		Map<String, String> attr = new HashMap<>();
		TabularData td = (TabularData) cd.get("transformationAttributes");
		Object[] values = td.values().toArray();
		for (int i = 0; i < values.length; i++) {
			CompositeData cdValue = (CompositeData) values[i];
			String value = (String) cdValue.get("value");
			String key = (String) cdValue.get("key");
			attr.put(key, value);
		}

		return new JFRTransformDescriptor((String) cd.get("id"), (String) cd.get("className"),
				Method.from((CompositeData) cd.get("method")), attr, params,
				ReturnValue.from((CompositeData) cd.get("returnValue")), fields);
	}

	public String getEventClassName() {
		return eventClassName;
	}

	public String getEventLabel() {
		return eventLabel;
	}

	public String getClassPrefix() {
		return classPrefix;
	}

	public String getEventPath() {
		return eventPath;
	}

	public String getEventDescription() {
		return eventDescription;
	}

	public boolean isRecordStackTrace() {
		return recordStackTrace;
	}

	public boolean isUseRethrow() {
		return useRethrow;
	}

	public boolean isAllowToString() {
		return allowToString;
	}

	public boolean isAllowConverter() {
		return allowConverter;
	}

	public boolean isEmitOnException() {
		return emitOnException;
	}

	private String initializeClassPrefix() {
		String prefix = getTransformationAttribute(ATTRIBUTE_CLASS_PREFIX);
		if (prefix != null && TypeUtils.isValidJavaIdentifier(prefix)) {
			return prefix;
		}
		return DEFAULT_CLASS_PREFIX;
	}

	private String initializeEventLabel() {
		String eventLabel = getTransformationAttribute(ATTRIBUTE_EVENT_LABEL);
		if (eventLabel == null || eventLabel.length() == 0) {
			eventLabel = getMethod().getName();
			Logger.getLogger(JFRTransformDescriptor.class.getName()).log(Level.INFO,
					"Could not find an event name, generated one: " + eventLabel); //$NON-NLS-1$
		}
		return eventLabel;
	}

	private String initializeEventDescription() {
		String eventDescription = getTransformationAttribute(ATTRIBUTE_JFR_EVENT_DESCRIPTION);
		if (eventDescription == null || eventDescription.length() == 0) {
			Logger.getLogger(JFRTransformDescriptor.class.getName()).log(Level.INFO,
					"Could not find an event description for " + eventClassName); //$NON-NLS-1$
		}
		return eventDescription;
	}

	private String initializeEventClassName() {
		return TypeUtils.getPathPart(getClassName()) + getClassPrefix()
				+ TypeUtils.deriveIdentifierPart(getEventLabel());
	}

	private String initializeEventPath() {
		String eventPath = getTransformationAttribute(ATTRIBUTE_JFR_EVENT_PATH);
		if (eventPath == null || eventPath.length() == 0) {
			Logger.getLogger(JFRTransformDescriptor.class.getName()).log(Level.INFO,
					"Could not find an event path for " + eventClassName + " Will use the class name as path."); //$NON-NLS-1$ //$NON-NLS-2$
			eventPath = eventClassName;
		}
		return eventPath;
	}

	private boolean getBoolean(String attribute, boolean defaultValue) {
		String strVal = getTransformationAttribute(attribute);
		if (strVal == null || strVal.isEmpty()) {
			Logger.getLogger(JFRTransformDescriptor.class.getName()).log(Level.FINE,
					"The boolean attribute " + attribute //$NON-NLS-1$
							+ " was not set for the event " + eventLabel + ". Assuming " + defaultValue + "."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			return defaultValue;
		}
		return Boolean.parseBoolean(strVal);
	}

	@Override
	public String toString() {
		return String.format("JFRTransformDescriptor [method:%s, eventName:%s, #params:%d]", getMethod().toString(), //$NON-NLS-1$
				eventLabel, parameters.size());
	}

	public List<Parameter> getParameters() {
		return parameters;
	}

	public List<Field> getFields() {
		return fields;
	}

	public ReturnValue getReturnValue() {
		return returnValue;
	}

	public boolean isAllowedEventFieldType(Convertable convertable, Type type) {
		if (isAllowToString()) {
			return true;
		}
		// FIXME: Add better validation, such as checking the class is available
		if (isAllowConverter() && convertable.hasConverter()) {
			return true;
		}
		return TypeUtils.isSupportedType(type);
	}

	public void matchFound(boolean matched) {
		this.matchFound = matched;
	}

	public boolean isMatchFound() {
		return matchFound;
	}

}
