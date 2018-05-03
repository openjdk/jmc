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
package org.openjdk.jmc.ui.misc;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;

/**
 * Progress circle. Configuration is not thread safe, i.e. must be done before drawing starts. Could
 * easily be extended to expose all configuration options and to be made configurable in runtime,
 * but that is left as an exercise. ;)
 */
public class ProgressCircle extends JComponent {
	private static final long serialVersionUID = -7643464242630006633L;

	private static final int DEFAULT_SUBDIVISIONS = 18;
	private static final double DEFAULT_SIZE = .8;
	private static final double DEFAULT_INNER_CIRCLE_SIZE = .2;
	private static final int INNER_OUTER_ALPHA_OFFSET = 0;
	private static final double DEFAULT_INNER_OUTER_GAP = 0.025;
	private static final double FRACTION_OF_CIRCLE_TO_COLOUR = .8;
	private static final double FRACTION_OF_ARC_TO_COLOUR = .25;

	private volatile Color[] colors;
	private int subdivisions;
	private int alpha;
	private double relativeSize;
	private double innerOuterGap = DEFAULT_INNER_OUTER_GAP;
	private double innerOuterRatio = .85;
	private double decayPower = 6.0d;

	private final Direction innerDirection = Direction.CLOCKWISE;
	private final Direction outerDirection = Direction.COUNTERCLOCKWISE;

	private boolean isAntialiasing = true;

	private static enum Direction {
		CLOCKWISE(-1), COUNTERCLOCKWISE(1);

		private int value;

		Direction(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public ProgressCircle() {
		setBackground(Color.WHITE);
		setForeground(Color.BLACK);
		setDoubleBuffered(true);
		setSubdivisions(DEFAULT_SUBDIVISIONS);
		setRelativeSize(DEFAULT_SIZE);
	}

	/**
	 * @param subdivisions
	 *            the number of subdivisions of the circle.
	 */
	public void setSubdivisions(int subdivisions) {
		this.subdivisions = subdivisions;
		initializeColours(subdivisions);
	}

	private void initializeColours(int subdivisions) {
		Color[] newColors = new Color[subdivisions];
		for (int i = 0; i < subdivisions; i++) {
			newColors[i] = getBackground();
		}
		int toColor = Math.min(newColors.length, (int) Math.round(subdivisions * FRACTION_OF_CIRCLE_TO_COLOUR));
		double angle = 360.0 / toColor;
		for (int i = 0; i < toColor; i++) {
			newColors[newColors.length - 1 - i] = interpolateColor(360 - i * angle);
		}
		colors = newColors;
	}

	/**
	 * @param relativeSize
	 *            the relative size of the circle.
	 */
	public void setRelativeSize(double relativeSize) {
		this.relativeSize = relativeSize;
	}

	@Override
	public void paint(Graphics g) {
		if (isOpaque()) {
			clear(g);
		}
		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, useAntialiasing());
		int radius = (int) (Math.min(getWidth(), getHeight()) * relativeSize / 2);
		int innerRadius = (int) (radius * innerOuterRatio);
		int centerX = getWidth() / 2;
		int centerY = getHeight() / 2;

		if (!isOpaque()) {
			drawCenterCircle(g, centerX, centerY, radius);
		}
		drawOuterArcs(g, centerX, centerY, radius);
		drawCenterCircle(g, centerX, centerY, (int) Math.round(innerRadius + innerOuterGap * radius));
		drawArcs(g, centerX, centerY, innerRadius);
		drawCenterCircle(g, centerX, centerY, (int) (radius * DEFAULT_INNER_CIRCLE_SIZE));
	}

	private Object useAntialiasing() {
		return isAntialiasing() ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF;
	}

	private void drawOuterArcs(Graphics g, int centerX, int centerY, int radius) {
		int x = centerX - radius;
		int y = centerY - radius;
		int diameter = radius * 2;
		double angle = 360.0d / subdivisions;
		int paintAngle = (int) Math.round(angle * FRACTION_OF_ARC_TO_COLOUR);
		g.setColor(getForeground());
		for (int i = 0; i < subdivisions; i++) {
			int startAngle = (int) ((Math.round(angle * i) + alpha + INNER_OUTER_ALPHA_OFFSET)
					* outerDirection.getValue());
			g.setColor(colors[i]);
			g.fillArc(x, y, diameter, diameter, startAngle, paintAngle);
		}
	}

	private void clear(Graphics g) {
		g.setColor(getBackground());
		g.fillRect(0, 0, getWidth(), getHeight());
	}

	private void drawArcs(Graphics g, int centerX, int centerY, int radius) {
		int x = centerX - radius;
		int y = centerY - radius;
		int diameter = radius * 2;
		double angle = 360.0d / subdivisions;
		int paintAngle = (int) Math.round(angle * FRACTION_OF_ARC_TO_COLOUR);
		g.setColor(getForeground());
		for (int i = 0; i < subdivisions; i++) {
			int startAngle = (int) ((Math.round(angle * i) + alpha) * innerDirection.getValue());
			g.setColor(colors[i]);
			g.fillArc(x, y, diameter, diameter, startAngle, paintAngle);
		}
	}

	private Color interpolateColor(double angle) {
		double fraction = power(angle);
		int redDelta = (int) Math.round((getForeground().getRed() - getBackground().getRed()) * fraction);
		int greenDelta = (int) Math.round((getForeground().getGreen() - getBackground().getGreen()) * fraction);
		int blueDelta = (int) Math.round((getForeground().getBlue() - getBackground().getBlue()) * fraction);
		return new Color(getBackground().getRed() + redDelta, getBackground().getGreen() + greenDelta,
				getBackground().getBlue() + blueDelta);
	}

	private void drawCenterCircle(Graphics g, int centerX, int centerY, int radius) {
		g.setColor(getBackground());
		g.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
	}

	/**
	 * @param alpha
	 *            the absolute angle to move the component to.
	 */
	public void setAngle(int alpha) {
		double angle = 360.0d / subdivisions;
		this.alpha = (int) (Math.round(alpha / angle) * angle);
	}

	public double power(double angle) {
		return Math.pow(1.0d / 360.0d * angle, decayPower);
	}

	// Swing optimization for more efficient rendering...
	@Override
	public boolean isOptimizedDrawingEnabled() {
		return true;
	}

	@Override
	public void setBackground(Color bg) {
		super.setBackground(bg);
		initializeColours(subdivisions);
	}

	@Override
	public void setForeground(Color fg) {
		super.setForeground(fg);
		initializeColours(subdivisions);
	}

	/**
	 * Sets the relative size of the inner circle to the outer.
	 *
	 * @param innerOuterRatio
	 *            the relative size of the inner circle to the outer.
	 */
	public void setInnerOuterRatio(double innerOuterRatio) {
		this.innerOuterRatio = innerOuterRatio;
	}

	/**
	 * Sets the relative size of the gap between the inner circle and the outer relative to the
	 * entire radius.
	 *
	 * @param innerOuterGap
	 *            the relative size of the gap between the inner circle and the outer relative to
	 *            the entire radius.
	 */
	public void setInnerOuterGap(double innerOuterGap) {
		this.innerOuterGap = innerOuterGap;
	}

	/**
	 * @return the power of the color decay function. The higher, the faster the colored sections
	 *         will decay.
	 */
	public double getDecayPower() {
		return decayPower;
	}

	/**
	 * @param decayPower
	 *            sets the power of the color decay function. The higher, the faster the colored
	 *            sections will decay.
	 */
	public void setDecayPower(double decayPower) {
		this.decayPower = decayPower;
	}

	public void setAntialiasing(boolean isAntialiasing) {
		this.isAntialiasing = isAntialiasing;
	}

	public boolean isAntialiasing() {
		return isAntialiasing;
	}

}
