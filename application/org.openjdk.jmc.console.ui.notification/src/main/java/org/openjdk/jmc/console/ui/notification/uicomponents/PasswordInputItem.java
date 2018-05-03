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
package org.openjdk.jmc.console.ui.notification.uicomponents;

import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Text;
import org.openjdk.jmc.rjmx.triggers.fields.internal.Field;
import org.openjdk.jmc.rjmx.triggers.fields.internal.Field.FieldValueChangeListener;
import org.openjdk.jmc.rjmx.triggers.fields.internal.PasswordField;
import org.openjdk.jmc.ui.uibuilder.IUIBuilder;

/**
 * A password field input for trigger rules. Will store the actual password (hidden) in the text
 * field. PasswordField holds the actual value encrypted.
 */
public class PasswordInputItem extends InputItem implements FieldValueChangeListener {

	private Text m_password;

	public PasswordInputItem(PasswordField field, IUIBuilder builder) {
		super(field, builder);
	}

	private Text getText() {
		return m_password;
	}

	@Override
	protected void create() {
		createUI();
		addListeners();
	}

	private void createUI() {
		getUIBuilder().createLabel(getField().getName() + InputItem.FIELD_LABEL_SUFFIX, getField().getDescription());
		m_password = getUIBuilder().createText(getField().getPassword(), getField().getDescription(), SWT.PASSWORD);
		getUIBuilder().layout();
	}

	private void addListeners() {
		getField().addFieldValueListener(this);
		getText().addFocusListener(new FieldFocusListener(getText(), getField()));
		getText().addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				getField().removeFieldValueListener(PasswordInputItem.this);
			}
		});
		getText().addModifyListener(new ModifyListener() {
			// FIXME: Memory leak. Color instances need to be disposed but we must be sure that they are not used anywhere else first.
			private Color m_defaultColor = null;

			@Override
			public void modifyText(ModifyEvent e) {
				if (m_defaultColor == null) {
					m_defaultColor = m_password.getForeground();
				}
				if (!getField().validateValue(m_password.getText())) {
					getText().setForeground(JFaceColors.getErrorText(getText().getDisplay()));
				} else {
					getText().setForeground(m_defaultColor);
				}
			}
		});
	}

	@Override
	public void onChange(Field freshField) {
		if (freshField instanceof PasswordField) {
			getText().setText(getField().getPassword());
		} else {
			getText().setText(getField().getValue());
		}
	}
}
