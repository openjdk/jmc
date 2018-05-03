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
package org.openjdk.jmc.greychart.impl;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import org.openjdk.jmc.ui.common.xydata.DataSeries;
import org.openjdk.jmc.ui.common.xydata.IXYData;

import org.openjdk.jmc.greychart.AbstractGreyChart;
import org.openjdk.jmc.greychart.AbstractSeriesPlotRenderer;
import org.openjdk.jmc.greychart.GreyChartPanel;
import org.openjdk.jmc.greychart.SeriesMetadataProvider;
import org.openjdk.jmc.greychart.XYGreyChart;
import org.openjdk.jmc.greychart.XYPlotRenderer;
import org.openjdk.jmc.greychart.YAxis;
import org.openjdk.jmc.greychart.util.ChartRenderingToolkit;

/**
 * The default line renderer.
 */
public class DefaultXYLineRenderer extends AbstractSeriesPlotRenderer implements XYPlotRenderer {
	private Stroke[] m_strokes;
	private boolean m_useClip = true;
	private final Rectangle m_clipRect = new Rectangle(1, 0, 0, 0);
	private boolean drawOnXAxis;
	private boolean extrapolateGraph = true;
	private Paint extrapolationPaint;
	private Stroke extrapolationStroke;
	protected volatile IXYData<? extends Number, ? extends Number> circledValue;

	private int m_circleDiameter = 5;
	private int m_singleValueMarkSize = 4;

	/**
	 * Constructor.
	 *
	 * @param owner
	 *            the owner of this PlotRenderer.
	 */
	public DefaultXYLineRenderer(XYGreyChart owner) {
		super(owner);
		setupDefaultExtrapolationStroke();
		setupDefaultExtrapolationPaint();
	}

	/**
	 * @see org.openjdk.jmc.greychart.ChartRenderer#render(Graphics2D, Rectangle, Rectangle)
	 * @throws ClassCastException
	 *             if not an XYGraph!
	 */
	@Override
	public void render(Graphics2D ctx, Rectangle targetArea, Rectangle fullgraphArea) {
		if (getOwner().getDataProvider() == null) {
			return;
		}
		setRenderedBounds(targetArea);
		if (targetArea.width <= 0 || targetArea.height <= 0) {
			return;
		}
		XYGreyChart xychart = (XYGreyChart) getOwner();
		Shape clip = ctx.getClip();

		AffineTransform origTrans = ctx.getTransform();
		ctx.translate(targetArea.x, targetArea.y);
		if (m_useClip) {
			m_clipRect.width = targetArea.width;
			m_clipRect.height = targetArea.height;
			ctx.setClip(m_clipRect);
		} else if (drawOnXAxis) {
			m_clipRect.width = targetArea.width;
			m_clipRect.height = targetArea.height + 1;
			ctx.setClip(m_clipRect);
		}

		drawXYChart(ctx, targetArea, (DefaultXYGreyChart) xychart);
		drawCircledValue(ctx, (DefaultXYGreyChart) xychart);

		if (GreyChartPanel.DEBUG) {
			ChartRenderingToolkit.markBoundary(ctx, 0, 0, targetArea.width, targetArea.height, Color.RED);
		}
		ctx.setTransform(origTrans);
		ctx.setClip(clip);
	}

	private void setupDefaultExtrapolationPaint() {
		BufferedImage bi = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
		Graphics2D big = bi.createGraphics();
		big.setColor(new Color(255, 255, 255));
		big.fillRect(0, 0, 5, 5);
		big.setColor(new Color(200, 200, 200));
		big.drawLine(0, 0, 5, 5);
		Rectangle rect = new Rectangle(0, 0, 5, 5);
		setExtrapolationPaint(new TexturePaint(bi, rect));
	}

	private void setupDefaultExtrapolationStroke() {
		setExtrapolationStroke(
				new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[] {3f, 2f}, 1f));
	}

	private void drawSingleValue(Graphics2D ctx, int x, int y, Color color) {
		int markOffset = getSingleValueMarkSize() / 2;
		Color old = ctx.getColor();
		ctx.setColor(color);
		ctx.drawLine(x - markOffset, y - markOffset, x + markOffset, y + markOffset);
		ctx.drawLine(x + markOffset, y - markOffset, x - markOffset, y + markOffset);
		ctx.setColor(old);
	}

	private int getSingleValueMarkSize() {
		return m_singleValueMarkSize;
	}

	private void drawCircledValue(Graphics2D ctx, DefaultXYGreyChart xychart) {
		IXYData<? extends Number, ? extends Number> val = circledValue;
		if ((val != null)) {
			int tmpX = (val.getX()).intValue();
			int tmpY = (val.getY()).intValue();
			int diameter = getCircleDiameter();
			Color old = ctx.getColor();
			ctx.setColor(getForeground());
			ctx.drawOval(tmpX - (diameter / 2), tmpY - (diameter / 2), diameter, diameter);
			ctx.setColor(old);
		}
	}

	private int getCircleDiameter() {
		return m_circleDiameter;
	}

	private void drawXYChart(Graphics2D ctx, Rectangle targetArea, DefaultXYGreyChart chart) {
		OptimizingProvider parentOptimizingProvider = chart.getOptimizingProvider();

		LongWorldToDeviceConverter xWorldToDevice = createXConverter(parentOptimizingProvider, chart, targetArea.width);
		SeriesMetadataProvider smdp = chart.getMetadataProvider();
		for (OptimizingProvider optimizingProvider : parentOptimizingProvider.getChildren()) {
			DataSeries<?> ds = optimizingProvider.getDataSeries();
			YAxis yAxis = smdp.getYAxis(ds);
			if (yAxis == null) {
				yAxis = chart.getYAxis()[0];
			}
			WorldToDeviceConverter yWorldToDevice = optimizingProvider.getYSampleToDeviceConverterFor(yAxis);
			drawSeries(ctx, smdp, yWorldToDevice, xWorldToDevice, optimizingProvider);
		}
	}

	private void drawSeries(
		Graphics2D ctx, SeriesMetadataProvider smdp, WorldToDeviceConverter yWorldToDevice,
		LongWorldToDeviceConverter xWorldToDevice, OptimizingProvider optimizingProvider) {
		Polygon polygon = optimizingProvider.getSamplesPolygon(xWorldToDevice, yWorldToDevice);
		DataSeries ds = optimizingProvider.getDataSeries();
		if (extrapolateGraph) {
			// Must be done before closing the polygon
			drawExtrapolatedData(ctx, polygon, smdp, ds, xWorldToDevice, yWorldToDevice);
		}
		if (smdp.getFill(ds)) {
			closePolygonToXAxis(polygon, yWorldToDevice.getDeviceCoordinate(0));
		}
		drawData(ctx, polygon, smdp, ds, xWorldToDevice, yWorldToDevice);
	}

	private LongWorldToDeviceConverter createXConverter(
		OptimizingProvider optimizingProvider, AbstractGreyChart chart, int width) {
		return new LongWorldToDeviceConverter(0, width, optimizingProvider.getMinX(), optimizingProvider.getMaxX());
	}

	private void drawExtrapolatedData(
		Graphics2D ctx, Polygon samplesPolygon, SeriesMetadataProvider smdp, DataSeries ds,
		LongWorldToDeviceConverter xWorldToDeviceConverter, WorldToDeviceConverter yWorldToDeviceConverter) {
		float right = xWorldToDeviceConverter.getDeviceWidth();
		float bottom = yWorldToDeviceConverter.getDeviceCoordinate(0);

		if (samplesPolygon.npoints > 0) {
			int firstY = samplesPolygon.ypoints[0];
			int firstX = samplesPolygon.xpoints[0];
			int lastY = samplesPolygon.ypoints[samplesPolygon.npoints - 1];
			int lastX = samplesPolygon.xpoints[samplesPolygon.npoints - 1];

			Stroke oldStroke = ctx.getStroke();
			Color oldColor = ctx.getColor();
			Color strokeColor = smdp.getTopColor(ds) == null ? smdp.getLineColor(ds) : smdp.getTopColor(ds);

			if (smdp.getFill(ds)) {
				ctx.setPaint(extrapolationPaint);
				ctx.fillRect(0, firstY, firstX, (int) bottom - firstY);
				ctx.fillRect(lastX, lastY, (int) right - lastX, (int) bottom - lastY);
			}

			ctx.setStroke(extrapolationStroke);
			ctx.setColor(strokeColor);
			ctx.drawLine(0, firstY, firstX, firstY);
			ctx.drawLine(lastX, lastY, (int) right, lastY);
			ctx.setStroke(oldStroke);
			ctx.setColor(oldColor);
		}
	}

	private void drawData(
		Graphics2D ctx, Polygon polygon, SeriesMetadataProvider smdp, DataSeries ds,
		LongWorldToDeviceConverter xWorldToDeviceConverter, WorldToDeviceConverter yWorldToDeviceConverter) {

		if (polygon.npoints == 1) {
			drawSingleValue(ctx, polygon.xpoints[0], polygon.ypoints[0], smdp.getLineColor(ds));
		}

		if (polygon.npoints > 0) {
			if (smdp.getFill(ds)) {
				float top = 0;
				float bottom = yWorldToDeviceConverter.getDeviceCoordinate(0);
				ctx.setPaint(new GradientPaint(0, top, smdp.getTopColor(ds), 0, bottom, smdp.getBottomColor(ds)));
				ctx.fillPolygon(polygon);
				if (!smdp.getDrawLine(ds)) {
					// We must draw the line using the fill color even when we don't want a "visible" line.
					// Fill polygon only paints the area inside the line, not the line itself, and we want to
					// paint the area covered by the line too.
					// The only occasion when we don't draw the line at all is when neither fill nor draw
					// line is true.
					ctx.drawPolyline(polygon.xpoints, polygon.ypoints, polygon.npoints);
				}
				ctx.setPaint(null);
			}

			if (smdp.getDrawLine(ds)) {
				ctx.setColor(smdp.getLineColor(ds));
				ctx.drawPolyline(polygon.xpoints, polygon.ypoints, polygon.npoints);
			}
		}
	}

	private void closePolygonToXAxis(Polygon polygon, int xAxisYValue) {
		if (polygon.npoints > 0) {
			polygon.addPoint(polygon.xpoints[polygon.npoints - 1], xAxisYValue);
			polygon.addPoint(polygon.xpoints[0], xAxisYValue);
			polygon.addPoint(polygon.xpoints[0], polygon.ypoints[0]);
		}
	}

	public double getInterpolatedValue(double x, double startX, double endX, double startY, double endY) {
		double x_delta = endX - startX;
		double y_delta = endY - startY;
		if (x_delta > 0) {
			return (x - startX) * y_delta / x_delta + startY;
		} else {
			return 0.0;
		}
	}

	@Override
	public Dimension getPreferredDimensions(Graphics2D ctx, Rectangle totalDrawingArea) {
		// Greedy little thing. ;)
		return new Dimension(totalDrawingArea.width, totalDrawingArea.height);
	}

	/**
	 * A limited version of setUseClip that also uses a clip, but expands it by 1 pixel in the
	 * vertical direction, allowing the line-renderer to draw on the horizontal axis. The isUseClip
	 * method must be false for this to work.
	 *
	 * @param b
	 *            Whether to expand the clip by 1 pixel or not.
	 */
	public void setDrawOnXAxis(boolean b) {
		drawOnXAxis = b;
	}

	/**
	 * Returns whether the line renderer is allowed to draw on the horizontal axis or not.
	 *
	 * @return
	 */
	public boolean isDrawOnXAxis() {
		return drawOnXAxis;
	}

	/**
	 * @return true if the plot is clipped to only paint within the plot region. If false the
	 *         plotter is free to paint on the axis line.
	 */
	public boolean isUseClip() {
		return m_useClip;
	}

	/**
	 * @param useClip
	 *            set to true if you want the plot to be clipped to only paint within the plot
	 *            region. If false the plotter is free to paint on the axis line, but may render
	 *            faster.
	 */
	public void setUseClip(boolean useClip) {
		m_useClip = useClip;
	}

	@Override
	public Stroke getSeriesStroke(int index) {
		if (m_strokes == null) {
			return null;
		}
		return m_strokes[index % m_strokes.length];
	}

	@Override
	public void setSeriesStrokes(Stroke[] strokes) {
		m_strokes = strokes;
	}

	public void circleValue(IXYData<? extends Number, ? extends Number> point) {
		circledValue = point;
	}

	public void setSingleValueMarkSize(int markSize) {
		m_singleValueMarkSize = markSize;
	}

	public void setCircleDiameter(int circleDiameter) {
		m_circleDiameter = circleDiameter;
	}

	public void setExtrapolateMissingData(boolean extrapolate) {
		extrapolateGraph = extrapolate;
	}

	public boolean getExtrapolateMissnigData() {
		return extrapolateGraph;
	}

	public void setExtrapolationPaint(Paint paint) {
		extrapolationPaint = paint;
	}

	public Paint getExtrapolationPaint() {
		return extrapolationPaint;
	}

	public void setExtrapolationStroke(Stroke stroke) {
		extrapolationStroke = stroke;
	}

	public Stroke getExtrapolationStroke() {
		return extrapolationStroke;
	}
}
