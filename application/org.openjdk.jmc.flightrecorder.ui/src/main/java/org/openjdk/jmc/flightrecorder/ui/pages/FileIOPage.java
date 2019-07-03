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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.ColorToolkit;
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
import org.openjdk.jmc.flightrecorder.ui.common.DataPageToolkit;
import org.openjdk.jmc.flightrecorder.ui.common.DurationPercentileTable;
import org.openjdk.jmc.flightrecorder.ui.common.DurationPercentileTable.DurationPercentileTableBuilder;
import org.openjdk.jmc.flightrecorder.ui.common.FilterComponent;
import org.openjdk.jmc.flightrecorder.ui.common.FlavorSelector;
import org.openjdk.jmc.flightrecorder.ui.common.FlavorSelector.FlavorSelectorState;
import org.openjdk.jmc.flightrecorder.ui.common.ImageConstants;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram.HistogramSelection;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram.ItemHistogramBuilder;
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
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.layout.SimpleLayout;
import org.openjdk.jmc.ui.layout.SimpleLayoutData;
import org.openjdk.jmc.ui.misc.ChartCanvas;
import org.openjdk.jmc.ui.misc.PersistableSashForm;

public class FileIOPage extends AbstractDataPage {
	public static class FileIOPageFactory implements IDataPageFactory {

		@Override
		public String getName(IState state) {
			return Messages.FileIOPage_PAGE_NAME;
		}

		@Override
		public ImageDescriptor getImageDescriptor(IState state) {
			return FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.PAGE_IO);
		}

		@Override
		public String[] getTopics(IState state) {
			return new String[] {JfrRuleTopics.FILE_IO_TOPIC};
		}

		@Override
		public IDisplayablePage createPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
			return new FileIOPage(dpd, items, editor);
		}

	}

	private static final Color WRITE_COLOR = TypeLabelProvider.getColor(JdkTypeIDs.FILE_WRITE);
	private static final Color READ_COLOR = TypeLabelProvider.getColor(JdkTypeIDs.FILE_READ);
	private static final Color WRITE_ALPHA_COLOR = ColorToolkit.withAlpha(WRITE_COLOR, 80);
	private static final Color READ_ALPHA_COLOR = ColorToolkit.withAlpha(READ_COLOR, 80);
	private static final IItemFilter TABLE_ITEMS = ItemFilters.type(JdkTypeIDs.FILE_READ, JdkTypeIDs.FILE_WRITE);
	private static final String TOTAL_TIME = "totalTime"; //$NON-NLS-1$
	private static final String MAX_TIME = "maxTime"; //$NON-NLS-1$
	private static final String AVG_TIME = "avgTime"; //$NON-NLS-1$
	private static final String STDDEV_TIME = "stddevTime"; //$NON-NLS-1$
	private static final String READ_COUNT = "readCount"; //$NON-NLS-1$
	private static final String WRITE_COUNT = "writeCount"; //$NON-NLS-1$
	private static final String READ_SIZE = "readSize"; //$NON-NLS-1$
	private static final String WRITE_SIZE = "writeSize"; //$NON-NLS-1$
	private static final String READ_EOF = "endOfFile"; //$NON-NLS-1$
	private static final String PERCENTILE_READ_TIME = "percentileReadTime"; //$NON-NLS-1$
	private static final String PERCENTILE_READ_COUNT = "percentileReadCount"; //$NON-NLS-1$
	private static final String PERCENTILE_WRITE_TIME = "percentileWriteTime"; //$NON-NLS-1$
	private static final String PERCENTILE_WRITE_COUNT = "percentileWriteCount"; //$NON-NLS-1$

	private static final ItemHistogramBuilder HISTOGRAM = new ItemHistogramBuilder();
	private static final ItemListBuilder LIST = new ItemListBuilder();
	private static final DurationPercentileTableBuilder PERCENTILES = new DurationPercentileTableBuilder();

	static {
		HISTOGRAM.addCountColumn();
		HISTOGRAM.addColumn(TOTAL_TIME, JdkAggregators.TOTAL_IO_TIME);
		HISTOGRAM.addColumn(MAX_TIME, JdkAggregators.MAX_IO_TIME);
		HISTOGRAM.addColumn(AVG_TIME, JdkAggregators.AVG_IO_TIME);
		HISTOGRAM.addColumn(STDDEV_TIME, JdkAggregators.STDDEV_IO_TIME);
		HISTOGRAM.addColumn(READ_COUNT, JdkAggregators.FILE_READ_COUNT);
		HISTOGRAM.addColumn(WRITE_COUNT, JdkAggregators.FILE_WRITE_COUNT);
		HISTOGRAM.addColumn(READ_SIZE, JdkAggregators.FILE_READ_SIZE);
		HISTOGRAM.addColumn(WRITE_SIZE, JdkAggregators.FILE_WRITE_SIZE);
		LIST.addColumn(JdkAttributes.IO_PATH);
		LIST.addColumn(JfrAttributes.START_TIME);
		LIST.addColumn(JfrAttributes.END_TIME);
		LIST.addColumn(JfrAttributes.DURATION);
		LIST.addColumn(JdkAttributes.IO_FILE_BYTES_READ);
		LIST.addColumn(JdkAttributes.IO_FILE_BYTES_WRITTEN);
		LIST.addColumn(JfrAttributes.EVENT_THREAD);
		LIST.addColumn(JdkAttributes.IO_FILE_READ_EOF);

		PERCENTILES.addSeries(PERCENTILE_READ_TIME, Messages.FileIOPage_ROW_FILE_READ,
				PERCENTILE_READ_COUNT, JdkAggregators.FILE_READ_COUNT.getName(), JdkTypeIDs.FILE_READ);
		PERCENTILES.addSeries(PERCENTILE_WRITE_TIME, Messages.FileIOPage_ROW_FILE_WRITE,
				PERCENTILE_WRITE_COUNT, JdkAggregators.FILE_WRITE_COUNT.getName(), JdkTypeIDs.FILE_WRITE);
	}

	private class IOPageUi implements IPageUI {
		private static final String FILE_IO_TABLE = "fileIoTable"; //$NON-NLS-1$
		private static final String FILE_IO_LIST = "fileIoList"; //$NON-NLS-1$
		private static final String SASH_ELEMENT = "sash"; //$NON-NLS-1$
		private static final String LIST_ELEMENT = "eventList"; //$NON-NLS-1$
		private static final String TABLE_ELEMENT = "table"; //$NON-NLS-1$
		private static final String PERCENTILE_TABLE_ELEMENT = "percentileTable"; //$NON-NLS-1$

		private final ChartCanvas timelineCanvas;
		private final ChartCanvas durationCanvas;
		private final ChartCanvas sizeCanvas;
		private XYChart timelineChart;
		private IRange<IQuantity> timeRange;
		private IItemCollection selectionItems;
		private final ItemList itemList;
		private final ItemHistogram table;
		private final SashForm sash;
		private final IPageContainer pageContainer;
		private FilterComponent tableFilter;
		private FilterComponent itemListFilter;
		private FlavorSelector flavorSelector;
		private DurationPercentileTable percentileTable;
		private Composite durationParent;

		IOPageUi(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
			this.pageContainer = pageContainer;
			Form form = DataPageToolkit.createForm(parent, toolkit, getName(), getIcon());
			sash = new SashForm(form.getBody(), SWT.VERTICAL);
			toolkit.adapt(sash);

			addResultActions(form);

			table = HISTOGRAM.buildWithoutBorder(sash, JdkAttributes.IO_PATH,
					getTableSettings(state.getChild(TABLE_ELEMENT)));
			MCContextMenuManager mm = MCContextMenuManager.create(table.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(table.getManager(), mm);
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(), table,
					Messages.FileIOPage_HISTOGRAM_SELECTION, mm);
			table.getManager().getViewer().addSelectionChangedListener(e -> updateDetails());
			table.getManager().getViewer()
					.addSelectionChangedListener(e -> pageContainer.showSelection(table.getSelection().getItems()));
			tableFilter = FilterComponent.createFilterComponent(table, FileIOPage.this.tableFilter,
					getDataSource().getItems().apply(TABLE_ITEMS), pageContainer.getSelectionStore()::getSelections,
					this::onTableFilterChange);
			mm.add(tableFilter.getShowFilterAction());
			mm.add(tableFilter.getShowSearchAction());

			CTabFolder tabFolder = new CTabFolder(sash, SWT.NONE);
			toolkit.adapt(tabFolder);
			CTabItem t1 = new CTabItem(tabFolder, SWT.NONE);
			t1.setToolTipText(Messages.IO_PAGE_TIMELINE_DESCRIPTION);
			timelineCanvas = new ChartCanvas(tabFolder);
			t1.setText(Messages.PAGES_TIMELINE);
			t1.setControl(timelineCanvas);
			DataPageToolkit.createChartTimestampTooltip(timelineCanvas);

			CTabItem t2 = new CTabItem(tabFolder, SWT.NONE);
			durationParent = toolkit.createComposite(tabFolder);
			durationParent.setLayout(new SimpleLayout());
			t2.setToolTipText(Messages.IO_PAGE_DURATIONS_DESCRIPTION);
			durationCanvas = new ChartCanvas(durationParent);
			durationCanvas.setLayoutData(new SimpleLayoutData(3.5f));
			DataPageToolkit.createChartTooltip(durationCanvas);

			percentileTable = PERCENTILES.build(durationParent,
					TableSettings.forState(state.getChild(PERCENTILE_TABLE_ELEMENT)));
			percentileTable.getManager().getViewer().getControl().setLayoutData(new SimpleLayoutData(6.5f));
			MCContextMenuManager percentileTableMm = MCContextMenuManager
					.create(percentileTable.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(percentileTable.getManager(), percentileTableMm);
			SelectionStoreActionToolkit.addSelectionStoreActions(percentileTable.getManager().getViewer(),
					pageContainer.getSelectionStore(), percentileTable::getSelectedItems,
					Messages.FileIOPage_PERCENTILE_SELECTION, percentileTableMm);
			t2.setText(Messages.PAGES_DURATIONS);
			t2.setControl(durationParent);

			CTabItem t3 = new CTabItem(tabFolder, SWT.NONE);
			t3.setToolTipText(Messages.IO_PAGE_SIZE_DESCRIPTION);
			sizeCanvas = new ChartCanvas(tabFolder);
			t3.setText(Messages.PAGES_SIZE);
			t3.setControl(sizeCanvas);
			DataPageToolkit.createChartTooltip(sizeCanvas);

			CTabItem t4 = new CTabItem(tabFolder, SWT.NONE);
			t4.setToolTipText(Messages.IO_PAGE_EVENT_LOG_DESCRIPTION);
			itemList = LIST.buildWithoutBorder(tabFolder, getTableSettings(state.getChild(LIST_ELEMENT)));
			MCContextMenuManager itemListMm = MCContextMenuManager
					.create(itemList.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(itemList.getManager(), itemListMm);
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(), itemList,
					Messages.FileIOPage_LOG_SELECTION, itemListMm);
			itemList.getManager().getViewer().addSelectionChangedListener(
					e -> pageContainer.showSelection(ItemCollectionToolkit.build(itemList.getSelection().get())));
			t4.setText(Messages.PAGES_EVENT_LOG);
			itemListFilter = FilterComponent.createFilterComponent(itemList, FileIOPage.this.itemListFilter,
					getDataSource().getItems().apply(TABLE_ITEMS), pageContainer.getSelectionStore()::getSelections,
					this::onListFilterChange);
			itemListMm.add(itemListFilter.getShowFilterAction());
			itemListMm.add(itemListFilter.getShowSearchAction());
			t4.setControl(itemListFilter.getComponent());

			tableFilter.loadState(state.getChild(FILE_IO_TABLE));
			itemListFilter.loadState(state.getChild(FILE_IO_LIST));

			tabFolder.setSelection(tabFolderIndex);
			tabFolder.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					tabFolderIndex = ((CTabFolder) e.getSource()).getSelectionIndex();
				}
			});

			timelineChart = createTimelineChart(pageContainer);
			hookUpTimeLineChart();
			PersistableSashForm.loadState(sash, state.getChild(SASH_ELEMENT));

			flavorSelector = FlavorSelector.itemsWithTimerange(form, TABLE_ITEMS, getDataSource().getItems(),
					pageContainer, this::onInputSelected, this::onShowFlavor, flavorSelectorState);

			table.getManager().setSelectionState(tableSelection);
			percentileTable.getManager().setSelectionState(percentileSelection);
			itemList.getManager().setSelectionState(itemListSelection);
		}

		private XYChart createTimelineChart(IPageContainer pageContainer) {
			XYChart timelineChart = new XYChart(pageContainer.getRecordingRange(), RendererToolkit.empty(), 180);
			timelineChart.setVisibleRange(timelineRange.getStart(), timelineRange.getEnd());
			timelineChart.addVisibleRangeListener(range -> timelineRange = range);
			return timelineChart;
		}

		private void onTableFilterChange(IItemFilter filter) {
			tableFilter.filterChangeHelper(filter, table, getDataSource().getItems().apply(TABLE_ITEMS));
			itemListFilter.notifyListener();
			FileIOPage.this.tableFilter = filter;
		}

		private void onListFilterChange(IItemFilter filter) {
			itemListFilter.filterChangeHelper(filter, itemList, getDataSource().getItems().apply(TABLE_ITEMS));
			FileIOPage.this.itemListFilter = filter;
		}

		@Override
		public void saveTo(IWritableState writableState) {
			PersistableSashForm.saveState(sash, writableState.createChild(SASH_ELEMENT));
			table.getManager().getSettings().saveState(writableState.createChild(TABLE_ELEMENT));
			tableFilter.saveState(writableState.createChild(FILE_IO_TABLE));
			itemList.getManager().getSettings().saveState(writableState.createChild(LIST_ELEMENT));
			itemListFilter.saveState(writableState.createChild(FILE_IO_LIST));
			percentileTable.getManager().getSettings().saveState(writableState.createChild(PERCENTILE_TABLE_ELEMENT));

			saveToLocal();
		}

		private void saveToLocal() {
			tableSelection = table.getManager().getSelectionState();
			itemListSelection = itemList.getManager().getSelectionState();
			flavorSelectorState = flavorSelector.getFlavorSelectorState();
			percentileSelection = percentileTable.getManager().getSelectionState();
		}

		private void onShowFlavor(Boolean show) {
			IRange<IQuantity> range = show ? timeRange : pageContainer.getRecordingRange();
			timelineChart.setVisibleRange(range.getStart(), range.getEnd());
			hookUpTimeLineChart();
		}

		private void hookUpTimeLineChart() {
			DataPageToolkit.setChart(timelineCanvas, timelineChart, pageContainer::showSelection);
			SelectionStoreActionToolkit.addSelectionStoreRangeActions(pageContainer.getSelectionStore(), timelineChart,
					JfrAttributes.LIFETIME, Messages.FileIOPage_TIMELINE_SELECTION, timelineCanvas.getContextMenu());
		}

		private void onInputSelected(IItemCollection items, IRange<IQuantity> timeRange) {
			this.selectionItems = items;
			this.timeRange = timeRange;
			updateDetails();
		}

		private void updateDetails() {
			IItemCollection items = selectionItems != null ? selectionItems.apply(TABLE_ITEMS)
					: getDataSource().getItems().apply(TABLE_ITEMS);
			table.show(items);
			HistogramSelection histogramSelection = table.getSelection();
			IItemCollection selectedItems = histogramSelection.getRowCount() == 0 ? items
					: histogramSelection.getItems();

			String pathCount = pathCount(histogramSelection.getRowCount());
			List<IXDataRenderer> timelineRows = new ArrayList<>();
			List<IXDataRenderer> durationRows = new ArrayList<>();
			List<IXDataRenderer> sizeRows = new ArrayList<>();
			IItemCollection readItems = selectedItems.apply(JdkFilters.FILE_READ);
			if (readItems.hasItems()) {
				timelineRows.add(DataPageToolkit.buildSizeRow(Messages.FileIOPage_ROW_FILE_READ + pathCount,
						JdkAggregators.FILE_READ_SIZE.getDescription(), readItems, JdkAggregators.FILE_READ_SIZE,
						READ_COLOR, FileIOPage::getColor));
				durationRows.add(DataPageToolkit.buildDurationHistogram(Messages.FileIOPage_ROW_FILE_READ + pathCount,
						JdkAggregators.FILE_READ_COUNT.getDescription(), readItems, JdkAggregators.FILE_READ_COUNT,
						READ_COLOR));
				sizeRows.add(DataPageToolkit.buildSizeHistogram(Messages.FileIOPage_ROW_FILE_READ + pathCount,
						JdkAggregators.FILE_READ_COUNT.getDescription(), readItems, JdkAggregators.FILE_READ_COUNT,
						READ_COLOR, JdkAttributes.IO_FILE_BYTES_READ));
			}
			IItemCollection writeItems = selectedItems.apply(JdkFilters.FILE_WRITE);
			if (writeItems.hasItems()) {
				timelineRows.add(DataPageToolkit.buildSizeRow(Messages.FileIOPage_ROW_FILE_WRITE + pathCount,
						JdkAggregators.FILE_WRITE_SIZE.getDescription(), writeItems, JdkAggregators.FILE_WRITE_SIZE,
						WRITE_COLOR, FileIOPage::getColor));
				durationRows.add(DataPageToolkit.buildDurationHistogram(Messages.FileIOPage_ROW_FILE_WRITE + pathCount,
						JdkAggregators.FILE_WRITE_COUNT.getDescription(), writeItems, JdkAggregators.FILE_WRITE_COUNT,
						WRITE_COLOR));
				sizeRows.add(DataPageToolkit.buildSizeHistogram(Messages.FileIOPage_ROW_FILE_WRITE + pathCount,
						JdkAggregators.FILE_WRITE_COUNT.getDescription(), writeItems, JdkAggregators.FILE_WRITE_COUNT,
						WRITE_COLOR, JdkAttributes.IO_FILE_BYTES_WRITTEN));
			}
//			ItemRow[] pathRows = selection.getSelectedRows(FileIOPage::buildPathLane).toArray(ItemRow[]::new);

			timelineCanvas.replaceRenderer(RendererToolkit.uniformRows(timelineRows));

			IXDataRenderer durationRoot = RendererToolkit.uniformRows(durationRows);
			// FIXME: X-auto-range should be done properly
			IQuantity max = selectedItems.getAggregate(JdkAggregators.LONGEST_EVENT);
			// FIXME: Workaround to make max value included
			max = max == null ? UnitLookup.MILLISECOND.quantity(20) : max.add(UnitLookup.MILLISECOND.quantity(20));
			XYChart durationChart = new XYChart(UnitLookup.MILLISECOND.quantity(0), max, durationRoot, 180);
			DataPageToolkit.setChart(durationCanvas, durationChart, JfrAttributes.DURATION,
					selection -> pageContainer.showSelection(selection));
			durationChart.setVisibleRange(durationRange.getStart(), durationRange.getEnd());
			durationChart.addVisibleRangeListener(range -> durationRange = range);
			durationCanvas.setChart(durationChart);
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(), durationChart,
					JfrAttributes.DURATION, Messages.FileIOPage_DURATION_SELECTION, durationCanvas.getContextMenu());
			itemList.show(selectedItems);
			percentileTable.update(selectedItems);

			IXDataRenderer sizeRoot = RendererToolkit.uniformRows(sizeRows);
			IQuantity sizeMax = selectedItems.getAggregate(JdkAggregators.FILE_READ_LARGEST);
			// FIXME: Workaround to make max value included
			sizeMax = sizeMax == null ? UnitLookup.BYTE.quantity(64): sizeMax.add(UnitLookup.BYTE.quantity(64));
			XYChart sizeChart = new XYChart(UnitLookup.BYTE.quantity(0), sizeMax, sizeRoot, 180);
			DataPageToolkit.setChart(sizeCanvas, sizeChart, JdkAttributes.IO_SIZE,
					selection -> pageContainer.showSelection(selection));
			sizeChart.setVisibleRange(sizeRange.getStart(), sizeRange.getEnd());
			sizeChart.addVisibleRangeListener(range -> sizeRange = range);
			sizeCanvas.setChart(sizeChart);
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(), sizeChart,
					JdkAttributes.IO_SIZE, Messages.FileIOPage_SIZE_SELECTION, sizeCanvas.getContextMenu());
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
					new ColumnSettings(READ_EOF, false, 80, false)));
		} else {
			return new TableSettings(state);
		}
	}

	@Override
	public IPageUI display(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
		return new IOPageUi(parent, toolkit, pageContainer, state);
	}

	private SelectionState tableSelection;
	private SelectionState itemListSelection;
	private SelectionState percentileSelection;
	private IItemFilter tableFilter = null;
	private IItemFilter itemListFilter = null;
	private int tabFolderIndex = 0;
	private IRange<IQuantity> timelineRange;
	private IRange<IQuantity> durationRange;
	private IRange<IQuantity> sizeRange;
	public FlavorSelectorState flavorSelectorState;

	public FileIOPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
		super(dpd, items, editor);
		timelineRange = editor.getRecordingRange();
		durationRange = editor.getRecordingRange();
		sizeRange = DataPageToolkit.buildSizeRange(items.getItems(), false);
	}

//	private static ItemRow buildPathLane(Object path, Supplier<Stream<ItemStream>> pathItems) {
//		String pathName = String.valueOf(path);
//		pathName = pathName.length() > 26 ? pathName.substring(0, 23) + "..." : pathName; //$NON-NLS-1$
//		return new ItemRow(pathName, buildSpanRenderer(pathItems), pathItems);
//	}

	private static Color getColor(IItem item) {
		return JdkTypeIDs.FILE_READ.equals(item.getType().getIdentifier()) ? READ_ALPHA_COLOR : WRITE_ALPHA_COLOR;
	}

	@Override
	public IItemFilter getDefaultSelectionFilter() {
		return TABLE_ITEMS;
	}

	private static String pathCount(int count) {
		switch (count) {
		case 0:
			return ""; //$NON-NLS-1$
		case 1:
			return " (" + Messages.FileIOPage_SELECTED_PATH + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		default:
			return " (" + NLS.bind(Messages.FileIOPage_SELECTED_PATHS, count) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
}
