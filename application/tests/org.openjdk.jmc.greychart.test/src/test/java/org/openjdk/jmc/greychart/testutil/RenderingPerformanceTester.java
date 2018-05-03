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
package org.openjdk.jmc.greychart.testutil;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Date;
import java.util.HashSet;

import org.openjdk.jmc.greychart.DefaultMetadataProvider;
import org.openjdk.jmc.greychart.GreyChartPanel;
import org.openjdk.jmc.greychart.YAxis.Position;
import org.openjdk.jmc.greychart.data.SeriesProviderSet;
import org.openjdk.jmc.greychart.impl.DateXAxis;
import org.openjdk.jmc.greychart.impl.DefaultVerticalIndexRenderer;
import org.openjdk.jmc.greychart.impl.DefaultXYGreyChart;
import org.openjdk.jmc.greychart.impl.DefaultYAxis;
import org.openjdk.jmc.greychart.util.ChartRenderingToolkit;
import org.openjdk.jmc.ui.common.xydata.DefaultTimestampedData;
import org.openjdk.jmc.ui.common.xydata.ITimestampedData;

/**
 * Tests the rendering performance of the ordinary XY line graph.
 */
public class RenderingPerformanceTester {
	private final static long PERIOD = 10000;
	private final static long SLEEP_TIME = 20;

	private final static double MAX_Y = 5;
	private final static double MIN_Y = -5;

	private static AdderThread m_adder;

	private static DateXAxis xaxis;
	private static TimestampDataSeries SINUS_DATA;
	private static TimestampDataSeries SAW_DATA;

	private final static Date START_TIME = new Date();
	private static GreyChartPanel panel;

	private static class MyMouseListener extends MouseAdapter {
		/**
		 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
		 */
		@Override
		public void mouseClicked(MouseEvent e) {
			System.out.println("Adding value!"); //$NON-NLS-1$
			if (m_adder != null) {
				m_adder.doStep();
			}
		}

		/**
		 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
		 */
		@Override
		public void mouseEntered(MouseEvent e) {
			System.out.println("Entered graph!"); //$NON-NLS-1$
		}

		/**
		 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
		 */
		@Override
		public void mouseExited(MouseEvent e) {
			System.out.println("Exited graph!"); //$NON-NLS-1$
		}

	}

	/**
	 * Entry point.
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		HashSet<String> set = new HashSet<>();
		for (String arg : args) {
			set.add(arg.toLowerCase());
		}
		SeriesProviderSet<ITimestampedData> provider = new SeriesProviderSet<>();
		SINUS_DATA = new TimestampDataSeries();
		SAW_DATA = new TimestampDataSeries();
		provider.addDataSeries(SAW_DATA);
		provider.addDataSeries(SINUS_DATA);
		String title = "Mode: "; //$NON-NLS-1$
		DefaultXYGreyChart<ITimestampedData> graph = new DefaultXYGreyChart<>();

		DefaultYAxis yaxis_left = new DefaultYAxis(graph);
		DefaultYAxis yaxis_right = new DefaultYAxis(graph);
		yaxis_left.setMin(MIN_Y);
		yaxis_left.setMax(MAX_Y);
		yaxis_left.setTickSize(4);
		yaxis_left.setNumberOfTicks(20);
		yaxis_left.setAlwaysShowZero(false);
		yaxis_left.setAutoPadding(0.05);
		yaxis_left.setTitle("Saw value"); //$NON-NLS-1$
		yaxis_left.setPosition(Position.LEFT);
		yaxis_right.setMin(MIN_Y);
		yaxis_right.setMax(MAX_Y);
		yaxis_right.setTickSize(4);
		yaxis_right.setNumberOfTicks(20);
		yaxis_right.setAlwaysShowZero(false);
		yaxis_right.setAutoPadding(0.05);
		yaxis_right.setTitle("Sine value"); //$NON-NLS-1$
		yaxis_right.setPosition(Position.RIGHT);
//		yaxis.setVisible(false);
//		xaxis.setVisible(false);

		graph.setMetadataProvider(new DefaultMetadataProvider());
		graph.setAutoUpdateOnAxisChange(true);
		xaxis = new DateXAxis(graph);
		xaxis.setRange(START_TIME.getTime(), START_TIME.getTime() + 60000);
		xaxis.setPaintGridLinesEnabled(true);
		xaxis.setNumberOfTicks(20);

		graph.setDataProvider(provider);

		graph.setXAxis(xaxis);
		graph.addYAxis(yaxis_left);
		graph.addYAxis(yaxis_right);

		graph.getXAxis().setTitle("Time"); //$NON-NLS-1$

		panel = new GreyChartPanel(graph);
		graph.setIndexRenderer(new DefaultVerticalIndexRenderer(graph));
		panel.setBackground(new Color(240, 240, 240));

		if (set.contains("gradient")) { //$NON-NLS-1$
			title += "gradient "; //$NON-NLS-1$
		}
		if (set.contains("fill")) { //$NON-NLS-1$
			title += "fill "; //$NON-NLS-1$
		}
//		if (set.contains("averaging")) {
//			((DefaultXYLineRenderer) graph.getSeriesPlotRenderer())
//					.setMode(DefaultXYLineRenderer.RENDERING_MODE_AVERAGING);
//			title += "averaging ";
//		} else if (set.contains("subsampling")) {
//			((DefaultXYLineRenderer) graph.getSeriesPlotRenderer())
//					.setMode(DefaultXYLineRenderer.RENDERING_MODE_SUBSAMPLING);
//			title += "subsampling ";
//		}

		panel.addMouseListener(new MyMouseListener());
		panel.setName("rendering performance"); //$NON-NLS-1$
		panel.setBackground(Color.WHITE);
		panel.getChart().getPlotRenderer().setBackground(Color.LIGHT_GRAY);
		graph.setAntialiasingEnabled(false);
		ChartRenderingToolkit.testComponent(panel, 1200, 800);
		graph.setTitle(title);
		m_adder = new AdderThread();
		m_adder.start();
	}

	private static class AdderThread extends Thread {
		private final long m_startTime = System.currentTimeMillis();
		private final DefaultSignalGenerator m_saw = new DefaultSignalGenerator(DefaultSignalGenerator.SAW, PERIOD,
				MIN_Y, MAX_Y, 0, m_startTime);
		private final DefaultSignalGenerator m_sine = new DefaultSignalGenerator(DefaultSignalGenerator.SINUS, PERIOD,
				MIN_Y, MAX_Y, 0, m_startTime);

		/**
		 * Data adder thread. Sleeps SLEEP_TIME and adds data.
		 */
		@Override
		public void run() {
			while (true) {
				doStep();
				try {
					sleep(SLEEP_TIME);
				} catch (InterruptedException e) {
					// Ignore.
				}
			}
		}

		/**
		 * Adds one point of data to the two series.
		 */
		public void doStep() {
			long time = System.currentTimeMillis();
			SINUS_DATA.addData(new DefaultTimestampedData(time, Double.valueOf(m_sine.getValue())));
			SAW_DATA.addData(new DefaultTimestampedData(time, Double.valueOf(m_saw.getValue())));
			xaxis.setRange(m_startTime + 100, Math.max(m_startTime + 100, time - 300) + 100);
		}
	}
}
