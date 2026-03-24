/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.rjmx.common;

import java.io.IOException;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServerConnection;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;

/**
 * Simple decorator for inserting JMC specific handling to a server connection
 */
public class ConnectionDecorator implements MBeanServerConnection {

	private final MBeanServerConnection delegate;

	public ConnectionDecorator(final MBeanServerConnection delegate) {
		this.delegate = delegate;
	}

	public ObjectInstance createMBean(String className, ObjectName name)
			throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException,
			NotCompliantMBeanException, IOException {
		return delegate.createMBean(className, name);
	}

	public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName)
			throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException,
			NotCompliantMBeanException, InstanceNotFoundException, IOException {
		return delegate.createMBean(className, name, loaderName);
	}

	public ObjectInstance createMBean(String className, ObjectName name, Object[] params, String[] signature)
			throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException,
			NotCompliantMBeanException, IOException {
		return delegate.createMBean(className, name, params, signature);
	}

	public ObjectInstance createMBean(
		String className, ObjectName name, ObjectName loaderName, Object[] params, String[] signature)
			throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException,
			NotCompliantMBeanException, InstanceNotFoundException, IOException {
		return delegate.createMBean(className, name, loaderName, params, signature);
	}

	public void unregisterMBean(ObjectName name)
			throws InstanceNotFoundException, MBeanRegistrationException, IOException {
		delegate.unregisterMBean(name);
	}

	public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException, IOException {
		return delegate.getObjectInstance(name);
	}

	public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) throws IOException {
		return delegate.queryMBeans(name, query);
	}

	public Set<ObjectName> queryNames(ObjectName name, QueryExp query) throws IOException {
		return delegate.queryNames(name, query);
	}

	public boolean isRegistered(ObjectName name) throws IOException {
		return delegate.isRegistered(name);
	}

	public Integer getMBeanCount() throws IOException {
		return delegate.getMBeanCount();
	}

	public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException,
			InstanceNotFoundException, ReflectionException, IOException {
		return delegate.getAttribute(name, attribute);
	}

	public AttributeList getAttributes(ObjectName name, String[] attributes)
			throws InstanceNotFoundException, ReflectionException, IOException {
		return delegate.getAttributes(name, attributes);
	}

	public void setAttribute(ObjectName name, Attribute attribute)
			throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException,
			MBeanException, ReflectionException, IOException {
		delegate.setAttribute(name, attribute);
	}

	public AttributeList setAttributes(ObjectName name, AttributeList attributes)
			throws InstanceNotFoundException, ReflectionException, IOException {
		return delegate.setAttributes(name, attributes);
	}

	public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature)
			throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
		return delegate.invoke(name, operationName, params, signature);
	}

	public String getDefaultDomain() throws IOException {
		return delegate.getDefaultDomain();
	}

	public String[] getDomains() throws IOException {
		return delegate.getDomains();
	}

	public void addNotificationListener(
		ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback)
			throws InstanceNotFoundException, IOException {
		delegate.addNotificationListener(name, listener, filter, handback);
	}

	public void addNotificationListener(
		ObjectName name, ObjectName listener, NotificationFilter filter, Object handback)
			throws InstanceNotFoundException, IOException {
		delegate.addNotificationListener(name, listener, filter, handback);
	}

	public void removeNotificationListener(ObjectName name, ObjectName listener)
			throws InstanceNotFoundException, ListenerNotFoundException, IOException {
		delegate.removeNotificationListener(name, listener);
	}

	public void removeNotificationListener(
		ObjectName name, ObjectName listener, NotificationFilter filter, Object handback)
			throws InstanceNotFoundException, ListenerNotFoundException, IOException {
		delegate.removeNotificationListener(name, listener, filter, handback);
	}

	public void removeNotificationListener(ObjectName name, NotificationListener listener)
			throws InstanceNotFoundException, ListenerNotFoundException, IOException {
		delegate.removeNotificationListener(name, listener);
	}

	public void removeNotificationListener(
		ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback)
			throws InstanceNotFoundException, ListenerNotFoundException, IOException {
		delegate.removeNotificationListener(name, listener, filter, handback);
	}

	public MBeanInfo getMBeanInfo(ObjectName name)
			throws InstanceNotFoundException, IntrospectionException, ReflectionException, IOException {
		return delegate.getMBeanInfo(name);
	}

	public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException, IOException {
		return delegate.isInstanceOf(name, className);
	}

}
