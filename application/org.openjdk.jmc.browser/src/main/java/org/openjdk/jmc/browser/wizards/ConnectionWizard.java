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
package org.openjdk.jmc.browser.wizards;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.openjdk.jmc.browser.JVMBrowserPlugin;
import org.openjdk.jmc.rjmx.servermodel.internal.Server;
import org.openjdk.jmc.ui.common.action.UserActionJob;
import org.openjdk.jmc.ui.common.util.AdapterUtil;
import org.openjdk.jmc.ui.misc.DialogToolkit;

/**
 * The wizard used to create and edit connections, and connect to them.
 */
public class ConnectionWizard extends Wizard implements INewWizard {
	private final static String SECTION_CONNECTION_WIZARD = "org.openjdk.jmc.browser.wizards.ConnectionWizard"; //$NON-NLS-1$
	private final Server server;
	private final String serverPath;
	private final boolean startOnNewConnectionPage;
	private final ConnectionWizardModel serverConnectModel;

	public ConnectionWizard() {
		this(null);
	}

	private ConnectionWizard(String serverPath) {
		this(null, serverPath, true, Messages.ConnectionWizard_TITLE_NEW_CONNECTION);
	}

	public static void opeNewServerWizard(String serverPath) {
		DialogToolkit.openWizardWithHelp(new ConnectionWizard(serverPath));
	}

	public static void openPropertiesWizard(Server server) {
		DialogToolkit.openWizardWithHelp(
				new ConnectionWizard(server, null, true, Messages.ConnectionWizard_TITLE_CONNECTION_PROPERTIES));
	}

	public static void openConnectWizard() {
		DialogToolkit
				.openWizardWithHelp(new ConnectionWizard(null, null, false, Messages.ConnectionWizard_TITLE_CONNECT));
	}

	private ConnectionWizard(Server server, String serverPath, boolean startOnNewConnectionPage, String title) {
		this.server = server;
		this.serverPath = serverPath;
		setNeedsProgressMonitor(false);
		initializeDefaultPageImageDescriptor();
		initializeDialogSettings();
		this.startOnNewConnectionPage = startOnNewConnectionPage;
		serverConnectModel = new ConnectionWizardModel();
		setWindowTitle(title);
	}

	private void initializeDialogSettings() {
		IDialogSettings wizardSettings = JVMBrowserPlugin.getDefault().getDialogSettings()
				.getSection(SECTION_CONNECTION_WIZARD);
		if (wizardSettings == null) {
			JVMBrowserPlugin.getDefault().getDialogSettings().addNewSection(SECTION_CONNECTION_WIZARD);
		}
		setDialogSettings(wizardSettings);
	}

	private void initializeDefaultPageImageDescriptor() {
		setDefaultPageImageDescriptor(
				JVMBrowserPlugin.getDefault().getMCImageDescriptor(JVMBrowserPlugin.ICON_BANNER_CONNECTION_WIZARD));
	}

	@Override
	public boolean performFinish() {
		if (getConnectionWizardPage().isTheCurrentPage()) {
			getConnectionWizardPage().updateModel();
		} else if (serverConnectModel.action != null) {
			// If action is a wizard then it will be appended to the connection wizard by ActionWizardPage.getNextPage()
			if (!AdapterUtil.hasAdapter(serverConnectModel.action, IWizard.class)) {
				new UserActionJob(serverConnectModel.action).schedule();
			}
		}
		if (serverConnectModel.createdServer != null) {
			serverConnectModel.serverModel.insert(serverConnectModel.createdServer);
		}
		return true;
	}

	// FIXME: Rewrite this. Create different wizards, perhaps reusing the same wizard pages.
	@Override
	public boolean canFinish() {
		if (getConnectionSelectionWizardPage().isTheCurrentPage()) {
			return false;
		}
		if (getConnectionWizardPage().isTheCurrentPage()) {
			return getConnectionWizardPage().isPageComplete();
		}
		if (getUserActionWizardPage().isTheCurrentPage()) {
			return getUserActionWizardPage().isPageComplete();
		}
		return false;
	}

	@Override
	public void addPages() {
		addPage(new ConnectionSelectionWizardPage(serverConnectModel));
		addPage(new ConnectionWizardPage(server, serverPath, serverConnectModel));
		addPage(new ActionWizardPage(serverConnectModel));

		if (startOnNewConnectionPage) {
			getConnectionWizardPage().setPreviousPage(null);
			getConnectionWizardPage().setNextPage(getUserActionWizardPage());
			getUserActionWizardPage().setPreviousPage(getConnectionWizardPage());
		} else {
			getConnectionSelectionWizardPage().setNextPage(getUserActionWizardPage());
			getUserActionWizardPage().setPreviousPage(getConnectionSelectionWizardPage());
		}
		if (server != null) {
			getConnectionWizardPage().setPreviousPage(null);
			getConnectionWizardPage().setNextPage(null);
		}
	}

	ConnectionSelectionWizardPage getConnectionSelectionWizardPage() {
		return (ConnectionSelectionWizardPage) getPage(ConnectionSelectionWizardPage.PAGE_NAME);
	}

	ConnectionWizardPage getConnectionWizardPage() {
		return (ConnectionWizardPage) getPage(ConnectionWizardPage.PAGE_NAME);
	}

	ActionWizardPage getUserActionWizardPage() {
		return (ActionWizardPage) getPage(ActionWizardPage.PAGE_NAME);
	}

	@Override
	public IWizardPage getStartingPage() {
		return server != null ? getConnectionWizardPage()
				: startOnNewConnectionPage ? getConnectionWizardPage() : getConnectionSelectionWizardPage();
	}

	@Override
	public IWizardPage getPreviousPage(IWizardPage page) {
		return page.getPreviousPage();
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {

	}
}
