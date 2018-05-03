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
package org.openjdk.jmc.flightrecorder.ext.g1.visualizer;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;

import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.QuantityRange;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.ext.g1.ColorMap;
import org.openjdk.jmc.flightrecorder.ext.g1.G1Constants;
import org.openjdk.jmc.flightrecorder.ext.g1.visualizer.region.HeapRegion;
import org.openjdk.jmc.flightrecorder.ui.IPageContainer;
import org.openjdk.jmc.flightrecorder.ui.common.DataPageToolkit;
import org.openjdk.jmc.flightrecorder.ui.common.ItemRow;
import org.openjdk.jmc.ui.charts.AWTChartToolkit.IColorProvider;
import org.openjdk.jmc.ui.charts.IRenderedRow;
import org.openjdk.jmc.ui.charts.ISpanSeries;
import org.openjdk.jmc.ui.charts.IXDataRenderer;
import org.openjdk.jmc.ui.charts.RenderedRowBase;
import org.openjdk.jmc.ui.charts.RendererToolkit;
import org.openjdk.jmc.ui.charts.SpanRenderer;
import org.openjdk.jmc.ui.charts.SubdividedQuantityRange;
import org.openjdk.jmc.ui.charts.XYChart;
import org.openjdk.jmc.ui.charts.XYQuantities;
import org.openjdk.jmc.ui.misc.ChartCanvas;

public class HeapRegionView {

	private static class HeapRegionPayload implements IAdaptable {
		IItem item;
		IQuantity start;
		IQuantity end;
		double rangeInPixels;
		HeapRegion region;

		HeapRegionPayload(HeapRegion region, IQuantity end, double rangeInPixels) {
			this.item = region.getItem();
			this.start = region.getTimestamp();
			this.end = end;
			this.rangeInPixels = rangeInPixels;
			this.region = region;
		}

		void combineWith(HeapRegion region, IQuantity end, double rangeInPixels) {
			if (this.start.compareTo(region.getTimestamp()) < 0) {
				// Will choose the item that starts last
				this.item = region.getItem();
				this.start = region.getTimestamp();
				this.end = end;
				this.region = region;
				// might want to add color blending if the colors differ
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

	private Map<Integer, List<HeapRegion>> regionStates;
	private IQuantity start;
	private IQuantity end;
	private final ChartCanvas chartCanvas;
	private IPageContainer pageContainer;

	private ColorMap colors;
	private XYChart chart;
	private IXDataRenderer regionRows;
	private IXDataRenderer markerRenderer;
	private ItemRow gcPauseRenderer;

	public HeapRegionView(ColorMap colorMap, Composite parent, IPageContainer pageContainer, int style) {
		colors = colorMap;
		this.pageContainer = pageContainer;
		regionStates = Collections.emptyMap();
		chartCanvas = new ChartCanvas(parent);
	}

	public void redraw() {
		chartCanvas.redraw();
		chartCanvas.redrawChart();
	}

	private ISpanSeries<HeapRegionPayload> rangeSeries(List<HeapRegion> states) {
		return new ISpanSeries<HeapRegionPayload>() {
			@Override
			public XYQuantities<HeapRegionPayload[]> getQuantities(SubdividedQuantityRange xBucketRange) {
				SubdividedQuantityRange xRange = xBucketRange.copyWithPixelSubdividers();
				List<HeapRegionPayload> spanningPixels = new ArrayList<>();
				HeapRegionPayload[] pixelBuckets = new HeapRegionPayload[xRange.getNumSubdividers()];

				ListIterator<HeapRegion> stateIterator = states.listIterator();
				while (stateIterator.hasNext()) {
					HeapRegion state = stateIterator.next();
					IQuantity endTime = end;
					if (stateIterator.hasNext()) {
						endTime = stateIterator.next().getTimestamp().subtract(UnitLookup.NANOSECOND.quantity(1));
						stateIterator.previous();
					}
					int xPos = xRange.getFloorSubdivider(state.getTimestamp());
					int endPos = xRange.getFloorSubdivider(endTime);
					if (xPos < pixelBuckets.length && endPos >= 0) {
						double rangeInPixels = xRange.getPixel(endTime) - xRange.getPixel(state.getTimestamp());
						if (xPos != endPos) {
							spanningPixels.add(new HeapRegionPayload(state, endTime, rangeInPixels));
						} else if (pixelBuckets[xPos] == null) {
							pixelBuckets[xPos] = new HeapRegionPayload(state, endTime, rangeInPixels);
						} else {
							pixelBuckets[xPos].combineWith(state, endTime, rangeInPixels);
						}
					}
				}
				HeapRegionPayload[] sorted = Stream
						.concat(Stream.of(pixelBuckets).filter(Objects::nonNull), spanningPixels.stream())
						.sorted(Comparator.comparing(r -> r.start)).toArray(HeapRegionPayload[]::new);

				List<IQuantity> starts = Stream.of(sorted).map(r -> r.start).collect(Collectors.toList());
				List<IQuantity> ends = Stream.of(sorted).map(r -> r.end).collect(Collectors.toList());
				return XYQuantities.create(sorted, starts, ends, xRange);
			}

			@Override
			public IQuantity getStartX(HeapRegionPayload payload) {
				return payload.start;
			}
		};
	}

	public void setStart(IQuantity start) {
		this.start = start;
	}

	public void setEnd(IQuantity end) {
		this.end = end;
	}

	public void show(IItemCollection items) {
		List<HeapRegion> regions = new ArrayList<>();
		for (IItemIterable itemIterable : items) {
			IType<IItem> type = itemIterable.getType();
			IMemberAccessor<IQuantity, IItem> startTimeAccessor = JfrAttributes.START_TIME.getAccessor(type);
			IMemberAccessor<IQuantity, IItem> indexAccessor = G1Constants.REGION_INDEX.getAccessor(type);
			IMemberAccessor<IQuantity, IItem> usedAccessor = G1Constants.REGION_USED.getAccessor(type);
			IMemberAccessor<String, IItem> typeAccessor = G1Constants.TYPE.getAccessor(type);

			for (IItem item : itemIterable) {
				HeapRegion region = new HeapRegion(
						indexAccessor.getMember(item).clampedIntFloorIn(UnitLookup.NUMBER_UNITY),
						typeAccessor.getMember(item), startTimeAccessor.getMember(item), usedAccessor.getMember(item),
						item);
				regions.add(region);
			}
		}
		regionStates = regions.stream().sorted((r1, r2) -> r1.getTimestamp().compareTo(r2.getTimestamp()))
				.collect(Collectors.groupingBy(HeapRegion::getIndex, TreeMap::new, Collectors.toList()));
		List<IXDataRenderer> rows = new ArrayList<>();
		regionStates.forEach((index, states) -> {
			IXDataRenderer build = SpanRenderer.build(rangeSeries(states), new IColorProvider<HeapRegionPayload>() {
				@Override
				public java.awt.Color getColor(HeapRegionPayload payload) {
					Color color = colors.getColor(payload.region);
					return new java.awt.Color(color.getRed(), color.getGreen(), color.getBlue());
				}
			});
			ItemRow itemRow = new ItemRow(index.toString(), G1Constants.REGION_USED.getDescription(), build,
					items.apply(ItemFilters.equals(G1Constants.REGION_INDEX, UnitLookup.NUMBER_UNITY.quantity(index))));
			rows.add(itemRow);
		});
		regionRows = RendererToolkit.uniformRows(rows);
		IRange<IQuantity> currentRange = QuantityRange.createWithEnd(start, end);
		chart = new XYChart(currentRange, RendererToolkit.empty(), 180);
		DataPageToolkit.setChart(chartCanvas, chart, pageContainer::showSelection);
		DataPageToolkit.createChartTimestampTooltip(chartCanvas);
		setAllRenderers();
	}

	public void showGC(IItemCollection gcItems) {
		gcPauseRenderer = DataPageToolkit.buildGcPauseRow(gcItems);
		setAllRenderers();
	}

	private void setAllRenderers() {
		if (regionRows != null) {
			List<IXDataRenderer> renderers = new ArrayList<>(3);
			renderers.add(regionRows);
			if (gcPauseRenderer != null) {
				renderers.add(gcPauseRenderer);
			}
			if (markerRenderer != null) {
				renderers.add(markerRenderer);
			}
			chartCanvas.replaceRenderer(RendererToolkit.layers(renderers));
		}
	}

	public void setCurrentTime(IQuantity currentTime) {
		if (currentTime != null) {
			markerRenderer = new IXDataRenderer() {
				@Override
				public IRenderedRow render(Graphics2D context, SubdividedQuantityRange xRange, int height) {
					int pixel = (int) xRange.getPixel(currentTime);
					context.drawLine(pixel, 0, pixel, height);
					return new RenderedRowBase(height);
				}
			};
		}
		setAllRenderers();
	}
}
