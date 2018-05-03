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

import java.util.Arrays;
import java.util.function.Consumer;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
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
import org.openjdk.jmc.flightrecorder.ui.common.FlavorSelector;
import org.openjdk.jmc.flightrecorder.ui.common.FlavorSelector.FlavorSelectorState;
import org.openjdk.jmc.flightrecorder.ui.common.ImageConstants;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram.ItemHistogramBuilder;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogramWithInput;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStoreActionToolkit;
import org.openjdk.jmc.ui.column.ColumnManager.SelectionState;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.column.TableSettings.ColumnSettings;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.PersistableSashForm;

public class LockInstancesPage extends AbstractDataPage {

	private static final IAggregator<IQuantity, ?> BY_THREAD_AGGREGATOR = Aggregators.filter(
			Aggregators.countDistinct(Messages.LockInstancesPage_AGGR_BY_THREAD,
					Messages.LockInstancesPage_AGGR_BY_THREAD_DESC, JfrAttributes.EVENT_THREAD),
			JdkFilters.MONITOR_ENTER);

	private static final IAggregator<IQuantity, ?> BY_ADDRESS_AGGREGATOR = Aggregators.filter(
			Aggregators.countDistinct(Messages.LockInstancesPage_AGGR_BY_ADDRESS,
					Messages.LockInstancesPage_AGGR_BY_ADDRESS_DESC, JdkAttributes.MONITOR_ADDRESS),
			JdkFilters.MONITOR_ENTER);

	public static class LockInstancesPageFactory implements IDataPageFactory {
		@Override
		public String getName(IState state) {
			return Messages.LockInstancesPage_PAGE_NAME;
		}

		@Override
		public ImageDescriptor getImageDescriptor(IState state) {
			return FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.PAGE_LOCKINSTANCES);
		}

		@Override
		public String[] getTopics(IState state) {
			return new String[] {JfrRuleTopics.LOCK_INSTANCES_TOPIC};
		}

		@Override
		public IDisplayablePage createPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
			return new LockInstancesPage(dpd, items, editor);
		}

	}

	private static final IItemFilter TABLE_ITEMS = ItemFilters.type(JdkTypeIDs.MONITOR_ENTER);

	private static final ItemHistogramBuilder BY_CLASS_HISTOGRAM = new ItemHistogramBuilder();
	private static final ItemHistogramBuilder BY_ADDRESS_HISTOGRAM = new ItemHistogramBuilder();
	private static final ItemHistogramBuilder BY_THREAD_HISTOGRAM = new ItemHistogramBuilder();
	private static final String STD_DEV_DURATION = "stdDevDuration"; //$NON-NLS-1$
	private static final String AVG_DURATION = "avgDuration"; //$NON-NLS-1$
	private static final String MAX_DURATION = "maxDuration"; //$NON-NLS-1$

	static {
		BY_CLASS_HISTOGRAM.addCountColumn();
		BY_CLASS_HISTOGRAM.addColumn(JfrAttributes.DURATION.getIdentifier(), JdkAggregators.TOTAL_BLOCKED_TIME);
		BY_CLASS_HISTOGRAM.addColumn(MAX_DURATION, JdkAggregators.MAX_BLOCKED_TIME);
		BY_CLASS_HISTOGRAM.addColumn(AVG_DURATION, JdkAggregators.AVG_BLOCKED_TIME);
		BY_CLASS_HISTOGRAM.addColumn(STD_DEV_DURATION, JdkAggregators.STDDEV_BLOCKED_TIME);
		BY_CLASS_HISTOGRAM.addColumn(JdkAttributes.MONITOR_CLASS.getIdentifier(), BY_ADDRESS_AGGREGATOR);
		BY_CLASS_HISTOGRAM.addColumn(JfrAttributes.EVENT_THREAD.getIdentifier(), BY_THREAD_AGGREGATOR);
		BY_ADDRESS_HISTOGRAM.addCountColumn();
		BY_ADDRESS_HISTOGRAM.addColumn(JfrAttributes.DURATION.getIdentifier(), JdkAggregators.TOTAL_BLOCKED_TIME);
		BY_ADDRESS_HISTOGRAM.addColumn(MAX_DURATION, JdkAggregators.MAX_BLOCKED_TIME);
		BY_ADDRESS_HISTOGRAM.addColumn(AVG_DURATION, JdkAggregators.AVG_BLOCKED_TIME);
		BY_ADDRESS_HISTOGRAM.addColumn(STD_DEV_DURATION, JdkAggregators.STDDEV_BLOCKED_TIME);
		BY_ADDRESS_HISTOGRAM.addColumn(JfrAttributes.EVENT_THREAD.getIdentifier(), BY_THREAD_AGGREGATOR);
		BY_THREAD_HISTOGRAM.addCountColumn();
		BY_THREAD_HISTOGRAM.addColumn(JfrAttributes.DURATION.getIdentifier(), JdkAggregators.TOTAL_BLOCKED_TIME);
		BY_THREAD_HISTOGRAM.addColumn(MAX_DURATION, JdkAggregators.MAX_BLOCKED_TIME);
		BY_THREAD_HISTOGRAM.addColumn(AVG_DURATION, JdkAggregators.AVG_BLOCKED_TIME);
		BY_THREAD_HISTOGRAM.addColumn(STD_DEV_DURATION, JdkAggregators.STDDEV_BLOCKED_TIME);
	}

	private class LockInstancesPageUi implements IPageUI {

		private static final String CLASS_FILTER = "classFilter"; //$NON-NLS-1$
		private static final String ADDRESS_FILTER = "addressFilter"; //$NON-NLS-1$
		private static final String THREAD_FILTER = "threadFilter"; //$NON-NLS-1$
		private static final String SASH_ELEMENT = "sash"; //$NON-NLS-1$
		private static final String BY_CLASS_TABLE_ELEMENT = "byClassTable"; //$NON-NLS-1$
		private static final String BY_ADDRESS_TABLE_ELEMENT = "byClassTable"; //$NON-NLS-1$
		private static final String BY_THREAD_TABLE_ELEMENT = "byClassTable"; //$NON-NLS-1$

		private final ItemHistogram byClassTable;
		private final ItemHistogram byAddressTable;
		private final ItemHistogram byThreadTable;
		private final SashForm sash;
		private Consumer<IItemCollection> histogramChain;
		private FilterComponent byClassFilter;
		private FilterComponent byAddressFilter;
		private FilterComponent byThreadFilter;
		private FlavorSelector flavorSelector;
		private IItemCollection selectionItems;

		LockInstancesPageUi(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
			Form form = DataPageToolkit.createForm(parent, toolkit, getName(), getIcon());
			selectionItems = getDataSource().getItems();

			sash = new SashForm(form.getBody(), SWT.VERTICAL);
			toolkit.adapt(sash);

			byClassTable = BY_CLASS_HISTOGRAM.buildWithoutBorder(sash, JdkAttributes.MONITOR_CLASS,
					getTableSettings(state.getChild(BY_CLASS_TABLE_ELEMENT)));
			MCContextMenuManager mm = MCContextMenuManager.create(byClassTable.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(byClassTable.getManager(), mm);
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(), byClassTable,
					Messages.LockInstancesPage_CLASS_HISTOGRAM_SELECTION, mm);

			byClassFilter = FilterComponent.createFilterComponent(byClassTable, LockInstancesPage.this.byClassFilter,
					getDataSource().getItems().apply(TABLE_ITEMS), pageContainer.getSelectionStore()::getSelections,
					this::onClassFilterChange);
			mm.add(byClassFilter.getShowFilterAction());
			mm.add(byClassFilter.getShowSearchAction());

			byAddressTable = BY_ADDRESS_HISTOGRAM.buildWithoutBorder(sash, JdkAttributes.MONITOR_ADDRESS,
					getTableSettings(state.getChild(BY_ADDRESS_TABLE_ELEMENT)));
			mm = MCContextMenuManager.create(byAddressTable.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(byAddressTable.getManager(), mm);
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(), byAddressTable,
					Messages.LockInstancesPage_ADDRESS_HISTOGRAM_SELECTION, mm);

			byAddressFilter = FilterComponent.createFilterComponent(byAddressTable,
					LockInstancesPage.this.byAddressFilter, getDataSource().getItems().apply(TABLE_ITEMS),
					pageContainer.getSelectionStore()::getSelections, this::onAddressFilterChange);
			mm.add(byAddressFilter.getShowFilterAction());
			mm.add(byAddressFilter.getShowSearchAction());

			byThreadTable = BY_THREAD_HISTOGRAM.buildWithoutBorder(sash, JfrAttributes.EVENT_THREAD,
					getTableSettings(state.getChild(BY_THREAD_TABLE_ELEMENT)));
			mm = MCContextMenuManager.create(byThreadTable.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(byThreadTable.getManager(), mm);
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(), byThreadTable,
					Messages.LockInstancesPage_THREAD_HISTOGRAM_SELECTION, mm);

			byThreadFilter = FilterComponent.createFilterComponent(byThreadTable, LockInstancesPage.this.byThreadFilter,
					getDataSource().getItems().apply(TABLE_ITEMS), pageContainer.getSelectionStore()::getSelections,
					this::onThreadFilterChange);
			mm.add(byThreadFilter.getShowFilterAction());
			mm.add(byThreadFilter.getShowSearchAction());

			histogramChain = ItemHistogramWithInput.chain(byClassTable, pageContainer::showSelection, byAddressTable,
					byThreadTable);
			PersistableSashForm.loadState(sash, state.getChild(SASH_ELEMENT));
			flavorSelector = FlavorSelector.itemsWithTimerange(form, TABLE_ITEMS, getDataSource().getItems(),
					pageContainer, this::onInputSelected, flavorSelectorState);

			addResultActions(form);

			byClassFilter.loadState(state.getChild(CLASS_FILTER));
			byAddressFilter.loadState(state.getChild(ADDRESS_FILTER));
			byThreadFilter.loadState(state.getChild(THREAD_FILTER));

			byClassTable.getManager().setSelectionState(byClassState);
			onClassFilterChange(LockInstancesPage.this.byClassFilter);
			byAddressTable.getManager().setSelectionState(byAddressState);
			onAddressFilterChange(LockInstancesPage.this.byAddressFilter);
			byThreadTable.getManager().setSelectionState(byThreadState);
			onThreadFilterChange(LockInstancesPage.this.byThreadFilter);
		}

		private void onClassFilterChange(IItemFilter filter) {
			byClassFilter.filterChangeHelper(filter, byClassTable, selectionItems.apply(TABLE_ITEMS));
			LockInstancesPage.this.byClassFilter = filter;
			byAddressFilter.notifyListener();
			byThreadFilter.notifyListener();
		}

		private void onAddressFilterChange(IItemFilter filter) {
			byAddressFilter.filterChangeHelper(filter, byAddressTable, selectionItems.apply(TABLE_ITEMS));
			byThreadFilter.notifyListener();
			LockInstancesPage.this.byAddressFilter = filter;
		}

		private void onThreadFilterChange(IItemFilter filter) {
			byThreadFilter.filterChangeHelper(filter, byThreadTable, selectionItems.apply(TABLE_ITEMS));
			LockInstancesPage.this.byThreadFilter = filter;
		}

		@Override
		public void saveTo(IWritableState writableState) {
			PersistableSashForm.saveState(sash, writableState.createChild(SASH_ELEMENT));
			byClassTable.getManager().getSettings().saveState(writableState.createChild(BY_CLASS_TABLE_ELEMENT));
			byClassFilter.saveState(writableState.createChild(CLASS_FILTER));
			byAddressTable.getManager().getSettings().saveState(writableState.createChild(BY_CLASS_TABLE_ELEMENT));
			byAddressFilter.saveState(writableState.createChild(ADDRESS_FILTER));
			byThreadTable.getManager().getSettings().saveState(writableState.createChild(BY_CLASS_TABLE_ELEMENT));
			byThreadFilter.saveState(writableState.createChild(THREAD_FILTER));

			saveToLocal();
		}

		private void saveToLocal() {
			byClassState = byClassTable.getManager().getSelectionState();
			byAddressState = byAddressTable.getManager().getSelectionState();
			byThreadState = byThreadTable.getManager().getSelectionState();
			flavorSelectorState = flavorSelector.getFlavorSelectorState();
		}

		private void onInputSelected(IItemCollection items, IRange<IQuantity> timeRange) {
			selectionItems = items != null ? items.apply(TABLE_ITEMS) : getDataSource().getItems().apply(TABLE_ITEMS);
			histogramChain.accept(selectionItems);
		}
	}

	private static TableSettings getTableSettings(IState state) {
		if (state == null) {
			return new TableSettings(JfrAttributes.DURATION.getIdentifier(),
					Arrays.asList(new ColumnSettings(ItemHistogram.KEY_COL_ID, false, 500, null)));
		} else {
			return new TableSettings(state);
		}
	}

	private IItemFilter byAddressFilter;
	private SelectionState byAddressState;
	private IItemFilter byThreadFilter;
	private SelectionState byThreadState;
	private IItemFilter byClassFilter;
	private SelectionState byClassState;
	public FlavorSelectorState flavorSelectorState;

	@Override
	public IPageUI display(Composite parent, FormToolkit toolkit, IPageContainer editor, IState state) {
		return new LockInstancesPageUi(parent, toolkit, editor, state);
	}

	@Override
	public IItemFilter getDefaultSelectionFilter() {
		return TABLE_ITEMS;
	}

	public LockInstancesPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
		super(dpd, items, editor);
	}

}
