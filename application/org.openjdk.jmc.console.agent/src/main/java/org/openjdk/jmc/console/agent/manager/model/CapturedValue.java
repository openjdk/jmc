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

import java.net.URI;
import java.net.URISyntaxException;

import org.openjdk.jmc.console.agent.messages.internal.Messages;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class CapturedValue implements ICapturedValue {

	private static final String DEFAULT_STRING_FIELD = ""; // $NON-NLS-1$
	private static final Object DEFAULT_OBJECT_TYPE = null;
	// Regex for matching converter names e.g. someClass.converter
	private static final String CONVERTER_REGEX = "([a-zA-Z_$][a-zA-Z0-9_$]*\\.)*([a-zA-Z_$][a-zA-Z0-9_$]*)"; // $NON-NLS-1$

	private static final String XML_TAG_CAPTURED_VALUE = "capturedvalue"; // $NON-NLS-1$
	private static final String XML_TAG_NAME = "name"; // $NON-NLS-1$
	private static final String XML_TAG_DESCRIPTION = "description"; // $NON-NLS-1$
	private static final String XML_TAG_CONTENT_TYPE = "contenttype"; // $NON-NLS-1$
	private static final String XML_TAG_RELATION_KEY = "relationkey"; // $NON-NLS-1$
	private static final String XML_TAG_CONVERTER = "converter"; // $NON-NLS-1$

	private String name;
	private String description;
	private ContentType contentType;
	private String relationKey;
	private String converter;

	CapturedValue() {
		name = DEFAULT_STRING_FIELD;
		description = DEFAULT_STRING_FIELD;
		contentType = (ContentType) DEFAULT_OBJECT_TYPE;
		relationKey = DEFAULT_STRING_FIELD;
		converter = DEFAULT_STRING_FIELD;
	}

	CapturedValue(Element element) {
		this();

		NodeList elements;
		elements = element.getElementsByTagName(XML_TAG_NAME);
		if (elements.getLength() != 0) {
			name = elements.item(0).getTextContent();
		}

		elements = element.getElementsByTagName(XML_TAG_DESCRIPTION);
		if (elements.getLength() != 0) {
			description = elements.item(0).getTextContent();
		}

		elements = element.getElementsByTagName(XML_TAG_CONTENT_TYPE);
		if (elements.getLength() != 0) {
			contentType = ContentType.valueOf(elements.item(0).getTextContent());
		}

		elements = element.getElementsByTagName(XML_TAG_RELATION_KEY);
		if (elements.getLength() != 0) {
			relationKey = elements.item(0).getTextContent();
		}

		elements = element.getElementsByTagName(XML_TAG_CONVERTER);
		if (elements.getLength() != 0) {
			converter = elements.item(0).getTextContent();
		}
	}

	@Override
	public Element buildElement(Document document) {
		Element element = document.createElement(XML_TAG_CAPTURED_VALUE);

		if (name != null && !name.isEmpty()) {
			Element nameElement = document.createElement(XML_TAG_NAME);
			nameElement.setTextContent(name);
			element.appendChild(nameElement);
		}

		if (description != null && !description.isEmpty()) {
			Element descriptionElement = document.createElement(XML_TAG_DESCRIPTION);
			descriptionElement.setTextContent(description);
			element.appendChild(descriptionElement);
		}

		if (contentType != null) {
			Element contentTypeElement = document.createElement(XML_TAG_CONTENT_TYPE);
			contentTypeElement.setTextContent(contentType.toString());
			element.appendChild(contentTypeElement);
		}

		if (relationKey != null && !relationKey.isEmpty()) {
			Element relationKeyElement = document.createElement(XML_TAG_RELATION_KEY);
			relationKeyElement.setTextContent(relationKey);
			element.appendChild(relationKeyElement);
		}

		if (converter != null && !converter.isEmpty()) {
			Element converterElement = document.createElement(XML_TAG_CONVERTER);
			converterElement.setTextContent(converter);
			element.appendChild(converterElement);
		}

		return element;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public ContentType getContentType() {
		return contentType;
	}

	@Override
	public void setContentType(ContentType contentType) {
		this.contentType = contentType;
	}

	@Override
	public String getRelationKey() {
		return relationKey;
	}

	@Override
	public void setRelationKey(String relationKey) {
		if (relationKey != null && !relationKey.isEmpty()) {
			relationKey = relationKey.trim();
			try {
				new URI(relationKey);
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException(Messages.CapturedValue_ERROR_RELATION_KEY_HAS_INCORRECT_SYNTAX);
			}
		}

		this.relationKey = relationKey;
	}

	@Override
	public String getConverter() {
		return converter;
	}

	@Override
	public void setConverter(String converter) {
		if (converter != null && !converter.isEmpty()) {
			converter = converter.trim();
			if (!converter.matches(CONVERTER_REGEX)) {
				throw new IllegalArgumentException(Messages.CapturedValue_ERROR_CONVERTER_HAS_INCORRECT_SYNTAX);
			}
		}

		this.converter = converter;
	}

	protected void copyContentToWorkingCopy(CapturedValue copy) {
		copy.name = name;
		copy.description = description;
		copy.contentType = contentType;
		copy.relationKey = relationKey;
		copy.converter = converter;
	}
}
