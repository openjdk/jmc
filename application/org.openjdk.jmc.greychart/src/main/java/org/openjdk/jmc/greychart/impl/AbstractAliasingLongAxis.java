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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import org.openjdk.jmc.greychart.AbstractAxis;
import org.openjdk.jmc.greychart.GreyChartPanel;
import org.openjdk.jmc.greychart.LongAxis;
import org.openjdk.jmc.greychart.SeriesGreyChart;
import org.openjdk.jmc.greychart.XAxis;
import org.openjdk.jmc.greychart.util.ChartRenderingToolkit;

public abstract class AbstractAliasingLongAxis extends AbstractAxis implements LongAxis, XAxis {
	public final static long DEFAULT_RANGE = DateFormatter.MINUTE;
	// The aliasing array is used to find the right tick starting value and distance.
	private long m_diff;
	private long m_fixedRange;
	private final long[] m_aliasingArray;

	/**
	 * Constructor.
	 *
	 * @param owner
	 *            the chart that owns the axis.
	 */
	public AbstractAliasingLongAxis(SeriesGreyChart owner, long[] aliasingArray, long defaultRange, long defaultDiff) {
		super(owner);
		setTickMarksEnabled(true);
		setNumberOfTicks(5);
		m_diff = defaultDiff;
		m_fixedRange = defaultRange;
		m_aliasingArray = aliasingArray;
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
			long tickInterval = getNormalizedTickDistance();

			Font oldfont = ctx.getFont();
			FontMetrics fm = ctx.getFontMetrics();
			Font font = oldfont.deriveFont(getActualFontSize(oldfont.getSize()));
			if (oldfont.getSize() != font.getSize()) {
				ctx.setFont(font);
				fm = ctx.getFontMetrics();
			}

			int tickSize = (int) (getRelativeTickSize() * (fm.getMaxAscent() + fm.getMaxDescent()));

			long max = m_max.longValue();
			long min = m_min.longValue();

			long startVal = getFirstTickValue(tickInterval);
			Rectangle worstBounds = fm
					.getStringBounds(formatString(getLastTickValue(tickInterval), min, max, tickInterval), ctx)
					.getBounds();
			long numberY = tickSize * 2 + worstBounds.height;

			int tickIntervalX = (int) (Math.round((getRenderedWidth() / (double) m_diff) * tickInterval));
			if (tickIntervalX < 1) {
				tickIntervalX = 1;
			}
			int skipCount = (int) (worstBounds.getWidth() * 1.2) / tickIntervalX;
			int count = 0;
			for (long tickVal = startVal; tickVal <= max; tickVal += tickInterval) {
				int x = convertLongAxisValueToDrawingCoordinate(tickVal);
				ctx.drawLine(x, -tickSize, x, tickSize);

				// Paint grid lines...
				if (isPaintGridLinesEnabled()) {
					Stroke oldStroke = ctx.getStroke();
					// FIXME: Add customizable dash line color
					ctx.setColor(getTitleColor());

					ctx.setStroke(DASH_STROKE);
					ctx.drawLine(x, 0, x, -plotArea.height);
					ctx.setColor(getForeground());
					ctx.setStroke(oldStroke);
				}

				// Paint number.
				if (skipCount == 0 || count % (skipCount + 1) == 0) {
					String text = formatString(tickVal, min, max, tickInterval);
					Rectangle2D bounds = fm.getStringBounds(text, ctx);
					ctx.drawString(text, (int) Math.round(x - bounds.getWidth() / 2.0), numberY);
				}
				count++;
			}
			String title = getTitle();
			String unitString = getFormatter().getUnitString(m_min.longValue(), m_max.longValue());
			if (unitString.length() > 0) {
				title = title + unitString;
			}

			Rectangle2D bounds = fm.getStringBounds(title, ctx);

			ctx.setColor(getTitleColor());
			ctx.drawString(title, getRenderedWidth() / 2 - (int) bounds.getWidth() / 2,
					(getRenderedHeight() - fm.getDescent() - tickSize));

			if (GreyChartPanel.DEBUG) {
				ChartRenderingToolkit.markBoundary(ctx, 0, 0, targetArea.width, targetArea.height, Color.RED);
			}
			ctx.setFont(oldfont);
			ctx.setTransform(trans);
		}
	}

	private String formatString(long tickValue, long min, long max, long labelDistance) {
		return getFormatter().format(tickValue, min, max, labelDistance);
	}

	public void setRange(Number min, Number max) {
		m_min = min;
		m_max = max;
		setRangeInternal(min, max);
		// FIXME: Notify chart of change!
	}

	public void setRange(Number max) {
		setRange(max.longValue() - getFixedRange(), max);
	}

	private void setRangeInternal(Number min, Number max) {
		m_diff = max.longValue() - min.longValue();
		if (m_diff <= 0) {
			// FIXME: Consider throwing a checked exception instead.
			m_diff = 1;
		}
		fireAxisChange();
	}

	@Override
	public int convertAxisValueToDrawingCoordinate(double value) {
		return Math.round((float) ((getRenderedWidth() / (double) m_diff) * (value - m_min.longValue())));
	}

	@Override
	public double convertDrawingCoordinateToAxisValue(int coordinate) {
		return m_diff * (((double) coordinate) / ((double) getRenderedWidth())) + m_min.doubleValue();
	}

	@Override
	public Dimension getPreferredDimensions(Graphics2D ctx, Rectangle totalDrawingArea) {
		if (!isVisible()) {
			return new Dimension(totalDrawingArea.width, 0);
		}
		Font oldfont = ctx.getFont();
		Font newFont = oldfont;
		float actualSize = getActualFontSize(oldfont.getSize());

		if (actualSize != oldfont.getSize()) {
			newFont = oldfont.deriveFont(actualSize);
		}

		FontMetrics fm = ctx.getFontMetrics(newFont);
		int fontHeight = fm.getMaxAscent() + fm.getMaxDescent();
		int tickSize = (int) (getRelativeTickSize() * fontHeight);
		int titleSize = hasTitle() ? fontHeight * 2 : fontHeight;
		return new Dimension(totalDrawingArea.width, titleSize + tickSize * 6);
	}

	/**
	 * @return the value in ticks that represents where the first tick mark will be drawn.
	 */
	private long getFirstTickValue(long tickDistance) {
		long ticksBeforeMin = getMin().longValue() / tickDistance;
		return (ticksBeforeMin + 1) * tickDistance;
	}

	/**
	 * @return the value in ticks that represents where the last tick mark will be drawn.
	 */
	private long getLastTickValue(long tickDistance) {
		long ticksBeforeMax = getMax().longValue() / tickDistance;
		return ticksBeforeMax * tickDistance;
	}

	/**
	 * @return returns the tick distance after it has been rounded appropriately to "look good"
	 */
	private long getNormalizedTickDistance() {
		long range = getLongRange();
		long minTickDist = range / getNumberOfTicks();
		long quantum = getLargestTickQuantumBelow(minTickDist);
		long quantaPerTick = minTickDist / quantum + 1;
		return quantaPerTick * quantum;
	}

	private long getLargestTickQuantumBelow(long range) {
		for (int i = 1; i < m_aliasingArray.length; i++) {
			if (range <= m_aliasingArray[i]) {
				return m_aliasingArray[i - 1];
			}
		}
		return m_aliasingArray[m_aliasingArray.length - 1];
	}

	/**
	 * @return if > 0, the chart will be rendered with the range [maxX - getFixedRange(), maxX].
	 */
	public long getFixedRange() {
		return m_fixedRange;
	}

	/**
	 * If fixedRange is set to nonzero, the chart will be rendered using the range [maxX -
	 * getFixedRange(), maxX].
	 * <p>
	 * If fixed range is set, the range will automatically be updated upon rendering.
	 *
	 * @param fixedRange
	 *            the range used when drawing the chart.
	 */
	public void setFixedRange(long fixedRange) {
		m_fixedRange = fixedRange;
	}

	@Override
	public String toString() {
		return "DateXAxis range: " + (getMax().longValue() - getMin().longValue()) / (1000 * 1000 * 1000) + " s min:" //$NON-NLS-1$ //$NON-NLS-2$
				+ getMin() + " max:" + getMax(); //$NON-NLS-1$
	}

	@Override
	public long convertDrawingCoordinateToLongAxisValue(int coordinate) {
		return (long) (m_diff * (((double) coordinate) / ((double) getRenderedWidth())) + m_min.longValue());
	}

	@Override
	public int convertLongAxisValueToDrawingCoordinate(long value) {
		return (int) (getRenderedWidth() * ((value - m_min.longValue()) / (double) m_diff));
	}

	@Override
	public long getLongRange() {
		return getMax().longValue() - getMin().longValue();
	}
}
