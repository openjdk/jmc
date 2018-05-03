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

/**
 * This is a sub-sampling sampler for the {@link SubsamplingProvider}. It will at most contain four
 * times the range, but typically much less if the amount of samples is less than the number of
 * buckets (the resolution).
 * <p>
 * Each bucket represents the in/max/min/out value of the samples in the bucket.
 */
public class SubsamplingBuffer extends AbstractSampler {

	public SubsamplingBuffer(int size) {
		super(size);
	}

	public void addDataPoint(double normalizedX, double currentY) {
		if (normalizedX < 0 || normalizedX > 1.0) {
			throw new IllegalArgumentException("Must add a normalized value [0, 1]! Value was " + normalizedX); //$NON-NLS-1$
		}
		double currentX = getSize() * normalizedX;
		int currentIndex = normalizedX == 1.0 ? getSize() - 1 : getIndex(currentX);
		SamplePoint point = getSamplePoint(currentIndex);

		point.min = Math.min(point.min, currentY);
		point.max = Math.max(point.max, currentY);

		if (currentX < point.xIn) {
			point.xIn = currentX;
			point.y = currentY;
		}

		if (currentX >= point.xOut) {
			point.xOut = currentX;
			point.yOut = currentY;
		}

		point.count++;
		m_maxY = Math.max(m_maxY, point.max);
		m_minY = Math.min(m_minY, point.min);
	}
}
