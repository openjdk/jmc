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
import org.w3c.dom.NodeList;

public class Field extends CapturedValue implements IField {

	private static final String DEFAULT_FIELD_NAME = "New Field"; // $NON-NLS-1$
	private static final String DEFAULT_FIELD_EXPRESSION = "myField"; // $NON-NLS-1$
	// Regex for matching expressions for locating fields to capture (e.g. InstrumentMe.STATIC_STRING_FIELD)
	private static final String EXPRESSION_REGEX = "([a-zA-Z_$][a-zA-Z0-9_$]*\\.)*([a-zA-Z_$][a-zA-Z0-9_$]*)(\\.[a-zA-Z_$][a-zA-Z_$]*)*"; // $NON-NLS-1$

	private static final String XML_TAG_FIELD = "field"; // $NON-NLS-1$
	private static final String XML_TAG_EXPRESSION = "expression"; // $NON-NLS-1$

	private final Event event;

	private String expression;

	Field(Event event) {
		super();
		this.event = event;

		expression = DEFAULT_FIELD_EXPRESSION;
		setName(DEFAULT_FIELD_NAME);
	}

	Field(Event event, Element element) {
		super(element);
		this.event = event;

		NodeList elements = element.getElementsByTagName(XML_TAG_EXPRESSION);
		if (elements.getLength() != 0) {
			expression = elements.item(0).getTextContent();
		}
	}

	@Override
	public Element buildElement(Document document) {
		Element element = super.buildElement(document);
		element = (Element) document.renameNode(element, null, XML_TAG_FIELD);

		Element expressionElement = document.createElement(XML_TAG_EXPRESSION);
		expressionElement.setTextContent(expression);
		element.appendChild(expressionElement);

		return element;
	}

	@Override
	public void setName(String name) {
		if (name == null || name.isEmpty()) {
			throw new IllegalArgumentException(Messages.Field_ERROR_NAME_CANNOT_BE_EMPTY_OR_NULL);
		}

		super.setName(name);
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		if (expression == null || expression.isEmpty()) {
			throw new IllegalArgumentException(Messages.Field_ERROR_EXPRESSION_CANNOT_BE_EMPTY_OR_NULL);
		}

		expression = expression.trim();
		if (!expression.matches(EXPRESSION_REGEX)) {
			throw new IllegalArgumentException(Messages.Field_ERROR_EXPRESSION_HAS_INCORRECT_SYNTAX);
		}

		this.expression = expression;
	}

	@Override
	public Field createWorkingCopy() {
		Field copy = new Field(event);

		copyContentToWorkingCopy(copy);
		copy.expression = expression;

		return copy;
	}

	@Override
	public Field createDuplicate() {
		Field duplicate = createWorkingCopy();
		duplicate.setName(event.nextUniqueFieldName(getName()));

		return duplicate;
	}
}
