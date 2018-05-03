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

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.w3c.dom.Document;

import org.openjdk.jmc.browser.JVMBrowserPlugin;
import org.openjdk.jmc.common.util.XmlToolkit;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.servermodel.internal.Server;
import org.openjdk.jmc.rjmx.servermodel.internal.ServerModel;
import org.openjdk.jmc.ui.common.util.StatusFactory;
import org.openjdk.jmc.ui.wizards.SimpleImportFromFileWizardPage;

/**
 * Wizard for importing {@link Server}s.
 */
public class ConnectionImportWizard extends Wizard implements IImportWizard {
	private SimpleImportFromFileWizardPage m_wizardPage;

	/**
	 *
	 */
	@Override
	public boolean performFinish() {
		File file = m_wizardPage.getFile();
		if (file != null) {
			try {
				Document doc = XmlToolkit.loadDocumentFromFile(file);
				RJMXPlugin.getDefault().getService(ServerModel.class).importServers(doc);
				m_wizardPage.storeFilename();
				return true;
			} catch (Exception e) {
				ErrorDialog
						.openError(getShell(), Messages.ConnectionImportWizard_ERROR_IMPORTING_CONNECTIONS_TITLE,
								MessageFormat.format(Messages.ConnectionImportWizard_ERROR_IMPORTING_FROM_FILE_X_TEXT,
										new Object[] {file.toString()}),
								StatusFactory.createErr(e.getMessage(), e, true));
			}
		}
		return false;
	}

	/**
	 * Initializes the wizard. That is:
	 * <li>set {@link DialogSettings}
	 * <li>add an import wizard page
	 * <li>set the window icon
	 * <li>set the title.
	 */
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setDialogSettings(JVMBrowserPlugin.getDefault().getDialogSettings());
		m_wizardPage = new SimpleImportFromFileWizardPage(Messages.ConnectionImportWizard_IMPORT_CONNECTION_WIZARD_NAME,
				"xml"); //$NON-NLS-1$
		m_wizardPage.setTitle(Messages.ConnectionImportWizard_IMPORTCONNECTIONS_TITLE);
		m_wizardPage.setMessage(Messages.ConnectionImportWizard_SELECT_CONNECTION_FILE_FOR_IMPORT_TEXT);
		m_wizardPage.setImageDescriptor(
				JVMBrowserPlugin.getDefault().getMCImageDescriptor(JVMBrowserPlugin.ICON_BANNER_CONNECTION_WIZARD));
		addPage(m_wizardPage);
	}
}
