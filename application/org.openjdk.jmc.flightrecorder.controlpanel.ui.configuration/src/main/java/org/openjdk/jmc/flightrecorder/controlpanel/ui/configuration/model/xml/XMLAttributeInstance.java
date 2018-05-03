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

public final class XMLAttributeInstance {
	private final XMLAttribute m_attribute;

	/**
	 * The value of this attribute, or null to mean that it hasn't been set explicitly and the
	 * default value should be used.
	 */
	private String m_value;

	XMLAttributeInstance(XMLAttribute attribute) {
		m_attribute = attribute;
		m_value = null;
	}

	public XMLAttribute getAttribute() {
		return m_attribute;
	}

	/**
	 * Does this attribute have its default value because none was set explicitly? This is typically
	 * used when outputting the XML as text, to omit implicit attributes, thereby enabling
	 * preservation of the text representation.
	 *
	 * @return true iff the value of this attribute has not been set explicitly.
	 */
	public boolean isImplicitDefault() {
		return (m_value == null);
	}

	/**
	 * @return the current value of this attribute, possibly the default, never null.
	 */
	public String getValue() {
		if (m_value != null) {
			return m_value;
		}
		String def = m_attribute.getDefaultValue();
		return (def != null) ? def : ""; //$NON-NLS-1$
	}

	/**
	 * Similar to {@link #getValue()}, but returns null if the attribute hasn't explicitly been
	 * specified, and the attribute doesn't have a default value.
	 *
	 * @return the current value of this attribute, or the default if one has explicitly been set,
	 *         otherwise null.
	 */
	public String getExplicitValue() {
		return (m_value != null) ? m_value : m_attribute.getDefaultValue();
	}

	/**
	 * Set the value of this attribute, either to the specified value, or implicitly to the default
	 * value. Specifying null will effectively remove the attribute from the text representation of
	 * its parent element.
	 *
	 * @param value
	 *            the desired value, or null to implicitly use the default.
	 * @return true iff this instance changed in any way
	 */
	public boolean setValue(String value) {
		// FIXME: Validate against allowed values
		if ((m_value == null) ? (value != null) : !m_value.equals(value)) {
			m_value = value;
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return m_attribute.getName() + "=\"" + getValue() + '"'; //$NON-NLS-1$
	}
}
