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

import java.awt.Color;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkQueries;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.IDataPageFactory;
import org.openjdk.jmc.flightrecorder.ui.IDisplayablePage;
import org.openjdk.jmc.flightrecorder.ui.IPageContainer;
import org.openjdk.jmc.flightrecorder.ui.IPageDefinition;
import org.openjdk.jmc.flightrecorder.ui.IPageUI;
import org.openjdk.jmc.flightrecorder.ui.ItemCollectionToolkit;
import org.openjdk.jmc.flightrecorder.ui.StreamModel;
import org.openjdk.jmc.flightrecorder.ui.common.AbstractDataPage;
import org.openjdk.jmc.flightrecorder.ui.common.DataPageToolkit;
import org.openjdk.jmc.flightrecorder.ui.common.FilterComponent;
import org.openjdk.jmc.flightrecorder.ui.common.FlavorSelector;
import org.openjdk.jmc.flightrecorder.ui.common.FlavorSelector.FlavorSelectorState;
import org.openjdk.jmc.flightrecorder.ui.common.ImageConstants;
import org.openjdk.jmc.flightrecorder.ui.common.ItemList;
import org.openjdk.jmc.flightrecorder.ui.common.ItemList.ItemListBuilder;
import org.openjdk.jmc.flightrecorder.ui.common.ItemRow;
import org.openjdk.jmc.flightrecorder.ui.common.TypeLabelProvider;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStoreActionToolkit;
import org.openjdk.jmc.ui.charts.IXDataRenderer;
import org.openjdk.jmc.ui.charts.RendererToolkit;
import org.openjdk.jmc.ui.charts.XYChart;
import org.openjdk.jmc.ui.column.ColumnManager.SelectionState;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.ChartCanvas;
import org.openjdk.jmc.ui.misc.PersistableSashForm;

public class CompilationsPage extends AbstractDataPage {

	private static final Color COMPILATIONS_COLOR = TypeLabelProvider.getColor(JdkTypeIDs.COMPILATION);

	private static final ItemListBuilder COMPILATIONS_LIST = new ItemListBuilder();
	private static final ItemListBuilder FAILED_COMPILATIONS_LIST = new ItemListBuilder();

	static {
		COMPILATIONS_LIST.addColumn(JdkAttributes.COMPILER_COMPILATION_ID);
		COMPILATIONS_LIST.addColumn(JdkAttributes.COMPILER_METHOD);
		COMPILATIONS_LIST.addColumn(JfrAttributes.START_TIME);
		COMPILATIONS_LIST.addColumn(JfrAttributes.DURATION);
		COMPILATIONS_LIST.addColumn(JfrAttributes.END_TIME);
		COMPILATIONS_LIST.addColumn(JfrAttributes.EVENT_THREAD);
		COMPILATIONS_LIST.addColumn(JdkAttributes.COMPILER_CODE_SIZE);
		COMPILATIONS_LIST.addColumn(JdkAttributes.COMPILER_INLINED_SIZE);
		COMPILATIONS_LIST.addColumn(JdkAttributes.COMPILER_COMPILATION_LEVEL);
		COMPILATIONS_LIST.addColumn(JdkAttributes.COMPILER_COMPILATION_SUCCEEDED);
		COMPILATIONS_LIST.addColumn(JdkAttributes.COMPILER_IS_OSR);
		FAILED_COMPILATIONS_LIST.addColumn(JfrAttributes.EVENT_TIMESTAMP);
		FAILED_COMPILATIONS_LIST.addColumn(JfrAttributes.EVENT_THREAD);
		FAILED_COMPILATIONS_LIST.addColumn(JdkAttributes.COMPILER_COMPILATION_ID);
		FAILED_COMPILATIONS_LIST.addColumn(JdkAttributes.COMPILER_FAILED_MESSAGE);
	}

	public static class CompilationsPageFactory implements IDataPageFactory {
		@Override
		public String getName(IState state) {
			return Messages.CompilationsPage_PAGE_NAME;
		}

		@Override
		public ImageDescriptor getImageDescriptor(IState state) {
			return FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.PAGE_COMPILATIONS);
		}

		@Override
		public String[] getTopics(IState state) {
			return new String[] {JfrRuleTopics.COMPILATIONS_TOPIC};
		}

		@Override
		public IDisplayablePage createPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
			return new CompilationsPage(dpd, items, editor);
		}

	}

	private class CompilationsPageUi implements IPageUI {

		private static final String COMPILATIONS_FILTER = "compilationsFilter"; //$NON-NLS-1$
		private static final String FAILED_COMPILATIONS_FILTER = "failedCompilationsFilter"; //$NON-NLS-1$
		private final SashForm sash;
		private final ChartCanvas durationCanvas;
		private final ItemList compilationsTable;
		private final ItemList compilationsFailedTable;
		private FilterComponent compilationsFilter;
		private FilterComponent compilationsFailedFilter;
		private CTabFolder tabFolder;
		private XYChart durationChart;
		private IItemCollection selectedItems;
		private FlavorSelector flavorSelector;

		CompilationsPageUi(Composite container, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
			Form form = DataPageToolkit.createForm(container, toolkit, getName(), getIcon());

			sash = new SashForm(form.getBody(), SWT.VERTICAL);

			durationCanvas = new ChartCanvas(sash);
			DataPageToolkit.createChartTooltip(durationCanvas);

			tabFolder = new CTabFolder(sash, SWT.NONE);

			// FIXME: Might like to have Method Formatting Options here
			compilationsTable = COMPILATIONS_LIST.buildWithoutBorder(tabFolder,
					TableSettings.forState(state.getChild(COMPILATIONS_TABLE)));
			compilationsTable.getManager().getViewer().addSelectionChangedListener(e -> pageContainer
					.showSelection(ItemCollectionToolkit.build(compilationsTable.getSelection().get())));
			compilationsFilter = FilterComponent.createFilterComponent(compilationsTable, compilationsFilterState,
					getDataSource().getItems().apply(JdkFilters.COMPILATION),
					pageContainer.getSelectionStore()::getSelections, this::onCompilationsFilterChange);
			MCContextMenuManager compilationsMm = MCContextMenuManager
					.create(compilationsTable.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(compilationsTable.getManager(), compilationsMm);
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(), compilationsTable,
					Messages.CompilationsPage_COMPILATIONS_TABLE_SELECTION, compilationsMm);
			compilationsMm.add(compilationsFilter.getShowFilterAction());
			compilationsMm.add(compilationsFilter.getShowSearchAction());
			DataPageToolkit.addTabItem(tabFolder, compilationsFilter.getComponent(),
					Messages.CompilationsPage_TAB_COMPILATIONS);

			compilationsFailedTable = FAILED_COMPILATIONS_LIST.buildWithoutBorder(tabFolder,
					TableSettings.forState(state.getChild(FAILED_COMPILATIONS_TABLE)));
			compilationsFailedTable.getManager().getViewer().addSelectionChangedListener(e -> pageContainer
					.showSelection(ItemCollectionToolkit.build(compilationsFailedTable.getSelection().get())));
			compilationsFailedFilter = FilterComponent.createFilterComponent(compilationsFailedTable,
					compilationsFailedFilterState, getDataSource().getItems().apply(JdkFilters.COMPILER_FAILURE),
					pageContainer.getSelectionStore()::getSelections, this::onCompilationsFailedFilterChange);
			MCContextMenuManager compilationsFailedMm = MCContextMenuManager
					.create(compilationsFailedTable.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(compilationsFailedTable.getManager(), compilationsFailedMm);
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(),
					compilationsFailedTable, Messages.CompilationsPage_COMPILATIONS_FAILED_TABLE_SELECTION,
					compilationsFailedMm);
			compilationsFailedMm.add(compilationsFailedFilter.getShowFilterAction());
			compilationsFailedMm.add(compilationsFailedFilter.getShowSearchAction());
			DataPageToolkit.addTabItem(tabFolder, compilationsFailedFilter.getComponent(),
					Messages.CompilationsPage_TAB_COMPILATIONS_FAILED);

			IQuantity max = getDataSource().getItems().getAggregate(JdkAggregators.LONGEST_COMPILATION);
			// FIXME: Workaround to make max value included
			max = max == null ? UnitLookup.MILLISECOND.quantity(20) : max.add(UnitLookup.MILLISECOND.quantity(20));
			IXDataRenderer durationRoot = RendererToolkit.layers(DataPageToolkit.buildDurationHistogram(
					Messages.CompilationsPage_ROW_DURATIONS, JdkAggregators.COMPILATIONS_COUNT.getDescription(),
					getDataSource().getItems().apply(JdkFilters.COMPILATION), JdkAggregators.COMPILATIONS_COUNT,
					COMPILATIONS_COLOR));
			durationChart = new XYChart(UnitLookup.MILLISECOND.quantity(0), max, durationRoot, 180);
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(), durationChart,
					JfrAttributes.DURATION, Messages.CompilationsPage_COMPILATIONS_DURATION_SELECTION,
					durationCanvas.getContextMenu());
			durationCanvas.setChart(durationChart);
			durationCanvas.setSelectionListener(() -> {
				pageContainer.showSelection(ItemRow.getSelection(durationChart, JfrAttributes.DURATION));
				compilationsTable.show(ItemRow.getSelection(durationChart, JfrAttributes.DURATION));
			});
			if (durationsRange != null) {
				durationChart.setVisibleRange(durationsRange.getStart(), durationsRange.getEnd());
			}

			durationChart.addVisibleRangeListener(r -> durationsRange = r);
			tabFolder.setSelection(tabFolderIndex);

			compilationsFilter.loadState(getState().getChild(COMPILATIONS_FILTER));
			compilationsFailedFilter.loadState(getState().getChild(FAILED_COMPILATIONS_FILTER));
			PersistableSashForm.loadState(sash, state.getChild(SASH));

			flavorSelector = FlavorSelector.itemsWithTimerange(form, TABLE_FILTER, getDataSource().getItems(),
					pageContainer, this::onInputSelected, flavorSelectorState);
			addResultActions(form);

			compilationsTable.getManager().setSelectionState(compilationsState);
			compilationsFailedTable.getManager().setSelectionState(compilationsFailedState);
		}

		private void onCompilationsFilterChange(IItemFilter filter) {
			compilationsFilter.filterChangeHelper(filter, compilationsTable, getItems().apply(JdkFilters.COMPILATION));
			compilationsFilterState = filter;
		}

		private void onCompilationsFailedFilterChange(IItemFilter filter) {
			compilationsFailedFilter.filterChangeHelper(filter, compilationsFailedTable,
					getItems().apply(JdkFilters.COMPILER_FAILURE));
			compilationsFailedFilterState = filter;
		}

		private IItemCollection getItems() {
			return selectedItems != null ? selectedItems : getDataSource().getItems();
		}

		@Override
		public void saveTo(IWritableState memento) {
			PersistableSashForm.saveState(sash, memento.createChild(SASH));
			compilationsTable.getManager().getSettings().saveState(memento.createChild(COMPILATIONS_TABLE));
			compilationsFailedTable.getManager().getSettings()
					.saveState(memento.createChild(FAILED_COMPILATIONS_TABLE));
			compilationsFilter.saveState(memento.createChild(COMPILATIONS_FILTER));
			compilationsFailedFilter.saveState(memento.createChild(FAILED_COMPILATIONS_FILTER));

			saveToLocal();
		}

		private void saveToLocal() {
			compilationsState = compilationsTable.getManager().getSelectionState();
			compilationsFailedState = compilationsFailedTable.getManager().getSelectionState();
			tabFolderIndex = tabFolder.getSelectionIndex();
			flavorSelectorState = flavorSelector.getFlavorSelectorState();
		}

		private void onInputSelected(IItemCollection items, IRange<IQuantity> timeRange) {
			selectedItems = items != null ? items : getDataSource().getItems();
			IItemCollection compilationItems = selectedItems.apply(JdkFilters.COMPILATION);
			IItemCollection failedCompilations = selectedItems.apply(JdkFilters.COMPILER_FAILURE);

			IXDataRenderer durationRoot = RendererToolkit.layers(DataPageToolkit.buildDurationHistogram(
					Messages.CompilationsPage_ROW_DURATIONS, JdkAggregators.COMPILATIONS_COUNT.getDescription(),
					compilationItems, JdkAggregators.COMPILATIONS_COUNT, COMPILATIONS_COLOR));
			durationChart.setRendererRoot(durationRoot);
			durationCanvas.redrawChart();
			compilationsTable.show(compilationItems);
			compilationsFailedTable.show(failedCompilations);
			onCompilationsFilterChange(compilationsFilterState);
			onCompilationsFailedFilterChange(compilationsFailedFilterState);
		}
	}

	private static final String SASH = "sash"; //$NON-NLS-1$
	// Renamed from failedCompilationsTable to avoid build errors from unrecognized element in plugin.xml
	private static final String FAILED_COMPILATIONS_TABLE = "fCompilationsTable"; //$NON-NLS-1$
	private static final String COMPILATIONS_TABLE = "compilationsTable"; //$NON-NLS-1$
	private static final IItemFilter TABLE_FILTER = ItemFilters.or(JdkQueries.COMPILER_FAILURE.getFilter(),
			JdkQueries.COMPILATION.getFilter());

	@Override
	public IPageUI display(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
		return new CompilationsPageUi(parent, toolkit, pageContainer, state);
	}

	private SelectionState compilationsState;
	private IItemFilter compilationsFilterState;
	private SelectionState compilationsFailedState;
	private IItemFilter compilationsFailedFilterState;
	private int tabFolderIndex = 0;
	private IRange<IQuantity> durationsRange;
	public FlavorSelectorState flavorSelectorState;

	public CompilationsPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
		super(dpd, items, editor);
	}

	@Override
	public IItemFilter getDefaultSelectionFilter() {
		return TABLE_FILTER;
	}
}
