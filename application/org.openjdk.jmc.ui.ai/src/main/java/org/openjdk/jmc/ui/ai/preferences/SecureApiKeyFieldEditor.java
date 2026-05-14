/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, Datadog, Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.ui.ai.preferences;

import java.util.logging.Level;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.openjdk.jmc.common.security.ISecurityManager;
import org.openjdk.jmc.common.security.SecurityException;
import org.openjdk.jmc.common.security.SecurityManagerFactory;
import org.openjdk.jmc.ui.ai.AIPlugin;

/**
 * A password-masked field editor that stores its value in JMC's PBE-encrypted SecureStore rather
 * than in plaintext plugin preferences.
 */
public class SecureApiKeyFieldEditor extends FieldEditor {

	private Text text;

	public SecureApiKeyFieldEditor(String name, String labelText, Composite parent) {
		init(name, labelText);
		createControl(parent);
	}

	@Override
	protected void adjustForNumColumns(int numColumns) {
		((GridData) text.getLayoutData()).horizontalSpan = numColumns - 1;
	}

	@Override
	protected void doFillIntoGrid(Composite parent, int numColumns) {
		getLabelControl(parent);
		text = new Text(parent, SWT.BORDER | SWT.PASSWORD | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = numColumns - 1;
		text.setLayoutData(gd);
		text.addModifyListener(e -> setPresentsDefaultValue(false));
	}

	@Override
	protected void doLoad() {
		if (text == null) {
			return;
		}
		try {
			ISecurityManager sm = SecurityManagerFactory.getSecurityManager();
			if (sm != null && sm.hasKey(getPreferenceName())) {
				Object val = sm.get(getPreferenceName());
				if (val instanceof String[]) {
					String[] arr = (String[]) val;
					text.setText(arr.length > 0 && arr[0] != null ? arr[0] : ""); //$NON-NLS-1$
					return;
				}
			}
		} catch (SecurityException e) {
			AIPlugin.getLogger().log(Level.WARNING, "Failed to load API key from secure store", e); //$NON-NLS-1$
		}
		text.setText(""); //$NON-NLS-1$
	}

	@Override
	protected void doLoadDefault() {
		if (text != null) {
			text.setText(""); //$NON-NLS-1$
		}
		setPresentsDefaultValue(true);
	}

	@Override
	protected void doStore() {
		if (text == null) {
			return;
		}
		String value = text.getText();
		try {
			ISecurityManager sm = SecurityManagerFactory.getSecurityManager();
			if (sm != null) {
				sm.storeWithKey(getPreferenceName(), value);
			} else {
				AIPlugin.getLogger().log(Level.WARNING,
						"SecureStore unavailable; API key for " + getPreferenceName() + " was not saved"); //$NON-NLS-1$
			}
		} catch (SecurityException e) {
			AIPlugin.getLogger().log(Level.WARNING, "Failed to store API key in secure store", e); //$NON-NLS-1$
		}
	}

	@Override
	public int getNumberOfControls() {
		return 2;
	}
}
