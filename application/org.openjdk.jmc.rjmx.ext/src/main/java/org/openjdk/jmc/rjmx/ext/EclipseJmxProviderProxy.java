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
package org.openjdk.jmc.rjmx.ext;

import java.io.IOException;
import java.net.MalformedURLException;
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
import org.eclipse.core.runtime.Platform;

/**
 * JMX protocol extender using an Eclipse extension point.
 */
public class EclipseJmxProviderProxy implements JMXConnectorProvider {
	private final static String EXTENSION_POINT = "org.openjdk.jmc.rjmx.jmxProtocols"; //$NON-NLS-1$
	private final static String TAG_CLIENT = "client"; //$NON-NLS-1$
	private final static String ATTRIBUTE_PROTOCOL = "protocol"; //$NON-NLS-1$
	private final static String ATTRIBUTE_CLASS = "class"; //$NON-NLS-1$
	private final static String TAG_PROPERTY = "property"; //$NON-NLS-1$
	private final static String ATTRIBUTE_KEY = "name"; //$NON-NLS-1$
	private final static String ATTRIBUTE_VALUE = "value"; //$NON-NLS-1$
	private final static String TAG_SYSPROPERTY = "sysproperty"; //$NON-NLS-1$
	private final static String ATTRIBUTE_INCLUDE = "include"; //$NON-NLS-1$
	private final static String ATTRIBUTE_SEPARATOR = "separator"; //$NON-NLS-1$

	public EclipseJmxProviderProxy() {
	}

	@Override
	public JMXConnector newJMXConnector(JMXServiceURL serviceURL, Map<String, ?> environment) throws IOException {
		@SuppressWarnings("unchecked")
		JMXConnectorProvider realProvider = extendEnv(serviceURL, (Map<String, Object>) environment);
		if (realProvider == null) {
			throw new MalformedURLException("No Eclipse extension for JMX protocol " + serviceURL.getProtocol() //$NON-NLS-1$
					+ " was found."); //$NON-NLS-1$
		}
		return realProvider.newJMXConnector(serviceURL, environment);
	}

	private IConfigurationElement[] getConfigElements() {
		return Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_POINT);
	}

	public JMXConnectorProvider extendEnv(JMXServiceURL serviceURL, Map<String, Object> environment) {
		String protocol = serviceURL.getProtocol();

		for (IConfigurationElement element : getConfigElements()) {
			if (TAG_CLIENT.equals(element.getName()) && element.getAttribute(ATTRIBUTE_PROTOCOL).equals(protocol)) {
				// Add to environment
				for (IConfigurationElement prop : element.getChildren(TAG_PROPERTY)) {
					environment.put(prop.getAttribute(ATTRIBUTE_KEY), prop.getAttribute(ATTRIBUTE_VALUE));
				}

				// For the badly written protocols, extend system properties.
				for (IConfigurationElement prop : element.getChildren(TAG_SYSPROPERTY)) {
					ensureSystemProperty(prop.getAttribute(ATTRIBUTE_KEY), prop.getAttribute(ATTRIBUTE_INCLUDE),
							prop.getAttribute(ATTRIBUTE_SEPARATOR));
				}

				try {
					Object provider = element.createExecutableExtension(ATTRIBUTE_CLASS);
					if (provider instanceof JMXConnectorProvider) {
						ClassLoader loader = provider.getClass().getClassLoader();
						environment.put(JMXConnectorFactory.DEFAULT_CLASS_LOADER, loader);
						// Used by MX4J.
						environment.put(JMXConnectorServerFactory.PROTOCOL_PROVIDER_CLASS_LOADER, loader);
						return (JMXConnectorProvider) provider;
					}
				} catch (CoreException e) {
					Logger.getLogger("org.openjdk.jmc.rjmx.ext").log(Level.WARNING, e.getMessage(), e); //$NON-NLS-1$
				}
			}
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
}
