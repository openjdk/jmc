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
package org.openjdk.jmc.console.persistence;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;

import org.eclipse.jface.resource.ImageRegistry;
import org.osgi.framework.BundleContext;

import org.openjdk.jmc.ui.MCAbstractUIPlugin;
import org.openjdk.jmc.ui.UIPlugin;

/**
 * The activator class controls the plug-in life cycle
 */
public class PersistencePlugin extends MCAbstractUIPlugin {
	public final static String ICON_PERSISTENCE = "persistence-16.gif"; //$NON-NLS-1$
	public final static String ICON_SCREENSHOT_ENABLE_PERSISTENCE = "enable_persistence.PNG"; //$NON-NLS-1$
	public final static String PLUGIN_ID = "org.openjdk.jmc.console.persistence"; //$NON-NLS-1$

	// The shared instance
	private static PersistencePlugin plugin;

	/**
	 * The constructor
	 */
	public PersistencePlugin() {
		super(PLUGIN_ID);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Initialize images
	 */
	@Override
	protected void initializeImageRegistry(ImageRegistry registry) {
		registerImage(registry, ICON_PERSISTENCE, ICON_PERSISTENCE);
		try {
			URL imageUrl = new URL(Images.PersistencePlugin_ENABLE_PERSISTENCE_IMAGE_URL);
			registerImage(registry, ICON_SCREENSHOT_ENABLE_PERSISTENCE, imageUrl);
		} catch (MalformedURLException e) {
			UIPlugin.getDefault().getLogger().log(Level.SEVERE,
					"Malformed URL for image: " + Images.PersistencePlugin_ENABLE_PERSISTENCE_IMAGE_URL); //$NON-NLS-1$
			registerImage(registry, ICON_SCREENSHOT_ENABLE_PERSISTENCE, ICON_SCREENSHOT_ENABLE_PERSISTENCE);
		}
	}

	/**
	 * Returns the shared instance.
	 *
	 * @return the shared instance
	 */
	public static PersistencePlugin getDefault() {
		return plugin;
	}
}
