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

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServerConnection;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;

import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.subscription.internal.AttributeValueToolkit;
import org.openjdk.jmc.ui.common.util.Environment;

/**
 * Delegating MBean server connection. Provides support for synthetic attributes.
 */
public final class MCMBeanServerConnection implements MBeanServerConnection {
	private final SyntheticAttributeRepository attributeRepository;
	private final SyntheticNotificationRepository notificationRepository;
	private final MBeanServerConnection delegate;

	public MCMBeanServerConnection(MBeanServerConnection delegate) {
		this.delegate = delegate;
		attributeRepository = new SyntheticAttributeRepository(this);
		notificationRepository = new SyntheticNotificationRepository(this);
		attributeRepository.initializeFromExtensions();
		notificationRepository.initializeFromExtensions();
	}

	/**
	 * All MBeans are attempted to be created in the delegate.
	 */
	@Override
	public ObjectInstance createMBean(String className, ObjectName name)
			throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException,
			NotCompliantMBeanException, IOException {
		return delegate.createMBean(className, name);
	}

	/**
	 * All MBeans are attempted to be created in the delegate.
	 */
	@Override
	public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName)
			throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException,
			NotCompliantMBeanException, InstanceNotFoundException, IOException {
		return delegate.createMBean(className, name, loaderName);
	}

	/**
	 * All MBeans are attempted to be created in the delegate.
	 */
	@Override
	public ObjectInstance createMBean(String className, ObjectName name, Object[] params, String[] signature)
			throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException,
			NotCompliantMBeanException, IOException {
		return delegate.createMBean(className, name, params, signature);
	}

	/**
	 * All MBeans are attempted to be created in the delegate.
	 */
	@Override
	public ObjectInstance createMBean(
		String className, ObjectName name, ObjectName loaderName, Object[] params, String[] signature)
			throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException,
			NotCompliantMBeanException, InstanceNotFoundException, IOException {
		return delegate.createMBean(className, name, loaderName, params, signature);
	}

	/**
	 * Synthetics cannot be unregistered.
	 */
	@Override
	public void unregisterMBean(ObjectName name)
			throws InstanceNotFoundException, MBeanRegistrationException, IOException {
		delegate.unregisterMBean(name);
	}

	/**
	 * First attempt to get real object instance. If no joy, try synthetics.
	 */
	@Override
	public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException, IOException {
		ObjectInstance instance = delegate.getObjectInstance(name);
		if (instance == null) {
			instance = attributeRepository.getObjectInstance(name);
		}
		if (instance == null) {
			instance = notificationRepository.getObjectInstance(name);
		}
		return instance;
	}

	@Override
	public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) throws IOException {
		return unify(delegate.queryMBeans(name, query),
				unify(attributeRepository.queryMBeans(name, query), notificationRepository.queryMBeans(name, query)));
	}

	@Override
	public Set<ObjectName> queryNames(ObjectName name, QueryExp query) throws IOException {
		return unify(delegate.queryNames(name, query),
				unify(attributeRepository.queryNames(name, query), notificationRepository.queryNames(name, query)));
	}

	@Override
	public boolean isRegistered(ObjectName name) throws IOException {
		return delegate.isRegistered(name) || attributeRepository.isRegistered(name)
				|| notificationRepository.isRegistered(name);
	}

	@Override
	public Integer getMBeanCount() throws IOException {
		return queryNames(null, null).size();
	}

	@Override
	public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException,
			InstanceNotFoundException, ReflectionException, IOException {
		if (attributeRepository.hasAttribute(name, attribute)) {
			return attributeRepository.getAttribute(name, attribute);
		}
		return AttributeValueToolkit.getAttribute(delegate, name, attribute);
	}

	@Override
	public AttributeList getAttributes(ObjectName name, String[] attributes)
			throws InstanceNotFoundException, ReflectionException, IOException {
		AttributeList resultList = attributeRepository.getExistingAttributes(name, attributes);
		if (resultList.size() == attributes.length) {
			return resultList;
		}
		if (resultList.size() > 0) {
			attributes = filterFoundAttributes(attributes, resultList);
			resultList.addAll(AttributeValueToolkit.getAttributes(delegate, name, Arrays.asList(attributes)));
			return resultList;
		}
		return AttributeValueToolkit.getAttributes(delegate, name, Arrays.asList(attributes));
	}

	private String[] filterFoundAttributes(String[] attributes, AttributeList attributeList) {
		// Arrays.asList(...) returns a list that does not support remove(...)
		List<String> filteredAttributes = new ArrayList<>(Arrays.asList(attributes));
		for (Object obj : attributeList) {
			Attribute attribute = (Attribute) obj;
			filteredAttributes.remove(attribute.getName());
		}
		attributes = filteredAttributes.toArray(new String[filteredAttributes.size()]);
		return attributes;
	}

	@Override
	public void setAttribute(ObjectName name, Attribute attribute)
			throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException,
			MBeanException, ReflectionException, IOException {
		if (attributeRepository.hasAttribute(name, attribute.getName())) {
			attributeRepository.setAttribute(name, attribute);
		} else {
			delegate.setAttribute(name, attribute);
		}
	}

	@Override
	public AttributeList setAttributes(ObjectName name, AttributeList attributes)
			throws InstanceNotFoundException, ReflectionException, IOException {
		AttributeList resultList = attributeRepository.setExistingAttributes(name, attributes);
		if (resultList.size() == attributes.size()) {
			return resultList;
		}
		if (resultList.size() > 0) {
			attributes = filterFoundAttributes(attributes, resultList);
			resultList.addAll(delegate.setAttributes(name, attributes));
			return resultList;
		}
		return delegate.setAttributes(name, attributes);
	}

	private AttributeList filterFoundAttributes(AttributeList attributes, AttributeList resultList) {
		AttributeList filteredAttributes = new AttributeList();
		for (Object obj : attributes) {
			boolean add = true;
			Attribute attribute = (Attribute) obj;
			for (Object result : resultList) {
				Attribute resultAttribute = (Attribute) result;
				if (attribute.getName().equals(resultAttribute.getName())) {
					add = false;
					break;
				}
			}
			if (add) {
				filteredAttributes.add(attribute);
			}
		}
		return filteredAttributes;
	}

	
	@Override
	public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature)
			throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
		logInvokeMessage(name, operationName, params);
		return delegate.invoke(name, operationName, params, signature);
	}

	private void logInvokeMessage(ObjectName name, String operationName, Object[] params) {
		if (Environment.isDebug()) {
			if (params == null) {
				RJMXPlugin.getDefault().getLogger().log(Level.FINE,
						String.format("Invoking operation %s on %s", operationName, name)); //$NON-NLS-1$
			} else {
				RJMXPlugin.getDefault().getLogger().log(Level.FINE,
						String.format("Invoking operation %s on %s, with parameters %s", operationName, name, //$NON-NLS-1$
								toString(params)));
			}
		}
	}

	private Object toString(Object[] params) {
		StringBuilder builder = new StringBuilder();
		for (Object o : params) {
			builder.append(o.toString());
			builder.append(' ');
		}
		return builder.toString();
	}

	/**
	 * Don't have a default domain for synthetics.
	 */
	@Override
	public String getDefaultDomain() throws IOException {
		return delegate.getDefaultDomain();
	}

	@Override
	public String[] getDomains() throws IOException {
		return unify(unify(notificationRepository.getDomains(), attributeRepository.getDomains(), String.class),
				delegate.getDomains(), String.class);
	}

	@Override
	public void addNotificationListener(
		ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback)
			throws InstanceNotFoundException, IOException {
		if (notificationRepository.hasNotification(name)) {
			notificationRepository.addNotificationListener(name, listener, filter, handback);
			tryRegisteringListener(name, listener, filter, handback);
		} else {
			delegate.addNotificationListener(name, listener, filter, handback);
		}
	}

	private void tryRegisteringListener(
		ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback)
			throws IOException, InstanceNotFoundException {
		try {
			if (!delegate.isRegistered(name)) {
				return;
			}
			MBeanNotificationInfo[] infos = delegate.getMBeanInfo(name).getNotifications();
			if (infos != null && infos.length > 0) {
				try {
					delegate.addNotificationListener(name, listener, filter, handback);
				} catch (Exception e) {
					// Silently ignore, veni, vidi and lost.
				}
			}
		} catch (IntrospectionException e) {
			RJMXPlugin.getDefault().getLogger().log(Level.WARNING, "Could not add listener!", e); //$NON-NLS-1$
		} catch (ReflectionException e) {
			RJMXPlugin.getDefault().getLogger().log(Level.WARNING, "Could not add listener!", e); //$NON-NLS-1$
		}
	}

	@Override
	public void addNotificationListener(
		ObjectName name, ObjectName listener, NotificationFilter filter, Object handback)
			throws InstanceNotFoundException, IOException {
		if (notificationRepository.hasNotification(name)) {
			notificationRepository.addNotificationListener(name, listener, filter, handback);
			tryRegisteringListener(name, listener, filter, handback);
		} else {
			delegate.addNotificationListener(name, listener, filter, handback);
		}
	}

	private void tryRegisteringListener(
		ObjectName name, ObjectName listener, NotificationFilter filter, Object handback)
			throws IOException, InstanceNotFoundException {
		try {
			if (!delegate.isRegistered(name)) {
				return;
			}
			MBeanNotificationInfo[] infos = delegate.getMBeanInfo(name).getNotifications();
			if (infos != null && infos.length > 0) {
				try {
					delegate.addNotificationListener(name, listener, filter, handback);
				} catch (Exception e) {
					// Silently ignore, veni, vidi and lost.
				}
			}
		} catch (IntrospectionException e) {
			RJMXPlugin.getDefault().getLogger().log(Level.WARNING, "Could not add listener!", e); //$NON-NLS-1$
		} catch (ReflectionException e) {
			RJMXPlugin.getDefault().getLogger().log(Level.WARNING, "Could not add listener!", e); //$NON-NLS-1$
		}
	}

	@Override
	public void removeNotificationListener(ObjectName name, ObjectName listener)
			throws InstanceNotFoundException, ListenerNotFoundException, IOException {
		if (notificationRepository.hasNotification(name)) {
			notificationRepository.removeNotificationListener(name, listener);
			tryRemoveListener(name, listener);
		} else {
			delegate.removeNotificationListener(name, listener);
		}
	}

	private void tryRemoveListener(ObjectName name, ObjectName listener) throws IOException, InstanceNotFoundException {
		try {
			if (!delegate.isRegistered(name)) {
				return;
			}
			MBeanNotificationInfo[] infos = delegate.getMBeanInfo(name).getNotifications();
			if (infos != null && infos.length > 0) {
				try {
					delegate.removeNotificationListener(name, listener);
				} catch (Exception e) {
					// Silently ignore, veni, vidi and lost.
				}
			}
		} catch (IntrospectionException e) {
			RJMXPlugin.getDefault().getLogger().log(Level.WARNING, "Could not add listener!", e); //$NON-NLS-1$
		} catch (ReflectionException e) {
			RJMXPlugin.getDefault().getLogger().log(Level.WARNING, "Could not add listener!", e); //$NON-NLS-1$
		}
	}

	@Override
	public void removeNotificationListener(
		ObjectName name, ObjectName listener, NotificationFilter filter, Object handback)
			throws InstanceNotFoundException, ListenerNotFoundException, IOException {
		if (notificationRepository.hasNotification(name)) {
			notificationRepository.removeNotificationListener(name, listener, filter, handback);
			tryRemoveListener(name, listener, filter, handback);
		} else {
			delegate.removeNotificationListener(name, listener, filter, handback);
		}
	}

	private void tryRemoveListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback)
			throws IOException, InstanceNotFoundException {
		try {
			if (!delegate.isRegistered(name)) {
				return;
			}
			MBeanNotificationInfo[] infos = delegate.getMBeanInfo(name).getNotifications();
			if (infos != null && infos.length > 0) {
				try {
					delegate.removeNotificationListener(name, listener);
				} catch (Exception e) {
					// Silently ignore, veni, vidi and lost.
				}
			}
		} catch (IntrospectionException e) {
			RJMXPlugin.getDefault().getLogger().log(Level.WARNING, "Could not add listener!", e); //$NON-NLS-1$
		} catch (ReflectionException e) {
			RJMXPlugin.getDefault().getLogger().log(Level.WARNING, "Could not add listener!", e); //$NON-NLS-1$
		}
	}

	@Override
	public void removeNotificationListener(ObjectName name, NotificationListener listener)
			throws InstanceNotFoundException, ListenerNotFoundException, IOException {
		if (notificationRepository.hasNotification(name)) {
			notificationRepository.removeNotificationListener(name, listener);
			tryRemoveListener(name, listener);
		} else {
			delegate.removeNotificationListener(name, listener);
		}
	}

	private void tryRemoveListener(ObjectName name, NotificationListener listener)
			throws IOException, InstanceNotFoundException {
		try {
			if (!delegate.isRegistered(name)) {
				return;
			}
			MBeanNotificationInfo[] infos = delegate.getMBeanInfo(name).getNotifications();
			if (infos != null && infos.length > 0) {
				try {
					delegate.removeNotificationListener(name, listener);
				} catch (Exception e) {
					// Silently ignore, veni, vidi and lost.
				}
			}
		} catch (IntrospectionException e) {
			RJMXPlugin.getDefault().getLogger().log(Level.WARNING, "Could not add listener!", e); //$NON-NLS-1$
		} catch (ReflectionException e) {
			RJMXPlugin.getDefault().getLogger().log(Level.WARNING, "Could not add listener!", e); //$NON-NLS-1$
		}
	}

	@Override
	public void removeNotificationListener(
		ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback)
			throws InstanceNotFoundException, ListenerNotFoundException, IOException {
		if (notificationRepository.hasNotification(name)) {
			notificationRepository.removeNotificationListener(name, listener, filter, handback);
			tryRemoveListener(name, listener, filter, handback);
		} else {
			delegate.removeNotificationListener(name, listener, filter, handback);
		}
	}

	private void tryRemoveListener(
		ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback)
			throws IOException, InstanceNotFoundException {
		try {
			if (!delegate.isRegistered(name)) {
				return;
			}
			MBeanNotificationInfo[] infos = delegate.getMBeanInfo(name).getNotifications();
			if (infos != null && infos.length > 0) {
				try {
					delegate.removeNotificationListener(name, listener);
				} catch (Exception e) {
					// Silently ignore, veni, vidi and lost.
				}
			}
		} catch (IntrospectionException e) {
			RJMXPlugin.getDefault().getLogger().log(Level.WARNING, "Could not add listener!", e); //$NON-NLS-1$
		} catch (ReflectionException e) {
			RJMXPlugin.getDefault().getLogger().log(Level.WARNING, "Could not add listener!", e); //$NON-NLS-1$
		}
	}

	@Override
	public MBeanInfo getMBeanInfo(ObjectName name)
			throws InstanceNotFoundException, IntrospectionException, ReflectionException, IOException {
		boolean hasSyntheticAttribute = attributeRepository.isRegistered(name);
		boolean hasSyntheticNotification = notificationRepository.isRegistered(name);

		if (!hasSyntheticAttribute && !hasSyntheticNotification) {
			return delegate.getMBeanInfo(name);
		}

		return unifyWithDelegateMBeanInfo(name, unifySyntheticMBeanInfo(name));
	}

	private MBeanInfo unifySyntheticMBeanInfo(ObjectName name)
			throws IntrospectionException, InstanceNotFoundException, ReflectionException {
		if (!notificationRepository.isRegistered(name)) {
			return attributeRepository.getMBeanInfo(name);
		} else if (!attributeRepository.isRegistered(name)) {
			return notificationRepository.getMBeanInfo(name);
		}
		return unifyMBeanInfo(notificationRepository.getMBeanInfo(name), attributeRepository.getMBeanInfo(name));
	}

	private MBeanInfo unifyWithDelegateMBeanInfo(ObjectName name, MBeanInfo syntheticMBeanInfo)
			throws IOException, InstanceNotFoundException, IntrospectionException, ReflectionException {
		if (!delegate.isRegistered(name)) {
			return syntheticMBeanInfo;
		}
		return unifyMBeanInfo(syntheticMBeanInfo, delegate.getMBeanInfo(name));
	}

	private MBeanInfo unifyMBeanInfo(MBeanInfo synthetic, MBeanInfo real) {
		String description = real.getDescription();
		if (description == null || description.length() == 0) {
			description = synthetic.getDescription();
		}
		if (description != null && description.length() > 0) {
			description += " [Extended with synthetic attribute(s)]"; //$NON-NLS-1$
		} else {
			description = null;
		}
		String className = real.getClassName();
		return new MBeanInfo(className, description,
				unify(synthetic.getAttributes(), real.getAttributes(), MBeanAttributeInfo.class),
				real.getConstructors(), real.getOperations(),
				unify(synthetic.getNotifications(), real.getNotifications(), MBeanNotificationInfo.class));
	}

	@Override
	public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException, IOException {
		if (delegate.isRegistered(name)) {
			return delegate.isInstanceOf(name, className);
		}
		if (attributeRepository.isRegistered(name)) {
			return attributeRepository.isInstanceOf(name, className);
		}
		if (notificationRepository.isRegistered(name)) {
			return notificationRepository.isInstanceOf(name, className);
		}
		return delegate.isInstanceOf(name, className);
	}

	private <T> T[] unify(T[] masterArray, T[] slaveArray, Class<T> arrayType) {
		List<T> unified = new ArrayList<>(masterArray.length + slaveArray.length);
		for (T master : masterArray) {
			unified.add(master);
		}
		for (T slave : slaveArray) {
			if (!unified.contains(slave)) {
				unified.add(slave);
			}
		}
		T[] unifiedArray = createArray(arrayType, unified.size());
		return unified.toArray(unifiedArray);
	}

	private <T> T[] createArray(Class<T> componentType, int size) {
		@SuppressWarnings("unchecked")
		T[] array = (T[]) Array.newInstance(componentType, size);
		return array;
	}

	private <T> Set<T> unify(Set<T> masterSet, Set<T> slaveSet) {
		Set<T> unifiedSet = new HashSet<>();
		unifiedSet.addAll(masterSet);
		unifiedSet.addAll(slaveSet);
		return unifiedSet;
	}

	public void dispose() {
		notificationRepository.dispose();
		attributeRepository.dispose();
	}
}
