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
package org.openjdk.jmc.rcp.application;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.openjdk.jmc.ui.MCPathEditorInput;
import org.openjdk.jmc.ui.WorkbenchToolkit;

/**
 * Listens for SWT.OpenDocument events, opens the file in the event if it exists.
 */
public class OpenDocumentEventProcessor implements Listener {
	private final ArrayList<String> filesToOpen = new ArrayList<>(1);

	@Override
	public synchronized void handleEvent(Event event) {
		if (event.type == SWT.OpenDocument) {
			String file = event.text;
			/*
			 * Eclipse executable on OS X (at least on 10.9 and 10.10) incorrectly interprets
			 * Spotlight search terms as filenames, seemingly with a strange prefix (slash + Unicode
			 * BOM), but we cannot be sure this always is the case. For now, ignore these that we
			 * won't be able to open anyways. Finder also sends this, but just the prefix.
			 */
			if ((file != null) && !file.startsWith("/\uFEFF")) { //$NON-NLS-1$
				filesToOpen.add(file);
			}
		}
	}

	public synchronized void openFiles() {
		if (filesToOpen.isEmpty()) {
			return;
		}

		String[] filePaths = filesToOpen.toArray(new String[filesToOpen.size()]);
		filesToOpen.clear();
		for (String path : filePaths) {
			WorkbenchToolkit.openEditor(new MCPathEditorInput(new File(path)));
		}
	}
}
