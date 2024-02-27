/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2023, Red Hat Inc. All rights reserved.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorProvider;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class ProtocolInitializer {
	private final static String EXTENSION_POINT = "org.openjdk.jmc.rjmx.jmxProtocols";//$NON-NLS-1$
	private final static String EXTENSION_ELEMENT_CLIENT = "client";//$NON-NLS-1$
	private final static String EXTENSION_ATTRIBUTE_CLASS = "class";//$NON-NLS-1$
	private final static String EXTENSION_ATTRIBUTE_PROTOCOL = "protocol";//$NON-NLS-1$
	private final static String ATTRIBUTE_KEY = "name"; //$NON-NLS-1$
	private final static String TAG_SYSPROPERTY = "sysproperty"; //$NON-NLS-1$
	private final static String ATTRIBUTE_INCLUDE = "include"; //$NON-NLS-1$
	private final static String ATTRIBUTE_SEPARATOR = "separator"; //$NON-NLS-1$

	public JMXConnector newJMXConnector(JMXServiceURL serviceURL, Map<String, ?> environment) throws IOException {
		@SuppressWarnings("unchecked")
		JMXConnectorProvider realProvider = extendEnv(serviceURL, (Map<String, Object>) environment);
		if (realProvider == null) {
			return null;
		}
		return realProvider.newJMXConnector(serviceURL, environment);
	}

	public JMXConnectorProvider extendEnv(JMXServiceURL serviceURL, Map<String, Object> environment) {
		String filter = '(' + EXTENSION_ATTRIBUTE_PROTOCOL + '=' + serviceURL.getProtocol() + ')';
		try {
			final BundleContext context = FrameworkUtil.getBundle(getClass()).getBundleContext();
			Collection<ServiceReference<JMXConnectorProvider>> refs;
			refs = context.getServiceReferences(JMXConnectorProvider.class, filter);

			for (ServiceReference<JMXConnectorProvider> ref : refs) {
				JMXConnectorProvider provider = context.getService(ref);
				if (provider != null) {
					for (String key : ref.getPropertyKeys()) {
						Object value = ref.getProperty(key);

						String property = getIfPrefixed("environment:", key); //$NON-NLS-1$
						if (property != null) {
							environment.put(property, value);
						}

						property = getIfPrefixed("system:", key); //$NON-NLS-1$
						if (property != null) {
							ensureSystemProperty(property, value.toString(), "|"); //$NON-NLS-1$
						}
					}

					ClassLoader loader = provider.getClass().getClassLoader();
					environment.put(JMXConnectorFactory.DEFAULT_CLASS_LOADER, loader);
					// Used by MX4J.
					environment.put(JMXConnectorServerFactory.PROTOCOL_PROVIDER_CLASS_LOADER, loader);

					// FIXME: Add WeakRef/ReferenceQueue or listener to "unget" service?
					// return provider.newJMXConnector(serviceURL, environment);
					return provider;
				}
			}
		} catch (InvalidSyntaxException e) {
			// Assume this is a bad protocol name. At least we don't support it.
			Logger.getLogger("org.openjdk.jmc.rjmx.ext").log(Level.WARNING, "Bad protocol syntax", e); //$NON-NLS-1$ //$NON-NLS-2$
		}

		return null;
	}

	private static String getIfPrefixed(String prefix, String candidate) {
		if (candidate.startsWith(prefix)) {
			return candidate.substring(prefix.length());
		}
		return null;
	}

	private static void ensureSystemProperty(String key, String include, String separator) {
		String org = System.getProperty(key);
		if (org == null) {
			System.setProperty(key, include);
			return;
		}
		if ((separator + org + separator).indexOf(separator + include + separator) < 0) {
			System.setProperty(key, org + separator + include);
		}
	}

	public static List<JMXConnectorProvider> initializeFromExtensions() {
		List<JMXConnectorProvider> providers = new ArrayList<>();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		for (IConfigurationElement config : registry.getConfigurationElementsFor(EXTENSION_POINT)) {
			if (config.getName().equals(EXTENSION_ELEMENT_CLIENT)) {
				try {
					JMXConnectorProvider provider = (JMXConnectorProvider) config
							.createExecutableExtension(EXTENSION_ATTRIBUTE_CLASS);
					providers.add(provider);
					for (IConfigurationElement prop : config.getChildren(TAG_SYSPROPERTY)) {
						ensureSystemProperty(prop.getAttribute(ATTRIBUTE_KEY), prop.getAttribute(ATTRIBUTE_INCLUDE),
								prop.getAttribute(ATTRIBUTE_SEPARATOR));
					}
				} catch (CoreException e) {
					RJMXPlugin.getDefault().getLogger().log(Level.SEVERE,
							"Could not instantiate JMX protocol provider!", e);
				}
			}
		}
		return providers;
	}

}
