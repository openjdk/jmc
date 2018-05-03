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
package org.openjdk.jmc.flightrecorder.ui.preferences;

import java.util.LinkedList;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.misc.QuantityKindProposal;

/**
 * Preference dialog for Flight Recorder
 */
public class GeneralPage extends PreferencePage implements IWorkbenchPreferencePage {
	// private IWorkbench m_workbench;

	private Button noDefaultRadio;
	private Button wholeRadio;
	private Button timespanRadio;

	public GeneralPage() {
		setPreferenceStore(FlightRecorderUI.getDefault().getPreferenceStore());
		setDescription(Messages.PREFERENCES_GENERAL_SETTINGS_TEXT);
	}

	private class CheckBox {
		private final Button m_button;
		private final String m_preference;

		public CheckBox(Button button, String preference) {
			m_button = button;
			m_preference = preference;
		}

		public void setToDefault() {
			getPreferenceStore().setToDefault(m_preference);
			m_button.setSelection(getPreferenceStore().getBoolean(m_preference));
		}
	}

	private final LinkedList<CheckBox> checkBoxes = new LinkedList<>();

	private Text timespanValue;
	private Text selectionStoreValue;
	private Text itemListValue;
	private Text propertiesArrayStringSizeValue;
	private Text editorRuleEvaluationThreadsValue;

	@Override
	protected Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());

		Button removeRecordingsCheckbox = createRemoveRecordingsCheckBox(container);
		removeRecordingsCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		Button confirmRemoveTemplateCheckbox = createConfirmRemoveTemplateCheckBox(container);
		confirmRemoveTemplateCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		Button showMonitoringWarningCheckbox = createShowMonitoringWarningCheckBox(container);
		showMonitoringWarningCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		Button enableAnalysisCheckbox = createEnableAnalysisCheckBox(container);
		enableAnalysisCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		Button includeExperimental = createIncludeExperimentalEventsAndFieldsCheckBox(container);
		includeExperimental.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		Button allowIncompleteRecording = createAllowIncompleteRecordingFileCheckBox(container);
		allowIncompleteRecording.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		Group dumpDefaultGroup = new Group(container, SWT.NONE);
		dumpDefaultGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		dumpDefaultGroup.setText(Messages.DUMP_RECORDING_DEFAULT_TIMESPAN_TO_DUMP);
		dumpDefaultGroup.setLayout(new GridLayout());

		noDefaultRadio = new Button(dumpDefaultGroup, SWT.RADIO);
		noDefaultRadio.setText(Messages.DUMP_RECORDING_NO_DEFAULT);
		wholeRadio = new Button(dumpDefaultGroup, SWT.RADIO);
		wholeRadio.setText(Messages.DUMP_RECORDING_WHOLE);
		timespanRadio = new Button(dumpDefaultGroup, SWT.RADIO);
		timespanRadio.setText(Messages.DUMP_RECORDING_TIMESPAN);

		// If we have several text fields then we want them to be in the same container with a shared GridLayout so that
		// they become aligned. Since we only have one text field in this preference page then we can use a custom
		// container with its own layout.
		Composite defaultTimespanContainer = new Composite(container, SWT.NONE);
		defaultTimespanContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		defaultTimespanContainer.setLayout(new GridLayout(2, false));

		Label timespanLabel = new Label(defaultTimespanContainer, SWT.NONE);
		timespanLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		timespanLabel.setText(Messages.DUMP_RECORDING_TIMESPAN_VALUE);
		timespanValue = new Text(defaultTimespanContainer, SWT.BORDER);
		timespanValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		QuantityKindProposal.install(timespanValue, UnitLookup.TIMESPAN);

		Label selectionStoreLabel = new Label(defaultTimespanContainer, SWT.NONE);
		selectionStoreLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		selectionStoreLabel.setText(Messages.STORED_SELECTIONS_SIZE_PREF);
		selectionStoreValue = new Text(defaultTimespanContainer, SWT.BORDER);
		selectionStoreValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		QuantityKindProposal.install(selectionStoreValue, UnitLookup.NUMBER);

		Label itemListLabel = new Label(defaultTimespanContainer, SWT.NONE);
		itemListLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		itemListLabel.setText(Messages.ITEM_LIST_SIZE_PREF);
		itemListLabel.setToolTipText(Messages.ITEM_LIST_SIZE_PREF_TOOLTIP);
		itemListValue = new Text(defaultTimespanContainer, SWT.BORDER);
		itemListValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		QuantityKindProposal.install(itemListValue, UnitLookup.NUMBER);

		Label propertiesArrayStringSizeLabel = new Label(defaultTimespanContainer, SWT.NONE);
		propertiesArrayStringSizeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		propertiesArrayStringSizeLabel.setText(Messages.PREFERENCES_PROPERTIES_ARRAY_STRING_SIZE_TEXT);
		propertiesArrayStringSizeLabel.setToolTipText(Messages.PREFERENCES_PROPERTIES_ARRAY_STRING_SIZE_TOOLTIP);
		propertiesArrayStringSizeValue = new Text(defaultTimespanContainer, SWT.BORDER);
		propertiesArrayStringSizeValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		QuantityKindProposal.install(propertiesArrayStringSizeValue, UnitLookup.NUMBER);

		Label editorRuleEvaluationThreadsLabel = new Label(defaultTimespanContainer, SWT.NONE);
		editorRuleEvaluationThreadsLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		editorRuleEvaluationThreadsLabel.setText(Messages.PREFERENCES_EVALUATION_THREAD_NUMBER_TEXT);
		editorRuleEvaluationThreadsLabel.setToolTipText(Messages.PREFERENCES_EVALUATION_THREAD_NUMBER_TOOLTIP);
		editorRuleEvaluationThreadsValue = new Text(defaultTimespanContainer, SWT.BORDER);
		editorRuleEvaluationThreadsValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		QuantityKindProposal.install(editorRuleEvaluationThreadsValue, UnitLookup.NUMBER);

		loadDumpTypeFromPrefStore(false);
		loadTimespanFromPrefStore(false);
		loadSelectionStoreSizeFromPrefStore(false);
		loadItemListSizeFromPrefStore(false);
		loadPropertiesArrayStringSizeFromPrefStore(false);
		loadEditorRuleEvaluationThreadsFromPrefStore(false);
		timespanValue.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validatePage();
			}
		});
		selectionStoreValue.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validatePage();
			}
		});
		itemListValue.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validatePage();
			}
		});
		propertiesArrayStringSizeValue.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validatePage();
			}
		});
		editorRuleEvaluationThreadsValue.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validatePage();
			}
		});

		return container;
	}

	private void validatePage() {
		String error = validateSelectionStoreSize(selectionStoreValue.getText());
		setErrorMessage(error);

		String error2 = validateItemListSize(selectionStoreValue.getText());
		if (error == null) {
			setErrorMessage(error2);
		}
		String error3 = FlightRecorderUI.validateDumpTimespan(timespanValue.getText());
		if (error == null && error2 == null) {
			setErrorMessage(error3);
		}
		String error4 = validateNumEvaluationThreads(editorRuleEvaluationThreadsValue.getText());
		if (error == null && error2 == null && error3 == null) {
			setErrorMessage(error4);
		}
		setValid(error == null && error2 == null && error3 == null && error4 == null);
	}

	public static String validateNumEvaluationThreads(String text) {
		try {
			IQuantity size = UnitLookup.NUMBER.parseInteractive(text);
			if (size.longValue() < 1) {
				return Messages.PREFERENCES_EVALUATION_THREAD_NUMBER_LESS_THAN_ONE;
			}
		} catch (QuantityConversionException qce) {
			return NLS.bind(Messages.PREFERENCES_EVALUATION_THREAD_NUMBER_UNPARSEABLE, qce.getLocalizedMessage());
		}
		return null;
	}

	public static String validateSelectionStoreSize(String text) {
		try {
			IQuantity size = UnitLookup.NUMBER.parseInteractive(text);
			if (size.longValue() <= 0) {
				return Messages.STORED_SELECTIONS_SIZE_LESS_THAN_ZERO;
			}
		} catch (QuantityConversionException qce) {
			return NLS.bind(Messages.STORED_SELECTIONS_SIZE_UNPARSABLE, qce.getLocalizedMessage());
		}
		return null;
	}

	public static String validateItemListSize(String text) {
		try {
			IQuantity size = UnitLookup.NUMBER.parseInteractive(text);
			if (size.longValue() <= 0) {
				return Messages.ITEM_LIST_SIZE_LESS_THAN_ZERO;
			}
		} catch (QuantityConversionException qce) {
			return NLS.bind(Messages.ITEM_LIST_SIZE_UNPARSABLE, qce.getLocalizedMessage());
		}
		return null;
	}

	private void loadDumpTypeFromPrefStore(boolean loadDefault) {
		int dumpType = loadDefault ? getPreferenceStore().getDefaultInt(PreferenceKeys.PROPERTY_DEFAULT_DUMP_TYPE)
				: getPreferenceStore().getInt(PreferenceKeys.PROPERTY_DEFAULT_DUMP_TYPE);
		timespanRadio.setSelection(dumpType == PreferenceKeys.DUMP_TIMESPAN);
		wholeRadio.setSelection(dumpType == PreferenceKeys.DUMP_WHOLE);
		noDefaultRadio.setSelection(dumpType == PreferenceKeys.NO_DEFAULT_DUMP);
	}

	private void loadTimespanFromPrefStore(boolean loadDefault) {
		String timespan = loadDefault
				? getPreferenceStore().getDefaultString(PreferenceKeys.PROPERTY_DEFAULT_DUMP_TIMESPAN)
				: getPreferenceStore().getString(PreferenceKeys.PROPERTY_DEFAULT_DUMP_TIMESPAN);
		timespanValue.setText(FlightRecorderUI.parseDumpTimespan(timespan).interactiveFormat());
	}

	private void loadSelectionStoreSizeFromPrefStore(boolean loadDefault) {
		String size = loadDefault ? getPreferenceStore().getDefaultString(PreferenceKeys.PROPERTY_SELECTION_STORE_SIZE)
				: getPreferenceStore().getString(PreferenceKeys.PROPERTY_SELECTION_STORE_SIZE);
		selectionStoreValue.setText(FlightRecorderUI.parseSelectionStoreSize(size).interactiveFormat());
	}

	private void loadItemListSizeFromPrefStore(boolean loadDefault) {
		String size = loadDefault ? getPreferenceStore().getDefaultString(PreferenceKeys.PROPERTY_ITEM_LIST_SIZE)
				: getPreferenceStore().getString(PreferenceKeys.PROPERTY_ITEM_LIST_SIZE);
		itemListValue.setText(FlightRecorderUI.parseItemListSize(size).interactiveFormat());
	}

	private void loadPropertiesArrayStringSizeFromPrefStore(boolean loadDefault) {
		String size = loadDefault
				? getPreferenceStore().getDefaultString(PreferenceKeys.PROPERTY_MAXIMUM_PROPERTIES_ARRAY_STRING_SIZE)
				: getPreferenceStore().getString(PreferenceKeys.PROPERTY_MAXIMUM_PROPERTIES_ARRAY_STRING_SIZE);
		propertiesArrayStringSizeValue.setText(FlightRecorderUI.parseItemListSize(size).interactiveFormat());
	}

	private void loadEditorRuleEvaluationThreadsFromPrefStore(boolean loadDefault) {
		String size = loadDefault
				? getPreferenceStore().getDefaultString(PreferenceKeys.PROPERTY_NUM_EDITOR_RULE_EVALUATION_THREADS)
				: getPreferenceStore().getString(PreferenceKeys.PROPERTY_NUM_EDITOR_RULE_EVALUATION_THREADS);
		editorRuleEvaluationThreadsValue.setText(FlightRecorderUI.parseItemListSize(size).interactiveFormat());
	}

//	private Button createClearButton(Composite parent) {
//		Button button = new Button(parent, SWT.NONE);
//		button.setText(Messages.PREFERENCES_CLEAR_USER_SETTINGS_TEXT);
//		button.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				if (ensureAllEditorsClosed()) {
//					showConfirmUserSettingReset();
//				}
//			}
//		});
//		return button;
//	}
//
//	private boolean ensureAllEditorsClosed() {
//		for (IEditorReference ref : m_workbench.getActiveWorkbenchWindow().getActivePage().getEditorReferences()) {
//			if (JfrEditor.EDITOR_ID.equals(ref.getId())) {
//				showMustCloseEditor();
//				return false;
//			}
//		}
//		return true;
//	}
//
//	private void showMustCloseEditor() {
//		MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_INFORMATION);
//		messageBox.setMessage(Messages.PREFERENCES_OPEN_FLIGHT_RECORDING_DIALOG_TEXT);
//		messageBox.setText(Messages.PREFERENCES_OPEN_FLIGHT_RECORDING_DIALOG_TITLE);
//		messageBox.open();
//	}
//
//	private void showConfirmUserSettingReset() {
//		MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_WARNING | SWT.YES | SWT.NO);
//		messageBox.setMessage(Messages.PREFERENCES_RESET_USER_SETTINGS_DIALOG_TEXT);
//		messageBox.setText(Messages.PREFERENCES_RESET_USER_SETTINGS_DIALOG_TITLE);
//		if (messageBox.open() == SWT.YES) {
//			ComponentsPlugin.getDefault().getUserInterfaceRepository().clearUserSettings(JfrEditor.EDITOR_ID);
//		}
//	}

	private Button createRemoveRecordingsCheckBox(Composite parent) {
		return createCheckBox(parent, Messages.PREFERENCES_REMOVE_FINISHED_RECORDING_TEXT,
				PreferenceKeys.PROPERTY_REMOVE_FINISHED_RECORDING);
	}

	private Button createConfirmRemoveTemplateCheckBox(Composite parent) {
		return createCheckBox(parent, Messages.PREFERENCES_CONFIRM_REMOVE_TEMPLATE_TEXT,
				PreferenceKeys.PROPERTY_CONFIRM_REMOVE_TEMPLATE);
	}

	private Button createShowMonitoringWarningCheckBox(Composite parent) {
		return createCheckBox(parent, Messages.PREFERENCES_SHOW_MONITORING_WARNING_TEXT,
				PreferenceKeys.PROPERTY_SHOW_MONITORING_WARNING);
	}

	private Button createEnableAnalysisCheckBox(Composite parent) {
		return createCheckBox(parent, Messages.PREFERENCES_ENABLE_RECORDING_ANALYSIS,
				PreferenceKeys.PROPERTY_ENABLE_RECORDING_ANALYSIS);
	}

	private Button createIncludeExperimentalEventsAndFieldsCheckBox(Composite parent) {
		return createCheckBox(parent, Messages.PREFERENCES_INCLUDE_EXPERIMENTAL_EVENTS_AND_FIELDS,
				PreferenceKeys.PROPERTY_INCLUDE_EXPERIMENTAL_EVENTS_AND_FIELDS);
	}

	private Button createAllowIncompleteRecordingFileCheckBox(Composite parent) {
		return createCheckBox(parent, Messages.PREFERENCES_ALLOW_INCOMPLETE_RECORDING_FILE,
				PreferenceKeys.PROPERTY_ALLOW_INCOMPLETE_RECORDING_FILE);
	}

	@Override
	protected void performDefaults() {
		for (CheckBox checkBox : checkBoxes) {
			checkBox.setToDefault();
		}
		loadDumpTypeFromPrefStore(true);
		loadTimespanFromPrefStore(true);
		loadSelectionStoreSizeFromPrefStore(true);
		loadItemListSizeFromPrefStore(true);
		loadPropertiesArrayStringSizeFromPrefStore(true);
		loadEditorRuleEvaluationThreadsFromPrefStore(true);
		super.performDefaults();
	}

	private Button createCheckBox(Composite parent, String text, final String name) {
		final Button checkBox = new Button(parent, SWT.CHECK);
		checkBox.setSelection(getPreferenceStore().getBoolean(name));
		checkBox.setText(text);
		checkBoxes.add(new CheckBox(checkBox, name));
		return checkBox;
	}

	@Override
	public boolean performOk() {
		try {
			IQuantity timespan = UnitLookup.TIMESPAN.parseInteractive(timespanValue.getText());
			getPreferenceStore().setValue(PreferenceKeys.PROPERTY_DEFAULT_DUMP_TIMESPAN, timespan.persistableString());
		} catch (QuantityConversionException qce) {
			setErrorMessage(qce.getLocalizedMessage());
			return false;
		}
		try {
			IQuantity size = UnitLookup.NUMBER.parseInteractive(selectionStoreValue.getText());
			getPreferenceStore().setValue(PreferenceKeys.PROPERTY_SELECTION_STORE_SIZE, size.persistableString());
		} catch (QuantityConversionException qce) {
			setErrorMessage(qce.getLocalizedMessage());
			return false;
		}
		try {
			IQuantity size = UnitLookup.NUMBER.parseInteractive(itemListValue.getText());
			getPreferenceStore().setValue(PreferenceKeys.PROPERTY_ITEM_LIST_SIZE, size.persistableString());
		} catch (QuantityConversionException qce) {
			setErrorMessage(qce.getLocalizedMessage());
			return false;
		}
		try {
			IQuantity size = UnitLookup.NUMBER.parseInteractive(propertiesArrayStringSizeValue.getText());
			getPreferenceStore().setValue(PreferenceKeys.PROPERTY_MAXIMUM_PROPERTIES_ARRAY_STRING_SIZE,
					size.persistableString());
		} catch (QuantityConversionException qce) {
			setErrorMessage(qce.getLocalizedMessage());
			return false;
		}
		try {
			IQuantity size = UnitLookup.NUMBER.parseInteractive(editorRuleEvaluationThreadsValue.getText());
			size = UnitLookup.NUMBER_UNITY.quantity(size.clampedFloorIn(UnitLookup.NUMBER_UNITY));
			getPreferenceStore().setValue(PreferenceKeys.PROPERTY_NUM_EDITOR_RULE_EVALUATION_THREADS,
					size.persistableString());
		} catch (QuantityConversionException qce) {
			setErrorMessage(qce.getLocalizedMessage());
			return false;
		}
		setErrorMessage(null);
		if (timespanRadio.getSelection()) {
			getPreferenceStore().setValue(PreferenceKeys.PROPERTY_DEFAULT_DUMP_TYPE, PreferenceKeys.DUMP_TIMESPAN);
		} else if (wholeRadio.getSelection()) {
			getPreferenceStore().setValue(PreferenceKeys.PROPERTY_DEFAULT_DUMP_TYPE, PreferenceKeys.DUMP_WHOLE);
		} else {
			getPreferenceStore().setValue(PreferenceKeys.PROPERTY_DEFAULT_DUMP_TYPE, PreferenceKeys.NO_DEFAULT_DUMP);
		}
		for (CheckBox checkBox : checkBoxes) {
			getPreferenceStore().setValue(checkBox.m_preference, checkBox.m_button.getSelection());
		}
		return super.performOk();
	}

	@Override
	public void init(IWorkbench workbench) {
//		m_workbench = workbench;
	}
}
