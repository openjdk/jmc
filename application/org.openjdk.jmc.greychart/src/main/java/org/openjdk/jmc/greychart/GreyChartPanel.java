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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

import javax.swing.JComponent;

import org.openjdk.jmc.ui.common.util.Environment;

/**
 * Simple JComponent containing a GreyChart. Thanks to: Janne Andersson @ MINQ
 */
public class GreyChartPanel extends JComponent implements ChartChangeListener {
	public final static Logger LOGGER = Logger.getLogger("org.openjdk.jmc.greychart"); //$NON-NLS-1$

	private static final String PROPERTY_KEY_DEBUG = "org.openjdk.jmc.greychart.debug"; //$NON-NLS-1$
	private static final String PROPERTY_KEY_SHOWRENDERINGTIME = "org.openjdk.jmc.greychart.showrenderingtime"; //$NON-NLS-1$
	static final long serialVersionUID = 0x0001;
	/**
	 * DEBUG flag initialized from the system property org.openjdk.jmc.greychart.debug on startup. Will
	 * cause rendering to take place directly on screen.
	 */
	public final static boolean DEBUG;
	private final static boolean SHOW_RENDERING_TIME;
	private static final int[] EMPTY_INT_ARRAY = new int[0];
	private static final double[] EMPTY_DOUBLE_ARRAY = new double[0];

	private transient GreyChart m_chart;
	private transient Image m_imageBuffer;
	private final Rectangle m_lastImageRenderingArea = new Rectangle(0, 0, 0, 0);
	private final Rectangle m_lastTargetArea = new Rectangle(0, 0, 0, 0);
	private final Insets m_paintViewInsets = new Insets(0, 0, 0, 0);

	private Font m_debugFont = null;

	private int m_selectionStartX = -1;
	private int m_selectionEndX = -1;

	static {
		DEBUG = isUseDebug();
		SHOW_RENDERING_TIME = isShowRenderingTime();
	}

	/**
	 * Constructor.
	 *
	 * @param chart
	 *            The GreyChart to display in the panel.
	 */
	public GreyChartPanel(GreyChart chart) {
		m_chart = chart;
		m_chart.addChangeListener(this);
		initializeColors();
//		selectionHandler = new SelectionHandler(this);
	}

	private static boolean isShowRenderingTime() {
		if (System.getProperties().containsKey(PROPERTY_KEY_SHOWRENDERINGTIME)) {
			return Boolean.getBoolean(PROPERTY_KEY_SHOWRENDERINGTIME);
		}
		return Environment.isDebug();
	}

	private static boolean isUseDebug() {
		if (System.getProperties().containsKey(PROPERTY_KEY_DEBUG)) {
			return Boolean.getBoolean(PROPERTY_KEY_DEBUG);
		}
		return Environment.isDebug();
	}

	/**
	 * @return the chart displayed in this panel.
	 */
	public GreyChart getChart() {
		return m_chart;
	}

	/**
	 * Initializes colors for the panel and the graph in the panel
	 */
	public void initializeColors() {
		setBackground(FontAndColors.getDefaultBackground());
		setForeground(FontAndColors.getDefaultForeground());
	}

	/**
	 * Sets a new chart to display in this panel.
	 *
	 * @param chart
	 *            the chart to display.
	 */
	public void setChart(GreyChart chart) {
		m_chart = chart;
	}

	/**
	 * @see JComponent#paintComponent(java.awt.Graphics)
	 */
	@Override
	public void paintComponent(Graphics g) {
		long start = System.currentTimeMillis();
		// Handle borders
		Insets insets = getInsets(m_paintViewInsets);
		m_lastTargetArea.x = insets.left;
		m_lastTargetArea.y = insets.top;
		m_lastTargetArea.width = getWidth() - (insets.left + insets.right);
		m_lastTargetArea.height = getHeight() - (insets.top + insets.bottom);

		if (m_chart != null) {
			if (DEBUG) {
				/*
				 * FIXME: This assumes that every component beyond this point restores the altered
				 * properties of the graphics context when exiting its rendering function. We might
				 * want to do Graphics2D g2d = (Graphics2D) g.create(); and finally dispose of the
				 * graphics.
				 */ 
				m_lastTargetArea.width = getWidth();
				m_lastTargetArea.height = getHeight();
				m_chart.render((Graphics2D) g, m_lastTargetArea);
			} else {
				g.drawImage(createPlotImage(m_lastTargetArea.width, m_lastTargetArea.height), m_lastTargetArea.x,
						m_lastTargetArea.y, null);
				int[] selection = getSelection();
				if (selection.length > 0) {
					Rectangle renderedChartBounds = getRenderedChartBounds();
					g.setXORMode(Color.black);
					int selStart = selection[0];
					int selWidth = selection[1] - selection[0];
					if (selWidth < 0) {
						selWidth = -selWidth;
						selStart = selStart - selWidth;
					}
					g.fillRect(selStart + renderedChartBounds.x, renderedChartBounds.y, selWidth,
							renderedChartBounds.height);
				}
			}
		} else {
			super.paint(g);
		}

		if (SHOW_RENDERING_TIME) {
			if (m_debugFont == null) {
				m_debugFont = g.getFont().deriveFont(12.0f);
			}
			g.setFont(m_debugFont);
			int fontHeight = g.getFontMetrics().getAscent() + g.getFontMetrics().getDescent();
			String str = "Time: " + (System.currentTimeMillis() - start) + " ms "; //$NON-NLS-1$ //$NON-NLS-2$
			drawDropShadowString(g, fontHeight, str);
		}
	}

	private void drawDropShadowString(Graphics g, int y, String str) {
		g.setColor(getForeground());
		g.drawString(str, 40, y);
		g.setColor(Color.RED);
		g.drawString(str, 39, y - 1);
	}

	/**
	 * Creates an image of the plot of the specified width and height.
	 *
	 * @param width
	 *            the width of the plot.
	 * @param height
	 *            the height of the plot.
	 * @return an image containing the graph.
	 */
	public Image createPlotImage(int width, int height) {
		if (m_imageBuffer == null || width != m_lastImageRenderingArea.width
				|| height != m_lastImageRenderingArea.height) {
			GraphicsConfiguration gc = getGraphicsConfiguration();

			if (gc != null) {
				m_imageBuffer = gc.createCompatibleImage(width, height, Transparency.BITMASK);
			} else {
				m_imageBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			}

			m_lastImageRenderingArea.width = width;
			m_lastImageRenderingArea.height = height;
		}

		m_chart.render((Graphics2D) m_imageBuffer.getGraphics(), m_lastImageRenderingArea);
		return m_imageBuffer;
	}

	/**
	 * @see JComponent#update(java.awt.Graphics)
	 */
	@Override
	public void update(Graphics g) {
		paintComponent(g);
	}

	@Override
	public void onChartChanged(ChartChangeEvent e) {
		// FIXME: Maybe add a custom repaint manager, and ignore the Swing one?
//		paint(m_canvas.getGraphics());
		repaint();
	}

	@Override
	public void setBackground(Color bg) {
		if (bg == null) {
			bg = FontAndColors.getDefaultBackground();
		}
		super.setBackground(bg);
		getChart().setBackground(bg);
	}

	@Override
	public void setForeground(Color fg) {
		if (fg == null) {
			fg = FontAndColors.getDefaultForeground();
		}
		getChart().setForeground(fg);
	}

	public void setSelection(int startX, int endX) {
		Rectangle renderedChartBounds = getRenderedChartBounds();
		if ((renderedChartBounds == null) || (renderedChartBounds.height < 2)) {
			return;
		}
		if (((startX >= 0) && (startX <= renderedChartBounds.width))
				&& ((endX >= 0) && (endX <= renderedChartBounds.width))) {
			if (startX < endX) {
				m_selectionStartX = startX;
				m_selectionEndX = endX;
			} else {
				m_selectionStartX = endX;
				m_selectionEndX = startX;
			}
		} else {
			throw new IllegalArgumentException("Coordinates must be inside selectable area."); //$NON-NLS-1$
		}
	}

	public int[] getSelection() {
		if (m_selectionStartX >= 0) {
			return new int[] {m_selectionStartX, m_selectionEndX};
		} else {
			return EMPTY_INT_ARRAY;
		}
	}

	/**
	 * Returns the selection as percentages of the width of the graph.
	 */
	public double[] getSelectionPercentages() {
		int[] pixelSelection = getSelection();
		if (pixelSelection.length > 0) {
			Rectangle renderedChartBounds = getRenderedChartBounds();
			return new double[] {(double) pixelSelection[0] / (double) renderedChartBounds.width,
					(double) pixelSelection[1] / (double) renderedChartBounds.width};
		} else {
			return EMPTY_DOUBLE_ARRAY;
		}
	}

	public void clearSelection() {
		m_selectionStartX = -1;
		m_selectionEndX = -1;
	}

	public Rectangle getRenderedChartBounds() {
		return m_chart.getPlotRenderer().getRenderedBounds();
	}
}
