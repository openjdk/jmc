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
package org.openjdk.jmc.browser;

import org.eclipse.jface.resource.ImageRegistry;
import org.osgi.framework.BundleContext;

import org.openjdk.jmc.browser.preferences.PreferenceConstants;
import org.openjdk.jmc.ui.MCAbstractUIPlugin;

/**
 * The main plugin class, controlling the life cycle.
 */
public class JVMBrowserPlugin extends MCAbstractUIPlugin {
	public static final String PLUGIN_ID = "org.openjdk.jmc.browser"; //$NON-NLS-1$

	public static final String ICON_NEW_FOLDER = "newfolder.gif"; //$NON-NLS-1$
	public static final String ICON_NEW_CONNECTION = "newconnection.gif"; //$NON-NLS-1$
	public static final String ICON_PROPERTIES = "properties.gif"; //$NON-NLS-1$
	public static final String ICON_BANNER_CONNECTION_WIZARD = "bannerconwiz.gif"; //$NON-NLS-1$
	public static final String ICON_OVERLAY_CONNECTED = "overlay_connected.png"; //$NON-NLS-1$
	public static final String ICON_OVERLAY_DISCONNECTED = "overlay_disconnected.png"; //$NON-NLS-1$
	public static final String ICON_OVERLAY_UNCONNECTABLE = "overlay_unconnectable.png"; //$NON-NLS-1$
	public static final String ICON_TREE_MODE = "tree_mode.png"; //$NON-NLS-1$
	public static final String ICON_PADLOCK = "padlock.png"; //$NON-NLS-1$
	public static final String ICON_CONNECT = "connect.gif"; //$NON-NLS-1$
	public static final String ICON_DISCONNECT = "disconnect.gif"; //$NON-NLS-1$
	public static final String ICON_UNCONNECTABLE = "unconnectable.gif"; //$NON-NLS-1$

	// The shared instance.
	private static JVMBrowserPlugin plugin;

	public JVMBrowserPlugin() {
		super(PLUGIN_ID);
		plugin = this;
	}

	@Override
	protected final void initializeImageRegistry(final ImageRegistry registry) {
		registerImage(registry, ICON_NEW_FOLDER, ICON_NEW_FOLDER);
		registerImage(registry, ICON_NEW_CONNECTION, ICON_NEW_CONNECTION);
		registerImage(registry, ICON_PROPERTIES, ICON_PROPERTIES);
		registerImage(registry, ICON_BANNER_CONNECTION_WIZARD, ICON_BANNER_CONNECTION_WIZARD);
		registerImage(registry, ICON_OVERLAY_CONNECTED, ICON_OVERLAY_CONNECTED);
		registerImage(registry, ICON_OVERLAY_DISCONNECTED, ICON_OVERLAY_DISCONNECTED);
		registerImage(registry, ICON_OVERLAY_UNCONNECTABLE, ICON_OVERLAY_UNCONNECTABLE);
		registerImage(registry, ICON_TREE_MODE, ICON_TREE_MODE);
		registerImage(registry, ICON_PADLOCK, ICON_PADLOCK);
		registerImage(registry, ICON_CONNECT, ICON_CONNECT);
		registerImage(registry, ICON_DISCONNECT, ICON_DISCONNECT);
		registerImage(registry, ICON_UNCONNECTABLE, ICON_UNCONNECTABLE);
	}

	/**
	 * This method is called upon plug-in activation
	 */
	@Override
	public final void start(final BundleContext context) throws Exception {
		super.start(context);
	}

	/**
	 * @return the shared instance.
	 */
	public static JVMBrowserPlugin getDefault() {
		return plugin;
	}

	public boolean getWarnNoLocalJVMs() {
		return getPreferenceStore().getBoolean(PreferenceConstants.P_WARN_NO_LOCAL_JVMs);
	}

	public void setWarnNoLocalJVMs(boolean warn) {
		getPreferenceStore().setValue(PreferenceConstants.P_WARN_NO_LOCAL_JVMs, warn);
	}

}
