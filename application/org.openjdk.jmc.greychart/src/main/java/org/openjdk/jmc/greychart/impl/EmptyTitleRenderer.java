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
import java.awt.Graphics2D;
import java.awt.Rectangle;

import org.openjdk.jmc.greychart.ChartRenderer;
import org.openjdk.jmc.greychart.FontAndColors;
import org.openjdk.jmc.greychart.GreyChart;

public class EmptyTitleRenderer implements ChartRenderer {
	private final int preferredHeight;
	private final GreyChart chart;

	public EmptyTitleRenderer(GreyChart chart, int height) {
		preferredHeight = height;
		this.chart = chart;
	}

	@Override
	public Color getBackground() {
		return FontAndColors.getDefaultBackground();
	}

	@Override
	public Color getForeground() {
		return FontAndColors.getDefaultForeground();
	}

	@Override
	public Rectangle getRenderedBounds() {
		return new Rectangle();
	}

	@Override
	public int getRenderedHeight() {
		return 0;
	}

	@Override
	public Dimension getPreferredDimensions(Graphics2D ctx, Rectangle totalDrawingArea) {
		return new Dimension(totalDrawingArea.width, preferredHeight);
	}

	@Override
	public int getRenderedWidth() {
		return 0;
	}

	@Override
	public void render(Graphics2D ctx, Rectangle targetArea, Rectangle fullGraphArea) {
		// not used
	}

	@Override
	public void setBackground(java.awt.Color color) {
		// not used
	}

	@Override
	public void setForeground(java.awt.Color color) {
		// not used
	}

	public GreyChart getOwner() {
		return chart;
	}

}
