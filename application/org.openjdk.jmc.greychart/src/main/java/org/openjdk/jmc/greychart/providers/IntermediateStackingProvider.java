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

/**
 * A stacking {@link OptimizingProvider}. Stacks the sample lists from a number of optimizing
 * providers into a single sample list. Min and max Y values consider the stacked sample list.
 * <p>
 * Works best when the stacked optimizing providers return samples for all points in the range.
 */

public class IntermediateStackingProvider implements OptimizingProvider {

	private final OptimizingProvider[] providers;
	private final OptimizingProvider topProvider;

	private long m_requestedStartX = Long.MIN_VALUE;
	private long m_requestedEndX = Long.MAX_VALUE;
	private int m_requestedResolution = 0;

	private StackingBuffer m_sampleBuffer;

	private int m_lastSubSampleWidth = -1;
	private volatile boolean dataChangeOccured = false;

	/**
	 * @param topProvider
	 *            - The "main" provider
	 * @param providers
	 *            - All providers in the stack (including the topProvider)
	 */
	public IntermediateStackingProvider(OptimizingProvider topProvider, OptimizingProvider[] providers) {
		int nProviders = 0;
		for (OptimizingProvider prov : providers) {
			if (prov != null) {
				nProviders++;
			}
		}
		OptimizingProvider[] myProviders = new OptimizingProvider[nProviders];
		int index = 0;
		for (OptimizingProvider prov : providers) {
			if (prov != null) {
				myProviders[index] = prov;
				index++;
			}
		}
		this.providers = myProviders;
		this.topProvider = topProvider;
		m_sampleBuffer = new StackingBuffer(0);
	}

	@Override
	public void setDataChanged(boolean changed) {
		for (OptimizingProvider provider : providers) {
			provider.setDataChanged(changed);
		}
		dataChangeOccured = true;
	}

	@Override
	public boolean hasDataChanged() {
		if (dataChangeOccured) {
			return true;
		}
		for (OptimizingProvider provider : providers) {
			if (provider.hasDataChanged()) {
				return true;
			}
		}
		return false;
	}

	private boolean hasRangeChanged() {
		return getMinX() != m_requestedStartX || getMaxX() != m_requestedEndX;
	}

	private StackingBuffer createStackSampledBuffer(int width) {
		dataChangeOccured = false;
		m_lastSubSampleWidth = width;
		StackingBuffer stackSampleBuffer = new StackingBuffer(width);

		for (OptimizingProvider provider : providers) {
			Iterator<SamplePoint> it = provider.getSamples(width);
			stackSampleBuffer.startSeries();
			while (it.hasNext()) {
				SamplePoint point = it.next();
				stackSampleBuffer.addNonNormalizedDataPoint(point.x, point.y);
			}
		}
		return stackSampleBuffer;
	}

	@Override
	public Iterator<SamplePoint> getSamples(int width) {
		// FIXME: Currently not checking if we need to re-calculate the sample stack
		if (width != m_lastSubSampleWidth || hasDataChanged() || hasRangeChanged()) {
			m_sampleBuffer = createStackSampledBuffer(width);
		}
		return new SamplePointIterator(m_sampleBuffer.getSamples());
	}

	@Override
	public Polygon getSamplesPolygon(LongWorldToDeviceConverter xWorldToDevice, WorldToDeviceConverter yWorldToDevice) {
		final int deviceWidth = xWorldToDevice.getDeviceWidth();
		// Leaving space to close the polygon when rendering filled.
		int[] xs = new int[deviceWidth * 2 + 3];
		int[] ys = new int[deviceWidth * 2 + 3];

		int index = 0;
		if (yWorldToDevice.canCalculateDeviceCoordinate()) {
			Iterator<SamplePoint> it = getSamples(deviceWidth);
			int previousY = 0;
			/*
			 * If the stack contains sample counting providers, we want to render a visible bar for
			 * every x value where there are any samples, even if the bar would normally be rounded
			 * down to zero or one pixels in height. This feature is mainly intended for the range
			 * navigator in JFR. A 2 pixels high bar is the smallest that is useful/visible in the
			 * range navigator.
			 */
			// FIXME: We should formalize this so that this can be set optionally.
			boolean sampleCounting = topProvider instanceof SampleCountingProvider;
			int onePixelY = yWorldToDevice.getDeviceCoordinate(0) - 2;
			while (it.hasNext() && index < deviceWidth * 2) {
				SamplePoint sp = it.next();
				int currentY = yWorldToDevice.getDeviceCoordinate(sp.y);
				if (sampleCounting) {
					if (sp.y > 0 && currentY > onePixelY) {
						currentY = onePixelY;
					}
					if (currentY != previousY && index > 0) // Don't add a zero at the first index
					{
						xs[index] = sp.x;
						ys[index] = previousY;
						index++;
					}
					xs[index] = sp.x;
					ys[index] = currentY;
					index++;
					previousY = currentY;
				} else if (sp.count > 0) {
					xs[index] = sp.x;
					ys[index] = currentY;
					index++;
				}

			}
		}
		return new Polygon(xs, ys, index);
	}

	@Override
	public void setResolution(int resolution) {
		for (OptimizingProvider provider : providers) {
			provider.setResolution(resolution);
		}
		m_requestedResolution = resolution;
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
		return topProvider.getDataSeries();
	}

	@Override
	public long getMaxX() {
		return topProvider.getMaxX();
	}

	@Override
	public double getMaxY() {
		if (hasDataChanged() || hasRangeChanged() || m_sampleBuffer.getSize() == 0) {
			m_sampleBuffer = createStackSampledBuffer(m_requestedResolution);
		}
		return m_sampleBuffer.getMaxY();
	}

	@Override
	public double getMinY() {
		if (hasDataChanged() || hasRangeChanged() || m_sampleBuffer.getSize() == 0) {
			m_sampleBuffer = createStackSampledBuffer(m_requestedResolution);
		}
		return m_sampleBuffer.getMinY();
	}

	@Override
	public WorldToDeviceConverter getYSampleToDeviceConverterFor(YAxis yAxis) {
		// Assume all samples converted to world coordinates by this provider.
		double minY = yAxis.getMin().doubleValue();
		double maxY = yAxis.getMax().doubleValue();

		// Swapping minimum with maximum so we get zero at the bottom.
		return new WorldToDeviceConverter(yAxis.getRenderedHeight(), 0, minY, maxY);
	}

	@Override
	public long getMinX() {
		return topProvider.getMinX();
	}

	@Override
	public void setRange(long start, long end) {
		m_requestedStartX = start;
		m_requestedEndX = end;
		for (OptimizingProvider provider : providers) {
			provider.setRange(start, end);
		}
	}

	public void setIntegrate(boolean integrate) {
		// ignore
	}

}
