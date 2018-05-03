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
package org.openjdk.jmc.console.ui.tabs.threads;

import java.text.DateFormat;
import java.util.Date;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.forms.IManagedForm;

import org.openjdk.jmc.console.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.handlers.CopySelectionAction;
import org.openjdk.jmc.ui.handlers.InFocusHandlerActivator;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.CopySettings;
import org.openjdk.jmc.ui.misc.DisplayToolkit;
import org.openjdk.jmc.ui.misc.FormatToolkit;
import org.openjdk.jmc.ui.misc.MCLayoutFactory;
import org.openjdk.jmc.ui.misc.MCSectionPart;
import org.openjdk.jmc.ui.polling.PollManager.Pollable;
import org.openjdk.jmc.ui.polling.RefreshPollAction;

public class StackTraceSectionPart extends MCSectionPart implements Pollable, ISelectionChangedListener {
	private final DateFormat m_dateFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM);
	private final IThreadsModel m_threadsModel;
	private final RefreshPollAction m_pollAction;
	private final TreeViewer tree;

	public StackTraceSectionPart(Composite parent, IManagedForm form, IWorkbenchPartSite site,
			IThreadsModel threadsModel) {
		super(parent, form.getToolkit(), MCSectionPart.DEFAULT_TITLE_DESCRIPTION_STYLE);
		getSection().setText(Messages.StackTraceSectionPart_SECTION_TEXT);
		m_threadsModel = threadsModel;

		m_pollAction = new RefreshPollAction(Messages.StackTraceSectionPart_ACTION_REFRESH_STACK_TRACE_TEXT,
				m_threadsModel.getPollManager(), this);
		getMCToolBarManager().add(m_pollAction);

		Composite body = createSectionBody(MCLayoutFactory.createMarginFreeFormPageLayout());
		Tree control = form.getToolkit().createTree(body,
				SWT.FULL_SELECTION | SWT.MULTI | SWT.VIRTUAL | SWT.H_SCROLL | SWT.V_SCROLL);
		control.setLayoutData(MCLayoutFactory.createFormPageLayoutData());
		control.setHeaderVisible(false);
		control.setLinesVisible(true);
		tree = new TreeViewer(control);
		ColumnLabelProvider lp = new StackTraceLabelProvider();
		tree.setLabelProvider(lp);
		tree.setAutoExpandLevel(2);
		tree.setContentProvider(new StackTraceContentProvider(m_threadsModel));
		CopySelectionAction copyAction = new CopySelectionAction(tree, FormatToolkit.selectionFormatter(lp));
		InFocusHandlerActivator.install(control, copyAction);
		IContributionItem copyMenu = CopySettings.getInstance().createContributionItem();
		MCContextMenuManager mm = MCContextMenuManager.create(control);
		mm.appendToGroup(MCContextMenuManager.GROUP_EDIT, copyAction);
		mm.appendToGroup(MCContextMenuManager.GROUP_EDIT, copyMenu);
		site.registerContextMenu(mm, tree);
	}

	@Override
	public boolean poll() {
		DisplayToolkit.safeAsyncExec(new Runnable() {
			@Override
			public void run() {
				if (!m_threadsModel.isConnected()) {
					m_threadsModel.getPollManager().stop();
				}
				if (!tree.getControl().isDisposed()) {
					tree.refresh();
					updateTimestamp();
				}
			}
		});
		return true;
	}

	public void updateTimestamp() {
		if (getSection() != null && !getSection().isDisposed()) {
			getSection().setDescription(
					NLS.bind(Messages.StackTraceSectionPart_SECTION_DESCRIPTION_DATE, m_dateFormat.format(new Date())));
		}
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		tree.setInput(event.getSelection());
		updateTimestamp();
	}
}
