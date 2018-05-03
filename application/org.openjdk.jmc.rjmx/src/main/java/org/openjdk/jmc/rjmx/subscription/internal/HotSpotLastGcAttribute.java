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

import java.util.Set;

import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;

/**
 * An aggregator for the LastGcInfo attribute found in GarbageCollector MBeans. On initialization it
 * will query all LastGcInfo attributes and save the latest one in a local cache. After this it will
 * set up a subscription for GC notifications from each MBean. Each received notification will
 * update the locally cached value.
 */
public class HotSpotLastGcAttribute extends AbstractSyntheticAttribute {

	private static final String COM_SUN_MANAGEMENT_GC_NOTIFICATION = "com.sun.management.gc.notification"; //$NON-NLS-1$

	private MBeanServerConnection m_connection = null;
	private Set<ObjectName> m_garbageCollectorMxBeans;
	private final NotificationListener m_listener;
	private CompositeData m_lastGcInfo = null;

	public HotSpotLastGcAttribute() {
		m_listener = createListener();
	}

	@Override
	public Object getValue(MBeanServerConnection connection) throws MBeanException, ReflectionException {
		return m_lastGcInfo;
	}

	@Override
	public void setValue(MBeanServerConnection connection, Object value) throws MBeanException, ReflectionException {
		// Ignore
	}

	@Override
	public void init(MBeanServerConnection connection) {
		super.init(connection);
		m_connection = connection;
		m_garbageCollectorMxBeans = SyntheticAttributeToolkit.lookupMxBeans(m_connection, "java.lang", //$NON-NLS-1$
				"GarbageCollector"); //$NON-NLS-1$
		m_lastGcInfo = findLatestGcInfo(m_garbageCollectorMxBeans);
		SyntheticAttributeToolkit.subscribeToNotifications(m_connection, m_listener, m_garbageCollectorMxBeans,
				COM_SUN_MANAGEMENT_GC_NOTIFICATION);
	}

	@Override
	public void stop() {
		SyntheticAttributeToolkit.unsubscribeFromNotifications(m_connection, m_listener, m_garbageCollectorMxBeans);
	}

	private NotificationListener createListener() {
		return new NotificationListener() {
			@Override
			public void handleNotification(Notification notification, Object handback) {
				synchronized (HotSpotLastGcAttribute.this) {
					m_lastGcInfo = (CompositeData) ((CompositeData) notification.getUserData()).get("gcInfo"); //$NON-NLS-1$
				}
			}
		};
	}

	private CompositeData findLatestGcInfo(Set<ObjectName> garbageCollectorMxBeans) {
		// Find the GC type with the latest end timestamp of its last GC
		long lastTimestamp = 0;
		CompositeData lastLastGc = null;
		for (ObjectName objectName : garbageCollectorMxBeans) {
			try {
				CompositeData lastGcInfo = (CompositeData) AttributeValueToolkit.getAttribute(m_connection, objectName,
						"LastGcInfo"); //$NON-NLS-1$
				if (lastGcInfo != null) {
					long endTime = (Long) lastGcInfo.get("endTime"); //$NON-NLS-1$
					if (endTime > lastTimestamp) {
						lastTimestamp = endTime;
						lastLastGc = lastGcInfo;
					}
				}
			} catch (Exception e) {
				// Ignore
			}
		}
		return lastLastGc;
	}

	@Override
	public boolean hasResolvedDependencies(MBeanServerConnection connection) {
		return !SyntheticAttributeToolkit.lookupMxBeans(connection, "java.lang", "GarbageCollector").isEmpty(); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
