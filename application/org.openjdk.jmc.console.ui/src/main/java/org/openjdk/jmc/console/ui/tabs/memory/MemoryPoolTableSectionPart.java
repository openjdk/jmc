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
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistable;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.console.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.column.ColumnBuilder;
import org.openjdk.jmc.ui.column.ColumnManager;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.IColumn;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.BackgroundFractionDrawer;
import org.openjdk.jmc.ui.misc.DisplayToolkit;
import org.openjdk.jmc.ui.misc.MCLayoutFactory;
import org.openjdk.jmc.ui.misc.MCSectionPart;
import org.openjdk.jmc.ui.misc.MementoToolkit;
import org.openjdk.jmc.ui.misc.TreeStructureContentProvider;

/**
 * Class responsible for showing memory pool usage.
 */
public class MemoryPoolTableSectionPart extends MCSectionPart implements IPersistable {

	private final ColumnManager columnManager;

	public MemoryPoolTableSectionPart(Composite parent, FormToolkit toolkit, MemoryPoolModel poolModel,
			IMemento state) {
		super(parent, toolkit, Messages.PoolTableSectionPart_SECTION_TEXT);

		Composite body = createSectionBody(MCLayoutFactory.createMarginFreeFormPageLayout());
		Table table = toolkit.createTable(body, SWT.FULL_SELECTION | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		table.setLayoutData(MCLayoutFactory.createFormPageLayoutData());
		TableViewer viewer = new TableViewer(table);
		viewer.setContentProvider(TreeStructureContentProvider.INSTANCE);
		viewer.setInput(poolModel.getAllPools());
		ColumnViewerToolTipSupport.enableFor(viewer);

		IColumn nameColumn = new ColumnBuilder(Messages.POOL_NAME_NAME_TEXT, "name", MemoryPoolInformation::getPoolName) //$NON-NLS-1$
				.build();
		IColumn typeColumn = new ColumnBuilder(Messages.POOL_TYPE_NAME_TEXT, "type", MemoryPoolInformation::getPoolType) //$NON-NLS-1$
				.build();
		IColumn usedColumn = new ColumnBuilder(Messages.POOL_CUR_USED_NAME_TEXT, "currentUsed", //$NON-NLS-1$
				MemoryPoolInformation::getCurUsed).style(SWT.RIGHT).build();
		IColumn maxColumn = new ColumnBuilder(Messages.POOL_CUR_MAX_NAME_TEXT, "currentMax", //$NON-NLS-1$
				MemoryPoolInformation::getCurMax).style(SWT.RIGHT).build();
		IColumn usageColumn = new ColumnBuilder(Messages.POOL_CUR_USAGE_NAME_TEXT, "currentUsage", //$NON-NLS-1$
				MemoryPoolInformation::getCurUsage).style(SWT.RIGHT)
						.columnDrawer(BackgroundFractionDrawer.unchecked(MemoryPoolInformation::getCurUsage)).build();
		IColumn peakColumn = new ColumnBuilder(Messages.POOL_PEAK_USED_NAME_TEXT, "peakUsed", //$NON-NLS-1$
				MemoryPoolInformation::getPeakUsed).style(SWT.RIGHT).build();
		IColumn peakMaxColumn = new ColumnBuilder(Messages.POOL_PEAK_MAX_NAME_TEXT, "peakMax", //$NON-NLS-1$
				MemoryPoolInformation::getPeakMax).style(SWT.RIGHT).build();
		List<IColumn> columns = Arrays.asList(nameColumn, typeColumn, usedColumn, maxColumn, usageColumn, peakColumn,
				peakMaxColumn);
		columnManager = ColumnManager.build(viewer, columns, TableSettings.forState(MementoToolkit.asState(state)));
		ColumnMenusFactory.addDefaultMenus(columnManager, MCContextMenuManager.create(table));
		poolModel.addObserver(new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				DisplayToolkit.safeAsyncExec(new Runnable() {
					@Override
					public void run() {
						if (!columnManager.getViewer().getControl().isDisposed()) {
							columnManager.getViewer().refresh();
						}
					}
				});
			}
		});
	}

	@Override
	public void saveState(IMemento memento) {
		columnManager.getSettings().saveState(MementoToolkit.asWritableState(memento));
	}

}
