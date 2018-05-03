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

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.rjmx.ConnectionDescriptorBuilder;
import org.openjdk.jmc.rjmx.ConnectionException;
import org.openjdk.jmc.rjmx.IConnectionDescriptor;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.IServerDescriptor;
import org.openjdk.jmc.rjmx.IServerHandle;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.servermodel.IServer;
import org.openjdk.jmc.rjmx.servermodel.IServerModel;
import org.openjdk.jmc.rjmx.subscription.IMBeanHelperService;
import org.openjdk.jmc.rjmx.subscription.IMRISubscription;
import org.openjdk.jmc.rjmx.subscription.IMRIValueListener;
import org.openjdk.jmc.rjmx.subscription.ISubscriptionService;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRI.Type;
import org.openjdk.jmc.rjmx.subscription.MRIValueEvent;
import org.openjdk.jmc.rjmx.subscription.PolicyFactory;
import org.openjdk.jmc.ui.common.util.Environment;

/**
 * This test suite is supposed to test the example code that we ship with the documentation for the
 * org.openjdk.jmc.rjmx bundle. That is, for each code example included in
 * org.openjdk.jmc.rjmx/src/org/openjdk/jmc/rjmx/package.html, there should be a test method in here
 * with a verbatim copy of that code.
 * <p>
 * Included in the RJMXTestSuite.
 */
// NOTE: If you change the verbatim test YOU MUST update the corresponding package.html document.
public class PackageExampleTest {
	private volatile boolean gotEvent;

	/**
	 * Tests that the package documentation example actually makes sense and compiles as expected.
	 *
	 * @throws ConnectionException
	 */
	@Test
	public void testPackageExampleVerbatim() throws Exception {
		IConnectionDescriptor descriptor = new ConnectionDescriptorBuilder().hostName("localhost").port(0).build(); //$NON-NLS-1$
		IServerHandle serverHandle = IServerHandle.create(descriptor);
		IConnectionHandle handle = serverHandle.connect("Usage description"); //$NON-NLS-1$
		try {
			ISubscriptionService service = handle.getServiceOrThrow(ISubscriptionService.class);
			MRI attribute = new MRI(Type.ATTRIBUTE, "java.lang:type=Threading", "ThreadCount"); //$NON-NLS-1$ //$NON-NLS-2$
			service.addMRIValueListener(attribute, new IMRIValueListener() {
				@Override
				public void valueChanged(MRIValueEvent event) {
					System.out.println(event.getValue());
				}
			});
			IMRISubscription subscription = service.getMRISubscription(attribute);
			subscription.setUpdatePolicy(PolicyFactory.createSimpleUpdatePolicy(1500));
		} finally {
			// Always close IConnectionHandle when done
			IOToolkit.closeSilently(handle);
		}

	}

	@Test
	public void testPackageExampleFunctionality() throws Exception {
		ConnectionDescriptorBuilder builder = new ConnectionDescriptorBuilder();
		IConnectionDescriptor descriptor = builder.hostName("localhost").port(0).build(); //$NON-NLS-1$
		IConnectionHandle handle = IServerHandle.create(descriptor).connect("Usage description"); //$NON-NLS-1$
		try {
			ISubscriptionService service = handle.getServiceOrThrow(ISubscriptionService.class);
			gotEvent = false;
			MRI attribute = new MRI(Type.ATTRIBUTE, "java.lang:type=Threading", "ThreadCount"); //$NON-NLS-1$ //$NON-NLS-2$
			service.addMRIValueListener(attribute, new IMRIValueListener() {
				@Override
				public void valueChanged(MRIValueEvent event) {
					synchronized (PackageExampleTest.this) {
						gotEvent = true;
						PackageExampleTest.this.notifyAll();
					}
				}
			});
			IMRISubscription subscription = service.getMRISubscription(attribute);
			subscription.setUpdatePolicy(PolicyFactory.createSimpleUpdatePolicy(1500));
			synchronized (PackageExampleTest.this) {
				this.wait(4000);
			}
		} finally {
			IOToolkit.closeSilently(handle);
		}
		assertTrue("Never got any event!", gotEvent); //$NON-NLS-1$
	}

	@Test
	public void testUseServerModel() throws Exception {
		IServerModel model = RJMXPlugin.getDefault().getService(IServerModel.class);
		for (IServer server : model.elements()) {
			IServerDescriptor descriptor = server.getServerHandle().getServerDescriptor();
			if (descriptor.getJvmInfo() != null
					&& Integer.valueOf(Environment.getThisPID()).equals(descriptor.getJvmInfo().getPid())) {
				IConnectionHandle handle = server.getServerHandle().connect("Usage description"); //$NON-NLS-1$
				try {
					handle.getServiceOrThrow(IMBeanHelperService.class).getMBeanNames().size();
					return;
				} finally {
					IOToolkit.closeSilently(handle);
				}
			}
		}
	}
}
