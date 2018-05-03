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
package org.openjdk.jmc.ui.misc;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.openjdk.jmc.common.util.ExceptionToolkit;

/**
 * Based on the InternalErrorDialog in eclipse.
 */
public class ExceptionDialog extends MessageDialog {
	private final Throwable detail;
	private int detailButtonID = -1;
	private Text text;

	// Workaround. SWT does not seem to set the default button if
	// there is not control with focus. Bug: 14668
	private int defaultButtonIndex = 0;

	/**
	 * Size of the text in lines.
	 */
	private static final int TEXT_LINE_COUNT = 15;

	/**
	 * Create a new dialog.
	 *
	 * @param parentShell
	 *            the parent shell
	 * @param dialogTitle
	 *            the title
	 * @param dialogTitleImage
	 *            the title image
	 * @param dialogMessage
	 *            the message
	 * @param detail
	 *            the error to display
	 * @param dialogImageType
	 *            the type of image
	 * @param dialogButtonLabels
	 *            the button labels
	 * @param defaultIndex
	 *            the default selected button index
	 */
	public ExceptionDialog(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage,
			Throwable detail, int dialogImageType, String[] dialogButtonLabels, int defaultIndex) {
		super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, dialogButtonLabels,
				defaultIndex);
		defaultButtonIndex = defaultIndex;
		this.detail = detail;
		// Added SWT.RESIZE, since Dialog constructor sets the style to unresizeable
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	// Workaround. SWT does not seem to set the default button correctly if there is not a control with focus.
	@Override
	public int open() {
		create();
		Button b = getButton(defaultButtonIndex);
		b.setFocus();
		b.getShell().setDefaultButton(b);
		return super.open();
	}

	/**
	 * Set the detail button index
	 *
	 * @param index
	 *            the detail button index
	 */
	public void setDetailButton(int index) {
		detailButtonID = index;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == detailButtonID) {
			toggleDetailsArea();
		} else {
			setReturnCode(buttonId);
			close();
		}
	}

	/**
	 * Toggles the unfolding of the details area. This is triggered by the user pressing the details
	 * button.
	 */
	private void toggleDetailsArea() {
		Point windowSize = getShell().getSize();
		Point oldSize = getContents().computeSize(SWT.DEFAULT, SWT.DEFAULT);

		if (text != null) {
			text.dispose();
			text = null;
			getButton(detailButtonID).setText(IDialogConstants.SHOW_DETAILS_LABEL);
		} else {
			createDropDownText((Composite) getContents());
			getButton(detailButtonID).setText(IDialogConstants.HIDE_DETAILS_LABEL);
		}

		Point newSize = getContents().computeSize(SWT.DEFAULT, SWT.DEFAULT);
		getShell().setSize(new Point(windowSize.x, windowSize.y + (newSize.y - oldSize.y)));
	}

	/**
	 * Create this dialog's drop-down list component.
	 *
	 * @param parent
	 *            the parent composite
	 */
	protected void createDropDownText(Composite parent) {
		text = createDropDownText(parent, detail);
	}

	public static Text createDropDownText(Composite parent, Throwable detail) {
		// create the list
		Text text = new Text(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.READ_ONLY);
		text.setFont(parent.getFont());
		if (detail != null) {
			text.setText(ExceptionToolkit.toString(detail));
		} else {
			text.setText(Messages.ExceptionDialog_NO_DETAILS_AVAILABLE);
		}

		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL
				| GridData.VERTICAL_ALIGN_FILL | GridData.GRAB_VERTICAL);
		data.heightHint = text.getLineHeight() * TEXT_LINE_COUNT;
		data.horizontalSpan = 2;
		text.setLayoutData(data);
		return text;
	}

	/**
	 * Convenience method to open a simple Yes/No question dialog with exception details and a help
	 * button.
	 *
	 * @param parent
	 *            the parent shell of the dialog, or {@code null} if none
	 * @param title
	 *            the dialog's title, or {@code null} if none
	 * @param message
	 *            the message
	 * @param detail
	 *            the error
	 * @return {@code true} if the user presses the OK button, {@code false} otherwise
	 */
	static boolean openExceptionDialog(Shell parent, String title, String message, Throwable detail) {
		List<String> labels = new ArrayList<>();
		int index = 0, detailsIndex = -1;
		labels.add(IDialogConstants.OK_LABEL);
		if (detail != null) {
			labels.add(IDialogConstants.SHOW_DETAILS_LABEL);
			detailsIndex = ++index;
		}

		if ((message == null) || (message.trim().length() == 0)) {
			message = NLS.bind("An unanticipated {0} occurred", (detail != null) //$NON-NLS-1$
					? detail.getClass().getName() : "exception"); //$NON-NLS-1$
		}

		ExceptionDialog dialog = new ExceptionDialog(parent, title, null, message, detail, ERROR,
				labels.toArray(new String[labels.size()]), 0);
		if (detail != null) {
			dialog.setDetailButton(detailsIndex);
		}
		return dialog.open() == 0;
	}

}
