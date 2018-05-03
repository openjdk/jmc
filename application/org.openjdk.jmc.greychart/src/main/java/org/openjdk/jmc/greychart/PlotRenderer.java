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
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Interface for the renders that draw the actual plots.
 */
public interface PlotRenderer extends ChartRenderer {
	/**
	 * The default relative font size to use.
	 *
	 * @see #setRelativeFontSize(float)
	 */
	public final static float DEFAULT_RELATIVE_FONT_SIZE = 0.025f;

	/**
	 * Sets the relative font size to use when drawing default text elements in the chart as a
	 * fraction of the overall height of the chart.
	 *
	 * @param relativeFontSize
	 *            font size as a fraction of the total chart height.
	 */
	void setRelativeFontSize(float relativeFontSize);

	/**
	 * @return the relative font size to use when drawing default text elements in the chart in
	 *         percent of the overall height of the chart.
	 */
	float getRelativeFontSize();

	/**
	 * Returns the background color in use.
	 *
	 * @see #clear(Graphics2D, Rectangle)
	 * @return the background color in use.
	 */
	@Override
	Color getBackground();

	/**
	 * Sets the background color to use in the plot.
	 *
	 * @see #clear(Graphics2D, Rectangle)
	 * @param color
	 *            the background color.
	 */
	@Override
	void setBackground(Color color);

	/**
	 * Used by the renderer to clear the background of the plot. This should be called before any
	 * other element, including the plot is rendered.
	 *
	 * @param ctx
	 *            the graphics context to use.
	 * @param area
	 *            the area to clear.
	 */
	void clear(Graphics2D ctx, Rectangle area);

}
