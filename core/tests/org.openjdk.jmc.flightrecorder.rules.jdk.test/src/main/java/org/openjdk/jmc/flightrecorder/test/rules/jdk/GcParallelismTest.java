/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2023, 2025, Red Hat Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.test.rules.jdk;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmc.flightrecorder.rules.jdk.memory.GcInvertedParallelismRule;

public class GcParallelismTest {

	@Test
	public void testCalcParallelism() {
		int timeUser = 90;
		int timeSys = 10;
		int timeReal = 10;
		Assert.assertEquals(1000, GcInvertedParallelismRule.calcParallelism(timeUser, timeSys, timeReal));
	}

	@Test
	public void testCalcParallelismRounded() {
		int timeUser = 90;
		int timeSys = 10;
		int timeReal = 1000;
		Assert.assertEquals(10, GcInvertedParallelismRule.calcParallelism(timeUser, timeSys, timeReal));
	}

	@Test
	public void testCalcParallelismRoundedUp() {
		int timeUser = 90;
		int timeSys = 10;
		int timeReal = 199;
		Assert.assertEquals(51, GcInvertedParallelismRule.calcParallelism(timeUser, timeSys, timeReal));
	}

	@Test
	public void testCalcParallelismUserZero() {
		int timeUser = 0;
		int timeSys = 0;
		int timeReal = 100;
		Assert.assertEquals(0, GcInvertedParallelismRule.calcParallelism(timeUser, timeSys, timeReal));
	}

	@Test
	public void testCalcParallelismRealZero() {
		int timeUser = 100;
		int timeSys = 0;
		int timeReal = 0;
		Assert.assertEquals(Integer.MAX_VALUE, GcInvertedParallelismRule.calcParallelism(timeUser, timeSys, timeReal));
	}

	@Test
	public void testCalcParallelismUserZeroRealZero() {
		int timeUser = 0;
		int timeSys = 0;
		int timeReal = 0;
		Assert.assertEquals(100, GcInvertedParallelismRule.calcParallelism(timeUser, timeSys, timeReal));
	}

	@Test
	public void testParallelism() {
		Assert.assertTrue(GcInvertedParallelismRule.isInvertedParallelism(0));
		Assert.assertTrue(GcInvertedParallelismRule.isInvertedParallelism(99));
		Assert.assertFalse(GcInvertedParallelismRule.isInvertedParallelism(100));
	}
}
