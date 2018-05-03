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
package org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.openjdk.jmc.common.util.XmlToolkit;

final class PrettyPrinter {
	private final PrintWriter m_out;
	// Elements to output on a single line
	private final Set<XMLTag> m_detailTags;

	/**
	 * Create a pretty printer to output {@link XMLModel}s as text.
	 *
	 * @param out
	 *            where to output
	 * @param oneLineElements
	 *            XML tags to output on a single line
	 */
	PrettyPrinter(PrintWriter out, Set<XMLTag> oneLineElements) {
		m_out = out;
		if (oneLineElements != null) {
			m_detailTags = oneLineElements;
		} else {
			m_detailTags = Collections.emptySet();
		}
	}

	public void print(XMLModel model) {
		printHeader();
		prettyPrint("", model.getRoot()); //$NON-NLS-1$
	}

	private void printHeader() {
		// FIXME: Don't hardcode to UTF8.
		m_out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
	}

	private void prettyPrint(final String padding, XMLTagInstance tagInstance) {
		String tagName = tagInstance.getTag().getName();

		if (tagInstance.getTagsInstances().size() == 0 && !tagInstance.hasContent()) {
			m_out.print(padding + '<' + tagName);
			printAttributes(tagInstance.getAttributeInstances());
			m_out.println("/>"); //$NON-NLS-1$
		} else {
			// TODO: clean up logic
			m_out.print(padding + '<' + tagName);
			printAttributes(tagInstance.getAttributeInstances());
			m_out.print('>');
			m_out.print(XmlToolkit.escapeAll(tagInstance.getContent().trim()));
			if (tagInstance.getTagsInstances().size() > 0) {
				m_out.println(""); //$NON-NLS-1$
				boolean first = true;
				for (XMLTagInstance childTagInstance : tagInstance.getTagsInstances()) {
					boolean keepOnOneRow = m_detailTags.contains(childTagInstance.getTag());
					if (first && !keepOnOneRow) {
						m_out.println(""); //$NON-NLS-1$
					}
					prettyPrint(padding + "  ", childTagInstance); //$NON-NLS-1$
					if (!keepOnOneRow) {
						m_out.println(""); //$NON-NLS-1$
					}
					first = false;
				}
				m_out.println(padding + "</" + tagName + '>'); //$NON-NLS-1$
			} else {
				m_out.println("</" + tagName + '>'); //$NON-NLS-1$
			}
		}
	}

	private void printAttributes(List<XMLAttributeInstance> attributeInstances) {
		for (XMLAttributeInstance ai : attributeInstances) {
			if (!ai.isImplicitDefault() || ai.getAttribute().isRequired()) {
				m_out.print(' ');
				m_out.print(ai.getAttribute().getName());
				m_out.print("=\""); //$NON-NLS-1$
				m_out.print(XmlToolkit.escapeAll(ai.getValue()));
				m_out.print('\"');
			}
		}
	}
}
