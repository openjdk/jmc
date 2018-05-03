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
package org.openjdk.jmc.ui.misc;

import java.io.File;
import java.text.MessageFormat;
import java.util.Locale;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Composite that can select a file. Consists of a
 * <ul>
 * <li>a label describing what kind of file it is.
 * <li>a text input where the user can enter the filename.
 * <li>a browse button where the user can select a file from a file dialog
 * </ul>
 * </p>
 * <p>
 * Users may attach a {@link IPropertyChangeListener} to listen to state changes. See
 * {@link FileSelector#PROPERTY_NO_FILE_SPECIFIED} {@link FileSelector#PROPERTY_FILE_DOES_NOT_EXIST}
 * {@link FileSelector#PROPERTY_VALID_FILE_NAME_ENTERED}
 * {@link FileSelector#PROPERTY_DIR_DOES_NOT_EXIST}
 * </p>
 */
public class FileSelector extends Composite {
	public final static String PROPERTY_NO_FILE_SPECIFIED = "no.file.specified"; //$NON-NLS-1$
	public final static String PROPERTY_FILE_DOES_NOT_EXIST = "file.does.not.exist"; //$NON-NLS-1$
	public final static String PROPERTY_DIR_DOES_NOT_EXIST = "directory.does.not.exist"; //$NON-NLS-1$
	public final static String PROPERTY_VALID_FILE_NAME_ENTERED = "valid.file.name.specified"; //$NON-NLS-1$

	public final static String LAST_FILE_NAME = "last.file.name"; //$NON-NLS-1$
	public final static String FILENAME_FIELD_NAME = "wizards.export.connections.text.filename"; //$NON-NLS-1$

	private final IDialogSettings m_settings;
	private final ListenerList<IPropertyChangeListener> m_listeners = new ListenerList<>();
	private final boolean m_mustExist;
	private final String m_labelText;

	private Text m_filename;
	private final String m_fileExtension;
	private File m_file;
	private final int style;

	/**
	 * Constructs a new {@link FileSelector}
	 *
	 * @param parent
	 *            the parent composite
	 * @param settings
	 *            {@link DialogSettings} to store the last filename, or null if not to be used
	 * @param label
	 *            the label to show before the text field
	 * @param mustExistToBeValid
	 *            flag indicating if the file must exist for the file to be considered valid
	 * @param style
	 *            the style of the composite, SWT.SAVE for File save dialog and SWT.OPEN for a File
	 *            open dialog
	 * @param defaultFileName
	 *            the default file name (or null if no default file name is available)
	 * @param fileExt
	 *            the file extension, e.g. "xml".
	 */
	public FileSelector(Composite parent, IDialogSettings settings, String label, boolean mustExistToBeValid, int style,
			String defaultFileName, String fileExt) {
		super(parent, style);
		this.style = style;
		m_fileExtension = fileExt == null ? null : fileExt.startsWith(".") ? fileExt : '.' + fileExt; //$NON-NLS-1$
		m_labelText = label;
		m_settings = settings;
		m_mustExist = mustExistToBeValid;
		createFileSelection(this);
		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				m_listeners.clear();
			}
		});
		String storedFileName = getStoredFileName();
		if (defaultFileName == null) {
			m_filename.setText(storedFileName == null ? "" : storedFileName); //$NON-NLS-1$
		} else {
			File pf = storedFileName == null ? new File(".") : new File(storedFileName).getParentFile(); //$NON-NLS-1$
			int i = 0;
			File suggestion = new File(pf, defaultFileName + i + m_fileExtension);
			while (suggestion.exists()) {
				suggestion = new File(pf, defaultFileName + (++i) + m_fileExtension);
			}
			m_filename.setText(suggestion.getAbsolutePath());
		}
	}

	/**
	 * Updates the file and notifies any listener of the current state.
	 */
	private void updateFileName() {
		m_file = null;
		String fileText = m_filename.getText();
		if (fileText == null || fileText.trim().length() == 0) {
			fireEvent(PROPERTY_NO_FILE_SPECIFIED, Messages.FileSelectorComposite_PLEASE_SPECIFY_FILE);
		} else {
			File file = new File(fileText);
			if (!file.isAbsolute()) {
				fireEvent(PROPERTY_DIR_DOES_NOT_EXIST, Messages.FileSelectorComposite_DIR_DOES_NOT_EXIST);
			} else if (file.isDirectory()) {
				fireEvent(PROPERTY_NO_FILE_SPECIFIED, Messages.FileSelectorComposite_PLEASE_SPECIFY_FILE);
			} else if (fileText.endsWith("/") || fileText.endsWith("\\") || !file.getParentFile().isDirectory()) { //$NON-NLS-1$ //$NON-NLS-2$
				fireEvent(PROPERTY_DIR_DOES_NOT_EXIST, Messages.FileSelectorComposite_DIR_DOES_NOT_EXIST);
			} else {
				if (m_fileExtension != null && !fileText.toLowerCase(Locale.ENGLISH)
						.endsWith(m_fileExtension.toLowerCase(Locale.ENGLISH))) {
					file = new File(fileText + m_fileExtension);
				}
				if (m_mustExist && !file.exists()) {
					String msg = MessageFormat.format(Messages.FILE_DOES_NOT_EXIST, new Object[] {fileText});
					fireEvent(PROPERTY_FILE_DOES_NOT_EXIST, msg);
				} else {
					m_file = file;
					fireEvent(PROPERTY_VALID_FILE_NAME_ENTERED, null);
				}
			}
		}
	}

	/**
	 * Add a property change listener that gives the status of the file current selection. See
	 * {@link FileSelector#PROPERTY_FILE_DOES_NOT_EXIST},
	 * {@link FileSelector#PROPERTY_FILE_DOES_NOT_EXIST}
	 * {@link FileSelector#PROPERTY_DIR_DOES_NOT_EXIST} and
	 * {@link FileSelector#PROPERTY_VALID_FILE_NAME_ENTERED} for properties.
	 *
	 * @param listener
	 */
	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		m_listeners.add(listener);
		updateFileName();
	}

	/**
	 * Removes any {@link IPropertyChangeListener} any added listeners.
	 *
	 * @param listener
	 */
	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		m_listeners.remove(listener);
	}

	/**
	 * Notifies any {@link IPropertyChangeListener} listening on this component there has been a
	 * change.
	 *
	 * @param property
	 *            the property
	 * @param message
	 *            the message for the property
	 */
	private void fireEvent(String property, String message) {
		for (IPropertyChangeListener p : m_listeners) {
			p.propertyChange(new PropertyChangeEvent(this, property, null, message));
		}
	}

	/**
	 * Gets the file name from the {@link IDialogSettings} supplied in the constructor.
	 *
	 * @return the stored file name
	 */
	private String getStoredFileName() {
		return m_settings != null ? m_settings.get(LAST_FILE_NAME) : null;
	}

	/**
	 * Create the composite for selecting a file
	 *
	 * @param container
	 *            the parent container
	 * @return a file selection composite
	 */
	protected Composite createFileSelection(Composite container) {
		GridLayout layout = GridLayoutFactory.fillDefaults().create();
		layout.numColumns = 3;
		container.setLayout(layout);

		// Create the label
		GridData gridDataLabel = new GridData(SWT.FILL, SWT.CENTER, false, false);
		Label l = createLabel(container, m_labelText);
		l.setLayoutData(gridDataLabel);

		// Create the text input
		GridData gridDataFileName = new GridData(SWT.FILL, SWT.CENTER, true, false);
		m_filename = createTextInput(container);
		m_filename.setLayoutData(gridDataFileName);
		m_filename.setData("name", FILENAME_FIELD_NAME); //$NON-NLS-1$

		// Create the browse button
		GridData gridDataFileSelectionButton = new GridData(SWT.FILL, SWT.CENTER, false, false);
		Button fileSectionButton = createBrowseButton(container);
		fileSectionButton.setLayoutData(gridDataFileSelectionButton);

		return container;
	}

	/**
	 * Create the text widget to input text from
	 *
	 * @param container
	 *            the parent container
	 * @return the text widget
	 */
	private Text createTextInput(Composite container) {
		Text text = new Text(container, SWT.BORDER);
		text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				updateFileName();
			}
		});
		return text;
	}

	/**
	 * Create the label for the text widget
	 *
	 * @param container
	 *            the parent container
	 * @param text
	 *            the text this label should have
	 * @return
	 */
	private Label createLabel(Composite container, String text) {
		Label label = new Label(container, SWT.NONE);
		label.setText(text);
		return label;
	}

	/**
	 * Create the browse button this widget should have
	 *
	 * @param container
	 *            the parent container
	 */
	private Button createBrowseButton(Composite container) {
		Button fileSectionButton = new Button(container, SWT.NONE);
		fileSectionButton.setText(Messages.FileSelectorComposite_FILE_SELECTOR_BROWSE_BUTTON_TEXT);
		fileSectionButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!m_filename.isDisposed()) {
					openSelectFileDialog();
				}
			}
		});
		return fileSectionButton;
	}

	private void openSelectFileDialog() {
		FileDialog dialog = new FileDialog(getShell(), style);
		if (m_fileExtension != null) {
			dialog.setFilterExtensions(new String[] {"*" + m_fileExtension}); //$NON-NLS-1$
		}

		if (m_filename.getText() != null && m_filename.getText().trim().length() > 0) {
			File f = new File(m_filename.getText());
			if (f.isAbsolute()) {
				if (f.isDirectory()) {
					dialog.setFilterPath(f.getAbsolutePath());
				} else if (f.getParentFile().isDirectory()) {
					dialog.setFilterPath(f.getParentFile().getAbsolutePath());
				}
			}
		}
		String filename = dialog.open();
		if (filename != null) {
			m_filename.setText(filename);
			m_filename.setSelection(m_filename.getText().length());
			updateFileName();
		}
	}

	/**
	 * Returns the selected file, or null if not available or invalid.
	 *
	 * @return the selected file, or null if N/A.
	 */
	public File getFile() {
		return m_file;
	}

	/**
	 * Store the file name to the {@link IDialogSettings} supplied in the constructor.
	 */
	public void storeFilename() {
		if (m_settings != null && m_file != null) {
			m_settings.put(LAST_FILE_NAME, m_file.getAbsolutePath());
		}
	}
}
