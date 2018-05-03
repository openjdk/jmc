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
package org.openjdk.jmc.greychart.impl;

import org.junit.Assert;
import org.junit.Test;

import org.openjdk.jmc.greychart.providers.SubsamplingBuffer;

public class SubsamplingBufferTest {
	@Test
	public void testSingleValue() {
		SubsamplingBuffer buffer = new SubsamplingBuffer(5);
		buffer.addDataPoint(0.5, 100);
		Assert.assertEquals(1, countValids(buffer));
		Assert.assertEquals(100.0, buffer.getSamples()[2].y, 0);
	}

	@Test
	public void testTwoValues() {
		SubsamplingBuffer buffer = new SubsamplingBuffer(5);
		buffer.addDataPoint(0.5, 50);
		buffer.addDataPoint(0.54, 100);
		Assert.assertEquals(1, countValids(buffer));
		Assert.assertEquals(2, buffer.getSamples()[2].count);
		Assert.assertEquals(50, Math.round(buffer.getSamples()[2].y));
		Assert.assertEquals(100, Math.round(buffer.getSamples()[2].yOut));
		Assert.assertEquals(100, Math.round(buffer.getSamples()[2].max));
		Assert.assertEquals(50, Math.round(buffer.getSamples()[2].min));
	}

	@Test
	public void testFourValues() {
		SubsamplingBuffer buffer = new SubsamplingBuffer(5);
		buffer.addDataPoint(0.2, 2f);
		buffer.addDataPoint(0.4, 4f);
		buffer.addDataPoint(0.6, 6f);
		buffer.addDataPoint(0.8, 8f);
		Assert.assertEquals(4, countValids(buffer));
		SamplePoint[] samples = buffer.getSamples();

		for (int i = 1; i < samples.length; i++) {
			Assert.assertEquals(1, samples[i].count);
			double value = i * 2;
			Assert.assertEquals(value, samples[i].y, 0);
			Assert.assertEquals(value, samples[i].yOut, 0);
			Assert.assertEquals(value, samples[i].max, 0);
			Assert.assertEquals(value, samples[i].min, 0);
		}
	}

	public static int countValids(SubsamplingBuffer buffer) {
		int valids = 0;
		for (SamplePoint p : buffer.getSamples()) {
			if (p != null) {
				valids++;
			}
		}
		return valids;
	}
}
