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


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openjdk.jmc.common.IMCClassLoader;
import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.item.RangeMatchPolicy;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
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
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram.ItemHistogramBuilder;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogramWithInput;
import org.openjdk.jmc.flightrecorder.ui.common.ItemList;
import org.openjdk.jmc.flightrecorder.ui.common.ItemList.ItemListBuilder;
import org.openjdk.jmc.flightrecorder.ui.common.TypeLabelProvider;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStoreActionToolkit;
import org.openjdk.jmc.ui.charts.IXDataRenderer;
import org.openjdk.jmc.ui.charts.RendererToolkit;
import org.openjdk.jmc.ui.charts.XYChart;
import org.openjdk.jmc.ui.column.ColumnManager.SelectionState;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.handlers.ActionToolkit;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.ActionUiToolkit;
import org.openjdk.jmc.ui.misc.ChartCanvas;
import org.openjdk.jmc.ui.misc.PersistableSashForm;

public class ClassLoadingPage extends AbstractDataPage {
	public static class ClassLoadingPageFactory implements IDataPageFactory {

		@Override
		public String getName(IState state) {
			return Messages.ClassLoadingPage_PAGE_NAME;
		}

		@Override
		public ImageDescriptor getImageDescriptor(IState state) {
			return FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.PAGE_CLASSLOADING);
		}

		@Override
		public String[] getTopics(IState state) {
			return new String[] {JfrRuleTopics.CLASS_LOADING_TOPIC};
		}

		@Override
		public IDisplayablePage createPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
			return new ClassLoadingPage(dpd, items, editor);
		}
	}

	private static final IItemFilter TABLE_FILTER = ItemFilters.or(JdkQueries.CLASS_LOAD.getFilter(),
			JdkQueries.CLASS_UNLOAD.getFilter());
	private static final ItemHistogramBuilder CLASSLOADER_HISTOGRAM = new ItemHistogramBuilder();
	private static final ItemListBuilder CLASS_LOADING_LIST = new ItemListBuilder();
	private static final ItemListBuilder CLASS_UNLOADING_LIST = new ItemListBuilder();
	private static final ItemListBuilder CLASS_DEFINE_LIST = new ItemListBuilder();
	private static final ItemListBuilder CLASS_LOADER_STATISTICS_LIST = new ItemListBuilder();
	private static final Map<String, Boolean> LEGEND_ITEMS = new LinkedHashMap<>();
	private static final String LOADED_COUNT = "loadedCount"; //$NON-NLS-1$
	private static final String UNLOADED_COUNT = "unloadedCount"; //$NON-NLS-1$
	private static final String CLASS_LOAD = "classLoad"; //$NON-NLS-1$
	private static final String CLASS_UNLOAD = "classUnload"; //$NON-NLS-1$

	static {
		CLASSLOADER_HISTOGRAM.addColumn(LOADED_COUNT,
				Aggregators.count(Messages.ClassLoadingPage_AGGR_CLASSES_LOADED_BY_CLASSLOADER,
						Messages.ClassLoadingPage_AGGR_CLASSES_LOADED_BY_CLASSLOADER_DESC,
						ItemFilters.type(JdkTypeIDs.CLASS_LOAD)));
		CLASSLOADER_HISTOGRAM.addColumn(UNLOADED_COUNT,
				Aggregators.count(Messages.ClassLoadingPage_AGGR_CLASSES_UNLOADED_BY_CLASSLOADER,
						Messages.ClassLoadingPage_AGGR_CLASSES_UNLOADED_BY_CLASSLOADER_DESC,
						ItemFilters.type(JdkTypeIDs.CLASS_UNLOAD)));

		CLASS_LOADING_LIST.addColumn(JdkAttributes.CLASS_LOADED);
		CLASS_LOADING_LIST.addColumn(JdkAttributes.CLASS_DEFINING_CLASSLOADER);
		CLASS_LOADING_LIST.addColumn(JdkAttributes.CLASS_INITIATING_CLASSLOADER);
		CLASS_LOADING_LIST.addColumn(JfrAttributes.START_TIME);
		CLASS_LOADING_LIST.addColumn(JfrAttributes.DURATION);
		CLASS_LOADING_LIST.addColumn(JfrAttributes.END_TIME);
		CLASS_LOADING_LIST.addColumn(JfrAttributes.EVENT_THREAD);
		
		CLASS_UNLOADING_LIST.addColumn(JfrAttributes.EVENT_TIMESTAMP);
		CLASS_UNLOADING_LIST.addColumn(JfrAttributes.EVENT_THREAD);
		CLASS_UNLOADING_LIST.addColumn(JdkAttributes.CLASS_UNLOADED);
		CLASS_UNLOADING_LIST.addColumn(JdkAttributes.CLASS_DEFINING_CLASSLOADER);
		
		CLASS_DEFINE_LIST.addColumn(JfrAttributes.START_TIME);
		CLASS_DEFINE_LIST.addColumn(JdkAttributes.CLASS_DEFINING_CLASSLOADER);
		CLASS_DEFINE_LIST.addColumn(JdkAttributes.CLASS_DEFINED);
		CLASS_DEFINE_LIST.addColumn(JfrAttributes.EVENT_THREAD);
		
		CLASS_LOADER_STATISTICS_LIST.addColumn(JdkAttributes.ANONYMOUS_BLOCK_SIZE);
		CLASS_LOADER_STATISTICS_LIST.addColumn(JdkAttributes.ANONYMOUS_CHUNK_SIZE);
		CLASS_LOADER_STATISTICS_LIST.addColumn(JdkAttributes.ANONYMOUS_CLASS_COUNT);
		CLASS_LOADER_STATISTICS_LIST.addColumn(JdkAttributes.BLOCK_SIZE);
		CLASS_LOADER_STATISTICS_LIST.addColumn(JdkAttributes.CHUNK_SIZE);
		CLASS_LOADER_STATISTICS_LIST.addColumn(JdkAttributes.CLASS_COUNT);
		CLASS_LOADER_STATISTICS_LIST.addColumn(JdkAttributes.CLASS_LOADER_DATA);
		CLASS_LOADER_STATISTICS_LIST.addColumn(JdkAttributes.PARENT_CLASSLOADER);
		CLASS_LOADER_STATISTICS_LIST.addColumn(JdkAttributes.CLASSLOADER);
		CLASS_LOADER_STATISTICS_LIST.addColumn(JfrAttributes.START_TIME);
		// FIXME: Need to make a label provider for this
		// FIXME: Want to have this in the same order

		LEGEND_ITEMS.put(JdkAttributes.CLASSLOADER_LOADED_COUNT.getIdentifier(), Boolean.TRUE);
		LEGEND_ITEMS.put(JdkAttributes.CLASSLOADER_UNLOADED_COUNT.getIdentifier(), Boolean.FALSE);
		LEGEND_ITEMS.put(CLASS_LOAD, Boolean.TRUE);
		LEGEND_ITEMS.put(CLASS_UNLOAD, Boolean.FALSE);
	}

	private class ClassLoadingUi implements IPageUI {

		private final ChartCanvas classLoadingChart;
		private final ItemList classLoadingTable;
		private final ItemList classUnloadingTable;
		private final ItemList classDefineTable;
		private final ItemList classLoaderStatisticsTable;
		private FilterComponent classLoadingFilter;
		private FilterComponent classUnloadingFilter;
		private FilterComponent classDefineFilter;
		private FilterComponent classLoaderStatisticsFilter;
		private final SashForm sash;
		private final IPageContainer pageContainer;
		private IItemCollection selectionItems;
		private ItemHistogram classloaderHistogram;
		private FilterComponent classloaderHistogramFilter;
		private final IAction classLoadAction = DataPageToolkit.createTypeCheckAction(CLASS_LOAD, JdkTypeIDs.CLASS_LOAD,
				Messages.ClassLoadingPage_CLASS_LOADING_ACTION, Messages.ClassLoadingPage_CLASS_LOADING_ACTION_DESC,
				b -> updateChart());
		private final IAction classUnloadAction = DataPageToolkit.createTypeCheckAction(CLASS_UNLOAD,
				JdkTypeIDs.CLASS_UNLOAD, Messages.ClassLoadingPage_CLASS_UNLOADING_ACTION,
				Messages.ClassLoadingPage_CLASS_UNLOADING_ACTION_DESC, b -> updateChart());
		private final Stream<IAction> statsActions = Stream
				.of(JdkAttributes.CLASSLOADER_LOADED_COUNT, JdkAttributes.CLASSLOADER_UNLOADED_COUNT)
				.map(a -> DataPageToolkit.createAttributeCheckAction(a, b -> updateChart()));
		private final List<IAction> allChartSeriesActions = Stream
				.concat(Stream.of(classLoadAction, classUnloadAction), statsActions).collect(Collectors.toList());
		private CTabFolder tabFolder;
		private CTabFolder classloaderFolder;
		private XYChart chart;
		private IRange<IQuantity> timeRange;
		private FlavorSelector flavorSelector;

		ClassLoadingUi(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
			this.pageContainer = pageContainer;

			Form form = DataPageToolkit.createForm(parent, toolkit, getName(), getIcon());

			sash = new SashForm(form.getBody(), SWT.VERTICAL);

			Composite chartComp = new Composite(sash, SWT.NONE);
			chartComp.setLayout(new GridLayout());
			Control legend = ActionUiToolkit.buildCheckboxControl(chartComp, allChartSeriesActions.stream(), false);
			legend.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			ActionToolkit.loadCheckState(state.getChild(CHART), allChartSeriesActions.stream());

			chart = new XYChart(pageContainer.getRecordingRange(), RendererToolkit.empty(), 180);
			chart.setVisibleRange(timelineRange.getStart(), timelineRange.getEnd());
			chart.addVisibleRangeListener(r -> timelineRange = r);
			classLoadingChart = new ChartCanvas(chartComp);
			classLoadingChart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			DataPageToolkit.createChartTimestampTooltip(classLoadingChart);
			DataPageToolkit.setChart(classLoadingChart, chart, pageContainer::showSelection, this::onChartSelection);
			SelectionStoreActionToolkit.addSelectionStoreRangeActions(pageContainer.getSelectionStore(), chart,
					JfrAttributes.LIFETIME, Messages.ClassLoadingPage_CLASS_LOADING_TIMELINE_SELECTION,
					classLoadingChart.getContextMenu());

			classloaderFolder = new CTabFolder(sash, SWT.NONE);

			classloaderHistogram = CLASSLOADER_HISTOGRAM.buildWithoutBorder(classloaderFolder,
					JdkAttributes.CLASS_DEFINING_CLASSLOADER, TableSettings.forState(state.getChild(HISTOGRAM)));
			classloaderHistogramFilter = FilterComponent.createFilterComponent(classloaderHistogram, null,
					getDataSource().getItems().apply(JdkFilters.CLASS_LOAD_OR_UNLOAD),
					pageContainer.getSelectionStore()::getSelections, this::onHistogramFilterChange);
			classloaderHistogram.getManager().getViewer().addSelectionChangedListener(
					e -> pageContainer.showSelection(classloaderHistogram.getSelection().getItems()));
			MCContextMenuManager classLoaderHistogramMm = MCContextMenuManager
					.create(classloaderHistogram.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(classloaderHistogram.getManager(), classLoaderHistogramMm);
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(),
					classloaderHistogram, Messages.ClassLoadingPage_CLASS_LOADING_HISTOGRAM_SELECTION,
					classLoaderHistogramMm);
			classLoaderHistogramMm.add(classloaderHistogramFilter.getShowFilterAction());
			classLoaderHistogramMm.add(classloaderHistogramFilter.getShowSearchAction());
			classloaderHistogramFilter.loadState(state.getChild(HISTOGRAM_FILTER));
			DataPageToolkit.addTabItem(classloaderFolder, classloaderHistogramFilter.getComponent(),
					Messages.ClassLoadingPage_CLASS_LOADER_TAB);

			ItemHistogramWithInput.chain(classloaderHistogram, this::updateTables);

			classLoaderStatisticsTable = CLASS_LOADER_STATISTICS_LIST.buildWithoutBorder(classloaderFolder,
					TableSettings.forState(state.getChild(CLASS_LOADER_STATISTICS_TABLE)));
			classLoaderStatisticsTable.getManager().getViewer().addSelectionChangedListener(e -> {
				// The standard aggregators will skip the null classloader, so we need to do this manually.
				IItemCollection selection = ItemCollectionToolkit.build(classLoaderStatisticsTable.getSelection().get());
				Stream<IMCClassLoader> stream = ItemCollectionToolkit.values(selection, JdkAttributes.CLASSLOADER).get().distinct();
				Set<IMCClassLoader> selected = stream.collect(Collectors.toSet());
				IItemFilter selectionFilter =  ItemFilters.and(ItemFilters.or(JdkFilters.CLASS_LOAD_OR_UNLOAD,
				JdkFilters.CLASS_DEFINE), ItemFilters.memberOf(JdkAttributes.CLASS_DEFINING_CLASSLOADER, selected));
 				IItemCollection filteredItems = getDataSource().getItems().apply(selectionFilter);
				pageContainer.showSelection(filteredItems);
				updateTables(filteredItems);
			});
			classLoaderStatisticsFilter = FilterComponent.createFilterComponent(classLoaderStatisticsTable, null,
					getDataSource().getItems().apply(JdkFilters.CLASS_LOADER_STATISTICS),
					pageContainer.getSelectionStore()::getSelections, this::onClassLoaderStatisticsFilterChange);
			MCContextMenuManager classLoaderStatisticsTableMm = MCContextMenuManager
					.create(classLoaderStatisticsTable.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(classLoaderStatisticsTable.getManager(), classLoaderStatisticsTableMm);
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(), classLoaderStatisticsTable,
					Messages.ClassLoadingPage_CLASS_LOADER_STATISTICS_LIST_SELECTION, classLoaderStatisticsTableMm);
			classLoaderStatisticsTableMm.add(classLoaderStatisticsFilter.getShowFilterAction());
			classLoaderStatisticsTableMm.add(classLoaderStatisticsFilter.getShowSearchAction());
			classLoaderStatisticsFilter.loadState(state.getChild(CLASS_LOADER_STATISTICS_FILTER));
			DataPageToolkit.addTabItem(classloaderFolder, classLoaderStatisticsFilter.getComponent(),
					Messages.ClassLoadingPage_CLASS_LOADER_STATISTICS_TAB_TITLE);

			tabFolder = new CTabFolder(sash, SWT.NONE);

			classLoadingTable = CLASS_LOADING_LIST.buildWithoutBorder(tabFolder,
					TableSettings.forState(state.getChild(CLASS_LOADING_TABLE)));
			classLoadingTable.getManager().getViewer().addSelectionChangedListener(e -> pageContainer
					.showSelection(ItemCollectionToolkit.build(classLoadingTable.getSelection().get())));
			classLoadingFilter = FilterComponent.createFilterComponent(classLoadingTable, null,
					getDataSource().getItems().apply(JdkFilters.CLASS_LOAD),
					pageContainer.getSelectionStore()::getSelections, this::onClassLoadFilterChange);
			MCContextMenuManager classLoadingTableMm = MCContextMenuManager
					.create(classLoadingTable.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(classLoadingTable.getManager(), classLoadingTableMm);
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(), classLoadingTable,
					Messages.ClassLoadingPage_CLASS_LOADING_LIST_SELECTION, classLoadingTableMm);
			classLoadingTableMm.add(classLoadingFilter.getShowFilterAction());
			classLoadingTableMm.add(classLoadingFilter.getShowSearchAction());
			classLoadingFilter.loadState(state.getChild(CLASS_LOADING_FILTER));
			DataPageToolkit.addTabItem(tabFolder, classLoadingFilter.getComponent(),
					Messages.ClassLoadingPage_CLASS_LOADING_TAB_TITLE);
			
			classDefineTable = CLASS_DEFINE_LIST.buildWithoutBorder(tabFolder,
					TableSettings.forState(state.getChild(CLASS_DEFINE_TABLE)));
			classDefineTable.getManager().getViewer().addSelectionChangedListener(e -> pageContainer
					.showSelection(ItemCollectionToolkit.build(classDefineTable.getSelection().get())));
			classDefineFilter = FilterComponent.createFilterComponent(classDefineTable, null,
					getDataSource().getItems().apply(JdkFilters.CLASS_DEFINE),
					pageContainer.getSelectionStore()::getSelections, this::onClassDefineFilterChange);
			MCContextMenuManager classDefineTableMm = MCContextMenuManager
					.create(classDefineTable.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(classDefineTable.getManager(), classDefineTableMm);
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(), classDefineTable,
					Messages.ClassLoadingPage_CLASS_DEFINE_LIST_SELECTION, classDefineTableMm);
			classDefineTableMm.add(classDefineFilter.getShowFilterAction());
			classDefineTableMm.add(classDefineFilter.getShowSearchAction());
			classDefineFilter.loadState(state.getChild(CLASS_DEFINE_FILTER));
			DataPageToolkit.addTabItem(tabFolder, classDefineFilter.getComponent(),
					Messages.ClassLoadingPage_CLASS_DEFINE_TAB_TITLE);

			classUnloadingTable = CLASS_UNLOADING_LIST.buildWithoutBorder(tabFolder,
					TableSettings.forState(state.getChild(CLASS_UNLOADING_TABLE)));
			classUnloadingTable.getManager().getViewer().addSelectionChangedListener(e -> pageContainer
					.showSelection(ItemCollectionToolkit.build(classUnloadingTable.getSelection().get())));
			classUnloadingFilter = FilterComponent.createFilterComponent(classUnloadingTable, null,
					getDataSource().getItems().apply(JdkFilters.CLASS_UNLOAD),
					pageContainer.getSelectionStore()::getSelections, this::onClassUnloadFilterChange);
			MCContextMenuManager classUnloadingTableMm = MCContextMenuManager
					.create(classUnloadingTable.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(classUnloadingTable.getManager(), classUnloadingTableMm);
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(), classUnloadingTable,
					Messages.ClassLoadingPage_CLASS_UNLOADING_LIST_SELECTION, classUnloadingTableMm);
			classUnloadingTableMm.add(classUnloadingFilter.getShowFilterAction());
			classUnloadingTableMm.add(classUnloadingFilter.getShowSearchAction());
			classUnloadingFilter.loadState(state.getChild(CLASS_UNLOADING_FILTER));
			DataPageToolkit.addTabItem(tabFolder, classUnloadingFilter.getComponent(),
					Messages.ClassLoadingPage_CLASS_UNLOADING_TAB_TITLE);

			tabFolder.setSelection(tabFolderIndex);
			classloaderFolder.setSelection(tabFolderIndex);

			PersistableSashForm.loadState(sash, state.getChild(SASH));
			flavorSelector = FlavorSelector.itemsWithTimerange(form, TABLE_FILTER, getDataSource().getItems(),
					pageContainer, this::onInputSelected, this::onShow, flavorSelectorState);
			addResultActions(form);

			onHistogramFilterChange(histogramFilter);
			onClassLoadFilterChange(classLoadTableFilter);
			onClassUnloadFilterChange(classUnloadTableFilter);
			onClassDefineFilterChange(classDefineTableFilter);
			onClassLoaderStatisticsFilterChange(classLoaderStatisticsTableFilter);

			classloaderHistogram.getManager().setSelectionState(histogramSelection);
			classLoadingTable.getManager().setSelectionState(classLoadingTableSelection);
			classUnloadingTable.getManager().setSelectionState(classUnloadingTableSelection);
			classDefineTable.getManager().setSelectionState(classDefineTableSelection);
			classLoaderStatisticsTable.getManager().setSelectionState(classLoaderStatisticsTableSelection);
		}

		private void onHistogramFilterChange(IItemFilter filter) {
			classloaderHistogramFilter.filterChangeHelper(filter, classloaderHistogram,
					getDataSource().getItems().apply(ItemFilters.or(JdkFilters.CLASS_LOAD_OR_UNLOAD, JdkFilters.CLASS_DEFINE)));
			if (classLoadingFilter != null) {
				classLoadingFilter.notifyListener();
			}
			if (classUnloadingFilter != null) {
				classUnloadingFilter.notifyListener();
			}
			if (classDefineFilter != null) {
				classDefineFilter.notifyListener();
			}
			if (classLoaderStatisticsFilter != null) {
				classLoaderStatisticsFilter.notifyListener();
			}
			histogramFilter = filter;
		}

		private void onClassLoadFilterChange(IItemFilter filter) {
			classLoadingFilter.filterChangeHelper(filter, classLoadingTable,
					getDataSource().getItems().apply(JdkFilters.CLASS_LOAD));
			classLoadTableFilter = filter;
		}

		private void onClassUnloadFilterChange(IItemFilter filter) {
			classUnloadingFilter.filterChangeHelper(filter, classUnloadingTable,
					getDataSource().getItems().apply(JdkFilters.CLASS_UNLOAD));
			classUnloadTableFilter = filter;
		}
		
		private void onClassDefineFilterChange(IItemFilter filter) {
			classDefineFilter.filterChangeHelper(filter, classDefineTable,
					getDataSource().getItems().apply(JdkFilters.CLASS_DEFINE));
			classDefineTableFilter = filter;
		}
		
		private void onClassLoaderStatisticsFilterChange(IItemFilter filter) {
			classLoaderStatisticsFilter.filterChangeHelper(filter, classLoaderStatisticsTable,
					getDataSource().getItems().apply(JdkFilters.CLASS_LOADER_STATISTICS));
			classLoaderStatisticsTableFilter = filter;
		}
	
		@Override
		public void saveTo(IWritableState state) {
			PersistableSashForm.saveState(sash, state.createChild(SASH));
			classloaderHistogram.getManager().getSettings().saveState(state.createChild(HISTOGRAM));
			classLoadingTable.getManager().getSettings().saveState(state.createChild(CLASS_LOADING_TABLE));
			classUnloadingTable.getManager().getSettings().saveState(state.createChild(CLASS_UNLOADING_TABLE));
			classDefineTable.getManager().getSettings().saveState(state.createChild(CLASS_DEFINE_TABLE));
			classLoaderStatisticsTable.getManager().getSettings().saveState(state.createChild(CLASS_LOADER_STATISTICS_TABLE));
			classloaderHistogramFilter.saveState(state.createChild(HISTOGRAM_FILTER));
			classLoadingFilter.saveState(state.createChild(CLASS_LOADING_FILTER));
			classUnloadingFilter.saveState(state.createChild(CLASS_UNLOADING_FILTER));
			classDefineFilter.saveState(state.createChild(CLASS_DEFINE_FILTER));
			classLoaderStatisticsFilter.saveState(state.createChild(CLASS_LOADER_STATISTICS_FILTER));
			ActionToolkit.saveCheckState(state.createChild(CHART), allChartSeriesActions.stream());

			saveToLocal();
		}

		private void saveToLocal() {
			histogramSelection = classloaderHistogram.getManager().getSelectionState();
			classLoadingTableSelection = classLoadingTable.getManager().getSelectionState();
			classUnloadingTableSelection = classUnloadingTable.getManager().getSelectionState();
			classDefineTableSelection = classDefineTable.getManager().getSelectionState();
			classLoaderStatisticsTableSelection = classLoaderStatisticsTable.getManager().getSelectionState();
			tabFolderIndex = tabFolder.getSelectionIndex();
			flavorSelectorState = flavorSelector.getFlavorSelectorState();
		}

		private void onShow(Boolean show) {
			IRange<IQuantity> range = show ? timeRange : pageContainer.getRecordingRange();
			chart.setVisibleRange(range.getStart(), range.getEnd());
			updateChart();
		}

		private void onInputSelected(IItemCollection items, IRange<IQuantity> timeRange) {
			selectionItems = items;
			this.timeRange = timeRange;
			updateHistogram(getItems());
			updateTables(getItems());
			updateChart();
		}

		private IItemCollection getItems() {
			return selectionItems != null ? selectionItems : getDataSource().getItems();
		}

		private void updateChart() {
			List<IXDataRenderer> rows = new ArrayList<>();

			DataPageToolkit.buildLinesRow(Messages.ClassLoadingPage_ROW_CLASS_LOADING_STATISTICS,
					JdkAttributes.CLASSLOADER_LOADED_COUNT.getDescription(), getDataSource().getItems(), false,
					JdkQueries.CLASS_LOAD_STATISTICS, this::isAttributeEnabled, UnitLookup.NUMBER_UNITY.quantity(0),
					null).ifPresent(rows::add);

			if (classLoadAction.isChecked()) {
				rows.add(DataPageToolkit.buildTimestampHistogram(Messages.ClassLoadingPage_ROW_CLASSES_LOADED,
						Messages.ClassLoadingPage_AGGR_CLASSES_LOADED_BY_CLASSLOADER_DESC,
						getItems().apply(JdkFilters.CLASS_LOAD),
						Aggregators.count(Messages.ClassLoadingPage_AGGR_CLASSES_LOADED,
								Messages.ClassLoadingPage_AGGR_CLASSES_LOADED_DESC, JdkFilters.CLASS_LOAD),
						TypeLabelProvider.getColor(JdkTypeIDs.CLASS_LOAD)));
			}
			if (classUnloadAction.isChecked()) {
				rows.add(DataPageToolkit.buildTimestampHistogram(Messages.ClassLoadingPage_ROW_CLASSES_UNLOADED,
						Messages.ClassLoadingPage_AGGR_CLASSES_UNLOADED_DESC, getItems().apply(JdkFilters.CLASS_UNLOAD),
						Aggregators.count(Messages.ClassLoadingPage_AGGR_CLASSES_UNLOADED,
								Messages.ClassLoadingPage_AGGR_CLASSES_UNLOADED_DESC, JdkFilters.CLASS_UNLOAD),
						TypeLabelProvider.getColor(JdkTypeIDs.CLASS_UNLOAD)));
			}
			classLoadingChart.replaceRenderer(RendererToolkit.uniformRows(rows));
		}

		private boolean isAttributeEnabled(IAttribute<IQuantity> attr) {
			return allChartSeriesActions.stream().filter(a -> attr.getIdentifier().equals(a.getId())).findAny().get()
					.isChecked();
		}

		private void updateHistogram(IItemCollection items) {
			if (classloaderHistogram != null) {
				classloaderHistogram.show(items.apply(JdkFilters.CLASS_LOAD_OR_UNLOAD));
			}
		}

		private void updateTables(IItemCollection selectedItems) {
			if (classLoadingTable != null && classUnloadingTable != null && classDefineTable != null
					&& classLoaderStatisticsTable != null) {
				classLoadingTable.show(selectedItems.apply(JdkQueries.CLASS_LOAD.getFilter()));
				classUnloadingTable.show(selectedItems.apply(JdkQueries.CLASS_UNLOAD.getFilter()));
				classDefineTable.show(selectedItems.apply(JdkQueries.CLASS_DEFINE.getFilter()));
			}
		}

		private void onChartSelection(IRange<IQuantity> range) {
			// FIXME: Make this depend on the legend as well? And maybe on which chart row has been selected?
			IItemCollection itemsInRange = range != null ? getItems().apply(ItemFilters
					.matchRange(RangeMatchPolicy.CENTER_CONTAINED_IN_RIGHT_OPEN, JfrAttributes.LIFETIME, range))
					: getItems();
			updateTables(itemsInRange);
			updateHistogram(itemsInRange);
		}

	}

	private static final String SASH = "sash"; //$NON-NLS-1$
	private static final String HISTOGRAM = "histogram"; //$NON-NLS-1$
	private static final String HISTOGRAM_FILTER = "histogramFilter"; //$NON-NLS-1$
	private static final String CLASS_LOADING_TABLE = "classLoadingTable"; //$NON-NLS-1$
	private static final String CLASS_UNLOADING_TABLE = "classUnloadingTable"; //$NON-NLS-1$
	private static final String CLASS_DEFINE_TABLE = "classDefineTable"; //$NON-NLS-1$
	private static final String CLASS_LOADER_STATISTICS_TABLE = "classLoaderStatisticsTable"; //$NON-NLS-1$
	private static final String CLASS_LOADING_FILTER = "classLoadingFilter"; //$NON-NLS-1$
	private static final String CLASS_UNLOADING_FILTER = "classUnloadingFilter"; //$NON-NLS-1$
	private static final String CLASS_DEFINE_FILTER = "classDefineFilter"; //$NON-NLS-1$
	private static final String CLASS_LOADER_STATISTICS_FILTER = "classLoaderStatisticsFilter"; //$NON-NLS-1$
	private static final String CHART = "chart"; //$NON-NLS-1$

	@Override
	public IPageUI display(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
		return new ClassLoadingUi(parent, toolkit, pageContainer, state);
	}

	private SelectionState histogramSelection;
	private SelectionState classLoadingTableSelection;
	private SelectionState classUnloadingTableSelection;
	private SelectionState classDefineTableSelection;
	private SelectionState classLoaderStatisticsTableSelection;
	private IItemFilter histogramFilter;
	private IItemFilter classLoadTableFilter;
	private IItemFilter classUnloadTableFilter;
	private IItemFilter classDefineTableFilter;
	private IItemFilter classLoaderStatisticsTableFilter;
	private int tabFolderIndex = 0;
	private IRange<IQuantity> timelineRange;
	private FlavorSelectorState flavorSelectorState;

	public ClassLoadingPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
		super(dpd, items, editor);
		timelineRange = editor.getRecordingRange();
	}

	@Override
	public IItemFilter getDefaultSelectionFilter() {
		return ItemFilters.or(TABLE_FILTER, JdkFilters.CLASS_LOAD_STATISTICS);
	}

}
