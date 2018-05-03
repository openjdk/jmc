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
package org.openjdk.jmc.rjmx.test.triggers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.UUID;

import javax.management.JMException;
import javax.security.auth.login.FailedLoginException;

import org.junit.Before;
import org.junit.Test;

import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.rjmx.ConnectionToolkit;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadata;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRI.Type;
import org.openjdk.jmc.rjmx.test.RjmxTestCase;
import org.openjdk.jmc.rjmx.test.testutil.TestToolkit;
import org.openjdk.jmc.rjmx.test.triggers.NotificationActionCallback.NotificationActionCallbackReceiver;
import org.openjdk.jmc.rjmx.triggers.TriggerEvent;
import org.openjdk.jmc.rjmx.triggers.TriggerRule;
import org.openjdk.jmc.rjmx.triggers.internal.NotificationRegistry;
import org.openjdk.jmc.rjmx.triggers.internal.NotificationTrigger;
import org.openjdk.jmc.rjmx.triggers.internal.ValueEvaluatorBoolean;
import org.openjdk.jmc.rjmx.triggers.internal.ValueEvaluatorNumberMax;

/**
 * Tests for the notification framework.
 */
public class NotificationTriggerAndRuleTest extends RjmxTestCase
		implements NotificationActionCallback.NotificationActionCallbackReceiver {
	/** For code coverage */

	public final static int TIMEOUT = 9000;
	public static final Class<?>[] COVERED_CLASSES = new Class[] {
			// NotificationRule.class,
			NotificationTrigger.class};

	private TriggerEvent m_lastEvent;
	private final Object m_notifObj = new Object();
	private NotificationRegistry m_notificationRegistry;

	/**
	 * Tests registering and unregistering a rule.
	 *
	 * @throws IOException
	 * @throws FailedLoginException
	 */
	@Test
	public void testUnregisterRule() throws FailedLoginException, IOException {
		TriggerRule rule = createTestNotificationRule(
				new MRI(Type.ATTRIBUTE, "java.lang:type=OperatingSystem", "SystemCpuLoad")); //$NON-NLS-1$ //$NON-NLS-2$
		String serverGuid = UUID.randomUUID().toString();
		m_notificationRegistry.registerRule(rule, serverGuid);
		m_notificationRegistry.unregisterRule(rule, serverGuid);
		assertEquals("Failed to remove rule!", m_notificationRegistry.getRegisteredRules(serverGuid).size(), 0); //$NON-NLS-1$
	}

	/**
	 * Tests that a notification rule triggers correctly.
	 *
	 * @throws JMException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testTriggerNotification() throws Exception {
		assertNull(m_lastEvent);
		TriggerRule rule = createRule();
		String serverGuid = m_connectionHandle.getServerDescriptor().getGUID();
		m_notificationRegistry.activateTriggersFor(m_connectionHandle);
		m_notificationRegistry.registerRule(rule, serverGuid);
		assertEquals("Didn't register rule!", 1, m_notificationRegistry.getRegisteredRules(serverGuid).size()); //$NON-NLS-1$
		synchronized (m_notifObj) {

			try {
				m_notifObj.wait(TIMEOUT);
			} catch (InterruptedException e) {
				fail("Timedout while waiting for notification!"); //$NON-NLS-1$
			}
		}
		m_notificationRegistry.unregisterRule(rule, serverGuid);
		assertNotNull("Never received any notification!", m_lastEvent); //$NON-NLS-1$
	}

	/**
	 * Tests creating a rule and that it is setup correctly.
	 *
	 * @throws IOException
	 * @throws FailedLoginException
	 */
	@Test
	public void testRuleCreation() throws FailedLoginException, IOException {
		TriggerRule aRule = new TriggerRule();
		assertNotNull(aRule);
		assertTrue(!aRule.hasAction());
		assertTrue(!aRule.hasTrigger());
		assertTrue(!aRule.isComplete());

		TriggerRule anotherRule = createTestNotificationRule(
				new MRI(Type.ATTRIBUTE, "java.lang:type=OperatingSystem", "SystemCpuLoad")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(anotherRule.hasAction());
		assertTrue(!anotherRule.hasConstraints());
		assertTrue(anotherRule.hasTrigger());
		assertTrue(anotherRule.isComplete());
		assertEquals("TestRule", anotherRule.toString()); //$NON-NLS-1$
		assertEquals("TestRule", anotherRule.getName()); //$NON-NLS-1$

		aRule.setName("Abrakadabra"); //$NON-NLS-1$
		// Test sorting
		assertTrue(aRule.compareTo(anotherRule) < 0);
	}

	private TriggerRule createTestNotificationRule(MRI descriptor) {
		NotificationTrigger trigger = new NotificationTrigger(descriptor, new ValueEvaluatorBoolean());
		return new TriggerRule("TestRule", trigger, new NotificationActionCallback(this)); //$NON-NLS-1$
	}

	/**
	 * @see NotificationActionCallbackReceiver#callback(TriggerEvent)
	 */
	@Override
	public void onNotificationAction(TriggerEvent e) {
		synchronized (m_notifObj) {
			TestToolkit.println("Got a callback: " + e); //$NON-NLS-1$
			m_lastEvent = e;
			m_notifObj.notify();
		}
	}

	/**
	 * Registers a rule used by the test.
	 *
	 * @return the new rule
	 * @throws JMException
	 * @throws IOException
	 */
	protected TriggerRule createRule() throws Exception {
		MRI uptimeDescriptor = new MRI(Type.ATTRIBUTE, "java.lang:type=Runtime", //$NON-NLS-1$
				"Uptime"); //$NON-NLS-1$
		long uptime = ConnectionToolkit.getRuntimeBean(getMBeanServerConnection()).getUptime();
		IMRIMetadata metadata = getMRIMetadataService().getMetadata(uptimeDescriptor);
		IUnit unit = UnitLookup.getUnitOrDefault(metadata.getUnitString());
		TriggerRule rule = createTestNotificationRule(uptimeDescriptor);
		ValueEvaluatorNumberMax eval = new ValueEvaluatorNumberMax();
		eval.setMax(unit.quantity(uptime));
		rule.setTrigger(new NotificationTrigger(uptimeDescriptor, eval));
		return rule;
	}

	@Before
	public void setUp() throws Exception {
		m_notificationRegistry = new NotificationRegistry();
	}
}
