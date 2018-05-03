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
 * An {@link OptimizingProvider} that provides a view of the data by averaging the values at each
 * point.
 */

public final class AveragingProvider implements OptimizingProvider {
	private final DataSeries<IXYData> m_dataSeries;
	private final double m_yMultiplier;
	private final XAxis m_xAxis;
	private final CancelService m_cancelService;
	private AveragingSampleBuffer m_sampleBuffer = new AveragingSampleBuffer(0);
	private long m_startX;
	private long m_endX;
	private int m_requestedResolution = 0;
	private long m_requestedStartX = Long.MIN_VALUE;
	private long m_requestedEndX = Long.MAX_VALUE;
	private volatile boolean dataChangeOccured = false;

	public AveragingProvider(DataSeries<IXYData> s, double yMultiplier, XAxis xAxis, CancelService cancelService) {
		m_dataSeries = s;
		m_yMultiplier = yMultiplier;
		m_cancelService = cancelService;
		m_xAxis = xAxis;
	}

	@Override
	public boolean update() {
		if (hasRangeChanged() || hasDataChanged() || isSampleBufferInvalid()) {
			m_sampleBuffer = createSampleBuffer(m_requestedResolution);
			m_startX = m_requestedStartX;
			m_endX = m_requestedEndX;
			dataChangeOccured = false;
			return true;
		}
		return false;
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

	@Override
	public Iterator<SamplePoint> getSamples(int width) {
		return new SamplePointIterator(m_sampleBuffer.getSamples());
	}

	@Override
	public Polygon getSamplesPolygon(LongWorldToDeviceConverter xWorldToDevice, WorldToDeviceConverter yWorldToDevice) {
		final int deviceWidth = xWorldToDevice.getDeviceWidth();
		// Leaving space to close the polygon when rendering filled.
		int[] xs = new int[deviceWidth + 3];
		int[] ys = new int[deviceWidth + 3];

		int index = 0;
		if (yWorldToDevice.canCalculateDeviceCoordinate()) {
			Iterator<SamplePoint> it = getSamples(deviceWidth);
			while (it.hasNext() && index < deviceWidth) {
				SamplePoint sp = it.next();
				xs[index] = sp.x;
				ys[index] = yWorldToDevice.getDeviceCoordinate(sp.y);
				index++;
			}
		}
		return new Polygon(xs, ys, index);
	}

	private AveragingSampleBuffer createSampleBuffer(int width) {
		Iterator<IXYData> it = m_dataSeries.createIterator(m_requestedStartX, m_requestedEndX);
		if (!it.hasNext()) {
			return new AveragingSampleBuffer(0);
		}
		long worldWidth = m_xAxis.getMax().longValue() - m_xAxis.getMin().longValue();
		long leftEdge = m_xAxis.getMin().longValue();
		AveragingSampleBuffer sampleBuffer = new AveragingSampleBuffer(width);

		IXYData data = addFirstBoundaryPoint(sampleBuffer, it, worldWidth, leftEdge);

		long x = getXAsLong(data);
		long oldx = getXAsLong(data);
		if (x >= leftEdge && x <= (leftEdge + worldWidth)) {
			addXYDataPoint(sampleBuffer, worldWidth, leftEdge, data);
		}

		while (it.hasNext() && x < leftEdge + worldWidth && m_cancelService.isNotCancelled()) {
			IXYData newData = it.next();
			x = getXAsLong(newData);
			if (x < oldx) {
				throw new IllegalArgumentException("Data points out of order: " + x + " is lower than " + oldx); //$NON-NLS-1$ //$NON-NLS-2$
			} else if (x >= leftEdge + worldWidth) {
				// adds the last interpolated point
				addInterpolatedNormalizedPoint(sampleBuffer, 1.0, data, newData, worldWidth, leftEdge);
			} else {
				addXYDataPoint(sampleBuffer, worldWidth, leftEdge, newData);
				oldx = x;
			}
			data = newData;
		}
		sampleBuffer.fixSamples();
		return sampleBuffer;
	}

	private long getXAsLong(IXYData data) {
		return ((Number) data.getX()).longValue();
	}

	private IXYData addFirstBoundaryPoint(
		AveragingSampleBuffer sampleBuffer, Iterator<IXYData> it, long worldWidth, long leftEdge) {
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
			if (x >= leftEdge) {
				addInterpolatedNormalizedPoint(sampleBuffer, 0.0, firstDataPoint, data, worldWidth, leftEdge);
				if (x >= leftEdge + worldWidth) {
					// The first value across the boundary is outside the graph on the right side
					addInterpolatedNormalizedPoint(sampleBuffer, 1.0, firstDataPoint, data, worldWidth, leftEdge);
				}
				return data;
			} else {
				// Still haven't crossed boundary...
				firstDataPoint = data;
			}
		}
		return firstDataPoint;
	}

	private void addInterpolatedNormalizedPoint(
		AveragingSampleBuffer sampleBuffer, double boundary, IXYData beforeData, IXYData afterData, long worldWidth,
		long leftEdge) {
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

	private void addXYDataPoint(AveragingSampleBuffer sampleBuffer, long worldWidth, long leftEdge, IXYData data) {
		double n = getNormalizedX(data, worldWidth, leftEdge);
		double y = ((Number) data.getY()).doubleValue();
		sampleBuffer.addDataPoint(n, y);
	}

	private double getNormalizedX(IXYData data, long worldWidth, long leftEdge) {
		return ((double) (getXAsLong(data) - leftEdge)) / ((double) worldWidth);
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

	public void setIntegrate(boolean integrate) {
		// Ignore
	}
}
