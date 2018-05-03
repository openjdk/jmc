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
package org.openjdk.jmc.ui.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openjdk.jmc.common.io.IOToolkit;

/**
 * Provides Mission Control version information.
 */
public class MCVersion {

	private final static Logger LOGGER = Logger.getLogger("org.openjdk.jmc.ui.common"); //$NON-NLS-1$

	private static final String PROPERTY_FULL_VERSION = "jmc.fullversion"; //$NON-NLS-1$
	private static final String DEFAULT_FULL_VERSION = "debug"; //$NON-NLS-1$
	private final static String FULL_VERSION;

	private static final String PROPERTY_QUALIFIER = "jmc.qualifier"; //$NON-NLS-1$
	private static final String DEFAULT_QUALIFIER = "0"; //$NON-NLS-1$
	private final static String QUALIFIER;

	private static final String PROPERTY_BUILD_ID = "jmc.buildid"; //$NON-NLS-1$
	private static final String DEFAULT_BUILD_ID = "0"; //$NON-NLS-1$
	private final static String BUILD_ID;

	private static final String PROPERTY_CHANGE_ID = "jmc.changeid"; //$NON-NLS-1$
	private static final String DEFAULT_CHANGE_ID = "0"; //$NON-NLS-1$
	private static final String CHANGE_ID;

	private static final String PROPERTY_DATE = "jmc.date"; //$NON-NLS-1$
	private static final String DEFAULT_DATE = "0"; //$NON-NLS-1$
	private static final String DATE;

	static {
		Properties versionProperties = getVersionProperties();
		FULL_VERSION = getVersionProperty(versionProperties, PROPERTY_FULL_VERSION, DEFAULT_FULL_VERSION);
		QUALIFIER = getVersionProperty(versionProperties, PROPERTY_QUALIFIER, DEFAULT_QUALIFIER);
		BUILD_ID = getVersionProperty(versionProperties, PROPERTY_BUILD_ID, DEFAULT_BUILD_ID);
		CHANGE_ID = getVersionProperty(versionProperties, PROPERTY_CHANGE_ID, DEFAULT_CHANGE_ID);
		DATE = getVersionProperty(versionProperties, PROPERTY_DATE, DEFAULT_DATE);
	}
	
	private static String getVersionProperty(Properties versionProperties, String propertyName, String defaultValue) {
		if (versionProperties != null) {
			String propertyValue = versionProperties.getProperty(propertyName);
			if (propertyValue != null && !propertyValue.startsWith("@")) { //$NON-NLS-1$
				return propertyValue;
			}
		}
		return defaultValue;
	}

	private static Properties getVersionProperties() {
		// Just one thread executing this when it gets executed.
		Properties versionProperties = new Properties();
		InputStream is = MCVersion.class.getResourceAsStream("/version.properties"); //$NON-NLS-1$
		if (is == null) {
			LOGGER.log(Level.SEVERE, "Could not open version.properties file."); //$NON-NLS-1$
			return null;
		}
		try {
			versionProperties.load(is);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Error loading version.properties file.", e); //$NON-NLS-1$
			return null;
		} finally {
			IOToolkit.closeSilently(is);
		}
		return versionProperties;
	}

	public static String getFullVersion() {
		return FULL_VERSION;
	}

	public static String getChangeId() {
		return CHANGE_ID;
	}

	public static String getQualifier() {
		return QUALIFIER;
	}

	public static String getBuildId() {
		return BUILD_ID;
	}

	public static String getDate() {
		return DATE;
	}
}
