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
import javax.security.auth.login.FailedLoginException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.openjdk.jmc.common.test.MCTestCase;
import org.openjdk.jmc.common.version.JavaVersionSupport;
import org.openjdk.jmc.rjmx.ConnectionDescriptorBuilder;
import org.openjdk.jmc.rjmx.ConnectionException;
import org.openjdk.jmc.rjmx.ConnectionToolkit;
import org.openjdk.jmc.rjmx.IConnectionDescriptor;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.IServerHandle;
import org.openjdk.jmc.rjmx.ServiceNotAvailableException;
import org.openjdk.jmc.rjmx.services.IDiagnosticCommandService;
import org.openjdk.jmc.rjmx.subscription.IMBeanHelperService;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataService;
import org.openjdk.jmc.rjmx.subscription.ISubscriptionService;

/**
 */
public class RjmxTestCase extends MCTestCase {
	/**
	 * The host running the management server.
	 */
	public final static String PROPERTY_RJMX_HOST = "jmc.test.rjmx.host"; //$NON-NLS-1$

	/**
	 * The port of the management server. (Used for both JMX over RMI and RMP.)
	 */
	public final static String PROPERTY_RJMX_PORT = "jmc.test.rjmx.port"; //$NON-NLS-1$

	/**
	 * Boolean option to use RMP to talk to the management server. (False means to use JMX over
	 * RMI.)
	 */
	public final static String PROPERTY_RJMX_RMP = "jmc.test.rjmx.rmp"; //$NON-NLS-1$

	/**
	 * The service URL to the management server. (If set, has precedence over host, port and
	 * protocol.)
	 */
	public final static String PROPERTY_JMX_SERVICE_URL = "jmc.test.rjmx.serviceURL"; //$NON-NLS-1$

	/**
	 * The default host to test against.
	 */
	public final static String DEFAULT_HOST = "localhost"; //$NON-NLS-1$

	protected String m_host;
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

	protected static boolean probe(IConnectionDescriptor descriptor) {
		long start = System.currentTimeMillis();
		try {
			System.out.println("Probing Service URL " + descriptor.createJMXServiceURL() + " ..."); //$NON-NLS-1$ //$NON-NLS-2$
			IConnectionHandle handle = createConnectionHandle(descriptor);
			long up = System.currentTimeMillis();
			System.out.println("... connected in " + (up - start) + " ms ..."); //$NON-NLS-1$ //$NON-NLS-2$
			// Just in case we fail ...
			start = up;
			handle.close();
			long down = System.currentTimeMillis();
			System.out.println("... closed in " + (down - start) + " ms."); //$NON-NLS-1$ //$NON-NLS-2$
			return true;
		} catch (Exception e) {
			long fail = System.currentTimeMillis();
			System.out.println("... failed in " + (fail - start) + " ms."); //$NON-NLS-1$ //$NON-NLS-2$
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
		System.out.println("Retrieving system properties (prefixed with '" + prefix + "') ..."); //$NON-NLS-1$ //$NON-NLS-2$
		MBeanServerConnection server = connector.getServiceOrThrow(MBeanServerConnection.class);
		Map<String, String> systemProperties = ConnectionToolkit.getRuntimeBean(server).getSystemProperties();
		if (systemProperties != null) {
			for (Entry<String, String> e : systemProperties.entrySet()) {
				props.setProperty(prefix + e.getKey(), e.getValue());
			}
		} else {
			System.out.println("Could not retrieve system properties"); //$NON-NLS-1$
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
		System.out.println("Connecting to " + connDesc.createJMXServiceURL() + " ..."); //$NON-NLS-1$ //$NON-NLS-2$
		IConnectionHandle connectionHandle = createConnectionHandle(connDesc);
		getServerProperties(connectionHandle, props, prefix);
		System.out.println("Disconnecting ..."); //$NON-NLS-1$
		connectionHandle.close();
	}

	/**
	 * @see org.junit.Test#Before
	 */
	@Before
	public synchronized void mcTestCaseBefore() throws Exception {
		m_connectionDescriptor = getTestConnectionDescriptor();
		m_host = ConnectionToolkit.getHostName(m_connectionDescriptor.createJMXServiceURL());
		m_connectionHandle = createConnectionHandle(m_connectionDescriptor);
		Assert.assertTrue(m_connectionHandle.isConnected());
	}

	/**
	 * Quick'n'Dirty way to create a {@link IConnectionHandle}.
	 *
	 * @return an {@link IConnectionHandle}
	 * @throws FailedLoginException
	 * @throws ConnectionException
	 */
	private static IConnectionHandle createConnectionHandle(IConnectionDescriptor descriptor)
			throws IOException, FailedLoginException, ConnectionException {
		return IServerHandle.create(descriptor).connect("Test"); //$NON-NLS-1$
	}

	/**
	 * @see org.junit.Test#After
	 */
	@After
	public synchronized void mcTestCaseAfter() throws Exception {
		if (m_connectionHandle != null) {
			m_connectionHandle.close();
			m_connectionHandle = null;
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
		Assume.assumeTrue("This test assumes JDK 8 (HotSpot 25) or later!", ConnectionToolkit.isHotSpot(handle) //$NON-NLS-1$
				&& ConnectionToolkit.isJavaVersionAboveOrEqual(handle, JavaVersionSupport.JDK_8));
	}

	protected void assumeHotSpot7u4OrLater(IConnectionHandle handle) {
		Assume.assumeTrue("This test assumes JDK 7u4 (HotSpot 23) or later!", ConnectionToolkit.isHotSpot(handle) //$NON-NLS-1$
				&& ConnectionToolkit.isJavaVersionAboveOrEqual(handle, JavaVersionSupport.JDK_7_U_4));
	}

	protected void assumeHotSpot7u12OrLater(IConnectionHandle handle) {
		Assume.assumeTrue("This test assumes JDK 7u12 (HotSpot 24) or later!", ConnectionToolkit.isHotSpot(handle) //$NON-NLS-1$
				&& ConnectionToolkit.isJavaVersionAboveOrEqual(handle, JavaVersionSupport.JDK_7_U_40));
	}

	protected void assumeHasDiagnosticCommandsService(IConnectionHandle handle) {
		Assume.assumeTrue("This test needs a working diagnostic commands service!", //$NON-NLS-1$
				handle.hasService(IDiagnosticCommandService.class));
	}
}
