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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openjdk.jmc.greychart.AbstractGreyChart;
import org.openjdk.jmc.greychart.AxisListener;
import org.openjdk.jmc.greychart.ChartChangeEvent;
import org.openjdk.jmc.greychart.FontAndColors;
import org.openjdk.jmc.greychart.XAxis;
import org.openjdk.jmc.greychart.XYGreyChart;
import org.openjdk.jmc.greychart.YAxis;
import org.openjdk.jmc.greychart.YAxis.Position;
import org.openjdk.jmc.greychart.data.DataSeriesProvider;
import org.openjdk.jmc.greychart.data.RenderingMode;
import org.openjdk.jmc.greychart.providers.AveragingProvider;
import org.openjdk.jmc.greychart.providers.CompositeOptimizingProvider;
import org.openjdk.jmc.greychart.providers.IntermediateStackingProvider;
import org.openjdk.jmc.greychart.providers.SampleCountingProvider;
import org.openjdk.jmc.greychart.providers.SubsamplingProvider;
import org.openjdk.jmc.greychart.util.Messages;
import org.openjdk.jmc.ui.common.xydata.DataSeries;

/**
 * The default little XY chart. This class currently implements rather much. Whenever there is a
 * need for other kinds of charts, abstract superclasses will be created and functionality will be
 * moved up the hierarchy. It currently lays out the chart in the following fashion.
 *
 * <pre>
  *              -------------
 *              ##=========++
 *              ##=========++
 *              ##=========++
 *              &&&&&&&&&&&&&
 *
 *              - = title area
 *              # = y axis area
 *              & = x axis area
 *              = = plot area
 *              + = index area (provides a series index explanation)
 * </pre>
 */
public class DefaultXYGreyChart<XYData> extends AbstractGreyChart<XYData> implements XYGreyChart<XYData>, AxisListener {
	private final ChartChangeEvent AXIS_CHANGE_EVENT = new ChartChangeEvent(this,
			ChartChangeEvent.ChangeType.OTHER_CHANGED);
	private XAxis m_xAxis;
	private YAxis[] m_yAxis;

	// These are objects used when rendering. Created once and reused.
	Rectangle m_titleRect = new Rectangle(), m_xAxisRect = new Rectangle(), m_plotRect = new Rectangle(),
			m_indexRect = new Rectangle();
	private OptimizingProvider m_optimizingProvider;
	private OptimizingProvider m_leftYAxisProvider;
	private OptimizingProvider m_rightYAxisProvider;
	private boolean m_isAutoUpdateOnAxisChange = true;
	private int m_oldResolution = 0;
	private final CancelService m_cancelService = new CancelService();

	private static class YAxisData {
		YAxis axis;
		int width;
	}

	/**
	 * Constructor.
	 */
	public DefaultXYGreyChart() {
		super(Messages.getString(Messages.DefaultXYGreyChart_DEFAULT_TITLE));
		m_xAxis = new DefaultXAxis(this);
		m_yAxis = new DefaultYAxis[0];
		setPlotRenderer(new DefaultXYLineRenderer(this));
		m_titleRenderer = new DefaultTitleRenderer(this);
		setBackground(FontAndColors.getDefaultBackground());
//		m_indexRenderer = new DefaultVerticalIndexRenderer(this);
	}

	@Override
	public DefaultXYLineRenderer getPlotRenderer() {
		return (DefaultXYLineRenderer) super.getPlotRenderer();
	}

	/**
	 * @see org.openjdk.jmc.greychart.XYGreyChart#setXAxis(XAxis)
	 */
	@Override
	public void setXAxis(XAxis axis) {
		m_xAxis = axis;
		rebuildOptimizingProvider(getDataProvider());
		updateAxisListeners();
	}

	/**
	 * @see org.openjdk.jmc.greychart.XYGreyChart#getXAxis()
	 */
	@Override
	public XAxis getXAxis() {
		return m_xAxis;
	}

	/**
	 * @see org.openjdk.jmc.greychart.XYGreyChart#setYAxis(YAxis)
	 */
	@Override
	public void addYAxis(YAxis axis) {
		// FIXME: generalize
		addToYAxisArray(axis);
		updateAxisListeners();
	}

	private void addToYAxisArray(YAxis axis) {
		List<YAxis> list = new ArrayList<>(Arrays.asList(m_yAxis));
		list.add(axis);
		m_yAxis = list.toArray(new YAxis[list.size()]);
	}

	/**
	 * @see org.openjdk.jmc.greychart.XYGreyChart#getYAxis()
	 */
	@Override
	public YAxis[] getYAxis() {
		return m_yAxis;
	}

	/**
	 * @see org.openjdk.jmc.greychart.GreyChart#render(Graphics2D, Rectangle)
	 */
	@Override
	public void render(Graphics2D ctx, Rectangle where) {
		if (m_oldResolution == 0) {
			m_oldResolution = (int) (Math.round(where.getWidth()));
		}
		if (hasDataProvider() && m_optimizingProvider != null) { 
			// FIXME: we shouldn't have to check the optimizing provider, it should go away if data provider is null
			// FIXME: Resolution should account for y axis width
			m_optimizingProvider.setResolution(m_oldResolution);
			m_optimizingProvider.setRange(getXAxis().getMin().longValue(), getXAxis().getMax().longValue());
			if (m_optimizingProvider.update()) {
				// FIXME: Update required. Schedule new redraw
			}
		}

		Shape oldClip = ctx.getClip();
		int title_h, xaxis_h, plot_h, plot_w, index_w, total_left, total_right;

		// Set the default font used in the rest of the chart... Cache this font later

		ctx.setFont(FontAndColors.getDefaultFont());

		Rectangle allMinusIndex = new Rectangle(where);
		index_w = getIndexRenderer() == null ? 0 : getIndexRenderer().getPreferredDimensions(ctx, where).width;
		allMinusIndex.width = allMinusIndex.width - index_w;

		YAxisData[] dataArray = createYAxisData(m_yAxis, ctx, allMinusIndex);

		// Start by getting the basic geometry straight.
		// Heights
		title_h = getTitleRenderer() == null ? 0 : getTitleRenderer().getPreferredDimensions(ctx, where).height;
		xaxis_h = getXAxis().getPreferredDimensions(ctx, where).height;
		plot_h = Math.max(where.height - xaxis_h - title_h, 0);

		// Widths
		total_left = sum(dataArray, YAxis.Position.LEFT);
		total_right = sum(dataArray, YAxis.Position.RIGHT);
		plot_w = Math.max(where.width - index_w - total_left - total_right, 0);

		if (hasDataProvider() && m_optimizingProvider != null && m_oldResolution != plot_w) { 
			// FIXME: we shouldn't have to check the optimizing provider, it should go away if data provider is null
			m_optimizingProvider.setResolution(plot_w);
			m_oldResolution = plot_w;
			m_optimizingProvider.update();
		}

		ctx.setClip(where);

		ctx.setColor(getBackground());
		ctx.fillRect(where.x, where.y, where.width, where.height);

		if (isAntaliasingEnabled()) {
			ctx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		} else {
			ctx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		}

		// First paint title
		if (title_h > 0) {
			m_titleRect.x = where.x;
			m_titleRect.width = where.width;
			m_titleRect.y = where.y;
			m_titleRect.height = title_h;
			getTitleRenderer().render(ctx, m_titleRect, where);
		}

		// Calc. some rects.
		int yaxisY = m_titleRect.y + m_titleRect.height;
		m_xAxisRect.x = total_left;
		m_xAxisRect.width = plot_w;
		m_xAxisRect.y = yaxisY + plot_h;
		m_xAxisRect.height = xaxis_h;
		m_plotRect.x = m_xAxisRect.x;
		m_plotRect.y = yaxisY;
		m_plotRect.width = plot_w;
		m_plotRect.height = plot_h;

		if (index_w > 0) {
			// Since index may not access share same x as last plot pixel
			// we subtract one here, and add one to the x of the index rect.
			m_plotRect.width -= 1;
			m_xAxisRect.width -= 1;
			m_indexRect.x = m_xAxisRect.x + m_xAxisRect.width + 1 + total_right;
			m_indexRect.y = m_plotRect.y;
			m_indexRect.height = plot_h;
			m_indexRect.width = index_w;
			getIndexRenderer().render(ctx, m_indexRect, where);
		}

		// FIXME: Could improve rendering times further by caching the result of the Y axis rendering. 
		// If Y-axis min/max is the same and height/width of chart hasn't changed - render the cached image.
		getPlotRenderer().clear(ctx, m_plotRect);

		for (YAxis y : getYAxis()) {
			// Then go for the vertical axis
			y.render(ctx, caculateYRect(y, m_plotRect, dataArray), m_plotRect);
		}

		// Then plot the horizontal axis
		getXAxis().render(ctx, m_xAxisRect, m_plotRect);

		// Plot the chart
		if (m_optimizingProvider != null) {
			getPlotRenderer().render(ctx, m_plotRect, where);
		}

		ctx.setClip(oldClip);
	}

	private Rectangle caculateYRect(YAxis y, Rectangle mPlotRect, YAxisData[] dataArray) {
		int startX = 0;
		int myWidth = 0;
		if (y.getPosition() == Position.RIGHT) {
			startX += mPlotRect.width;
			for (YAxisData data : dataArray) {
				if (data.axis.getPosition() == Position.LEFT) {
					startX += data.width;
				}
			}
		}

		for (YAxisData data : dataArray) {
			if (data.axis == y) {
				myWidth = data.width;
				break;
			} else if (data.axis.getPosition() == y.getPosition()) {
				startX += data.width;
			}
		}
		return new Rectangle(startX, mPlotRect.y, myWidth, mPlotRect.height);
	}

	private int sum(YAxisData[] dataArray, Position pos) {
		int sum = 0;
		for (YAxisData data : dataArray) {
			if (data.axis.getPosition() == pos) {
				sum += data.width;
			}
		}
		return sum;
	}

	private YAxisData[] createYAxisData(YAxis[] yAxis, Graphics2D ctx, Rectangle totalDrawingArea) {
		YAxisData[] data = new YAxisData[yAxis.length];
		for (int i = 0; i < data.length; i++) {
			data[i] = new YAxisData();
			data[i].axis = yAxis[i];
			Dimension dim = yAxis[i].getPreferredDimensions(ctx, totalDrawingArea);

			data[i].width = dim.width;
		}
		return data;
	}

	private boolean hasDataProvider() {
		return getDataProvider() != null;
	}

	/**
	 * @see org.openjdk.jmc.greychart.GreyChart#setBackground(java.awt.Color)
	 */
	@Override
	public void setBackground(Color bgColor) {
		super.setBackground(bgColor);
		getPlotRenderer().setBackground(bgColor);
		getXAxis().setBackground(bgColor);
		if (getLastYAxis() != null) {
			getLastYAxis().setBackground(bgColor);
		}
		if (getIndexRenderer() != null) {
			getIndexRenderer().setBackground(bgColor);
		}
		if (getTitleRenderer() != null) {
			getTitleRenderer().setBackground(bgColor);
		}
		int r = (bgColor.getRed() + getForeground().getRed()) / 2;
		int g = (bgColor.getGreen() + getForeground().getGreen()) / 2;
		int b = (bgColor.getBlue() + getForeground().getBlue()) / 2;
		Color titleColor = new Color(r, g, b);
		setAxisTitleColor(titleColor);
	}

	private YAxis getLastYAxis() {
		if (m_yAxis.length == 0) {
			return null;
		} else {
			return m_yAxis[m_yAxis.length - 1];
		}
	}

	/**
	 * @see org.openjdk.jmc.greychart.GreyChart#setForeground(java.awt.Color)
	 */
	@Override
	public void setForeground(Color fgColor) {
		super.setForeground(fgColor);
		getPlotRenderer().setForeground(fgColor);
		getXAxis().setForeground(fgColor);
		for (YAxis yAxis : getYAxis()) {
			yAxis.setForeground(fgColor);
		}
		if (getIndexRenderer() != null) {
			getIndexRenderer().setForeground(fgColor);
		}
		if (getTitleRenderer() != null) {
			getTitleRenderer().setForeground(fgColor);
		}
		int r = (fgColor.getRed() + getBackground().getRed()) / 2;
		int g = (fgColor.getGreen() + getBackground().getGreen()) / 2;
		int b = (fgColor.getBlue() + getBackground().getBlue()) / 2;
		Color titleColor = new Color(r, g, b);
		setAxisTitleColor(titleColor);
	}

	/**
	 * Sets the title color for the X axis and the Y axis.
	 *
	 * @param titleColor
	 */
	public void setAxisTitleColor(Color titleColor) {
		getXAxis().setTitleColor(titleColor);
		for (YAxis yAxis : getYAxis()) {
			yAxis.setTitleColor(titleColor);
		}
	}

	/**
	 * @see org.openjdk.jmc.greychart.GreyChart#setTitle(java.lang.String)
	 */
	@Override
	public void setTitle(String title) {
		super.setTitle(title);
	}

	@Override
	public void setDataProvider(DataSeriesProvider<XYData> dataProvider) {
		rebuildOptimizingProvider(dataProvider);
		super.setDataProvider(dataProvider);
	}

	private void rebuildOptimizingProvider(DataSeriesProvider<XYData> sp) {
		if (sp == null) {
			return;
		}
		DataSeries[] series = sp.getDataSeries();
		OptimizingProvider[] optimizingProviders = new OptimizingProvider[series.length];
		OptimizingProvider[][] stacks = new OptimizingProvider[series.length][0];
		for (int n = 0; n < series.length; n++) {
			OptimizingProvider provider = createOptimizingProvider(series[n],
					getMetadataProvider().getMultiplier(series[n]), getMetadataProvider().getMode(series[n]));
			if (getMetadataProvider().getStacking(series[n])) {
				stackProvider(provider, stacks, n, series.length);
			}
			optimizingProviders[n] = provider;
		}
		if (series.length == 0) {
			m_optimizingProvider = null;
			m_leftYAxisProvider = null;
			m_rightYAxisProvider = null;
		} else {
			buildStacks(series, optimizingProviders, stacks);
			buildLeftAndRightProviders(series, optimizingProviders);
			m_optimizingProvider = new CompositeOptimizingProvider(optimizingProviders);
		}
	}

	private OptimizingProvider createOptimizingProvider(DataSeries series, double yMultiplier, RenderingMode mode) {
		boolean integrating = mode == RenderingMode.DENSITY_INTEGRATING || mode == RenderingMode.INTEGRATING;
		switch (mode) {
		case DENSITY:
		case DENSITY_INTEGRATING:
			return new SampleCountingProvider(series, yMultiplier, getXAxis(), m_cancelService, integrating);
		case AVERAGING:
			return new AveragingProvider(series, yMultiplier, getXAxis(), m_cancelService);
		case INTEGRATING:
		case SUBSAMPLING:
		default:
			return new SubsamplingProvider(series, yMultiplier, getXAxis(), m_cancelService, integrating);
		}
	}

	private void buildStacks(
		DataSeries[] series, OptimizingProvider[] optimizingProviders, OptimizingProvider[][] stacks) {
		for (int n = 0; n < series.length; n++) {
			if (stacks[n].length > 0) {
				// create stacking provider
				optimizingProviders[n] = new IntermediateStackingProvider(stacks[n][0], stacks[n]);
			}
		}
	}

	private void buildLeftAndRightProviders(DataSeries[] series, OptimizingProvider[] optimizingProviders) {
		List<OptimizingProvider> leftAxisProviders = new ArrayList<>();
		List<OptimizingProvider> rightAxisProviders = new ArrayList<>();

		for (int n = 0; n < series.length; n++) {
			YAxis yAxis = getMetadataProvider().getYAxis(series[n]);
			if (yAxis != null && yAxis.getPosition() == Position.RIGHT) {
				rightAxisProviders.add(optimizingProviders[n]);
			} else {
				leftAxisProviders.add(optimizingProviders[n]);
			}
		}
		m_leftYAxisProvider = new CompositeOptimizingProvider(
				leftAxisProviders.toArray(new OptimizingProvider[leftAxisProviders.size()]));
		m_rightYAxisProvider = new CompositeOptimizingProvider(
				rightAxisProviders.toArray(new OptimizingProvider[rightAxisProviders.size()]));
	}

	private void stackProvider(OptimizingProvider provider, OptimizingProvider[][] stacks, int n, int maxLength) {
		stacks[n] = new OptimizingProvider[maxLength];
		stacks[n][0] = provider;
		for (int m = 0; m < n; m++) {
			if (stacks[m].length > 0) {
				stacks[m][n] = stacks[n][0];
			}
		}
	}

	public OptimizingProvider getOptimizingProvider() {
		return m_optimizingProvider;
	}

	public boolean isAutoUpdateOnAxisChange() {
		return m_isAutoUpdateOnAxisChange;
	}

	public void setAutoUpdateOnAxisChange(boolean enable) {
		m_isAutoUpdateOnAxisChange = enable;
		updateAxisListeners();
	}

	private void updateAxisListeners() {
		if (isAutoUpdateOnAxisChange()) {
			getXAxis().addAxisListener(this);
			for (YAxis yAxis : getYAxis()) {
				yAxis.addAxisListener(this);
			}
		} else {
			getXAxis().removeAxisListener(this);
			for (YAxis yAxis : getYAxis()) {
				yAxis.removeAxisListener(this);
			}
		}
	}

	@Override
	public void onAxisChanged() {
		fireChangeEvent(AXIS_CHANGE_EVENT);
	}

	public OptimizingProvider getYAxisProvider(Position position) {
		if (position == Position.RIGHT) {
			return m_rightYAxisProvider;
		} else {
			return m_leftYAxisProvider;
		}
	}

	public void abort() {
		m_cancelService.cancel();
	}
}
