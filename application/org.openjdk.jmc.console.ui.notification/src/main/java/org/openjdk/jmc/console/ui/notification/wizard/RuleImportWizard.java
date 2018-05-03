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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import org.openjdk.jmc.common.util.XmlToolkit;
import org.openjdk.jmc.console.ui.notification.NotificationPlugin;
import org.openjdk.jmc.rjmx.triggers.TriggerRule;
import org.openjdk.jmc.ui.common.util.StatusFactory;
import org.openjdk.jmc.ui.misc.DialogToolkit;
import org.openjdk.jmc.ui.wizards.SimpleImportFromFileWizardPage;

/**
 * Wizard for importing trigger rules
 */
public class RuleImportWizard extends Wizard implements IImportWizard {
	private static final String RULE_CANT_BE_IMPORTED = "* {0}\\{1}"; //$NON-NLS-1$

	private SimpleImportFromFileWizardPage m_wizardPage;

	@Override
	public boolean performFinish() {
		File file = m_wizardPage.getFile();
		if (file != null) {
			try {
				importRules(file);
				m_wizardPage.storeFilename();
				return true;
			} catch (Exception e) {
				ErrorDialog
						.openError(getShell(), Messages.RuleImportWizard_ERROR_IMPORT_DIALOG_TITLE,
								MessageFormat.format(Messages.RuleImportWizard_ERROR_IMPORTING_FROM_FILE_X,
										new Object[] {file.toString()}),
								StatusFactory.createErr(e.getMessage(), e, true));
			}
		}
		return false;
	}

	private void showLog(List<TriggerRule> conflictingRules) {
		StringBuilder messageBuilder = new StringBuilder();
		messageBuilder.append(Messages.RuleImportWizard_RULES_WITH_THESE_NAMES_COULD_NOT_IMPORT);
		for (TriggerRule r : conflictingRules) {
			messageBuilder
					.append(MessageFormat.format(RULE_CANT_BE_IMPORTED, new Object[] {r.getRulePath(), r.getName()}));
			messageBuilder.append(SWT.LF);
		}
		DialogToolkit.showWarning(getShell(), Messages.RuleImportWizard_WARNING_DIALOG_TITLE,
				messageBuilder.toString());
	}

	private void importRules(File file) throws FileNotFoundException, IOException, SAXException {
		Element root = XmlToolkit.loadDocumentFromFile(file).getDocumentElement();
		List<TriggerRule> badRules = NotificationPlugin.getDefault().getNotificationRepository().importFromXML(root);
		if (!badRules.isEmpty()) {
			showLog(badRules);
		}
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setDialogSettings(NotificationPlugin.getDefault().getDialogSettings());
		setWindowTitle(Messages.RuleImportWizard_IMPORT_TIGGER_RULES_TITLE);

		m_wizardPage = new SimpleImportFromFileWizardPage(Messages.RuleImportWizard_WIZARD_NAME_IMPORT_RULES, "xml"); //$NON-NLS-1$
		m_wizardPage.setImageDescriptor(
				NotificationPlugin.getDefault().getMCImageDescriptor(NotificationPlugin.IMG_RULE_WIZRD));
		m_wizardPage.setTitle(Messages.RuleImportWizard_WINDOW_TITLE_IMPORT_TRIGGER_RULES);
		m_wizardPage.setMessage(Messages.RuleImportWizard_WIZARD_MESSAGE_IMPORT_RULES);

		addPage(m_wizardPage);
	}
}
