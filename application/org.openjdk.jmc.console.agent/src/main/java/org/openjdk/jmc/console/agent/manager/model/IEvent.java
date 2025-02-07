/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, 2025, Red Hat Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.console.agent.manager.model;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public interface IEvent {
	enum Location {
		ENTRY, EXIT, WRAP,
	}

	String getId();

	void setId(String id);

	String getName();

	void setName(String name);

	String getClazz();

	void setClazz(String clazz);

	String getDescription();

	void setDescription(String description);

	String getPath();

	void setPath(String path);

	boolean getStackTrace();

	void setStackTrace(boolean enabled);

	boolean getRethrow();

	void setRethrow(boolean enabled);

	Location getLocation();

	void setLocation(Location location);

	String getMethodName();

	void setMethodName(String methodName);

	String getMethodDescriptor();

	void setMethodDescriptor(String methodDescriptor);

	IMethodParameter[] getMethodParameters();

	void addMethodParameter(IMethodParameter methodParameter);

	void removeMethodParameter(IMethodParameter methodParameter);

	boolean containsMethodParameter(IMethodParameter methodParameter);

	void setMethodReturnValue(IMethodReturnValue methodReturnValue);

	IMethodReturnValue getMethodReturnValue();

	IField[] getFields();

	void addField(IField field);

	void removeField(IField field);

	boolean containsField(IField field);

	IEvent createWorkingCopy();

	IEvent createDuplicate();

	int nextUniqueParameterIndex();

	String nextUniqueParameterName(String originalName);

	String nextUniqueFieldName(String originalName);

	IMethodReturnValue createMethodReturnValue();

	IMethodParameter createMethodParameter();

	void updateMethodParameter(IMethodParameter original, IMethodParameter workingCopy);

	IField createField();

	void updateField(IField original, IField workingCopy);

	Element buildElement(Document document);
}
