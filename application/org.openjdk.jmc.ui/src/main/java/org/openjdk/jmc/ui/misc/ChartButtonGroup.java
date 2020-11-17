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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.charts.SubdividedQuantityRange;
import org.openjdk.jmc.ui.charts.XYChart;
import org.openjdk.jmc.ui.misc.PatternFly.Palette;

public class ChartButtonGroup extends Composite {
	private static final String DEFAULT_CURSOR = "defaultCursor";
	private static final String HAND_CURSOR = "handCursor";
	private static final String ZOOM_IN_CURSOR = "zoomInCursor";
	private static final String ZOOM_OUT_CURSOR = "zoomOutCursor";
	private static final int ZOOM_INCREMENT = 1;
	private List<Button> buttonGroup;
	private Map<String, Cursor> cursors;
	private Button scaleToFitBtn;
	private Button selectionBtn;
	private Button zoomInBtn;
	private Button zoomOutBtn;
	private Button zoomPanBtn;
	private ChartCanvas chartCanvas;
	private ChartTextCanvas textCanvas;
	private Runnable resetLaneHeightAction;
	private XYChart chart;
	private ZoomPan zoomPan;

	public ChartButtonGroup(Composite parent) {
		super(parent, SWT.NONE);
		GridLayout gl = new GridLayout(5, true);
		gl.horizontalSpacing = 0;
		gl.marginWidth = 0;
		this.setLayout(gl);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		cursors = new HashMap<>();
		initCursorMap();

		buttonGroup = new ArrayList<>();
		initSelectionButton();
		initZoomInButton();
		initZoomOutButton();
		initZoomPanButton();
		initScaleToFitButton();
	}

	private void initCursorMap() {
		cursors.put(DEFAULT_CURSOR, getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
		cursors.put(HAND_CURSOR, getDisplay().getSystemCursor(SWT.CURSOR_HAND));
		cursors.put(ZOOM_IN_CURSOR, new Cursor(getDisplay(),
				UIPlugin.getDefault().getImage(UIPlugin.ICON_FA_ZOOM_IN).getImageData(), 0, 0));
		cursors.put(ZOOM_OUT_CURSOR, new Cursor(getDisplay(),
				UIPlugin.getDefault().getImage(UIPlugin.ICON_FA_ZOOM_OUT).getImageData(), 0, 0));
	}

	private void initSelectionButton() {
		selectionBtn = new Button(this, SWT.TOGGLE);
		selectionBtn.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		selectionBtn.setImage(UIPlugin.getDefault().getImage(UIPlugin.ICON_FA_SELECTION));
		selectionBtn.setSelection(true);
		selectionBtn.setToolTipText(Messages.ChartDisplayControlBar_SELECTION_TOOL_TOOLTIP);
		selectionBtn.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				setButtonSelectionStates(selectionBtn, null);
				changeCursor(DEFAULT_CURSOR);
			};
		});
		buttonGroup.add(selectionBtn);
	}

	private void initZoomInButton() {
		zoomInBtn = new Button(this, SWT.TOGGLE);
		zoomInBtn.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		zoomInBtn.setImage(UIPlugin.getDefault().getImage(UIPlugin.ICON_FA_ZOOM_IN));
		zoomInBtn.setSelection(false);
		StringBuilder sb = new StringBuilder();
		sb.append(Messages.ChartDisplayControlBar_ZOOM_IN_CLICK_TOOLTIP);
		sb.append(System.getProperty("line.separator"));
		sb.append(Messages.ChartDisplayControlBar_ZOOM_IN_HOLD_TOOLTIP);
		zoomInBtn.setToolTipText(sb.toString());
		zoomInBtn.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				setButtonSelectionStates(zoomInBtn, zoomPanBtn);
				changeCursor(ZOOM_IN_CURSOR);
			}
		});
		zoomInBtn.addMouseListener(new LongPressListener(ZOOM_INCREMENT));
		buttonGroup.add(zoomInBtn);
	}

	private void initZoomOutButton() {
		zoomOutBtn = new Button(this, SWT.TOGGLE);
		zoomOutBtn.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		zoomOutBtn.setImage(UIPlugin.getDefault().getImage(UIPlugin.ICON_FA_ZOOM_OUT));
		zoomOutBtn.setSelection(false);
		StringBuilder sb = new StringBuilder();
		sb.append(Messages.ChartDisplayControlBar_ZOOM_OUT_CLICK_TOOLTIP);
		sb.append(System.getProperty("line.separator"));
		sb.append(Messages.ChartDisplayControlBar_ZOOM_OUT_HOLD_TOOLTIP);
		zoomOutBtn.setToolTipText(sb.toString());
		zoomOutBtn.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event e) {
				setButtonSelectionStates(zoomOutBtn, zoomPanBtn);
				changeCursor(ZOOM_OUT_CURSOR);
			}
		});
		zoomOutBtn.addMouseListener(new LongPressListener(-ZOOM_INCREMENT));
		buttonGroup.add(zoomOutBtn);
	}

	private void initZoomPanButton() {
		zoomPanBtn = new Button(this, SWT.TOGGLE);
		zoomPanBtn.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		zoomPanBtn.setImage(UIPlugin.getDefault().getImage(UIPlugin.ICON_FA_ZOOM_PAN));
		zoomPanBtn.setSelection(false);
		zoomPanBtn.setToolTipText(Messages.ChartDisplayControlBar_ZOOM_PAN_TOOLTIP);
		zoomPanBtn.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				showZoomPanDisplay(zoomPanBtn.getSelection());
			}
		});
		buttonGroup.add(zoomPanBtn);
	}

	private void initScaleToFitButton() {
		scaleToFitBtn = new Button(this, SWT.PUSH);
		scaleToFitBtn.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		scaleToFitBtn.setImage(UIPlugin.getDefault().getImage(UIPlugin.ICON_FA_SCALE_TO_FIT));
		scaleToFitBtn.setSelection(false);
		scaleToFitBtn.setToolTipText(Messages.ChartDisplayControlBar_SCALE_TO_FIT_TOOLTIP);
		scaleToFitBtn.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				chart.resetTimeline();
				chartCanvas.redrawChart();
				getDisplay().syncExec(resetLaneHeightAction);
			}
		});
		buttonGroup.add(scaleToFitBtn);
	}

	void setChart(XYChart chart) {
		this.chart = chart;
	}

	void setResetLaneHeightAction(Runnable resetLaneHeightAction) {
		this.resetLaneHeightAction = resetLaneHeightAction;
	}

	void setChartCanvas(ChartCanvas chartCanvas) {
		this.chartCanvas = chartCanvas;
	}

	void setTextCanvas(ChartTextCanvas textCanvas) {
		this.textCanvas = textCanvas;
	}

	public void updateZoomPanIndicator() {
		zoomPan.redraw();
	}

	public void zoomOnClick(Boolean mouseDown) {
		boolean shouldZoom = zoomInBtn.getSelection() || zoomOutBtn.getSelection();
		if (shouldZoom) {
			if (mouseDown) {
				chart.clearSelection();
			} else {
				int zoomAmount = zoomInBtn.getSelection() ? ZOOM_INCREMENT : -ZOOM_INCREMENT;
				zoom(zoomAmount);
				if (textCanvas != null) {
					textCanvas.redrawChartText();
				}
			}
		}
	}

	public void zoomToSelection() {
		if (zoomInBtn.getSelection()) {
			IQuantity selectionStart = chart.getSelectionStart();
			IQuantity selectionEnd = chart.getSelectionEnd();
			if (selectionStart == null || selectionEnd == null) {
				chart.clearVisibleRange();
			} else {
				chart.setVisibleRange(selectionStart, selectionEnd);
				chartCanvas.redrawChart();
			}
		}
	}

	private void changeCursor(String cursorName) {
		chartCanvas.changeCursor(cursors.get(cursorName));
	}

	private void setButtonSelectionStates(Button buttonSelected, Button dependentButton) {
		for (Button button : buttonGroup) {
			if ((button.getStyle() & SWT.TOGGLE) != 0) {
				if (button.equals(buttonSelected)) {
					button.setSelection(true);
				} else if (dependentButton != null) {
					if (button.equals(dependentButton)) {
						button.setSelection(true);
					} else {
						button.setSelection(false);
					}
				} else {
					button.setSelection(false);
				}
			}
			showZoomPanDisplay(zoomPanBtn.getSelection());
		}
	}

	private class LongPressListener extends MouseAdapter {

		private static final long LONG_PRESS_TIME = 500;
		private Timer timer;
		private int zoomAmount;

		LongPressListener(int zoomAmount) {
			this.zoomAmount = zoomAmount;
		}

		@Override
		public void mouseDown(MouseEvent e) {
			if (e.button == 1) {
				timer = new Timer();
				timer.schedule(new LongPress(), LONG_PRESS_TIME, (long) (LONG_PRESS_TIME * 1.5));
			}
		}

		@Override
		public void mouseUp(MouseEvent e) {
			timer.cancel();
		}

		public class LongPress extends TimerTask {

			@Override
			public void run() {
				doZoomInOut(zoomAmount);
			}
		}

		private void doZoomInOut(int zoomAmount) {
			DisplayToolkit.inDisplayThread().execute(() -> zoom(zoomAmount));
		}
	}

	private void zoom(int zoomAmount) {
		chart.zoom(zoomAmount);
		chartCanvas.redrawChart();
	}

	public void createZoomPan(Composite parent) {
		zoomPan = new ZoomPan(parent);
		parent.setVisible(false);
	}

	private void showZoomPanDisplay(boolean show) {
		if (show) {
			zoomPan.getParent().setVisible(true);
			zoomPan.redrawZoomPan();
		} else {
			zoomPan.getParent().setVisible(false);
		}
	}

	private class ZoomPan extends Canvas {
		private static final int BORDER_PADDING = 2;
		private static final double MIN_HEIGHT_PERCENT = 0.15;
		private static final double MIN_WIDTH_PERCENT = 0.08;
		private IRange<IQuantity> chartRange;
		private IRange<IQuantity> lastChartZoomedRange;
		private Rectangle zoomRect;

		public ZoomPan(Composite parent) {
			super(parent, SWT.NO_BACKGROUND);
			addPaintListener(new Painter());
			PanDetector panDetector = new PanDetector();
			addMouseListener(panDetector);
			addMouseMoveListener(panDetector);
			addMouseWheelListener(panDetector);
			chartRange = chart.getVisibleRange();
		}

		public void redrawZoomPan() {
			redraw();
		}

		private class PanDetector extends MouseAdapter implements MouseMoveListener, MouseWheelListener {
			Point currentSelection;
			Point lastSelection;
			boolean isPan = false;

			@Override
			public void mouseDown(MouseEvent e) {
				if (e.button == 1 && inBounds(e)) {
					isPan = true;
					chart.setIsZoomPanDrag(isPan);
					currentSelection = chartCanvas.translateDisplayToImageCoordinates(e.x, e.y);
				}
			}

			private boolean inBounds(MouseEvent e) {
				Point zoomCanvasBounds = getParent().getSize();
				if (zoomRect.height < MIN_HEIGHT_PERCENT * zoomCanvasBounds.y
						|| zoomRect.width < MIN_WIDTH_PERCENT * zoomCanvasBounds.x) {
					return zoomCanvasBounds.x >= e.x && zoomCanvasBounds.y >= e.y;
				} else {
					return zoomRect.contains(e.x, e.y);
				}
			}

			@Override
			public void mouseUp(MouseEvent e) {
				isPan = false;
				chart.setIsZoomPanDrag(isPan);
			}

			@Override
			public void mouseMove(MouseEvent e) {
				zoomPan.setCursor(cursors.get(HAND_CURSOR));
				if (isPan && getParent().getSize().x >= e.x && getParent().getSize().y >= e.y) {
					lastSelection = currentSelection;
					currentSelection = chartCanvas.translateDisplayToImageCoordinates(e.x, e.y);
					int xdiff = currentSelection.x - lastSelection.x;
					int ydiff = currentSelection.y - lastSelection.y;
					updateZoomRectFromPan(xdiff, ydiff);
				}
			}

			@Override
			public void mouseScrolled(MouseEvent e) {
				updateZoomRectFromPan(0, -e.count);
			}
		}

		private void updateZoomRectFromPan(int xdiff, int ydiff) {
			Point bounds = getParent().getSize();
			boolean xModified = false;
			boolean yModified = false;

			int xOld = zoomRect.x;
			zoomRect.x += xdiff;
			if (zoomRect.x > (bounds.x - zoomRect.width - BORDER_PADDING - 1)) {
				zoomRect.x = bounds.x - zoomRect.width - BORDER_PADDING - 1;
			} else if (zoomRect.x < BORDER_PADDING) {
				zoomRect.x = BORDER_PADDING;
			}
			xModified = xOld != zoomRect.x;

			int yOld = zoomRect.y;
			zoomRect.y += ydiff;
			if (zoomRect.y < BORDER_PADDING) {
				zoomRect.y = BORDER_PADDING;
			} else if (zoomRect.y > (bounds.y - zoomRect.height - BORDER_PADDING - 1)) {
				zoomRect.y = bounds.y - zoomRect.height - BORDER_PADDING - 1;
			}
			yModified = yOld != zoomRect.y;

			if (xModified || yModified) {
				updateChartFromZoomRect(xModified, yModified);
				chartCanvas.redrawChart();
			}
		}

		private void updateChartFromZoomRect(boolean updateXRange, boolean updateYRange) {
			Rectangle zoomCanvasBounds = new Rectangle(0, 0, getParent().getSize().x, getParent().getSize().y);
			Rectangle totalBounds = chartCanvas.getBounds();

			if (updateXRange) {
				double ratio = getVisibilityRatio(zoomRect.x - BORDER_PADDING, zoomCanvasBounds.x,
						zoomCanvasBounds.width - BORDER_PADDING);
				int start = getPixelLocation(ratio, totalBounds.width, 0);

				ratio = getVisibilityRatio(zoomRect.x + zoomRect.width + BORDER_PADDING + 1, zoomCanvasBounds.width,
						zoomCanvasBounds.width - BORDER_PADDING);
				int end = getPixelLocation(ratio, totalBounds.width, totalBounds.width);

				SubdividedQuantityRange xAxis = new SubdividedQuantityRange(chartRange.getStart(), chartRange.getEnd(),
						totalBounds.width, 1);
				chart.setVisibleRange(xAxis.getQuantityAtPixel(start), xAxis.getQuantityAtPixel(end));
				lastChartZoomedRange = chart.getVisibleRange();
			}
			if (updateYRange) {
				double ratio = getVisibilityRatio(zoomRect.y - BORDER_PADDING, 0,
						zoomCanvasBounds.height - BORDER_PADDING);
				int top = getPixelLocation(ratio, totalBounds.height, 0);

				Point p = ((ScrolledComposite) chartCanvas.getParent()).getOrigin();
				p.y = top;

				if (textCanvas != null) {
					textCanvas.syncScroll(p);
				}
				chartCanvas.syncScroll(p);
			}
		}

		class Painter implements PaintListener {
			@Override
			public void paintControl(PaintEvent e) {

				Rectangle backgroundRect = new Rectangle(0, 0, getParent().getSize().x, getParent().getSize().y);
				GC gc = e.gc;

				gc.setBackground(Palette.PF_BLACK_400.getSWTColor());
				gc.fillRectangle(backgroundRect);
				gc.setForeground(Palette.PF_BLACK_900.getSWTColor());
				gc.drawRectangle(0, 0, backgroundRect.width - 1, backgroundRect.height - 1);

				updateZoomRectFromChart();

				gc.setBackground(Palette.PF_BLACK_100.getSWTColor());
				gc.fillRectangle(zoomRect);
				gc.setForeground(Palette.PF_BLACK_900.getSWTColor());
				gc.drawRectangle(zoomRect);
			}
		}

		private void updateZoomRectFromChart() {
			Rectangle zoomCanvasBounds = new Rectangle(0, 0, getParent().getSize().x, getParent().getSize().y);
			IRange<IQuantity> zoomedRange = chart.getVisibleRange();
			IQuantity visibleWidth = chartRange.getExtent();
			double visibleHeight = chartCanvas.getParent().getBounds().height;
			Rectangle totalBounds = chartCanvas.getBounds();

			if (zoomRect == null) {
				zoomRect = new Rectangle(0, 0, 0, 0);
			}
			if (!chart.getVisibleRange().equals(lastChartZoomedRange)) {
				double ratio = getVisibilityRatio(zoomedRange.getStart(), chartRange.getStart(), visibleWidth);
				int start = getPixelLocation(ratio, zoomCanvasBounds.width, 0);

				ratio = getVisibilityRatio(zoomedRange.getEnd(), chartRange.getEnd(), visibleWidth);
				int end = getPixelLocation(ratio, zoomCanvasBounds.width, zoomCanvasBounds.width);

				zoomRect.x = start + BORDER_PADDING;
				zoomRect.width = end - start - 2 * BORDER_PADDING - 1;
				lastChartZoomedRange = chart.getVisibleRange();
			}
			double ratio = getVisibilityRatio(0, totalBounds.y, totalBounds.height);
			int top = getPixelLocation(ratio, zoomCanvasBounds.height, 0);

			ratio = getVisibilityRatio(visibleHeight, totalBounds.height + totalBounds.y, totalBounds.height);
			int bottom = getPixelLocation(ratio, zoomCanvasBounds.height, zoomCanvasBounds.height);

			zoomRect.y = top + BORDER_PADDING;
			zoomRect.height = bottom - top - 2 * BORDER_PADDING - 1;

		}

		private double getVisibilityRatio(double visibleBound, double borderBound, double totalLength) {
			double diff = visibleBound - borderBound;
			return diff / totalLength;
		}

		private double getVisibilityRatio(IQuantity visibleBound, IQuantity borderBound, IQuantity totalLength) {
			IQuantity diff = visibleBound.subtract(borderBound);
			return diff.ratioTo(totalLength);
		}

		private int getPixelLocation(double visiblityRatio, int totalLength, int offset) {
			return offset + (int) (visiblityRatio * totalLength);
		}
	}
}
