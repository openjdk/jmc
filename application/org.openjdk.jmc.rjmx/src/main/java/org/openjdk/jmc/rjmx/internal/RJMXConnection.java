/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.rjmx.internal;

import java.io.Closeable;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.rmi.UnmarshalException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.JMRuntimeException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerNotification;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.rmi.ssl.SslRMIClientSocketFactory;

import org.eclipse.core.runtime.ListenerList;
import org.openjdk.jmc.common.version.JavaVersion;
import org.openjdk.jmc.common.version.JavaVersionSupport;
import org.openjdk.jmc.rjmx.ConnectionException;
import org.openjdk.jmc.rjmx.ConnectionToolkit;
import org.openjdk.jmc.rjmx.IConnectionDescriptor;
import org.openjdk.jmc.rjmx.IServerDescriptor;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.services.IOperation;
import org.openjdk.jmc.rjmx.subscription.IMBeanHelperService;
import org.openjdk.jmc.rjmx.subscription.IMBeanServerChangeListener;
import org.openjdk.jmc.rjmx.subscription.IMRIService;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.internal.AttributeValueToolkit;
import org.openjdk.jmc.rjmx.subscription.internal.InvoluntaryDisconnectException;
import org.openjdk.jmc.rjmx.subscription.internal.MBeanMRIMetadataDB;
import org.openjdk.jmc.ui.common.jvm.JVMDescriptor;

/**
 * This class simplifies and hides some of the complexity of connecting to a JVM (supporting JSR-174
 * and JSR-160) using Remote JMX. The RJMXConnection is shared between several
 * {@link DefaultConnectionHandle}s, and when the last {@link DefaultConnectionHandle} using the
 * JRMXConnection is closed, the RJMXConnection will be automatically closed.
 */
public class RJMXConnection implements Closeable, IMBeanHelperService {

	public final static String KEY_SOCKET_FACTORY = "com.sun.jndi.rmi.factory.socket"; //$NON-NLS-1$

	/**
	 * The default port JMX
	 */
	public static final int VALUE_DEFAULT_REMOTE_PORT_JMX = 7091;

	/**
	 * Default recalibration interval. The server to client timediff is recalibrated every two
	 * minutes per default.
	 */
	private static final long VALUE_RECALIBRATION_INTERVAL = 120000;
	private static final long REMOTE_START_TIME_UNDEFINED = -1;

	// The ConnectionDescriptor used to create this RJMXConnection
	private final IConnectionDescriptor m_connectionDescriptor;

	private final IServerDescriptor m_serverDescriptor;

	// The MBean server connection used for all local and remote communication.
	private volatile MCMBeanServerConnection m_server;

	// The underlying JMX connection used when communicating remote.
	private JMXConnector m_jmxc;

	private final MBeanMRIMetadataDB m_mbeanDataProvider;

	// Variables used for calibrating the offset to the server clock.
	private long m_serverOffset;
	private long m_lastRecalibration;
	private long m_remoteStartTime = REMOTE_START_TIME_UNDEFINED;

	private boolean m_hasInitializedAllMBeans = false;
	private final HashMap<ObjectName, MBeanInfo> m_cachedInfos = new HashMap<>();
	private volatile Set<ObjectName> m_cachedMBeanNames = new HashSet<>();
	private final Runnable m_onFailCallback;
	private final ListenerList<IMBeanServerChangeListener> m_mbeanListeners = new ListenerList<>();
	private final NotificationListener m_registrationListener = new NotificationListener() {
		@Override
		public void handleNotification(Notification notification, Object handback) {
			if (notification instanceof MBeanServerNotification) {
				ObjectName name = ((MBeanServerNotification) notification).getMBeanName();
				if (notification.getType().equals(MBeanServerNotification.REGISTRATION_NOTIFICATION)) {
					try {
						synchronized (m_cachedInfos) {
							getMBeanInfo(name);
							if (m_cachedMBeanNames.size() > 0) {
								m_cachedMBeanNames.add(name);
							}
						}
						for (IMBeanServerChangeListener l : m_mbeanListeners) {
							l.mbeanRegistered(name);
						}
					} catch (Exception e) {
						RJMXPlugin.getDefault().getLogger().log(Level.WARNING,
								"Could not retrieve MBean information for " + name + '!', e); //$NON-NLS-1$
					}
				} else if (notification.getType().equals(MBeanServerNotification.UNREGISTRATION_NOTIFICATION)) {
					synchronized (m_cachedInfos) {
						m_cachedInfos.remove(name);
						m_cachedMBeanNames.remove(name);
					}
					for (IMBeanServerChangeListener l : m_mbeanListeners) {
						l.mbeanUnregistered(name);
					}
				}
			}
		}
	};

	private final NotificationListener m_disconnectListener = new NotificationListener() {

		@Override
		public void handleNotification(Notification notification, Object handback) {
			if (notification != null && (JMXConnectionNotification.CLOSED.equals(notification.getType())
					|| JMXConnectionNotification.FAILED.equals(notification.getType()))) {
				close();
				if (m_onFailCallback != null) {
					m_onFailCallback.run();
				}
			}
		}

	};

	private final Object connectionStateLock = new Object();

	/**
	 * Creates a new remote JMX connection to the specified host, using the supplied credentials. If
	 * password is null or empty, it will be ignored. Will attempt to set up a connection to the
	 * server immediately. The Constructor will fail if no connection could be established.
	 *
	 * @throws MalformedURLException
	 */
	public RJMXConnection(IConnectionDescriptor connectionDescriptor, IServerDescriptor serverDescriptor,
			Runnable onFailCallback) {
		if (connectionDescriptor == null) {
			throw new IllegalArgumentException("Connection descriptor must not be null!"); //$NON-NLS-1$
		}
		if (serverDescriptor == null) {
			throw new IllegalArgumentException("Server descriptor must not be null!"); //$NON-NLS-1$
		}
		m_onFailCallback = onFailCallback;
		m_connectionDescriptor = connectionDescriptor;
		m_serverDescriptor = serverDescriptor;
		m_mbeanDataProvider = new MBeanMRIMetadataDB(this);
		addMBeanServerChangeListener(m_mbeanDataProvider);
	}

	public IServerDescriptor getServerDescriptor() {
		return m_serverDescriptor;
	}

	public IConnectionDescriptor getConnectionDescriptor() {
		return m_connectionDescriptor;
	}

	/**
	 * Disconnects the connection from the RJMX server
	 */
	@Override
	public void close() {
		synchronized (connectionStateLock) {
			if (isConnected()) {
				m_server.dispose();
				tryRemovingListener();
				clearCollections();
				m_server = null;
				if (m_jmxc != null) {
					try {
						m_jmxc.close();
					} catch (Exception e) {
						RJMXPlugin.getDefault().getLogger().log(Level.INFO, "Problem when closing connection.", e); //$NON-NLS-1$
					}
					m_jmxc = null;
				}
			}
		}
	}

	/**
	 * Sometimes we can fail to remove the unregister listeners from the MBeanConnection, causing
	 * JMX to keep a reference to this instance. To minimize impact if this happens, we clear all
	 * collections from data.
	 */
	private void clearCollections() {
		clearCache();
	}

	private void tryRemovingListener() {
		try {
			ensureConnected().removeNotificationListener(MBeanServerDelegate.DELEGATE_NAME, m_registrationListener);
		} catch (Exception e) {
			RJMXPlugin.getDefault().getLogger().log(Level.WARNING,
					"Failed to remove unregistration listener! Lost connection?", e); //$NON-NLS-1$
		}
	}

	/**
	 * Returns whether the underlying connector is connected
	 *
	 * @return true if the underlying connector is still connected
	 */
	public boolean isConnected() {
		return m_server != null;
	}

	@Override
	public Set<ObjectName> getMBeanNames() throws IOException {
		synchronized (m_cachedInfos) {
			if (m_cachedMBeanNames.size() == 0) {
				MBeanServerConnection server = ensureConnected();
				m_cachedMBeanNames = server.queryNames(null, null);
			}
			return new HashSet<>(m_cachedMBeanNames);
		}
	}

	/**
	 * Returns the bean information for the MBeans matching the domain and query.
	 *
	 * @param domain
	 *            the domain for which to retrieve the information.
	 * @param query
	 *            a query to filter for which MBeans to retrieve the information.
	 * @return a map with the ObjectNames and their associated MBeanInfos.
	 * @throws IOException
	 *             if the connection failed or some other IO related problem occurred.
	 * @throws MalformedObjectNameException
	 *             if a particularly malign (malformatted) domain was specified.
	 */
	private HashMap<ObjectName, MBeanInfo> getMBeanInfos(String domain, QueryExp query)
			throws MalformedObjectNameException, IOException {
		MBeanServerConnection server = ensureConnected();
		ObjectName objectName = null;
		int skippedMBeanCounter = 0;
		if (domain != null) {
			objectName = new ObjectName(domain + ":*"); //$NON-NLS-1$
		}
		Set<ObjectName> names = server.queryNames(objectName, query);
		HashMap<ObjectName, MBeanInfo> infos = new HashMap<>(names.size());

		Iterator<ObjectName> iter = names.iterator();
		while (iter.hasNext()) {
			ObjectName name = iter.next();
			try {
				infos.put(name, getMBeanInfo(name));
			} catch (NullPointerException e) {
				/*
				 * Skip problematic MBeans when connecting. Workaround implemented so that we can
				 * connect to JBoss 4.2.3.
				 */
				RJMXPlugin.getDefault().getLogger().log(Level.WARNING, "Skipping " + name.toString() //$NON-NLS-1$
						+ ". Could not retrieve the MBean info for the MBean. Set log level to fine for stacktrace!"); //$NON-NLS-1$
				RJMXPlugin.getDefault().getLogger().log(Level.FINE, e.getMessage(), e);
				skippedMBeanCounter++;
			} catch (UnmarshalException e) {
				RJMXPlugin.getDefault().getLogger().log(Level.WARNING, "Skipping " //$NON-NLS-1$
						+ name.toString()
						+ ". Could not retrieve the MBean info due to marshalling problems. Set log level to fine for stacktrace!"); //$NON-NLS-1$
				RJMXPlugin.getDefault().getLogger().log(Level.FINE, e.getMessage(), e);
				skippedMBeanCounter++;
			} catch (InstanceNotFoundException e) {
				/*
				 * We may end up here if the MBean was unregistered between the call to
				 * getMBeanNames and getMBeanInfo(). Should not be very common though.
				 */
				RJMXPlugin.getDefault().getLogger().log(Level.WARNING, "Skipping " + name.toString() //$NON-NLS-1$
						+ ". It could not be found and may have been unregistered very recently. Set log level to fine to fine for stacktrace!"); //$NON-NLS-1$
				RJMXPlugin.getDefault().getLogger().log(Level.FINE, e.getMessage(), e);
			} catch (IntrospectionException e) {
				IOException exception = new IOException("Error accessing the bean."); //$NON-NLS-1$
				exception.initCause(e);
				throw exception;
			} catch (ReflectionException e) {
				IOException exception = new IOException("Error accessing the bean."); //$NON-NLS-1$
				exception.initCause(e);
				throw exception;
			}
		}
		if (skippedMBeanCounter > 0) {
			RJMXPlugin.getDefault().getLogger().log(Level.WARNING,
					"Skipped " + skippedMBeanCounter + " MBeans because of marshalling related issues."); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return infos;
	}

	/**
	 * Tries to add a dedicated notification listener that removes unloaded MBeans.
	 */
	private void tryToAddMBeanNotificationListener() {
		try {
			ensureConnected().addNotificationListener(MBeanServerDelegate.DELEGATE_NAME, m_registrationListener, null,
					null);
		} catch (InstanceNotFoundException e) {
			// Will typically not happen.
		} catch (IOException e) {
			// Will typically not happen.
		}
	}

	/**
	 * Tries to populate the MBean information cache if it is empty.
	 *
	 * @throws IOException
	 *             if the connection failed or some other IO related problem occurred.
	 */
	private void initializeMBeanInfos() throws IOException {
		synchronized (m_cachedInfos) {
			if (!m_hasInitializedAllMBeans) {
				try {
					getMBeanInfos(null, null);
					m_hasInitializedAllMBeans = true;
				} catch (MalformedObjectNameException e) {
					assert (false); // Should not be able to get here!
				}
			}
		}
	}

	@Override
	public HashMap<ObjectName, MBeanInfo> getMBeanInfos() throws IOException {
		synchronized (m_cachedInfos) {
			initializeMBeanInfos();
			return new HashMap<>(m_cachedInfos);
		}
	}

	@Override
	public MBeanInfo getMBeanInfo(ObjectName mbean)
			throws InstanceNotFoundException, IntrospectionException, ReflectionException, IOException {
		synchronized (m_cachedInfos) {
			MBeanInfo mbeanInfo = m_cachedInfos.get(mbean);
			if (mbeanInfo == null) {
				MBeanServerConnection server = ensureConnected();
				mbeanInfo = server.getMBeanInfo(mbean);
				if (mbeanInfo != null) {
					m_cachedInfos.put(mbean, mbeanInfo);
				}
			}
			return mbeanInfo;
		}
	}

	@Override
	public Object getAttributeValue(MRI attribute) throws AttributeNotFoundException, MBeanException, IOException,
			InstanceNotFoundException, ReflectionException {
		try {
			MBeanServerConnection server = ensureConnected();
			return AttributeValueToolkit.getAttribute(server, attribute);
		} catch (JMRuntimeException e) {
			throw new MBeanException(e, e.getMessage());
		}
	}

	public boolean connect() throws ConnectionException {
		JVMDescriptor jvmInfo = getServerDescriptor().getJvmInfo();
		if (jvmInfo != null && jvmInfo.getJavaVersion() != null
				&& !new JavaVersion(jvmInfo.getJavaVersion()).isGreaterOrEqualThan(JavaVersionSupport.JDK_6)) {
			throw new ConnectionException("Too low JDK Version. JDK 1.6 or higher is supported."); //$NON-NLS-1$
		}
		synchronized (connectionStateLock) {
			if (isConnected()) {
				return false;
			}
			JMXServiceURL url;
			try {
				url = m_connectionDescriptor.createJMXServiceURL();
			} catch (IOException e1) {
				throw new WrappedConnectionException(m_serverDescriptor.getDisplayName(), null, e1);
			}

			try {
				// Use same convention as Sun. localhost:0 means "VM, monitor thyself!"
				String hostName = ConnectionToolkit.getHostName(url);
				if (hostName != null && (hostName.equals("localhost")) //$NON-NLS-1$
						&& ConnectionToolkit.getPort(url) == 0) {
					m_server = new MCMBeanServerConnection(ManagementFactory.getPlatformMBeanServer());
				} else {
					establishConnection(url, m_connectionDescriptor.getEnvironment());
				}
				tryToAddMBeanNotificationListener();
				m_remoteStartTime = fetchServerStartTime();
				return true;
			} catch (Exception e) {
				m_server = null;
				throw new WrappedConnectionException(m_serverDescriptor.getDisplayName(), url, e);
			}
		}
	}

	private long fetchServerStartTime() throws IOException {
		try {
			return ConnectionToolkit.getRuntimeBean(ensureConnected()).getStartTime();
		} catch (IllegalArgumentException e) {
			RJMXPlugin.getDefault().getLogger().log(Level.WARNING,
					"Could not find the Runtime MBean. You are probably connecting to a custom MBean server. Functionality will be limited.", //$NON-NLS-1$
					e);
			return REMOTE_START_TIME_UNDEFINED;
		}
	}

	/**
	 * Attempts to establish a connection. If the connection fails due to symptoms indicating the
	 * registry using SSL, another attempt to connect will be performed, with the required additions
	 * to the env.
	 */
	private void establishConnection(JMXServiceURL serviceURL, Map<String, Object> env) throws IOException {
		try {
			connectJmxConnector(serviceURL, env);
		} catch (IOException exception) {
			try {
				if (env.get(KEY_SOCKET_FACTORY) instanceof SslRMIClientSocketFactory) {
					env.remove(KEY_SOCKET_FACTORY);
				} else {
					env.put(KEY_SOCKET_FACTORY, new SslRMIClientSocketFactory());
				}
				connectJmxConnector(serviceURL, env);
			} catch (IOException ioe) {
				// So we failed even when changing to secure sockets. Original exception was probably spot on...
				throw exception;
			}
		}
		m_server = new MCMBeanServerConnection(m_jmxc.getMBeanServerConnection());
	}

	private void connectJmxConnector(JMXServiceURL serviceURL, Map<String, Object> env) throws IOException {
		m_jmxc = JMXConnectorFactory.newJMXConnector(serviceURL, env);
		m_jmxc.addConnectionNotificationListener(m_disconnectListener, null, null);
		// This is a hack to provide SSL properties to the RMI SSL server socket factory using system properties
		JMXRMISystemPropertiesProvider.setup();
		// According to javadocs, has to pass env here too (which mSA RMI took literally).
		m_jmxc.connect(env);
	}

	@Override
	public long getApproximateServerTime(long localTime) {
		long startTime = System.currentTimeMillis();
		if ((startTime - m_lastRecalibration) > VALUE_RECALIBRATION_INTERVAL
				&& m_remoteStartTime != REMOTE_START_TIME_UNDEFINED) {
			try {
				/*
				 * FIXME: JMC-4270 - Server time approximation is not reliable. Since JDK-6523160,
				 * getUptime can no longer be used to derive the current server time. Find some
				 * other way to do this.
				 */
				long uptime = ConnectionToolkit.getRuntimeBean(ensureConnected()).getUptime();
				long returnTime = System.currentTimeMillis();
				long localTimeEstimate = (startTime + returnTime) / 2;
				m_serverOffset = m_remoteStartTime + uptime - localTimeEstimate;
				m_lastRecalibration = returnTime;
			} catch (Exception e) {
				RJMXPlugin.getDefault().getLogger().log(Level.SEVERE, "Could not recalibrate server offset", e); //$NON-NLS-1$
			}
		}
		return localTime + m_serverOffset;
	}

	/**
	 * Returns the MBeanServerConnection. Yes, this breaks abstraction a bit, and should only be
	 * used by the MBeanBrowser. Everybody else should be using subscriptions anyway.
	 *
	 * @return the MBeanServerConnection currently in use by this connection. May be null if none is
	 *         currently in use.
	 */
	MBeanServerConnection getMBeanServer() {
		return m_server;
	}

	/**
	 * Ok, so this method may not be very useful, from a strict synchronization perspective, but at
	 * least it is now done in ONE place.
	 *
	 * @return a MBeanServerConnection, if connected (or at least non-null).
	 * @throws IOException
	 *             if not connected.
	 */
	private MBeanServerConnection ensureConnected() throws IOException {
		MBeanServerConnection server = m_server;
		if (server == null) {
			throw new InvoluntaryDisconnectException("Server is disconnected!"); //$NON-NLS-1$
		}
		return server;
	}

	public void clearCache() {
		synchronized (m_cachedInfos) {
			m_cachedInfos.clear();
			m_cachedMBeanNames.clear();
			m_hasInitializedAllMBeans = false;
		}
	}

	@Override
	public String toString() {
		return "RJMX Connection: " + m_serverDescriptor.getDisplayName(); //$NON-NLS-1$
	}

	@Override
	public void removeMBeanServerChangeListener(IMBeanServerChangeListener listener) {
		m_mbeanListeners.remove(listener);
	}

	@Override
	public void addMBeanServerChangeListener(IMBeanServerChangeListener listener) {
		m_mbeanListeners.add(listener);
	}

	@Override
	public Map<MRI, Map<String, Object>> getMBeanMetadata(ObjectName mbean) {
		return m_mbeanDataProvider.getMBeanData(mbean);
	}

	/**
	 * Returns the IOperations available for the specified MBean.
	 *
	 * @param mbean
	 *            the MBean for which to return the information.
	 * @return the operations that can be invoked on this mbean.
	 * @throws Exception
	 *             if the connection failed or some other problem occurred when trying create
	 *             operations.
	 */
	public Collection<IOperation> getOperations(ObjectName mbean) throws Exception {
		MBeanServerConnection srv = ensureConnected();
		return MBeanOperationWrapper.createOperations(mbean, srv.getMBeanInfo(mbean).getOperations(), srv);
	}

	IMRIService getMRIService() {
		return m_mbeanDataProvider;
	}

}
