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
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.FileDialog;

import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.rcp.application.scripting.model.OperatingSystem;
import org.openjdk.jmc.ui.UIPlugin;

/**
 * Loads a script program
 */
public final class LoadAction extends ProcessAction {
	private final StyledText m_styleText;

	public LoadAction(StyledText s, OperatingSystem os) {
		super(os, "Load", IAction.AS_PUSH_BUTTON); //$NON-NLS-1$
		setImageDescriptor(UIPlugin.getDefault().getMCImageDescriptor(UIPlugin.ICON_IMPORT));
		setDisabledImageDescriptor(UIPlugin.getDefault().getMCImageDescriptor(UIPlugin.ICON_IMPORT));
		m_styleText = s;
	}

	@Override
	public void run() {
		FileDialog fileDialog = new FileDialog(m_styleText.getShell(), SWT.OPEN);
		fileDialog.setFilterNames(new String[] {"Mission Control Script File (*.mcs)" //$NON-NLS-1$
		});
		fileDialog.setFilterExtensions(new String[] {"*.mcs" //$NON-NLS-1$
		});
		fileDialog.setFilterIndex(0);
		fileDialog.setFileName("*.mcs"); //$NON-NLS-1$
		String fileName = fileDialog.open();
		if (fileName != null) {
			load(fileName);
		}
	}

	private void load(String fileName) {
		try {
			List<String> sourceCode = IOToolkit.loadFromFile(new File(fileName));
			m_styleText.setText(""); //$NON-NLS-1$
			m_styleText.setStyleRanges(new StyleRange[0]);
			StringBuffer s = new StringBuffer();
			for (String line : sourceCode) {
				s.append(line);
				s.append('\r');
			}
			m_styleText.append(s.toString());
		} catch (IOException e) {
			MessageDialog.openError(m_styleText.getShell(), "Error loading program", e.getMessage()); //$NON-NLS-1$
		}
	}

	@Override
	protected void updateStatus() {
		setEnabled(!getProcess().isRunning());
	}
}
