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
import org.openjdk.jmc.greychart.providers.SampleCountingBuffer;

public class SampleCountingBufferTest {
	public void testSingleValue() {
		SampleCountingBuffer buffer = new SampleCountingBuffer(20);
		buffer.addDataPoint(0.5, 100);
		Assert.assertEquals(1, countValids(buffer));
		Assert.assertEquals(1.0, buffer.getSamples()[(int) (20 * .5)].y, 0);
	}

	// Two values in the same bucket
	@Test
	public void testTwoValues() {
		SampleCountingBuffer buffer = new SampleCountingBuffer(10);
		buffer.addDataPoint(0.5, 50);
		buffer.addDataPoint(0.55, 100);
		System.out.println(buffer.toString());
		Assert.assertEquals(1, countValids(buffer));
		Assert.assertEquals(2, buffer.getSamples()[(int) (10 * .5)].count);
		Assert.assertEquals(2.0, buffer.getSamples()[(int) (10 * .5)].y, 0);
	}

	// Two values in different buckets next to each other
	@Test
	public void testOneBorderCross() {
		SampleCountingBuffer buffer = new SampleCountingBuffer(2);
		buffer.addDataPoint(0.25, 40);
		buffer.addDataPoint(0.75, 80);
		System.out.println(buffer.toString());
		Assert.assertEquals(2, countValids(buffer));
		Assert.assertEquals(1.0, buffer.getSamples()[0].y, 0);
		Assert.assertEquals(1.0, buffer.getSamples()[1].y, 0);
	}

	// Two values in different buckets not next to each other
	@Test
	public void testTwoBorderCrossings() {
		SampleCountingBuffer buffer = new SampleCountingBuffer(3);
		buffer.addDataPoint(1.0 / 6.0, 40);
		buffer.addDataPoint(5.0 / 6.0, 80);
		System.out.println(buffer.toString());
		Assert.assertEquals(2, countValids(buffer));
		Assert.assertEquals(1.0, buffer.getSamples()[0].y, 0);
		Assert.assertEquals(0.0, buffer.getSamples()[1].y, 0);
		Assert.assertEquals(1.0, buffer.getSamples()[2].y, 0);
	}

	// One value in the first bucket, one value in the second bucket
	@Test
	public void testBorderCases1() {
		SampleCountingBuffer buffer = new SampleCountingBuffer(4);
		buffer.addDataPoint(0, 50);
		buffer.addDataPoint(1.5 / 4, 80);
		System.out.println(buffer.toString());
		Assert.assertEquals(2, countValids(buffer));
		Assert.assertEquals(1.0, buffer.getSamples()[0].y, 0);
		Assert.assertEquals(1.0, buffer.getSamples()[1].y, 0);
	}

	// One value in the last bucket and one value just outside the last bucket
	@Test
	public void testBorderCases3() {
		SampleCountingBuffer buffer = new SampleCountingBuffer(4);
		buffer.addDataPoint(2.5 / 4, 80);
		buffer.addDataPoint(4.0 / 4, 50);
		System.out.println(buffer.toString());
		// NOTE: The sample counting buffer ignores all samples outside the buffer
		Assert.assertEquals(1, countValids(buffer));
		Assert.assertEquals(0, buffer.getSamples()[3].count);
		Assert.assertEquals(1.0, buffer.getSamples()[2].y, 0);
		Assert.assertEquals(0.0, buffer.getSamples()[3].y, 0);
	}

	public static int countValids(SampleCountingBuffer buffer) {
		int valids = 0;
		for (SamplePoint p : buffer.getSamples()) {
			if (p != null && p.y > 0) {
				valids++;
			}
		}
		return valids;
	}

}
