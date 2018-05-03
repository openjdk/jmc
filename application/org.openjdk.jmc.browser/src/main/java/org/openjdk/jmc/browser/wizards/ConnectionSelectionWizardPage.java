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

import java.util.Observable;
import java.util.Observer;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import org.openjdk.jmc.browser.IJVMBrowserContextIDs;
import org.openjdk.jmc.browser.views.BrowserLabelProvider;
import org.openjdk.jmc.browser.views.Folder;
import org.openjdk.jmc.browser.views.FolderStructure;
import org.openjdk.jmc.browser.views.JVMBrowserView;
import org.openjdk.jmc.rjmx.servermodel.IServer;
import org.openjdk.jmc.ui.misc.TreeStructureContentProvider;
import org.openjdk.jmc.ui.wizards.RelinkableWizardPage;

/**
 * Lets user select a single server JVM to connect to.
 */
public class ConnectionSelectionWizardPage extends RelinkableWizardPage implements Observer {

	final static String PAGE_NAME = "org.openjdk.jmc.browser.server.connect.server.selection"; //$NON-NLS-1$
	private TreeViewer treeViewer;
	private Object selectedNode;
	private final ConnectionWizardModel serverConnectModel;

	public ConnectionSelectionWizardPage(ConnectionWizardModel serverConnectModel) {
		super(PAGE_NAME, Messages.ServerConnectWizardPage_SERVER_SELECT_DESCRIPTION, null);
		this.serverConnectModel = serverConnectModel;
	}

	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		container.setLayout(layout);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		treeViewer = new TreeViewer(container, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		treeViewer.getTree().setLayoutData(gd);

		treeViewer.setAutoExpandLevel(3);
		treeViewer.setContentProvider(new TreeStructureContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				Object[] existing = super.getElements(inputElement);
				Object[] elements = new Object[existing.length + 1];
				elements[0] = new NewConnectionNode();
				System.arraycopy(existing, 0, elements, 1, existing.length);
				return elements;
			};
		});
		treeViewer.setComparator(new ViewerComparator() {
			@Override
			public int category(Object element) {
				if (element instanceof NewConnectionNode) {
					return 0;
				} else if (element instanceof Folder) {
					return ((Folder) element).isModifiable() ? 2 : 1;
				} else {
					return 3;
				}
			}
		});
		serverConnectModel.serverModel.addObserver(this);
		treeViewer.setInput(new FolderStructure(serverConnectModel.serverModel, null));
		treeViewer.setLabelProvider(new BrowserLabelProvider());
		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				select((IStructuredSelection) event.getSelection());
			}
		});
		setControl(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJVMBrowserContextIDs.SELECT_SERVER);
	}

	@Override
	public void performHelp() {
		PlatformUI.getWorkbench().getHelpSystem().displayHelp(IJVMBrowserContextIDs.SELECT_SERVER);
	}

	@Override
	public void update(Observable o, final Object element) {
		Display.getDefault().asyncExec(new Runnable() {
			boolean runAgain = true;

			@Override
			public void run() {
				if (!treeViewer.getTree().isDisposed()) {
					treeViewer.refresh(element);
					if (runAgain) {
						runAgain = false;
						Display.getCurrent().timerExec(JVMBrowserView.getHighlightTime(), this);
					}
				}
			}
		});
	}

	@Override
	public void dispose() {
		serverConnectModel.serverModel.deleteObserver(this);
		super.dispose();
	}

	private void select(IStructuredSelection ss) {
		if (!ss.isEmpty()) {
			selectedNode = ss.getFirstElement();
			if (selectedNode instanceof IServer) {
				serverConnectModel.connectToServer = (IServer) selectedNode;
			}
		}
		setPageComplete(isServerSelected());
	}

	private boolean isServerSelected() {
		return selectedNode instanceof IServer || selectedNode instanceof NewConnectionNode;
	}

	@Override
	public boolean isPageComplete() {
		return isServerSelected();
	}

	@Override
	public IWizardPage getNextPage() {
		if (selectedNode instanceof NewConnectionNode) {
			ConnectionWizardPage cwp = ((ConnectionWizard) getWizard()).getConnectionWizardPage();
			cwp.setPreviousPage(this);
			cwp.setNextPage(super.getNextPage());
			return cwp;
		}
		return super.getNextPage();
	}
}
