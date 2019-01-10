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
package org.openjdk.jmc.rjmx.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXServiceURL;

import org.junit.Test;

import org.openjdk.jmc.rjmx.ConnectionDescriptorBuilder;
import org.openjdk.jmc.rjmx.ConnectionToolkit;
import org.openjdk.jmc.rjmx.IConnectionDescriptor;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.IServerHandle;
import org.openjdk.jmc.rjmx.internal.JMXConnectionDescriptor;
import org.openjdk.jmc.rjmx.subscription.IMRISubscription;
import org.openjdk.jmc.rjmx.subscription.IMRIValueListener;
import org.openjdk.jmc.rjmx.subscription.ISubscriptionService;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRI.Type;
import org.openjdk.jmc.rjmx.subscription.MRIValueEvent;
import org.openjdk.jmc.rjmx.subscription.PolicyFactory;

/**
 * Basic tests for the new cleaned up RJMX API.
 */
public class BasicCommunicationTest extends ServerHandleTestCase {
	final static int DEFAULT_PORT = 0;
	final static String DEFAULT_HOST = "localhost".intern(); //$NON-NLS-1$
	final static String ALTERNATIVE_HOST = "127.1.0.1".intern(); //$NON-NLS-1$

	/**
	 * Attempts creating a {@link JMXConnectionDescriptor} with a bunch of options.
	 */
	@Test
	public void testCreateJMXDescriptor() throws IOException {
		IConnectionDescriptor descriptor = LocalRJMXTestToolkit.createDefaultDescriptor();
		JMXServiceURL url = descriptor.createJMXServiceURL();
		assertEquals(DEFAULT_HOST, ConnectionToolkit.getHostName(url));
		assertEquals(DEFAULT_PORT, ConnectionToolkit.getPort(url));
	}

	@Test
	public void testIConnectionHandle() throws Exception {
		IConnectionHandle handle = getDefaultServer().connect("Test"); //$NON-NLS-1$
		MRI descriptor = new MRI(Type.ATTRIBUTE, "java.lang:type=Threading", //$NON-NLS-1$
				"ThreadCount"); //$NON-NLS-1$
		MBeanServerConnection connection = handle.getServiceOrThrow(MBeanServerConnection.class);
		assertBetween(1L, 1000L,
				((Number) connection.getAttribute(descriptor.getObjectName(), descriptor.getDataPath())).longValue());
		handle.close();
	}

	@Test
	public void testServerHandle() throws Exception {
		IConnectionHandle handle = getDefaultServer().connect("Test"); //$NON-NLS-1$

		assertEquals(1, getDefaultServer().getConnectionHandles().length);
		assertEquals(handle, getDefaultServer().getConnectionHandles()[0]);

		handle.close();
		assertEquals(0, getDefaultServer().getConnectionHandles().length);
	}

	protected void muppTestConnection() throws Exception {
		ConnectionDescriptorBuilder builder = new ConnectionDescriptorBuilder();
		IConnectionDescriptor descriptor = builder.hostName("localhost").port(0).build(); //$NON-NLS-1$
		IConnectionHandle handle = IServerHandle.create(descriptor).connect("Test"); //$NON-NLS-1$

		ISubscriptionService service = handle.getServiceOrThrow(ISubscriptionService.class);
		MRI attribute = new MRI(Type.ATTRIBUTE, "java.lang:type=Threading", //$NON-NLS-1$
				"ThreadCount"); //$NON-NLS-1$
		service.addMRIValueListener(attribute, new IMRIValueListener() {
			@Override
			public void valueChanged(MRIValueEvent event) {
				System.out.println(event.getValue());
			}
		});

		IMRISubscription subscription = service.getMRISubscription(attribute);
		subscription.setUpdatePolicy(PolicyFactory.createSimpleUpdatePolicy(1500));

		Thread.sleep(5000);
		handle.close();
	}
}
