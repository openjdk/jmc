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
package org.openjdk.jmc.greychart.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;

import javax.swing.JComponent;
import javax.swing.JFrame;

/**
 * Toolkit for graphics stuff.
 */
public final class ChartRenderingToolkit {
	private final static double LOG10FACT = 1.0 / Math.log(10);
	private final static double LOG2FACT = 1.0 / Math.log(2);
	private final static Stroke BOUNDARY_STROKE;

	static {
		float[] dash = new float[2];
		dash[0] = 3;
		dash[1] = 8;
		BOUNDARY_STROKE = new BasicStroke(.5f, 0, 0, 1.0f, dash, 0);
	}

	private ChartRenderingToolkit() {
		// Toolkit
	}

	/**
	 * Marks the boundary using a dotted stroke and the specified color. Used when rendering graphs
	 * in debug mode.
	 *
	 * @param ctx
	 *            the graphics context to render on.
	 * @param x
	 *            the start x value of the boundary.
	 * @param y
	 *            the start y value of the boundary.
	 * @param width
	 *            the width of the boundary.
	 * @param height
	 *            the height of the boundary.
	 * @param c
	 *            the color to paint with.
	 */
	public static void markBoundary(Graphics2D ctx, int x, int y, int width, int height, Color c) {
		Color oldColor = ctx.getColor();
		Stroke oldStroke = ctx.getStroke();

		ctx.setColor(c);
		ctx.setStroke(BOUNDARY_STROKE);
		ctx.drawRect(x, y, width, height);

		ctx.setColor(oldColor);
		ctx.setStroke(oldStroke);
	}

	/**
	 * Paint a cross with the specified attributes.
	 *
	 * @param ctx
	 *            the context to paint on.
	 * @param x
	 *            coordinate.
	 * @param y
	 *            coordinate.
	 * @param size
	 *            the size of the cross in user space.
	 * @param c
	 *            the color of the cross.
	 */
	public static void paintCross(Graphics2D ctx, int x, int y, int size, Color c) {
		Color oldColor = ctx.getColor();
		Stroke oldStroke = ctx.getStroke();

		ctx.setColor(c);
		ctx.drawLine(x - size, y - size, x + size, y + size);
		ctx.drawLine(x - size, y + size, x + size, y - size);

		ctx.setColor(oldColor);
		ctx.setStroke(oldStroke);
	}

	/**
	 * Calculates log10 (ln(n)/ln(10)).
	 *
	 * @param n
	 *            the number to calculate log10 of.
	 * @return (ln(n)/ln(10)).
	 */
	public static double log10(double n) {
		return Math.log(n) * LOG10FACT;
	}

	/**
	 * Calculates log2 (ln(n)/ln(2)).
	 *
	 * @param n
	 *            the number to calculate log2 of.
	 * @return (ln(n)/ln(2)).
	 */
	public static double log2(double n) {
		return Math.log(n) * LOG2FACT;
	}

	/**
	 * Displays the panel to be tested in a frame with the specified initial size.
	 *
	 * @param panel
	 *            the panel to display.
	 * @param width
	 *            the initial width of the frame.
	 * @param height
	 *            the initial height of the frame.
	 */
	public static void testComponent(JComponent panel, int width, int height) {
		JFrame frame = new JFrame("Testing " + panel.getName()); //$NON-NLS-1$
		frame.setContentPane(panel);
		frame.setSize(width, height);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}

	public static double fastFloor(double f) {
		if (f < 0) {
			return (long) f - 1;
		} else {
			return (long) f;
		}
	}
}
