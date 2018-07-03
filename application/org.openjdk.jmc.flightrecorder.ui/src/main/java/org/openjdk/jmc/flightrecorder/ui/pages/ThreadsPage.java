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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
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
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
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
import org.openjdk.jmc.flightrecorder.ui.common.FlavorSelector.FlavorSelectorState;
import org.openjdk.jmc.flightrecorder.ui.common.ImageConstants;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram.HistogramSelection;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram.ItemHistogramBuilder;
import org.openjdk.jmc.flightrecorder.ui.common.ItemRow;
import org.openjdk.jmc.flightrecorder.ui.common.ThreadGraphLanes;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.charts.IXDataRenderer;
import org.openjdk.jmc.ui.charts.RendererToolkit;
import org.openjdk.jmc.ui.column.ColumnManager.SelectionState;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.handlers.ActionToolkit;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;

public class ThreadsPage extends AbstractDataPage {

	public static class ThreadsPageFactory implements IDataPageFactory {

		@Override
		public String getName(IState state) {
			return Messages.ThreadsPage_NAME;
		}

		@Override
		public String[] getTopics(IState state) {
			return new String[] {JfrRuleTopics.THREADS_TOPIC};
		}

		@Override
		public ImageDescriptor getImageDescriptor(IState state) {
			return FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.PAGE_THREADS);
		}

		@Override
		public IDisplayablePage createPage(IPageDefinition definition, StreamModel items, IPageContainer editor) {
			return new ThreadsPage(definition, items, editor);
		}

	}

	private static final String THREAD_START_COL = "threadStart"; //$NON-NLS-1$
	private static final String THREAD_END_COL = "threadEnd"; //$NON-NLS-1$
	private static final String THREAD_DURATION_COL = "threadDuration"; //$NON-NLS-1$
	private static final String THREAD_LANE = "threadLane"; //$NON-NLS-1$

	private static final IItemFilter pageFilter = ItemFilters.hasAttribute(JfrAttributes.EVENT_THREAD);
	private static final ItemHistogramBuilder HISTOGRAM = new ItemHistogramBuilder();

	static {
		HISTOGRAM.addColumn(JdkAttributes.EVENT_THREAD_GROUP_NAME);
		HISTOGRAM.addColumn(JdkAttributes.EVENT_THREAD_ID);
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

	private class ThreadsPageUi extends ChartAndTableUI {
		private static final String THREADS_TABLE_FILTER = "threadsTableFilter"; //$NON-NLS-1$
		private ThreadGraphLanes lanes;
		private MCContextMenuManager mm;

		ThreadsPageUi(Composite parent, FormToolkit toolkit, IPageContainer editor, IState state) {
			super(pageFilter, getDataSource(), parent, toolkit, editor, state, getName(), pageFilter, getIcon(),
					flavorSelectorState);
			mm = (MCContextMenuManager) chartCanvas.getContextMenu();
			sash.setOrientation(SWT.HORIZONTAL);
			mm.add(new Separator());
			// FIXME: The lanes field is initialized by initializeChartConfiguration which is called by the super constructor. This is too indirect for SpotBugs to resolve and should be simplified.
			lanes.updateContextMenu(mm);

			form.getToolBarManager()
					.add(ActionToolkit.action(() -> lanes.openEditLanesDialog(mm), Messages.ThreadsPage_EDIT_LANES,
							FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.ICON_LANES_EDIT)));
			form.getToolBarManager().update(true);
			chartLegend.getControl().dispose();
			buildChart();
			table.getManager().setSelectionState(histogramSelectionState);
			tableFilterComponent.loadState(state.getChild(THREADS_TABLE_FILTER));
			chart.setVisibleRange(visibleRange.getStart(), visibleRange.getEnd());
			onFilterChange(tableFilter);
		}

		@Override
		protected ItemHistogram buildHistogram(Composite parent, IState state) {
			ItemHistogram build = HISTOGRAM.buildWithoutBorder(parent, JfrAttributes.EVENT_THREAD,
					TableSettings.forState(state));
			return build;
		}

		@Override
		protected IXDataRenderer getChartRenderer(IItemCollection itemsInTable, HistogramSelection tableSelection) {
			List<IXDataRenderer> rows = new ArrayList<>();

			IItemCollection selectedItems;
			HistogramSelection selection;
			if (tableSelection.getRowCount() == 0) {
				selectedItems = itemsInTable;
				selection = table.getAllRows();
			} else {
				selectedItems = tableSelection.getItems();
				selection = tableSelection;
			}
			boolean useDefaultSelection = rows.size() > 1;
			if (lanes.getLaneDefinitions().stream().anyMatch(a -> a.isEnabled()) && selection.getRowCount() > 0) {
				List<IXDataRenderer> threadRows = selection
						.getSelectedRows((object, items) -> lanes.buildThreadRenderer(object, items))
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
			IXDataRenderer root = rows.size() == 1 ? rows.get(0) : RendererToolkit.uniformRows(rows);
			// We don't use the default selection when there is only one row. This is to get the correct payload.
			return useDefaultSelection ? new ItemRow(root, selectedItems.apply(lanes.getEnabledLanesFilter())) : root;
		}

		@Override
		protected void onFilterChange(IItemFilter filter) {
			super.onFilterChange(filter);
			tableFilter = filter;
		}

		@Override
		public void saveTo(IWritableState state) {
			super.saveTo(state);
			tableFilterComponent.saveState(state.createChild(THREADS_TABLE_FILTER));
			saveToLocal();
		}

		private void saveToLocal() {
			flavorSelectorState = flavorSelector.getFlavorSelectorState();
			histogramSelectionState = table.getManager().getSelectionState();
			visibleRange = chart.getVisibleRange();
		}

		@Override
		protected List<IAction> initializeChartConfiguration(IState state) {
			lanes = new ThreadGraphLanes(() -> getDataSource(), () -> buildChart());
			return lanes.initializeChartConfiguration(Stream.of(state.getChildren(THREAD_LANE)));
		}
	}

	private FlavorSelectorState flavorSelectorState;
	private SelectionState histogramSelectionState;
	private IItemFilter tableFilter;
	private IRange<IQuantity> visibleRange;

	public ThreadsPage(IPageDefinition definition, StreamModel model, IPageContainer editor) {
		super(definition, model, editor);
		visibleRange = editor.getRecordingRange();
	}

	@Override
	public IPageUI display(Composite parent, FormToolkit toolkit, IPageContainer editor, IState state) {
		return new ThreadsPageUi(parent, toolkit, editor, state);
	}

}
