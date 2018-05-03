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

import org.openjdk.jmc.greychart.AbstractGreyChart;
import org.openjdk.jmc.greychart.AbstractSeriesRenderer;
import org.openjdk.jmc.greychart.ChartChangeEvent;
import org.openjdk.jmc.greychart.ChartChangeListener;
import org.openjdk.jmc.greychart.GreyChartPanel;
import org.openjdk.jmc.greychart.IndexRenderer;
import org.openjdk.jmc.greychart.util.ChartRenderingToolkit;

/**
 * Default index renderer. Prefers a width depending on the currently selected font size.
 */
public class DefaultVerticalIndexRenderer extends AbstractSeriesRenderer implements IndexRenderer {
	public static final float DEFAULT_RELATIVE_WIDTH = 0.07f;

	private Image m_indexImage;
	private int m_maxStringLength = IndexRenderer.DEFAULT_MAX_STRING_LENGTH;
	private float m_relativeWidth = DEFAULT_RELATIVE_WIDTH;

	/**
	 * Listens for changes in the underlying data structure, and makes sure that the index will be
	 * regenerated if the structure changes.
	 */
	private class ChartListener implements ChartChangeListener {
		/**
		 * @see org.openjdk.jmc.greychart.ChartChangeListener#onChartChanged(org.openjdk.jmc.greychart.ChartChangeEvent)
		 */
		@Override
		public void onChartChanged(final ChartChangeEvent event) {
			if (event.getType() == ChartChangeEvent.ChangeType.DATA_STRUCTURE_CHANGED) {
				structureChanged();
			}
		}
	}

	/**
	 * Constructor.
	 *
	 * @param owner
	 *            the owner of this IndexRenderer.
	 */
	public DefaultVerticalIndexRenderer(AbstractGreyChart owner) {
		super(owner);
		owner.addChangeListener(new ChartListener());
	}

	public void structureChanged() {
		setRenderedBounds(new Rectangle(-1, -1, -1, -1));
		m_indexImage = null;
	}

	@Override
	public void render(Graphics2D ctx, Rectangle targetArea, Rectangle fullGraphArea) {
		if (targetArea.width <= 0 || targetArea.height <= 0) {
			clearRenderedBounds();
			return;
		}

		AffineTransform trans = ctx.getTransform();
		ctx.translate(targetArea.x, targetArea.y);

		if (!targetArea.equals(getRenderedBounds())) {
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

			int totalHeight = boxSize * names.length + padding * (names.length - 1) + padding;

			BufferedImage img = new BufferedImage(targetArea.width, totalHeight, ColorSpace.TYPE_RGB);
			Graphics2D gctx = (Graphics2D) img.getGraphics();
			gctx.setFont(font);
			gctx.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					ctx.getRenderingHint(RenderingHints.KEY_ANTIALIASING));
			// FIXME: re-implement
//			drawIndex(gctx, targetArea.width, totalHeight, boxSize, padding, getSeriesOwner().getDescriptors());
			m_indexImage = img;
			setRenderedBounds(targetArea);
		}
		int y = (targetArea.height - m_indexImage.getHeight(null)) / 2;
		ctx.drawImage(m_indexImage, 0, y, m_indexImage.getWidth(null), m_indexImage.getHeight(null), null);
		if (GreyChartPanel.DEBUG) {
			ChartRenderingToolkit.markBoundary(ctx, 0, 0, targetArea.width, targetArea.height, Color.GREEN);
		}
		ctx.setTransform(trans);
	}

//	private void drawIndex(
//		Graphics2D ctx, int width, int height, int boxSize, int padding,
//		DefaultDataSeriesDescriptor[] defaultDataSeriesDescriptors) {
//		if (GreyChartPanel.DEBUG) {
//			ctx.setColor(Color.PINK);
//		} else {
//			ctx.setColor(getBackground());
//		}
//
//		ctx.fillRect(0, 0, width, height);
//		FontMetrics fm = ctx.getFontMetrics();
//
//		int startY = 0;
//
//		for (DefaultDataSeriesDescriptor descriptor : defaultDataSeriesDescriptors) {
//			Color fillColor = descriptor.getTopColor() == null ? descriptor.getLineColor() : descriptor.getTopColor();
//			fillColor = fillColor == null ? getBackground() : fillColor;
//			Color lineColor = descriptor.getLineColor() == null ? getForeground() : descriptor.getLineColor();
//			ctx.setColor(fillColor);
//			ctx.fillRect(padding, startY, boxSize, boxSize);
//			ctx.setColor(lineColor);
//			ctx.drawRect(padding, startY, boxSize, boxSize);
//			int textX = 2 * padding + boxSize;
//			String name = descriptor.getName();
//			Rectangle2D bounds = fm.getStringBounds(name, ctx);
//			ctx.drawString(name, textX,
//					Math.round(startY + boxSize - (boxSize - bounds.getHeight()) / 2.0f) - fm.getDescent());
//			startY += (boxSize + padding);
//		}
//	}

	@Override
	public Dimension getPreferredDimensions(Graphics2D ctx, Rectangle totalDrawingArea) {
		int maxWidth = 0;

		FontMetrics fm = null;

		if (ctx.getFont().getSize() < getMinimumFontSize()) {
			fm = ctx.getFontMetrics(ctx.getFont().deriveFont(getMinimumFontSize()));
		} else {
			fm = ctx.getFontMetrics();
		}

		String[] names = new String[0]; 
		// FIXME: Get series names
//		String[] names = getTruncatedSeriesNames(m_maxStringLength);
//		if (names == null) {
//			return new Dimension(0, totalDrawingArea.height);
//		}

		int boxSize = Math.max(2, fm.getAscent() + fm.getDescent());
		int padding = Math.max(1, Math.round(boxSize * .3f));

		for (String name : names) {
			maxWidth = Math.max(maxWidth, fm.getStringBounds(name, ctx).getBounds().width);
		}
		// Height's not very interesting right now.
		return new Dimension(maxWidth + padding * 3 + boxSize, totalDrawingArea.height);
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
	 * @return the relative width of this IndexRenderer.
	 */
	public float getRelativeWidth() {
		return m_relativeWidth;
	}

	/**
	 * Sets the relative width of this index renderer.
	 *
	 * @param relativeWidth
	 *            the new relative width.
	 */
	public void setRelativeWidth(float relativeWidth) {
		m_relativeWidth = relativeWidth;
	}
}
