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
package org.openjdk.jmc.console.ui.editor.internal;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.XMLMemento;

import org.openjdk.jmc.ui.misc.MementoToolkit;

/**
 * Action to present a dialog to the user with the current state of the page. The shown XML can then
 * be migrated to {@code plugin.xml} or similar.
 */
public class ShowTabStateAction extends Action {

	private final ConsoleFormPage page;

	ShowTabStateAction(ConsoleFormPage page) {
		super("State"); //$NON-NLS-1$
		this.page = page;
	}

	@Override
	public void run() {
		XMLMemento state = XMLMemento.createWriteRoot("root"); //$NON-NLS-1$
		Shell current = Display.getCurrent().getActiveShell();
		if (page.saveState(state)) {
			final String stateString = MementoToolkit.asString(state);
			Dialog dialog = new Dialog(current) {
				@Override
				protected Control createDialogArea(Composite parent) {
					Text text = new Text(parent, SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL);
					text.setLayoutData(new GridData(900, 600));
					text.setText(stateString);
					return text;
				};
			};
			dialog.open();
		} else {
			MessageDialog.openInformation(current, "Tab state", "Tab saves no state"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
}
