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

import java.util.List;
import java.util.Optional;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.ui.IPageContainer;
import org.openjdk.jmc.flightrecorder.ui.IPageUI;
import org.openjdk.jmc.flightrecorder.ui.StreamModel;
import org.openjdk.jmc.flightrecorder.ui.common.DataPageToolkit;
import org.openjdk.jmc.flightrecorder.ui.common.FilterComponent;
import org.openjdk.jmc.flightrecorder.ui.common.FlavorSelector;
import org.openjdk.jmc.flightrecorder.ui.common.FlavorSelector.FlavorSelectorState;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram.HistogramSelection;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStoreActionToolkit;
import org.openjdk.jmc.ui.charts.IXDataRenderer;
import org.openjdk.jmc.ui.charts.RendererToolkit;
import org.openjdk.jmc.ui.charts.XYChart;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.handlers.ActionToolkit;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.ActionUiToolkit;
import org.openjdk.jmc.ui.misc.ChartCanvas;
import org.openjdk.jmc.ui.misc.PersistableSashForm;

abstract class ChartAndTableUI implements IPageUI {

	private static final String SASH = "sash"; //$NON-NLS-1$
	private static final String TABLE = "table"; //$NON-NLS-1$
	private static final String CHART = "chart"; //$NON-NLS-1$
	private static final String SELECTED = "selected"; //$NON-NLS-1$
	private final IItemFilter pageFilter;
	private final StreamModel model;
	protected CheckboxTableViewer chartLegend;
	protected final Form form;
	protected final Composite chartContainer;
	protected final ChartCanvas chartCanvas;
	protected final FilterComponent tableFilterComponent;
	protected final ItemHistogram table;
	protected final SashForm sash;
	private final IPageContainer pageContainer;
	protected List<IAction> allChartSeriesActions;
	private IItemCollection selectionItems;
	private IRange<IQuantity> timeRange;
	protected XYChart chart;
	protected FlavorSelector flavorSelector;

	ChartAndTableUI(IItemFilter pageFilter, StreamModel model, Composite parent, FormToolkit toolkit,
			IPageContainer pageContainer, IState state, String sectionTitle, IItemFilter tableFilter, Image icon,
			FlavorSelectorState flavorSelectorState) {
		this.pageFilter = pageFilter;
		this.model = model;
		this.pageContainer = pageContainer;
		form = DataPageToolkit.createForm(parent, toolkit, sectionTitle, icon);
		sash = new SashForm(form.getBody(), SWT.VERTICAL);
		toolkit.adapt(sash);

		table = buildHistogram(sash, state.getChild(TABLE));
		MCContextMenuManager mm = MCContextMenuManager.create(table.getManager().getViewer().getControl());
		ColumnMenusFactory.addDefaultMenus(table.getManager(), mm);
		table.getManager().getViewer().addSelectionChangedListener(e -> buildChart());
		table.getManager().getViewer()
				.addSelectionChangedListener(e -> pageContainer.showSelection(table.getSelection().getItems()));
		SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(), table,
				NLS.bind(Messages.ChartAndTableUI_HISTOGRAM_SELECTION, sectionTitle), mm);
		tableFilterComponent = FilterComponent.createFilterComponent(table.getManager().getViewer().getControl(),
				table.getManager(), tableFilter, model.getItems().apply(pageFilter),
				pageContainer.getSelectionStore()::getSelections, this::onFilterChange);
		mm.add(tableFilterComponent.getShowFilterAction());
		mm.add(tableFilterComponent.getShowSearchAction());

		chartContainer = toolkit.createComposite(sash);
		chartContainer.setLayout(new GridLayout(2, false));
		chartCanvas = new ChartCanvas(chartContainer);
		chartCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		allChartSeriesActions = initializeChartConfiguration(state);
		IState chartState = state.getChild(CHART);
		ActionToolkit.loadCheckState(chartState, allChartSeriesActions.stream());
		chartLegend = ActionUiToolkit.buildCheckboxViewer(chartContainer, allChartSeriesActions.stream());
		GridData gd = new GridData(SWT.FILL, SWT.FILL, false, true);
		gd.widthHint = 180;
		chartLegend.getControl().setLayoutData(gd);
		PersistableSashForm.loadState(sash, state.getChild(SASH));
		DataPageToolkit.createChartTimestampTooltip(chartCanvas);

		chart = new XYChart(pageContainer.getRecordingRange(), RendererToolkit.empty(), 180);
		DataPageToolkit.setChart(chartCanvas, chart, pageContainer::showSelection);
		SelectionStoreActionToolkit.addSelectionStoreRangeActions(pageContainer.getSelectionStore(), chart,
				JfrAttributes.LIFETIME, NLS.bind(Messages.ChartAndTableUI_TIMELINE_SELECTION, form.getText()),
				chartCanvas.getContextMenu());
		buildChart();

		if (chartState != null) {
			final String legendSelection = chartState.getAttribute(SELECTED);

			if (legendSelection != null) {
				allChartSeriesActions.stream().filter(ia -> legendSelection.equals(ia.getId())).findFirst()
						.ifPresent(a -> chartLegend.setSelection(new StructuredSelection(a)));
			}
		}

		flavorSelector = FlavorSelector.itemsWithTimerange(form, pageFilter, model.getItems(), pageContainer,
				this::onFlavorSelected, this::onSetRange, flavorSelectorState);
	}

	protected void onFilterChange(IItemFilter filter) {
		IItemCollection items = getItems();
		if (tableFilterComponent.isVisible()) {
			table.show(items.apply(filter));
			tableFilterComponent.setColor(table.getAllRows().getRowCount());
		} else {
			table.show(items);
		}
	}

	@Override
	public void saveTo(IWritableState writableState) {
		PersistableSashForm.saveState(sash, writableState.createChild(SASH));
		table.getManager().getSettings().saveState(writableState.createChild(TABLE));
		IWritableState chartState = writableState.createChild(CHART);

		ActionToolkit.saveCheckState(chartState, allChartSeriesActions.stream());
		Object legendSelection = ((IStructuredSelection) chartLegend.getSelection()).getFirstElement();
		if (legendSelection != null) {
			chartState.putString(SELECTED, ((IAction) legendSelection).getId());
		}
	}

	private void onSetRange(Boolean useRange) {
		IRange<IQuantity> range = useRange ? timeRange : pageContainer.getRecordingRange();
		chart.setVisibleRange(range.getStart(), range.getEnd());
		buildChart();
	}

	private void onFlavorSelected(IItemCollection items, IRange<IQuantity> timeRange) {
		this.selectionItems = items;
		this.timeRange = timeRange;
		table.show(getItems());

		if (selectionItems != null) {
			Object[] tableInput = ((Object[]) table.getManager().getViewer().getInput());
			if (tableInput != null) {
				table.getManager().getViewer().setSelection(new StructuredSelection(tableInput));
			} else {
				table.getManager().getViewer().setSelection(null);
			}
		}
	}

	protected void buildChart() {
		IXDataRenderer rendererRoot = getChartRenderer(getItems(), table.getSelection());
		chartCanvas.replaceRenderer(rendererRoot);
	}

	private IItemCollection getItems() {
		return selectionItems != null ? selectionItems.apply(pageFilter) : model.getItems().apply(pageFilter);
	}

	protected boolean isAttributeEnabled(IAttribute<IQuantity> attr) {
		Optional<IAction> action = allChartSeriesActions.stream().filter(a -> attr.getIdentifier().equals(a.getId()))
				.findAny();
		return action.isPresent() && action.get().isChecked();
	}

	protected abstract ItemHistogram buildHistogram(Composite parent, IState state);

	protected abstract IXDataRenderer getChartRenderer(IItemCollection itemsInTable, HistogramSelection selection);

	protected abstract List<IAction> initializeChartConfiguration(IState state);
}
