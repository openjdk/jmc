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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Holds metadata about a tag attribute.
 */
public final class XMLAttribute extends XMLNode {
	private final Collection<String> m_validValues;
	private final String m_defaultValue;
	private final boolean m_required;

	/**
	 * Creates an attribute allowing any value, with no default value.
	 *
	 * @param name
	 *            the name of the attribute
	 * @param required
	 *            if the attribute is necessary
	 * @param type
	 *            which kind of node it is
	 */
	public XMLAttribute(String name, boolean required, XMLNodeType type) {
		this(name, required, type, (String) null);
	}

	/**
	 * Creates an attribute with a specified default value, but allowing any value.
	 *
	 * @param name
	 *            the name of the attribute
	 * @param required
	 *            if the attribute is necessary
	 * @param type
	 *            which kind of node it is
	 * @param defaultValue
	 *            the default value. (Any value will still be allowed.)
	 */
	public XMLAttribute(String name, boolean required, XMLNodeType type, String defaultValue) {
		super(name, type);

		m_validValues = Collections.emptyList();
		m_defaultValue = defaultValue;
		m_required = required;
	}

	/**
	 * Creates an attribute only allowing specified values, with the first value as default.
	 *
	 * @param name
	 *            the name of the attribute
	 * @param required
	 *            if the attribute is necessary
	 * @param type
	 *            which kind of node it is
	 * @param values
	 *            valid values. The first value in the array becomes the default value.
	 */
	public XMLAttribute(String name, boolean required, XMLNodeType type, String ... values) {
		super(name, type);

		assert values.length > 0;
		List<String> lowerCasedValues = new ArrayList<>(values.length);
		for (String value : values) {
			assert value != null;
			lowerCasedValues.add(value.trim().toLowerCase());
		}
		m_validValues = Collections.unmodifiableList(lowerCasedValues);
		m_defaultValue = lowerCasedValues.get(0);
		m_required = required;
	}

	public boolean isRequired() {
		return m_required;
	}

	/**
	 * A collection of valid values, all in lower case.
	 *
	 * @return a collection of valid values, or an empty collection if any value is valid.
	 */
	public Collection<String> getValidValues() {
		return m_validValues;
	}

	/**
	 * @return the default value, or null if none has been set explicitly
	 */
	public String getDefaultValue() {
		return m_defaultValue;
	}

	@Override
	public String toString() {
		return "Attribute: " + getName(); //$NON-NLS-1$
	}

	public String getLabel() {
		if (getName().length() >= 2) {
			return getName().substring(0, 1).toUpperCase() + getName().substring(1);
		} else {
			return getName();
		}
	}
}
