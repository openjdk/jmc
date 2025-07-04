/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.rjmx.common.internal;

import java.io.IOException;
import java.lang.ref.Cleaner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.management.MBeanServerConnection;

import org.openjdk.jmc.rjmx.common.RJMXCorePlugin;
import org.openjdk.jmc.rjmx.common.ConnectionException;
import org.openjdk.jmc.rjmx.common.ConnectionToolkit;
import org.openjdk.jmc.rjmx.common.IConnectionHandle;
import org.openjdk.jmc.rjmx.common.IConnectionListener;
import org.openjdk.jmc.rjmx.common.IServerDescriptor;
import org.openjdk.jmc.rjmx.common.ServiceNotAvailableException;
import org.openjdk.jmc.rjmx.common.services.internal.ServiceEntry;
import org.openjdk.jmc.rjmx.common.subscription.IMBeanHelperService;
import org.openjdk.jmc.rjmx.common.subscription.IMRIService;
import org.openjdk.jmc.rjmx.common.services.internal.ServiceFactoryManager;

/**
 * This class represents a connection to a JVM.
 * <p>
 * This class implements automatic resource cleanup using the {@link Cleaner} API. While the cleaner
 * provides a safety net for resource cleanup, it is strongly recommended to explicitly call
 * {@link #close()} or use try-with-resources for predictable resource management.
 *
 * @see org.openjdk.jmc.rjmx.common.internal.RJMXConnection
 */
public class DefaultConnectionHandle implements IConnectionHandle {

	// The services exposed by this IConnectionHandle (<class,object>)
	private final Map<Class<?>, Object> services = Collections.synchronizedMap(new LinkedHashMap<Class<?>, Object>());

	private final String description;
	private final RJMXConnection connection;
	private final IConnectionListener[] listeners;

	private static final ServiceFactoryManager FACTORY_MANAGER = new ServiceFactoryManager();

	private volatile Long closeDownThreadId; // Set to -1 when handle is closed
	private final Cleaner.Cleanable cleanable;

	public DefaultConnectionHandle(RJMXConnection connection, String description, IConnectionListener[] listeners) {
		this(connection, description, listeners, new ArrayList<>());
	}

	public DefaultConnectionHandle(RJMXConnection connection, String description, IConnectionListener[] listeners,
			List<ServiceEntry<?>> serviceEntries) {
		this.connection = connection;
		this.description = description;
		this.listeners = listeners == null ? new IConnectionListener[0] : listeners;
		FACTORY_MANAGER.initializeFromExtensions(serviceEntries);
		registerDefaultServices();
		this.cleanable = ConnectionToolkit.CLEANER.register(this, new CleanupAction(services, connection));
	}

	@Override
	public IServerDescriptor getServerDescriptor() {
		return connection.getServerDescriptor();
	}

	@Override
	public boolean isConnected() {
		return isOpen() && connection.isConnected();
	}

	private boolean isOpen() {
		// Access allowed if we are not closed, or for the closing thread during shutdown
		return closeDownThreadId == null || Thread.currentThread().getId() == closeDownThreadId;
	}

	@Override
	public void close() throws IOException {
		try {
			synchronized (services) {
				if (closeDownThreadId != null) {
					return;
				}
				// Allow disposing services to get other services, but refuse all other
				closeDownThreadId = Thread.currentThread().getId();
				shutdownServices();
				closeDownThreadId = -1L; // No more access, refuse all
			}
			for (IConnectionListener l : listeners) {
				try {
					l.onConnectionChange(this);
				} catch (Exception e) {
					RJMXCorePlugin.getDefault().getLogger().log(Level.WARNING,
							"DefaultConnectionHandle listener " + l + " failed", e); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		} catch (RuntimeException e) {
			throw new IOException("Failed to close connection", e);
		} finally {
			cleanable.clean();
		}
	}

	private void shutdownServices() {
		Object[] servicesArray = services.values().toArray();
		for (int i = 0; i < servicesArray.length; i++) {
			Object service = servicesArray[servicesArray.length - i - 1];
			if (service instanceof IDisposableService) {
				try {
					((IDisposableService) service).dispose();
				} catch (RuntimeException e) {
					RJMXCorePlugin.getDefault().getLogger().log(Level.WARNING,
							"Could not shut down the " + service.getClass().getName() //$NON-NLS-1$
									+ " service.", //$NON-NLS-1$
							e);
				}
			}
		}
		services.clear();
	}

	@Override
	public String toString() {
		return description + " - " + connection.toString(); //$NON-NLS-1$
	}

	@Override
	public <T> T getServiceOrThrow(Class<T> serviceInterface) throws ConnectionException, ServiceNotAvailableException {
		if (isOpen()) {
			T service = getService(serviceInterface, false);
			if (service != null) {
				return service;
			}
		} else {
			throw new ConnectionException("Connection closed!"); //$NON-NLS-1$
		}
		throw new ServiceNotAvailableException("Service '" + serviceInterface.getName() + "' not available!"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public <T> T getServiceOrDummy(Class<T> serviceInterface) {
		if (!serviceInterface.isInterface()) {
			/*
			 * If you get this, you need to call createServiceOrNull() instead and handle the null
			 * case. (And yes, we want to check this before attempting to create a service. Ideally,
			 * we would like to check this at compile time, but langtools couldn't come up with a
			 * good way to do so when we asked. Although JSR 308 and the Checker Framework should
			 * change that.)
			 */
			throw new IllegalArgumentException("Will not be able to create dummy implementations of " //$NON-NLS-1$
					+ serviceInterface.getName() + " since the service is not an interface"); //$NON-NLS-1$
		} else if (isOpen()) {
			return getService(serviceInterface, true);
		} else {
			return createDummyService(serviceInterface);
		}
	}

	@Override
	public <T> T getServiceOrNull(Class<T> serviceInterface) {
		if (isOpen()) {
			return getService(serviceInterface, false);
		} else {
			return null;
		}
	}

	private <T> T getService(Class<T> serviceInterface, boolean acceptDummy) {
		synchronized (services) {
			@SuppressWarnings("unchecked")
			T service = (T) services.get(serviceInterface);
			if (service == null || (ServiceFactoryManager.isDummy(service) && !acceptDummy)) {
				service = FACTORY_MANAGER.createService(serviceInterface, this);
				if ((service == null) && acceptDummy) {
					service = createDummyService(serviceInterface);
				}
				if (service != null) {
					services.put(serviceInterface, service);
				}
			}
			return (acceptDummy || !ServiceFactoryManager.isDummy(service)) ? service : null;
		}
	}

	private static <T> T createDummyService(Class<T> serviceInterface) throws IllegalArgumentException {
		return ServiceFactoryManager.createDummyService(serviceInterface, null);
	}

	private synchronized void registerDefaultServices() {
		synchronized (services) {
			services.put(MBeanServerConnection.class, connection.getMBeanServer());
			services.put(IMBeanHelperService.class, connection);
			services.put(IMRIService.class, connection.getMRIService());
		}
	}

	@Override
	public boolean hasService(Class<?> serviceClass) {
		synchronized (services) {
			return getServiceOrNull(serviceClass) != null;
		}
	}

	@Override
	public String getDescription() {
		return description;
	}

	private static class CleanupAction implements Runnable {
		private final Map<Class<?>, Object> services;
		private final RJMXConnection connection;

		CleanupAction(Map<Class<?>, Object> services, RJMXConnection connection) {
			this.services = new LinkedHashMap<>(services);
			this.connection = connection;
		}

		@Override
		public void run() {
			try {
				shutdownServicesQuietly(services);
				connection.close();
			} catch (Exception e) {
				// Ignore all exceptions during cleanup
			}
		}

		private static void shutdownServicesQuietly(Map<Class<?>, Object> services) {
			Object[] servicesArray = services.values().toArray();
			for (int i = 0; i < servicesArray.length; i++) {
				Object service = servicesArray[servicesArray.length - i - 1];
				if (service instanceof IDisposableService) {
					try {
						((IDisposableService) service).dispose();
					} catch (Exception e) {
						// Ignore exceptions during cleanup
					}
				}
			}
			services.clear();
		}
	}
}
