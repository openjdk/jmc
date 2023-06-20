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
import java.util.Set;
import java.util.logging.Level;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;

import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRI.Type;

/**
 * Toolkit for setting and retrieving values from a {@link MBeanServerConnection} using {@link MRI}.
 */
public final class AttributeValueToolkit {

	private AttributeValueToolkit() throws InstantiationException {
		throw new InstantiationException("Should not be instantiated!"); //$NON-NLS-1$
	}

	public static Object getAttribute(MBeanServerConnection connection, MRI attribute)
			throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException,
			IOException {
		assert attribute.getType() == Type.ATTRIBUTE;
		return getAttribute(connection, attribute.getObjectName(), attribute.getDataPath());
	}

	public static Object getAttribute(MBeanServerConnection connection, ObjectName name, String attribute)
			throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException,
			IOException {
		Object value = connection.getAttribute(name, getAttributeName(attribute));
		return lookupValue(attribute, value);
	}

	public static Map<MRI, Object> getAttributes(MBeanServerConnection connection, Iterable<MRI> attributes)
			throws InstanceNotFoundException, ReflectionException, IOException {
		if (attributes == null) {
			throw new IllegalArgumentException("Can't fetch anything! attributes == null!"); //$NON-NLS-1$
		}
		Map<MRI, Object> results = new HashMap<>();
		// coalesce all attributes belonging to the same MBean
		Map<ObjectName, List<String>> mbeanMap = new HashMap<>();
		for (MRI attribute : attributes) {
			assert attribute.getType() == Type.ATTRIBUTE;
			List<String> dataPathList = mbeanMap.get(attribute.getObjectName());
			if (dataPathList == null) {
				dataPathList = new ArrayList<>();
				mbeanMap.put(attribute.getObjectName(), dataPathList);
			}
			dataPathList.add(attribute.getDataPath());
		}
		for (Entry<ObjectName, List<String>> entry : mbeanMap.entrySet()) {
			AttributeList values = getAttributes(connection, entry.getKey(), entry.getValue());
			for (Object obj : values) {
				Attribute value = (Attribute) obj;
				results.put(new MRI(Type.ATTRIBUTE, entry.getKey(), value.getName()), value.getValue());
			}
		}
		return results;
	}

	public static AttributeList getAttributes(
		MBeanServerConnection connection, ObjectName name, Iterable<String> dataPaths)
			throws InstanceNotFoundException, ReflectionException, IOException {
		AttributeList results = new AttributeList();
		Map<String, List<String>> attributeMap = new HashMap<>();
		// coalesce all data paths to look up form a single attribute name
		for (String dataPath : dataPaths) {
			String attributeName = getAttributeName(dataPath);
			List<String> attributeList = attributeMap.get(attributeName);
			if (attributeList == null) {
				attributeList = new ArrayList<>();
				attributeMap.put(attributeName, attributeList);
			}
			attributeList.add(dataPath);
		}
		AttributeList values = connection.getAttributes(name, keysAsArray(attributeMap));
		if (values == null) {
			/*
			 * If the MBean implementor for some reason has not implemented the getAttributes method
			 * correctly (for example having a dynamic MBean with a Eclipse default implementation
			 * "return null") we should try to handle it as no attributes where read. This in turn
			 * will most probably force JMC to do a getAttribute invocation for each and every
			 * attribute. Still better than dying with a NPE.
			 */
			values = new AttributeList();
		}
		for (Object obj : values) {
			Attribute value = (Attribute) obj;
			for (String dataPath : attributeMap.get(value.getName())) {
				try {
					Attribute attribute = new Attribute(dataPath, lookupValue(dataPath, value.getValue()));
					results.add(attribute);
				} catch (AttributeNotFoundException e) {
					// skip attributes not found
				}
			}
		}
		return results;
	}

	private static String[] keysAsArray(Map<String, List<String>> attributeMap) {
		Set<String> attributesToGet = attributeMap.keySet();
		return attributesToGet.toArray(new String[attributesToGet.size()]);
	}

	// FIXME: Do a proper look up
	public static Object lookupValue(String dataPath, Object value) throws AttributeNotFoundException {
		String[] compositeParts = dataPath.split(MRI.VALUE_COMPOSITE_DELIMITER_STRING);
		if (compositeParts.length > 1) {
			if (value != null && !(value instanceof CompositeData)) {
				throw new AttributeNotFoundException("Unable to lookup up " + dataPath + " in non-composite value!"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			CompositeData compositeData = (CompositeData) value;
			if (compositeParts.length != 2) {
				String msg = "Could not resolve the composite data for " + dataPath + ". Number of components " //$NON-NLS-1$ //$NON-NLS-2$
						+ compositeParts.length + " is not supported yet."; //$NON-NLS-1$
				RJMXPlugin.getDefault().getLogger().log(Level.WARNING, msg);
				throw new AttributeNotFoundException(msg);
			}
			if (compositeData == null) {
				throw new AttributeNotFoundException("Attribute " + compositeParts[0] + " is null!"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (!compositeData.containsKey(compositeParts[1])) {
				throw new AttributeNotFoundException("Attribute " + dataPath + " not found!"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return compositeData.get(compositeParts[1]);
		} else {
			return value;
		}
	}

	// FIXME: Should do a proper match against existing attributes
	public static String getAttributeName(String dataPath) {
		if (dataPath == null) {
			return dataPath;
		}
		int index = dataPath.indexOf(MRI.VALUE_COMPOSITE_DELIMITER);
		if (index >= 0) {
			return dataPath.substring(0, index);
		}
		return dataPath;
	}

	public static void setAttribute(MBeanServerConnection connection, MRI attribute, Object value)
			throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException,
			MBeanException, ReflectionException, IOException {
		connection.setAttribute(attribute.getObjectName(), new Attribute(attribute.getDataPath(), value));
	}
}
