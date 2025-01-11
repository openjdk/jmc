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
package org.openjdk.jmc.jolokia.preferences;

import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.openjdk.jmc.jolokia.JmcJolokiaPlugin;

public class JolokiaPreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage, PreferenceConstants {

	private Map<Control, Object> dependantControls = new WeakHashMap<>();

	public JolokiaPreferencePage() {
		super(GRID);
		setPreferenceStore(JmcJolokiaPlugin.getDefault().getPreferenceStore());
		setDescription(Messages.JolokiaPreferencePage_Description);
	}

	public void createFieldEditors() {
		BooleanFieldEditor mainEnabler = new BooleanFieldEditor(P_SCAN, Messages.JolokiaPreferencePage_Label,
				getFieldEditorParent()) {
			@Override
			protected void valueChanged(boolean oldValue, boolean newValue) {
				super.valueChanged(oldValue, newValue);
				enableDependantFields(newValue);
			}
		};
		addField(mainEnabler);
		this.addTextField(new StringFieldEditor(P_MULTICAST_GROUP, Messages.JolokiaPreferencePage_MulticastGroupLabel,
				getFieldEditorParent()), Messages.JolokiaPreferencePage_MulticastGroupTooltip);
		this.addTextField(new IntegerFieldEditor(P_MULTICAST_PORT, Messages.JolokiaPreferencePage_MulticastPortLabel,
				getFieldEditorParent()), Messages.JolokiaPreferencePage_MulticastPortTooltip);
		this.addDependantField(new IntegerFieldEditor(P_DISCOVER_TIMEOUT,
				Messages.JolokiaPreferencePage_DiscoverTimeoutLabel, getFieldEditorParent()), getControl());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
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
}
