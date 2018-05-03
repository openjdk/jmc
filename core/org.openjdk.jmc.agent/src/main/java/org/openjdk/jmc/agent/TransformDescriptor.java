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
package org.openjdk.jmc.agent;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.openjdk.jmc.agent.jfr.JFRTransformDescriptor;
import org.openjdk.jmc.agent.text.impl.TextTransformDescriptor;

/**
 * General metadata describing a transform to take place for a method.
 */
public abstract class TransformDescriptor {
	public static final String ATTRIBUTE_CLASS_PREFIX = "classprefix"; //$NON-NLS-1$
	public static final String ATTRIBUTE_ALLOW_TO_STRING = "allowtostring"; //$NON-NLS-1$
	public static final String ATTRIBUTE_LOGGING_TYPE = "loggingtype"; //$NON-NLS-1$

	public static final String DEFAULT_CLASS_PREFIX = "__JFREvent"; //$NON-NLS-1$

	private final String id;
	private final String className;
	private final Method method;
	private final Map<String, String> transformationAttributes;
	private volatile boolean pendingTransforms = true;

	public TransformDescriptor(String id, String className, Method method,
			Map<String, String> transformationAttributes) {
		this.id = id;
		this.className = className;
		this.method = method;
		this.transformationAttributes = transformationAttributes;
	}

	public String getId() {
		return id;
	}

	public String getClassName() {
		return className;
	}

	public Method getMethod() {
		return method;
	}

	public Map<String, String> getTransformationAttributes() {
		return transformationAttributes;
	}

	public boolean isPendingTransforms() {
		return pendingTransforms;
	}

	public void setPendingTransforms(boolean hasPendingTransforms) {
		this.pendingTransforms = hasPendingTransforms;
	}

	protected String getTransformationAttribute(String attribute) {
		return transformationAttributes.get(attribute);
	}

	/**
	 * Factory method for creating {@link TransformDescriptor} instances.
	 *
	 * @param id
	 *            transform id
	 * @param internalName
	 *            the class name in VM internal form.
	 * @param method
	 *            the method (see {@link Method})
	 * @param values
	 *            the values describing the transform.
	 * @param parameters
	 *            the parameters to include (see {@link Parameter}).
	 * @return the instantiated {@link TransformDescriptor}.
	 */
	public static TransformDescriptor create(
		String id, String internalName, Method method, Map<String, String> values, List<Parameter> parameters) {
		TransformType transformType = TransformType.parse(values.get(ATTRIBUTE_LOGGING_TYPE));
		if (transformType == TransformType.TEXT) {
			return new TextTransformDescriptor(id, internalName, method, values);
		} else if (transformType == TransformType.JFR) {
			return new JFRTransformDescriptor(id, internalName, method, values, parameters);
		} else {
			Logger.getLogger(Transformer.class.getName()).warning(
					String.format("Unknown logging type requested: %s. Will skip instrumenting %s.%s.", values.get( //$NON-NLS-1$
							"loggingtype"), internalName, method)); //$NON-NLS-1$
			return null;
		}
	}

	@Override
	public String toString() {
		return String.format("TransformDescriptor [method:%s]", method.toString()); //$NON-NLS-1$
	}
}
