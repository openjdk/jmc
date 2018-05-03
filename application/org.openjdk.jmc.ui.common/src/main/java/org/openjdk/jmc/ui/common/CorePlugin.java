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
package org.openjdk.jmc.ui.common;

import java.io.File;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import org.openjdk.jmc.ui.common.idesupport.IDESupportFactory;
import org.openjdk.jmc.ui.common.idesupport.IIDESupport;

/**
 * The Core Plug-in class for Mission Control
 */
public class CorePlugin implements BundleActivator {

	public static final String PLUGIN_ID = "org.openjdk.jmc.ui.common"; //$NON-NLS-1$

	private static CorePlugin plugin;
	private IEclipsePreferences preferences;
	private final Logger m_logger;

	public CorePlugin() {
		plugin = this;
		m_logger = Logger.getLogger(PLUGIN_ID);
	}

	public static CorePlugin getDefault() {
		return plugin;
	}

	public Logger getLogger() {
		return m_logger;
	}

	/**
	 * @return The Eclipse workspace directory. If that is not a usable directory, then returns the
	 *         user's home directory.
	 */
	public File getWorkspaceDirectory() {
		IPath workspaceDir = Platform.getLocation();
		if (workspaceDir != null && workspaceDir.toFile().isDirectory()) {
			return workspaceDir.toFile();
		}
		return new File(System.getProperty("user.home", ".")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public IIDESupport getIDESupport() {
		return IDESupportFactory.getIDESupport();
	}

	public IEclipsePreferences getPreferences() {
		// Create the preference store lazily.
		if (preferences == null) {
			preferences = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
		}
		return preferences;
	}

	@Override
	public void start(BundleContext context) throws Exception {
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}
}
