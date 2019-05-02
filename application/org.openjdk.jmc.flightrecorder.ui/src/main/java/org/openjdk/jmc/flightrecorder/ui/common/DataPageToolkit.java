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
package org.openjdk.jmc.flightrecorder.ui.common;

import java.awt.Color;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.ICanonicalAccessorFactory;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IItemQuery;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.item.ItemToolkit;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.KindOfQuantity;
import org.openjdk.jmc.common.unit.QuantitiesToolkit;
import org.openjdk.jmc.common.unit.QuantityRange;
import org.openjdk.jmc.common.unit.RangeContentType;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.ColorToolkit;
import org.openjdk.jmc.common.util.CompositeKey;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.IPageContainer;
import org.openjdk.jmc.flightrecorder.ui.ItemCollectionToolkit;
import org.openjdk.jmc.flightrecorder.ui.ItemIterableToolkit;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram.CompositeKeyHistogramBuilder;
import org.openjdk.jmc.flightrecorder.ui.common.ItemList.ItemListBuilder;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.overview.ResultOverview;
import org.openjdk.jmc.flightrecorder.ui.selection.IFilterFlavor;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStore.SelectionStoreEntry;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStoreActionToolkit;
import org.openjdk.jmc.ui.charts.AWTChartToolkit.IColorProvider;
import org.openjdk.jmc.ui.charts.IQuantitySeries;
import org.openjdk.jmc.ui.charts.ISpanSeries;
import org.openjdk.jmc.ui.charts.IXDataRenderer;
import org.openjdk.jmc.ui.charts.QuantitySeries;
import org.openjdk.jmc.ui.charts.RendererToolkit;
import org.openjdk.jmc.ui.charts.SpanRenderer;
import org.openjdk.jmc.ui.charts.SubdividedQuantityRange;
import org.openjdk.jmc.ui.charts.XYChart;
import org.openjdk.jmc.ui.charts.XYDataRenderer;
import org.openjdk.jmc.ui.charts.XYQuantities;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.column.TableSettings.ColumnSettings;
import org.openjdk.jmc.ui.handlers.ActionToolkit;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.ChartCanvas;
import org.openjdk.jmc.ui.misc.CompositeToolkit;
import org.openjdk.jmc.ui.misc.DisplayToolkit;
import org.openjdk.jmc.ui.misc.FilterEditor;
import org.openjdk.jmc.ui.misc.FilterEditor.AttributeValueProvider;
import org.openjdk.jmc.ui.misc.OverlayImageDescriptor;
import org.openjdk.jmc.ui.misc.SWTColorToolkit;

public class DataPageToolkit {

	public static final IColorProvider<IItem> ITEM_COLOR = item -> TypeLabelProvider
			.getColorOrDefault(item.getType().getIdentifier());

	public static final IColorProvider<IItem> getAttributeValueColor(final IAttribute<?> attribute) {
		return new IColorProvider<IItem>() {

			@Override
			public Color getColor(IItem item) {
				IMemberAccessor<?, IItem> accessor = attribute.getAccessor(ItemToolkit.getItemType(item));
				Object attributeValue = accessor != null ? accessor.getMember(item) : null;
				// FIXME: Should we include the type or not?
				return attributeValue != null
						? TypeLabelProvider.getColorOrDefault(attribute.getIdentifier() + "=" + attributeValue + "(" //$NON-NLS-1$ //$NON-NLS-2$
								+ item.getType().getIdentifier() + ")") //$NON-NLS-1$
						: ITEM_COLOR.getColor(item);
			}
		};
	}

	private static final Map<String, Color> FIELD_COLOR_MAP = new HashMap<>();
	private static final Map<String, Integer> DEFAULT_COLUMNS_ORDER;

	static {

		// FIXME: Create FieldAppearance class, similar to TypeAppearence?
		FIELD_COLOR_MAP.put(JdkAttributes.MACHINE_TOTAL.getIdentifier(), new Color(255, 128, 0));
		FIELD_COLOR_MAP.put(JdkAttributes.JVM_SYSTEM.getIdentifier(), new Color(128, 128, 128));
		FIELD_COLOR_MAP.put(JdkAttributes.JVM_USER.getIdentifier(), new Color(0, 0, 255));
		FIELD_COLOR_MAP.put(JdkAttributes.JVM_TOTAL.getIdentifier(), new Color(64, 64, 191));

		// FIXME: Handle ColorProvider and combined events
		Map<String, Integer> columnsOrderMap = new HashMap<>();
		columnsOrderMap.put(createColumnId(JfrAttributes.START_TIME), 1);
		columnsOrderMap.put(createColumnId(JfrAttributes.DURATION), 2);
		columnsOrderMap.put(createColumnId(JfrAttributes.END_TIME), 3);
		columnsOrderMap.put(createColumnId(JfrAttributes.EVENT_THREAD), 4);
		DEFAULT_COLUMNS_ORDER = Collections.unmodifiableMap(columnsOrderMap);
	}

	public static final Color ALLOCATION_COLOR = new Color(64, 144, 230);

	public static final String FORM_TOOLBAR_PAGE_RESULTS = "pageResults"; //$NON-NLS-1$
	public static final String FORM_TOOLBAR_PAGE_SETUP = "pageSetup"; //$NON-NLS-1$
	public static final String FORM_TOOLBAR_PAGE_NAV = "pageNav"; //$NON-NLS-1$

	public static final String RESULT_ACTION_ID = "resultAction"; //$NON-NLS-1$

	public static Color getFieldColor(String fieldId) {
		return FIELD_COLOR_MAP.getOrDefault(fieldId, ColorToolkit.getDistinguishableColor(fieldId));
	}

	public static Color getFieldColor(IAttribute<?> attribute) {
		return getFieldColor(attribute.getIdentifier());
	}

	public static TableSettings createTableSettingsByOrderByAndColumnsWithDefaultOrdering(
		final String orderBy, final Collection<ColumnSettings> columns) {
		final Stream<ColumnSettings> defaultOrderColumns = columns.stream()
				.filter(c -> DEFAULT_COLUMNS_ORDER.containsKey(c.getId())).filter(c -> !c.isHidden())
				.sorted((c1, c2) -> Integer.compare(DEFAULT_COLUMNS_ORDER.get(c1.getId()),
						DEFAULT_COLUMNS_ORDER.get(c2.getId())));
		final Stream<ColumnSettings> naturalOrderColumns = columns.stream()
				.filter(c -> !DEFAULT_COLUMNS_ORDER.containsKey(c.getId()))
				.sorted((c1, c2) -> String.CASE_INSENSITIVE_ORDER.compare(c1.getId(), c2.getId()));
		final List<ColumnSettings> resultColumns = Stream.concat(defaultOrderColumns, naturalOrderColumns)
				.collect(Collectors.toList());
		return new TableSettings(orderBy, resultColumns);
	}

	public static TableSettings createTableSettingsByAllAndVisibleColumns(
		final Collection<String> allColumns, final Collection<String> visibleColumns) {
		final List<ColumnSettings> defaultListCols = new ArrayList<>();
		for (String columnId : allColumns) {
			defaultListCols.add(new ColumnSettings(columnId, !visibleColumns.contains(columnId), null, null));
		}
		return createTableSettingsByOrderByAndColumnsWithDefaultOrdering(null, defaultListCols);
	}

	private static String createColumnId(IAttribute<?> attr) {
		return new StringBuilder().append(attr.getIdentifier()).append(":") //$NON-NLS-1$
				.append(attr.getContentType().getIdentifier()).toString();
	}

	public static IAction createAttributeCheckAction(IAttribute<?> attribute, Consumer<Boolean> onChange) {
		return createCheckAction(attribute.getName(), attribute.getDescription(), attribute.getIdentifier(),
				getFieldColor(attribute), onChange);
	}

	public static IAction createTypeCheckAction(
		String actionId, String typeId, String name, String description, Consumer<Boolean> onChange) {
		return createCheckAction(name, description, actionId, TypeLabelProvider.getColorOrDefault(typeId), onChange);
	}

	public static IAction createAggregatorCheckAction(
		IAggregator<?, ?> aggregator, String id, Color color, Consumer<Boolean> onChange) {
		return createCheckAction(aggregator.getName(), aggregator.getDescription(), id, color, onChange);
	}

	public static IAction createCheckAction(
		String name, String description, String id, Color color, Consumer<Boolean> onChange) {
		return createCheckAction(name, description, id,
				SWTColorToolkit.getColorThumbnailDescriptor(SWTColorToolkit.asRGB(color)), onChange);
	}

	public static IAction createCheckAction(
		String name, String description, String id, ImageDescriptor icon, Consumer<Boolean> onChange) {
		return ActionToolkit.checkAction(onChange, name, description, icon, id);
	}

	public static Optional<IXDataRenderer> buildLinesRow(
		String title, String description, IItemCollection items, boolean fill, IItemQuery query,
		Predicate<IAttribute<IQuantity>> attributeFilter, IQuantity includeLow, IQuantity includeHigh) {
		XYDataRenderer renderer = includeHigh != null
				? new XYDataRenderer(includeLow, includeHigh, true, title, description)
				: new XYDataRenderer(includeLow, title, description);
		IItemCollection filteredItemsSupplier = items.apply(query.getFilter());
		Stream<IAttribute<IQuantity>> attributes = getQuantityAttributes(query);
		if (attributeFilter != null) {
			attributes = attributes.filter(attributeFilter);
		}
		if (DataPageToolkit.addEndTimeLines(renderer, filteredItemsSupplier, fill, attributes)) {
			return Optional.of(new ItemRow(title, description, renderer, filteredItemsSupplier));
		}
		return Optional.empty();
	}

	/**
	 * @param q
	 *            A query containing only {@code IAttribute<IQuantity>} attributes. Queries
	 *            containing non-quantity attributes are not supported and may cause
	 *            ClassCastExceptions later when the attributes are used.
	 * @return a stream of the query attributes
	 */
	/*
	 * FIXME: JMC-5125 - This cast chain is scary and should be reworked.
	 * 
	 * If the query contains any non-quantity attributes then there will be a ClassCastException
	 * later when the attributes are used to extract values.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static Stream<IAttribute<IQuantity>> getQuantityAttributes(IItemQuery q) {
		return (Stream) q.getAttributes().stream();
	}

	public static void setChart(ChartCanvas canvas, XYChart chart, Consumer<IItemCollection> selectionListener) {
		setChart(canvas, chart, selectionListener, null);
	}

	public static void setChart(
		ChartCanvas canvas, XYChart chart, Consumer<IItemCollection> selectionListener,
		Consumer<IRange<IQuantity>> selectRangeConsumer) {
		IMenuManager contextMenu = canvas.getContextMenu();
		contextMenu.removeAll();
		canvas.getContextMenu().add(new Action(Messages.CHART_ZOOM_TO_SELECTED_RANGE) {
			@Override
			public void run() {
				IQuantity selectionStart = chart.getSelectionStart();
				IQuantity selectionEnd = chart.getSelectionEnd();
				if (selectionStart == null || selectionEnd == null) {
					chart.clearVisibleRange();
				} else {
					chart.setVisibleRange(selectionStart, selectionEnd);
				}
				canvas.redrawChart();
			}
		});

		canvas.setSelectionListener(() -> {
			selectionListener.accept(ItemRow.getRangeSelection(chart, JfrAttributes.LIFETIME));
			IQuantity start = chart.getSelectionStart();
			IQuantity end = chart.getSelectionEnd();
			if (selectRangeConsumer != null) {
				selectRangeConsumer
						.accept(start != null && end != null ? QuantityRange.createWithEnd(start, end) : null);
			}
		});
		canvas.setChart(chart);
	}

	public static void setChart(
		ChartCanvas canvas, XYChart chart, IAttribute<IQuantity> selectionAttribute,
		Consumer<IItemCollection> selectionListener) {
		IMenuManager contextMenu = canvas.getContextMenu();
		contextMenu.removeAll();
		canvas.setSelectionListener(() -> selectionListener.accept(ItemRow.getSelection(chart, selectionAttribute)));
		canvas.setChart(chart);
	}

	/**
	 * Only works for items that are either fully overlapping, or disjunct. Must be ensured by
	 * client code.
	 */
	private static class RangePayload implements IAdaptable {
		IItem item;
		IQuantity start;
		IQuantity end;
		double rangeInPixels;

		RangePayload(IItem item, IQuantity start, IQuantity end, double rangeInPixels) {
			this.item = item;
			this.start = start;
			this.end = end;
			this.rangeInPixels = rangeInPixels;
		}

		void combineWith(IItem item, IQuantity start, IQuantity end, double rangeInPixels) {
			if (this.start.compareTo(start) < 0) {
				// Will choose the item that starts last
				this.start = start;
				this.end = end;
				this.item = item;
				extendRangeInPixels(this.end.compareTo(start) > 0, rangeInPixels);
			} else {
				extendRangeInPixels(end.compareTo(this.start) > 0, rangeInPixels);
			}
		}

		void extendRangeInPixels(boolean overlapping, double rangeInPixels) {
			this.rangeInPixels = overlapping ? Math.max(this.rangeInPixels, rangeInPixels)
					: this.rangeInPixels + rangeInPixels;
		}

		@Override
		public <T> T getAdapter(Class<T> adapter) {
			return IItem.class.isAssignableFrom(adapter) ? adapter.cast(item) : null;
		}
	}

	private static ISpanSeries<RangePayload> rangeSeries(
		IItemCollection events, IAttribute<IQuantity> startAttribute, IAttribute<IQuantity> endAttribute) {
		return new ISpanSeries<RangePayload>() {
			@Override
			public XYQuantities<RangePayload[]> getQuantities(SubdividedQuantityRange xBucketRange) {
				SubdividedQuantityRange xRange = xBucketRange.copyWithPixelSubdividers();
				List<RangePayload> spanningPixels = new ArrayList<>();
				RangePayload[] pixelBuckets = new RangePayload[xRange.getNumSubdividers()];
				events.forEach(is -> {
					IMemberAccessor<IQuantity, IItem> startAccessor = startAttribute.getAccessor(is.getType());
					IMemberAccessor<IQuantity, IItem> endAccessor = endAttribute.getAccessor(is.getType());
					is.forEach(item -> {
						IQuantity start = startAccessor.getMember(item);
						IQuantity end = endAccessor.getMember(item);
						int xPos = xRange.getFloorSubdivider(start);
						int endPos = xRange.getFloorSubdivider(end);
						if (xPos < pixelBuckets.length && endPos >= 0) {
							// FIXME: If we have very short events (nanosecond scale) we can sometimes get a negative range.
							double rangeInPixels = xRange.getPixel(end) - xRange.getPixel(start);
							if (xPos != endPos) {
								spanningPixels.add(new RangePayload(item, start, end, rangeInPixels));
							} else if (pixelBuckets[xPos] == null) {
								pixelBuckets[xPos] = new RangePayload(item, start, end, rangeInPixels);
							} else {
								pixelBuckets[xPos].combineWith(item, start, end, rangeInPixels);
							}
						}
					});
				});
				RangePayload[] sorted = Stream
						.concat(Stream.of(pixelBuckets).filter(Objects::nonNull), spanningPixels.stream())
						.sorted(Comparator.comparing(r -> r.start)).toArray(RangePayload[]::new);
				// FIXME: Should make it possible to use the RangePayload[] directly instead
				List<IQuantity> starts = Stream.of(sorted).map(r -> r.start).collect(Collectors.toList());
				List<IQuantity> ends = Stream.of(sorted).map(r -> r.end).collect(Collectors.toList());
				return XYQuantities.create(sorted, starts, ends, xRange);
			}

			@Override
			public IQuantity getStartX(RangePayload payload) {
				return payload.start;
			}
		};
	}

	public final static Color GC_BASE_COLOR = TypeLabelProvider.getColor(JdkTypeIDs.GC_PAUSE);
	private final static Color VM_OPERATIONS_BASE_COLOR = TypeLabelProvider.getColor(JdkTypeIDs.VM_OPERATIONS);
	private final static IColorProvider<RangePayload> GC_COLOR = payload -> adjustAlpha(GC_BASE_COLOR,
			payload.rangeInPixels);
	private final static IColorProvider<RangePayload> APPLICATION_PAUSE_COLOR = payload -> adjustAlpha(
			payload.item.getType().getIdentifier().equals(JdkTypeIDs.GC_PAUSE) ? GC_BASE_COLOR
					: VM_OPERATIONS_BASE_COLOR,
			payload.rangeInPixels);
	public final static ImageDescriptor GC_LEGEND_ICON = new OverlayImageDescriptor(
			SWTColorToolkit.getColorThumbnailDescriptor(SWTColorToolkit.asRGB(GC_BASE_COLOR)), false,
			FlightRecorderUI.getDefault().getMCImageDescriptor("trash_overlay.png")); //$NON-NLS-1$

	/**
	 * Return a color with alpha calculated from a fraction.
	 *
	 * @param color
	 *            a base color
	 * @param fraction
	 *            A value where 0 gives the lowest alpha value and 1 gives the highest. Fractions
	 *            above 1 are accepted and treated as 1. Negative fractions should not be used.
	 * @return a color with RGB from the base color and an alpha value depending on the fraction
	 */
	private static Color adjustAlpha(Color color, double fraction) {
		return ColorToolkit.withAlpha(color, Math.min(200, (int) ((Math.max(0, fraction) + 0.15) * 255)));
	}

	public static ItemRow buildGcPauseRow(IItemCollection items) {
		IItemCollection pauseEvents = items.apply(JdkFilters.GC_PAUSE);
		ISpanSeries<RangePayload> gcBackdrop = rangeSeries(pauseEvents, JfrAttributes.START_TIME,
				JfrAttributes.END_TIME);
		return new ItemRow(SpanRenderer.build(gcBackdrop, GC_COLOR), pauseEvents);
	}

	public static ItemRow buildApplicationPauseRow(IItemCollection items) {
		IItemFilter vmOperationPauseFilter = ItemFilters.and(JdkFilters.VM_OPERATIONS,
				ItemFilters.equals(JdkAttributes.SAFEPOINT, true));
		IItemCollection applicationPauses = items
				.apply(ItemFilters.or(JdkFilters.GC_PAUSE, JdkFilters.SAFE_POINTS, vmOperationPauseFilter));
		ISpanSeries<RangePayload> pausesSeries = rangeSeries(applicationPauses, JfrAttributes.START_TIME,
				JfrAttributes.END_TIME);
		return new ItemRow(SpanRenderer.build(pausesSeries, APPLICATION_PAUSE_COLOR), applicationPauses);
	}

	public static IXDataRenderer buildTimestampHistogramRenderer(
		IItemCollection items, IAggregator<IQuantity, ?> aggregator, IAttribute<IQuantity> timestampAttribute,
		Color color) {
		IQuantitySeries<IQuantity[]> aggregatorSeries = BucketBuilder.aggregatorSeries(items, aggregator,
				timestampAttribute);
		XYDataRenderer renderer = new XYDataRenderer(getKindOfQuantity(aggregator).getDefaultUnit().quantity(0),
				aggregator.getName(), aggregator.getDescription());
		renderer.addBarChart(aggregator.getName(), aggregatorSeries, color);
		return renderer;
	}

	public static IXDataRenderer buildTimestampHistogramRenderer(
		IItemCollection items, IAggregator<IQuantity, ?> aggregator, Color color) {
		return buildTimestampHistogramRenderer(items, aggregator, JfrAttributes.CENTER_TIME, color);
	}

	public static ItemRow buildTimestampHistogram(
		String title, String description, IItemCollection items, IAggregator<IQuantity, ?> aggregator,
		IAttribute<IQuantity> timestampAttribute, Color color) {
		return new ItemRow(title, description,
				buildTimestampHistogramRenderer(items, aggregator, timestampAttribute, color), items);
	}

	public static ItemRow buildTimestampHistogram(
		String title, String description, IItemCollection items, IAggregator<IQuantity, ?> aggregator, Color color) {
		return new ItemRow(title, description, buildTimestampHistogramRenderer(items, aggregator, color), items);
	}

	public static ItemHistogram createDistinctItemsTable(
		Composite parent, IItemCollection items, IItemQuery query, TableSettings settings) {
		CompositeKeyHistogramBuilder histogramBuilder = new CompositeKeyHistogramBuilder();
		for (IAttribute<?> attribute : query.getAttributes()) {
			histogramBuilder.addKeyColumn(attribute);
		}
		ItemHistogram table = histogramBuilder.buildWithoutBorder(parent, settings);
		return table;
	}

	public static IBaseLabelProvider createTableHighlightProvider(Pattern highlightPattern, boolean isWarning) {
		return new StyledCellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				org.eclipse.swt.graphics.Color color = isWarning
						? new org.eclipse.swt.graphics.Color(Display.getCurrent(), 240, 120, 140)
						: new org.eclipse.swt.graphics.Color(Display.getCurrent(), 255, 144, 4);
				String text = getText(cell.getElement(), cell.getColumnIndex());
				Matcher matcher = highlightPattern.matcher(text);
				if (matcher.find()) {
					cell.getViewerRow().setBackground(0, color);
					cell.getViewerRow().setBackground(1, color);
				}
				cell.setText(text);
				super.update(cell);
			}

			private String getText(Object element, int index) {
				Object key = AggregationGrid.getKey(element);
				Object[] keyElements = ((CompositeKey) key).getKeyElements();
				return keyElements[index].toString();
			}
		};
	}

	public static void addContextMenus(
		IPageContainer pc, ItemHistogram h, String selectionName, IAction ... extraActions) {
		MCContextMenuManager mm = MCContextMenuManager.create(h.getManager().getViewer().getControl());
		ColumnMenusFactory.addDefaultMenus(h.getManager(), mm);
		SelectionStoreActionToolkit.addSelectionStoreActions(pc.getSelectionStore(), h, selectionName, mm);
		for (IAction action : extraActions) {
			mm.add(action);
		}
	}

	public static IXDataRenderer buildSizeRow(
		String title, String description, IItemCollection items, IAggregator<IQuantity, ?> a, Color color,
		IColorProvider<IItem> cp) {
		return RendererToolkit.layers(buildSpanRenderer(items, cp),
				buildTimestampHistogram(title, description, items, a, color));
	}

	public static ItemRow buildDurationHistogram(
		String title, String description, IItemCollection items, IAggregator<IQuantity, ?> a, Color color) {
		IQuantitySeries<IQuantity[]> allocationSeries = BucketBuilder.aggregatorSeries(items, a,
				JfrAttributes.DURATION);
		XYDataRenderer renderer = new XYDataRenderer(getKindOfQuantity(a).getDefaultUnit().quantity(0), title,
				description);
		renderer.addBarChart(a.getName(), allocationSeries, color);
		return new ItemRow(title, description, renderer, items);
	}

	public static ItemRow buildSizeHistogram(
		String title, String description, IItemCollection items, IAggregator<IQuantity, ?> a, Color color, IAttribute<IQuantity> attribute) {
		IQuantitySeries<IQuantity[]> allocationSeries = BucketBuilder.aggregatorSeries(items, a,
				JdkAttributes.IO_SIZE);
		XYDataRenderer renderer = new XYDataRenderer(getKindOfQuantity(a).getDefaultUnit().quantity(0), title,
				description);
		renderer.addBarChart(a.getName(), allocationSeries, color);
		return new ItemRow(title, description, renderer, items);
	}

	public static IRange<IQuantity> buildSizeRange(IItemCollection items, boolean isSocket){
		IQuantity end = null;
		if(isSocket) {
			end = QuantitiesToolkit.maxPresent(items.getAggregate(JdkAggregators.SOCKET_READ_LARGEST),
					items.getAggregate(JdkAggregators.SOCKET_WRITE_LARGEST));
		} else {
			end = QuantitiesToolkit.maxPresent(items.getAggregate(JdkAggregators.FILE_READ_LARGEST),
					items.getAggregate(JdkAggregators.FILE_WRITE_LARGEST));
		}
		end = end == null ? UnitLookup.BYTE.quantity(1024) : end;
		return QuantityRange.createWithEnd(UnitLookup.BYTE.quantity(0), end);
	}

	// FIXME: Make something that can use something other than time as x-axis?
	public static IXDataRenderer buildSpanRenderer(IItemCollection pathItems, IColorProvider<IItem> cp) {
		ISpanSeries<IItem> dataSeries = QuantitySeries.max(pathItems, JfrAttributes.START_TIME, JfrAttributes.END_TIME);
		return SpanRenderer.withBoundaries(dataSeries, cp);
	}

	public static boolean addEndTimeLines(
		XYDataRenderer renderer, IItemCollection items, boolean fill, Stream<IAttribute<IQuantity>> yAttributes) {
		// FIXME: JMC-4520 - Handle multiple item iterables
		Iterator<IItemIterable> ii = items.iterator();
		if (ii.hasNext()) {
			IItemIterable itemStream = ii.next();
			IType<IItem> type = itemStream.getType();
			// FIXME: A better way to ensure sorting by endTime
			return yAttributes.peek(a -> addEndTimeLine(renderer, itemStream.iterator(), type, a, fill))
					.mapToLong(a -> 1L).sum() > 0;
		}
		return false;
	}

	public static void addEndTimeLine(
		XYDataRenderer renderer, Iterator<? extends IItem> items, IType<IItem> type, IAttribute<IQuantity> yAttribute,
		boolean fill) {
		IQuantitySeries<?> qs = buildQuantitySeries(items, type, JfrAttributes.END_TIME, yAttribute);
		renderer.addLineChart(yAttribute.getName(), qs, getFieldColor(yAttribute), fill);
	}

	public static IQuantitySeries<?> buildQuantitySeries(
		Iterator<? extends IItem> items, IType<IItem> type, IAttribute<IQuantity> xAttribute,
		IAttribute<IQuantity> yAttribute) {
		IMemberAccessor<IQuantity, IItem> yAccessor = yAttribute.getAccessor(type);
		if (yAccessor == null) {
			throw new RuntimeException(yAttribute.getIdentifier() + " is not an attribute for " + type.getIdentifier()); //$NON-NLS-1$
		}
		return buildQuantitySeries(items, type, xAttribute, yAccessor);
	}

	public static IQuantitySeries<?> buildQuantitySeries(
		Iterator<? extends IItem> items, IType<IItem> type, IAttribute<IQuantity> xAttribute,
		IMemberAccessor<? extends IQuantity, IItem> yAccessor) {
		IMemberAccessor<IQuantity, IItem> xAccessor = xAttribute.getAccessor(type);
		return QuantitySeries.all(items, xAccessor, yAccessor);
	}

	public static void createChartTooltip(ChartCanvas chart) {
		createChartTooltip(chart, ChartToolTipProvider::new);
	}

	public static void createChartTimestampTooltip(ChartCanvas chart) {
		createChartTooltip(chart, JfrAttributes.START_TIME, JfrAttributes.END_TIME, JfrAttributes.DURATION,
				JfrAttributes.EVENT_TYPE, JfrAttributes.EVENT_STACKTRACE);
	}

	public static void createChartTooltip(ChartCanvas chart, IAttribute<?> ... excludedAttributes) {
		createChartTooltip(chart, new HashSet<>(Arrays.asList(excludedAttributes)));
	}

	public static void createChartTooltip(ChartCanvas chart, Set<IAttribute<?>> excludedAttributes) {
		createChartTooltip(chart, () -> new ChartToolTipProvider() {
			@Override
			protected Stream<IAttribute<?>> getAttributeStream(IType<IItem> type) {
				return type.getAttributes().stream().filter(a -> !excludedAttributes.contains(a));
			}
		});
	}

	public static void createChartTooltip(ChartCanvas chart, Supplier<ChartToolTipProvider> toolTipProviderSupplier) {
		new ToolTip(chart) {
			String html;
			Map<String, Image> images;

			@Override
			protected boolean shouldCreateToolTip(Event event) {
				ChartToolTipProvider provider = toolTipProviderSupplier.get();
				chart.infoAt(provider, event.x, event.y);
				html = provider.getHTML();
				images = provider.getImages();
				return html != null;
			}

			@Override
			protected Composite createToolTipContentArea(Event event, Composite parent) {
				FormText formText = CompositeToolkit.createInfoFormText(parent);
				for (Map.Entry<String, Image> imgEntry : images.entrySet()) {
					formText.setImage(imgEntry.getKey(), imgEntry.getValue());
				}
				formText.setText(html, true, false);
				return formText;
			}

		};

	}

	private static KindOfQuantity<?> getKindOfQuantity(IAggregator<IQuantity, ?> a) {
		IType<? super IQuantity> ct = a.getValueType();
		// FIXME: Refactor to avoid this cast
		return ((KindOfQuantity<?>) ct);
	}

	public static Form createForm(Composite parent, FormToolkit toolkit, String title, Image img) {
		Form form = toolkit.createForm(parent);
		form.setText(title.replaceAll("&", "&&")); //$NON-NLS-1$ //$NON-NLS-2$
		form.setImage(img);
		toolkit.decorateFormHeading(form);
		FillLayout fillLayout = new FillLayout();
		fillLayout.marginHeight = 15;
		fillLayout.marginWidth = 8;
		form.getBody().setLayout(fillLayout);
		form.getToolBarManager().add(new Separator(FORM_TOOLBAR_PAGE_RESULTS));
		form.getToolBarManager().add(new Separator(FORM_TOOLBAR_PAGE_SETUP));
		form.getToolBarManager().add(new Separator(FORM_TOOLBAR_PAGE_NAV));
		return form;
	}

	public static class ShowResultAction extends Action {

		private String[] topics;
		private final IPageContainer pageContainer;
		private volatile Severity maxSeverity;
		private final List<Consumer<Result>> listeners = new ArrayList<>();

		ShowResultAction(String title, int style, ImageDescriptor icon, Supplier<String> tooltip,
				IPageContainer pageContainer, String ... topics) {
			super(title, style);
			setImageDescriptor(icon);
			setToolTipText(tooltip.get());
			this.topics = topics;
			this.pageContainer = pageContainer;
			maxSeverity = pageContainer.getRuleManager().getMaxSeverity(topics);
			for (String topic : topics) {
				Consumer<Result> listener = result -> {
					Severity severity = Severity.get(result.getScore());
					if (severity.compareTo(maxSeverity) > 0) {
						maxSeverity = severity;
						setImageDescriptor(getResultIcon(maxSeverity));
					} else if (severity.compareTo(maxSeverity) < 0) { // severity could be less than previous max
						maxSeverity = pageContainer.getRuleManager().getMaxSeverity(topics);
					}
					setToolTipText(tooltip.get());
				};
				listeners.add(listener);
				pageContainer.getRuleManager().addResultListener(topic, listener);
			}
		}

		private void removeListeners() {
			listeners.forEach(l -> pageContainer.getRuleManager().removeResultListener(l));
		}

		@Override
		public void run() {
			pageContainer.showResults(topics);
		}
	}

	public static void addRuleResultAction(
		Form form, IPageContainer pageContainer, Supplier<String> tooltip, String[] topics) {
		if (topics == null || topics.length == 0 || !FlightRecorderUI.getDefault().isAnalysisEnabled()) {
			return;
		}
		ImageDescriptor icon = getResultIcon(pageContainer.getRuleManager().getMaxSeverity(topics));
		ShowResultAction resultAction = new ShowResultAction(Messages.RULES_SHOW_RESULTS_ACTION, IAction.AS_PUSH_BUTTON,
				icon, tooltip, pageContainer, topics);
		resultAction.setId(RESULT_ACTION_ID);
		form.getToolBarManager().appendToGroup(DataPageToolkit.FORM_TOOLBAR_PAGE_RESULTS, resultAction);
		form.getToolBarManager().update(true);
		form.addDisposeListener(e -> resultAction.removeListeners());
	}

	private static ImageDescriptor getResultIcon(Severity severity) {
		switch (severity) {
		case OK:
			return ResultOverview.ICON_OK;
		case INFO:
			return ResultOverview.ICON_INFO;
		case WARNING:
			return ResultOverview.ICON_WARNING;
		case NA:
			return ResultOverview.ICON_NA;
		}
		return null;
	}

	/**
	 * Return a disabled Action.
	 *
	 * @param text
	 *            text to be displayed by the MenuItem, and represent it as it's id.
	 * @return an Action containing the desired text, which will be disabled in a UI component.
	 */
	public static IAction disabledAction(String text) {
		IAction disabledAction = new Action(text) {
			@Override
			public boolean isEnabled() {
				return false;
			}
		};
		disabledAction.setId(text);
		return disabledAction;
	}

	public static FilterEditor buildFilterSelector(
		Composite parent, IItemFilter filter, IItemCollection items, Supplier<Stream<SelectionStoreEntry>> selections,
		Consumer<IItemFilter> onSelect, boolean hasBorder) {
		Supplier<Collection<IAttribute<?>>> attributeSupplier = () -> getPersistableAttributes(
				getAttributes(filter != null ? items.apply(filter) : items)).collect(Collectors.toList());

		AttributeValueProvider valueSupplier = new AttributeValueProvider() {
			@Override
			public <V> V defaultValue(ICanonicalAccessorFactory<V> attribute) {
				return findValueForFilter(items, attribute);
			}
		};

		FilterEditor editor = new FilterEditor(parent, onSelect, filter, attributeSupplier, valueSupplier,
				TypeLabelProvider::getColorOrDefault, hasBorder ? SWT.BORDER : SWT.NONE);

		MenuManager addFromSelectionPredicate = new MenuManager(Messages.FILTER_ADD_FROM_SELECTION);
		editor.getContextMenu().prependToGroup(MCContextMenuManager.GROUP_NEW, addFromSelectionPredicate);
		addFromSelectionPredicate.setRemoveAllWhenShown(true);
		addFromSelectionPredicate.addMenuListener(new IMenuListener() {

			@Override
			public void menuAboutToShow(IMenuManager manager) {
				selections.get().forEach(entry -> {
					MenuManager selectionFlavors = new MenuManager(entry.getName());
					entry.getSelection().getFlavors(editor.getFilter(), items, null)
							.filter(f -> f instanceof IFilterFlavor).forEach(flavor -> {
								selectionFlavors.add(new Action(flavor.getName()) {
									@Override
									public void run() {
										editor.addRoot(((IFilterFlavor) flavor).getFilter());
									}
								});
							});
					if (!selectionFlavors.isEmpty()) {
						if (manager.find(Messages.FILTER_NO_SELECTION_AVAILABLE) != null) {
							manager.remove(Messages.FILTER_NO_SELECTION_AVAILABLE);
						}
						manager.add(selectionFlavors);
					} else {
						manager.add(disabledAction(Messages.FILTER_NO_SELECTION_AVAILABLE));
					}
				});
			}
		});

		// FIXME: This could potentially move into the FilterEditor class
		MenuManager addAttributeValuePredicate = new MenuManager(Messages.FILTER_ADD_FROM_ATTRIBUTE);
		editor.getContextMenu().prependToGroup(MCContextMenuManager.GROUP_NEW, addAttributeValuePredicate);
		addAttributeValuePredicate.setRemoveAllWhenShown(true);
		addAttributeValuePredicate.addMenuListener(new IMenuListener() {
			Collection<IAttribute<?>> attributes;

			@Override
			public void menuAboutToShow(IMenuManager manager) {
				if (attributes == null) {
					attributes = attributeSupplier.get();
				}
				if (!attributes.isEmpty()) {
					if (manager.find(Messages.FILTER_NO_ATTRIBUTE_AVAILABLE) != null) {
						manager.remove(Messages.FILTER_NO_ATTRIBUTE_AVAILABLE);
					}
					attributes.stream().distinct().sorted((a1, a2) -> a1.getName().compareTo(a2.getName()))
						.forEach(attr -> {
							addAttributeValuePredicate.add(new Action(attr.getName()) {
								@Override
								public void run() {
									IItemFilter filter = createDefaultFilter(items, attr);
									editor.addRoot(filter);
								}
							});
						});
				} else {
					manager.add(disabledAction(Messages.FILTER_NO_ATTRIBUTE_AVAILABLE));
				}

			}
		});
		return editor;
	}

	// FIXME: Move to some AttributeToolkit?
	private static Stream<IAttribute<?>> getAttributes(IItemCollection items) {
		return ItemCollectionToolkit.stream(items).filter(IItemIterable::hasItems)
				.flatMap(is -> is.getType().getAttributes().stream());
	}

	public static Stream<IAttribute<?>> getPersistableAttributes(Stream<IAttribute<?>> attributes) {
		// FIXME: Would like to always be able to persist a string representation of the attribute, because this is usable by filters.

		// FIXME: Should we always include event type? Does it make any sense, except on the custom pages?

		// FIXME: Transform both START_TIME and END_TIME to LIFETIME?
		// FIXME: Add derived attributes, like a conversion of any THREAD or CLASS attribute? Thread group?
		/*
		 * Make sure to do the conversions in the right order, so for example a stack trace can be
		 * converted to a top method, which then is converted to a method string.
		 */
		return attributes.map(a -> a.equals(JfrAttributes.EVENT_THREAD) ? JdkAttributes.EVENT_THREAD_NAME : a)
				.flatMap(a -> a.equals(JfrAttributes.EVENT_STACKTRACE) ? Stream.of(JdkAttributes.STACK_TRACE_STRING,
						JdkAttributes.STACK_TRACE_TOP_METHOD_STRING, JdkAttributes.STACK_TRACE_TOP_CLASS_STRING,
						JdkAttributes.STACK_TRACE_TOP_PACKAGE, JdkAttributes.STACK_TRACE_BOTTOM_METHOD_STRING)
						: Stream.of(a))
				.map(a -> a.equals(JdkAttributes.COMPILER_METHOD) ? JdkAttributes.COMPILER_METHOD_STRING : a)
				// FIXME: String or id?
				.map(a -> a.equals(JdkAttributes.REC_SETTING_FOR) ? JdkAttributes.REC_SETTING_FOR_NAME : a)
				.map(a -> a.equals(JdkAttributes.CLASS_DEFINING_CLASSLOADER)
						? JdkAttributes.CLASS_DEFINING_CLASSLOADER_STRING : a)
				.map(a -> a.equals(JdkAttributes.CLASS_INITIATING_CLASSLOADER)
						? JdkAttributes.CLASS_INITIATING_CLASSLOADER_STRING : a)
				.map(a -> a.equals(JdkAttributes.PARENT_CLASSLOADER)
						? JdkAttributes.PARENT_CLASSLOADER_STRING : a)
				.map(a -> a.equals(JdkAttributes.CLASSLOADER)
						? JdkAttributes.CLASSLOADER_STRING : a)
				.filter(a -> a.equals(JfrAttributes.EVENT_TYPE) || (a.getContentType() instanceof RangeContentType)
						|| (a.getContentType().getPersister() != null))
				.distinct();
	}

	/**
	 * Returns a value for attribute, firstly by trying to find one in the items, secondly by
	 * creating a default value for some known content types. Returns null if the first two cases
	 * fail.
	 *
	 * @param items
	 * @param attribute
	 * @return a value of type V, or null
	 */
	@SuppressWarnings("unchecked")
	private static <V> V findValueForFilter(IItemCollection items, ICanonicalAccessorFactory<V> attribute) {
		IItem firstItem = ItemCollectionToolkit.stream(items).filter(is -> is.getType().hasAttribute(attribute))
				.flatMap(ItemIterableToolkit::stream)
				.filter(i -> ((IMemberAccessor<V, IItem>) attribute.getAccessor(i.getType())).getMember(i) != null)
				.findFirst().orElse(null);
		if (firstItem != null) {
			IMemberAccessor<V, IItem> accessor = (IMemberAccessor<V, IItem>) attribute.getAccessor(firstItem.getType());
			return accessor.getMember(firstItem);
		}
		if (UnitLookup.PLAIN_TEXT.equals(attribute.getContentType())) {
			return (V) ""; //$NON-NLS-1$
		}
		if (attribute.getContentType() instanceof KindOfQuantity<?>) {
			return (V) ((KindOfQuantity<?>) attribute.getContentType()).getDefaultUnit().quantity(0);
		}
		return null;
	}

	/**
	 * Returns an default filter for attribute, which might be an equals filter, hasAttribute
	 * filter, or type filter, depending on the attribute and the contents of the items.
	 *
	 * @param items
	 * @param attribute
	 * @return a filter
	 */
	// FIXME: Should move to FilterEditor, or some subclass/specialization?
	private static <V> IItemFilter createDefaultFilter(IItemCollection items, ICanonicalAccessorFactory<V> attribute) {
		V value = findValueForFilter(items, attribute);
		if (value == null) {
			return ItemFilters.hasAttribute(attribute);
		} else if (attribute.equals(JfrAttributes.EVENT_TYPE)) {
			return ItemFilters.type(((IType<?>) value).getIdentifier());
		}
		return ItemFilters.equals(attribute, value);
	}

	public static void addRenameAction(Form form, IPageContainer editor) {
		form.getMenuManager().add(new Action(Messages.PAGE_RENAME_MENU_ACTION) {
			@Override
			public void run() {
				InputDialog dialog = new InputDialog(form.getShell(), Messages.PAGE_RENAME_DIALOG_TITLE,
						Messages.PAGE_RENAME_DIALOG_MESSAGE, form.getText(), null);
				if (dialog.open() == Window.OK) {
					form.setText(dialog.getValue());
					editor.currentPageRefresh();
				}
			}
		});
	}

	public static void addIconChangeAction(Form form, IPageContainer editor, Consumer<Image> newIconConsumer) {
		form.getMenuManager().add(new Action(Messages.PAGE_CHANGE_ICON_MENU_ACTION) {
			@Override
			public void run() {
				WizardDialog dialog = new WizardDialog(form.getShell(),
						new IconChangeWizard(form.getImage(), newIconConsumer));
				dialog.open();
				editor.currentPageRefresh();
			}
		});
	}

	private static class IconChangeWizard extends Wizard {

		private final Image currentImage;
		private final Consumer<Image> imageConsumer;
		private Label imageLabel;

		public IconChangeWizard(Image currentImage, Consumer<Image> imageConsumer) {
			setWindowTitle(Messages.PAGE_CHANGE_ICON_WIZARD_TITLE);
			this.currentImage = currentImage;
			this.imageConsumer = imageConsumer;
		}

		@Override
		public void addPages() {
			addPage(new WizardPage(Messages.PAGE_CHANGE_ICON_WIZARD_PAGE_TITLE) {

				@Override
				public String getTitle() {
					return Messages.PAGE_CHANGE_ICON_WIZARD_PAGE_TITLE;
				}

				@Override
				public String getDescription() {
					return Messages.PAGE_CHANGE_ICON_WIZARD_PAGE_DESC;
				}

				@Override
				public void createControl(Composite parent) {
					Composite container = new Composite(parent, SWT.NONE);
					GridLayout layout = new GridLayout(1, false);
					container.setLayout(layout);

					Button button = new Button(container, SWT.NONE);
					button.setText(Messages.PAGE_CHANGE_ICON_CHOOSE_IMAGE_FILE);

					button.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							chooseImageFileDialog();
						}
					});

					if (currentImage != null) {
						new Label(container, SWT.NONE).setText(Messages.PAGE_CHANGE_ICON_CURRENT_ICON);
						new Label(container, SWT.BORDER).setImage(currentImage);
					}
					new Label(container, SWT.NONE).setText(Messages.PAGE_CHANGE_ICON_NEW_ICON_PREVIEW);
					imageLabel = new Label(container, SWT.BORDER);
					GridData gd = new GridData(16, 16);
					imageLabel.setLayoutData(gd);

					setControl(container);
				}

				private void chooseImageFileDialog() {
					FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);
					String[] filterNames = new String[] {"Image Files", "All Files (*)"}; //$NON-NLS-1$ //$NON-NLS-2$
					String[] filterExtensions = new String[] {"*.gif;*.png;*.xpm;*.jpg;*.jpeg;*.tiff", "*"}; //$NON-NLS-1$ //$NON-NLS-2$
					fileDialog.setFilterNames(filterNames);
					fileDialog.setFilterExtensions(filterExtensions);
					String filename = fileDialog.open();
					if (filename == null) {
						// Dialog was cancelled. Bail out early to avoid handling that case later. Premature?
						return;
					}
					try (InputStream fis = new FileInputStream(filename)) {
						ImageData imageData = new ImageData(fis);
						// Validate image data
						if (imageData.width != 16 || imageData.height != 16) {
							imageData = resizeImage(imageData, 16, 16);
						}
						DisplayToolkit.dispose(imageLabel.getImage());
						imageLabel.setImage(new Image(getShell().getDisplay(), imageData));
						imageLabel.getParent().layout();
						setPageComplete(isPageComplete());
					} catch (Exception e) {
						// FIXME: Add proper logging
						e.printStackTrace();
					}
				}

				private ImageData resizeImage(ImageData imageData, int width, int height) {
					Image original = ImageDescriptor.createFromImageData(imageData).createImage();
					Image scaled = new Image(Display.getDefault(), width, height);
					GC gc = new GC(scaled);
					gc.setAntialias(SWT.ON);
					gc.setInterpolation(SWT.HIGH);
					gc.drawImage(original, 0, 0, imageData.width, imageData.height, 0, 0, width, height);
					gc.dispose();
					original.dispose();
					ImageData scaledData = scaled.getImageData();
					scaled.dispose();
					return scaledData;
				}

				@Override
				public boolean isPageComplete() {
					return imageLabel.getImage() != null;
				}

			});
		}

		@Override
		public boolean performFinish() {
			imageConsumer.accept(imageLabel.getImage());
			DisplayToolkit.dispose(currentImage);
			return true;
		}

		@Override
		public boolean performCancel() {
			DisplayToolkit.dispose(imageLabel.getImage());
			return true;
		}

	}

	public static ItemList createSimpleItemList(
		Composite parent, ItemListBuilder listBuilder, IPageContainer pageContainer, TableSettings tableSettings,
		String selectionName) {

		ItemList list = listBuilder.build(parent, tableSettings);
		ColumnViewer viewer = list.getManager().getViewer();
		MCContextMenuManager mm = MCContextMenuManager.create(viewer.getControl());
		ColumnMenusFactory.addDefaultMenus(list.getManager(), mm);
		viewer.addSelectionChangedListener(
				e -> pageContainer.showSelection(ItemCollectionToolkit.build(list.getSelection().get())));

		if (selectionName != null) {
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(), list, selectionName,
					mm);
		}

		return list;
	}

	public static void addTabItem(CTabFolder tabFolder, Control section, String name) {
		CTabItem tabItem = new CTabItem(tabFolder, SWT.NONE);
		tabItem.setControl(section);
		tabItem.setText(name);
	}

	public static TypeFilterBuilder buildEventTypeTree(
		Composite parent, FormToolkit toolkit, Runnable onChange, boolean checkbox) {
		// TODO: Make more accessible.
		// TODO: Add support for storing the expansion state in a memento.
		// TODO: Add input from selection store, output to selection store
		// TODO: Add toolbar for choosing tree or checkbox tree.
		Composite treeComposite = new Composite(parent, SWT.NONE);
		treeComposite.setLayout(new GridLayout());
		toolkit.adapt(treeComposite);
		Label caption = toolkit.createLabel(treeComposite, Messages.EVENT_TYPE_TREE_TITLE);
		caption.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
		caption.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		TypeFilterBuilder typeFilterTree = new TypeFilterBuilder(treeComposite, onChange, checkbox);

		typeFilterTree.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		return typeFilterTree;
	}

	public static boolean isTypeWithThreadAndDuration(IType<?> type) {
		return JfrAttributes.EVENT_THREAD.getAccessor(type) != null
				&& JfrAttributes.START_TIME.getAccessor(type) != JfrAttributes.END_TIME.getAccessor(type);
	}

}
