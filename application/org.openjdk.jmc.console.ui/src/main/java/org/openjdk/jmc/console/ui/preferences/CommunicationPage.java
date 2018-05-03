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
package org.openjdk.jmc.console.ui.preferences;

import static org.openjdk.jmc.common.unit.UnitLookup.MILLISECOND;

import java.util.logging.Level;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import org.openjdk.jmc.console.ui.ConsolePlugin;
import org.openjdk.jmc.console.ui.messages.internal.Messages;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.preferences.PreferencesKeys;
import org.openjdk.jmc.ui.common.security.PersistentCredentials;
import org.openjdk.jmc.ui.common.security.SecurityException;
import org.openjdk.jmc.ui.common.security.SecurityManagerFactory;
import org.openjdk.jmc.ui.misc.IntFieldEditor;
import org.openjdk.jmc.ui.preferences.LongQuantityFieldEditor;

/**
 * Preference dialog responsible for communications settings
 */
public class CommunicationPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	private IntFieldEditor mailServerPort;
	private Text userField;
	private Text passwordField;

	/**
	 * Constructor
	 *
	 * @param SWT
	 *            style
	 */
	public CommunicationPage() {
		super(GRID);
		setPreferenceStore(new ScopedPreferenceStore(InstanceScope.INSTANCE, RJMXPlugin.PLUGIN_ID));
		setDescription(Messages.CommunicationPage_DESCRIPTION);
	}

	@Override
	public void createFieldEditors() {
		LongQuantityFieldEditor updateInterval = new LongQuantityFieldEditor(PreferencesKeys.PROPERTY_UPDATE_INTERVAL,
				Messages.CommunicationPage_CAPTION_DEFAULT_UPDATE_INTERVAL, getFieldEditorParent(), MILLISECOND);
		updateInterval.setValidRange(MILLISECOND.quantity(1), MILLISECOND.quantity(Integer.MAX_VALUE));
		addField(updateInterval);

		IntegerFieldEditor retainedEventValues = new IntegerFieldEditor(PreferencesKeys.PROPERTY_RETAINED_EVENT_VALUES,
				Messages.CommunicationPage_CAPTION_RETAINED_EVENT_VALUES, getFieldEditorParent());
		retainedEventValues.setValidRange(1, Integer.MAX_VALUE);
		addField(retainedEventValues);

		StringFieldEditor mailServer = new StringFieldEditor(PreferencesKeys.PROPERTY_MAIL_SERVER,
				Messages.CommunicationPage_CAPTION_MAIL_SERVER, getFieldEditorParent());
		addField(mailServer);

		mailServerPort = new IntFieldEditor(PreferencesKeys.PROPERTY_MAIL_SERVER_PORT,
				Messages.CommunicationPage_CAPTION_MAIL_SERVER_PORT, getFieldEditorParent());
		addField(mailServerPort);

		BooleanFieldEditor mailServerSecure = new BooleanFieldEditor(PreferencesKeys.PROPERTY_MAIL_SERVER_SECURE,
				Messages.CommunicationPage_CAPTION_SECURE_MAIL_SERVER, getFieldEditorParent());
		addField(mailServerSecure);

		createCredentialFields();
		loadCredentials();
	}

	private void createCredentialFields() {
		Label userLabel = new Label(getFieldEditorParent(), SWT.NONE);
		userLabel.setText(Messages.CommunicationPage_CAPTION_MAIL_SERVER_USER);
		userField = new Text(getFieldEditorParent(), SWT.SINGLE | SWT.BORDER);
		userField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label passLabel = new Label(getFieldEditorParent(), SWT.NONE);
		passLabel.setText(Messages.CommunicationPage_CAPTION_MAIL_SERVER_PASSWORD);
		passwordField = new Text(getFieldEditorParent(), SWT.PASSWORD | SWT.SINGLE | SWT.BORDER);
		passwordField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}

	private void loadCredentials() {
		String key = getPreferenceStore().getString(PreferencesKeys.PROPERTY_MAIL_SERVER_CREDENTIALS);
		if (key != null && !PreferencesKeys.DEFAULT_MAIL_SERVER_CREDENTIALS.equals(key)) {
			try {
				PersistentCredentials credentials = new PersistentCredentials(key);
				userField.setText(credentials.getUsername());
				passwordField.setText(credentials.getPassword());
				return;
			} catch (SecurityException e) {
				ConsolePlugin.getDefault().getLogger().log(Level.WARNING, "Could not load stored SMTP credentials!", e); //$NON-NLS-1$
			}
		}
		userField.setText(PreferencesKeys.DEFAULT_MAIL_SERVER_USER);
		passwordField.setText(PreferencesKeys.DEFAULT_MAIL_SERVER_PASSWORD);
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	public boolean performOk() {
		super.performOk();
		storeCredentials();
		return true;
	}

	private void storeCredentials() {
		String oldCredentialsKey = getPreferenceStore().getString(PreferencesKeys.PROPERTY_MAIL_SERVER_CREDENTIALS);
		if (userField.getText().equals(PreferencesKeys.DEFAULT_MAIL_SERVER_USER)
				&& passwordField.getText().equals(PreferencesKeys.DEFAULT_MAIL_SERVER_PASSWORD)) {
			getPreferenceStore().setValue(PreferencesKeys.PROPERTY_MAIL_SERVER_CREDENTIALS,
					PreferencesKeys.DEFAULT_MAIL_SERVER_CREDENTIALS);
			pruneOldCredentials(oldCredentialsKey);
			return;
		}
		try {
			PersistentCredentials credentials = new PersistentCredentials(userField.getText(), passwordField.getText());
			getPreferenceStore().setValue(PreferencesKeys.PROPERTY_MAIL_SERVER_CREDENTIALS,
					credentials.getExportedId());
			pruneOldCredentials(oldCredentialsKey);
		} catch (SecurityException e) {
			ConsolePlugin.getDefault().getLogger().log(Level.WARNING, "Could not store SMTP credentials!", e); //$NON-NLS-1$
		}
	}

	private void pruneOldCredentials(String oldCredentialsKey) {
		pruneSecurityManager(oldCredentialsKey, PreferencesKeys.DEFAULT_MAIL_SERVER_CREDENTIALS,
				"mail server credentials"); //$NON-NLS-1$
	}

	private void pruneSecurityManager(String oldKey, String defaultKey, String description) {
		if (oldKey != null && !oldKey.equals(defaultKey)) {
			try {
				SecurityManagerFactory.getSecurityManager().withdraw(oldKey);
			} catch (SecurityException e) {
				ConsolePlugin.getDefault().getLogger().log(Level.WARNING, "Could not remove old " + description + '!', //$NON-NLS-1$
						e);
			}
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(FieldEditor.VALUE)) {
			Object src = event.getSource();
			if (src instanceof FieldEditor) {
				FieldEditor editor = (FieldEditor) src;
				if (PreferencesKeys.PROPERTY_MAIL_SERVER_SECURE.equals(editor.getPreferenceName())) {
					boolean secure = ((BooleanFieldEditor) editor).getBooleanValue();
					int port = mailServerPort.getIntValue();
					if (secure && port == 25) {
						mailServerPort.setStringValue(String.valueOf(465));
					} else if (port == 465) {
						mailServerPort.setStringValue(String.valueOf(25));
					}
					return;
				}
			}
		}
		super.propertyChange(event);
	}
}
