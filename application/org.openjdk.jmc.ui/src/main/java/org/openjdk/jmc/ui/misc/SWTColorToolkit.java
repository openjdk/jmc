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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import org.openjdk.jmc.common.util.ColorToolkit;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.UIPlugin.ImageRegistryPrefixes;

public class SWTColorToolkit {

	private static final int THUMBNAIL_SIZE = 16;
	private static final RGB BG_COLOR = new RGB(255, 255, 255);
	private static final RGB BORDER_COLOR = new RGB(0, 0, 0);

	private SWTColorToolkit() {
		throw new UnsupportedOperationException("Do not instantiate toolkit"); //$NON-NLS-1$
	}

	public static java.awt.Color asAwtColor(RGB rgb) {
		return new java.awt.Color(rgb.red, rgb.green, rgb.blue);
	}

	public static RGB asRGB(java.awt.Color color) {
		if (color.getAlpha() < 255) {
			// FIXME: Take the actual background color into account?
			color = ColorToolkit.blend(color, java.awt.Color.WHITE);
		}
		return new RGB(color.getRed(), color.getGreen(), color.getBlue());
	}

	public static ImageDescriptor getColorThumbnailDescriptor(RGB color) {
		ImageRegistry ir = UIPlugin.getDefault().getImageRegistry();
		String id = getColorKey(color);
		ImageDescriptor desc = ir.getDescriptor(id);
		if (desc == null) {
			Image i = createColoredSquare(Display.getDefault(), THUMBNAIL_SIZE, color);
			ir.put(id, i);
		}
		// Descriptor will be created by ImageRegistry.put(String, Image)
		return ir.getDescriptor(id);
	}

	public static Image getColorThumbnail(java.awt.Color color) {
		return getColorThumbnail(asRGB(color));
	}

	public static Image getColorThumbnail(RGB color) {
		ImageRegistry ir = UIPlugin.getDefault().getImageRegistry();
		String id = getColorKey(color);
		Image i = ir.get(id);
		if (i == null) {
			i = createColoredSquare(Display.getDefault(), THUMBNAIL_SIZE, color);
			ir.put(id, i);
		}
		return i;
	}

	private static String getColorKey(RGB color) {
		return ImageRegistryPrefixes.COLORED_SQUARE.name() + color;
	}

	private static Image createColoredSquare(Display disp, int size, RGB color) {
		Image i = new Image(disp, size, size);
		GC gc = new GC(i);
		gc.setBackground(getColor(BG_COLOR));
		gc.fillRectangle(0, 0, size - 1, size - 1);
		gc.setBackground(getColor(color));
		gc.fillRectangle(1, 1, size - 3, size - 3);
		gc.setForeground(getColor(BORDER_COLOR));
		gc.drawRectangle(1, 1, size - 3, size - 3);
		gc.dispose();
		return i;
	}

	public static ImageDescriptor createGradientThumbnail(RGB start, RGB end, boolean vertical) {
		// FIXME: Could potentially do the same as for ColoredSquare and save this in the ImageRegistry
		int size = THUMBNAIL_SIZE;
		Image i = new Image(Display.getDefault(), size, size);
		GC gc = new GC(i);
		gc.setBackground(getColor(BG_COLOR));
		gc.fillRectangle(0, 0, size - 1, size - 1);
		gc.setForeground(getColor(start));
		gc.setBackground(getColor(end));
		gc.fillGradientRectangle(1, 1, size - 3, size - 3, vertical);
		gc.setForeground(getColor(BORDER_COLOR));
		gc.drawRectangle(1, 1, size - 3, size - 3);
		gc.dispose();
		ImageDescriptor id = ImageDescriptor.createFromImageData(i.getImageData());
		i.dispose();
		return id;
	}

	public static Color getColor(RGB rgb) {
		String key = rgb.toString();
		Color color = JFaceResources.getColorRegistry().get(key);
		if (color == null) {
			JFaceResources.getColorRegistry().put(key, rgb);
			color = JFaceResources.getColorRegistry().get(key);
		}
		return color;

	}
}
