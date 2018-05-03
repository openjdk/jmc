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

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.openjdk.jmc.ui.misc.DisplayToolkit;

public class SizeConstrainedWizardDialog extends WizardDialog {
	// Defaults
	private static final int DIALOG_MAX_HEIGHT = 700;
	private static final int DIALOG_MIN_HEIGHT = 600;
	private static final int DIALOG_MAX_WIDTH = 800;
	private static final int DIALOG_MIN_WIDTH = 800;

	public SizeConstrainedWizardDialog(Shell parentShell, IWizard newWizard) {
		super(parentShell, newWizard);
		setWidthConstraint(DIALOG_MIN_WIDTH, DIALOG_MAX_WIDTH);
		setHeightConstraint(DIALOG_MIN_HEIGHT, DIALOG_MAX_HEIGHT);
	}

	@Override
	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);
		updateShellSize();
		DisplayToolkit.placeDialogInCenter(getParentShell(), getShell());
		return control;
	}

	private int minWidth;
	private int maxWidth;
	private int minHeight;
	private int maxHeight;

	public void setWidthConstraint(int min, int max) {
		minWidth = min;
		maxWidth = max;
	}

	public void setHeightConstraint(int min, int max) {
		minHeight = min;
		maxHeight = max;
	}

	protected void updateShellSize() {
		getShell().setMinimumSize(minWidth, minHeight);
		getShell().pack();
		Point size = getShell().getSize();
		int width = calculateConstrained(minWidth, size.x, maxWidth);
		int height = calculateConstrained(minHeight, size.y, maxHeight);
		if (width != size.x || height != size.y) {
			getShell().setSize(new Point(width, height));
		}
	}

	private static int calculateConstrained(int min, int preferred, int max) {
		return Math.min(Math.max(min, preferred), max);
	}
}
