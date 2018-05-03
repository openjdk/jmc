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
package org.openjdk.jmc.console.ui.diagnostic;

import org.eclipse.jface.resource.ImageRegistry;
import org.osgi.framework.BundleContext;

import org.openjdk.jmc.ui.MCAbstractUIPlugin;

/**
 * Activator for the Diagnostic Plugin
 */
public class DiagnosticPlugin extends MCAbstractUIPlugin {
	final public static String ICON_CLEAR_CONSOLE = "clear_co.gif"; //$NON-NLS-1$
	final public static String ICON_DISABLED_APPEND = "console-diagnostic-command-output-16.png"; //$NON-NLS-1$
	final public static String ICON_ENABLED_APPEND = "console-diagnostic-cmd-output-apnd-16.png"; //$NON-NLS-1$

	private static final String PLUGIN_ID = "org.openjdk.jmc.console.ui.diagnostic"; //$NON-NLS-1$
	private static DiagnosticPlugin s_plugin;

	/**
	 * The constructor.
	 */
	public DiagnosticPlugin() {
		super(PLUGIN_ID);
		s_plugin = this;
	}

	/**
	 * Returns the shared instance.
	 */
	public static DiagnosticPlugin getDefault() {
		return s_plugin;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		s_plugin = this;
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		s_plugin = null;
		super.stop(context);
	}

	/**
	 * Initialize image regsitry
	 */
	@Override
	protected void initializeImageRegistry(ImageRegistry registry) {
		registerImage(registry, ICON_CLEAR_CONSOLE, ICON_CLEAR_CONSOLE);
		registerImage(registry, ICON_DISABLED_APPEND, ICON_DISABLED_APPEND);
		registerImage(registry, ICON_ENABLED_APPEND, ICON_ENABLED_APPEND);
	}

}
