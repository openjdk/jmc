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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An XML element, with attribute values and child elements.
 */
public final class XMLTagInstance {
	private final List<XMLAttributeInstance> m_attributeInstances = new ArrayList<>();
	private final List<XMLTagInstance> m_childElements = new ArrayList<>();
	private final XMLTag m_tag;
	// NOTE: Non-final to support re-parenting.
	private XMLTagInstance m_parent;

	private String m_content = ""; //$NON-NLS-1$

	XMLTagInstance(XMLTagInstance parent, XMLTag tag) {
		m_parent = parent;
		m_tag = tag;
	}

	public XMLTag getTag() {
		return m_tag;
	}

	public List<XMLAttributeInstance> getAttributeInstances() {
		return m_attributeInstances;
	}

	public List<XMLTagInstance> getTagsInstances() {
		return m_childElements;
	}

	public XMLTagInstance getParent() {
		return m_parent;
	}

	public XMLTagInstance create(String tag, Map<String, String> attributes) {
		for (XMLTag xmlTag : getTag().getTags()) {
			if (tag.equalsIgnoreCase(xmlTag.getName()) && xmlTag.accepts(attributes)) {
				XMLTagInstance element = new XMLTagInstance(this, xmlTag);

				for (XMLAttribute attribute : xmlTag.getAttributes()) {
					XMLAttributeInstance ia = new XMLAttributeInstance(attribute);
					element.getAttributeInstances().add(ia);
					if (attributes.containsKey(attribute.getName())) {
						ia.setValue(attributes.get(attribute.getName()));
					}
				}
				m_childElements.add(element);
				return element;
			}
		}
		// FIXME: We might need to allow <xs:any/> elements (processContents="skip" or "lax", see XMLSchema).
		throw new IllegalArgumentException(tag + " is not allowed under " + getTag().getName()); //$NON-NLS-1$
	}

	public boolean remove(XMLTagInstance child) {
		return m_childElements.remove(child);
	}

	public void adopt(XMLTagInstance child) {
		XMLTagInstance oldParent = child.m_parent;
		if (oldParent != this) {
			m_childElements.add(child);
			child.m_parent = this;
			oldParent.m_childElements.remove(child);
		}
	}

	public void setContent(String content) {
		if (content == null) {
			throw new IllegalArgumentException("Content can't be null. Use empty string if missing"); //$NON-NLS-1$
		}
		m_content = content;
	}

	public boolean hasContent() {
		return getTag().getType() == XMLNodeType.ELEMENT_WITH_CONTENT;
	}

	public XMLTagInstance create(XMLTag tag) {
		return create(tag.getName(), new HashMap<String, String>());
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("<"); //$NON-NLS-1$
		builder.append(getTag().getName());
		for (XMLAttributeInstance a : getAttributeInstances()) {
			if (!a.isImplicitDefault()) {
				builder.append(" "); //$NON-NLS-1$
				builder.append(a.getAttribute().getName());
				builder.append("=\""); //$NON-NLS-1$
				builder.append(a.getValue());
				builder.append("\""); //$NON-NLS-1$
			}
		}
		builder.append("/>"); //$NON-NLS-1$
		return builder.toString();
	}

	/**
	 * @param attribute
	 * @return the value for the given {@link XMLAttribute attribute}, possibly the default, never
	 *         null.
	 * @see #getExplicitValue(XMLAttribute)
	 * @throws IllegalArgumentException
	 *             if this tag has no such attribute
	 */
	public String getValue(XMLAttribute attribute) {
		for (XMLAttributeInstance i : m_attributeInstances) {
			if (i.getAttribute().equals(attribute)) {
				return i.getValue();
			}
		}
		throw new IllegalArgumentException("Unknown attribute '" + attribute.getName() + "' in element '" //$NON-NLS-1$ //$NON-NLS-2$
				+ getTag().getName() + "'"); //$NON-NLS-1$
	}

	/**
	 * @param attribute
	 * @return the value for the given {@link XMLAttribute attribute}, or the default if one has
	 *         explicitly been set, otherwise null.
	 * @see #getValue(XMLAttribute)
	 * @throws IllegalArgumentException
	 *             if this tag has no such attribute
	 */
	public String getExplicitValue(XMLAttribute attribute) {
		for (XMLAttributeInstance i : m_attributeInstances) {
			if (i.getAttribute().equals(attribute)) {
				return i.getExplicitValue();
			}
		}
		throw new IllegalArgumentException("Unknown attribute '" + attribute.getName() + "' in element '" //$NON-NLS-1$ //$NON-NLS-2$
				+ getTag().getName() + "'"); //$NON-NLS-1$
	}

	/**
	 * Set the value of {@code attribute}, either to the specified value, or implicitly to the
	 * default value. Specifying null will effectively remove the attribute from the text
	 * representation of this element.
	 *
	 * @param newValue
	 *            the desired value, or null to implicitly use the default.
	 * @return true iff the attribute changed in any way
	 */
	public boolean setValue(XMLAttribute attribute, String newValue) {
		for (XMLAttributeInstance i : m_attributeInstances) {
			if (i.getAttribute().equals(attribute)) {
				return i.setValue(newValue);
			}
		}
		throw new IllegalArgumentException("Unknown attribute '" + attribute.getName() + "' in element '" //$NON-NLS-1$ //$NON-NLS-2$
				+ getTag().getName() + "'"); //$NON-NLS-1$
	}

	public List<XMLTagInstance> getTagsInstances(XMLTag tag) {
		List<XMLTagInstance> elements = new ArrayList<>();
		for (XMLTagInstance element : m_childElements) {
			if (element.getTag() == tag) {
				elements.add(element);
			}
		}
		return elements;
	}

	public XMLTagInstance findTagWithAttribute(XMLTag tag, XMLAttribute attribute, String value) {
		for (XMLTagInstance element : m_childElements) {
			if ((element.getTag() == tag) && value.equals(element.getValue(attribute))) {
				return element;
			}
		}
		return null;
	}

	public XMLTagInstance findNestedTagWithAttribute(
		XMLTag recurseTag, XMLTag targetTag, XMLAttribute attribute, String value) {
		for (XMLTagInstance element : m_childElements) {
			XMLTag tag = element.getTag();
			if (tag == targetTag) {
				if (value.equals(element.getValue(attribute))) {
					return element;
				}
			} else if (tag == recurseTag) {
				XMLTagInstance result = element.findNestedTagWithAttribute(recurseTag, targetTag, attribute, value);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}

	public String getContent() {
		if (hasContent()) {
			return m_content;
		} else {
			return ""; //$NON-NLS-1$
		}
	}
}
