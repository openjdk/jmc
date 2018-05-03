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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openjdk.jmc.ui.common.xydata.DataSeries;

import org.openjdk.jmc.greychart.DefaultMetadataProvider;
import org.openjdk.jmc.greychart.GreyChartPanel;
import org.openjdk.jmc.greychart.data.SeriesProviderSet;
import org.openjdk.jmc.greychart.impl.DefaultPieGreyChart;
import org.openjdk.jmc.greychart.util.ChartRenderingToolkit;

/**
 * A simple class testing the pie chart.
 */
public class PieChartTester {
	private static class MyMouseListener extends MouseAdapter {
		/**
		 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
		 */
		@Override
		public void mouseClicked(MouseEvent e) {
			System.out.println("Adding value!"); //$NON-NLS-1$
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
		DefaultPieGreyChart<Number> graph = new DefaultPieGreyChart<>();
		SeriesProviderSet<Number> seriesProvider = new SeriesProviderSet<>();

		addNumberSeries(graph, seriesProvider, 40);
		addNumberSeries(graph, seriesProvider, 60);
		addNumberSeries(graph, seriesProvider, 80);
		graph.setTitle("Test pie chart"); //$NON-NLS-1$
		graph.setMetadataProvider(new DefaultMetadataProvider());
		graph.setDataProvider(seriesProvider);
		GreyChartPanel panel = new GreyChartPanel(graph);
		panel.setName("default pie chart"); //$NON-NLS-1$
		panel.addMouseListener(new MyMouseListener());
		ChartRenderingToolkit.testComponent(panel, 320, 250);
	}

	private static void addNumberSeries(
		DefaultPieGreyChart<Number> graph, SeriesProviderSet<Number> seriesProvider, final Number figure) {
		DataSeries<Number> s = new DataSeries<Number>() {
			@Override
			public Iterator<Number> createIterator(long min, long max) {
				List<Number> number = new ArrayList<>();
				number.add(figure);
				return number.iterator();
			}
		};
		seriesProvider.addDataSeries(s);
	}
}
