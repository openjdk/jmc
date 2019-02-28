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
import java.util.Set;

import org.openjdk.jmc.ui.common.xydata.DefaultTimestampedData;
import org.openjdk.jmc.ui.common.xydata.ITimestampedData;

import org.openjdk.jmc.greychart.DefaultMetadataProvider;
import org.openjdk.jmc.greychart.GreyChartPanel;
import org.openjdk.jmc.greychart.YAxis.Position;
import org.openjdk.jmc.greychart.data.SeriesProviderSet;
import org.openjdk.jmc.greychart.impl.DateXAxis;
import org.openjdk.jmc.greychart.impl.DefaultVerticalIndexRenderer;
import org.openjdk.jmc.greychart.impl.DefaultXYGreyChart;
import org.openjdk.jmc.greychart.impl.DefaultYAxis;
import org.openjdk.jmc.greychart.util.ChartRenderingToolkit;

/**
 * Tests adding samples out of order.
 */
public class RenderingOutOfOrderTester {
	private final static long PERIOD = 1000;

	private final DefaultSignalGenerator m_saw = new DefaultSignalGenerator(DefaultSignalGenerator.SAW, PERIOD, MIN_Y,
			MAX_Y, 0, 0);
	private final DefaultSignalGenerator m_sine = new DefaultSignalGenerator(DefaultSignalGenerator.SINUS, PERIOD,
			MIN_Y, MAX_Y, 0, 0);

	private final static double MAX_Y = 5;
	private final static double MIN_Y = -5;

	private static DateXAxis xaxis;
	private static TimestampDataSeries SINUS_DATA;
	private static TimestampDataSeries SAW_DATA;

	private final static Date START_TIME = new Date();
	private static GreyChartPanel panel;

	private long m_time = 0;

	private class MyMouseListener extends MouseAdapter {
		/**
		 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
		 */
		@Override
		public void mouseClicked(MouseEvent e) {
			System.out.println("Adding value!");
			doStep();
		}

		/**
		 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
		 */
		@Override
		public void mouseEntered(MouseEvent e) {
			System.out.println("Entered graph!");
		}

		/**
		 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
		 */
		@Override
		public void mouseExited(MouseEvent e) {
			System.out.println("Exited graph!");
		}

	}

	/**
	 * Entry point.
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		Set<String> options = new HashSet<>();
		for (String s : args) {
			options.add(s);
		}
		RenderingOutOfOrderTester tester = new RenderingOutOfOrderTester();
		tester.setup(options);
	}

	private void setup(Set<String> options) {
		SeriesProviderSet<ITimestampedData> provider = new SeriesProviderSet<>();
		SINUS_DATA = new TimestampDataSeries();
		SAW_DATA = new TimestampDataSeries();
		provider.addDataSeries(SAW_DATA);
		provider.addDataSeries(SINUS_DATA);
		String title = "Out of order rendering test. Click for next point!";
		DefaultXYGreyChart<ITimestampedData> graph = new DefaultXYGreyChart<>();

		DefaultYAxis yaxis_left = new DefaultYAxis(graph);
		DefaultYAxis yaxis_right = new DefaultYAxis(graph);
		yaxis_left.setMin(MIN_Y);
		yaxis_left.setMax(MAX_Y);
		yaxis_left.setTickSize(4);
		yaxis_left.setNumberOfTicks(20);
		yaxis_left.setAlwaysShowZero(false);
		yaxis_left.setAutoPadding(0.05);
		yaxis_left.setTitle("Saw value");
		yaxis_left.setPosition(Position.LEFT);
		yaxis_right.setMin(MIN_Y);
		yaxis_right.setMax(MAX_Y);
		yaxis_right.setTickSize(4);
		yaxis_right.setNumberOfTicks(20);
		yaxis_right.setAlwaysShowZero(false);
		yaxis_right.setAutoPadding(0.05);
		yaxis_right.setTitle("Sine value");
		yaxis_right.setPosition(Position.RIGHT);

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

		graph.getXAxis().setTitle("Time");

		panel = new GreyChartPanel(graph);
		graph.setIndexRenderer(new DefaultVerticalIndexRenderer(graph));
		panel.setBackground(new Color(240, 240, 240));

		panel.addMouseListener(new MyMouseListener());
		panel.setName("rendering performance");
		panel.setBackground(Color.WHITE);
		panel.getChart().getPlotRenderer().setBackground(Color.LIGHT_GRAY);
		// Range chosen to provoke interpolation.
		xaxis.setRange(105, 1015);
		graph.setAntialiasingEnabled(false);
		ChartRenderingToolkit.testComponent(panel, 1200, 800);
		graph.setTitle(title);
		for (int i = 0; i < 9; i++) {
			doStep();
		}
	}

	/**
	 * Adds two points of data, out of order, to the series.
	 */
	public void doStep() {
		DefaultTimestampedData tsd1Sin = new DefaultTimestampedData(m_time, Double.valueOf(m_sine.getValue(m_time)));
		DefaultTimestampedData tsd1Saw = new DefaultTimestampedData(m_time, Double.valueOf(m_saw.getValue(m_time)));
		m_time += 10;
		DefaultTimestampedData tsd2Sin = new DefaultTimestampedData(m_time, Double.valueOf(m_sine.getValue(m_time)));
		DefaultTimestampedData tsd2Saw = new DefaultTimestampedData(m_time, Double.valueOf(m_saw.getValue(m_time)));

		SINUS_DATA.addData(tsd2Sin);
		SAW_DATA.addData(tsd2Saw);
		SINUS_DATA.addData(tsd1Sin);
		SAW_DATA.addData(tsd1Saw);
		panel.repaint();
		m_time += 20;
	}
}
