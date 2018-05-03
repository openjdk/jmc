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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.openjdk.jmc.flightrecorder.configuration.events.IEventConfiguration;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfigurationModel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfigurationRepository;
import org.openjdk.jmc.ui.wizards.IPerformFinishable;
import org.openjdk.jmc.ui.wizards.OnePageWizardDialog;

/**
 * Superclass for dialog controllers to edit a template.
 */
public abstract class TemplateEditPage extends WizardPage implements IPerformFinishable {
	private static final int ADVANCED_BUTTON_ID = IDialogConstants.CLIENT_ID + 1;
	protected final EventConfigurationModel model;
	protected final EventConfigurationRepository repository;
	private Text nameControl;
	private Text descriptionControl;

	public static Dialog createDialogFor(
		Shell shell, EventConfigurationModel model, EventConfigurationRepository repository) {
		return createDialogFor(shell, model, repository, true);
	}

	protected static Dialog createDialogFor(
		Shell shell, final EventConfigurationModel model, final EventConfigurationRepository repository,
		boolean preferSimple) {
		TemplateEditPage page;
		OnePageWizardDialog dialog;
		if (preferSimple && model.getConfiguration().hasControlElements()) {
			page = new TemplateEditSimplePage(model, repository);
			dialog = new OnePageWizardDialog(shell, page) {
				@Override
				protected void createButtonsForButtonBar(Composite parent) {
					// FIXME: Fix layout to left align this button.
					((GridLayout) parent.getLayout()).makeColumnsEqualWidth = false;
					createButton(parent, ADVANCED_BUTTON_ID, Messages.BUTTON_ADVANCED_TEXT, false);
					super.createButtonsForButtonBar(parent);
				};

				@Override
				protected void buttonPressed(int buttonId) {
					if (buttonId == ADVANCED_BUTTON_ID) {
						EventConfigurationModel advModel = model.createEditableClone();
						Dialog advDialog = createDialogFor(getShell(), advModel, repository, false);
						if (advDialog.open() == OK) {
							advModel.pushServerMetadataToLocalConfiguration(false);
							// Since the replacing of the contents has been taken care of by advDialogs performFinish(),
							// we must close this dialog as silently as possible, signaling that the repository
							// observers should be notified of the change.
							setReturnCode(OK);
							close();
						}
					} else {
						super.buttonPressed(buttonId);
					}
				}
			};
		} else {
			page = new TemplateEditAdvancedPage(model, repository);
			dialog = new OnePageWizardDialog(shell, page);
		}
		dialog.setWidthConstraint(600, 800);
		dialog.setHeightConstraint(600, 800);
		return dialog;
	}

	public TemplateEditPage(EventConfigurationModel model, EventConfigurationRepository repository, String pageName,
			String title) {
		super(pageName, title, null);
		this.model = model;
		this.repository = repository;
	}

	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);

		createGeneralArea(container).setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		createSpecificArea(container);

		setControl(container);
	}

	protected Control createGeneralArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 0;
		container.setLayout(layout);

		createNameControls(container);
		createDescriptionControls(container);

		return container;
	}

	private void createNameControls(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.LABEL_NAME_TEXT);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true));

		nameControl = new Text(parent, SWT.BORDER);
		nameControl.setText(model.getConfiguration().getName());
		nameControl.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				model.getConfiguration().setName(nameControl.getText());
				verifyName();
			}
		});
		nameControl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		// Need to check first when opening the advanced dialog from the simple dialog.
		verifyName();
	}

	private void verifyName() {
		String newName = nameControl.getText();
		String oldName = model.getConfiguration().getOriginal().getName();
		boolean ok = newName.equals(oldName) || repository.isAllowedName(newName);
		setErrorMessage(ok ? null : Messages.SAVE_TEMPLATE_WIZARD_PAGE_DUPLICATE_ERROR_MSG);
		setPageComplete(ok);
	}

	private void createDescriptionControls(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.LABEL_DESCRIPTION_TEXT);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 2, 1));

		descriptionControl = new Text(parent, SWT.WRAP | SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
		descriptionControl.setText(model.getConfiguration().getDescription());
		descriptionControl.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				model.getConfiguration().setDescription(descriptionControl.getText());
			}
		});
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		gd.heightHint = convertHeightInCharsToPixels(4);
		descriptionControl.setLayoutData(gd);
	}

	protected abstract void createSpecificArea(Composite parent);

	@Override
	public boolean performFinish() {
		if (isPageComplete()) {
			IEventConfiguration workingCopy = model.getConfiguration();
			return repository.replaceOriginalContentsFor(workingCopy);
		}
		return false;
	}
}
