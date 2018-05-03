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
package org.openjdk.jmc.flightrecorder.ext.g1.visualizer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.ext.g1.ColorMap;
import org.openjdk.jmc.flightrecorder.ext.g1.visualizer.region.HeapRegion;

// Remove this suppress when translation is required
@SuppressWarnings("nls")
public class HeapLayout extends Canvas {

	private class HighlightExitListener implements MouseTrackListener {
		@Override
		public void mouseHover(MouseEvent e) {
		}

		@Override
		public void mouseExit(MouseEvent e) {
			if (lastHighlighted != null) {
				GC gc = new GC(lastDraw);
				gc.setBackground(colors.getColor(lastHighlighted));
				gc.fillRectangle(lastHighlighted.getPosition());
				drawPad(gc, lastHighlighted.getPosition());
				gc.dispose();
				lastHighlighted = null;
			}
			redraw();
		}

		@Override
		public void mouseEnter(MouseEvent e) {
		}
	}

	private class HighlightListener implements MouseMoveListener {

		@Override
		public void mouseMove(MouseEvent e) {
			if (lastDraw == null) {
				return;
			}
			GC gc = new GC(lastDraw);
			if (lastHighlighted != null) {
				if (lastHighlighted.getPosition().contains(e.x, e.y)) {
					gc.dispose();
					return;
				}
				gc.setBackground(colors.getColor(lastHighlighted));
				gc.fillRectangle(lastHighlighted.getPosition());
				drawPad(gc, lastHighlighted.getPosition());
				lastHighlighted = null;
				redraw();
			}
			for (HeapRegion region : regions) {
				if (region.getPosition().contains(e.x, e.y)) {
					Color selectionColor = getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
					gc.setForeground(selectionColor);
					gc.drawRectangle(region.getBorder());
					lastHighlighted = region;
					redraw();
					break;
				}
			}
			gc.dispose();
		}
	}

	private class RegionSelectionListener implements MouseListener {

		@Override
		public void mouseDoubleClick(MouseEvent e) {
		}

		@Override
		public void mouseDown(MouseEvent e) {
		}

		@Override
		public void mouseUp(MouseEvent e) {
			GC gc = new GC(lastDraw);
			for (HeapRegion region : regions) {
				if (region.getPosition().contains(e.x, e.y)) {
					if ((e.stateMask & SWT.SHIFT) != 0) {
						if (focusRegions.size() == 1) {
							int firstSelectedIndex = focusRegions.get(0);
							int secondSelectedIndex = region.getIndex();
							if (secondSelectedIndex > firstSelectedIndex) {
								for (int i = firstSelectedIndex; i < secondSelectedIndex; i++) {
									focusRegions.add(i);
								}
							} else {
								for (int i = secondSelectedIndex; i < firstSelectedIndex; i++) {
									focusRegions.add(i);
								}
							}
						}
					} else if ((e.stateMask & SWT.CTRL) != 0) {
						Integer remove = null;
						for (Integer r : lastFocuses) {
							if (regions.get(r).getPosition().equals(region.getPosition())) {
								remove = r;
								clearFocus(gc, r);
							}
						}
						if (remove != null) {
							lastFocuses.remove(remove);
							focusRegions.remove(remove);
							redraw();
							notifyListeners(SWT.Selection, new HeapRegionSelectionEvent(focusRegions.stream()
									.map(r -> UnitLookup.NUMBER_UNITY.quantity(r)).collect(Collectors.toSet())));
							break;
						}
					} else if ((e.stateMask & SWT.CTRL) == 0) {
						for (Integer r : lastFocuses) {
							clearFocus(gc, r);
						}
						focusRegions.clear();
						lastFocuses.clear();
					}
					focusRegions.add(region.getIndex());
					updateFocus(gc);
					redraw();
					notifyListeners(SWT.Selection, new HeapRegionSelectionEvent(focusRegions.stream()
							.map(r -> UnitLookup.NUMBER_UNITY.quantity(r)).collect(Collectors.toSet())));
					break;
				}
			}
			gc.dispose();
		}

	}

	public enum CurveType {
		HILBERT, LEFT_RIGHT, ALTERNATING,
	}

	private boolean pad = false;

	private List<HeapRegion> regions;
	private Image lastDraw;
	private List<Integer> focusRegions;
	private List<Integer> lastFocuses;
	private HeapRegion lastHighlighted;
	private boolean fullRedraw;
	private List<HeapRegion> dirtyRegions;
	private CurveType curveType;
	private ColorMap colors;

	private int scalingFactorY = -1;
	private int scalingFactorX = -1;

	public HeapLayout(ColorMap colors, Composite parent, int style) {
		super(parent, style);
		this.addPaintListener(e -> drawCurve(e.gc));
		this.colors = colors;
		regions = new ArrayList<>();
		curveType = CurveType.LEFT_RIGHT;
		dirtyRegions = new ArrayList<>();
		focusRegions = new ArrayList<>();
		lastFocuses = new ArrayList<>();
		addMouseTrackListener(new HighlightExitListener());
		addMouseMoveListener(new HighlightListener());
		addMouseListener(new RegionSelectionListener());
	}

	public void setPadding(Boolean padding) {
		pad = padding;
	}

	public boolean isPadding() {
		return pad;
	}

	public void fullRedraw() {
		fullRedraw = true;
		redraw();
	}

	private void clearFocus(GC gc, Integer index) {
		HeapRegion region = regions.get(index);
		gc.setBackground(colors.getColor(region));
		gc.fillRectangle(region.getPosition());
		drawPad(gc, region.getPosition());
	}

	public void setCurveType(CurveType type) {
		curveType = type;
		fullRedraw = true;
	}

	public CurveType getCurveType() {
		return curveType;
	}

	public void show(List<HeapRegion> regions) {
		this.regions.clear();
		if (regions != null) {
			this.regions.addAll(regions);
		}
		fullRedraw = true;
	}

	public void updateRegion(int index, String type) {
		regions.get(index).setType(type);
		dirtyRegions.add(regions.get(index));
	}

	private void updateFocus(GC gc) {
		for (Integer focusRegionIndex : focusRegions) {
			HeapRegion focusRegion = regions.get(focusRegionIndex);
			gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
			gc.setBackground(colors.getColor(focusRegion));
			gc.fillRectangle(focusRegion.getPosition());
			gc.drawRectangle(focusRegion.getBorder());
		}
		lastFocuses = focusRegions;
	}

	protected void drawCurve(GC gc) {
		int n = closestLargerPowerOfTwo((int) Math.round(Math.sqrt(regions.size())));
		int scaleY = Math.floorDiv(getClientArea().height, n);
		int scaleX = Math.floorDiv(getClientArea().width, n);
		int i = 0, j = 0, size = 0;
		try {
			size = Math.floorDiv(getClientArea().width, scaleX);
		} catch (ArithmeticException ae) {
			return;
		}
		Rectangle last = new Rectangle(0, 0, 0, 0);
		boolean first = true, flip = false;
		if (dirtyRegions.size() == 0 && scaleY == scalingFactorY && scaleX == scalingFactorX && !fullRedraw) {
			gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
			GC gc2 = new GC(lastDraw);
			updateFocus(gc2);
			gc2.dispose();
			gc.drawImage(lastDraw, 0, 0);
		} else if (dirtyRegions.size() > 0 && !fullRedraw) {
			GC buffer = new GC(lastDraw);
			for (HeapRegion region : dirtyRegions) {
				buffer.setBackground(colors.getColor(region));
				buffer.fillRectangle(region.getPosition());
				drawPad(buffer, region.getPosition());
			}
			updateFocus(buffer);
			gc.drawImage(lastDraw, 0, 0);
			buffer.dispose();
			dirtyRegions.clear();
		} else if (regions.size() == 0) {
			gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
			gc.fillRectangle(this.getClientArea());
		} else {
			scalingFactorY = (scaleY == 0) ? 1 : scaleY;
			scalingFactorX = (scaleX == 0) ? 1 : scaleX;
			Rectangle fullSize = new Rectangle(0, 0, 0, 0);
			for (HeapRegion region : regions) {
				Rectangle pos = new Rectangle(0, 0, scalingFactorX, scalingFactorY);
				switch (curveType) {
				case HILBERT:
					Point hilbert = convertTo2D(n, i);
					pos = new Rectangle(hilbert.x * scalingFactorX, hilbert.y * scalingFactorY, scalingFactorX,
							scalingFactorY);
					break;
				case LEFT_RIGHT:
					if (j < size && !first) {
						pos.x = last.x + scaleX;
						pos.y = last.y;
						j++;
					} else if (first) {
						pos.x = 0;
						pos.y = 0;
						j++;
						first = false;
					} else {
						j = 1;
						pos.x = 0;
						pos.y = last.y + scaleY;
					}
					last = pos;
					break;
				case ALTERNATING:
					if (j < size && !first) {
						pos.x = flip ? last.x - scaleX : last.x + scaleX;
						pos.y = last.y;
						j++;
					} else if (first) {
						pos.x = 0;
						pos.y = 0;
						++j;
						first = false;
					} else {
						j = 1;
						pos.x = last.x;
						pos.y = last.y + scaleY;
						flip = !flip;
					}
					last = pos;
					break;
				}
				region.setPosition(pos);
				fullSize.add(pos);
				i++;
			}
			Image current = new Image(getDisplay(), fullSize);
			GC buffer = new GC(current);
			for (HeapRegion region : regions) {
				buffer.setBackground(colors.getColor(region));
				buffer.fillRectangle(region.getPosition());
				drawPad(buffer, region.getPosition());
			}
			gc.drawImage(current, 0, 0);
			updateFocus(buffer);
			this.setBounds(current.getBounds());
			this.getParent().layout(true, true);
			buffer.dispose();
			lastDraw = current;
			fullRedraw = false;
		}
	}

	private void drawPad(GC buffer, Rectangle pos) {
		if (pad) {
			buffer.setForeground(new Color(getDisplay(), 240, 240, 240));
			buffer.drawRectangle(pos.x, pos.y, pos.width, pos.height);
		}
	}

	public static int closestLargerPowerOfTwo(int n) {
		int powerOfTwo = 2;
		while (powerOfTwo <= 0x10000000) {
			if (powerOfTwo >= n) {
				return powerOfTwo;
			}
			powerOfTwo = powerOfTwo << 1;
		}
		throw new IllegalArgumentException("Input value too large!");
	}

	/**
	 * Maps the given 1-dimensional point d to a 2D Point in a Hilbert curve, given a specific
	 * number of iterations
	 *
	 * @param n
	 * @param d
	 * @return
	 */
	private Point convertTo2D(int n, int d) {
		int s, t = d;
		Point rp = new Point(0, 0);
		Point p = new Point(0, 0);
		for (s = 1; s < n; s *= 2) {
			rp.x = 1 & (t / 2);
			rp.y = 1 & (t ^ rp.x);
			rotateQuadrant(s, p, rp);
			p.x += s * rp.x;
			p.y += s * rp.y;
			t /= 4;
		}
		return p;
	}

	private Point rotateQuadrant(int n, Point p, Point rp) {
		int t;
		if (rp.y == 0) {
			if (rp.x == 1) {
				p.x = n - 1 - p.x;
				p.y = n - 1 - p.y;
			}
			t = p.x;
			p.x = p.y;
			p.y = t;
		}
		return p;
	}
}
