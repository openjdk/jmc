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

/**
 * This is an averaging sampler. It will typically contains as many samples as the drawing area is
 * pixles wide.
 * <p>
 * Each bucket represents the average value of the events that span the bucket. Events must be added
 * in order.
 */
public final class AveragingSampleBuffer extends AbstractSampler {
	private double previousX = Double.NaN;
	private double previousY = Double.NaN;

	public AveragingSampleBuffer(int size) {
		super(size);
	}

	public void addDataPoint(double normalizedX, double currentY) {
		if (normalizedX < 0 || normalizedX > 1.0) {
			throw new IllegalArgumentException("Must add a normalized value [0, 1]! Value was " + normalizedX); //$NON-NLS-1$
		}
		addSamplePoint(getSize() * normalizedX, currentY);
	}

	private void addSamplePoint(double currentX, double currentY) {
		if (Double.isNaN(previousX)) {
			previousX = (int) ChartRenderingToolkit.fastFloor(currentX);
			previousY = currentY;
		}
		if (currentX > previousX) {
			addBucketSample(currentX, currentY);
		}
		previousX = currentX;
		previousY = currentY;
	}

	private void addBucketSample(double currentX, double currentY) {
		double k = (currentY - previousY) / (currentX - previousX);
		int prevBucket = getIndex(previousX);
		int currentBucket = Math.min(getIndex(currentX), getSize() - 1);
		if (prevBucket != currentBucket) {
			// x-range spans more than one bucket
			addSegmentToBucket(prevBucket, previousY, k, prevBucket + 1 - previousX);
			addSegmentToBucket(currentBucket, currentY, -k, currentX - currentBucket);
		} else {
			addSegmentToBucket(prevBucket, previousY, k, currentX - previousX);
		}
	}

	private void addSegmentToBucket(
		int bucketIndex, double segmentHeigth, double segmentGradient, double segmentWidth) {
		double segmentArea = (segmentHeigth + segmentGradient * segmentWidth / 2) * segmentWidth;
		SamplePoint sp = getSamplePoint(bucketIndex);	
		sp.y += segmentArea;
		sp.count++;
	}

	public void fixSamples() {
		int lastIndex = getIndex(previousX);
		if (lastIndex < getSize()) {
			addSegmentToBucket(lastIndex, previousY, 0, lastIndex + 1 - previousX);
		}
		invalidateStatistics();
	}
}
