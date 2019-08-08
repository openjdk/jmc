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

import static org.openjdk.jmc.flightrecorder.jdk.JdkQueries.CODE_CACHE_ADAPTORS_SEGMENTED;
import static org.openjdk.jmc.flightrecorder.jdk.JdkQueries.CODE_CACHE_ENTRIES;
import static org.openjdk.jmc.flightrecorder.jdk.JdkQueries.CODE_CACHE_ENTRIES_SEGMENTED;
import static org.openjdk.jmc.flightrecorder.jdk.JdkQueries.CODE_CACHE_METHODS_SEGMENTED;
import static org.openjdk.jmc.flightrecorder.jdk.JdkQueries.CODE_CACHE_UNALLOCATED;
import static org.openjdk.jmc.flightrecorder.jdk.JdkQueries.CODE_CACHE_UNALLOCATED_SEGMENTED;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.version.JavaVersion;
import org.openjdk.jmc.common.version.JavaVersionSupport;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
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
import org.openjdk.jmc.flightrecorder.ui.common.ItemList;
import org.openjdk.jmc.flightrecorder.ui.common.ItemList.ItemListBuilder;
import org.openjdk.jmc.flightrecorder.ui.common.ItemRow;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStoreActionToolkit;
import org.openjdk.jmc.ui.charts.AWTChartToolkit;
import org.openjdk.jmc.ui.charts.ISpanSeries;
import org.openjdk.jmc.ui.charts.IXDataRenderer;
import org.openjdk.jmc.ui.charts.QuantitySeries;
import org.openjdk.jmc.ui.charts.RendererToolkit;
import org.openjdk.jmc.ui.charts.SpanRenderer;
import org.openjdk.jmc.ui.charts.XYChart;
import org.openjdk.jmc.ui.column.ColumnManager.SelectionState;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.ChartCanvas;
import org.openjdk.jmc.ui.misc.PersistableSashForm;
import org.openjdk.jmc.ui.misc.SWTColorToolkit;

public class CodeCachePage extends AbstractDataPage {

	public static class CodeCachePageFactory implements IDataPageFactory {

		@Override
		public String getName(IState state) {
			return Messages.CodeCachePage_PAGE_NAME;
		}

		@Override
		public ImageDescriptor getImageDescriptor(IState state) {
			return FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.PAGE_COMPILATIONS);
		}

		@Override
		public String[] getTopics(IState state) {
			return new String[] {JfrRuleTopics.CODE_CACHE_TOPIC};
		}

		@Override
		public IDisplayablePage createPage(IPageDefinition definition, StreamModel items, IPageContainer editor) {
			return new CodeCachePage(definition, items, editor);
		}

	}

	private static ColumnLabelProvider LEGEND_LP = new ColumnLabelProvider() {

		@Override
		public String getText(Object element) {
			return getText(element, IDescribable::getName);
		}

		@Override
		public String getToolTipText(Object element) {
			return getText(element, IDescribable::getDescription);
		};

		private String getText(Object element, Function<IDescribable, String> accessor) {
			for (IAttribute<?> a : CODE_CACHE_ENTRIES.getAttributes()) {
				if (a.getIdentifier().equals(element)) {
					return accessor.apply(a);
				}
			}
			for (IAttribute<?> a : CODE_CACHE_UNALLOCATED.getAttributes()) {
				if (a.getIdentifier().equals(element)) {
					return accessor.apply(a);
				}
			}
			for (IAttribute<?> a : CODE_CACHE_UNALLOCATED_SEGMENTED.getAttributes()) {
				if (a.getIdentifier().equals(element)) {
					return accessor.apply(a);
				}
			}
			for (IAttribute<?> a : CODE_CACHE_ENTRIES_SEGMENTED.getAttributes()) {
				if (a.getIdentifier().equals(element)) {
					return accessor.apply(a);
				}
			}
			for (IAttribute<?> a : CODE_CACHE_ADAPTORS_SEGMENTED.getAttributes()) {
				if (a.getIdentifier().equals(element)) {
					return accessor.apply(a);
				}
			}
			for (IAttribute<?> a : CODE_CACHE_METHODS_SEGMENTED.getAttributes()) {
				if (a.getIdentifier().equals(element)) {
					return accessor.apply(a);
				}
			}
			switch ((String) element) {
			case TOTAL_SWEPT_ID:
				return accessor.apply(JdkAggregators.SWEEP_METHOD_SUM);
			case TOTAL_FLUSHED_ID:
				return accessor.apply(JdkAggregators.SWEEP_FLUSHED_SUM);
			case TOTAL_RECLAIMED_ID:
				return accessor.apply(JdkAggregators.SWEEP_RECLAIMED_SUM);
			case TOTAL_ZOMBIFIED_ID:
				return accessor.apply(JdkAggregators.SWEEP_ZOMBIFIED_SUM);
			case SWEEPS_ID:
				return accessor.apply(SWEEPS_DESCRIPTION);
			}
			return null;
		};

		@Override
		public Image getImage(Object element) {
			if (element.equals(SWEEPS_ID)) {
				return SWTColorToolkit.getColorThumbnail(SWTColorToolkit.asRGB(SWEEP_BACKDROP_COLOR));
			} else {
				return SWTColorToolkit
						.getColorThumbnail(SWTColorToolkit.asRGB(DataPageToolkit.getFieldColor((String) element)));
			}

		};
	};

	private static final ItemListBuilder CODE_SWEEP_LIST = new ItemListBuilder();

	static {
		CODE_SWEEP_LIST.addColumn(JfrAttributes.START_TIME);
		CODE_SWEEP_LIST.addColumn(JfrAttributes.DURATION);
		CODE_SWEEP_LIST.addColumn(JdkAttributes.SWEEP_INDEX);
		CODE_SWEEP_LIST.addColumn(JdkAttributes.SWEEP_FRACTION_INDEX);
		CODE_SWEEP_LIST.addColumn(JdkAttributes.SWEEP_METHOD_FLUSHED);
		CODE_SWEEP_LIST.addColumn(JdkAttributes.SWEEP_METHOD_RECLAIMED);
		CODE_SWEEP_LIST.addColumn(JdkAttributes.SWEEP_METHOD_SWEPT);
		CODE_SWEEP_LIST.addColumn(JdkAttributes.SWEEP_METHOD_ZOMBIFIED);
		CODE_SWEEP_LIST.addColumn(JfrAttributes.EVENT_THREAD);
	}

	private static final Color SWEEP_BACKDROP_COLOR = new Color(0, 100, 200, 80);
	private static final String SASH = "sash"; //$NON-NLS-1$
	private static final String CODE_SWEEP_TABLE = "codeSweepTable"; //$NON-NLS-1$
	private static final String CODE_SWEEP_FILTER = "codeSweepFilter"; //$NON-NLS-1$
	private static final String CHART = "chart"; //$NON-NLS-1$
	private static final String SERIES = "series"; //$NON-NLS-1$
	private static final String ID_ATTRIBUTE = "id"; //$NON-NLS-1$
	private static final String TOTAL_SWEPT_ID = "totalMethodsSwept"; //$NON-NLS-1$
	private static final String TOTAL_FLUSHED_ID = "totalMethodsFlushed"; //$NON-NLS-1$
	private static final String TOTAL_RECLAIMED_ID = "totalMethodsReclaimed"; //$NON-NLS-1$
	private static final String TOTAL_ZOMBIFIED_ID = "totalMethodsZombified"; //$NON-NLS-1$
	private static final String SWEEPS_ID = "codeCacheSweeps"; //$NON-NLS-1$
	private static final IDescribable SWEEPS_DESCRIPTION = new IDescribable() {

		@Override
		public String getName() {
			return Messages.CodeCachePage_OVERLAY_SWEEPS;
		}

		@Override
		public String getDescription() {
			return Messages.CodeCachePage_OVERLAYS_SWEEPS_DESC;
		}
	};

	private class CodeCachePageUI implements IPageUI {

		private final SashForm sash;
		private final IPageContainer pageContainer;
		private final CheckboxTableViewer chartLegend;
		private final ChartCanvas chartCanvas;
		private final ItemList codeSweepTable;
		private final FilterComponent codeSweepFilter;
		private IItemCollection selectionItems;
		private XYChart chart;
		private IRange<IQuantity> currentRange;
		private FlavorSelector flavorSelector;

		CodeCachePageUI(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
			this.pageContainer = pageContainer;
			Form form = DataPageToolkit.createForm(parent, toolkit, getName(), getIcon());
			sash = new SashForm(form.getBody(), SWT.VERTICAL);
			toolkit.adapt(sash);

			Composite chartContainer = toolkit.createComposite(sash);
			chartContainer.setLayout(new GridLayout(2, false));
			chartCanvas = new ChartCanvas(chartContainer);
			chart = new XYChart(pageContainer.getRecordingRange(), RendererToolkit.empty(), 180);
			chart.setVisibleRange(timelineRange.getStart(), timelineRange.getEnd());
			chart.addVisibleRangeListener(r -> timelineRange = r);
			chartCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			DataPageToolkit.setChart(chartCanvas, chart, pageContainer::showSelection);
			SelectionStoreActionToolkit.addSelectionStoreRangeActions(pageContainer.getSelectionStore(), chart,
					JfrAttributes.LIFETIME, Messages.CodeCachePage_SWEEPS_TIMELINE_SELECTION,
					chartCanvas.getContextMenu());

			chartLegend = CheckboxTableViewer.newCheckList(chartContainer, SWT.BORDER);
			GridData gd = new GridData(SWT.FILL, SWT.FILL, false, true);
			gd.widthHint = 180;
			chartLegend.getTable().setLayoutData(gd);
			chartLegend.setContentProvider(ArrayContentProvider.getInstance());
			chartLegend.setLabelProvider(LEGEND_LP);
			chartLegend.addCheckStateListener(e -> buildChart());
			chartLegend.addSelectionChangedListener(e -> buildChart());
			ColumnViewerToolTipSupport.enableFor(chartLegend);
			List<Object> chartSeries = new ArrayList<>();
			JavaVersion version = RulesToolkit.getJavaVersion(getDataSource().getItems()); 
			if (version != null && version.isGreaterOrEqualThan(JavaVersionSupport.JDK_9)) {
				CODE_CACHE_UNALLOCATED_SEGMENTED.getAttributes().stream().map(IAttribute::getIdentifier)
						.forEach(chartSeries::add);
				CODE_CACHE_ENTRIES_SEGMENTED.getAttributes().stream().map(IAttribute::getIdentifier)
						.forEach(chartSeries::add);
				CODE_CACHE_ADAPTORS_SEGMENTED.getAttributes().stream().map(IAttribute::getIdentifier)
						.forEach(chartSeries::add);
				CODE_CACHE_METHODS_SEGMENTED.getAttributes().stream().map(IAttribute::getIdentifier)
						.forEach(chartSeries::add);
			} else {
				CODE_CACHE_ENTRIES.getAttributes().stream().map(IAttribute::getIdentifier).forEach(chartSeries::add);
				CODE_CACHE_UNALLOCATED.getAttributes().stream().map(IAttribute::getIdentifier)
						.forEach(chartSeries::add);
			}
			chartSeries.addAll(
					Arrays.asList(TOTAL_SWEPT_ID, TOTAL_FLUSHED_ID, TOTAL_RECLAIMED_ID, TOTAL_ZOMBIFIED_ID, SWEEPS_ID));
			chartLegend.setInput(chartSeries.toArray());
			IState chartState = state.getChild(CHART);
			if (chartState != null) {
				for (IState c : chartState.getChildren()) {
					chartLegend.setChecked(c.getAttribute(ID_ATTRIBUTE), true);
				}
			}

			codeSweepTable = CODE_SWEEP_LIST.buildWithoutBorder(sash,
					TableSettings.forState(state.getChild(CODE_SWEEP_TABLE)));
			codeSweepTable.getManager().getViewer().addSelectionChangedListener(
					e -> pageContainer.showSelection(ItemCollectionToolkit.build(codeSweepTable.getSelection().get())));
			codeSweepFilter = FilterComponent.createFilterComponent(codeSweepTable, codeSweepFilterState,
					getDataSource().getItems().apply(JdkFilters.SWEEP_CODE_CACHE),
					pageContainer.getSelectionStore()::getSelections, this::onFilterChange);
			MCContextMenuManager mm = MCContextMenuManager.create(codeSweepTable.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(codeSweepTable.getManager(), mm);
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(), codeSweepTable,
					Messages.CodeCachePage_SWEEP_TABLE_SELECTION, mm);
			mm.add(codeSweepFilter.getShowFilterAction());
			mm.add(codeSweepFilter.getShowSearchAction());
			codeSweepFilter.loadState(state.getChild(CODE_SWEEP_FILTER));

			DataPageToolkit.createChartTimestampTooltip(chartCanvas);

			PersistableSashForm.loadState(sash, state.getChild(SASH));
			flavorSelector = FlavorSelector.itemsWithTimerange(form, JdkFilters.CODE_CACHE, getDataSource().getItems(),
					pageContainer, this::onInputSelected, this::onShow, flavorSelectorState);
			addResultActions(form);
			onFilterChange(codeSweepFilterState);
			codeSweepTable.getManager().setSelectionState(codeSweepSelectionState);
		}

		private Optional<ItemRow> buildBarChart(
			IItemCollection items, IAggregator<IQuantity, ?> aggregator, String id) {
			if (chartLegend.getChecked(id)) {
				return Optional.of(DataPageToolkit.buildTimestampHistogram(aggregator.getName(),
						aggregator.getDescription(), items, aggregator, DataPageToolkit.getFieldColor(id)));
			}
			return Optional.empty();
		}

		private void buildChart() {
			IItemCollection itemsInRange = getItems();
			List<IXDataRenderer> rows = new ArrayList<>();

			// FIXME: All these descriptions are null, add strings describing each code cache row
			Predicate<IAttribute<IQuantity>> legendFilter = a -> chartLegend.getChecked(a.getIdentifier());
			DataPageToolkit
					.buildLinesRow(Messages.CodeCachePage_ROW_UNALLOCATED, null, itemsInRange, false,
							CODE_CACHE_UNALLOCATED, legendFilter, UnitLookup.BYTE.quantity(0), null)
					.ifPresent(rows::add);
			DataPageToolkit
					.buildLinesRow(Messages.CodeCachePage_ROW_UNALLOCATED, null, itemsInRange, false,
							CODE_CACHE_UNALLOCATED_SEGMENTED, legendFilter, UnitLookup.BYTE.quantity(0), null)
					.ifPresent(rows::add);
			DataPageToolkit.buildLinesRow(Messages.CodeCachePage_ROW_ENTRIES, null, itemsInRange, false,
					CODE_CACHE_ENTRIES, legendFilter, UnitLookup.NUMBER_UNITY.quantity(0), null).ifPresent(rows::add);
			DataPageToolkit
					.buildLinesRow(Messages.CodeCachePage_ROW_ENTRIES, null, itemsInRange, false,
							CODE_CACHE_ENTRIES_SEGMENTED, legendFilter, UnitLookup.NUMBER_UNITY.quantity(0), null)
					.ifPresent(rows::add);
			DataPageToolkit
					.buildLinesRow(Messages.CodeCachePage_ROW_ADAPTORS, null, itemsInRange, false,
							CODE_CACHE_ADAPTORS_SEGMENTED, legendFilter, UnitLookup.NUMBER_UNITY.quantity(0), null)
					.ifPresent(rows::add);
			DataPageToolkit
					.buildLinesRow(Messages.CodeCachePage_ROW_METHODS, null, itemsInRange, false,
							CODE_CACHE_METHODS_SEGMENTED, legendFilter, UnitLookup.NUMBER_UNITY.quantity(0), null)
					.ifPresent(rows::add);
			IItemCollection sweepEvents = itemsInRange.apply(JdkFilters.SWEEP_CODE_CACHE);
			buildBarChart(sweepEvents, JdkAggregators.SWEEP_METHOD_SUM, TOTAL_SWEPT_ID).ifPresent(rows::add);
			buildBarChart(sweepEvents, JdkAggregators.SWEEP_FLUSHED_SUM, TOTAL_FLUSHED_ID).ifPresent(rows::add);
			buildBarChart(sweepEvents, JdkAggregators.SWEEP_RECLAIMED_SUM, TOTAL_RECLAIMED_ID).ifPresent(rows::add);
			buildBarChart(sweepEvents, JdkAggregators.SWEEP_ZOMBIFIED_SUM, TOTAL_ZOMBIFIED_ID).ifPresent(rows::add);

			IXDataRenderer root = RendererToolkit.uniformRows(rows);
			if (chartLegend.getChecked(SWEEPS_ID)) {
				ISpanSeries<IItem> sweepBackdrop = QuantitySeries.max(sweepEvents, JfrAttributes.START_TIME,
						JfrAttributes.END_TIME);
				root = RendererToolkit.layers(root,
						new ItemRow(null, null,
								SpanRenderer.build(sweepBackdrop, AWTChartToolkit.staticColor(SWEEP_BACKDROP_COLOR)),
								sweepEvents));
			}
			chartCanvas.replaceRenderer(root);
		}

		private void onShow(Boolean show) {
			IRange<IQuantity> range = show ? currentRange : pageContainer.getRecordingRange();
			chart.setVisibleRange(range.getStart(), range.getEnd());
			buildChart();
		}

		private void onInputSelected(IItemCollection items, IRange<IQuantity> timeRange) {
			this.currentRange = timeRange;
			selectionItems = items;
			codeSweepTable.show(getItems().apply(JdkFilters.SWEEP_CODE_CACHE));
			buildChart();
		}

		private IItemCollection getItems() {
			return selectionItems != null ? selectionItems.apply(JdkFilters.CODE_CACHE)
					: getDataSource().getItems().apply(JdkFilters.CODE_CACHE);
		}

		private void onFilterChange(IItemFilter filter) {
			codeSweepFilter.filterChangeHelper(filter, codeSweepTable,
					getDataSource().getItems().apply(JdkFilters.SWEEP_CODE_CACHE));
			codeSweepFilterState = filter;
		}

		@Override
		public void saveTo(IWritableState state) {
			PersistableSashForm.saveState(sash, state.createChild(SASH));
			IWritableState chartState = state.createChild(CHART);
			codeSweepTable.getManager().getSettings().saveState(state.createChild(CODE_SWEEP_TABLE));
			codeSweepFilter.saveState(state.createChild(CODE_SWEEP_FILTER));
			for (Object o : chartLegend.getCheckedElements()) {
				chartState.createChild(SERIES).putString(ID_ATTRIBUTE, ((String) o));
			}

			saveToLocal();
		}

		private void saveToLocal() {
			codeSweepSelectionState = codeSweepTable.getManager().getSelectionState();
			flavorSelectorState = flavorSelector.getFlavorSelectorState();
		}
	}

	private IItemFilter codeSweepFilterState;
	private SelectionState codeSweepSelectionState;
	private IRange<IQuantity> timelineRange;
	private FlavorSelectorState flavorSelectorState;

	public CodeCachePage(IPageDefinition defintion, StreamModel items, IPageContainer editor) {
		super(defintion, items, editor);
		timelineRange = editor.getRecordingRange();
	}

	@Override
	public IItemFilter getDefaultSelectionFilter() {
		return ItemFilters.type(JdkTypeIDs.CODE_CACHE_STATISTICS, JdkTypeIDs.SWEEP_CODE_CACHE);
	}

	@Override
	public IPageUI display(Composite parent, FormToolkit toolkit, IPageContainer editor, IState state) {
		return new CodeCachePageUI(parent, toolkit, editor, state);
	}

}
