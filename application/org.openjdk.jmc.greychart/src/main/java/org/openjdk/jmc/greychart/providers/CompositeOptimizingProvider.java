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
package org.openjdk.jmc.greychart.providers;

import java.awt.Polygon;
import java.util.Iterator;

import org.openjdk.jmc.ui.common.xydata.DataSeries;

import org.openjdk.jmc.greychart.YAxis;
import org.openjdk.jmc.greychart.impl.LongWorldToDeviceConverter;
import org.openjdk.jmc.greychart.impl.OptimizingProvider;
import org.openjdk.jmc.greychart.impl.SamplePoint;
import org.openjdk.jmc.greychart.impl.WorldToDeviceConverter;

public class CompositeOptimizingProvider implements OptimizingProvider {
	private final OptimizingProvider[] providers;

	public CompositeOptimizingProvider(OptimizingProvider[] providers) {
		this.providers = providers;
	}

	@Override
	public void setDataChanged(boolean changed) {
		for (OptimizingProvider provider : providers) {
			provider.setDataChanged(changed);
		}
	}

	@Override
	public boolean hasDataChanged() {
		return false;
	}

	@Override
	public Iterator<SamplePoint> getSamples(int width) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Polygon getSamplesPolygon(LongWorldToDeviceConverter xWorldToDevice, WorldToDeviceConverter yWorldToDevice) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setResolution(int resolution) {
		for (OptimizingProvider provider : providers) {
			provider.setResolution(resolution);
		}
	}

	@Override
	public boolean update() {
		boolean update = false;
		for (OptimizingProvider provider : providers) {
			if (provider.update()) {
				update = true;
			}
		}
		return update;
	}

	@Override
	public OptimizingProvider[] getChildren() {
		return providers;
	}

	@Override
	public DataSeries getDataSeries() {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getMaxX() {
		long max = Long.MIN_VALUE;
		for (OptimizingProvider provider : providers) {
			long maxX = provider.getMaxX();
			if (!Double.isNaN(maxX)) {
				max = Math.max(maxX, max);
			}
		}
		return max;
	}

	@Override
	public double getMaxY() {
		double max = Double.NEGATIVE_INFINITY;
		for (OptimizingProvider provider : providers) {
			double maxY = provider.getMaxY();
			if (!Double.isNaN(maxY)) {
				max = Math.max(maxY, max);
			}
		}
		return max;
	}

	@Override
	public double getMinY() {
		double min = Double.POSITIVE_INFINITY;
		for (OptimizingProvider provider : providers) {
			double minY = provider.getMinY();
			if (!Double.isNaN(minY)) {
				min = Math.min(minY, min);
			}
		}
		return min;
	}

	@Override
	public WorldToDeviceConverter getYSampleToDeviceConverterFor(YAxis yAxis) {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getMinX() {
		long min = Long.MAX_VALUE;
		for (OptimizingProvider provider : providers) {
			long minX = provider.getMinX();
			if (!Double.isNaN(minX)) {
				min = Math.min(minX, min);
			}
		}
		return min;
	}

	@Override
	public void setRange(long start, long end) {
		for (OptimizingProvider provider : providers) {
			provider.setRange(start, end);
		}
	}

	public void setIntegrate(boolean integrate) {
		// Ignore
	}
}
