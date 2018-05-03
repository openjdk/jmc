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

import java.util.List;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.openjdk.jmc.test.jemmy.MCJemmyTestBase;
import org.openjdk.jmc.test.jemmy.MCUITestRule;
import org.openjdk.jmc.test.jemmy.misc.helpers.ConnectionHelper;
import org.openjdk.jmc.test.jemmy.misc.helpers.EventSettingsData;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JfrUi;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JfrWizard;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MC;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCMenu;

/**
 * Class for testing the Flight Recorder wizard
 */
public class JfrWizardTest extends MCJemmyTestBase {

	@Rule
	public MCUITestRule testRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void after() {
			MCMenu.closeActiveEditor();
		}
	};

	@ClassRule
	public static MCUITestRule classTestRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void before() {
			Assume.assumeTrue("This feature is only valid on JDK7u40 or later.",
					ConnectionHelper.is7u40orLater(TEST_CONNECTION));
		}
	};

	/**
	 * Verifies that setting minimal recording settings in the wizard also is in effect in the
	 * actual recording. Note that this test requires the test VM not have any other concurrent
	 * recording running as recording settings for different recording result in a union setting in
	 * each of the recordings
	 */
	@Test
	public void verifyMinimalRecordingEventSettings() {
		JfrWizard recordingWizard = MC.jvmBrowser.startFlightRecordingWizard();
		recordingWizard.setDurationText("1 s");

		// Disable all events except "Flight Recorder|Recording Setting"
		recordingWizard.disableEvent("Flight Recorder");
		recordingWizard.disableEvent("Java Application");
		recordingWizard.disableEvent("Java Virtual Machine");
		recordingWizard.disableEvent("Operating System");
		recordingWizard.enableEvent("Flight Recorder", "Recording Setting");

		// Get the current settings (in the wizard)
		EventSettingsData wizardEventSettings = recordingWizard.getCurrentEventSettings();

		// Do the recording
		recordingWizard.startAndWaitForRecordingEditor();

		// Verify after finishing the wizard (to not leave it open in case of error)
		Assert.assertTrue("Failed to set minimal event settings in the Flight Recording wizard",
				verifyOnlyRecordingSettingEventEnabled(wizardEventSettings));

		// Get the settings from the opened recording file
		EventSettingsData recordingEventSettings = JfrUi.parseEventSettingsTable();
		Assert.assertTrue("The recording did not contain the minimal event settings set in the Flight Recording wizard",
				verifyOnlyRecordingSettingEventEnabled(recordingEventSettings));
	}

	/**
	 * Verifies that using one of the default recording templates result in a matching recording (settings wise)
	 */
	@Test
	public void verifyDefaultRecordingEventSettings() {
		JfrWizard recordingWizard = MC.jvmBrowser.startFlightRecordingWizard();
		recordingWizard.setDurationText("1 s");
		// choose one of the default templates (with default settings)
		for (String template : recordingWizard.getTemplates()) {
			if (!template.endsWith("- last started")) {
				recordingWizard.chooseTemplate(template);
				break;
			}
		}
		// Get the current settings (in the wizard)
		EventSettingsData wizardEventSettings = recordingWizard.getCurrentEventSettings();

		// Do the recording
		recordingWizard.startAndWaitForRecordingEditor();

		// Get the settings from the opened recording file
		EventSettingsData recordingEventSettings = JfrUi.parseEventSettingsTable();

		// Compare the settings
		Assert.assertTrue(
				"Settings differ between what was set in the wizard and what is actually present in the recording",
				recordingEventSettings.canBeResultOf(wizardEventSettings, false, IS_JFR_NEXT));
	}

	/**
	 * Verifies that a default recording template reflects the last run recording template 
	 */
	@Test
	public void testOneTemplateAddedAfterStartingRecording() {
		String recordingName = "TemplateAdditionTest";
		String dynamicTemplateName = "Settings for '" + recordingName + "' - last started";
		JfrWizard recordingWizard = MC.jvmBrowser.startFlightRecordingWizard();
		List<String> initialListOfTemplates = recordingWizard.getTemplatesInDropdown();
		recordingWizard.setRecordingName(recordingName);
		recordingWizard.setDurationText("1 s");
		recordingWizard.startAndWaitForRecordingEditor();
		recordingWizard = MC.jvmBrowser.startFlightRecordingWizard();
		List<String> laterListOfTemplates = recordingWizard.getTemplatesInDropdown();
		recordingWizard.cancelWizard();

		// Assertions
		Assert.assertFalse("Template '" + dynamicTemplateName + "' was already in the list!",
				initialListOfTemplates.contains(dynamicTemplateName));
		Assert.assertTrue(
				"Name of added template was not correct. Expected '" + dynamicTemplateName
						+ "' in the list but this was found: " + laterListOfTemplates.toString(),
				laterListOfTemplates.contains(dynamicTemplateName));
	}

	private boolean verifyOnlyRecordingSettingEventEnabled(EventSettingsData eventSettings) {
		List<String> events = eventSettings.getAllEventNames();
		boolean recordingSettingEnabled = false;
		boolean incorrectEventSetting = true;
		for (String event : events) {
			if (event.equals("Recording Setting")) {
				if (eventSettings.getLatest(event).getSetting(EventSettingsData.SETTING_ENABLED).equals("true")) {
					recordingSettingEnabled = true;
				} else {
					System.out.println("The event '" + event + "' was not enabled, but should be.");
					incorrectEventSetting = false;
				}
			} else {
				if (eventSettings.getLatest(event).getSetting(EventSettingsData.SETTING_ENABLED).equals("true")) {
					System.out.println("The event '" + event + "' was enabled, but should not be.");
					incorrectEventSetting = false;
				}
			}
		}
		if (events.size() == 0) {
			System.out.println("No event settings found in the provided EventSettingsData");
		}
		return recordingSettingEnabled && incorrectEventSetting;
	}
}
