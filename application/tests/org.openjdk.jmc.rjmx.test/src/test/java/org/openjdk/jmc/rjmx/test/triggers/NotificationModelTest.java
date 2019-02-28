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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;

import javax.security.auth.login.FailedLoginException;

import org.junit.Before;
import org.junit.Test;

import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadata;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRI.Type;
import org.openjdk.jmc.rjmx.test.RjmxTestCase;
import org.openjdk.jmc.rjmx.test.triggers.NotificationActionCallback.NotificationActionCallbackReceiver;
import org.openjdk.jmc.rjmx.triggers.TriggerEvent;
import org.openjdk.jmc.rjmx.triggers.TriggerRule;
import org.openjdk.jmc.rjmx.triggers.internal.NotificationRegistry;
import org.openjdk.jmc.rjmx.triggers.internal.NotificationTrigger;
import org.openjdk.jmc.rjmx.triggers.internal.ValueEvaluatorNumberMin;

/**
 * Tests for the notification framework.
 */
public class NotificationModelTest extends RjmxTestCase
		implements NotificationActionCallback.NotificationActionCallbackReceiver {
	private final Object m_monitor = new Object();
	private NotificationRegistry m_notificationRegistry;

	/**
	 * Tests registering a notification rule.
	 *
	 * @throws IOException
	 * @throws FailedLoginException
	 */
	@Test
	public void testRegisterRule() throws Exception {
		TriggerRule rule = createTestNotificationRule();
		String serverGuid = m_connectionHandle.getServerDescriptor().getGUID();
		m_notificationRegistry.registerRule(rule, serverGuid);
		Collection<TriggerRule> rulesList = m_notificationRegistry.getRegisteredRules(serverGuid);

		assertTrue("Failed to register anything at all!", rulesList.size() > 0);
		TriggerRule regRule = rulesList.iterator().next();
		assertTrue("Failed rule comparison!", regRule == rule);
	}

	/**
	 * Tests unregistering a notification rule.
	 *
	 * @throws IOException
	 * @throws FailedLoginException
	 */
	@Test
	public void testUnregisterRule() throws Exception {
		assertTrue(m_connectionHandle.isConnected());
		TriggerRule rule = createTestNotificationRule();
		String serverGuid = m_connectionHandle.getServerDescriptor().getGUID();
		m_notificationRegistry.registerRule(rule, serverGuid);
		m_notificationRegistry.unregisterRule(rule, serverGuid);
		assertTrue("Failed to unregister rule!", m_notificationRegistry.getRegisteredRules(serverGuid).size() == 0);
	}

	private TriggerRule createTestNotificationRule() throws Exception {
		assertTrue(m_connectionHandle.isConnected());
		NotificationTrigger trigger = new NotificationTrigger();
		MRI mri = new MRI(Type.ATTRIBUTE, "java.lang:type=Memory", "HeapMemoryUsage/used");
		IMRIMetadata metadata = getMRIMetadataService().getMetadata(mri);
		IUnit unit = UnitLookup.getUnitOrNull(metadata.getUnitString());
		trigger.setAttributeDescriptor(mri);
		trigger.setValueEvaluator(new ValueEvaluatorNumberMin(unit.quantity(100000)));
		return new TriggerRule("TestRule", trigger, new NotificationActionCallback(this));
	}

	/**
	 * @see NotificationActionCallbackReceiver#callback(TriggerEvent)
	 */
	@Override
	public void onNotificationAction(TriggerEvent e) {
		synchronized (m_monitor) {
			m_monitor.notifyAll();
		}
	}

	@Before
	public void setUp() throws Exception {
		m_notificationRegistry = new NotificationRegistry();
	}
}
