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
package org.openjdk.jmc.ui.wizards;

import java.io.File;

import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import org.openjdk.jmc.ui.misc.FileSelector;

/**
 * Simple import wizard that imports all the data from a file. A more advanced import wizard could
 * let the user inspect the data in the file and select what he/she wants to import.
 */
public class SimpleImportFromFileWizardPage extends WizardPage {
	private static final String DIALOG_SETTINGS_NAME = "simple.import.settings"; //$NON-NLS-1$
	private final String m_fileExtension;

	private IDialogSettings m_importSettings;
	private FileSelector m_fsc;

	public SimpleImportFromFileWizardPage(String pageName, String fileExtension) {
		super(pageName);
		m_fileExtension = fileExtension;
	}

	/**
	 * Initialized this wizard page. That is, initializes the {@link DialogSettings}this wizard is
	 * going to use.
	 */
	private void initialize() {
		m_importSettings = getDialogSettings().getSection(DIALOG_SETTINGS_NAME);
		if (m_importSettings == null) {
			m_importSettings = getDialogSettings().addNewSection(DIALOG_SETTINGS_NAME);
		}
	}

	/**
	 * Creates the UI for this wizard page.
	 */
	@Override
	public void createControl(Composite parent) {
		initialize();
		GridLayout layout = new GridLayout();
		Composite control = new Composite(parent, SWT.NONE);
		control.setLayout(layout);
		setPageComplete(false);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		m_fsc = new FileSelector(control, m_importSettings,
				Messages.SimpleImportFromFileWizardPage_IMPORT_FROM_FILE_TEXT, true, SWT.OPEN, null, m_fileExtension);
		m_fsc.setLayoutData(gd);
		m_fsc.addPropertyChangeListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (FileSelector.PROPERTY_FILE_DOES_NOT_EXIST.equals(event.getProperty())) {
					setErrorMessage((String) event.getNewValue());
					setMessage(null);
					setPageComplete(false);
				}
				if (FileSelector.PROPERTY_DIR_DOES_NOT_EXIST.equals(event.getProperty())) {
					setErrorMessage(((String) event.getNewValue()));
					setMessage(null);
					setPageComplete(false);
				}
				if (FileSelector.PROPERTY_NO_FILE_SPECIFIED.equals(event.getProperty())) {
					setMessage(((String) event.getNewValue()));
					setErrorMessage(null);
					setPageComplete(false);
				}
				if (FileSelector.PROPERTY_VALID_FILE_NAME_ENTERED.equals(event.getProperty())) {
					setMessage(Messages.SimpleImportFromFileWizardPage_MESSAGE_CLICK_FINISH_TO_START_IMPORT);
					setErrorMessage(null);
					setPageComplete(true);
				}
			}
		});
		setControl(control);
	}

	/**
	 * Gets the file the user want to import data from, or null if a valid file has not been
	 * entered.
	 *
	 * @return a valid file or null.
	 */
	public File getFile() {
		if (m_fsc != null && !m_fsc.isDisposed()) {
			return m_fsc.getFile();
		}

		return null;
	}

	/**
	 * Store the file name to the {@link IDialogSettings} supplied in the file selection part.
	 */
	public void storeFilename() {
		m_fsc.storeFilename();
	}
}
