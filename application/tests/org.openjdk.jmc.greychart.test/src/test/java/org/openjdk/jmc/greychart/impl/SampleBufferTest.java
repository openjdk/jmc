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
import org.openjdk.jmc.greychart.providers.AveragingSampleBuffer;

public class SampleBufferTest {

	@Test
	public void testSingleValue() {
		AveragingSampleBuffer buffer = new AveragingSampleBuffer(20);
		buffer.addDataPoint(0.5, 100);
		buffer.fixSamples();
		Assert.assertEquals(1, countValids(buffer));
		Assert.assertEquals(100.0, buffer.getSamples()[(int) (20 * .5)].y, 0);
	}

	// Two values in the same bucket
	@Test
	public void testTwoValues() {
		AveragingSampleBuffer buffer = new AveragingSampleBuffer(10);
		buffer.addDataPoint(0.5, 50);
		buffer.addDataPoint(0.55, 100);
		buffer.fixSamples();
		Assert.assertEquals(1, countValids(buffer));
		Assert.assertEquals(2, buffer.getSamples()[(int) (10 * .5)].count);
		Assert.assertEquals(88, Math.round(buffer.getSamples()[(int) (10 * .5)].y));
	}

	// Two values in different buckets next to each other
	@Test
	public void testOneBorderCross() {
		AveragingSampleBuffer buffer = new AveragingSampleBuffer(2);
		buffer.addDataPoint(0.25, 40);
		buffer.addDataPoint(0.75, 80);
		buffer.fixSamples();
		Assert.assertEquals(2, countValids(buffer));
		Assert.assertEquals(45, Math.round(buffer.getSamples()[0].y));
		Assert.assertEquals(75, Math.round(buffer.getSamples()[1].y));
	}

	/*
	 * Two values in different buckets not next to each other.
	 * 
	 * Previously this test assumed that intermediary points would be interpolated. That does not
	 * seem to be required anymore by the consumers of the API.
	 */
	@Test
	public void testTwoBorderCrossings() {
		AveragingSampleBuffer buffer = new AveragingSampleBuffer(3);
		buffer.addDataPoint(1.0 / 6.0, 40);
		buffer.addDataPoint(5.0 / 6.0, 80);
		buffer.fixSamples();
		Assert.assertEquals(2, countValids(buffer));
		Assert.assertEquals(42.5, buffer.getSamples()[0].y, 0);
		double dy = buffer.getSamples()[2].y - buffer.getSamples()[0].y;
		int dx =  buffer.getSamples()[2].x - buffer.getSamples()[0].x;
		double midY = ((dy / dx) * (dx / 2)) + buffer.getSamples()[0].y;
		Assert.assertEquals(60, Math.round(midY), 0);
		Assert.assertEquals(77.5, buffer.getSamples()[2].y, 0);
	}

	@Test
	public void testBorderCases1() {
		AveragingSampleBuffer buffer = new AveragingSampleBuffer(4);
		buffer.addDataPoint(0, 50);
		buffer.addDataPoint(1.5 / 4, 80);
		buffer.fixSamples();
		Assert.assertEquals(2, countValids(buffer));
		Assert.assertEquals(60.0, buffer.getSamples()[0].y, 0);
		Assert.assertEquals(77.5, buffer.getSamples()[1].y, 0);
	}

	@Test
	public void testBorderCases3() {
		AveragingSampleBuffer buffer = new AveragingSampleBuffer(4);
		buffer.addDataPoint(2.5 / 4, 80);
		buffer.addDataPoint(4.0 / 4, 50);
		buffer.fixSamples();
		Assert.assertEquals(2, countValids(buffer));
		Assert.assertEquals(1, buffer.getSamples()[3].count);
		Assert.assertEquals(77.5, buffer.getSamples()[2].y, 0);
		Assert.assertEquals(60.0, buffer.getSamples()[3].y, 0);
	}

	public static int countValids(AveragingSampleBuffer buffer) {
		int valids = 0;
		for (SamplePoint p : buffer.getSamples()) {
			if (p != null) {
				valids++;
			}
		}
		return valids;
	}

}
