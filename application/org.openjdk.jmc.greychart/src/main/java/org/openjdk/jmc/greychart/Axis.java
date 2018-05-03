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
 */
public interface Axis extends ChartRenderer {
	/**
	 * Converts a value in the value space to a drawing space coordinate.
	 *
	 * @param value
	 *            the value to convert to a drawing space coordinate.
	 * @return the drawing coordinate.
	 */
	int convertAxisValueToDrawingCoordinate(double value);

	/**
	 * Converts a coordinate in drawing space to a value in the value space.
	 *
	 * @param coordinate
	 *            the drawing coordinate to convert to an axis coordinate.
	 * @return the drawing coordinate.
	 */
	double convertDrawingCoordinateToAxisValue(int coordinate);

	/**
	 * @return the minimum value in the current range. If null, auto range for the min point will be
	 *         enabled.
	 */
	Number getMin();

	/**
	 * @return the maximum value in the current range. If null, auto range for the max point will be
	 *         enabled.
	 */
	Number getMax();

	/**
	 * Set the minimum value for the axis. or null if auto range should be enabled.
	 *
	 * @param min
	 *            Minimum value
	 */
	void setMin(Number min);

	/**
	 * Set the maximum value for the axis, or null if auto range should be enabled
	 *
	 * @param max
	 *            Maximum value
	 */
	void setMax(Number max);

	/**
	 * In an axis, the second rectangle argument to render should be the plotArea.
	 *
	 * @param ctx
	 *            the graphics context to render to.
	 * @param targetArea
	 *            the target are to render into.
	 * @param plotArea
	 *            the area where the actual plotting takes place.
	 */
	@Override
	void render(Graphics2D ctx, Rectangle targetArea, Rectangle plotArea);

	/**
	 * @return the title of the graph.
	 */
	String getTitle();

	/**
	 * @param title
	 *            the new title of the graph.
	 */
	void setTitle(String title);

	/**
	 * @return the title color of the graph.
	 */
	Color getTitleColor();

	/**
	 * @param color
	 *            the new title color of the graph.
	 */
	void setTitleColor(Color title);

	void setVisible(boolean enable);

	boolean isVisible();

	void addAxisListener(AxisListener l);

	void removeAxisListener(AxisListener l);

	String getContentType();
}
