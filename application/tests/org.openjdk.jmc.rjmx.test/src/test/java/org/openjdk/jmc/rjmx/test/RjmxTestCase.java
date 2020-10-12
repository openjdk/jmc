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

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXServiceURL;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.common.test.MCTestCase;
import org.openjdk.jmc.common.version.JavaVersionSupport;
import org.openjdk.jmc.rjmx.ConnectionDescriptorBuilder;
import org.openjdk.jmc.rjmx.ConnectionException;
import org.openjdk.jmc.rjmx.ConnectionToolkit;
import org.openjdk.jmc.rjmx.IConnectionDescriptor;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.IServerDescriptor;
import org.openjdk.jmc.rjmx.ServiceNotAvailableException;
import org.openjdk.jmc.rjmx.internal.DefaultConnectionHandle;
import org.openjdk.jmc.rjmx.internal.RJMXConnection;
import org.openjdk.jmc.rjmx.internal.ServerDescriptor;
import org.openjdk.jmc.rjmx.services.IDiagnosticCommandService;
import org.openjdk.jmc.rjmx.subscription.IMBeanHelperService;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataService;
import org.openjdk.jmc.rjmx.subscription.ISubscriptionService;
import org.openjdk.jmc.ui.common.jvm.Connectable;
import org.openjdk.jmc.ui.common.jvm.JVMDescriptor;
import org.openjdk.jmc.ui.common.jvm.JVMType;

/**
 */
public class RjmxTestCase extends MCTestCase {
	/**
	 * The host running the management server.
	 */
	public final static String PROPERTY_RJMX_HOST = "jmc.test.rjmx.host";

	/**
	 * The port of the management server. (Used for both JMX over RMI and RMP.)
	 */
	public final static String PROPERTY_RJMX_PORT = "jmc.test.rjmx.port";

	/**
	 * Boolean option to use RMP to talk to the management server. (False means to use JMX over
	 * RMI.)
	 */
	public final static String PROPERTY_RJMX_RMP = "jmc.test.rjmx.rmp";

	/**
	 * The service URL to the management server. (If set, has precedence over host, port and
	 * protocol.)
	 */
	public final static String PROPERTY_JMX_SERVICE_URL = "jmc.test.rjmx.serviceURL";

	/**
	 * The default host to test against.
	 */
	public final static String DEFAULT_HOST = "localhost";

	protected String m_host;
	protected RJMXConnection m_connection;
	protected IConnectionHandle m_connectionHandle;
	protected IConnectionDescriptor m_connectionDescriptor;

	protected boolean isLocal14 = false;

	/**
	 * Do not change access. Use {@link #getDefaultConnectionDescriptor()} instead.
	 */
	private static volatile IConnectionDescriptor SHARED_DESCRIPTOR;

	/**
	 * Obtain a RJMX ConnectionDescriptor for the server to run tests against. The descriptor is
	 * formed by taking "jmc.test.rjmx.*" properties and the JDK level of the current JVM into
	 * account. If more than one is possible, attempts to probe once.
	 *
	 * @return The ConnectionDescriptor
	 * @throws MalformedURLException
	 */
	public static IConnectionDescriptor getDefaultConnectionDescriptor() throws MalformedURLException {
		if (SHARED_DESCRIPTOR == null) {
			String serviceURL = System.getProperty(PROPERTY_JMX_SERVICE_URL);
			if (serviceURL != null) {
				SHARED_DESCRIPTOR = new ConnectionDescriptorBuilder().url(new JMXServiceURL(serviceURL)).build();
			} else {
				String host = System.getProperty(PROPERTY_RJMX_HOST, DEFAULT_HOST);
				int jmxPort = Integer.getInteger(PROPERTY_RJMX_PORT, ConnectionDescriptorBuilder.DEFAULT_PORT)
						.intValue();
				IConnectionDescriptor candidate = new ConnectionDescriptorBuilder().hostName(host).port(jmxPort)
						.build();
				SHARED_DESCRIPTOR = candidate;
			}
		}
		return SHARED_DESCRIPTOR;
	}

	/**
	 * Creates a server descriptor with information matching the currently running JVM.
	 * 
	 * @return the server descriptor.
	 */
	public static IServerDescriptor createDefaultServerDesciptor() {
		String jvmName = System.getProperty("java.vm.name");
		// Assume hooking up to same JVM version as running the tests...
		JVMDescriptor jvmDescriptor = new JVMDescriptor(System.getProperty("java.vm.version"),
				JVMType.getJVMType(jvmName), null, "", "", jvmName, System.getProperty("java.vm.vendor"), null, false,
				Connectable.MGMNT_AGENT_STARTED);
		return new ServerDescriptor(null, "Test", jvmDescriptor);
	}

	/**
	 * Creates a server descriptor with information derived from the JVM with the provided
	 * connection.
	 * 
	 * @param connection
	 *            an active {@link MBeanServerConnection}.
	 * @return the server descriptor.
	 * @throws IOException
	 */
	public static IServerDescriptor createDefaultServerDesciptor(MBeanServerConnection connection) throws IOException {
		Map<String, String> properties = ConnectionToolkit.getRuntimeBean(connection).getSystemProperties();
		String jvmName = properties.get("java.vm.name");
		// Assume hooking up to same JVM version as running the tests...
		JVMDescriptor jvmDescriptor = new JVMDescriptor(properties.get("java.vm.version"), JVMType.getJVMType(jvmName),
				null, "", "", jvmName, properties.get("java.vm.vendor"), null, false, Connectable.MGMNT_AGENT_STARTED);
		return new ServerDescriptor(null, "Test", jvmDescriptor);
	}

	/**
	 * Creates a server descriptor with information derived from the JVM described by the server
	 * descriptor. Will connect temporarily to derive the information.
	 * 
	 * @param descriptor
	 *            the descriptor defining the JVM to connect to.
	 * @return the server descriptor, with information derived from the actual connection, or an
	 *         {@link IOException} if one could not be derived.
	 * @throws IOException
	 */
	public static IServerDescriptor createDefaultServerDesciptor(IConnectionDescriptor descriptor) throws IOException {
		RJMXConnection rjmxConnection = new RJMXConnection(descriptor, RjmxTestCase.createDefaultServerDesciptor(),
				null);
		if (!rjmxConnection.connect()) {
			rjmxConnection.close();
			throw new IOException("Could not connect to " + descriptor);
		}
		try (DefaultConnectionHandle handle = new DefaultConnectionHandle(rjmxConnection, "derive server descriptor",
				null)) {
			MBeanServerConnection mbeanServer = handle.getServiceOrNull(MBeanServerConnection.class);
			if (mbeanServer != null) {
				return createDefaultServerDesciptor(mbeanServer);
			}
		} finally {
			IOToolkit.closeSilently(rjmxConnection);
		}
		throw new IOException("Could not derive the server descriptor for " + descriptor.toString());
	}

	protected static boolean probe(IConnectionDescriptor descriptor) {
		long start = System.currentTimeMillis();
		IConnectionHandle handle = null;
		RJMXConnection connection = null;
		try {
			System.out.println("Probing Service URL " + descriptor.createJMXServiceURL() + " ...");
			connection = new RJMXConnection(descriptor, createDefaultServerDesciptor(descriptor), null);
			connection.connect();
			handle = new DefaultConnectionHandle(connection, "Test", null);
			long up = System.currentTimeMillis();
			System.out.println("... connected in " + (up - start) + " ms ...");
			// Just in case we fail ...
			start = up;
			handle.close();
			long down = System.currentTimeMillis();
			System.out.println("... closed in " + (down - start) + " ms.");
			return true;
		} catch (Exception e) {
			long fail = System.currentTimeMillis();
			IOToolkit.closeSilently(handle);
			IOToolkit.closeSilently(connection);
			System.out.println("... failed in " + (fail - start) + " ms.");
			return false;
		}
	}

	/**
	 * The {@link IConnectionDescriptor} to run the test against. Instance method so it can be
	 * overridden. Mainly intended for tests suites that run against multiple JVMs.
	 *
	 * @return The ConnectionDescriptor
	 * @throws MalformedURLException
	 */
	protected IConnectionDescriptor getTestConnectionDescriptor() throws MalformedURLException {
		return getDefaultConnectionDescriptor();
	}

	/**
	 * Adds all system properties from the given connector to props with the given prefix. No
	 * parameters are allowed to be null.
	 *
	 * @param connector
	 * @param props
	 * @param prefix
	 *            prefix to use for server properties
	 * @throws Exception
	 */
	public static void getServerProperties(IConnectionHandle connector, Properties props, String prefix)
			throws Exception {
		System.out.println("Retrieving system properties (prefixed with '" + prefix + "') ...");
		MBeanServerConnection server = connector.getServiceOrThrow(MBeanServerConnection.class);
		Map<String, String> systemProperties = ConnectionToolkit.getRuntimeBean(server).getSystemProperties();
		if (systemProperties != null) {
			for (Entry<String, String> e : systemProperties.entrySet()) {
				props.setProperty(prefix + e.getKey(), e.getValue());
			}
		} else {
			System.out.println("Could not retrieve system properties");
		}
	}

	/**
	 * Adds all system properties from the given ConnectionDescriptor to props with the given
	 * prefix. No parameters are allowed to be null.
	 *
	 * @param connDesc
	 * @param props
	 * @param prefix
	 *            prefix to use for server properties
	 * @throws Exception
	 */
	public static void getServerProperties(IConnectionDescriptor connDesc, Properties props, String prefix)
			throws Exception {
		System.out.println("Connecting to " + connDesc.createJMXServiceURL() + " ...");
		RJMXConnection connection = new RJMXConnection(connDesc, createDefaultServerDesciptor(connDesc), null);
		IConnectionHandle connectionHandle = new DefaultConnectionHandle(connection, "Get Properties", null);
		getServerProperties(connectionHandle, props, prefix);
		System.out.println("Disconnecting ...");
		IOToolkit.closeSilently(connectionHandle);
		IOToolkit.closeSilently(connection);
	}

	/**
	 * @see org.junit.Test#Before
	 */
	@Before
	public synchronized void mcTestCaseBefore() throws Exception {
		m_connectionDescriptor = getTestConnectionDescriptor();
		m_host = ConnectionToolkit.getHostName(m_connectionDescriptor.createJMXServiceURL());
		m_connection = new RJMXConnection(m_connectionDescriptor, createDefaultServerDesciptor(m_connectionDescriptor),
				null);
		m_connection.connect();
		m_connectionHandle = new DefaultConnectionHandle(m_connection, "Test", null);
		Assert.assertTrue(m_connectionHandle.isConnected());
	}

	/**
	 * @see org.junit.Test#After
	 */
	@After
	public synchronized void mcTestCaseAfter() throws Exception {
		if (m_connectionHandle != null) {
			m_connectionHandle.close();
			m_connectionHandle = null;
			if (m_connection != null) {
				m_connection.close();
				m_connection = null;
			}
		}
	}

	protected MBeanServerConnection getMBeanServerConnection()
			throws ConnectionException, ServiceNotAvailableException {
		return m_connectionHandle.getServiceOrThrow(MBeanServerConnection.class);
	}

	protected IMBeanHelperService getMBeanHelperService() throws ConnectionException, ServiceNotAvailableException {
		return m_connectionHandle.getServiceOrThrow(IMBeanHelperService.class);
	}

	protected IMRIMetadataService getMRIMetadataService() throws ConnectionException, ServiceNotAvailableException {
		return m_connectionHandle.getServiceOrThrow(IMRIMetadataService.class);
	}

	protected ISubscriptionService getAttributeSubscriptionService()
			throws ConnectionException, ServiceNotAvailableException {
		return m_connectionHandle.getServiceOrThrow(ISubscriptionService.class);
	}

	protected IDiagnosticCommandService getDiagnosticCommandService()
			throws ConnectionException, ServiceNotAvailableException {
		assumeHasDiagnosticCommandsService(m_connectionHandle);
		return m_connectionHandle.getServiceOrThrow(IDiagnosticCommandService.class);
	}

	protected IConnectionHandle getConnectionHandle() {
		return m_connectionHandle;
	}

	protected void assumeHotSpot8OrLater(IConnectionHandle handle) {
		Assume.assumeTrue("This test assumes JDK 8 (HotSpot 25) or later!", ConnectionToolkit.isHotSpot(handle)
				&& ConnectionToolkit.isJavaVersionAboveOrEqual(handle, JavaVersionSupport.JDK_8));
	}

	protected void assumeHotSpot7u4OrLater(IConnectionHandle handle) {
		Assume.assumeTrue("This test assumes JDK 7u4 (HotSpot 23) or later!", ConnectionToolkit.isHotSpot(handle)
				&& ConnectionToolkit.isJavaVersionAboveOrEqual(handle, JavaVersionSupport.JDK_7_U_4));
	}

	protected void assumeHotSpot7u12OrLater(IConnectionHandle handle) {
		Assume.assumeTrue("This test assumes JDK 7u12 (HotSpot 24) or later!", ConnectionToolkit.isHotSpot(handle)
				&& ConnectionToolkit.isJavaVersionAboveOrEqual(handle, JavaVersionSupport.JDK_7_U_40));
	}

	protected void assumeHasDiagnosticCommandsService(IConnectionHandle handle) {
		Assume.assumeTrue("This test needs a working diagnostic commands service!",
				handle.hasService(IDiagnosticCommandService.class));
	}
}
