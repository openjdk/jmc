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
package org.openjdk.jmc.flightrecorder.ui;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

import org.openjdk.jmc.common.util.StateToolkit;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.misc.DialogToolkit;
import org.openjdk.jmc.ui.wizards.ExportToFileWizardPage;

// FIXME: Choose an appropriate icon for this wizard
public class PagesExportWizard extends Wizard implements IExportWizard {
	private static String WIZARD_KEY = "exportFlightRecordingPagesToFile"; //$NON-NLS-1$
	public static final Charset CHARSET = StandardCharsets.UTF_8;
	public static final String FILE_ENDING = "xml"; //$NON-NLS-1$

	private static class ExportPagesWizardPage extends ExportToFileWizardPage {
		private CheckboxTreeViewer ctw;

		public ExportPagesWizardPage() {
			super(WIZARD_KEY, FILE_ENDING);
			setTitle(Messages.PAGE_EXPORT_WIZARD_TITLE);
		}

		@Override
		protected Composite createContents(Composite parent) {
			ctw = new CheckboxTreeViewer(parent);
			ctw.setContentProvider(JfrOutlinePage.CONTENT_PROVIDER);
			ctw.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					return ((DataPageDescriptor) element).getName();
				}

				@Override
				public Image getImage(Object element) {
					return (Image) JFaceResources.getResources()
							.get(((DataPageDescriptor) element).getImageDescriptor());
				}
			});
			ctw.addCheckStateListener(change -> updatePageComplete());
			ctw.setInput(FlightRecorderUI.getDefault().getPageManager().getRootPages());
			return ctw.getTree();
		}

		@Override
		protected boolean isSelectionValid() {
			Object[] selected = getSelectedItems();
			return selected != null && selected.length > 0;
		}

		public Object[] getSelectedItems() {
			return ctw.getCheckedElements();
		}
	}

	private ExportPagesWizardPage wizardPage;

	@Override
	public boolean performFinish() {
		if (wizardPage.isExportToFileOk()) {
			@SuppressWarnings({"rawtypes", "unchecked"})
			Set<DataPageDescriptor> pages = new HashSet(Arrays.asList(wizardPage.getSelectedItems()));
			try (FileOutputStream fos = new FileOutputStream(wizardPage.getFile())) {
				OutputStreamWriter osw = new OutputStreamWriter(new BufferedOutputStream(fos), CHARSET);
				StateToolkit.writeAsXml(state -> PageManager.savePages(pages, state), osw);
				wizardPage.storeFilename();
				return true;
			} catch (IOException e) {
				DialogToolkit.showExceptionDialogAsync(wizardPage.getControl().getDisplay(),
						Messages.PAGE_EXPORT_ERROR_TITLE,
						MessageFormat.format(Messages.PAGE_EXPORT_ERROR_MESSAGE, e.getLocalizedMessage()), e);
			}
		}
		return false;
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setDialogSettings(FlightRecorderUI.getDefault().getDialogSettings());
		wizardPage = new ExportPagesWizardPage();
		setWindowTitle(wizardPage.getTitle());
		addPage(wizardPage);
	}

	@Override
	public boolean canFinish() {
		return wizardPage != null && wizardPage.isPageComplete();
	}

}
