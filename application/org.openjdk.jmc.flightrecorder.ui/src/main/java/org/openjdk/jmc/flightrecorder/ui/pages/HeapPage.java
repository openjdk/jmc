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

import static org.openjdk.jmc.flightrecorder.jdk.JdkQueries.HEAP_SUMMARY;
import static org.openjdk.jmc.flightrecorder.jdk.JdkQueries.OS_MEMORY_SUMMARY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IItemIterable;
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
import org.openjdk.jmc.flightrecorder.rules.jdk.dataproviders.ObjectStatisticsDataProvider;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.IDataPageFactory;
import org.openjdk.jmc.flightrecorder.ui.IDisplayablePage;
import org.openjdk.jmc.flightrecorder.ui.IPageContainer;
import org.openjdk.jmc.flightrecorder.ui.IPageDefinition;
import org.openjdk.jmc.flightrecorder.ui.IPageUI;
import org.openjdk.jmc.flightrecorder.ui.ItemCollectionToolkit;
import org.openjdk.jmc.flightrecorder.ui.ItemIterableToolkit;
import org.openjdk.jmc.flightrecorder.ui.StreamModel;
import org.openjdk.jmc.flightrecorder.ui.common.AbstractDataPage;
import org.openjdk.jmc.flightrecorder.ui.common.DataPageToolkit;
import org.openjdk.jmc.flightrecorder.ui.common.FlavorSelector.FlavorSelectorState;
import org.openjdk.jmc.flightrecorder.ui.common.ImageConstants;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram.HistogramSelection;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram.ItemHistogramBuilder;
import org.openjdk.jmc.flightrecorder.ui.common.ItemRow;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.charts.IQuantitySeries;
import org.openjdk.jmc.ui.charts.IXDataRenderer;
import org.openjdk.jmc.ui.charts.RendererToolkit;
import org.openjdk.jmc.ui.charts.XYDataRenderer;
import org.openjdk.jmc.ui.column.ColumnManager.SelectionState;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.column.TableSettings.ColumnSettings;

public class HeapPage extends AbstractDataPage {
	public static class HeapPageFactory implements IDataPageFactory {

		@Override
		public String getName(IState state) {
			return Messages.HeapPage_PAGE_NAME;
		}

		@Override
		public ImageDescriptor getImageDescriptor(IState state) {
			return FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.PAGE_HEAP);
		}

		@Override
		public String[] getTopics(IState state) {
			return new String[] {JfrRuleTopics.HEAP_TOPIC};
		}

		@Override
		public IDisplayablePage createPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
			return new HeapPage(dpd, items, editor);
		}

	}

	private static final IItemFilter TABLE_ITEMS = ItemFilters.or(JdkFilters.OBJECT_COUNT, JdkFilters.ALLOC_ALL);
	private static final String INSTANCES_COL = "instances"; //$NON-NLS-1$
	private static final String SIZE_COL = "size"; //$NON-NLS-1$
	private static final String INCREASE_COL = "increase"; //$NON-NLS-1$
	private static final String ALLOCATION_COL = "allocation"; //$NON-NLS-1$
	private static final String INSIDE_TLAB_COL = "insideTlabSize"; //$NON-NLS-1$
	private static final String OUTSIDE_TLAB_COL = "outsideTlabSize"; //$NON-NLS-1$
	private static final String GC_PAUSE_ID = "gcPause"; //$NON-NLS-1$

	private static final ItemHistogramBuilder HISTOGRAM = new ItemHistogramBuilder();

	static {
		HISTOGRAM.addColumn(INSTANCES_COL, JdkAggregators.OBJECT_COUNT_MAX_INSTANCES);
		HISTOGRAM.addColumn(SIZE_COL, JdkAggregators.OBJECT_COUNT_MAX_SIZE);
		HISTOGRAM.addColumn(INCREASE_COL, ObjectStatisticsDataProvider.getIncreaseAggregator());
		HISTOGRAM.addColumn(ALLOCATION_COL, JdkAggregators.ALLOCATION_TOTAL);
		HISTOGRAM.addColumn(INSIDE_TLAB_COL, JdkAggregators.ALLOC_INSIDE_TLAB_SUM);
		HISTOGRAM.addColumn(OUTSIDE_TLAB_COL, JdkAggregators.ALLOC_OUTSIDE_TLAB_SUM);
	}

	private class ObjectStatisticsUi extends ChartAndTableUI {

		private static final String HEAP_FILTER = "heapFilter"; //$NON-NLS-1$

		private IAction gcPauseAction;
		private IAction sizeAction;
		private IAction allocationAction;

		ObjectStatisticsUi(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
			super(TABLE_ITEMS, getDataSource(), parent, toolkit, pageContainer, state, getName(), tableFilter,
					getIcon(), flavorSelectorState);
			tableFilterComponent.loadState(state.getChild(HEAP_FILTER));
			addResultActions(form);
			chart.setVisibleRange(visibleRange.getStart(), visibleRange.getEnd());
			chart.addVisibleRangeListener(r -> visibleRange = r);
			table.getManager().setSelectionState(histogramState);
		}

		@Override
		public void saveTo(IWritableState writableState) {
			super.saveTo(writableState);
			tableFilterComponent.saveState(writableState.createChild(HEAP_FILTER));

			saveToLocal();
		}

		private void saveToLocal() {
			histogramState = table.getManager().getSelectionState();
			flavorSelectorState = flavorSelector.getFlavorSelectorState();
		}

		@Override
		protected ItemHistogram buildHistogram(Composite parent, IState state) {
			return HISTOGRAM.buildWithoutBorder(parent, JdkAttributes.OBJECT_CLASS, getTableSettings(state));
		}

		@Override
		protected IXDataRenderer getChartRenderer(IItemCollection itemsInTable, HistogramSelection selection) {
			List<IXDataRenderer> rows = new ArrayList<>();

			IItemCollection allItems = getDataSource().getItems();
			String classCount = classCount(selection.getRowCount());
			IItemCollection selectedItems = selection.getRowCount() == 0 ? itemsInTable : selection.getItems();
			if (allocationAction.isChecked()) {
				rows.add(DataPageToolkit.buildTimestampHistogram(Messages.HeapPage_ROW_ALLOCATION + classCount,
						JdkAggregators.ALLOCATION_TOTAL.getDescription(), selectedItems.apply(JdkFilters.ALLOC_ALL),
						JdkAggregators.ALLOCATION_TOTAL, DataPageToolkit.ALLOCATION_COLOR));
			}

			XYDataRenderer heapRenderer = new XYDataRenderer(UnitLookup.MEMORY.getDefaultUnit().quantity(0),
					Messages.HeapPage_ROW_MEMORY_USAGE, Messages.HeapPage_ROW_MEMORY_USAGE_DESC);
			IItemCollection allEvents = null;
			IItemCollection heapSummaryEvents = allItems.apply(HEAP_SUMMARY.getFilter());
			Stream<IAttribute<IQuantity>> hsAttributes = DataPageToolkit.getQuantityAttributes(HEAP_SUMMARY)
					.filter(this::isAttributeEnabled);
			if (DataPageToolkit.addEndTimeLines(heapRenderer, heapSummaryEvents, false, hsAttributes)) {
				allEvents = heapSummaryEvents;
			}

			IItemCollection memorySummaryEvents = allItems.apply(OS_MEMORY_SUMMARY.getFilter());
			Stream<IAttribute<IQuantity>> msAttributes = DataPageToolkit.getQuantityAttributes(OS_MEMORY_SUMMARY)
					.filter(this::isAttributeEnabled);
			if (DataPageToolkit.addEndTimeLines(heapRenderer, memorySummaryEvents, false, msAttributes)) {
				allEvents = allEvents == null ? memorySummaryEvents
						: ItemCollectionToolkit.merge(() -> Stream.of(heapSummaryEvents, memorySummaryEvents));
			}
			if (allEvents != null) {
				rows.add(new ItemRow(Messages.HeapPage_ROW_MEMORY_USAGE, Messages.HeapPage_ROW_MEMORY_USAGE_DESC,
						heapRenderer, allEvents));
			}
			if (sizeAction.isChecked()) {
				boolean noSelection = selection.getRowCount() == 0;
				HistogramSelection selectedOrAll = noSelection ? table.getAllRows() : selection;
				ObjectCountLane ocLane = new ObjectCountLane(noSelection);
				long noClasses = selectedOrAll.getSelectedRows(ocLane::addClass).filter(Optional::isPresent).count();
				if (noClasses > 0) {
					// FIXME: Add a better description.
					rows.add(new ItemRow(Messages.HeapPage_ROW_LIVE_SIZE + classCount((int) noClasses),
							Messages.HeapPage_ROW_LIVE_SIZE_DESC, ocLane.renderer,
							selectedItems.apply(JdkFilters.OBJECT_COUNT)));
				}
			}

			IXDataRenderer rr = RendererToolkit.uniformRows(rows);
			IXDataRenderer root = gcPauseAction.isChecked()
					? RendererToolkit.layers(rr, DataPageToolkit.buildGcPauseRow(allItems)) : rr;
			return new ItemRow(root, selectedItems.apply(JdkFilters.ALLOC_ALL));
		}

		@Override
		protected void onFilterChange(IItemFilter filter) {
			super.onFilterChange(filter);
			tableFilter = filter;
		}

		@Override
		protected List<IAction> initializeChartConfiguration(IState state) {
			gcPauseAction = DataPageToolkit.createCheckAction(Messages.HeapPage_OVERLAY_GC,
					Messages.HeapPage_OVERLAY_GC_DESC, GC_PAUSE_ID, DataPageToolkit.GC_LEGEND_ICON, b -> buildChart());
			sizeAction = DataPageToolkit.createCheckAction(Messages.HeapPage_ROW_LIVE_SIZE,
					Messages.HeapPage_ROW_LIVE_SIZE_DESC, SIZE_COL,
					FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.PAGE_HEAP), b -> buildChart());
			allocationAction = DataPageToolkit.createAggregatorCheckAction(JdkAggregators.ALLOCATION_TOTAL,
					ALLOCATION_COL, DataPageToolkit.ALLOCATION_COLOR, b -> buildChart());
			Stream<IAction> attributeActions = Stream
					.concat(HEAP_SUMMARY.getAttributes().stream(), OS_MEMORY_SUMMARY.getAttributes().stream())
					.map(a -> DataPageToolkit.createAttributeCheckAction(a, b -> buildChart()));

			return Stream.concat(Stream.concat(Stream.of(gcPauseAction, allocationAction), attributeActions),
					Stream.of(sizeAction)).collect(Collectors.toList());
		}
	}

	private static TableSettings getTableSettings(IState state) {
		if (state == null) {
			return new TableSettings(SIZE_COL,
					Arrays.asList(new ColumnSettings(ItemHistogram.KEY_COL_ID, false, 500, null),
							new ColumnSettings(INSTANCES_COL, false, 120, false),
							new ColumnSettings(SIZE_COL, false, 120, false),
							new ColumnSettings(INCREASE_COL, false, 120, false),
							new ColumnSettings(ALLOCATION_COL, false, 120, false)));
		} else {
			return new TableSettings(state);
		}
	}

	private static String classCount(int count) {
		switch (count) {
		case 0:
			return ""; //$NON-NLS-1$
		case 1:
			return " (" + Messages.HeapPage_SELECTED_CLASS + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		default:
			return " (" + NLS.bind(Messages.HeapPage_SELECTED_CLASSES, count) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private IRange<IQuantity> visibleRange;
	private IItemFilter tableFilter;
	private SelectionState histogramState;
	private FlavorSelectorState flavorSelectorState;

	public HeapPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
		super(dpd, items, editor);
		visibleRange = editor.getRecordingRange();
	}

	@Override
	public IItemFilter getDefaultSelectionFilter() {
		return ItemFilters.or(TABLE_ITEMS, JdkFilters.HEAP_SUMMARY);
	}

	@Override
	public IPageUI display(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
		return new ObjectStatisticsUi(parent, toolkit, pageContainer, state);
	}

	private static class ObjectCountLane {
		private final XYDataRenderer renderer = new XYDataRenderer(UnitLookup.MEMORY.getDefaultUnit().quantity(0));
		private final boolean plot;

		ObjectCountLane(boolean plot) {
			this.plot = plot;
		}

		private Optional<Object> addClass(Object klass, IItemCollection items) {
			return ItemCollectionToolkit.join(items, JdkTypeIDs.OBJECT_COUNT).map(ocItems -> addClass(klass, ocItems));
		}

		private Object addClass(Object klass, IItemIterable ocItems) {
			Iterator<? extends IItem> sorted = ItemIterableToolkit
					.sorted(ocItems, JfrAttributes.END_TIME, Comparator.naturalOrder()).iterator();
			IQuantitySeries<?> qs = DataPageToolkit.buildQuantitySeries(sorted, ocItems.getType(),
					JfrAttributes.END_TIME, JdkAttributes.HEAP_TOTAL);
			String text = NLS.bind(Messages.HeapPage_LIVE_SIZE_OF_CLASS, klass);
			if (plot) {
				renderer.addPlotChart(text, qs, ColorToolkit.getDistinguishableColor(klass), true);
			} else {
				renderer.addLineChart(text, qs, ColorToolkit.getDistinguishableColor(klass), false);
			}
			return klass;
		}

	}
}
