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
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import org.openjdk.jmc.greychart.AbstractSeriesRenderer;
import org.openjdk.jmc.greychart.GreyChartPanel;
import org.openjdk.jmc.greychart.IndexRenderer;
import org.openjdk.jmc.greychart.SeriesGreyChart;
import org.openjdk.jmc.greychart.util.ChartRenderingToolkit;

/**
 * Default index renderer. Prefers a height depending on the currently selected font size.
 */
public class DefaultHorizontalIndexRenderer extends AbstractSeriesRenderer implements IndexRenderer {
	/**
	 * The default relative height of the index.
	 */
	public final static float DEFAULT_RELATIVE_HEIGHT = 0.07f;
	private Image m_indexImage;
	private int m_maxStringLength = IndexRenderer.DEFAULT_MAX_STRING_LENGTH;
	private float m_relativeWidth = DEFAULT_RELATIVE_HEIGHT;

	/**
	 * Constructor.
	 *
	 * @param owner
	 *            the chart that owns this index.
	 */
	public DefaultHorizontalIndexRenderer(SeriesGreyChart owner) {
		super(owner);
	}

	/**
	 * May only be used with an XY Graph for now.
	 *
	 * @see org.openjdk.jmc.greychart.ChartRenderer#render(Graphics2D, Rectangle, Rectangle)
	 */
	@Override
	public void render(Graphics2D ctx, Rectangle rect, Rectangle chartRect) {
		if (rect.width <= 0 || rect.height <= 0) {
			clearRenderedBounds();
			return;
		}

		AffineTransform trans = ctx.getTransform();
		ctx.translate(rect.x, rect.y);

		// FIXME: Need to add change listener for when series names or number of series have changed.
		if (!rect.equals(getRenderedBounds())) {
			Font font = null;
			if (ctx.getFont().getSize() < getMinimumFontSize()) {
				font = ctx.getFont().deriveFont(getMinimumFontSize());
			} else {
				font = ctx.getFont();
			}

			String[] names = new String[0];
			// FIXME: Get series names
//			String[] names = getTruncatedSeriesNames(m_maxStringLength);

			int boxSize = Math.max(2, ctx.getFontMetrics(font).getAscent() + ctx.getFontMetrics(font).getDescent());
			int padding = Math.max(1, Math.round(boxSize * .3f));

			int totalWidth = 0;
			FontMetrics fm = ctx.getFontMetrics();
			for (String name : names) {
				totalWidth += (int) (fm.getStringBounds(name, ctx).getWidth() + padding * 2 + boxSize);
			}

			BufferedImage img = new BufferedImage(totalWidth, rect.height, ColorSpace.TYPE_RGB);
			Graphics2D gctx = (Graphics2D) img.getGraphics();
			gctx.setFont(font);
			gctx.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					ctx.getRenderingHint(RenderingHints.KEY_ANTIALIASING));
			// drawIndex(gctx, totalWidth, rect.height, boxSize, padding, getSeriesOwner().getDescriptors());
			m_indexImage = img;
			setRenderedBounds(rect);
		}
		int x = (rect.width - m_indexImage.getWidth(null)) / 2;
		ctx.drawImage(m_indexImage, x, 0, m_indexImage.getWidth(null), m_indexImage.getHeight(null), null);
		if (GreyChartPanel.DEBUG) {
			ChartRenderingToolkit.markBoundary(ctx, 0, 0, rect.width, rect.height, Color.GREEN);
		}
		ctx.setTransform(trans);
	}

//	private void drawIndex(
//		Graphics2D ctx, int width, int height, int boxSize, int padding,
//		DefaultDataSeriesDescriptor[] dataSeriesDescriptors) {
//		if (GreyChartPanel.DEBUG) {
//			ctx.setColor(Color.PINK);
//		} else {
//			ctx.setColor(getBackground());
//		}
//
//		ctx.fillRect(0, 0, width, height);
//		FontMetrics fm = ctx.getFontMetrics();
//
//		int y = Math.round(boxSize - (boxSize - fm.getHeight()) / 2.0f) - fm.getDescent() + padding;
//		int startX = 0;
//
//		for (DefaultDataSeriesDescriptor descriptor : dataSeriesDescriptors) {
//			ctx.setPaint(descriptor.getTopColor());
//			ctx.fillRect(startX, padding, boxSize, boxSize);
//			ctx.setColor(descriptor.getLineColor());
//			ctx.drawRect(startX, padding, boxSize, boxSize);
//			String name = descriptor.getName();
//			Rectangle2D bounds = fm.getStringBounds(name, ctx);
//			ctx.setColor(getForeground());
//			ctx.drawString(name, startX + boxSize + padding, y);
//			startX += (bounds.getWidth() + boxSize + padding * 2);
//		}
//	}

	@Override
	public Dimension getPreferredDimensions(Graphics2D ctx, Rectangle totalDrawingArea) {
		FontMetrics fm = null;

		if (ctx.getFont().getSize() < getMinimumFontSize()) {
			fm = ctx.getFontMetrics(ctx.getFont().deriveFont(getMinimumFontSize()));
		} else {
			fm = ctx.getFontMetrics();
		}

		int boxSize = Math.max(2, fm.getAscent() + fm.getDescent());
		int padding = Math.max(1, Math.round(boxSize * .3f));

		// Width is not very interesting for now.
		return new Dimension(totalDrawingArea.width, boxSize + padding * 2);
	}

	/**
	 * Must be > 2.
	 *
	 * @see org.openjdk.jmc.greychart.IndexRenderer#setMaxStringLength(int)
	 */
	@Override
	public void setMaxStringLength(int stringLength) {
		if (stringLength <= 2) {
			throw new IllegalArgumentException("The max string length must be larger than 2!"); //$NON-NLS-1$
		}
		m_maxStringLength = stringLength;
	}

	@Override
	public int getMaxStringLength() {
		return m_maxStringLength;
	}

	/**
	 * @return the relative width of this index renderer.
	 */
	public float getRelativeWidth() {
		return m_relativeWidth;
	}

	/**
	 * Sets the relative width for this index renderer.
	 *
	 * @param relativeWidth
	 *            new width.
	 */
	public void setRelativeWidth(float relativeWidth) {
		m_relativeWidth = relativeWidth;
	}
}
