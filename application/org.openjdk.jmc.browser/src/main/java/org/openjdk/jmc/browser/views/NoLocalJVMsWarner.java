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
package org.openjdk.jmc.browser.views;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.widgets.Control;
import org.openjdk.jmc.attach.AttachToolkit;
import org.openjdk.jmc.browser.JVMBrowserPlugin;
import org.openjdk.jmc.browser.attach.LocalJVMToolkit;
import org.openjdk.jmc.ui.misc.DisplayToolkit;

/**
 * Warns if there are no local jvms available. Shows warning when the control is painted.
 */
public class NoLocalJVMsWarner {

	public static void warnIfNoLocalJVMs(final Control control) {
		PaintListener noLocalJVMWarner = new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				control.removePaintListener(this);
				new Thread(new Runnable() {
					@Override
					public void run() {
						if (!AttachToolkit.isLocalAttachAvailable()
								|| LocalJVMToolkit.getAttachableJVMs().length == 0) {
							showNoLocalJVMsWarning(control);
						}
					}
				}).start();
			}
		};
		control.addPaintListener(noLocalJVMWarner);
	}

	private static void showNoLocalJVMsWarning(final Control control) {
		DisplayToolkit.safeAsyncExec(control, new Runnable() {
			@Override
			public void run() {
				// FIXME: Show a link to the FAQ section of the online help. Need to create special dialog with FormText.
				MessageDialogWithToggle warnDialog = MessageDialogWithToggle.openWarning(control.getShell(),
						Messages.JVMBrowserView_NO_LOCAL_JVMS_TITLE,
						Messages.JVMBrowserView_NO_LOCAL_JVMS_MESSAGE + System.getProperty("line.separator") //$NON-NLS-1$
								+ Messages.JVMBrowserView_NO_LOCAL_JVMS_WARN_CAUSE,
						Messages.JVMBrowserView_NO_LOCAL_JVMS_WARN_PREFERENCE, true, null, null);
				if (warnDialog.getReturnCode() == IDialogConstants.OK_ID) {
					if (!warnDialog.getToggleState()) {
						JVMBrowserPlugin.getDefault().setWarnNoLocalJVMs(false);
					}
				}
			}
		});
	}

}
