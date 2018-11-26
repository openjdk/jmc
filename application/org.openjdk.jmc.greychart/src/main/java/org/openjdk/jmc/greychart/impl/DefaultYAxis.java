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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openjdk.jmc.greychart.AbstractAxis;
import org.openjdk.jmc.greychart.AxisContentType;
import org.openjdk.jmc.greychart.GreyChartPanel;
import org.openjdk.jmc.greychart.TickDensity;
import org.openjdk.jmc.greychart.TickFormatter;
import org.openjdk.jmc.greychart.YAxis;
import org.openjdk.jmc.greychart.util.ChartRenderingToolkit;

/**
 */
public class DefaultYAxis extends AbstractAxis implements YAxis {
	private static final double RELATIVE_TICK_SIZE = 0.25;

	private boolean m_markZero = true;

	private Number m_tickSize;

	private double m_autoPadding;

	private boolean m_alwaysShowZero = true;

	private boolean m_autoRangeEnabled;

	private Position m_position;

	private boolean m_titleLegendEnabled;

	private String m_contentType;

	private double m_oldMax;
	private double m_oldMin;
	private String m_oldContentType;
	private TickDensity m_oldTickDensity;
	private TickFormatter m_oldFormatter;

	private int m_oldDrawingAreaHeight;
	private int m_oldYAxisHeight;

	private double m_tickInterval;

	// Cache list for tick labels. Don't want to realloc the array all the time.
	private List m_tickLabels = new ArrayList();

	private int m_minimumWidth;

	/**
	 * Constructor.
	 *
	 * @param owner
	 *            the owner of the axis.
	 * @param autoRange
	 *            true if range is adaptively configured to the dataset.
	 */
	public DefaultYAxis(DefaultXYGreyChart owner) {
		super(owner);
		setTickMarksEnabled(true);
		setNumberOfTicks(10);
		setPaintGridLinesEnabled(true);
		setFormatter(new DoubleFormatter());
	}

	@Override
	protected DefaultXYGreyChart getOwner() {
		return (DefaultXYGreyChart) super.getOwner();
	}

	@Override
	public void render(Graphics2D ctx, Rectangle targetArea, Rectangle plotRect) {
		setRenderedBounds(targetArea);
		if (targetArea.width <= 0 || targetArea.height <= 0) {
			return;
		}
		AffineTransform originalTrans = ctx.getTransform();

		if (GreyChartPanel.DEBUG) {
			ChartRenderingToolkit.markBoundary(ctx, targetArea.x, targetArea.y, targetArea.width, targetArea.height,
					Color.BLUE);
		}

		ctx.translate(targetArea.x, targetArea.y);
		AffineTransform labelTrans = ctx.getTransform();
		// paint background
		drawBackground(ctx, targetArea);
		// paint axis line
		drawAxisLine(ctx);

		// paint tick marks
		if (isTickMarksEnabled()) {
			// determine font to use
			Font oldfont = ctx.getFont();
			ctx.setFont(getFontToUse(oldfont));
			FontMetrics fm = ctx.getFontMetrics();
			int titleHeight = -1;
			if (hasTitle()) {
				titleHeight = getMaxHeight(fm);
			}
			// paint tick marks
			paintTickMarks(ctx, fm, plotRect, targetArea, titleHeight);

			// paint title
			if (hasTitle()) {
				paintTitle(ctx, labelTrans, getTitleTransform(ctx, targetArea, originalTrans, labelTrans));
			}
			ctx.setFont(oldfont);
		}
		ctx.setTransform(originalTrans);
	}

	private void drawBackground(Graphics2D ctx, Rectangle targetArea) {
		ctx.setColor(getBackground());
		ctx.fillRect(0, 0, targetArea.width, targetArea.height);
	}

	private void drawAxisLine(Graphics2D ctx) {
		ctx.setColor(getForeground());
		if (getPosition() == Position.LEFT) {
			ctx.drawLine(getRenderedWidth(), 0, getRenderedWidth(), getRenderedHeight());
		} else {
			ctx.drawLine(0, 0, 0, getRenderedHeight());
		}
	}

	private void paintTitle(Graphics2D ctx, AffineTransform labelTrans, AffineTransform titleTrans) {
		Rectangle titleBounds = ctx.getFontMetrics().getStringBounds(getTitle(), ctx).getBounds();
		ctx.setTransform(titleTrans);
		ctx.setColor(getTitleColor());
		// calculate size for "legend" boxes in title
		int boxSize = Math.max(2, ctx.getFontMetrics().getHeight() / 2);
		// calculate padding, title bounds and text width
		int padding = Math.max(1, Math.round(boxSize * .5f));
		int textWidth = titleBounds.width;
		// add space for "legend" boxes in title, if necessary
		OptimizingProvider[] providers = new OptimizingProvider[0];
		OptimizingProvider yAxisProvider = getOwner().getYAxisProvider(getPosition());
		if (yAxisProvider != null) {
			providers = yAxisProvider.getChildren();
		}
		if (isTitleLegendEnabled()) {
			titleBounds.width += (boxSize + padding) * providers.length;
		}
		// calculate y position
		int yPosition;
		if (getPosition() == Position.LEFT) {
			yPosition = -titleBounds.y;
		} else {
			yPosition = getRenderedWidth() - ctx.getFontMetrics().getMaxDescent();
		}
		// print title text
		ctx.drawString(getTitle(), -titleBounds.width / 2, yPosition);
		// draw "legend" boxes if applicable
		if (isTitleLegendEnabled()) {
			drawTitleLegend(ctx, providers, padding, textWidth + padding - titleBounds.width / 2, yPosition - boxSize,
					boxSize);
		}
		ctx.setTransform(labelTrans);
	}

	private AffineTransform getTitleTransform(
		Graphics2D ctx, Rectangle targetArea, AffineTransform originalTrans, AffineTransform labelTrans) {
		ctx.setTransform(originalTrans);
		ctx.translate(targetArea.x, targetArea.y + targetArea.height / 2);
		ctx.rotate(-Math.PI / 2);
		AffineTransform titleTrans = ctx.getTransform();
		ctx.setTransform(labelTrans);
		return titleTrans;
	}

	private void drawTitleLegend(
		Graphics2D ctx, OptimizingProvider[] optimizingProviders, int padding, int startX, int startY, int boxSize) {
		for (OptimizingProvider provider : optimizingProviders) {
			Color fillColor = getOwner().getMetadataProvider().getTopColor(provider.getDataSeries());
			if (fillColor == null) {
				fillColor = getOwner().getMetadataProvider().getLineColor(provider.getDataSeries());
			}
			fillColor = fillColor == null ? getBackground() : fillColor;
			Color lineColor = getForeground();
			ctx.setColor(fillColor);
			ctx.fillRect(startX, startY, boxSize, boxSize);
			ctx.setColor(lineColor);
			ctx.drawRect(startX, startY, boxSize, boxSize);
			startX += (boxSize + padding);
		}
	}

	private double getNiceMinTick(double min) {
		return Math.ceil(min / m_tickInterval) * m_tickInterval;
	}

	private double getNiceMaxTick(double min, double max) {
		return ChartRenderingToolkit.fastFloor((max - min) / m_tickInterval) * m_tickInterval + min;
	}

	private void paintTickMarks(
		Graphics2D ctx, FontMetrics fm, Rectangle plotRect, Rectangle targetArea, int titleHeight) {

		// Calculate tick interval and generate new tick labels if
		// min/max values or plot area height has changed
		if (hasContentChanged() || targetArea.height != m_oldYAxisHeight) {
			calculateAndGenerateTickLabels(fm, targetArea);
			m_oldYAxisHeight = targetArea.height;
		}
		double min = getNiceMinTick(getMin().doubleValue());
		double max = getNiceMaxTick(min, getMax().doubleValue());
		if (max == min) {
			m_tickInterval = Math.max(Math.abs(max), 1);
		}

		int tickSize = calculateTickSize(targetArea, titleHeight, getMaxHeight(fm));

		// draw labels, grid lines and tick marks
		double maxPlotValue = max;
		double tickVal;
		for (int i = 0; (tickVal = min + i * m_tickInterval) <= maxPlotValue && i < m_tickLabels.size(); i++) {
			int y = convertAxisValueToDrawingCoordinate(tickVal);
			paintTick(ctx, tickSize, tickVal, y);
			if (isPaintGridLinesEnabled()) {
				paintGridLine(ctx, plotRect, tickSize, tickVal, y);
			}
			drawLabel(ctx, fm, (String) (m_tickLabels.get(i)), tickSize, y);
		}
	}

	private boolean hasContentChanged() {
		return getMax().doubleValue() != m_oldMax || getMin().doubleValue() != m_oldMin || m_oldContentType == null
				|| !m_oldContentType.equals(getContentType()) || getTickDensity() != m_oldTickDensity
				|| getFormatter() != m_oldFormatter;
	}

	private void calculateAndGenerateTickLabels(FontMetrics fm, Rectangle targetArea) {
		double max = getMax().doubleValue();
		double min = getMin().doubleValue();
		String contentType = getContentType();
		int fontHeight = fm.getMaxAscent();
		double minTickDistance = max
				- convertDrawingCoordinateToAxisValue(Math.max(getMinTickDistance(), fontHeight), targetArea.height);
		double coarseTickDistance = calculateCoarseTickDistance((max - min),
				max - convertDrawingCoordinateToAxisValue(fontHeight, targetArea.height));
		m_tickInterval = calculateNiceTickDistance(Math.max(coarseTickDistance, minTickDistance), minTickDistance);
		generateTickLabels(max, min);
		m_oldMax = max;
		m_oldMin = min;
		m_oldContentType = contentType;
		m_oldTickDensity = getTickDensity();
		m_oldFormatter = getFormatter();
	}

	private double calculateCoarseTickDistance(double height, double fontHeight) {

		TickDensity density = getTickDensity();
		switch (density) {
		case VERY_SPARSE:
			return fontHeight * 5;
		case SPARSE:
			return fontHeight * 3.5;
		case NORMAL:
			return fontHeight * 2.5;
		case DENSE:
			return fontHeight * 1.7;
		case VERY_DENSE:
			return fontHeight * 1;
		default:
			// When density == VARIABLE
			return height / getNumberOfTicks();
		}
	}

	private void drawLabel(Graphics2D ctx, FontMetrics fm, String text, int tickSize, int y) {
		Rectangle bounds = fm.getStringBounds(text, ctx).getBounds();

		int xPosition;
		if (getPosition() == Position.LEFT) {
			xPosition = getRenderedWidth() - (tickSize * 2) - bounds.width;
		} else {
			xPosition = tickSize * 2;
		}

		ctx.drawString(text, xPosition, y - bounds.height / 2 - bounds.y);
	}

	private void paintTick(Graphics2D ctx, int tickSize, double tickVal, int y) {
		if (getPosition() == Position.LEFT) {
			ctx.drawLine(getRenderedWidth() - tickSize, y, getRenderedWidth() + tickSize, y);
		} else {
			ctx.drawLine(0 - tickSize, y, 0 + tickSize, y);

		}
	}

	private void paintGridLine(Graphics2D ctx, Rectangle plotRect, int tickSize, double tickVal, int y) {
		Stroke oldStroke = ctx.getStroke();
		if (m_markZero && tickVal == 0) {
			ctx.setColor(getForeground());
		} else {
			// FIXME: Add customizable dash line color
			ctx.setColor(getTitleColor());
		}

		ctx.setStroke(DASH_STROKE);
		if (getPosition() == Position.LEFT) {
			ctx.drawLine(getRenderedWidth() + tickSize, y, plotRect.width + plotRect.x, y);
		} else {
			ctx.drawLine(0 - plotRect.width, y, 0 - tickSize, y);
		}
		ctx.setColor(getForeground());
		ctx.setStroke(oldStroke);
	}

	private int calculateTickSize(Rectangle targetArea, int titleHeight, int labelHeight) {
		int tickSize;
		if (m_tickSize != null) {
			tickSize = m_tickSize.intValue();
		} else {
			tickSize = (int) (labelHeight * RELATIVE_TICK_SIZE);
			if (titleHeight != -1) {
				int width = targetArea.width - titleHeight;
				tickSize = Math.min(tickSize, (int) (width * 0.5));
			}
		}
		tickSize = Math.max(tickSize, 1);
		return tickSize;
	}

	@Override
	public double convertDrawingCoordinateToAxisValue(int coordinate) {
		return convertDrawingCoordinateToAxisValue(coordinate, getRenderedHeight());
	}

	private double convertDrawingCoordinateToAxisValue(int coordinate, int renderedHeight) {
		return getMax().doubleValue() - coordinate * (getMax().doubleValue() - getMin().doubleValue()) / renderedHeight;
	}

	private void generateTickLabels(double max, double min) {
		// for each label...
		double tickInterval = m_tickInterval;
		min = getNiceMinTick(min);
		max = getNiceMaxTick(min, max);
		if (max == min) {
			tickInterval = Math.max(Math.abs(max), 1);
		}
		// Removed list.clear to avoid concurrent modification.
		// Create a new list thread local
		ArrayList tickLabels = new ArrayList();
		double tickVal;
		for (int i = 0; (tickVal = min + i * tickInterval) <= max; i++) {
			tickLabels.add(getFormatter().format(tickVal, min, max, tickInterval));
		}
		m_tickLabels = tickLabels;
	}

	@Override
	public Dimension getPreferredDimensions(Graphics2D ctx, Rectangle totalDrawingArea) {
		if (!isVisible()) {
			return new Dimension(0, totalDrawingArea.height);
		}
		Font oldfont = ctx.getFont();
		Font newfont = getFontToUse(oldfont);
		FontMetrics fm = ctx.getFontMetrics(newfont);

		// Calculate tick interval and generate new tick labels if
		// min/max values or plot area height has changed
		if (hasContentChanged() || totalDrawingArea.height != m_oldDrawingAreaHeight) {
			calculateAndGenerateTickLabels(fm, totalDrawingArea);
			m_oldDrawingAreaHeight = totalDrawingArea.height;
		}

		Iterator tickIter = m_tickLabels.iterator();
		int longestLabelWidth = 0;
		while (tickIter.hasNext()) {
			String currentLabel = (String) tickIter.next();
			longestLabelWidth = Math.max(longestLabelWidth, fm.getStringBounds(currentLabel, ctx).getBounds().width);
		}
		// Let each tick be 1/4 of the font height and at least 1 pixel and add
		// space for the tick to the axis width.
		// Also use the tick size for space between tickers and labels and between
		// title and labels (thus we add two extra tick sizes to the axis width).
		int tickSize = (int) Math.max((getMaxHeight(fm) * RELATIVE_TICK_SIZE), 1);
		int width = Math.max(m_minimumWidth, longestLabelWidth + tickSize * 3);

		// Size for a vertical title
		if (hasTitle()) {
			width += getMaxHeight(ctx.getFontMetrics(newfont));
		}
		return new Dimension(width, totalDrawingArea.height);
	}

	/**
	 * Given the font currently decides on which font to use.
	 *
	 * @param oldfont
	 *            the font currently in use
	 * @return the font to use during rendering
	 */
	private Font getFontToUse(Font oldfont) {
		Font newfont = oldfont;
		float fontSize = getActualFontSize(oldfont.getSize());
		if (fontSize != oldfont.getSize()) {
			newfont = oldfont.deriveFont(fontSize);
		}
		return newfont;
	}

	/**
	 * Returns the max height (max ascent plus max descent) of given font metrics.
	 *
	 * @param fm
	 *            the {@link FontMetrics} to use
	 * @return the maximum height of the text
	 */
	private int getMaxHeight(FontMetrics fm) {
		return fm.getMaxAscent() + fm.getMaxDescent();
	}

	/**
	 * @return returns true if the zero should be marked.
	 */
	public boolean isMarkZero() {
		return m_markZero;
	}

	/**
	 * Set to true if you wish to highlight the tick mark for zero.
	 *
	 * @param markZero
	 *            set to true if you wish to highlight the tick mark for zero.
	 */
	public void setMarkZero(boolean markZero) {
		m_markZero = markZero;
	}

	/**
	 * @param coarseTickDistance
	 * @param byteVal
	 * @return
	 */
	private double calculateNiceTickDistance(double coarseTickDistance, double minTickDistance) {
		if (coarseTickDistance == 0) {
			coarseTickDistance = 1;
		}
		if (AxisContentType.BYTES.equals(getContentType())) {
			return calculateNiceByteTickDistance(coarseTickDistance, minTickDistance);
		}

		double exp = ChartRenderingToolkit.log10(coarseTickDistance);
		double calcExp = ChartRenderingToolkit.fastFloor(exp);
		double magnitude = Math.pow(10, calcExp);
		double fact = coarseTickDistance / magnitude;
		magnitude = adjustMagnitude(magnitude, fact);
		if (magnitude <= minTickDistance) {
			magnitude = magnitude * 2;
		}
		if (AxisContentType.COUNT.equals(getContentType()) || AxisContentType.TIME.equals(getContentType())) {
			return Math.max(1, (int) magnitude);
		} else {
			return magnitude;
		}
	}

	private double adjustMagnitude(double magnitude, double fact) {
		if (fact > 8) {
			magnitude *= 10;
		} else if (fact >= 3) {
			magnitude *= 5;
		} else if (fact >= 1.5) {
			magnitude *= 2;
		}
		return magnitude;
	}

	private double adjustByteMagnitude(double magnitude, double fact) {
		double exp = ChartRenderingToolkit.fastFloor(ChartRenderingToolkit.log2(fact));
		double minExp = Math.pow(2, exp);
		double maxExp = Math.pow(2, exp + 1);
		if ((fact - minExp) > (maxExp - fact)) {
			return magnitude * maxExp;
		} else {
			return magnitude * minExp;
		}
	}

	private double calculateNiceByteTickDistance(double coarseTickDistance, double minTickDistance) {
		double sign = Math.signum(coarseTickDistance);
		double absVal = Math.abs(coarseTickDistance);
		int byteExp = getByteExp(absVal);
		double magnitude = Math.pow(1024, byteExp);
		double fact = coarseTickDistance / magnitude;
		magnitude = adjustByteMagnitude(magnitude, fact);
		if (magnitude <= minTickDistance) {
			magnitude = magnitude * 2;
		}
		return Math.max(1, magnitude * sign);
	}

	private int getByteExp(double absVal) {
		int exp = 0;
		while (absVal > 1024) {
			absVal /= 1024;
			exp++;
		}
		return exp;
	}

	@Override
	public int convertAxisValueToDrawingCoordinate(double value) {
		int height = getRenderedHeight();
		double min = getMin().doubleValue();
		double max = getMax().doubleValue();
		double plotHeight = max - min;
		double plotValue = value - min;
		double coordinate = height - height / plotHeight * plotValue;

		return Math.round((float) coordinate);
	}

	public void setTickSize(Number n) {
		m_tickSize = n;
	}

	public void setAutoPadding(double fraction) {
		m_autoPadding = fraction;
	}

	public void setAlwaysShowZero(boolean alwaysShow) {
		m_alwaysShowZero = alwaysShow;
	}

	@Override
	public Number getMax() {
		Number max = super.getMax();
		double maxValue;
		double minValue;

		OptimizingProvider provider = getOwner().getYAxisProvider(getPosition());
		// get max value from data series
		if (max == null || isAutoRangeEnabled()) {
			if (provider == null) {
				// Auto range and no data provider
				return 1;
			}
			maxValue = provider.getMaxY();
			minValue = provider.getMinY();
		} else {
			maxValue = max.doubleValue();
			minValue = super.getMin().doubleValue();
		}

		if (Double.isNaN(minValue) || Double.isInfinite(minValue)) {
			// Didn't get anything useful from the provider
			// FIXME: Can the provider return NaN? The check above is to be safe. If we can be sure that NaN is never returned, then we can remove it.
			minValue = 0;
			maxValue = 0;
		}
		if (m_alwaysShowZero && minValue > 0) {
			// adjust the minimum value for always show zero
			minValue = 0;
		}
		if (maxValue == minValue) {
			// Pad a bit if min and max are the same. The amount of padding doesn't really matter in this case.
			if (maxValue == 0) {
				maxValue = 1;
			} else {
				maxValue = maxValue + Math.abs(maxValue) * 0.1;
			}
		} else {
			// add auto padding (could be zero)
			maxValue = maxValue + m_autoPadding * (maxValue - minValue);
		}

		if (m_alwaysShowZero && maxValue < 0) {
			// adjust for always show zero
			return 0.0;
		}
		return maxValue;
	}

	@Override
	public Number getMin() {
		Number min = super.getMin();
		double minValue;
		double maxValue;

		OptimizingProvider provider = getOwner().getYAxisProvider(getPosition());

		if (min == null || isAutoRangeEnabled()) {
			if (provider == null) {
				// Auto range and no data provider
				return 0;
			}
			maxValue = provider.getMaxY();
			minValue = provider.getMinY();
		} else {
			minValue = min.doubleValue();
			maxValue = super.getMax().doubleValue();
		}

		if (Double.isNaN(maxValue) || Double.isInfinite(maxValue)) {
			// Didn't get anything useful from the provider
			// FIXME: Can the provider return NaN? The check above is to be safe. If we can be sure that NaN is never returned, then we can remove it.
			maxValue = 0;
			minValue = 0;
		}
		if (m_alwaysShowZero && maxValue < 0) {
			// adjust the max value for always show zero
			maxValue = 0;
		}
		double paddedValue;
		if (maxValue == minValue) {
			// Pad a bit if min and max are the same. The amount of padding doesn't really matter in this case.
			if (minValue != 0) {
				paddedValue = minValue * 0.9;
			} else {
				paddedValue = 0;
			}
		} else {
			// add auto padding (could be zero)
			paddedValue = minValue - m_autoPadding * (maxValue - minValue);
		}

		if (minValue > 0) {
			// Don't auto pad to a value below zero unless the lowest value actually is below zero
			minValue = Math.max(paddedValue, 0);
		}

		if (m_alwaysShowZero && minValue > 0) {
			// adjust for always show zero
			return 0.0;
		}
		return minValue;
	}

	public void setAutoRangeEnabled(boolean enable) {
		m_autoRangeEnabled = enable;
		m_max = null;
		m_min = null;
	}

	public boolean isAutoRangeEnabled() {
		return m_autoRangeEnabled;
	}

	public void setRange(Number min, Number max) {
		setMin(min);
		setMax(max);
	}

	@Override
	public Position getPosition() {
		return m_position;
	}

	public void setPosition(Position position) {
		m_position = position;
	}

	public boolean isTitleLegendEnabled() {
		return m_titleLegendEnabled;
	}

	public void setTitleLegendEnabled(boolean show) {
		m_titleLegendEnabled = show;
	}

	public void setContentType(String contentType) {
		m_contentType = contentType;
	}

	@Override
	public String getContentType() {
		return m_contentType;
	}

	@Override
	public void setMinimumWidth(int width) {
		m_minimumWidth = width;
	}

	public int getMinimumWidth() {
		return m_minimumWidth;
	}
}
