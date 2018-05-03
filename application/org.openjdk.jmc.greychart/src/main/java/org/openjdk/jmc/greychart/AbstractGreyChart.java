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
package org.openjdk.jmc.greychart;

import java.awt.Color;
import java.util.ArrayList;

import org.openjdk.jmc.greychart.data.DataSeriesProvider;

/**
 * Abstract superclass for the GreyChart's.
 */
public abstract class AbstractGreyChart<T> implements SeriesGreyChart<T> {
	private String m_title;
	private boolean m_useAntialiasing = true;
	private DataSeriesProvider<T> m_dataProvider;
	private SeriesPlotRenderer m_plotRenderer;
	protected Color m_background = FontAndColors.getDefaultBackground();
	protected Color m_foreground = FontAndColors.getDefaultForeground();
	private final ArrayList m_graphChangeListeners = new ArrayList(5);
	protected ChartRenderer m_titleRenderer;
	protected IndexRenderer m_indexRenderer;
	private SeriesMetadataProvider m_metadataProvider;

	/**
	 * Constructor.
	 *
	 * @param title
	 *            the title of the graph.
	 */
	public AbstractGreyChart(String title) {
		setTitle(title);
	}

	protected void setPlotRenderer(SeriesPlotRenderer plotRenderer) {
		m_plotRenderer = plotRenderer;
	}

	/**
	 * @see org.openjdk.jmc.greychart.GreyChart#setTitle(java.lang.String)
	 */
	@Override
	public void setTitle(String title) {
		m_title = title;
	}

	/**
	 * @see org.openjdk.jmc.greychart.GreyChart#getTitle()
	 */
	@Override
	public String getTitle() {
		return m_title;
	}

	/**
	 * @see org.openjdk.jmc.greychart.GreyChart#setDataProvider(DataProvider)
	 */
	@Override
	public void setDataProvider(DataSeriesProvider<T> provider) {
		m_dataProvider = provider;
		fireChangeEvent(new ChartChangeEvent(this, ChartChangeEvent.ChangeType.DATA_STRUCTURE_CHANGED));
	}

	/**
	 * @see org.openjdk.jmc.greychart.GreyChart#getDataProvider()
	 */
	@Override
	public DataSeriesProvider<T> getDataProvider() {
		return m_dataProvider;
	}

	/**
	 * @see org.openjdk.jmc.greychart.GreyChart#removeChangeListener(ChartChangeListener)
	 */
	@Override
	public void removeChangeListener(ChartChangeListener l) {
		m_graphChangeListeners.remove(l);
	}

	/**
	 * @see org.openjdk.jmc.greychart.GreyChart#addChangeListener(ChartChangeListener)
	 */
	@Override
	public void addChangeListener(ChartChangeListener l) {
		if (l == null) {
			throw new IllegalArgumentException("May not add null as chart change listener!"); //$NON-NLS-1$
		}
		m_graphChangeListeners.add(l);
	}

	/**
	 * @see org.openjdk.jmc.greychart.GreyChart#getPlotRenderer()
	 */
	@Override
	public SeriesPlotRenderer getPlotRenderer() {
		return m_plotRenderer;
	}

	protected void fireChangeEvent(ChartChangeEvent event) {
		for (int i = 0, size = m_graphChangeListeners.size(); i < size; i++) {
			((ChartChangeListener) m_graphChangeListeners.get(i)).onChartChanged(event);
		}
	}

	/**
	 * @see org.openjdk.jmc.greychart.GreyChart#isAntaliasingEnabled()
	 */
	@Override
	public boolean isAntaliasingEnabled() {
		return m_useAntialiasing;
	}

	/**
	 * @see org.openjdk.jmc.greychart.GreyChart#setAntialiasingEnabled(boolean)
	 */
	@Override
	public void setAntialiasingEnabled(boolean enable) {
		m_useAntialiasing = enable;
	}

	/**
	 * @see org.openjdk.jmc.greychart.GreyChart#getBackground()
	 */
	@Override
	public Color getBackground() {
		return m_background;
	}

	/**
	 * @see org.openjdk.jmc.greychart.GreyChart#setBackground(java.awt.Color)
	 */
	@Override
	public void setBackground(Color bgColor) {
		m_background = bgColor;
	}

	/**
	 * @see org.openjdk.jmc.greychart.GreyChart#getForeground()
	 */
	@Override
	public Color getForeground() {
		return m_foreground;
	}

	/**
	 * @see org.openjdk.jmc.greychart.GreyChart#setForeground(java.awt.Color)
	 */
	@Override
	public void setForeground(Color fgColor) {
		m_foreground = fgColor;
	}

	@Override
	public SeriesMetadataProvider getMetadataProvider() {
		return m_metadataProvider;
	}

	public void setMetadataProvider(SeriesMetadataProvider smdp) {
		this.m_metadataProvider = smdp;
	}

	/**
	 * @see org.openjdk.jmc.greychart.SeriesGreyChart#setIndexRenderer(org.openjdk.jmc.greychart.IndexRenderer)
	 */
	@Override
	public void setIndexRenderer(IndexRenderer indexRenderer) {
		m_indexRenderer = indexRenderer;
	}

	/**
	 * @see org.openjdk.jmc.greychart.SeriesGreyChart#getIndexRenderer()
	 */
	@Override
	public IndexRenderer getIndexRenderer() {
		return m_indexRenderer;
	}

	/**
	 * @see org.openjdk.jmc.greychart.SeriesGreyChart#setTitleRenderer(ChartRenderer)
	 */
	@Override
	public void setTitleRenderer(ChartRenderer titleRenderer) {
		m_titleRenderer = titleRenderer;
	}

	/**
	 * @see org.openjdk.jmc.greychart.SeriesGreyChart#getTitleRenderer()
	 */
	@Override
	public ChartRenderer getTitleRenderer() {
		return m_titleRenderer;
	}
}
