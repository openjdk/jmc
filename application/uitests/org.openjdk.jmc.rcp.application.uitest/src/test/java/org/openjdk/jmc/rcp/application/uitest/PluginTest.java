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

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import org.openjdk.jmc.test.jemmy.MCJemmyTestBase;
import org.openjdk.jmc.test.jemmy.MCUITestRule;
import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCInstallation;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTable.TableRow;

/**
 * General UI tests for plugins.
 */
public class PluginTest extends MCJemmyTestBase {
	static final List<String> ORACLE_SIGNATURE_PATTERN_LIST = Arrays.asList(new String[] {"L=Redwood City",
			"CN=Oracle America", "OU=Software Engineering", "ST=California", "C=US", "O=Oracle America"});
	private static final Long MILLISECONDS_PER_WEEK = Long.valueOf(1000 * 60 * 60 * 24 * 7);
	private static MCInstallation installation;

	@ClassRule
	public static MCUITestRule classTestRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void before() {
			installation = new MCInstallation();
		}

		@Override
		public void after() {
			installation.close();
		}
	};

	private boolean isCorrectSignature(String signingCertificateString) {
		// testing that we have equal amount of tokens
		if (signingCertificateString.split("=").length != ORACLE_SIGNATURE_PATTERN_LIST.size() + 1) {
			return false;
		}
		// making sure that we have each token
		for (String token : ORACLE_SIGNATURE_PATTERN_LIST) {
			if (!signingCertificateString.contains(token)) {
				return false;
			}
		}
		return true;
	}

	private Set<String> getPluginList(String propertyName) {
		String plugins = System.getProperty(propertyName);
		ArrayList<String> allPlugins = new ArrayList<>(Arrays.asList(plugins.split(",")));
		// Remove any empty element
		allPlugins.removeAll(Arrays.asList("", null));
		return new HashSet<>(allPlugins);
	}

	private void showPluginsTab() {
		installation.pluginsTab.show();
	}

	private Set<String> getOraclePlugins() {
		List<TableRow> pluginRows = installation.pluginsTab.table().getRows();
		System.out.println("Total number of plugins: " + pluginRows.size());

		installation.pluginsTab.filter().setText("oracle");
		MCJemmyBase.waitForIdle();
		pluginRows = installation.pluginsTab.table().getRows();
		System.out.println("Total number of Oracle plugins: " + pluginRows.size());

		Set<String> plugins = new HashSet<>();
		String pluginId;
		for (TableRow plugin : pluginRows) {
			pluginId = plugin.getColumns().get(4); // The fourth column is the id of the plugin
			System.out.println("Found: '" + pluginId + "'");
			plugins.add(pluginId);
		}
		return plugins;
	}

	/**
	 * The purpose of the test is to verify that all the required plugins are installed, but none of
	 * the forbidden ones are. This test is designed to run twice. The first run will be before the
	 * installation of plug-ins. We then verify that JMC is a vanilla installation (no additional
	 * plug-ins installed). The second run is after the plugins should have been installed, and we
	 * then verify that they have indeed been installed. No parameters are used, but the list of
	 * required and forbidden plugins is supposed to be defined through two system properties:
	 * required.plugins and forbidden.plugins.
	 */
	@Test
	public void existenceOfPlugins() {
		assumePropertySet("required.plugins");
		assumePropertySet("forbidden.plugins");

		HashSet<String> requiredPlugins = new HashSet<>(getPluginList("required.plugins"));
		System.out.println("Required plug-ins are: " + requiredPlugins);

		HashSet<String> forbiddenPlugins = new HashSet<>(getPluginList("forbidden.plugins"));
		System.out.println("Forbidden plug-ins are: " + forbiddenPlugins);

		showPluginsTab();

		Set<String> foundPlugins = getOraclePlugins();

		HashSet<String> foundRequiredPlugins = new HashSet<>(foundPlugins);
		foundRequiredPlugins.retainAll(requiredPlugins);

		HashSet<String> foundForbiddenPlugins = new HashSet<>(foundPlugins);
		foundForbiddenPlugins.retainAll(forbiddenPlugins);

		Assert.assertTrue("Forbidden plug-in(s) found: " + foundForbiddenPlugins.toString(),
				foundForbiddenPlugins.size() == 0);
		Assert.assertTrue("Required plug-in(s) not found: " + requiredPlugins.toString(),
				foundRequiredPlugins.size() == requiredPlugins.size());
	}

	/**
	 * Tests that all the Oracle plugins are signed, matching a predefined pattern. Also verifies
	 * that the plugins are signed in the past. If a plugin is signed more than a year in the past,
	 * this will be logged but not considered a test failure.
	 */
	@Test
	public void pluginsAreSigned() {
		assumePropertySet("plugins.installed");
		HashSet<String> installedPlugins = new HashSet<>(getPluginList("plugins.installed"));

		showPluginsTab();
		// Only showing Oracle plugins will make the table smaller and the test faster
		Set<String> foundOraclePlugins = getOraclePlugins();

		// Verifying that the plugins that should be installed are present
		ArrayList<String> notInstalledPlugins = new ArrayList<>();
		for (String plugin : installedPlugins) {
			if (!foundOraclePlugins.contains(plugin)) {
				notInstalledPlugins.add(plugin);
			}
		}
		Assert.assertTrue(
				"The following plugins should be installed, but is not: " + String.join(", ", notInstalledPlugins),
				notInstalledPlugins.isEmpty());

		Boolean signingInfoDisplayed = false;
		ArrayList<String> pluginsSignedInTheFuture = new ArrayList<>();
		ArrayList<String> pluginsWithUnparsableSignatureDate = new ArrayList<>();
		ArrayList<String> pluginsWithSignatureNotMatchingPattern = new ArrayList<>();
		for (String plugin : installedPlugins) {
			installation.pluginsTab.table().select(plugin);
			if (!signingInfoDisplayed) {
				// A plugin must be selected before the button is displayed
				installation.pluginsTab.signingInfoButton().click();
				signingInfoDisplayed = true;
			}
			String sd = installation.pluginsTab.getSigningDate();
			try {
				Date signedDate = DateFormat.getDateInstance().parse(sd);
				Date now = new Date();
				if (!signedDate.before(now)) {
					pluginsSignedInTheFuture.add(plugin + " was signed in the future. Signed on: "
							+ signedDate.toString() + " and now is: " + now.toString());
				}

				if (((now.getTime() - signedDate.getTime()) / (52 * MILLISECONDS_PER_WEEK)) > 1) {
					System.out.println("The plugin " + plugin + " was signed more than a year ago (on "
							+ signedDate.toString() + ").");
				}
			} catch (ParseException e) {
				pluginsWithUnparsableSignatureDate.add("Plugin: '" + plugin + "' Signature date: '" + sd + "'");
				e.printStackTrace();
			}
			String sc = installation.pluginsTab.getSigningCertificate();

			if (!isCorrectSignature(sc)) {
				pluginsWithSignatureNotMatchingPattern.add("Plugin '" + plugin + "': Expected content='"
						+ ORACLE_SIGNATURE_PATTERN_LIST + "' Actual content='" + sc + "'.");
			}
		}

		Assert.assertTrue("Problems with future signature dates for plugins was detected:\n"
				+ String.join(";\n", pluginsSignedInTheFuture), pluginsSignedInTheFuture.isEmpty());
		Assert.assertTrue(
				"Problems with parsing signature dates for plugins was detected:\n"
						+ String.join(";\n", pluginsWithUnparsableSignatureDate),
				pluginsWithUnparsableSignatureDate.isEmpty());
		Assert.assertTrue(
				"Problems with not matching signature content for plugins was detected:\n"
						+ String.join(";\n", pluginsWithSignatureNotMatchingPattern),
				pluginsWithSignatureNotMatchingPattern.isEmpty());
	}
}
