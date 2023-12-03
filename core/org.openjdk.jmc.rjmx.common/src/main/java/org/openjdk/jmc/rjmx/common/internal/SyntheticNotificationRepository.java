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
package org.openjdk.jmc.rjmx.common.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;

import org.openjdk.jmc.rjmx.common.RJMXCorePlugin;

public class SyntheticNotificationRepository {
	private final Map<ObjectName, Set<SyntheticNotificationEntry>> mbeans = new HashMap<>();
	private final MBeanServer server = MBeanServerFactory.newMBeanServer();
	private final MBeanServerConnection compoundServer;

	public SyntheticNotificationRepository(MBeanServerConnection compoundServer) {
		this.compoundServer = compoundServer;
	}

	public String[] getDomains() {
		return server.getDomains();
	}

	public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException {
		return server.getObjectInstance(name);
	}

	public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
		return server.queryMBeans(name, query);
	}

	public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
		return server.queryNames(name, query);
	}

	public boolean isRegistered(ObjectName name) {
		return server.isRegistered(name);
	}

	public Integer getMBeanCount() {
		return server.getMBeanCount();
	}

	public boolean hasNotification(ObjectName name) {
		Set<SyntheticNotificationEntry> entries = mbeans.get(name);
		return entries != null;
	}

	public MBeanInfo getMBeanInfo(ObjectName name)
			throws IntrospectionException, InstanceNotFoundException, ReflectionException {
		return server.getMBeanInfo(name);
	}

	public void addNotificationListener(
		ObjectName name, ObjectName listener, NotificationFilter filter, Object handback)
			throws InstanceNotFoundException, IOException {
		server.addNotificationListener(name, listener, filter, handback);
	}

	public void addNotificationListener(
		ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback)
			throws InstanceNotFoundException {
		server.addNotificationListener(name, listener, filter, handback);
	}

	public void removeNotificationListener(ObjectName name, ObjectName listener)
			throws InstanceNotFoundException, ListenerNotFoundException {
		server.removeNotificationListener(name, listener);
	}

	public void removeNotificationListener(
		ObjectName name, ObjectName listener, NotificationFilter filter, Object handback)
			throws InstanceNotFoundException, ListenerNotFoundException {
		server.removeNotificationListener(name, listener, filter, handback);
	}

	public void removeNotificationListener(ObjectName name, NotificationListener listener)
			throws InstanceNotFoundException, ListenerNotFoundException {
		server.removeNotificationListener(name, listener);
	}

	public void removeNotificationListener(
		ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback)
			throws InstanceNotFoundException, ListenerNotFoundException {
		server.removeNotificationListener(name, listener, filter, handback);
	}

	public void initializeFromExtensions(List<SyntheticNotificationEntry> notificationCandidates) {
		boolean hasResolved = true;
		while (!notificationCandidates.isEmpty() && hasResolved) {
			hasResolved = false;
			Iterator<SyntheticNotificationEntry> iterator = notificationCandidates.iterator();
			while (iterator.hasNext()) {
				SyntheticNotificationEntry candidate = iterator.next();
				if (candidate.getNotification().hasResolvedDependencies(compoundServer)) {
					hasResolved = true;
					iterator.remove();
					registerEntry(candidate);
				}
			}
		}
		registerMBeans();
	}

	private void registerMBeans() {
		for (Entry<ObjectName, Set<SyntheticNotificationEntry>> notificationEntry : mbeans.entrySet()) {
			ObjectName objectName = notificationEntry.getKey();
			try {
				SyntheticNotificationMBean mbean = createMBean(objectName);
				for (SyntheticNotificationEntry entry : notificationEntry.getValue()) {
					entry.getNotification().init(mbean);
					entry.getNotification().init(compoundServer, entry.getNotificationDescriptor().getDataPath(),
							entry.getMessage());
				}

			} catch (Exception e) {
				RJMXCorePlugin.getDefault().getLogger().log(Level.SEVERE,
						"Failed to register synthetic notification mbean " + objectName.toString(), e); //$NON-NLS-1$
			}
		}
	}

	private SyntheticNotificationMBean createMBean(ObjectName name) throws Exception {
		Set<SyntheticNotificationEntry> entries = mbeans.get(name);
		SyntheticNotificationMBean mbean = new SyntheticNotificationMBean(
				entries.toArray(new SyntheticNotificationEntry[entries.size()]));
		server.registerMBean(mbean, name);
		return mbean;
	}

	private void registerEntry(SyntheticNotificationEntry entry) {
		Set<SyntheticNotificationEntry> notificationEntries = mbeans
				.get(entry.getNotificationDescriptor().getObjectName());
		if (notificationEntries == null) {
			notificationEntries = new HashSet<>();
			mbeans.put(entry.getNotificationDescriptor().getObjectName(), notificationEntries);
		}
		notificationEntries.add(entry);
	}

	public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException {
		return server.isInstanceOf(name, className);
	}

	public void dispose() {
		for (Set<SyntheticNotificationEntry> notificationEntrySet : mbeans.values()) {
			for (SyntheticNotificationEntry entry : notificationEntrySet) {
				try {
					entry.getNotification().stop();
				} catch (Throwable t) {
					// silently ignore
				}
			}
		}
	}
}
