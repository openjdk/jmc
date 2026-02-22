/*
 * Copyright (c) 2026 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026 IBM Corporation. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.ui.websocket.preferences;

import org.openjdk.jmc.ui.websocket.WebsocketPlugin;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class WebsocketPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private IntegerFieldEditor portField;

	public WebsocketPreferencePage() {
		super(GRID);
		setPreferenceStore(WebsocketPlugin.getDefault().getPreferenceStore());
		setDescription(Messages.WebsocketPreferencePage_DESCRIPTION);
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(PreferenceConstants.P_SERVER_ENABLED, Messages.WebsocketPreferencePage_ENABLE,
				getFieldEditorParent()));
		portField = new IntegerFieldEditor(PreferenceConstants.P_SERVER_PORT, Messages.WebsocketPreferencePage_PORT,
				getFieldEditorParent());
		addField(portField);
		enableWebsocketFields(isWebsocketPluginEnabled());
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(FieldEditor.VALUE)) {
			FieldEditor editor = (FieldEditor) event.getSource();
			if (PreferenceConstants.P_SERVER_ENABLED.equals(editor.getPreferenceName())) {
				enableWebsocketFields((boolean) event.getNewValue());
			}
		}
	}

	private boolean isWebsocketPluginEnabled() {
		return WebsocketPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.P_SERVER_ENABLED);
	}

	private void enableWebsocketFields(boolean enable) {
		portField.setEnabled(enable, getFieldEditorParent());
	}
}
