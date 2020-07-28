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
package org.openjdk.jmc.ui.charts;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.QuantitiesToolkit;
import org.openjdk.jmc.common.unit.QuantityRange;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.ui.charts.IChartInfoVisitor.ITick;
import org.openjdk.jmc.ui.misc.ChartButtonGroup;
import org.openjdk.jmc.ui.misc.ChartControlBar;
import org.openjdk.jmc.ui.misc.TimelineCanvas;
import org.openjdk.jmc.ui.misc.PatternFly.Palette;

public class XYChart {
	private static final String ELLIPSIS = "..."; //$NON-NLS-1$
	private static final Color SELECTION_COLOR = new Color(255, 255, 255, 220);
	private static final Color RANGE_INDICATION_COLOR = new Color(255, 60, 20);
	private static final int BASE_ZOOM_LEVEL = 100;
	private static final int RANGE_INDICATOR_HEIGHT = 7;
	private final IQuantity start;
	private final IQuantity end;
	private IQuantity rangeDuration;
	private IXDataRenderer rendererRoot;
	private IRenderedRow rendererResult;
	private final int xOffset;
	private int yOffset = 35;
	private final int bucketWidth;
	// FIXME: Use bucketWidth * ticksPerBucket instead of hardcoded value?
//	private final int ticksPerBucket = 4;

	private IQuantity currentStart;
	private IQuantity currentEnd;

	private final Set<Object> selectedRows = new HashSet<>();
	private int axisWidth;
	private int rowColorCounter;
	private IQuantity selectionStart;
	private IQuantity selectionEnd;
	private SubdividedQuantityRange xBucketRange;
	private SubdividedQuantityRange xTickRange;

	// JFR Threads Page
	private static final double ZOOM_PAN_FACTOR = 0.05;
	private static final int ZOOM_PAN_MODIFIER = 2;
	private double zoomPanPower = ZOOM_PAN_FACTOR / ZOOM_PAN_MODIFIER;
	private double currentZoom;
	private int zoomSteps;
	private ChartButtonGroup buttonGroup;
	private ChartControlBar controlBar;
	private Stack<Integer> modifiedSteps;
	private TimelineCanvas timelineCanvas;
	private int longestCharWidth = 0;

	public XYChart(IRange<IQuantity> range, IXDataRenderer rendererRoot) {
		this(range.getStart(), range.getEnd(), rendererRoot);
	}

	public XYChart(IRange<IQuantity> range, IXDataRenderer rendererRoot, int xOffset) {
		this(range.getStart(), range.getEnd(), rendererRoot, xOffset);
	}

	// JFR Threads Page
	public XYChart(IRange<IQuantity> range, IXDataRenderer rendererRoot, int xOffset, Integer yOffset,
			TimelineCanvas timelineCanvas, ChartControlBar controlBar, ChartButtonGroup buttonGroup) {
		this(range.getStart(), range.getEnd(), rendererRoot, xOffset);
		this.yOffset = yOffset;
		this.timelineCanvas = timelineCanvas;
		this.controlBar = controlBar;
		this.buttonGroup = buttonGroup;
		this.rangeDuration = range.getExtent();
		this.currentZoom = BASE_ZOOM_LEVEL;
		this.isZoomCalculated = false;
	}

	public XYChart(IRange<IQuantity> range, IXDataRenderer rendererRoot, int xOffset, int bucketWidth) {
		this(range.getStart(), range.getEnd(), rendererRoot, xOffset, bucketWidth);
	}

	public XYChart(IQuantity start, IQuantity end, IXDataRenderer rendererRoot) {
		this(start, end, rendererRoot, 60);
	}

	public XYChart(IQuantity start, IQuantity end, IXDataRenderer rendererRoot, int xOffset) {
		this(start, end, rendererRoot, xOffset, 25);
	}

	public XYChart(IQuantity start, IQuantity end, IXDataRenderer rendererRoot, int xOffset, int bucketWidth) {
		this.rendererRoot = rendererRoot;
		// Start value must always be strictly less than end
		assert (start.compareTo(end) < 0);
		this.currentStart = start;
		this.start = start;
		this.currentEnd = end;
		this.end = end;
		this.xOffset = xOffset;
		this.bucketWidth = bucketWidth;
	}

	public void setRendererRoot(IXDataRenderer rendererRoot) {
		clearSelection();
		this.rendererRoot = rendererRoot;
		longestCharWidth = 0;
	}

	public IXDataRenderer getRendererRoot() {
		return rendererRoot;
	}

	public Object[] getSelectedRows() {
		return selectedRows.toArray(new Object[selectedRows.size()]);
	}

	public IQuantity getSelectionStart() {
		return selectionStart;
	}

	public IQuantity getSelectionEnd() {
		return selectionEnd;
	}

	public IRange<IQuantity> getSelectionRange() {
		return (selectionStart != null) && (selectionEnd != null)
				? QuantityRange.createWithEnd(selectionStart, selectionEnd) : null;
	}

	public void renderChart(Graphics2D context, int width, int height) {
		if (width > xOffset && height > yOffset) {
			axisWidth = width - xOffset;
			// FIXME: xBucketRange and xTickRange should be more related, so that each tick is typically an integer number of buckets (or possibly 2.5 buckets).
			xBucketRange = new SubdividedQuantityRange(currentStart, currentEnd, axisWidth, bucketWidth);
			// FIXME: Use bucketWidth * ticksPerBucket instead of hardcoded value?
			xTickRange = new SubdividedQuantityRange(currentStart, currentEnd, axisWidth, 100);
			AffineTransform oldTransform = context.getTransform();
			context.translate(xOffset, 0);
			doRenderChart(context, height - yOffset);
			context.setTransform(oldTransform);
		}
	}

	public void renderTextCanvasText(Graphics2D context, int width, int height) {
		AffineTransform oldTransform = context.getTransform();
		axisWidth = width - xOffset;
		doRenderTextCanvasText(context, height);
		context.setTransform(oldTransform);
	}

	public void renderText(Graphics2D context, int width, int height) {
		if (width > xOffset && height > yOffset) {
			axisWidth = xOffset;
			AffineTransform oldTransform = context.getTransform();
			doRenderText(context);
			context.setTransform(oldTransform);
			axisWidth = width - xOffset;
		}
	}

	private void renderRangeIndication(Graphics2D context, int rangeIndicatorY) {
		// FIXME: Extract the needed functionality from SubdividedQuantityRange
		SubdividedQuantityRange fullRangeAxis = new SubdividedQuantityRange(start, end, axisWidth, 25);
		int x1 = (int) fullRangeAxis.getPixel(currentStart);
		int x2 = (int) Math.ceil(fullRangeAxis.getPixel(currentEnd));

		if (timelineCanvas != null) {
			timelineCanvas.renderRangeIndicator(x1, x2);
			updateZoomPanIndicator();
		} else {
			context.setPaint(RANGE_INDICATION_COLOR);
			context.fillRect(x1, rangeIndicatorY, x2 - x1, RANGE_INDICATOR_HEIGHT);
			context.setPaint(Color.DARK_GRAY);
			context.drawRect(0, rangeIndicatorY, axisWidth - 1, RANGE_INDICATOR_HEIGHT);
		}
	}

	public void updateZoomPanIndicator() {
		if (buttonGroup != null) {
			buttonGroup.updateZoomPanIndicator();
		}
	}

	private IRenderedRow getRendererResult(Graphics2D context, int axisHeight) {
		if (xBucketRange == null) {
			xBucketRange = getXBucketRange();
		}
		return rendererRoot.render(context, xBucketRange, axisHeight);
	}

	private SubdividedQuantityRange getXBucketRange() {
		return new SubdividedQuantityRange(currentStart, currentEnd, axisWidth, bucketWidth);
	}

	private void doRenderChart(Graphics2D context, int axisHeight) {
		rowColorCounter = 0;
		context.setPaint(Color.LIGHT_GRAY);
		AWTChartToolkit.drawGrid(context, xTickRange, axisHeight, false);
		// Attempt to make graphs so low they cover the axis show by drawing the full axis first ...
		context.setPaint(Color.BLACK);
		if (timelineCanvas != null) {
			timelineCanvas.setXTickRange(xTickRange);
		} else {
			AWTChartToolkit.drawAxis(context, xTickRange, axisHeight - 1, false, 1 - xOffset, false);
		}
		// ... then the graph ...
		rendererResult = getRendererResult(context, axisHeight);
		AffineTransform oldTransform = context.getTransform();

		context.setTransform(oldTransform);
		if (!selectedRows.isEmpty()) {
			renderSelectionChart(context, rendererResult);
			context.setTransform(oldTransform);
		}
		// .. and finally a semitransparent axis line again.
		context.setPaint(new Color(0, 0, 0, 64));
		context.drawLine(0, axisHeight - 1, axisWidth - 1, axisHeight - 1);
		renderRangeIndication(context, axisHeight + 25);
	}

	private void doRenderText(Graphics2D context) {
		AffineTransform oldTransform = context.getTransform();
		rowColorCounter = -1;
		renderText(context, rendererResult);
		context.setTransform(oldTransform);
	}

	private void doRenderTextCanvasText(Graphics2D context, int height) {
		if (rendererResult == null) {
			rendererResult = getRendererResult(context, height - yOffset);
		}
		AffineTransform oldTransform = context.getTransform();
		rowColorCounter = 0;
		renderText(context, rendererResult);
		context.setTransform(oldTransform);
		if (!selectedRows.isEmpty()) {
			renderSelectionText(context, rendererResult);
			context.setTransform(oldTransform);
		}
	}

	private void renderSelectionText(Graphics2D context, IRenderedRow row) {
		if (selectedRows.contains(row.getPayload())) {
			if (row.getHeight() != rendererResult.getHeight()) {
				Color highlight = new Color(0, 206, 209, 20);
				context.setColor(highlight);
				context.fillRect(0, 0, axisWidth, row.getHeight());
			} else {
				selectedRows.clear();
			}
		} else {
			List<IRenderedRow> subdivision = row.getNestedRows();
			if (subdivision.isEmpty()) {
				dimRect(context, 0, axisWidth, row.getHeight());
			} else {
				for (IRenderedRow nestedRow : row.getNestedRows()) {
					renderSelectionText(context, nestedRow);
				}
				return;
			}
		}
		context.translate(0, row.getHeight());
	}

	private void renderSelectionChart(Graphics2D context, IRenderedRow row) {
		if (selectedRows.contains(row.getPayload())) {
			renderSelection(context, xBucketRange, row.getHeight());
		} else {
			List<IRenderedRow> subdivision = row.getNestedRows();
			if (subdivision.isEmpty()) {
				dimRect(context, 0, axisWidth, row.getHeight());
			} else {
				for (IRenderedRow nestedRow : row.getNestedRows()) {
					renderSelectionChart(context, nestedRow);
				}
				return;
			}
		}
		context.translate(0, row.getHeight());
	}

	// Paint the background of every-other row in a slightly different shade
	// to better differentiate the thread lanes from one another
	private void paintRowBackground(Graphics2D context, int height) {
		if (rowColorCounter >= 0) {
			if (rowColorCounter % 2 == 0) {
				context.setColor(Palette.PF_BLACK_100.getAWTColor());
			} else {
				context.setColor(Palette.PF_BLACK_200.getAWTColor());
			}
			context.fillRect(0, 0, axisWidth, height);
			rowColorCounter++;
		}
	}

	private void renderText(Graphics2D context, IRenderedRow row) {
		String text = row.getName();
		int height = row.getHeight();
		if (height >= context.getFontMetrics().getHeight()) {
			if (text != null) {
				paintRowBackground(context, row.getHeight());
				context.setColor(Color.BLACK);
				context.drawLine(0, height - 1, axisWidth - 15, height - 1);
				int y = ((height - context.getFontMetrics().getHeight()) / 2) + context.getFontMetrics().getAscent();
				int charsWidth = context.getFontMetrics().charsWidth(text.toCharArray(), 0, text.length());
				if (charsWidth > longestCharWidth) {
					longestCharWidth = charsWidth;
				}
				if (xOffset > 0 && charsWidth > xOffset) {
					float fitRatio = ((float) xOffset) / (charsWidth
							+ context.getFontMetrics().charsWidth(ELLIPSIS.toCharArray(), 0, ELLIPSIS.length()));
					text = text.substring(0, ((int) (text.length() * fitRatio)) - 1) + ELLIPSIS;
				}
				context.drawString(text, 2, y);
			} else {
				List<IRenderedRow> subdivision = row.getNestedRows();
				if (!subdivision.isEmpty()) {
					for (IRenderedRow nestedRow : row.getNestedRows()) {
						renderText(context, nestedRow);
					}
					return;
				}
			}
		}
		context.translate(0, height);
	}

	/**
	 * Get the longest character width of a thread name to be rendered
	 * 
	 * @return the character width of longest thread name
	 */
	public int getLongestCharWidth() {
		return longestCharWidth;
	}

	/**
	 * Pan the view.
	 *
	 * @param rightPercent
	 * @return true if the bounds changed. That is, if a redraw is required.
	 */
	public boolean pan(int rightPercent) {
		if (rangeDuration != null) {
			return panRange(Integer.signum(rightPercent));
		}
		if (xBucketRange != null) {
			IQuantity oldStart = currentStart;
			IQuantity oldEnd = currentEnd;
			if (rightPercent > 0) {
				currentEnd = QuantitiesToolkit
						.min(xBucketRange.getQuantityAtPixel(axisWidth + axisWidth * rightPercent / 100), end);
				currentStart = QuantitiesToolkit
						.max(xBucketRange.getQuantityAtPixel(xBucketRange.getPixel(currentEnd) - axisWidth), start);
			} else if (rightPercent < 0) {
				currentStart = QuantitiesToolkit.max(xBucketRange.getQuantityAtPixel(axisWidth * rightPercent / 100),
						start);
				currentEnd = QuantitiesToolkit
						.min(xBucketRange.getQuantityAtPixel(xBucketRange.getPixel(currentStart) + axisWidth), end);
			}
			return (currentStart.compareTo(oldStart) != 0) || (currentEnd.compareTo(oldEnd) != 0);
		}
		// Return true since a redraw forces creation of xBucketRange.
		return true;
	}

	/**
	 * Pan the view at a rate relative the current zoom level.
	 * 
	 * @param panDirection
	 *            -1 to pan left, 1 to pan right
	 * @return true if the chart needs to be redrawn
	 */
	public boolean panRange(int panDirection) {
		if (zoomSteps == 0 || panDirection == 0 || (currentStart.compareTo(start) == 0 && panDirection == -1)
				|| (currentEnd.compareTo(end) == 0 && panDirection == 1)) {
			return false;
		}

		IQuantity panDiff = rangeDuration.multiply(panDirection * zoomPanPower);
		IQuantity newStart = currentStart.in(UnitLookup.EPOCH_NS).add(panDiff);
		IQuantity newEnd = currentEnd.in(UnitLookup.EPOCH_NS).add(panDiff);

		// if panning would flow over the recording range start or end time,
		// calculate the difference and add it so the other side.
		if (newStart.compareTo(start) < 0) {
			IQuantity diff = start.subtract(newStart);
			newStart = start;
			newEnd = newEnd.add(diff);
		} else if (newEnd.compareTo(end) > 0) {
			IQuantity diff = newEnd.subtract(end);
			newStart = newStart.add(diff);
			newEnd = end;
		}
		currentStart = newStart;
		currentEnd = newEnd;
		controlBar.setStartTime(currentStart);
		controlBar.setEndTime(currentEnd);
		isZoomCalculated = true;
		return true;
	}

	/**
	 * Zoom the view.
	 *
	 * @param zoomInSteps
	 * @return true if the bounds changed. That is, if a redraw is required.
	 */
	public boolean zoom(int zoomInSteps) {
		if (rangeDuration != null) {
			return zoomRange(zoomInSteps);
		}
		return zoomXAxis(axisWidth / 2, zoomInSteps);
	}

	/**
	 * Zoom the view.
	 *
	 * @param x
	 * @param zoomInSteps
	 * @return true if the bounds changed. That is, if a redraw is required.
	 */
	public boolean zoom(int x, int zoomInSteps) {
		return zoomXAxis(x - xOffset, zoomInSteps);
	}

	// Default zoom mechanics
	private boolean zoomXAxis(int x, int zoomInSteps) {
		if (xBucketRange == null) {
			// Return true since a redraw forces creation of xBucketRange.
			return true;
		}
		if ((x > 0) && (x < axisWidth)) {
			IQuantity oldStart = currentStart;
			IQuantity oldEnd = currentEnd;
			// Absolute value of zoomFactor must be less than 1. Currently it ranges between -0.5 and 0.5.
			double zoomFactor = Math.atan(zoomInSteps) / Math.PI;
			int newStart = (int) (zoomFactor * x);
			int newEnd = (int) (axisWidth * (1 - zoomFactor)) + newStart;
			SubdividedQuantityRange xAxis = new SubdividedQuantityRange(currentStart, currentEnd, axisWidth, 1);
			setVisibleRange(xAxis.getQuantityAtPixel(newStart), xAxis.getQuantityAtPixel(newEnd));
			return (currentStart.compareTo(oldStart) != 0) || (currentEnd.compareTo(oldEnd) != 0);
		}
		return false;
	}

	/**
	 * Zoom to a specific step count
	 * 
	 * @param zoomToStep
	 *            the desired end zoom step amount
	 * @return true if a redraw is required as a result of a successful zoom
	 */
	public boolean zoomToStep(int zoomToStep) {
		if (zoomToStep == 0) {
			resetTimeline();
			return true;
		} else {
			return zoomRange(zoomToStep - zoomSteps);
		}
	}

	/**
	 * Zoom based on a percentage of the recording range
	 * 
	 * @param zoomInSteps
	 *            the amount of desired steps to take
	 * @return true if a redraw is required as a result of a successful zoom
	 */
	private boolean zoomRange(int steps) {
		if (steps == 0) {
			return false;
		} else if (steps > 0) {
			zoomIn(steps);
		} else {
			zoomOut(steps);
		}
		return true;
	}

	/**
	 * Zoom into the chart at a rate of 5% of the overall recording range at each step. If the chart
	 * is zoomed in far enough such that one more step at 5% is not possible, the zoom power is
	 * halved and the zoom will proceed. <br>
	 * Every time the zoom power is halved, the instigating step value is pushed onto the
	 * modifiedSteps stack. This stack is consulted on zoom out events in order to ensure the chart
	 * zooms out the same way it was zoomed in.
	 */
	private void zoomIn(int steps) {
		do {
			IQuantity zoomDiff = rangeDuration.multiply(zoomPanPower);
			IQuantity newStart = currentStart.in(UnitLookup.EPOCH_NS).add(zoomDiff);
			IQuantity newEnd = currentEnd.in(UnitLookup.EPOCH_NS).subtract(zoomDiff);
			if (newStart.compareTo(newEnd) >= 0) { // adjust the zoom factor
				if (modifiedSteps == null) {
					modifiedSteps = new Stack<Integer>();
				}
				modifiedSteps.push(zoomSteps);
				zoomPanPower = zoomPanPower / ZOOM_PAN_MODIFIER;
				zoomDiff = rangeDuration.multiply(zoomPanPower);
				newStart = currentStart.in(UnitLookup.EPOCH_NS).add(zoomDiff);
				newEnd = currentEnd.in(UnitLookup.EPOCH_NS).subtract(zoomDiff);
			}
			currentZoom = currentZoom + (zoomPanPower * ZOOM_PAN_MODIFIER * 100);
			isZoomCalculated = true;
			zoomSteps++;
			setVisibleRange(newStart, newEnd);
			steps--;
		} while (steps > 0);
	}

	/**
	 * Zoom out of the chart at a rate equal to the how the chart was zoomed in.
	 */
	private void zoomOut(int steps) {
		do {
			if (modifiedSteps != null && modifiedSteps.size() > 0 && modifiedSteps.peek() == zoomSteps) {
				modifiedSteps.pop();
				zoomPanPower = zoomPanPower * ZOOM_PAN_MODIFIER;
			}
			IQuantity zoomDiff = rangeDuration.multiply(zoomPanPower);
			IQuantity newStart = currentStart.in(UnitLookup.EPOCH_NS).subtract(zoomDiff);
			IQuantity newEnd = currentEnd.in(UnitLookup.EPOCH_NS).add(zoomDiff);

			// if zooming out would flow over the recording range start or end time,
			// calculate the difference and add it to the other side.
			if (newStart.compareTo(start) < 0) {
				IQuantity diff = start.subtract(newStart);
				newStart = start;
				newEnd = newEnd.add(diff);
			} else if (newEnd.compareTo(end) > 0) {
				IQuantity diff = newEnd.subtract(end);
				newStart = newStart.subtract(diff);
				newEnd = end;
			}
			currentZoom = currentZoom - (zoomPanPower * ZOOM_PAN_MODIFIER * 100);
			if (currentZoom < BASE_ZOOM_LEVEL) {
				currentZoom = BASE_ZOOM_LEVEL;
			}
			isZoomCalculated = true;
			zoomSteps--;
			setVisibleRange(newStart, newEnd);
			steps++;
		} while (steps < 0);
	}

	// need to check from ChartAndPopupTableUI if not using the OG start/end position,
	// will have to calculate the new zoom level
	public void resetZoomFactor() {
		zoomSteps = 0;
		zoomPanPower = ZOOM_PAN_FACTOR / ZOOM_PAN_MODIFIER;
		currentZoom = BASE_ZOOM_LEVEL;
		modifiedSteps = new Stack<Integer>();
	}

	/**
	 * Reset the visible range to be the recording range, and reset the zoom-related objects
	 */
	public void resetTimeline() {
		resetZoomFactor();
		setVisibleRange(start, end);
	}

	private void selectionZoom(IQuantity newStart, IQuantity newEnd) {
		double percentage = calculateZoom(newStart, newEnd);
		zoomSteps = calculateZoomSteps(percentage);
		currentZoom = BASE_ZOOM_LEVEL + (percentage * 100);
	}

	/**
	 * When a drag-select zoom occurs, use the new range value to determine how many steps have been
	 * taken, and adjust zoomSteps and zoomPower accordingly
	 */
	private double calculateZoom(IQuantity newStart, IQuantity newEnd) {
		// calculate the new visible range, and it's percentage of the total range
		IQuantity newRange = newEnd.in(UnitLookup.EPOCH_NS).subtract(newStart.in(UnitLookup.EPOCH_NS));
		return 1 - (newRange.longValue() / (double) rangeDuration.in(UnitLookup.NANOSECOND).longValue());
	}

	/**
	 * Calculate the number of steps required to achieve the passed zoom percentage
	 */
	private int calculateZoomSteps(double percentage) {
		int steps = (int) Math.floor(percentage / ZOOM_PAN_FACTOR);
		double tempPercent = steps * ZOOM_PAN_FACTOR;

		if (tempPercent < percentage) {
			if (percentage > 1 - ZOOM_PAN_FACTOR) {
				double factor = ZOOM_PAN_FACTOR;
				do {
					factor = factor / ZOOM_PAN_MODIFIER;
					tempPercent = tempPercent + factor;
					if (modifiedSteps == null) {
						modifiedSteps = new Stack<Integer>();
					}
					if (modifiedSteps.size() == 0 || modifiedSteps.peek() < steps) {
						modifiedSteps.push(steps);
					}
					steps++;
				} while (tempPercent <= percentage);
				zoomPanPower = factor / ZOOM_PAN_MODIFIER;
			} else {
				steps++;
			}
		}
		return steps;
	}

	private boolean isZoomCalculated;
	private boolean isZoomPanDrag;

	public void setIsZoomPanDrag(boolean isZoomPanDrag) {
		this.isZoomPanDrag = isZoomPanDrag;
	}

	private boolean getIsZoomPanDrag() {
		return isZoomPanDrag;
	}

	public void setVisibleRange(IQuantity rangeStart, IQuantity rangeEnd) {
		if (rangeDuration != null && !isZoomCalculated && !getIsZoomPanDrag()) {
			selectionZoom(rangeStart, rangeEnd);
		}
		isZoomCalculated = false;
		rangeStart = QuantitiesToolkit.max(rangeStart, start);
		rangeEnd = QuantitiesToolkit.min(rangeEnd, end);
		if (rangeStart.compareTo(rangeEnd) < 0) {
			SubdividedQuantityRange testRange = new SubdividedQuantityRange(rangeStart, rangeEnd, 10000, 1);
			if (testRange.getQuantityAtPixel(0).compareTo(testRange.getQuantityAtPixel(1)) < 0) {
				currentStart = rangeStart;
				currentEnd = rangeEnd;
			} else {
				// Ensures that zoom out is always allowed
				currentStart = QuantitiesToolkit.min(rangeStart, currentStart);
				currentEnd = QuantitiesToolkit.max(rangeEnd, currentEnd);
			}
			if (controlBar != null) {
				controlBar.setStartTime(currentStart);
				controlBar.setEndTime(currentEnd);
			}
			rangeListeners.stream().forEach(l -> l.accept(getVisibleRange()));
		}
	}

	private List<Consumer<IRange<IQuantity>>> rangeListeners = new ArrayList<>();

	public void addVisibleRangeListener(Consumer<IRange<IQuantity>> rangeListener) {
		rangeListeners.add(rangeListener);
	}

	public IRange<IQuantity> getVisibleRange() {
		return (currentStart != null) && (currentEnd != null) ? QuantityRange.createWithEnd(currentStart, currentEnd)
				: null;
	}

	public void clearVisibleRange() {
		currentStart = start;
		currentEnd = end;
	}

	public boolean select(int x1, int x2, int y1, int y2, boolean clear) {
		int xStart = Math.min(x1, x2);
		int xEnd = Math.max(x1, x2);

		if (xBucketRange != null && (xEnd != xStart) && xEnd - xOffset >= 0) {
			return select(xBucketRange.getQuantityAtPixel(Math.max(0, xStart - xOffset)),
					xBucketRange.getQuantityAtPixel(xEnd - xOffset), y1, y2, clear);
		} else {
			return select(null, null, y1, y2, clear);
		}
	}

	public boolean select(IQuantity xStart, IQuantity xEnd, int y1, int y2, boolean clear) {
		if (xStart != null && xStart.compareTo(start) < 0) {
			xStart = start;
		}
		if (xEnd != null && xEnd.compareTo(end) > 0) {
			xEnd = end;
		}
		Set<Object> oldRows = null;
		if (QuantitiesToolkit.same(selectionStart, xStart) && QuantitiesToolkit.same(selectionEnd, xEnd)) {
			oldRows = new HashSet<>(selectedRows);
		}
		if (clear) {
			selectedRows.clear();
		}
		addSelectedRows(rendererResult, 0, Math.min(y1, y2), Math.max(y1, y2));
		selectionStart = xStart;
		selectionEnd = xEnd;
		return (oldRows == null) || !oldRows.equals(selectedRows);
	}

	public boolean clearSelection() {
		if ((selectionStart == null) && (selectionEnd == null) && selectedRows.isEmpty()) {
			return false;
		}
		selectedRows.clear();
		selectionStart = selectionEnd = null;
		return true;
	}

	private boolean addSelectedRows(IRenderedRow row, int yRowStart, int ySelectionStart, int ySelectionEnd) {
		List<IRenderedRow> subdivision = row.getNestedRows();
		if (subdivision.isEmpty()) {
			return addPayload(row);
		} else {
			boolean nestedHasPayload = false;
			for (IRenderedRow nestedRow : row.getNestedRows()) {
				int yRowEnd = yRowStart + nestedRow.getHeight();
				if (yRowStart > ySelectionEnd) {
					break;
				} else if (yRowEnd > ySelectionStart) {
					nestedHasPayload |= addSelectedRows(nestedRow, yRowStart, ySelectionStart, ySelectionEnd);
				}
				yRowStart = yRowEnd;
			}
			return nestedHasPayload || addPayload(row);
		}
	}

	private boolean addPayload(IRenderedRow row) {
		Object payload = row.getPayload();
		if (payload != null) {
			if (selectedRows.contains(payload)) { // ctrl+click deselection
				selectedRows.remove(payload);
			} else {
				selectedRows.add(payload);
			}
			return true;
		}
		return false;
	}

	private void renderSelection(Graphics2D context, SubdividedQuantityRange xRange, int height) {
		int selFrom = 0;
		int selTo = axisWidth;
		if (selectionStart != null && selectionEnd != null) {
			selFrom = (int) xRange.getPixel(selectionStart);
			// Removed "+ 1" for now to make the selection symmetrical with respect to chart highlights.
			selTo = (int) xRange.getPixel(selectionEnd);
		}
		// FIXME: Would like to show selection by graying out the other parts, can we do that?
//		if (selWidth > 0) {
//			context.setColor(Color.WHITE);
//			context.setXORMode(Color.BLACK);
//			Stroke oldStroke = context.getStroke();
//			context.setStroke(SELECTION_STROKE);
//			context.drawRect(selFrom, 0, selWidth, height);
//			context.setStroke(oldStroke);
//			context.setPaintMode();
//		}
		if (selFrom > 0) {
			dimRect(context, 0, selFrom, height);
			context.setColor(Color.BLACK);
			context.drawLine(selFrom, 0, selFrom, height);
		}
		if (selTo < axisWidth) {
			dimRect(context, selTo, axisWidth - selTo, height);
			context.setColor(Color.BLACK);
			context.drawLine(selTo, 0, selTo, height);
		}
	}

	private static void dimRect(Graphics2D context, int from, int width, int height) {
		context.setColor(SELECTION_COLOR);
		context.fillRect(from, 0, width, height);
	}

	/**
	 * Let the {@code visitor} visit the chart elements in the vicinity of {@code x} and {@code y}.
	 * The visitation should adhere to a basic front to back ordering, so that elements which
	 * <em>conceptually</em> are at the front should be visited first. Note that this has no direct
	 * link to the drawing order. Also, this doesn't dictate any particular order between elements
	 * that conceptually are at the same level. (Good practice is to visit elements from different
	 * sub charts in a consistent order. If the sub charts have some kind of fixed ordering, such as
	 * stacked line charts, this order from top to bottom seems most appropriate.)
	 *
	 * @param visitor
	 * @param x
	 * @param y
	 */
	public void infoAt(IChartInfoVisitor visitor, int x, int y) {
		if (rendererResult == null) {
			return;
		}
		final int height = rendererResult.getHeight();
		if (y < height) {
			rendererResult.infoAt(visitor, x - xOffset, y, new Point(xOffset, 0));
		} else {
			x -= xOffset;
			if (x >= 0) {
				// Snap to closest of ticks and buckets (useful even if no bar charts are shown).
				int tickIndex = xTickRange.getClosestSubdividerAtPixel(x);
				double tickX = xTickRange.getSubdividerPixel(tickIndex);
				int bucketIndex = xBucketRange.getClosestSubdividerAtPixel(x);
				double bucketX = xBucketRange.getSubdividerPixel(bucketIndex);
				if (Math.abs(x - bucketX) < Math.abs(x - tickX)) {
					visitor.visit(tickFor(xBucketRange, bucketIndex));
				} else {
					visitor.visit(tickFor(xTickRange, tickIndex));
				}
			}
		}
	}

	private ITick tickFor(final SubdividedQuantityRange xRange, final int index) {
		return new ITick() {
			@Override
			public IDisplayable getValue() {
				return xRange.getSubdivider(index);
			}

			@Override
			public Point2D getTarget() {
				return new Point(xOffset + (int) xRange.getSubdividerPixel(index), rendererResult.getHeight() - 1);
			}
		};
	}
}
