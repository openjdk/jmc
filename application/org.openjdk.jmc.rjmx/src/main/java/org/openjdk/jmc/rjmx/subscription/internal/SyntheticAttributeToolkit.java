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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServerConnection;
import javax.management.NotificationFilter;
import javax.management.NotificationFilterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRI.Type;

/**
 * Utility class for common operations performed by synthetic attributes and notifications.
 */
public class SyntheticAttributeToolkit {

	public static Set<ObjectName> lookupMxBeans(MBeanServerConnection connection, String domain, String type) {
		Set<ObjectName> mxBeans = new HashSet<>();
		try {
			for (ObjectName objectName : connection.queryNames(null, null)) {
				if (objectName.getDomain().equals(domain)) {
					if (type.equals(objectName.getKeyProperty("type"))) { //$NON-NLS-1$
						mxBeans.add(objectName);
					}
				}
			}
		} catch (IOException e) {
			// Ignore
		}
		return mxBeans;
	}

	public static MRI[] createNotificationDescriptors(
		MBeanServerConnection connection, Set<ObjectName> mxBeans, String type) {
		List<MRI> notificationDescriptors = new ArrayList<>();
		NEXT_BEAN: for (ObjectName mxBean : mxBeans) {
			try {
				for (MBeanNotificationInfo notification : connection.getMBeanInfo(mxBean).getNotifications()) {
					for (String notifType : notification.getNotifTypes()) {
						if (notifType.equals(type)) {
							notificationDescriptors.add(new MRI(Type.NOTIFICATION, mxBean, type));
							continue NEXT_BEAN;
						}
					}
				}
			} catch (Exception e) {
				// Ignore
			}
		}
		return notificationDescriptors.toArray(new MRI[notificationDescriptors.size()]);
	}

	public static void subscribeToNotifications(
		MBeanServerConnection connection, NotificationListener listener, Iterable<? extends ObjectName> beans,
		String type) {
		for (ObjectName bean : beans) {
			try {
				connection.addNotificationListener(bean, listener, createNotificationFilter(type), null);
			} catch (Exception e) {
				// Ignore
			}
		}
	}

	private static NotificationFilter createNotificationFilter(String type) {
		NotificationFilterSupport filter = new NotificationFilterSupport();
		filter.enableType(type);
		return filter;
	}

	public static void unsubscribeFromNotifications(
		MBeanServerConnection connection, NotificationListener listener, Iterable<? extends ObjectName> beans) {
		for (ObjectName bean : beans) {
			try {
				connection.removeNotificationListener(bean, listener);
			} catch (Exception e) {
				// Ignore
			}
		}
	}

}
