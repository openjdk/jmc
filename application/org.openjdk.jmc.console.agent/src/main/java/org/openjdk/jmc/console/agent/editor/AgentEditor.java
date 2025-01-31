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
package org.openjdk.jmc.console.agent.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.ProgressIndicator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;
import org.openjdk.jmc.console.agent.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.rjmx.IServerHandle;
import org.openjdk.jmc.rjmx.common.IConnectionHandle;
import org.openjdk.jmc.rjmx.common.IConnectionListener;
import org.openjdk.jmc.ui.WorkbenchToolkit;
import org.openjdk.jmc.ui.misc.CompositeToolkit;

import java.util.stream.Stream;

public class AgentEditor extends EditorPart implements IConnectionListener {
	public static final String EDITOR_ID = "org.openjdk.jmc.console.agent.editor.AgentEditor"; //$NON-NLS-1$

	private Composite parentComposite;
	private FormToolkit formToolkit;
	private StackLayout stackLayout;
	private Form form;

	@Override
	public void onConnectionChange(IConnectionHandle connection) {
		boolean serverDisposed = getAgentEditorInput().getServerHandle().getState() == IServerHandle.State.DISPOSED;
		if (serverDisposed) {
			WorkbenchToolkit.asyncCloseEditor(AgentEditor.this);
			return;
		}

		if (!connection.isConnected() && form != null) {
			form.setMessage(Messages.AgentEditor_CONNECTION_LOST, IMessageProvider.ERROR);
		}
	}

	@Override
	public void doSave(IProgressMonitor iProgressMonitor) {
		// intentionally empty
	}

	@Override
	public void doSaveAs() {
		// intentionally empty
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void setFocus() {
		parentComposite.setFocus();
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);

		try {
			getAgentEditorInput();
		} catch (Exception e) {
			throw new PartInitException(e.getMessage(), e);
		}

		setPartName(getAgentEditorInput().getName());

		getAgentEditorInput().getAgentJmxHelper().addConnectionChangedListener(this);
	}

	protected AgentEditorInput getAgentEditorInput() {
		AgentEditorInput aei;
		IEditorInput input = super.getEditorInput();
		if (input instanceof AgentEditorInput) {
			aei = (AgentEditorInput) input;
		} else {
			aei = input.getAdapter(AgentEditorInput.class);
		}

		if (aei == null) {
			// Not likely to be null, but guard just in case
			throw new RuntimeException("The agent editor cannot handle the provided editor input"); //$NON-NLS-1$
		}

		return (AgentEditorInput) super.getEditorInput();
	}

	@Override
	public void createPartControl(Composite parent) {
		parentComposite = parent;
		stackLayout = new StackLayout();
		parentComposite.setLayout(stackLayout);

		formToolkit = new FormToolkit(FlightRecorderUI.getDefault().getFormColors(Display.getCurrent()));
		formToolkit.setBorderStyle(SWT.NULL);

		stackLayout.topControl = formToolkit.createComposite(parent);
		ProgressIndicator progressIndicator = CompositeToolkit.createWaitIndicator((Composite) stackLayout.topControl,
				formToolkit);
		progressIndicator.beginTask(1);

		createAgentEditorUi(parent);
	}

	private void createAgentEditorUi(Composite parent) {
		form = formToolkit.createForm(parent);
		form.setText(Messages.AgentEditor_AGENT_EDITOR_TITLE);
		form.setImage(getTitleImage());
		formToolkit.decorateFormHeading(form);

		IToolBarManager manager = form.getToolBarManager();

		AgentEditorAction[] actions = new AgentEditorAction[] {
				new AgentEditorAction(AgentEditorAction.AgentEditorActionType.REFRESH), //
				new AgentEditorAction(AgentEditorAction.AgentEditorActionType.LOAD_PRESET), //
				new AgentEditorAction(AgentEditorAction.AgentEditorActionType.SAVE_AS_PRESET), //
		};
		Stream.of(actions).forEach(manager::add);
		Stream.of(actions).forEach((action) -> action.setEnabled(false));
		form.updateToolBar();

		Composite body = form.getBody();
		body.setLayout(new FillLayout());

		AgentEditorUi agentEditorUi = new AgentEditorUi(this, actions);
		agentEditorUi.createContent(form, formToolkit);
		agentEditorUi.refresh(() -> {
			stackLayout.topControl.dispose();
			stackLayout.topControl = form;
			parentComposite.layout();
		});
	}
}
