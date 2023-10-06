/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.ui.preferences;

import java.util.logging.Level;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.openjdk.jmc.common.security.ActionNotGrantedException;
import org.openjdk.jmc.common.security.SecurityException;
import org.openjdk.jmc.common.security.SecurityManagerFactory;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.misc.TrayManager;

public class MissionControlPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private Combo cipherCombo;

	public MissionControlPage() {
		super(FLAT);
		setPreferenceStore(UIPlugin.getDefault().getPreferenceStore());
		setDescription(Messages.MissionControlPage_DESCRIPTION);
	}

	/**
	 * Creates the field editors. Field editors are abstractions of the common GUI blocks needed to
	 * manipulate various types of preferences. Each field editor knows how to save and restore
	 * itself.
	 */
	@Override
	public void createFieldEditors() {
		addField(new BooleanFieldEditor(PreferenceConstants.P_ANTI_ALIASING,
				Messages.MissionControlPage_CAPTION_USE_ANTI_ALIASING, getFieldEditorParent()));

		TrayManager tm = UIPlugin.getDefault().getTrayManager();
		if (tm != null && tm.isTraySupported()) {
			addField(new BooleanFieldEditor(PreferenceConstants.P_MINIMIZE_TO_TRAY_ON_CLOSE,
					Messages.MissionControlPage_MINIMIZE_TO_TRAY_ICON_TEXT, getFieldEditorParent()));
		}
		addField(new BooleanFieldEditor(PreferenceConstants.P_ENABLE_BACKROUND_RENDERING,
				Messages.MissionControlPage_ENABLE_THREADED_RENDERING_TEXT, getFieldEditorParent()));
		Composite bottomPartParent = getFieldEditorParent();
		bottomPartParent.setLayout(GridLayoutFactory.fillDefaults().create());
		createNoteControl(bottomPartParent, Messages.MissionControlPage_RENDERING_NOTE_TEXT)
				.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		createAccessibilityOptionsGroup(bottomPartParent)
				.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		createSecurityGroup(bottomPartParent).setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	}

	private Group createSecurityGroup(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setLayout(new GridLayout());
		group.setText(Messages.MissionControlPage_SECURITY_OPTIONS_GROUP_TEXT);

		Composite row1 = new Composite(group, SWT.NONE);
		GridLayout row1Layout = new GridLayout();
		row1Layout.numColumns = 2;
		row1.setLayout(row1Layout);
		Label label = new Label(row1, SWT.BOLD);
		label.setText(Messages.MissionControlPage_CIPHER_LABEL_TEXT);

		cipherCombo = new Combo(row1, SWT.READ_ONLY);
		for (String cipher : SecurityManagerFactory.getSecurityManager().getEncryptionCiphers()) {
			cipherCombo.add(cipher);
		}
		cipherCombo.setText(SecurityManagerFactory.getSecurityManager().getEncryptionCipher());

		Composite row2 = new Composite(group, SWT.NONE);
		GridLayout row2Layout = new GridLayout();
		row2Layout.numColumns = 2;
		row2.setLayout(row2Layout);

		Button changeButton = new Button(row2, SWT.NONE);
		changeButton.setText(Messages.MissionControlPage_BUTTON_CHANGE_MASTER_PASSWORD);
		changeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					SecurityManagerFactory.getSecurityManager().changeMasterPassword();
				} catch (ActionNotGrantedException e) {
					// Aborted
				} catch (SecurityException e) {
					UIPlugin.getDefault().getLogger().log(Level.SEVERE, "Could not change the master password!", e); //$NON-NLS-1$
				}
			}
		});
		return group;
	}

	private static Composite createNoteControl(Composite parent, String message) {
		Composite noteParent = new Composite(parent, SWT.NONE);
		PreferencesToolkit.fillNoteControl(noteParent, message);
		return noteParent;
	}

	/**
	 * Creates a {@link Group} holding the accessibility options.
	 *
	 * @param parent
	 *            the parent composite to hold the {@link Group}.
	 */
	private Group createAccessibilityOptionsGroup(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setLayout(new GridLayout());
		group.setText(Messages.MissionControlPage_ACCESSIBILITY_OPTIONS_GROUP_TEXT);

		addField(new BooleanFieldEditor(PreferenceConstants.P_ACCESSIBILITY_MODE,
				Messages.MissionControlPage_ACCESSIBILITY_MODE_TEXT, createIndentedComposite(group, 0, 0)));
		addField(new BooleanFieldEditor(PreferenceConstants.P_ACCESSIBILITY_BUTTONS_AS_TEXT,
				Messages.MissionControlPage_ACCESSIBILITY_BUTTONS_AS_TEXT_TEXT, createIndentedComposite(group, 0, 0)));
		createNoteControl(group, Messages.MissionControlPage_HELP_NOTE_TEXT)
				.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return group;
	}

	/**
	 * Since {@link FieldEditorPreferencePage} messes around with the layout this method is used to
	 * get some formatting on the accessibility options. It basicly wraps a {@link Composite} around
	 * the component by "inserting" it between the real parent and the field editor.
	 *
	 * @param parent
	 *            the real {@link Composite}.
	 * @param horizontalIndent
	 *            number of pixels to indent horizontal.
	 * @param verticalIndent
	 *            number of pixels to indent vertical.
	 * @return a new composite to hold the field editor.
	 */
	private Composite createIndentedComposite(Composite parent, int horizontalIndent, int verticalIndent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gridData.horizontalIndent = horizontalIndent;
		gridData.verticalIndent = verticalIndent;
		composite.setLayoutData(gridData);
		return composite;
	}

	@Override
	public void init(IWorkbench workbench) {
		// Not used
	}

	@Override
	protected void performDefaults() {
		cipherCombo.setText(SecurityManagerFactory.getSecurityManager().getEncryptionCipher());
		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		try {
			String cipher = cipherCombo.getText();
			String currentCipher = SecurityManagerFactory.getSecurityManager().getEncryptionCipher();
			if (cipher != null && cipher.trim().length() > 0 && !cipher.equals(currentCipher)) {
				SecurityManagerFactory.getSecurityManager().setEncryptionCipher(cipher);
			}
		} catch (SecurityException e) {
			return false;
		}
		return super.performOk();
	}

}
