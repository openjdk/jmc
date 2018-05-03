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
package org.openjdk.jmc.flightrecorder.controlpanel.ui.wizards;

import java.io.IOException;

import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.openjdk.jmc.flightrecorder.configuration.events.IEventConfiguration;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.ControlPanel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.ConfigurationRepositoryFactory;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfigurationRepository;
import org.openjdk.jmc.ui.misc.DialogToolkit;
import org.openjdk.jmc.ui.wizards.ExportToFileWizardPage;

public class TemplateExportWizard extends Wizard implements IExportWizard {
	private static String WIZARD_KEY = "exportTemplateToFile"; //$NON-NLS-1$

	private static final class ExportTemplateToFile extends ExportToFileWizardPage {
		private IEventConfiguration selected;

		private ExportTemplateToFile(String pageName) {
			super(pageName, IEventConfiguration.JFC_FILE_EXTENSION);
		}

		@Override
		protected Composite createContents(Composite parent) {
			Table table = new Table(parent, SWT.SINGLE | SWT.BORDER);
			TableViewer tv = new TableViewer(table);
			EventConfigurationRepository repository = ConfigurationRepositoryFactory.create();

			tv.setContentProvider(new TemplateProvider(null));
			tv.setInput(repository);
			tv.setLabelProvider(new TemplateLabelProvider(true));
			tv.setSelection(null);
			tv.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					Object selectedObject = ((IStructuredSelection) event.getSelection()).getFirstElement();
					if (selectedObject instanceof IEventConfiguration) {
						selected = (IEventConfiguration) selectedObject;
					} else {
						selected = null;
					}
					updatePageComplete();
				}
			});
			return table;
		}

		@Override
		protected boolean isSelectionValid() {
			return selected != null;
		}

	}

	private ExportTemplateToFile m_wizardPage;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setDialogSettings(DialogSettings.getOrCreateSection(ControlPanel.getDefault().getDialogSettings(), WIZARD_KEY));
		setWindowTitle(Messages.EXPORT_TEMPLATE_WIZARD_TITLE);

		m_wizardPage = new ExportTemplateToFile(Messages.EXPORT_TEMPLATE_WIZARD_TITLE);
		// FIXME: Need large icon here, same as for the import wizard
//		m_wizardPage.setImageDescriptor(ControlPanel.getDefault()
//				.getMCImageDescriptor(ImageConstants.ICON_FLIGHT_RECORDING_CONFIGURATION_TEMPLATE));
		m_wizardPage.setTitle(Messages.EXPORT_TEMPLATE_WIZARD_TITLE);

		addPage(m_wizardPage);
	}

	@Override
	public boolean performFinish() {
		if (m_wizardPage.isExportToFileOk()) {
			try {
				m_wizardPage.selected.exportToFile(m_wizardPage.getFile());
				m_wizardPage.storeFilename();
				return true;
			} catch (IOException ioe) {
				DialogToolkit.showExceptionDialogAsync(m_wizardPage.getControl().getDisplay(),
						Messages.IMPORT_EXPORT_TOOLKIT_COULD_NOT_EXPORT_DIALOG_TITLE,
						NLS.bind(Messages.IMPORT_EXPORT_TOOLKIT_COULD_NOT_EXPORT_DIALOG_MESSAGE,
								ioe.getLocalizedMessage()),
						ioe);
			}
		}
		return false;
	}
}
