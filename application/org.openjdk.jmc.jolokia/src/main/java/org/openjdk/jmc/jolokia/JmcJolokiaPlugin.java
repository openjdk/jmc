/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2024, Kantega AS. All rights reserved.
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
package org.openjdk.jmc.jolokia;

import java.util.Arrays;
import java.util.TreeSet;

import org.jolokia.server.core.config.ConfigKey;
import org.jolokia.server.core.config.StaticConfiguration;
import org.jolokia.server.core.detector.ServerDetector;
import org.jolokia.server.core.restrictor.AllowAllRestrictor;
import org.jolokia.server.core.service.JolokiaServiceManagerFactory;
import org.jolokia.server.core.service.api.JolokiaContext;
import org.jolokia.server.core.service.api.JolokiaServiceManager;
import org.jolokia.server.core.service.impl.JulLogHandler;
import org.openjdk.jmc.jolokia.preferences.PreferenceConstants;
import org.openjdk.jmc.ui.MCAbstractUIPlugin;

public class JmcJolokiaPlugin extends MCAbstractUIPlugin implements JolokiaDiscoverySettings, PreferenceConstants {

	public final static String PLUGIN_ID = "org.openjdk.jmc.jolokia"; //$NON-NLS-1$
	private static JmcJolokiaPlugin plugin;

	public JmcJolokiaPlugin() {
		super(PLUGIN_ID);
		plugin = this;
	}

	public static JmcJolokiaPlugin getDefault() {
		return plugin;
	}

	@Override
	public boolean shouldRunDiscovery() {
		return getPreferenceStore().getBoolean(P_SCAN);
	}

	/**
	 * @return a very basic Jolokia context to satisfy discovery. We are not interested in the
	 *         server side aspects here.
	 */
	@Override
	public JolokiaContext getJolokiaContext() {
		StaticConfiguration configuration = new StaticConfiguration(ConfigKey.AGENT_ID, "jmc");//$NON-NLS-1$
		JolokiaServiceManager serviceManager = JolokiaServiceManagerFactory.createJolokiaServiceManager(configuration,
				new JulLogHandler(PLUGIN_ID), new AllowAllRestrictor(),
				() -> new TreeSet<ServerDetector>(Arrays.asList(ServerDetector.FALLBACK)));
		return serviceManager.start();
	}

	@Override
	public String getMulticastGroup() {
		return this.getPreferenceStore().getString(P_MULTICAST_GROUP);
	}

	@Override
	public int getMulticastPort() {
		return this.getPreferenceStore().getInt(P_MULTICAST_PORT);
	}

	@Override
	public int getDiscoveryTimeout() {
		return this.getPreferenceStore().getInt(P_DISCOVER_TIMEOUT);
	}
}
