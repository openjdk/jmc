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
package org.openjdk.jmc.rjmx.subscription.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ReflectionException;

import org.openjdk.jmc.rjmx.IPropertySyntheticAttribute;
import org.openjdk.jmc.rjmx.subscription.MRI;

/**
 * Abstract base class for synthetic property attributes that needs to retrieve values from MBeans
 * having attribute descriptors as properties. Also supports a "factor" property that can optional
 * be used. The value if present must be a double.
 */
public abstract class AbstractPropertySyntheticAttribute extends AbstractSyntheticAttribute
		implements IPropertySyntheticAttribute {

	private Map<String, Object> m_properties;

	@Override
	public void setProperties(Map<String, Object> values) {
		m_properties = values;
	}

	/**
	 * Checks if the attribute denoted by the property key is present.
	 *
	 * @param connection
	 *            the connection to retrieve the value from
	 * @param key
	 *            which property to use as an attribute descriptor
	 * @return <tt>true</tt> if attribute is available, <tt>false<tt> otherwise
	 */
	protected boolean hasResolvedAttribute(MBeanServerConnection connection, String key) {
		Object value = m_properties.get(key);
		if (value == null) {
			return false;
		}
		try {
			MRI attribute = MRI.createFromQualifiedName(value.toString());
			String attributeName = AttributeValueToolkit.getAttributeName(attribute.getDataPath());
			MBeanInfo info = connection.getMBeanInfo(attribute.getObjectName());
			for (MBeanAttributeInfo attributeInfo : info.getAttributes()) {
				if (attributeInfo.getName().equals(attributeName)) {
					return true;
				}
			}
		} catch (Exception e) {
		}
		return false;
	}

	@Override
	public void setValue(MBeanServerConnection connection, Object value) {
		// by default no value can be set
	}

	/**
	 * Retrieves several attribute values from a connection.
	 *
	 * @param connection
	 *            the connection to retrieve the value from
	 * @param keys
	 *            which properties to use as attribute descriptors
	 * @return a map of property keys and values
	 * @throws MBeanException
	 *             wraps an exception thrown by the MBean's getter
	 * @throws ReflectionException
	 *             wraps an exception thrown while trying to invoke the getter
	 */
	protected Map<String, Object> getPropertyAttributes(MBeanServerConnection connection, String[] keys)
			throws ReflectionException, MBeanException {
		Map<MRI, String> attributeKeyMap = new HashMap<>();
		List<MRI> attributes = new ArrayList<>();
		for (String key : keys) {
			MRI attribute = lookupAttribute(key);
			if (attribute != null) {
				attributeKeyMap.put(attribute, key);
				attributes.add(attribute);
			}
		}
		Map<MRI, Object> values;
		try {
			values = AttributeValueToolkit.getAttributes(connection, attributes);
		} catch (InstanceNotFoundException e) {
			throw new MBeanException(e);
		} catch (IOException e) {
			throw new MBeanException(e);
		}
		Map<String, Object> result = new HashMap<>();
		for (Entry<MRI, Object> entry : values.entrySet()) {
			result.put(attributeKeyMap.get(entry.getKey()), entry.getValue());
		}
		return result;
	}

	private MRI lookupAttribute(String key) {
		Object value = getProperty(key);
		if (value == null) {
			return null;
		}
		return MRI.createFromQualifiedName(value.toString());
	}

	/**
	 * Retrieves a property from the property map.
	 *
	 * @param key
	 *            the key to look up
	 * @return the value of the key or <tt>null</tt> if not available
	 */
	protected Object getProperty(String key) {
		return m_properties.get(key);
	}
}
