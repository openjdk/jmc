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

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import org.openjdk.jmc.greychart.LongAxis;
import org.openjdk.jmc.greychart.SeriesGreyChart;
import org.openjdk.jmc.greychart.XAxis;

/**
 * Simple X axis that renders a line with a little arrow at the end.
 */
public class SimpleXAxis extends AbstractSimpleAxis implements XAxis, LongAxis {
	private final Dimension m_dimension = new Dimension();

	/**
	 * Constructor.
	 *
	 * @param owner
	 *            the chart of the owner.
	 */
	public SimpleXAxis(SeriesGreyChart owner) {
		super(owner);
		setTickMarksEnabled(false);
	}

	@Override
	public void render(Graphics2D ctx, Rectangle targetArea, Rectangle plotArea) {
		super.render(ctx, targetArea, plotArea);
		int endX = targetArea.x + getRenderedWidth();
		ctx.drawLine(targetArea.x, targetArea.y, endX, targetArea.y);
		// Paint arrow
		ctx.drawLine(endX - getMargin(), targetArea.y - getMargin(), endX, targetArea.y);
		ctx.drawLine(endX, targetArea.y, endX - getMargin(), targetArea.y + getMargin());
	}

	@Override
	public Dimension getPreferredDimensions(Graphics2D ctx, Rectangle totalDrawingArea) {
		m_dimension.height = getMargin();
		m_dimension.width = totalDrawingArea.width;
		return m_dimension;
	}

	@Override
	public long convertDrawingCoordinateToLongAxisValue(int coordinate) {
		return (long) ((getMax().longValue() - getMin().longValue())
				* (((double) coordinate) / ((double) getRenderedHeight())) + m_min.longValue());
	}

	@Override
	public int convertLongAxisValueToDrawingCoordinate(long value) {
		return (int) ((getRenderedWidth() * (value - m_min.longValue()))
				/ ((getMax().longValue() - getMin().longValue())));
	}

	@Override
	public int convertAxisValueToDrawingCoordinate(double value) {
		return (int) ((getRenderedWidth()
				/ ((getMax().longValue() - getMin().longValue()) * (value - m_min.longValue()))) + .5);
	}

	@Override
	public double convertDrawingCoordinateToAxisValue(int coordinate) {
		return ((getMax().longValue() - getMin().longValue()) * (((double) coordinate) / ((double) getRenderedHeight()))
				+ m_min.longValue());
	}

	@Override
	public long getLongRange() {
		return getMax().longValue() - getMin().longValue();
	}
}
