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
package org.openjdk.jmc.rjmx.ui.attributes;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.openjdk.jmc.common.unit.ITypedQuantity;
import org.openjdk.jmc.common.unit.LinearKindOfQuantity;
import org.openjdk.jmc.common.unit.LinearUnit;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.ui.misc.DisplayToolkit;
import org.openjdk.jmc.ui.misc.QuantityKindProposal;

/**
 * Simple dialog for editing a quantity. Currently used only to define custom units, but could be
 * generalized so the texts are customizable.
 */
public class QuantityInputDialog extends TitleAreaDialog {
	private final LinearKindOfQuantity kindOfQuantity;
	private ITypedQuantity<LinearUnit> quantity;
	private Text quantityText;

	public static ITypedQuantity<LinearUnit> promptForCustomUnit(
		Shell shell, ITypedQuantity<LinearUnit> initialQuantity) {
		QuantityInputDialog dialog = new QuantityInputDialog(shell, initialQuantity);
		return (dialog.open() == OK) ? dialog.quantity : null;
	}

	public QuantityInputDialog(Shell parentShell, ITypedQuantity<LinearUnit> initialQuantity) {
		super(parentShell);
		kindOfQuantity = initialQuantity.getUnit().getContentType();
		quantity = initialQuantity;
	}

	@Override
	protected Control createContents(Composite parent) {
		getShell().setText(Messages.QuantityInputDialog_DIALOG_TITLE);
		Control contents = super.createContents(parent);
		contents.getShell().setSize(400, 220);
		DisplayToolkit.placeDialogInCenter(getParentShell(), getShell());
		setMessage(Messages.QuantityInputDialog_DIALOG_MESSAGE);
		setTitle(Messages.QuantityInputDialog_DIALOG_TITLE);
		validateInput();
		return contents;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite dialogArea = (Composite) super.createDialogArea(parent);
		dialogArea.setLayout(new GridLayout());
		Label label = new Label(dialogArea, SWT.NONE);
		label.setText(Messages.QuantityInputDialog_LABEL_TEXT);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		quantityText = new Text(dialogArea, SWT.SINGLE | SWT.BORDER);
		quantityText.setText(quantity.interactiveFormat());
		QuantityKindProposal.install(quantityText, kindOfQuantity);
		quantityText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validateInput();
			}
		});
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
		gridData.widthHint = 80;
		quantityText.setLayoutData(gridData);
		return dialogArea;
	}

	private void validateInput() {
		Button okButton = getButton(IDialogConstants.OK_ID);
		try {
			quantity = kindOfQuantity.parseInteractive(quantityText.getText());
			setErrorMessage(null);
			okButton.setEnabled(true);
		} catch (QuantityConversionException e) {
			setErrorMessage(e.getLocalizedMessage());
			okButton.setEnabled(false);
		}
	}
}
