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
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.ColorToolkit;
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
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram.HistogramSelection;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram.ItemHistogramBuilder;
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
import org.openjdk.jmc.ui.column.TableSettings.ColumnSettings;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.ChartCanvas;
import org.openjdk.jmc.ui.misc.PersistableSashForm;

public class VMOperationPage extends AbstractDataPage {
	private static final ItemListBuilder LIST = new ItemListBuilder();
	private static final ItemHistogramBuilder VM_OPERATIONS_HISTOGRAM = new ItemHistogramBuilder();
	private static final Color VM_OPERATIONS_COLOR = new Color(0xFF0000);
	private static final Color VM_OPERATIONS_SPAN_COLOR = ColorToolkit
			.withAlpha(TypeLabelProvider.getColor(JdkTypeIDs.VM_OPERATIONS), 80);

	static {
		VM_OPERATIONS_HISTOGRAM.addColumn("maxDuration", Aggregators.max(JfrAttributes.DURATION)); //$NON-NLS-1$
		VM_OPERATIONS_HISTOGRAM.addColumn("totalDuration", Aggregators.sum(JfrAttributes.DURATION)); //$NON-NLS-1$
		VM_OPERATIONS_HISTOGRAM.addColumn("stddev", Aggregators.stddevp(JfrAttributes.DURATION)); //$NON-NLS-1$
		VM_OPERATIONS_HISTOGRAM.addCountColumn();

		LIST.addColumn(JdkAttributes.OPERATION);
		LIST.addColumn(JdkAttributes.BLOCKING);
		LIST.addColumn(JdkAttributes.SAFEPOINT);
		LIST.addColumn(JfrAttributes.START_TIME);
		LIST.addColumn(JfrAttributes.END_TIME);
		LIST.addColumn(JfrAttributes.DURATION);
		LIST.addColumn(JfrAttributes.EVENT_THREAD);
		LIST.addColumn(JdkAttributes.CALLER);
	}

	public static class VMOperationsPageFactory implements IDataPageFactory {

		@Override
		public String getName(IState state) {
			return Messages.VMOperationPage_PAGE_NAME;
		}

		@Override
		public ImageDescriptor getImageDescriptor(IState state) {
			// FIXME: Change to another icon
			return FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.PAGE_JVM_INTERNALS);
		}

		@Override
		public String[] getTopics(IState state) {
			return new String[] {JfrRuleTopics.VM_OPERATIONS_TOPIC};
		}

		@Override
		public IDisplayablePage createPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
			return new VMOperationPage(dpd, items, editor);
		}

	}

	private class VMOperationsUi implements IPageUI {
		private static final String SASH_ELEMENT = "sash"; //$NON-NLS-1$
		private static final String LIST_ELEMENT = "eventList"; //$NON-NLS-1$
		private static final String LIST_FILTER_ELEMENT = "eventListFilter"; //$NON-NLS-1$
		private static final String VM_OPERATIONS_ELEMENT = "vmOperationsTable"; //$NON-NLS-1$
		private static final String VM_OPERATIONS_FILTER_ELEMENT = "vmOperationsFilter"; //$NON-NLS-1$

		private final ChartCanvas timelineCanvas;
		private final ChartCanvas durationCanvas;
		private final ItemHistogram vmOperationsTable;
		private FilterComponent vmOperationsFilter;
		private final ItemList itemList;
		private FilterComponent itemFilter;

		private final SashForm sash;
		private final IPageContainer pageContainer;
		private IItemCollection selectionItems;
		private CTabFolder tabFolder;
		private XYChart durationChart;
		private XYChart timelineChart;
		private IRange<IQuantity> timeRange;
		private FlavorSelector flavorSelector;

		VMOperationsUi(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
			this.pageContainer = pageContainer;
			Form form = DataPageToolkit.createForm(parent, toolkit, getName(), getIcon());
			sash = new SashForm(form.getBody(), SWT.VERTICAL);

			vmOperationsTable = VM_OPERATIONS_HISTOGRAM.buildWithoutBorder(sash, JdkAttributes.OPERATION,
					getTableSettings(state.getChild(VM_OPERATIONS_ELEMENT)));
			vmOperationsFilter = FilterComponent.createFilterComponent(vmOperationsTable, vmOperationsHistogramFilter,
					getDataSource().getItems().apply(JdkFilters.VM_OPERATIONS),
					pageContainer.getSelectionStore()::getSelections, this::onFilterChange);
			vmOperationsTable.getManager().getViewer().addSelectionChangedListener(e -> updateDetails());
			vmOperationsTable.getManager().getViewer().addSelectionChangedListener(
					e -> pageContainer.showSelection(vmOperationsTable.getSelection().getItems()));
			MCContextMenuManager vmOperationsHistogramMm = MCContextMenuManager
					.create(vmOperationsTable.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(vmOperationsTable.getManager(), vmOperationsHistogramMm);
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(), vmOperationsTable,
					"VM Operations Histogram Selection", vmOperationsHistogramMm); //$NON-NLS-1$
			vmOperationsHistogramMm.add(vmOperationsFilter.getShowFilterAction());
			vmOperationsHistogramMm.add(vmOperationsFilter.getShowSearchAction());
			vmOperationsFilter.loadState(state.getChild(VM_OPERATIONS_FILTER_ELEMENT));

			tabFolder = new CTabFolder(sash, SWT.NONE);
			toolkit.adapt(tabFolder);
			CTabItem t1 = new CTabItem(tabFolder, SWT.NONE);
			t1.setToolTipText(Messages.VMOPERATION_PAGE_TIMELINE_DESCRIPTION);
			timelineCanvas = new ChartCanvas(tabFolder);
			t1.setText(Messages.PAGES_TIMELINE);
			t1.setControl(timelineCanvas);
			DataPageToolkit.createChartTimestampTooltip(timelineCanvas);

			CTabItem t2 = new CTabItem(tabFolder, SWT.NONE);
			t2.setToolTipText(Messages.VMOPERATION_PAGE_DURATIONS_DESCRIPTION);
			durationCanvas = new ChartCanvas(tabFolder);
			t2.setText(Messages.PAGES_DURATIONS);
			t2.setControl(durationCanvas);
			DataPageToolkit.createChartTooltip(durationCanvas);

			CTabItem t3 = new CTabItem(tabFolder, SWT.NONE);
			t3.setToolTipText(Messages.VMOPERATION_PAGE_EVENT_LOG_DESCRIPTION);
			itemList = LIST.buildWithoutBorder(tabFolder, getTableSettings(state.getChild(LIST_ELEMENT)));
			itemFilter = FilterComponent.createFilterComponent(itemList, itemListFilter,
					getDataSource().getItems().apply(JdkFilters.VM_OPERATIONS),
					pageContainer.getSelectionStore()::getSelections, this::onEventsFilterChange);
			MCContextMenuManager itemListMm = MCContextMenuManager
					.create(itemList.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(itemList.getManager(), itemListMm);
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(), itemList,
					Messages.VMOperationPage_LOG_SELECTION, itemListMm);
			itemListMm.add(itemFilter.getShowFilterAction());
			itemListMm.add(itemFilter.getShowSearchAction());
			itemList.getManager().getViewer().addSelectionChangedListener(
					e -> pageContainer.showSelection(ItemCollectionToolkit.build(itemList.getSelection().get())));
			t3.setText(Messages.PAGES_EVENT_LOG);
			t3.setControl(itemFilter.getComponent());
			itemFilter.loadState(state.getChild(LIST_FILTER_ELEMENT));

			IQuantity max = getItems().getAggregate(JdkAggregators.LONGEST_EVENT);
			// FIXME: Same workaround as in SocketIOPage to include max value
			max = max == null ? UnitLookup.MILLISECOND.quantity(20) : max.add(UnitLookup.MILLISECOND.quantity(20));
			durationChart = new XYChart(UnitLookup.MILLISECOND.quantity(0), max, RendererToolkit.empty(), 180);
			durationChart.setVisibleRange(durationsRange.getStart(), durationsRange.getEnd());
			durationChart.addVisibleRangeListener(r -> durationsRange = r);

			timelineChart = new XYChart(pageContainer.getRecordingRange(), RendererToolkit.empty(), 180);
			timelineChart.setVisibleRange(timelineRange.getStart(), timelineRange.getEnd());
			timelineChart.addVisibleRangeListener(r -> timelineRange = r);

			tabFolder.setSelection(tabFolderIndex);

			PersistableSashForm.loadState(sash, state.getChild(SASH_ELEMENT));

			flavorSelector = FlavorSelector.itemsWithTimerange(form, JdkQueries.VM_OPERATIONS.getFilter(),
					getDataSource().getItems(), pageContainer, this::onInputSelected, this::onShow,
					flavorSelectorState);

			addResultActions(form);

			onFilterChange(vmOperationsHistogramFilter);
			onEventsFilterChange(itemListFilter);

			vmOperationsTable.getManager().setSelectionState(vmOperationsSelection);
			itemList.getManager().setSelectionState(itemListSelection);
		}

		private void onFilterChange(IItemFilter filter) {
			vmOperationsFilter.filterChangeHelper(filter, vmOperationsTable,
					getDataSource().getItems().apply(JdkFilters.VM_OPERATIONS));
			vmOperationsHistogramFilter = filter;
		}

		private void onEventsFilterChange(IItemFilter filter) {
			itemFilter.filterChangeHelper(filter, itemList, getDataSource().getItems().apply(JdkFilters.VM_OPERATIONS));
			itemListFilter = filter;
		}

		@Override
		public void saveTo(IWritableState state) {
			PersistableSashForm.saveState(sash, state.createChild(SASH_ELEMENT));
			vmOperationsTable.getManager().getSettings().saveState(state.createChild(VM_OPERATIONS_ELEMENT));
			itemList.getManager().getSettings().saveState(state.createChild(LIST_ELEMENT));
			vmOperationsFilter.saveState(state.createChild(VM_OPERATIONS_FILTER_ELEMENT));
			itemFilter.saveState(state.createChild(LIST_FILTER_ELEMENT));

			saveToLocal();
		}

		private void saveToLocal() {
			vmOperationsSelection = vmOperationsTable.getManager().getSelectionState();
			itemListSelection = itemList.getManager().getSelectionState();
			tabFolderIndex = tabFolder.getSelectionIndex();
			flavorSelectorState = flavorSelector.getFlavorSelectorState();
		}

		private void onInputSelected(IItemCollection items, IRange<IQuantity> timeRange) {
			selectionItems = items;
			this.timeRange = timeRange;
			vmOperationsTable.show(getItems());

			XYChart timelineChart = new XYChart(timeRange, RendererToolkit.empty(), 180);
			DataPageToolkit.setChart(timelineCanvas, timelineChart, pageContainer::showSelection);
			SelectionStoreActionToolkit.addSelectionStoreRangeActions(pageContainer.getSelectionStore(), timelineChart,
					JfrAttributes.LIFETIME, Messages.VMOperationPage_TIMELINE_SELECTION,
					timelineCanvas.getContextMenu());

			updateDetails();
		}

		private void onShow(Boolean show) {
			IRange<IQuantity> range = show ? timeRange : pageContainer.getRecordingRange();
			timelineChart.setVisibleRange(range.getStart(), range.getEnd());
			updateDetails();
		}

		private IItemCollection getItems() {
			return selectionItems != null ? selectionItems.apply(JdkFilters.VM_OPERATIONS)
					: getDataSource().getItems().apply(JdkFilters.VM_OPERATIONS);
		}

		private void updateDetails() {
			HistogramSelection selection = vmOperationsTable.getSelection();
			IItemCollection selectedItems = selection.getRowCount() == 0 ? getItems() : selection.getItems();

			List<IXDataRenderer> timelineRows = new ArrayList<>();
			List<IXDataRenderer> durationRows = new ArrayList<>();

			timelineRows.add(DataPageToolkit.buildSizeRow(Messages.VMOperationPage_ROW_VM_OPERATIONS,
					JdkAggregators.VM_OPERATION_DURATION.getDescription(), selectedItems,
					JdkAggregators.VM_OPERATION_DURATION, VM_OPERATIONS_COLOR, VMOperationPage::getColor));
			durationRows.add(DataPageToolkit.buildDurationHistogram(Messages.VMOperationPage_ROW_VM_OPERATIONS,
					JdkAggregators.VM_OPERATION_COUNT.getDescription(), selectedItems,
					JdkAggregators.VM_OPERATION_COUNT, VM_OPERATIONS_COLOR));

			timelineChart.setRendererRoot(RendererToolkit.uniformRows(timelineRows));
			DataPageToolkit.setChart(timelineCanvas, timelineChart, pageContainer::showSelection);

			durationChart.setRendererRoot(RendererToolkit.uniformRows(durationRows));
			durationCanvas.setSelectionListener(
					() -> pageContainer.showSelection(ItemRow.getSelection(durationChart, JfrAttributes.DURATION)));
			durationCanvas.setChart(durationChart);
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(), durationChart,
					JfrAttributes.DURATION, Messages.VMOperationPage_DURATION_SELECTION,
					durationCanvas.getContextMenu());
			itemList.show(selectedItems);
		}
	}

	@Override
	public IPageUI display(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
		return new VMOperationsUi(parent, toolkit, pageContainer, state);
	}

	private int tabFolderIndex = 0;
	private SelectionState vmOperationsSelection;
	private SelectionState itemListSelection;
	private IItemFilter vmOperationsHistogramFilter;
	private IItemFilter itemListFilter;
	private IRange<IQuantity> timelineRange;
	private IRange<IQuantity> durationsRange;
	private FlavorSelectorState flavorSelectorState;

	public VMOperationPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
		super(dpd, items, editor);
		timelineRange = editor.getRecordingRange();
		durationsRange = editor.getRecordingRange();
	}

	@Override
	public IItemFilter getDefaultSelectionFilter() {
		return JdkFilters.VM_OPERATIONS;
	}

	private static TableSettings getTableSettings(IState state) {
		if (state == null) {
			return new TableSettings("(duration)", //$NON-NLS-1$
					Arrays.asList(new ColumnSettings(ItemHistogram.KEY_COL_ID, false, 400, null),
							new ColumnSettings("(duration)", //$NON-NLS-1$
									false, 120, false),
							new ColumnSettings("blocked", false, 70, false), //$NON-NLS-1$
							new ColumnSettings("safepoint", false, 70, false), //$NON-NLS-1$
							new ColumnSettings(ItemHistogram.COUNT_COL_ID, false, 120, false),
							new ColumnSettings("(startTime)", false, 120, false), new ColumnSettings("(endTime)", false, //$NON-NLS-1$ //$NON-NLS-2$
									120, false)));
		}
		return new TableSettings(state);
	}

	private static Color getColor(IItem item) {
		return VM_OPERATIONS_SPAN_COLOR;
	}
}
