/*
 * Copyright (c) 2026 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026 IBM Corporation. All rights reserved.
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
package org.openjdk.jmc.ui.websocket;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.ui.websocket.preferences.PreferenceConstants;
import org.osgi.framework.BundleContext;

public class WebsocketPlugin extends AbstractUIPlugin implements IStartup {

	public final static String PLUGIN_ID = "org.openjdk.jmc.ui.websocket"; //$NON-NLS-1$
	private static final Logger LOGGER = Logger.getLogger(PLUGIN_ID);

	private static WebsocketPlugin plugin;
	private MCWebsocketServer server;

	public WebsocketPlugin() {
	}

	public static WebsocketPlugin getDefault() {
		return plugin;
	}

	public static Logger getLogger() {
		return LOGGER;
	}

	public void start(BundleContext bundleContext) throws Exception {
		super.start(bundleContext);
		this.getPreferenceStore().addPropertyChangeListener(preferenceChangeListener);
		plugin = this;
		startServer(getCryostatPort());
		LOGGER.log(Level.INFO, "JMC Websocket Server is live!");
	}

	public void stop(BundleContext bundleContext) throws Exception {
		if (server != null) {
			server.shutdown();
			server = null;
		}
		plugin = null;
		super.stop(bundleContext);
	}

	public void notifyAll(IItemCollection events) {
		if (server != null) {
			server.notifyAll(events);
		}
	}

	private void startServer(int port) {
		if (getServerEnabled()) {
			server = new MCWebsocketServer(port);
		}
	}

	private void stopServer() {
		if (server != null) {
			try {
				server.shutdown();
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error shutting down the Jetty WebSocket server", e);
			}
		}
	}

	private IPropertyChangeListener preferenceChangeListener = new IPropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent event) {
			if (event.getProperty().equals(PreferenceConstants.P_SERVER_ENABLED)) {
				if (getServerEnabled()) {
					startServer(getCryostatPort());
				} else {
					stopServer();
				}
			}
			if (event.getProperty().equals(PreferenceConstants.P_SERVER_PORT) && getServerEnabled()) {
				stopServer();
				startServer(getCryostatPort());
			}
		}
	};

	private int getCryostatPort() {
		return this.getPreferenceStore().getInt(PreferenceConstants.P_SERVER_PORT);
	}

	public boolean getServerEnabled() {
		return this.getPreferenceStore().getBoolean(PreferenceConstants.P_SERVER_ENABLED);
	}

	@Override
	public void earlyStartup() {
		// do nothing, we're only implementing IStartup to force a load of the plugin
	}
}
