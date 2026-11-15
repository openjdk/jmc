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
package org.openjdk.jmc.flightrecorder.ui.common;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.QuantityRange;
import org.openjdk.jmc.ui.charts.SubdividedQuantityRange;
import org.openjdk.jmc.ui.misc.AwtCanvas;

// FIXME: rename class
public class JComponentNavigator extends Canvas {

	private final static int GRABBER_RADIUS_Y = 5;
	private final static int GRABBER_RADIUS_X = 3;

	private final FormToolkit toolkit;

	private int lastDragPosition = -1;

	private boolean showGrabbers = false;

	private Image coloredImage;
	private Image greyImage;
	// Size in SWT points, and pixels per point, of the images
	private int imageWidth;
	private int imageHeight;
	private double imagePixelScale = 1;
	private volatile boolean invalidateNavigator;

	private DragHandles dragHandles;
	private SubdividedQuantityRange axis;
	private IRange<IQuantity> restorableRange;
	private IRange<IQuantity> selectedRange;
	private int height;

	public JComponentNavigator(Composite parent, FormToolkit toolkit) {
		super(parent, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
		this.toolkit = toolkit;
		addListeners();
	}

	/**
	 * The maximum range the component navigator can be zoomed out
	 *
	 * @param minX
	 * @param maxX
	 */
	public void setNavigatorRange(IRange<IQuantity> range) {
		setAxis(range, getClientArea().width);
		doSetCurrentRange(range);
	}

	private void setAxis(IRange<IQuantity> range, int width) {
		// FIXME: Extract the needed functionality from SubdividedQuantityRange
		axis = new SubdividedQuantityRange(range.getStart(), range.getEnd(), width == 0 ? 100 : width, 1);
	}

	public void setCurrentRange(IRange<IQuantity> range) {
		doSetCurrentRange(range);
		rangeUpdated();
	}

	protected void doSetCurrentRange(IRange<IQuantity> range) {
		restorableRange = isAxis(range) ? (isAxis(selectedRange) ? restorableRange : selectedRange) : null;
		selectedRange = range;
		dragHandles = null;
	}

	private boolean isAxis(IRange<IQuantity> range) {
		return range != null && axis.getStart().compareTo(range.getStart()) == 0
				&& axis.getEnd().compareTo(range.getEnd()) == 0;
	}

	private void rangeUpdated() {
		redraw();
		onRangeChange();
	}

	protected void onRangeChange() {
	}

	protected void renderBackdrop(Graphics2D context, int width, int height) {
	}

	private void addListeners() {
		addMouseListener(new MouseListener() {

			@Override
			public void mouseUp(MouseEvent e) {
				if (dragHandles == null) {
					updateCursor(e.x, e.y);
				} else {
					DragHandles handles = dragHandles;
					if (dragHandles.left < 0) {
						handles = comapareAndCreateRange(getStartPosition(), dragHandles.right);
					} else if (dragHandles.right < 0) {
						handles = comapareAndCreateRange(dragHandles.left, getEndPosition());
					}
					IQuantity start = selectedRange.getStart();
					IQuantity end = selectedRange.getEnd();
					if (handles.left != getStartPosition()) {
						start = axis.getQuantityAtPixel(handles.left);
					}
					if (handles.right != getEndPosition()) {
						end = axis.getQuantityAtPixel(handles.right + 1);
					}
					if (start.compareTo(selectedRange.getStart()) != 0 || end.compareTo(selectedRange.getEnd()) != 0) {
						setCurrentRange(QuantityRange.createWithEnd(start, end));
					}
					dragHandles = null;
				}

			}

			@Override
			public void mouseDown(MouseEvent e) {
				// NOTE: Skip for CTRL+click on OS X
				if ((e.button != 1) || ((e.stateMask & SWT.MOD4) != 0)) {
					return;
				}
				int startPosition = getStartPosition();
				int endPosition = getEndPosition();
				if (hitTest(startPosition, e.x, e.y)) {
					dragHandles = new DragHandles(startPosition, -1);
				} else if (hitTest(endPosition, e.x, e.y)) {
					dragHandles = new DragHandles(-1, endPosition);
				} else if (e.x > startPosition && e.x < endPosition) {
					dragHandles = new DragHandles(startPosition, endPosition);
				}
				lastDragPosition = e.x;
			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				if (restorableRange == null) {
					setCurrentRange(axis);
				} else {
					setCurrentRange(restorableRange);
				}
			}
		});
		addMouseMoveListener(new MouseMoveListener() {

			@Override
			public void mouseMove(MouseEvent e) {
				if (dragHandles == null) {
					updateCursor(e.x, e.y);
				} else {
					dragHandles.move(e.x - lastDragPosition);
					lastDragPosition = e.x;
					redraw();
				}
			}
		});
		addMouseTrackListener(new MouseTrackAdapter() {

			@Override
			public void mouseExit(MouseEvent e) {
				setShowGrabbers(false);
			}

			@Override
			public void mouseEnter(MouseEvent e) {
				setShowGrabbers(true);
			}
		});
		addPaintListener(new PaintListener() {

			@Override
			public void paintControl(PaintEvent e) {
				Rectangle clientArea = getClientArea();
				if (isChartImagesInvalid(clientArea)) {
					redrawChartImages(e, clientArea);
				}
				DragHandles handles = dragHandles;
				if (handles == null) {
					handles = new DragHandles(getStartPosition(), getEndPosition());
				} else if (handles.left < 0) {
					handles = comapareAndCreateRange(getStartPosition(), handles.right);
				} else if (handles.right < 0) {
					handles = comapareAndCreateRange(handles.left, getEndPosition());
				}
				drawBackground(e.gc, clientArea, handles.left, handles.right);
				drawSlider(e.gc, clientArea, handles.left, handles.right);
			}
		});
		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				disposeResources();
			}
		});

		addControlListener(new ControlAdapter() {

			@Override
			public void controlResized(ControlEvent e) {
				Rectangle clientArea = getClientArea();
				if (axis != null) {
					setAxis(axis, clientArea.width);
				}
				height = clientArea.height;
			}
		});
	}

	private void updateCursor(int x, int y) {
		int startPosition = getStartPosition();
		int endPosition = getEndPosition();
		if (hitTest(startPosition, x, y) || hitTest(endPosition, x, y)) {
			setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_SIZEW));
		} else if (x > startPosition && x < endPosition) {
			setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_HAND));
		} else {
			setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_ARROW));
		}
	}

	public IRange<IQuantity> getCurrentRange() {
		return selectedRange;
	}

	public boolean isLimitingMin() {
		return selectedRange.getStart().compareTo(axis.getStart()) > 0;
	}

	public boolean isLimitingMax() {
		return selectedRange.getEnd().compareTo(axis.getEnd()) < 0;
	}

	private int getStartPosition() {
		return (int) axis.getPixel(selectedRange.getStart());
	}

	private int getEndPosition() {
		return (int) axis.getPixel(selectedRange.getEnd());
	}

	private boolean isChartImagesInvalid(Rectangle clientArea) {
		return invalidateNavigator || (coloredImage == null) || coloredImage.isDisposed()
				|| (imageWidth != clientArea.width) || (imageHeight != clientArea.height)
				|| (imagePixelScale != AwtCanvas.getPixelScale(this));
	}

	private void drawBackground(GC gc, Rectangle clientArea, int leftW, int winRight) {
		int margin = AwtCanvas.MARGIN;
		if (margin > 0) {
			// The control can be a pixel larger than the images drawn over this fill
			gc.fillRectangle(clientArea.x + clientArea.width - 1, clientArea.y, 1 + margin, clientArea.height + margin);
			gc.fillRectangle(clientArea.x, clientArea.y + clientArea.height - 1, clientArea.width, 1 + margin);
		}
		gc.drawImage(greyImage, clientArea.x, clientArea.y);
		drawImagePart(gc, coloredImage, leftW, winRight - leftW, clientArea);
	}

	/**
	 * Draws a vertical slice, given in SWT points, of an image.
	 */
	private void drawImagePart(GC gc, Image image, int x, int width, Rectangle clientArea) {
		x = Math.max(0, x);
		width = Math.min(width, imageWidth - x);
		if (width <= 0) {
			return;
		}
		// SWT on Windows misreads the clipping of an unclipped GC in a right-to-left paint
		boolean clipped = gc.isClipped();
		Region originalClipping = new Region(gc.getDevice());
		Region sliceClipping = new Region(gc.getDevice());
		try {
			sliceClipping.add(new Rectangle(clientArea.x + x, clientArea.y, width, clientArea.height));
			if (clipped) {
				gc.getClipping(originalClipping);
				sliceClipping.intersect(originalClipping);
			}
			gc.setClipping(sliceClipping);
			// Drawing the full image avoids source-rectangle resampling at fractional zoom levels
			gc.drawImage(image, clientArea.x, clientArea.y);
		} finally {
			gc.setClipping(clipped ? originalClipping : null);
			sliceClipping.dispose();
			originalClipping.dispose();
		}
	}

	private void drawSlider(GC gc, Rectangle clientArea, int windowLeft, int windowRight) {
		int top = 1;
		int bottom = clientArea.height - 2;

		gc.setForeground(toolkit.getColors().getBorderColor());
		gc.drawRectangle(0, 0, clientArea.width - 1, clientArea.height - 1);
		gc.setForeground(toolkit.getColors().getForeground());

		drawEdge(gc, windowLeft, bottom, top - bottom);
		int oldAlpha = gc.getAlpha();
		gc.setAlpha(128);
		gc.drawLine(windowLeft, top, windowRight, top);
		gc.drawLine(windowLeft, bottom, windowRight, bottom);
		gc.setAlpha(oldAlpha);
		drawEdge(gc, windowRight, bottom, top - bottom);
	}

	// FIXME: need to clean up this mess
	private void redrawChartImages(PaintEvent e, Rectangle rect) {
		if (coloredImage != null && !coloredImage.isDisposed()) {
			coloredImage.dispose();
		}
		if (greyImage != null && !greyImage.isDisposed()) {
			greyImage.dispose();
		}

		// The client area is in SWT points, render with the pixels it covers on screen
		imagePixelScale = AwtCanvas.getPixelScale(this);
		imageWidth = rect.width;
		imageHeight = rect.height;
		int pixelWidth = AwtCanvas.toPixels(rect.width, imagePixelScale);
		int pixelHeight = AwtCanvas.toPixels(rect.height, imagePixelScale);
		BufferedImage imageAWT = AwtCanvas.createBufferedImage(pixelWidth, pixelHeight);
		PaletteData vpPalette = new PaletteData(0xff, 0xff00, 0xff0000);
		byte[] byteData = ((DataBufferByte) imageAWT.getRaster().getDataBuffer()).getData();
		Graphics2D graphicsAWT = imageAWT.createGraphics();
		ImageData coloredImageData = new ImageData(pixelWidth, pixelHeight, 24, vpPalette, 4, byteData);
		try {
			graphicsAWT.scale(imagePixelScale, imagePixelScale);
			// Don't try to draw on the first and last pixel of the client area, BUG# 9312855
			graphicsAWT.translate(1, 1);
			renderBackdrop(graphicsAWT, rect.width - 2, rect.height - 2);
		} finally {
			graphicsAWT.dispose();
		}
		coloredImage = AwtCanvas.createImage(e.display, coloredImageData, imageWidth, imageHeight);
		ImageData greyImageData = (ImageData) coloredImageData.clone();
		// Convert BGR pixels, leaving the scanline padding unchanged
		byte[] greyPixels = greyImageData.data;
		for (int y = 0; y < pixelHeight; y++) {
			int rowStart = y * greyImageData.bytesPerLine;
			int rowEnd = rowStart + pixelWidth * 3;
			for (int offset = rowStart; offset < rowEnd; offset += 3) {
				byte value = (byte) (192 + .20 * (0.3 * (greyPixels[offset + 2] & 0xff)
						+ 0.59 * (greyPixels[offset + 1] & 0xff) + 0.11 * (greyPixels[offset] & 0xff)));
				greyPixels[offset] = value;
				greyPixels[offset + 1] = value;
				greyPixels[offset + 2] = value;
			}
		}
		greyImage = AwtCanvas.createImage(e.display, greyImageData, imageWidth, imageHeight);
		invalidateNavigator = false;
	}

	private void drawEdge(GC gc, int x, int y, int height) {
		if (getShowGrabbers()) {
			final int grabber_bottom = y + height / 2 + GRABBER_RADIUS_Y;
			final int grabber_top = y + height / 2 - GRABBER_RADIUS_Y;
			gc.drawLine(x, y, x, grabber_bottom);
			gc.fillRoundRectangle(x - GRABBER_RADIUS_X, grabber_top, GRABBER_RADIUS_X * 2, GRABBER_RADIUS_Y * 2,
					GRABBER_RADIUS_X - 1, GRABBER_RADIUS_Y);
			gc.drawRoundRectangle(x - GRABBER_RADIUS_X, grabber_top, GRABBER_RADIUS_X * 2, GRABBER_RADIUS_Y * 2,
					GRABBER_RADIUS_X - 1, GRABBER_RADIUS_Y);
			gc.drawLine(x, grabber_top, x, y + height);

			gc.drawLine(x - 1, grabber_top + 3, x - 1, grabber_bottom - 3);
			gc.drawLine(x + 1, grabber_top + 3, x + 1, grabber_bottom - 3);
		} else {
			gc.drawLine(x, y, x, y + height);
		}
	}

	public void invalidateNavigator(boolean invalidateNavigator) {
		this.invalidateNavigator = invalidateNavigator;
	}

	private boolean getShowGrabbers() {
		return showGrabbers;
	}

	private void setShowGrabbers(boolean showGrabbers) {
		this.showGrabbers = showGrabbers;
		redraw();
	}

	private void disposeResources() {
		if (coloredImage != null && !coloredImage.isDisposed()) {
			coloredImage.dispose();
		}
		if (greyImage != null && !greyImage.isDisposed()) {
			greyImage.dispose();
		}
	}

	private class DragHandles {
		int left;
		int right;

		DragHandles(int left, int right) {
			this.left = left;
			this.right = right;
		}

		void move(int offset) {
			if (left >= 0) {
				left = getNewPos(left, offset);
			}
			if (right >= 0) {
				right = getNewPos(right, offset);
			}
		}

		int getNewPos(int pos, int offset) {
			return Math.min(axis.getPixelExtent() - 1, Math.max(0, pos + offset));
		}

	}

	private DragHandles comapareAndCreateRange(int a, int b) {
		return a < b ? new DragHandles(a, b) : new DragHandles(b, a);
	}

	private boolean hitTest(int position, int x, int y) {
		return Math.abs(x - position) <= GRABBER_RADIUS_X && Math.abs(y - height / 2) <= GRABBER_RADIUS_Y;
	}

}
