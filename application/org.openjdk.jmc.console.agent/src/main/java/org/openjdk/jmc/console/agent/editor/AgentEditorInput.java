/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, 2021 Red Hat Inc. All rights reserved.
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
package org.openjdk.jmc.console.agent.editor;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.openjdk.jmc.console.agent.AgentJmxHelper;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.IServerHandle;

public class AgentEditorInput implements IEditorInput {

	private final IServerHandle serverHandle;
	private final IConnectionHandle connectionHandle;
	private final AgentJmxHelper agentJmxHelper;

	public AgentEditorInput(IServerHandle serverHandle, IConnectionHandle connectionHandle,
			AgentJmxHelper agentJmxHelper) {
		this.serverHandle = serverHandle;
		this.connectionHandle = connectionHandle;
		this.agentJmxHelper = agentJmxHelper;
	}

	@Override
	public boolean exists() {
		return true;
	}

	public IServerHandle getServerHandle() {
		return serverHandle;
	}

	public IConnectionHandle getConnectionHandle() {
		return connectionHandle;
	}

	public AgentJmxHelper getAgentJmxHelper() {
		return agentJmxHelper;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_ELEMENT);
	}

	@Override
	public String getName() {
		return serverHandle.getServerDescriptor().getDisplayName();
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return serverHandle.getServerDescriptor().getDisplayName();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof AgentEditorInput && ((AgentEditorInput) obj).serverHandle.equals(serverHandle);
	}

	@Override
	public int hashCode() {
		return serverHandle.hashCode();
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

}
