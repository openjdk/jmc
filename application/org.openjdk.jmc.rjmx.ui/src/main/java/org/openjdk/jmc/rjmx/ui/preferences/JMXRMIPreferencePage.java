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
package org.openjdk.jmc.rjmx.ui.preferences;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import org.openjdk.jmc.rjmx.preferences.JMXRMIPreferences;
import org.openjdk.jmc.ui.common.security.SecurityManagerFactory;
import org.openjdk.jmc.ui.misc.DialogToolkit;
import org.openjdk.jmc.ui.misc.PasswordFieldEditor;

public class JMXRMIPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private Label clearLabel;
	private boolean pageUnlocked;
	private volatile boolean doClearPrefs;
	private final PreferenceStore store = new PreferenceStore() {
		@Override
		public void save() throws IOException {
			try {
				if (allPrefsEmpty() || doClearPrefs) {
					JMXRMIPreferences.getInstance().remove();
				} else {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					save(baos, null);
					JMXRMIPreferences.getInstance().set(baos.toByteArray());
				}
			} catch (Exception e) {
				throw new IOException(e.getMessage(), e);
			}
		};

		@Override
		public boolean needsSaving() {
			return doClearPrefs || super.needsSaving();
		};

		private boolean allPrefsEmpty() {
			for (String key : preferenceNames()) {
				String val = getString(key);
				if (val != null && !val.isEmpty()) {
					return false;
				}
			}
			return true;
		}
	};
	private final List<FieldEditor> fields = new ArrayList<>(4);

	public JMXRMIPreferencePage() {
		super(GRID);
		setPreferenceStore(store);
		setDescription(Messages.JMXRMIPreferencePage_DESCRIPTION);
	}

	@Override
	public void init(IWorkbench workbench) {
		// Don't care
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		if (!pageUnlocked && JMXRMIPreferences.getInstance().exists()) {
			clearLabel.setVisible(true);
			getFieldEditorParent().layout();
			doClearPrefs = true;
		}
	}

	@Override
	protected void createFieldEditors() {
		pageUnlocked = !SecurityManagerFactory.getSecurityManager().isLocked();
		if (pageUnlocked) {
			unlockAndLoadPage();
		}
		doAddFieldEditor(new FileFieldEditor(JMXRMIPreferences.PROPERTY_KEY_KEYSTORE,
				Messages.JMXRMIPreferencePage_CAPTION_KEY_STORE, getFieldEditorParent()));
		doAddFieldEditor(new PasswordFieldEditor(JMXRMIPreferences.PROPERTY_KEY_KEYSTORE_PASSWORD,
				Messages.JMXRMIPreferencePage_CAPTION_KEY_STORE_PASSWORD, getFieldEditorParent()));
		doAddFieldEditor(new FileFieldEditor(JMXRMIPreferences.PROPERTY_KEY_TRUSTSTORE,
				Messages.JMXRMIPreferencePage_CAPTION_TRUST_STORE, getFieldEditorParent()));
		doAddFieldEditor(new PasswordFieldEditor(JMXRMIPreferences.PROPERTY_KEY_TRUSTSTORE_PASSWORD,
				Messages.JMXRMIPreferencePage_CAPTION_TRUST_STORE_PASSWORD, getFieldEditorParent()));
		final Button unlockButton = new Button(getFieldEditorParent(), SWT.NONE);
		unlockButton.setText(Messages.JMXRMIPreferencePage_UNLOCK_BUTTON_TEXT);
		unlockButton.setEnabled(!pageUnlocked);

		unlockButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (unlockAndLoadPage()) {
					for (FieldEditor fe : fields) {
						fe.setEnabled(true, getFieldEditorParent());
					}
					initialize();
					unlockButton.setEnabled(false);
					clearLabel.setVisible(false);
					doClearPrefs = false;
				}
			}
		});
		clearLabel = new Label(getFieldEditorParent(), SWT.NONE);
		clearLabel.setText(Messages.JMXRMIPreferencePage_CLEAR_SETTINGS_WARNING);
		clearLabel.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
		clearLabel.setForeground(JFaceColors.getErrorText(clearLabel.getDisplay()));
		clearLabel.setVisible(false);
	}

	@Override
	public boolean performCancel() {
		doClearPrefs = false;
		return super.performCancel();
	}

	private void doAddFieldEditor(FieldEditor field) {
		field.setEnabled(pageUnlocked, getFieldEditorParent());
		fields.add(field);
		addField(field);
	}

	private boolean unlockAndLoadPage() {
		try {
			SecurityManagerFactory.getSecurityManager().unlock();
			byte[] value = JMXRMIPreferences.getInstance().get();
			if (value != null) {
				store.load(new ByteArrayInputStream(value));
			}
			pageUnlocked = true;
			return true;
		} catch (Exception e) {
			DialogToolkit.showException(getFieldEditorParent().getShell(),
					Messages.JMXRMIPreferencePage_LOAD_CONTENT_FAILED_TITLE,
					Messages.JMXRMIPreferencePage_LOAD_CONTENT_FAILED_TEXT, e);
		}
		return false;
	}
}
