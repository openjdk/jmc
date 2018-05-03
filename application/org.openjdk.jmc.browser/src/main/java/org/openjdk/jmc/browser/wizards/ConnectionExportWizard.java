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

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

import org.openjdk.jmc.browser.JVMBrowserPlugin;
import org.openjdk.jmc.browser.views.BrowserLabelProvider;
import org.openjdk.jmc.browser.views.Folder;
import org.openjdk.jmc.common.util.XmlToolkit;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.servermodel.internal.Server;
import org.openjdk.jmc.rjmx.servermodel.internal.ServerModel;
import org.openjdk.jmc.ui.common.util.StatusFactory;
import org.openjdk.jmc.ui.misc.TreeStructureContentProvider;
import org.openjdk.jmc.ui.wizards.ExportTreeToFileWizardPage;

/**
 * Wizard for exporting {@link Server}s
 */
public class ConnectionExportWizard extends Wizard implements IExportWizard {

	private final ServerModel model = RJMXPlugin.getDefault().getService(ServerModel.class);

	private class ExportConnectionsToFile extends ExportTreeToFileWizardPage {

		private ExportConnectionsToFile(String pageName) {
			super(pageName, "xml"); //$NON-NLS-1$
		}

		@Override
		protected void initializeViewer(TreeViewer viewer) {
			viewer.setAutoExpandLevel(2);
			viewer.setContentProvider(new TreeStructureContentProvider());
			Folder root = new Folder(null, "root"); //$NON-NLS-1$
			for (Server server : model.elements()) {
				if (server.getDiscoveryInfo() == null) {
					root.getFolder(server.getPath()).addLeaf(server);
				}
			}
			viewer.setInput(root);
			viewer.setLabelProvider(new BrowserLabelProvider());
		}
	}

	private ExportConnectionsToFile m_wizardPage;

	/**
	 *
	 */
	@Override
	public boolean performFinish() {
		if (m_wizardPage.isExportToFileOk()) {
			try {
				Server[] servers = filterOutNodes(m_wizardPage.getSelectedItems());
				XmlToolkit.storeDocumentToFile(model.exportServers(servers), m_wizardPage.getFile());
				m_wizardPage.storeFilename();
				return true;
			} catch (Exception e) {
				ErrorDialog.openError(getShell(), Messages.ConnectionExportWizard_EXPORT_FILE_ERROR_TITLE,
						Messages.ConnectionExportWizard_FILE_ERROR_EXPORT_TEXT + "\n\n" + m_wizardPage.getFile(), //$NON-NLS-1$
						StatusFactory.createErr(e.getMessage(), e, true));
			}
		}
		return false;
	}

	/**
	 * Filter out what the nodes that should be exported
	 *
	 * @param collection
	 *            the collection to filter
	 * @return the filter collection
	 */
	protected Server[] filterOutNodes(Collection<?> collection) {
		ArrayList<Server> servers = new ArrayList<>();
		for (Object next : collection) {
			if (next instanceof Server) {
				servers.add((Server) next);
			}
		}
		return servers.toArray(new Server[servers.size()]);
	}

	/**
	 * Initializes the wizard. That is:
	 * <li>sets the {@link DialogSettings}
	 * <li>adds an export wizard page
	 * <li>sets the window icon
	 * <li>sets the title.
	 */
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setDialogSettings(JVMBrowserPlugin.getDefault().getDialogSettings());
		m_wizardPage = new ExportConnectionsToFile(Messages.ConnectionExportWizard_EXPORT_CONNECTIONS_WIZARD_PAGE_NAME);
		m_wizardPage.setTitle(Messages.ConnectionExportWizard_EXPORT_CONNECTIONS_TITLE);
		m_wizardPage.setMessage(Messages.ConnectionExportWizard_SELECTIONCONNECTIONS_TO_EXPORT);
		m_wizardPage.setImageDescriptor(
				JVMBrowserPlugin.getDefault().getMCImageDescriptor(JVMBrowserPlugin.ICON_BANNER_CONNECTION_WIZARD));
		setWindowTitle(Messages.ConnectionExportWizard_WIZARD_EXPORT_CONNECTION_TITLE);
		addPage(m_wizardPage);
	}

	/**
	 * return whether this wizard can finish or not.
	 */
	@Override
	public boolean canFinish() {
		return m_wizardPage != null && m_wizardPage.isPageComplete();
	}

}
