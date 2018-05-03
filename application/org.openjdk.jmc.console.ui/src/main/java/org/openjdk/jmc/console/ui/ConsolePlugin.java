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
package org.openjdk.jmc.console.ui;

import org.eclipse.jface.resource.ImageRegistry;
import org.osgi.framework.BundleContext;

import org.openjdk.jmc.ui.MCAbstractUIPlugin;

/**
 * The main Console plugin class.
 */
final public class ConsolePlugin extends MCAbstractUIPlugin {
	public static final String PLUGIN_ID = "org.openjdk.jmc.console.ui"; //$NON-NLS-1$
	public static final String IMAGE_HELP = "help.gif"; //$NON-NLS-1$
	public static final String IMG_ATTRIBUTE_SELECTOR_BANNER = "attribute-browser-wiz.gif"; //$NON-NLS-1$

	public static final String ICON_OVERVIEW = "overview_obj.gif"; //$NON-NLS-1$

	public static final String IMG_TOOLBAR_RUNTIME = "toolbar-runtime-64.png"; //$NON-NLS-1$
	public static final String IMG_TOOLBAR_MISC = "toolbar-misc-64.png"; //$NON-NLS-1$
	public static final String IMG_TOOLBAR_ADVANCED = "toolbar-advanced-64.png"; //$NON-NLS-1$
	public static final String IMG_TOOLBAR_MBEAN = "toolbar-mbean-64.png"; //$NON-NLS-1$
	private static ConsolePlugin plugin;

	/**
	 * Returns the shared instance.
	 */
	public static ConsolePlugin getDefault() {
		return plugin;
	}

	/**
	 * The constructor.
	 */
	public ConsolePlugin() {
		super(PLUGIN_ID);
		plugin = this;
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		plugin = null;
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry registry) {
		registerImage(registry, IMAGE_HELP, IMAGE_HELP);
		registerImage(registry, IMG_ATTRIBUTE_SELECTOR_BANNER, IMG_ATTRIBUTE_SELECTOR_BANNER);
		registerImage(registry, ICON_OVERVIEW, ICON_OVERVIEW);

		registerImage(registry, IMG_TOOLBAR_MBEAN, IMG_TOOLBAR_MBEAN);
		registerImage(registry, IMG_TOOLBAR_RUNTIME, IMG_TOOLBAR_RUNTIME);
		registerImage(registry, IMG_TOOLBAR_MISC, IMG_TOOLBAR_MISC);
		registerImage(registry, IMG_TOOLBAR_ADVANCED, IMG_TOOLBAR_ADVANCED);
	}
}
