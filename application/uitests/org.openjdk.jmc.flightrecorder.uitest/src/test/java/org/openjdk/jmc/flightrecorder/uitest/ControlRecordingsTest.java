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
package org.openjdk.jmc.flightrecorder.uitest;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.openjdk.jmc.test.jemmy.MCJemmyTestBase;
import org.openjdk.jmc.test.jemmy.MCUITestRule;
import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;
import org.openjdk.jmc.test.jemmy.misc.helpers.EventSettingsData;
import org.openjdk.jmc.test.jemmy.misc.helpers.EventSettingsData.EventSettings;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JfrUi;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JfrWizard;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MC;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCButton;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCButton.Labels;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCDialog;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCMenu;

/**
 * Tests that perform various actions on recordings
 */
@SuppressWarnings("restriction")
public class ControlRecordingsTest extends MCJemmyTestBase {
	private static final String TEST_RECORDING_LENGTH = "10 min";
	private static final String TEST_PERIOD = "5000 ms";
	private static final String TEST_THRESHOLD = "5 ms";
	private static final String BUTTON_WHOLE_RECORDING_TEXT = org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages.BUTTON_WHOLE_RECORDING_TEXT;
	private static final String BUTTON_LAST_PART_OF_RECORDING_TEXT = org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages.BUTTON_LAST_PART_OF_RECORDING_TEXT;
	private static final String BUTTON_INTERVAL_OF_RECORDING_TEXT = org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages.BUTTON_INTERVAL_OF_RECORDING_TEXT;
	private static final String DUMP_RECORDING_NO_DEFAULT = org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages.DUMP_RECORDING_NO_DEFAULT;
	private static final String DUMP_RECORDING_WHOLE = org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages.DUMP_RECORDING_WHOLE;
	private static final String DUMP_RECORDING_TIMESPAN = org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages.DUMP_RECORDING_TIMESPAN;
	private static final String TEST_RECORDING_NAME = "TestRecording";
	private EventSettingsData recordingSettings;

	@Rule
	public MCUITestRule testRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void before() {
			if (!MC.jvmBrowser.hasRecording(TEST_RECORDING_NAME)) {
				startContinuousTestRecording();
			}
		}

		@Override
		public void after() {
			MCMenu.closeAllEditors();
		}
	};

	@ClassRule
	public static MCUITestRule classTestRule = new MCUITestRule(verboseRuleOutput) {

		@Override
		public void before() {
			skipIfEarlierThan8u0(TEST_CONNECTION);
		}

		@Override
		public void after() {
			// Clean up, if needed
			if (testsRun() && MC.jvmBrowser.hasRecording(TEST_RECORDING_NAME)) {
				MC.jvmBrowser.closeRecording(TEST_RECORDING_NAME);
			}
		}
	};

	/**
	 * Verifies that a recording is opened.
	 */
	@Test
	public void dumpLastPart() {
		String recordingFileName = getRunningTestRecordingFileNamePrefix();
		MC.jvmBrowser.dumpLastPartOfRecording(TEST_RECORDING_NAME);
		Assert.assertTrue("Could not find the dumped recording with name containing \"" + recordingFileName + "\"",
				MCJemmyBase.waitForSubstringMatchedEditor(recordingFileName));
	}

	/**
	 * Verifies that a recording is opened.
	 */
	@Test
	public void dumpWholeRecording() {
		String recordingFileName = getRunningTestRecordingFileNamePrefix();
		MC.jvmBrowser.dumpWholeRecording(TEST_RECORDING_NAME);
		Assert.assertTrue("Could not find the dumped recording with name containing \"" + recordingFileName + "\"",
				MCJemmyBase.waitForSubstringMatchedEditor(recordingFileName));
	}

	/**
	 * Verifies that recordings are opened for each of the three alternatives in the dump dialog
	 */
	@Test
	public void dumpWholeWithDialog() {
		Assert.assertTrue("Could not find the dumped recording when using the \"" + BUTTON_WHOLE_RECORDING_TEXT
				+ "\" dump option", runDumpTestRecordingWithDialog(BUTTON_WHOLE_RECORDING_TEXT));
	}

	/**
	 * Verifies that recordings are opened for each of the three alternatives in the dump dialog
	 */
	@Test
	public void dumpLastPartWithDialog() {
		Assert.assertTrue("Could not find the dumped recording when using the \"" + BUTTON_LAST_PART_OF_RECORDING_TEXT
				+ "\" dump option", runDumpTestRecordingWithDialog(BUTTON_LAST_PART_OF_RECORDING_TEXT));
	}

	/**
	 * Verifies that recordings are opened for each of the three alternatives in the dump dialog
	 */
	@Test
	public void dumpIntervalWithDialog() {
		Assert.assertTrue("Could not find the dumped recording when using the \"" + BUTTON_INTERVAL_OF_RECORDING_TEXT
				+ "\" dump option", runDumpTestRecordingWithDialog(BUTTON_INTERVAL_OF_RECORDING_TEXT));
	}

	/**
	 * Verifies that the dialog and recording is opened.
	 */
	@Test
	public void doubleClickDumpWithNoDefault() {
		boolean dialogFound = true;
		String recordingFileName = getRunningTestRecordingFileNamePrefix();
		setDumpPreference(DUMP_RECORDING_NO_DEFAULT);
		MCDialog dumpDialog = MC.jvmBrowser.doubleClickRecording(TEST_RECORDING_NAME);
		if (dumpDialog == null) {
			dialogFound = false;
		} else {
			dumpDialog.clickButton(Labels.FINISH);
		}
		Assert.assertTrue(
				"Could not find the dumped recording with name containing \"" + recordingFileName
						+ "\" and with dump preference set to \"" + DUMP_RECORDING_NO_DEFAULT + "\"",
				MCJemmyBase.waitForSubstringMatchedEditor(recordingFileName));
		MCMenu.closeActiveEditor();
		// We do the assert late in order to clean up regardless of how things went earlier (i.e the dialog showed up or not)
		Assert.assertTrue("Double click on recording was expected to generate a dump dialog", dialogFound);
	}

	/**
	 * Verifies that the default dump preference setting results in a dumped and opened recording
	 */
	@Test
	public void doubleClickAndVerifyDefaultDumpSetting() {
		String recordingFileName = getRunningTestRecordingFileNamePrefix();
		for (String dumpOption : new String[] {DUMP_RECORDING_WHOLE, DUMP_RECORDING_TIMESPAN}) {
			setDumpPreference(dumpOption);
			MCDialog dumpDialog = MC.jvmBrowser.doubleClickRecording(TEST_RECORDING_NAME);
			if (dumpDialog != null) {
				dumpDialog.clickButton(Labels.CANCEL);
				Assert.fail("Double click on recording with dump preference set to \"" + dumpOption
						+ "\" was not expected to generate a dump dialog");
			}
			Assert.assertTrue(
					"Could not find the dumped recording with name containing \"" + recordingFileName
							+ "\" for dump preference \"" + dumpOption + "\"",
					MCJemmyBase.waitForSubstringMatchedEditor(recordingFileName));
			MCMenu.closeActiveEditor();
		}
	}

	/**
	 * Sets each default dump option and verifies that the setting is persisted between visits of
	 * the preference page
	 */
	@Test
	public void verifyDefaultDumpSettingPersistence() {
		for (String desiredSetting : new String[] {DUMP_RECORDING_NO_DEFAULT, DUMP_RECORDING_WHOLE,
				DUMP_RECORDING_TIMESPAN}) {
			String resultingSetting = setAndReturnCurrentDefaultDumpSetting(desiredSetting);
			Assert.assertTrue("Double-click recording dump preference should be \"" + desiredSetting + "\" but was \""
					+ resultingSetting + "\"", desiredSetting.equals(resultingSetting));
		}
	}

	/**
	 * Verifies that a recording is closed without a resulting recording editor
	 */
	@Test
	public void closeRecording() {
		String recordingFileName = getRunningTestRecordingFileNamePrefix();
		MC.jvmBrowser.closeRecording(TEST_RECORDING_NAME);
		Assert.assertFalse(
				"Should not have found a matching recording editor with name containing \"" + recordingFileName + "\"",
				MCJemmyBase.waitForSubstringMatchedEditor(recordingFileName));
	}

	/**
	 * Verifies that a recording isn't opened when stopped (for continuous recordings)
	 */
	@Test
	public void stopContinuousRecording() {
		String recordingFileName = getRunningTestRecordingFileNamePrefix();
		MC.jvmBrowser.stopRecording(TEST_RECORDING_NAME);
		MC.jvmBrowser.closeRecording(TEST_RECORDING_NAME);
		Assert.assertFalse("Did not expect to find the stopped recording with prefix " + recordingFileName,
				MCJemmyBase.waitForSubstringMatchedEditor(recordingFileName));
	}

	/**
	 * Verifies that a recording is opened when stopped
	 */
	@Test
	public void stopTimeFixedRecording() {
		MC.jvmBrowser.closeRecording(TEST_RECORDING_NAME);
		startTimeFixedTestRecording();
		String recordingFileName = getRunningTestRecordingFileNamePrefix();
		MC.jvmBrowser.stopRecording(TEST_RECORDING_NAME);
		Assert.assertTrue("Could not find the stopped recording with prefix " + recordingFileName,
				MCJemmyBase.waitForSubstringMatchedEditor(recordingFileName));
	}

	/**
	 * Verifies that recording events can be added/removed on the fly
	 */
	@Test
	public void modifyRecordingEvents() {
		// Dump the test recording to get the current event settings (combined from, possibly multiple recordings)
		MC.jvmBrowser.dumpLastPartOfRecording(TEST_RECORDING_NAME);
		EventSettingsData currentSettings = JfrUi.parseEventSettingsTable();
		MCMenu.closeActiveEditor();
		// Find an event that's turned off
		String eventToTurnOn = null;
		for (String eventName : currentSettings.getAllEventNames()) {
			EventSettingsData.EventSettings es = currentSettings.getLatest(eventName);
			if (es.getSetting(EventSettingsData.SETTING_ENABLED).equals("false")) {
				eventToTurnOn = eventName;
				break;
			}
		}
		// Assert that we did indeed find a suitable event
		Assert.assertNotNull("All events were already turned on. Could not proceed with the test", eventToTurnOn);
		// Edit the test recording and enable an event
		JfrWizard recordingWizard = MC.jvmBrowser.editRecording(TEST_RECORDING_NAME);
		String[] eventPath = getEventPath(recordingWizard, eventToTurnOn);
		recordingWizard.enableEvent(eventPath);
		recordingWizard.startRecording();
		// Dump the edited recording
		MC.jvmBrowser.dumpLastPartOfRecording(TEST_RECORDING_NAME);
		// Parse the recording settings table to verify that the event was turned on
		currentSettings = JfrUi.parseEventSettingsTable();
		MCMenu.closeActiveEditor();
		Assert.assertTrue("Event \"" + eventToTurnOn + "\" should be turned on",
				currentSettings.getLatest(eventToTurnOn).getSetting(EventSettingsData.SETTING_ENABLED).equals("true"));
		// Edit the test recording and disable/remove the event previously enabled
		recordingWizard = MC.jvmBrowser.editRecording(TEST_RECORDING_NAME);
		recordingWizard.disableEvent(eventPath);
		recordingWizard.startRecording();
		// Dump the edited recording
		MC.jvmBrowser.dumpLastPartOfRecording(TEST_RECORDING_NAME);
		// Parse the recording settings table to verify that the event was turned on
		currentSettings = JfrUi.parseEventSettingsTable();
		MCMenu.closeActiveEditor();
		Assert.assertFalse("Event \"" + eventToTurnOn + "\" should be turned off",
				currentSettings.getLatest(eventToTurnOn).getSetting(EventSettingsData.SETTING_ENABLED).equals("true"));
	}

	/**
	 * Verifies that recording event threshold settings can be modified on the fly
	 */
	@Test
	public void modifyEventThreshold() {
		// Dump the test recording to get the current event settings (combined from, possibly multiple recordings)
		MC.jvmBrowser.dumpLastPartOfRecording(TEST_RECORDING_NAME);
		EventSettingsData currentSettings = JfrUi.parseEventSettingsTable();
		MCMenu.closeActiveEditor();
		// Find an event that has a threshold value
		String eventToModify = null;
		for (String eventName : currentSettings.getAllEventNames()) {
			if (hasValidThreshold(currentSettings, eventName)) {
				eventToModify = eventName;
				break;
			}
		}
		// Assert that we did indeed find a suitable event
		Assert.assertNotNull("Could not find a suitable event to modify. Could not proceed with the test."
				+ getAllSettingValues(currentSettings, EventSettingsData.SETTING_THRESHOLD), eventToModify);
		// Edit the test recording and edit the threshold value for the selected event
		JfrWizard recordingWizard = MC.jvmBrowser.editRecording(TEST_RECORDING_NAME);
		EventSettings modifiedSettings = recordingWizard.setEventThreshold(TEST_THRESHOLD,
				getEventPath(recordingWizard, eventToModify));
		recordingWizard.startRecording();
		// Dump the edited recording
		MC.jvmBrowser.dumpLastPartOfRecording(TEST_RECORDING_NAME);
		// Parse the recording settings table to verify that the event threshold was modified correctly
		currentSettings = JfrUi.parseEventSettingsTable();
		MCMenu.closeActiveEditor();
		String thresholdValue = currentSettings.getLatest(eventToModify)
				.getSetting(EventSettingsData.SETTING_THRESHOLD);
		Assert.assertTrue(
				"Event \"" + eventToModify + "\" threshold value should be " + TEST_THRESHOLD + " but was: "
						+ thresholdValue,
				currentSettings.getLatest(eventToModify).canBeResultOf(modifiedSettings, IS_JFR_NEXT));
	}

	/**
	 * Verifies that recording event period settings can be modified on the fly
	 */
	@Test
	public void modifyEventPeriod() {
		// FIXME: JMC-5207 - Remove the assume call once the GTK3 related bug has been fixed
		Assume.assumeFalse("Skipping on Linux due to GTK3 related bug",
				MCJemmyBase.OS_NAME.contains("linux"));
		// Dump the test recording to get the current event settings (combined from, possibly multiple recordings)
		MC.jvmBrowser.dumpLastPartOfRecording(TEST_RECORDING_NAME);
		EventSettingsData currentSettings = JfrUi.parseEventSettingsTable();
		MCMenu.closeActiveEditor();
		// Find an event that has a period value
		String eventToModify = null;
		for (String eventName : currentSettings.getAllEventNames()) {
			if (hasValidPeriod(currentSettings, eventName)) {
				eventToModify = eventName;
				break;
			}
		}
		// Assert that we did indeed find a suitable event
		Assert.assertNotNull("Could not find a suitable event to modify. Could not proceed with the test."
				+ getAllSettingValues(currentSettings, EventSettingsData.SETTING_PERIOD), eventToModify);
		// Edit the test recording and add an event that isn't currently enabled
		JfrWizard recordingWizard = MC.jvmBrowser.editRecording(TEST_RECORDING_NAME);
		EventSettings modifiedSettings = recordingWizard.setEventPeriod(TEST_PERIOD,
				getEventPath(recordingWizard, eventToModify));
		recordingWizard.startRecording();
		// Dump the test recording to get the current event settings (combined from, possibly multiple recordings). Verify that the event that was enabled actually is enabled
		MC.jvmBrowser.dumpLastPartOfRecording(TEST_RECORDING_NAME);
		// Parse the recording settings table to verify that the event period was modified correctly
		currentSettings = JfrUi.parseEventSettingsTable();
		MCMenu.closeActiveEditor();
		String periodValue = currentSettings.getLatest(eventToModify).getSetting(EventSettingsData.SETTING_PERIOD);
		Assert.assertTrue(
				"Event \"" + eventToModify + "\" period value was \"" + periodValue + "\" but should be " + TEST_PERIOD,
				currentSettings.getLatest(eventToModify).canBeResultOf(modifiedSettings, IS_JFR_NEXT));
	}

	private String getAllSettingValues(EventSettingsData data, String setting) {
		StringBuilder sb = new StringBuilder();
		for (String eventName : data.getAllEventNames()) {
			sb.append(getSettingValues(data, setting, eventName));
		}
		return sb.toString();
	}

	private String getSettingValues(EventSettingsData data, String setting, String eventName) {
		StringBuilder sb = new StringBuilder();
		sb.append(eventName + ": " + setting + ": ");
		for (EventSettings settings : data.get(eventName)) {
			String settingString = settings.getSetting(setting);
			sb.append(settingString + " ");
		}
		sb.append("\n");
		return sb.toString();
	}

	private boolean isValidThreshold(String thresholdValue) {
		return thresholdValue != null && !"0 ns".equals(thresholdValue);
	}

	private boolean hasValidThreshold(EventSettingsData data, String eventName) {
		boolean isOkThreshold = false;
		for (EventSettings setting : data.get(eventName)) {
			String thresholdValue = setting.getSetting(EventSettingsData.SETTING_THRESHOLD);
			if (isValidThreshold(thresholdValue)) {
				isOkThreshold = true;
				break;
			}
		}
		return isOkThreshold
				&& !TEST_THRESHOLD.equals(data.getLatest(eventName).getSetting(EventSettingsData.SETTING_THRESHOLD));
	}

	private boolean isValidPeriod(String periodValue) {
		return periodValue != null && periodValue.matches("\\d+\\s*[a-zA-Z]+");
	}

	private boolean hasValidPeriod(EventSettingsData data, String eventName) {
		boolean isOkPeriod = false;
		for (EventSettings setting : data.get(eventName)) {
			// go through all settings of the named event to see if it has a time bound period setting
			String periodValue = setting.getSetting(EventSettingsData.SETTING_PERIOD);
			if (isValidPeriod(periodValue)) {
				isOkPeriod = true;
				break;
			}
		}
		if (isOkPeriod) {
			// now also check that the period value is larger than what we want to set with our change
			String currentPeriodValue = data.getLatest(eventName).getSetting(EventSettingsData.SETTING_PERIOD);
			int periodComparison = EventSettingsData.comparePeriod(IS_JFR_NEXT, eventName, currentPeriodValue,
					TEST_PERIOD);
			return periodComparison > 0 && periodComparison < 99;
		}
		return false;
	}

	private String[] getEventPath(JfrWizard recordingWizard, String eventName) {
		// As the set of events won't change between the different tests we can speed up things by not traversing the settings tree more than once for the whole test suite
		if (recordingSettings == null) {
			recordingSettings = recordingWizard.getCurrentEventSettings();
		}
		return recordingSettings.getLatest(eventName).getEventPath();
	}

	private void startTimeFixedTestRecording() {
		startTestRecording(TEST_RECORDING_NAME, TEST_RECORDING_LENGTH);
	}

	private void startContinuousTestRecording() {
		startTestRecording(TEST_RECORDING_NAME, null);
	}

	private void startTestRecording(String recordingName, String duration) {
		JfrWizard wizard = MC.jvmBrowser.startFlightRecordingWizard();
		wizard.setRecordingName(recordingName);
		if (duration != null) {
			wizard.setDurationText(duration);
		} else {
			wizard.ensureContinuousRecording();
		}
		wizard.chooseTemplate("Continuous - on server");
		wizard.startRecording();
		if (duration != null) {
			MCDialog progressDialog = MCDialog.getByAnyDialogTitle(false, "Recording " + TEST_RECORDING_NAME);
			if (progressDialog != null && !progressDialog.isDisposed()) {
				progressDialog.clickButton("Run in &Background");
			}
		}
		int retries = 0;
		while (!MC.jvmBrowser.hasRecording(recordingName) && retries < 50) {
			sleep(100);
			retries++;
		}
	}

	private static MCDialog openFlrPreferences() {
		MCDialog preferences = MCMenu.openPreferencesDialog();
		preferences.setIdleUiWait(false);
		preferences.selectTreeItem("JDK Mission Control", "Flight Recorder");
		return preferences;
	}

	private boolean runDumpTestRecordingWithDialog(String dumpOption) {
		String recordingFileName = getRunningTestRecordingFileNamePrefix();
		MCDialog dumpDialog = MC.jvmBrowser.dumpRecording(TEST_RECORDING_NAME);
		dumpDialog.setButtonState(dumpOption, true);
		dumpDialog.clickButton(Labels.FINISH);
		return MCJemmyBase.waitForSubstringMatchedEditor(recordingFileName);
	}

	private String setAndReturnCurrentDefaultDumpSetting(String desiredSetting) {
		setDumpPreference(desiredSetting);
		return getCurrentDumpPreference();
	}

	private String getCurrentDumpPreference() {
		MCDialog preferences = openFlrPreferences();
		String dumpPreference = null;
		if (preferences.getButtonState(DUMP_RECORDING_NO_DEFAULT) == true) {
			dumpPreference = DUMP_RECORDING_NO_DEFAULT;
		} else if (preferences.getButtonState(DUMP_RECORDING_WHOLE) == true) {
			dumpPreference = DUMP_RECORDING_WHOLE;
		} else {
			dumpPreference = DUMP_RECORDING_TIMESPAN;
		}
		preferences.clickButton(Labels.CANCEL);
		return dumpPreference;
	}

	private void setDumpPreference(String dumpOption) {
		MCDialog preferences = openFlrPreferences();
		preferences.setButtonState(dumpOption, true);
		preferences.clickButton(MCButton.Labels.APPLY_AND_CLOSE);
	}

	private String getRunningTestRecordingFileNamePrefix() {
		String recordingFileName = MC.jvmBrowser.getRunningRecordingFileName(TEST_RECORDING_NAME);
		return recordingFileName.substring(0, recordingFileName.indexOf("."));
	}
}
