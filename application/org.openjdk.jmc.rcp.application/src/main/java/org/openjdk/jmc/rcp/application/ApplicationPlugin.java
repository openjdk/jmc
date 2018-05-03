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
package org.openjdk.jmc.rcp.application;

import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.rcp.application.p2.AddRepositoriesJob;
import org.openjdk.jmc.rcp.logging.LoggingToolkit;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class ApplicationPlugin extends AbstractUIPlugin {
	// The plug-in ID
	public static final String PLUGIN_ID = "org.openjdk.jmc.rcp.application"; //$NON-NLS-1$

	// Version
	private final static String VERSION_PROPERTIES = "/about.mappings"; //$NON-NLS-1$
	private final static String UNKNOWN_VERSION = "unknown"; //$NON-NLS-1$
	// The shared instance
	private static ApplicationPlugin plugin;

	public final static String VERSION;

	public final static String FULL_VERSION;

	public static final String ICON_UPDATE_SEARCH = "/icons/usearch_obj.gif"; //$NON-NLS-1$
	public static final String ICON_ERROR_MARKER = "/icons/error_marker.gif"; //$NON-NLS-1$
	public static final String ICON_INSTRUCTION_POINTER = "/icons/inst_ptr_top.gif"; //$NON-NLS-1$

	static {
		String version = UNKNOWN_VERSION;
		String fullVersion = UNKNOWN_VERSION;
		Properties props = new Properties();
		InputStream is = null;
		try {
			is = ApplicationPlugin.class.getResourceAsStream(VERSION_PROPERTIES);
			props.load(is);
			version = props.getProperty("0"); //$NON-NLS-1$
			fullVersion = props.getProperty("4"); //$NON-NLS-1$
		} catch (Exception e) {
			// Skip
		} finally {
			IOToolkit.closeSilently(is);
		}

		VERSION = version;
		FULL_VERSION = fullVersion;
	}

	/**
	 * The constructor
	 */
	public ApplicationPlugin() {
		plugin = this;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		initializeDebug();
		LoggingToolkit.initializeLogging();
		// We are adding the repositories in a separate job, as it may block other tasks from happening correctly.
		new AddRepositoriesJob(context).schedule();
	}

	private void initializeDebug() {
		// Eclipse docs: "osgi.debug {-debug} -- if set to a non-null value, the platform is put in debug mode."
		if (System.getProperty("osgi.debug") != null) { //$NON-NLS-1$
			System.setProperty("org.openjdk.jmc.debug", "true"); //$NON-NLS-1$ //$NON-NLS-2$
			getLogger().log(Level.INFO, "JMC debug mode enabled"); //$NON-NLS-1$
		}
	}

	public static Logger getLogger() {
		return LoggingToolkit.getLogger();
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		reg.put(ICON_ERROR_MARKER, getImageDescriptor(ICON_ERROR_MARKER));
		reg.put(ICON_INSTRUCTION_POINTER, getImageDescriptor(ICON_INSTRUCTION_POINTER));
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static ApplicationPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in relative path
	 *
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
