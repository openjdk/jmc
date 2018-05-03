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

import static org.openjdk.jmc.common.item.Aggregators.max;
import static org.openjdk.jmc.common.item.Aggregators.min;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
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
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkQueries;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.jdk.dataproviders.MethodProfilingDataProvider;
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
import org.openjdk.jmc.flightrecorder.ui.common.FlavorSelector.FlavorSelectorState;
import org.openjdk.jmc.flightrecorder.ui.common.ImageConstants;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram.HistogramSelection;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram.ItemHistogramBuilder;
import org.openjdk.jmc.flightrecorder.ui.common.ItemRow;
import org.openjdk.jmc.flightrecorder.ui.common.PairBucketBuilder;
import org.openjdk.jmc.flightrecorder.ui.common.ThreadGraphLanes;
import org.openjdk.jmc.flightrecorder.ui.common.TypeLabelProvider;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.OrientationAction;
import org.openjdk.jmc.ui.charts.AWTChartToolkit.IColorProvider;
import org.openjdk.jmc.ui.charts.IQuantitySeries;
import org.openjdk.jmc.ui.charts.IXDataRenderer;
import org.openjdk.jmc.ui.charts.RendererToolkit;
import org.openjdk.jmc.ui.charts.XYDataRenderer;
import org.openjdk.jmc.ui.column.ColumnManager.SelectionState;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.SWTColorToolkit;

public class JavaApplicationPage extends AbstractDataPage {
	public static class JavaApplicationPageFactory implements IDataPageFactory {

		@Override
		public String getName(IState state) {
			return Messages.JavaApplicationPage_PAGE_NAME;
		}

		@Override
		public ImageDescriptor getImageDescriptor(IState state) {
			return FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.PAGE_JAVA_APPLICATION);
		}

		@Override
		public String[] getTopics(IState state) {
			return new String[] {JfrRuleTopics.JAVA_APPLICATION_TOPIC, JfrRuleTopics.METHOD_PROFILING_TOPIC};
		}

		@Override
		public IDisplayablePage createPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
			return new JavaApplicationPage(dpd, items, editor);
		}
	}

	// FIXME: Does this really have to be so green?
	private static final Color EXCEPTIONS_COLOR = TypeLabelProvider.getColor(JdkTypeIDs.EXCEPTIONS_THROWN);
	private static final IColorProvider<IQuantity> PROFILING_COLOR = new IColorProvider<IQuantity>() {

		@Override
		public Color getColor(IQuantity balance) {
			double fraction = balance == null ? 0 : Math.max(0, Math.min(balance.doubleValue(), 1));
			float hue = 0.12f - (float) (fraction * 0.1); // ~ 43 - 7 deg
			float saturation = fraction < 0.5 ? 0.35f + (float) fraction : 0.85f; // include (1, 0.85), (0.5, 0.85) and (0, 0.35)
			return Color.getHSBColor(hue, saturation, 0.95f);
		}

	};

	private static final ImageDescriptor PROFILING_LEGEND_ICON = SWTColorToolkit.createGradientThumbnail(
			SWTColorToolkit.asRGB(PROFILING_COLOR.getColor(UnitLookup.NUMBER_UNITY.quantity(1))),
			SWTColorToolkit.asRGB(PROFILING_COLOR.getColor(UnitLookup.NUMBER_UNITY.quantity(0))), true);

	private static final IItemFilter ALL_THREAD_EVENTS = ItemFilters.hasAttribute(JfrAttributes.EVENT_THREAD);

	private static final String THREAD_LANE = "threadLane"; //$NON-NLS-1$
	private static final String PROFILING_COUNT_COL = "profilingCount"; //$NON-NLS-1$
	private static final String ALLOCATION_COL = "allocation"; //$NON-NLS-1$
	private static final String EXCEPTIONS_COL = "exceptions"; //$NON-NLS-1$
	private static final String THREAD_START_COL = "threadStart"; //$NON-NLS-1$
	private static final String THREAD_END_COL = "threadEnd"; //$NON-NLS-1$
	private static final String THREAD_DURATION_COL = "threadDuration"; //$NON-NLS-1$
	private static final String IO_TIME_COL = "ioTime"; //$NON-NLS-1$
	private static final String IO_COUNT_COL = "ioCount"; //$NON-NLS-1$
	private static final String BLOCKED_TIME_COL = "blockedTime"; //$NON-NLS-1$
	private static final String BLOCKED_COUNT_COL = "blockedCount"; //$NON-NLS-1$
	private static final String CLASSLOAD_COUNT_COL = "classloadingCount"; //$NON-NLS-1$
	private static final String CLASSLOAD_TIME_COL = "classloadingTime"; //$NON-NLS-1$
	private static final String APPLICATION_PAUSE_ID = "applicationPause"; //$NON-NLS-1$
	private static final String ACTIVITY_LANES_ID = "threadActivityLanes"; //$NON-NLS-1$

	private static final ItemHistogramBuilder HISTOGRAM = new ItemHistogramBuilder();

	static {
		HISTOGRAM.addColumn(JdkAttributes.EVENT_THREAD_GROUP_NAME);
		HISTOGRAM.addColumn(JdkAttributes.EVENT_THREAD_ID);
		// FIXME: Decide which columns we should actually have
		HISTOGRAM.addColumn(PROFILING_COUNT_COL, JdkAggregators.EXECUTION_SAMPLE_COUNT);
		// FIXME: Do we want combined samples/balance column here?
		HISTOGRAM.addColumn(ALLOCATION_COL, JdkAggregators.ALLOCATION_TOTAL);
		HISTOGRAM.addColumn(IO_TIME_COL, JdkAggregators.TOTAL_IO_TIME);
		HISTOGRAM.addColumn(IO_COUNT_COL, JdkAggregators.TOTAL_IO_COUNT);
		HISTOGRAM.addColumn(BLOCKED_TIME_COL, JdkAggregators.TOTAL_BLOCKED_TIME);
		HISTOGRAM.addColumn(BLOCKED_COUNT_COL, JdkAggregators.TOTAL_BLOCKED_COUNT);
		HISTOGRAM.addColumn(CLASSLOAD_COUNT_COL, JdkAggregators.CLASS_LOADING_COUNT);
		HISTOGRAM.addColumn(CLASSLOAD_TIME_COL, JdkAggregators.CLASS_LOADING_TIME_SUM);
		HISTOGRAM.addColumn(EXCEPTIONS_COL, JdkAggregators.THROWABLES_COUNT);
		HISTOGRAM.addColumn(THREAD_START_COL,
				min(Messages.JavaApplicationPage_COLUMN_THREAD_START,
						Messages.JavaApplicationPage_COLUMN_THREAD_START_DESC, JdkTypeIDs.JAVA_THREAD_START,
						JfrAttributes.EVENT_TIMESTAMP));
		/*
		 * Will order empty cells before first end time.
		 * 
		 * It should be noted that no event (empty column cell) is considered less than all values
		 * (this is common for all columns), which causes the column to sort threads without end
		 * time (indicating that the thread ended after the end of the recording) is ordered before
		 * the thread that ended first. While this is not optimal, we decided to accept it as it's
		 * not obviously better to have this particular column ordering empty cells last in contrast
		 * to all other columns.
		 */
		HISTOGRAM.addColumn(THREAD_END_COL,
				max(Messages.JavaApplicationPage_COLUMN_THREAD_END, Messages.JavaApplicationPage_COLUMN_THREAD_END_DESC,
						JdkTypeIDs.JAVA_THREAD_END, JfrAttributes.EVENT_TIMESTAMP));
		HISTOGRAM.addColumn(JdkAttributes.EVENT_THREAD_ID);
		HISTOGRAM.addColumn(THREAD_DURATION_COL, ic -> {
			IQuantity threadStart = ic.apply(ItemFilters.type(JdkTypeIDs.JAVA_THREAD_START))
					.getAggregate((IAggregator<IQuantity, ?>) Aggregators.min(JfrAttributes.EVENT_TIMESTAMP));
			IQuantity threadEnd = ic.apply(ItemFilters.type(JdkTypeIDs.JAVA_THREAD_END))
					.getAggregate((IAggregator<IQuantity, ?>) Aggregators.max(JfrAttributes.EVENT_TIMESTAMP));
			if (threadStart != null && threadEnd != null) {
				return threadEnd.subtract(threadStart);
			}
			return null;
		}, Messages.JavaApplicationPage_COLUMN_THREAD_DURATION,
				Messages.JavaApplicationPage_COLUMN_THREAD_DURATION_DESC);
	}

	private class JavaApplicationUi extends ChartAndTableUI {

		private static final String METHOD_PROFILING_TABLE_FILTER = "methodProfilingTableFilter"; //$NON-NLS-1$

		private IAction applicationPauseIdAction;
		private IAction profilingCountAction;
		private IAction allocationAction;
		private IAction exceptionsAction;
		private IAction threadActivityAction;
		private MCContextMenuManager mm;
		private ThreadGraphLanes lanes;

		JavaApplicationUi(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
			super(ALL_THREAD_EVENTS, getDataSource(), parent, toolkit, pageContainer, state, getName(), tableFilter,
					getIcon(), flavorSelectorState);
			mm = MCContextMenuManager.create(chartLegend.getControl());

			// FIXME: The lanes field is initialized by initializeChartConfiguration which is called by the super constructor. This is too indirect for SpotBugs to resolve and should be simplified.
			lanes.updateContextMenu(mm);
			buildChart();

			addResultActions(form);
			tableFilterComponent.loadState(state.getChild(METHOD_PROFILING_TABLE_FILTER));

			form.getToolBarManager().add(new Separator());
			OrientationAction.installActions(form, sash);

			chart.addVisibleRangeListener(r -> visibleRange = r);
			chart.setVisibleRange(visibleRange.getStart(), visibleRange.getEnd());
			table.getManager().setSelectionState(tableState);
		}

		@Override
		protected ItemHistogram buildHistogram(Composite parent, IState state) {
			ItemHistogram build = HISTOGRAM.buildWithoutBorder(parent, JfrAttributes.EVENT_THREAD,
					TableSettings.forState(state));
			return build;
		}

		@Override
		protected IXDataRenderer getChartRenderer(IItemCollection itemsInTable, HistogramSelection selection) {
			List<IXDataRenderer> rows = new ArrayList<>();

			IItemCollection allItems = getDataSource().getItems();

			DataPageToolkit.buildLinesRow(Messages.JavaApplicationPage_ROW_CPU_USAGE,
					Messages.JavaApplicationPage_ROW_CPU_USAGE_DESC, allItems, true, JdkQueries.CPU_USAGE_SIMPLE_QUERY,
					this::isAttributeEnabled, UnitLookup.PERCENT.quantity(0), UnitLookup.PERCENT.quantity(100))
					.ifPresent(rows::add);
			DataPageToolkit.buildLinesRow(Messages.JavaApplicationPage_ROW_HEAP_USAGE,
					JdkAttributes.HEAP_USED.getDescription(), allItems, false, JdkQueries.HEAP_SUMMARY,
					this::isAttributeEnabled, UnitLookup.BYTE.quantity(0), null).ifPresent(rows::add);

			IItemCollection selectedItems = selection.getRowCount() == 0 ? itemsInTable : selection.getItems();
			String threadCount = threadCount(selection.getRowCount());

			if (profilingCountAction.isChecked()) {
				IItemCollection profilingItems = selectedItems.apply(JdkFilters.EXECUTION_SAMPLE);
				IQuantitySeries<IQuantity[]> aggregatorSeries = PairBucketBuilder.aggregatorSeries(profilingItems,
						JdkAggregators.EXECUTION_SAMPLE_COUNT, MethodProfilingDataProvider.TOP_FRAME_BALANCE,
						JfrAttributes.END_TIME);
				// Y-axis max >= 100 samples to distinguish areas with too few samples to rely on the statistics
				XYDataRenderer renderer = new XYDataRenderer(UnitLookup.NUMBER_UNITY.quantity(0),
						UnitLookup.NUMBER_UNITY.quantity(100), true, Messages.JavaApplicationPage_METHOD_PROFILING,
						JdkAggregators.EXECUTION_SAMPLE_COUNT.getDescription());
				renderer.addBarChart(Messages.JavaApplicationPage_METHOD_PROFILING, aggregatorSeries, PROFILING_COLOR);
				rows.add(new ItemRow(Messages.JavaApplicationPage_METHOD_PROFILING + threadCount,
						JdkAggregators.EXECUTION_SAMPLE_COUNT.getDescription(), renderer, profilingItems));
			}
			if (allocationAction.isChecked()) {
				// FIXME: Add color based on top frame balance here as well?
				rows.add(DataPageToolkit.buildTimestampHistogram(
						Messages.JavaApplicationPage_ROW_ALLOCATION + threadCount,
						JdkAggregators.ALLOCATION_TOTAL.getDescription(), selectedItems.apply(JdkFilters.ALLOC_ALL),
						JdkAggregators.ALLOCATION_TOTAL, DataPageToolkit.ALLOCATION_COLOR));
			}
			if (exceptionsAction.isChecked()) {
				rows.add(DataPageToolkit.buildTimestampHistogram(
						JdkAggregators.THROWABLES_COUNT.getName() + threadCount,
						JdkAggregators.THROWABLES_COUNT.getDescription(), selectedItems.apply(JdkFilters.THROWABLES),
						JdkAggregators.THROWABLES_COUNT, EXCEPTIONS_COLOR));
			}
			boolean useDefaultSelection = rows.size() > 1;
			if (threadActivityAction.isChecked() && lanes.getLaneDefinitions().stream().anyMatch(a -> a.isEnabled())
					&& selection.getRowCount() > 0) {
				List<IXDataRenderer> threadRows = selection
						.getSelectedRows((thread, items) -> lanes.buildThreadRenderer(thread, items))
						.collect(Collectors.toList());
				double threadsWeight = Math.sqrt(threadRows.size()) * 0.15;
				double otherRowWeight = Math.max(threadsWeight * 0.1, (1 - threadsWeight) / rows.size());
				List<Double> weights = Stream
						.concat(Stream.generate(() -> otherRowWeight).limit(rows.size()), Stream.of(threadsWeight))
						.collect(Collectors.toList());
				rows.add(RendererToolkit.uniformRows(threadRows));
				useDefaultSelection = true;
				rows = Arrays.asList(RendererToolkit.weightedRows(rows, weights));
			}
			IXDataRenderer rr = rows.size() == 1 ? rows.get(0) : RendererToolkit.uniformRows(rows);
			IXDataRenderer root = applicationPauseIdAction.isChecked()
					? RendererToolkit.layers(DataPageToolkit.buildApplicationPauseRow(allItems), rr) : rr;
			// NOTE: Don't use the default selection when there is only one row. This is to get the correct payload.
			return useDefaultSelection ? new ItemRow(root, getDefaultSelection(selectedItems)) : root;
		}

		@Override
		protected List<IAction> initializeChartConfiguration(IState state) {
			applicationPauseIdAction = DataPageToolkit.createCheckAction(Messages.JavaApplicationPage_HALTS_ACTION,
					Messages.JavaApplicationPage_HALTS_ACTION_DESC, APPLICATION_PAUSE_ID,
					FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.ICON_STOP), b -> buildChart());
			Stream<IAction> attributeActions = Stream
					.concat(JdkQueries.CPU_USAGE_SIMPLE_QUERY.getAttributes().stream(),
							Stream.of(JdkAttributes.HEAP_USED))
					.map(a -> DataPageToolkit.createAttributeCheckAction(a, b -> buildChart()));

			// FIXME: Consider using a custom tooltip instead where the color could be shown graphically.
			// NOTE: Depending on how JMC-4276 is resolved, this may be removed instead.
			profilingCountAction = DataPageToolkit.createCheckAction(Messages.JavaApplicationPage_METHOD_PROFILING,
					Messages.JavaApplicationPage_METHOD_PROFILING_DESC, PROFILING_COUNT_COL, PROFILING_LEGEND_ICON,
					b -> buildChart());

			allocationAction = DataPageToolkit.createAggregatorCheckAction(JdkAggregators.ALLOCATION_TOTAL,
					ALLOCATION_COL, DataPageToolkit.ALLOCATION_COLOR, b -> buildChart());
			exceptionsAction = DataPageToolkit.createAggregatorCheckAction(JdkAggregators.THROWABLES_COUNT,
					EXCEPTIONS_COL, EXCEPTIONS_COLOR, b -> buildChart());

			threadActivityAction = DataPageToolkit.createCheckAction(
					Messages.JavaApplicationPage_THREAD_ACTIVITY_ACTION,
					Messages.JavaApplicationPage_THREAD_ACTIVITY_ACTION_DESC, ACTIVITY_LANES_ID,
					FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.ICON_LANES), b -> buildChart());

			lanes = new ThreadGraphLanes(() -> getDataSource(), () -> buildChart());
			lanes.initializeChartConfiguration(Stream.of(state.getChildren(THREAD_LANE)));
			return Stream
					.concat(Stream.of(applicationPauseIdAction), Stream.concat(attributeActions,
							Stream.of(profilingCountAction, allocationAction, exceptionsAction, threadActivityAction)))
					.collect(Collectors.toList());
		}

		private IItemCollection getDefaultSelection(IItemCollection items) {
			// FIXME: JMC-5192 - Should we use the selection even though it might be unchecked?
			Object firstElement = ((IStructuredSelection) chartLegend.getSelection()).getFirstElement();
			if (firstElement != null) {
				switch (((IAction) firstElement).getId()) {
				case PROFILING_COUNT_COL:
					return items.apply(JdkFilters.EXECUTION_SAMPLE);
				case ALLOCATION_COL:
					return items.apply(JdkFilters.ALLOC_ALL);
				case EXCEPTIONS_COL:
					return items.apply(JdkFilters.THROWABLES);
				case ACTIVITY_LANES_ID:
					return items.apply(lanes.getEnabledLanesFilter());
				}
			}
			// FIXME: Could we return the other type chart items that do not depend on the thread selection, like cpu, heap etc?
			return ItemCollectionToolkit.EMPTY;
		}

		@Override
		protected void onFilterChange(IItemFilter filter) {
			super.onFilterChange(filter);
			tableFilter = filter;
		}

		@Override
		public void saveTo(IWritableState writableState) {
			super.saveTo(writableState);
			lanes.saveTo(writableState);
			tableFilterComponent.saveState(writableState.createChild(METHOD_PROFILING_TABLE_FILTER));

			saveToLocal();
		}

		private void saveToLocal() {
			tableState = table.getManager().getSelectionState();
			flavorSelectorState = flavorSelector.getFlavorSelectorState();
		}
	}

	private IRange<IQuantity> visibleRange;
	private IItemFilter tableFilter;
	private SelectionState tableState;
	private FlavorSelectorState flavorSelectorState;

	@Override
	public IPageUI display(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
		return new JavaApplicationUi(parent, toolkit, pageContainer, state);
	}

	public JavaApplicationPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
		super(dpd, items, editor);
		visibleRange = editor.getRecordingRange();
	}

	@Override
	public IItemFilter getDefaultSelectionFilter() {
		return ALL_THREAD_EVENTS;
	}

	private static String threadCount(int count) {
		switch (count) {
		case 0:
			return ""; //$NON-NLS-1$
		case 1:
			return " (" + Messages.JavaApplicationPage_SELECTED_THREAD + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		default:
			return NLS.bind(" (" + Messages.JavaApplicationPage_SELECTED_THREADS + ")", count); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
}
