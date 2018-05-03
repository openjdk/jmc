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
package org.openjdk.jmc.rcp.intro;

import java.io.File;
import java.util.Properties;
import java.util.logging.Level;

import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.intro.IIntroManager;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.intro.config.IIntroAction;

import org.openjdk.jmc.rcp.application.actions.OpenFileAction;
import org.openjdk.jmc.ui.MCPathEditorInput;
import org.openjdk.jmc.ui.WorkbenchToolkit;
import org.openjdk.jmc.ui.misc.DialogToolkit;

/**
 * Intro action for opening an example flight recording
 */
// FIXME: This action is currently not used, but if we want to use it, we need to update this to use something other than sample recording.
public class FlightRecordingExampleAction extends Object implements IIntroAction {

	@Override
	public void run(IIntroSite site, Properties params) {
		File file = null;
		boolean readable = false;
		String filename = OpenFileAction.getSamplePath() + File.separator + params.getProperty("file"); //$NON-NLS-1$
		IntroToolkit.getLogger().log(Level.INFO, "Trying to open flight recording sample from: " + filename); //$NON-NLS-1$

		try {
			file = new File(filename);
			readable = file.canRead();
		} catch (Exception e) {
			IntroToolkit.logException(e);
			DialogToolkit.showException(site.getShell(), Messages.OPEN_SAMPLE_RECORDING_PROBLEM,
					NLS.bind(Messages.OPEN_SAMPLE_RECORDING_EXCEPTION, filename), e);
		}
		if (readable) {
			WorkbenchToolkit.openEditor(site.getWorkbenchWindow(), new MCPathEditorInput(file));
			IIntroManager manager = PlatformUI.getWorkbench().getIntroManager();
			manager.closeIntro(manager.getIntro());
		} else {
			DialogToolkit.showError(site.getShell(), Messages.OPEN_SAMPLE_RECORDING_PROBLEM,
					NLS.bind(Messages.OPEN_SAMPLE_RECORDING_NONEXISTENT, filename));
		}
	}
}
