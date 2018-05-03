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
package org.openjdk.jmc.flightrecorder.ui.pages;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IItemQuery;
import org.openjdk.jmc.flightrecorder.ui.IPageContainer;
import org.openjdk.jmc.flightrecorder.ui.IPageDefinition;
import org.openjdk.jmc.flightrecorder.ui.IPageUI;
import org.openjdk.jmc.flightrecorder.ui.StreamModel;
import org.openjdk.jmc.flightrecorder.ui.common.AbstractDataPage;
import org.openjdk.jmc.flightrecorder.ui.common.DataPageToolkit;
import org.openjdk.jmc.flightrecorder.ui.common.FilterComponent;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStoreActionToolkit;
import org.openjdk.jmc.ui.column.ColumnManager.SelectionState;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;

class DistinctItemsPage extends AbstractDataPage {

	private class DistinctItemsUi implements IPageUI {

		private final ItemHistogram table;
		private final FilterComponent filter;
		private final StreamModel items;
		private final IItemQuery query;

		DistinctItemsUi(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state, String name,
				Image icon, IItemQuery query, StreamModel items, IBaseLabelProvider labelProvider, String description) {
			Form form = DataPageToolkit.createForm(parent, toolkit, name, icon);
			this.items = items;
			this.query = query;

			table = DataPageToolkit.createDistinctItemsTable(form.getBody(), items.getItems(), query,
					new TableSettings(state.getChild(TABLE)));
			filter = FilterComponent.createFilterComponent(table, tableFilter,
					items.getItems().apply(query.getFilter()), pageContainer.getSelectionStore()::getSelections,
					this::onFilterChange);
			MCContextMenuManager mm = MCContextMenuManager.create(table.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(table.getManager(), mm);
			// FIXME: Either provide a selection name that is not the same as the page name or do not support selections here
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(), table, name, mm);
			mm.add(filter.getShowFilterAction());
			mm.add(filter.getShowSearchAction());
			table.getManager().getViewer()
					.addSelectionChangedListener(e -> pageContainer.showSelection(table.getSelection().getItems()));
			table.show(items.getItems().apply(query.getFilter()));
			filter.loadState(state.getChild(FILTER));

			addResultActions(form);
			if (labelProvider != null) {
				table.getManager().getViewer().setLabelProvider(labelProvider);
			}
			onFilterChange(tableFilter);
			table.getManager().setSelectionState(tableSelection);
		}

		private void onFilterChange(IItemFilter newFilter) {
			filter.filterChangeHelper(newFilter, table, items.getItems().apply(query.getFilter()));
			tableFilter = newFilter;
		}

		@Override
		public void saveTo(IWritableState state) {
			table.getManager().getSettings().saveState(state.createChild(TABLE));
			filter.saveState(state.createChild(FILTER));

			saveToLocal();
		}

		private void saveToLocal() {
			tableSelection = table.getManager().getSelectionState();
		}

	}

	private static final String TABLE = "table"; //$NON-NLS-1$
	private static final String FILTER = "filter"; //$NON-NLS-1$
	private IItemQuery query;
	private IBaseLabelProvider labelProvider;
	private IItemFilter tableFilter;
	private SelectionState tableSelection;

	public DistinctItemsPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
		super(dpd, items, editor);
	}

	void setLabelProvider(IBaseLabelProvider provider) {
		this.labelProvider = provider;
	}

	void setTableDefinition(IItemQuery query) {
		this.query = query;
	}

	@Override
	public IPageUI display(Composite container, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
		return new DistinctItemsUi(container, toolkit, pageContainer, state, getName(), getIcon(), query,
				getDataSource(), labelProvider, getDescription());
	}
}
