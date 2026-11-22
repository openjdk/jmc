/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.test.internal.parser;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openjdk.jmc.flightrecorder.internal.parser.TicksConverter;

public class TicksConverterTest {

	@Test
	public void testWholeNanosecondTicksConvertExactly() {
		TicksConverter tenMegahertz = new TicksConverter(10_000_000L);
		assertEquals(7_197_400L, tenMegahertz.toNanos(71_974L));
		assertEquals(-300L, tenMegahertz.toNanos(-3L));
		// Epoch scale values, beyond what a double holds exactly
		assertEquals(1_795_335_833_208_422_300L, tenMegahertz.toNanos(17_953_358_332_084_223L));

		TicksConverter oneGigahertz = new TicksConverter(1_000_000_000L);
		assertEquals(Long.MAX_VALUE, oneGigahertz.toNanos(Long.MAX_VALUE));
		assertEquals(1_795_335_833_208_422_301L, oneGigahertz.toNanos(1_795_335_833_208_422_301L));
	}

	@Test
	public void testSubNanosecondTicksTruncate() {
		TicksConverter threeGigahertz = new TicksConverter(3_000_000_000L);
		assertEquals(0L, threeGigahertz.toNanos(2L));
		assertEquals(1L, threeGigahertz.toNanos(3L));
		assertEquals(1_000_000_000L, threeGigahertz.toNanos(3_000_000_000L));

		long ticksPerSecond = 2_903_998_123L;
		TicksConverter tsc = new TicksConverter(ticksPerSecond);
		long ticks = 123_456_789_012L;
		assertEquals((long) (ticks / (ticksPerSecond / 1_000_000_000.0)), tsc.toNanos(ticks));
	}

	@Test
	public void testFractionalPeriodAboveOneNanosecondTruncates() {
		// 3 MHz gives 333.33... ns per tick, which is not a whole number of nanoseconds
		TicksConverter threeMegahertz = new TicksConverter(3_000_000L);
		assertEquals(333L, threeMegahertz.toNanos(1L));
		assertEquals(1_000L, threeMegahertz.toNanos(3L));
	}

	@Test
	public void testOverflowFallsBackToFloatingPoint() {
		// One tick per second: 10^9 ns per tick overflows a long beyond about 9.2 * 10^9 ticks
		TicksConverter oneHertz = new TicksConverter(1L);
		long ticks = Long.MAX_VALUE / 1_000_000_000L + 1;
		assertEquals((long) (ticks / (1 / 1_000_000_000.0)), oneHertz.toNanos(ticks));
		assertEquals(5_000_000_000L, oneHertz.toNanos(5L));
	}

	@Test
	public void testFractionalTicksScaleBeforeTruncation() {
		TicksConverter tenMegahertz = new TicksConverter(10_000_000L);
		assertEquals(50L, tenMegahertz.toNanos(0.5));
		assertEquals(-50L, tenMegahertz.toNanos(-0.5));
		assertEquals(4_290L, tenMegahertz.toNanos(42.9));
		assertEquals(Long.MAX_VALUE, tenMegahertz.toNanos(Double.POSITIVE_INFINITY));
		assertEquals(Long.MIN_VALUE, tenMegahertz.toNanos(Double.NEGATIVE_INFINITY));

		TicksConverter threeMegahertz = new TicksConverter(3_000_000L);
		assertEquals(500L, threeMegahertz.toNanos(1.5));

		TicksConverter tsc = new TicksConverter(2_903_998_123L);
		// 2.95 ticks is > 1 ns, whereas truncating to 2 ticks first would yield 0 ns
		assertEquals(1L, tsc.toNanos(2.95));
	}
}
