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
package org.openjdk.jmc.browser.wizards;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.openjdk.jmc.browser.IJVMBrowserContextIDs;
import org.openjdk.jmc.browser.views.BrowserLabelProvider;
import org.openjdk.jmc.ui.common.action.IUserAction;
import org.openjdk.jmc.ui.common.util.AdapterUtil;
import org.openjdk.jmc.ui.misc.DialogToolkit;
import org.openjdk.jmc.ui.wizards.RelinkableWizardPage;

/**
 * Lets user select an action to connect to the server.
 */
public class ActionWizardPage extends RelinkableWizardPage {

	final static String PAGE_NAME = "org.openjdk.jmc.browser.server.connect.action.selection"; //$NON-NLS-1$

	private final ConnectionWizardModel m_serverConnectModel;
	private TableViewer m_serverComponentViewer;
	private Object m_currentSelected;

	protected ActionWizardPage(ConnectionWizardModel serverConnectModel) {
		// FIXME: Write a better description
		super(PAGE_NAME, NLS.bind(Messages.ServerConnectWizardPage_TOOL_SELECT_DESCRIPTION, ""), null); //$NON-NLS-1$
		m_serverConnectModel = serverConnectModel;
	}

	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);

		createViewer(container);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		m_serverComponentViewer.getControl().setLayoutData(gd);

		setControl(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJVMBrowserContextIDs.SELECT_ACTION);
	}

	@Override
	public void performHelp() {
		PlatformUI.getWorkbench().getHelpSystem().displayHelp(IJVMBrowserContextIDs.SELECT_ACTION);
	}

	private void createViewer(Composite container) {
		m_serverComponentViewer = new TableViewer(container);
		m_serverComponentViewer.setContentProvider(new UserActionContentProvider());
		m_serverComponentViewer.setLabelProvider(new BrowserLabelProvider());
		m_serverComponentViewer.setInput(null);
		m_serverComponentViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				handleSelection(((IStructuredSelection) event.getSelection()).getFirstElement());
			}
		});
	}

	protected void showException(Exception e) {
		DialogToolkit.showException(getShell(), Messages.ServerConnectWizardPage_SERVER_COMPONENT_ERROR, e);
	}

	private void handleSelection(Object selected) {
		if (selected == null || !selected.equals(m_currentSelected)) {
			setNextPage(null);
			m_currentSelected = selected;
			if (selected instanceof IUserAction) {
				IUserAction action = (IUserAction) selected;
				handleSelectedAction(action);
			}
			setPageComplete(selected != null);
		}
	}

	private void handleSelectedAction(IUserAction action) {
		m_serverConnectModel.action = action;
		if (!AdapterUtil.hasAdapter(action, IWizard.class)) {
			setNextPage(null);
		}
	}

	@Override
	public IWizardPage getNextPage() {
		try {
			IWizard w = AdapterUtil.getAdapter(m_serverConnectModel.action, IWizard.class);
			if (w != null) {
				w.addPages();
				return w.getStartingPage();
			}
		} catch (Exception e) {
			m_currentSelected = null;
			showException(e);
			m_serverComponentViewer.refresh();
		}
		return null;
	}

	@Override
	protected boolean hasNextPage() {
		return AdapterUtil.hasAdapter(m_serverConnectModel.action, IWizard.class);
	}

	@Override
	public boolean isPageComplete() {
		return m_serverConnectModel.connectToServer != null && m_currentSelected != null && !hasNextPage();
	}

	@Override
	public boolean canFlipToNextPage() {
		return m_currentSelected != null && hasNextPage();
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			Object input = createInput();
			if (m_serverComponentViewer.getInput() == null || !m_serverComponentViewer.getInput().equals(input)) {
				m_serverComponentViewer.setInput(input);
			}
			String serverName = m_serverConnectModel.connectToServer != null
					? m_serverConnectModel.connectToServer.getServerHandle().getServerDescriptor().getDisplayName()
					: ""; //$NON-NLS-1$
			setTitle(NLS.bind(Messages.ServerConnectWizardPage_TOOL_SELECT_DESCRIPTION, serverName));
		}
	}

	private Object createInput() {
		if (m_serverConnectModel.connectToServer != null) {
			return m_serverConnectModel.connectToServer.getActionProvider();
		}
		return null;
	}
}
