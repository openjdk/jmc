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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistable;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openjdk.jmc.console.ui.ConsolePlugin;
import org.openjdk.jmc.console.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.column.ColumnBuilder;
import org.openjdk.jmc.ui.column.ColumnManager;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.ColumnsFilter;
import org.openjdk.jmc.ui.column.IColumn;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.BackgroundFractionDrawer;
import org.openjdk.jmc.ui.misc.DialogToolkit;
import org.openjdk.jmc.ui.misc.DisplayToolkit;
import org.openjdk.jmc.ui.misc.MCLayoutFactory;
import org.openjdk.jmc.ui.misc.MCSectionPart;
import org.openjdk.jmc.ui.misc.MementoToolkit;
import org.openjdk.jmc.ui.misc.TreeStructureContentProvider;
import org.openjdk.jmc.ui.polling.PollManager.Pollable;
import org.openjdk.jmc.ui.polling.RefreshPollAction;

/**
 * Class responsible for showing live thread information
 */
public class ThreadTableSectionPart extends MCSectionPart implements Pollable, IPersistable {

	private final DateFormat m_dateFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM);
	private final IThreadsModel m_threadsModel;
	private boolean m_informedMonitoredDeadlockedThreads;
	private final Button m_buttonCpuTiming;
	private final Button m_buttonDeadlockDetection;
	private final Button m_buttonAllocation;
	final ColumnManager columnManager;

	public ThreadTableSectionPart(Composite parent, FormToolkit toolkit, IThreadsModel threadsModel, IMemento state) {
		super(parent, toolkit, MCSectionPart.DEFAULT_TITLE_DESCRIPTION_STYLE);
		getSection().setText(Messages.ThreadTableSectionPart_SECTION_TEXT);
		m_threadsModel = threadsModel;

		Composite body = createSectionBody(MCLayoutFactory.createMarginFreeFormPageLayout());
		Composite topBar = toolkit.createComposite(body, SWT.NONE);
		topBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		topBar.setLayout(MCLayoutFactory.createMarginFreeFormPageLayout(5));

		Table table = toolkit.createTable(body, SWT.FULL_SELECTION | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		table.setLayoutData(MCLayoutFactory.createFormPageLayoutData());
		TableViewer viewer = new TableViewer(table);
		viewer.setContentProvider(new TreeStructureContentProvider());
		viewer.setInput(threadsModel);

		columnManager = createColumnManager(viewer, state);
		ColumnMenusFactory.addDefaultMenus(columnManager, MCContextMenuManager.create(table));

		ColumnsFilter.addFilterControl(topBar, toolkit, columnManager);
		m_buttonCpuTiming = createTimingButton(topBar, toolkit);
		m_buttonDeadlockDetection = createDeadlockDetection(topBar, toolkit);
		m_buttonAllocation = createAllocation(topBar, toolkit);

		IAction pollAction = new RefreshPollAction(Messages.ThreadTableSectionPart_REFRESH_STACK_TRACE,
				m_threadsModel.getPollManager(), this);
		getMCToolBarManager().add(pollAction);
	}

	void addSelectionListener(ISelectionChangedListener listener) {
		columnManager.getViewer().addSelectionChangedListener(listener);
	}

	private static ColumnManager createColumnManager(TableViewer viewer, IMemento state) {
		IColumn name = new ColumnBuilder(Messages.THREAD_NAME_NAME_TEXT, "threadName", new ColumnLabelProvider() { //$NON-NLS-1$
			@Override
			public String getText(Object element) {
				return ((ThreadInfoCompositeSupport) element).getThreadName();
			}

			@Override
			public Image getImage(Object element) {
				return StackTraceLabelProvider.getThreadImage((ThreadInfoCompositeSupport) element);
			}
		}).description(Messages.THREAD_NAME_DESCRIPTION_TEXT).build();

		IColumn cpuUsage = new ColumnBuilder(Messages.CPU_USAGE_NAME_TEXT, "cpuUsage", //$NON-NLS-1$
				ThreadInfoCompositeSupport.GET_CPU_USAGE).description(Messages.CPU_USAGE_DESCRIPTION_TEXT)
						.style(SWT.RIGHT)
						.columnDrawer(
								BackgroundFractionDrawer.unchecked(ThreadInfoCompositeSupport::getPartOfTimeRunning))
						.build();
		IColumn isDeadlocked = new ColumnBuilder(Messages.IS_DEADLOCKED_NAME_TEXT, "isDeadlocked", //$NON-NLS-1$
				ThreadInfoCompositeSupport.IS_DEADLOCKED).description(Messages.IS_DEADLOCKED_DESCRIPTION_TEXT).build();
		IColumn alloc = new ColumnBuilder(Messages.ALLOCATED_MEMORY_NAME_TEXT, "allocatedMemory", //$NON-NLS-1$
				ThreadInfoCompositeSupport.GET_ALLOCATED_BYTES).description(Messages.ALLOCATED_MEMORY_DESCRIPTION_TEXT)
						.style(SWT.RIGHT).build();
		IColumn blockedCount = new ColumnBuilder(Messages.BLOCKED_COUNT_NAME_TEXT, "blockedCount", //$NON-NLS-1$
				ThreadInfoCompositeSupport.GET_BLOCKED_COUNT).description(Messages.BLOCKED_COUNT_DESCRIPTION_TEXT)
						.style(SWT.RIGHT).build();
		IColumn blockedTime = new ColumnBuilder(Messages.BLOCKED_TIME_NAME_TEXT, "blockedTime", //$NON-NLS-1$
				ThreadInfoCompositeSupport.GET_BLOCKED_TIME).description(Messages.BLOCKED_TIME_DESCRIPTION_TEXT)
						.style(SWT.RIGHT).build();
		IColumn lockName = new ColumnBuilder(Messages.LOCK_NAME_NAME_TEXT, "lockName", //$NON-NLS-1$
				ThreadInfoCompositeSupport.GET_LOCK_NAME).description(Messages.LOCK_NAME_DESCRIPTION_TEXT).build();
		IColumn lockOwnerId = new ColumnBuilder(Messages.LOCK_OWNER_ID_NAME_TEXT, "lockOwnerId", //$NON-NLS-1$
				ThreadInfoCompositeSupport.GET_LOCK_OWNER_ID).description(Messages.LOCK_OWNER_ID_DESCRIPTION_TEXT)
						.build();
		IColumn lockOwnerName = new ColumnBuilder(Messages.LOCK_OWNER_NAME_NAME_TEXT, "lockOwnerName", //$NON-NLS-1$
				ThreadInfoCompositeSupport.GET_LOCK_OWNER_NAME).description(Messages.LOCK_OWNER_NAME_DESCRIPTION_TEXT)
						.build();
		IColumn threadId = new ColumnBuilder(Messages.THREAD_ID_NAME_TEXT, "threadId", //$NON-NLS-1$
				ThreadInfoCompositeSupport.GET_THREAD_ID).description(Messages.THREAD_ID_DESCRIPTION_TEXT).build();
		IColumn threadState = new ColumnBuilder(Messages.THREAD_STATE_NAME_TEXT, "threadState", //$NON-NLS-1$
				ThreadInfoCompositeSupport.GET_THREAD_STATE).description(Messages.THREAD_STATE_DESCRIPTION_TEXT)
						.build();
		IColumn waitCount = new ColumnBuilder(Messages.WAITED_COUNT_NAME_TEXT, "waitCount", //$NON-NLS-1$
				ThreadInfoCompositeSupport.GET_WAITED_COUNT).description(Messages.WAITED_COUNT_DESCRIPTION_TEXT)
						.style(SWT.RIGHT).build();
		IColumn waitTime = new ColumnBuilder(Messages.WAITED_TIME_NAME_TEXT, "waitTime", //$NON-NLS-1$
				ThreadInfoCompositeSupport.GET_WAITED_TIME).description(Messages.WAITED_TIME_DESCRIPTION_TEXT)
						.style(SWT.RIGHT).build();
		IColumn isInNative = new ColumnBuilder(Messages.IS_NATIVE_NAME_TEXT, "isInNative", //$NON-NLS-1$
				ThreadInfoCompositeSupport.IS_IN_NATIVE).description(Messages.IS_NATIVE_DESCRIPTION_TEXT).build();
		IColumn isSuspended = new ColumnBuilder(Messages.IS_SUSPENDED_NAME_TEXT, "isSuspended", //$NON-NLS-1$
				ThreadInfoCompositeSupport.IS_SUSPENDED).description(Messages.IS_SUSPENDED_DESCRIPTION_TEXT).build();

		List<IColumn> columns = Arrays.asList(name, blockedCount, blockedTime, lockName, lockOwnerId, lockOwnerName,
				threadId, threadState, waitCount, waitTime, isInNative, isSuspended, isDeadlocked, cpuUsage, alloc);
		return ColumnManager.build(viewer, columns, TableSettings.forState(MementoToolkit.asState(state)));

	}

	private Button createTimingButton(Composite parent, FormToolkit toolkit) {
		final Button button = toolkit.createButton(parent,
				Messages.ThreadTableSectionPart_ENABLE_THREAD_CPU_PROFILING_BUTTON_TEXT, SWT.CHECK);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				m_threadsModel.setCPUTimeEnabled(button.getSelection());

			}
		});
		return button;
	}

	private Button createDeadlockDetection(Composite parent, FormToolkit toolkit) {
		final Button button = toolkit.createButton(parent,
				Messages.ThreadTableSectionPart_ENABLE_DEADLOCK_DETECTION_BUTTON_TEXT, SWT.CHECK);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				m_threadsModel.setDeadlockDetectionEnabled(button.getSelection());
			}
		});
		return button;
	}

	private Button createAllocation(Composite parent, FormToolkit toolkit) {
		final Button button = toolkit.createButton(parent,
				Messages.ThreadTableSectionPart_ENABLE_THREAD_ALLOCATION_BUTTON_TEXT, SWT.CHECK);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				m_threadsModel.setAllocationEnabled(button.getSelection());
			}
		});
		return button;
	}

	@Override
	public boolean poll() {
		try {
			m_threadsModel.update();
			DisplayToolkit.safeAsyncExec(new Runnable() {
				@Override
				public void run() {
					if (!m_threadsModel.isConnected()) {
						m_threadsModel.getPollManager().stop();
					}
					Section section = getSection();
					if (!section.isDisposed()) {
						columnManager.getViewer().refresh();
						section.setDescription(NLS.bind(Messages.ThreadTableSectionPart_SECTION_DESCRIPTION_DATE,
								m_dateFormat.format(new Date())));
					}
					if (!m_informedMonitoredDeadlockedThreads && m_threadsModel.isUsingMonitoredThreadlockedThreads()) {
						m_informedMonitoredDeadlockedThreads = true;
						DialogToolkit.showWarningDialogAsync(Display.getCurrent(),
								Messages.ThreadTableSectionPart_USING_FIND_MONITORED_DEADLOCKED_THREADS_HEADER,
								Messages.ThreadTableSectionPart_USING_FIND_MONITORED_DEADLOCKED_THREADS_TEXT);
					}
				}
			});
		} catch (ThreadModelException e) {
			// TODO: JMC-5902 - Improve error handling. Separate expected (connection lost) from unexpected errors
			ConsolePlugin.getDefault().getLogger().log(Level.WARNING, "Could not update the threads table!", e); //$NON-NLS-1$
			return false;
		} finally {
			updateButtonStatedFromModel();
		}
		return true;
	}

	private void updateButtonStatedFromModel() {
		DisplayToolkit.safeAsyncExec(new Runnable() {
			@Override
			public void run() {
				if (!m_buttonCpuTiming.isDisposed()) {
					m_buttonCpuTiming.setSelection(m_threadsModel.isCPUTimeEnabled());
					m_buttonAllocation.setSelection(m_threadsModel.isAllocationEnabled());
					m_buttonDeadlockDetection.setSelection(m_threadsModel.isDeadlockeDetectionEnabled());
				}
			}
		});
	}

	@Override
	public void saveState(IMemento memento) {
		columnManager.getSettings().saveState(MementoToolkit.asWritableState(memento));
	}
}
