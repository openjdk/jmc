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
package org.openjdk.jmc.flightrecorder.ui.pages.itemhandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.LinearKindOfQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.Pair;
import org.openjdk.jmc.common.util.StateToolkit;
import org.openjdk.jmc.common.util.StringToolkit;
import org.openjdk.jmc.common.util.TypeHandling;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.IPageContainer;
import org.openjdk.jmc.flightrecorder.ui.ItemCollectionToolkit;
import org.openjdk.jmc.flightrecorder.ui.common.BucketBuilder;
import org.openjdk.jmc.flightrecorder.ui.common.DataPageToolkit;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram.HistogramSelection;
import org.openjdk.jmc.flightrecorder.ui.common.ItemList;
import org.openjdk.jmc.flightrecorder.ui.common.ItemRow;
import org.openjdk.jmc.flightrecorder.ui.common.TypeLabelProvider;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStoreActionToolkit;
import org.openjdk.jmc.ui.charts.IXDataRenderer;
import org.openjdk.jmc.ui.charts.RendererToolkit;
import org.openjdk.jmc.ui.charts.XYChart;
import org.openjdk.jmc.ui.charts.XYDataRenderer;
import org.openjdk.jmc.ui.misc.ChartCanvas;

class ItemChart {

	private static final int CHART_TITLE_OFFSET = 180;
	private static final String AGGREGATOR_DELIMITER = ";"; //$NON-NLS-1$
	private static final String ATTRIBUTE_DELIMITER = ","; //$NON-NLS-1$
	private static final String COLOR_BY = "colorBy"; //$NON-NLS-1$
	private static final String LINE_CHART_ATTRIBUTES = "lineChartAttributes"; //$NON-NLS-1$
	private static final String BAR_CHART_ATTRIBUTES = "barChartAttributes"; //$NON-NLS-1$
	private static final String BAR_CHART_TYPES = "barChartTypes"; //$NON-NLS-1$
	private static final String BAR_CHART_AGGREGATES = "barChartAggregates"; //$NON-NLS-1$
	private static final String SHOW_GROUPING_AS_SPAN_CHART = "showSpanChart"; //$NON-NLS-1$

	private static final String SPAN_CONFIG_MENU_GROUP = "SpanConfig"; //$NON-NLS-1$
	private static final String CHART_MENU_GROUP = "Chart"; //$NON-NLS-1$
	private static final String OTHER_MENU_GROUP = "Other"; //$NON-NLS-1$

	private IMenuManager chartContextMenuManager;
	private final Map<LinearKindOfQuantity, List<IAttribute<?>>> attributesToLineChart = new LinkedHashMap<>();
	private final Map<String, Pair<IAttribute<?>, IAggregator<?, ?>>> attributeAggregatesToBarChart = new LinkedHashMap<>();
	private final Map<String, Pair<IType<?>, IAggregator<?, ?>>> typeAggregatesToBarChart = new LinkedHashMap<>();
	private final List<IAggregator<IQuantity, ?>> aggregatesToBarChart = new ArrayList<>();
	private final IPageContainer controller;
	private ChartCanvas chartCanvas;
	private IAttribute<?> colorBy;

	private IItemCollection chartItems;
	private IRange<IQuantity> currentRange;
	private HistogramSelection histogramSelection;
	private boolean grouped = false;
	private boolean showSpanChart;
	private AttributeComponentConfiguration acc;
	private String pageName;
	private XYChart xyChart;

	public ItemChart(Composite container, FormToolkit toolkit, String pageName, AttributeComponentConfiguration acc,
			IState state, IPageContainer controller) {
		this.pageName = pageName;
		this.controller = controller;
		this.acc = acc;
		initializeChartAttributes(state);
		createChart(container, toolkit);
		initializeChartMenu();
	}

	public Control getControl() {
		return chartCanvas;
	}

	private void addMenuOption(MenuManager menu, String group) {
		if (chartContextMenuManager.find(menu.getId()) == null) {
			if (group != null) {
				chartContextMenuManager.appendToGroup(group, menu);
			} else {
				chartContextMenuManager.add(menu);
			}
		}
	}

	private void createChart(Composite parent, FormToolkit toolkit) {
		if (chartCanvas == null) {
			chartCanvas = new ChartCanvas(parent);
			toolkit.adapt(chartCanvas);
			DataPageToolkit.createChartTimestampTooltip(chartCanvas);
			IXDataRenderer cleanRow = RendererToolkit.uniformRows(Collections.emptyList());
			xyChart = new XYChart(controller.getRecordingRange(), cleanRow, CHART_TITLE_OFFSET);
			DataPageToolkit.setChart(chartCanvas, xyChart, controller::showSelection);
			chartContextMenuManager = chartCanvas.getContextMenu();
			SelectionStoreActionToolkit.addSelectionStoreRangeActions(controller.getSelectionStore(), xyChart,
					JfrAttributes.LIFETIME, NLS.bind(Messages.ITEM_CHART_TIMELINE_SELECTION, pageName),
					chartContextMenuManager);
		}
	}

	private void initializeChartAttributes(IState chartSettings) {
		if (chartSettings == null) {
			onShowAggregateInBarChart(Aggregators.count(), true);
		} else {
			String colorByAttribute = chartSettings.getAttribute(COLOR_BY);
			if (colorByAttribute != null) {
				colorBy = acc.getAllAttributes().get(colorByAttribute);
			}
			String lineChartAttributes = chartSettings.getAttribute(LINE_CHART_ATTRIBUTES);
			if (lineChartAttributes != null) {
				for (String a : lineChartAttributes.split(ATTRIBUTE_DELIMITER)) {
					IAttribute<?> attribute = acc.getLineChartableAttributes().get(a);
					showAttributeInLineChart(attribute, true);
				}
			}
			String barChartAttributes = chartSettings.getAttribute(BAR_CHART_ATTRIBUTES);
			if (barChartAttributes != null) {
				for (String attrAggrString : barChartAttributes.split(ATTRIBUTE_DELIMITER)) {
					IAggregator<?, ?> aggregator = null;
					IAttribute<?> attribute = null;
					String[] attrAggr = attrAggrString.split(AGGREGATOR_DELIMITER);
					if (attrAggr.length >= 2) {
						String attributeString = attrAggr[0];
						String aggregatorString = attrAggr[1];
						attribute = acc.getCommonChartableAttributes().get(attributeString);
						if (attribute == null) {
							attribute = acc.getUncommonChartableAttributes().get(attributeString);
						}
						if (attribute != null && attribute.getContentType() instanceof LinearKindOfQuantity) {
							@SuppressWarnings("unchecked")
							IAttribute<IQuantity> qAttribute = (IAttribute<IQuantity>) attribute;
							aggregator = Aggregators.getQuantityAggregator(aggregatorString, qAttribute);
						}
					}
					if (aggregator != null && attribute != null) {
						showAttributeAggregateInBarChart(attribute, aggregator, true);
					} else {
						FlightRecorderUI.getDefault().getLogger().log(Level.WARNING,
								"Unable to read in chart state, undefined attribute/aggregator tuple: " //$NON-NLS-1$
										+ attrAggrString);
					}
				}
			}
			String barChartTypes = chartSettings.getAttribute(BAR_CHART_TYPES);
			if (barChartTypes != null) {
				for (String typeAggrString : barChartTypes.split(ATTRIBUTE_DELIMITER)) {
					IAggregator<IQuantity, ?> aggregator = null;
					IType<?> type = null;
					String[] typeAggr = typeAggrString.split(AGGREGATOR_DELIMITER);
					if (typeAggr.length >= 2) {
						String typeString = typeAggr[0];
						String aggregatorString = typeAggr[1];
						type = acc.getAllTypes().get(typeString);
						aggregator = Aggregators.getQuantityAggregator(aggregatorString, type);
					}
					if (aggregator != null) {
						showTypeAggregateInBarChart(type, aggregator, true);
					} else {
						FlightRecorderUI.getDefault().getLogger().log(Level.WARNING,
								"Unable to read in chart state, undefined type/aggregator tuple: " + typeAggrString); //$NON-NLS-1$
					}
				}
			}
			String barChartAggregators = chartSettings.getAttribute(BAR_CHART_AGGREGATES);
			if (barChartAggregators != null) {
				for (String aggrString : barChartAggregators.split(ATTRIBUTE_DELIMITER)) {
					IAggregator<IQuantity, ?> aggregator = Aggregators.getQuantityAggregator(aggrString);
					if (aggregator != null) {
						showAggregateInBarChart(aggregator, true);
					} else {
						FlightRecorderUI.getDefault().getLogger().log(Level.WARNING,
								"Unable to read in chart state, undefined aggregator tuple: " + aggrString); //$NON-NLS-1$
					}
				}
			}

			showSpanChart = StateToolkit.readBoolean(chartSettings, SHOW_GROUPING_AS_SPAN_CHART, false);
		}
	}

	public void onGrouped(Boolean grouped) {
		if (grouped != this.grouped) {
			this.grouped = grouped;
		}
		updateCharts();
	}

	private void initializeChartMenu() {
		chartContextMenuManager.add(new Separator());
		chartContextMenuManager.add(new GroupMarker(CHART_MENU_GROUP));
		chartContextMenuManager.appendToGroup(CHART_MENU_GROUP, new Separator());
		chartContextMenuManager.add(new GroupMarker(SPAN_CONFIG_MENU_GROUP));
		chartContextMenuManager.appendToGroup(SPAN_CONFIG_MENU_GROUP, new Separator());
		chartContextMenuManager.add(new GroupMarker(OTHER_MENU_GROUP));
		chartContextMenuManager.appendToGroup(OTHER_MENU_GROUP, new Separator());

		List<IAggregator<IQuantity, ?>> aggregators = new ArrayList<>();
		aggregators.add(Aggregators.count());
		List<Function<IType<?>, IAggregator<IQuantity, ?>>> typeAggregators = new ArrayList<>();
		typeAggregators.add(type -> Aggregators.count(type));

		List<Function<IAttribute<?>, IAggregator<IQuantity, ?>>> attributeAggregators = createAttributeAggregators();

		// TODO: Allow the user to select type/attribute/aggregator combinations?
		addMenuOption(AttributeMenuFactory.aggregatorMenu(this::onShowAggregateInBarChart,
				this::onShowAttributeAggregateInBarChart, this::onShowTypeAggregateInBarChart,
				() -> acc.getCommonChartableAttributes().values().stream(),
				() -> acc.getUncommonChartableAttributes().values().stream(), attributeAggregators,
				() -> acc.getAllTypes().values().stream(), typeAggregators.get(0), aggregators,
				Messages.ITEM_CHART_SHOW_IN_BAR_CHART, true, this::chartState), CHART_MENU_GROUP);

		addMenuOption(AttributeMenuFactory.attributeMenu(false, this::onShowAttributeInLineChart,
				() -> acc.getLineChartableAttributes().values().stream(), () -> Stream.empty(),
				Messages.ITEM_CHART_SHOW_IN_LINE_CHART, true, this::chartState), CHART_MENU_GROUP);

		chartContextMenuManager.appendToGroup(SPAN_CONFIG_MENU_GROUP,
				new Action(Messages.ITEM_CHART_SHOW_GROUPING_AS_SPAN_CHART, IAction.AS_CHECK_BOX) {
					{
						setChecked(showSpanChart);
					}

					@Override
					public void run() {
						onShowSpanChart(isChecked());
					}

					@Override
					public boolean isEnabled() {
						return grouped;
					};
				});
		addMenuOption(AttributeMenuFactory.attributeMenu(true, () -> grouped && showSpanChart, this::onColorBy,
				() -> acc.getCommonAttributes().values().stream(), () -> acc.getUncommonAttributes().values().stream(),
				Messages.ITEM_CHART_COLOR_SPAN_BY, true, this::colorbyState), SPAN_CONFIG_MENU_GROUP);

		// FIXME: Decide if we want a separate Clear Chart option or not.
//		chartContextMenuManager.appendToGroup(CHART_MENU_GROUP, new Action(Messages.ITEM_CHART_CLEAR_CHART) {
//			@Override
//			public void run() {
//				onClearChart();
//			}
//
//			@Override
//			public boolean isEnabled() {
//				return showSpanChart || attributeAggregatesToBarChart.isEmpty() || !attributesToLineChart.isEmpty()
//						|| !typeAggregatesToBarChart.isEmpty() || !aggregatesToBarChart.isEmpty();
//
//			};
//		});
	}

	@SuppressWarnings("unchecked")
	private List<Function<IAttribute<?>, IAggregator<IQuantity, ?>>> createAttributeAggregators() {
		List<Function<IAttribute<?>, IAggregator<IQuantity, ?>>> attributeAggregators = new ArrayList<>();
		attributeAggregators.add(a -> Aggregators.max((IAttribute<IQuantity>) a));
		attributeAggregators.add(a -> Aggregators.min((IAttribute<IQuantity>) a));
		attributeAggregators.add(a -> Aggregators.sum((IAttribute<IQuantity>) a));
		attributeAggregators.add(a -> Aggregators.avg((IAttribute<IQuantity>) a));
		return attributeAggregators;
	}

	public void update(
		IItemCollection chartItems, IRange<IQuantity> currentRange, HistogramSelection histogramSelection,
		Boolean grouped) {
		// FIXME: Can we remove histogramSelection and grouped, and instead check if chartItems is actually a histogramSelection?
		this.chartItems = chartItems;
		this.currentRange = currentRange;
		this.histogramSelection = histogramSelection;
		this.grouped = grouped;
		updateCharts();
	}

	private void updateCharts() {
		if (chartCanvas != null) {
			IXDataRenderer rendererRoot;
			if (((showSpanChart && grouped) || !attributesToLineChart.isEmpty()
					|| !attributeAggregatesToBarChart.isEmpty() || !typeAggregatesToBarChart.isEmpty()
					|| !aggregatesToBarChart.isEmpty())) {
				List<IXDataRenderer> rows = new ArrayList<>();
				IItemCollection itemsToChart = grouped && histogramSelection != null ? histogramSelection.getItems()
						: chartItems;
				if (grouped) {
					if (showSpanChart) {
						rows.addAll(updateSpanChart(histogramSelection));
					}
				}
				if (ItemCollectionToolkit.stream(chartItems).count() > 0 && !attributesToLineChart.isEmpty()) {
					rows.addAll(updateLineChart(chartItems));
				}
				rows.addAll(updateBarChart(itemsToChart));

				rendererRoot = RendererToolkit.uniformRows(rows);
			} else {
				rendererRoot = RendererToolkit.uniformRows(Collections.emptyList());
			}
			xyChart.setRendererRoot(rendererRoot);
			chartCanvas.setChart(xyChart);
			chartContextMenuManager.update(IAction.ENABLED);
		}
	}

	private void showAttributeInLineChart(IAttribute<?> a, Boolean enabled) {
		if (a != null) {
			ContentType<?> q = a.getContentType();
			if (q instanceof LinearKindOfQuantity) {
				List<IAttribute<?>> aList = attributesToLineChart.get(q);
				if (aList == null) {
					aList = new ArrayList<>();
					attributesToLineChart.put((LinearKindOfQuantity) q, aList);
				}
				if (!aList.contains(a) && enabled) {
					aList.add(a);
				} else if (aList.contains(a) && !enabled) {
					aList.remove(a);
				}
			}
		}
	}

	private void onShowAttributeInLineChart(IAttribute<?> a, Boolean enabled) {
		showAttributeInLineChart(a, enabled);
		updateCharts();
	}

	private void showAggregateInBarChart(IAggregator<IQuantity, ?> aggregator, Boolean enabled) {
		if (aggregator != null) {
			if (!aggregatesToBarChart.contains(aggregator) && enabled) {
				aggregatesToBarChart.add(aggregator);
			} else if (aggregatesToBarChart.contains(aggregator) && !enabled) {
				aggregatesToBarChart.remove(aggregator);
			}
		}
	}

	private void showTypeAggregateInBarChart(IType<?> type, IAggregator<?, ?> aggregator, Boolean enabled) {
		if (aggregator != null) {
			String id = getKey(type, aggregator);
			if (!typeAggregatesToBarChart.containsKey(id) && enabled) {
				typeAggregatesToBarChart.put(id, new Pair<IType<?>, IAggregator<?, ?>>(type, aggregator));
			} else if (typeAggregatesToBarChart.containsKey(id) && !enabled) {
				typeAggregatesToBarChart.remove(id);
			}
		}
	}

	private void showAttributeAggregateInBarChart(
		IAttribute<?> attribute, IAggregator<?, ?> aggregator, Boolean enabled) {
		if (aggregator != null) {
			String id = getKey(attribute, aggregator);
			if (!attributeAggregatesToBarChart.containsKey(id) && enabled) {
				Pair<IAttribute<?>, IAggregator<?, ?>> pair = new Pair<>(attribute, aggregator);
				attributeAggregatesToBarChart.put(id, pair);
			} else if (attributeAggregatesToBarChart.containsKey(id) && !enabled) {
				attributeAggregatesToBarChart.remove(id);
			}
		}
	}

	private void onShowAggregateInBarChart(IAggregator<IQuantity, ?> aggregator, Boolean enabled) {
		showAggregateInBarChart(aggregator, enabled);
		updateCharts();
	}

	private void onShowTypeAggregateInBarChart(IType<?> type, IAggregator<?, ?> aggregator, Boolean enabled) {
		showTypeAggregateInBarChart(type, aggregator, enabled);
		updateCharts();
	}

	private void onShowAttributeAggregateInBarChart(
		IAttribute<?> attribute, IAggregator<?, ?> aggregator, Boolean enabled) {
		showAttributeAggregateInBarChart(attribute, aggregator, enabled);
		updateCharts();
	}

	// FIXME: Decide if we want a separate Clear Chart option or not.
//	protected void onClearChart() {
////		showSpanChart = false;
//		attributesToLineChart.clear();
//		attributeAggregatesToBarChart.clear();
//		typeAggregatesToBarChart.clear();
//		aggregatesToBarChart.clear();
//		updateCharts();
//	}

	protected void onShowSpanChart(boolean show) {
		showSpanChart = show;
		updateCharts();
	}

	private void onColorBy(IAttribute<?> a, Boolean ignore) {
		colorBy = a;
		updateCharts();
	}

	Boolean chartState(Object chartItem) {
		if (chartItem instanceof IAttribute) {
			return attributesToLineChart.values().stream().flatMap(l -> l.stream()).anyMatch(a -> a.equals(chartItem));
		} else if (chartItem instanceof IAggregator) {
			return aggregatesToBarChart.contains(chartItem);
		} else if (chartItem instanceof Pair && ((Pair<?, ?>) chartItem).right instanceof IAggregator) {
			@SuppressWarnings("unchecked")
			Pair<?, IAggregator<?, ?>> p = (Pair<?, IAggregator<?, ?>>) chartItem;
			if (p.left instanceof IType) {
				return typeAggregatesToBarChart.containsKey(getKey((IType<?>) p.left, p.right));
			} else if (p.left instanceof IAttribute) {
				return attributeAggregatesToBarChart.containsKey(getKey((IAttribute<?>) p.left, p.right));
			}
		}
		return false;
	}

	Boolean colorbyState(Object colorAttribute) {
		return Objects.equals(colorBy, colorAttribute);
	}

	void setVisibleRange(IRange<IQuantity> visibleRange) {
		xyChart.setVisibleRange(visibleRange.getStart(), visibleRange.getEnd());
	}

	IRange<IQuantity> getVisibleRange() {
		return xyChart.getVisibleRange();
	}

	private String getKey(IAttribute<?> attribute, IAggregator<?, ?> aggregator) {
		return ItemList.getColumnId(attribute) + AGGREGATOR_DELIMITER + aggregator.getName();
	}

	private String getKey(IType<?> type, IAggregator<?, ?> aggregator) {
		return (type != null ? type.getIdentifier() : "null") + AGGREGATOR_DELIMITER + aggregator.getName(); //$NON-NLS-1$
	}

	public void saveState(IWritableState chartSettings) {
		if (colorBy != null) {
			chartSettings.putString(COLOR_BY, ItemList.getColumnId(colorBy));
		}

		if (!attributeAggregatesToBarChart.isEmpty()) {
			chartSettings.putString(BAR_CHART_ATTRIBUTES,
					attributeAggregatesToBarChart.keySet().stream().collect(Collectors.joining(ATTRIBUTE_DELIMITER)));
		}
		if (!typeAggregatesToBarChart.isEmpty()) {
			chartSettings.putString(BAR_CHART_TYPES,
					typeAggregatesToBarChart.keySet().stream().collect(Collectors.joining(ATTRIBUTE_DELIMITER)));
		}
		if (!aggregatesToBarChart.isEmpty()) {
			chartSettings.putString(BAR_CHART_AGGREGATES, aggregatesToBarChart.stream().map(a -> a.getName())
					.collect(Collectors.joining(ATTRIBUTE_DELIMITER)));
		}
		if (!attributesToLineChart.isEmpty()) {
			chartSettings.putString(LINE_CHART_ATTRIBUTES,
					attributesToLineChart.values().stream().flatMap(l -> l.stream()).map(a -> ItemList.getColumnId(a))
							.collect(Collectors.joining(ATTRIBUTE_DELIMITER)));
		}
		StateToolkit.writeBoolean(chartSettings, SHOW_GROUPING_AS_SPAN_CHART, showSpanChart);
	}

	private Collection<ItemRow> updateLineChart(IItemCollection itemsToChart) {
		// TODO: Can we get for example "Heap Used" as two different line charts, for "Before GC" and "After GC"?
		boolean fill = false;
		if (attributesToLineChart.size() > 0 && itemsToChart.hasItems()) {
			List<ItemRow> rows = new ArrayList<>();
			for (Entry<LinearKindOfQuantity, List<IAttribute<?>>> entry : attributesToLineChart.entrySet()) {
				List<IAttribute<?>> attributes = entry.getValue();
				if (attributes != null && attributes.size() > 0) {
					String name = StringToolkit.join(
							attributes.stream().map(a -> a.getName()).collect(Collectors.toList()),
							ATTRIBUTE_DELIMITER);
					String description = StringToolkit.join(
							attributes.stream().map(b -> b.getDescription()).collect(Collectors.toList()),
							ATTRIBUTE_DELIMITER);

					XYDataRenderer xyRenderer = new XYDataRenderer(entry.getKey().getDefaultUnit().quantity(0), name,
							description);
					for (IAttribute<?> attribute : attributes) {
						// TODO: something other than time on x-axis?
						Iterator<IItemIterable> chartItemsWithAttributeSomeType = itemsToChart
								.apply(ItemFilters.hasAttribute(attribute)).iterator();
						if (chartItemsWithAttributeSomeType.hasNext()
								&& attribute.getContentType() instanceof LinearKindOfQuantity) {
							IItemIterable is = chartItemsWithAttributeSomeType.next();
							if (chartItemsWithAttributeSomeType.hasNext()) {
								// FIXME: JMC-4520 - Add support for multiple item iterables
								FlightRecorderUI.getDefault().getLogger().log(Level.INFO,
										"Only charting a subset of the events!"); //$NON-NLS-1$
							}
							@SuppressWarnings("unchecked")
							IAttribute<IQuantity> qAttribute = (IAttribute<IQuantity>) attribute;
							DataPageToolkit.addEndTimeLine(xyRenderer, is.iterator(), is.getType(), qAttribute, fill);
						}
					}
					rows.add(new ItemRow(name, description, xyRenderer, itemsToChart));
				}
			}
			return rows;

		} else {
			return Collections.emptyList();
		}
	}

	private Collection<ItemRow> updateBarChart(IItemCollection itemsToChart) {
		// TODO: allow something other than time on x-axis?
		if (attributeAggregatesToBarChart.size() > 0 || typeAggregatesToBarChart.size() > 0
				|| aggregatesToBarChart.size() > 0) {
			List<ItemRow> rows = new ArrayList<>();
			for (Pair<IAttribute<?>, IAggregator<?, ?>> attributeAggregatorPair : attributeAggregatesToBarChart
					.values()) {
				LinearKindOfQuantity ct = (LinearKindOfQuantity) attributeAggregatorPair.left.getContentType();
				IQuantity zero = ct != null ? ct.getDefaultUnit().quantity(0) : UnitLookup.NUMBER_UNITY.quantity(0);
				// FIXME: Different rows, or transparent color? Otherwise total always wins
				@SuppressWarnings("unchecked")
				IAggregator<IQuantity, ?> aggregator = (IAggregator<IQuantity, ?>) attributeAggregatorPair.right;
				XYDataRenderer xyRenderer = new XYDataRenderer(zero, aggregator.getName(), aggregator.getDescription());
				xyRenderer.addBarChart(aggregator.getName(),
						BucketBuilder.aggregatorSeries(itemsToChart, aggregator, JfrAttributes.END_TIME),
						TypeLabelProvider.getColorOrDefault(aggregator.getName()));
				rows.add(new ItemRow(aggregator.getName(), aggregator.getDescription(), xyRenderer, itemsToChart));
			}
			for (Pair<IType<?>, IAggregator<?, ?>> typeAggregatorPair : typeAggregatesToBarChart.values()) {
				if (typeAggregatorPair.right.getValueType() instanceof LinearKindOfQuantity) {
					IQuantity zero = UnitLookup.NUMBER_UNITY.quantity(0);
					@SuppressWarnings("unchecked")
					IAggregator<IQuantity, ?> aggregator = (IAggregator<IQuantity, ?>) typeAggregatorPair.right;
					XYDataRenderer xyRenderer = new XYDataRenderer(zero, aggregator.getName(),
							aggregator.getDescription());
					xyRenderer.addBarChart(aggregator.getName(),
							BucketBuilder.aggregatorSeries(itemsToChart, aggregator, JfrAttributes.END_TIME),
							TypeLabelProvider.getColorOrDefault(aggregator.getName()));
					rows.add(new ItemRow(aggregator.getName(), aggregator.getDescription(), xyRenderer, itemsToChart));
				}
			}
			for (IAggregator<IQuantity, ?> aggregator : aggregatesToBarChart) {
				IQuantity zero = UnitLookup.NUMBER_UNITY.quantity(0);
				XYDataRenderer xyRenderer = new XYDataRenderer(zero, aggregator.getName(), aggregator.getDescription());
				xyRenderer.addBarChart(aggregator.getName(),
						BucketBuilder.aggregatorSeries(itemsToChart, aggregator, JfrAttributes.END_TIME),
						TypeLabelProvider.getColorOrDefault(aggregator.getName()));
				rows.add(new ItemRow(aggregator.getName(), aggregator.getDescription(), xyRenderer, itemsToChart));
			}
			return rows;

		} else {
			return Collections.emptyList();
		}
	}

	private Collection<IXDataRenderer> updateSpanChart(HistogramSelection selection) {
		if (selection != null && selection.getRowCount() > 0) {
			IXDataRenderer spanRoot = RendererToolkit
					.uniformRows(selection.getSelectedRows(this::buildSpanRow).collect(Collectors.toList()));
			return Arrays.asList(spanRoot);
		}
		return Collections.emptyList();
	}

	private ItemRow buildSpanRow(final Object rowTitle, IItemCollection items) {
		String title = rowTitle != null ? TypeHandling.getValueString(rowTitle) : ""; //$NON-NLS-1$
		return buildSpanRow(title, null, items);
	}

	private ItemRow buildSpanRow(String laneTitle, String laneDescription, IItemCollection items) {
		// FIXME: What should we have as the default for the items that don't have the attribute? Type? Grey?
		// FIXME: Probably want more colors to choose from here, it's ok if they are not as distinguishable.
		IXDataRenderer spanRenderer = DataPageToolkit.buildSpanRenderer(items,
				colorBy != null ? DataPageToolkit.getAttributeValueColor(colorBy) : DataPageToolkit.ITEM_COLOR);
		return new ItemRow(laneTitle, laneDescription, spanRenderer, items);
	}

	public IMenuManager getMenuManager() {
		return chartContextMenuManager;
	}

	public void onUseRange(Boolean useRange) {
		IRange<IQuantity> range = useRange ? currentRange : controller.getRecordingRange();
		xyChart.setVisibleRange(range.getStart(), range.getEnd());
		updateCharts();
	}
}
