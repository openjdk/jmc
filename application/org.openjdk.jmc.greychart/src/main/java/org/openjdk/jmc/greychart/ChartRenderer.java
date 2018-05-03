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
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Interface for classes capable of drawing a key chart element at a certain position.
 * <p>
 * The rendering typically takes place in the following fashion:
 * <ol>
 * <li>The chart controlling all the renderers will ask each renderer how much space it would need
 * given a certain drawing area, plus clear the rendering area.
 * <li>renderGraph will be called on each renderer with the partition of space alotted to the
 * renderer plus the space used by the chart. It is okay to render in the plot area to produce
 * gridlines etc. The chart will always be rendered last.
 * </ol>
 */
public interface ChartRenderer {
	/**
	 * The default background color.
	 *
	 * @see #setBackground(Color)
	 */

	/**
	 * Returns the preferred dimensions this renderer would like to render in.
	 *
	 * @param ctx
	 *            the graphics context to eventually render in.
	 * @param totalDrawingArea
	 *            the total drawing area the chart will use.
	 * @return the preferred dimensions.
	 */
	Dimension getPreferredDimensions(Graphics2D ctx, Rectangle totalDrawingArea);

	/**
	 * Allocated bounds during last rendering.
	 *
	 * @return the position and size used during the last rendering.
	 */
	Rectangle getRenderedBounds();

	/**
	 * Allocated width during last rendering.
	 *
	 * @return the width used during the last rendering.
	 */
	int getRenderedWidth();

	/**
	 * Allocated height during last rendering.
	 *
	 * @return the height used during the last rendering.
	 */
	int getRenderedHeight();

	/**
	 * Renders the chart element in the specified target area.
	 *
	 * @param ctx
	 *            the graphics context to render to.
	 * @param targetArea
	 *            the target are to render into.
	 * @param fullGraphArea
	 *            the full chart area.
	 */
	void render(Graphics2D ctx, Rectangle targetArea, Rectangle fullGraphArea);

	/**
	 * @return the background color of this renderer.
	 */
	Color getBackground();

	/**
	 * Sets the background of this renderer.
	 *
	 * @param color
	 *            the new background color to use.
	 */
	void setBackground(Color color);

	/**
	 * @return the foreground color of this renderer.
	 */
	Color getForeground();

	/**
	 * Sets the foreground of this renderer.
	 *
	 * @param color
	 *            the new foreground color to use.
	 */
	void setForeground(Color color);

}
