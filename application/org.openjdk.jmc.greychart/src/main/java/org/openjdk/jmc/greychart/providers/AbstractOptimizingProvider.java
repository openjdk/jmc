/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2025, Datadog, Inc. All rights reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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

import java.util.Iterator;

import org.openjdk.jmc.common.xydata.DataSeries;
import org.openjdk.jmc.common.xydata.IXYData;
import org.openjdk.jmc.greychart.impl.OptimizingProvider;

/**
 * Abstract base class for OptimizingProvider implementations that provides common functionality for
 * tracking full data bounds independent of current range restrictions.
 */
public abstract class AbstractOptimizingProvider implements OptimizingProvider {
	private long m_dataMinX = Long.MAX_VALUE;
	private long m_dataMaxX = Long.MIN_VALUE;
	private double m_dataMinY = Double.POSITIVE_INFINITY;
	private double m_dataMaxY = Double.NEGATIVE_INFINITY;
	private boolean m_dataBoundsComputed = false;

	@Override
	public void setDataChanged(boolean changed) {
		if (changed) {
			m_dataBoundsComputed = false;
		}
	}

	@Override
	public long getDataMinX() {
		computeDataBounds();
		return m_dataMinX;
	}

	@Override
	public long getDataMaxX() {
		computeDataBounds();
		return m_dataMaxX;
	}

	@Override
	public double getDataMinY() {
		computeDataBounds();
		return m_dataMinY;
	}

	@Override
	public double getDataMaxY() {
		computeDataBounds();
		return m_dataMaxY;
	}

	/**
	 * Computes the full data bounds by iterating through all data in the series. This method is
	 * called lazily and caches the results until data changes. Subclasses can override this method
	 * to provide custom bounds computation.
	 */
	protected void computeDataBounds() {
		if (m_dataBoundsComputed) {
			return;
		}
		m_dataMinX = Long.MAX_VALUE;
		m_dataMaxX = Long.MIN_VALUE;
		m_dataMinY = Double.POSITIVE_INFINITY;
		m_dataMaxY = Double.NEGATIVE_INFINITY;

		DataSeries<IXYData<Long, Number>> dataSeries = getDataSeries();
		if (dataSeries != null) {
			// Iterate through all data to find true bounds
			Iterator<IXYData<Long, Number>> it = dataSeries.createIterator(Long.MIN_VALUE, Long.MAX_VALUE);
			while (it.hasNext()) {
				IXYData<Long, Number> data = it.next();
				long x = data.getX();
				double y = data.getY().doubleValue();

				m_dataMinX = Math.min(m_dataMinX, x);
				m_dataMaxX = Math.max(m_dataMaxX, x);
				m_dataMinY = Math.min(m_dataMinY, y);
				m_dataMaxY = Math.max(m_dataMaxY, y);
			}
		}
		m_dataBoundsComputed = true;
	}
}
