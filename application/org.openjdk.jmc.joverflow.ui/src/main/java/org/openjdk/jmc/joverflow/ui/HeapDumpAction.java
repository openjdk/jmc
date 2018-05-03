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
package org.openjdk.jmc.joverflow.ui;

import java.io.File;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;

import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.IServerHandle;
import org.openjdk.jmc.rjmx.actionprovider.IActionFactory;
import org.openjdk.jmc.ui.MCPathEditorInput;
import org.openjdk.jmc.ui.WorkbenchToolkit;
import org.openjdk.jmc.ui.common.CorePlugin;
import org.openjdk.jmc.ui.common.action.Executable;
import org.openjdk.jmc.ui.common.jvm.JVMDescriptor;
import org.openjdk.jmc.ui.common.util.Filename;
import org.openjdk.jmc.ui.misc.DialogToolkit;
import org.openjdk.jmc.ui.misc.DisplayToolkit;

public class HeapDumpAction implements IActionFactory {

	private final static String DEFAULT_FILENAME = "dump_";
	private final static String HPROF_FILE_EXTENSION = "hprof";

	private static class FileOpener implements Runnable {
		File file;

		@Override
		public void run() {
			WorkbenchToolkit.openEditor(new MCPathEditorInput(file));
		}
	}

	@Override
	public Executable createAction(final IServerHandle serverHandle) {
		return new Executable() {

			@Override
			public void execute() {
				IConnectionHandle connector = null;
				try {
					JVMDescriptor jvmInfo = serverHandle.getServerDescriptor().getJvmInfo();
					FileOpener opener = getFileOpener(jvmInfo != null && jvmInfo.isAttachable());
					if (opener.file != null) {
						connector = serverHandle.connect("Create Heap Dump");
						MBeanServerConnection connection = connector.getServiceOrThrow(MBeanServerConnection.class);
						Object[] params = new Object[] {opener.file.getAbsolutePath(), Boolean.TRUE};
						String[] sig = new String[] {String.class.getName(), boolean.class.getName()};
						connection.invoke(new ObjectName("com.sun.management:type=HotSpotDiagnostic"), "dumpHeap", params, sig);
						DisplayToolkit.safeAsyncExec(opener);
					}
				} catch (Exception e) {
					Throwable root = e;
					while (root.getCause() != null) {
						root = root.getCause();
					}
					final String message = root.getMessage() != null ? root.getMessage() : root.toString();
					DisplayToolkit.safeAsyncExec(new Runnable() {

						@Override
						public void run() {
							DialogToolkit.showError(Display.getCurrent().getActiveShell(),
									"Failed to create Heap Dump", message);
						}
					});
				} finally {
					IOToolkit.closeSilently(connector);
				}
			}
		};
	}

	private static FileOpener getFileOpener(boolean forLocalServer) {
		if (forLocalServer) {
			FileOpener opener = new FileOpener();
			File dir = CorePlugin.getDefault().getWorkspaceDirectory();
			Filename filename = new Filename(DEFAULT_FILENAME, HPROF_FILE_EXTENSION);
			opener.file = new File(dir, filename.toString());
			while (opener.file.exists()) {
				opener.file = new File(dir, filename.asRandomFilename().toString());
			}
			return opener;
		} else {
			final FileOpener opener = new FileOpener() {

				@Override
				public void run() {
					FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.OPEN);
					if (file.exists()) {
						dialog.setFilterPath(file.getPath());
					}
					dialog.setFilterExtensions(new String[] {"*." + HPROF_FILE_EXTENSION});
					dialog.setText("Locate the hprof file on your local filesystem");
					String filePath = dialog.open();
					if (filePath != null) {
						file = new File(filePath);
						super.run();
					}
				}
			};
			DisplayToolkit.safeSyncExec(new Runnable() {
				@Override
				public void run() {
					InputDialog dialog = new InputDialog(Display.getCurrent().getActiveShell(), "Enter a destination file",
							"Enter a path to the destination file in the remote filesystem. "
									+ "You will have to make the file available in the local filesystem manually, "
									+ "for example by moving it or using a shared filesystem.", "", null);
					if (dialog.open() == Window.OK) {
						String s = dialog.getValue();
						opener.file = new File(s.endsWith(HPROF_FILE_EXTENSION) ? s : s + "." + HPROF_FILE_EXTENSION);
					}
				}
			});
			return opener;
		}

	}
}
