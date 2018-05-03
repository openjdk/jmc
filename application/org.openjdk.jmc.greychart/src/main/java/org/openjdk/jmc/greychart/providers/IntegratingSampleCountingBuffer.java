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
 * This is an integrating buffer for the sample counting provider.
 * <p>
 * Each bucket represents the number of events that span that bucket.
 * <p>
 * The method fixSamples() must be called once all data points are added in order to fill out all
 * the values in the sample buffer.
 */
public class IntegratingSampleCountingBuffer extends AbstractSampler {

	public IntegratingSampleCountingBuffer(int size) {
		super(size);
	}

	public void addDataPoint(double normalizedStartX, double normalizedEndX, double value) {
		if (normalizedStartX < 0 || normalizedStartX > 1.0) {
			throw new IllegalArgumentException("Must add a normalized value [0, 1]! Value was " + normalizedStartX); //$NON-NLS-1$
		}
		if (normalizedEndX < 0 || normalizedEndX > 1.0) {
			throw new IllegalArgumentException("Must add a normalized value [0, 1]! Value was " + normalizedEndX); //$NON-NLS-1$
		}
		addSamplePoint(getSize() * normalizedStartX, value);
		// FIXME: not safe, end might end up outside buffer
		addSamplePoint(Math.max(getSize() * normalizedEndX, getSize() * normalizedStartX + 1), -value);
	}

	private void addSamplePoint(double currentX, double currentY) {
		int currentIndex = getIndex(currentX);
		if (currentIndex >= 0 && currentIndex < getSize()) {
			SamplePoint point = getSamplePoint(currentIndex);
			point.count++;
			point.y += currentY;
		}
	}

	public void fixSamples() {
		double currentY = 0;
		for (int n = 0; n < getSize(); n++) {
			SamplePoint point = getSamplePoint(n);
			point.y = point.y + currentY;
			currentY = point.y;
		}
		if (getSize() > 0 && getSamples()[(getSize() - 1)] == null && currentY > 0) {
			getSamplePoint(getSize() - 1).y = currentY;
		}
		invalidateStatistics();
	}
}
