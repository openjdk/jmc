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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.util.ArrayList;

import org.openjdk.jmc.greychart.util.Messages;

/**
 * Abstract superclass for an Axis.
 */
public abstract class AbstractAxis extends AbstractSeriesRenderer implements Axis {
	/**
	 * The default relative tick size.
	 *
	 * @see #setRelativeTickSize(float)
	 */
	public final static float DEFAULT_RELATIVE_TICK_SIZE = .2f;

	/**
	 * The default number of ticks.
	 *
	 * @see #setNumberOfTicks(int)
	 */
	public final static int DEFAULT_NUMBER_OF_TICKS = 5;

	/**
	 * The default minimum tick distance.
	 *
	 * @see #setMinTickDistance(int)
	 */
	public final static int DEFAULT_MINIMUM_TICK_DISTANCE = 10;

	/**
	 * The default tick density.
	 *
	 * @see #setTickDensity(TickDensity)
	 */
	public final static TickDensity DEFAULT_TICK_DENSITY = TickDensity.VARIABLE;

	/**
	 * The default for painting grid lines.
	 *
	 * @see #setPaintGridLinesEnabled(boolean)
	 */
	public final static boolean DEFAULT_PAINT_GRIDLINES = false;
	/**
	 * The default stroke to use when painting the grid lines.
	 */
	public final static Stroke DASH_STROKE;

	private boolean m_paintTickMarks;
	private float m_relativeTickSize = DEFAULT_RELATIVE_TICK_SIZE;
	private int m_minTickDistance = DEFAULT_MINIMUM_TICK_DISTANCE;
	private int m_numberOfTicks = DEFAULT_NUMBER_OF_TICKS;
	private TickDensity m_tickDensity = DEFAULT_TICK_DENSITY;
	private boolean m_paintGridLinesEnabled = DEFAULT_PAINT_GRIDLINES;
	private Color m_titleColor;
	private String m_title = Messages.getString(Messages.AbstractAxis_DEFAULT_TITLE);
	private final ArrayList<AxisListener> m_axisListeners = new ArrayList<>();
	protected TickFormatter formatter;

	// FIXME: Add fix range axis and auto range axis.
	protected Number m_max = 100;
	protected Number m_min = 0;

	static {
		float[] dash = new float[2];
		dash[0] = 4;
		dash[1] = 2;
		DASH_STROKE = new BasicStroke(.5f, 0, 0, 1.0f, dash, 0);
	}

	/**
	 * Constructor.
	 */
	public AbstractAxis(SeriesGreyChart owner) {
		super(owner);
	}

	/**
	 * @return true if tick marks should be calculated and painted.
	 */
	public boolean isTickMarksEnabled() {
		return m_paintTickMarks;
	}

	/**
	 * Set this to true to enable tick mark rendering.
	 *
	 * @param paintTickMarks
	 *            true to enable tick marks, false to disable.
	 */
	public void setTickMarksEnabled(boolean paintTickMarks) {
		m_paintTickMarks = paintTickMarks;
	}

	/**
	 * @return returns the number of tick marks to use. This will not be strictly adhered to. This
	 *         is a suggestion. The underlying tick mark algorithm may choose another amount of tick
	 *         marks that makes more sense to the end user.
	 */
	public int getNumberOfTicks() {
		return m_numberOfTicks;
	}

	/**
	 * Sets the number of tick marks to use. This may not be strictly adhered to. This is a
	 * suggestion. The underlying tick mark algorithm may choose another amount of tick marks that
	 * makes more sense to the end user.
	 *
	 * @param numberOfTicks
	 *            the number of tick marks to paint.
	 */
	public void setNumberOfTicks(int numberOfTicks) {
		m_numberOfTicks = numberOfTicks;
	}

	/**
	 * Returns the minimum tick distance. This is the closest distance in pixels that ticks will be
	 * rendered. This may affect the total number of tick marks actually rendered.
	 *
	 * @return the minimum distance between ticks.
	 */
	public int getMinTickDistance() {
		return m_minTickDistance;
	}

	/**
	 * Sets the minimum tick distance. This is the closest distance in pixels that ticks will be
	 * rendered. This may affect the total number of tick marks actually rendered.
	 *
	 * @param minTickDistance
	 *            the tick distance in axis rendering coordinates. (pixels if rendered to screen)
	 */
	public void setMinTickDistance(int minTickDistance) {
		m_minTickDistance = minTickDistance;
	}

	/**
	 * This is the tick size that will be used. It is specified as a fraction of the height of the
	 * font used to paint the axis labels. It defaults to DEFAULT_RELATIVE_TICK_SIZE.
	 *
	 * @return the relative tick size.
	 */
	public float getRelativeTickSize() {
		return m_relativeTickSize;
	}

	/**
	 * This is the tick size that will be used. It is specified as a fraction of the height of the
	 * font used to paint the axis labels. It defaults to DEFAULT_RELATIVE_TICK_SIZE.
	 *
	 * @param relativeTickSize
	 *            the new relative tick size.
	 */
	public void setRelativeTickSize(float relativeTickSize) {
		m_relativeTickSize = relativeTickSize;
	}

	/**
	 * @return true if grid lines will be painted from the tick markes on the background of the plot
	 *         area.
	 */
	public boolean isPaintGridLinesEnabled() {
		return m_paintGridLinesEnabled;
	}

	/**
	 * Set this attribute to true if you want grid lines to be painted on the backround of the plot
	 * area.
	 *
	 * @param paintGridLinesEnabled
	 *            set to true to enable grid lines, false to disable.
	 */
	public void setPaintGridLinesEnabled(boolean paintGridLinesEnabled) {
		m_paintGridLinesEnabled = paintGridLinesEnabled;
	}

	/**
	 * Sets the tick density which decides how dense the tick marks will be. If the tick density is
	 * set to variable, the tick density should be determined by the preferred number of ticks.
	 *
	 * @see #setNumberOfTicks(int)
	 * @param value
	 *            the new tick density
	 */
	public void setTickDensity(TickDensity value) {
		m_tickDensity = value;
	}

	/**
	 * @return Returns the current tick density.
	 */
	public TickDensity getTickDensity() {
		return m_tickDensity;
	}

	/**
	 * @return the title of the axis.
	 */
	@Override
	public String getTitle() {
		return m_title;
	}

	/**
	 * @return whether this axis has a non-null, non-empty string as title
	 */
	public boolean hasTitle() {
		return m_title != null && m_title.length() > 0;
	}

	/**
	 * Sets the title of the axis.
	 *
	 * @param title
	 *            the new title of axis.
	 */
	@Override
	public void setTitle(String title) {
		m_title = title;
	}

	/**
	 * @return the title color of the graph.
	 */
	@Override
	public Color getTitleColor() {
		return m_titleColor;
	}

	/**
	 * @param color
	 *            the new title color of the graph.
	 */
	@Override
	public void setTitleColor(Color color) {
		m_titleColor = color;
	}

	@Override
	public void setMax(Number max) {
		if (max == null) {
			m_max = null;
		} else {
			m_max = max.doubleValue();
		}

	}

	@Override
	public void setMin(Number min) {
		if (min == null) {
			m_min = null;
		} else {
			m_min = min.doubleValue();
		}

	}

	@Override
	public Number getMin() {
		return m_min;
	}

	@Override
	public Number getMax() {
		return m_max;
	}

	@Override
	public void addAxisListener(AxisListener l) {
		m_axisListeners.add(l);
	}

	@Override
	public void removeAxisListener(AxisListener l) {
		m_axisListeners.remove(l);
	}

	protected void fireAxisChange() {
		for (AxisListener l : m_axisListeners) {
			l.onAxisChanged();
		}
	}

	private boolean isVisible = true;

	@Override
	public boolean isVisible() {
		return isVisible;
	}

	@Override
	public void setVisible(boolean enable) {
		isVisible = enable;
	}

	public void setFormatter(TickFormatter formatter) {
		this.formatter = formatter;
	}

	public TickFormatter getFormatter() {
		return formatter;
	}

	@Override
	public String getContentType() {
		return AxisContentType.UNKNOWN;
	}

}
