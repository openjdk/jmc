/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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

import org.eclipse.swt.graphics.Rectangle;

import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.ui.UIPlugin;

/**
 * Currently all backgrounds range from 0 to a positive value. When adding backgrounds that do not,
 * this class should be rewritten. All background images must have the same size.
 */
class DialDevice {

	private static final ImageDescription[] BACKGROUNDS = new ImageDescription[] {
			ImageDescription.createStandardConfiguration(UIPlugin.getDefault().getImage(UIPlugin.ICON_DIAL_PANEL_1_10),
					10),
			ImageDescription
					.createStandardConfiguration(UIPlugin.getDefault().getImage(UIPlugin.ICON_DIAL_PANEL_10_100), 100),
			ImageDescription.createStandardConfiguration(
					UIPlugin.getDefault().getImage(UIPlugin.ICON_DIAL_PANEL_100_1000), 1000)};

	private static final ImageDescription[] DARK_BACKGROUNDS = new ImageDescription[] {
			ImageDescription.createStandardConfiguration(
					UIPlugin.getDefault().getImage(UIPlugin.ICON_DIAL_PANEL_1_10_DARK), 10),
			ImageDescription.createStandardConfiguration(
					UIPlugin.getDefault().getImage(UIPlugin.ICON_DIAL_PANEL_10_100_DARK), 100),
			ImageDescription.createStandardConfiguration(
					UIPlugin.getDefault().getImage(UIPlugin.ICON_DIAL_PANEL_100_1000_DARK), 1000)};

	private final IUnit unit;
	private final ImageDescription background;
	private final Boolean positive;
	private final boolean isDarkMode;

	private DialDevice(IUnit unit, ImageDescription background, Boolean positive, boolean isDarkMode) {
		this.unit = unit;
		this.background = background;
		this.positive = positive;
		this.isDarkMode = isDarkMode;
	}

	String getTitle() {
		String us = unit.getLocalizedSymbol();
		return positive == null ? "| " + us + " |" : positive ? us : "- " + us; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	ImageDescription getBackground() {
		return background;
	}

	double normalizeForDevice(IQuantity quantity) {
		return Math.min(1, Math.abs(quantity.doubleValueIn(unit) / getBackground().dialEndValue));
	}

	@Override
	public int hashCode() {
		return unit.hashCode() + background.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DialDevice) {
			DialDevice other = (DialDevice) obj;
			return other.unit.equals(unit) && other.background.equals(background)
					&& (positive != null ? positive.equals(other.positive) : other.positive == null);
		}
		return false;
	}

	static Rectangle getBackgroundSize() {
		// All background images must have the same size.
		return BACKGROUNDS[0].image.getBounds();
	}

	static DialDevice buildSuitableDevice(double minValue, double maxValue, IUnit inUnit, boolean isDarkMode) {
		if (minValue > maxValue || Double.isInfinite(minValue) || Double.isInfinite(maxValue)) {
			// no value background
			return new DialDevice(inUnit.getContentType().getPreferredUnit(inUnit.quantity(1), 1.0, 1000),
					isDarkMode ? DARK_BACKGROUNDS[0] : BACKGROUNDS[0], true, isDarkMode);
		}
		IQuantity quantity = inUnit.quantity(Math.max(Math.abs(minValue), Math.abs(maxValue)));
		IUnit preferredUnit = inUnit.getContentType().getPreferredUnit(quantity, 1.0,
				BACKGROUNDS[BACKGROUNDS.length - 1].dialEndValue);
		double value = quantity.doubleValueIn(preferredUnit);
		ImageDescription[] backgrounds = isDarkMode ? DARK_BACKGROUNDS : BACKGROUNDS;
		for (ImageDescription bg : backgrounds) {
			if (value <= bg.dialEndValue) {
				Boolean positive = maxValue < 0 ? Boolean.FALSE : (minValue < 0 ? null : Boolean.TRUE);
				return new DialDevice(preferredUnit, bg, positive, isDarkMode);
			}
		}

		/*
		 * FIXME: Could not find a suitable unit, this shouldn't happen.
		 * 
		 * Choosing a hopefully useful dial device, but too large values will be normalized to 1.
		 * Consider throwing an exception instead.
		 */
		return new DialDevice(preferredUnit, backgrounds[backgrounds.length - 1], true, isDarkMode);
		// throw new IllegalArgumentException("Cannot find a suitable dial device");
	}
}
