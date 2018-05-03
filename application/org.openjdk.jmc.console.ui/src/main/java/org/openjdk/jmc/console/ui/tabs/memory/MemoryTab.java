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
package org.openjdk.jmc.console.ui.tabs.memory;

import java.util.logging.Level;

import javax.inject.Inject;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

import org.openjdk.jmc.console.ui.ConsolePlugin;
import org.openjdk.jmc.console.ui.editor.IConsolePageContainer;
import org.openjdk.jmc.console.ui.editor.IConsolePageStateHandler;
import org.openjdk.jmc.console.ui.editor.internal.ConsoleEditor;
import org.openjdk.jmc.console.ui.editor.internal.ConsoleFormPage;
import org.openjdk.jmc.console.ui.messages.internal.Messages;
import org.openjdk.jmc.rjmx.ConnectionToolkit;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.services.IDiagnosticCommandService;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.misc.DialogToolkit;
import org.openjdk.jmc.ui.misc.MCActionContributionItem;
import org.openjdk.jmc.ui.misc.MCLayoutFactory;

/**
 * Garbage Collection Tab
 */
public class MemoryTab implements IConsolePageStateHandler {

	private static final String HEAP_HISTOGRAM_ID = "heapHistogram"; //$NON-NLS-1$
	private static final String GC_TABLE_ID = "gcTable"; //$NON-NLS-1$
	private static final String MEMORY_POOL_ID = "memoryPool"; //$NON-NLS-1$
	private HeapHistogram histogram;
	private GcTableSectionPart gcTableSectionPart;
	private MemoryPoolTableSectionPart poolTable;

	private static class GCAction extends Action {
		private final IConnectionHandle handle;
		private final Shell messageShell;

		public GCAction(IConnectionHandle connection, Shell shell) {
			super(null, IAction.AS_PUSH_BUTTON);
			setText(Messages.MemoryTab_RUN_GC_ACTION_DESCRIPTION_TEXT);
			setToolTipText(Messages.MemoryTab_RUN_GC_ACTION_DESCRIPTION_TEXT);
			setImageDescriptor(UIPlugin.getDefault().getMCImageDescriptor(UIPlugin.ICON_GARBAGE_BIN));
			setId("gc"); //$NON-NLS-1$
			handle = connection;
			messageShell = shell;
		}

		@Override
		public void run() {
			try {
				ConnectionToolkit.getMemoryBean(handle.getServiceOrThrow(MBeanServerConnection.class)).gc();
			} catch (Exception o) {
				ConsolePlugin.getDefault().getLogger().log(Level.SEVERE, Messages.MemoryTab_TITLE_COULD_NOT_RUN_GC, o);
				DialogToolkit.showException(messageShell, Messages.MemoryTab_TITLE_COULD_NOT_RUN_GC, o);
			}
		}
	};

	@Inject
	protected void createPageContent(IConsolePageContainer page, IManagedForm managedForm, IConnectionHandle handle) {
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();

		Composite container = managedForm.getForm().getBody();
		container.setLayout(MCLayoutFactory.createFormPageLayout());

		form.getToolBarManager().appendToGroup(IConsolePageContainer.TB_FIRST_GROUP, new MCActionContributionItem(
				new GCAction(handle, ((ConsoleFormPage) page).getEditorSite().getShell())));
		form.getToolBarManager().update(true);

		IMemento currentState = page.loadConfig();

		histogram = HeapHistogram.create(container, toolkit, handle, currentState.getChild(HEAP_HISTOGRAM_ID));
		if (histogram != null) {
			managedForm.addPart(histogram);
			MCLayoutFactory.addGrabOnExpandLayoutData(histogram.getSection());
		}

		gcTableSectionPart = new GcTableSectionPart(((ConsoleEditor) page.getEditor()).getSectionPartManagers().get(0),
				container, toolkit, handle, currentState.getChild(GC_TABLE_ID));
		managedForm.addPart(gcTableSectionPart);
		MCLayoutFactory.addGrabOnExpandLayoutData(gcTableSectionPart.getSection());

		poolTable = new MemoryPoolTableSectionPart(container, toolkit, new MemoryPoolModel(handle),
				currentState.getChild(MEMORY_POOL_ID));
		managedForm.addPart(poolTable);
		MCLayoutFactory.addGrabOnExpandLayoutData(poolTable.getSection());
		validateDependencies(page, handle);
	}

	private void validateDependencies(IConsolePageContainer page, IConnectionHandle connectionHandle) {
		MBeanServerConnection connection = connectionHandle.getServiceOrNull(MBeanServerConnection.class);
		try {
			if (connection.queryNames(new ObjectName("java.lang:type=GarbageCollector,*"), null).isEmpty() //$NON-NLS-1$
					|| connection.queryNames(new ObjectName("java.lang:type=MemoryPool,*"), null).isEmpty()) { //$NON-NLS-1$
				page.presentError(Messages.ConsoleEditor_PLATFORM_MBEANS_UNAVAILABLE);
			} else if (!(connectionHandle.getServiceOrNull(IDiagnosticCommandService.class) != null)) {
				page.presentError(Messages.ConsoleEditor_DIAGNOSTIC_COMMANDS_UNAVAILABLE);
			}
		} catch (Exception e) {
			page.presentError(Messages.ConsoleEditor_CONNECTION_LOST);
		}
	}

	@Override
	public boolean saveState(IMemento state) {
		if (histogram != null) {
			histogram.saveState(state.createChild(HEAP_HISTOGRAM_ID));
		}
		gcTableSectionPart.saveState(state.createChild(GC_TABLE_ID));
		poolTable.saveState(state.createChild(MEMORY_POOL_ID));
		return true;
	}

	@Override
	public void dispose() {
	}
}
