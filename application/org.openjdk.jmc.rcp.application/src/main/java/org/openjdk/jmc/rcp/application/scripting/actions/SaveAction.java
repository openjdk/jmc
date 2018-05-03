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
package org.openjdk.jmc.rcp.application.scripting.actions;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.rcp.application.scripting.model.OperatingSystem;
import org.openjdk.jmc.ui.UIPlugin;

/**
 * Saves a script to a file
 */
public class SaveAction extends ProcessAction {
	private final Shell m_shell;

	public SaveAction(Shell shell, OperatingSystem os) {
		super(os, "Save", IAction.AS_PUSH_BUTTON); //$NON-NLS-1$
		setImageDescriptor(UIPlugin.getDefault().getMCImageDescriptor(UIPlugin.ICON_EXPORT));
		setDisabledImageDescriptor(UIPlugin.getDefault().getMCImageDescriptor(UIPlugin.ICON_EXPORT));

		m_shell = shell;
	}

	@Override
	public void run() {
		FileDialog file = new FileDialog(m_shell, SWT.SAVE);
		file.setFilterNames(new String[] {"Mission Control Script File (*.mcs)"}); //$NON-NLS-1$
		file.setFilterExtensions(new String[] {"*.mcs"}); //$NON-NLS-1$
		file.setFileName("script.mcs"); //$NON-NLS-1$
		String fileName = file.open();
		try {
			List<String> sourceCodeList = getProcess().getProgram().getSourceLines();
			IOToolkit.saveToFile(new File(fileName), sourceCodeList);
		} catch (IOException e) {
			MessageDialog.openError(m_shell, "Error loading program", e.getMessage()); //$NON-NLS-1$
		}
	}

	@Override
	protected void updateStatus() {
		// always enabled
	}
}
