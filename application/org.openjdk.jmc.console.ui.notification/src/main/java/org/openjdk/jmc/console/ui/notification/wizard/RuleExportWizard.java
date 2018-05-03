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
package org.openjdk.jmc.console.ui.notification.wizard;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.w3c.dom.Document;

import org.openjdk.jmc.common.util.XmlToolkit;
import org.openjdk.jmc.console.ui.notification.NotificationPlugin;
import org.openjdk.jmc.console.ui.notification.tab.TriggerContentProvider;
import org.openjdk.jmc.console.ui.notification.tab.TriggerLabelProvider;
import org.openjdk.jmc.rjmx.triggers.TriggerRule;
import org.openjdk.jmc.rjmx.triggers.internal.NotificationRegistry;
import org.openjdk.jmc.ui.common.util.StatusFactory;
import org.openjdk.jmc.ui.wizards.ExportTreeToFileWizardPage;

/**
 * Wizard for exporting rules
 */
public class RuleExportWizard extends Wizard implements IExportWizard {
	/**
	 * WizardPage for rules export
	 */
	class RuleExportWizardPage extends ExportTreeToFileWizardPage {

		public RuleExportWizardPage(String pageName) {
			super(pageName, "xml"); //$NON-NLS-1$
		}

		@Override
		protected void initializeViewer(TreeViewer viewer) {
			ITreeContentProvider itcp = new TriggerContentProvider(model);
			Object[] rootNodes = itcp.getElements("Whatever. The notification model is static!"); //$NON-NLS-1$
			viewer.setAutoExpandLevel(rootNodes.length == 1 ? AbstractTreeViewer.ALL_LEVELS : 0);
			viewer.setContentProvider(new TriggerContentProvider(model));
			viewer.setLabelProvider(new TriggerLabelProvider());
			viewer.setComparator(new ViewerComparator());
			viewer.setInput("Whatever. The notification model is static!"); //$NON-NLS-1$
		}
	}

	private RuleExportWizardPage m_wizardPage;
	private NotificationRegistry model;

	@Override
	public boolean performFinish() {
		if (m_wizardPage.isExportToFileOk()) {
			try {
				boolean export = export(m_wizardPage.getFile(), filterOutRules(m_wizardPage.getSelectedItems()));
				if (export) {
					m_wizardPage.storeFilename();
				}
				return export;
			} catch (IOException e) {
				ErrorDialog.openError(getShell(), Messages.RuleExportWizard_ERROR_EXPORTING_RULES_TITLE,
						Messages.RuleExportWizard_ERROR_EXPORTING_RULES_TO_FILE_X_TEXT + "\n\n" //$NON-NLS-1$
								+ m_wizardPage.getFile(),
						StatusFactory.createErr(e.getMessage(), e, true));
			}
		}
		return false;
	}

	private boolean export(File file, Collection<TriggerRule> selection) throws IOException {
		Document doc = model.exportToXml(selection, false);
		XmlToolkit.storeDocumentToFile(doc, file);
		return true;
	}

	private Collection<TriggerRule> filterOutRules(Collection<?> collection) {
		HashSet<TriggerRule> rules = new HashSet<>();
		for (Object next : collection) {
			if (next instanceof TriggerRule) {
				rules.add((TriggerRule) next);
			}
		}
		return rules;
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setDialogSettings(NotificationPlugin.getDefault().getDialogSettings());
		setWindowTitle(Messages.RuleExportWizard_TITLE_EXPORT_TRIGGER_RULES);

		model = NotificationPlugin.getDefault().getNotificationRepository();
		m_wizardPage = new RuleExportWizardPage(Messages.RuleExportWizard_RULE_EXPORT_WIZARD_NAME);
		m_wizardPage.setImageDescriptor(
				NotificationPlugin.getDefault().getMCImageDescriptor(NotificationPlugin.IMG_RULE_WIZRD));
		m_wizardPage.setTitle(Messages.RuleExportWizard_WINDOW_TITLE_EXPORT_TRIGGER_RULES);
		m_wizardPage.setMessage(Messages.RuleExportWizard_DEFAULT_WIZARD_EXPORT_MESSAGE);

		addPage(m_wizardPage);
	}

	@Override
	public boolean canFinish() {
		return m_wizardPage != null && m_wizardPage.isPageComplete();
	}
}
