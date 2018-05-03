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
package org.openjdk.jmc.rjmx.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.Descriptor;
import javax.management.DynamicMBean;
import javax.management.ImmutableDescriptor;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ReflectionException;

public class SyntheticAttributeMBeanEntry implements DynamicMBean {
	private final Map<String, SyntheticAttributeEntry> attributeMap = new HashMap<>();
	private final MBeanServerConnection connection;

	public SyntheticAttributeMBeanEntry(MBeanServerConnection connection) {
		this.connection = connection;
	}

	public void addSyntheticAttribute(SyntheticAttributeEntry entry) {
		attributeMap.put(entry.getAttributeDescriptor().getDataPath(), entry);
	}

	public void removeSyntheticAttribute(SyntheticAttributeEntry entry) {
		attributeMap.remove(entry.getAttributeDescriptor().getDataPath());
	}

	Collection<SyntheticAttributeEntry> getSyntheticAttributes() {
		return attributeMap.values();
	}

	@Override
	public Object getAttribute(String attribute) throws MBeanException, ReflectionException {
		return attributeMap.get(attribute).getAttribute().getValue(connection);
	}

	@Override
	public void setAttribute(Attribute attribute)
			throws InvalidAttributeValueException, MBeanException, ReflectionException {
		attributeMap.get(attribute.getName()).getAttribute().setValue(connection, attribute.getValue());
	}

	@Override
	public AttributeList getAttributes(String[] attributes) {
		AttributeList al = new AttributeList();
		for (String attribute : attributes) {
			try {
				Attribute a = new Attribute(attribute, attributeMap.get(attribute).getAttribute().getValue(connection));
				al.add(a);
			} catch (MBeanException e) {
				// Skip the attribute that couldn't be obtained.
			} catch (ReflectionException e) {
				// Skip the attribute that couldn't be obtained.
			}
		}
		return al;
	}

	@Override
	public AttributeList setAttributes(AttributeList attributes) {
		AttributeList result = new AttributeList();
		for (Object a : attributes) {
			try {
				Attribute attribute = (Attribute) a;
				setAttribute(attribute);
				result.add(attribute);
			} catch (InvalidAttributeValueException e) {
				// Skip the attribute that couldn't be obtained.
			} catch (MBeanException e) {
				// Skip the attribute that couldn't be obtained.
			} catch (ReflectionException e) {
				// Skip the attribute that couldn't be obtained.
			}
		}
		return result;
	}

	@Override
	public Object invoke(String actionName, Object[] params, String[] signature)
			throws MBeanException, ReflectionException {
		throw new IllegalArgumentException("Not implemented yet!"); //$NON-NLS-1$
	}

	@Override
	public MBeanInfo getMBeanInfo() {
		return new MBeanInfo(getClass().getName(), "Dynamic Synthetic MBean", createAttributeInfos(), null, null, null); //$NON-NLS-1$
	}

	private MBeanAttributeInfo[] createAttributeInfos() {
		Collection<SyntheticAttributeEntry> entries = attributeMap.values();
		MBeanAttributeInfo[] attributeInfos = new MBeanAttributeInfo[entries.size()];
		int i = 0;
		for (SyntheticAttributeEntry entry : attributeMap.values()) {
			attributeInfos[i++] = createAttributeInfo(entry);
		}
		return attributeInfos;
	}

	private MBeanAttributeInfo createAttributeInfo(SyntheticAttributeEntry entry) {
		return new MBeanAttributeInfo(entry.getAttributeDescriptor().getDataPath(), entry.getType(),
				entry.getDescription(), entry.isReadable(), entry.isWriteable(), entry.isIs(), createDescriptor());
	}

	private Descriptor createDescriptor() {
		return new ImmutableDescriptor("synthetic=true"); //$NON-NLS-1$
	}

	public boolean hasDataPath(String dataPath) {
		// FIXME: Perform proper lookup
		for (SyntheticAttributeEntry entry : attributeMap.values()) {
			if (entry.getAttributeDescriptor().getDataPath().equals(dataPath)) {
				return true;
			}
		}
		return false;
	}
}
