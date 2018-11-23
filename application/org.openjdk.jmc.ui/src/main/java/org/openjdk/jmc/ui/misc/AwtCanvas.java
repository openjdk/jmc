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

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.widgets.Display;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.common.util.Environment;
import org.openjdk.jmc.ui.preferences.PreferenceConstants;

public class AwtCanvas {
	private ImageData imageDataSWT;
	private BufferedImage imageAWT;

	private static final double X_SCALE = (Display.getCurrent().getDPI().x) / Environment.getNormalDPI();
	private static final double Y_SCALE = (Display.getCurrent().getDPI().y) / Environment.getNormalDPI();

	public boolean hasImage(int width, int height) {
		return (imageDataSWT != null) && (imageDataSWT.width == width) && (imageDataSWT.height == height);
	}

	public Graphics2D getGraphics(int width, int height) {
		if ((imageDataSWT == null) || (imageDataSWT.width != width) || (imageDataSWT.height != height)) {
			imageAWT = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
			PaletteData vpPalette = new PaletteData(0xff, 0xff00, 0xff0000);
			int scanlinePad = width * 3;
			byte[] byteData = ((DataBufferByte) imageAWT.getRaster().getDataBuffer()).getData();
			imageDataSWT = new ImageData(width, height, 24, vpPalette, scanlinePad, byteData);
			Graphics2D graphicsAWT = imageAWT.createGraphics();
			setAntiAliasing(graphicsAWT);
			graphicsAWT.setFont(new Font("OptionPane.font", Font.PLAIN, 12));
			fixDPI(graphicsAWT);
			return graphicsAWT;
		} else {
			Graphics2D graphicsAWT = imageAWT.createGraphics();
			setAntiAliasing(graphicsAWT);
			graphicsAWT.setFont(new Font("OptionPane.font", Font.PLAIN, 12));
			graphicsAWT.clearRect(0, 0, width, height);
			fixDPI(graphicsAWT);
			return graphicsAWT;
		}
	}
	
	private void setAntiAliasing(Graphics2D ctx) {
		Boolean antiAliasing = UIPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.P_ANTI_ALIASING);
		if (antiAliasing) {
			ctx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		} else {
			ctx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		}
	}

	private void fixDPI(Graphics2D ctx) {
		AffineTransform defaultTransform = ctx.getDeviceConfiguration().getDefaultTransform();
		defaultTransform.scale(X_SCALE, Y_SCALE);
		ctx.setTransform(defaultTransform);
	}

	public void paint(PaintEvent e, int x, int y) {
		try {
			Image img = new Image(e.display, imageDataSWT);
			e.gc.drawImage(img, x, y);
			img.dispose();
		} catch (ArrayIndexOutOfBoundsException ex) {
			// Workaround for image construction bug
		}
	}
}
