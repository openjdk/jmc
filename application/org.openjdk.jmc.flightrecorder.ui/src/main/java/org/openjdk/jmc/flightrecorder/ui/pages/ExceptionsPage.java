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

import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.EXCEPTION_MESSAGE;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.EXCEPTION_THROWNCLASS;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
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
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.StateToolkit;
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
import org.openjdk.jmc.flightrecorder.ui.common.CompositeKeyAccessorFactory;
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
import org.openjdk.jmc.flightrecorder.ui.common.ItemRow;
import org.openjdk.jmc.flightrecorder.ui.common.TypeLabelProvider;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStoreActionToolkit;
import org.openjdk.jmc.ui.charts.IQuantitySeries;
import org.openjdk.jmc.ui.charts.IXDataRenderer;
import org.openjdk.jmc.ui.charts.QuantitySeries;
import org.openjdk.jmc.ui.charts.RendererToolkit;
import org.openjdk.jmc.ui.charts.XYChart;
import org.openjdk.jmc.ui.charts.XYDataRenderer;
import org.openjdk.jmc.ui.column.ColumnManager.SelectionState;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.handlers.ActionToolkit;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.ChartCanvas;
import org.openjdk.jmc.ui.misc.PersistableSashForm;

public class ExceptionsPage extends AbstractDataPage {
	public static class ExceptionsPageFactory implements IDataPageFactory {
		@Override
		public String getName(IState state) {
			return Messages.ExceptionsPage_PAGE_NAME;
		}

		@Override
		public ImageDescriptor getImageDescriptor(IState state) {
			return FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.PAGE_EXCEPTIONS);
		}

		@Override
		public String[] getTopics(IState state) {
			return new String[] {JfrRuleTopics.EXCEPTIONS_TOPIC};
		}

		@Override
		public IDisplayablePage createPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
			return new ExceptionsPage(dpd, items, editor);
		}

	}

	private enum HistogramType {
		CLASS, MESSAGE, CLASS_AND_MESSAGE
	}

	private class ExceptionsUi implements IPageUI {

		private static final String EVENTS_FILTER = "eventFilter"; //$NON-NLS-1$
		private static final String PRIMARY_FILTER = "primaryFilter"; //$NON-NLS-1$
		private static final String SECONDARY_FILTER = "secondaryFilter"; //$NON-NLS-1$

		private final IPageContainer pageContainer;
		private final ChartCanvas exceptionChartCanvas;
		private final ItemList eventList;
		private final SashForm sash;
		private final Composite histogramParent;
		private ItemHistogram primaryHistogram;
		private FilterComponent primaryFilter;
		private FilterComponent secondaryFilter;
		private FilterComponent eventFilter;
		private ItemHistogram secondaryHistogram;
		private Supplier<TableSettings> secondaryHistogramSettings;
		private Consumer<IItemCollection> itemConsumerRoot;
		private HistogramType histogramType;
		private XYChart exceptionsChart;
		private IItemCollection selectionItems;
		private IRange<IQuantity> currentRange;
		private CTabFolder tabFolder;
		private FlavorSelector flavorSelector;

		ExceptionsUi(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
			this.pageContainer = pageContainer;
			Form form = DataPageToolkit.createForm(parent, toolkit, getName(), getIcon());
			sash = new SashForm(form.getBody(), SWT.VERTICAL);
			histogramParent = toolkit.createComposite(sash);
			histogramParent.setLayout(new FillLayout(SWT.VERTICAL));
			histogramType = StateToolkit.readEnum(state, HISTGRAM_TYPE, HistogramType.CLASS, HistogramType.class);
			buildHistograms(TableSettings.forState(state.getChild(EXCEPTIONS_TABLE)),
					TableSettings.forState(state.getChild(SECONDARY_EXCEPTIONS_TABLE)));

			tabFolder = new CTabFolder(sash, SWT.NONE);
			toolkit.adapt(tabFolder);
			CTabItem t1 = new CTabItem(tabFolder, SWT.NONE);
			exceptionChartCanvas = new ChartCanvas(tabFolder);
			DataPageToolkit.createChartTimestampTooltip(exceptionChartCanvas);
			t1.setText(Messages.PAGES_TIMELINE);
			t1.setControl(exceptionChartCanvas);
			tabFolder.setSelection(tabFolderIndex);

			exceptionsChart = createExceptionsChart(pageContainer);
			hookUpExceptionsChart();

			CTabItem t2 = new CTabItem(tabFolder, SWT.NONE);
			eventList = LIST.buildWithoutBorder(tabFolder, TableSettings.forState(state.getChild(LIST_ELEMENT)));
			MCContextMenuManager eventListMm = MCContextMenuManager
					.create(eventList.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(eventList.getManager(), eventListMm);
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(), eventList,
					Messages.ExceptionsPage_THROWABLES_LOG_SELECTION, eventListMm);
			eventList.getManager().getViewer().addSelectionChangedListener(
					e -> pageContainer.showSelection(ItemCollectionToolkit.build(eventList.getSelection().get())));
			t2.setText(Messages.PAGES_EVENT_LOG);
			eventFilter = FilterComponent.createFilterComponent(eventList, eventListFilter,
					getDataSource().getItems().apply(JdkFilters.THROWABLES),
					pageContainer.getSelectionStore()::getSelections,
					(Consumer<IItemFilter>) this::onEventFilterChange);
			eventListMm.add(eventFilter.getShowFilterAction());
			eventListMm.add(eventFilter.getShowSearchAction());
			t2.setControl(eventFilter.getComponent());
			eventFilter.loadState(state.getChild(EVENTS_FILTER));
			onEventFilterChange(eventListFilter);
			eventList.getManager().setSelectionState(eventListSelection);

			PersistableSashForm.loadState(sash, state.getChild(SASH_ELEMENT));

			flavorSelector = FlavorSelector.itemsWithTimerange(form, JdkFilters.THROWABLES, getDataSource().getItems(),
					pageContainer, this::onInputSelected, this::onShow, flavorSelectorState);

			form.getToolBarManager().appendToGroup(DataPageToolkit.FORM_TOOLBAR_PAGE_SETUP, buildHistogramTypeAction(
					HistogramType.CLASS, Messages.ExceptionsPage_GROUP_BY_CLASS_ACTION,
					FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.ICON_EXCEPTION_BY_CLASS)));
			form.getToolBarManager().appendToGroup(DataPageToolkit.FORM_TOOLBAR_PAGE_SETUP, buildHistogramTypeAction(
					HistogramType.MESSAGE, Messages.ExceptionsPage_GROUP_BY_MESSAGE_ACTION,
					FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.ICON_EXCEPTION_BY_MESSAGE)));
			form.getToolBarManager().appendToGroup(DataPageToolkit.FORM_TOOLBAR_PAGE_SETUP,
					buildHistogramTypeAction(HistogramType.CLASS_AND_MESSAGE,
							Messages.ExceptionsPage_GROUP_BY_CLASS_AND_MESSAGE_ACTION, FlightRecorderUI.getDefault()
									.getMCImageDescriptor(ImageConstants.ICON_EXCEPTION_BY_CLASS_AND_MESSAGE)));

			addResultActions(form);

			form.getToolBarManager().update(true);
		}

		private XYChart createExceptionsChart(IPageContainer pageContainer) {
			XYChart chart = new XYChart(pageContainer.getRecordingRange(), RendererToolkit.empty(), 100);
			chart.setVisibleRange(timelineRange.getStart(), timelineRange.getEnd());
			chart.addVisibleRangeListener(r -> timelineRange = r);
			return chart;
		}

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
			if (histogramType == HistogramType.CLASS_AND_MESSAGE) {
				primaryHistogram = HISTOGRAM.buildWithoutBorder(histogramParent,
						Messages.ExceptionsPage_CLASS_AND_MESSAGE, UnitLookup.UNKNOWN, CLASS_AND_MESSAGE_AF,
						primarySettings);
				primaryFilter = FilterComponent.createFilterComponent(primaryHistogram,
						primaryTableFilter.get(histogramType), getDataSource().getItems().apply(JdkFilters.THROWABLES),
						pageContainer.getSelectionStore()::getSelections, this::onFilterChange);
				secondaryHistogram = null;
				secondaryHistogramSettings = () -> secondarySettings;
				secondaryFilter = null;
				onFilterChange(primaryTableFilter.get(histogramType));
				primaryHistogram.getManager().setSelectionState(primaryTableSelection.get(histogramType));
				itemConsumerRoot = ItemHistogramWithInput.chain(primaryHistogram, this::updateChartAndListDetails);
			} else {
				SashForm s2 = new SashForm(histogramParent, SWT.VERTICAL);
				IAttribute<?> masterAttr = histogramType == HistogramType.CLASS ? EXCEPTION_THROWNCLASS
						: EXCEPTION_MESSAGE;
				IAttribute<?> slaveAttr = histogramType == HistogramType.CLASS ? EXCEPTION_MESSAGE
						: EXCEPTION_THROWNCLASS;
				primaryHistogram = HISTOGRAM.buildWithoutBorder(s2, masterAttr, primarySettings);
				primaryFilter = FilterComponent.createFilterComponent(primaryHistogram,
						primaryTableFilter.get(histogramType), getDataSource().getItems().apply(JdkFilters.THROWABLES),
						pageContainer.getSelectionStore()::getSelections, this::onFilterChange);
				secondaryHistogram = HISTOGRAM.buildWithoutBorder(s2, slaveAttr, secondarySettings);
				secondaryFilter = FilterComponent.createFilterComponent(secondaryHistogram,
						secondaryTableFilter.get(histogramType),
						getDataSource().getItems().apply(JdkFilters.THROWABLES),
						pageContainer.getSelectionStore()::getSelections, this::onSecondaryFilterChange);
				secondaryHistogramSettings = secondaryHistogram.getManager()::getSettings;
				onFilterChange(primaryTableFilter.get(histogramType));
				primaryHistogram.getManager().setSelectionState(primaryTableSelection.get(histogramType));
				onSecondaryFilterChange(secondaryTableFilter.get(histogramType));
				secondaryHistogram.getManager().setSelectionState(secondaryTableSelection.get(histogramType));
				itemConsumerRoot = ItemHistogramWithInput.chain(primaryHistogram, this::updateChartAndListDetails,
						secondaryHistogram);
				addContextMenu(secondaryHistogram, secondaryFilter);
				secondaryFilter.loadState(getState().getChild(SECONDARY_FILTER));
			}
			addContextMenu(primaryHistogram, primaryFilter);
			primaryFilter.loadState(getState().getChild(PRIMARY_FILTER));
			histogramParent.layout();
		}

		private void onFilterChange(IItemFilter filter) {
			primaryFilter.filterChangeHelper(filter, primaryHistogram,
					getDataSource().getItems().apply(JdkFilters.THROWABLES));
			if (secondaryFilter != null) {
				secondaryFilter.notifyListener();
			}
			primaryTableFilter.put(histogramType, filter);
		}

		private void onSecondaryFilterChange(IItemFilter filter) {
			secondaryFilter.filterChangeHelper(filter, secondaryHistogram,
					getDataSource().getItems().apply(JdkFilters.THROWABLES));
			secondaryTableFilter.put(histogramType, filter);
		}

		private void onEventFilterChange(IItemFilter filter) {
			eventFilter.filterChangeHelper(filter, eventList, getDataSource().getItems().apply(JdkFilters.THROWABLES));
			eventListFilter = filter;
		}

		private void addContextMenu(ItemHistogram h, FilterComponent filter) {
			MCContextMenuManager mm = MCContextMenuManager.create(h.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(h.getManager(), mm);
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(), h,
					Messages.ExceptionsPage_THROWABLES_HISTOGRAM_SELECTION, mm);
			if (filter != null) {
				mm.add(filter.getShowFilterAction());
				mm.add(filter.getShowSearchAction());
			}
		}

		@Override
		public void saveTo(IWritableState state) {
			StateToolkit.writeEnum(state, HISTGRAM_TYPE, histogramType);
			PersistableSashForm.saveState(sash, state.createChild(SASH_ELEMENT));
			primaryHistogram.getManager().getSettings().saveState(state.createChild(EXCEPTIONS_TABLE));
			Optional.ofNullable(secondaryHistogramSettings.get())
					.ifPresent(settings -> settings.saveState(state.createChild(SECONDARY_EXCEPTIONS_TABLE)));
			eventList.getManager().getSettings().saveState(state.createChild(LIST_ELEMENT));
			primaryFilter.saveState(state.createChild(PRIMARY_FILTER));
			// The secondary histogram and filter does not exist when the page is configured "By Class and Message"
			if (secondaryFilter != null) {
				secondaryFilter.saveState(state.createChild(SECONDARY_FILTER));
			}
			eventFilter.saveState(state.createChild(EVENTS_FILTER));

			saveToLocal();
		}

		private void saveToLocal() {
			eventListSelection = eventList.getManager().getSelectionState();
			primaryTableSelection.put(histogramType, primaryHistogram.getManager().getSelectionState());
			if (secondaryHistogram != null) {
				secondaryTableSelection.put(histogramType, secondaryHistogram.getManager().getSelectionState());
			}
			tabFolderIndex = tabFolder.getSelectionIndex();
			flavorSelectorState = flavorSelector.getFlavorSelectorState();
		}

		private void hookUpExceptionsChart() {
			DataPageToolkit.setChart(exceptionChartCanvas, exceptionsChart,
					selection -> pageContainer.showSelection(selection));
			SelectionStoreActionToolkit.addSelectionStoreRangeActions(pageContainer.getSelectionStore(),
					exceptionsChart, JfrAttributes.LIFETIME, Messages.ExceptionsPage_THROWABLES_TIMELINE_SELECTION,
					exceptionChartCanvas.getContextMenu());
		}

		private void onShow(Boolean show) {
			IRange<IQuantity> range = show ? currentRange : pageContainer.getRecordingRange();
			exceptionsChart.setVisibleRange(range.getStart(), range.getEnd());
			hookUpExceptionsChart();
			refreshPageItems();
		}

		private void onInputSelected(IItemCollection items, IRange<IQuantity> timeRange) {
			this.selectionItems = items;
			this.currentRange = timeRange;
			refreshPageItems();
		}

		private void refreshPageItems() {
			IItemCollection items = selectionItems != null ? selectionItems : getDataSource().getItems();
			itemConsumerRoot.accept(items.apply(JdkFilters.THROWABLES));
		}

		private void updateChartAndListDetails(IItemCollection selectedItems) {
			List<IXDataRenderer> rows = new ArrayList<>();
			XYDataRenderer xyRenderer = new XYDataRenderer(UnitLookup.NUMBER.getDefaultUnit().quantity(0),
					Messages.ExceptionsPage_ROW_STATISTICS, JdkAttributes.EXCEPTION_THROWABLES_COUNT.getDescription());

			// FIXME: What should we do with the selection input here?
			IItemCollection statsItems = getDataSource().getItems().apply(JdkQueries.THROWABLES_STATISTICS.getFilter());
			IQuantitySeries<?> adjustedStatsSeries = zeroIndexStatisticsEvents(statsItems);
			xyRenderer.addLineChart(JdkAttributes.EXCEPTION_THROWABLES_COUNT.getName(), adjustedStatsSeries,
					STATISTICS_EVENT_COLOR, false);
			rows.add(new ItemRow(Messages.ExceptionsPage_ROW_STATISTICS, Messages.ExceptionsPage_ROW_STATISTICS,
					xyRenderer, statsItems));

			IItemCollection exceptionItems = selectedItems.apply(JdkFilters.EXCEPTIONS);
			if (exceptionItems.hasItems()) {
				rows.add(DataPageToolkit.buildTimestampHistogram(Messages.ExceptionsPage_ROW_EXCEPTIONS,
						JdkAggregators.EXCEPTIONS_COUNT.getDescription(), exceptionItems,
						JdkAggregators.EXCEPTIONS_COUNT, EXCEPTIONS_EVENT_COLOR));
			}
			IItemCollection errorItems = selectedItems.apply(JdkFilters.ERRORS);
			if (errorItems.hasItems()) {
				rows.add(DataPageToolkit.buildTimestampHistogram(Messages.ExceptionsPage_ROW_ERRORS,
						JdkAggregators.ERROR_COUNT.getDescription(), errorItems, JdkAggregators.ERROR_COUNT,
						ERRORS_EVENT_COLOR));
			}

			exceptionChartCanvas.replaceRenderer(RendererToolkit.uniformRows(rows));
			eventList.show(selectedItems);
			pageContainer.showSelection(selectedItems);
		}

		private IQuantitySeries<?> zeroIndexStatisticsEvents(IItemCollection statsItems) {
			// will only be null if event collection is empty - in that case no new value sets are generated
			IQuantity first = findFirstStatEvent(statsItems);
			List<IQuantity> xValues = new ArrayList<>();
			List<IQuantity> yValues = new ArrayList<>();
			for (IItemIterable next : statsItems) {
				IMemberAccessor<IQuantity, IItem> xValueAccessor = JfrAttributes.END_TIME.getAccessor(next.getType());
				IMemberAccessor<IQuantity, IItem> yValueAccessor = JdkAttributes.EXCEPTION_THROWABLES_COUNT
						.getAccessor(next.getType());
				for (IItem item : next) {
					xValues.add(xValueAccessor.getMember(item));
					yValues.add(yValueAccessor.getMember(item).subtract(first));
				}
			}
			IQuantitySeries<?> adjustedStatsSeries = QuantitySeries.all(xValues, yValues);
			return adjustedStatsSeries;
		}

		private IQuantity findFirstStatEvent(IItemCollection stats) {
			IQuantity firstValue = null;
			for (IItemIterable next : stats) {
				IMemberAccessor<IQuantity, IItem> yValueAccessor = JdkAttributes.EXCEPTION_THROWABLES_COUNT
						.getAccessor(next.getType());
				for (IItem item : next) {
					IQuantity itemValue = yValueAccessor.getMember(item);
					if (firstValue == null || firstValue.subtract(itemValue).longValue() > 0) {
						firstValue = itemValue;
					}
				}
			}
			return firstValue;
		}
	}

	private static final String SASH_ELEMENT = "sash"; //$NON-NLS-1$
	private static final String LIST_ELEMENT = "eventList"; //$NON-NLS-1$
	private static final String EXCEPTIONS_TABLE = "exceptionsTable"; //$NON-NLS-1$
	private static final String SECONDARY_EXCEPTIONS_TABLE = "secondaryExceptionsTable"; //$NON-NLS-1$
	private static final String HISTGRAM_TYPE = "histogramType"; //$NON-NLS-1$
	private static final Color STATISTICS_EVENT_COLOR = TypeLabelProvider
			.getColorOrDefault(JdkTypeIDs.THROWABLES_STATISTICS);
	private static final Color EXCEPTIONS_EVENT_COLOR = TypeLabelProvider
			.getColorOrDefault(JdkTypeIDs.EXCEPTIONS_THROWN);
	private static final Color ERRORS_EVENT_COLOR = TypeLabelProvider.getColorOrDefault(JdkTypeIDs.ERRORS_THROWN);
	private static final IAccessorFactory<IDisplayable> CLASS_AND_MESSAGE_AF = CompositeKeyAccessorFactory.displayable(
			" : ", JdkAttributes.EXCEPTION_THROWNCLASS, //$NON-NLS-1$
			JdkAttributes.EXCEPTION_MESSAGE);
	private static final ItemHistogramBuilder HISTOGRAM = new ItemHistogramBuilder();
	private static final ItemListBuilder LIST = new ItemListBuilder();

	static {
		HISTOGRAM.addCountColumn();
		LIST.addColumn(JfrAttributes.START_TIME);
		LIST.addColumn(JdkAttributes.EXCEPTION_THROWNCLASS);
		LIST.addColumn(JfrAttributes.EVENT_THREAD);
		LIST.addColumn(JdkAttributes.EXCEPTION_MESSAGE);
		LIST.addColumn(JfrAttributes.END_TIME);
		LIST.addColumn(JfrAttributes.EVENT_TYPE);
	}

	@Override
	public IPageUI display(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
		return new ExceptionsUi(parent, toolkit, pageContainer, state);
	}

	private Map<HistogramType, SelectionState> primaryTableSelection;
	private Map<HistogramType, SelectionState> secondaryTableSelection;
	private SelectionState eventListSelection;
	private Map<HistogramType, IItemFilter> primaryTableFilter;
	private Map<HistogramType, IItemFilter> secondaryTableFilter;
	private IItemFilter eventListFilter;
	private IRange<IQuantity> timelineRange;
	private int tabFolderIndex = 0;
	public FlavorSelectorState flavorSelectorState;

	public ExceptionsPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
		super(dpd, items, editor);
		timelineRange = editor.getRecordingRange();
		primaryTableSelection = new HashMap<>();
		secondaryTableSelection = new HashMap<>();
		primaryTableFilter = new HashMap<>();
		secondaryTableFilter = new HashMap<>();
	}

	@Override
	public IItemFilter getDefaultSelectionFilter() {
		return JdkFilters.THROWABLES;
	}
}
