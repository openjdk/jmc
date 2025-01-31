/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, 2025, Red Hat Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.console.agent.wizards;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.openjdk.jmc.console.agent.AgentJmxHelper;
import org.openjdk.jmc.console.agent.editor.AgentEditor;
import org.openjdk.jmc.console.agent.editor.AgentEditorInput;
import org.openjdk.jmc.console.agent.messages.internal.Messages;
import org.openjdk.jmc.common.jvm.JVMDescriptor;
import org.openjdk.jmc.ui.misc.DialogToolkit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class StartAgentWizard extends Wizard {

	private final AgentJmxHelper helper;
	private final StartAgentWizardPage startAgentWizardPage;

	public StartAgentWizard(AgentJmxHelper helper) {
		this.helper = helper;
		startAgentWizardPage = new StartAgentWizardPage(helper);
		this.setHelpAvailable(false);
		WizardDialog.setDialogHelpAvailable(false);
	}

	@Override
	public boolean canFinish() {
		return !startAgentWizardPage.getAgentJarPath().isEmpty();
	}

	@Override
	public boolean performFinish() {
		JVMDescriptor targetJvm = startAgentWizardPage.getTargetJvm();
		String agentJarPath = startAgentWizardPage.getAgentJarPath();
		String agentXmlPath = startAgentWizardPage.getAgentXmlPath();

		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		try {
			VirtualMachine vm = VirtualMachine.attach(targetJvm.getPid() + "");
			loadAgent(vm, agentJarPath, agentXmlPath);
			IEditorInput ei = new AgentEditorInput(helper.getServerHandle(), helper.getConnectionHandle(), helper);
			window.getActivePage().openEditor(ei, AgentEditor.EDITOR_ID, true);
		} catch (IllegalArgumentException e) {
			DialogToolkit.showException(window.getShell(), Messages.StartAgentWizard_MESSAGE_FAILED_TO_START_AGENT,
					Messages.StartAgentWizard_MESSAGE_INVALID_AGENT_CONFIG, e);
			return false;
		} catch (AgentLoadException e) {
			DialogToolkit.showException(window.getShell(), Messages.StartAgentWizard_MESSAGE_FAILED_TO_START_AGENT,
					Messages.StartAgentWizard_MESSAGE_FAILED_TO_LOAD_AGENT, e);
			return false;
		} catch (AttachNotSupportedException | IOException e) {
			DialogToolkit.showException(window.getShell(), Messages.StartAgentWizard_MESSAGE_FAILED_TO_START_AGENT,
					Messages.StartAgentWizard_MESSAGE_UNEXPECTED_ERROR_HAS_OCCURRED, e);
			return false;
		} catch (AgentInitializationException e) {
			DialogToolkit.showException(window.getShell(), Messages.StartAgentWizard_MESSAGE_FAILED_TO_START_AGENT,
					Messages.StartAgentWizard_MESSAGE_ACCESS_TO_UNSAFE_REQUIRED, e);
			return false;
		} catch (PartInitException e) {
			DialogToolkit.showException(window.getShell(),
					Messages.StartAgentWizard_MESSAGE_FAILED_TO_OPEN_AGENT_EDITOR,
					Messages.StartAgentWizard_MESSAGE_UNEXPECTED_ERROR_HAS_OCCURRED, e);
			return false;
		}

		return true;
	}

	private void loadAgent(VirtualMachine vm, String agentJar, String xmlPath)
			throws IOException, AgentLoadException, AgentInitializationException {
		if (agentJar.isEmpty() || Files.notExists(Paths.get(agentJar))) {
			throw new IllegalArgumentException("the Agent JAR path does exists");
		}

		if (!xmlPath.isEmpty() && Files.notExists(Paths.get(xmlPath))) {
			throw new IllegalArgumentException("the Agent configuration path does exists");
		}

		vm.loadAgent(agentJar, xmlPath);
	}

	@Override
	public void addPages() {
		addPage(startAgentWizardPage);
	}
}
