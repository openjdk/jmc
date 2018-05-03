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
package org.openjdk.jmc.greychart.data;

import java.util.ArrayList;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

import org.openjdk.jmc.ui.common.xydata.DataSeries;
import org.openjdk.jmc.ui.common.xydata.DefaultTimestampedData;
import org.openjdk.jmc.ui.common.xydata.ITimestampedData;

import org.openjdk.jmc.greychart.testutil.DefaultSignalGenerator;

/**
 */
public class TimestampedDataProviderTest {
	public final static long X_SPAN = 60 * 60 * 1000 * 24; // 24h
	public final static long X_STEP = 100; // 20s
	public final static long PERIODS = 4;

	public final static double MAX_Y = 5.0;
	public final static double MIN_Y = -5.0;

	public static double maxX = X_SPAN;
	public static double minX = 0;

	@Test
	public void testAdd() {
		DataSeriesProvider<ITimestampedData> provider = getTestSet();
		Assert.assertNotNull(provider);
	}

	public static SeriesProviderSet<ITimestampedData> getTestSet() {
		SeriesProviderSet<ITimestampedData> provider = new SeriesProviderSet<>();
		DefaultSignalGenerator sinus = new DefaultSignalGenerator(DefaultSignalGenerator.SINUS, X_SPAN / PERIODS, MIN_Y,
				MAX_Y, 0.0, 0);
		addDataSeries(provider, sinus);

		DefaultSignalGenerator saw = new DefaultSignalGenerator(DefaultSignalGenerator.SAW, X_SPAN / PERIODS, MIN_Y,
				MAX_Y, 0.0, 0);
		addDataSeries(provider, saw);

		DefaultSignalGenerator sinus2 = new DefaultSignalGenerator(DefaultSignalGenerator.SINUS, X_SPAN / PERIODS,
				MIN_Y / 2, MAX_Y / 2, MAX_Y / 2, 0);
		addDataSeries(provider, sinus2);

		return provider;
	}

	private static void addDataSeries(
		SeriesProviderSet<ITimestampedData> provider, final DefaultSignalGenerator dataSeries) {
		provider.addDataSeries(new DataSeries<ITimestampedData>() {
			private ArrayList<ITimestampedData> data;

			@Override
			public Iterator<ITimestampedData> createIterator(long min, long max) {
				if (min == Double.NEGATIVE_INFINITY) {
					min = 0;
				}
				if (max == Double.POSITIVE_INFINITY) {
					max = min + X_SPAN;
				}
				if (data == null) {
					minX = min;
					maxX = max;
					data = new ArrayList<>();
					for (double x = min; x < max; x += X_STEP) {
						long time = Math.round(x);
						data.add(new DefaultTimestampedData(time, dataSeries.getValue(time)));
					}
				}
				return data.iterator();
			}

		});
	}

	public static Number getMinX() {
		return minX;
	}

	public static Number getMaxX() {
		return maxX;
	}
}
