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
package org.openjdk.jmc.flightrecorder.ext.jfx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.ext.jfx.JfxVersionUtil.JavaFxEventAvailability;
import org.openjdk.jmc.flightrecorder.ui.IDataPageFactory;
import org.openjdk.jmc.flightrecorder.ui.IDisplayablePage;
import org.openjdk.jmc.flightrecorder.ui.IPageContainer;
import org.openjdk.jmc.flightrecorder.ui.IPageDefinition;
import org.openjdk.jmc.flightrecorder.ui.IPageUI;
import org.openjdk.jmc.flightrecorder.ui.ItemCollectionToolkit;
import org.openjdk.jmc.flightrecorder.ui.StreamModel;
import org.openjdk.jmc.flightrecorder.ui.common.AbstractDataPage;
import org.openjdk.jmc.flightrecorder.ui.common.AggregationGrid;
import org.openjdk.jmc.flightrecorder.ui.common.DataPageToolkit;
import org.openjdk.jmc.flightrecorder.ui.common.FilterComponent;
import org.openjdk.jmc.flightrecorder.ui.common.FlavorSelector;
import org.openjdk.jmc.flightrecorder.ui.common.FlavorSelector.FlavorSelectorState;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram.HistogramSelection;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram.ItemHistogramBuilder;
import org.openjdk.jmc.flightrecorder.ui.common.ItemList;
import org.openjdk.jmc.flightrecorder.ui.common.ItemList.ItemListBuilder;
import org.openjdk.jmc.flightrecorder.ui.common.ItemRow;
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
import org.openjdk.jmc.ui.misc.CompositeToolkit;
import org.openjdk.jmc.ui.misc.PersistableSashForm;

public class JfxPage extends AbstractDataPage {

	public static class Factory implements IDataPageFactory {
		@Override
		public String getName(IState state) {
			return Messages.JfxPage_JAVA_FX;
		}

		@Override
		public ImageDescriptor getImageDescriptor(IState state) {
			return AbstractUIPlugin.imageDescriptorFromPlugin("org.openjdk.jmc.flightrecorder.ext.jfx", //$NON-NLS-1$
					"icons/pulse.png"); //$NON-NLS-1$
		}

		@Override
		public String[] getTopics(IState state) {
			return new String[] {JfxConstants.JFX_RULE_PATH};
		}

		@Override
		public IDisplayablePage createPage(IPageDefinition definition, StreamModel items, IPageContainer editor) {
			return new JfxPage(definition, items, editor);
		}
	}

	private static final ItemHistogramBuilder BY_PULSE_HISTOGRAM = new ItemHistogramBuilder();
	private static final ItemHistogramBuilder INPUT_HISTOGRAM = new ItemHistogramBuilder();
	private final ItemListBuilder phaseList = new ItemListBuilder();

	private static final String TOTAL_DURATION = "totalDuration"; //$NON-NLS-1$
	private static final String PULSE_START = "pulseStart"; //$NON-NLS-1$

	static {
		BY_PULSE_HISTOGRAM.addCountColumn();
		BY_PULSE_HISTOGRAM.addColumn(TOTAL_DURATION, Aggregators.sum(JfrAttributes.DURATION));
		BY_PULSE_HISTOGRAM.addColumn(PULSE_START, JfxConstants.PULSE_START);

		INPUT_HISTOGRAM.addCountColumn();
		INPUT_HISTOGRAM.addColumn(TOTAL_DURATION, Aggregators.sum(JfrAttributes.DURATION));
	}

	private class JfxUI implements IPageUI {

		private static final String PULSES_FILTER = "pulsesFilter"; //$NON-NLS-1$
		private static final String PHASES_FILTER = "phasesFilter"; //$NON-NLS-1$
		private static final String INPUT_FILTER = "inputFilter"; //$NON-NLS-1$
		private final ChartCanvas chartCanvas;
		private final ItemHistogram pulsesTable;
		private final ItemHistogram inputTable;
		private final ItemList phasesTable;
		private IPageContainer pageContainer;
		private final SashForm tableSash;
		private final SashForm mainSash;
		private final SashForm phasesSash;
		private IItemCollection selectionItems;
		private XYChart chart;
		private IItemCollection phaseItems;

		private static final String MAIN_SASH = "mainSash"; //$NON-NLS-1$
		private static final String TABLE_SASH = "tableSash"; //$NON-NLS-1$
		private static final String PHASES_SASH = "phasesSash"; //$NON-NLS-1$
		private static final String PULSES_TABLE = "pulseTable"; //$NON-NLS-1$
		private static final String PHASES_TABLE = "phaseTable"; //$NON-NLS-1$
		private static final String INPUT_TABLE = "inputTable"; //$NON-NLS-1$
		private final StreamModel items;
		private IRange<IQuantity> currentRange;
		private FlavorSelector flavorSelector;
		private FilterComponent pulsesFilter;
		private FilterComponent phasesFilter;
		private FilterComponent inputFilter;

		public JfxUI(Composite parent, FormToolkit toolkit, IPageContainer editor, IState state, StreamModel items,
				String name, Image icon) {
			this.pageContainer = editor;
			this.items = items;
			Form form = DataPageToolkit.createForm(parent, toolkit, name, icon);

			JavaFxEventAvailability availability = JfxVersionUtil.getAvailability(getItems());
			
			mainSash = new SashForm(form.getBody(), SWT.VERTICAL | SWT.SMOOTH);
			toolkit.adapt(mainSash);
			tableSash = new SashForm(mainSash, SWT.HORIZONTAL | SWT.SMOOTH);
			toolkit.adapt(tableSash);

			Section phases = CompositeToolkit.createSection(tableSash, toolkit, Messages.JfxPage_PHASES);
			phasesSash = new SashForm(phases, SWT.HORIZONTAL | SWT.SMOOTH);
			phases.setClient(phasesSash);
			pulsesTable = BY_PULSE_HISTOGRAM.buildWithoutBorder(phasesSash, JfxVersionUtil.getPulseIdAttribute(availability),
					getPulseTableSettings(state.getChild(PULSES_TABLE)));
			pulsesFilter = FilterComponent.createFilterComponent(pulsesTable, pulsesTableFilter,
					getItems().apply(JfxConstants.JFX_PULSE_FILTER), pageContainer.getSelectionStore()::getSelections,
					this::onPulsesFilterChange);
			pulsesTable.getManager().getViewer().addSelectionChangedListener(e -> onPulsesSelected());
			DataPageToolkit.addContextMenus(pageContainer, pulsesTable, Messages.JfxPage_PULSE_HISTOGRAM_SELECTION,
					pulsesFilter.getShowSearchAction(), pulsesFilter.getShowFilterAction());
			pulsesFilter.loadState(state.getChild(PULSES_FILTER));

			phaseList.addColumn(JfrAttributes.DURATION);
			phaseList.addColumn(JfrAttributes.START_TIME);
			phaseList.addColumn(JfxVersionUtil.getPhaseNameAttribute(availability));
			phaseList.addColumn(JfrAttributes.EVENT_THREAD);
			phaseList.addColumn(JfxVersionUtil.getPulseIdAttribute(availability));
			
			phasesTable = phaseList.buildWithoutBorder(phasesSash, getPhaseListSettings(state.getChild(PHASES_TABLE)));
			phasesFilter = FilterComponent.createFilterComponent(phasesTable, phasesTableFilter,
					getItems().apply(JfxConstants.JFX_PULSE_FILTER), pageContainer.getSelectionStore()::getSelections,
					this::onPhasesFilterChange);
			phasesTable.getManager().getViewer()
					.addSelectionChangedListener(e -> onPhasesSelected(!e.getSelection().isEmpty()));
			MCContextMenuManager itemMM = MCContextMenuManager
					.create(phasesTable.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(phasesTable.getManager(), itemMM);
			itemMM.add(phasesFilter.getShowSearchAction());
			itemMM.add(phasesFilter.getShowFilterAction());
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(), phasesTable,
					Messages.JfxPage_PHASE_TABLE_SELECTION, itemMM);
			phasesFilter.loadState(state.getChild(PHASES_FILTER));

			Section input = CompositeToolkit.createSection(tableSash, toolkit, Messages.JfxPage_INPUT);
			inputTable = INPUT_HISTOGRAM.buildWithoutBorder(input, JfxConstants.INPUT_TYPE,
					getInputTableSettings(state.getChild(INPUT_TABLE)));
			inputFilter = FilterComponent.createFilterComponent(inputTable, inputTableFilter, getItems(),
					pageContainer.getSelectionStore()::getSelections, this::onInputFilterChange);
			inputTable.getManager().getViewer().addSelectionChangedListener(e -> buildChart());
			inputTable.getManager().getViewer().addSelectionChangedListener(
					e -> pageContainer.showSelection(inputTable.getSelection().getItems()));
			DataPageToolkit.addContextMenus(pageContainer, inputTable, Messages.JfxPage_INPUT_HISTOGRAM_SELECTION,
					inputFilter.getShowSearchAction(), inputFilter.getShowFilterAction());
			input.setClient(inputFilter.getComponent());
			inputFilter.loadState(state.getChild(INPUT_FILTER));

			chartCanvas = new ChartCanvas(mainSash);
			DataPageToolkit.createChartTimestampTooltip(chartCanvas);
			chart = new XYChart(editor.getRecordingRange(), RendererToolkit.empty(), 180);
			chart.setVisibleRange(timelineRange.getStart(), timelineRange.getEnd());
			chart.addVisibleRangeListener(r -> timelineRange = r);
			DataPageToolkit.setChart(chartCanvas, chart, pageContainer::showSelection);

			PersistableSashForm.loadState(mainSash, state.getChild(MAIN_SASH));
			PersistableSashForm.loadState(tableSash, state.getChild(TABLE_SASH));
			PersistableSashForm.loadState(phasesSash, state.getChild(PHASES_SASH));

			flavorSelector = FlavorSelector.itemsWithTimerange(form, JfxConstants.JFX_FILTER, items.getItems(),
					pageContainer, this::onInputSelected, this::onShow, flavorSelectorState);

			onPulsesFilterChange(pulsesTableFilter);
			onPhasesFilterChange(phasesTableFilter);
			onInputFilterChange(inputTableFilter);

			pulsesTable.getManager().setSelectionState(pulseSelection);
			phasesTable.getManager().setSelectionState(phaseSelection);
			inputTable.getManager().setSelectionState(inputSelection);
		}

		private void onPulsesFilterChange(IItemFilter newFilter) {
			pulsesFilter.filterChangeHelper(newFilter, pulsesTable, getItems().apply(JfxConstants.JFX_PULSE_FILTER));
			pulsesTableFilter = newFilter;
		}

		private void onPhasesFilterChange(IItemFilter newFilter) {
			phasesFilter.filterChangeHelper(newFilter, phasesTable, getItems().apply(JfxConstants.JFX_PULSE_FILTER));
			phasesTableFilter = newFilter;
		}

		private void onInputFilterChange(IItemFilter newFilter) {
			inputFilter.filterChangeHelper(newFilter, inputTable, getItems().apply(JfxConstants.JFX_INPUT_FILTER));
			inputTableFilter = newFilter;
		}

		@Override
		public void saveTo(IWritableState state) {
			PersistableSashForm.saveState(mainSash, state.createChild(MAIN_SASH));
			PersistableSashForm.saveState(tableSash, state.createChild(TABLE_SASH));
			PersistableSashForm.saveState(phasesSash, state.createChild(PHASES_SASH));
			pulsesTable.getManager().getSettings().saveState(state.createChild(PULSES_TABLE));
			phasesTable.getManager().getSettings().saveState(state.createChild(PHASES_TABLE));
			inputTable.getManager().getSettings().saveState(state.createChild(INPUT_TABLE));
			pulsesFilter.saveState(state.createChild(PULSES_FILTER));
			phasesFilter.saveState(state.createChild(PHASES_FILTER));
			inputFilter.saveState(state.createChild(INPUT_FILTER));

			saveToLocal();
		}

		private void saveToLocal() {
			pulseSelection = pulsesTable.getManager().getSelectionState();
			phaseSelection = phasesTable.getManager().getSelectionState();
			inputSelection = inputTable.getManager().getSelectionState();
			flavorSelectorState = flavorSelector.getFlavorSelectorState();
		}

		private void onShow(Boolean show) {
			IRange<IQuantity> range = show ? currentRange : pageContainer.getRecordingRange();
			chart.setVisibleRange(range.getStart(), range.getEnd());
			chartCanvas.redrawChart();
		}

		private void onInputSelected(IItemCollection items, IRange<IQuantity> currentRange) {
			selectionItems = items;
			this.currentRange = currentRange;
			phaseItems = getItems().apply(JfxConstants.JFX_PULSE_FILTER);
			pulsesTable.show(phaseItems);
			inputTable.show(getItems().apply(JfxConstants.JFX_INPUT_FILTER));

			buildChart();
		}

		private IItemCollection getItems() {
			return selectionItems != null ? selectionItems : items.getItems();
		}

		private void buildChart() {
			List<IXDataRenderer> rows = new ArrayList<>();
			Stream<IXDataRenderer> phaseRows = AggregationGrid.mapItems(ItemCollectionToolkit.stream(phaseItems),
					JfrAttributes.EVENT_THREAD, JfxPage::buildThreadRenderer);
			phaseRows.forEach(rows::add);

			HistogramSelection inputSelection = inputTable.getSelection();
			IItemCollection inputItems = inputSelection.getRowCount() > 0 ? inputSelection.getItems()
					: getItems().apply(JfxConstants.JFX_INPUT_FILTER);
			IXDataRenderer inputRenderer = DataPageToolkit.buildSpanRenderer(inputItems,
					DataPageToolkit.getAttributeValueColor(JfxConstants.INPUT_TYPE));
			rows.add(new ItemRow(Messages.JfxPage_INPUTS, JfxConstants.INPUT_TYPE.getDescription(), inputRenderer,
					inputItems));

			chartCanvas.replaceRenderer(RendererToolkit.uniformRows(rows));
		}

		private void onPulsesSelected() {
			phasesTable.show(pulsesTable.getSelection().getItems());
			onPhasesSelected(false);
		}

		private void onPhasesSelected(boolean fromPhasesTable) {
			HistogramSelection s = pulsesTable.getSelection();
			if (fromPhasesTable) {
				IItemCollection phasesItems = ItemCollectionToolkit.build(phasesTable.getSelection().get());
				setChartVisibleRange(phasesItems);
				showSelectedPhases(phasesItems);
			} else if (s.getRowCount() > 0) {
				IItemCollection selectedItems = s.getItems();
				setChartVisibleRange(selectedItems);
				showSelectedPhases(selectedItems);
			} else {
				showSelectedPhases(getItems().apply(JfxConstants.JFX_PULSE_FILTER));
			}
		}

		private void setChartVisibleRange(IItemCollection toShowItems) {
			IAggregator<IQuantity, ?> firstStartAggregator = Aggregators.min(JfrAttributes.START_TIME);
			IQuantity firstSelected = toShowItems.getAggregate(firstStartAggregator);
			IAggregator<IQuantity, ?> lastEndAggregator = Aggregators.max(JfrAttributes.END_TIME);
			IQuantity lastSelected = toShowItems.getAggregate(lastEndAggregator);
			chart.setVisibleRange(firstSelected, lastSelected);
		}

		private void showSelectedPhases(IItemCollection phasesItems) {
			this.phaseItems = phasesItems;
			buildChart();
			pageContainer.showSelection(phasesItems);
		}

	}

	private IRange<IQuantity> timelineRange;
	private SelectionState pulseSelection;
	private SelectionState phaseSelection;
	private SelectionState inputSelection;
	private FlavorSelectorState flavorSelectorState;
	private IItemFilter pulsesTableFilter;
	private IItemFilter phasesTableFilter;
	private IItemFilter inputTableFilter;

	public JfxPage(IPageDefinition definition, StreamModel items, IPageContainer editor) {
		super(definition, items, editor);
		timelineRange = editor.getRecordingRange();
	}

	@Override
	public IItemFilter getDefaultSelectionFilter() {
		return JfxConstants.JFX_FILTER;
	}

	private static IXDataRenderer buildThreadRenderer(Object threadName, IItemCollection items) {
		// Attribute only used for looking up color and name information here
		IXDataRenderer phaseRenderer = DataPageToolkit.buildSpanRenderer(items,
				DataPageToolkit.getAttributeValueColor(JfxConstants.ATTRIBUTE_PHASE_NAME_12));
		return new ItemRow(String.valueOf(threadName), JfxConstants.ATTRIBUTE_PHASE_NAME_12.getDescription(), phaseRenderer, items);
	}

	private static TableSettings getPulseTableSettings(IState state) {
		if (state == null) {
			return new TableSettings(TOTAL_DURATION,
					Arrays.asList(new ColumnSettings(ItemHistogram.KEY_COL_ID, false, 75, false),
							new ColumnSettings(TOTAL_DURATION, false, 75, false)));
		} else {
			return new TableSettings(state);
		}
	}

	private static TableSettings getPhaseListSettings(IState state) {
		if (state == null) {
			return new TableSettings(JfrAttributes.DURATION.getIdentifier(),
					Arrays.asList(new ColumnSettings(JfrAttributes.DURATION.getIdentifier(), false, 100, false),
							new ColumnSettings(JfrAttributes.DURATION.getIdentifier(), false, 200, false),
							new ColumnSettings(JfxConstants.ATTRIBUTE_PHASE_NAME_12.getIdentifier(), false, 100, false),
							new ColumnSettings(JfxConstants.ATTRIBUTE_PULSE_ID_12.getIdentifier(), false, 100, false),
							new ColumnSettings(JfrAttributes.EVENT_THREAD.getIdentifier(), false, 200, false)));
		} else {
			return new TableSettings(state);
		}
	}

	private static TableSettings getInputTableSettings(IState state) {
		if (state == null) {
			return new TableSettings(TOTAL_DURATION,
					Arrays.asList(new ColumnSettings(ItemHistogram.KEY_COL_ID, false, 100, false),
							new ColumnSettings(TOTAL_DURATION, false, 75, false),
							new ColumnSettings(ItemHistogram.COUNT_COL_ID, false, 100, false)));
		} else {
			return new TableSettings(state);
		}
	}

	@Override
	public IPageUI display(Composite parent, FormToolkit toolkit, IPageContainer editor, IState state) {
		return new JfxUI(parent, toolkit, editor, state, getDataSource(), getName(), getIcon());
	}

}
