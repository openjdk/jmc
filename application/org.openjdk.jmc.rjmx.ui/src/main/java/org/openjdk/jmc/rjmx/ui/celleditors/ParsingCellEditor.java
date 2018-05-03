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
package org.openjdk.jmc.rjmx.ui.celleditors;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.widgets.Composite;

import org.openjdk.jmc.ui.celleditors.ClearableTextCellEditor;
import org.openjdk.jmc.ui.misc.ControlDecorationToolkit;

public class ParsingCellEditor extends ClearableTextCellEditor {

	private final ControlDecoration errorDecorator;
	private Object value;

	public ParsingCellEditor(Composite parent) {
		super(parent);
		errorDecorator = ControlDecorationToolkit.createErrorDecorator(text);
	}

	@Override
	public void activate() {
		super.activate();
		errorDecorator.hide();
	}

	@Override
	protected void editOccured(ModifyEvent e) {
		try {
			String str = text.getText();
			value = (allowClear() && "".equals(str.trim())) ? null : parse(str); //$NON-NLS-1$
			setValueValid(true);
			errorDecorator.hide();
		} catch (Exception ex) {
			errorDecorator.setDescriptionText(ex.getLocalizedMessage());
			errorDecorator.show();
			setValueValid(false);
		}
	}

	protected Object parse(String str) throws Exception {
		return str;
	}

	protected String format(Object value) {
		return value == null ? "" : value.toString(); //$NON-NLS-1$
	}

	@Override
	protected void doSetValue(Object value) {
		this.value = value;
		super.doSetValue(format(value));
	}

	@Override
	protected Object doGetValue() {
		return value;
	}

}
