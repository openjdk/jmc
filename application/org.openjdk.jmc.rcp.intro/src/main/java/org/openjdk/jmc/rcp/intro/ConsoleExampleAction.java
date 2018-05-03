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

import java.util.Properties;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.intro.IIntroManager;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.intro.config.IIntroAction;

import org.openjdk.jmc.console.ui.editor.internal.ConsoleEditorInput;
import org.openjdk.jmc.rjmx.IServerDescriptor;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.servermodel.IServer;
import org.openjdk.jmc.rjmx.servermodel.IServerModel;
import org.openjdk.jmc.ui.common.util.Environment;

public class ConsoleExampleAction implements IIntroAction {

	@Override
	public void run(IIntroSite site, Properties params) {

		IWorkbenchWindow window = site.getWorkbenchWindow();
		String g = "org.openjdk.jmc.ui.idesupport.StandardPerspective"; //$NON-NLS-1$
		try {
			window.getWorkbench().showPerspective(g, window);
		} catch (WorkbenchException e) {
			IntroToolkit.logException(e);
		}
		IServerModel model = RJMXPlugin.getDefault().getService(IServerModel.class);
		for (IServer server : model.elements()) {
			IServerDescriptor descriptor = server.getServerHandle().getServerDescriptor();
			if (descriptor.getJvmInfo() != null
					&& Integer.valueOf(Environment.getThisPID()).equals(descriptor.getJvmInfo().getPid())) {
				try {
					IEditorInput ei = new ConsoleEditorInput(server.getServerHandle());
					window.getActivePage().openEditor(ei, ConsoleEditorInput.EDITOR_ID, true);
				} catch (PartInitException e) {
					IntroToolkit.logException(e);
				}
				IIntroManager manager = PlatformUI.getWorkbench().getIntroManager();
				manager.closeIntro(manager.getIntro());
				return;
			}
		}

	}

}
