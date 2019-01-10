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
package org.openjdk.jmc.rcp.application.uitest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.openjdk.jmc.test.jemmy.MCJemmyTestBase;
import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCButton;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCDialog;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCMenu;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTable;

/**
 * Class testing update site related components
 */
public class UpdateSiteTest extends MCJemmyTestBase {
	private static final String UPDATESITE_PROP_PREFIX = "updatesite.";
	private static final String JMC_VERSION = System.getProperty("jmc.test.jmc.version", "7.1.0");
	private static final String KEY_UPDATE_PROPERTIES_PATH = "org.openjdk.jmc.updatesites.properties";

	/**
	 * This test verifies that, when the property "org.openjdk.jmc.updatesites.properties" is set to
	 * something we're supposed to add the update sites from the properties file to the ones
	 * available for updates. Note that this only tests that, at least, the sites in the properties
	 * file or the predefined external sites (specified in the default properties file shipped with
	 * JMC) are present.
	 */
	@Test
	public void testUpdateSiteProperty() {
		Assume.assumeTrue("Update site properties file property not set",
				System.getProperty(KEY_UPDATE_PROPERTIES_PATH) != null);
		String updateSitePropertiesPath = System.getProperty(KEY_UPDATE_PROPERTIES_PATH);
		if (updateSitePropertiesPath != null) {
			List<String> propsUpdateSites = getUpdateSitesFromPropsFile(updateSitePropertiesPath);
			Assert.assertTrue(
					"Update site properties file at " + updateSitePropertiesPath + " is empty or incorrectly formatted",
					propsUpdateSites.size() > 0);
			List<String> updateSites = getUpdateSitesFromPrefs();
			for (String site : propsUpdateSites) {
				Assert.assertTrue(
						"Update site \"" + site + "\" is missing from the list of update sites. Expected sites: "
								+ propsUpdateSites + ", Found sites (in preferences): " + updateSites,
						updateSites.contains(site));
			}
		}
	}

	private static List<String> getUpdateSitesFromPrefs() {
		MCDialog preferences = MCMenu.openPreferencesDialog();
		preferences.selectTreeItem("Install/Update", "Available Software Sites");
		MCTable siteTable = preferences.getFirstTable();
		List<String> updateSiteURLs = siteTable.getColumnItemTexts("Location");
		// Saving picture of preferences page in case we find no update sites (for easier debugging)
		if (updateSiteURLs.size() == 0) {
			MCJemmyBase.saveMcImage("UpdateSitePrefs");
		}
		preferences.clickButton(MCButton.Labels.APPLY_AND_CLOSE);
		return updateSiteURLs;
	}

	private static List<String> getUpdateSitesFromPropsFile(String propsFilePath) {
		Properties props = readProperties(propsFilePath);
		List<String> updateSites = new ArrayList<>();
		int i = 0;
		String url;
		do {
			url = props.getProperty(UPDATESITE_PROP_PREFIX + i++);
			if (url != null) {
				updateSites.add((MessageFormat.format(url, JMC_VERSION)));
			}
		} while (url != null);
		return updateSites;
	}

	private static Properties readProperties(String propertiesPath) {
		if (propertiesPath != null) {
			File propertiesFile = new File(propertiesPath);
			if (propertiesFile.isFile() && propertiesFile.canRead()) {
				try (InputStream is = new FileInputStream(propertiesFile)) {
					Properties props = new Properties();
					props.load(is);
					return props;
				} catch (IOException e) {
					Assert.fail("Could not load update sites properties file: " + e);
				}
			} else {
				Assert.fail(
						"Update sites properties file " + propertiesPath + " isn't readable or doesn't seem to exist");
			}
		}
		return null;
	}
}
