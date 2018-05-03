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
package org.openjdk.jmc.greychart;

import java.awt.Color;
import java.awt.Rectangle;

/**
 * An abstract super class for the graph renderers.
 */
public abstract class AbstractChartRenderer implements ChartRenderer {
	/**
	 * The default minimum font size.
	 */
	public final static float DEFAULT_MINIMUM_FONT_SIZE = 10.0f;

	private Color m_background = FontAndColors.getDefaultBackground();
	private Color m_foreground = FontAndColors.getDefaultForeground();
	private float m_minimumFontSize = DEFAULT_MINIMUM_FONT_SIZE;
	private int m_fontSize;

	private Rectangle m_renderedBounds;

	@Override
	public Rectangle getRenderedBounds() {
		return m_renderedBounds;
	}

	/**
	 * Saves the bounds used during rendering.
	 *
	 * @param renderedBounds
	 *            the position and size used during rendering.
	 */
	public void setRenderedBounds(Rectangle renderedBounds) {
		m_renderedBounds = renderedBounds;
	}

	/**
	 * Clears the rendering bounds information. Used when chart renderer was to small or not
	 * rendered at all.
	 */
	public void clearRenderedBounds() {
		m_renderedBounds = new Rectangle();
	}

	@Override
	public int getRenderedWidth() {
		return getRenderedBounds().width;
	}

	@Override
	public int getRenderedHeight() {
		return getRenderedBounds().height;
	}

	/**
	 * @return if font size is relative, this returns 0, otherwise it will return the fixed font
	 *         size.
	 */
	public int getFontSize() {
		return m_fontSize;
	}

	/**
	 * Sets the font size to use when render the tick values and the axis title.
	 *
	 * @param fontSize
	 *            if 0, the font size will be relatice to the graph size and calculated
	 *            automatically. If non zero, this is the font size that will be used.
	 */
	public void setFontSize(int fontSize) {
		m_fontSize = fontSize;
	}

	/**
	 * If fonSize is 0 and this value is non-zero, this signifies the minimum size of the font that
	 * will be used to draw the tick values.
	 *
	 * @return the minimum font size.
	 */
	public float getMinimumFontSize() {
		return m_minimumFontSize;
	}

	/**
	 * If fonSize is 0 and this value is non-zero, this signifies the minimum size of the font that
	 * will be used to draw the tick values.
	 *
	 * @param minimumFontSize
	 *            the minimum font size to use.
	 */
	public void setMinimumFontSize(float minimumFontSize) {
		m_minimumFontSize = minimumFontSize;
	}

	/**
	 * Must return the actual font size based on the preferred one. Override this to provide own
	 * decision process.
	 *
	 * @param preferredFontSize
	 *            the font size that would be preferred, not considering other constraints.
	 * @return the actual font size to use to render this axis.
	 */
	protected float getActualFontSize(float preferredFontSize) {
		if (getFontSize() != 0) {
			return Math.max(getMinimumFontSize(), getFontSize());
		}
		return Math.max(getMinimumFontSize(), preferredFontSize);
	}

	@Override
	public Color getBackground() {
		return m_background;
	}

	@Override
	public void setBackground(Color color) {
		m_background = color;
	}

	@Override
	public Color getForeground() {
		return m_foreground;
	}

	@Override
	public void setForeground(Color color) {
		m_foreground = color;
	}
}
