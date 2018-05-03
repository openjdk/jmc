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

import java.io.File;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import org.openjdk.jmc.console.ui.messages.internal.Messages;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.preferences.PreferencesKeys;
import org.openjdk.jmc.ui.misc.IntFieldEditor;

/**
 * Preference dialog responsible for persistence settings
 */
public class PersistencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	public PersistencePage() {
		super(GRID);
		setPreferenceStore(new ScopedPreferenceStore(InstanceScope.INSTANCE, RJMXPlugin.PLUGIN_ID));
		setDescription(Messages.PersistencePage_DESCRIPTION);
	}

	@Override
	protected void createFieldEditors() {
		createPersistenceDirectory();
		createLogRotationLimit();
		createSpace();
	}

	private void createSpace() {
		GridData gd = new GridData();
		gd.heightHint = 12;
		Composite c = new Composite(getFieldEditorParent(), SWT.NONE);
		c.setLayoutData(gd);
	}

	private void createLogRotationLimit() {
		IntFieldEditor logRotationLimit = new IntFieldEditor(PreferencesKeys.PROPERTY_PERSISTENCE_LOG_ROTATION_LIMIT_KB,
				Messages.PersistencePage_CAPTION_LOG_ROTATION_LIMIT_KB, getFieldEditorParent());
		addField(logRotationLimit);
		logRotationLimit.setValidRange(1, Integer.MAX_VALUE);
	}

	private void createPersistenceDirectory() {
		DirectoryFieldEditor persistenceDirectory = new DirectoryFieldEditor(
				PreferencesKeys.PROPERTY_PERSISTENCE_DIRECTORY, Messages.PersistencePage_CAPTION_PERSISTENCE_DIRECTORY,
				getFieldEditorParent()) {
			@Override
			protected boolean doCheckState() {
				String fileName = getTextControl().getText();
				fileName = fileName.trim();
				File file = new File(fileName);
				while (file != null) {
					if (file.isDirectory()) {
						return true;
					} else if (file.exists()) {
						return false;
					} else {
						file = file.getParentFile();
					}
				}
				return false;
			}
		};
		persistenceDirectory.setErrorMessage(Messages.PersistencePage_ERROR_DIRECTORY_MUST_EXIST_OR_BE_CREATABLE);
		addField(persistenceDirectory);
		persistenceDirectory.setEmptyStringAllowed(true);
	}

	@Override
	public void init(IWorkbench workbench) {
	}
}
