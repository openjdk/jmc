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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.TypeHandling;
import org.openjdk.jmc.console.ui.ConsolePlugin;
import org.openjdk.jmc.console.ui.messages.internal.Messages;
import org.openjdk.jmc.console.ui.preferences.ConsoleConstants;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.services.IDiagnosticCommandService;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.column.ColumnBuilder;
import org.openjdk.jmc.ui.column.ColumnManager;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.IColumn;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.handlers.ActionToolkit;
import org.openjdk.jmc.ui.handlers.InFocusHandlerActivator;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.DisplayToolkit;
import org.openjdk.jmc.ui.misc.MCLayoutFactory;
import org.openjdk.jmc.ui.misc.MCSectionPart;
import org.openjdk.jmc.ui.misc.MementoToolkit;

public class HeapHistogram extends MCSectionPart {
	private static final Pattern LINE_PATTERN = Pattern.compile("(\\d+)\\s+(\\d+)\\s+(.+)"); //$NON-NLS-1$
	private static final String DIAGNOSTIC_COMMAND = "GC.class_histogram -all=false"; //$NON-NLS-1$
	private final IDiagnosticCommandService service;
	private final Map<String, HistogramItem> model = new HashMap<>();
	private final ColumnManager columnManager;

	private static class HistogramItem {
		String className;
		int count;
		long memory;
		long memoryReference;

		static final IMemberAccessor<String, HistogramItem> GET_CLASS = new IMemberAccessor<String, HistogramItem>() {

			@Override
			public String getMember(HistogramItem i) {
				return TypeHandling.simplifyType(i.className);
			}

		};

		static final IMemberAccessor<Integer, HistogramItem> GET_COUNT = new IMemberAccessor<Integer, HistogramItem>() {

			@Override
			public Integer getMember(HistogramItem i) {
				return i.count;
			}

		};

		static final IMemberAccessor<IQuantity, HistogramItem> GET_SIZE = new IMemberAccessor<IQuantity, HistogramItem>() {

			@Override
			public IQuantity getMember(HistogramItem i) {
				return UnitLookup.BYTE.quantity(i.memory);
			}

		};

		static final IMemberAccessor<IQuantity, HistogramItem> GET_DELTA = new IMemberAccessor<IQuantity, HistogramItem>() {

			@Override
			public IQuantity getMember(HistogramItem i) {
				return UnitLookup.BYTE.quantity(i.memory - i.memoryReference);
			}

		};

	}

	private final Action resetAction = new Action(Messages.HeapHistogram_RESET_DELTA_ACTION_TEXT) {
		@Override
		public void run() {
			for (HistogramItem item : model.values()) {
				item.memoryReference = item.memory;
			}
			columnManager.getViewer().refresh();
		};
	};

	private void refreshHistogram() {
		boolean showWarning = ConsolePlugin.getDefault().getPreferenceStore()
				.getBoolean(ConsoleConstants.PROPERTY_HEAPHISTOGRAM_UPDATE_WARNING);
		boolean doUpdate = false;
		if (showWarning) {
			MessageDialogWithToggle dialog = MessageDialogWithToggle.openOkCancelConfirm(getSection().getShell(),
					Messages.HeapHistogram_WARNING_DIALOG_TITLE,
					Messages.HeapHistogram_JVM_PERFORMANCE_WILL_BE_AFFECTED,
					Messages.HeapHistogram_SHOW_WARNING_BEFORE_UPDATING, true, null, null);
			if (dialog.getReturnCode() == IDialogConstants.OK_ID) {
				doUpdate = true;

				if (!dialog.getToggleState()) {
					ConsolePlugin.getDefault().getPreferenceStore()
							.setValue(ConsoleConstants.PROPERTY_HEAPHISTOGRAM_UPDATE_WARNING, false);
				}
			}
		} else {
			doUpdate = true;
		}

		if (doUpdate) {
			new Job(Messages.HeapHistogram_REFRESHING_HEAP_HISTOGRAM) {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						final Object result = service.runCtrlBreakHandlerWithResult(DIAGNOSTIC_COMMAND);
						if (!monitor.isCanceled()) {
							DisplayToolkit.safeAsyncExec(new Runnable() {

								@Override
								public void run() {
									if (!columnManager.getViewer().getControl().isDisposed()) {
										boolean firstUpdate = model.size() == 0;
										parseHistogram(String.valueOf(result));
										if (firstUpdate) {
											for (HistogramItem item : model.values()) {
												item.memoryReference = item.memory;
											}
										}
										columnManager.getViewer().refresh();
									}
								}
							});
						}
						return Status.OK_STATUS;
					} catch (Exception e) {
						return new Status(IStatus.ERROR, ConsolePlugin.PLUGIN_ID,
								Messages.HeapHistogram_FAILED_TO_REFRESH, e);
					}

				}
			}.schedule();
		}
	};

	private HeapHistogram(Composite parent, FormToolkit toolkit, IDiagnosticCommandService service, IMemento state) {
		super(parent, toolkit, Messages.HeapHistogram_TITLE);
		this.service = service;
		resetAction.setDescription(Messages.HeapHistogram_RESET_DELTA_ACTION_DESCRIPTION);
		resetAction.setToolTipText(Messages.HeapHistogram_RESET_DELTA_ACTION_TOOLTOP);
		// FIXME: Create a new icon. We should not use the same icon as 'reset to defaults' and 'undo'.
		resetAction.setImageDescriptor(UIPlugin.getDefault().getMCImageDescriptor(UIPlugin.ICON_RESET_TO_DEFAULTS));
		IAction refreshAction = ActionToolkit.commandAction(this::refreshHistogram,
				IWorkbenchCommandConstants.FILE_REFRESH);
		refreshAction.setToolTipText(Messages.HeapHistogram_REFRESH_ACTION_TOOLTIP);
		refreshAction.setDescription(Messages.HeapHistogram_REFRESH_ACTION_TOOLTIP);

		Composite body = createSectionBody(MCLayoutFactory.createMarginFreeFormPageLayout());
		Table table = toolkit.createTable(body,
				SWT.FULL_SELECTION | SWT.MULTI | SWT.VIRTUAL | SWT.H_SCROLL | SWT.V_SCROLL);
		table.setLayoutData(MCLayoutFactory.createFormPageLayoutData());
		TableViewer viewer = new TableViewer(table);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setInput(model.values());
		ColumnViewerToolTipSupport.enableFor(viewer);

		IColumn classColumn = new ColumnBuilder(Messages.HeapHistogram_CLASS_COLUMN_TEXT, "className", //$NON-NLS-1$
				HistogramItem.GET_CLASS).build();
		IColumn countColumn = new ColumnBuilder(Messages.HeapHistogram_INSTANCES_COLUMN_TEXT, "count", //$NON-NLS-1$
				HistogramItem.GET_COUNT).style(SWT.RIGHT).build();
		IColumn bytesColumn = new ColumnBuilder(Messages.HeapHistogram_SIZE_COLUMN_TEXT, "size", HistogramItem.GET_SIZE) //$NON-NLS-1$
				.style(SWT.RIGHT).build();
		IColumn deltaColumn = new ColumnBuilder(Messages.HeapHistogram_DELTA_COLUMN_TEXT, "delta", //$NON-NLS-1$
				HistogramItem.GET_DELTA).style(SWT.RIGHT).build();
		List<IColumn> columns = Arrays.asList(classColumn, countColumn, bytesColumn, deltaColumn);
		columnManager = ColumnManager.build(viewer, columns, TableSettings.forState(MementoToolkit.asState(state)));
		MCContextMenuManager mm = MCContextMenuManager.create(table);
		ColumnMenusFactory.addDefaultMenus(columnManager, mm);
		mm.appendToGroup(MCContextMenuManager.GROUP_ADDITIONS, refreshAction);
		mm.appendToGroup(MCContextMenuManager.GROUP_ADDITIONS, resetAction);
		InFocusHandlerActivator.install(table, refreshAction);
		getMCToolBarManager().add(resetAction);
		getMCToolBarManager().add(refreshAction);
		getMCToolBarManager().update();
	}

	void saveState(IMemento memento) {
		columnManager.getSettings().saveState(MementoToolkit.asWritableState(memento));
	}

	private void parseHistogram(String histogram) {
		model.values().forEach(item -> item.count = 0);
		Matcher m = LINE_PATTERN.matcher(histogram);
		while (m.find()) {
			int count = Integer.parseInt(m.group(1));
			long memory = Long.parseLong(m.group(2));
			String className = m.group(3).trim();

			HistogramItem item = model.get(className);
			if (item == null) {
				item = new HistogramItem();
				item.className = className;
				model.put(className, item);
			}
			item.count = count;
			item.memory = memory;
		}
		Iterator<HistogramItem> iterator = model.values().iterator();
		while (iterator.hasNext()) {
			if (iterator.next().count == 0) {
				iterator.remove();
			}
		}
	}

	public static HeapHistogram create(
		Composite parent, FormToolkit toolkit, IConnectionHandle connection, IMemento state) {
		IDiagnosticCommandService service = connection.getServiceOrNull(IDiagnosticCommandService.class);
		return service == null ? null : new HeapHistogram(parent, toolkit, service, state);

	}

}
