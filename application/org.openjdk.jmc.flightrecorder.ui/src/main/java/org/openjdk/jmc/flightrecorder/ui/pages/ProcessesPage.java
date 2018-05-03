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
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAccessorFactory;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemFilters;
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
import org.openjdk.jmc.flightrecorder.ui.StreamModel;
import org.openjdk.jmc.flightrecorder.ui.common.AbstractDataPage;
import org.openjdk.jmc.flightrecorder.ui.common.CompositeKeyAccessorFactory;
import org.openjdk.jmc.flightrecorder.ui.common.DataPageToolkit;
import org.openjdk.jmc.flightrecorder.ui.common.FilterComponent;
import org.openjdk.jmc.flightrecorder.ui.common.FlavorSelector;
import org.openjdk.jmc.flightrecorder.ui.common.FlavorSelector.FlavorSelectorState;
import org.openjdk.jmc.flightrecorder.ui.common.ImageConstants;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram.CompositeKeyHistogramBuilder;
import org.openjdk.jmc.flightrecorder.ui.common.TypeLabelProvider;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.charts.IXDataRenderer;
import org.openjdk.jmc.ui.charts.RendererToolkit;
import org.openjdk.jmc.ui.charts.XYChart;
import org.openjdk.jmc.ui.column.ColumnManager.SelectionState;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.ChartCanvas;
import org.openjdk.jmc.ui.misc.PersistableSashForm;

public class ProcessesPage extends AbstractDataPage {
	public static class ProcessesPageFactory implements IDataPageFactory {
		@Override
		public String getName(IState state) {
			return Messages.ProcessesPage_PAGE_NAME;
		}

		@Override
		public ImageDescriptor getImageDescriptor(IState state) {
			return FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.PAGE_PROCESSES);
		}

		@Override
		public String[] getTopics(IState state) {
			return new String[] {JfrRuleTopics.PROCESSES_TOPIC};
		}

		@Override
		public IDisplayablePage createPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
			return new ProcessesPage(dpd, items, editor);
		}
	}

	private static final CompositeKeyHistogramBuilder HISTOGRAM = new CompositeKeyHistogramBuilder();

	static {
		HISTOGRAM.addKeyColumn(JdkAttributes.PID);
		HISTOGRAM.addKeyColumn(JdkAttributes.COMMAND_LINE);
		HISTOGRAM.addColumn("firstSample", //$NON-NLS-1$
				Aggregators.min(Messages.ProcessesPage_AGGR_FIRST_SAMPLE, Messages.ProcessesPage_AGGR_FIRST_SAMPLE_DESC,
						JdkTypeIDs.PROCESSES, JfrAttributes.END_TIME));
		HISTOGRAM.addColumn("lastSample", Aggregators.max(Messages.ProcessesPage_AGGR_LAST_SAMPLE, //$NON-NLS-1$
				Messages.ProcessesPage_AGGR_LAST_SAMPLE_DESC, JdkTypeIDs.PROCESSES, JfrAttributes.END_TIME));
	}

	private class ProcessesUi implements IPageUI {

		private final ChartCanvas cpuCanvas;
		private final ItemHistogram processesTable;
		private FilterComponent processesFilter;
		private final SashForm sash;
		private final IPageContainer pageContainer;
		private XYChart cpuChart;
		private IRange<IQuantity> timeRange;
		private FlavorSelector flavorSelector;

		// FIXME: Want to display a string with the rule results, and also a link of some sort to more explanations
		ProcessesUi(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
			this.pageContainer = pageContainer;
			Form form = DataPageToolkit.createForm(parent, toolkit, getName(), getIcon());

			sash = new SashForm(form.getBody(), SWT.VERTICAL);

			// FIXME: Configure y-axis to always show 100%?
			cpuCanvas = new ChartCanvas(sash);
			cpuChart = new XYChart(pageContainer.getRecordingRange(), RendererToolkit.empty(), 100);
			cpuChart.setVisibleRange(timelineRange.getStart(), timelineRange.getEnd());
			cpuChart.addVisibleRangeListener(r -> timelineRange = r);
			DataPageToolkit.createChartTimestampTooltip(cpuCanvas);

			processesTable = HISTOGRAM.buildWithoutBorder(sash,
					TableSettings.forState(state.getChild(PROCESSES_TABLE)));
			processesFilter = FilterComponent.createFilterComponent(processesTable, processesFilterState,
					getDataSource().getItems().apply(JdkQueries.PROCESSES.getFilter()),
					pageContainer.getSelectionStore()::getSelections, this::onFilterChange);

			Viewer viewer = processesTable.getManager().getViewer();
			viewer.addSelectionChangedListener(
					e -> pageContainer.showSelection(processesTable.getSelection().getItems()));
			MCContextMenuManager mm = MCContextMenuManager.create(viewer.getControl());
			ColumnMenusFactory.addDefaultMenus(processesTable.getManager(), mm);
			mm.add(processesFilter.getShowFilterAction());
			mm.add(processesFilter.getShowSearchAction());
			processesFilter.loadState(state.getChild(PROCESSES_FILTER));

			PersistableSashForm.loadState(sash, state.getChild(SASH));

			flavorSelector = FlavorSelector.itemsWithTimerange(form, JdkQueries.PROCESSES.getFilter(),
					getDataSource().getItems(), pageContainer, this::onInputSelected, this::onShow,
					flavorSelectorState);

			addResultActions(form);

			onFilterChange(processesFilterState);
			processesTable.getManager().setSelectionState(processesSelection);
		}

		private void onFilterChange(IItemFilter filter) {
			processesFilter.filterChangeHelper(filter, processesTable,
					getDataSource().getItems().apply(JdkQueries.PROCESSES.getFilter()));
			processesFilterState = filter;
		}

		@Override
		public void saveTo(IWritableState memento) {
			PersistableSashForm.saveState(sash, memento.createChild(SASH));
			processesTable.getManager().getSettings().saveState(memento.createChild(PROCESSES_TABLE));
			processesFilter.saveState(memento.createChild(PROCESSES_FILTER));

			saveToLocal();
		}

		private void saveToLocal() {
			processesSelection = processesTable.getManager().getSelectionState();
			flavorSelectorState = flavorSelector.getFlavorSelectorState();
		}

		private void onShow(Boolean show) {
			IRange<IQuantity> range = show ? timeRange : pageContainer.getRecordingRange();
			cpuChart.setVisibleRange(range.getStart(), range.getEnd());
			cpuCanvas.redrawChart();
		}

		private void onInputSelected(IItemCollection items, IRange<IQuantity> timeRange) {
			IItemCollection processItems = items != null ? items.apply(JdkFilters.PROCESSES)
					: getDataSource().getItems().apply(JdkFilters.PROCESSES);
			this.timeRange = timeRange;
			// FIXME: Would like to show tooltip if the event is not enabled, but is probably a general thing to fix on the pages.
			List<IXDataRenderer> rows = new ArrayList<>();
			rows.add(DataPageToolkit
					.buildLinesRow(Messages.ProcessesPage_ROW_CPU_USAGE, Messages.ProcessesPage_ROW_CPU_USAGE_DESC,
							getDataSource().getItems(), true, JdkQueries.CPU_USAGE_DETAILED_GRAPH_QUERY, null,
							UnitLookup.PERCENT.quantity(0), UnitLookup.PERCENT.quantity(100))
					.orElse(RendererToolkit.empty()));
			IQuantity processesCount = processItems.getAggregate(Aggregators.count());
			if (processesCount != null && processesCount.compareTo(UnitLookup.NUMBER_UNITY.quantity(0)) > 0) {
				rows.add(DataPageToolkit.buildTimestampHistogram(Messages.ProcessesPage_ROW_CONCURRENT_PROCESSES,
						Messages.ProcessesPage_AGGR_CONCURRENT_PROCESSES_DESC, processItems,
						Aggregators.countDistinct(Messages.ProcessesPage_AGGR_CONCURRENT_PROCESSES,
								Messages.ProcessesPage_AGGR_CONCURRENT_PROCESSES_DESC, COMMANDLINE_PID_AF),
						TypeLabelProvider.getColor(JdkTypeIDs.PROCESSES)));
			}
			cpuChart.setRendererRoot(RendererToolkit.uniformRows(rows));
			DataPageToolkit.setChart(cpuCanvas, cpuChart, pageContainer::showSelection, this::onChartRangeSelection);
			processesTable.show(processItems);
		}

		private void onChartRangeSelection(IRange<IQuantity> range) {
			// FIXME: Do we want to use the timerange from the chart, or the actually selected items?
			IItemCollection itemsInRange = range != null ? getDataSource().getItems(range) : getDataSource().getItems();
			IItemCollection processItems = itemsInRange.apply(JdkQueries.PROCESSES.getFilter());
			processesTable.show(processItems);
		}
	}

	private static final String SASH = "sash"; //$NON-NLS-1$
	private static final String PROCESSES_TABLE = "processesTable"; //$NON-NLS-1$
	private static final String PROCESSES_FILTER = "processesFilter"; //$NON-NLS-1$
	private final IAccessorFactory<IDisplayable> COMMANDLINE_PID_AF = CompositeKeyAccessorFactory.displayable(" + ", //$NON-NLS-1$
			JdkAttributes.COMMAND_LINE, JdkAttributes.PID);

	@Override
	public IPageUI display(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
		return new ProcessesUi(parent, toolkit, pageContainer, state);
	}

	private IRange<IQuantity> timelineRange;
	private IItemFilter processesFilterState;
	private SelectionState processesSelection;
	private FlavorSelectorState flavorSelectorState;

	public ProcessesPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
		super(dpd, items, editor);
		timelineRange = editor.getRecordingRange();
	}

	@Override
	public IItemFilter getDefaultSelectionFilter() {
		return ItemFilters.or(JdkFilters.CPU_LOAD, JdkFilters.PROCESSES);
	}

}
