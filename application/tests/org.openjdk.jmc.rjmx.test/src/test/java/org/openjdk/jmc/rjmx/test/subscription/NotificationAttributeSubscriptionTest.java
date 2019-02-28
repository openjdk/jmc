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
package org.openjdk.jmc.rjmx.test.subscription;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import javax.management.MBeanServerConnection;

import org.junit.Test;

import org.openjdk.jmc.rjmx.ConnectionToolkit;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.subscription.IMRIValueListener;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRIValueEvent;
import org.openjdk.jmc.rjmx.subscription.MRI.Type;
import org.openjdk.jmc.rjmx.test.RjmxTestCase;
import org.openjdk.jmc.rjmx.test.testutil.TestToolkit;

/**
 * Tests attribute subscriptions based on JMX notification.
 */
public class NotificationAttributeSubscriptionTest extends RjmxTestCase implements IMRIValueListener {
	private final static int SLEEP_TIME = 6000;

	/**
	 * Tests the normal one shot subscription to the strategy.
	 */
	@Test
	public void testGetGCSubscription() {
		try {
			// RMP does not support notifications
			MRI descriptor = null;
			synchronized (this) {
				// Starting up a subscription on a one shot attribute.
				descriptor = new MRI(Type.NOTIFICATION, "com.sun.management:type=GarbageCollectionAggregator",
						"com.sun.management.gc.notification");
				getAttributeSubscriptionService().addMRIValueListener(descriptor, this);

				secondThreadException = null;
				final IConnectionHandle threadModel = m_connectionHandle;
				new Thread(new Runnable() {
					@Override
					public void run() {
						synchronized (NotificationAttributeSubscriptionTest.this) {
							try {
								ConnectionToolkit
										.getMemoryBean(threadModel.getServiceOrThrow(MBeanServerConnection.class)).gc();
							} catch (Exception e) {
								secondThreadException = e;
							}
						}
					}
				}).start();
				wait(SLEEP_TIME);
				if (secondThreadException != null) {
					throw secondThreadException;
				}
			}
			assertNotNull(getAttributeSubscriptionService().getLastMRIValueEvent(descriptor));
			getAttributeSubscriptionService().removeMRIValueListener(this);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private Exception secondThreadException = null;

	/**
	 * @param event
	 * @see IMRIValueListener#valueChanged(MRIValueEvent)
	 */
	@Override
	public synchronized void valueChanged(MRIValueEvent event) {
		TestToolkit.println(event);
		// Only notify if we got a real server side event
		if (event.getValue() != null) {
			notifyAll();
		}
	}
}
