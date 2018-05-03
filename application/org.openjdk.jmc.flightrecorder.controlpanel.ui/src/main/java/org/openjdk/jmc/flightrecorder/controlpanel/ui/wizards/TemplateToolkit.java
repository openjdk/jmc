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
package org.openjdk.jmc.flightrecorder.controlpanel.ui.wizards;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.swt.widgets.Display;

import org.openjdk.jmc.flightrecorder.configuration.events.IEventConfiguration;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.XMLModel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfiguration;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfigurationRepository;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.PrivateStorageDelegate;
import org.openjdk.jmc.ui.misc.DialogToolkit;

public class TemplateToolkit {

	/**
	 * Import the given files into the given repository.
	 *
	 * @param repository
	 *            the repository to import into
	 * @param files
	 *            the files to import
	 * @return a list of the templates that were actually imported, never null
	 */
	public static List<IEventConfiguration> importFilesTo(
		EventConfigurationRepository repository, Collection<File> files) {
		List<IEventConfiguration> imported = new ArrayList<>(files.size());
		StringBuilder errBuf = new StringBuilder();
		for (File file : files) {
			try {
				XMLModel model = EventConfiguration.createModel(file);
				IEventConfiguration template = new EventConfiguration(model, PrivateStorageDelegate.getDelegate());
				if (repository.addAsUnique(template)) {
					imported.add(template);
				} else {
					errBuf.append(file.getName());
					errBuf.append(" (local storage)\n"); //$NON-NLS-1$
				}
			} catch (IOException ioe) {
				// FIXME: Create IStatus:es instead, and show them in a generic IStatus dialog with expandable details.
				errBuf.append(file.getName());
				errBuf.append(" (" + ioe.getMessage() + ")\n"); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (ParseException pe) {
				// FIXME: Create IStatus:es instead, and show them in a generic IStatus dialog with expandable details.
				errBuf.append(file.getName());
				errBuf.append(" (" + pe.getMessage() + ")\n"); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (IllegalArgumentException iae) {
				// FIXME: Create IStatus:es instead, and show them in a generic IStatus dialog with expandable details.
				errBuf.append(file.getName());
				errBuf.append(" (" + iae.getMessage() + ")\n"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		if (errBuf.length() != 0) {
			DialogToolkit.showErrorDialogAsync(Display.getDefault(),
					Messages.IMPORT_FILE_TEMPLATE_WIZARD_PAGE_ERROR_DIALOG_TITLE,
					Messages.IMPORT_FILE_TEMPLATE_WIZARD_PAGE_ERROR_DIALOG_MSG + errBuf.toString());
		}
		return imported;
	}
}
