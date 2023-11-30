/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.configuration.model.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class XMLTag extends XMLNode {
	static class Predicated extends XMLTag {
		private final String key;
		private final String value;

		public Predicated(String name, XMLAttribute attribute, String value) {
			super(name);
			key = attribute.getName();
			this.value = value;
		}

		@Override
		public boolean accepts(Map<String, String> attributes) {
			return value.equals(attributes.get(key));
		}
	}

	private final List<XMLTag> m_tags = new ArrayList<>();
	private final List<XMLAttribute> m_attributes = new ArrayList<>();

	public XMLTag(String name) {
		super(name, XMLNodeType.ELEMENT);
	}

	public XMLTag(String name, XMLNodeType type) {
		super(name, type);
	}

	public List<XMLTag> getTags() {
		return m_tags;
	}

	public List<XMLAttribute> getAttributes() {
		return m_attributes;
	}

	public void add(XMLNode ... nodes) {
		for (XMLNode node : nodes) {
			if (node instanceof XMLTag) {
				m_tags.add((XMLTag) node);
			}
			if (node instanceof XMLAttribute) {
				m_attributes.add((XMLAttribute) node);
			}
		}
	}

	@Override
	public String toString() {
		return "Tag: " + getName(); //$NON-NLS-1$
	}

	public boolean accepts(Map<String, String> attributes) {
		return true;
	}
}
