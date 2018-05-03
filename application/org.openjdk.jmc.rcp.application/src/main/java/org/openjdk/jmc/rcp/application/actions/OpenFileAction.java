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
package org.openjdk.jmc.rcp.application.actions;

import java.io.File;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IFileEditorMapping;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.openjdk.jmc.rcp.application.ApplicationPlugin;
import org.openjdk.jmc.ui.MCPathEditorInput;
import org.openjdk.jmc.ui.WorkbenchToolkit;

/**
 * Class for opening a file
 */
public class OpenFileAction extends Action {
	private final static String KLEENE_STAR_DOT = "*."; //$NON-NLS-1$
	private final static String FILE_OPEN_FILTER_PATH = "file.open.filter.path"; //$NON-NLS-1$
	private final static String DEMO_DIRECTORY = "flightrecordings"; //$NON-NLS-1$
	private final IWorkbenchWindow window;

	public OpenFileAction(IWorkbenchWindow window) {
		super(Messages.OpenFileAction_OPEN_FILE_TITLE_TEXT);
		this.window = window;
		setEnabled(true);
	}

	private static String getDefaultFilterPath() {
		String result = getLastFilterPath();
		if (result == null) {
			result = getSamplePath();
			if (result == null) {
				result = getUserHomePath();
				if (result == null) {
					result = "./"; //$NON-NLS-1$
				}
			}
		}
		return result;
	}

	private static String getLastFilterPath() {
		return getIfExists(ApplicationPlugin.getDefault().getDialogSettings().get(FILE_OPEN_FILTER_PATH));
	}

	private static String getUserHomePath() {
		return getIfExists(System.getProperty("user.home")); //$NON-NLS-1$;
	}

	public static String getSamplePath() {
		String result = null;

		result = getJoinedIfExists(".", DEMO_DIRECTORY); //$NON-NLS-1$

		if (result == null) {
			result = getJoinedIfExists(getEclipseLaunchedSamplesFolder(), DEMO_DIRECTORY);
		}
		return result;
	}

	private static String getEclipseLaunchedSamplesFolder() {
		String app = org.eclipse.core.runtime.Platform.getBundle("org.openjdk.jmc.rcp.application").getLocation(); //$NON-NLS-1$
		app = app.substring("reference:file:/".length()); //$NON-NLS-1$
		File base = new File(app).getParentFile();
		return getJoinedIfExists(base.getAbsolutePath(), "org.openjdk.jmc.rcp.product/rootfiles/"); //$NON-NLS-1$
	}

	private static String getIfExists(String path) {
		if (exists(path)) {
			return path;
		} else {
			return null;
		}
	}

	private static boolean exists(String path) {
		if (path != null) {
			return new File(path).exists();
		} else {
			return false;
		}
	}

	private static String getJoinedIfExists(String base, String child) {
		if (base != null && child != null) {
			return getIfExists(new File(base, child).getPath());
		} else {
			return null;
		}
	}

	private static void setDefaultFilterPath(String path) {
		IDialogSettings settings = ApplicationPlugin.getDefault().getDialogSettings();
		settings.put(FILE_OPEN_FILTER_PATH, path);
	}

	@Override
	public void run() {
		openFile(window);
	}

	public static void openFile(IWorkbenchWindow inWindow) {
		FileDialog dialog = new FileDialog(inWindow.getShell(), SWT.OPEN | SWT.MULTI);
		dialog.setFilterPath(getDefaultFilterPath());
		setFilterNamesAndExtensions(dialog);
		dialog.setText(Messages.OpenFileAction_OPEN_FILE_TITLE);

		if (dialog.open() == null) {
			return;
		}

		String[] names = dialog.getFileNames();

		if (names != null) {
			String filterPath = dialog.getFilterPath();
			setDefaultFilterPath(filterPath);
			int numberOfFilesNotFound = 0;
			StringBuffer notFound = new StringBuffer();
			for (String name : names) {
				final File file = new File(filterPath + File.separator + name);
				if (file.exists()) {
					WorkbenchToolkit.openEditor(inWindow, new MCPathEditorInput(file));
				} else {
					if (++numberOfFilesNotFound > 1) {
						notFound.append('\n');
					}
					notFound.append(file.getName());
				}
			}

			if (numberOfFilesNotFound > 0) {
				String msgFmt = numberOfFilesNotFound == 1 ? Messages.OpenFileAction_CANT_FIND_SINGLE_FILE_TEXT
						: Messages.OpenFileAction_CANT_FIND_MULTIPLE_FILES_TEXT;
				String msg = NLS.bind(msgFmt, new Object[] {notFound.toString()});
				MessageDialog.openError(inWindow.getShell(), Messages.OpenFileAction_ERROR_WHEN_OPENING_FILE_TEXT_TITLE,
						msg);
			}
		}
	}

	private static void setFilterNamesAndExtensions(FileDialog dialog) {
		IFileEditorMapping[] fileExtensions = PlatformUI.getWorkbench().getEditorRegistry().getFileEditorMappings();
		String[] filterExtensions = new String[fileExtensions.length + 1];
		for (int i = 0; i < fileExtensions.length; i++) {
			filterExtensions[i] = KLEENE_STAR_DOT + fileExtensions[i].getExtension();
		}
		filterExtensions[fileExtensions.length] = "*"; //$NON-NLS-1$
		dialog.setFilterExtensions(filterExtensions);
		dialog.setFilterIndex(fileExtensions.length);
	}
}
