/*
 * Copyright (c) 2020, 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, 2026, Red Hat Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.console.agent.manager.wizards;

import java.math.BigInteger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.openjdk.jmc.console.agent.manager.model.IPreset;
import org.openjdk.jmc.console.agent.manager.model.IPreset.CollectionTrackingMode;
import org.openjdk.jmc.console.agent.messages.internal.Messages;
import org.openjdk.jmc.console.agent.wizards.BaseWizardPage;

public class PresetEditingWizardConfigPage extends BaseWizardPage {
	private final IPreset preset;

	private Text fileNameText;
	private Text classPrefixText;
	private Button allowToStringButton;
	private Button allowConverterButton;
	private Combo collectionTrackingCombo;
	private Text collectionTrackingMinSizeText;

	protected PresetEditingWizardConfigPage(IPreset preset) {
		super(Messages.PresetEditingWizardConfigPage_PAGE_NAME);

		this.preset = preset;
	}

	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		setTitle(Messages.PresetEditingWizardConfigPage_MESSAGE_PRESET_EDITING_WIZARD_CONFIG_PAGE_TITLE);
		setDescription(Messages.PresetEditingWizardConfigPage_MESSAGE_PRESET_EDITING_WIZARD_CONFIG_PAGE_DESCRIPTION);

		ScrolledComposite sc = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		Composite container = new Composite(sc, SWT.NONE);
		sc.setContent(container);

		container.setLayout(new GridLayout());

		createFileNameContainer(container).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createSeparator(container).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createGlobalConfigContainer(container).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createSeparator(container).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createCollectionTrackingContainer(container).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		populateUi();
		bindListeners();

		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		setControl(sc);
	}

	private Composite createFileNameContainer(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		int cols = 5;
		GridLayout layout = new GridLayout(cols, false);
		layout.horizontalSpacing = 8;
		container.setLayout(layout);

		fileNameText = createTextInput(container, cols, Messages.PresetEditingWizardConfigPage_LABEL_FILE_NAME,
				Messages.PresetEditingWizardConfigPage_MESSAGE_NAME_OF_THE_SAVED_XML);

		return container;
	}

	private Composite createCollectionTrackingContainer(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		int cols = 5;
		GridLayout layout = new GridLayout(cols, false);
		layout.horizontalSpacing = 8;
		container.setLayout(layout);

		collectionTrackingCombo = createComboInput(container, cols,
				Messages.PresetEditingWizardConfigPage_LABEL_COLLECTION_TRACKING,
				new String[] {Messages.PresetEditingWizardConfigPage_COLLECTION_TRACKING_KEEP_CURRENT,
						Messages.PresetEditingWizardConfigPage_COLLECTION_TRACKING_ENABLED,
						Messages.PresetEditingWizardConfigPage_COLLECTION_TRACKING_DISABLED});
		collectionTrackingMinSizeText = createTextInput(container, cols,
				Messages.PresetEditingWizardConfigPage_LABEL_COLLECTION_TRACKING_MIN_SIZE,
				Messages.PresetEditingWizardConfigPage_MESSAGE_COLLECTION_TRACKING_MIN_SIZE);
		collectionTrackingMinSizeText
				.setToolTipText(Messages.PresetEditingWizardConfigPage_MESSAGE_COLLECTION_TRACKING_MIN_SIZE);

		return container;
	}

	private Composite createGlobalConfigContainer(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		int cols = 5;
		GridLayout layout = new GridLayout(cols, false);
		layout.horizontalSpacing = 8;
		container.setLayout(layout);

		classPrefixText = createTextInput(container, cols, Messages.PresetEditingWizardConfigPage_LABEL_CLASS_PREFIX,
				Messages.PresetEditingWizardConfigPage_MESSAGE_PREFIX_ADDED_TO_GENERATED_EVENT_CLASSES);
		allowToStringButton = createCheckboxInput(parent, cols,
				Messages.PresetEditingWizardConfigPage_LABEL_ALLOW_TO_STRING);
		allowConverterButton = createCheckboxInput(parent, cols,
				Messages.PresetEditingWizardConfigPage_LABEL_ALLOW_CONVERTER);

		return container;
	}

	private void bindListeners() {
		fileNameText.addModifyListener(
				handleExceptionIfAny((ModifyListener) e -> preset.setFileName(fileNameText.getText())));
		classPrefixText.addModifyListener(
				handleExceptionIfAny((ModifyListener) e -> preset.setClassPrefix(classPrefixText.getText())));
		allowToStringButton.addListener(SWT.Selection,
				handleExceptionIfAny((Listener) e -> preset.setAllowToString(allowToStringButton.getSelection())));
		allowConverterButton.addListener(SWT.Selection,
				handleExceptionIfAny((Listener) e -> preset.setAllowConverter(allowConverterButton.getSelection())));
		collectionTrackingCombo.addListener(SWT.Selection,
				handleExceptionIfAny((Listener) e -> updateCollectionTracking()));
		collectionTrackingMinSizeText
				.addModifyListener(handleExceptionIfAny((ModifyListener) e -> updateCollectionTrackingMinSize()));
	}

	private void updateCollectionTracking() {
		int selected = collectionTrackingCombo.getSelectionIndex();
		CollectionTrackingMode mode = selected == 0 ? CollectionTrackingMode.UNCHANGED
				: selected == 1 ? CollectionTrackingMode.ENABLED : CollectionTrackingMode.DISABLED;
		preset.setCollectionTrackingMode(mode);
		collectionTrackingMinSizeText.setEnabled(mode == CollectionTrackingMode.ENABLED);
		if (mode == CollectionTrackingMode.UNCHANGED) {
			setText(collectionTrackingMinSizeText, null);
		}
		handleExceptionIfAny(collectionTrackingMinSizeText, () -> updateCollectionTrackingMinSize());
	}

	private void updateCollectionTrackingMinSize() {
		if (!collectionTrackingMinSizeText.isEnabled()) {
			return;
		}
		String value = collectionTrackingMinSizeText.getText().trim();
		BigInteger minSize;
		try {
			minSize = value.isEmpty() ? null : new BigInteger(value);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(Messages.Preset_ERROR_COLLECTION_TRACKING_MIN_SIZE);
		}
		preset.setCollectionTrackingMinSize(minSize);
	}

	private void populateUi() {
		setText(fileNameText, preset.getFileName());
		setText(classPrefixText, preset.getClassPrefix());
		allowToStringButton.setSelection(preset.getAllowToString());
		allowConverterButton.setSelection(preset.getAllowConverter());
		CollectionTrackingMode mode = preset.getCollectionTrackingMode();
		collectionTrackingCombo
				.select(mode == CollectionTrackingMode.UNCHANGED ? 0 : mode == CollectionTrackingMode.ENABLED ? 1 : 2);
		BigInteger minSize = preset.getCollectionTrackingMinSize();
		setText(collectionTrackingMinSizeText, minSize == null ? null : minSize.toString());
		collectionTrackingMinSizeText.setEnabled(mode == CollectionTrackingMode.ENABLED);
	}
}
