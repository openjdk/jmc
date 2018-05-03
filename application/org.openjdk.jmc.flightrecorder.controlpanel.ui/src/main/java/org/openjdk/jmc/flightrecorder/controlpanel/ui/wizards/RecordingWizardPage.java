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

import java.util.Arrays;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormText;
import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.unit.IDescribedMap;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.configuration.events.IEventConfiguration;
import org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.ControlPanel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.ImageConstants;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.common.resource.MCFile;
import org.openjdk.jmc.ui.common.util.Environment;
import org.openjdk.jmc.ui.misc.QuantityKindProposal;

/**
 * Wizard page for configuring and starting a recording
 */
public class RecordingWizardPage extends WizardPage {
	// UI Constants
	private static final int VISIBLE_ITEMS_IN_COMBO = 10;

	public static final String COMPONENT_ID = "recordingwizard"; //$NON-NLS-1$

	public static final String PAGE_NAME = "startRecordingWizard"; //$NON-NLS-1$
	public static final String EDIT_PAGE_NAME = "editRecordingWizard"; //$NON-NLS-1$
	private static final int WIZARD_STAGE = 0;

	private TemplateProvider m_comboProvider;
	// UI controls
	private ComboViewer m_comboViewer;
	private Text m_description;
	private Text m_filenameText;
	private Button m_fixedOption;
	private Text m_durationText;
	private Text m_delayText;
	private Button m_continuousOption;
	private Text m_maxSizeText;
	private Text m_maxAgeText;
	private FormText m_infoText;
	protected Text m_nameText;

	private final RecordingWizardModel m_model;

	private final boolean m_displayMaxAge;
	private final boolean m_displayMaxSize;
	private final boolean m_displaySettingsDescription;
	private final boolean m_displayInfo;
	private final boolean m_displayHelp;
	private final boolean m_displayDelay;
	private boolean m_disableSettingsForContinuous;
	private boolean m_disableNameForContinuous;

	private final Observer m_modelObserver = new Observer() {

		@Override
		public void update(Observable o, Object arg) {
			setMessage(getModel().getWarningMessage(), IMessageProvider.WARNING);
			setErrorMessage(getModel().checkForErrors(m_comboProvider.hasExtraTemplate()));
			setPageComplete(getErrorMessage() == null);
			if (arg != null) {
				if (arg.equals(RecordingWizardModel.JRE_VERSION_CHANGED)) {
					m_comboProvider.setVersion(m_model.getVersion());
					refreshTemplateCombo();
				}
			}
		}
	};

	// FIXME: This should be handled by using an IDescribedMap<String> with the options.
	public RecordingWizardPage(RecordingWizardModel model, boolean displayMaxAge, boolean displayMaxSize,
			boolean displaySettingsDescription, boolean displayInfo, boolean displayHelp, boolean displayDelay,
			boolean disableSettingsForContinuous, boolean disableNameForContinuous) {
		super(model.isEditing() ? EDIT_PAGE_NAME : PAGE_NAME);
		m_model = model;
		m_displayMaxAge = displayMaxAge;
		m_displayMaxSize = displayMaxSize;
		m_displaySettingsDescription = displaySettingsDescription;
		m_displayInfo = displayInfo;
		m_displayHelp = displayHelp;
		m_displayDelay = displayDelay;
		m_disableSettingsForContinuous = disableSettingsForContinuous;
		m_disableNameForContinuous = disableNameForContinuous;

		setImageDescriptor(ControlPanel.getDefault().getMCImageDescriptor(ImageConstants.IMAGE_WIZARD_START_RECORDING));

		m_model.addObserver(m_modelObserver);
	}

	public RecordingWizardPage(RecordingWizardModel model) {
		this(model, true, true, true, true, true, false, false, false);
	}

	@Override
	public IWizardPage getNextPage() {
		IWizardPage next;
		if (m_model.getCurrentConfigurationAt(WIZARD_STAGE).hasControlElements()) {
			next = getWizard().getPage(RecordingEventOptionsWizardPage.PAGE_NAME);
		} else {
			next = getWizard().getPage(AdvancedWizardPage.PAGE_NAME);
		}
		return next;
	}

	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		int indent = convertWidthInCharsToPixels(4);

		if (m_model.isEditing()) {
			setTitle(Messages.EDIT_RECORDING_WIZARD_PAGE_TITLE);
			setDescription(Messages.EDIT_RECORDING_WIZARD_PAGE_DESCRIPTION);
		} else {
			setTitle(Messages.START_RECORDING_WIZARD_PAGE_TITLE);
			setDescription(Messages.START_RECORDING_WIZARD_PAGE_DESCRIPTION);
		}
		ScrolledComposite sc = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		Composite container = new Composite(sc, SWT.NONE);
		sc.setContent(container);

		GridLayout layout = new GridLayout();
		container.setLayout(layout);

		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, true, false);
		Composite settingsContainer = createSettingsContainer(container, indent);
		settingsContainer.setLayoutData(gd1);

		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, false);
		Control separator = createSeparator(container);
		separator.setLayoutData(gd2);

		GridData gd3 = new GridData(SWT.FILL, SWT.FILL, true, false);
		Composite templateContainer = createTemplateContainer(container);
		templateContainer.setLayoutData(gd3);

		Composite descriptionContainer = createDescriptionControls(container);
		if (descriptionContainer != null) {
			GridData gdd = new GridData(SWT.FILL, SWT.FILL, true, true);
			descriptionContainer.setLayoutData(gdd);
		}

		FormText infoText = createInfoText(container);
		if (infoText != null) {
			GridData gd4 = new GridData(SWT.FILL, SWT.FILL, true, false);
			infoText.setLayoutData(gd4);
		}

		refreshTemplateCombo();
		selectInitialOption();

		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		setControl(sc);

		/*
		 * Workaround to avoid blank wizard page on Linux (encountered on OEL 7.1). This should be
		 * considered a quick & dirty fix as this may well happen in other dialogs too.
		 */
		if (Environment.getOSType() == Environment.OSType.LINUX) {
			getShell().layout();
		}

		if (m_displayHelp) {
			PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.RECORDING_WIZARD);
		}

	}

	@Override
	public void performHelp() {
		if (m_displayHelp) {
			PlatformUI.getWorkbench().getHelpSystem().displayHelp(IHelpContextIds.RECORDING_WIZARD);
		}
	}

	private FormText createInfoText(Composite parent) {
		if (m_displayInfo) {
			m_infoText = new FormText(parent, SWT.NONE);
			return m_infoText;
		}
		return null;
	}

	void selectInitialOption() {
		boolean fixedRecording = m_model.isFixedRecording();
		setContinuous(!fixedRecording);
	}

	@Override
	public boolean isPageComplete() {
		return getModel().checkForErrors(m_comboProvider.hasExtraTemplate()) == null;
	}

	private void createTemplateControls(Composite parent, int cols) {
		GridData gd0 = new GridData(SWT.FILL, SWT.FILL, false, false);
		Label label = createLabel(parent, Messages.LABEL_TEMPLATE);
		label.setLayoutData(gd0);

		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd1.horizontalSpan = cols - 2;
		m_comboProvider = createTemplateProvider();
		m_comboViewer = createSelector(parent, m_comboProvider);
		m_comboViewer.getControl().setLayoutData(gd1);

		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, false, false);
		Button templateManagerButton = createTemplateManagerButton(parent);
		templateManagerButton.setLayoutData(gd2);

		hookSelectionListener();
	}

	protected TemplateProvider createTemplateProvider() {
		return new TemplateProvider(m_model.getVersion());
	}

	private void hookSelectionListener() {
		m_comboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IEventConfiguration config = getSelectedTemplate();
				if (config == null || m_comboProvider.clearExtraTemplateUnless(config)) {
					m_comboViewer.refresh();
				}
				setDescriptionText(config);

				m_model.setActiveConfigurationTemplate(getSelectedTemplate());
			}
		});
	}

	private Button createTemplateManagerButton(Composite parent) {
		Button button = new Button(parent, SWT.NONE);
		button.setText(Messages.BUTTON_TEMPLATE_MANAGER_TEXT);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openTemplateManager();
			}
		});
		return button;
	}

	public void refreshTemplateCombo() {
		// Currently, we do not show the derived active configuration in the
		// combo box,
		// just the original template it was based on (level 0). So select that.
		IEventConfiguration config = m_model.getCurrentConfigurationAt(WIZARD_STAGE);

		if (config != null && config.isDeletable() && !m_model.getTemplateRepository().contains(config)) {
			m_comboProvider.setExtraTemplate(config);
		} else {
			m_comboProvider.setExtraTemplate(null);
		}
		m_comboViewer.refresh();

		setTemplate(config);
	}

	/**
	 * The currently selected recording configuration template. Note that currently this differs
	 * from the active configuration (which is a working copy of this template).
	 *
	 * @return
	 */
	private IEventConfiguration getSelectedTemplate() {
		IStructuredSelection ss = (IStructuredSelection) m_comboViewer.getSelection();
		return (IEventConfiguration) (!ss.isEmpty() ? ss.getFirstElement() : null);
	}

	private void setDescriptionText(IEventConfiguration rt) {
		if (m_description != null && !m_description.isDisposed()) {
			String description = (rt != null) ? rt.getDescription() : ""; //$NON-NLS-1$
			m_description.setText(description);
			m_description.setToolTipText(description);
		}
	}

	private Composite createSettingsContainer(Composite parent, int indent) {
		Composite container = new Composite(parent, SWT.NONE);
		int cols = 5;
		GridLayout layout = new GridLayout(cols, false);
		layout.horizontalSpacing = 8; // Make room for the content proposal decorator
		container.setLayout(layout);

		createFileNameInput(container, cols);
		createName(container, cols);
		createTimeFixedOptions(container, indent, cols);
		createContinuousOptions(container, indent, cols);

		setContinuous(m_model.isContinuous());

		return container;
	}

	private void createContinuousOptions(Composite container, int indent, int cols) {
		if (shouldHaveContinuousControls()) {
			createContinuousOption(container, cols);
			createMaxSize(container, indent, cols);
			createMaxAge(container, indent, cols);
		}
	}

	private void createFileNameInput(Composite parent, int cols) {
		GridData gd1 = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		Label label = createLabel(parent, Messages.LABEL_FILENAME_TEXT);
		label.setLayoutData(gd1);

		GridData gd2 = new GridData(SWT.FILL, SWT.CENTER, true, true);
		gd2.horizontalSpan = cols - 2;
		m_filenameText = createFilenameText(parent);
		gd2.minimumWidth = 0;
		gd2.widthHint = 400;
		m_filenameText.setLayoutData(gd2);

		GridData gd3 = new GridData(SWT.FILL, SWT.FILL, false, true);
		Button browseButton = createFilenameBrowseButton(parent);
		browseButton.setLayoutData(gd3);

		setFileName(m_model.getPath());
	}

	private Text createFilenameText(Composite parent) {
		Text text = new Text(parent, SWT.READ_ONLY | SWT.BORDER);
		text.setEnabled(false);
		return text;
	}

	private Button createFilenameBrowseButton(Composite parent) {
		final Button button = new Button(parent, SWT.NONE);
		button.setText(Messages.BUTTON_BROWSE_TEXT);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				MCFile path = ControlPanel.openRecordingFileBrowser(getModel().getPath());
				if (path != null) {
					setFileName(path);
					getModel().setPath(path);
				}
				// Setting focus back to the button, otherwise focus
				// will just disappear!
				button.setFocus();
			}
		});
		return button;
	}

	private void createName(Composite parent, int cols) {
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, false, true);
		Label label = createLabel(parent, Messages.LABEL_NAME_TEXT);
		label.setLayoutData(gd1);

		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd2.horizontalSpan = cols - 1;
		m_nameText = createNameText(parent);
		m_nameText.setLayoutData(gd2);
	}

	private Text createNameText(Composite parent) {
		final Text text = new Text(parent, SWT.BORDER);
		findToolTipText(text, RecordingOptionsBuilder.KEY_NAME);
		text.setText(m_model.getName());
		text.setEditable(!m_model.isEditing());
		text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getModel().setName(text.getText());
			}
		});
		return text;
	}

	private void createTimeFixedOptions(Composite parent, int indent, int cols) {
		if (shouldHaveDurationControl()) {
			createTimeFixedOption(parent, cols);
			createDuration(parent, indent, cols);
			createDelay(parent, indent, cols);
		}
	}

	private void createTimeFixedOption(Composite parent, int cols) {
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd1.horizontalSpan = cols;
		m_fixedOption = new Button(parent, SWT.RADIO);
		m_fixedOption.setText(Messages.BUTTON_TIME_FIXED_RECORDING_TEXT);
		m_fixedOption.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (m_fixedOption.getSelection()) {
					setContinuous(false);
					storeContinuous(false);
				}
			}
		});
		m_fixedOption.setLayoutData(gd1);
	}

	private void createDuration(Composite parent, int indent, int cols) {
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, false, true);
		gd1.horizontalIndent = indent;
		Label label = createLabel(parent, Messages.LABEL_RECORDING_TIME_TEXT);
		label.setLayoutData(gd1);

		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd2.horizontalSpan = cols - 1;
		Text text = createDurationText(parent);
		text.setLayoutData(gd2);
	}

	private Text createDurationText(Composite parent) {
		m_durationText = new Text(parent, SWT.BORDER);
		findToolTipText(m_durationText, RecordingOptionsBuilder.KEY_DURATION);
		m_durationText.setText(m_model.getDurationString());
		QuantityKindProposal.install(m_durationText, UnitLookup.TIMESPAN);
		m_durationText.setData("name", COMPONENT_ID + '.' + "duration"); //$NON-NLS-1$ //$NON-NLS-2$
		m_durationText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getModel().setDuration(m_durationText.getText());
			}
		});
		return m_durationText;
	}

	private void createDelay(Composite parent, int indent, int cols) {
		if (m_displayDelay) {
			GridData gd1 = new GridData(SWT.FILL, SWT.FILL, false, true);
			gd1.horizontalIndent = indent;
			Label label = createLabel(parent, Messages.LABEL_DELAY_TIME_TEXT);
			label.setLayoutData(gd1);

			GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, true);
			gd2.horizontalSpan = cols - 1;
			Text text = createDelayText(parent);
			text.setLayoutData(gd2);
		}
	}

	private Text createDelayText(Composite parent) {
		m_delayText = new Text(parent, SWT.BORDER);
		// FIXME: Fix the tooltip for delay.
//		findToolTipText(m_durationText, RecordingOptionsBuilder.KEY_DELAY);
		m_delayText.setText(getModel().getDelayString());
		QuantityKindProposal.install(m_delayText, UnitLookup.TIMESPAN);
		m_delayText.setData("name", COMPONENT_ID + '.' + "delay"); //$NON-NLS-1$ //$NON-NLS-2$
		m_delayText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getModel().setDelay(m_delayText.getText());
			}
		});
		return m_delayText;
	}

	private void createMaxSize(Composite parent, int indent, int cols) {
		if (m_displayMaxSize) {
			GridData gd1 = new GridData(SWT.FILL, SWT.FILL, false, true);
			gd1.horizontalIndent = indent;
			Label label = createLabel(parent, Messages.LABEL_MAX_SIZE);
			label.setLayoutData(gd1);

			GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, true);
			gd2.horizontalSpan = cols - 1;
			Text text = createMaxSizeText(parent);
			text.setLayoutData(gd2);
		}
	}

	private Text createMaxSizeText(Composite parent) {
		m_maxSizeText = new Text(parent, SWT.BORDER);
		m_maxSizeText.setText(m_model.getMaxSizeString());
		QuantityKindProposal.install(m_maxSizeText, UnitLookup.MEMORY);
		findToolTipText(m_maxSizeText, RecordingOptionsBuilder.KEY_MAX_SIZE);
		m_maxSizeText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getModel().setMaxSize(m_maxSizeText.getText());
			}
		});
		return m_maxSizeText;
	}

	private void createMaxAge(Composite parent, int indent, int cols) {
		if (m_displayMaxAge) {
			GridData gd1 = new GridData(SWT.FILL, SWT.FILL, false, true);
			gd1.horizontalIndent = indent;
			Label label = createLabel(parent, Messages.LABEL_MAX_AGE);
			label.setLayoutData(gd1);

			GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, true);
			gd2.horizontalSpan = cols - 1;
			Text text = createMaxAgeText(parent);
			text.setLayoutData(gd2);
		}
	}

	private Text createMaxAgeText(Composite parent) {
		m_maxAgeText = new Text(parent, SWT.BORDER);
		m_maxAgeText.setText(m_model.getMaxAgeString());
		QuantityKindProposal.install(m_maxAgeText, UnitLookup.TIMESPAN);
		findToolTipText(m_maxAgeText, RecordingOptionsBuilder.KEY_MAX_AGE);
		m_maxAgeText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getModel().setMaxAge(m_maxAgeText.getText());
			}

		});
		return m_maxAgeText;
	}

	protected Label createLabel(Composite parent, String text) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(text);
		return label;
	}

	protected void setFixedRecordingControlsEnabled(boolean fixed) {
		if (m_fixedOption != null) {
			m_fixedOption.setSelection(fixed);
		}
		if (m_durationText != null) {
			m_durationText.setEnabled(fixed);
		}
		if (m_delayText != null) {
			m_delayText.setEnabled(fixed);
		}
		if (fixed) {
			setInfoText(Messages.RECORDING_WIZARD_PAGE_DURATION_NOTE);
		}
	}

	protected void setContinuousRecordingControlsEnabled(boolean continuous) {
		if (m_continuousOption != null) {
			m_continuousOption.setSelection(continuous);
		}
		if (m_maxAgeText != null) {
			m_maxSizeText.setEnabled(continuous);
		}
		if (m_maxSizeText != null) {
			m_maxAgeText.setEnabled(continuous);
		}
		if (m_nameText != null) {
			m_nameText.setEnabled(!(continuous && m_disableNameForContinuous));
		}
		if (m_comboViewer != null) {
			m_comboViewer.getControl().setEnabled(!(continuous && m_disableSettingsForContinuous));
		}

		if (continuous) {
			setInfoText(Messages.RECORDING_WIZARD_PAGE_CONTINUOUS_NOTE);
		}
	}

	protected void setInfoText(String info) {
		if (m_infoText != null) {
			m_infoText.setText(info, true, false);
		}
	}

	private void createContinuousOption(Composite parent, int cols) {
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd1.horizontalSpan = cols;
		m_continuousOption = new Button(parent, SWT.RADIO);
		m_continuousOption.setText(Messages.BUTTON_CONTINUOUS_RECORDING_TEXT);
		m_continuousOption.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (m_continuousOption.getSelection()) {
					setContinuous(true);
					storeContinuous(true);
				}
			}
		});
		m_continuousOption.setLayoutData(gd1);
	}

	private void storeContinuous(boolean continuous) {
		if (!getModel().isEditing()) {
			getModel().setFixedRecording(!continuous);
		}
	}

	private boolean shouldHaveDurationControl() {
		return !m_model.isEditing() || m_model.isFixedRecording();
	}

	private boolean shouldHaveContinuousControls() {
		return !m_model.isEditing() || !m_model.isFixedRecording();
	}

	// FIXME: Does not work for the JFR IDE launch (offline) use case.
	private void findToolTipText(Control control, String key) {
		IDescribedMap<String> options = m_model.getAvailableRecordingOptions();
		if (options != null) {
			IDescribable descriptor = options.getDescribable(key);
			if (descriptor != null) {
				control.setToolTipText(descriptor.getDescription());
			}
		}
	}

	protected void openTemplateManager() {
		Dialog dialog = TemplateManagerWizardPage.createDialogFor(getShell(), m_model.getTemplateRepository(),
				m_model.getEventOptions(), m_model.getEventTypeInfoMap(), m_model.getVersion());
		dialog.open();
		// FIXME: Should we observe the repository instead? Maybe through the m_model?
		refreshTemplateCombo();
	}

	private Composite createTemplateContainer(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		int cols = 5;
		GridLayout layout = new GridLayout(cols, false);
		container.setLayout(layout);

		createTemplateControls(container, cols);
		return container;
	}

	protected Composite createDescriptionControls(Composite parent) {
		if (m_displaySettingsDescription) {
			Composite container = new Composite(parent, SWT.NONE);
			container.setLayout(new GridLayout());
			GridData gd1 = new GridData(SWT.FILL, SWT.FILL, false, false);
			Control label = createLabel(container, Messages.LABEL_TEMPLATE_DESCRIPTION);
			label.setLayoutData(gd1);

			GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, true);
			gd2.heightHint = 40;
			gd2.minimumWidth = 0;
			Control description = createDescriptionText(container);
			description.setLayoutData(gd2);
			return container;
		} else {
			return null;
		}
	}

	private Control createDescriptionText(Composite parent) {
		m_description = new Text(parent, SWT.WRAP | SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL | SWT.BORDER);
		m_description.setEnabled(true);
		return m_description;
	}

	protected Label createSeparator(Composite parent) {
		return new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
	}

	private ComboViewer createSelector(Composite parent, IContentProvider contentProvider) {
		ComboViewer comboViewer = new ComboViewer(parent);
		comboViewer.getCombo().setVisibleItemCount(VISIBLE_ITEMS_IN_COMBO);
		comboViewer.setContentProvider(contentProvider);
		comboViewer.setInput(m_model.getTemplateRepository());
		comboViewer.setLabelProvider(new TemplateLabelProvider(false));
		return comboViewer;
	}

	public void setContinuous(boolean continuous) {
		setContinuousRecordingControlsEnabled(continuous);
		setFixedRecordingControlsEnabled(!continuous);
	}

	public void setDuration(String duration) {
		m_durationText.setText(duration);
	}

	public void setDelay(String delayString) {
		m_delayText.setText(delayString);
	}

	public void setName(String name) {
		m_nameText.setText(name);
	}

	public void setFileName(MCFile path) {
		m_filenameText.setText(path.getPath());
		m_filenameText.setToolTipText(path.getPath());
	}

	public void setBehaviorForContinuous(boolean disableSettingsForContinuous, boolean disableNameForContinous) {
		m_disableSettingsForContinuous = disableSettingsForContinuous;
		m_disableNameForContinuous = disableNameForContinous;
		setContinuous(m_continuousOption.getSelection());
	}

	public void setTemplate(IEventConfiguration selectConfig) {
		if (selectConfig != null) {
			if (Arrays.asList(m_comboProvider.getElements(getModel().getTemplateRepository())).contains(selectConfig)) {
				m_comboViewer.setSelection(new StructuredSelection(selectConfig), true);
			} else {
				m_comboViewer.setSelection(null, true);
			}
		}
	}

	private RecordingWizardModel getModel() {
		return m_model;
	}
}
