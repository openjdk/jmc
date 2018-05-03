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
 * This is an integrating sample buffer for the {@link SubsamplingProvider}.
 * <p>
 * Each bucket represents the highest Y value of the events that span the bucket. If events overlap
 * they are "joined", so that the Y value will be equal to the highest Y value of the events that
 * overlap. If you want overlapping events to be "stacked", you must put them in separate data
 * series and stack them.
 * <p>
 * The method fixSamples() must be called once all data points are added in order to fill out all
 * the values in the sample buffer.
 */
public class IntegratingSubsamplingBuffer extends AbstractSampler {

	public IntegratingSubsamplingBuffer(int size) {
		super(size);
	}

	public void addDataPoint(double normalizedStartX, double normalizedEndX, double value) {
		if (normalizedStartX < 0 || normalizedStartX > 1.0) {
			throw new IllegalArgumentException("Must add a normalized value [0, 1]! Value was " + normalizedStartX); //$NON-NLS-1$
		}
		if (normalizedEndX < 0 || normalizedEndX > 1.0) {
			throw new IllegalArgumentException("Must add a normalized value [0, 1]! Value was " + normalizedEndX); //$NON-NLS-1$
		}

		addStartSamplePoint(getSize() * normalizedStartX, value);
		addEndSamplePoint(Math.max(getSize() * normalizedEndX, getSize() * normalizedStartX + 1), -value);
	}

	protected void addStartSamplePoint(double currentX, double currentY) {
		int currentIndex = getIndex(currentX);
		if (currentIndex >= 0 && currentIndex < getSize()) {
			SamplePoint sp = getSamplePoint(currentIndex);
			sp.count++;
			sp.y += currentY;
			sp.max = Math.max(currentY, sp.max);
		}
	}

	protected void addEndSamplePoint(double currentX, double currentY) {
		int currentIndex = getIndex(currentX);
		if (currentIndex >= 0 && currentIndex < getSize()) {
			SamplePoint sp = getSamplePoint(currentIndex);
			sp.count++;
			sp.y += currentY;
		}
	}

	public void fixSamples() {
		double currentSum = 0;
		double currentY = 0;

		for (SamplePoint sp : getSamples()) {
			if (sp != null) {
				sp.min = 0;
				double maxY = sp.max;
				double incY = sp.y;
				double oldY = currentY;

				currentSum = currentSum + incY;

				double maxNewY = Math.max(currentY, maxY);
				currentY = Math.min(maxNewY, currentSum);

				sp.y = oldY;
				sp.yOut = currentY;
				sp.count = 2;
			}
		}
		if (getSize() > 0 && getSamples()[(getSize() - 1)] == null && currentY > 0) {
			SamplePoint point = getSamplePoint(getSize() - 1);
			point.y = currentY;
			point.yOut = currentY;
			point.count = 1;
		}
		invalidateStatistics();
	}
}
