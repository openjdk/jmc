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
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.openjdk.jmc.greychart.AbstractSeriesPlotRenderer;
import org.openjdk.jmc.greychart.FontAndColors;
import org.openjdk.jmc.greychart.SeriesGreyChart;
import org.openjdk.jmc.greychart.SeriesMetadataProvider;
import org.openjdk.jmc.greychart.data.DataSeriesProvider;
import org.openjdk.jmc.ui.common.xydata.DataSeries;

/**
 * Renders a simple pie. Not really optimized for dynamic charts (creates arrays). Can be optimized
 * if we ever want these to be dynamic.
 */
public class DefaultPieRenderer extends AbstractSeriesPlotRenderer {
	/**
	 * Default Color for the text in this PieChart.
	 */
	public final static Color DEFAULT_TEXT_COLOR = Color.DARK_GRAY;

	/**
	 * Default Color for the lines in the PieChart.
	 */
	public final static Color DEFAULT_LINE_COLOR = Color.DARK_GRAY;

	/**
	 * Default format string for the text drawn.
	 */
	public final static String DEFAULT_TEXT_FORMAT_STRING = "{0} ({1,number,##0.0%})"; //$NON-NLS-1$

	private final Dimension m_preferred = new Dimension(100, 100);
	private boolean m_labelsEnabled = true;
	private Color m_textColor = DEFAULT_TEXT_COLOR;
	private Color m_lineColor = DEFAULT_LINE_COLOR;
	private String m_messageFormat = DEFAULT_TEXT_FORMAT_STRING;

	/**
	 * Constructor.
	 *
	 * @param owner
	 *            the owner of this pie renderer.
	 */
	public DefaultPieRenderer(SeriesGreyChart owner) {
		super(owner);
	}

	@Override
	public Dimension getPreferredDimensions(Graphics2D ctx, Rectangle totalDrawingArea) {
		m_preferred.height = (int) (totalDrawingArea.height * .6);
		m_preferred.width = totalDrawingArea.width;
		return m_preferred;
	}

	@Override
	public void render(Graphics2D ctx, Rectangle targetArea, Rectangle fullGraphArea) {
		setRenderedBounds(targetArea);
		DataSeriesProvider<Number> provider = getOwner().getDataProvider();

		DataSeries[] dataSeries = provider.getDataSeries();
		double sum = 0;
		Map<DataSeries<Number>, Number> values = new HashMap<>();
		Map<DataSeries<Number>, Point> coordinates = new HashMap<>();
		int size = (int) Math.round(Math.min(targetArea.width, targetArea.height) * .9);
		int x = targetArea.x + (targetArea.width - size) / 2, y = targetArea.y + (targetArea.height - size) / 2;
		for (DataSeries<Number> s : dataSeries) {
			Number val = s.createIterator(Long.MIN_VALUE, Long.MAX_VALUE).next();
			if (val != null) {
				values.put(s, val);
				sum += val.doubleValue();
			}
		}
		double arcPos = 90;

		// Draw from max to min.
		SeriesMetadataProvider smdp = getOwner().getMetadataProvider();
		for (DataSeries s : dataSeries) {
			double arc = ((values.get(s).doubleValue() / sum) * 360.0);
			ctx.setPaint(smdp.getTopColor(s));
			ctx.fillArc(x, y, size, size, (int) arcPos, (int) -arc);

			ctx.setPaint(smdp.getLineColor(s));
			ctx.drawArc(x, y, size, size, (int) arcPos, (int) -arc);
			if (isLabelsEnabled()) {
				// Calculate line target coords
				double bisectris = Math.toRadians((2 * arcPos - arc) / 2);
				double distance = (size / 2.0) * .6;

				int coordX = (int) Math.round((targetArea.getCenterX() + Math.cos(bisectris) * distance));
				int coordY = (int) Math.round((targetArea.getCenterY() - Math.sin(bisectris) * distance));
				coordinates.put(s, new Point(coordX, coordY));
			}
			arcPos -= arc;
		}

		// Next paint lines etc over everything already painted
		if (isLabelsEnabled()) {
			FontMetrics fm = ctx.getFontMetrics();
			int rightY, leftY;
			rightY = leftY = targetArea.y;
			int lastX = 0;
			ctx.setColor(FontAndColors.getDefaultForeground());
			for (DataSeries s : dataSeries) {
				// FIXME: Paint it in the right section, fix truncation some day.
				// FIXME: Re-implement
				Object[] arguments = new Object[] {"Series", //$NON-NLS-1$
//						getTruncatedSeriesName(18, s),
						Double.valueOf(values.get(s).doubleValue() / sum)};
				String text = MessageFormat.format(getMessageFormat(), arguments);
				Rectangle2D b = fm.getStringBounds(text, ctx);
				Point coordinate = coordinates.get(s);
				if (coordinate.x >= targetArea.getCenterX()) {
					rightY += (b.getHeight() * 2);
					lastX = (int) (targetArea.x + targetArea.width - b.getWidth());
					ctx.setColor(getLineColor());
					ctx.drawLine(coordinate.x, coordinate.y, lastX - 5, rightY);
					ctx.drawLine(lastX - 5, rightY, targetArea.x + targetArea.width, rightY);
					ctx.setColor(getTextColor());
					ctx.drawString(text, lastX, rightY);
				} else {
					leftY += (b.getHeight() * 2);
					lastX = (int) b.getWidth() + targetArea.x;
					ctx.setColor(getLineColor());
					ctx.drawLine(coordinate.x, coordinate.y, lastX + 5, leftY);
					ctx.drawLine(lastX + 5, leftY, targetArea.x, leftY);
					ctx.setColor(getTextColor());
					ctx.drawString(text, targetArea.x, leftY);

				}
			}
		}

	}

	/**
	 * @return Returns true if
	 */
	public boolean isLabelsEnabled() {

		return m_labelsEnabled;
	}

	/**
	 * @param renderLabels
	 *            The renderLabels to set.
	 */
	public void setLabelsEnabled(boolean renderLabels) {
		m_labelsEnabled = renderLabels;
	}

	/**
	 * @return Returns the lineColor.
	 */
	public Color getLineColor() {
		return m_lineColor;
	}

	/**
	 * @param lineColor
	 *            The lineColor to set.
	 */
	public void setLineColor(Color lineColor) {
		m_lineColor = lineColor;
	}

	/**
	 * @return Returns the textColor.
	 */
	public Color getTextColor() {
		return m_textColor;
	}

	/**
	 * @param textColor
	 *            The textColor to set.
	 */
	public void setTextColor(Color textColor) {
		m_textColor = textColor;
	}

	/**
	 * @return Returns the messageFormat.
	 */
	public String getMessageFormat() {
		return m_messageFormat;
	}

	/**
	 * @param messageFormat
	 *            The messageFormat to set.
	 */
	public void setMessageFormat(String messageFormat) {
		m_messageFormat = messageFormat;
	}

}
