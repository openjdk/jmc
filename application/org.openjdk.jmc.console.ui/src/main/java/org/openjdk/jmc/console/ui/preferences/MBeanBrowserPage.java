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

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.openjdk.jmc.console.ui.messages.internal.Messages;
import org.openjdk.jmc.rjmx.ui.RJMXUIPlugin;
import org.openjdk.jmc.rjmx.ui.internal.RJMXUIConstants;
import org.openjdk.jmc.ui.preferences.PreferencesToolkit;

// FIXME: This preference page would make more sense in the console.ui.mbeanbrowser plug-in?
public class MBeanBrowserPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private BooleanFieldEditor mBeanAskUserBeforeDeleteEditor;
	private StringFieldEditor mBeanPropertyKeyOrderEditor;
	private StringFieldEditor mBeanSuffixPropertyKeyOrderEditor;
	private BooleanFieldEditor mBeanPropertiesInAlphabeticOrderEditor;
	private BooleanFieldEditor mBeanCaseInsensitivePropertyOrderEditor;
	private BooleanFieldEditor mBeanShowCompressedPathsEditor;

	/**
	 * Constructor
	 *
	 * @param SWT
	 *            style
	 */
	public MBeanBrowserPage() {
		super(FLAT);
		setPreferenceStore(RJMXUIPlugin.getDefault().getPreferenceStore());
		setDescription(Messages.MBeanBrowserPage_LABEL_MBEAN_BROWSER_PREFERENCES_TEXT);
	}

	@Override
	public void createFieldEditors() {
		mBeanAskUserBeforeDeleteEditor = new BooleanFieldEditor(
				RJMXUIConstants.PROPERTY_ASK_USER_BEFORE_MBEAN_UNREGISTER,
				Messages.MBeanBrowserPage_LABEL_PROPERTY_ASK_USER_BEFORE_MBEAN_UNREGISTER, getFieldEditorParent());
		addField(mBeanAskUserBeforeDeleteEditor);
		mBeanPropertyKeyOrderEditor = new StringFieldEditor(RJMXUIConstants.PROPERTY_MBEAN_PROPERTY_KEY_ORDER,
				Messages.MBeanBrowserPage_LABEL_PROPERTY_KEY_ORDER_OVERRIDE_TEXT, getFieldEditorParent());
		addField(mBeanPropertyKeyOrderEditor);
		mBeanSuffixPropertyKeyOrderEditor = new StringFieldEditor(
				RJMXUIConstants.PROPERTY_MBEAN_SUFFIX_PROPERTY_KEY_ORDER,
				Messages.MBeanBrowserPage_LABEL_SUFFIX_PROPERTY_KEY_ORDER_OVERRIDE_TEXT, getFieldEditorParent());
		addField(mBeanSuffixPropertyKeyOrderEditor);
		mBeanPropertiesInAlphabeticOrderEditor = new BooleanFieldEditor(
				RJMXUIConstants.PROPERTY_MBEAN_PROPERTIES_IN_ALPHABETIC_ORDER,
				Messages.MBeanBrowserPage_LABEL_PROPERTIES_IN_ALPHABETIC_ORDER_OVERRIDE_TEXT, getFieldEditorParent());
		addField(mBeanPropertiesInAlphabeticOrderEditor);
		mBeanCaseInsensitivePropertyOrderEditor = new BooleanFieldEditor(
				RJMXUIConstants.PROPERTY_MBEAN_CASE_INSENSITIVE_PROPERTY_ORDER,
				Messages.MBeanBrowserPage_LABEL_CASE_INSENSITIVE_KEY_COMPARISON_OVERRIDE_TEXT, getFieldEditorParent());
		addField(mBeanCaseInsensitivePropertyOrderEditor);
		mBeanShowCompressedPathsEditor = new BooleanFieldEditor(RJMXUIConstants.PROPERTY_MBEAN_SHOW_COMPRESSED_PATHS,
				Messages.MBeanBrowserPage_LABEL_SHOW_COMPRESSED_PATHS_TEXT, getFieldEditorParent());
		addField(mBeanShowCompressedPathsEditor);
		PreferencesToolkit.fillNoteControl(getFieldEditorParent(), Messages.MBeanBrowserPage_NOTE_PROPERTIES_TEXT);
	}

	@Override
	public void init(IWorkbench workbench) {
	}
}
