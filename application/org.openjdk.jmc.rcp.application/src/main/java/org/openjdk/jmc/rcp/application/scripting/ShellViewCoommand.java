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
package org.openjdk.jmc.rcp.application.scripting;

import java.util.logging.Level;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

import org.openjdk.jmc.ui.ActivitiesToolkit;
import org.openjdk.jmc.ui.UIPlugin;

public class ShellViewCoommand extends Action {
	private static final String SHELL_ACTIVITY = "org.openjdk.jmc.activity.shell"; //$NON-NLS-1$

	public ShellViewCoommand() {
		super("Show Shell"); //$NON-NLS-1$
		setId("org.openjdk.jmc.rcp.application.commands.shellview"); //$NON-NLS-1$
		setActionDefinitionId("org.openjdk.jmc.rcp.application.commands.shellview"); //$NON-NLS-1$
	}

	@Override
	public void run() {
		IWorkbenchWindow w = UIPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
		if (w != null) {
			IWorkbenchPage page = w.getActivePage();
			if (page != null) {
				toggleCommandView(page);
			}
		}
	}

	boolean isViewVisible(IWorkbenchPage page) {
		IViewPart part = getViewPart(page);
		if (part == null) {
			return false;
		}
		return page.isPartVisible(part);
	}

	private IViewPart getViewPart(IWorkbenchPage page) {
		return page.findView(ScriptView.ID);
	}

	private void toggleCommandView(IWorkbenchPage page) {
		if (!isViewVisible(page)) {
			showView(page);
		} else {
			hideView(page);
		}
	}

	private void hideView(IWorkbenchPage page) {
		IViewPart part = getViewPart(page);
		if (part != null) {
			ActivitiesToolkit.disableActivity(SHELL_ACTIVITY);
			page.hideView(part);
		}
	}

	private void showView(IWorkbenchPage page) {
		try {
			ActivitiesToolkit.enableActivity(SHELL_ACTIVITY);
			page.showView(ScriptView.ID);
		} catch (PartInitException e) {
			UIPlugin.getDefault().getLogger().log(Level.SEVERE, "Could not open command view"); //$NON-NLS-1$
		}
	}
}
