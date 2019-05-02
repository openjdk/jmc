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
package org.openjdk.jmc.test.jemmy.misc.wrappers;

import java.util.List;

import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.StyledText;
import org.jemmy.control.Wrap;
import org.jemmy.interfaces.Parent;
import org.jemmy.interfaces.Selectable;

import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;

/**
 * The Jemmy wrapper for the Mission Control Installation dialogs
 */
public class MCInstallation extends MCJemmyBase {
	private MCDialog aboutDialog;
	private MCDialog installationDetails;
	private final Wrap<? extends CTabFolder> tabFolder;

	public enum Tabs {
		INSTALLED_SOFTWARE("Installed Software"),
		INSTALLATION_HISTORY("Installation History"),
		FEATURES("Features"),
		PLUGINS("Plug-ins"),
		CONFIGURATION("Configuration");
		private final String text;

		private Tabs(final String text) {
			this.text = text;
		}

		@Override
		public String toString() {
			return text;
		}
	};

	public class InstalledSoftware {
		public MCButton updateButton() {
			show();
			return MCButton.getAnyByLabel(installationDetails.getDialogShell(), "Update...");
		}

		public MCButton uninstallButton() {
			show();
			return MCButton.getAnyByLabel(installationDetails.getDialogShell(), "Uninstall...");
		}

		public MCButton propertiesButton() {
			show();
			return MCButton.getAnyByLabel(installationDetails.getDialogShell(), "Properties");
		}

		public void show() {
			showTab(Tabs.INSTALLED_SOFTWARE);
		}
	}

	public class InstallationHistory {
		public MCTable table() {
			show();
			return MCTable.getByIndex(installationDetails.getDialogShell(), 0);
		}

		public MCButton compareButton() {
			show();
			return MCButton.getAnyByLabel(installationDetails.getDialogShell(), "Compare");
		}

		public MCButton deleteButton() {
			show();
			return MCButton.getAnyByLabel(installationDetails.getDialogShell(), "Delete");
		}

		public MCButton revertButton() {
			show();
			return MCButton.getAnyByLabel(installationDetails.getDialogShell(), "Revert");
		}

		public void show() {
			showTab(Tabs.INSTALLATION_HISTORY);
		}
	}

	public class Plugins {
		public MCTable table() {
			show();
			return MCTable.getAll(installationDetails).get(0);
		}

		public MCText filter() {
			show();
			return MCText.getFirstVisible(installationDetails.getDialogShell());
		}

		public MCButton legalInfoButton() {
			show();
			return MCButton.getAnyByLabel(installationDetails.getDialogShell(), "Legal Info");
		}

		public MCButton signingInfoButton() {
			show();
			return MCButton.getAnyByLabel(installationDetails.getDialogShell(), "Show Signing Info");
		}

		public MCButton columnsButton() {
			show();
			return MCButton.getAnyByLabel(installationDetails.getDialogShell(), "Columns...");
		}

		public void show() {
			showTab(Tabs.PLUGINS);
		}

		/*
		 * Returns the signing date for the currently selected plugin in the table. This method will
		 * fail unless the Signing Info pane is diplayed. If the plugin is not signed, the empty
		 * string will be returned.
		 *
		 * @return A string with the date when the plugin was signed, or empty string if not signed.
		 */
		public String getSigningDate() {
			show();
			return MCText.getVisible(installationDetails.getDialogShell(), 1).getText();
		}

		/*
		 * Returns the signing certificate for the currently selected plugin in the table. This
		 * method will fail unless the Signing Info pane is diplayed. If the plugin is not signed,
		 * the empty string will be returned.
		 *
		 * @return A string with the information about the certificate used when the plugin was
		 * signed, or empty string if not signed.
		 */
		@SuppressWarnings("unchecked")
		public String getSigningCertificate() {
			show();
			List<Wrap<? extends StyledText>> listOfVisibleStyledTextWraps = getVisible(
					installationDetails.getDialogShell().as(Parent.class, StyledText.class).lookup(StyledText.class));
			if (listOfVisibleStyledTextWraps.size() > 0) {
				return (String) listOfVisibleStyledTextWraps.get(0).getProperty("text");
			} else {
				return "";
			}
		}
	}

	@SuppressWarnings("unused")
	private class Configuration {
		public MCText text() {
			show();
			return MCText.getFirstVisible(installationDetails.getDialogShell());
		}

		public MCButton viewErrorLogButton() {
			show();
			return MCButton.getAnyByLabel(installationDetails.getDialogShell(), "View Error Log");
		}

		public MCButton copyToClipboardButton() {
			show();
			return MCButton.getAnyByLabel(installationDetails.getDialogShell(), "Copy to Clipboard");
		}

		public void show() {
			showTab(Tabs.CONFIGURATION);
		}
	}

	public InstalledSoftware installedSoftwareTab;
	public InstallationHistory historyTab;
	public Plugins pluginsTab;
	public Configuration configurationTab;

	@SuppressWarnings("unchecked")
	public MCInstallation() {
		aboutDialog = MCMenu.openAboutDialog();
		aboutDialog.clickButton("Installation Details");

		// Note: Different name when running from Eclipse
		installationDetails = new MCDialog("Installation Details");

		tabFolder = installationDetails.getDialogShell().as(Parent.class, CTabFolder.class).lookup(CTabFolder.class)
				.wrap();

		// Installed Software is the default tab
		installedSoftwareTab = new InstalledSoftware();
		historyTab = new InstallationHistory();
		pluginsTab = new Plugins();
		configurationTab = new Configuration();
		// The tab Feature is sometimes visible (when launched from Eclipse?)
	}

	/**
	 * Closes the installation dialog
	 */
	public void close() {
		installationDetails.closeWithButton(MCButton.Labels.CLOSE);
		installationDetails = null;
		aboutDialog.closeWithButton(MCButton.Labels.CLOSE);
	}

	/**
	 * Shows the tab
	 *
	 * @param tab
	 *            The tab to show
	 */
	@SuppressWarnings("unchecked")
	public void showTab(MCInstallation.Tabs tab) {
		// Only doing the selection if necessary (substring matching both for the check and the actual selection)
		if (!String.class.cast(tabFolder.getProperty(Selectable.STATE_PROP_NAME)).contains(tab.toString())) {
			Selectable<String> selectable = tabFolder.as(Selectable.class);
			selectable.selector().select(tab.toString());
			waitForIdle();
		}
	}
}
