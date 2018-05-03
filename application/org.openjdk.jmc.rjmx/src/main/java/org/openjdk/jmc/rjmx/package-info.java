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
/**
 * This package contains the core RJMX API. RJMX is an Equinox plug-in that extends JMX with
 * pluggable JMX based services and convenience look-up services to find agents to connect to. The
 * default services encompass a compatibility layer, an attribute subscription engine, a
 * notification service and others.
 * <p>
 * Example usage:
 *
 * <pre>
// Connect to thyself
IConnectionDescriptor descriptor = new ConnectionDescriptorBuilder().hostName("localhost").port(0).build(); //$NON-NLS-1$
IServerHandle server = IServerHandle.create(descriptor);
try {
	IConnectionHandle connection = server.connect("Usage description"); //$NON-NLS-1$
	// set up an attribute listener that will print value changes to System.out
	ISubscriptionService service = connection.getServiceOrDummy(ISubscriptionService.class);
	MRI attribute = new MRI(Type.ATTRIBUTE, "java.lang:type=Threading", "ThreadCount"); //$NON-NLS-1$ //$NON-NLS-2$
	service.addMRIValueListener(attribute, new IMRIValueListener() {
		{@literal @}Override
		public void valueChanged(MRIValueEvent event) {
			System.out.println(event.getValue());
		}
	});
	IMRISubscription subscription = service.getMRISubscription(attribute);
	subscription.setUpdatePolicy(PolicyFactory.createSimpleUpdatePolicy(1500));
} finally {
	server.dispose()
}

// Iterate the model and try to connect to the ourselves
IServerModel model = RJMXPlugin.getDefault().getService(IServerModel.class);
for (IServer server : model.elements()) {
	IServerDescriptor descriptor = server.getServerHandle().getServerDescriptor();
	if (descriptor.getJvmInfo() != null &amp;&amp; Integer.valueOf(LocalMBeanToolkit.getThisPID()).equals(descriptor.getJvmInfo().getPid())) {
		IConnectionHandle connection = server.getServerHandle().connect("Usage description"); //$NON-NLS-1$
		try {
			connection.getServiceOrDummy(IMBeanHelperService.class).getMBeanNames().size();
			return;
		} finally {
			IOToolkit.closeSilently(connection);
		}
	}
}
 * </pre>
 *
 * Certain services provided by RJMX are available through dependency injection. These include
 * {@link org.openjdk.jmc.rjmx.IConnectionHandle},
 * {@link org.openjdk.jmc.rjmx.subscription.ISubscriptionService} and
 * {@link javax.management.MBeanServerConnection}. See the classes for examples.
 * <p>
 * Notable interfaces and starting points:
 * <ul>
 * <li>{@link org.openjdk.jmc.rjmx.IConnectionDescriptor} represents a way to reach a server.</li>
 * <li>{@link org.openjdk.jmc.rjmx.IConnectionHandle} is an active connection to a server. Must
 * always be closed when not used anymore.</li>
 * <li>{@link org.openjdk.jmc.rjmx.IServerHandle} is a handle used to connect to a server, share the
 * connection between users ( {@link org.openjdk.jmc.rjmx.IConnectionHandle}s), keep track of all
 * open connection handles and close the connection when all connection handles are closed.</li>
 * <li>{@link org.openjdk.jmc.rjmx.servermodel.IServer} represents the entry point to a single server
 * in the model.</li>
 * </ul>
 * Notice that the subscription thread is a daemon thread - if trying the example above in a main,
 * add a {@code Thread.sleep(10000)} before the disconnect.
 */
package org.openjdk.jmc.rjmx;
