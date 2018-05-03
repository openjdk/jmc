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

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import org.openjdk.jmc.console.ui.ConsolePlugin;
import org.openjdk.jmc.console.ui.messages.internal.Messages;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.preferences.PreferencesKeys;
import org.openjdk.jmc.ui.misc.IntFieldEditor;

public class GeneralPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private IntegerFieldEditor aggregateSizeField;
	private BooleanFieldEditor heapHistogramUpdateWarn;
	private IntegerFieldEditor threadUpdateInterval;

	public GeneralPage() {
		super(GRID);
		setDescription(Messages.GeneralPage_DESCRIPTION);
	}

	@Override
	public void createFieldEditors() {
		threadUpdateInterval = new IntFieldEditor(ConsoleConstants.PROPERTY_THREAD_DUMP_INTERVAL,
				Messages.CommunicationPage_UPDATE_INTERVAL_THREAD_STACK0, getFieldEditorParent());
		threadUpdateInterval.setValidRange(50, Integer.MAX_VALUE);
		addField(threadUpdateInterval);

		heapHistogramUpdateWarn = new BooleanFieldEditor(ConsoleConstants.PROPERTY_HEAPHISTOGRAM_UPDATE_WARNING,
				Messages.GeneralPage_SHOW_WARNING_BEFORE_UPDATING_HEAP_HISTOGRAM, getFieldEditorParent());
		addField(heapHistogramUpdateWarn);
		aggregateSizeField = new IntegerFieldEditor(PreferencesKeys.PROPERTY_LIST_AGGREGATE_SIZE,
				Messages.GeneralPage_LIST_AGGREGATE_SIZE, getFieldEditorParent());
		addField(aggregateSizeField);
	}

	@Override
	protected void initialize() {
		initialize(threadUpdateInterval, ConsolePlugin.getDefault().getPreferenceStore());
		initialize(heapHistogramUpdateWarn, ConsolePlugin.getDefault().getPreferenceStore());
		initialize(aggregateSizeField, new ScopedPreferenceStore(InstanceScope.INSTANCE, RJMXPlugin.PLUGIN_ID));
	}

	private void initialize(FieldEditor editor, IPreferenceStore store) {
		editor.setPage(this);
		editor.setPropertyChangeListener(this);
		editor.setPreferenceStore(store);
		editor.load();
	}

	@Override
	public void init(IWorkbench workbench) {
	}
}
