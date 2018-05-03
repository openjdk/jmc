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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Composite;

import org.openjdk.jmc.ui.celleditors.ClearableTextCellEditor;

public class CharacterEditor extends ClearableTextCellEditor {

	private static final String PREFIX = "\\u"; //$NON-NLS-1$
	private Character c;
	private boolean allowNull;

	public CharacterEditor(Composite parent, boolean allowNull) {
		super(parent);
		this.allowNull = allowNull;
		text.addVerifyListener(new VerifyListener() {

			@Override
			public void verifyText(VerifyEvent e) {
				StringBuilder sb = new StringBuilder(text.getText());
				if (e.character == SWT.BS || e.character == SWT.DEL) {
					sb.delete(e.start, e.end);
				} else {
					sb.replace(e.start, e.end, e.text);
				}
				e.doit = sb.length() < 2 || isUnicodeChar(sb.toString());
			}

			boolean isUnicodeChar(String str) {
				if (str.equals(PREFIX)) {
					return true;
				} else if (str.length() > 6 || !str.startsWith(PREFIX)) {
					return false;
				}
				for (char c : str.substring(PREFIX.length()).toCharArray()) {
					if (Character.digit(c, 16) < 0) {
						return false;
					}
				}
				return true;
			}
		});
	}

	@Override
	public boolean allowClear() {
		return allowNull;
	}

	@Override
	protected void editOccured(ModifyEvent e) {
		try {
			c = parseChar(text.getText().trim());
			setValueValid(true);
		} catch (Exception ex) {
			setValueValid(false);
		}
	}

	private Character parseChar(String str) {
		if (allowNull && "".equals(str)) { //$NON-NLS-1$
			return null;
		} else if (str.length() == 1) {
			return str.charAt(0);
		} else {
			return Character.valueOf((char) Integer.parseInt(str.substring(PREFIX.length()), 16));
		}
	}

	private static String formatChar(Character c) {
		int charValue = c.charValue();
		if (charValue >= 32 && charValue < 256) {
			return c.toString();
		} else {
			return PREFIX + String.format("%04X", charValue); //$NON-NLS-1$
		}
	}

	@Override
	protected void doSetValue(Object value) {
		c = (Character) value;
		super.doSetValue(value == null ? "" : formatChar(c)); //$NON-NLS-1$
	}

	@Override
	protected Object doGetValue() {
		return c;
	}

}
