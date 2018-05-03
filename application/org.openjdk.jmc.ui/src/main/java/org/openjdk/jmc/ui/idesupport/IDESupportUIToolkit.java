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
package org.openjdk.jmc.ui.idesupport;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.openjdk.jmc.ui.common.util.Filename;
import org.openjdk.jmc.ui.misc.DialogToolkit;

/**
 */

// FIXME: This class hasn't got anything to do with IDESupport. Should be renamed to FileBrowserToolkit or something
public class IDESupportUIToolkit {

	private IDESupportUIToolkit() {
		// Toolkit
	}

	public static File browseForSaveAsFile(String title, File suggestedFile, String requiredExt, String description) {
		FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.SAVE);
		File parent = suggestedFile.getParentFile();
		if (parent != null && parent.isDirectory()) {
			dialog.setFilterPath(parent.getAbsolutePath());
		}
		String reqFileEnding = ""; //$NON-NLS-1$
		if (requiredExt != null) {
			dialog.setFilterExtensions(new String[] {"*." + requiredExt}); //$NON-NLS-1$
			reqFileEnding = "." + requiredExt; //$NON-NLS-1$
		}
		dialog.setFileName(suggestedFile.getName());
		dialog.setText(title);
		String file = dialog.open();
		if (file == null) {
			// User cancels operation
			return null;
		}
		if (!file.endsWith(reqFileEnding)) {
			file += reqFileEnding;
		}
		return new File(file);
	}

	public static File browseForOpenFile(String title, File suggestedFile) {
		FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.OPEN);
		File parent = suggestedFile.getParentFile();
		if (parent != null && parent.isDirectory()) {
			dialog.setFilterPath(parent.getAbsolutePath());
		}
		String fileExt = Filename.splitFilename(suggestedFile.getName()).getExtension();
		if (!fileExt.isEmpty()) {
			dialog.setFilterExtensions(new String[] {"*." + fileExt}); //$NON-NLS-1$
		}
		dialog.setText(title);
		String file = dialog.open();
		if (file != null) {
			return new File(file);
		}
		// User cancels operation
		return null;
	}

	/**
	 * @param theFile
	 * @return returns true if the file exists and the user does not want to overwrite it
	 */
	public static boolean checkAlreadyExists(File theFile) {
		if (theFile.exists()) {
			return !DialogToolkit.openQuestionOnUiThread(Messages.BasicIDESupport_FILE_ALREADY_EXISTS_TITLE,
					Messages.BasicIDESupport_FILE_ALREADY_MESSAGE_TEXT + SWT.LF + theFile.getAbsolutePath());
		}
		return false;
	}
}
