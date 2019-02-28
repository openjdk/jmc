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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.IServerHandle;
import org.openjdk.jmc.rjmx.subscription.IMRIValueListener;
import org.openjdk.jmc.rjmx.subscription.ISubscriptionService;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRI.Type;
import org.openjdk.jmc.rjmx.subscription.MRIValueEvent;
import org.openjdk.jmc.rjmx.test.LocalRJMXTestToolkit;
import org.openjdk.jmc.rjmx.test.RjmxTestCase;
import org.openjdk.jmc.rjmx.test.testutil.TestToolkit;

@SuppressWarnings("nls")
public class AttributeSubscriptionTest extends RjmxTestCase implements IMRIValueListener {
	private static int TEST_TIMEOUT_TIME = 30000;
//	private static int TEST_TIMEOUT_TIME = Integer.MAX_VALUE;
	private int counter;

	@Test
	public void testSubscribeToCPULoad() throws Exception {
		IConnectionHandle handle = IServerHandle.create(LocalRJMXTestToolkit.createDefaultDescriptor()).connect("Test");
		ISubscriptionService subscriptionService = handle.getServiceOrThrow(ISubscriptionService.class);
		try {
			MRI attributeDescriptor = new MRI(Type.ATTRIBUTE, "java.lang:type=OperatingSystem", "SystemCpuLoad");
			subscriptionService.addMRIValueListener(attributeDescriptor, this);
			synchronized (this) {
				for (int i = 0; i < 4; i++) {
					this.wait(TEST_TIMEOUT_TIME);
				}
			}
			assertNotNull(subscriptionService.getLastMRIValueEvent(attributeDescriptor));
		} finally {
			subscriptionService.removeMRIValueListener(this);
			handle.close();
		}
		assertTrue(getCounter() > 3);
	}

	@Test
	public void testGetAttributeSubscriptionOneShot() throws Exception {
		// Starting up a subscription on a one shot attribute.
		IConnectionHandle handle = IServerHandle.create(LocalRJMXTestToolkit.createDefaultDescriptor()).connect("Test");
		ISubscriptionService subscriptionService = handle.getServiceOrThrow(ISubscriptionService.class);
		try {
			MRI availableProcessorsAttribute = new MRI(Type.ATTRIBUTE, "java.lang:type=OperatingSystem",
					"AvailableProcessors");

			subscriptionService.addMRIValueListener(availableProcessorsAttribute, this);

			// Since it's a one shot and pretty fast, it may already have been retrieved...
			if (subscriptionService.getLastMRIValueEvent(availableProcessorsAttribute) == null) {
				synchronized (this) {
					for (int i = 0; i < 2; i++) {
						if (subscriptionService.getLastMRIValueEvent(availableProcessorsAttribute) != null) {
							break;
						}
						wait(TEST_TIMEOUT_TIME);
					}
				}
			}
			assertNotNull(subscriptionService.getLastMRIValueEvent(availableProcessorsAttribute));
			assertNotNull(subscriptionService.getLastMRIValueEvent(availableProcessorsAttribute).getValue());
		} finally {
			subscriptionService.removeMRIValueListener(this);
			handle.close();
		}
	}

	@Test
	public void testGetAttributeSubscriptionOne() {
		try {
			// Starting up a subscription on a one shot attribute.
			MRI physicalMemoryUsedAttribute = new MRI(Type.ATTRIBUTE, "java.lang:type=OperatingSystem",
					"UsedPhysicalMemorySize");
			getAttributeSubscriptionService().addMRIValueListener(physicalMemoryUsedAttribute, this);

			synchronized (this) {
				wait(TEST_TIMEOUT_TIME);
			}
			assertNotNull(getAttributeSubscriptionService().getLastMRIValueEvent(physicalMemoryUsedAttribute));
			getAttributeSubscriptionService().removeMRIValueListener(this);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testGetAttributeSubscriptionTwo() {
		try {
			// Starting up a subscription on a one shot attribute.
			MRI tcad = new MRI(Type.ATTRIBUTE, "java.lang:type=OperatingSystem", "UsedPhysicalMemorySize");
			getAttributeSubscriptionService().addMRIValueListener(tcad, this);
			MRI tstcad = new MRI(Type.ATTRIBUTE, "java.lang:type=OperatingSystem", "FreePhysicalMemorySize");
			getAttributeSubscriptionService().addMRIValueListener(tstcad, this);

			for (int i = 0; i < 7; i++) {
				synchronized (this) {
					wait(TEST_TIMEOUT_TIME);
					if (getAttributeSubscriptionService().getLastMRIValueEvent(tcad) != null
							&& getAttributeSubscriptionService().getLastMRIValueEvent(tstcad) != null) {
						break;
					}
				}
				Thread.yield();
			}
			assertNotNull(getAttributeSubscriptionService().getLastMRIValueEvent(tcad));
			assertNotNull(getAttributeSubscriptionService().getLastMRIValueEvent(tstcad));
			getAttributeSubscriptionService().removeMRIValueListener(this);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testGetSyntheticSubscription() {
		try {
			// Starting up a subscription on a one shot attribute.
			MRI synthad = new MRI(Type.ATTRIBUTE, "java.lang:type=Memory", "HeapMemoryUsagePercent");
			getAttributeSubscriptionService().addMRIValueListener(synthad, this);

			synchronized (this) {
				wait(TEST_TIMEOUT_TIME);
			}
			assertNotNull(getAttributeSubscriptionService().getLastMRIValueEvent(synthad));
			getAttributeSubscriptionService().removeMRIValueListener(this);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testGetSyntheticPhysicalMemSubscription() {
		try {
			// Starting up a subscription on a one shot attribute.

			MRI synthad = new MRI(Type.ATTRIBUTE, "java.lang:type=OperatingSystem", "PhysicalMemoryUsagePercent");
			getAttributeSubscriptionService().addMRIValueListener(synthad, this);

			synchronized (this) {
				wait(TEST_TIMEOUT_TIME);
			}
			assertNotNull(getAttributeSubscriptionService().getLastMRIValueEvent(synthad));
			getAttributeSubscriptionService().removeMRIValueListener(this);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Before
	public void setUp() throws Exception {
		counter = 0;
	}

	@Override
	public void valueChanged(MRIValueEvent event) {
		synchronized (this) {
			TestToolkit.println(event);
			if (event.getValue() != null) {
				notify();
			}
			incrementCounter();
		}
	}

	public synchronized int getCounter() {
		return counter;
	}

	public synchronized void incrementCounter() {
		counter += 1;
	}
}
