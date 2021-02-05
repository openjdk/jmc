/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.accessibility.FocusTracker;
import org.openjdk.jmc.ui.charts.IChartInfoVisitor;
import org.openjdk.jmc.ui.charts.IXDataRenderer;
import org.openjdk.jmc.ui.charts.XYChart;
import org.openjdk.jmc.ui.common.util.Environment;
import org.openjdk.jmc.ui.common.util.Environment.OSType;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.PatternFly.Palette;

public class ChartCanvas extends Canvas {
	private int laneHeight;
	private int minLaneHeight = -1;
	private int minReadableLaneHeight;
	private int savedLaneHeight;
	private int numItems = 0;
	private int lastMouseX = -1;
	private int lastMouseY = -1;
	private List<Rectangle2D> highlightRects;
	private Object hoveredItemData;

	private class Selector extends MouseAdapter implements MouseMoveListener, MouseTrackListener {

		int selectionStartX = -1;
		int selectionStartY = -1;
		Point highlightSelectionStart;
		Point highlightSelectionEnd;
		Point lastSelection;
		boolean selectionIsClick = false;

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
				selectionStartX = e.x;
				selectionStartY = e.y;
				highlightSelectionEnd = new Point(-1, -1);
				lastSelection = new Point(-1, -1);
				selectionIsClick = true;
				toggleSelect(selectionStartX, selectionStartY);
			} else if (((e.stateMask & SWT.CTRL) != 0) && (e.button == 1)) {
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
				select(highlightSelectionStart.x, highlightSelectionEnd.x, highlightSelectionStart.y,
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
				select((int) (selectionStartX / xScale), (int) (x / xScale), (int) (selectionStartY / yScale),
						(int) (y / yScale), true);
			}
		}

		@Override
		public void mouseUp(MouseEvent e) {
			if (selectionStartX >= 0 && (e.button == 1)) {
				updateSelectionState(e);
				highlightSelectionStart = new Point(selectionStartX, selectionStartY);
				selectionStartX = -1;
				selectionStartY = -1;
				if (selectionIsClick) {
					notifyZoomOnClickListener(e.button);
				}
				if (selectionListener != null) {
					selectionListener.run();
					if (zoomToSelectionListener != null && !selectionIsClick) {
						zoomToSelectionListener.run();
					}
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
			Rectangle rect = new Rectangle(0, 0, getParent().getSize().x, getParent().getSize().y);
			if (minLaneHeight == -1) {
				minLaneHeight = calculateMinLaneHeight(rect);
				laneHeight = minLaneHeight;
			}
			if (getNumItems() == 0) {
				rect = getClientArea();
			} else if (getNumItems() == 1 || (laneHeight * getNumItems() < rect.height)) {
				// it fills the height
			} else {
				rect.height = laneHeight * getNumItems();
			}

			if (awtNeedsRedraw || !awtCanvas.hasImage(rect.width, rect.height)) {
				Graphics2D g2d = awtCanvas.getGraphics(rect.width, rect.height);
				Point adjusted = translateDisplayToImageCoordinates(rect.width, rect.height);
				g2d.setColor(Palette.PF_BLACK_100.getAWTColor());
				g2d.fillRect(0, 0, adjusted.x, adjusted.y);
				render(g2d, adjusted.x, adjusted.y);
				if (isScrollableChart()) {
					minReadableLaneHeight = (int) (g2d.getFontMetrics().getHeight() * xScale);
					minLaneHeight = initMinLaneHeight();
					((ScrolledComposite) getParent()).setMinSize(rect.width, rect.height);
				}
				awtNeedsRedraw = false;
			}
			awtCanvas.paint(e, 0, 0);
			// Crude, flickering highlight of areas also delivered to tooltips.
			// FIXME: Remove flicker by drawing in a buffered stage (AWT or SWT).
			List<Rectangle2D> rs = highlightRects;
			if (rs != null) {
				GC gc = e.gc;
				gc.setForeground(getForeground());
				for (Rectangle2D r : rs) {
					int x = (int) (((int) r.getX()) * xScale);
					int y = (int) (((int) r.getY()) * yScale);
					if ((r.getWidth() == 0) && (r.getHeight() == 0)) {
						int width = (int) Math.round(4 * xScale);
						int height = (int) Math.round(4 * yScale);
						gc.drawOval(x - (int) Math.round(2 * xScale), y - (int) Math.round(2 * yScale), width, height);
					} else {
						int width = (int) Math.round(r.getWidth() * xScale);
						int height = (int) Math.round(r.getHeight() * yScale);
						gc.drawRectangle(x, y, width, height);
					}
				}
				updateHighlightRects();
			}
		}
	}

	public void setNumItems(int numItems) {
		this.numItems = numItems;
	}

	private int getNumItems() {
		return numItems;
	}

	private boolean isScrollableChart() {
		return getParent() instanceof ScrolledComposite;
	}

	protected int initMinLaneHeight() {
		// if there are no items, or
		// if the min readable lane height * the number of items exceeds the screen, then use min readable height
		if (getNumItems() == 0 || minReadableLaneHeight * getNumItems() > getParent().getSize().y) {
			return minReadableLaneHeight;
		} else { // if the minimum readable lane height * the number of items leaves extra space, then the min height is the height / number of items
			return getParent().getSize().y / getNumItems();
		}
	}

	protected int calculateMinLaneHeight(Rectangle rect) {
		return (int) (awtCanvas.getGraphics(rect.width, rect.height).getFontMetrics().getHeight() * xScale);
	}

	public boolean isLaneHeightMinimumSize() {
		return laneHeight == minLaneHeight;
	}

	void setOverviewLaneHeight() {
		this.savedLaneHeight = laneHeight;
		setLaneHeight(-1);
		zoomer = new Zoomer();
		addListener(SWT.MouseVerticalWheel, zoomer);
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
		removeListener(SWT.MouseVerticalWheel, zoomer);
	}

	public void resetLaneHeight() {
		if (minLaneHeight != -1) {
			if (laneHeight == -1) {
				removeListener(SWT.MouseVerticalWheel, zoomer);
			}
			minLaneHeight = initMinLaneHeight();
			laneHeight = minLaneHeight;
		}
	}

	class Zoomer implements Listener {

		@Override
		public void handleEvent(Event event) {
			handleWheelEvent(event.stateMask, event.x, event.count);
		}

	}

	/**
	 * Steals the wheel events from the currently focused control while hovering over this
	 * (ChartCanvas) control. Used on Windows to allow zooming without having to click in the chart
	 * first as click causes a selection.
	 */
	class WheelStealingZoomer implements Listener, MouseTrackListener, FocusListener {

		private Control stealWheelFrom;

		@Override
		public void handleEvent(Event event) {
			if (isDisposed()) {
				stop();
			} else if (stealWheelFrom != null && !stealWheelFrom.isDisposed()) {
				Point canvasSize = getSize();
				Point canvasPoint = toControl(stealWheelFrom.toDisplay(event.x, event.y));
				if (canvasPoint.x >= 0 && canvasPoint.y >= 0 && canvasPoint.x < canvasSize.x
						&& canvasPoint.y < canvasSize.y) {
					handleWheelEvent(event.stateMask, canvasPoint.x, event.count);
					event.doit = false;
				}
			}
		}

		private void stop() {
			if (stealWheelFrom != null && !stealWheelFrom.isDisposed()) {
				stealWheelFrom.removeListener(SWT.MouseVerticalWheel, this);
				stealWheelFrom.removeFocusListener(this);
				stealWheelFrom = null;
			}
		}

		@Override
		public void mouseEnter(MouseEvent e) {
			stop();
			Control stealWheelFrom = getDisplay().getFocusControl();
			if (stealWheelFrom != null && stealWheelFrom != ChartCanvas.this) {
				stealWheelFrom.addListener(SWT.MouseVerticalWheel, this);
				stealWheelFrom.addFocusListener(this);
				this.stealWheelFrom = stealWheelFrom;
			}
		}

		@Override
		public void mouseExit(MouseEvent e) {
		}

		@Override
		public void mouseHover(MouseEvent e) {
		};

		@Override
		public void focusGained(FocusEvent e) {
		}

		@Override
		public void focusLost(FocusEvent e) {
			stop();
		}
	}

	class KeyNavigator implements KeyListener {

		@Override
		public void keyPressed(KeyEvent event) {
			switch (event.character) {
			case '+':
				zoom(1);
				break;
			case '-':
				zoom(-1);
				break;
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
				case SWT.ARROW_RIGHT:
					pan(10);
					break;
				case SWT.ARROW_LEFT:
					pan(-10);
					break;
				case SWT.ARROW_UP:
					zoom(1);
					break;
				case SWT.ARROW_DOWN:
					zoom(-1);
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
			redrawChart();
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

	private final AwtCanvas awtCanvas = new AwtCanvas();
	private boolean awtNeedsRedraw;
	private Runnable selectionListener;
	private Runnable zoomToSelectionListener;
	private Consumer<Boolean> zoomOnClickListener;
	private IPropertyChangeListener aaListener;
	private XYChart awtChart;
	private MCContextMenuManager chartMenu;
	private ChartTextCanvas textCanvas;
	private Listener zoomer;

	public ChartCanvas(Composite parent) {
		super(parent, SWT.NO_BACKGROUND);
		addPaintListener(new Painter());
		Selector selector = new Selector();
		addMouseListener(selector);
		addMouseMoveListener(selector);
		FocusTracker.enableFocusTracking(this);
		addKeyListener(new KeyNavigator());
		aaListener = new AntiAliasingListener();
		UIPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(aaListener);
		addDisposeListener(e -> UIPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(aaListener));
		if (Environment.getOSType() == OSType.WINDOWS) {
			addMouseTrackListener(new WheelStealingZoomer());
		}
		if (isScrollableChart()) { // JFR Threads Page
			((ScrolledComposite) getParent()).getVerticalBar().addListener(SWT.Selection, e -> vBarScroll());
		} else {
			addMouseTrackListener(selector);
			addListener(SWT.MouseVerticalWheel, new Zoomer());
		}
	}

	private void vBarScroll() {
		if (textCanvas != null) {
			Point location = ((ScrolledComposite) getParent()).getOrigin();
			textCanvas.syncScroll(location);
			awtChart.updateZoomPanIndicator();
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
			awtChart.renderChart(context, width, height);
			if (textCanvas == null) {
				awtChart.renderText(context, width, height);
			}
			awtChart.updateZoomPanIndicator();
		}
	}

	/**
	 * Translates display coordinates into image coordinates for the chart.
	 *
	 * @param x
	 *            the provided x coordinate
	 * @param y
	 *            the provided y coordinate
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
	 * @return the x coordinate in the chart's coordinate space
	 */
	protected int translateDisplayToImageXCoordinates(int x) {
		return (int) Math.round(x / xScale);
	}

	/**
	 * Translates a display x coordinate into an image x coordinate for the chart.
	 *
	 * @param x
	 *            the provided display x coordinate
	 * @return the x coordinate in the chart's coordinate space
	 */
	protected int translateDisplayToImageYCoordinates(int y) {
		return (int) Math.round(y / yScale);
	}

	public Object getHoveredItemData() {
		return this.hoveredItemData;
	}

	public void setHoveredItemData(Object data) {
		this.hoveredItemData = data;
	}

	public void resetHoveredItemData() {
		this.hoveredItemData = null;
	}

	public void syncHighlightedRectangles(List<Rectangle2D> newRects) {
		highlightRects = newRects;
		redraw();
	}

	private void updateHighlightRects() {
		List<Rectangle2D> newRects = new ArrayList<>();
		infoAt(new IChartInfoVisitor.Adapter() {
			@Override
			public void visit(IBucket bucket) {
				newRects.add(bucket.getTarget());
			}

			@Override
			public void visit(IPoint point) {
				Point2D target = point.getTarget();
				newRects.add(new Rectangle2D.Double(target.getX(), target.getY(), 0, 0));
			}

			@Override
			public void visit(ISpan span) {
				newRects.add(span.getTarget());
			}

			@Override
			public void visit(ITick tick) {
				Point2D target = tick.getTarget();
				newRects.add(new Rectangle2D.Double(target.getX(), target.getY(), 0, 0));
			}

			@Override
			public void visit(ILane lane) {
				// FIXME: Do we want this highlighted?
			}

			@Override
			public void hover(Object data) {
				if (data != null) {
					setHoveredItemData(data);
				}
			}
		}, lastMouseX, lastMouseY);
		// Attempt to reduce flicker by avoiding unnecessary updates.
		if (!newRects.equals(highlightRects)) {
			highlightRects = newRects;
			if (textCanvas != null) {
				textCanvas.syncHighlightedRectangles(highlightRects);
			}
			redraw();
		}
	}

	private void clearHighlightRects() {
		if (highlightRects != null) {
			highlightRects = null;
			redraw();
		}
	}

	private void handleWheelEvent(int stateMask, int x, int count) {
		// SWT.MOD1 is CMD on OS X and CTRL elsewhere.
		if ((stateMask & SWT.MOD1) != 0) {
			pan(count * 3);
		} else {
			zoom(translateDisplayToImageXCoordinates(x), count);
		}
	}

	private void pan(int rightPercent) {
		if ((awtChart != null) && awtChart.pan(rightPercent)) {
			redrawChart();
		}
	}

	private void zoom(int zoomInSteps) {
		if ((awtChart != null) && awtChart.zoom(zoomInSteps)) {
			redrawChart();
		}
	}

	private void zoom(int x, int zoomInSteps) {
		if ((awtChart != null) && awtChart.zoom(x, zoomInSteps)) {
			redrawChart();
		}
	}

	private void select(int x1, int x2, int y1, int y2, boolean clear) {
		Point p1 = translateDisplayToImageCoordinates(x1, y1);
		Point p2 = translateDisplayToImageCoordinates(x2, y2);
		if ((awtChart != null) && awtChart.select(p1.x, p2.x, p1.y, p2.y, clear)) {
			redrawChart();
			redrawChartText();
		}
	}

	private void toggleSelect(int x, int y) {
		Point p = translateDisplayToImageCoordinates(x, y);
		if (awtChart != null) {
			final IQuantity[] range = new IQuantity[2];
			infoAt(new IChartInfoVisitor.Adapter() {
				@Override
				public void visit(IBucket bucket) {
					if (range[0] == null) {
						range[0] = (IQuantity) bucket.getStartX();
						range[1] = (IQuantity) bucket.getEndX();
					}
				}

				@Override
				public void visit(ISpan span) {
					if (range[0] == null) {
						IDisplayable x0 = span.getStartX();
						IDisplayable x1 = span.getEndX();
						range[0] = (x0 instanceof IQuantity) ? (IQuantity) x0 : null;
						range[1] = (x1 instanceof IQuantity) ? (IQuantity) x1 : null;
					}
				}
			}, x, y);
			if ((range[0] != null) || (range[1] != null)) {
				if (!awtChart.select(range[0], range[1], p.y, p.y, true)) {
					awtChart.clearSelection();
				}
			} else {
				if (!awtChart.select(p.x, p.x, p.y, p.y, true)) {
					awtChart.clearSelection();
				}
			}
			notifyZoomOnClickListener(SWT.MouseDown);
			redrawChart();
			redrawChartText();
		}
	}

	public void setChart(XYChart awtChart) {
		this.awtChart = awtChart;
		notifyListener();
		redrawChart();
	}

	public void setTextCanvas(ChartTextCanvas textCanvas) {
		this.textCanvas = textCanvas;
	}

	public void syncScroll(Point scrollPoint) {
		((ScrolledComposite) getParent()).setOrigin(scrollPoint);
	}

	public void replaceRenderer(IXDataRenderer rendererRoot) {
		assert awtChart != null;
		awtChart.setRendererRoot(rendererRoot);
		notifyListener();
		redrawChart();
	}

	public void setSelectionListener(Runnable selectionListener) {
		this.selectionListener = selectionListener;
	}

	public void setZoomToSelectionListener(Runnable zoomListener) {
		this.zoomToSelectionListener = zoomListener;
	}

	public void setZoomOnClickListener(Consumer<Boolean> clickListener) {
		this.zoomOnClickListener = clickListener;
	}

	private void notifyZoomOnClickListener(Integer button) {
		if (zoomOnClickListener != null) {
			zoomOnClickListener.accept(button == SWT.MouseDown);
		}
	}

	private void notifyListener() {
		if (selectionListener != null) {
			selectionListener.run();
		}
	}

	public void changeCursor(Cursor cursor) {
		setCursor(cursor);
	}

	public void infoAt(IChartInfoVisitor visitor, int x, int y) {
		Point p = translateDisplayToImageCoordinates(x, y);
		if (awtChart != null) {
			awtChart.infoAt(visitor, p.x, p.y);
		}
	}

	/**
	 * Mark both the (AWT) chart and the SWT control as needing a redraw.
	 */
	public void redrawChart() {
		awtNeedsRedraw = true;
		getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (!isDisposed()) {
					redraw();
				}
			}
		});
	}

	private void redrawChartText() {
		if (textCanvas != null) {
			textCanvas.redrawChartText();
		}
	}

}
