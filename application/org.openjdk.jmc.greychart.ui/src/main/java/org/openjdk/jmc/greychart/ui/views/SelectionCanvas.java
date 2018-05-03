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
package org.openjdk.jmc.greychart.ui.views;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.openjdk.jmc.ui.common.util.Environment;

/**
 * Canvas typcially used for a grey chart that you want to make a selection on
 */
public abstract class SelectionCanvas extends Canvas {
	private int selectionStart = -1;
	private int selectionEnd = -1;
	private Rectangle selectable;

	// Cache
	private ImageData m_imageDataSWT;
	private BufferedImage m_imageAWT;
	private Graphics2D m_graphicsAWT;
	private int lastDesiredWidth;
	protected double xScale = Display.getDefault().getDPI().x / Environment.getNormalDPI();
	protected double yScale = Display.getDefault().getDPI().y / Environment.getNormalDPI();

	public SelectionCanvas(Composite parent) {
		super(parent, SWT.NO_BACKGROUND);
		addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				paint(e.gc);
			}
		});
		ChartSelectionListener chs = new ChartSelectionListener();
		addMouseMoveListener(chs);
		addMouseListener(chs);
	}

	public void paint(GC gc) {
		org.eclipse.swt.graphics.Rectangle rect = getClientArea();
		if ((m_imageDataSWT == null) || (lastDesiredWidth != rect.width) || (m_imageDataSWT.height != rect.height)) {
			lastDesiredWidth = rect.width;
			/*
			 * We adjust the width to prevent SWT from performing expensive conversions due to
			 * scan-line padding lengths.
			 */
			int width = rect.width;
			// This calculation should be explained.
			while ((((((width * 24 + 7) / 8) + 2) / 3 * 3) % 4) != 0) {
				width++;
			}
			m_imageAWT = new BufferedImage(width, rect.height, BufferedImage.TYPE_3BYTE_BGR);
			PaletteData vpPalette = new PaletteData(0xff, 0xff00, 0xff0000);
			int scanlinePad = 3;
			byte[] byteData = ((DataBufferByte) m_imageAWT.getRaster().getDataBuffer()).getData();
			m_graphicsAWT = m_imageAWT.createGraphics();
			AffineTransform defaultTransform = m_graphicsAWT.getDeviceConfiguration().getDefaultTransform();
			defaultTransform.scale(xScale, yScale);
			m_graphicsAWT.setTransform(defaultTransform);
			m_imageDataSWT = new ImageData(width, rect.height, 24, vpPalette, scanlinePad, byteData);
		}
		int adjustedWidth = (int) Math.round(rect.width / xScale);
		int adjustedHeight = (int) Math.round(rect.height / yScale);
		selectable = render(m_graphicsAWT, new Rectangle(adjustedWidth, adjustedHeight));
		if (selectable != null && selectionStart >= 0 && selectionEnd >= 0) {
			m_graphicsAWT.setColor(Color.WHITE);
			m_graphicsAWT.setXORMode(Color.BLACK);
			int drawSelFrom = Math.max(selectable.x, Math.min(selectionStart, selectionEnd));
			int drawSelTo = Math.min(selectable.x + selectable.width, Math.max(selectionStart, selectionEnd));
			int selectionWidth = Math.max(0, drawSelTo - drawSelFrom);
			m_graphicsAWT.fillRect(drawSelFrom, selectable.y, selectionWidth, selectable.height);
			m_graphicsAWT.setPaintMode();
		}
		Image img = new Image(gc.getDevice(), m_imageDataSWT);
		gc.drawImage(img, rect.x, rect.y);
		img.dispose();
	}

	/**
	 * Translates display coordinates into image coordinates for the chart.
	 *
	 * @param x
	 *            the provided display x coordinate
	 * @param y
	 *            the provided display y coordinate
	 * @return a Point that represents the (x,y) coordinates in the chart's coordinate space
	 */
	protected Point translateDisplayToImageCoordinates(int x, int y) {
		int xImage = (int) Math.round(x / xScale);
		int yImage = (int) Math.round(y / yScale);
		return new Point(xImage, yImage);
	}

	/**
	 * Translates a display x coordinate into an image x coordinate for the chart.
	 *
	 * @param x
	 *            the provided display x coordinate
	 * @return the x value translated for use in the image
	 */
	protected int translateDisplayToImageXCoordinate(int x) {
		return (int) Math.round(x / xScale);
	}

	protected abstract Rectangle render(Graphics2D ctx, Rectangle where);

	protected abstract void selectionStart();

	protected abstract void selectionComplete(double start, double end);

	private class ChartSelectionListener extends MouseAdapter implements MouseMoveListener {

		@Override
		public void mouseDown(MouseEvent e) {
			/*
			 * On Mac OS X, CTRL + left mouse button can be used to trigger a context menu. (This is
			 * for historical reasons when the primary input device on Macs were a mouse with a
			 * single physical button. All modern Macs have other means to bring up the context
			 * menu, typically a two finger tap.)
			 * 
			 * Although I think it would be best to check that this MouseEvent does not cause a
			 * platform specific popup trigger, like java.awt.event.MouseEvent.isPopupTrigger() for
			 * AWT, SWT doesn't seem to have something as simple. It has the MenuDetectEvent, but
			 * the order in relation to this MouseEvent is unspecified.
			 * 
			 * The code below instead relies on ignoring mouse down events when SWT.MOD4 is
			 * depressed. Since MOD4 is CTRL on OS X and 0 on all other current platforms, this
			 * suffices. Except for an additional platform check, this approach is also used in
			 * org.eclipse.swt.custom.StyledText.handleMouseDown(Event).
			 */
			if ((e.button == 1) && ((e.stateMask & SWT.MOD4) == 0) && selectable != null) {
				selectionStart = translateDisplayToImageXCoordinate(e.x);
				selectionStart();
			}
		}

		@Override
		public void mouseMove(MouseEvent e) {
			if (selectionStart >= 0) {
				selectionEnd = translateDisplayToImageXCoordinate(e.x);
				redraw();
			}
		}

		@Override
		public void mouseUp(MouseEvent e) {
			if (selectionStart >= 0 && (e.button == 1)) {
				int x = translateDisplayToImageXCoordinate(e.x);
				int minLimit = Math.max(0, Math.min(selectionStart, x) - selectable.x);
				int maxLimit = Math.min(selectable.width, Math.max(selectionStart, x) - selectable.x);
				selectionStart = -1;
				selectionEnd = -1;
				if (maxLimit > minLimit + 2) {
					selectionComplete(minLimit / (double) selectable.width, maxLimit / (double) selectable.width);
				}

			}
		}
	}
}
