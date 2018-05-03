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

import static org.openjdk.jmc.common.unit.UnitLookup.MILLISECOND;
import static org.openjdk.jmc.common.unit.UnitLookup.TIMESPAN;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.rjmx.subscription.internal.UpdatePolicyToolkit;
import org.openjdk.jmc.ui.misc.QuantityKindProposal;

/**
 * Simple input dialog the get a custom update interval in milliseconds.
 */
public class UpdateIntervalDialog extends InputDialog {

	public UpdateIntervalDialog(Shell parentShell, int initialValue) {
		super(parentShell, Messages.UpdateIntervalDialog_DIALOG_TITLE, Messages.UpdateIntervalDialog_DIALOG_MESSAGE,
				MILLISECOND.quantity(initialValue).interactiveFormat(), createValidator());
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Control control = super.createDialogArea(parent);
		QuantityKindProposal.install(getText(), TIMESPAN);
		return control;
	}

	private static IInputValidator createValidator() {
		return new IInputValidator() {
			@Override
			public String isValid(String newText) {
				try {
					parseInterval(newText);
					return null;
				} catch (QuantityConversionException e) {
					return e.getLocalizedMessage();
				}
			}
		};
	}

	public int getUpdateInterval() {
		try {
			return parseInterval(getValue());
		} catch (QuantityConversionException e) {
			return UpdatePolicyToolkit.getDefaultUpdateInterval();
		}
	}

	private static int parseInterval(String value) throws QuantityConversionException {
		IQuantity q = TIMESPAN.parseInteractive(value);
		int interval = (int) q.longValueIn(MILLISECOND, Integer.MAX_VALUE);
		if (interval <= 0) {
			throw QuantityConversionException.tooLow(q, MILLISECOND.quantity(1));
		}
		return interval;
	}
}
