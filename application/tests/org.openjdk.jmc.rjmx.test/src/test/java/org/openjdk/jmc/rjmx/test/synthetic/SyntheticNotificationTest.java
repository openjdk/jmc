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
package org.openjdk.jmc.rjmx.test.synthetic;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.test.ServerHandleTestCase;

public class SyntheticNotificationTest extends ServerHandleTestCase {

	private IConnectionHandle handle;
	private MBeanServerConnection connection;
	private volatile boolean gotNotification = false;

	private class SyntheticNotificationListener implements NotificationListener {
		private Notification lastNotification;

		@Override
		public void handleNotification(Notification notification, Object handback) {
			lastNotification = notification;
			gotNotification = true;
			synchronized (SyntheticNotificationTest.this) {
				SyntheticNotificationTest.this.notify();
			}
		}

		public Notification getLastNotification() {
			return lastNotification;
		}
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		handle = getDefaultServer().connect("Test"); //$NON-NLS-1$
		connection = handle.getServiceOrThrow(MBeanServerConnection.class);
		gotNotification = false;
	}

	@Override
	public void tearDown() throws Exception {
		handle.close();
	}

	/**
	 * Tests that we can add synthetic notifications.
	 */
	@Test
	public void testGetNotificationMetadata() throws InstanceNotFoundException, IntrospectionException,
			MalformedObjectNameException, ReflectionException, NullPointerException, IOException {
		MBeanInfo info = connection.getMBeanInfo(new ObjectName("org.openjdk.jmc.test:type=Test")); //$NON-NLS-1$
		assertTrue(info.getNotifications().length > 0);
	}

	/**
	 * Tests that we can have attributes AND notifications on the same synthetic.
	 */
	@Test
	public void testCombinedMetadata() throws InstanceNotFoundException, IntrospectionException,
			MalformedObjectNameException, ReflectionException, NullPointerException, IOException {
		MBeanInfo info = connection.getMBeanInfo(new ObjectName("org.openjdk.jmc.test:type=Test")); //$NON-NLS-1$
		assertTrue(info.getNotifications().length > 0);
		assertTrue(info.getAttributes().length > 0);
	}

	/**
	 * Tests that we can overload existing real MBean with notification and still get values.
	 */
	@Test
	public void testOverloadMetadata() throws InstanceNotFoundException, IntrospectionException,
			MalformedObjectNameException, ReflectionException, NullPointerException, IOException {
		MBeanInfo info = connection.getMBeanInfo(new ObjectName("java.lang:type=ClassLoading")); //$NON-NLS-1$
		assertTrue(info.getNotifications().length > 0);
		assertTrue(info.getAttributes().length > 0);
	}

	/**
	 * Tests that we can shadow an existing notification.
	 */
	@Test
	public void testShadowMetadata() throws InstanceNotFoundException, IntrospectionException,
			MalformedObjectNameException, ReflectionException, NullPointerException, IOException {
		Assume.assumeTrue("FIXME: Shadowing does not work yet!", false); //$NON-NLS-1$
		MBeanInfo info = connection.getMBeanInfo(new ObjectName("java.lang:type=Memory")); //$NON-NLS-1$
		assertTrue(info.getNotifications().length > 0);
		for (MBeanNotificationInfo notificationInfo : info.getNotifications()) {
			if (notificationInfo.getName().equals("java.management.memory.collection.threshold.exceeded")) { //$NON-NLS-1$
				assertTrue("Failed to shadow description", notificationInfo.getDescription().contains("shadow")); //$NON-NLS-1$ //$NON-NLS-2$
				assertTrue("Got the wrong type:" + notificationInfo.getNotifTypes()[0], "int".equals(notificationInfo //$NON-NLS-1$ //$NON-NLS-2$
						.getNotifTypes()[0]));
			} else {
				assertTrue("Should NOT contain shadow!", !notificationInfo.getDescription().contains("shadow")); //$NON-NLS-1$ //$NON-NLS-2$
				assertTrue("Should not be int!", !"int".equals(notificationInfo.getNotifTypes()[0])); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	@Test
	public void testNotificationListener() throws InstanceNotFoundException, MalformedObjectNameException,
			NullPointerException, IOException, InterruptedException, ListenerNotFoundException {
		Notification notif = null;
		SyntheticNotificationListener listener = new SyntheticNotificationListener();
		ObjectName testMBean = new ObjectName("org.openjdk.jmc.test:type=Test"); //$NON-NLS-1$
		connection.addNotificationListener(testMBean, listener, null, null);
		synchronized (this) {
			this.wait(30000);
			notif = listener.getLastNotification();
		}
		assertTrue("Never got any notification!", gotNotification); //$NON-NLS-1$
		assertNotNull(notif);
		assertTrue("Expected a user data > 0!", ((Integer) notif.getUserData()) > 0); //$NON-NLS-1$
		assertTrue("Expected Woho!", notif.getMessage().startsWith("Woho!")); //$NON-NLS-1$ //$NON-NLS-2$
		connection.removeNotificationListener(testMBean, listener, null, null);
	}
}
