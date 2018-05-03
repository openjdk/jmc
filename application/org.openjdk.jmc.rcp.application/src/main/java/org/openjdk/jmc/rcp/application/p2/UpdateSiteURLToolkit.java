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
package org.openjdk.jmc.rcp.application.p2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.openjdk.jmc.rcp.application.ApplicationPlugin;

/**
 * Helper for resolving the update site URLs.
 */
public final class UpdateSiteURLToolkit {
	// Set to path of properties file for overriding update sites
	private static final String KEY_UPDATE_PROPERTIES_PATH = "org.openjdk.jmc.updatesites.properties"; //$NON-NLS-1$

	private static final Object SITES_LOCK = new Object();
	private static List<String> SITES;

	public static List<String> getUpdateSites() {
		synchronized (SITES_LOCK) {
			if (SITES == null) {
				List<String> updateSites = new ArrayList<>();
				Properties props = readOverrideProperties();
				if (props == null) {
					props = readDefaultProperties();
				}
				if (props != null) {
					int i = 0;
					String site;
					do {
						site = props.getProperty("updatesite." + i++); //$NON-NLS-1$
						if (site != null) {
							updateSites.add(MessageFormat.format(site, ApplicationPlugin.FULL_VERSION));
						}
					} while (site != null);
				}
				SITES = updateSites;
			}
			if (SITES.size() == 0) {
				ApplicationPlugin.getLogger().log(Level.INFO,
						"No updatesites configured. To manually configure updatesites set the " //$NON-NLS-1$
								+ KEY_UPDATE_PROPERTIES_PATH
								+ " property on the commandline to point to a file that specifies one or more 'updatesite.<num from 0 and up>=<updatesite url>' properties"); //$NON-NLS-1$
			}
			return SITES;
		}
	}

	private static Properties readDefaultProperties() {
		try (InputStream is = UpdateSiteURLToolkit.class.getClassLoader()
				.getResourceAsStream("updatesites.properties")) { //$NON-NLS-1$
			if (is != null) {
				Properties props = new Properties();
				props.load(is);
				return props;
			}
		} catch (IOException e) {
			ApplicationPlugin.getLogger().log(Level.WARNING, "Could not load default update sites", e); //$NON-NLS-1$
		}
		return null;
	}

	private static Properties readOverrideProperties() {
		String propertiesPath = System.getProperty(KEY_UPDATE_PROPERTIES_PATH);
		if (propertiesPath != null) {
			File propertiesFile = new File(propertiesPath);
			if (propertiesFile.isFile() && propertiesFile.canRead()) {
				try (InputStream is = new FileInputStream(propertiesFile)) {
					Properties props = new Properties();
					props.load(is);
					return props;
				} catch (IOException e) {
					ApplicationPlugin.getLogger().log(Level.WARNING, "Could not load override update sites", e); //$NON-NLS-1$
				}
			}
		}
		return null;
	}

	private UpdateSiteURLToolkit() {
		throw new AssertionError("Not to be instantiated!"); //$NON-NLS-1$
	}
}
