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
package org.openjdk.jmc.test.jemmy.misc.wrappers;

import static org.openjdk.jmc.common.unit.UnitLookup.MILLISECOND;
import static org.openjdk.jmc.common.unit.UnitLookup.TIMESPAN;

import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Shell;
import org.jemmy.control.Wrap;
import org.jemmy.interfaces.Parent;
import org.jemmy.lookup.Any;
import org.jemmy.resources.StringComparePolicy;
import org.jemmy.swt.ComboWrap;
import org.junit.Assert;

import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;
import org.openjdk.jmc.test.jemmy.misc.helpers.EventSettingsData;
import org.openjdk.jmc.test.jemmy.misc.helpers.EventSettingsData.EventSettings;

/**
 * The Jemmy wrapper for the Flight Recorder wizard. This is used for starting recording as well as
 * editing running recordings.
 */
public class JfrWizard extends MCJemmyBase {
	private static final String FOLDER_EVENT_SETTINGS_TREE_ITEM_TEXT = "Flight Recorder";
	private EventSettingsData eventSettings;
	private MCDialog wizardDialog;
	public static final String EDIT_RECORDING_WIZARD_PAGE_TITLE = org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages.EDIT_RECORDING_WIZARD_PAGE_TITLE;
	public static final String START_RECORDING_WIZARD_PAGE_TITLE = org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages.START_RECORDING_WIZARD_PAGE_TITLE;
	private static final String ENABLED_BUTTON_NAME = org.openjdk.jmc.flightrecorder.controlpanel.ui.recordingconfiguration.EventConfigurationPart.RECORDINGTEMPLATEPART_PREFIX
			+ "Enabled"; // org.openjdk.jmc.flightrecorder.controlpanel.ui.recordingconfiguration.PropertyContentBuilder
	private static final String STACKTRACE_BUTTON_NAME = org.openjdk.jmc.flightrecorder.controlpanel.ui.recordingconfiguration.EventConfigurationPart.RECORDINGTEMPLATEPART_PREFIX
			+ "Stack Trace"; // org.openjdk.jmc.flightrecorder.controlpanel.ui.recordingconfiguration.PropertyContentBuilder
	private static final String RECORDINGWIZARD_DURATION = org.openjdk.jmc.flightrecorder.controlpanel.ui.wizards.RecordingWizardPage.COMPONENT_ID
			+ ".duration"; // org.openjdk.jmc.flightrecorder.controlpanel.ui.wizards.RecordingWizardPage.createDurationText()
	private static final String EVENT_PERIOD = org.openjdk.jmc.flightrecorder.controlpanel.ui.recordingconfiguration.EventConfigurationPart.RECORDINGTEMPLATEPART_PREFIX
			+ "Period";
	private static final String EVENT_THRESHOLD = org.openjdk.jmc.flightrecorder.controlpanel.ui.recordingconfiguration.EventConfigurationPart.RECORDINGTEMPLATEPART_PREFIX
			+ "Threshold";
	private static final String BUTTON_CONTINUOUS_RECORDING_TEXT = org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages.BUTTON_CONTINUOUS_RECORDING_TEXT;
	private static final String BUTTON_TIME_FIXED_RECORDING_TEXT = org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages.BUTTON_TIME_FIXED_RECORDING_TEXT;

	/**
	 * Get a wrapper for the flight recording wizard (using one of the two known titles)
	 */
	public JfrWizard() {
		wizardDialog = MCDialog.getByAnyDialogTitle(START_RECORDING_WIZARD_PAGE_TITLE,
				EDIT_RECORDING_WIZARD_PAGE_TITLE);
	}

	/**
	 * Get a wrapper for the flight recording wizard
	 * 
	 * @param wizardTitle
	 *            the title of the wizard shell
	 */
	public JfrWizard(String wizardTitle) {
		wizardDialog = new MCDialog(wizardTitle);
	}

	/**
	 * @return the eventSettingsTree
	 */
	private MCTree getEventSettingsTree() {
		return MCTree.getFirst(wizardDialog.getDialogShell());
	}

	/**
	 * @return the templateManagerButton
	 */
	private MCButton getTemplateManagerButton() {
		return MCButton.getByLabel(wizardDialog.getDialogShell(), "Template Manager", false);
	}

	/**
	 * @return the backButton
	 */
	private MCButton getBackButton() {
		return MCButton.getByLabel(wizardDialog.getDialogShell(), "< &Back", false);
	}

	/**
	 * @return the nextButton
	 */
	private MCButton getNextButton() {
		return MCButton.getByLabel(wizardDialog.getDialogShell(), "&Next >", false);
	}

	/**
	 * Ensures that the continuous recording option is set
	 */
	public void ensureContinuousRecording() {
		ensurePage(WizardPage.FIRST);
		MCButton.getByLabel(wizardDialog.getDialogShell(), BUTTON_CONTINUOUS_RECORDING_TEXT, false).setState(true);
	}

	/**
	 * Ensures that the time fixed recording option in set
	 */
	public void ensureTimeFixedRecording() {
		ensurePage(WizardPage.FIRST);
		MCButton.getByLabel(wizardDialog.getDialogShell(), BUTTON_TIME_FIXED_RECORDING_TEXT, false).setState(true);
	}

	/**
	 * Sets the duration string
	 *
	 * @param time
	 *            the running time for the recording
	 */
	public void setDurationText(String time) {
		ensurePage(WizardPage.FIRST);
		MCText.getByName(wizardDialog.getDialogShell(), RECORDINGWIZARD_DURATION).setText(time);
	}

	/**
	 * @return the duration string
	 */
	public String getDurationText() {
		ensurePage(WizardPage.FIRST);
		return MCText.getByName(wizardDialog.getDialogShell(), RECORDINGWIZARD_DURATION).getText();
	}

	/**
	 * Sets the recording name
	 * 
	 * @param name
	 *            the name of the recording
	 */
	public void setRecordingName(String name) {
		ensurePage(WizardPage.FIRST);
		MCText.getByToolTip(wizardDialog, "Recording name").setText(name);
	}

	/**
	 * @return the name of the recording
	 */
	public String getRecordingName() {
		ensurePage(WizardPage.FIRST);
		return MCText.getByToolTip(wizardDialog, "Recording name").getText();
	}

	/**
	 * @return the names of the templates in the template drop down
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public List<String> getTemplatesInDropdown() {
		ensurePage(WizardPage.FIRST);
		Wrap<? extends Combo> templateComboWrap = wizardDialog.getWrap().as(Parent.class, Shell.class)
				.lookup(Combo.class, new Any()).wrap();
		ComboWrap cw = (ComboWrap) templateComboWrap;
		List<String> currentList = cw.getStates();
		return currentList;
	}

	/**
	 * Opens the flight recorder template manager
	 * 
	 * @return a {@link MCTemplateManager}
	 */
	public MCTemplateManager openTemplateManager() {
		ensurePage(WizardPage.FIRST);
		getTemplateManagerButton().click();
		return new MCTemplateManager();
	}

	/**
	 * @return the name of the file the recording will get (when finished)
	 */
	public String getFileName() {
		String[] pathTokens = getCompleteFilePath().replace("\\", "/").split("/");
		return pathTokens[pathTokens.length - 1];
	}

	/**
	 * @return the full path of the name of the file the recording will get
	 */
	public String getCompleteFilePath() {
		ensurePage(WizardPage.FIRST);
		return MCText.getByText(wizardDialog.getDialogShell(), ".jfr", StringComparePolicy.SUBSTRING).getText();
	}

	/**
	 * @param newValue
	 *            the desired event threshold value
	 * @param path
	 *            the path of the event
	 * @return an {@link EventSettings} object for the modified event holding all settings
	 */
	public EventSettings setEventThreshold(String newValue, String ... path) {
		return setEventTextSetting(newValue, EVENT_THRESHOLD, path);
	}

	/**
	 * @param path
	 *            the path of the event
	 * @return the event threshold setting.
	 */
	public String getEventThreshold(String ... path) {
		return getEventTextSetting(EVENT_THRESHOLD, path);
	}

	/**
	 * @param newValue
	 *            the desired event threshold value
	 * @param path
	 *            the path of the event
	 * @return an {@link EventSettings} object for the modified event holding all settings
	 */
	public EventSettings setEventPeriod(String newValue, String ... path) {
		return setEventTextSetting(newValue, EVENT_PERIOD, path);
	}

	/**
	 * @param path
	 *            the path of the event
	 * @return the event threshold setting.
	 */
	public String getEventPeriod(String ... path) {
		return getEventTextSetting(EVENT_PERIOD, path);
	}

	/**
	 * @param path
	 *            the path of the event
	 * @return the stack trace setting. {@code true} or {@code false}
	 */
	public boolean getStackTraceState(String ... path) {
		return setEventSetting(null, STACKTRACE_BUTTON_NAME, path);
	}

	/**
	 * Enable the stack trace setting
	 * 
	 * @param path
	 *            the path of the event
	 * @return the stack trace setting prior to enabling ({@code true} or {@code false})
	 */
	public boolean enableStackTrace(String ... path) {
		return setEventSetting(true, STACKTRACE_BUTTON_NAME, path);
	}

	/**
	 * Disable the stack trace setting
	 * 
	 * @param path
	 *            the path of the event
	 * @return the stack trace setting prior to disabling ({@code true} or {@code false})
	 */
	public boolean disableStackTrace(String ... path) {
		return setEventSetting(false, STACKTRACE_BUTTON_NAME, path);
	}

	/**
	 * @param path
	 *            the path of the event
	 * @return the recording setting of the event
	 */
	public boolean getEventState(String ... path) {
		return setEventSetting(null, ENABLED_BUTTON_NAME, path);
	}

	/**
	 * Enable the recording of the event
	 * 
	 * @param path
	 *            the path of the event
	 * @return the event setting prior to enabling ({@code true} or {@code false})
	 */
	public boolean enableEvent(String ... path) {
		return setEventSetting(true, ENABLED_BUTTON_NAME, path);
	}

	/**
	 * Disable the recording of the event
	 * 
	 * @param path
	 *            the path of the event
	 * @return the event setting prior to disabling ({@code true} or {@code false})
	 */
	public boolean disableEvent(String ... path) {
		return setEventSetting(false, ENABLED_BUTTON_NAME, path);
	}

	/**
	 * Selects the supplied recording template name from the Combo
	 *
	 * @param templateName
	 *            the name of the template
	 */
	public void chooseTemplate(String templateName) {
		MCCombo profileCombo = getProfileCombo();
		profileCombo.select(templateName);
	}

	/**
	 * @return all available templates in the Combo
	 */
	public List<String> getTemplates() {
		MCCombo profileCombo = getProfileCombo();
		return profileCombo.getStates();
	}

	/**
	 * Starts the recording by clicking finish and waits for the recording to be done and opened
	 * before returning
	 */
	public void startAndWaitForRecordingEditor() {
		startRecording(true);
	}

	/**
	 * Starts the recording by clicking finish
	 */
	public void startRecording() {
		startRecording(false);
	}

	private void startRecording(boolean waitForEditor) {
		String fileName = null;
		String durationText = null;
		if (waitForEditor) {
			durationText = getDurationText();
			fileName = getFileName();
		}
		wizardDialog.closeWithButton(MCButton.Labels.FINISH);
		if (waitForEditor) {
			/*
			 * Will try to sleep for the recording duration in order not to time out the wait (which
			 * has a default time out of 10s)
			 */
			try {
				sleep(TIMESPAN.parseInteractive(durationText).longValueIn(MILLISECOND));
			} catch (QuantityConversionException e) {
				System.out.println(
						"Could not parse the duration text so sleeping for that period of time failed (the recording should not have been able to start with the duration of \""
								+ durationText + "\")");
				e.printStackTrace();
			}
			Assert.assertTrue("Could not find JFR editor for file \"" + fileName + "\"",
					waitForSubstringMatchedEditor(30000, fileName));
		}
	}

	/**
	 * Cancels the flight recorder wizard (without starting a recording)
	 */
	public void cancelWizard() {
		wizardDialog.closeWithButton(MCButton.Labels.CANCEL);
	}

	/**
	 * Parses the event settings and returns an EventSettingsData object
	 *
	 * @return the {@link EventSettingsData} object with the current settings
	 */
	public EventSettingsData getCurrentEventSettings() {
		ensurePage(WizardPage.LAST);
		eventSettings = new EventSettingsData();
		MCTree eventSettingsTree = getEventSettingsTree();
		eventSettingsTree.select(true, FOLDER_EVENT_SETTINGS_TREE_ITEM_TEXT);
		Image folderImage = eventSettingsTree.fetchImageFromSelectedTreeItem();

		int currentIndex = 0;
		List<List<String>> paths = eventSettingsTree.getAllItemTexts();

		while (currentIndex < paths.size()) {
			String[] eventPath = paths.get(currentIndex).toArray(new String[0]);
			eventSettingsTree.select(true, eventPath);
			String itemName = eventSettingsTree.getState();
			if (eventSettingsTree.fetchImageFromSelectedTreeItem().equals(folderImage)) {
				eventSettingsTree.expand();
				paths = eventSettingsTree.getAllItemTexts();
			} else {
				addCurrentlySelectedEventToEventSettingsData(itemName, eventSettings, eventPath);
				currentIndex++;
			}
		}
		return eventSettings;
	}

	/**
	 * Selects the event specified by the path and returns an EventSettings with all settings
	 *
	 * @param eventPath
	 *            the path of the event to read
	 * @return an {@link EventSettings} object holding the settings of the event
	 */
	public EventSettings getEventSettings(String[] eventPath) {
		ensurePage(WizardPage.LAST);
		getEventSettingsTree().select(eventPath);
		String eventName = getEventSettingsTree().getState();
		EventSettingsData eventSettingsData = new EventSettingsData();
		addCurrentlySelectedEventToEventSettingsData(eventName, eventSettingsData, eventPath);
		return eventSettingsData.getLatest(eventName);
	}

	private MCCombo getProfileCombo() {
		ensurePage(WizardPage.FIRST);
		return MCCombo.getVisible(wizardDialog.getDialogShell(), true).get(0);
	}

	private boolean setEventSetting(Boolean desiredState, String settingName, String ... path) {
		ensurePage(WizardPage.LAST);
		getEventSettingsTree().select(path);
		MCButton button = MCButton.getByName(wizardDialog.getDialogShell(), settingName);
		boolean currentState = button.getSelection();
		if (desiredState != null && desiredState != currentState) {
			button.setState(desiredState);
		}
		return currentState;
	}

	private EventSettings setEventTextSetting(String desiredText, String settingName, String ... path) {
		ensurePage(WizardPage.LAST);
		getEventSettingsTree().select(path);
		MCText textField = MCText.getVisibleByName(wizardDialog.getDialogShell(), settingName, 1000);
		if (textField != null) {
			if (desiredText != textField.getText()) {
				textField.setText(desiredText);
			}
		} else {
			MCCombo combo = MCCombo.getVisibleByName(wizardDialog.getDialogShell(), settingName, 0);
			if (desiredText != combo.getText()) {
				combo.setText(desiredText);
			}
		}
		return getEventSettings(path);
	}

	private String getEventTextSetting(String settingName, String ... path) {
		ensurePage(WizardPage.LAST);
		getEventSettingsTree().select(path);
		MCText textField = MCText.getVisibleByName(wizardDialog.getDialogShell(), settingName, 1000);
		if (textField == null) {
			return MCCombo.getVisibleByName(shell, settingName, 0).getText();
		} else {
			return textField.getText();
		}
	}

	private void ensurePage(WizardPage desiredPage) {
		// We do a maximum of 5 navigational retries before failing
		int maxRetries = 5;

		while (desiredPage.getPage() != getCurrentPage() && maxRetries > 0) {
			boolean forward = (getCurrentPage() - desiredPage.getPage() > 0) ? false : true;
			if (forward) {
				getNextButton().click();
			} else {
				getBackButton().click();
			}
			maxRetries--;
		}
		Assert.assertTrue("Could not navigate to page " + desiredPage.getPage(), maxRetries > 0);
	}

	private int getCurrentPage() {
		if (getNextButton().isEnabled() && getBackButton().isEnabled()) {
			return 2;
		} else if (getNextButton().isEnabled()) {
			return 1;
		} else {
			return 3;
		}
	}

	private void addCurrentlySelectedEventToEventSettingsData(
		String eventName, EventSettingsData eventSettings, String[] eventPath) {
		List<MCButton> buttons = MCButton.getVisible(wizardDialog.getDialogShell(), false);
		List<MCText> texts = MCText.getVisible(wizardDialog.getDialogShell(), false);
		List<MCCombo> combos = MCCombo.getVisible(wizardDialog.getDialogShell(), false, 50);

		for (MCButton button : buttons) {
			String name = button.getName();
			if (name != null) {
				switch (name) {
				case ENABLED_BUTTON_NAME:
					eventSettings.add(eventName, eventPath, EventSettingsData.SETTING_ENABLED,
							((Boolean) button.getSelection()).toString());
					break;
				case STACKTRACE_BUTTON_NAME:
					eventSettings.add(eventName, eventPath, EventSettingsData.SETTING_STACKTRACE,
							((Boolean) button.getSelection()).toString());
				}
			}
		}

		for (MCText text : texts) {
			String name = text.getName();
			if (name != null) {
				switch (name) {
				case EVENT_PERIOD:
					eventSettings.add(eventName, eventPath, EventSettingsData.SETTING_PERIOD, text.getText());
					break;
				case EVENT_THRESHOLD:
					eventSettings.add(eventName, eventPath, EventSettingsData.SETTING_THRESHOLD, text.getText());
				}
			}
		}
		for (MCCombo combo : combos) {
			String name = combo.getName();
			if (name != null) {
				switch (name) {
				case EVENT_PERIOD:
					eventSettings.add(eventName, eventPath, EventSettingsData.SETTING_PERIOD, combo.getText());
					break;
				case EVENT_THRESHOLD:
					eventSettings.add(eventName, eventPath, EventSettingsData.SETTING_THRESHOLD, combo.getText());
				}
			}
		}
	}

	private enum WizardPage {
		FIRST(1), SECOND(2), LAST(3);

		private int pageNumber;

		WizardPage(int pageNumber) {
			this.pageNumber = pageNumber;
		}

		public int getPage() {
			return pageNumber;
		}
	}
}
