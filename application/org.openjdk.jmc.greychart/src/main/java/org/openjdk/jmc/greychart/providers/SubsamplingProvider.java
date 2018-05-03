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
import org.openjdk.jmc.ui.common.xydata.IXYData;

import org.openjdk.jmc.greychart.XAxis;
import org.openjdk.jmc.greychart.YAxis;
import org.openjdk.jmc.greychart.impl.CancelService;
import org.openjdk.jmc.greychart.impl.LongWorldToDeviceConverter;
import org.openjdk.jmc.greychart.impl.OptimizingProvider;
import org.openjdk.jmc.greychart.impl.SamplePoint;
import org.openjdk.jmc.greychart.impl.WorldToDeviceConverter;

/**
 * This is a {@link OptimizingProvider} which provides a view of the data through sub-sampling. The
 * resulting polygon will, at most, be proportional to 4 times the number of pixels in the
 * resolution.
 * <p>
 * In evenly distributed sparse (i.e. less data points than resolution) graphs, the number of data
 * points will be proportional to the number of data points.
 * <p>
 * The graph will closely resemble what the graph would have looked like if every single data point
 * would have been drawn directly.
 */
public final class SubsamplingProvider implements OptimizingProvider {
	private final DataSeries<IXYData> m_dataSeries;
	private final double m_yMultiplier;
	private final XAxis m_xAxis;
	private AbstractSampler m_sampleBuffer;

	private long m_startX;
	private long m_endX;
	private int m_requestedResolution = 0;
	private long m_requestedStartX = Long.MIN_VALUE;
	private long m_requestedEndX = Long.MAX_VALUE;
	private volatile boolean dataChangeOccured = false;
	private final boolean m_integrate;
	private final CancelService m_cancelService;

	public SubsamplingProvider(DataSeries<IXYData> s, double yMultiplier, XAxis xAxis, CancelService cancelService,
			boolean integrate) {
		m_dataSeries = s;
		m_yMultiplier = yMultiplier;
		m_cancelService = cancelService;
		m_xAxis = xAxis;
		m_integrate = integrate;
	}

	@Override
	public boolean update() {
		if (isScheduleResample()) {
			scheduleResample(m_requestedResolution);
			dataChangeOccured = false;
			return true;
		}
		return false;
	}

	private boolean isScheduleResample() {
		return hasRangeChanged() || hasDataChanged() || isSampleBufferInvalid();
	}

	private boolean hasRangeChanged() {
		return m_startX != m_requestedStartX || m_endX != m_requestedEndX;
	}

	@Override
	public void setDataChanged(boolean changed) {
		dataChangeOccured = changed;
	}

	@Override
	public boolean hasDataChanged() {
		return dataChangeOccured;
	}

	private boolean isSampleBufferInvalid() {
		return m_sampleBuffer == null || m_sampleBuffer.getSize() != m_requestedResolution;
	}

	@Override
	public void setResolution(int resolution) {
		m_requestedResolution = resolution;
	}

	private void scheduleResample(int resolution) {
		m_sampleBuffer = createSampleBuffer(resolution);
		m_startX = m_requestedStartX;
		m_endX = m_requestedEndX;
	}

	@Override
	public Polygon getSamplesPolygon(LongWorldToDeviceConverter xWorldToDevice, WorldToDeviceConverter yWorldToDevice) {
		final int deviceWidth = xWorldToDevice.getDeviceWidth();
		int resultingSamples = calculateResultingSamples(deviceWidth);

		// Leaving space to close the polygon when rendering filled.
		int[] xs = new int[resultingSamples + 3];
		int[] ys = new int[resultingSamples + 3];
		int index = 0;

		if (yWorldToDevice.canCalculateDeviceCoordinate()) {
			Iterator<SamplePoint> it = getSamples(deviceWidth);
			while (it.hasNext() && index < resultingSamples) {
				SamplePoint sp = it.next();
				xs[index] = sp.x;

				switch ((int) sp.count) {
				case 0:
					assert (false);
					break;
				case 1:
					ys[index++] = yWorldToDevice.getDeviceCoordinate(sp.y);
					break;
				case 2:
					ys[index] = yWorldToDevice.getDeviceCoordinate(sp.y);
					xs[++index] = sp.x;
					ys[index++] = yWorldToDevice.getDeviceCoordinate(sp.yOut);
					break;
				default:
					ys[index] = yWorldToDevice.getDeviceCoordinate(sp.y);
					xs[++index] = sp.x;
					ys[index] = yWorldToDevice.getDeviceCoordinate(sp.min);
					xs[++index] = sp.x;
					ys[index] = yWorldToDevice.getDeviceCoordinate(sp.max);
					xs[++index] = sp.x;
					ys[index++] = yWorldToDevice.getDeviceCoordinate(sp.yOut);
				}
			}
		}
		return new Polygon(xs, ys, index);
	}

	// This is to avoid to allocate more than two arrays, or use lists,
	// which would lead to a lot of object allocations.

	// This could be more optimized...
	private int calculateResultingSamples(int deviceWidth) {
		int resultCount = 0;
		for (Iterator<SamplePoint> iter = getSamples(deviceWidth); iter.hasNext();) {
			resultCount += calculateNeededSamples(iter.next());
		}
		return resultCount;
	}

	private int calculateNeededSamples(SamplePoint sp) {
		switch ((int) sp.count) {
		case 0:
			assert (false);
			return 0;
		case 1:
			return 1;
		case 2:
			return 2;
		default:
			return 4;
		}
	}

	private AbstractSampler createSampleBuffer(int width) {
		Iterator<IXYData> it = m_dataSeries.createIterator(m_requestedStartX, m_requestedEndX);
		if (!it.hasNext()) {
			return isIntegrate() ? new IntegratingSubsamplingBuffer(0) : new SubsamplingBuffer(0);
		}

		AbstractSampler sampleBuffer = isIntegrate() ? new IntegratingSubsamplingBuffer(width)
				: new SubsamplingBuffer(width);
		long worldWidth = m_xAxis.getMax().longValue() - m_xAxis.getMin().longValue();
		long leftEdge = m_xAxis.getMin().longValue();
		long rightEdge = leftEdge + worldWidth;

		IXYData leftMost = null;
		IXYData rightMost = null;
		IXYData rightMostWithin = null;
		IXYData leftMostWithin = null;

		while (it.hasNext() && m_cancelService.isNotCancelled()) {
			IXYData newData = it.next();
			long x = getXAsLong(newData);
			if (x < leftEdge && (leftMost == null || x >= getXAsLong(leftMost))) {
				leftMost = newData;
			} else if (x > rightEdge && (rightMost == null || x < getXAsLong(rightMost))) {
				rightMost = newData;
			}
			if (x >= leftEdge && x <= rightEdge) {
				addXYDataPoint(sampleBuffer, worldWidth, leftEdge, newData);
				if (leftMostWithin == null) {
					leftMostWithin = newData;
					rightMostWithin = newData;
				} else {
					if (getXAsLong(leftMostWithin) > x) {
						leftMostWithin = newData;
					} else if (getXAsLong(rightMostWithin) <= x) {
						rightMostWithin = newData;
					}
				}
			}
		}

		if (isIntegrate()) {
			((IntegratingSubsamplingBuffer) sampleBuffer).fixSamples();
		} else {
			// If we had a sample left of the left edge, or right of the right edge, we must add an interpolated
			// sample at the edge (unless the actual internal values were already on the borders).
			leftMostWithin = leftMostWithin == null ? rightMost : leftMostWithin;
			rightMostWithin = rightMostWithin == null ? leftMost : rightMostWithin;
			if (leftMost != null && leftMostWithin != null && getXAsLong(leftMostWithin) != leftEdge
					&& getXAsLong(leftMost) < leftEdge) {
				addInterpolatedNormalizedPoint((SubsamplingBuffer) sampleBuffer, 0.0, leftMost, leftMostWithin,
						worldWidth, leftEdge);
			}
			if (rightMost != null && rightMostWithin != null && getXAsLong(rightMostWithin) != rightEdge
					&& getXAsLong(rightMost) > rightEdge) {
				addInterpolatedNormalizedPoint((SubsamplingBuffer) sampleBuffer, 1.0, rightMostWithin, rightMost,
						worldWidth, leftEdge);
			}
		}
		return sampleBuffer;
	}

	private long getXAsLong(IXYData data) {
		return ((Number) data.getX()).longValue();
	}

	private long getYAsLong(IXYData data) {
		return ((Number) data.getY()).longValue();
	}

	private void addInterpolatedNormalizedPoint(
		SubsamplingBuffer sampleBuffer, double boundary, IXYData beforeData, IXYData afterData, long worldWidth,
		long leftEdge) {
		assert (!isIntegrate());
		double n1 = getNormalizedX(beforeData, worldWidth, leftEdge);
		double n2 = getNormalizedX(afterData, worldWidth, leftEdge);
		double y1 = getY(beforeData);
		double y2 = getY(afterData);
		double k = (y2 - y1) / (n2 - n1);
		double yResult = (boundary - n1) * k + y1;
		sampleBuffer.addDataPoint(boundary, yResult);
	}

	private double getY(IXYData data) {
		return ((Number) data.getY()).doubleValue();
	}

	private void addXYDataPoint(AbstractSampler sampleBuffer, long worldWidth, long leftEdge, IXYData data) {
		if (isIntegrate()) {
			addIntegratedXYDataPoint((IntegratingSubsamplingBuffer) sampleBuffer, worldWidth, leftEdge, data);
		} else {
			addNormalXYDataPoint((SubsamplingBuffer) sampleBuffer, worldWidth, leftEdge, data);
		}
	}

	private void addNormalXYDataPoint(SubsamplingBuffer sampleBuffer, long worldWidth, long leftEdge, IXYData data) {
		double n = getNormalizedX(data, worldWidth, leftEdge);
		double y = ((Number) data.getY()).doubleValue();
		sampleBuffer.addDataPoint(n, y);
	}

	private void addIntegratedValue(
		IntegratingSubsamplingBuffer sampleBuffer, long worldWidth, long leftEdge, long x, long y, long duration) {
		double n = getNormalizedX(x, worldWidth, leftEdge);
		double n2 = getNormalizedX(Math.min(leftEdge + worldWidth, x + duration), worldWidth, leftEdge);
		sampleBuffer.addDataPoint(n, n2, y);
	}

	private void addIntegratedXYDataPoint(
		IntegratingSubsamplingBuffer sampleBuffer, long worldWidth, long leftEdge, IXYData data) {
		addIntegratedValue(sampleBuffer, worldWidth, leftEdge, getXAsLong(data), getYAsLong(data), getYAsLong(data));
	}

	private double getNormalizedX(IXYData data, long worldWidth, long leftEdge) {
		return ((double) (getXAsLong(data) - leftEdge)) / ((double) worldWidth);
	}

	private double getNormalizedX(long x, long worldWidth, long leftEdge) {
		return ((double) (x - leftEdge)) / ((double) worldWidth);
	}

	@Override
	public DataSeries getDataSeries() {
		return m_dataSeries;
	}

	@Override
	public OptimizingProvider[] getChildren() {
		return new OptimizingProvider[0];
	}

	@Override
	public synchronized long getMaxX() {
		return m_endX;
	}

	@Override
	public synchronized long getMinX() {
		return m_startX;
	}

	@Override
	public double getMaxY() {
		return m_sampleBuffer == null ? Double.NaN : (m_sampleBuffer.getMaxY() * m_yMultiplier);

	}

	@Override
	public double getMinY() {
		return m_sampleBuffer == null ? Double.NaN : (m_sampleBuffer.getMinY() * m_yMultiplier);
	}

	@Override
	public WorldToDeviceConverter getYSampleToDeviceConverterFor(YAxis yAxis) {
		double minY = yAxis.getMin().doubleValue() / m_yMultiplier;
		double maxY = yAxis.getMax().doubleValue() / m_yMultiplier;

		// swapping minimum with maximum so we get zero at the bottom.
		return new WorldToDeviceConverter(yAxis.getRenderedHeight(), 0, minY, maxY);
	}

	@Override
	public synchronized void setRange(long start, long end) {
		m_requestedStartX = start;
		m_requestedEndX = end;
		update();
	}

	@Override
	public Iterator<SamplePoint> getSamples(int width) {
		return new SamplePointIterator(m_sampleBuffer.getSamples());
	}

	private boolean isIntegrate() {
		return m_integrate;
	}
}
