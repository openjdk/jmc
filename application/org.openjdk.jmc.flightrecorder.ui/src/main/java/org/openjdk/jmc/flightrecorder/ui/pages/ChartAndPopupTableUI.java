/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, Red Hat Inc. All rights reserved.
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

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
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
import org.openjdk.jmc.flightrecorder.ui.StreamModel;
import org.openjdk.jmc.flightrecorder.ui.common.DataPageToolkit;
import org.openjdk.jmc.flightrecorder.ui.common.FilterComponent;
import org.openjdk.jmc.flightrecorder.ui.common.FlavorSelector;
import org.openjdk.jmc.flightrecorder.ui.common.FlavorSelector.FlavorSelectorState;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStoreActionToolkit;
import org.openjdk.jmc.ui.charts.IXDataRenderer;
import org.openjdk.jmc.ui.charts.RendererToolkit;
import org.openjdk.jmc.ui.charts.XYChart;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.common.util.Environment;
import org.openjdk.jmc.ui.handlers.ActionToolkit;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.ActionUiToolkit;
import org.openjdk.jmc.ui.misc.ChartCanvas;
import org.openjdk.jmc.ui.misc.ChartControlBar;
import org.openjdk.jmc.ui.misc.ChartButtonGroup;
import org.openjdk.jmc.ui.misc.ChartTextCanvas;
import org.openjdk.jmc.ui.misc.PersistableSashForm;
import org.openjdk.jmc.ui.misc.TimelineCanvas;

abstract class ChartAndPopupTableUI extends ChartAndTableUI {

	private static final double Y_SCALE = Display.getCurrent().getDPI().y / Environment.getNormalDPI();
	private static final String TABLE = "table"; //$NON-NLS-1$
	private static final String CHART = "chart"; //$NON-NLS-1$
	private static final String CANVAS_SASH = "canvasSash"; //$NON-NLS-1$
	private static final String PAGE_SASH = "pageSash"; //$NON-NLS-1$
	private static final String SELECTED = "selected"; //$NON-NLS-1$
	private static final int TIMELINE_HEIGHT = 40;
	private static final int X_OFFSET = 0;
	private static final int Y_OFFSET = 0;
	protected ChartControlBar controlBar;
	protected ChartTextCanvas textCanvas;
	protected IPageContainer pageContainer;
	private ChartButtonGroup buttonGroup;
	private IItemCollection selectionItems;
	private IItemFilter pageFilter;
	private IRange<IQuantity> timeRange;
	protected SashForm canvasSash;
	private TimelineCanvas timelineCanvas;

	ChartAndPopupTableUI(IItemFilter pageFilter, StreamModel model, Composite parent, FormToolkit toolkit,
			IPageContainer pageContainer, IState state, String sectionTitle, IItemFilter tableFilter, Image icon,
			FlavorSelectorState flavorSelectorState, IAttribute<?> classifier) {
		super(pageFilter, model, parent, toolkit, pageContainer, state, sectionTitle, tableFilter, icon,
				flavorSelectorState, classifier);
	}

	protected void init(
		IItemFilter pageFilter, StreamModel model, Composite parent, FormToolkit toolkit, IPageContainer pageContainer,
		IState state, String sectionTitle, IItemFilter tableFilter, Image icon, FlavorSelectorState flavorSelectorState,
		IAttribute<?> classifier) {
		this.pageFilter = pageFilter;
		this.model = model;
		this.pageContainer = pageContainer;
		form = DataPageToolkit.createForm(parent, toolkit, sectionTitle, icon);
		sash = new SashForm(form.getBody(), SWT.VERTICAL);
		toolkit.adapt(sash);

		table = buildHistogram(sash, state.getChild(TABLE), classifier);
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

		/**
		 * Scrolled Composite Page Container - Contains all page functionality Chart Container (1
		 * column gridlayout) - Contains filter bar & graph container Graph Container (2 column
		 * gridlayout) - Contains chart and timeline container & display bar Chart and Timeline
		 * Container (1 column gridlayout) Contains chart and text container and timeline canvas
		 * Timeline and Height Buttons Container (2 column gridlayout) Contains timeline and lane
		 * height button controls Zoom-pan and Chart Container (formlayout) - Contains chart and
		 * text container contents and zoom-pan overlay Zoom-pan Container (filllayout) - Contains
		 * zoom-pan chart overlay Full screen Chart Container (1 column gridlayout) - Contains chart
		 * container Chart and Text Container (2 column gridlayout) - Contains scText and
		 * textCanvas) & scChart (and chart canvas)
		 */

		// Scrolled Composite containing all page functionality
		ScrolledComposite scPageContainer = new ScrolledComposite(sash, SWT.H_SCROLL | SWT.V_SCROLL);

		chartContainer = toolkit.createComposite(scPageContainer);
		chartContainer.setLayout(new GridLayout());
		chartContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		scPageContainer.setContent(chartContainer);
		scPageContainer.setAlwaysShowScrollBars(false);
		scPageContainer.setExpandHorizontal(true);
		scPageContainer.setExpandVertical(true);
		scPageContainer.addListener(SWT.Resize, new Listener() {
			@Override
			public void handleEvent(Event event) {
				int width = controlBar.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
				int height = controlBar.computeSize(SWT.DEFAULT, SWT.DEFAULT).y
						+ buttonGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
				if (width > 0 && height > 0) {
					scPageContainer.setMinSize(scPageContainer.computeSize(width, height));
					scPageContainer.removeListener(SWT.Resize, this);
				}
			}
		});

		// Filter Controls
		Listener resetListener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				onSetRange(false);
				table.getManager().getViewer().setSelection(null);
			}
		};
		controlBar = new ChartControlBar(chartContainer, resetListener, pageContainer.getRecordingRange());
		controlBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		// Container to hold the chart (& timeline)
		Composite graphContainer = toolkit.createComposite(chartContainer);
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		graphContainer.setLayout(gridLayout);
		graphContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// Container to hold the chart and timeline canvas
		Composite chartAndTimelineContainer = toolkit.createComposite(graphContainer);
		gridLayout = new GridLayout();
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		chartAndTimelineContainer.setLayout(gridLayout);
		chartAndTimelineContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// Container to hold the chart and a zoom-pan overlay
		Composite zoomPanAndChartContainer = toolkit.createComposite(chartAndTimelineContainer);
		zoomPanAndChartContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		zoomPanAndChartContainer.setLayout(new FormLayout());

		// Container to hold fixed zoom-pan display
		Composite zoomPanContainer = toolkit.createComposite(zoomPanAndChartContainer);
		zoomPanContainer.setLayout(new FillLayout());
		FormData fd = new FormData();
		fd.height = 80;
		fd.width = 150;
		fd.bottom = new FormAttachment(100, -12);
		fd.right = new FormAttachment(100, -12);
		zoomPanContainer.setLayoutData(fd);

		// Container to hold the chart
		Composite fullScreenChartContainer = toolkit.createComposite(zoomPanAndChartContainer);
		fullScreenChartContainer.setLayout(gridLayout);
		fd = new FormData();
		fd.right = new FormAttachment(100, -1);
		fd.top = new FormAttachment(0, 1);
		fd.left = new FormAttachment(0, 1);
		fd.bottom = new FormAttachment(100, -1);
		fullScreenChartContainer.setLayoutData(fd);

		// Container to hold the text and chart canvases
		Composite chartAndTextContainer = toolkit.createComposite(fullScreenChartContainer);
		gridLayout = new GridLayout(2, false);
		gridLayout.horizontalSpacing = 0;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		chartAndTextContainer.setLayout(gridLayout);
		chartAndTextContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		canvasSash = new SashForm(chartAndTextContainer, SWT.HORIZONTAL);
		canvasSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		toolkit.adapt(canvasSash);

		ScrolledComposite scText = new ScrolledComposite(canvasSash, SWT.BORDER | SWT.V_SCROLL);
		GridData scTextGd = new GridData(SWT.FILL, SWT.FILL, false, true);
		scTextGd.widthHint = 180;
		scText.setLayoutData(scTextGd);
		textCanvas = new ChartTextCanvas(scText);
		textCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));

		ScrolledComposite scChart = new ScrolledComposite(canvasSash, SWT.BORDER | SWT.V_SCROLL);
		scChart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		chartCanvas = new ChartCanvas(scChart);
		chartCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		chartCanvas.setTextCanvas(textCanvas);
		textCanvas.setChartCanvas(chartCanvas);

		scChart.setContent(chartCanvas);
		scChart.setAlwaysShowScrollBars(true);
		scChart.setExpandHorizontal(true);
		scChart.setExpandVertical(true);
		scText.setContent(textCanvas);
		scText.setAlwaysShowScrollBars(false);
		scText.setExpandHorizontal(true);
		scText.setExpandVertical(true);

		timelineCanvas = new TimelineCanvas(chartAndTimelineContainer, chartCanvas, canvasSash, Y_SCALE);
		GridData gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
		gridData.heightHint = (int) (TIMELINE_HEIGHT * Y_SCALE);
		timelineCanvas.setLayoutData(gridData);

		// add the display bar to the right of the chart scrolled composite
		buttonGroup = controlBar.getButtonGroup();

		allChartSeriesActions = initializeChartConfiguration(state);
		IState chartState = state.getChild(CHART);
		ActionToolkit.loadCheckState(chartState, allChartSeriesActions.stream());
		chartLegend = ActionUiToolkit.buildCheckboxViewer(chartContainer, allChartSeriesActions.stream());
		gridData = new GridData(SWT.FILL, SWT.FILL, false, true);
		gridData.widthHint = 180;
		chartLegend.getControl().setLayoutData(gridData);
		DataPageToolkit.createChartTimestampTooltip(chartCanvas);

		chart = new XYChart(pageContainer.getRecordingRange(), RendererToolkit.empty(), X_OFFSET, Y_OFFSET,
				timelineCanvas, controlBar, buttonGroup);
		DataPageToolkit.setChart(chartCanvas, chart, pageContainer::showSelection);
		DataPageToolkit.setChart(textCanvas, chart, pageContainer::showSelection);
		SelectionStoreActionToolkit.addSelectionStoreRangeActions(pageContainer.getSelectionStore(), chart,
				JfrAttributes.LIFETIME, NLS.bind(Messages.ChartAndTableUI_TIMELINE_SELECTION, form.getText()),
				chartCanvas.getContextMenu());
		SelectionStoreActionToolkit.addSelectionStoreRangeActions(pageContainer.getSelectionStore(), chart,
				JfrAttributes.LIFETIME, NLS.bind(Messages.ChartAndTableUI_TIMELINE_SELECTION, form.getText()),
				textCanvas.getContextMenu());

		chartCanvas.setZoomOnClickListener(mouseDown -> buttonGroup.zoomOnClick(mouseDown));
		chartCanvas.setZoomToSelectionListener(() -> buttonGroup.zoomToSelection());

		// Wire-up the chart & text canvases to the control bar and button group
		controlBar.setChart(chart);
		controlBar.setChartCanvas(chartCanvas);
		controlBar.setTextCanvas(textCanvas);
		buttonGroup.createZoomPan(zoomPanContainer);
		timelineCanvas.setChart(chart);

		if (chartState != null) {
			final String legendSelection = chartState.getAttribute(SELECTED);

			if (legendSelection != null) {
				allChartSeriesActions.stream().filter(ia -> legendSelection.equals(ia.getId())).findFirst()
						.ifPresent(a -> chartLegend.setSelection(new StructuredSelection(a)));
			}
		}

		if (state.getChild(PAGE_SASH) == null) {
			sash.setWeights(new int[] {0, 3});
		} else {
			PersistableSashForm.loadState(sash, state.getChild(PAGE_SASH));
		}

		if (state.getChild(CANVAS_SASH) == null) {
			canvasSash.setWeights(new int[] {1, 4});
		} else {
			PersistableSashForm.loadState(canvasSash, state.getChild(CANVAS_SASH));
		}

		flavorSelector = FlavorSelector.itemsWithTimerange(form, pageFilter, model.getItems(), pageContainer,
				this::onFlavorSelected, this::onSetRange, flavorSelectorState);
	}

	protected void onFilterChange(IItemFilter filter) {
		IItemCollection items = getItems();
		if (tableFilterComponent.isVisible()) {
			table.show(items.apply(filter));
			tableFilterComponent.setColor(table.getAllRows().getRowCount());
		} else if (table != null) {
			table.show(items);
		}
	}

	private void onSetRange(Boolean useRange) {
		IRange<IQuantity> range = useRange ? timeRange : pageContainer.getRecordingRange();
		chart.setVisibleRange(range.getStart(), range.getEnd());
		chart.resetZoomFactor();
		if (table != null) {
			table.getManager().getViewer().setSelection(null);
		}
		controlBar.resetLaneHeightToMinimum();
		buildChart();
	}

	@Override
	public void saveTo(IWritableState writableState) {
		super.saveTo(writableState);
		PersistableSashForm.saveState(sash, writableState.createChild(PAGE_SASH));
		PersistableSashForm.saveState(canvasSash, writableState.createChild(CANVAS_SASH));
	}

	private void onFlavorSelected(IItemCollection items, IRange<IQuantity> timeRange) {
		this.selectionItems = items;
		this.timeRange = timeRange;
		table.show(getItems());

		if (selectionItems != null) {
			Object[] tableInput = (Object[]) table.getManager().getViewer().getInput();
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
		textCanvas.replaceRenderer(rendererRoot);
	}

	private IItemCollection getItems() {
		return selectionItems != null ? selectionItems.apply(pageFilter) : model.getItems().apply(pageFilter);
	}

	public void setTimeRange(IRange<IQuantity> range) {
		this.timeRange = range;
	}
}
