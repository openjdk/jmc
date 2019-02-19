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
import java.awt.Paint;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Arrays;

import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.ui.charts.XYQuantities.AbstractSpan;

public class QuantitySpanRenderer implements IXDataRenderer {
	/**
	 * Sentinel value used to indicate a missing (unknown) start timestamp.
	 */
	public static final IQuantity MISSING_START = UnitLookup.EPOCH_S.quantity(0);

	/**
	 * Sentinel value used to indicate a missing (unknown) end timestamp.
	 */
	public static final IQuantity MISSING_END = UnitLookup.EPOCH_S.quantity(Long.MAX_VALUE);

	private final IQuantitySeries<?> ranges;
	private final Paint paint;
	private final int minOutlineHeight;
	private final IXDataRenderer content;
	private final String text;
	private final String description;

	public QuantitySpanRenderer(IQuantitySeries<?> ranges, IXDataRenderer content, Paint paint, int minOutlineHeight,
			String text, String description) {
		this.ranges = ranges;
		this.content = content;
		this.paint = paint;
		this.minOutlineHeight = minOutlineHeight;
		this.text = text;
		this.description = description;
	}

	private static int calcMargin(int height) {
		return Math.min(5, (height + 10) / 20);
	}

	@Override
	public IRenderedRow render(Graphics2D context, SubdividedQuantityRange xRange, int height) {
		int margin = calcMargin(height);
		int innerHeight = height - 2 * margin;
		context.translate(0, margin);
		context.setPaint(paint);
		XYQuantities<?> quantities = ranges.getQuantities(xRange);
		// Need to set y range to same as x range to be able to draw ranges (and eliminate creation of quantities).
		quantities.setYRange(xRange);
		AWTChartToolkit.drawRanges(context, quantities, innerHeight, true);
		IRenderedRow renderedContent = content.render(context, xRange, innerHeight);
		if (innerHeight >= minOutlineHeight) {
			context.setPaint(Color.BLACK);
			AWTChartToolkit.drawRanges(context, quantities, innerHeight, false);
		}
		context.translate(0, -margin);
		return new QuantitySpanRendering(margin, quantities, renderedContent, paint, text, description);
	}

	private static class QuantitySpanRendering extends RenderedRowBase {

		private final XYQuantities<?> points;
		private final IRenderedRow content;
		private final Paint paint;
		private final int margin;
		private String description;

		public QuantitySpanRendering(int margin, XYQuantities<?> points, IRenderedRow content, Paint paint, String text,
				String description) {
			super(Arrays.asList(new RenderedRowBase(margin), content, new RenderedRowBase(margin)),
					content.getHeight() + 2 * margin, text, description, null);
			this.margin = margin;
			this.points = points;
			this.content = content;
			this.paint = paint;
			this.description = description;
		}

		@Override
		public void infoAt(IChartInfoVisitor visitor, int x, int y, Point offset) {
			offset = new Point(offset.x, offset.y + margin);
			content.infoAt(visitor, x, y, offset);

			// FIXME: Only output this if near the boundaries? At least handle infinite lengths.
			if (points != null) {
				int bucket = points.floorIndexAtX(x);
				if (bucket < points.getSize()) {
					Span span = new Span(bucket, offset);
					while (bucket >= 0) {
						double x2 = points.getPixelY(bucket);
						if (x < x2) {
							span.setIndex(bucket);
							visitor.visit(span);
							// Break now, or can there be more to collect?
							break;
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
			protected XYQuantities<?> getXYSet() {
				return points;
			}

			@Override
			protected int getHeight() {
				return content.getHeight();
			}

			@Override
			public Color getColor() {
				return (paint instanceof Color) ? (Color) paint : null;
			}

			private void setIndex(int index) {
				this.index = index;
			}

			@Override
			public IQuantity getStartX() {
				IQuantity org = super.getStartX();
				return (org == MISSING_START) ? null : org;
			}

			@Override
			public IQuantity getEndX() {
				IQuantity org = super.getEndX();
				return (org == MISSING_END) ? null : org;
			}

			@Override
			public String getDescription() {
				return description;
			}
		}

	}

}
