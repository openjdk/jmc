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
package org.openjdk.jmc.rjmx.services.internal;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.services.IDummyService;
import org.openjdk.jmc.rjmx.services.IServiceFactory;

/**
 * Manager for service factories.
 */
@SuppressWarnings("nls")
public class ServiceFactoryManager {
	private final static String EXTENSION_POINT = "org.openjdk.jmc.rjmx.service";
	private final static String EXTENSION_ELEMENT_SERVICE = "service";
	private final static String EXTENSION_ATTRIBUTE_FACTORY = "factory";
	private final static String EXTENSION_ATTRIBUTE_NAME = "name";
	private final static String EXTENSION_ATTRIBUTE_DESCRIPTION = "description";

	private final static Map<Class<?>, Collection<? extends ServiceEntry<?>>> factoryMap = new HashMap<>();

	private static class UnsupportedInvocationHandler implements InvocationHandler {
		private final Throwable cause;

		public UnsupportedInvocationHandler(Throwable cause) {
			this.cause = cause;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			String msg;
			if ((cause != null) && (cause.getMessage() != null)) {
				msg = cause.getMessage();
			} else {
				msg = "Does not support method " + method.getName();
			}
			throw new UnsupportedOperationException(msg, cause);
		}
	}

	public ServiceFactoryManager() {
		initializeFromExtensions();
	}

	private Logger getLogger() {
		return RJMXPlugin.getDefault().getLogger();
	}

	@SuppressWarnings("unchecked")
	public static <T> T createDummyService(Class<T> serviceInterface, Throwable cause) {
		Class<?>[] interfaces = new Class[] {serviceInterface, IDummyService.class};
		ClassLoader cl = serviceInterface.getClassLoader();
		return (T) Proxy.newProxyInstance(cl == null ? IDummyService.class.getClassLoader() : cl, interfaces,
				new UnsupportedInvocationHandler(cause));
	}

	/**
	 * Create a service instance of {@code serviceClass}. Normally only proper services or null will
	 * be returned, but there are two exceptional cases in which dummy services will be returned:
	 * <ul>
	 * <li>If no proper service could be created, but some service factory provided a dummy service,
	 * this will be returned.</li>
	 * <li>If neither a proper nor a dummy service was provided, but some service factory threw an
	 * exception (and {@code serviceClass} denotes an interface), a dummy service will be
	 * constructed. This dummy service will use the thrown exception as the cause of the
	 * {@link UnsupportedOperationException}s that its methods will throw.
	 * </ul>
	 *
	 * @param serviceClass
	 * @param handle
	 * @return A service instance of {@code serviceClass}, or a dummy service.
	 */
	public <T> T createService(Class<T> serviceClass, IConnectionHandle handle) {
		T firstDummyService = null;
		Exception firstException = null;

		for (ServiceEntry<T> entry : getFactoriesFor(serviceClass)) {
			try {
				T service = entry.getServiceFactory().getServiceInstance(handle);
				if (service != null) {
					if (!isDummy(service)) {
						return service;
					}
					if (firstDummyService == null) {
						firstDummyService = service;
					}
				}
			} catch (Exception e) {
				if (firstException == null) {
					firstException = e;
				}
				getLogger().log(Level.FINE, "Could not create service!", e);
			}
		}

		if ((firstDummyService == null) && (firstException != null) && serviceClass.isInterface()) {
			try {
				firstDummyService = createDummyService(serviceClass, firstException);
			} catch (Exception e) {
				// Just log and ignore.
				getLogger().log(Level.FINE, "Could not create dummy service to wrap exception!", e);
			}
		}

		return firstDummyService;
	}

	public static boolean isDummy(Object service) {
		return service instanceof IDummyService;
	}

	@SuppressWarnings("unchecked")
	private <T> Collection<ServiceEntry<T>> getFactoriesFor(Class<T> clazz) {
		Collection<ServiceEntry<T>> factories = (Collection<ServiceEntry<T>>) factoryMap.get(clazz);
		if (factories == null) {
			factories = Collections.emptyList();
		}
		return factories;
	}

	// FIXME: Suggested improvement to service factories. Might not give enough benefits to be worth the effort.
//	public <T> boolean hasService(Class<T> serviceClass, IConnectionHandle handle) {
//		for (ServiceEntry<T> entry : getFactoriesFor(serviceClass)) {
//			if (entry.getServiceFactory().canCreateService(serviceClass, handle)) {
//				return true;
//			}
//		}
//		return false;
//	}

	private void initializeFromExtensions() {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		for (IConfigurationElement config : registry.getConfigurationElementsFor(EXTENSION_POINT)) {
			if (config.getName().equals(EXTENSION_ELEMENT_SERVICE)) {
				try {
					IServiceFactory<?> factory = (IServiceFactory<?>) config
							.createExecutableExtension(EXTENSION_ATTRIBUTE_FACTORY);
					registerService(createServiceEntry(factory, config));
				} catch (CoreException e) {
					getLogger().log(Level.SEVERE, "Could not instantiate service factory!", e);
				}
			}
		}
	}

	private <T> void registerService(ServiceEntry<T> entry) {
		Class<T> serviceClass = entry.getServiceClass();
		Collection<ServiceEntry<T>> factories;
		// HINT: A little convoluted to avoid suppressing "unchecked" warnings here.
		if (factoryMap.containsKey(serviceClass)) {
			factories = getFactoriesFor(entry.getServiceClass());
		} else {
			factories = new ArrayList<>();
			factoryMap.put(entry.getServiceClass(), factories);
		}
		factories.add(entry);
	}

	private <T> ServiceEntry<T> createServiceEntry(IServiceFactory<T> factory, IConfigurationElement config) {
		String name = config.getAttribute(EXTENSION_ATTRIBUTE_NAME);
		String description = config.getAttribute(EXTENSION_ATTRIBUTE_DESCRIPTION);
		return new ServiceEntry<>(factory, name, description);
	}
}
