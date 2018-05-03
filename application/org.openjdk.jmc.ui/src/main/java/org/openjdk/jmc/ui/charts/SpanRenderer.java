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
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;

import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.ui.charts.AWTChartToolkit.IColorProvider;
import org.openjdk.jmc.ui.charts.XYQuantities.AbstractSpan;

public class SpanRenderer<T> implements IXDataRenderer {

	private final ISpanSeries<T> series;
	private final IColorProvider<? super T> colorProvider;
	private final boolean markBoundaries;

	public static <T> IXDataRenderer withBoundaries(ISpanSeries<T> series, IColorProvider<? super T> colorProvider) {
		return new SpanRenderer<>(series, colorProvider, true);
	}

	public static <T> IXDataRenderer build(ISpanSeries<T> series, IColorProvider<? super T> colorProvider) {
		return new SpanRenderer<>(series, colorProvider, false);
	}

	private SpanRenderer(ISpanSeries<T> series, IColorProvider<? super T> colorProvider, boolean markBoundaries) {
		this.series = series;
		this.colorProvider = colorProvider;
		this.markBoundaries = markBoundaries;
	}

	@Override
	public IRenderedRow render(Graphics2D context, SubdividedQuantityRange xRange, int height) {
		XYQuantities<T[]> quantities = series.getQuantities(xRange);
		// Need to set y range to same as x range to be able to draw spans (and eliminate creation of quantities).
		quantities.setYRange(xRange);
		AWTChartToolkit.drawSpan(context, quantities, height, markBoundaries, colorProvider);
		return new SpanRendering<>(height, quantities, series, colorProvider);
	}

	private static class SpanRendering<T> extends RenderedRowBase {

		private final IColorProvider<? super T> colorProvider;
		private final ISpanSeries<T> series;
		private final XYQuantities<T[]> points;

		SpanRendering(int height, XYQuantities<T[]> quantities, ISpanSeries<T> series,
				IColorProvider<? super T> colorProvider) {
			super(height);
			this.points = quantities;
			this.series = series;
			this.colorProvider = colorProvider;

		}

		@Override
		public void infoAt(IChartInfoVisitor visitor, int x, int y, Point offset) {
			if (points != null) {
				int bucket = points.floorIndexAtX(x);
				if (bucket >= 0 && bucket < points.getSize()) {
					T[] payload = points.getPayload();
					Span span = new Span(bucket, offset);
					double limitPixel = (payload[bucket] != null)
							? Math.max(x, points.getXRange().getPixel(span.getStartX())) : x;
					while (bucket >= 0) {
						if (payload[bucket] != null) {
							// FIXME: Are x1 and x2 guaranteed to be ordered?
							if (limitPixel <= points.getPixelY(bucket)) {
								span.index = bucket;
								visitor.visit(span);
							}
						}
						bucket--;
					}
				}
			}
		}

		private class Span extends AbstractSpan {
			public Span(int index, Point2D offset) {
				super(index, offset);
			}

			@Override
			protected XYQuantities<T[]> getXYSet() {
				return points;
			}

			@Override
			protected int getHeight() {
				return SpanRendering.this.getHeight();
			}

			@Override
			public Color getColor() {
				T[] payload = points.getPayload();
				return (colorProvider != null) ? colorProvider.getColor(payload[index]) : null;
			}

			@Override
			public IQuantity getStartX() {
				T[] payload = points.getPayload();
				IQuantity start = series.getStartX(payload[index]);
				return start == null ? super.getStartX() : start;
			}

		}

	}
}
