/*
 * Copyright (c) 2018, 2020 Oracle and/or its affiliates. All rights reserved.
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
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.IDataPageFactory;
import org.openjdk.jmc.flightrecorder.ui.IDisplayablePage;
import org.openjdk.jmc.flightrecorder.ui.IPageContainer;
import org.openjdk.jmc.flightrecorder.ui.IPageDefinition;
import org.openjdk.jmc.flightrecorder.ui.IPageUI;
import org.openjdk.jmc.flightrecorder.ui.StreamModel;
import org.openjdk.jmc.flightrecorder.ui.common.AbstractDataPage;
import org.openjdk.jmc.flightrecorder.ui.common.BucketBuilder;
import org.openjdk.jmc.flightrecorder.ui.common.DataPageToolkit;
import org.openjdk.jmc.flightrecorder.ui.common.FlavorSelector.FlavorSelectorState;
import org.openjdk.jmc.flightrecorder.ui.common.ImageConstants;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram.HistogramSelection;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram.ItemHistogramBuilder;
import org.openjdk.jmc.flightrecorder.ui.common.ItemRow;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.charts.AWTChartToolkit;
import org.openjdk.jmc.ui.charts.IXDataRenderer;
import org.openjdk.jmc.ui.charts.XYDataRenderer;
import org.openjdk.jmc.ui.column.ColumnManager.SelectionState;
import org.openjdk.jmc.ui.column.TableSettings;

public class TlabPage extends AbstractDataPage {
	public static class TlabPageFactory implements IDataPageFactory {
		@Override
		public String getName(IState state) {
			return Messages.TlabPage_PAGE_NAME;
		}

		@Override
		public ImageDescriptor getImageDescriptor(IState state) {
			return FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.PAGE_TLAB_ALLOCATIONS);
		}

		@Override
		public String[] getTopics(IState state) {
			return new String[] {JfrRuleTopics.TLAB};
		}

		@Override
		public IDisplayablePage createPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
			return new TlabPage(dpd, items, editor);
		}

	}

	private static final Color INSIDE_COLOR = new Color(0, 182, 0);
	private static final Color OUTSIDE_COLOR = new Color(164, 64, 255, 128);

	private static final IItemFilter TLAB_EVENTS = JdkFilters.ALLOC_ALL;

	private static final String INSIDE_SIZE = "insideSize"; //$NON-NLS-1$
	private static final String OUTSIDE_SIZE = "outsideSize"; //$NON-NLS-1$

	private static final String INSIDE_COUNT_COL = "insideCount"; //$NON-NLS-1$
	private static final String OUTSIDE_COUNT_COL = "outsideCount"; //$NON-NLS-1$
	private static final String AVERAGE_INSIDE_SIZE_COL = "averageInsideSize"; //$NON-NLS-1$
	private static final String AVERAGE_OUTSIDE_SIZE_COL = "averageOutsideSize"; //$NON-NLS-1$
	private static final String TOTAL_INSIDE_SIZE_COL = "totalInsideSize"; //$NON-NLS-1$
	private static final String TOTAL_INSIDE_SIZE_PERCENT_COL = "totalInsideSizePercent"; //$NON-NLS-1$
	private static final String TOTAL_OUTSIDE_SIZE_COL = "totalOutsideSize"; //$NON-NLS-1$
	private static final String TOTAL_OUTSIDE_SIZE_PERCENT_COL = "totalOutsideSizePercent"; //$NON-NLS-1$

	private static final ItemHistogramBuilder HISTOGRAM = new ItemHistogramBuilder();

	static {
		HISTOGRAM.addColumn(INSIDE_COUNT_COL, JdkAggregators.INSIDE_TLAB_COUNT);
		HISTOGRAM.addColumn(OUTSIDE_COUNT_COL, JdkAggregators.OUTSIDE_TLAB_COUNT);
		HISTOGRAM.addColumn(AVERAGE_INSIDE_SIZE_COL, JdkAggregators.ALLOC_INSIDE_TLAB_AVG);
		HISTOGRAM.addColumn(AVERAGE_OUTSIDE_SIZE_COL, JdkAggregators.ALLOC_OUTSIDE_TLAB_AVG);
		HISTOGRAM.addColumn(TOTAL_INSIDE_SIZE_COL, JdkAggregators.ALLOC_INSIDE_TLAB_SUM);
		HISTOGRAM.addPercentageColumn(TOTAL_INSIDE_SIZE_PERCENT_COL, JdkAggregators.ALLOC_INSIDE_TLAB_SUM,
				Messages.TlabPage_INSIDE_TLAB_SUM_PERCENTAGE, Messages.TlabPage_INSIDE_TLAB_SUM_PERCENTAGE_DESC);
		HISTOGRAM.addColumn(TOTAL_OUTSIDE_SIZE_COL, JdkAggregators.ALLOC_OUTSIDE_TLAB_SUM);
		HISTOGRAM.addPercentageColumn(TOTAL_OUTSIDE_SIZE_PERCENT_COL, JdkAggregators.ALLOC_OUTSIDE_TLAB_SUM,
				Messages.TlabPage_OUTSIDE_TLAB_SUM_PERCENTAGE, Messages.TlabPage_OUTSIDE_TLAB_SUM_PERCENTAGE_DESC);
	}

	private class TlabUI implements IPageUI {

		private CTabFolder tabFolder;
		private TlabChartTable threadsCT;
		private TlabChartTable methodsCT;

		private int tabFolderIndex = 0;

		public TlabUI(Composite parent, FormToolkit toolkit, IPageContainer editor, IState state) {
			tabFolder = new CTabFolder(parent, SWT.BOTTOM);

			threadsCT = new TlabChartTable(tabFolder, toolkit, editor, state, JfrAttributes.EVENT_THREAD);
			DataPageToolkit.addTabItem(tabFolder, threadsCT.getComponent(), Messages.TlabPage_THREADS_TAB_NAME);

			methodsCT = new TlabChartTable(tabFolder, toolkit, editor, state, JdkAttributes.STACK_TRACE_TOP_METHOD);
			DataPageToolkit.addTabItem(tabFolder, methodsCT.getComponent(), Messages.TlabPage_METHODS_TAB_NAME);

			tabFolder.setSelection(tabFolderIndex);
		}

		@Override
		public void saveTo(IWritableState state) {
			threadsCT.saveTo(state);
			methodsCT.saveTo(state);

			this.saveToLocal();
		}

		private void saveToLocal() {
			tabFolderIndex = tabFolder.getSelectionIndex();
		}
	}

	private class TlabChartTable extends ChartAndTableUI {
		private static final String TLAB_TABLE_FILTER = "tlabTableFilter"; //$NON-NLS-1$

		private IAction insideSizeAction;
		private IAction outsideSizeAction;

		TlabChartTable(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state,
				IAttribute<?> classifier) {
			// FIXME: This page could probably use a horizontal legend instead.
			super(TLAB_EVENTS, getDataSource(), parent, toolkit, pageContainer, state, getName(), tableFilter,
					getIcon(), flavorSelectorState, classifier);
			addResultActions(form);

			tableFilterComponent.loadState(state.getChild(TLAB_TABLE_FILTER));
			chart.setVisibleRange(visibleRange.getStart(), visibleRange.getEnd());
			chart.addVisibleRangeListener(r -> visibleRange = r);
			table.getManager().setSelectionState(tableState);
		}

		@Override
		public void saveTo(IWritableState state) {
			super.saveTo(state);
			tableFilterComponent.saveState(state.createChild(TLAB_TABLE_FILTER));

			saveToLocal();
		}

		private void saveToLocal() {
			tableState = table.getManager().getSelectionState();
			flavorSelectorState = flavorSelector.getFlavorSelectorState();
		}

		@Override
		protected ItemHistogram buildHistogram(Composite parent, IState state, IAttribute<?> classifier) {
			return HISTOGRAM.buildWithoutBorder(parent, classifier, TableSettings.forState(state));
		}

		@Override
		protected IXDataRenderer getChartRenderer(IItemCollection itemsInTable, HistogramSelection selection) {
			IItemCollection selectedItems = selection.getRowCount() == 0 ? itemsInTable : selection.getItems();
			String selectionCount = selectionCount(selection.getRowCount());
			IItemCollection filter = selectedItems.apply(JdkFilters.ALLOC_ALL);
			XYDataRenderer renderer = new XYDataRenderer(UnitLookup.MEMORY.getDefaultUnit().quantity(0),
					Messages.TlabPage_ROW_TLAB_ALLOCATIONS, Messages.TlabPage_ROW_TLAB_ALLOCATIONS_DESC);
			if (insideSizeAction.isChecked()) {
				renderer.addBarChart(
						JdkAggregators.ALLOC_INSIDE_TLAB_SUM.getName(), BucketBuilder.aggregatorSeries(filter,
								JdkAggregators.ALLOC_INSIDE_TLAB_SUM, JfrAttributes.END_TIME),
						AWTChartToolkit.staticColor(INSIDE_COLOR));
			}
			if (outsideSizeAction.isChecked()) {
				renderer.addBarChart(
						JdkAggregators.ALLOC_OUTSIDE_TLAB_SUM.getName(), BucketBuilder.aggregatorSeries(filter,
								JdkAggregators.ALLOC_OUTSIDE_TLAB_SUM, JfrAttributes.END_TIME),
						AWTChartToolkit.staticColor(OUTSIDE_COLOR));
			}
			return new ItemRow(Messages.TlabPage_ROW_TLAB_ALLOCATIONS + selectionCount, null, renderer, filter);
		}

		@Override
		protected void onFilterChange(IItemFilter filter) {
			super.onFilterChange(filter);
			tableFilter = filter;
		}

		@Override
		protected List<IAction> initializeChartConfiguration(IState state) {
			insideSizeAction = DataPageToolkit.createAggregatorCheckAction(JdkAggregators.ALLOC_INSIDE_TLAB_SUM,
					INSIDE_SIZE, INSIDE_COLOR, b -> buildChart());
			outsideSizeAction = DataPageToolkit.createAggregatorCheckAction(JdkAggregators.ALLOC_OUTSIDE_TLAB_SUM,
					OUTSIDE_SIZE, OUTSIDE_COLOR, b -> buildChart());

			return Arrays.asList(insideSizeAction, outsideSizeAction);
		}
	}

	private static String selectionCount(int count) {
		switch (count) {
		case 0:
			return ""; //$NON-NLS-1$
		case 1:
			return " (" + Messages.TlabPage_SELECTED_ONE + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		default:
			return " (" + NLS.bind(Messages.TlabPage_SELECTED_MANY, count) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	@Override
	public IPageUI display(Composite parent, FormToolkit toolkit, IPageContainer editor, IState state) {
		return new TlabUI(parent, toolkit, editor, state);
	}

	private IRange<IQuantity> visibleRange;
	private IItemFilter tableFilter;
	private SelectionState tableState;
	private FlavorSelectorState flavorSelectorState;

	public TlabPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
		super(dpd, items, editor);
		visibleRange = editor.getRecordingRange();
	}
}
