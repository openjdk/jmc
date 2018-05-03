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
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.color.ColorSpace;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.openjdk.jmc.greychart.AbstractSeriesRenderer;
import org.openjdk.jmc.greychart.FontAndColors;
import org.openjdk.jmc.greychart.GreyChartPanel;
import org.openjdk.jmc.greychart.SeriesGreyChart;
import org.openjdk.jmc.greychart.TitleRenderer;
import org.openjdk.jmc.greychart.util.ChartRenderingToolkit;

/**
 * Renders a title.
 */
public class DefaultTitleRenderer extends AbstractSeriesRenderer implements TitleRenderer {
	/**
	 * The default minimum font size to use when rendering a title.
	 */
	public final static float DEFAULT_MINIMUM_FONT_SIZE = FontAndColors.getDefaultFont().getSize();

	/**
	 * The default font size, relative to the height of the chart.
	 */
	public final static float DEFAULT_TITLE_RELATIVE_FONT_SIZE = 0.04f;
	private Color m_textColor = FontAndColors.getDefaultMiddleColor();
	private float m_relativeFontSize = DEFAULT_TITLE_RELATIVE_FONT_SIZE;
	private Image m_titleImageCache;
	private String m_lastTitle;
	private Object cachedImageAntialiasingValue;
	private final Rectangle m_lastRendered = new Rectangle(0, 0);

	/**
	 * Constructor.
	 *
	 * @param owner
	 *            the owner of the title renderer.
	 */
	public DefaultTitleRenderer(SeriesGreyChart owner) {
		super(owner);
		setRelativeFontSize(DEFAULT_TITLE_RELATIVE_FONT_SIZE);
		setMinimumFontSize(DEFAULT_MINIMUM_FONT_SIZE);
	}

	@Override
	public void render(Graphics2D ctx, Rectangle targetArea, Rectangle fullGraphArea) {
		if (targetArea.height <= 0) {
			clearRenderedBounds();
			return;
		}

		String title = getTitle();

		if (!targetArea.equals(m_lastRendered) || !title.equals(m_lastTitle)
				|| (ctx.getRenderingHint(RenderingHints.KEY_ANTIALIASING) != cachedImageAntialiasingValue)) {
			// Need size of height & boundingbox width.
			float fontSize = getActualFontSize(fullGraphArea.height * getRelativeFontSize());
			Font font = null;
			if (ctx.getFont().getSize2D() < fontSize) {
				font = ctx.getFont().deriveFont(fontSize);
			} else {
				font = ctx.getFont();
			}

			BufferedImage img = new BufferedImage(
					(int) ctx.getFontMetrics(font)
							.getStringBounds((title == null) || title.equals("") ? " " : title, ctx).getWidth(), //$NON-NLS-1$ //$NON-NLS-2$
					targetArea.height, ColorSpace.TYPE_RGB);

			Graphics2D gctx = (Graphics2D) img.getGraphics();
			gctx.setFont(font);
			gctx.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					ctx.getRenderingHint(RenderingHints.KEY_ANTIALIASING));
			drawTitle(gctx, targetArea, title);
			m_titleImageCache = img;
			cachedImageAntialiasingValue = ctx.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
			setRenderedBounds(targetArea);
			m_lastTitle = title;
			m_lastRendered.setBounds(targetArea);
		}
		int width = m_titleImageCache.getWidth(null);

		ctx.drawImage(m_titleImageCache, targetArea.x + (targetArea.width - width) / 2, targetArea.y, width,
				targetArea.height, null);
		if (GreyChartPanel.DEBUG) {
			ChartRenderingToolkit.markBoundary(ctx, targetArea.x, targetArea.y, targetArea.width, targetArea.height,
					Color.PINK);
		}
	}

	/**
	 * @return the color to use when rendering the title.
	 */
	public Color getTextColor() {
		return m_textColor;
	}

	/**
	 * Sets the color to use when rendering the title.
	 *
	 * @param textColor
	 *            the color to use when rendering the title.
	 */
	public void setTextColor(Color textColor) {
		m_textColor = textColor;
	}

	@Override
	public Dimension getPreferredDimensions(Graphics2D ctx, Rectangle totalDrawingArea) {
		float fontSize = getActualFontSize(totalDrawingArea.height * getRelativeFontSize());
		FontMetrics fm = ctx.getFontMetrics(ctx.getFont().deriveFont(fontSize));

		Rectangle2D bounds = fm.getStringBounds(getTitle(), ctx);
		Dimension result = new Dimension((int) Math.ceil(bounds.getWidth()),
				(int) Math.ceil(bounds.getHeight() + fm.getMaxDescent()));

		return result;
	}

	protected String getTitle() {
		return getOwner().getTitle();
	}

	/**
	 * The size of the title relative to the height of the chart.
	 *
	 * @param relativeFontSize
	 *            the new relative font size.
	 */
	public void setRelativeFontSize(float relativeFontSize) {
		m_relativeFontSize = relativeFontSize;
	}

	/**
	 * @return the size of the font relative to the height of the chart.
	 */
	public float getRelativeFontSize() {
		return m_relativeFontSize;
	}

	/**
	 * Draws the title on the supplied graphics context.
	 *
	 * @param ctx
	 *            context to draw on.
	 * @param rect
	 *            the rectangle to draw inside.
	 * @param title
	 *            the text to draw.
	 */
	public void drawTitle(Graphics2D ctx, Rectangle rect, String title) {
		setRenderedBounds(rect);
		if (GreyChartPanel.DEBUG) {
			ctx.setColor(Color.PINK);
		} else {
			ctx.setColor(getBackground());
		}

		ctx.fillRect(0, 0, rect.width, rect.height);
		ctx.setColor(m_textColor);
		Rectangle2D bounds = ctx.getFontMetrics().getStringBounds(getTitle(), ctx);
		ctx.drawString(title, 0, (int) (bounds.getHeight() - ctx.getFontMetrics().getDescent()));
	}
}
