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
package org.openjdk.jmc.common.util;

import java.awt.Color;
import java.awt.GradientPaint;

/**
 * Toolkit for working with {@link Color AWT colors}.
 */
public class ColorToolkit {

	private static float normalHashValue = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz".hashCode(); //$NON-NLS-1$

	private ColorToolkit() {
		throw new UnsupportedOperationException("Do not instantiate toolkit"); //$NON-NLS-1$
	}

	/**
	 * Encode a color as a hexadecimal string starting with '#'. This string can be decoded using
	 * {@link #decode(String)}.
	 *
	 * @param color
	 *            color to encode
	 * @return a hexadecimal string representing the color
	 */
	public static String encode(Color color) {
		return "#" + Integer.toHexString((color.getRed() << 16) + (color.getGreen() << 8) + (color.getBlue())); //$NON-NLS-1$
	}

	/**
	 * Decode a string created by {@link #encode(Color)} and create a corresponding color instance.
	 *
	 * @param hexColor
	 *            hexadecimal string to decode
	 * @return a color instance
	 * @throws NumberFormatException
	 *             if the string can't be decoded
	 */
	public static Color decode(String hexColor) throws NumberFormatException {
		return Color.decode(hexColor);
	}

	/**
	 * Generate a color based on an object. The goal is to be able to run this on a number of
	 * different objects and get colors that are distinguishable from each other.
	 * <p>
	 * The algorithm for generating colors is arbitrary and may be changed.
	 *
	 * @param o
	 *            object to get a color for
	 * @return a color instance
	 */
	public static Color getDistinguishableColor(Object o) {
		/*
		 * The 19 offset seems to land more of the commonly encountered string values closer to the
		 * blue/violet end of the spectrum.
		 */
		// FIXME: Need more variety in the generated colors
		int hash = String.valueOf(o).hashCode();
		float saturation = Math.max(0.4f, Math.min(0.95f, Math.abs(hash / normalHashValue)));
		float brightness = Math.max(0.7f, Math.min(0.95f, Math.abs(hash / normalHashValue)));
		float hue = (hash + 19) % 30 / 29.0f;
		// return Color.getHSBColor((hash + 19) % 35 / 34.0f, saturation, brightness);
		return Color.getHSBColor(hue, saturation, brightness);
		// return Color.getHSBColor((hash + 19) % 30 / 29.0f, 0.72f, 0.9f);
	}

	/**
	 * Get a gradient paint based on a top color. The bottom color will be generated based on the
	 * top color.
	 *
	 * @param topColor
	 *            color for the top of the gradient
	 * @param top
	 *            X coordinate for the top color
	 * @return a gradient with a generated bottom color at X=0 and the top color at X={@code top}
	 */
	public static GradientPaint getGradientPaint(Color topColor, int top) {
		Color bottomColor = ColorToolkit.getGradientBottomColor(topColor);
		return new GradientPaint(0, 0, bottomColor, 0, top, topColor);
	}

	private static Color getGradientBottomColor(Color topColor) {
		// FIXME: The gradient might not be optimal, maybe use the brightness balance stuff currently in DataPageToolkit?
		// FIXME: Is it OK to create a new color or do we risk a memory leak?
		int r = topColor.getRed();
		int g = topColor.getGreen();
		int b = topColor.getBlue();
		r = getGradientComponent(r);
		g = getGradientComponent(g);
		b = getGradientComponent(b);
		return new Color(r, g, b);
	}

	private static int getGradientComponent(int c) {
		// NOTE: Designed after a typical gradient combo 255, 128, 0 -> 254, 191, 127
		if (c > 255 * 0.75) {
			return c - 1;
		} else if (c > 255 * 0.25) {
			return c + 63;
		} else {
			return c + 127;
		}
	}

	/**
	 * Create a color with a specified alpha value.
	 *
	 * @param color
	 *            base color
	 * @param alpha
	 *            alpha value
	 * @return a color based on the base color and with the specified alpha value
	 */
	public static Color withAlpha(Color color, int alpha) {
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}

	public static Color blend(Color fg, Color bg) {
		double fga = fg.getAlpha() / 255.0;
		double bga = bg.getAlpha() / 255.0;
		double fgTransparency = (1 - fga);

		double r = fga * fg.getRed() + bga * bg.getRed() * fgTransparency;
		double g = fga * fg.getGreen() + bga * bg.getGreen() * fgTransparency;
		double b = fga * fg.getBlue() + bga * bg.getBlue() * fgTransparency;
		double a = fga + bga * fgTransparency;

		return new Color((int) r, (int) g, (int) b, (int) a);
	}

}
