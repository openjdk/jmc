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

import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import org.openjdk.jmc.common.item.IMemberAccessor;

public class PieChartCanvas extends Canvas {

	public static class AccumulatedSlice {
		private int count;
		private double size;

		public int getCount() {
			return count;
		}

		public double getSize() {
			return size;
		}

	}

	private static final int MARGIN = 10;
	private static final int LINE_WIDTH = 1;
	private static final RGB PIE_OUTILNE_COLOR = new RGB(0, 0, 0);
	private static final RGB PIE_NOT_VALID_COLOR = new RGB(192, 192, 192);
	private static final int START_ANGLE_OFFSET = 90;

	private IColorProvider colorProvider;
	private IMemberAccessor<Object, Object> field;
	private Iterable<?> elements;
	private final Object[] elementAtAngle = new Object[360];
	private boolean clockwise;
	private Point center = new Point(0, 0);
	private int radius;

	public PieChartCanvas(Composite parent) {
		super(parent, SWT.DOUBLE_BUFFERED);
		addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				Rectangle bounds = getBounds();
				GC gc = e.gc;
				gc.fillRectangle(bounds);
				center = new Point(bounds.width / 2, bounds.height / 2);
				radius = Math.min(bounds.width / 2, bounds.height / 2) - MARGIN;

				drawDropShadow(gc);
				drawPie(gc);
			}
		});
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public <T> void configure(Iterable<? extends T> elements, IMemberAccessor<?, T> field, boolean clockwise) {
		this.field = (IMemberAccessor) field;
		this.elements = elements;
		this.clockwise = clockwise;
	}

	private double calculateTotal() {
		double total = 0;
		if (elements != null && field != null) {
			for (Object element : elements) {
				Object value = field.getMember(element);
				if (!(value instanceof Number)) {
					return 0;
				}
				total += ((Number) value).doubleValue();
			}
		}
		return total;
	}

	public <T> void setColorProvider(IColorProvider colorProvider) {
		this.colorProvider = colorProvider;
	}

	private void drawSlices(GC gc) {
		double total = calculateTotal();
		PieSliceRenderer sliceRenderer = new PieSliceRenderer(gc, center, radius);
		if (total <= 0 || colorProvider == null) {
			sliceRenderer.addElement(1, null);
		} else {
			for (Object element : elements) {
				double value = ((Number) field.getMember(element)).doubleValue();
				double normalizedShare = value / total;
				sliceRenderer.addElement(normalizedShare, element);
			}
			sliceRenderer.addAccumulatedSlice();
		}
	}

	private class PieSliceRenderer {
		private final GC gc;
		private final Point center;
		private final int radius;
		private AccumulatedSlice accumulated = new AccumulatedSlice();
		private double currentValue = 0;
		private int lastAngle = 0;
		private final Color background;

		public PieSliceRenderer(GC gc, Point center, int radius) {
			this.gc = gc;
			this.center = center;
			this.radius = radius;
			background = gc.getBackground();
		}

		private void addElement(double elementSize, Object element) {
			if (elementSize > 0.005) {
				addAccumulatedSlice();
				addSlice(elementSize, element);
			} else {
				accumulated.count++;
				accumulated.size += elementSize;
			}
		}

		private void addAccumulatedSlice() {
			if (addSlice(accumulated.size, accumulated)) {
				accumulated = new AccumulatedSlice();
			}
		}

		private boolean addSlice(double size, Object value) {
			int nextAngle = fractionToAngle(size + currentValue);
			if (lastAngle < nextAngle) {
				currentValue += size;
				Color backgroundColor = colorProvider.getBackground(value);
				if (backgroundColor != null) {
					gc.setBackground(backgroundColor);
				} else {
					gc.setBackground(background);
				}
				drawPieSliceFromOffset(gc, center, radius, lastAngle, nextAngle);
				fill(lastAngle, nextAngle, value);
				lastAngle = nextAngle;
				return true;
			}
			return false;
		}

	}

	private void fill(int from, int to, Object value) {
		for (int i = from; i < to; i++) {
			elementAtAngle[i] = value;
		}
	}

	private static int fractionToAngle(double fraction) {
		return (int) (360.0 * fraction + 0.5);
	}

	private void drawPieSliceFromOffset(GC gc, Point center, int radius, int startAngle, int endAngle) {
		// Convert from clockwise from 12 o'clock, to counter clockwise from 3 o'clock
		int startWithOffset = (clockwise ? -startAngle : startAngle) + START_ANGLE_OFFSET;
		int endWithOffset = (clockwise ? -endAngle : endAngle) + START_ANGLE_OFFSET;
		drawPieSlice(gc, center.x, center.y, radius, startWithOffset, endWithOffset - startWithOffset);
		if (startAngle > 0 || endAngle < 360) {
			drawPieLine(gc, center.x, center.y, radius, endWithOffset);
		}
	}

	protected static void drawPieSlice(GC gc, int x, int y, int radius, int startAngle, int arcAngle) {
		gc.fillArc(x - radius, y - radius, radius * 2, radius * 2, startAngle, arcAngle);
		gc.drawArc(x - radius, y - radius, radius * 2, radius * 2, startAngle, arcAngle);
	}

	private static void drawPieLine(GC gc, int x, int y, int radius, int startWithOffset) {
		int x1 = (int) (radius * Math.cos(Math.toRadians(startWithOffset)));
		int y1 = (int) (radius * Math.sin(Math.toRadians(startWithOffset)));

		Color oldColor = gc.getForeground();
		int oldAlpha = gc.getAlpha();

		Color color = new Color(gc.getDevice(), 0, 0, 0);
		gc.setAlpha(96);
		gc.setForeground(color);
		gc.drawLine(x, y, x + x1, y - y1);
		color.dispose();

		gc.setForeground(oldColor);
		gc.setAlpha(oldAlpha);
	}

	private void drawPie(GC gc) {
		Color forground = gc.getForeground();
		Color background = gc.getBackground();
		int linewidth = gc.getLineWidth();
		int antiAlising = gc.getAntialias();
		gc.setBackground(SWTColorToolkit.getColor(PIE_NOT_VALID_COLOR));
		gc.setForeground(SWTColorToolkit.getColor(PIE_OUTILNE_COLOR));
		gc.setLineWidth(LINE_WIDTH);
		gc.setAntialias(SWT.ON);
		drawSlices(gc);
		gc.setBackground(background);
		gc.setForeground(forground);
		gc.setLineWidth(linewidth);
		gc.setAntialias(antiAlising);
	}

	private void drawDropShadow(GC gc) {
		if (radius < 1) {
			return;
		}
		gc.setAntialias(SWT.ON);
		for (int r = 0; r < 8; r++) {
			Color d = new Color(gc.getDevice(), 250 - r * 12, 250 - r * 12, 250 - r * 12);
			gc.setBackground(d);
			gc.fillArc(center.x - radius, center.y - radius, radius * 2 + 8 - r, radius * 2 + 8 - r, 0, 360);
			d.dispose();
		}
		gc.setAntialias(SWT.OFF);
	}

	public Object getElementAt(int x, int y) {
		if (center != null) {
			if (isInsideCircle(x, y, center.x, center.y, radius)) {
				int angle = ((int) (-getMathAngle(center.x, -center.y, x, -y) + START_ANGLE_OFFSET + 360 + 0.5)) % 360;
				return elementAtAngle[angle];
			}
		}
		return null;
	}

	private static boolean isInsideCircle(int x, int y, int centerX, int centerY, int radius) {
		return ((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY) < radius * radius);
	}

	private static double getMathAngle(int centerX, int centerY, int positionX, int positionY) {
		return Math.toDegrees(Math.atan2(positionY - centerY, positionX - centerX));
	}

}
