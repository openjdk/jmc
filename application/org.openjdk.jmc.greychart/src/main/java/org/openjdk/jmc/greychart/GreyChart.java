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
 * Interface for Charts. Provides methods to render, and to get at the underlying data.
 */
public interface GreyChart {
	/**
	 * Sets the title of the chart.
	 *
	 * @param title
	 *            the chart title.
	 */
	void setTitle(String title);

	/**
	 * @return the title of the chart.
	 */
	String getTitle();

	/**
	 * Renders the chart on the supplied graphics context.
	 *
	 * @param ctx
	 *            the context to render on.
	 * @param where
	 *            the rectangle defining the area to render in.
	 */
	void render(Graphics2D ctx, Rectangle where);

	/**
	 * Removes the specified listener from the chart.
	 *
	 * @param l
	 *            the listener to remove.
	 */
	void removeChangeListener(ChartChangeListener l);

	/**
	 * Adds a change listener to the chart.
	 *
	 * @param l
	 *            the listener to add.
	 */
	void addChangeListener(ChartChangeListener l);

	/**
	 * @return the renderer responsible for plotting the graphical representation of the values.
	 */
	PlotRenderer getPlotRenderer();

	/**
	 * @return true if antialiasing will be used when rendering the graph, false otherwise.
	 */
	boolean isAntaliasingEnabled();

	/**
	 * Enables or disables anti aliasing for the chart.
	 *
	 * @param enable
	 *            if true, anti aliasing will be enabled, if false, anti aliasing will be disabled.
	 */
	void setAntialiasingEnabled(boolean enable);

	/**
	 * Convenience method that sets the background for the entire chart.
	 *
	 * @param bgColor
	 *            the background color to use.
	 */
	void setBackground(Color bgColor);

	/**
	 * @return the background color, if set. Note that the color can be set individually for each
	 *         renderer, and that the renderers will need to be queried individually for their
	 *         background color for a fully accurate result.
	 */
	Color getBackground();

	/**
	 * Convenience method that sets the foreground for the entire chart. The foreground color can be
	 * used for text, chart lines etc when appropriate.
	 *
	 * @param fgColor
	 *            the foreground color to use.
	 */
	void setForeground(Color fgColor);

	/**
	 * @return the foreground color, if set. Note that the color can be set individually for each
	 *         renderer, and that the renderers will need to be queried individually for their
	 *         foreground color for a fully accurate result.
	 */
	Color getForeground();
}
