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
package org.openjdk.jmc.greychart.impl;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import org.openjdk.jmc.greychart.AbstractAxis;
import org.openjdk.jmc.greychart.AxisContentType;
import org.openjdk.jmc.greychart.SeriesGreyChart;
import org.openjdk.jmc.greychart.XAxis;
import org.openjdk.jmc.greychart.util.ChartRenderingToolkit;

/**
 * Default X axis.
 */
public class DefaultXAxis extends AbstractAxis implements XAxis {
	public static final long SECONDS = 1000;
	public static final long MINUTES = 60 * SECONDS;
	public static final long HOURS = 60 * MINUTES;
	public static final long DAYS = 24 * HOURS;

	private Long m_maxX = System.currentTimeMillis();
	private Long m_minX = System.currentTimeMillis() - 3 * HOURS;
	private long m_diff = 3 * HOURS;

	/**
	 * Constructor.
	 *
	 * @param owner
	 *            the chart of the owner.
	 */
	public DefaultXAxis(SeriesGreyChart owner) {
		super(owner);
		setTickMarksEnabled(true);
		setNumberOfTicks(10);
	}

	@Override
	public void render(Graphics2D ctx, Rectangle targetArea, Rectangle plotArea) {
		setRenderedBounds(targetArea);
		if (targetArea.width <= 0 || targetArea.height <= 0) {
			return;
		}

		AffineTransform trans = ctx.getTransform();
		ctx.translate(targetArea.x, targetArea.y);
		ctx.setColor(getBackground());
		ctx.fillRect(0, 0, getRenderedWidth(), getRenderedHeight());
		ctx.setColor(getForeground());
		ctx.drawLine(0, 0, getRenderedWidth(), 0);

		// paint tick marks
		if (isTickMarksEnabled()) {
			long tickInterval = calculateNiceTickDistance(m_diff / getNumberOfTicks());

			FontMetrics fm = ctx.getFontMetrics();
			int tickSize = (int) (getRelativeTickSize() * (fm.getMaxAscent() + fm.getMaxDescent()));

			long min = m_minX.longValue();
			long max = m_maxX.longValue();

			if (tickInterval != 0) {
				long startVal = (long) Math.ceil(min / (float) tickInterval) * tickInterval;
				Rectangle initBounds = fm.getStringBounds(Long.toString(startVal), ctx).getBounds();
				long numberY = tickSize * 2 + initBounds.height;
				// add min due to cancellation effects with large values.
				int tickIntervalX = convertAxisValueToDrawingCoordinate(tickInterval + min)
						- convertAxisValueToDrawingCoordinate(min);
				int skipCount = (int) Math.round(initBounds.getWidth() / tickIntervalX);
				for (long tickVal = startVal; tickVal <= max; tickVal += tickInterval) {
					int x = convertAxisValueToDrawingCoordinate(tickVal);
					ctx.drawLine(x, -tickSize, x, tickSize);

					// Paint number.
					// Just paint as longs right now.
					if (skipCount == 0) {
						String text = Long.toString(tickVal);
						Rectangle2D bounds = fm.getStringBounds(text, ctx);
						ctx.drawString(text, (int) Math.round(x - bounds.getWidth() / 2.0), numberY);

						if (bounds.getWidth() > tickIntervalX) {
							skipCount = (int) Math.round(bounds.getWidth() / tickIntervalX);
						}
					} else {
						skipCount--;
					}
				}
			}
			if (hasTitle()) {
				Rectangle2D bounds = fm.getStringBounds(getTitle(), ctx);

				ctx.setColor(getTitleColor());
				ctx.drawString(getTitle(), getRenderedWidth() / 2 - (int) bounds.getWidth() / 2,
						(getRenderedHeight() - fm.getDescent() - tickSize));
			}

			ctx.setTransform(trans);
		}
	}

	@Override
	public Number getMin() {
		return m_minX;
	}

	@Override
	public Number getMax() {
		return m_maxX;
	}

	/**
	 * Sets the x axis range.
	 *
	 * @param min
	 *            the starting value
	 * @param max
	 *            the ending value.
	 */
	public void setRange(Number min, Number max) {
		setRangeInternal(min, max);
		// FIXME: Notify chart of change!
	}

	private void setRangeInternal(Number min, Number max) {
		m_maxX = (Long) max;
		m_minX = (Long) min;
		m_diff = max.longValue() - min.longValue();
	}

	private long calculateNiceTickDistance(long tick) {
		double multiplier = Math.pow(10, ChartRenderingToolkit.fastFloor(ChartRenderingToolkit.log10(tick)));
		return (long) (Math.round(tick / multiplier) * multiplier);
	}

	@Override
	public int convertAxisValueToDrawingCoordinate(double value) {
		return (int) Math.round((getRenderedWidth() / (double) m_diff) * (value - m_minX.floatValue()));
	}

	@Override
	public double convertDrawingCoordinateToAxisValue(int coordinate) {
		return coordinate * m_diff / (double) getRenderedHeight() + m_minX.longValue();
	}

	@Override
	public Dimension getPreferredDimensions(Graphics2D ctx, Rectangle totalDrawingArea) {
		FontMetrics fm = ctx.getFontMetrics();
		int fontSize = fm.getMaxAscent() + fm.getMaxDescent();
		int tickSize = (int) (getRelativeTickSize() * fontSize);
		return new Dimension(totalDrawingArea.width, fontSize * (hasTitle() ? 2 : 1) + tickSize * 6);
	}

	@Override
	public String getContentType() {
		return AxisContentType.TIME;
	}
}
