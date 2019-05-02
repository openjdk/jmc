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

import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.IO_ADDRESS;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.IO_PORT;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.IAccessorFactory;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.QuantitiesToolkit;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.ColorToolkit;
import org.openjdk.jmc.common.util.StateToolkit;
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
import org.openjdk.jmc.flightrecorder.ui.ItemCollectionToolkit;
import org.openjdk.jmc.flightrecorder.ui.StreamModel;
import org.openjdk.jmc.flightrecorder.ui.common.AbstractDataPage;
import org.openjdk.jmc.flightrecorder.ui.common.CompositeKeyAccessorFactory;
import org.openjdk.jmc.flightrecorder.ui.common.DataPageToolkit;
import org.openjdk.jmc.flightrecorder.ui.common.FilterComponent;
import org.openjdk.jmc.flightrecorder.ui.common.FlavorSelector;
import org.openjdk.jmc.flightrecorder.ui.common.FlavorSelector.FlavorSelectorState;
import org.openjdk.jmc.flightrecorder.ui.common.ImageConstants;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram.HistogramSelection;
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
import org.openjdk.jmc.ui.column.TableSettings.ColumnSettings;
import org.openjdk.jmc.ui.handlers.ActionToolkit;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.ChartCanvas;
import org.openjdk.jmc.ui.misc.PersistableSashForm;

public class SocketIOPage extends AbstractDataPage {
	public static class SocketIOPageFactory implements IDataPageFactory {
		@Override
		public String getName(IState state) {
			return Messages.SocketIOPage_PAGE_NAME;
		}

		@Override
		public ImageDescriptor getImageDescriptor(IState state) {
			return FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.PAGE_IO);
		}

		@Override
		public String[] getTopics(IState state) {
			return new String[] {JfrRuleTopics.SOCKET_IO_TOPIC};
		}

		@Override
		public IDisplayablePage createPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
			return new SocketIOPage(dpd, items, editor);
		}

	}

	private static final Color WRITE_COLOR = TypeLabelProvider.getColor(JdkTypeIDs.SOCKET_WRITE);
	private static final Color READ_COLOR = TypeLabelProvider.getColor(JdkTypeIDs.SOCKET_READ);
	private static final Color WRITE_ALPHA_COLOR = ColorToolkit.withAlpha(WRITE_COLOR, 80);
	private static final Color READ_ALPHA_COLOR = ColorToolkit.withAlpha(READ_COLOR, 80);
	private static final IItemFilter TABLE_ITEMS = ItemFilters.type(JdkTypeIDs.SOCKET_READ, JdkTypeIDs.SOCKET_WRITE);
	private static final String TOTAL_TIME = "totalTime"; //$NON-NLS-1$
	private static final String MAX_TIME = "maxTime"; //$NON-NLS-1$
	private static final String AVG_TIME = "avgTime"; //$NON-NLS-1$
	private static final String STDDEV_TIME = "stddevTime"; //$NON-NLS-1$
	private static final String READ_COUNT = "readCount"; //$NON-NLS-1$
	private static final String WRITE_COUNT = "writeCount"; //$NON-NLS-1$
	private static final String READ_SIZE = "readSize"; //$NON-NLS-1$
	private static final String WRITE_SIZE = "writeSize"; //$NON-NLS-1$
	private static final String READ_EOS = "endOfStream"; //$NON-NLS-1$
	private static final String IO_TIMEOUT = "timeout"; //$NON-NLS-1$
	private static final IAccessorFactory<IDisplayable> HOST_AND_PORT_AF = CompositeKeyAccessorFactory.displayable(
			" : ", JdkAttributes.IO_ADDRESS, //$NON-NLS-1$
			JdkAttributes.IO_PORT);

	private static final ItemHistogramBuilder HISTOGRAM = new ItemHistogramBuilder();
	private static final ItemListBuilder LIST = new ItemListBuilder();

	static {
		HISTOGRAM.addCountColumn();
		HISTOGRAM.addColumn(TOTAL_TIME, JdkAggregators.TOTAL_IO_TIME);
		HISTOGRAM.addColumn(MAX_TIME, JdkAggregators.MAX_IO_TIME);
		HISTOGRAM.addColumn(AVG_TIME, JdkAggregators.AVG_IO_TIME);
		HISTOGRAM.addColumn(STDDEV_TIME, JdkAggregators.STDDEV_IO_TIME);
		HISTOGRAM.addColumn(READ_COUNT, JdkAggregators.SOCKET_READ_COUNT);
		HISTOGRAM.addColumn(WRITE_COUNT, JdkAggregators.SOCKET_WRITE_COUNT);
		HISTOGRAM.addColumn(READ_SIZE, JdkAggregators.SOCKET_READ_SIZE);
		HISTOGRAM.addColumn(WRITE_SIZE, JdkAggregators.SOCKET_WRITE_SIZE);
		// FIXME: Would we like to include # of hosts, # of ports and host name in the new histograms?

		LIST.addColumn(JdkAttributes.IO_ADDRESS);
		LIST.addColumn(JdkAttributes.IO_HOST);
		LIST.addColumn(JdkAttributes.IO_PORT);
		LIST.addColumn(JfrAttributes.START_TIME);
		LIST.addColumn(JfrAttributes.END_TIME);
		LIST.addColumn(JfrAttributes.DURATION);
		LIST.addColumn(JdkAttributes.IO_SOCKET_BYTES_READ);
		LIST.addColumn(JdkAttributes.IO_SOCKET_BYTES_WRITTEN);
		LIST.addColumn(JfrAttributes.EVENT_THREAD);
		LIST.addColumn(JdkAttributes.IO_SOCKET_READ_EOS);
		LIST.addColumn(JdkAttributes.IO_TIMEOUT);
	}

	private enum HistogramType {
		HOST, PORT, HOST_AND_PORT
	}

	private class IOPageUi implements IPageUI {
		private static final String PRIMARY_FILTER = "primaryFilter"; //$NON-NLS-1$
		private static final String SECONDARY_FILTER = "secondaryFilter"; //$NON-NLS-1$
		private static final String EVENT_FILTER = "eventFilter"; //$NON-NLS-1$
		private static final String SASH_ELEMENT = "sash"; //$NON-NLS-1$
		private static final String LIST_ELEMENT = "eventList"; //$NON-NLS-1$
		private static final String SOCKETIO_TABLE_ELEMENT = "socketTable"; //$NON-NLS-1$
		private static final String SECONDARY_SOCKETIO_TABLE_ELEMENT = "secondarySocketTable"; //$NON-NLS-1$
		private static final String HISTGRAM_TYPE = "histogramType"; //$NON-NLS-1$

		private final ChartCanvas timelineCanvas;
		private final ChartCanvas durationCanvas;
		private final ChartCanvas sizeCanvas;
		private final ItemList itemList;

		private final SashForm sash;
		private final IPageContainer pageContainer;
		private final Composite histogramParent;
		private ItemHistogram primaryHistogram;
		private Supplier<TableSettings> secondaryHistogramSettings;
		private Consumer<IItemCollection> itemConsumerRoot;
		private HistogramType histogramType;
		private ItemHistogram secondaryHistogram;
		private FilterComponent primaryFilter;
		private FilterComponent secondaryFilter;
		private FilterComponent eventFilter;
		private IRange<IQuantity> timeRange;
		private IItemCollection selectionItems;
		private XYChart timelineChart;
		private XYChart durationChart;
		private XYChart sizeChart;
		private CTabFolder tabFolder;
		private FlavorSelector flavorSelector;

		IOPageUi(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
			this.pageContainer = pageContainer;
			Form form = DataPageToolkit.createForm(parent, toolkit, getName(), getIcon());
			sash = new SashForm(form.getBody(), SWT.VERTICAL);
			toolkit.adapt(sash);
			histogramParent = toolkit.createComposite(sash);
			histogramParent.setLayout(new FillLayout(SWT.VERTICAL));
			histogramType = StateToolkit.readEnum(state, HISTGRAM_TYPE, HistogramType.HOST, HistogramType.class);
			buildHistograms(TableSettings.forState(state.getChild(SOCKETIO_TABLE_ELEMENT)),
					TableSettings.forState(state.getChild(SECONDARY_SOCKETIO_TABLE_ELEMENT)));

			tabFolder = new CTabFolder(sash, SWT.NONE);
			toolkit.adapt(tabFolder);
			CTabItem t1 = new CTabItem(tabFolder, SWT.NONE);
			t1.setToolTipText(Messages.IO_PAGE_TIMELINE_DESCRIPTION);
			timelineCanvas = new ChartCanvas(tabFolder);
			t1.setText(Messages.PAGES_TIMELINE);
			t1.setControl(timelineCanvas);
			DataPageToolkit.createChartTimestampTooltip(timelineCanvas);
			timelineChart = new XYChart(pageContainer.getRecordingRange(), RendererToolkit.empty(), 180);
			timelineChart.setVisibleRange(timelineRange.getStart(), timelineRange.getEnd());
			timelineChart.addVisibleRangeListener(r -> timelineRange = r);
			IItemCollection socketItems = getDataSource().getItems().apply(JdkFilters.SOCKET_READ_OR_WRITE);
			// FIXME: X-auto-range should be done properly
			IQuantity max = socketItems.getAggregate(JdkAggregators.LONGEST_EVENT);
			// FIXME: Workaround to make max value included
			max = max == null ? UnitLookup.MILLISECOND.quantity(20) : max.add(UnitLookup.MILLISECOND.quantity(20));
			durationChart = new XYChart(UnitLookup.MILLISECOND.quantity(0), max, RendererToolkit.empty(), 180);
			durationChart.setVisibleRange(durationRange.getStart(), durationRange.getEnd());
			durationChart.addVisibleRangeListener(r -> durationRange = r);
			buildChart();

			CTabItem t2 = new CTabItem(tabFolder, SWT.NONE);
			t2.setToolTipText(Messages.IO_PAGE_DURATIONS_DESCRIPTION);
			durationCanvas = new ChartCanvas(tabFolder);
			t2.setText(Messages.PAGES_DURATIONS);
			t2.setControl(durationCanvas);
			DataPageToolkit.createChartTooltip(durationCanvas);
			DataPageToolkit.setChart(durationCanvas, durationChart, JfrAttributes.DURATION,
					pageContainer::showSelection);
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(), durationChart,
					JfrAttributes.DURATION, Messages.SocketIOPage_DURATION_SELECTION, durationCanvas.getContextMenu());

			IQuantity sizeMax = QuantitiesToolkit.maxPresent(socketItems.getAggregate(JdkAggregators.SOCKET_READ_LARGEST),
					socketItems.getAggregate(JdkAggregators.SOCKET_WRITE_LARGEST));
			// FIXME: Workaround to make max value included
			sizeMax = sizeMax == null ? UnitLookup.BYTE.quantity(64): sizeMax.add(UnitLookup.BYTE.quantity(64));
			sizeChart = new XYChart(UnitLookup.BYTE.quantity(0), sizeMax, RendererToolkit.empty(), 180);
			sizeChart.setVisibleRange(sizeRange.getStart(), sizeMax);
			sizeChart.addVisibleRangeListener(range -> sizeRange = range);

			CTabItem t3 = new CTabItem(tabFolder, SWT.NONE);
			t3.setToolTipText(Messages.IO_PAGE_SIZE_DESCRIPTION);
			sizeCanvas = new ChartCanvas(tabFolder);
			t3.setText(Messages.PAGES_SIZE);
			t3.setControl(sizeCanvas);
			DataPageToolkit.createChartTooltip(sizeCanvas);
			DataPageToolkit.setChart(sizeCanvas, sizeChart, JdkAttributes.IO_SIZE,
					pageContainer::showSelection);
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(), sizeChart,
					JdkAttributes.IO_SIZE, Messages.SocketIOPage_SIZE_SELECTION, sizeCanvas.getContextMenu());

			CTabItem t4 = new CTabItem(tabFolder, SWT.NONE);
			t4.setToolTipText(Messages.IO_PAGE_EVENT_LOG_DESCRIPTION);
			itemList = LIST.buildWithoutBorder(tabFolder, getTableSettings(state.getChild(LIST_ELEMENT)));
			MCContextMenuManager itemListMm = MCContextMenuManager
					.create(itemList.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(itemList.getManager(), itemListMm);
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(), itemList,
					Messages.SocketIOPage_LOG_SELECTION, itemListMm);
			itemList.getManager().getViewer().addSelectionChangedListener(
					e -> pageContainer.showSelection(ItemCollectionToolkit.build(itemList.getSelection().get())));
			t4.setText(Messages.PAGES_EVENT_LOG);
			eventFilter = FilterComponent.createFilterComponent(itemList, itemListFilter,
					getDataSource().getItems().apply(TABLE_ITEMS), pageContainer.getSelectionStore()::getSelections,
					this::onEventFilterChange);
			itemListMm.add(eventFilter.getShowFilterAction());
			itemListMm.add(eventFilter.getShowSearchAction());
			t4.setControl(eventFilter.getComponent());
			eventFilter.loadState(state.getChild(EVENT_FILTER));
			onEventFilterChange(itemListFilter);
			itemList.getManager().setSelectionState(itemListSelection);

			tabFolder.setSelection(tabFolderIndex);

			PersistableSashForm.loadState(sash, state.getChild(SASH_ELEMENT));

			flavorSelector = FlavorSelector.itemsWithTimerange(form, TABLE_ITEMS, getDataSource().getItems(),
					pageContainer, this::onInputSelected, this::onUseRange, flavorSelectorState);

			form.getToolBarManager()
					.appendToGroup(DataPageToolkit.FORM_TOOLBAR_PAGE_SETUP, buildHistogramTypeAction(HistogramType.HOST,
							Messages.SocketIOPage_BY_HOST_ACTION,
							FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.ICON_IO_BY_HOST)));
			form.getToolBarManager()
					.appendToGroup(DataPageToolkit.FORM_TOOLBAR_PAGE_SETUP, buildHistogramTypeAction(HistogramType.PORT,
							Messages.SocketIOPage_BY_PORT_ACTION,
							FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.ICON_IO_BY_PORT)));
			form.getToolBarManager().appendToGroup(DataPageToolkit.FORM_TOOLBAR_PAGE_SETUP, buildHistogramTypeAction(
					HistogramType.HOST_AND_PORT, Messages.SocketIOPage_BY_HOST_AND_PORT_ACTION,
					FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.ICON_IO_BY_HOST_AND_PORT)));

			addResultActions(form);
		}

		// FIXME: Break out this to a "ConfigurableHistogramUi or something? This is copy-pasted from ExceptionsPage
		private IAction buildHistogramTypeAction(HistogramType histogramType, String text, ImageDescriptor icon) {
			IAction a = ActionToolkit.radioAction(() -> setHistogramType(histogramType), text, icon);
			a.setChecked(histogramType == this.histogramType);
			return a;
		}

		private void setHistogramType(HistogramType histogramType) {
			if (histogramType != this.histogramType) {
				primaryTableSelection.put(this.histogramType, primaryHistogram.getManager().getSelectionState());
				if (secondaryHistogram != null) {
					secondaryTableSelection.put(this.histogramType,
							secondaryHistogram.getManager().getSelectionState());
				}
				this.histogramType = histogramType;
				TableSettings primarySettings = primaryHistogram.getManager().getSettings();
				TableSettings secondarySettings = secondaryHistogramSettings.get();
				for (Control c : histogramParent.getChildren()) {
					c.dispose();
				}
				buildHistograms(primarySettings, secondarySettings);
				refreshPageItems();
			}
		}

		private void buildHistograms(TableSettings primarySettings, TableSettings secondarySettings) {
			if (histogramType == HistogramType.HOST_AND_PORT) {
				primaryHistogram = HISTOGRAM.buildWithoutBorder(histogramParent, Messages.SocketIOPage_HOST_AND_PORT,
						UnitLookup.UNKNOWN, HOST_AND_PORT_AF, primarySettings);
				primaryFilter = FilterComponent.createFilterComponent(primaryHistogram,
						primaryTableFilter.get(histogramType), getDataSource().getItems().apply(TABLE_ITEMS),
						pageContainer.getSelectionStore()::getSelections, this::onPrimaryFilterChange);
				secondaryHistogram = null;
				secondaryHistogramSettings = () -> secondarySettings;
				secondaryFilter = null;
				onPrimaryFilterChange(primaryTableFilter.get(histogramType));
				primaryHistogram.getManager().setSelectionState(primaryTableSelection.get(histogramType));
				itemConsumerRoot = ItemHistogramWithInput.chain(primaryHistogram, this::updateChartAndListDetails);
			} else {
				SashForm s2 = new SashForm(histogramParent, SWT.VERTICAL);
				IAttribute<?> masterAttr = histogramType == HistogramType.HOST ? IO_ADDRESS : IO_PORT;
				IAttribute<?> slaveAttr = histogramType == HistogramType.PORT ? IO_ADDRESS : IO_PORT;
				primaryHistogram = HISTOGRAM.buildWithoutBorder(s2, masterAttr, primarySettings);
				primaryFilter = FilterComponent.createFilterComponent(primaryHistogram,
						primaryTableFilter.get(histogramType), getDataSource().getItems().apply(TABLE_ITEMS),
						pageContainer.getSelectionStore()::getSelections, this::onPrimaryFilterChange);

				secondaryHistogram = HISTOGRAM.buildWithoutBorder(s2, slaveAttr, secondarySettings);
				secondaryFilter = FilterComponent.createFilterComponent(secondaryHistogram,
						secondaryTableFilter.get(histogramType), getDataSource().getItems().apply(TABLE_ITEMS),
						pageContainer.getSelectionStore()::getSelections, this::onSecondaryFilterChange);
				secondaryHistogramSettings = secondaryHistogram.getManager()::getSettings;
				onPrimaryFilterChange(primaryTableFilter.get(histogramType));
				onSecondaryFilterChange(secondaryTableFilter.get(histogramType));
				primaryHistogram.getManager().setSelectionState(primaryTableSelection.get(histogramType));
				secondaryHistogram.getManager().setSelectionState(secondaryTableSelection.get(histogramType));
				itemConsumerRoot = ItemHistogramWithInput.chain(primaryHistogram, this::updateChartAndListDetails,
						secondaryHistogram);
				addContextMenu(secondaryHistogram, secondaryFilter.getShowFilterAction(),
						secondaryFilter.getShowSearchAction());
				secondaryFilter.loadState(getState().getChild(SECONDARY_FILTER));
			}
			addContextMenu(primaryHistogram, primaryFilter.getShowFilterAction(), primaryFilter.getShowSearchAction());
			primaryFilter.loadState(getState().getChild(PRIMARY_FILTER));
			histogramParent.layout();
		}

		private void addContextMenu(ItemHistogram h, IAction ... actions) {
			MCContextMenuManager mm = MCContextMenuManager.create(h.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(h.getManager(), mm);
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(), h,
					Messages.SocketIOPage_HISTOGRAM_SELECTION, mm);
			for (IAction action : actions) {
				mm.add(action);
			}
		}

		private void onPrimaryFilterChange(IItemFilter filter) {
			primaryFilter.filterChangeHelper(filter, primaryHistogram, getDataSource().getItems().apply(TABLE_ITEMS));
			if (secondaryFilter != null) {
				secondaryFilter.notifyListener();
			}
			primaryTableFilter.put(histogramType, filter);
		}

		private void onSecondaryFilterChange(IItemFilter filter) {
			secondaryFilter.filterChangeHelper(filter, secondaryHistogram,
					getDataSource().getItems().apply(TABLE_ITEMS));
			secondaryTableFilter.put(histogramType, filter);
		}

		private void onEventFilterChange(IItemFilter filter) {
			eventFilter.filterChangeHelper(filter, itemList, getDataSource().getItems().apply(TABLE_ITEMS));
			itemListFilter = filter;
		}

		@Override
		public void saveTo(IWritableState writableState) {
			StateToolkit.writeEnum(writableState, HISTGRAM_TYPE, histogramType);
			PersistableSashForm.saveState(sash, writableState.createChild(SASH_ELEMENT));
			primaryHistogram.getManager().getSettings().saveState(writableState.createChild(SOCKETIO_TABLE_ELEMENT));
			primaryFilter.saveState(writableState.createChild(PRIMARY_FILTER));
			Optional.ofNullable(secondaryHistogramSettings.get()).ifPresent(
					settings -> settings.saveState(writableState.createChild(SECONDARY_SOCKETIO_TABLE_ELEMENT)));
			if (secondaryFilter != null) {
				secondaryFilter.saveState(writableState.createChild(SECONDARY_FILTER));
			}
			itemList.getManager().getSettings().saveState(writableState.createChild(LIST_ELEMENT));
			eventFilter.saveState(writableState.createChild(EVENT_FILTER));

			saveToLocal();
		}

		private void saveToLocal() {
			primaryTableSelection.put(histogramType, primaryHistogram.getManager().getSelectionState());
			if (secondaryHistogram != null) {
				secondaryTableSelection.put(histogramType, secondaryHistogram.getManager().getSelectionState());
			}
			itemListSelection = itemList.getManager().getSelectionState();
			tabFolderIndex = tabFolder.getSelectionIndex();
			flavorSelectorState = flavorSelector.getFlavorSelectorState();
		}

		private void onUseRange(Boolean show) {
			IRange<IQuantity> range = show ? timeRange : pageContainer.getRecordingRange();
			timelineChart.setVisibleRange(range.getStart(), range.getEnd());
			buildChart();
		}

		private void buildChart() {
			DataPageToolkit.setChart(timelineCanvas, timelineChart,
					selection -> pageContainer.showSelection(selection));
			SelectionStoreActionToolkit.addSelectionStoreRangeActions(pageContainer.getSelectionStore(), timelineChart,
					JfrAttributes.LIFETIME, Messages.SocketIOPage_TIMELINE_SELECTION, timelineCanvas.getContextMenu());
		}

		private void onInputSelected(IItemCollection items, IRange<IQuantity> timeRange) {
			this.selectionItems = items;
			this.timeRange = timeRange;
			refreshPageItems();
		}

		private void refreshPageItems() {
			IItemCollection items = selectionItems != null ? selectionItems : getDataSource().getItems();
			itemConsumerRoot.accept(items.apply(JdkFilters.SOCKET_READ_OR_WRITE));
		}

		private void updateChartAndListDetails(IItemCollection selectedItems) {
			String hostCount = hostPortCount();

			List<IXDataRenderer> timelineRows = new ArrayList<>();
			List<IXDataRenderer> durationRows = new ArrayList<>();
			List<IXDataRenderer> sizeRows = new ArrayList<>();
			IItemCollection readItems = selectedItems.apply(JdkFilters.SOCKET_READ);
			if (readItems.hasItems()) {
				timelineRows.add(DataPageToolkit.buildSizeRow(Messages.SocketIOPage_ROW_SOCKET_READ + hostCount,
						JdkAggregators.SOCKET_READ_SIZE.getDescription(), readItems, JdkAggregators.SOCKET_READ_SIZE,
						READ_COLOR, SocketIOPage::getColor));
				durationRows
						.add(DataPageToolkit.buildDurationHistogram(Messages.SocketIOPage_ROW_SOCKET_READ + hostCount,
								JdkAggregators.SOCKET_READ_COUNT.getDescription(), readItems,
								JdkAggregators.SOCKET_READ_COUNT, READ_COLOR));
				sizeRows.add(DataPageToolkit.buildSizeHistogram(Messages.SocketIOPage_ROW_SOCKET_READ + hostCount,
						JdkAggregators.SOCKET_READ_COUNT.getDescription(), readItems,
						JdkAggregators.SOCKET_READ_COUNT, READ_COLOR, JdkAttributes.IO_SOCKET_BYTES_READ));
			}
			IItemCollection writeItems = selectedItems.apply(JdkFilters.SOCKET_WRITE);
			if (writeItems.hasItems()) {
				timelineRows.add(DataPageToolkit.buildSizeRow(Messages.SocketIOPage_ROW_SOCKET_WRITE + hostCount,
						JdkAggregators.SOCKET_WRITE_SIZE.getDescription(), writeItems, JdkAggregators.SOCKET_WRITE_SIZE,
						WRITE_COLOR, SocketIOPage::getColor));
				durationRows
						.add(DataPageToolkit.buildDurationHistogram(Messages.SocketIOPage_ROW_SOCKET_WRITE + hostCount,
								JdkAggregators.SOCKET_WRITE_COUNT.getDescription(), writeItems,
								JdkAggregators.SOCKET_WRITE_COUNT, WRITE_COLOR));
				sizeRows.add(DataPageToolkit.buildSizeHistogram(Messages.SocketIOPage_ROW_SOCKET_WRITE + hostCount,
						JdkAggregators.SOCKET_WRITE_COUNT.getDescription(), writeItems,
						JdkAggregators.SOCKET_WRITE_COUNT, WRITE_COLOR, JdkAttributes.IO_SOCKET_BYTES_WRITTEN));
			}
			if (timelineCanvas != null) {
				timelineCanvas.replaceRenderer(RendererToolkit.uniformRows(timelineRows));
				durationCanvas.replaceRenderer(RendererToolkit.uniformRows(durationRows));
				sizeCanvas.replaceRenderer(RendererToolkit.uniformRows(sizeRows));

				itemList.show(selectedItems);
				pageContainer.showSelection(selectedItems);
			}
		}

		public String hostPortCount() {
			HistogramSelection hostSelection = histogramType == HistogramType.HOST ? primaryHistogram.getSelection()
					: histogramType == HistogramType.PORT ? secondaryHistogram.getSelection() : null;
			HistogramSelection portSelection = histogramType == HistogramType.PORT ? primaryHistogram.getSelection()
					: histogramType == HistogramType.HOST ? secondaryHistogram.getSelection() : null;
			HistogramSelection hostPortSelection = histogramType == HistogramType.HOST_AND_PORT
					? primaryHistogram.getSelection() : null;

			return hostPortCount(hostSelection != null ? hostSelection.getRowCount() : 0,
					portSelection != null ? portSelection.getRowCount() : 0,
					hostPortSelection != null ? hostPortSelection.getRowCount() : 0);
		}

		public String hostPortCount(int hostCount, int portCount, int hostPortCount) {
			switch (hostPortCount) {
			case 0:
				switch (hostCount) {
				case 0:
					switch (portCount) {
					case 0:
						return ""; //$NON-NLS-1$
					case 1:
						return " (" + Messages.SocketIOPage_SELECTED_PORT + ")"; //$NON-NLS-1$ //$NON-NLS-2$
					default:
						return " (" + NLS.bind(Messages.SocketIOPage_SELECTED_PORTS, portCount) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
					}
				case 1:
					switch (portCount) {
					case 0:
						return " (" + Messages.SocketIOPage_SELECTED_HOST + ")"; //$NON-NLS-1$ //$NON-NLS-2$
					case 1:
						return " (" + Messages.SocketIOPage_SELECTED_HOST_AND_PORT + ")"; //$NON-NLS-1$ //$NON-NLS-2$
					default:
						return " (" + NLS.bind(Messages.SocketIOPage_SELECTED_HOST_AND_PORTS, portCount) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
					}
				default:
					switch (portCount) {
					case 0:
						return " (" + NLS.bind(Messages.SocketIOPage_SELECTED_HOSTS, hostCount) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
					case 1:
						return " (" + NLS.bind(Messages.SocketIOPage_SELECTED_HOSTS_AND_PORT, hostCount) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
					default:
						return " (" + NLS.bind(Messages.SocketIOPage_SELECTED_HOSTS_AND_PORTS, hostCount, portCount) //$NON-NLS-1$
								+ ")"; //$NON-NLS-1$
					}
				}
			default:
				return " (" + NLS.bind(Messages.SocketIOPage_SELECTED_HOSTS_PORTS, hostPortCount) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	private static TableSettings getTableSettings(IState state) {
		if (state == null) {
			return new TableSettings(TOTAL_TIME, Arrays.asList(
					new ColumnSettings(ItemHistogram.KEY_COL_ID, false, 500, null),
					new ColumnSettings(TOTAL_TIME, true, 120, false), new ColumnSettings(MAX_TIME, false, 120, false),
					new ColumnSettings(AVG_TIME, false, 120, false), new ColumnSettings(STDDEV_TIME, false, 120, false),
					new ColumnSettings(READ_COUNT, false, 120, false),
					new ColumnSettings(WRITE_COUNT, false, 120, false),
					new ColumnSettings(READ_SIZE, false, 120, false), new ColumnSettings(WRITE_SIZE, false, 120, false),
					new ColumnSettings(READ_EOS, false, 80, false), new ColumnSettings(IO_TIMEOUT, false, 50, false)));
		} else {
			return new TableSettings(state);
		}
	}

	@Override
	public IPageUI display(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
		return new IOPageUi(parent, toolkit, pageContainer, state);
	}

	private Map<HistogramType, SelectionState> primaryTableSelection;
	private Map<HistogramType, SelectionState> secondaryTableSelection;
	private SelectionState itemListSelection;
	private Map<HistogramType, IItemFilter> primaryTableFilter;
	private Map<HistogramType, IItemFilter> secondaryTableFilter;
	private IItemFilter itemListFilter;
	private IRange<IQuantity> timelineRange;
	private IRange<IQuantity> durationRange;
	private IRange<IQuantity> sizeRange;
	private int tabFolderIndex = 0;
	public FlavorSelectorState flavorSelectorState;

	public SocketIOPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
		super(dpd, items, editor);
		primaryTableSelection = new HashMap<>();
		secondaryTableSelection = new HashMap<>();
		primaryTableFilter = new HashMap<>();
		secondaryTableFilter = new HashMap<>();
		timelineRange = editor.getRecordingRange();
		durationRange = editor.getRecordingRange();
		sizeRange = DataPageToolkit.buildSizeRange(items.getItems(), true);
	}

	@Override
	public IItemFilter getDefaultSelectionFilter() {
		return TABLE_ITEMS;
	}

	private static Color getColor(IItem item) {
		return JdkTypeIDs.SOCKET_READ.equals(item.getType().getIdentifier()) ? READ_ALPHA_COLOR : WRITE_ALPHA_COLOR;
	}

}
