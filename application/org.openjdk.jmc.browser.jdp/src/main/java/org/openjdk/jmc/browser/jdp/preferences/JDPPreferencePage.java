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
package org.openjdk.jmc.browser.jdp.preferences;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import org.openjdk.jmc.browser.jdp.JDPPlugin;

public class JDPPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private StringFieldEditor jdpAddress;
	private IntegerFieldEditor jdpPort;
	private IntegerFieldEditor heartBeatTimeout;

	public JDPPreferencePage() {
		super(GRID);
		setPreferenceStore(JDPPlugin.getDefault().getPreferenceStore());
		setDescription(Messages.JDPPreferencePage_JDP_PREFERENCES_DESCRIPTION);
	}

	@Override
	public void createFieldEditors() {
		addField(new BooleanFieldEditor(PreferenceConstants.PROPERTY_KEY_JDP_AUTO_DISCOVERY,
				Messages.JDPPreferencePage_JDP_PREFERENCES_ENABLE_AUTO_DISCOVERY, getFieldEditorParent()));
		jdpAddress = new StringFieldEditor(PreferenceConstants.PROPERTY_KEY_JDP_ADDRESS,
				Messages.JDPPreferencePage_CAPTION_MULTICAST_ADDRESS, getFieldEditorParent());
		addField(jdpAddress);
		jdpPort = new IntegerFieldEditor(PreferenceConstants.PROPERTY_KEY_JDP_PORT,
				Messages.JDPPreferencePage_CAPTION_MULTICAST_PORT, getFieldEditorParent());
		addField(jdpPort);
		heartBeatTimeout = new IntegerFieldEditor(PreferenceConstants.PROPERTY_KEY_HEART_BEAT_TIMEOUT,
				Messages.JDPPreferencePage_CAPTION_MAX_HEART_BEAT_TIMEOUT, getFieldEditorParent());
		addField(heartBeatTimeout);

		enableJdpFields(isJdpAutoDiscoveryEnabled());
	}

	@Override
	public void init(IWorkbench workbench) {

	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(FieldEditor.VALUE)) {
			FieldEditor editor = (FieldEditor) event.getSource();
			if (PreferenceConstants.PROPERTY_KEY_JDP_AUTO_DISCOVERY.equals(editor.getPreferenceName())) {
				if ((boolean) event.getNewValue()) {
					MessageDialog.openWarning(getShell(),
							Messages.JDPPreferencePage_JDP_PREFERENCES_ENABLE_WARNING_TITLE,
							Messages.JDPPreferencePage_JDP_PREFERENCES_ENABLE_WARNING_INFO);
				}
				enableJdpFields((boolean) event.getNewValue());
			}
		}
		super.propertyChange(event);
	}

	private boolean isJdpAutoDiscoveryEnabled() {
		return JDPPlugin.getDefault().getPreferenceStore()
				.getBoolean(PreferenceConstants.PROPERTY_KEY_JDP_AUTO_DISCOVERY);
	}

	private void enableJdpFields(boolean enable) {
		jdpAddress.setEnabled(enable, getFieldEditorParent());
		jdpPort.setEnabled(enable, getFieldEditorParent());
		heartBeatTimeout.setEnabled(enable, getFieldEditorParent());
	}

}
