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

import org.eclipse.jface.wizard.IWizard;
import org.openjdk.jmc.browser.attach.BrowserAttachPlugin;
import org.openjdk.jmc.browser.attach.Messages;
import org.openjdk.jmc.rjmx.IServerHandle;
import org.openjdk.jmc.ui.wizards.AbstractWizardUserAction;

public class RemoteJMXAgentWizardAction extends AbstractWizardUserAction {

	private final IServerHandle serverHandle;

	public RemoteJMXAgentWizardAction(IServerHandle serverHandle) {
		// NOTE: Can't change action name depending on agent state, because the state can change after the action is created.
		super(Messages.RemoteJMXStarterAction_CONTROL_REMOTE_JMX_AGENT,
				Messages.RemoteJMXStarterAction_CONTROL_REMOTE_JMX_AGENT_DESCRIPTION,
				BrowserAttachPlugin.getDefault().getImageDescriptor("icons/antenna_play_stop_16.png")); //$NON-NLS-1$
		this.serverHandle = serverHandle;
	}

	@Override
	public IWizard doCreateWizard() throws Exception {

		Properties currentAgentSettings = RemoteJMXAgentSettings.getCurrentAgentSettings(serverHandle);
		RemoteJMXAgentWizard wizard = new RemoteJMXAgentWizard(serverHandle,
				currentAgentSettings != null ? !currentAgentSettings.isEmpty() : null, currentAgentSettings);
		return wizard;
	}
}
