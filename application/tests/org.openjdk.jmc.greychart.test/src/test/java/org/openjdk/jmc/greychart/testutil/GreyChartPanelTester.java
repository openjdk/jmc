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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.openjdk.jmc.greychart.DefaultMetadataProvider;
import org.openjdk.jmc.greychart.GreyChartPanel;
import org.openjdk.jmc.greychart.TickFormatter;
import org.openjdk.jmc.greychart.data.SeriesProviderSet;
import org.openjdk.jmc.greychart.data.TimestampedDataProviderTest;
import org.openjdk.jmc.greychart.impl.DateXAxis;
import org.openjdk.jmc.greychart.impl.DefaultXYGreyChart;
import org.openjdk.jmc.greychart.impl.DefaultYAxis;
import org.openjdk.jmc.greychart.util.ChartRenderingToolkit;
import org.openjdk.jmc.ui.common.xydata.ITimestampedData;

/**
 * Little test program creating a graph containing a huge amount of data.
 */
public class GreyChartPanelTester {
	private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("HH:mm:ss"); //$NON-NLS-1$

	/**
	 * Program entry point.
	 *
	 * @param args
	 *            disregarded
	 */
	public static void main(String[] args) {
		DefaultXYGreyChart<ITimestampedData> graph = new DefaultXYGreyChart<>();
		final SeriesProviderSet<ITimestampedData> provider = TimestampedDataProviderTest.getTestSet();
		DateXAxis xaxis = new DateXAxis(graph);
		xaxis.setRange(TimestampedDataProviderTest.getMinX(), TimestampedDataProviderTest.getMaxX());
		xaxis.setFormatter(new TickFormatter() {
			@Override
			public String format(Number value, Number min, Number max, Number labelDistance) {
				return FORMATTER.format(new Date(value.longValue()));
			}

			@Override
			public String getUnitString(Number min, Number max) {
				return ""; //$NON-NLS-1$
			}
		});

		DefaultYAxis yaxis = new DefaultYAxis(graph);
		yaxis.setMin(Double.valueOf(-5.0));
		yaxis.setMax(Double.valueOf(5.0));
		graph.setMetadataProvider(new DefaultMetadataProvider());
		graph.setDataProvider(provider);
		xaxis.setVisible(false);
		yaxis.setVisible(false);
		graph.setXAxis(xaxis);
		graph.addYAxis(yaxis);
		graph.setAntialiasingEnabled(true);

//		((DefaultXYLineRenderer) graph.getSeriesPlotRenderer()).setMode(DefaultXYLineRenderer.RENDERING_MODE_AVERAGING);
		GreyChartPanel panel = new GreyChartPanel(graph);
		panel.setName(" graph with lot's of entries"); //$NON-NLS-1$
		panel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				System.out.println("Changed structure!"); //$NON-NLS-1$
				provider.sendNotification();
			}
		});
		ChartRenderingToolkit.testComponent(panel, 800, 600);

	}
}
