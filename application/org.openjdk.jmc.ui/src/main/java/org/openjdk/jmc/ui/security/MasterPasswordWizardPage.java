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
package org.openjdk.jmc.ui.security;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.wizards.IPerformFinishable;

/**
 * Wizard page for input of the master password.
 */
final class MasterPasswordWizardPage extends WizardPage implements IPerformFinishable {
	static final String PAGE_NAME = Messages.MASTER_PASSWORD_WIZARD_PAGE;

	private Text passwordField;
	private Text passwordField2;

	private final boolean usePasswordVerification;
	private final boolean warnForDataClear;
	private static final int MIN_PASSWORD_LENGTH = 5;

	private String password;

	private class InputVerifier implements ModifyListener {

		@Override
		public void modifyText(ModifyEvent e) {
			checkPageComplete();
		}
	}

	String getMasterPassword() {
		return password;
	}

	public MasterPasswordWizardPage(boolean usePasswordVerification, boolean warnForDataClear) {
		super(PAGE_NAME);
		this.usePasswordVerification = usePasswordVerification;
		this.warnForDataClear = warnForDataClear;
		setImageDescriptor(UIPlugin.getDefault().getImageRegistry().getDescriptor(UIPlugin.ICON_CLASS_PUBLIC));
		if (usePasswordVerification) {
			setTitle(Messages.MasterPasswordWizardPage_SET_MASTER_PASSWORD_TITLE);
		} else {
			setTitle(Messages.MasterPasswordWizardPage_VERIFY_MASTER_PASSWORD_TITLE);
		}
	}

	@Override
	public void createControl(Composite parent) {
		initializeMessages();
		ModifyListener listener = new InputVerifier();

		parent.setLayout(new GridLayout());
		Composite container = new Composite(parent, SWT.LEFT);
		container.setLayout(new GridLayout());
		Composite passwordContainer = new Composite(container, SWT.LEFT);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		// limit x size so dialog doesn't get too big if the descriptor is very large
		gd.widthHint = 400;
		passwordContainer.setLayoutData(gd);
		passwordContainer.setLayout(new GridLayout(2, false));
		String labelText = usePasswordVerification ? Messages.MasterPasswordWizardPage_CAPTION_NEW_PASSWORD
				: Messages.MasterPasswordWizardPage_CAPTION_ENTER_PASSWORD;
		Label firstPasswordLabel = new Label(passwordContainer, SWT.LEFT);
		firstPasswordLabel.setText(labelText);
		firstPasswordLabel.setToolTipText(Messages.MasterPasswordWizardPage_TOOLTIP_ENTER_PASSWORD);
		passwordField = new Text(passwordContainer, SWT.SINGLE | SWT.BORDER | SWT.PASSWORD);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		passwordField.setLayoutData(data);
		passwordField.addModifyListener(listener);
		passwordField.setData("name", Constants.PASSWORD1_FIELD_NAME); //$NON-NLS-1$

		if (usePasswordVerification) {
			Label secondPasswordLabel = new Label(passwordContainer, SWT.LEFT);
			secondPasswordLabel.setText(Messages.MasterPasswordWizardPage_CAPTION_CONFIRM_PASSWORD);
			secondPasswordLabel.setToolTipText(Messages.MasterPasswordWizardPage_TOOLTIP_CONFIRM_PASSWORD);
			data = new GridData(GridData.FILL_HORIZONTAL);
			secondPasswordLabel.setLayoutData(data);
			passwordField2 = new Text(passwordContainer, SWT.SINGLE | SWT.BORDER | SWT.PASSWORD);
			GridData data2 = new GridData(GridData.FILL_HORIZONTAL);
			passwordField2.setLayoutData(data2);
			passwordField2.addModifyListener(listener);
			passwordField2.setData("name", Constants.PASSWORD2_FIELD_NAME); //$NON-NLS-1$
		}
		setControl(passwordContainer);
		setPageComplete(false);
		if (warnForDataClear) {
			Label dataClearWarnLabel = new Label(container, SWT.LEFT | SWT.BOLD);
			dataClearWarnLabel.setText(Messages.MasterPasswordWizardPage_WARN_DATA_CLEAR_TEXT);
			dataClearWarnLabel.setToolTipText(Messages.MasterPasswordWizardPage_WARN_DATA_CLEAR_TEXT);
			dataClearWarnLabel.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
		}
		container.layout();
	}

	private void initializeMessages() {
		setErrorMessage(null);
		if (usePasswordVerification) {
			setMessage(Messages.MasterPasswordWizardPage_SET_MASTER_PASSWORD_DESCRIPTION_TEXT);
		} else {
			setMessage(Messages.MasterPasswordWizardPage_VERIFY_MASTER_PASSWORD_DESCRIPTION_TEXT);
		}
	}

	private void checkPageComplete() {
		if (passwordField.getText().length() <= 0) {
			setErrorMessage(Messages.MasterPasswordWizardPage_ERROR_PASSWORD_EMPTY_TEXT);
			return;
		}
		if (passwordField.getText().length() < MIN_PASSWORD_LENGTH) {
			setErrorMessage(
					NLS.bind(Messages.MasterPasswordWizardPage_ERROR_MESSAGE_PASSWORD_SHORTER_THAN_X_CHARACTERS_TEXT,
							MIN_PASSWORD_LENGTH));
			return;
		}
		if (usePasswordVerification) {
			if (!passwordField.getText().equals(passwordField2.getText())) {
				setErrorMessage(Messages.MasterPasswordWizardPage_ERROR_MESSAGE_PASSWORDS_DO_NOT_MATCH_TEXT);
				return;
			}
		}
		setErrorMessage(null);
		setPageComplete(true);
	}

	@Override
	public boolean performFinish() {
		password = passwordField.getText();
		return true;
	}
}
