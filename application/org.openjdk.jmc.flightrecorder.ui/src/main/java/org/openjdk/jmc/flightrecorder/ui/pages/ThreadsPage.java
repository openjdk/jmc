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

import static org.openjdk.jmc.common.item.Aggregators.max;
import static org.openjdk.jmc.common.item.Aggregators.min;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IAttribute;
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
import org.openjdk.jmc.flightrecorder.ui.common.DropdownLaneFilter;
import org.openjdk.jmc.flightrecorder.ui.common.ThreadGraphLanes;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.charts.IXDataRenderer;
import org.openjdk.jmc.ui.charts.QuantitySpanRenderer;
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
			return new String[] {JfrRuleTopics.THREADS};
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

	private class ThreadsPageUi extends ThreadsPageLayoutUI {
		private static final String THREADS_TABLE_FILTER = "threadsTableFilter"; //$NON-NLS-1$
		private static final String FOLD_CHART_ACTION = "foldChartAction"; //$NON-NLS-1$
		private static final String FOLD_TABLE_ACTION = "foldTableAction"; //$NON-NLS-1$
		private static final String HIDE_THREAD = "hideThread"; //$NON-NLS-1$
		private static final String RESET_CHART = "resetChart"; //$NON-NLS-1$
		public static final String TOOLBAR_FOLD_ACTIONS = "foldActions"; //$NON-NLS-1$
		private Boolean isChartMenuActionsInit;
		private Boolean isChartModified;
		private Boolean reloadThreads;
		private IAction foldChartAction;
		private IAction foldTableAction;
		private IAction hideThreadActionChart;
		private IAction hideThreadActionText;
		private IAction resetChartAction;
		private int[] weights;
		private List<IXDataRenderer> threadRows;
		private MCContextMenuManager mmChart;
		private MCContextMenuManager mmText;
		private MCContextMenuManager[] mms;
		private ThreadGraphLanes lanes;
		private DropdownLaneFilter laneFilter;

		ThreadsPageUi(Composite parent, FormToolkit toolkit, IPageContainer editor, IState state) {
			super(pageFilter, getDataSource(), parent, toolkit, editor, state, getName(), pageFilter, getIcon(),
					flavorSelectorState, JfrAttributes.EVENT_THREAD);
			mmChart = (MCContextMenuManager) chartCanvas.getContextMenu();
			mmText = (MCContextMenuManager) textCanvas.getContextMenu();
			mms = new MCContextMenuManager[] {mmChart, mmText};
			initializeStoredSashWeights();
			canvasSash.setOrientation(SWT.HORIZONTAL);
			addResizeListenerToTableAndChartComponents();
			addActionsToContextMenu();
			// FIXME: The lanes field is initialized by initializeChartConfiguration which is called by the super constructor. This is too indirect for SpotBugs to resolve and should be simplified.
			lanes.updateContextMenus(mms, false);
			addActionsToToolbar(form.getToolBarManager());
			chartLegend.getControl().dispose();
			form.getToolBarManager().update(true);
			setupLaneFilter();
			buildChart(true);
			table.getManager().setSelectionState(histogramSelectionState);
			tableFilterComponent.loadState(state.getChild(THREADS_TABLE_FILTER));
			for (Item columnWidget : ((TableViewer) table.getManager().getViewer()).getTable().getColumns()) {
				columnWidget.addListener(SWT.Selection, e -> buildChart(false));
			}
			chart.setVisibleRange(visibleRange.getStart(), visibleRange.getEnd());
			onFilterChange(tableFilter);
		}

		private void addActionsToToolbar(IToolBarManager tb) {
			foldTableAction = ActionToolkit.checkAction(selected -> {
				performToolbarAction(FOLD_TABLE_ACTION, selected);
			}, sash.getWeights()[0] == 0 ? Messages.ThreadsPage_SHOW_TABLE_TOOLTIP
					: Messages.ThreadsPage_FOLD_TABLE_TOOLTIP,
					FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.ICON_TABLE));
			foldTableAction.setChecked(sash.getWeights()[0] == 0 ? false : true);

			foldChartAction = ActionToolkit.checkAction(selected -> {
				performToolbarAction(FOLD_CHART_ACTION, selected);
			}, sash.getWeights()[1] == 0 ? Messages.ThreadsPage_SHOW_CHART_TOOLTIP
					: Messages.ThreadsPage_FOLD_CHART_TOOLTIP,
					FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.ICON_CHART_BAR));
			foldChartAction.setChecked(sash.getWeights()[1] == 0 ? false : true);

			tb.add(new GroupMarker(TOOLBAR_FOLD_ACTIONS));
			tb.appendToGroup(TOOLBAR_FOLD_ACTIONS, foldTableAction);
			tb.appendToGroup(TOOLBAR_FOLD_ACTIONS, foldChartAction);
			tb.appendToGroup(TOOLBAR_FOLD_ACTIONS, new Separator());

			tb.add(ActionToolkit.action(() -> lanes.openEditLanesDialog(mms, false), Messages.ThreadsPage_EDIT_LANES,
					FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.ICON_LANES_EDIT)));
		}

		private void addResizeListenerToTableAndChartComponents() {
			tableFilterComponent.getComponent().addListener(SWT.Resize, new Listener() {
				@Override
				public void handleEvent(Event event) {
					if (!foldTableAction.isChecked() && tableFilterComponent.getComponent().getSize().y > 0) {
						foldTableAction.setChecked(true);
					}
				}
			});

			canvasSash.addListener(SWT.Resize, new Listener() {
				@Override
				public void handleEvent(Event event) {
					if (!foldChartAction.isChecked() && chartCanvas.getSize().y > 0) {
						foldChartAction.setChecked(true);
					}
				}
			});
		}

		private void performToolbarAction(String action, boolean selected) {
			switch (action) {
			case FOLD_TABLE_ACTION:
				if (selected) {
					sash.setWeights(this.getStoredSashWeights());
					foldTableAction.setToolTipText(Messages.ThreadsPage_FOLD_TABLE_TOOLTIP);
				} else {
					// if the chart is folded, don't fold the table
					if (sash.getWeights()[1] == 0) {
						this.foldTableAction.setChecked(true);
					} else {
						this.setStoredSashWeights(sash.getWeights());
						sash.setWeights(new int[] {0, 2});
						foldTableAction.setToolTipText(Messages.ThreadsPage_SHOW_TABLE_TOOLTIP);
					}
				}
				break;
			case FOLD_CHART_ACTION:
				if (selected) {
					sash.setWeights(this.getStoredSashWeights());
					foldChartAction.setToolTipText(Messages.ThreadsPage_FOLD_CHART_TOOLTIP);
				} else {
					// if the table is folded, don't fold the chart
					if (sash.getWeights()[0] == 0) {
						this.foldChartAction.setChecked(true);
					} else {
						this.setStoredSashWeights(sash.getWeights());
						sash.setWeights(new int[] {1, 0});
						foldChartAction.setToolTipText(Messages.ThreadsPage_SHOW_CHART_TOOLTIP);
					}
				}
				break;
			}
		}

		private void initializeStoredSashWeights() {
			// if either the chart or table are folded on init, store a default value of {1, 2}
			if (sash.getWeights()[0] == 0 || sash.getWeights()[1] == 0) {
				this.setStoredSashWeights(new int[] {1, 2});
			} else {
				this.setStoredSashWeights(sash.getWeights());
			}
		}

		protected int[] getStoredSashWeights() {
			return this.weights;
		}

		protected void setStoredSashWeights(int[] weights) {
			this.weights = weights;
		}

		private void setupLaneFilter() {
			MCContextMenuManager[] mms = {mmChart, mmText};
			laneFilter = new DropdownLaneFilter(controlBar.getLaneFilterContainer(), lanes, mms);
			laneFilter.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		}

		/**
		 * Hides a thread from the chart and rebuilds the chart
		 */
		private void hideThread(Object thread) {
			if (this.threadRows != null && this.threadRows.size() > 0 && thread instanceof IMCThread) {
				int index = indexOfThread(thread);
				if (index != -1) {
					this.threadRows.remove(index);
					this.reloadThreads = false;
					buildChart(false);
					if (!this.isChartModified) {
						this.isChartModified = true;
						setResetChartActionEnablement(true);
					}
				}
				if (this.threadRows.size() == 0) {
					setHideThreadActionEnablement(false);
				}
			}
		}

		/**
		 * Locates the index of the target Thread in the current selection list
		 *
		 * @param thread
		 *            the thread of interest
		 * @return the index of the thread in the current selection, or -1 if not found
		 */
		private int indexOfThread(Object thread) {
			for (int i = 0; i < this.threadRows.size() && thread != null; i++) {
				if (this.threadRows.get(i) instanceof QuantitySpanRenderer) {
					if (thread.equals(((QuantitySpanRenderer) this.threadRows.get(i)).getData())) {
						return i;
					}
				}
			}
			return -1;
		}

		/**
		 * Update the context menu to include actions to hide threads and reset the chart
		 */
		private void addActionsToContextMenu() {
			mmChart.add(new Separator());
			mmText.add(new Separator());
			IAction hideThreadActionChart = ActionToolkit.action(
					() -> this.hideThread(chartCanvas.getHoveredItemData()), Messages.ThreadsPage_HIDE_THREAD_ACTION,
					UIPlugin.getDefault().getMCImageDescriptor(UIPlugin.ICON_DELETE));
			hideThreadActionChart.setId(HIDE_THREAD);
			this.hideThreadActionChart = hideThreadActionChart;
			mmChart.add(hideThreadActionChart);

			IAction hideThreadActionText = ActionToolkit.action(() -> this.hideThread(textCanvas.getHoveredItemData()),
					Messages.ThreadsPage_HIDE_THREAD_ACTION,
					UIPlugin.getDefault().getMCImageDescriptor(UIPlugin.ICON_DELETE));
			hideThreadActionText.setId(HIDE_THREAD);
			this.hideThreadActionText = hideThreadActionText;
			mmText.add(hideThreadActionText);

			IAction resetChartAction = ActionToolkit.action(() -> this.resetChartToSelection(),
					Messages.ThreadsPage_RESET_CHART_TO_SELECTION_ACTION,
					UIPlugin.getDefault().getMCImageDescriptor(UIPlugin.ICON_REFRESH));
			resetChartAction.setId(RESET_CHART);
			resetChartAction.setEnabled(this.isChartModified);
			this.resetChartAction = resetChartAction;
			mmChart.add(resetChartAction);
			mmText.add(resetChartAction);

			this.isChartMenuActionsInit = true;
		}

		/**
		 * Redraws the chart, and disables the reset chart menu action
		 */
		private void resetChartToSelection() {
			buildChart(false);
			this.isChartModified = false;
			setResetChartActionEnablement(false);
			setHideThreadActionEnablement(true);
		}

		private void setHideThreadActionEnablement(Boolean enabled) {
			this.hideThreadActionChart.setEnabled(enabled);
			this.hideThreadActionText.setEnabled(enabled);
		}

		private void setResetChartActionEnablement(Boolean enabled) {
			this.resetChartAction.setEnabled(enabled);
		}

		@Override
		protected ItemHistogram buildHistogram(Composite parent, IState state, IAttribute<?> classifier) {
			ItemHistogram build = HISTOGRAM.buildWithoutBorder(parent, classifier, TableSettings.forState(state));
			return build;
		}

		@Override
		protected IXDataRenderer getChartRenderer(IItemCollection itemsInTable, HistogramSelection tableSelection) {
			List<IXDataRenderer> rows = new ArrayList<>();
			ItemHistogram histogram = table;
			IItemCollection selectedItems;
			HistogramSelection selection;
			if (tableSelection.getRowCount() == 0) {
				selectedItems = itemsInTable;
				selection = histogram.getAllRows();
			} else {
				selectedItems = tableSelection.getItems();
				selection = tableSelection;
			}
			boolean useDefaultSelection = rows.size() > 1;
			if (lanes.getLaneDefinitions().stream().anyMatch(a -> a.isEnabled()) && selection.getRowCount() > 0) {
				if (this.reloadThreads) {
					this.threadRows = selection
							.getSelectedRows((object, items) -> lanes.buildThreadRenderer(object, items))
							.collect(Collectors.toList());
					chartCanvas.setNumItems(this.threadRows.size());
					textCanvas.setNumItems(this.threadRows.size());
					this.isChartModified = false;
					if (this.isChartMenuActionsInit) {
						setResetChartActionEnablement(false);
						setHideThreadActionEnablement(true);
					}
				} else {
					this.reloadThreads = true;
				}

				double threadsWeight = Math.sqrt(threadRows.size()) * 0.15;
				double otherRowWeight = Math.max(threadsWeight * 0.1, (1 - threadsWeight) / rows.size());
				List<Double> weights = Stream
						.concat(Stream.generate(() -> otherRowWeight).limit(rows.size()), Stream.of(threadsWeight))
						.collect(Collectors.toList());
				rows.add(RendererToolkit.uniformRows(this.threadRows));
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
			lanes.saveTo(state);
			saveToLocal();
		}

		private void saveToLocal() {
			flavorSelectorState = flavorSelector.getFlavorSelectorState();
			histogramSelectionState = table.getManager().getSelectionState();
			visibleRange = chart.getVisibleRange();
		}

		@Override
		protected List<IAction> initializeChartConfiguration(IState state) {
			this.isChartMenuActionsInit = false;
			this.isChartModified = false;
			this.reloadThreads = true;
			lanes = new ThreadGraphLanes(() -> getDataSource(), () -> buildChart(false));
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
