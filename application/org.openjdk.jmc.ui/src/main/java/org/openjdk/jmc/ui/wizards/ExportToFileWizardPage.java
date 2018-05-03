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
import java.text.MessageFormat;

import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.openjdk.jmc.ui.misc.DialogToolkit;
import org.openjdk.jmc.ui.misc.FileSelector;

/**
 * Preference page for exporting items to a file.
 */
public abstract class ExportToFileWizardPage extends WizardPage {
	private static final String DIALOG_SETTINGS_NAME = "export.to.file.wizard.settings"; //$NON-NLS-1$;
	private static final String OVERWRITE_OK = "overwrite.earning.check"; //$NON-NLS-1$
	private final String m_fileExtension;
	private final String m_defaultFileName;

	private IDialogSettings m_exportSettings;
	private FileSelector m_fileSelector;

	/**
	 * Create an export to file wizard page.
	 *
	 * @param pageName
	 *            the name of the wizard page
	 * @param fileExtension
	 *            the file extension to use as default when exporting the items.
	 * @param defaultFileName
	 *            the default file name.
	 */
	public ExportToFileWizardPage(String pageName, String fileExtension, String defaultFileName) {
		super(pageName);
		m_fileExtension = fileExtension;
		m_defaultFileName = defaultFileName;
	}

	/**
	 * Create an export to file wizard page.
	 *
	 * @param pageName
	 *            the name of the wizard page
	 * @param fileExtension
	 *            the file extension to use as default when exporting the items.
	 */
	public ExportToFileWizardPage(String pageName, String fileExtension) {
		this(pageName, fileExtension, null);
	}

	/**
	 * Check if is is OK to export the file. The Result depends on if overwrite warning is enabled
	 * and if the user has selected a file name could be valid(no guarantees).
	 *
	 * @return true if the file can be exported.
	 */
	public boolean isExportToFileOk() {
		if (!isOverwriteOK() && getFile() != null && getFile().exists()) {
			return DialogToolkit.openQuestionOnUiThread(Messages.ExportToFileWizardPage_OVERWRITE_QUESTION_TITLE,
					MessageFormat.format(Messages.ExportToFileWizardPage_OVERWRITE_QUESTION_TEXT,
							getFile().getAbsolutePath()));
		} else {
			return true;
		}
	}

	/**
	 * Create the user interface for this wizard page
	 */
	@Override
	public void createControl(Composite parent) {
		m_exportSettings = DialogSettings.getOrCreateSection(getDialogSettings(), DIALOG_SETTINGS_NAME);
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());

		Composite contents = createContents(container);
		contents.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Control fileSelector = createFileSelector(container);
		fileSelector.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		setControl(container);
	}

	protected abstract Composite createContents(Composite parent);

	/**
	 * Create the bottom container
	 */

	private Control createFileSelector(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(GridLayoutFactory.fillDefaults().create());

		GridData fileSectionGridData = new GridData(SWT.FILL, SWT.FILL, true, false);
		m_fileSelector = new FileSelector(container, m_exportSettings, Messages.ExportToFileWizardPage_EXPORT_TO_FILE,
				false, SWT.SAVE, m_defaultFileName, m_fileExtension);
		m_fileSelector.setLayoutData(fileSectionGridData);
		m_fileSelector.addPropertyChangeListener(new IPropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (FileSelector.PROPERTY_NO_FILE_SPECIFIED.equals(event.getProperty())) {
					setMessage(((String) event.getNewValue()));
					setErrorMessage(null);
					setPageComplete(false);
				}
				if (FileSelector.PROPERTY_DIR_DOES_NOT_EXIST.equals(event.getProperty())) {
					setErrorMessage(((String) event.getNewValue()));
					setMessage(null);
					setPageComplete(false);
				}
				if (FileSelector.PROPERTY_VALID_FILE_NAME_ENTERED.equals(event.getProperty())) {
					setMessage(Messages.ExportToFileWizardPage_CLICK_FINISH_MESSAGE_TEXT);
					setErrorMessage(null);
					updatePageComplete();
				}
			}
		});

		GridData overWriteCheckGridData = new GridData(SWT.FILL, SWT.FILL, true, false);
		Control overWriteontrol = createOvewriteWarningCheckbox(container);
		overWriteontrol.setLayoutData(overWriteCheckGridData);

		return container;
	}

	protected void updatePageComplete() {
		setPageComplete(isPageComplete());
	}

	/**
	 * Creates the overwrite CheckBox
	 *
	 * @param container
	 *            the parent container
	 * @return
	 */
	private Control createOvewriteWarningCheckbox(Composite container) {
		final Button button = new Button(container, SWT.CHECK);
		button.setText(Messages.ExportToFileWizardPage_WARN_IF_OVERWRITE_TEXT);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!button.isDisposed()) {
					setOverwriteOK(!button.getSelection());
				}
			}
		});
		button.setSelection(!m_exportSettings.getBoolean(OVERWRITE_OK));
		return button;
	}

	private boolean isOverwriteOK() {
		return m_exportSettings.getBoolean(OVERWRITE_OK);
	}

	private void setOverwriteOK(boolean overwrite) {
		m_exportSettings.put(OVERWRITE_OK, overwrite);
	}

	@Override
	public boolean isPageComplete() {
		if (getFile() != null) {
			if (!isSelectionValid()) {
				setMessage(Messages.ExportToFileWizardPage_SELECT_ITEMS);
				return false;
			} else {
				setMessage(Messages.ExportToFileWizardPage_CLICK_FINISH_MESSAGE_TEXT);
				return true;
			}
		}
		return false;
	}

	protected abstract boolean isSelectionValid();

	/**
	 * Return a file object for the filename the user has entered, or null if not a valid file names
	 * has been entered. If the file extension is missing it is added
	 *
	 * @return
	 */
	public File getFile() {
		if (m_fileSelector != null && !m_fileSelector.isDisposed()) {
			return m_fileSelector.getFile();
		}
		return null;
	}

	/**
	 * Store the file name to the {@link IDialogSettings} supplied in the file selection part.
	 */
	public void storeFilename() {
		m_fileSelector.storeFilename();
	}
}
