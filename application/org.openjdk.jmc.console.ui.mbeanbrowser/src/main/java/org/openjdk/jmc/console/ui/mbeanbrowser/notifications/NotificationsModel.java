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
package org.openjdk.jmc.console.ui.mbeanbrowser.notifications;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

/**
 * Model that keeps track of the JMX-notifications for a given MBean. This class is thread safe.
 */
public class NotificationsModel {

	final private ObjectName m_objectName;
	final private NotificationListener m_notificationListener;
	final private MBeanServerConnection m_mbeanServer;
	final private MBeanInfo m_info;

	final private List<Notification> m_notificationList = new ArrayList<>();
	final private Consumer<? super NotificationsModel> m_observer;
	private boolean m_enabled;

	public NotificationsModel(ObjectName objectName, MBeanServerConnection mbeanServer,
			Consumer<? super NotificationsModel> observer) throws Exception {
		m_objectName = objectName;
		m_observer = observer;
		m_info = mbeanServer.getMBeanInfo(objectName);
		m_mbeanServer = mbeanServer;
		m_notificationListener = new NotificationListener() {
			@Override
			public void handleNotification(Notification notification, Object handback) {
				synchronized (m_notificationList) {
					m_notificationList.add(notification);
				}
				notifyObserver();
			}
		};
	}

	public ObjectName getObjectName() {
		return m_objectName;
	}

	private void notifyObserver() {
		m_observer.accept(this);
	}

	public boolean supportsSubscriptions() {
		return m_info != null && m_info.getNotifications().length > 0;
	}

	public Stream<Notification> getNotifications() {
		synchronized (m_notificationList) {
			return Stream.of(m_notificationList.toArray(new Notification[m_notificationList.size()]));
		}
	}

	public void setSubscriptionEnabled(boolean enable)
			throws InstanceNotFoundException, ListenerNotFoundException, IOException {
		synchronized (m_notificationList) {
			if (m_enabled != enable) {
				if (enable) {
					getMBeanServerConnection().addNotificationListener(m_objectName, m_notificationListener, null,
							null);
				} else {
					getMBeanServerConnection().removeNotificationListener(m_objectName, m_notificationListener, null,
							null);
				}
				m_enabled = enable;
			}
		}
		notifyObserver();
	}

	public boolean getSubscriptionEnabled() {
		synchronized (m_notificationList) {
			return m_enabled;
		}
	}

	public MBeanInfo getMBeanNoticationInfo() {
		return m_info;
	}

	private MBeanServerConnection getMBeanServerConnection() {
		return m_mbeanServer;
	}

	public void dispose() {
		if (getSubscriptionEnabled()) {
			try {
				setSubscriptionEnabled(false);
			} catch (Exception e) {
				// ok, we're closing.
			}
		}
	}
}
