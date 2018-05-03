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
package org.openjdk.jmc.rjmx.triggers.fields.internal;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import org.w3c.dom.Element;

import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.util.XmlToolkit;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.triggers.ISetting;

/**
 * This class is responsible for capturing configuration options. Clients should not subclass
 */
abstract public class Field implements ISetting {
	private final String m_id;
	private final String m_name;
	private final String m_description;
	private String m_value;
	private final ArrayList<FieldValueChangeListener> m_fieldValueChangeListener = new ArrayList<>();

	protected Field(String id, String name, String defaultValue, String description) throws Exception {
		if (id == null) {
			throw new IllegalArgumentException("Id can't be null"); //$NON-NLS-1$
		}
		if (name == null) {
			throw new IllegalArgumentException("Name can't be null"); //$NON-NLS-1$
		}
		if (defaultValue == null) {
			throw new IllegalArgumentException("Default value can't be null"); //$NON-NLS-1$
		}
		if (description == null) {
			throw new IllegalArgumentException("Description can't be null"); //$NON-NLS-1$
		}

		m_id = id;
		initDefaultValue(defaultValue);
		initDefaultPreferenceValue();
		m_name = name;
		m_description = description;
	}

	abstract void initDefaultValue(String defaultValue);

	public void initDefaultPreferenceValue() {
		if (m_id != null) {
			String prefDefault = RJMXPlugin.getDefault().getRJMXPreferences().get(m_id, null);
			if (prefDefault != null && prefDefault.trim().length() != 0) {
				setValue(prefDefault);
			}
		}
	}

	public interface FieldValueChangeListener {
		public void onChange(Field freshField);
	}

	public synchronized void addFieldValueListener(FieldValueChangeListener listener) {
		m_fieldValueChangeListener.add(listener);
	}

	public synchronized boolean removeFieldValueListener(FieldValueChangeListener listener) {
		return m_fieldValueChangeListener.remove(listener);
	}

	public synchronized void updateListener() {
		Iterator<FieldValueChangeListener> i = m_fieldValueChangeListener.iterator();
		while (i.hasNext()) {
			FieldValueChangeListener fcl = i.next();
			fcl.onChange(this);
		}
	}

	/**
	 * Returns the raw text string value of the field.
	 *
	 * @return the raw field value.
	 */
	public synchronized String getValue() {
		return m_value;
	}

	/**
	 * Sets given value to this field.
	 *
	 * @param value
	 *            the value to store, can never store <tt>null</tt>
	 * @return <tt>true</tt> if the value could stored, <tt>false</tt> otherwise
	 */
	public synchronized boolean setValue(String value) {
		if (value == null) {
			return false;
		}

		String newValue;
		try {
			newValue = parsedValue(value);
		} catch (Exception nfe) {
			return false;
		}
		if (newValue == null) {
			return false;
		}

		if (!equalsValue(newValue)) {
			putValue(newValue);
			updateListener();
		}

		return true;
	}

	/**
	 * Sets the field value without checking that it can be parsed.
	 *
	 * @param value
	 *            the value to store, can never store <tt>null</tt>
	 * @return <tt>true</tt> if the value could stored, <tt>false</tt> otherwise
	 */
	public synchronized boolean setUncheckedValue(String value) {
		if (value == null) {
			return false;
		}

		if (!equalsValue(value)) {
			putValue(value);
			updateListener();
		}

		return true;
	}

	/**
	 * Writes the new value to this field. Should only be called from {@link #setValue(String)}.
	 *
	 * @param newValue
	 *            the new, already parsed and checked, value to store in this field
	 */
	protected void putValue(String newValue) {
		m_value = newValue;
	}

	/**
	 * Checks if the current raw value of this field matches the parameter value.
	 *
	 * @param value
	 *            the value to check this field against
	 * @return <tt>true</tt> if given value equals field value, <tt>false</tt> otherwise
	 */
	protected boolean equalsValue(String value) {
		String currentValue = getValue();
		if (value == null) {
			return currentValue == null;
		}
		return value.equals(currentValue);
	}

	/**
	 * Returns whether given value is a valid value for this field.
	 *
	 * @param value
	 *            the value to test
	 * @return <tt>true</tt> if the value is non-<tt>null</tt> and valid, <tt>false</tt> otherwise
	 */
	public boolean validateValue(String value) {
		try {
			return parsedValue(value) != null;
		} catch (Exception nfe) {
			return false;
		}
	}

	/**
	 * Returns a parsed string representation for given value.
	 *
	 * @param value
	 *            the value to parse
	 * @return a non-<tt>null</tt> string representing to value if valid, or <tt>null</tt>
	 * @throws Exception
	 *             if parsing raised an exception
	 */
	abstract String parsedValue(String value) throws Exception;

	public void initializeFromXml(Element node) {
		setValue(XmlToolkit.getSetting(node, getId(), "")); //$NON-NLS-1$
	}

	public void exportToXml(Element node) {
		String object = getValue();
		try {
			XmlToolkit.setSetting(node, getId(), object);
		} catch (Exception e) {
			System.out.println('|' + object + '|');
		}
	}

	@Override
	public String getId() {
		return m_id;
	}

	@Override
	public String getDescription() {
		return m_description;
	}

	@Override
	public String getName() {
		return m_name;
	}

	@Override
	public Date getDate() {
		return null;
	}

	@Override
	public Date getDateTime() {
		return null;
	}

	@Override
	public String getString() {
		return null;
	}

	@Override
	public Integer getInteger() {
		return null;
	}

	@Override
	public String getFileName() {
		return null;
	}

	@Override
	public Float getFloat() {
		return null;
	}

	@Override
	public Boolean getBoolean() {
		return null;
	}

	@Override
	public Long getLong() {
		return null;
	}

	@Override
	public String getPassword() {
		return null;
	}

	@Override
	public IQuantity getQuantity() {
		return null;
	}
}
