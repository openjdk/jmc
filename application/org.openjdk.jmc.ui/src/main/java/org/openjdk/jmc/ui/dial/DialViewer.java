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

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormColors;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.accessibility.AccessibilityConstants;
import org.openjdk.jmc.ui.accessibility.FocusTracker;
import org.openjdk.jmc.ui.accessibility.MCAccessibleListener;
import org.openjdk.jmc.ui.common.util.Environment;
import org.openjdk.jmc.ui.common.util.Environment.OSType;
import org.openjdk.jmc.ui.misc.IRefreshable;
import org.openjdk.jmc.ui.misc.SWTColorToolkit;

/**
 * Widget for drawing a dial. All methods must be called from the ui-thread.
 */
// FIXME: This class definitely needs to be cleaned up.
public class DialViewer extends Composite implements IRefreshable {
	private static final int TITLE_VERTICAL_PADDING = 3;
	private static final String DRAW_PROPERTY = "org.openjdk.jmc.ui.dial.immediatedraw"; //$NON-NLS-1$
	private static final boolean IMMEDIATE_DRAWING;

	static {
		// Workaround for console dials causing high CPU load on Mac
		boolean drawImmediately;
		if (System.getProperty(DRAW_PROPERTY) != null) {
			drawImmediately = Boolean.getBoolean(DRAW_PROPERTY);
		} else {
			// Enable on OS X due to https://bugs.eclipse.org/bugs/show_bug.cgi?id=410293
			drawImmediately = (Environment.getOSType() == OSType.MAC);
		}
		IMMEDIATE_DRAWING = drawImmediately;
	}
	// Model
	private IDialProvider[] m_dials = new IDialProvider[] {};
	final private Map<String, Object> m_inputs = new LinkedHashMap<>();

	// listeners
	final private MCAccessibleListener m_accessibleListener;
	// Dial properties
	private IQuantity m_gradientBegin;
	private IQuantity m_gradientEnd;
	private RGB m_gradientBeginColor;
	private RGB m_gradientEndColor;

	// Caching
	private DialDevice m_lastDialDevice;
	private Image m_lastBackGroundImage;
	private int m_lastWidth = 0;
	private int m_lastHeight = 0;
	private String m_title;

	private IUnit m_valueUnit;

	/**
	 * Constructs a {@link DialViewer} with a given configuration on the parent composite.
	 *
	 * @param parent
	 * @param style
	 * @param configuration
	 */
	public DialViewer(Composite parent, int style) {
		super(parent, SWT.DOUBLE_BUFFERED);

		m_accessibleListener = new MCAccessibleListener();
		m_accessibleListener.setComponentType(AccessibilityConstants.COMPONENT_TYPE_DIAL);
		getAccessible().addAccessibleListener(m_accessibleListener);
		addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				if (!getClientArea().isEmpty()) {
					draw(e.gc, 0, 0, getClientArea().width, getClientArea().height, false);
				}
			}
		});
		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				setCachedBackgroundImage(null);
			}
		});
	}

	private void setCachedBackgroundImage(Image image) {
		if (m_lastBackGroundImage != null && !m_lastBackGroundImage.isDisposed()) {
			m_lastBackGroundImage.dispose();
		}
		m_lastBackGroundImage = image;
	}

	/**
	 * Sets the title for this {@link DialViewer}
	 *
	 * @param title
	 *            the title, can't be null.
	 */
	public void setTitle(String title) {
		if (!isDisposed()) {
			m_accessibleListener.setName(title);
			m_title = title;
			redraw();
			/*
			 * Explicit calls to update() should be avoided unless absolutely necessary. They may
			 * have a negative performance impact and may cause issues on Mac OS X Cocoa (SWT 3.6).
			 * If it is required here, there must be a justifying comment.
			 */
			// update();
		}
	}

	/**
	 * Returns the title for this {@link DialViewer}
	 *
	 * @return the title
	 */
	public String getText() {
		return m_title;
	}

	public void setUnit(IUnit unit) {
		m_valueUnit = unit;
		setCachedBackgroundImage(null);
		m_lastWidth = -1;
		m_lastHeight = -1;
	}

	public IUnit getUnit() {
		return m_valueUnit;
	}

	/**
	 * Returns a {@link Point} in device coordinates from a normalized value in dial where the
	 * minimum value equals 0.0 and the maximum value equals 1.0.
	 *
	 * @param radius
	 *            the radius position
	 * @param normalizedValue
	 *            the normalized value
	 * @return a device coordinate
	 */
	private Point normalizedToDevice(
		ImageDescription config, int xOffset, int yOffset, double radius, double normalizedValue) {
		double radians = config.imageFunction.toRadians(normalizedValue);

		xOffset += config.origin.x + (int) Math.round(radius * Math.cos(radians));
		yOffset += config.origin.y - (int) Math.round(radius * Math.sin(radians));

		return new Point(xOffset, yOffset);
	}

	/**
	 * Sets the range for the gradient
	 *
	 * @param start
	 * @param end
	 */
	public void setGradientRange(
		IQuantity gradientBegin, IQuantity gradientEnd, RGB gradientBeginColor, RGB gradientEndColor) {
		m_gradientBegin = gradientBegin;
		m_gradientEnd = gradientEnd;
		m_gradientBeginColor = gradientBeginColor;
		m_gradientEndColor = gradientEndColor;
		setCachedBackgroundImage(null);
	}

	public void setProviders(IDialProvider ... providers) {
		m_dials = providers;
		refresh();
	}

	/**
	 * Refreshed the {@link DialViewer} with new data from the {@link IDialProvider}s
	 */
	@Override
	public boolean refresh() {
		if (!isDisposed()) {
			if (IMMEDIATE_DRAWING) {
				if (isVisible()) {
					Rectangle area = getClientArea();
					if (!area.isEmpty()) {
						GC gc = new GC(this);
						draw(gc, 0, 0, area.width, area.height, true);
						// Must redraw focus in the same way as FocusTracker
						if (isFocusControl()) {
							FocusTracker.drawFocusOn(this, gc);
						}
						gc.dispose();
					}
				}
			} else {
				redraw();
			}
			/*
			 * Explicit calls to update() should be avoided unless absolutely necessary. They may
			 * have a negative performance impact and may cause issues on Mac OS X Cocoa (SWT 3.6).
			 * If it is required here, there must be a justifying comment.
			 */
			// update();
			return true;
		}
		return false;
	}

	public boolean setInput(String identifier, Object input) {
		Object oldInput = getInput(identifier);
		if (oldInput != input) {
			m_inputs.put(identifier, input);
			return true;
		}
		return false;
	}

	private void drawGradientBackground(GC gc, DialDevice dialDevice, RGB startRGB, RGB endRGB) {
		double nA = dialDevice.normalizeForDevice(m_gradientBegin);
		double nB = dialDevice.normalizeForDevice(m_gradientEnd);
		Color oldColor = gc.getBackground();
		for (double n = 1.0; n >= 0.0; n -= 0.01) {
			Color color = createColor(gc.getDevice(), startRGB, endRGB, n, Math.min(nA, nB), Math.max(nA, nB));
			gc.setBackground(color);
			fillArc(gc, dialDevice.getBackground(), n);
			color.dispose();
		}
		gc.setBackground(oldColor);
	}

	private Color createColor(Device device, RGB startRGB, RGB endRGB, double n, double start, double end) {
		if (n < start) {
			return new Color(device, startRGB);
		}
		if (n > end) {
			return new Color(device, endRGB);
		}
		return new Color(device, blend(startRGB, endRGB, normalize(start, end, n)));
	}

	private void fillArc(GC gc, ImageDescription config, double n) {
		int r = (int) Math.round(config.gradientRadius);
		int lA = (int) Math.round(Math.toDegrees(config.imageFunction.toRadians(0.0)));
		int rA = (int) Math.round(Math.toDegrees(config.imageFunction.toRadians(n)));

		gc.fillArc(config.origin.x - r, config.origin.y - r, 2 * r, 2 * r, rA, lA - rA);
	}

	private static double normalize(double start, double end, double value) {
		double total = end - start;
		return total != 0 ? (value - start) / total : 0.0;
	}

	private static RGB blend(RGB a, RGB b, double p) {
		return new RGB(interpolateRGB(a.red, b.red, p), interpolateRGB(a.green, b.green, p),
				interpolateRGB(a.blue, b.blue, p));
	}

	private static int interpolateRGB(int startValue, int endValue, double percentage) {
		return (int) (startValue + Math.round(percentage * (endValue - startValue)));
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		Rectangle imageBounds = DialDevice.getBackgroundSize();

		// size of title
		Point size = computeTitleSize();

		// size of picture
		size.x = Math.max(size.x, imageBounds.width);
		size.y += imageBounds.height;

		// using code from Composite.computeSize
		if (wHint != SWT.DEFAULT) {
			size.x = wHint;
		}
		if (hHint != SWT.DEFAULT) {
			size.y = hHint;
		}
		Rectangle trim = computeTrim(0, 0, size.x, size.y);
		return new Point(trim.width, trim.height);
	}

	private Point computeTitleSize() {
		if (m_title == null) {
			return new Point(0, 0);
		}
		GC gc = new GC(this);
		Font oldFont = gc.getFont();
		FontData[] fontData = oldFont.getFontData();
		Font newFont = new Font(gc.getDevice(), fontData[0].getName(), fontData[0].getHeight(), SWT.BOLD);
		gc.setFont(newFont);
		Point size = gc.textExtent(m_title);
		size.y += TITLE_VERTICAL_PADDING;
		newFont.dispose();
		gc.dispose();
		return size;
	}

	/**
	 * Draw the control in the given GC, within the given rectangle. Either draw everything from
	 * scratch, or if {@code drawOptimal} is true only draw that which have changed. Note that this
	 * also means that each pixel touched has to have its color completely determined by this
	 * method. That is, if anti-aliasing or alpha is to be used, every affected pixel needs to have
	 * its color set to a fixed color beforehand. (This might cause flickering, but if so we need to
	 * do a manual double buffering instead.)
	 *
	 * @param gc
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @param drawOptimal
	 *            if true, the control has been drawn previously and only that which has changed
	 *            should be drawn.
	 */
	private void draw(GC gc, int x, int y, int width, int height, boolean drawOptimal) {

		double maxValue = Double.NEGATIVE_INFINITY;
		double minValue = Double.POSITIVE_INFINITY;
		java.awt.Color[] colors = new java.awt.Color[m_dials.length];
		double[] values = new double[m_dials.length];
		for (int n = 0; n < m_dials.length; n++) {
			IDialProvider provider = m_dials[n];
			Object input = getInput(provider.getId());
			colors[n] = provider.getColor(input);
			values[n] = provider.getValue(input);
			maxValue = Math.max(maxValue, values[n]);
			minValue = Math.min(minValue, values[n]);
		}
		DialDevice dd = DialDevice.buildSuitableDevice(minValue, maxValue, getUnit());
		ImageDescription bgConfig = dd.getBackground();
		Rectangle size = bgConfig.image.getBounds();
		int xOffset = (width - size.width) / 2;
		xOffset = Math.max(0, xOffset);
		// Draw the background
		int yOffset = height - size.height;
		updateBackgroundImage(dd, width, height, xOffset, 0);
		if (m_lastBackGroundImage != null) {
			gc.drawImage(m_lastBackGroundImage, 0, yOffset);
		}
		gc.setAntialias(SWT.ON);

		// Draw the dial title
		if (!drawOptimal && (m_title != null)) {
			drawTitle(gc, xOffset, height, bgConfig, m_title);
		}

		// Draw the dial text
		drawDialText(gc, xOffset, yOffset, bgConfig, dd.getTitle());

		// Draw the dials
		Color oldBackground = gc.getBackground();
		int oldAlpha = gc.getAlpha();
		int oldAntiAlias = gc.getAntialias();
		for (int n = 0; n < values.length; n++) {
			if (!Double.isInfinite(values[n])) {
				double normalizedValue = dd.normalizeForDevice(getUnit().quantity(values[n]));
				drawDial(gc, xOffset, yOffset, bgConfig, normalizedValue, colors[n].getAlpha(), colors[n]);
			}
		}
		// important to set back so that focus lines
		// gets drawn correct.
		gc.setAntialias(oldAntiAlias);
		gc.setAlpha(oldAlpha);
		gc.setBackground(oldBackground);
	}

	private Object getInput(String key) {
		return m_inputs.get(key);
	}

	private void updateBackgroundImage(DialDevice dialDevice, int width, int height, int xOffset, int yOffset) {
		if (m_lastBackGroundImage == null || m_lastDialDevice == null || !m_lastDialDevice.equals(dialDevice)
				|| m_lastWidth != width || m_lastHeight != height) {
			createBackground(dialDevice, width, height, xOffset, yOffset);
			m_lastDialDevice = dialDevice;
			m_lastHeight = height;
			m_lastWidth = width;
		}
	}

	private void createBackground(DialDevice dialDevice, int width, int height, int xOffset, int yOffset) {
		setCachedBackgroundImage(new Image(getDisplay(), width, height));
		GC fullBackgroundGC = new GC(m_lastBackGroundImage);

		// FIXME: Quick fix to avoid translating panel all over the place. Draw the panel Image first
		Rectangle bgBounds = dialDevice.getBackground().image.getBounds();
		Image panelImage = new Image(getDisplay(), bgBounds.width, bgBounds.height);

		GC dialGC = new GC(panelImage);
		Image g = UIPlugin.getDefault().getImage(UIPlugin.ICON_DIAL_BACKGROUND);
		for (int i = 0; i < panelImage.getBounds().width; i += g.getBounds().width) {
			dialGC.drawImage(g, i, 0);
		}
		drawBackground(dialGC, dialDevice);
		dialGC.dispose();

		// we have the panel. Let's create the full background
		for (int i = 0; i < m_lastBackGroundImage.getBounds().width; i += g.getBounds().width) {
			fullBackgroundGC.drawImage(g, i, yOffset);
		}
		fullBackgroundGC.drawImage(panelImage, xOffset, yOffset);
		fullBackgroundGC.dispose();
		panelImage.dispose();
	}

	private void drawTitle(GC gc, int xOffset, int height, ImageDescription config, String text) {
		Font oldFont = gc.getFont();
		FontData[] fontData = oldFont.getFontData();
		org.eclipse.swt.graphics.Font newFont = new Font(gc.getDevice(), fontData[0].getName(), fontData[0].getHeight(),
				SWT.BOLD);
		gc.setFont(newFont);
		Point textExtent = gc.textExtent(text);
		int x = config.dialTextCenter.x - Math.round(textExtent.x / 2.0f);
		int y = height - config.image.getBounds().height - TITLE_VERTICAL_PADDING - textExtent.y;
		y = Math.max(0, y);
		Color c = UIPlugin.getDefault().getFormColors(getDisplay()).getColor(IFormColors.TITLE);
		gc.setForeground(c);
		gc.setAlpha(192);
		gc.drawString(text, x + xOffset, y, true);
		gc.setAlpha(255);
		gc.setFont(oldFont);
		newFont.dispose();
	}

	private void drawDialText(GC gc, int xOffset, int YOffset, ImageDescription config, String text) {
		Font oldFont = gc.getFont();
		FontData[] fontData = oldFont.getFontData();
		org.eclipse.swt.graphics.Font newFont = new Font(gc.getDevice(), fontData[0].getName(), fontData[0].getHeight(),
				SWT.BOLD);
		gc.setFont(newFont);
		Point textExtent = gc.textExtent(text);
		int x = config.dialTextCenter.x - textExtent.x / 2 + xOffset;
		int y = config.dialTextCenter.y - textExtent.y / 2 + YOffset;
		Color foreground = new Color(gc.getDevice(), 0, 0, 0);
		gc.setForeground(foreground);
		gc.setAlpha(192);
		gc.drawString(text, x, y, true);
		gc.setAlpha(255);
		foreground.dispose();
		gc.setFont(oldFont);
		newFont.dispose();
	}

	private void drawDial(
		GC gc, int x, int y, ImageDescription config, double normalizedValue, double transparency,
		java.awt.Color color) {
		Point gaugeStart = normalizedToDevice(config, x, y, config.dialStartRadius, normalizedValue);
		Point gaugeEnd = normalizedToDevice(config, x, y, config.dialEndRadius, normalizedValue);
		Point gaugeEdge1 = normalizedToDevice(config, x, y, config.dialStartRadius, normalizedValue + 0.02);
		Point gaugeEdge2 = normalizedToDevice(config, x, y, config.dialStartRadius, normalizedValue - 0.02);

		int[] edges = new int[] {gaugeEnd.x, gaugeEnd.y, gaugeEdge1.x, gaugeEdge1.y, gaugeEdge2.x, gaugeEdge2.y};

		Color fillColor = SWTColorToolkit.getColor(SWTColorToolkit.asRGB(color));
		gc.setAlpha(color.getAlpha());
		gc.setBackground(fillColor);
		gc.setForeground(fillColor);
		gc.fillPolygon(edges);
		gc.drawPolygon(edges);
		gc.drawLine(gaugeStart.x, gaugeStart.y, gaugeEnd.x, gaugeEnd.y);
	}

	private void drawBackground(GC gc, DialDevice dialDevice) {
		if (m_gradientBegin == null || m_gradientEnd == null
				|| !m_valueUnit.getContentType().equals(m_gradientBegin.getUnit().getContentType())
				|| !m_valueUnit.getContentType().equals(m_gradientEnd.getUnit().getContentType())) {
			drawUnNormalizedbackground(gc, dialDevice, m_gradientBeginColor, m_gradientEndColor);
		} else {
			drawGradientBackground(gc, dialDevice, m_gradientBeginColor, m_gradientEndColor);
		}
		gc.drawImage(dialDevice.getBackground().image, 0, 0);
	}

	private void drawUnNormalizedbackground(GC gc, DialDevice dialDevice, RGB startRGB, RGB endRGB) {
		for (double n = 1.0; n >= 0.0; n -= 0.01) {
			Color color = new Color(gc.getDevice(), blend(startRGB, endRGB, n));
			gc.setBackground(color);
			fillArc(gc, dialDevice.getBackground(), n);
			color.dispose();
		}
	}

}
