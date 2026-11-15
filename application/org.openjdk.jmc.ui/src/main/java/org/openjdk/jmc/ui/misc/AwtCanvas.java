/*
 * Copyright (c) 2018, 2026, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.ui.misc;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageDataProvider;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.openjdk.jmc.common.util.Environment;
import org.openjdk.jmc.common.util.Environment.OSType;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.preferences.PreferenceConstants;

/**
 * An off-screen AWT image painted onto an SWT control. Sizes are in SWT points; the image is
 * allocated with the pixels the control covers on screen, see {@link #getPixelScale(Control)}.
 */
public class AwtCanvas {
	/**
	 * The SWT points to paint beyond the size of a control to cover it. On Windows SWT rounds the
	 * opposite edges of a control separately, which can make the control a pixel larger than its
	 * scaled size. Images use {@link #getMargin(Control)}.
	 */
	public static final int MARGIN = Environment.getOSType() == OSType.WINDOWS ? 1 : 0;

	private final Control control;
	private final int margin;
	private ImageData imageDataSWT;
	private BufferedImage imageAWT;
	private Image imageSWT;
	private int width = -1;
	private int height = -1;
	private double pixelScale = 1;

	private static final double X_SCALE = (Display.getCurrent().getDPI().x) / Environment.getNormalDPI();
	private static final double Y_SCALE = (Display.getCurrent().getDPI().y) / Environment.getNormalDPI();

	/**
	 * Creates a canvas rendering one pixel for each SWT point.
	 */
	public AwtCanvas() {
		this(null);
	}

	/**
	 * Creates a canvas rendering at the resolution of the monitor showing the control's window.
	 *
	 * @param control
	 *            the control to paint on
	 */
	public AwtCanvas(Control control) {
		this.control = control;
		margin = getMargin(control);
		if (control != null) {
			control.addDisposeListener(e -> disposeImage());
		}
	}

	/**
	 * @param control
	 *            the control to paint on
	 * @return the SWT points by which an image for the control exceeds it, 0 for a mirrored
	 *         control: its paint GC aligns the opposite edge of the image and would cut off the
	 *         first columns of a wider image
	 */
	public static int getMargin(Control control) {
		return control != null && (control.getStyle() & SWT.MIRRORED) == 0 ? MARGIN : 0;
	}

	/**
	 * @param control
	 *            the control to get the scale for
	 * @return the number of screen pixels for each SWT point, at least 1.0
	 */
	public static double getPixelScale(Control control) {
		if (control == null || control.isDisposed()) {
			return 1;
		}
		// The bounds of a scrolled control may extend onto another monitor, so ask the shell
		double scale = control.getShell().getMonitor().getZoom() / 100d;
		if (Environment.getOSType() == OSType.WINDOWS) {
			/*
			 * SWT may lay out controls at another zoom than the monitor (swt.autoScale) and report
			 * the difference as DPI, which the charts already scale by.
			 */
			scale = scale / (control.getDisplay().getDPI().x / Environment.getNormalDPI());
			// The DPI is an integer, remove the rounding error
			double quarter = Math.round(scale * 4) / 4d;
			if (Math.abs(scale - quarter) < 0.03) {
				scale = quarter;
			}
		}
		return Math.max(1, scale);
	}

	/**
	 * @param points
	 *            a length in SWT points
	 * @param pixelScale
	 *            the scale from {@link #getPixelScale(Control)}
	 * @return the length in pixels, at least 1
	 */
	public static int toPixels(int points, double pixelScale) {
		return Math.max(1, (int) Math.round(points * pixelScale));
	}

	/**
	 * Creates a BGR pixel buffer with four-byte aligned rows, so that its byte array can be shared
	 * with a 24-bit SWT {@link ImageData} using a scanline pad of 4 without a row conversion.
	 *
	 * @param width
	 *            the image width in pixels
	 * @param height
	 *            the image height in pixels
	 * @return the image with an aligned {@link DataBufferByte}
	 */
	public static BufferedImage createBufferedImage(int width, int height) {
		int scanlineStride = (width * 3 + 3) / 4 * 4;
		byte[] pixels = new byte[scanlineStride * height];
		DataBufferByte dataBuffer = new DataBufferByte(pixels, pixels.length);
		WritableRaster raster = Raster.createInterleavedRaster(dataBuffer, width, height, scanlineStride, 3,
				new int[] {2, 1, 0}, null);
		ComponentColorModel colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), false,
				false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
		return new BufferedImage(colorModel, raster, false, null);
	}

	/**
	 * Creates an SWT image with the given size in points from an already rendered pixel buffer.
	 * Supplying the data at its zoom avoids SWT enlarging it again as a 100% image. The data must
	 * remain unchanged until the returned image is disposed.
	 * <p>
	 * SWT also requires a 100% representation and may request it eagerly during construction. The
	 * rendered buffer is reused directly whenever its dimensions match the requested zoom. A 100%
	 * buffer is left for SWT to scale to other zooms, as it would a plain image.
	 *
	 * @param device
	 *            the device to create the image on
	 * @param data
	 *            the rendered pixels
	 * @param width
	 *            the image width in SWT points
	 * @param height
	 *            the image height in SWT points
	 * @return an image whose source coordinates are in SWT points
	 */
	public static Image createImage(Device device, ImageData data, int width, int height) {
		ImageDataProvider provider = zoom -> {
			int pixelWidth = toPixels(width, zoom / 100d);
			int pixelHeight = toPixels(height, zoom / 100d);
			if (data.width == pixelWidth && data.height == pixelHeight) {
				return data;
			}
			if (zoom != 100 && data.width == toPixels(width, 1) && data.height == toPixels(height, 1)) {
				// Let SWT scale a 100% buffer itself
				return null;
			}
			return data.scaledTo(pixelWidth, pixelHeight);
		};
		return new Image(device, provider);
	}

	/**
	 * Repeats the last pixel column and row of a rendering over the rest of its image.
	 *
	 * @param image
	 *            the image to extend
	 * @param width
	 *            the width of the rendering in pixels
	 * @param height
	 *            the height of the rendering in pixels
	 */
	public static void extendEdges(BufferedImage image, int width, int height) {
		WritableRaster raster = image.getRaster();
		int imageWidth = raster.getWidth();
		int imageHeight = raster.getHeight();
		int renderedWidth = Math.max(1, Math.min(width, imageWidth));
		int renderedHeight = Math.max(1, Math.min(height, imageHeight));
		if (renderedWidth < imageWidth) {
			Object column = raster.getDataElements(renderedWidth - 1, 0, 1, imageHeight, null);
			for (int x = renderedWidth; x < imageWidth; x++) {
				raster.setDataElements(x, 0, 1, imageHeight, column);
			}
		}
		if (renderedHeight < imageHeight) {
			Object row = raster.getDataElements(0, renderedHeight - 1, imageWidth, 1, null);
			for (int y = renderedHeight; y < imageHeight; y++) {
				raster.setDataElements(0, y, imageWidth, 1, row);
			}
		}
	}

	public boolean hasImage(int width, int height) {
		return (imageDataSWT != null) && (this.width == width) && (this.height == height)
				&& (pixelScale == getPixelScale(control));
	}

	public Graphics2D getGraphics(int width, int height) {
		return getGraphics(width, height, null);
	}

	/**
	 * Prepares the image for rendering and fills its entire pixel buffer with the background in one
	 * pass, including edge pixels affected by rounding at fractional scales.
	 *
	 * @param width
	 *            the canvas width in SWT points
	 * @param height
	 *            the canvas height in SWT points
	 * @param background
	 *            the opaque background color, or {@code null} to use the default clearing behavior
	 * @return the graphics context, with the DPI transform applied
	 */
	public Graphics2D getGraphics(int width, int height, Color background) {
		disposeImage();
		double currentScale = getPixelScale(control);
		boolean newImage = (imageDataSWT == null) || (this.width != width) || (this.height != height)
				|| (pixelScale != currentScale);
		if (newImage) {
			int pixelWidth = toPixels(width + margin, currentScale);
			int pixelHeight = toPixels(height + margin, currentScale);
			imageAWT = createBufferedImage(pixelWidth, pixelHeight);
			PaletteData vpPalette = new PaletteData(0xff, 0xff00, 0xff0000);
			int scanlinePad = 4;
			byte[] byteData = ((DataBufferByte) imageAWT.getRaster().getDataBuffer()).getData();
			imageDataSWT = new ImageData(pixelWidth, pixelHeight, 24, vpPalette, scanlinePad, byteData);
			this.width = width;
			this.height = height;
			pixelScale = currentScale;
		}
		Graphics2D graphicsAWT = imageAWT.createGraphics();
		setAntiAliasing(graphicsAWT);
		graphicsAWT.setFont(new Font("OptionPane.font", Font.PLAIN, 12)); //$NON-NLS-1$
		if (background != null) {
			graphicsAWT.setColor(background);
			graphicsAWT.fillRect(0, 0, imageAWT.getWidth(), imageAWT.getHeight());
		} else if (!newImage) {
			graphicsAWT.clearRect(0, 0, imageAWT.getWidth(), imageAWT.getHeight());
		}
		fixDPI(graphicsAWT);
		return graphicsAWT;
	}

	private void setAntiAliasing(Graphics2D ctx) {
		Boolean antiAliasing = UIPlugin.getDefault().getPreferenceStore()
				.getBoolean(PreferenceConstants.P_ANTI_ALIASING);
		if (antiAliasing) {
			ctx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		} else {
			ctx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		}
	}

	private void fixDPI(Graphics2D ctx) {
		AffineTransform defaultTransform = ctx.getDeviceConfiguration().getDefaultTransform();
		defaultTransform.scale(X_SCALE * pixelScale, Y_SCALE * pixelScale);
		ctx.setTransform(defaultTransform);
	}

	/**
	 * The image drawn exceeds the rendered size by the margin and relies on the GC being clipped to
	 * the control.
	 */
	public void paint(PaintEvent e, int x, int y) {
		Image image = null;
		try {
			image = imageSWT;
			if (image == null) {
				if (margin > 0) {
					extendEdges(imageAWT, toPixels(width, pixelScale), toPixels(height, pixelScale));
				}
				image = createImage(e.display, imageDataSWT, width + margin, height + margin);
				if (control != null) {
					imageSWT = image;
				}
			}
			e.gc.drawImage(image, x, y);
		} catch (ArrayIndexOutOfBoundsException ex) {
			// Workaround for image construction bug
		} finally {
			if (control == null && image != null) {
				image.dispose();
			}
		}
	}

	private void disposeImage() {
		if (imageSWT != null) {
			imageSWT.dispose();
			imageSWT = null;
		}
	}
}
