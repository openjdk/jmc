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

import java.util.logging.Logger;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.Preferences;

import org.openjdk.jmc.rjmx.internal.RJMXSingleton;
import org.openjdk.jmc.rjmx.triggers.internal.NotificationRegistry;

/**
 * There is one instance of the RJMX plugin available from {@link RJMXPlugin#getDefault()}. The
 * plugin provides:
 * <ul>
 * <li>access to the connection manager</li>
 * <li>access to the description repository</li>
 * <li>access to the global services</li>
 * </ul>
 * Clients may not instantiate or subclass this class.
 */
public final class RJMXPlugin extends Plugin {

	/**
	 * The plugin identifier.
	 */
	public static final String PLUGIN_ID = "org.openjdk.jmc.rjmx"; //$NON-NLS-1$

	/**
	 * The identifier for the server configuration.
	 */
	public static final String SERVER_CONFIG_ID = "serverConfig"; //$NON-NLS-1$

	// The logger.
	private final static Logger LOGGER = Logger.getLogger(PLUGIN_ID);

	// The shared instance
	private static RJMXPlugin plugin;

	private IEclipsePreferences rjmxPreferences;

	/**
	 * The default constructor.
	 */
	public RJMXPlugin() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// Avoid getPreferenceStore() so as not to create a store just for
		// saving it.
		synchronized (this) {
			if (rjmxPreferences != null) {
				RJMXSingleton.getDefault().storeAllSettings();
				rjmxPreferences.flush();
			}
		}
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance.
	 *
	 * @return the shared instance
	 */
	public static RJMXPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns the plugin preferences for this plugin.
	 * <p>
	 * This {@link IEclipsePreferences} is used to hold persistent settings for this plugin in the
	 * context of a workbench. Some of these settings will be user controlled, whereas others may be
	 * internal setting that are never exposed to the user.
	 * <p>
	 * If an error occurs reading these settings, an empty settings container is quietly created,
	 * initialized with defaults, and returned.
	 *
	 * @return the preference store
	 */
	public synchronized IEclipsePreferences getRJMXPreferences() {
		// Create the preference store lazily.
		if (rjmxPreferences == null) {
			rjmxPreferences = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
		}
		return rjmxPreferences;
	}

	/**
	 * Looks up the server preferences for given server.
	 *
	 * @param serverUid
	 *            the identifier of the server
	 * @return the preferences available for given server.
	 */
	public synchronized Preferences getServerPreferences(String serverUid) {
		return getRJMXPreferences().node(SERVER_CONFIG_ID).node(serverUid);
	}

	/**
	 * Returns the logger for RJMX.
	 *
	 * @return the {@link Logger}
	 */
	public Logger getLogger() {
		return LOGGER;
	}

	/**
	 * Returns a global RJMX service. Currently there is no way to register new global services.
	 *
	 * @param <T>
	 *            the service type to look up
	 * @param clazz
	 *            the {@link Class} of the service
	 * @return the service object registered for the given class.
	 */
	public <T> T getService(Class<T> clazz) {
		return RJMXSingleton.getDefault().getService(clazz);
	}

	public NotificationRegistry getNotificationRegistry() {
		return RJMXSingleton.getDefault().getNotificationRegistry();
	}
}
