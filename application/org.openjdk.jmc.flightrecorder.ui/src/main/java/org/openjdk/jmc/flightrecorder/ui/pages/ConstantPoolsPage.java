/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, 2025, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.ui.pages;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.item.ItemToolkit;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.IDataPageFactory;
import org.openjdk.jmc.flightrecorder.ui.IDisplayablePage;
import org.openjdk.jmc.flightrecorder.ui.IPageContainer;
import org.openjdk.jmc.flightrecorder.ui.IPageDefinition;
import org.openjdk.jmc.flightrecorder.ui.IPageUI;
import org.openjdk.jmc.flightrecorder.ui.StreamModel;
import org.openjdk.jmc.flightrecorder.ui.common.AbstractDataPage;
import org.openjdk.jmc.flightrecorder.ui.common.DataPageToolkit;
import org.openjdk.jmc.flightrecorder.ui.common.FilterComponent;
import org.openjdk.jmc.flightrecorder.ui.common.ImageConstants;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogramWithInput;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram.ItemHistogramBuilder;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.PersistableSashForm;

public class ConstantPoolsPage extends AbstractDataPage {
	public static class ConstantPoolsPageFactory implements IDataPageFactory {

		@Override
		public String getName(IState state) {
			return Messages.ConstantPoolsPage_PAGE_NAME;
		}

		@Override
		public ImageDescriptor getImageDescriptor(IState state) {
			return FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.PAGE_CONSTANT_POOL);
		}

		@Override
		public String[] getTopics(IState state) {
			return new String[] {JfrRuleTopics.CONSTANT_POOLS};
		}

		@Override
		public IDisplayablePage createPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
			return new ConstantPoolsPage(dpd, items, editor);
		}
	}

	private static final String TABLE = "table"; //$NON-NLS-1$
	private static final String CONSTANT_TABLE = "constantTable"; //$NON-NLS-1$
	private static final String TYPE_FILTER = "typeFilter"; //$NON-NLS-1$
	private static final String PERCENT_POOLS = "percentPools"; //$NON-NLS-1$
	private static final String VALUE_FILTER = "valueFilter"; //$NON-NLS-1$

	private static final ItemHistogramBuilder BY_TYPE_CONSTANT_POOLS_HISTOGRAM = new ItemHistogramBuilder();
	private static final ItemHistogramBuilder CONSTANT_HISTOGRAM = new ItemHistogramBuilder();
	static {
		BY_TYPE_CONSTANT_POOLS_HISTOGRAM.addColumn(JdkAttributes.CONSTANT_POOLS_COUNT);
		BY_TYPE_CONSTANT_POOLS_HISTOGRAM.addColumn(JdkAttributes.CONSTANT_POOLS_SIZE);
		BY_TYPE_CONSTANT_POOLS_HISTOGRAM.addPercentageColumn(PERCENT_POOLS,
				Aggregators.sum(JdkAttributes.CONSTANT_POOLS_SIZE), Messages.ConstantPoolsPage_SIZE_TOTAL_PERCENTAGE,
				Messages.ConstantPoolsPage_SIZE_TOTAL_PERCENTAGE_DESC);
		CONSTANT_HISTOGRAM.addCountColumn();
	}

	private class ConstantPoolsPageUi implements IPageUI {
		private static final String SASH_ELEMENT = "sash"; //$NON-NLS-1$

		private final IItemCollection constPoolItems;
		private final IPageContainer container;
		private final SashForm sash;
		private final ItemHistogram byTypeTable;
		private final ItemHistogram constantValueTable;
		private FilterComponent byTypeFilter;
		private FilterComponent constantValueFilter;
		private IItemCollection selectionConstPoolItems;
		private IItemCollection selectionConstantItems;
		private ItemHistogramWithInput byTypeHistogram;
		private ItemHistogramWithInput constantHistogram;

		ConstantPoolsPageUi(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
			container = pageContainer;
			constPoolItems = getDataSource().getConstantPools();
			selectionConstPoolItems = constPoolItems;
			selectionConstantItems = getDataSource().getConstants();
			Form form = DataPageToolkit.createForm(parent, toolkit, getName(), getIcon());
			sash = new SashForm(form.getBody(), SWT.VERTICAL);
			toolkit.adapt(sash);
			byTypeTable = BY_TYPE_CONSTANT_POOLS_HISTOGRAM.buildWithoutBorder(sash, JdkAttributes.CONSTANT_POOLS_NAME,
					new TableSettings(state.getChild(TABLE)));
			MCContextMenuManager mm = MCContextMenuManager.create(byTypeTable.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(byTypeTable.getManager(), mm);
			byTypeFilter = FilterComponent.createFilterComponent(byTypeTable, null, constPoolItems,
					pageContainer.getSelectionStore()::getSelections, this::onTypeFilterChange);
			//mm.add(byTypeFilter.getShowFilterAction());
			mm.add(byTypeFilter.getShowSearchAction());

			constantValueTable = CONSTANT_HISTOGRAM.buildWithoutBorder(sash, JdkAttributes.CONSTANT_VALUE,
					new TableSettings(state.getChild(CONSTANT_TABLE)));
			mm = MCContextMenuManager.create(constantValueTable.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(constantValueTable.getManager(), mm);
			constantValueFilter = FilterComponent.createFilterComponent(constantValueTable, null,
					getDataSource().getConstants(), pageContainer.getSelectionStore()::getSelections,
					this::onValueFilterChange);
			//mm.add(constantValueFilter.getShowFilterAction());
			mm.add(constantValueFilter.getShowSearchAction());

			// chains the 2 tables
			byTypeHistogram = new ItemHistogramWithInput(byTypeTable);
			constantHistogram = new ItemHistogramWithInput(constantValueTable);
			byTypeHistogram.addListener(this::typeHistogramListener);
			constantHistogram.addListener(container::showSelection);

			PersistableSashForm.loadState(sash, state.getChild(SASH_ELEMENT));
			byTypeFilter.loadState(state.getChild(TYPE_FILTER));
			constantValueFilter.loadState(state.getChild(VALUE_FILTER));
		}

		private void typeHistogramListener(IItemCollection constantPoolType) {
			String poolName = ItemToolkit.getFirstFound(constantPoolType, JdkAttributes.CONSTANT_POOLS_NAME);
			IItemCollection filteredItems = getDataSource().getConstants()
					.apply(ItemFilters.equals(JdkAttributes.CONSTANT_TYPE, poolName));
			constantHistogram.setInput(filteredItems);
			container.showSelection(filteredItems);
		}

		private void onTypeFilterChange(IItemFilter filter) {
			byTypeFilter.filterChangeHelper(filter, byTypeTable, selectionConstPoolItems);
		}

		private void onValueFilterChange(IItemFilter filter) {
			constantValueFilter.filterChangeHelper(filter, constantValueTable, selectionConstantItems);
		}

		@Override
		public void saveTo(IWritableState writableState) {
			PersistableSashForm.saveState(sash, writableState.createChild(SASH_ELEMENT));
			byTypeTable.getManager().getSettings().saveState(writableState.createChild(TABLE));
			byTypeFilter.saveState(writableState.createChild(TYPE_FILTER));
			constantValueTable.getManager().getSettings().saveState(writableState.createChild(CONSTANT_TABLE));
			constantValueFilter.saveState(writableState.createChild(VALUE_FILTER));
		}
	}

	public ConstantPoolsPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
		super(dpd, items, editor);
	}

	@Override
	public IPageUI display(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
		return new ConstantPoolsPageUi(parent, toolkit, pageContainer, state);
	}
}
