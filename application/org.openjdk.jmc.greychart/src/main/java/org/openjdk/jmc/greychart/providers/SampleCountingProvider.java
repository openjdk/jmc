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
 * A sample counting optimizing provider. Counts the number of samples at each X-value. The samples
 * can either be counted at the start of each event or for the entire duration of the event
 * (integrated).
 */

public class SampleCountingProvider implements OptimizingProvider {
	private final DataSeries<IXYData> m_dataSeries;
	private final double m_yMultiplier;
	private final XAxis m_xAxis;
	private final CancelService m_cancelService;

	private AbstractSampler m_sampleBuffer = new SampleCountingBuffer(0);

	private long m_startX;
	private long m_endX;
	private int m_requestedResolution = 0;
	private long m_requestedStartX = Long.MIN_VALUE;
	private long m_requestedEndX = Long.MAX_VALUE;

	private volatile boolean dataChangeOccured = false;
	private final boolean m_integrate;

	public SampleCountingProvider(DataSeries<IXYData> s, double yMultiplier, XAxis xAxis, CancelService cancelService,
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
	public Iterator<SamplePoint> getSamples(int width) {
		return new SamplePointIterator(m_sampleBuffer.getSamples());
	}

	@Override
	public Polygon getSamplesPolygon(LongWorldToDeviceConverter xWorldToDevice, WorldToDeviceConverter yWorldToDevice) {
		final int deviceWidth = xWorldToDevice.getDeviceWidth();
		// Leaving space to close the polygon when rendering filled.
		int[] xs = new int[deviceWidth * 3 + 3];
		int[] ys = new int[deviceWidth * 3 + 3];

		int index = 0;
		if (yWorldToDevice.canCalculateDeviceCoordinate()) {
			Iterator<SamplePoint> it = getSamples(deviceWidth);
			int previousY = 0;
			int zeroY = yWorldToDevice.getDeviceCoordinate(0);
			while (it.hasNext() && index < deviceWidth * 3) {
				SamplePoint sp = it.next();
				int currentY = yWorldToDevice.getDeviceCoordinate(sp.y);
				int currentX = sp.x;
				if (currentY != previousY && index > 0) {
					xs[index] = xs[index - 1];
					ys[index] = zeroY;
					index++;
					xs[index] = currentX;
					ys[index] = zeroY;
					index++;
				}
				xs[index] = currentX;
				ys[index] = currentY;
				index++;
				previousY = currentY;
			}
		}
		return new Polygon(xs, ys, index);
	}

	private AbstractSampler createSampleBuffer(int width) {
		Iterator<IXYData> it = m_dataSeries.createIterator(m_requestedStartX, m_requestedEndX);
		if (!it.hasNext()) {
			return isIntegrate() ? new IntegratingSampleCountingBuffer(0) : new SampleCountingBuffer(0);
		}
		AbstractSampler sampleBuffer = isIntegrate() ? new IntegratingSampleCountingBuffer(width)
				: new SampleCountingBuffer(width);
		long worldWidth = m_xAxis.getMax().longValue() - m_xAxis.getMin().longValue();
		long leftEdge = m_xAxis.getMin().longValue();
		IXYData data = findFirstPoint(sampleBuffer, it, worldWidth, leftEdge);

		long x = getXAsLong(data);
		if (x >= leftEdge && x <= (leftEdge + worldWidth)) {
			addXYDataPoint(sampleBuffer, worldWidth, leftEdge, data);
		}

		while (it.hasNext() && m_cancelService.isNotCancelled()) {
			data = it.next();
			x = getXAsLong(data);
			if (x < leftEdge && isIntegrate()) {
				addLeftEdgeCrossingValue(sampleBuffer, worldWidth, leftEdge, data);
			} else if (x < leftEdge + worldWidth) {
				addXYDataPoint(sampleBuffer, worldWidth, leftEdge, data);
			}
		}
		if (isIntegrate()) {
			((IntegratingSampleCountingBuffer) sampleBuffer).fixSamples();
		}
		return sampleBuffer;
	}

	private void addLeftEdgeCrossingValue(AbstractSampler sampleBuffer, long worldWidth, long leftEdge, IXYData data) {
		long x = getXAsLong(data);
		long y = getYAsLong(data);
		if (x + y >= leftEdge) {
			addIntegratedValue((IntegratingSampleCountingBuffer) sampleBuffer, worldWidth, leftEdge, leftEdge, y,
					x + y - leftEdge);
		}
	}

	private long getXAsLong(IXYData data) {
		return ((Number) data.getX()).longValue();
	}

	private long getYAsLong(IXYData data) {
		return ((Number) data.getY()).longValue();
	}

	private IXYData findFirstPoint(AbstractSampler sampleBuffer, Iterator<IXYData> it, long worldWidth, long leftEdge) {
		IXYData firstDataPoint = null;
		if (it.hasNext()) {
			firstDataPoint = it.next();
			long x = getXAsLong(firstDataPoint);
			// No boundary point - just return it.
			if (x >= leftEdge) {
				return firstDataPoint;
			}
		}
		// Look for boundary...
		while (it.hasNext()) {
			IXYData data = it.next();
			long x = getXAsLong(data);
			if (isIntegrate()) {
				addLeftEdgeCrossingValue(sampleBuffer, worldWidth, leftEdge, firstDataPoint);
			}
			if (x >= leftEdge) {
				return data;
			} else {
				// Still haven't crossed boundary...
				firstDataPoint = data;
			}
		}
		return firstDataPoint;
	}

	private void addXYDataPoint(AbstractSampler sampleBuffer, long worldWidth, long leftEdge, IXYData data) {
		if (isIntegrate()) {
			addIntegratedXYDataPoint((IntegratingSampleCountingBuffer) sampleBuffer, worldWidth, leftEdge, data);
		} else {
			addNormalXYDataPoint((SampleCountingBuffer) sampleBuffer, worldWidth, leftEdge, data);
		}
	}

	private void addNormalXYDataPoint(SampleCountingBuffer sampleBuffer, long worldWidth, long leftEdge, IXYData data) {
		double n = getNormalizedX(getXAsLong(data), worldWidth, leftEdge);
		double y = ((Number) data.getY()).doubleValue();
		sampleBuffer.addDataPoint(n, y);
	}

	private void addIntegratedValue(
		IntegratingSampleCountingBuffer sampleBuffer, long worldWidth, long leftEdge, long x, long y, long duration) {
		double n = getNormalizedX(x, worldWidth, leftEdge);
		double n2 = getNormalizedX(Math.min(leftEdge + worldWidth, x + duration), worldWidth, leftEdge);
		sampleBuffer.addDataPoint(n, n2, 1);
	}

	private void addIntegratedXYDataPoint(
		IntegratingSampleCountingBuffer sampleBuffer, long worldWidth, long leftEdge, IXYData data) {
		addIntegratedValue(sampleBuffer, worldWidth, leftEdge, getXAsLong(data), getYAsLong(data), getYAsLong(data));
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
		return m_sampleBuffer.getMaxY() * m_yMultiplier;
	}

	@Override
	public double getMinY() {
		return m_sampleBuffer.getMinY() * m_yMultiplier;
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

	private boolean isIntegrate() {
		return m_integrate;
	}

}
