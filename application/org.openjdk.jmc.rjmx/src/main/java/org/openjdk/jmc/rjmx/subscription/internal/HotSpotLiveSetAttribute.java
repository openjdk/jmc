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
import javax.management.openmbean.TabularData;

import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRI.Type;

/**
 * Calculates and saves the heap live set usage after each major GC. On initialization it selects
 * the GC MBean that is most probably the major GC performer and calculates an initial value based
 * on the last GC.
 */
public class HotSpotLiveSetAttribute extends AbstractSyntheticAttribute {

	private static final String COM_SUN_MANAGEMENT_GC_NOTIFICATION = "com.sun.management.gc.notification"; //$NON-NLS-1$
	private MBeanServerConnection m_connection = null;
	private Set<ObjectName> m_garbageCollectorMxBeans;
	private final NotificationListener m_listener;
	private Double m_liveSet = null;

	public HotSpotLiveSetAttribute() {
		m_listener = createListener();
	}

	@Override
	public Object getValue(MBeanServerConnection connection) throws MBeanException, ReflectionException {
		return m_liveSet;
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
		setInitialValue(m_garbageCollectorMxBeans);
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
				CompositeData userData = (CompositeData) notification.getUserData();
				if (isOldCollection(userData)) {
					CompositeData gcInfo = (CompositeData) userData.get("gcInfo"); //$NON-NLS-1$
					m_liveSet = calculateLiveSet(gcInfo, m_connection);
				}
			}

		};
	}

	private void setInitialValue(Set<ObjectName> garbageCollectorMxBeans) {
		ObjectName majorGcObjectName = findMajorGcMbean(garbageCollectorMxBeans);
		if (majorGcObjectName != null) {
			try {
				CompositeData gcInfo = (CompositeData) AttributeValueToolkit.getAttribute(m_connection,
						majorGcObjectName, "LastGcInfo"); //$NON-NLS-1$
				if (gcInfo != null) {
					m_liveSet = calculateLiveSet(gcInfo, m_connection);
				}
			} catch (Exception e) {
				// Ignore
			}
		}
	}

	private ObjectName findMajorGcMbean(Set<ObjectName> garbageCollectorMxBeans) {
		// Find the GC type with the highest number of memory pools and assume that this is the one used for major GCs
		// TODO: Is there a better way of finding the GC used for major collections?
		int maxGcPools = 0;
		ObjectName majorGcObjectName = null;
		for (ObjectName objectName : garbageCollectorMxBeans) {
			try {
				Object poolNames = AttributeValueToolkit.getAttribute(m_connection, objectName, "MemoryPoolNames"); //$NON-NLS-1$
				if (poolNames instanceof String[]) {
					int gcPools = ((String[]) poolNames).length;
					if (gcPools > maxGcPools) {
						majorGcObjectName = objectName;
						maxGcPools = gcPools;
					}
				}
			} catch (Exception e) {
				// Ignore
			}
		}
		return majorGcObjectName;
	}

	static Double calculateLiveSet(CompositeData gcInfo, MBeanServerConnection connection) {
		return calculateLiveSet(lookupUsedHeap(gcInfo), connection);
	}

	static Double calculateLiveSet(long usedHeap, MBeanServerConnection connection) {
		long committedHeap = getCommittedHeap(connection);
		if (committedHeap > 0) {
			return (double) usedHeap / (double) committedHeap;
		} else {
			return null;
		}
	}

	private static boolean isOldCollection(CompositeData data) {
		return ((String) data.get("gcAction")).indexOf("major") >= 0; //$NON-NLS-1$ //$NON-NLS-2$
	}

	static long lookupUsedHeap(CompositeData gcInfo) {
		long usedHeap = 0;
		TabularData memoryUsageAfterGc = (TabularData) gcInfo.get("memoryUsageAfterGc"); //$NON-NLS-1$
		for (Object memoryPool : memoryUsageAfterGc.values()) {
			usedHeap += getMemoryPoolUsed((CompositeData) memoryPool);
		}
		return usedHeap;
	}

	private static long getMemoryPoolUsed(CompositeData memoryPool) {
		if (includeMemoryPool(memoryPool.get("key").toString())) { //$NON-NLS-1$
			Long memoryPoolUsed = (Long) ((CompositeData) memoryPool.get("value")).get("used"); //$NON-NLS-1$ //$NON-NLS-2$
			return memoryPoolUsed.longValue();
		}
		return 0;
	}

	private static boolean includeMemoryPool(String memoryPoolName) {
		if (memoryPoolName.equals("Code Cache")) { //$NON-NLS-1$
			return false;
		} else if (memoryPoolName.contains("Perm Gen")) { //$NON-NLS-1$
			return false;
		} else if (memoryPoolName.contains("Metaspace")) { //$NON-NLS-1$
			return false;
		}
		return true;
	}

	private static long getCommittedHeap(MBeanServerConnection connection) {
		try {
			return ((Number) AttributeValueToolkit.getAttribute(connection,
					new MRI(Type.ATTRIBUTE, "java.lang:type=Memory", "HeapMemoryUsage/committed"))).longValue(); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (Exception e) {
			return -1;
		}
	}

	@Override
	public boolean hasResolvedDependencies(MBeanServerConnection connection) {
		return !SyntheticAttributeToolkit.lookupMxBeans(connection, "java.lang", "GarbageCollector").isEmpty(); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
