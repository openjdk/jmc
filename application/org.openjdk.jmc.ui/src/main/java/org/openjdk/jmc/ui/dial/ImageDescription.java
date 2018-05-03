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
package org.openjdk.jmc.ui.dial;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.common.CorePlugin;

/**
 * Class for storing the image configuration. Where the center of a dial should be and so on
 */
public class ImageDescription {
	/**
	 * The tick marks are not uniformly distributed, so I'll fit a third degree polynom through (0,
	 * 174), (0.1, 154), (0.9, 26) and (1, 6)
	 *
	 * <pre>
	 * f(n) = a*n^3 + b*n^2 + c*n + d
	 *
	 * f(0.0) = 174 f(0.1) = 154 f(0.9) = 26 f(1.0) = 6
	 *
	 * =>
	 *
	 * a = -800/9 b = 1200/9 c = -1912/9 d = 174
	 *
	 * f(x) = (-800 * n * n * n + 1200 * n * n - 1912 * n) / 9 + 174
	 * </pre>
	 */
	static class ImageFunction implements IImageFunction {
		@Override
		public double toRadians(double n) {
			return Math.toRadians((-800 * n * n * n + 1200 * n * n - 1912 * n) / 9 + 174);
		}
	}

	/**
	 * The image to use as a dial.
	 */
	public Image image;
	/**
	 * The center point of the dial in the image.
	 */
	public Point origin;

	/**
	 * The radius from the center where the base of the dial should be.
	 */
	public double dialStartRadius;

	/**
	 * The radius from the center where the tip of the dial should be.
	 */
	public double dialEndRadius;

	/**
	 * The radius of outer bound of the gradient.
	 */
	public double gradientRadius;

	/**
	 * The radius of the drawn dial in pixels.
	 */
	public static final int DIAL_RADIUS = 108;

	/**
	 * Function that maps a normalized value to dial value, in radians.
	 */
	public IImageFunction imageFunction;

	/**
	 * The center point for the dial text
	 */
	public Point dialTextCenter;

	public double dialEndValue;

	/**
	 * Create standard image configuration that can be used with
	 * {@link CorePlugin#ICON_DIAL_PANEL_1_10}, {@link UIPlugin#ICON_DIAL_PANEL_10_100} and
	 * {@link CorePlugin#ICON_DIAL_PANEL_100_1000}.
	 *
	 * @param image
	 * @param dialEndValue
	 * @return
	 */
	public static ImageDescription createStandardConfiguration(Image image, double dialEndValue) {
		ImageDescription config = new ImageDescription();
		config.image = image;
		config.dialStartRadius = 20;
		config.dialEndRadius = config.image.getBounds().width / 2 - 33;
		config.gradientRadius = config.image.getBounds().width / 2 - 10;
		config.origin = new Point(config.image.getBounds().width / 2, config.image.getBounds().height - 1);
		config.imageFunction = new ImageFunction();
		config.dialTextCenter = new Point(config.origin.x, config.origin.y - 35);
		config.dialEndValue = dialEndValue;
		return config;
	}

	private boolean safeEquals(Object a, Object b) {
		if (a == b) {
			return true;
		} else {
			return (a != null) && a.equals(b);
		}
	}

	// Need equals to compute whether to update image. Since mutable no good hashCode is available
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ImageDescription)) {
			return false;
		}
		ImageDescription desc = (ImageDescription) o;

		return o != null && safeEquals(desc.image, image) && safeEquals(desc.origin, origin)
				&& desc.dialStartRadius == dialStartRadius && desc.dialEndRadius == dialEndRadius
				&& desc.gradientRadius == gradientRadius && safeEquals(desc.dialTextCenter, dialTextCenter);
	}
}
