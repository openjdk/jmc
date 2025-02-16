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

import org.openjdk.jmc.console.agent.messages.internal.Messages;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class MethodParameter extends CapturedValue implements IMethodParameter {
	private static final String DEFAULT_PARAMETER_NAME = "New Parameter"; // $NON-NLS-1$
	private static final int DEFAULT_INDEX = 0;

	private static final String XML_TAG_PARAMETER = "parameter"; // $NON-NLS-1$
	private static final String XML_ATTRIBUTE_INDEX = "index"; // $NON-NLS-1$

	private final Event event;

	private int index;

	MethodParameter(Event event) {
		super();
		this.event = event;

		index = DEFAULT_INDEX;
		setName(DEFAULT_PARAMETER_NAME);
	}

	MethodParameter(Event event, Element element) {
		super(element);
		this.event = event;

		index = Integer.parseInt(element.getAttribute(XML_ATTRIBUTE_INDEX));
	}

	@Override
	public Element buildElement(Document document) {
		Element element = super.buildElement(document);
		element = (Element) document.renameNode(element, null, XML_TAG_PARAMETER);
		element.setAttribute(XML_ATTRIBUTE_INDEX, String.valueOf(index));
		return element;
	}

	@Override
	public void setName(String name) {
		if (name == null || name.isEmpty()) {
			throw new IllegalArgumentException(Messages.MethodParameter_ERROR_NAME_CANNOT_BE_EMPTY_OR_NULL);
		}

		super.setName(name);
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public void setIndex(int index) {
		if (index < 0) {
			throw new IllegalArgumentException(Messages.MethodParameter_ERROR_INDEX_CANNOT_BE_LESS_THAN_ZERO);
		}

		this.index = index;
	}

	@Override
	public MethodParameter createWorkingCopy() {
		MethodParameter parameter = new MethodParameter(event);

		copyContentToWorkingCopy(parameter);
		parameter.index = index;

		return parameter;
	}
}
