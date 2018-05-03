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
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Locale;
import java.util.logging.Level;

import javax.swing.UIManager;
import javax.swing.text.StyleContext;

import org.openjdk.jmc.ui.common.util.Environment;
import org.openjdk.jmc.ui.common.util.Environment.OSType;

public final class FontAndColors {
	private static Color bgcolor = createDefaultBackgroundColor();
	private static Color fgcolor = createDefaultForegroundColor();
	private static Color middlecolor = createDefaultMiddleColor();
	private static Font defaultFont = createDefaultFont();

	private static Font createDefaultFont() {
		int fontSize;
		String fontName;
		try {
			Font currFont = UIManager.getFont("OptionPane.font"); //$NON-NLS-1$
			fontSize = currFont.getSize() * 5 / 6;
			fontName = currFont.getName();
		} catch (Exception e) {
			// Couldn't get a system font, use the old default instead
			fontName = "SansSerif"; //$NON-NLS-1$
			fontSize = 12;
			GreyChartPanel.LOGGER.log(Level.WARNING, "Could not find option pane font, will proceed with default.", e); //$NON-NLS-1$
		}
		/*
		 * Fix problem with boxes on Japanese/Chinese fonts. The new font now should be a
		 * CompositeFont if the original font was. See
		 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6289072
		 */
		return extendFontForUsedLocale(StyleContext.getDefaultStyleContext().getFont(fontName, Font.PLAIN, fontSize));
	}

	private static Font extendFontForUsedLocale(Font font) {
		Locale used = Locale.getDefault();
		if (used.getLanguage().equals(Locale.JAPANESE.getLanguage())) {
			return extendFontForLocale(font, "\u30E1\u30E2\u30EA\u30FC"); //$NON-NLS-1$
		} else if (used.equals(Locale.SIMPLIFIED_CHINESE)) {
			return extendFontForLocale(font, "\u5185\u5B58"); //$NON-NLS-1$
		}
		return font;
	}

	private static Font extendFontForLocale(Font font, String string) {
		if (font.canDisplayUpTo(string) == -1) {
			return font;
		}
		for (Font f : GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()) {
			if (f.canDisplayUpTo(string) == -1) {
				return f.deriveFont(font.getStyle(), font.getSize());
			}
		}
		return font;
	}

	private static Color createDefaultForegroundColor() {
		Color fgcolor;

		try {
			fgcolor = UIManager.getColor("windowText"); //$NON-NLS-1$
		} catch (Exception e) {
			// Couldn't get a system color, use the old default instead
			fgcolor = Color.BLACK;
			GreyChartPanel.LOGGER.log(Level.WARNING, "Could not find windowText color, will proceed with default.", e); //$NON-NLS-1$
		}
		return fgcolor;
	}

	private static Color createDefaultBackgroundColor() {
		Color bgcolor;
		try {
			if (isMac() || isLinux()) {
				bgcolor = Color.WHITE;
				GreyChartPanel.LOGGER.log(Level.INFO,
						"Is running on Mac/Linux - explicitly setting background to White in GreyChart"); //$NON-NLS-1$
			} else {
				bgcolor = UIManager.getColor("window"); //$NON-NLS-1$
			}
		} catch (Exception e) {
			// Couldn't get a system color, use the old default instead
			bgcolor = Color.WHITE;
			GreyChartPanel.LOGGER.log(Level.WARNING, "Could not find window color, will proceed with default.", e); //$NON-NLS-1$
		}
		return bgcolor;
	}

	private static boolean isMac() {
		return Environment.getOSType() == OSType.MAC;
	}

	private static boolean isLinux() {
		return Environment.getOSType() == OSType.LINUX;
	}

	private static Color createDefaultMiddleColor() {
		int red = (fgcolor.getRed() + bgcolor.getRed()) / 2;
		int green = (fgcolor.getGreen() + bgcolor.getGreen()) / 2;
		int blue = (fgcolor.getBlue() + bgcolor.getBlue()) / 2;
		return new Color(red, green, blue);
	}

	public static Color getDefaultBackground() {
		return bgcolor;
	}

	public static Color getDefaultForeground() {
		return fgcolor;
	}

	public static Color getDefaultMiddleColor() {
		return middlecolor;
	}

	public static Font getDefaultFont() {
		return defaultFont;
	}
}
