/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, Kantega AS. All rights reserved. 
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
package org.openjdk.jmc.kubernetes;

import java.util.logging.Logger;

import org.openjdk.jmc.kubernetes.preferences.KubernetesScanningParameters;
import org.openjdk.jmc.kubernetes.preferences.PreferenceConstants;
import org.openjdk.jmc.ui.MCAbstractUIPlugin;

public class JmcKubernetesPlugin extends MCAbstractUIPlugin
		implements KubernetesScanningParameters, PreferenceConstants {

	public final static String PLUGIN_ID = "org.openjdk.jmc.kubernetes"; //$NON-NLS-1$

	// The logger.
	private final static Logger LOGGER = Logger.getLogger(PLUGIN_ID);

	// The shared instance.
	private static JmcKubernetesPlugin plugin;

	/**
	 * The constructor.
	 */
	public JmcKubernetesPlugin() {
		super(PLUGIN_ID);
		plugin = this;
	}

	/**
	 * @return the shared instance.
	 */
	public static JmcKubernetesPlugin getDefault() {
		return plugin;
	}

	public static Logger getPluginLogger() {
		return LOGGER;
	}

	@Override
	public boolean scanForInstances() {
		return getPreferenceStore().getBoolean(P_SCAN_FOR_INSTANCES);
	}

	@Override
	public boolean scanAllContexts() {
		return getPreferenceStore().getBoolean(P_SCAN_ALL_CONTEXTS);
	}

	@Override
	public String jolokiaPort() {
		return getPreferenceStore().getString(P_JOLOKIA_PORT);
	}

	@Override
	public String username() {
		return getPreferenceStore().getString(P_USERNAME);
	}

	@Override
	public String password() {
		return getPreferenceStore().getString(P_PASSWORD);
	}

	@Override
	public String jolokiaPath() {
		return getPreferenceStore().getString(P_JOLOKIA_PATH);
	}

	@Override
	public String requireLabel() {
		return getPreferenceStore().getString(P_REQUIRE_LABEL);
	}

	@Override
	public String jolokiaProtocol() {
		return getPreferenceStore().getString(P_JOLOKIA_PROTOCOL);
	}

}
