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

import java.io.File;
import java.text.MessageFormat;
import java.util.stream.Stream;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import org.openjdk.jmc.common.util.StateToolkit;
import org.openjdk.jmc.common.util.StatefulState;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.misc.DialogToolkit;
import org.openjdk.jmc.ui.wizards.SimpleImportFromFileWizardPage;

// FIXME: Choose an appropriate icon for this wizard
public class PagesImportWizard extends Wizard implements IImportWizard {
	private static String WIZARD_KEY = "importFlightRecordingPagesFromFile"; //$NON-NLS-1$

	private SimpleImportFromFileWizardPage wizardPage;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setDialogSettings(FlightRecorderUI.getDefault().getDialogSettings());
		wizardPage = new SimpleImportFromFileWizardPage(WIZARD_KEY, PagesExportWizard.FILE_ENDING);
		wizardPage.setTitle(Messages.PAGE_IMPORT_WIZARD_TITLE);
		setWindowTitle(wizardPage.getTitle());
		addPage(wizardPage);
	}

	@Override
	public boolean performFinish() {
		File file = wizardPage.getFile();
		if (file != null) {
			try {
				StatefulState persistedState = StatefulState
						.create(StateToolkit.statefulFromXMLFile(file, PagesExportWizard.CHARSET));
				FlightRecorderUI.getDefault().getPageManager().insertPages(Stream.of(persistedState.getChildren()));
				return true;
			} catch (RuntimeException e) {
				// thrown by statefulFromXMLFile when reading from file fails
				DialogToolkit.showExceptionDialogAsync(wizardPage.getControl().getDisplay(),
						Messages.PAGE_IMPORT_ERROR_TITLE,
						MessageFormat.format(Messages.PAGE_IMPORT_ERROR_MESSAGE, e.getLocalizedMessage()), e);
			}
		}
		return false;
	}

}
