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
package org.openjdk.jmc.ui.wizards;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.openjdk.jmc.ui.misc.DisplayToolkit;

/**
 * Wizard that wraps an {@link IWizardPage} so it looks like an ordinary {@link TitleAreaDialog}.
 * Benefits of using a wizard page instead of deriving from {@link TitleAreaDialog} is that the page
 * can be reused in an ordinary wizard.
 * <p>
 * This class also takes care of window title, shell image, placement.
 * <p>
 * If the {@link IWizardPage} implements {@link IPerformFinishable} the method
 * {@link IPerformFinishable#performFinish()} is called when the user presses OK.
 */
public class OnePageWizardDialog extends SizeConstrainedWizardDialog {
	private Image m_image;
	private boolean m_setFinishButtonAsOK = true;
	private boolean m_hideCancelButton;

	/**
	 * Creates a {@link WizardDialog} with only one {@link WizardPage}
	 *
	 * @param shell
	 *            the shell
	 * @param page
	 *            the {@link WizardPage}
	 * @param image
	 *            the image to use for the window
	 * @param title
	 *            the window title
	 */
	public OnePageWizardDialog(Shell shell, IWizardPage page) {
		super(shell, new OnePageWizard(page));
	}

	public OnePageWizardDialog(Shell shell, IWizardPage page, Image image) {
		super(shell, new OnePageWizard(page));
		m_image = image;
	}

	public void setDialogSettings(IDialogSettings settings) {
		getOnePageWizard().setDialogSettings(settings);
	}

	public IDialogSettings getDialogSettings() {
		return getOnePageWizard().getDialogSettings();
	}

	public IWizardPage getPage() {
		return getOnePageWizard().getPage();
	}

	private OnePageWizard getOnePageWizard() {
		return (OnePageWizard) getWizard();
	}

	public void setFinishButtonLabelAsOK(boolean setFinishButtonAsOK) {
		m_setFinishButtonAsOK = setFinishButtonAsOK;
	}

	public void setHideCancelButton(boolean hideCancelButton) {
		m_hideCancelButton = hideCancelButton;
	}

	@Override
	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);
		if (m_image != null) {
			getShell().setImage(m_image);
		}

		updateShellSize();
		DisplayToolkit.placeDialogInCenter(getParentShell(), getShell());
		if (m_setFinishButtonAsOK) {
			getButton(IDialogConstants.FINISH_ID).setText(IDialogConstants.OK_LABEL);
		}
		if (m_hideCancelButton) {
			Button showButton;
			Button hideButton;
			if (parent.getDisplay().getDismissalAlignment() == SWT.RIGHT) {
				// Linux
				showButton = getButton(IDialogConstants.FINISH_ID);
				hideButton = getButton(IDialogConstants.CANCEL_ID);
			} else {
				// Windows
				showButton = getButton(IDialogConstants.CANCEL_ID);
				hideButton = getButton(IDialogConstants.FINISH_ID);
			}
			hideButton.setVisible(false);
			showButton.setText(IDialogConstants.OK_LABEL);
			showButton.setFocus();
		}

		return control;
	}

	public boolean performFinish() {
		return getWizard().performFinish();
	}

	public static int open(IWizardPage wp, int width, int height) {
		OnePageWizardDialog d = new OnePageWizardDialog(Display.getCurrent().getActiveShell(), wp);
		d.setWidthConstraint(width, width);
		d.setHeightConstraint(height, height);
		return d.open();
	}
}
