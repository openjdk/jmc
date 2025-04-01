/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2025, Datadog, Inc. All rights reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.flightrecorder.ui.views.histogram;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.ItemCollectionToolkit;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.IPageContainer;
import org.openjdk.jmc.flightrecorder.ui.common.DataPageToolkit;
import org.openjdk.jmc.flightrecorder.ui.common.DurationPercentileTable;
import org.openjdk.jmc.flightrecorder.ui.common.DurationPercentileTable.DurationPercentileTableBuilder;
import org.openjdk.jmc.flightrecorder.ui.common.TypeLabelProvider;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.selection.ItemBackedSelection;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStore;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStoreActionToolkit;
import org.openjdk.jmc.ui.charts.IXDataRenderer;
import org.openjdk.jmc.ui.charts.RendererToolkit;
import org.openjdk.jmc.ui.charts.XYChart;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.column.TableSettings.ColumnSettings;
import org.openjdk.jmc.ui.common.util.AdapterUtil;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.layout.SimpleLayout;
import org.openjdk.jmc.ui.layout.SimpleLayoutData;
import org.openjdk.jmc.ui.misc.ChartCanvas;

public class HDRHistogramView extends ViewPart implements ISelectionListener {
	private IItemCollection currentItems;

	// UI Components
	private Composite parentComposite;
	private StackLayout stack;
	private SashForm sash;
	private Composite messageComposite;
	private Composite contentComposite;
	private ChartCanvas durationCanvas;

	private int[] sashWeights;

	private IRange<IQuantity> durationRange;
	private static final String SASH_ELEMENT = "sash"; //$NON-NLS-1$
	private static final DurationPercentileTableBuilder PERCENTILES_BUILDER = new DurationPercentileTableBuilder();
	private static final int[] DEFAULT_SASH_WEIGHTS = new int[] {60, 40};
	private static final Color GRAPH_COLOR = TypeLabelProvider.getColor(JdkTypeIDs.SOCKET_READ);

	private DurationPercentileTable percentileTable;
	private ViewSelectionProvider selectionProvider;

	static {
		PERCENTILES_BUILDER.addSeries("duration", Messages.HDRHistogramView_DURATION_COLUMN_NAME, "eventCount",
				Messages.HDRHistogramView_EVENT_COUNT_COLUMN_NAME, null);
	}

	private class ViewSelectionProvider implements ISelectionProvider {
		private ISelection selection;
		private final List<ISelectionChangedListener> listeners = new ArrayList<>();

		@Override
		public void addSelectionChangedListener(ISelectionChangedListener listener) {
			listeners.add(listener);
		}

		@Override
		public ISelection getSelection() {
			return selection;
		}

		@Override
		public void removeSelectionChangedListener(ISelectionChangedListener listener) {
			listeners.remove(listener);
		}

		@Override
		public void setSelection(ISelection selection) {
			this.selection = selection;
			SelectionChangedEvent event = new SelectionChangedEvent(this, selection);
			for (ISelectionChangedListener listener : listeners) {
				listener.selectionChanged(event);
			}
		}
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		getSite().getPage().addSelectionListener(this);

		// Restore state if available
		if (memento != null) {
			IMemento sashMemento = memento.getChild(SASH_ELEMENT);
			if (sashMemento != null) {
				// For now, we have 2 sash areas
				sashWeights = new int[2];

				Integer weight0 = sashMemento.getInteger("weight0");
				Integer weight1 = sashMemento.getInteger("weight1");

				if (weight0 != null && weight1 != null) {
					sashWeights[0] = weight0;
					sashWeights[1] = weight1;
				} else {
					sashWeights = DEFAULT_SASH_WEIGHTS;
				}
				// Reset any saved range
				durationRange = null;
			}
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		selectionProvider = new ViewSelectionProvider();
		getSite().setSelectionProvider(selectionProvider);

		this.parentComposite = new Composite(parent, SWT.NONE);
		this.stack = new StackLayout();
		parentComposite.setLayout(stack);

		// Composite for when there is no valid selection...
		createMessageComposite(parentComposite);

		// Composite for the content
		createContentComposite(parentComposite);

		if (currentItems != null) {
			updateWithItems(currentItems);
		} else {
			showMessage();
		}
	}

	private void createContentComposite(Composite parent) {
		contentComposite = new Composite(parent, SWT.NONE);
		contentComposite.setLayout(new FillLayout());
		sash = new SashForm(contentComposite, SWT.HORIZONTAL);

		SimpleLayoutData sashLayoutData = new SimpleLayoutData(SimpleLayout.INIFINITE_WEIGHT);
		sash.setLayoutData(sashLayoutData);

		durationCanvas = new ChartCanvas(sash);
		durationCanvas.setLayout(new FillLayout());

		Composite tableComposite = new Composite(sash, SWT.NONE);
		tableComposite.setLayout(new FillLayout());

		createPercentileTable(tableComposite);
		updateHistogramChart();

		// Set sash weights from saved state or default
		sash.setWeights(sashWeights != null ? sashWeights : DEFAULT_SASH_WEIGHTS);
	}

	private void createMessageComposite(Composite parent) {
		messageComposite = new Composite(parent, SWT.NONE);
		messageComposite.setLayout(new FillLayout());

		Label label = new Label(messageComposite, SWT.CENTER);
		label.setText(Messages.HDRHistogramView_NO_VALID_SELECTION_TEXT);
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (part != this && selection instanceof IStructuredSelection) {
			Object first = ((IStructuredSelection) selection).getFirstElement();
			IItemCollection items = AdapterUtil.getAdapter(first, IItemCollection.class);
			if (items == null) {
				updateWithItems(ItemCollectionToolkit.build(Stream.empty()));
			} else if (!items.equals(currentItems)) {
				updateWithItems(items);
			}
		}
	}

	private void updateWithItems(IItemCollection items) {
		currentItems = items;
		if (sash != null && !sash.isDisposed()) {
			IItemCollection itemsWithDuration = currentItems.apply(ItemFilters.hasAttribute(JfrAttributes.DURATION));

			if (itemsWithDuration.hasItems()) {
				updatePercentileTable(itemsWithDuration);
				updateHistogramChart();
				showContent();
			} else {
				showMessage();
			}
		}
	}

	private void updateHistogramChart() {
		if (currentItems != null && durationCanvas != null && !durationCanvas.isDisposed()) {
			IItemCollection itemsWithDuration = currentItems.apply(ItemFilters.hasAttribute(JfrAttributes.DURATION));
			// This should never happen as we check in updateWithItems, but just in case
			if (!itemsWithDuration.hasItems()) {
				showMessage();
				return;
			}

			List<IXDataRenderer> renderers = new ArrayList<>();
			renderers.add(DataPageToolkit.buildDurationHistogram(Messages.HDRHistogramView_DURATIONS_CHART_TITLE,
					Messages.HDRHistogramView_DURATIONS_CHART_DESCRIPTION, itemsWithDuration,
					(IAggregator<IQuantity, ?>) Aggregators.count(), GRAPH_COLOR));
			IXDataRenderer rendererRoot = RendererToolkit.uniformRows(renderers);

			// Get the maximum duration to set chart bounds
			IQuantity maxDuration = itemsWithDuration.getAggregate(JdkAggregators.LONGEST_EVENT);
			if (maxDuration == null) {
				maxDuration = UnitLookup.MILLISECOND.quantity(100);
			} else {
				maxDuration = UnitLookup.MILLISECOND.quantity(maxDuration.doubleValueIn(UnitLookup.MILLISECOND) * 1.1);
			}

			XYChart durationChart = new XYChart(UnitLookup.MILLISECOND.quantity(0), maxDuration, rendererRoot, 180);
			DataPageToolkit.setChart(durationCanvas, durationChart, JfrAttributes.DURATION, selection -> {
				if (selection != null && selectionProvider != null) {
					selectionProvider.setSelection(
							new ItemBackedSelection(selection, Messages.HDRHistogramView_DURATION_SELECTION));
				}
			});

			if (durationRange != null) {
				durationChart.setVisibleRange(durationRange.getStart(), durationRange.getEnd());
				durationRange = null;
			}

			durationCanvas.addControlListener(ControlListener.controlResizedAdapter(e -> {
				IRange<IQuantity> visibleRange = durationChart.getVisibleRange();
				durationChart.setVisibleRange(visibleRange.getStart(), visibleRange.getEnd());
				durationCanvas.redraw();
			}));
		}
	}

	private void updatePercentileTable(IItemCollection items) {
		if (percentileTable != null) {
			percentileTable.update(items);
		}
	}

	private void createPercentileTable(Composite parent) {
		List<ColumnSettings> columnSettings = new ArrayList<>();
		columnSettings
				.add(new ColumnSettings(DurationPercentileTable.TABLE_NAME + ".percentile", false, 80, Boolean.TRUE));
		columnSettings.add(new ColumnSettings("duration", false, 120, null));
		columnSettings.add(new ColumnSettings("eventCount", false, 100, null));
		TableSettings tableSettings = new TableSettings(null, columnSettings);
		percentileTable = PERCENTILES_BUILDER.build(parent, tableSettings);

		MCContextMenuManager percentileTableMm = MCContextMenuManager
				.create(percentileTable.getManager().getViewer().getControl());
		ColumnMenusFactory.addDefaultMenus(percentileTable.getManager(), percentileTableMm);

		// Add selection store actions directly like StacktraceView does
		SelectionStoreActionToolkit.addSelectionStoreActions(percentileTable.getManager().getViewer(),
				this::getSelectionStore, this::getSelectedItemsAsCollection,
				Messages.HDRHistogramView_PERCENTILE_SELECTION, percentileTableMm);
	}

	private IItemCollection getSelectedItemsAsCollection() {
		IItemCollection items = percentileTable.getSelectedItems();
		if (items == null) {
			return ItemCollectionToolkit.build(Stream.empty());
		}
		return items;
	}

	private SelectionStore getSelectionStore() {
		try {
			// Try to get active editor which should be an IPageContainer
			IWorkbenchPart editorPart = getSite().getPage().getActiveEditor();
			if (editorPart instanceof IPageContainer) {
				return ((IPageContainer) editorPart).getSelectionStore();
			}
		} catch (Exception e) {
			FlightRecorderUI.getDefault().getLogger().log(Level.INFO,
					"Got exception while trying to get the active editor", e);
		}
		return null;
	}

	@Override
	public void setFocus() {
		if (sash != null) {
			sash.setFocus();
		}
	}

	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		if (sash != null && !sash.isDisposed()) {
			IMemento sashMemento = memento.createChild(SASH_ELEMENT);
			int[] weights = sash.getWeights();
			if (weights.length > 0) {
				sashMemento.putInteger("weight0", weights[0]);
			}
			if (weights.length > 1) {
				sashMemento.putInteger("weight1", weights[1]);
			}
		}
	}

	@Override
	public void dispose() {
		if (getSite() != null && getSite().getWorkbenchWindow() != null) {
			getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(this);
		}
		super.dispose();
	}

	public void showMessage() {
		if (stack != null && stack.topControl == contentComposite) {
			sashWeights = sash.getWeights();
			stack.topControl = messageComposite;
		}
		parentComposite.layout();
	}

	public void showContent() {
		if (stack.topControl != contentComposite) {
			if (sashWeights != null) {
				sash.setWeights(sashWeights);
			}
			stack.topControl = contentComposite;
			parentComposite.layout();
		}
	}
}
