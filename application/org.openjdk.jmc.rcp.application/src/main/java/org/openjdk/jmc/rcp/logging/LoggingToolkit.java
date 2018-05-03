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
package org.openjdk.jmc.rcp.logging;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.eclipse.core.runtime.Platform;

import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.ui.common.util.Environment;

/**
 * Class for handling the java.util.logging subsystem.
 * <p>
 * JUL have the following problems:
 * <ol>
 * <li>All extensions are loaded using the system class loader - this makes adding our own handlers
 * hard.
 * <li>FileHandler does not create the necessary folders by default.
 * </ol>
 */
public final class LoggingToolkit {
	private final static String KEY_FILE_HANDLER_PATTERN = "java.util.logging.FileHandler.pattern"; //$NON-NLS-1$
	private final static Logger LOGGER = Logger.getLogger("org.openjdk.jmc.rcp.application"); //$NON-NLS-1$

	private LoggingToolkit() {
		throw new AssertionError("Toolkit!"); //$NON-NLS-1$
	}

	/**
	 * Accessor method for the logger for this component.
	 *
	 * @return the logger for this component.
	 */
	public static Logger getLogger() {
		return LOGGER;
	}

	public static void initializeLogging() {
		// Check if we're overriding using system properties.
		String file = System.getProperty("java.util.logging.config.file"); //$NON-NLS-1$

		// Using debug will override everything with our standardized debug
		// settings.
		if (Environment.isDebug()) {
			try {
				readConfiguration(
						LoggingToolkit.class.getClassLoader().getResourceAsStream("logging_debug.properties")); //$NON-NLS-1$
				getLogger().log(Level.INFO,
						"Debug settings enabled - loaded debug settings for the logger from logging_debug.properties."); //$NON-NLS-1$
			} catch (Exception e) {
				getLogger().log(Level.WARNING, "Could not initialize debug logger", e); //$NON-NLS-1$
				System.err.println("WARNING: Could not initialize debug logger"); //$NON-NLS-1$
				e.printStackTrace();
			}
		} else if (file == null || file.trim().equals("")) //$NON-NLS-1$
		{
			try {
				readConfiguration(LoggingToolkit.class.getClassLoader().getResourceAsStream("logging.properties")); //$NON-NLS-1$
			} catch (Exception e) {
				getLogger().log(Level.WARNING, "Could not initialize default logger", e); //$NON-NLS-1$
				System.err.println("WARNING: Could not initialize default logger"); //$NON-NLS-1$
				e.printStackTrace();
			}
		} else {
			try {
				if (new File(file).exists()) {
					readConfiguration(new FileInputStream(file));
					getLogger().log(Level.INFO, "Loaded user specified logging settings from " + file + "."); //$NON-NLS-1$ //$NON-NLS-2$
				} else {
					getLogger().log(Level.WARNING, "Could not find user specified logging settings at " + file + "."); //$NON-NLS-1$ //$NON-NLS-2$
				}
			} catch (Exception e) {
				getLogger().log(Level.WARNING, "Could not initialize user logger", e); //$NON-NLS-1$
				System.err.println("WARNING: Could not initialize user logger"); //$NON-NLS-1$
				e.printStackTrace();
			}
		}
		getLogger().log(Level.FINE, "Logger initialized"); //$NON-NLS-1$
		System.out.flush();
	}

	/**
	 * Closes the resourceAsStream input stream.
	 *
	 * @param resourceAsStream
	 * @throws SecurityException
	 * @throws IOException
	 */
	private static void readConfiguration(InputStream resourceAsStream) throws SecurityException, IOException {
		// Ahhh, got to love java util logging. Individual properties cannot be
		// set, so first read everything up into a properties collection - then
		// modify the necessary properties before passing them on.
		Properties props = new Properties();
		InputStream is = null;
		try {
			props.load(resourceAsStream);
			resolveProperties(props);
			createFolders(props);
			is = getAsInputStream(props);
			LogManager.getLogManager().readConfiguration(is);
		} finally {
			IOToolkit.closeSilently(resourceAsStream);
			IOToolkit.closeSilently(is);
		}
	}

	private static InputStream getAsInputStream(Properties props) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			props.store(baos, ""); //$NON-NLS-1$
			String newProps = baos.toString();
			return new ByteArrayInputStream(newProps.getBytes("UTF-8")); //$NON-NLS-1$
		} finally {
			IOToolkit.closeSilently(baos);
		}
	}

	private static void createFolders(Properties props) {
		String loggingFolder = props.getProperty(KEY_FILE_HANDLER_PATTERN);
		if (loggingFolder == null) {
			return;
		}
		String parentStr = new File(loggingFolder).getParent();
		if (parentStr.contains("%")) { //$NON-NLS-1$
			// Bailing out - still contains java.util.logging specific stuff
			// Let's simply hope the user has created the folder to begin with.
			return;
		}
		// Attempting to create the folder. If it succeeds, fine, if not user
		// will get error when attempting to log to file.
		new File(parentStr).mkdirs();
	}

	/**
	 * This method will resolve the properties we are interested in.
	 *
	 * @param props
	 */
	private static void resolveProperties(Properties props) {
		// Currently just this one.
		props.setProperty(KEY_FILE_HANDLER_PATTERN, resolvePath(props.getProperty(KEY_FILE_HANDLER_PATTERN)));
	}

	/**
	 * We need the full path to be able to automatically create the logging directory. We skip %u
	 * and other java.util.logging internal stuff.
	 */
	private static String resolvePath(String t) {
		// We only substitute %t, %h and %w.
		String newStr = t;
		if (newStr.contains("%t")) { //$NON-NLS-1$
			String tmpDir = System.getProperty("java.io.tmpdir"); //$NON-NLS-1$
			if (tmpDir == null) {
				tmpDir = System.getProperty("user.home"); //$NON-NLS-1$
			}
			newStr = newStr.replace("%t", System.getProperty("java.io.tmpdir")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (newStr.contains("%h")) { //$NON-NLS-1$
			newStr = newStr.replace("%h", System.getProperty("user.home")); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (newStr.contains("%w")) { //$NON-NLS-1$
			String location = Platform.getInstanceLocation().getURL().getFile();
			// The URL does not seem to be properly encoded so we don't have to decode it.
			// Normalize path to strip superfluous slashes
			location = new File(location).getPath();
			newStr = newStr.replace("%w", location); //$NON-NLS-1$
		}
		return newStr;
	}
}
