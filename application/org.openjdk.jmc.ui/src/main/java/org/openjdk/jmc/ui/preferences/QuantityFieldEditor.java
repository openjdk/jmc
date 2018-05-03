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
package org.openjdk.jmc.ui.preferences;

import java.text.MessageFormat;
import java.util.logging.Level;

import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.KindOfQuantity;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.misc.QuantityKindProposal;

public class QuantityFieldEditor extends StringFieldEditor {

	private final KindOfQuantity<?> type;
	private IQuantity min;
	private IQuantity max;

	public QuantityFieldEditor(String name, String labelText, Composite parent, KindOfQuantity<?> type) {
		this.type = type;
		init(name, labelText);
		createControl(parent);
		QuantityKindProposal.install(getTextControl(), type);
	}

	public void setValidRange(IQuantity min, IQuantity max) {
		if (!type.equals(min.getType()) || !type.equals(max.getType())) {
			throw new IllegalArgumentException(
					MessageFormat.format("Range limits {0} and {1} must be of kind {2}", min, max, type)); //$NON-NLS-1$
		}
		this.min = min;
		this.max = max;
	}

	@Override
	protected boolean checkState() {
		Text text = getTextControl();
		if (text == null) {
			return false;
		}
		try {
			validateQuantity(type.parseInteractive(text.getText()));
			clearErrorMessage();
			return true;
		} catch (QuantityConversionException e) {
			showErrorMessage(e.getLocalizedMessage());
		}
		return false;
	}

	protected void validateQuantity(IQuantity value) throws QuantityConversionException {
		if (min != null && min.compareTo(value) > 0) {
			throw QuantityConversionException.tooLow(value, min);
		}
		if (max != null && max.compareTo(value) < 0) {
			throw QuantityConversionException.tooHigh(value, max);
		}
	}

	@Override
	protected void doLoad() {
		Text text = getTextControl();
		if (text != null) {
			String value = getInteractiveValue(false);
			text.setText(value);
			oldValue = value;
		}
		valueChanged();
	}

	@Override
	protected void doLoadDefault() {
		Text text = getTextControl();
		if (text != null) {
			text.setText(getInteractiveValue(true));
		}
		valueChanged();
	}

	private String getInteractiveValue(boolean defaultValue) {
		IQuantity value = doGetQuantity(defaultValue);
		return value == null ? "" : value.interactiveFormat(); //$NON-NLS-1$
	}

	protected IQuantity doGetQuantity(boolean defaultValue) {
		try {
			if (defaultValue) {
				return type.parsePersisted(getPreferenceStore().getDefaultString(getPreferenceName()));
			} else {
				return type.parsePersisted(getPreferenceStore().getString(getPreferenceName()));
			}
		} catch (QuantityConversionException e) {
			UIPlugin.getDefault().getLogger().log(Level.WARNING, "Could not load value for " + getPreferenceName(), e); //$NON-NLS-1$
			return null;
		}
	}

	protected void doSetQuantity(IQuantity value) throws QuantityConversionException {
		getPreferenceStore().setValue(getPreferenceName(), value.persistableString());
	}

	@Override
	protected void doStore() {
		Text text = getTextControl();
		try {
			if (text != null) {
				doSetQuantity(type.parseInteractive(text.getText()));
			}
		} catch (QuantityConversionException e) {
			// FIXME: The conversion to a long in storageUnit shouldn't be clamped and could overflow, if it was performed in checkState()
		}
	}
}
