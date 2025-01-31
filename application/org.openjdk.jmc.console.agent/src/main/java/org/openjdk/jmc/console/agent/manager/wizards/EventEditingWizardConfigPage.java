/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, 2025, Red Hat Inc. All rights reserved.
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

import org.eclipse.jface.wizard.IWizardPage;
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
import org.openjdk.jmc.console.agent.manager.model.IEvent;
import org.openjdk.jmc.console.agent.manager.model.IEvent.Location;
import org.openjdk.jmc.console.agent.messages.internal.Messages;
import org.openjdk.jmc.console.agent.wizards.BaseWizardPage;

import java.util.Locale;
import java.util.stream.Stream;

public class EventEditingWizardConfigPage extends BaseWizardPage {

	private final IEvent event;

	private Text idText;
	private Text nameText;
	private Text descriptionText;
	private Text classText;
	private Text methodNameText;
	private Text methodDescriptorText;
	private Text pathText;
	private Combo locationCombo;
	private Button locationClearButton;
	private Button recordExceptionsButton;
	private Button recordStackTraceButton;

	protected EventEditingWizardConfigPage(IEvent event) {
		super(Messages.EventEditingWizardConfigPage_PAGE_NAME);

		this.event = event;
	}

	@Override
	public IWizardPage getNextPage() {
		return super.getNextPage();
	}

	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		setTitle(Messages.EventEditingWizardConfigPage_MESSAGE_EVENT_EDITING_WIZARD_CONFIG_PAGE_TITLE);
		setDescription(Messages.EventEditingWizardConfigPage_MESSAGE_EVENT_EDITING_WIZARD_CONFIG_PAGE_DESCRIPTION);

		ScrolledComposite sc = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		Composite container = new Composite(sc, SWT.NONE);
		sc.setContent(container);

		GridLayout layout = new GridLayout();
		container.setLayout(layout);

		createConfigContainer(container).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createSeparator(container).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createInstrumentationTargetContainer(container).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createSeparator(container).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createMetaInfoContainer(container).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		bindListeners();
		populateUi();

		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		setControl(sc);
	}

	private Composite createConfigContainer(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		int cols = 5;
		GridLayout layout = new GridLayout(cols, false);
		layout.horizontalSpacing = 8;
		container.setLayout(layout);

		idText = createTextInput(container, cols, Messages.EventEditingWizardConfigPage_LABEL_ID,
				Messages.EventEditingWizardConfigPage_MESSAGE_EVENT_ID);
		nameText = createTextInput(container, cols, Messages.EventEditingWizardConfigPage_LABEL_NAME,
				Messages.EventEditingWizardConfigPage_MESSAGE_NAME_OF_THE_EVENT);
		descriptionText = createMultiTextInput(container, cols, Messages.EventEditingWizardConfigPage_LABEL_DESCRIPTION,
				Messages.EventEditingWizardConfigPage_MESSAGE_OPTIONAL_DESCRIPTION_OF_THIS_EVENT);

		return container;
	}

	private Composite createInstrumentationTargetContainer(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		int cols = 8;
		GridLayout layout = new GridLayout(cols, false);
		layout.horizontalSpacing = 8;
		container.setLayout(layout);

		classText = createTextInput(container, cols, Messages.EventEditingWizardConfigPage_LABEL_CLASS,
				Messages.EventEditingWizardConfigPage_MESSAGE_FULLY_QUALIFIED_CLASS_NAME);
		Text[] receivers = createMultiInputTextInput(container, cols,
				Messages.EventEditingWizardConfigPage_LABEL_METHOD,
				new String[] {Messages.EventEditingWizardConfigPage_MESSAGE_METHOD_NAME,
						Messages.EventEditingWizardConfigPage_MESSAGE_METHOD_DESCRIPTOR});

		methodNameText = receivers[0];
		methodDescriptorText = receivers[1];

		return container;
	}

	private Composite createMetaInfoContainer(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		int cols = 8;
		GridLayout layout = new GridLayout(cols, false);
		layout.horizontalSpacing = 8;
		container.setLayout(layout);

		pathText = createTextInput(container, cols, Messages.EventEditingWizardConfigPage_LABEL_PATH,
				Messages.EventEditingWizardConfigPage_MESSAGE_PATH_TO_EVENT);
		locationCombo = createComboInput(container, cols - 2, Messages.EventEditingWizardConfigPage_LABEL_LOCATION,
				Stream.of(Location.values()).map(Location::toString).toArray(String[]::new));
		locationClearButton = createButton(container, Messages.EventEditingWizardConfigPage_LABEL_CLEAR);
		locationClearButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 0));

		recordExceptionsButton = createCheckboxInput(container, cols,
				Messages.EventEditingWizardConfigPage_LABEL_RECORD_EXCEPTIONS);
		recordStackTraceButton = createCheckboxInput(container, cols,
				Messages.EventEditingWizardConfigPage_LABEL_RECORD_STACK_TRACE);

		return container;
	}

	private void bindListeners() {
		idText.addModifyListener(handleExceptionIfAny((ModifyListener) e -> event.setId(idText.getText())));
		nameText.addModifyListener(handleExceptionIfAny((ModifyListener) e -> event.setName(nameText.getText())));
		descriptionText.addModifyListener(
				handleExceptionIfAny((ModifyListener) e -> event.setDescription(descriptionText.getText())));
		methodNameText.addModifyListener(
				handleExceptionIfAny((ModifyListener) e -> event.setMethodName(methodNameText.getText())));
		methodDescriptorText.addModifyListener(
				handleExceptionIfAny((ModifyListener) e -> event.setMethodDescriptor(methodDescriptorText.getText())));
		pathText.addModifyListener(handleExceptionIfAny((ModifyListener) e -> event.setPath(pathText.getText())));
		classText.addModifyListener(handleExceptionIfAny((ModifyListener) e -> event.setClazz(classText.getText())));
		locationCombo.addModifyListener(
				handleExceptionIfAny((ModifyListener) e -> event.setLocation(locationCombo.getSelectionIndex() == -1
						? null : Location.valueOf(locationCombo.getText().toUpperCase(Locale.ENGLISH)))));
		locationClearButton.addListener(SWT.Selection, e -> locationCombo.deselectAll());
		recordExceptionsButton.addListener(SWT.Selection,
				handleExceptionIfAny((Listener) e -> event.setRethrow(recordExceptionsButton.getSelection())));
		recordStackTraceButton.addListener(SWT.Selection,
				handleExceptionIfAny((Listener) e -> event.setStackTrace(recordStackTraceButton.getSelection())));
	}

	private void populateUi() {
		setText(idText, event.getId());
		setText(nameText, event.getName());
		setText(descriptionText, event.getDescription());
		setText(classText, event.getClazz());
		setText(methodNameText, event.getMethodName());
		setText(methodDescriptorText, event.getMethodDescriptor());
		setText(pathText, event.getPath());
		setText(locationCombo, event.getLocation() == null ? null : event.getLocation().toString());
		recordExceptionsButton.setSelection(event.getRethrow());
		recordStackTraceButton.setSelection(event.getStackTrace());
	}
}
