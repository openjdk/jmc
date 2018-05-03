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

import org.openjdk.jmc.greychart.impl.SamplePoint;
import org.openjdk.jmc.greychart.util.ChartRenderingToolkit;

public abstract class AbstractSampler {

	private final SamplePoint[] samples;
	protected double m_maxY = Double.NEGATIVE_INFINITY;
	protected double m_minY = Double.POSITIVE_INFINITY;

	public AbstractSampler(int size) {
		samples = new SamplePoint[Math.max(1, size)];
	}

	protected SamplePoint getSamplePoint(int index) {
		if (samples[index] == null) {
			samples[index] = new SamplePoint(index);
		}
		return samples[index];
	}

	protected int getIndex(double x) {
		return (int) ChartRenderingToolkit.fastFloor(x);
	}

	public SamplePoint[] getSamples() {
		return samples;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < samples.length; i++) {
			builder.append(String.valueOf(i));
			builder.append("  "); //$NON-NLS-1$
			builder.append(samples[i]);
			builder.append('\n');
		}
		return builder.toString();
	}

	public int getSize() {
		return samples.length;
	}

	public double getMaxY() {
		calculateStatistics();
		return m_maxY;
	}

	public double getMinY() {
		calculateStatistics();
		return m_minY;
	}

	private void calculateStatistics() {
		if (Double.isInfinite(m_maxY)) {
			// Note: if m_maxY is infinite, then m_minY should be infinite too
			m_maxY = Double.NaN;
			m_minY = Double.NaN;
			for (SamplePoint sample : samples) {
				if (sample != null) {
					if (Double.isNaN(m_maxY)) {
						// Note: if m_maxY is NaN, then m_minY should be NaN too
						m_maxY = sample.y;
						m_minY = sample.y;
					}
					m_maxY = Math.max(sample.y, m_maxY);
					m_minY = Math.min(sample.y, m_minY);
				}
			}
		}
	}

	protected void invalidateStatistics() {
		m_maxY = Double.NEGATIVE_INFINITY;
		m_minY = Double.POSITIVE_INFINITY;
	}
}
