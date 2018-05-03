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

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import org.openjdk.jmc.greychart.AbstractGreyChart;
import org.openjdk.jmc.greychart.IndexRenderer;
import org.openjdk.jmc.greychart.SeriesPlotRenderer;
import org.openjdk.jmc.greychart.TitleRenderer;
import org.openjdk.jmc.greychart.data.DataSeriesProvider;
import org.openjdk.jmc.greychart.util.Messages;

/**
 * The default little pie chart.
 */
public class DefaultPieGreyChart<T> extends AbstractGreyChart<T> {
	private final Rectangle m_titleArea = new Rectangle();
	private final Rectangle m_plotArea = new Rectangle();
	private final Rectangle m_indexArea = new Rectangle();
	private TitleRenderer m_titleRenderer;
	private IndexRenderer m_indexRenderer;

	/**
	 * Constructor.
	 */
	public DefaultPieGreyChart() {
		super(Messages.getString(Messages.DefaultPieGreyChart_DEFAULT_TITLE));
		m_titleRenderer = new DefaultTitleRenderer(this);
		setPlotRenderer(new DefaultPieRenderer(this));
		m_indexRenderer = new DefaultHorizontalIndexRenderer(this);
		setAntialiasingEnabled(true);
	}

	/**
	 * Constructor.
	 *
	 * @param provider
	 *            the DataProvider for the chart.
	 */
	public DefaultPieGreyChart(DataSeriesProvider<T> provider) {
		this();
		setDataProvider(provider);
	}

	/**
	 * @see org.openjdk.jmc.greychart.GreyChart#render(java.awt.Graphics2D, java.awt.Rectangle)
	 */
	@Override
	public void render(Graphics2D ctx, Rectangle where) {
		ctx.setColor(getBackground());
		ctx.fillRect(where.x, where.y, where.width, where.height);
		m_titleArea.height = getTitleRenderer() == null ? 0
				: getTitleRenderer().getPreferredDimensions(ctx, where).height;
		m_titleArea.width = where.width;
		m_titleRenderer.render(ctx, m_titleArea, where);
		m_indexArea.height = getIndexRenderer() == null ? 0
				: getIndexRenderer().getPreferredDimensions(ctx, where).height;
		m_indexArea.width = where.width;
		m_plotArea.width = where.width;
		m_plotArea.height = where.height - m_titleArea.height - m_indexArea.height;
		m_plotArea.y = m_titleArea.height;
		m_indexArea.y = m_plotArea.y + m_plotArea.height;

		if (isAntaliasingEnabled()) {
			ctx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		} else {
			ctx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		}

		if (m_titleArea.height > 0) {
			getTitleRenderer().render(ctx, m_titleArea, where);
		}
		if (m_indexArea.height > 0) {
			getIndexRenderer().render(ctx, m_indexArea, where);
		}

		getPlotRenderer().render(ctx, m_plotArea, where);
	}

	/**
	 * @return Returns the titleRenderer.
	 */
	@Override
	public TitleRenderer getTitleRenderer() {
		return m_titleRenderer;
	}

	/**
	 * @param titleRenderer
	 *            The titleRenderer to set.
	 */
	public void setTitleRenderer(TitleRenderer titleRenderer) {
		m_titleRenderer = titleRenderer;
	}

	/**
	 * @return Returns the indexRenderer.
	 */
	@Override
	public IndexRenderer getIndexRenderer() {
		return m_indexRenderer;
	}

	/**
	 * @param indexRenderer
	 *            The indexRenderer to set.
	 */
	@Override
	public void setIndexRenderer(IndexRenderer indexRenderer) {
		m_indexRenderer = indexRenderer;
	}

	public SeriesPlotRenderer getSeriesPlotRenderer() {
		return null;
	}

	public void setSeriesPlotRenderer(SeriesPlotRenderer renderer) {
		// TODO Auto-generated method stub

	}

}
