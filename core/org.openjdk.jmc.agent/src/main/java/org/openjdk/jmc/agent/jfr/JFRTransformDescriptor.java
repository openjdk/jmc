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
package org.openjdk.jmc.agent.jfr;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectweb.asm.Type;
import org.openjdk.jmc.agent.Method;
import org.openjdk.jmc.agent.Parameter;
import org.openjdk.jmc.agent.TransformDescriptor;
import org.openjdk.jmc.agent.util.TypeUtils;

public class JFRTransformDescriptor extends TransformDescriptor {
	private final static String ATTRIBUTE_EVENT_NAME = "name"; //$NON-NLS-1$
	private final static String ATTRIBUTE_JFR_EVENT_DESCRIPTION = "description"; //$NON-NLS-1$
	private final static String ATTRIBUTE_JFR_EVENT_KIND = "kind"; //$NON-NLS-1$
	private final static String ATTRIBUTE_JFR_EVENT_PATH = "path"; //$NON-NLS-1$
	private final static String ATTRIBUTE_STACK_TRACE = "stacktrace"; //$NON-NLS-1$
	private final static String ATTRIBUTE_THREAD = "thread"; //$NON-NLS-1$
	private final static String ATTRIBUTE_REUSE = "reuse"; //$NON-NLS-1$

	private final JFREventType eventType;
	private final String classPrefix;
	private final String eventDescription;
	private final String eventClassName;
	private final String eventName;
	private final String eventPath;
	private final boolean recordStackTrace;
	private final boolean recordThread;
	private final boolean reuseEventObject;
	private final boolean allowToString;
	private final List<Parameter> parameters;

	public JFRTransformDescriptor(String id, String className, Method method,
			Map<String, String> transformationAttributes, List<Parameter> parameters) {
		super(id, className, method, transformationAttributes);
		eventType = initializeEventType();
		classPrefix = initializeClassPrefix();
		eventName = initializeEventName();
		eventClassName = initializeEventClassName();
		eventPath = initializeEventPath();
		eventDescription = initializeEventDescription();
		recordStackTrace = getBoolean(ATTRIBUTE_STACK_TRACE, true);
		recordThread = getBoolean(ATTRIBUTE_THREAD, true);
		reuseEventObject = getBoolean(ATTRIBUTE_REUSE, false);
		allowToString = getBoolean(ATTRIBUTE_ALLOW_TO_STRING, true);
		this.parameters = parameters;
	}

	public JFREventType getEventType() {
		return eventType;
	}

	public String getEventClassName() {
		return eventClassName;
	}

	public String getEventName() {
		return eventName;
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

	public boolean isRecordThread() {
		return recordThread;
	}

	public boolean isReuseEventObject() {
		return reuseEventObject;
	}

	public boolean isAllowToString() {
		return allowToString;
	}

	private JFREventType initializeEventType() {
		String kind = getTransformationAttribute(ATTRIBUTE_JFR_EVENT_KIND);
		JFREventType type = JFREventType.parse(kind);
		if (type == JFREventType.UNDEFINED) {
			Logger.getLogger(JFRTransformDescriptor.class.getName()).log(Level.INFO,
					"The event kind was set to " + kind + ". Will assume DURATION."); //$NON-NLS-1$ //$NON-NLS-2$
			type = JFREventType.DURATION;
		}
		return type;
	}

	private String initializeClassPrefix() {
		String prefix = getTransformationAttribute(ATTRIBUTE_CLASS_PREFIX);
		if (prefix != null && TypeUtils.isValidJavaIdentifier(prefix)) {
			return prefix;
		}
		return DEFAULT_CLASS_PREFIX;
	}

	private String initializeEventName() {
		String eventName = getTransformationAttribute(ATTRIBUTE_EVENT_NAME);
		if (eventName == null || eventName.length() == 0) {
			eventName = getMethod().getName();
			Logger.getLogger(JFRTransformDescriptor.class.getName()).log(Level.INFO,
					"Could not find an event name, generated one: " + eventName); //$NON-NLS-1$
		}
		return eventName;
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
				+ TypeUtils.deriveIdentifierPart(getEventName());
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
							+ " was not set for the event " + eventName + ". Assuming " + defaultValue + "."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			return defaultValue;
		}
		return Boolean.parseBoolean(strVal);
	}

	@Override
	public String toString() {
		return String.format("JFRTransformDescriptor [method:%s, eventName:%s, #params:%d]", getMethod().toString(), //$NON-NLS-1$
				eventName, parameters.size());
	}

	public List<Parameter> getParameters() {
		return parameters;
	}

	public boolean isAllowedFieldType(Type type) {
		if (isAllowToString()) {
			return true;
		}
		return type.getSort() != Type.OBJECT && type.getSort() != Type.ARRAY;
	}
}
