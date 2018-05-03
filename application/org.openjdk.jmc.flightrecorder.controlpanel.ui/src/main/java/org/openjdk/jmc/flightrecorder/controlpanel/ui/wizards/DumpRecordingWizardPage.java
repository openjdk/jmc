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
package org.openjdk.jmc.flightrecorder.controlpanel.ui.wizards;

import static org.openjdk.jmc.common.unit.UnitLookup.EPOCH_MS;
import static org.openjdk.jmc.common.unit.UnitLookup.TIMESPAN;

import java.util.logging.Level;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.ControlPanel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.ImageConstants;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.RecordingProvider;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.jobs.DumpRecordingJob;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.preferences.PreferenceKeys;
import org.openjdk.jmc.rjmx.ConnectionException;
import org.openjdk.jmc.ui.common.idesupport.IDESupportToolkit;
import org.openjdk.jmc.ui.common.resource.MCFile;
import org.openjdk.jmc.ui.misc.DateTimeChooser;
import org.openjdk.jmc.ui.misc.QuantityKindProposal;
import org.openjdk.jmc.ui.wizards.IPerformFinishable;

/**
 * Wizard page for obtaining information to dump an recording existing on a server. Lets the user
 * choose where to locally save the recording and choose to get the whole recording or part of it.
 * <p>
 * Getters for values set by wizard (path, start and end time) are only valid when the wizard has
 * been closed correctly.
 */
public class DumpRecordingWizardPage extends WizardPage implements IPerformFinishable {

	private Text m_filenameText;
	private Button m_wholeRadioButton;
	private Button m_lastPartRadioButton;
	private Label m_lastPartLabel;
	private Text m_lastPartText;
	private Button m_intervalRadioButton;
	private Label m_startTimeLabel;
	private DateTimeChooser m_startTimeChooser;
	private Label m_endTimeLabel;
	private DateTimeChooser m_endTimeChooser;

	private final DumpRecordingWizardModel wizardModel;

	private Button m_lastPartDefaultCheckbox;

	/**
	 * Creates a new wizard page for given recording.
	 *
	 * @param dumpRecordingWizardModel
	 *            the recording to eventually dump
	 * @throws ConnectionException
	 */
	public DumpRecordingWizardPage(DumpRecordingWizardModel dumpRecordingWizardModel) {
		super("dumpRecording", Messages.DUMP_RECORDING_WIZARD_PAGE_TITLE, //$NON-NLS-1$
				ControlPanel.getDefault().getMCImageDescriptor(ImageConstants.IMAGE_WIZARD_START_RECORDING));
		wizardModel = dumpRecordingWizardModel;
	}

	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		int indent = convertWidthInCharsToPixels(4);

		setDescription(Messages.DUMP_RECORDING_WIZARD_PAGE_DESCRIPTION);
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		// column 1 - radio buttons, controls and control labels(indented)
		// column 2 - controls
		// column 3 - extra controls
		layout.numColumns = 3;
		container.setLayout(layout);

		createFileNameInput(container, indent);
		createWholeRecordingOption(container, indent);
		createLastPartRecordingOption(container, indent);
		createIntervalRecordingOption(container, indent);
		createDivider(container);
		createLastPartDefaultCheckbox(container);

		selectInitialOption();
		updateFilename();

		setControl(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.DUMP_RECORDING);
	}

	@Override
	public void performHelp() {
		PlatformUI.getWorkbench().getHelpSystem().displayHelp(IHelpContextIds.DUMP_RECORDING);
	}

	private void selectInitialOption() {
		if (FlightRecorderUI.getDefault().isSetDumpWhole()) {
			m_wholeRadioButton.setSelection(true);
			setLastPartControlsEnabled(false);
			setIntervalControlsEnabled(false);
		} else {
			m_lastPartRadioButton.setSelection(true);
			setLastPartControlsEnabled(true);
			setIntervalControlsEnabled(false);
		}
	}

	private void createFileNameInput(Composite parent, int indent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.LABEL_FILENAME_TEXT);
		label.setLayoutData(createGridData(false, indent));

		m_filenameText = new Text(parent, SWT.READ_ONLY | SWT.BORDER);
		m_filenameText.setLayoutData(createGridData(true));

		Button browseButton = createFilenameBrowseButton(parent);
		browseButton.setLayoutData(createGridData(false));
	}

	private Button createFilenameBrowseButton(Composite parent) {
		final Button button = new Button(parent, SWT.NONE);
		button.setText(Messages.BUTTON_BROWSE_TEXT);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				MCFile path = ControlPanel.openRecordingFileBrowser(wizardModel.getPath());
				if (path != null) {
					wizardModel.setPath(path);
					updateFilename();
				}
				button.setFocus();
			}

		});
		return button;
	}

	/**
	 * Updates the file name text box with the currently selected path.
	 */
	private void updateFilename() {
		m_filenameText.setText(wizardModel.getPath().getPath());
		validatePage();
	}

	private void createWholeRecordingOption(Composite parent, int indent) {
		m_wholeRadioButton = new Button(parent, SWT.RADIO);
		m_wholeRadioButton.setText(Messages.BUTTON_WHOLE_RECORDING_TEXT);
		m_wholeRadioButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setLastPartControlsEnabled(false);
				setDefaultControlsEnabled(true);
				setIntervalControlsEnabled(false);
			}
		});
		m_wholeRadioButton.setLayoutData(createGridData(false, 2, 0));
		createPadding(parent, 1);
	}

	private void createLastPartRecordingOption(Composite parent, int indent) {
		m_lastPartRadioButton = new Button(parent, SWT.RADIO);
		m_lastPartRadioButton.setText(Messages.BUTTON_LAST_PART_OF_RECORDING_TEXT);
		m_lastPartRadioButton.setLayoutData(createGridData(false, 2, 0));
		m_lastPartRadioButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setLastPartControlsEnabled(true);
				setDefaultControlsEnabled(true);
				setIntervalControlsEnabled(false);
			}
		});
		createPadding(parent, 1);

		m_lastPartLabel = createLabel(parent, Messages.LABEL_TIMESPAN);
		m_lastPartLabel.setLayoutData(createGridData(false, indent));

		m_lastPartText = new Text(parent, SWT.BORDER);
		QuantityKindProposal.install(m_lastPartText, UnitLookup.TIMESPAN);
		m_lastPartText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				validatePage();
			}
		});
		m_lastPartText.setLayoutData(createGridData(true));
		createPadding(parent, 1);
	}

	private void validatePage() {
		String lastPartError = FlightRecorderUI.validateDumpTimespan(m_lastPartText.getText());
		if (lastPartError != null) {
			setMessage(lastPartError, IMessageProvider.ERROR);
			setPageComplete(false);
		} else {
			IStatus validation = IDESupportToolkit.validateFileResourcePath(wizardModel.getPath().getPath());
			if (validation.getSeverity() == IStatus.ERROR) {
				setMessage(validation.getMessage(), IMessageProvider.ERROR);
				setPageComplete(false);
			} else {
				setMessage(ControlPanel.getRecordingFileValidationMessage(validation), IMessageProvider.WARNING);
				setPageComplete(true);
			}
		}
	}

	private static void createDivider(Composite container) {
		Label divider = new Label(container, SWT.HORIZONTAL | SWT.SEPARATOR);
		divider.setLayoutData(createGridData(true, 3, 0));
	}

	private void createLastPartDefaultCheckbox(Composite parent) {
		m_lastPartDefaultCheckbox = new Button(parent, SWT.CHECK);
		m_lastPartDefaultCheckbox.setText(Messages.DUMP_RECORDING_SAVE_LAST_PART_TO_DUMP);
		m_lastPartDefaultCheckbox.setLayoutData(createGridData(true, 2, 0));
	}

	/**
	 * Sets the enabled state for all last part controls.
	 *
	 * @param enabled
	 *            the new enabled state
	 */
	private void setLastPartControlsEnabled(boolean enabled) {
		m_lastPartText.setEnabled(enabled);
		m_lastPartLabel.setEnabled(enabled);
		if (FlightRecorderUI.validateDumpTimespan(m_lastPartText.getText()) != null) {
			m_lastPartText.setText(FlightRecorderUI.getDefault().getLastPartToDumpTimespan().interactiveFormat());
		}
	}

	private void setDefaultControlsEnabled(boolean enabled) {
		m_lastPartDefaultCheckbox.setEnabled(enabled);
	}

	private void createIntervalRecordingOption(Composite parent, int indent) {
		createIntervalRecordingRadioButton(parent);
		createStartTimeChooser(parent, indent);
		createEndTimeChooser(parent, indent);
		m_startTimeChooser.setConstraint(timestamp -> Math.min(timestamp, m_endTimeChooser.getTimestamp()));
		m_endTimeChooser.setConstraint(timestamp -> Math.max(timestamp, m_startTimeChooser.getTimestamp()));
	}

	private void createIntervalRecordingRadioButton(Composite parent) {
		m_intervalRadioButton = new Button(parent, SWT.RADIO);
		m_intervalRadioButton.setText(Messages.BUTTON_INTERVAL_OF_RECORDING_TEXT);
		m_intervalRadioButton.setLayoutData(createGridData(false, 2, 0));
		m_intervalRadioButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setIntervalControlsEnabled(true);
				setLastPartControlsEnabled(false);
				setDefaultControlsEnabled(false);
			}
		});
		createPadding(parent, 1);
	}

	private void createStartTimeChooser(Composite parent, int indent) {
		m_startTimeLabel = createLabel(parent, Messages.LABEL_START_TIME_TEXT);
		m_startTimeLabel.setLayoutData(createGridData(false, indent));
		m_startTimeChooser = new DateTimeChooser(parent, SWT.NONE);
		m_startTimeChooser.setTimestamp(wizardModel.getRecordingStartTime().clampedLongValueIn(EPOCH_MS));
		m_startTimeChooser.setLayoutData(createGridData(true));
		createPadding(parent, 1);
	}

	private void createEndTimeChooser(Composite parent, int indent) {
		m_endTimeLabel = createLabel(parent, Messages.LABEL_END_TIME_TEXT);
		m_endTimeLabel.setLayoutData(createGridData(false, indent));
		m_endTimeChooser = new DateTimeChooser(parent, SWT.NONE);
		m_endTimeChooser.setTimestamp(wizardModel.recordingEndTime().clampedLongValueIn(EPOCH_MS));
		m_endTimeChooser.setLayoutData(createGridData(true));
		createPadding(parent, 1);
	}

	/**
	 * Sets the enabled state for all interval controls.
	 *
	 * @param enabled
	 *            the new enabled state
	 */
	private void setIntervalControlsEnabled(boolean enabled) {
		m_startTimeLabel.setEnabled(enabled);
		m_startTimeChooser.setEnabled(enabled);
		m_endTimeLabel.setEnabled(enabled);
		m_endTimeChooser.setEnabled(enabled);
	}

	/**
	 * Helper method to create a label with given text and adding it to parent composite.
	 *
	 * @param parent
	 *            the parent composite
	 * @param text
	 *            text message for the label
	 * @return the created label
	 */
	private static Label createLabel(Composite parent, String text) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(text);
		return label;
	}

	/**
	 * Helper method to create a padding widget with a given horizontal span.
	 *
	 * @param parent
	 *            the parent composite
	 * @param horizontalSpan
	 *            the number of columns the padding should span
	 */
	private static void createPadding(Composite parent, int horizontalSpan) {
		Label label = new Label(parent, SWT.NONE);
		label.setLayoutData(createGridData(false, horizontalSpan, 0));
	}

	/**
	 * Helper method to create grid data layout object that spans one column and is not indented.
	 *
	 * @param grabExcessHorizontalSpace
	 *            whether to grab excess horizontal space or not
	 * @return the grid data layout object
	 */
	private static GridData createGridData(boolean grabExcessHorizontalSpace) {
		return createGridData(grabExcessHorizontalSpace, 1, 0);
	}

	/**
	 * Helper method to create grid data layout object that spans one column.
	 *
	 * @param grabExcessHorizontalSpace
	 *            whether to grab excess horizontal space or not
	 * @param indent
	 *            the indention to use in number of pixels
	 * @return the grid data layout object
	 */
	private static GridData createGridData(boolean grabExcessHorizontalSpace, int indent) {
		return createGridData(grabExcessHorizontalSpace, 1, indent);
	}

	/**
	 * Helper method to create grid data layout object.
	 *
	 * @param grabExcessHorizontalSpace
	 *            whether to grab excess horizontal space or not
	 * @param horizontalSpan
	 *            the number of columns the padding should span
	 * @param indent
	 *            the indention to use in number of pixels
	 * @return the grid data layout object
	 */
	private static GridData createGridData(boolean grabExcessHorizontalSpace, int horizontalSpan, int indent) {
		GridData gridData = new GridData(SWT.FILL, SWT.CENTER, grabExcessHorizontalSpace, false);
		gridData.horizontalSpan = horizontalSpan;
		gridData.horizontalIndent = indent;
		return gridData;
	}

	@Override
	public boolean performFinish() {
		RecordingProvider recDesc = wizardModel.getRecordingProvider();
		if (m_lastPartText.isEnabled()) {
			try {
				IQuantity timespan = TIMESPAN.parseInteractive(m_lastPartText.getText());
				if (m_lastPartDefaultCheckbox.getSelection()) {
					IPreferenceStore preferenceStore = FlightRecorderUI.getDefault().getPreferenceStore();
					preferenceStore.setValue(PreferenceKeys.PROPERTY_DEFAULT_DUMP_TIMESPAN,
							timespan.persistableString());
					preferenceStore.setValue(PreferenceKeys.PROPERTY_DEFAULT_DUMP_TYPE, PreferenceKeys.DUMP_TIMESPAN);
				}
				new DumpRecordingJob(recDesc, wizardModel.getPath(), timespan).schedule();
			} catch (QuantityConversionException qce) {
				ControlPanel.getDefault().getLogger().log(Level.SEVERE,
						"Dump recording wizard should not be closable if dump timespan is not valid", qce); //$NON-NLS-1$
				return false;
			}
		} else if (m_startTimeChooser.isEnabled()) {
			IQuantity startTime = EPOCH_MS.quantity(m_startTimeChooser.getTimestamp());
			IQuantity endTime = EPOCH_MS.quantity(m_endTimeChooser.getTimestamp());
			new DumpRecordingJob(recDesc, wizardModel.getPath(), startTime, endTime).schedule();
		} else {
			if (m_lastPartDefaultCheckbox.getSelection()) {
				IPreferenceStore preferenceStore = FlightRecorderUI.getDefault().getPreferenceStore();
				preferenceStore.setValue(PreferenceKeys.PROPERTY_DEFAULT_DUMP_TYPE, PreferenceKeys.DUMP_WHOLE);
			}
			new DumpRecordingJob(recDesc, wizardModel.getPath()).schedule();
		}
		return true;
	}

	@Override
	public void dispose() {
		wizardModel.dispose();
		super.dispose();
	}
}
