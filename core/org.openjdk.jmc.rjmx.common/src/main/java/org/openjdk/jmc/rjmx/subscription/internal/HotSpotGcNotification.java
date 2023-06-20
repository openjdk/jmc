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
import java.util.logging.Level;

import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import org.openjdk.jmc.rjmx.RJMXPlugin;

/**
 * A unifying GC notification attribute for HotSpot.
 */
public class HotSpotGcNotification extends AbstractSyntheticNotification {

	private static final String[] FIELD_NAMES = new String[] {"HeapLiveSet", "HeapLiveSetSize", "Duration"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	private static final CompositeType TYPE;
	static {
		try {
			TYPE = new CompositeType("org.openjdk.jmc.rjmx.subscription.internal.hotspotgcdata", "GC Data", FIELD_NAMES, //$NON-NLS-1$ //$NON-NLS-2$
					FIELD_NAMES, new OpenType[] {SimpleType.DOUBLE, SimpleType.LONG, SimpleType.LONG});
		} catch (OpenDataException e) {
			throw new RuntimeException(e);
		}
	}

	private static final String COM_SUN_MANAGEMENT_GC_NOTIFICATION = "com.sun.management.gc.notification"; //$NON-NLS-1$
	private MBeanServerConnection m_connection;
	private Set<ObjectName> m_garbageCollectorMxBeans;
	private CompositeData m_lastValue;
	private String m_lastMessage;
	private final NotificationListener m_listener;

	public HotSpotGcNotification() {
		m_listener = createListener();
	}

	@Override
	public void init(MBeanServerConnection connection, String type, String message) {
		super.init(connection, type, message);
		m_connection = connection;
		// TODO: Save metadata
//		IAttributeInfoService service = connectionHandle.getService(IAttributeInfoService.class);
//		service.setMetadata(getAttributeInfo().getAttributeDescriptor(),
//				AttributeMetadataManager.KEY_DESCRIPTION,
//				"Sends an event every time a garbage collection has taken place. Identity number is the sequence number of the collection since the JMC connection was established."
//				);
		m_garbageCollectorMxBeans = SyntheticAttributeToolkit.lookupMxBeans(m_connection, "java.lang", //$NON-NLS-1$
				"GarbageCollector"); //$NON-NLS-1$
		SyntheticAttributeToolkit.subscribeToNotifications(m_connection, m_listener, m_garbageCollectorMxBeans,
				COM_SUN_MANAGEMENT_GC_NOTIFICATION);
	}

	private NotificationListener createListener() {
		return new NotificationListener() {

			@Override
			public void handleNotification(Notification notification, Object handback) {
				synchronized (HotSpotGcNotification.this) {
					try {
						CompositeData userData = (CompositeData) notification.getUserData();
						CompositeData gcInfo = (CompositeData) userData.get("gcInfo"); //$NON-NLS-1$
						Object[] values = new Object[FIELD_NAMES.length];
						long usedHeap = HotSpotLiveSetAttribute.lookupUsedHeap(gcInfo);
						values[0] = HotSpotLiveSetAttribute.calculateLiveSet(usedHeap, m_connection);
						values[1] = usedHeap;
						values[2] = gcInfo.get("duration"); //$NON-NLS-1$
						m_lastValue = new CompositeDataSupport(TYPE, FIELD_NAMES, values);
					} catch (Exception e) {
						RJMXPlugin.getDefault().getLogger().log(Level.WARNING,
								"Failed to update HotSpotGcNotification value", e); //$NON-NLS-1$
					}

					m_lastMessage = notification.getMessage();
				}
				triggerNotification();
			}
		};
	}

	@Override
	protected String getMessage() {
		return m_lastMessage;
	}

	@Override
	public Object getValue() {
		return m_lastValue;
	}

	@Override
	public void stop() {
		SyntheticAttributeToolkit.unsubscribeFromNotifications(m_connection, m_listener, m_garbageCollectorMxBeans);
	}

	@Override
	public CompositeType getValueType() {
		return TYPE;
	}

	@Override
	public boolean hasResolvedDependencies(MBeanServerConnection connection) {
		return !SyntheticAttributeToolkit.lookupMxBeans(connection, "java.lang", "GarbageCollector").isEmpty(); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
