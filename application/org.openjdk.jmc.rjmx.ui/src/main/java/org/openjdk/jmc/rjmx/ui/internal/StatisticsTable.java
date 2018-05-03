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
package org.openjdk.jmc.rjmx.ui.internal;

import java.util.Arrays;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.actions.SelectionProviderAction;

import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.ui.column.ColumnBuilder;
import org.openjdk.jmc.ui.column.ColumnManager;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.IColumn;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.handlers.InFocusHandlerActivator;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.DelegatingLabelProvider;
import org.openjdk.jmc.ui.misc.MCArrayContentProvider;
import org.openjdk.jmc.ui.misc.MementoToolkit;

public class StatisticsTable {

	private final TableViewer viewer;
	private final ColumnManager columnManager;

	public StatisticsTable(Composite parent, AttributeLabelProvider attributeLp, final IAttributeSet attributes,
			final boolean addResetAction, IMemento state) {
		@SuppressWarnings({"unchecked", "rawtypes"})
		IMemberAccessor<?, Object> attributeAccessor = (IMemberAccessor) StatisticsCalculator.GET_ATTRIBUTE;
		IColumn name = new ColumnBuilder(Messages.StatisticsTable_ATTRIBUTE_NAME, "attribute", //$NON-NLS-1$
				new DelegatingLabelProvider(attributeLp, attributeAccessor))
						.description(Messages.StatisticsTable_ATTRIBUTE_DESCRIPTION).build();
		IColumn last = new ColumnBuilder(Messages.StatisticsTable_VALUE_NAME, "value", StatisticsCalculator.GET_LAST) //$NON-NLS-1$
				.description(Messages.StatisticsTable_VALUE_DESCRIPTION).style(SWT.RIGHT).build();
		IColumn min = new ColumnBuilder(Messages.StatisticsTable_MINIMUM_NAME, "min", StatisticsCalculator.GET_MIN) //$NON-NLS-1$
				.description(Messages.StatisticsTable_MINIMUM_DESCRIPTION).style(SWT.RIGHT).build();
		IColumn max = new ColumnBuilder(Messages.StatisticsTable_MAXIMUM_NAME, "max", StatisticsCalculator.GET_MAX) //$NON-NLS-1$
				.description(Messages.StatisticsTable_MAXIMUM_DESCRIPTION).style(SWT.RIGHT).build();
		IColumn av = new ColumnBuilder(Messages.StatisticsTable_AVERAGE_NAME, "average", //$NON-NLS-1$
				StatisticsCalculator.GET_AVERAGE).description(Messages.StatisticsTable_AVERAGE_DESCRIPTION)
						.style(SWT.RIGHT).build();
		IColumn sigma = new ColumnBuilder("\u03C3", "sigma", StatisticsCalculator.GET_SIGMA) //$NON-NLS-1$ //$NON-NLS-2$
				.description(Messages.StatisticsTable_SIGMA_DESCRIPTION).style(SWT.RIGHT).build();

		viewer = new TableViewer(parent,
				SWT.MULTI | SWT.VIRTUAL | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		ColumnViewerToolTipSupport.enableFor(viewer);
		viewer.setContentProvider(MCArrayContentProvider.INSTANCE);
		columnManager = ColumnManager.build(viewer, Arrays.asList(name, last, min, max, av, sigma),
				TableSettings.forState(MementoToolkit.asState(state)));

		RemoveAttributeAction removeAction = new RemoveAttributeAction(viewer, attributes);
		MCContextMenuManager mm = MCContextMenuManager.create(viewer.getControl());
		ColumnMenusFactory.addDefaultMenus(columnManager, mm);
		mm.appendToGroup(MCContextMenuManager.GROUP_EDIT, removeAction);
		if (addResetAction) {
			Action clearStatisticsAction = new SelectionProviderAction(viewer,
					Messages.AttributeDialSectionPart_CLEAR_STATISTICS_MENU_TEXT) {
				@Override
				public void run() {
					for (Object o : getStructuredSelection().toList()) {
						((StatisticsCalculator) o).reset();
					}
					viewer.refresh();
				};

				@Override
				public void selectionChanged(IStructuredSelection selection) {
					setEnabled(!selection.isEmpty());
				}
			};
			clearStatisticsAction.setEnabled(false);
			mm.appendToGroup(MCContextMenuManager.GROUP_ADDITIONS, clearStatisticsAction);
		}
		InFocusHandlerActivator.install(viewer.getControl(), removeAction);
	}

	public void saveState(IMemento state) {
		columnManager.getSettings().saveState(MementoToolkit.asWritableState(state));
	}

	public TableViewer getViewer() {
		return viewer;
	}

}
