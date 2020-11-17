/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, Red Hat Inc. All rights reserved.
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

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public class PatternFly {

	private static final String AWT = "AWT";
	private static final String SWT = "SWT";

	/**
	 * The following color Palette is based on the PatternFly palette for reinforcing application
	 * content and workflows. https://www.patternfly.org/v3/styles/color-palette/
	 */
	public enum Palette {
		/**
		 * Primary Colors: Black & Blue
		 */
		PF_BLACK("#030303"),
		PF_BLACK_100("#fafafa"),
		PF_BLACK_150("#f5f5f5"),
		PF_BLACK_200("#ededed"),
		PF_BLACK_300("#d1d1d1"),
		PF_BLACK_400("#bbbbbb"),
		PF_BLACK_500("#8b8d8f"),
		PF_BLACK_600("#72767b"),
		PF_BLACK_700("#4d5258"),
		PF_BLACK_800("#393f44"),
		PF_BLACK_900("#292e34"),

		PF_BLUE("#0088ce"),
		PF_BLUE_50("#def3ff"),
		PF_BLUE_100("#bee1f4"),
		PF_BLUE_200("#7dc3e8"),
		PF_BLUE_300("#39a5dc"),
		PF_BLUE_400("#0088ce"),
		PF_BLUE_500("#00659c"),
		PF_BLUE_600("#004368"),
		PF_BLUE_700("#002235"),

		/**
		 * Secondary Colors: Red, Orange, Gold, Light Green, Green, Light Blue, Purple
		 */
		PF_RED("#8b0000"),
		PF_RED_100("#cc0000"),
		PF_RED_200("#a30000"),
		PF_RED_300("#8b0000"),
		PF_RED_400("#470000"),
		PF_RED_500("#2c0000"),

		PF_ORANGE("#ec7a08"),
		PF_ORANGE_100("#fbdebf"),
		PF_ORANGE_200("#f7bd7f"),
		PF_ORANGE_300("#f39d3c"),
		PF_ORANGE_400("#ec7a08"),
		PF_ORANGE_500("#b35c00"),
		PF_ORANGE_600("#773d00"),
		PF_ORANGE_700("#3b1f00"),

		PF_GOLD("#f0ab00"),
		PF_GOLD_100("#fbeabc"),
		PF_GOLD_200("#f9d67a"),
		PF_GOLD_300("#f5c12e"),
		PF_GOLD_400("#f0ab00"),
		PF_GOLD_500("#b58100"),
		PF_GOLD_600("#795600"),
		PF_GOLD_700("#3d2c00"),

		PF_LIGHT_GREEN("#92d400"),
		PF_LIGHT_GREEN_100("#e4f5bc"),
		PF_LIGHT_GREEN_200("#c8eb79"),
		PF_LIGHT_GREEN_300("#ace12e"),
		PF_LIGHT_GREEN_400("#92d400"),
		PF_LIGHT_GREEN_500("#6ca100"),
		PF_LIGHT_GREEN_600("#486b00"),
		PF_LIGHT_GREEN_700("#253600"),

		PF_GREEN("#3f9c35"),
		PF_GREEN_100("#cfe7cd"),
		PF_GREEN_200("#9ecf99"),
		PF_GREEN_300("#6ec664"),
		PF_GREEN_400("#3f9c35"),
		PF_GREEN_500("#2d7623"),
		PF_GREEN_600("#1e4f18"),
		PF_GREEN_700("#0f280d"),

		PF_CYAN("#007a87"),
		PF_CYAN_100("#bedee1"),
		PF_CYAN_200("#7dbdc3"),
		PF_CYAN_300("#3a9ca6"),
		PF_CYAN_400("#007a87"),
		PF_CYAN_500("#005c66"),
		PF_CYAN_600("#003d44"),
		PF_CYAN_700("#001f22"),

		PF_LIGHT_BLUE("#00b9e4"),
		PF_LIGHT_BLUE_100("#beedf9"),
		PF_LIGHT_BLUE_200("#7cdbf3"),
		PF_LIGHT_BLUE_300("#35caed"),
		PF_LIGHT_BLUE_400("#00b9e4"),
		PF_LIGHT_BLUE_500("#008bad"),
		PF_LIGHT_BLUE_600("#005c73"),
		PF_LIGHT_BLUE_700("#002d39"),

		PF_PURPLE("#703fec"),
		PF_PURPLE_100("#c7bfff"),
		PF_PURPLE_200("#a18fff"),
		PF_PURPLE_300("#8461f7"),
		PF_PURPLE_400("#703fec"),
		PF_PURPLE_500("#582fc0"),
		PF_PURPLE_600("#40199a"),
		PF_PURPLE_700("#1f0066");

		private final String color;

		Palette(String color) {
			this.color = color;
		}

		/**
		 * Return a color object of the type corresponding to the constant passed into the method.
		 * This function first converts the PatternFly color hex value to an AWT Color object. Next,
		 * it either returns the AWT Color object if the constant type is AWT, or uses the RGB
		 * values to generate an SWT Color object.
		 *
		 * @param type
		 *            String constant: AWT or SWT
		 * @return a RGB color (as a regular Object to be casted later)
		 */
		private Object parseRGB(String type) {
			java.awt.Color awtColor = new java.awt.Color(java.awt.Color.decode(this.color).getRGB());
			switch (type) {
			case (AWT):
				return awtColor;
			case (SWT):
				return new org.eclipse.swt.graphics.Color(Display.getCurrent(), awtColor.getRed(), awtColor.getGreen(),
						awtColor.getBlue());
			default:
				return null;
			}
		}

		/**
		 * Converts the PatternFly hex color value to an AWT Color object
		 * 
		 * @return AWT Color of the selected PatternFly color
		 */
		public java.awt.Color getAWTColor() {
			return (java.awt.Color) parseRGB(AWT);
		}

		/**
		 * Converts the PatternFly hex color value to an SWT Color object
		 * 
		 * @return SWT Color of the selected PatternFly color
		 */
		public org.eclipse.swt.graphics.Color getSWTColor() {
			return (org.eclipse.swt.graphics.Color) parseRGB(SWT);
		}

		/**
		 * Page & Component Specific Colors
		 */
		public static Color getThreadsPageBackgroundColor() {
			return PF_BLACK_200.getSWTColor();
		}
	}
}
