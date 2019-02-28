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
package org.openjdk.jmc.common.test;

import static org.openjdk.jmc.common.unit.BinaryPrefix.NOBI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.junit.Assert;
import org.junit.Test;

import org.openjdk.jmc.common.unit.BinaryPrefix;

public class BinaryPrefixTest extends MCTestCase {

	private void assertAlignmentLog2(int expectedLog2, long value) throws Exception {
		Assert.assertEquals(expectedLog2, BinaryPrefix.getAlignmentLog2(value));
	}

	private void assertAlignmentLog2(int expectedLog2, double value) throws Exception {
		Assert.assertEquals(expectedLog2, BinaryPrefix.getAlignmentLog2(value));
	}

	@Test
	public void testZero() {
		assertEquals(0, BinaryPrefix.getFloorLog1024(0));
		assertEquals(NOBI, BinaryPrefix.getFloorPrefix(0));
	}

	@Test
	public void testZeroDouble() {
		assertEquals(0, BinaryPrefix.getFloorLog1024(0.0));
		assertEquals(NOBI, BinaryPrefix.getFloorPrefix(0.0));
	}

	@Test
	public void testPositive() {
		int oldLog1024 = -1;
		BinaryPrefix oldPrefix = null;
		int i = 0;
		for (long val = 1; val != 0; val <<= 1) {
			int log1024 = BinaryPrefix.getFloorLog1024(val);
			BinaryPrefix prefix = BinaryPrefix.getFloorPrefix(val);

			if ((i++ % 10) == 0) {
				assertGreaterThan("For value " + val, oldLog1024, log1024);
				if (oldPrefix == null) {
					assertNotNull(prefix);
				} else {
					assertGreaterThan(oldPrefix, prefix);
				}
			} else {
				assertEquals("For value " + val, oldLog1024, log1024);
				assertSame(oldPrefix, prefix);
			}

			oldLog1024 = log1024;
			oldPrefix = prefix;
		}
	}

	@Test
	public void testPositiveDouble() {
		int oldLog1024 = -1;
		BinaryPrefix oldPrefix = null;
		int i = 0;
		for (int powOf2 = 0; powOf2 < 90; powOf2++) {
			double val = Math.scalb(1.0, powOf2);
			int log1024 = BinaryPrefix.getFloorLog1024(val);
			BinaryPrefix prefix = BinaryPrefix.getFloorPrefix(val);

			if ((i++ % 10) == 0) {
				assertGreaterThan("For value " + val, oldLog1024, log1024);
				if (oldPrefix == null) {
					assertNotNull(prefix);
				} else {
					assertGreaterThan(oldPrefix, prefix);
				}
			} else {
				assertEquals("For value " + val, oldLog1024, log1024);
				assertSame(oldPrefix, prefix);
			}

			oldLog1024 = log1024;
			oldPrefix = prefix;
		}
	}

	@Test
	public void testNegative() {
		int oldLog1024 = -1;
		BinaryPrefix oldPrefix = null;
		int i = 0;
		for (long val = -1; val != 0; val <<= 1) {
			int log1024 = BinaryPrefix.getFloorLog1024(val);
			BinaryPrefix prefix = BinaryPrefix.getFloorPrefix(val);

			if ((i++ % 10) == 0) {
				assertGreaterThan("For value " + val, oldLog1024, log1024);
				if (oldPrefix == null) {
					assertNotNull(prefix);
				} else {
					assertGreaterThan(oldPrefix, prefix);
				}
			} else {
				assertEquals("For value " + val, oldLog1024, log1024);
				assertSame(oldPrefix, prefix);
			}

			oldLog1024 = log1024;
			oldPrefix = prefix;
		}
	}

	@Test
	public void testNegativeDouble() {
		int oldLog1024 = -1;
		BinaryPrefix oldPrefix = null;
		int i = 0;
		for (int powOf2 = 0; powOf2 < 90; powOf2++) {
			double val = -Math.scalb(1.0, powOf2);
			int log1024 = BinaryPrefix.getFloorLog1024(val);
			BinaryPrefix prefix = BinaryPrefix.getFloorPrefix(val);

			if ((i++ % 10) == 0) {
				assertGreaterThan("For value " + val, oldLog1024, log1024);
				if (oldPrefix == null) {
					assertNotNull(prefix);
				} else {
					assertGreaterThan(oldPrefix, prefix);
				}
			} else {
				assertEquals("For value " + val, oldLog1024, log1024);
				assertSame(oldPrefix, prefix);
			}

			oldLog1024 = log1024;
			oldPrefix = prefix;
		}
	}

	@Test
	public void testBinaryLongAlignment() throws Exception {
		assertAlignmentLog2(0, 1);
		assertAlignmentLog2(0, 17);
		assertAlignmentLog2(0, 1027);
		assertAlignmentLog2(1, 2);
		assertAlignmentLog2(1, 42);
		assertAlignmentLog2(1, 1030);
		assertAlignmentLog2(2, 4);
		assertAlignmentLog2(3, 8);
		assertAlignmentLog2(4, 16);
		assertAlignmentLog2(10, 1024);
	}

	@Test
	public void testBinaryDoubleAlignment() throws Exception {
		assertAlignmentLog2(0, 1.0);
		assertAlignmentLog2(0, 17.0);
		assertAlignmentLog2(0, 1027.0);
		assertAlignmentLog2(1, 2.0);
		assertAlignmentLog2(1, 42.0);
		assertAlignmentLog2(1, 1030.0);
		assertAlignmentLog2(2, 4.0);
		assertAlignmentLog2(3, 8.0);
		assertAlignmentLog2(4, 16.0);
		assertAlignmentLog2(10, 1024.0);

		assertAlignmentLog2(-1, 0.5);
		assertAlignmentLog2(-1, 1.5);
		assertAlignmentLog2(-1, 17.5);
		assertAlignmentLog2(-1, 16777216.5);

		assertAlignmentLog2(-2, 0.25);
		assertAlignmentLog2(-2, 1.25);
		assertAlignmentLog2(-2, 17.25);
		assertAlignmentLog2(-2, 16777216.25);
	}
}
