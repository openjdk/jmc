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
package org.openjdk.jmc.ui.charts;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.ColorToolkit;
import org.openjdk.jmc.ui.charts.AWTChartToolkit.IColorProvider;
import org.openjdk.jmc.ui.charts.IChartInfoVisitor.IBucket;
import org.openjdk.jmc.ui.charts.IChartInfoVisitor.ILane;
import org.openjdk.jmc.ui.charts.IChartInfoVisitor.IPoint;
import org.openjdk.jmc.ui.charts.IChartInfoVisitor.ITick;

public class XYDataRenderer implements IXDataRenderer {

	private static abstract class SeriesEntry<U> {
		final IQuantitySeries<U> series;
		transient XYQuantities<U> points;

		SeriesEntry(IQuantitySeries<U> series) {
			this.series = series;
		}

		void updatePointsCache(SubdividedQuantityRange xRange) {
			// FIXME: Improve to simply adjust the XYQuantities.xRange if xRange is compatible.
			if ((points == null) || !points.getXRange().equals(xRange)) {
				points = series.getQuantities(xRange);
			}
		}

		abstract void infoAt(IChartInfoVisitor visitor, int x, Point2D offset);
	}

	private static class BarSeriesEntry<T> extends SeriesEntry<T[]> {
		private final String title;
		private final IColorProvider<? super T> colorProvider;
		private final Color color;

		BarSeriesEntry(String title, IQuantitySeries<T[]> series, IColorProvider<? super T> cp, Color color) {
			super(series);
			this.title = title;
			this.color = color;
			colorProvider = cp;
		}

		@Override
		void infoAt(IChartInfoVisitor visitor, int x, Point2D offset) {
			if (points != null) {
				int bucket = points.floorIndexAtX(x);
				if (bucket >= 0 && bucket < points.getSize()) {
					T[] payload = points.getPayload();
					Color col = color;
					if (colorProvider != null) {
						col = colorProvider.getColor((payload != null) ? payload[bucket] : null);
					}
					IBucket bkt = new XYQuantities.Bucket(points, bucket, offset, title, col);
					visitor.visit(bkt);
				}
			}
		}
	}

	private static class LineSeriesEntry<T> extends SeriesEntry<T> {
		private final String title;
		private final boolean fill;
		private final boolean connect;
		private final Color color;

		LineSeriesEntry(String title, IQuantitySeries<T> series, Color color, boolean fill, boolean connect) {
			super(series);
			this.title = title;
			this.color = color;
			this.fill = fill;
			this.connect = connect;
		}

		@Override
		void infoAt(IChartInfoVisitor visitor, int x, Point2D offset) {
			if (points != null) {
				int index = Math.max(points.floorIndexAtX(x), 0);
				int size = points.getSize();
				if (index < size) {
					if (index < size - 1) {
						// Check if the next index is closer.
						double currentX = points.getPixelX(index);
						double nextX = points.getPixelX(index + 1);
						if ((currentX < 0) || (((nextX - x) < (x - currentX)) && (nextX < points.getWidth()))) {
							index++;
						}
					}
					IPoint point = new XYQuantities.Point(points, index, offset, title, color);
					visitor.visit(point);
				}
			}
		}
	}

	private final ArrayList<SeriesEntry<?>> entries = new ArrayList<>();
	private final boolean axisOnLeft;
	private final IQuantity includeLow;
	private final IQuantity includeHigh;
	private final String name;
	private final String description;

	public XYDataRenderer(IQuantity include) {
		this(include, include);
	}

	public XYDataRenderer(IQuantity include, String name, String description) {
		this(include, include, true, name, description);
	}

	public XYDataRenderer(IQuantity includeLow, IQuantity includeHigh) {
		this(includeLow, includeHigh, true, null, null);
	}

	public XYDataRenderer(IQuantity includeLow, IQuantity includeHigh, boolean axisOnLeft, String name,
			String description) {
		this.axisOnLeft = axisOnLeft;
		this.includeLow = includeLow;
		this.includeHigh = includeHigh;
		this.name = name;
		this.description = description;
	}

	public <T> void addBarChart(String title, IQuantitySeries<T[]> series, Color color) {
		entries.add(new BarSeriesEntry<>(title, series, null, color));
	}

	public <T> void addBarChart(String title, IQuantitySeries<T[]> series, IColorProvider<? super T> cp) {
		entries.add(new BarSeriesEntry<>(title, series, cp, null));
	}

	public <T> void addLineChart(String title, IQuantitySeries<T> series, Color color, boolean fill) {
		entries.add(new LineSeriesEntry<>(title, series, color, fill, true));
	}

	public <T> void addPlotChart(String title, IQuantitySeries<T> series, Color color, boolean fill) {
		entries.add(new LineSeriesEntry<>(title, series, color, fill, false));
	}

	@Override
	public IRenderedRow render(Graphics2D context, SubdividedQuantityRange xRange, int height) {
		int width = xRange.getPixelExtent();
		IQuantity yAxisMin = includeLow;
		IQuantity yAxisMax = includeHigh;
		for (SeriesEntry<?> se : entries) {
			se.updatePointsCache(xRange);
			if (se.points.getSize() > 0) {
				IQuantity seriesMinY = se.points.getMinY();
				if (yAxisMin == null || yAxisMin.compareTo(seriesMinY) > 0) {
					yAxisMin = seriesMinY;
				}
				IQuantity seriesMaxY = se.points.getMaxY();
				if (yAxisMax == null || yAxisMax.compareTo(seriesMaxY) < 0) {
					yAxisMax = seriesMaxY;
				}
			}
		}

		if (yAxisMin != null && yAxisMax != null) {
			FontMetrics fm = context.getFontMetrics();
			// If min=max, expand range to be [min, min+1], or [min, min+1024] in the case of 
			//a graph measured in bytes
			if (yAxisMin.compareTo(yAxisMax) == 0) {
				int offset = yAxisMin.getUnit() == UnitLookup.BYTE ? 1024 : 1;
				yAxisMax = yAxisMin.getUnit().quantity(yAxisMin.doubleValue() + offset);
			} else {
				// Add sufficient padding to ensure that labels for ticks <= yAxisMax fit,
				// and constant value graphs are discernible.
				double padFactor = ((double) (height + 1 + fm.getAscent() / 2)) / height;
				yAxisMax = yAxisMin.add(yAxisMax.subtract(yAxisMin).multiply(padFactor));
			}
			SubdividedQuantityRange yRange = new SubdividedQuantityRange(yAxisMin, yAxisMax, height, fm.getHeight());
			context.setPaint(Color.LIGHT_GRAY);
			AWTChartToolkit.drawGrid(context, yRange, width, true);
			Shape oldClip = context.getClip();
			context.setClip(new Rectangle(width, height));
			for (SeriesEntry<?> se : entries) {
				// Always set yRange since it is used in infoAt().
				se.points.setYRange(yRange);
				if (se.points.getSize() > 0) {
					if (se instanceof LineSeriesEntry) {
						LineSeriesEntry<?> lse = (LineSeriesEntry<?>) se;
						if (lse.connect) {
							context.setPaint(lse.fill ? ColorToolkit.getGradientPaint(lse.color, height) : lse.color);
							AWTChartToolkit.drawLineChart(context, se.points, width, height, lse.fill);
						} else {
							context.setPaint(lse.color);
							AWTChartToolkit.drawPlot(context, se.points, height, lse.fill);
						}
					} else if (se instanceof BarSeriesEntry) {
						drawBarChart(context, (BarSeriesEntry<?>) se, width, height);
					}
				}
			}
			context.setClip(oldClip);
			context.setPaint(Color.BLACK);
			if (axisOnLeft) {
				AWTChartToolkit.drawAxis(context, yRange, 0, true, 1, true);
			} else {
				AWTChartToolkit.drawAxis(context, yRange, width, false, 1, true);
			}
		}
		return new RenderedResult(height);
	}

	// FIXME: Must NOT be dependent on mutable state from XYDataRenderer
	private class RenderedResult extends RenderedRowBase {
		private static final int TICK_ZONE_WIDTH = 32;

		public RenderedResult(int height) {
			super(Collections.<IRenderedRow> emptyList(), height, name, null, null);
		}

		@Override
		public void infoAt(IChartInfoVisitor visitor, int x, int y, final Point offset) {
			if (x >= 0) {
				for (SeriesEntry<?> se : entries) {
					se.infoAt(visitor, x, offset);
				}
			} else if (axisOnLeft && !entries.isEmpty() && x >= -TICK_ZONE_WIDTH) {
				// FIXME: Factor out to support axis on right
				final SubdividedQuantityRange yRange = entries.get(0).points.getYRange();
				final int index = yRange.getClosestSubdividerAtPixel(yRange.getPixelExtent() - 1 - y);
				visitor.visit(new ITick() {
					@Override
					public IDisplayable getValue() {
						return yRange.getSubdivider(index);
					}

					@Override
					public Point2D getTarget() {
						int y = offset.y + yRange.getPixelExtent() - 1 - ((int) yRange.getSubdividerPixel(index));
						return new Point(offset.x, y);
					}
				});
			} else {
				visitor.visit(new ILane() {
					@Override
					public String getLaneName() {
						return name;
					}

					@Override
					public String getLaneDescription() {
						return description;
					}
				});
			}
		}
	}

	private static <T> void drawBarChart(Graphics2D context, BarSeriesEntry<T> se, int width, int height) {
		if (se.color != null) {
			context.setPaint(ColorToolkit.getGradientPaint(se.color, height));
		}
		AWTChartToolkit.drawBarChart(context, se.points, se.colorProvider, width, height);
	}
}
