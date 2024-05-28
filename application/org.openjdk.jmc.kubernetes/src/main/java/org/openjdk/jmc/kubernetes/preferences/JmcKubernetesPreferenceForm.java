/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2024, Kantega AS. All rights reserved.
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
package org.openjdk.jmc.kubernetes.preferences;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.openjdk.jmc.common.security.CredentialsNotAvailableException;
import org.openjdk.jmc.common.security.SecurityException;
import org.openjdk.jmc.kubernetes.JmcKubernetesPlugin;

/**
 * This class represents a preference page that is contributed to the Preferences dialog. By
 * subclassing <samp>FieldEditorPreferencePage</samp>, we can use the field support built into JFace
 * that allows us to create a page that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the preference store that
 * belongs to the main plug-in class. That way, preferences can be accessed directly via the
 * preference store.
 */
public class JmcKubernetesPreferenceForm extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage, PreferenceConstants {

	private Map<Control, Object> dependantControls = new WeakHashMap<>();
	private Text userField;
	private Text passwordField;
	private boolean credentialsDirty;

	public JmcKubernetesPreferenceForm() {
		super(GRID);
		setPreferenceStore(JmcKubernetesPlugin.getDefault().getPreferenceStore());
		setDescription(Messages.JmcKubernetesPreferenceForm_FormDescription);
	}

	/**
	 * Creates the field editors. Field editors are abstractions of the common GUI blocks needed to
	 * manipulate various types of preferences. Each field editor knows how to save and restore
	 * itself.
	 */
	public void createFieldEditors() {
		BooleanFieldEditor mainEnabler = new BooleanFieldEditor(P_SCAN_FOR_INSTANCES,
				Messages.JmcKubernetesPreferenceForm_ScanForPods, getFieldEditorParent()) {
			@Override
			protected void valueChanged(boolean oldValue, boolean newValue) {
				super.valueChanged(oldValue, newValue);
				enableDependantFields(newValue);
			}
		};
		addField(mainEnabler);

		final BooleanFieldEditor scanContextsEditor = new BooleanFieldEditor(P_SCAN_ALL_CONTEXTS,
				Messages.JmcKubernetesPreferenceForm_AllContexts, getFieldEditorParent());
		this.addDependantField(scanContextsEditor, scanContextsEditor.getDescriptionControl(getFieldEditorParent()));
		final BooleanFieldEditor logErrors = new BooleanFieldEditor(P_LOG_ERRORS,
				Messages.JmcKubernetesPreferenceForm_LogErrorsLabel, getFieldEditorParent());
		this.addDependantField(logErrors, logErrors.getDescriptionControl(getFieldEditorParent()));
		this.addTextField(new StringFieldEditor(P_REQUIRE_LABEL, Messages.JmcKubernetesPreferenceForm_RequireLabel,
				getFieldEditorParent()), Messages.JmcKubernetesPreferenceForm_LabelToolTip);
		this.addTextField(new StringFieldEditor(P_JOLOKIA_PATH, Messages.JmcKubernetesPreferenceForm_PathLabel,
				getFieldEditorParent()), Messages.JmcKubernetesPreferenceForm_PathTooltip);
		this.addTextField(new StringFieldEditor(P_JOLOKIA_PORT, Messages.JmcKubernetesPreferenceForm_PortLabel,
				getFieldEditorParent()), Messages.JmcKubernetesPreferenceForm_PortTooltip);
		this.addTextField(new StringFieldEditor(P_JOLOKIA_PROTOCOL, Messages.JmcKubernetesPreferenceForm_ProtocolLabel,
				getFieldEditorParent()), Messages.JmcKubernetesPreferenceForm_ProtocolTooltip);
		createCredentialFields();
		// set initial enablement
		enableDependantFields(JmcKubernetesPlugin.getDefault().scanForInstances());

	}

	private void createCredentialFields() {
		Label userLabel = new Label(getFieldEditorParent(), SWT.NONE);
		userLabel.setText(Messages.JmcKubernetesPreferenceForm_UsernameLabel);
		userLabel.setLayoutData(new GridData());
		this.userField = new Text(getFieldEditorParent(), SWT.SINGLE | SWT.BORDER);
		userField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		userField.setToolTipText(Messages.JmcKubernetesPreferenceForm_UsernameTooltip);
		this.dependantControls.put(userField, null);

		Label passLabel = new Label(getFieldEditorParent(), SWT.NONE);
		passLabel.setText(Messages.JmcKubernetesPreferenceForm_PasswordLabel);
		passLabel.setLayoutData(new GridData());
		this.passwordField = new Text(getFieldEditorParent(), SWT.PASSWORD | SWT.SINGLE | SWT.BORDER);
		passwordField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		this.dependantControls.put(passwordField, null);

		JmcKubernetesPlugin plugin = JmcKubernetesPlugin.getDefault();

		try {
			userField.setText(plugin.username());
			passwordField.setText(plugin.password());
		} catch (CredentialsNotAvailableException ignore) {
			// ignore if credentials are not stored
		} catch (SecurityException e) {
			plugin.getLogger().log(Level.WARNING, "Could not load kubernetes credentials", e); //$NON-NLS-1$
		}

		ModifyListener markCredentials = e -> credentialsDirty = true;
		this.userField.addModifyListener(markCredentials);
		this.passwordField.addModifyListener(markCredentials);
	}

	private void addTextField(StringFieldEditor field, String tooltip) {
		Text textControl = field.getTextControl(getFieldEditorParent());
		this.addDependantField(field, textControl);
		textControl.setToolTipText(tooltip);
		field.getLabelControl(getFieldEditorParent()).setToolTipText(tooltip);

	}

	private void addDependantField(FieldEditor field, Control control) {
		this.dependantControls.put(control, null);
		addField(field);
	}

	private void enableDependantFields(boolean enabled) {
		for (Control field : this.dependantControls.keySet()) {
			field.setEnabled(enabled);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

	@Override
	public boolean performOk() {
		updateCredentialsIfApplicable();
		return super.performOk();
	}

	private void updateCredentialsIfApplicable() {
		if (this.credentialsDirty) {
			try {
				JmcKubernetesPlugin.getDefault().storeCredentials(userField.getText(), passwordField.getText());
				this.credentialsDirty = false;
			} catch (SecurityException ex) {
				JmcKubernetesPlugin.getDefault().getLogger().log(Level.WARNING,
						"Could not store kubernetes credentials", ex); //$NON-NLS-1$
			}
		}
	}

}
