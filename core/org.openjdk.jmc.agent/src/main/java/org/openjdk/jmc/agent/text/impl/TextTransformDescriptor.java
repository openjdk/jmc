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
package org.openjdk.jmc.agent.text.impl;

import java.util.Map;

import org.openjdk.jmc.agent.Method;
import org.openjdk.jmc.agent.TransformDescriptor;

/**
 * Necessary transformation information for producing an event. For now limited to one event per
 * method.
 */
public class TextTransformDescriptor extends TransformDescriptor {
	private static final String ATTRIBUTE_MESSAGE_ENTRY = "messageEntry"; //$NON-NLS-1$
	private static final String ATTRIBUTE_MESSAGE_EXIT = "messageExit"; //$NON-NLS-1$

	public TextTransformDescriptor(String id, String className, Method method,
			Map<String, String> transformationAttributes) {
		super(id, className, method, transformationAttributes);
	}

	@Override
	public String toString() {
		return String.format("Text transform %s:%s pending:%s", getClassName(), getMethod().toString(), //$NON-NLS-1$
				String.valueOf(isPendingTransforms()));
	}

	public String getEnterMessage() {
		return getTransformationAttribute(ATTRIBUTE_MESSAGE_ENTRY);
	}

	public String getExitMessage() {
		return getTransformationAttribute(ATTRIBUTE_MESSAGE_EXIT);
	}
}
