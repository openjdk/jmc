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

import org.junit.Test;

import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.IServerHandle;
import org.openjdk.jmc.rjmx.subscription.IMRIValueListener;
import org.openjdk.jmc.rjmx.subscription.ISubscriptionService;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRIValueEvent;
import org.openjdk.jmc.rjmx.subscription.MRI.Type;
import org.openjdk.jmc.rjmx.test.LocalRJMXTestToolkit;
import org.openjdk.jmc.rjmx.test.RjmxTestCase;
import org.openjdk.jmc.rjmx.test.testutil.TestToolkit;

public class MultipleAttributeSubscriptionTest extends RjmxTestCase implements IMRIValueListener {

	/**
	 * Tests acquiring several subscriptions. Ensures that subscriptions are properly created and
	 * that events are properly received.
	 */
	@Test
	public void testMultipleSubscriptions() throws Exception {
		IConnectionHandle handle = IServerHandle.create(LocalRJMXTestToolkit.createDefaultDescriptor()).connect("Test");
		ISubscriptionService service = handle.getServiceOrThrow(ISubscriptionService.class);

		MRI cpuAttributeDescriptor = new MRI(Type.ATTRIBUTE, "java.lang:type=OperatingSystem", "SystemCpuLoad");
		service.addMRIValueListener(cpuAttributeDescriptor, this);
		MRI loadAttributeDescriptor = new MRI(Type.ATTRIBUTE, "java.lang:type=OperatingSystem", "ProcessCpuLoad");
		service.addMRIValueListener(loadAttributeDescriptor, this);

		synchronized (this) {
			for (int i = 0; i < 8; i++) {
				wait(8000);
			}
		}

		assertNotNull(service.getLastMRIValueEvent(cpuAttributeDescriptor));
		assertNotNull(service.getLastMRIValueEvent(loadAttributeDescriptor));
		handle.close();
	}

	@Override
	public void valueChanged(MRIValueEvent event) {
		synchronized (this) {
			TestToolkit.println(event);
			notify();
		}
	}
}
