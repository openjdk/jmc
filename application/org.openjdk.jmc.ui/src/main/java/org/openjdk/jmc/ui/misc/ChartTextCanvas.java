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

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.accessibility.FocusTracker;
import org.openjdk.jmc.ui.charts.IChartInfoVisitor;
import org.openjdk.jmc.ui.charts.IXDataRenderer;
import org.openjdk.jmc.ui.charts.XYChart;
import org.openjdk.jmc.ui.common.util.Environment;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.PatternFly.Palette;

public class ChartTextCanvas extends Canvas {
	private int laneHeight;
	private int savedLaneHeight;
	private int minLaneHeight = -1;
	private int numItems = 0;
	private int lastMouseX = -1;
	private int lastMouseY = -1;
	private List<Rectangle2D> highlightRects;

	private class Selector extends MouseAdapter implements MouseMoveListener, MouseTrackListener {

		int selectionStartX = -1;
		int selectionStartY = -1;
		Point highlightSelectionStart;
		Point highlightSelectionEnd;
		Point lastSelection;
		boolean selectionIsClick = false;
		Set<Point> highlightPoints;

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
			if ((e.button == 1) && ((e.stateMask & SWT.MOD4) == 0) && ((e.stateMask & SWT.CTRL) == 0)
					&& ((e.stateMask & SWT.SHIFT) == 0)) {
				highlightPoints = new HashSet<>();
				highlightPoints.add(new Point(e.x, e.y));
				selectionStartX = e.x;
				selectionStartY = e.y;
				highlightSelectionEnd = new Point(-1, -1);
				lastSelection = new Point(-1, -1);
				selectionIsClick = true;
				toggleSelect(selectionStartX, selectionStartY);
			} else if (((e.stateMask & SWT.CTRL) != 0) && (e.button == 1)) {
				highlightPoints.add(new Point(e.x, e.y));
				select(e.x, e.x, e.y, e.y, false);
				if (selectionListener != null) {
					selectionListener.run();
				}
			} else if (((e.stateMask & SWT.SHIFT) != 0) && (e.button == 1)) {
				if (highlightSelectionEnd.y == -1) {
					highlightSelectionEnd = new Point(e.x, e.y);
					lastSelection = highlightSelectionEnd;
					if (highlightSelectionStart.y > highlightSelectionEnd.y) {
						Point temp = highlightSelectionStart;
						highlightSelectionStart = highlightSelectionEnd;
						highlightSelectionEnd = temp;
					}
				} else {
					if (e.y > highlightSelectionStart.y && e.y < highlightSelectionEnd.y) {
						if (e.y < lastSelection.y) {
							highlightSelectionEnd = new Point(e.x, e.y);
						} else if (e.y > lastSelection.y) {
							highlightSelectionStart = new Point(e.x, e.y);
						}
					} else if (e.y < highlightSelectionStart.y) {
						highlightSelectionStart = new Point(e.x, e.y);
						lastSelection = highlightSelectionStart;
					} else if (e.y > highlightSelectionEnd.y) {
						highlightSelectionEnd = new Point(e.x, e.y);
						lastSelection = highlightSelectionEnd;
					}
				}
				select(highlightSelectionStart.x, highlightSelectionStart.x, highlightSelectionStart.y,
						highlightSelectionEnd.y, true);
				if (selectionListener != null) {
					selectionListener.run();
				}
			}
		}

		@Override
		public void mouseMove(MouseEvent e) {
			if (selectionStartX >= 0) {
				highlightRects = null;
				updateSelectionState(e);
			} else {
				lastMouseX = e.x;
				lastMouseY = e.y;
				updateHighlightRects();
			}
		}

		private void updateSelectionState(MouseEvent e) {
			int x = e.x;
			int y = e.y;
			if (selectionIsClick && ((Math.abs(x - selectionStartX) > 3) || (Math.abs(y - selectionStartY) > 3))) {
				selectionIsClick = false;
			}
			if (!selectionIsClick) {
				select((int) (selectionStartX / xScale), (int) (selectionStartX / xScale),
						(int) (selectionStartY / yScale), (int) (y / yScale), true);
			}
		}

		@Override
		public void mouseUp(MouseEvent e) {
			if (selectionStartX >= 0 && (e.button == 1)) {
				updateSelectionState(e);
				highlightSelectionStart = new Point(selectionStartX, selectionStartY);
				selectionStartX = -1;
				selectionStartY = -1;
				if (selectionListener != null) {
					selectionListener.run();
				}
			}
		}

		@Override
		public void mouseEnter(MouseEvent e) {
		}

		@Override
		public void mouseExit(MouseEvent e) {
			if (!getClientArea().contains(e.x, e.y)) {
				resetHoveredItemData();
			}
			clearHighlightRects();
		}

		@Override
		public void mouseHover(MouseEvent e) {
		}
	}

	class Painter implements PaintListener {

		@Override
		public void paintControl(PaintEvent e) {
			int minScrollWidth = (int) ((awtChart.getLongestCharWidth() + 10) * xScale);
			int rectWidth = Math.max(minScrollWidth, getParent().getSize().x);
			Rectangle rect = new Rectangle(0, 0, rectWidth, getParent().getSize().y);
			if (getNumItems() > 0) {
				if (minLaneHeight == -1) {
					minLaneHeight = chartCanvas.calculateMinLaneHeight(rect);
					laneHeight = minLaneHeight;
				}
				if (getNumItems() != 1 && !(laneHeight * getNumItems() < rect.height)) {
					rect.height = laneHeight * getNumItems();
				}
			}

			if (awtNeedsRedraw || !awtCanvas.hasImage(rect.width, rect.height)) {
				Graphics2D g2d = awtCanvas.getGraphics(rect.width, rect.height);
				minLaneHeight = (int) (g2d.getFontMetrics().getHeight() * xScale);
				Point adjusted = chartCanvas.translateDisplayToImageCoordinates(rect.width, rect.height);
				g2d.setColor(Palette.PF_BLACK_100.getAWTColor());
				g2d.fillRect(0, 0, adjusted.x, adjusted.y);
				render(g2d, adjusted.x, adjusted.y);
				((ScrolledComposite) getParent()).setMinSize(rect.width, rect.height);
				if (highlightRects != null) {
					updateHighlightRects();
				}
				awtNeedsRedraw = false;
			}
			awtCanvas.paint(e, 0, 0);
		}
	}

	public void setNumItems(int numItems) {
		this.numItems = numItems;
	}

	private int getNumItems() {
		return numItems;
	}

	void setOverviewLaneHeight() {
		this.savedLaneHeight = laneHeight;
		setLaneHeight(-1);
	}

	void adjustLaneHeight(int amount) {
		if (laneHeight == -1) {
			restoreLaneHeight();
		}
		laneHeight = Math.max(minLaneHeight, laneHeight + amount);
	}

	void setLaneHeight(int height) {
		this.laneHeight = height;
	}

	void restoreLaneHeight() {
		laneHeight = savedLaneHeight;
	}

	void resetLaneHeight() {
		if (minLaneHeight != -1) {
			minLaneHeight = chartCanvas.initMinLaneHeight();
			laneHeight = minLaneHeight;
		}
	}

	class KeyNavigator implements KeyListener {

		@Override
		public void keyPressed(KeyEvent event) {
			switch (event.character) {
			default:
				switch (event.keyCode) {
				case SWT.ESC:
					awtChart.clearSelection();
					if (selectionListener != null) {
						selectionListener.run();
					}
					redrawChart();
					redrawChartText();
					break;
				default:
					// Ignore
				}
			}
		}

		@Override
		public void keyReleased(KeyEvent event) {
			// Ignore
		}

	}

	private class AntiAliasingListener implements IPropertyChangeListener {

		@Override
		public void propertyChange(PropertyChangeEvent event) {
			redrawChartText();
		}

	}

	/**
	 * This gets the "normal" DPI value for the system (72 on MacOS and 96 on Windows/Linux. It's
	 * used to determine how much larger the current DPI is so that we can draw the charts based on
	 * how large that area would be given the "normal" DPI value. Every draw on this smaller chart
	 * is then scaled up by the Graphics2D objects DefaultTransform.
	 */
	private final double xScale = Display.getDefault().getDPI().x / Environment.getNormalDPI();
	private final double yScale = Display.getDefault().getDPI().y / Environment.getNormalDPI();

	public final AwtCanvas awtCanvas = new AwtCanvas();
	private boolean awtNeedsRedraw;
	private Runnable selectionListener;
	private IPropertyChangeListener aaListener;
	private XYChart awtChart;
	private ChartCanvas chartCanvas;
	private MCContextMenuManager chartMenu;
	private Object hoveredItemData;

	public ChartTextCanvas(Composite parent) {
		super(parent, SWT.NO_BACKGROUND);
		numItems = 0;
		addPaintListener(new Painter());
		Selector selector = new Selector();
		addMouseListener(selector);
		addMouseMoveListener(selector);
		FocusTracker.enableFocusTracking(this);
		addKeyListener(new KeyNavigator());
		aaListener = new AntiAliasingListener();
		UIPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(aaListener);
		addDisposeListener(e -> UIPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(aaListener));
		((ScrolledComposite) getParent()).getVerticalBar().addListener(SWT.Selection, e -> vBarScroll());
	}

	private void vBarScroll() {
		if (chartCanvas != null) {
			Point location = ((ScrolledComposite) getParent()).getOrigin();
			chartCanvas.syncScroll(location);
		}
	}

	public IMenuManager getContextMenu() {
		if (chartMenu == null) {
			chartMenu = MCContextMenuManager.create(this);
			chartMenu.addMenuListener(manager -> clearHighlightRects());
		}
		return chartMenu;
	}

	private void render(Graphics2D context, int width, int height) {
		if (awtChart != null) {
			awtChart.renderTextCanvasText(context, width, height);
		}
	}

	public Object getHoveredItemData() {
		return this.hoveredItemData;
	}

	public void setHoveredItemData(Object data) {
		this.hoveredItemData = data;
	}

	void resetHoveredItemData() {
		this.hoveredItemData = null;
	}

	public void syncHighlightedRectangles(List<Rectangle2D> newRects) {
		highlightRects = newRects;
		redraw();
	}

	private void updateHighlightRects() {
		infoAt(new IChartInfoVisitor.Adapter() {
			@Override
			public void hover(Object data) {
				if (data != null) {
					setHoveredItemData(data);
				}
			}
		}, lastMouseX, lastMouseY);
		redraw();
		if (chartCanvas != null) {
			chartCanvas.syncHighlightedRectangles(highlightRects);
		}
	}

	private void clearHighlightRects() {
		if (highlightRects != null) {
			highlightRects = null;
			redraw();
		}
	}

	public void select(int x1, int x2, int y1, int y2, boolean clear) {
		Point p1 = chartCanvas.translateDisplayToImageCoordinates(x1, y1);
		Point p2 = chartCanvas.translateDisplayToImageCoordinates(x2, y2);
		if ((awtChart != null) && awtChart.select(p1.x, p2.x, p1.y, p2.y, clear)) {
			redrawChartText();
			redrawChart();
		}
	}

	private void toggleSelect(int x, int y) {
		Point p = chartCanvas.translateDisplayToImageCoordinates(x, y);
		if (awtChart != null) {
			if (!awtChart.select(p.x, p.x, p.y, p.y, true)) {
				awtChart.clearSelection();
			}
			redrawChartText();
			redrawChart();
		}
	}

	public void setChart(XYChart awtChart) {
		this.awtChart = awtChart;
		notifyListener();
	}

	public void setChartCanvas(ChartCanvas chartCanvas) {
		this.chartCanvas = chartCanvas;
	}

	public void syncScroll(Point scrollPoint) {
		((ScrolledComposite) getParent()).setOrigin(scrollPoint);
	}

	public void replaceRenderer(IXDataRenderer rendererRoot) {
		assert awtChart != null;
		awtChart.setRendererRoot(rendererRoot);
		notifyListener();
		redrawChartText();
	}

	public void setSelectionListener(Runnable selectionListener) {
		this.selectionListener = selectionListener;
	}

	private void notifyListener() {
		if (selectionListener != null) {
			selectionListener.run();
		}
	}

	public void infoAt(IChartInfoVisitor visitor, int x, int y) {
		Point p = chartCanvas.translateDisplayToImageCoordinates(x, y);
		if (awtChart != null) {
			awtChart.infoAt(visitor, p.x, p.y);
		}
	}

	/**
	 * Mark both the (AWT) chart and the SWT control as needing a redraw.
	 */
	public void redrawChartText() {
		awtNeedsRedraw = true;
		getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (!isDisposed()) {
					redraw();
				}
			}
		});
	}

	private void redrawChart() {
		if (chartCanvas != null) {
			chartCanvas.redrawChart();
		}
	}
}
