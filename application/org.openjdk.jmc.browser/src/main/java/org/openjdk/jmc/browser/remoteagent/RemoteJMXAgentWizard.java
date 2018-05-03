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
package org.openjdk.jmc.browser.remoteagent;

import java.util.Properties;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.openjdk.jmc.browser.attach.LocalJVMToolkit;
import org.openjdk.jmc.browser.attach.Messages;
import org.openjdk.jmc.rjmx.IServerHandle;
import org.openjdk.jmc.rjmx.internal.ServerToolkit;
import org.openjdk.jmc.ui.misc.DialogToolkit;

public class RemoteJMXAgentWizard extends Wizard {
	private final Boolean agentStarted;
	private final Properties currentSettings;
	private final String serverName;
	private final String pid;

	public RemoteJMXAgentWizard(IServerHandle serverHandle, Boolean agentStarted, Properties currentSettings) {
		super();
		serverName = ServerToolkit.getDisplayName(serverHandle);
		pid = String.valueOf(ServerToolkit.getPid(serverHandle));
		this.agentStarted = agentStarted;
		this.currentSettings = currentSettings;
		setWindowTitle(RemoteJMXAgentWizardPage.getTitle(agentStarted, serverName));
	}

	@Override
	public void addPages() {
		addPage(new RemoteJMXAgentWizardPage(serverName, agentStarted, currentSettings));
	}

	@Override
	public boolean performFinish() {

		final String command = ((RemoteJMXAgentWizardPage) getPage(RemoteJMXAgentWizardPage.PAGE_NAME)).getCommand();
		if (command != null) {
			try {
				final String result = LocalJVMToolkit.executeCommandForPid(pid, command, true);
				if (result.length() != 0) {
					DialogToolkit.showWarning(getShell(), Messages.RemoteJMXStarterAction_REMOTE_JMX_AGENT_RESULT_TITLE,
							NLS.bind(Messages.RemoteJMXStarterAction_REMOTE_JMX_AGENT_RESULT_DESCRIPTION,
									new String[] {serverName, command, result}));
				}
				return true;
			} catch (final Throwable e) {
				// FIXME: Would like to show the causing exception for the problem, which is actually printed on the console.
				DialogToolkit.showException(getShell(), Messages.RemoteJMXStarterAction_REMOTE_JMX_AGENT_PROBLEM_TITLE,
						NLS.bind(Messages.RemoteJMXStarterAction_REMOTE_JMX_AGENT_PROBLEM_DESCRIPTION, serverName,
								command),
						e);
			}
		}
		return false;
	}
}
