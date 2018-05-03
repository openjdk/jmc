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

import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;

/**
 * Field editor for integers, with min and max limits included in error messages.
 */
public class IntFieldEditor extends IntegerFieldEditor {
	// Must keep copy, since superclass has them private.
	private int minOkValue = 0;
	private int maxOkValue = Integer.MAX_VALUE;

	private String generatedErrorMsg;

	public IntFieldEditor(String name, String labelText, Composite parent) {
		super(name, labelText, parent);
		// Clear this so we generate better, unless explicitly set ...
		setErrorMessage(null);
	}

	/**
	 * Creates an integer field editor.
	 *
	 * @param name
	 *            the name of the preference this field editor works on
	 * @param labelText
	 *            the label text of the field editor
	 * @param parent
	 *            the parent of the field editor's control
	 * @param textLimit
	 *            the maximum number of characters in the text.
	 */
	public IntFieldEditor(String name, String labelText, Composite parent, int textLimit) {
		super(name, labelText, parent, textLimit);
		// Clear this so we generate better, unless explicitly set ...
		setErrorMessage(null);
	}

	/*
	 * (non-Javadoc) Method declared on FieldEditor. Making public since we cannot rely on package
	 * access.
	 */
	@Override
	public void adjustForNumColumns(int numColumns) {
		super.adjustForNumColumns(numColumns);
	}

	@Override
	public void setValidRange(int min, int max) {
		// Must keep copy, since superclass has them private.
		minOkValue = min;
		maxOkValue = max;
		// Prevent super class from overriding both customized and our generated error message.
		String overriddenErrorMsg = getErrorMessage();
		super.setValidRange(min, max);
		setErrorMessage(overriddenErrorMsg);
		// Invalidate generated error message.
		generatedErrorMsg = null;
	}

	/**
	 * Shows the error message either set via {@link #setErrorMessage(String)} or dynamically
	 * generated to include the limits.
	 */
	@Override
	public void showErrorMessage() {
		showErrorMessage(getErrorMessage());
	}

	@Override
	public String getErrorMessage() {
		String msg = super.getErrorMessage();
		if (msg != null) {
			return msg;
		}
		if (generatedErrorMsg != null) {
			return generatedErrorMsg;
		}

		// Construct a localized error message
		boolean hasMax = (maxOkValue != Integer.MAX_VALUE);
		boolean hasMin = (minOkValue != Integer.MIN_VALUE);
		String valueName;
		if (minOkValue == 0) {
			valueName = Messages.IntFieldEditor_ERROR_MESSAGE_PART_POSITIVE_INTEGER;
			// No additional low limit should be shown.
			hasMin = false;
		} else if (minOkValue == 1) {
			valueName = Messages.IntFieldEditor_ERROR_MESSAGE_PART_NATURAL_NUMBER;
			// No additional low limit should be shown.
			hasMin = false;
		} else if (maxOkValue == 0) {
			valueName = Messages.IntFieldEditor_ERROR_MESSAGE_PART_NEGATIVE_INTEGER;
			// No additional high limit should be shown.
			hasMax = false;
		} else {
			valueName = Messages.IntFieldEditor_ERROR_MESSAGE_PART_INTEGER;
		}

		Object[] msgArgs = new Object[] {valueName, Integer.valueOf(minOkValue), Integer.valueOf(maxOkValue)};

		if (hasMax && hasMin) {
			msg = NLS.bind(Messages.NumberFieldEditor_ERROR_MESSAGE_INTERVAL, msgArgs);
		} else if (hasMax && !hasMin) {
			msg = NLS.bind(Messages.NumberFieldEditor_ERROR_MESSAGE_NO_GREATER, msgArgs);
		} else if (!hasMax && hasMin) {
			msg = NLS.bind(Messages.NumberFieldEditor_ERROR_MESSAGE_NO_SMALLER, msgArgs);
		} else {
			msg = NLS.bind(Messages.NumberFieldEditor_ERROR_MESSAGE_MUST_BE, msgArgs);
		}
		return generatedErrorMsg = msg;
	}

}
