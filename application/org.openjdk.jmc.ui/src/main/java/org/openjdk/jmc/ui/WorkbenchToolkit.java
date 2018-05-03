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
package org.openjdk.jmc.ui;

import java.util.logging.Level;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import org.openjdk.jmc.ui.misc.DialogToolkit;
import org.openjdk.jmc.ui.misc.DisplayToolkit;

public class WorkbenchToolkit {

	public static void asyncOpenEditor(final IPathEditorInput ei) {
		DisplayToolkit.safeAsyncExec(new Runnable() {

			@Override
			public void run() {
				openEditor(ei);
			}

		});
	}

	public static void openEditor(IPathEditorInput ei) {
		openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow(), ei);
	}

	public static void openEditor(IWorkbenchWindow window, IPathEditorInput ei) {
		IEditorDescriptor editorDesc = PlatformUI.getWorkbench().getEditorRegistry()
				.getDefaultEditor(ei.getPath().lastSegment());
		Shell shell = Display.getCurrent().getActiveShell();
		if (window == null) {
			DialogToolkit.showError(shell, Messages.COULD_NOT_OPEN_EDITOR, Messages.WORKBENCH_WINDOW_NOT_AVAILABLE);
			UIPlugin.getDefault().getLogger().severe("Workbench Window not available"); //$NON-NLS-1$
		} else if (editorDesc == null) {
			DialogToolkit.showError(shell, Messages.COULD_NOT_OPEN_EDITOR,
					NLS.bind(Messages.NO_ASSOCIATED_EDITOR, ei.getName()));
			UIPlugin.getDefault().getLogger().severe("Editor not found for " + ei.getName()); //$NON-NLS-1$
		} else {
			try {
				window.getActivePage().openEditor(ei, editorDesc.getId());
			} catch (PartInitException e) {
				DialogToolkit.showException(shell, Messages.COULD_NOT_OPEN_EDITOR, e.getLocalizedMessage(), e);
				UIPlugin.getDefault().getLogger().log(Level.SEVERE, "Failed to open " + editorDesc.getId(), e); //$NON-NLS-1$
			}
		}
	}

	public static void asyncCloseEditor(final IEditorPart editor) {
		DisplayToolkit.safeAsyncExec(new Runnable() {

			@Override
			public void run() {
				editor.getSite().getPage().closeEditor(editor, false);
			}
		});
	}
}
