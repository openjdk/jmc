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
package org.openjdk.jmc.rjmx;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;

import javax.management.JMException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXServiceURL;

import org.openjdk.jmc.common.version.JavaVMVersionToolkit;
import org.openjdk.jmc.common.version.JavaVersion;
import org.openjdk.jmc.rjmx.internal.RJMXConnection;

/**
 * Toolkit providing utility methods to retrieve MBean proxy objects, invoke JMX operations and
 * query a connection about its properties.
 */
public final class ConnectionToolkit {

	/**
	 * Object name for the {@link ManagementFactory#RUNTIME_MXBEAN_NAME} constant.
	 */
	public static final ObjectName RUNTIME_BEAN_NAME = createObjectName(ManagementFactory.RUNTIME_MXBEAN_NAME);
	/**
	 * Object name for the {@link ManagementFactory#MEMORY_MXBEAN_NAME} constant.
	 */
	public static final ObjectName MEMORY_BEAN_NAME = createObjectName(ManagementFactory.MEMORY_MXBEAN_NAME);
	/**
	 * Object name for the {@link ManagementFactory#THREAD_MXBEAN_NAME} constant.
	 */
	public static final ObjectName THREAD_BEAN_NAME = createObjectName(ManagementFactory.THREAD_MXBEAN_NAME);
	/**
	 * Object name for the {@link ManagementFactory#OPERATING_SYSTEM_MXBEAN_NAME} constant.
	 */
	public static final ObjectName OPERATING_SYSTEM_BEAN_NAME = createObjectName(
			ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);

	private ConnectionToolkit() {
		throw new IllegalArgumentException("Don't instantiate this toolkit"); //$NON-NLS-1$
	}

	/**
	 * Creates an object name for an MBean. Hides the fact that a
	 * {@link MalformedObjectNameException} might be thrown if the passed string has the wrong
	 * format.
	 *
	 * @param name
	 *            name of the object.
	 * @return the ObjectName
	 * @throws IllegalArgumentException
	 *             if an object name could not be created from the string
	 */
	public static ObjectName createObjectName(String name) {
		try {
			return new ObjectName(name);
		} catch (MalformedObjectNameException e) {
			// Should not happen - programmer error!
			assert (false);
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	/**
	 * Helper method to retrieve proxy object for platform Memory MXBean
	 * ({@code "java.lang:type=Memory"}).
	 *
	 * @param server
	 *            the connected server
	 * @return a proxy object or {@code null} if it does not exist.
	 * @throws IOException
	 *             if a communication problem occurred.
	 * @see ManagementFactory#newPlatformMXBeanProxy(MBeanServerConnection, String, Class)
	 */
	public static MemoryMXBean getMemoryBean(MBeanServerConnection server) throws IOException {
		return ManagementFactory.newPlatformMXBeanProxy(server, ManagementFactory.MEMORY_MXBEAN_NAME,
				MemoryMXBean.class);
	}

	/**
	 * Helper method to retrieve proxy object for platform Runtime MXBean
	 * ({@code "java.lang:type=Runtime"}).
	 *
	 * @param server
	 *            the connected server
	 * @return a proxy object or {@code null} if it does not exist.
	 * @throws IOException
	 *             if a communication problem occurred.
	 * @see ManagementFactory#newPlatformMXBeanProxy(MBeanServerConnection, String, Class)
	 */
	public static RuntimeMXBean getRuntimeBean(MBeanServerConnection server) throws IOException {
		return ManagementFactory.newPlatformMXBeanProxy(server, ManagementFactory.RUNTIME_MXBEAN_NAME,
				RuntimeMXBean.class);
	}

	/**
	 * Helper method to retrieve proxy object for platform Memory MXBean
	 * ({@code "java.lang:type=Threading"}).
	 *
	 * @param server
	 *            the connected server
	 * @return a proxy object or {@code null} if it does not exist.
	 * @throws IOException
	 *             if a communication problem occurred.
	 * @see ManagementFactory#newPlatformMXBeanProxy(MBeanServerConnection, String, Class)
	 */
	public static ThreadMXBean getThreadBean(MBeanServerConnection server) throws IOException {
		return ManagementFactory.newPlatformMXBeanProxy(server, ManagementFactory.THREAD_MXBEAN_NAME,
				ThreadMXBean.class);
	}

	/**
	 * Helper method to retrieve proxy object for platform Memory MXBean
	 * ({@code "java.lang:type=OperatingSystem"}).
	 *
	 * @param server
	 *            the connected server
	 * @return a proxy object or {@code null} if it does not exist.
	 * @throws IOException
	 *             if a communication problem occurred.
	 * @see ManagementFactory#newPlatformMXBeanProxy(MBeanServerConnection, String, Class)
	 */
	public static OperatingSystemMXBean getOperatingSystemBean(MBeanServerConnection server) throws IOException {
		return ManagementFactory.newPlatformMXBeanProxy(server, ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME,
				OperatingSystemMXBean.class);
	}

	/**
	 * Helper method to invoke MBean operation on a MBean server. Will try to deduce the correct
	 * method to invoke based on the provided parameters.
	 *
	 * @param server
	 *            the MBean server to invoke method on.
	 * @param on
	 *            the name of the MBean.
	 * @param operation
	 *            the name of the operation.
	 * @param parameters
	 *            the parameters for the method invocation.
	 * @return the object returned by the operation, which represents the result of invoking the
	 *         operation on the MBean specified.
	 * @throws JMException
	 *             some sort of exception due to unknown MBean or exception thrown in invoked
	 *             method.
	 * @throws IOException
	 *             if a communication problem occurred when talking to the MBean server.
	 */
	public static Object invokeOperation(
		MBeanServerConnection server, ObjectName on, String operation, Object ... parameters)
			throws JMException, IOException {
		return server.invoke(on, operation, parameters, extractSignature(parameters));
	}

	/**
	 * Automatically generates the signature to be used when invoking operations.
	 *
	 * @param param
	 *            the parameters for which to get the signature.
	 * @return the signature matching the parameters.
	 */
	private static String[] extractSignature(Object[] param) {
		String[] sig = new String[param.length];
		for (int i = 0; i < sig.length; i++) {
			if (param[i].getClass() == Boolean.class) {
				sig[i] = Boolean.TYPE.getName();
			} else if (Number.class.isAssignableFrom(param[i].getClass())) {
				try {
					sig[i] = ((Class<?>) param[i].getClass().getField("TYPE").get(param[i])).getName(); //$NON-NLS-1$
				} catch (IllegalArgumentException e) {
					throw new UndeclaredThrowableException(e);
				} catch (SecurityException e) {
					throw new UndeclaredThrowableException(e);
				} catch (IllegalAccessException e) {
					throw new UndeclaredThrowableException(e);
				} catch (NoSuchFieldException e) {
					throw new UndeclaredThrowableException(e);
				}
			} else if (CompositeData.class.isAssignableFrom(param[i].getClass())) {
				sig[i] = CompositeData.class.getName();
			} else if (TabularData.class.isAssignableFrom(param[i].getClass())) {
				sig[i] = TabularData.class.getName();
			} else if (List.class.isAssignableFrom(param[i].getClass())) {
				sig[i] = List.class.getName();
			} else {
				sig[i] = param[i].getClass().getName();
			}
		}
		return sig;
	}

	/**
	 * Will attempt to derive the host name from the {@link JMXServiceURL}. If the JXMServiceURL
	 * uses jmxrmi, the host name will be derived from the information in the JXMServiceURL.
	 *
	 * @param url
	 *            the {@link JMXServiceURL} to retrieve the host name from.
	 * @return the host name.
	 */
	public static String getHostName(JMXServiceURL url) {
		if (url.getHost() == null || "".equals(url.getHost().trim())) { //$NON-NLS-1$
			return deriveHost(url);
		} else {
			return url.getHost();
		}
	}

	/**
	 * Will attempt to derive the port from the {@link JMXServiceURL}. If the JXMServiceURL uses
	 * jmxrmi, the port will be derived from the information in the JXMServiceURL.
	 *
	 * @param url
	 *            the {@link JMXServiceURL} to derive the port from.
	 * @return the port number
	 */
	public static int getPort(JMXServiceURL url) {
		if (url.getPort() <= 0) {
			return derivePort(url);
		} else {
			return url.getPort();
		}
	}

	/**
	 * Helper method to try to derive the host name from a standard jmxrmi JMX service URL.
	 *
	 * @param url
	 *            service URL.
	 * @return the host name.
	 */
	private static String deriveHost(JMXServiceURL url) {
		StringTokenizer st = new StringTokenizer(url.getURLPath(), ":/"); //$NON-NLS-1$
		if (st.countTokens() == 5) {
			for (int i = 0; i < 2; i++) {
				st.nextToken();
			}
			String host = st.nextToken();
			// strip dashes
			return host;
		}
		return "unknown"; //$NON-NLS-1$
	}

	/**
	 * Evil helper method to try to derive the port number from a standard jmxrmi JMX service URL.
	 *
	 * @param url
	 *            service URL.
	 * @return the port number.
	 */
	private static int derivePort(JMXServiceURL url) {
		StringTokenizer st = new StringTokenizer(url.getURLPath(), ":/"); //$NON-NLS-1$
		if (st.countTokens() == 5) {
			for (int i = 0; i < 3; i++) {
				st.nextToken();
			}
			String port = st.nextToken();
			try {
				return Integer.parseInt(port);
			} catch (NumberFormatException e) {
				return -1;
			}
		}
		return 0;
	}

	/**
	 * Creates a "JMX over RMI" or "JMX over RMP" service URL.
	 *
	 * @param host
	 *            the host name.
	 * @param port
	 *            port or {@link ConnectionDescriptorBuilder#DEFAULT_PORT} for the default port for
	 *            the selected protocol
	 * @return the {@link JMXServiceURL}.
	 * @throws MalformedURLException
	 *             if the URL could not be created with the provided data.
	 */
	public static JMXServiceURL createServiceURL(String host, int port) throws MalformedURLException {
		int actualPort = (port != ConnectionDescriptorBuilder.DEFAULT_PORT) ? port
				: RJMXConnection.VALUE_DEFAULT_REMOTE_PORT_JMX;
		return new JMXServiceURL("rmi", "", 0, "/jndi/rmi://" + host + ':' + actualPort + "/jmxrmi"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	/**
	 * Returns the default port number for the management agent.
	 *
	 * @return the default port for the management agent. May vary depending on which JVM version
	 *         the method is executed in.
	 */
	public static int getDefaultPort() {
		return RJMXConnection.VALUE_DEFAULT_REMOTE_PORT_JMX;
	}

	/**
	 * Returns {@code true} if the connection handle is connected to a JRockit, {@code false}
	 * otherwise.
	 *
	 * @param connectionHandle
	 *            the connection handle to check.
	 * @return {@code true} if the connection handle is connected to a JRockit, {@code false}
	 *         otherwise.
	 */
	public static boolean isJRockit(IConnectionHandle connectionHandle) {

		String vmName = getVMName(connectionHandle);
		return JavaVMVersionToolkit.isJRockitJVMName(vmName);
	}

	/**
	 * Returns {@code true} if the connection handle is connected to a HotSpot, {@code false}
	 * otherwise. This method requires the connection handle to be connected.
	 *
	 * @param connectionHandle
	 *            the connection handle to check.
	 * @return {@code true} if the connection handle is connected to a HotSpot, {@code false}
	 *         otherwise.
	 */
	public static boolean isHotSpot(IConnectionHandle connectionHandle) {
		String vmName = getVMName(connectionHandle);
		return vmName != null && JavaVMVersionToolkit.isHotspotJVMName(vmName);
	}

	/**
	 * This will return true if the java version is above or equal the supplied value. (For example
	 * 1.7.0_40).
	 *
	 * @param connectionHandle
	 *            the connectionHandle to check.
	 * @param minVersion
	 *            the java version needed.
	 * @return {@code true} if the version is above or equal the supplied value, {@code true} if no
	 *         version can be obtained from the connection, {@code false} otherwise.
	 */
	public static boolean isJavaVersionAboveOrEqual(IConnectionHandle connectionHandle, JavaVersion minVersion) {
		JavaVersion version = getJavaVersion(connectionHandle);
		return version != null ? version.isGreaterOrEqualThan(minVersion) : true;
	}

	private static String getVMName(IConnectionHandle connectionHandle) {
		MBeanServerConnection connection = connectionHandle.getServiceOrDummy(MBeanServerConnection.class);
		try {
			// getAttribute may fail if the connection handle
			// has just been disconnected by the user, which is not a problem
			return getRuntimeBean(connection).getVmName();

		} catch (Exception e) {
			RJMXPlugin.getDefault().getLogger().log(Level.INFO, "Could not check the JVM name!", e); //$NON-NLS-1$
		}
		return null;
	}

	private static JavaVersion getJavaVersion(IConnectionHandle connectionHandle) {
		try {
			MBeanServerConnection server = connectionHandle.getServiceOrThrow(MBeanServerConnection.class);
			Map<String, String> serverProps = getRuntimeBean(server).getSystemProperties();
			String javaVersion = serverProps.get("java.version"); //$NON-NLS-1$
			if (javaVersion != null) {
				return new JavaVersion(javaVersion);
			}
			RJMXPlugin.getDefault().getLogger().log(Level.WARNING,
					"System Properties from " + connectionHandle.getDescription() //$NON-NLS-1$
							+ " contained no java.version property!"); //$NON-NLS-1$
		} catch (Exception e) {
			RJMXPlugin.getDefault().getLogger().log(Level.WARNING,
					"Could not check the java.version from System Properties!", e); //$NON-NLS-1$
		}
		return null;
	}

}
