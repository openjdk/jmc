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
package org.openjdk.jmc.common.util;

import java.io.IOException;
import java.io.Writer;

import org.openjdk.jmc.common.IWritableState;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A wrapper class used to write to an XML document using the {@link IWritableState} interface.
 */
class StateElementWriter implements IWritableState {

	private final Document document;
	private final Element element;

	/**
	 * Create a writer with a new XML document.
	 * 
	 * @param rootName
	 *            the name of the XML root element
	 * @throws IOException
	 *             if there is a problem creating the XML document
	 */
	StateElementWriter(String rootName) throws IOException {
		this(XmlToolkit.createNewDocument(rootName));
	}

	/**
	 * Create a writer with an existing XML document.
	 * 
	 * @param document
	 *            XML document to write to
	 */
	StateElementWriter(Document document) {
		this(document, document.getDocumentElement());
	}

	private StateElementWriter(Document document, Element element) {
		this.element = element;
		this.document = document;
	}

	@Override
	public IWritableState createChild(String type) {
		Element childElement = document.createElement(type);
		element.appendChild(childElement);
		return new StateElementWriter(document, childElement);
	}

	@Override
	public void putString(String key, String value) {
		if (value == null) {
			element.removeAttribute(key);
		} else {
			element.setAttribute(key, value);
		}
	}

	/**
	 * Print the XML document to a writer.
	 * 
	 * @param writer
	 *            writer to print the XML document to
	 */
	public void write(Writer writer) {
		XmlToolkit.prettyPrint(element, writer);
	}

	/**
	 * Get the XML document as a string.
	 * 
	 * @return the XML document as a string
	 */
	@Override
	public String toString() {
		return XmlToolkit.prettyPrint(element);
	}

}
