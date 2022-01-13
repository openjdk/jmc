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
package org.openjdk.jmc.common.test.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.openjdk.jmc.common.test.MCTestCase;
import org.openjdk.jmc.common.unit.DecimalPrefix;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.UnitLookup;

@SuppressWarnings("nls")
public class QuantityArithmeticsTest extends MCTestCase {
	/**
	 * A long value so big that it cannot be exactly represented by a double. Twice the value should
	 * also fit in a (signed) long, but not four times the value.
	 */
	private static long BIG_LONG = (1L << 61) + 1;
	private IUnit y;
	private IUnit d;
	private IUnit h;
	private IUnit min;
	private IUnit s;
	private IUnit ms;
	private IUnit ns;
	private IUnit ps;
	private IUnit msBiased;

	@Before
	public void setUp() throws Exception {

		y = UnitLookup.TIMESPAN.getUnit("a");
		h = UnitLookup.TIMESPAN.getUnit("h");
		d = UnitLookup.TIMESPAN.getUnit("d");
		min = UnitLookup.MINUTE;
		s = UnitLookup.SECOND;
		ms = UnitLookup.MILLISECOND;
		ns = UnitLookup.NANOSECOND;
		ps = UnitLookup.TIMESPAN.getUnit(DecimalPrefix.PICO);
		msBiased = UnitLookup.EPOCH_MS;
	}

	protected void assertSum(IQuantity expectedSum, IQuantity a, IQuantity b) {
		IQuantity sum = a.add(b);
		String msg = null;
		if (!expectedSum.equals(sum)) {
			msg = a + " + " + b + " =";
		}
		AdHocQuantityTest.assertNearlySame(msg, expectedSum, sum);
		// Try to make this hold, always.
		assertEquals(msg, expectedSum, sum);
	}

	protected void assertDiff(IQuantity expectedDiff, IQuantity a, IQuantity b) {
		IQuantity diff = a.subtract(b);
		String msg = null;
		if (!expectedDiff.equals(diff)) {
			msg = a + " - " + b + " =";
		}
		AdHocQuantityTest.assertNearlySame(msg, expectedDiff, diff);
		// Try to make this hold, always.
		assertEquals(msg, expectedDiff, diff);
	}

	/**
	 * Verify the desired properties of BIG_LONG, used in other tests.
	 */
	@Test
	public void testBigLong() {
		// Make sure that BIG_LONG and BIG_LONG + 1 can be distinguished when represented in longs ...
		assertFalse((BIG_LONG + 1) == BIG_LONG);
		// ... but not when attempting to represent them with doubles.
		assertEquals(BIG_LONG, ((double) BIG_LONG) + 1, 0);
		// Make sure this BIG_LONG * 2 fits ...
		assertTrue((BIG_LONG * 2) > BIG_LONG);
		// ... but not BIG_LONG * 4.
		assertFalse((BIG_LONG * 4) > BIG_LONG * 2);
	}

	@Test
	public void testDurationLongSum() {
		assertSum(s.quantity(100), s.quantity(17), s.quantity(83));
		assertSum(s.quantity(BIG_LONG + 1), s.quantity(BIG_LONG), s.quantity(1));
		assertSum(s.quantity(BIG_LONG + 1), s.quantity(1), s.quantity(BIG_LONG));
	}

	@Test
	public void testDurationDoubleSum() {
		assertSum(s.quantity(100.0), s.quantity(17.0), s.quantity(83.0));
	}

	@Test
	public void testDurationMixSum() {
		assertSum(s.quantity(100.0), s.quantity(17.0), s.quantity(83));
		assertSum(s.quantity(100.0), s.quantity(17), s.quantity(83.0));
	}

	@Test
	public void testDurationLongDiff() {
		assertDiff(s.quantity(42), s.quantity(59), s.quantity(17));
		assertDiff(s.quantity(1), s.quantity(BIG_LONG + 1), s.quantity(BIG_LONG));
	}

	@Test
	public void testDurationDoubleDiff() {
		assertDiff(s.quantity(42.0), s.quantity(59.0), s.quantity(17.0));
	}

	@Test
	public void testDurationMixDiff() {
		assertDiff(s.quantity(42.0), s.quantity(59.0), s.quantity(17));
		assertDiff(s.quantity(42.0), s.quantity(59), s.quantity(17.0));
	}

	@Test
	public void testTimestampSum() {
		try {
			assertSum(msBiased.quantity(100), msBiased.quantity(17), msBiased.quantity(83));
			fail("Shouldn't be possible to add two timestamps!");
		} catch (IllegalArgumentException expected) {
		}
	}

	@Test
	public void testTimestampPlusDuration() {
		assertSum(msBiased.quantity(100), msBiased.quantity(17), ms.quantity(83));
		assertSum(msBiased.quantity(BIG_LONG + 1), msBiased.quantity(BIG_LONG), ms.quantity(1));
		assertSum(msBiased.quantity(BIG_LONG + 1), msBiased.quantity(1), ms.quantity(BIG_LONG));
	}

	@Test
	public void testDurationPlusTimestamp() {
		assertSum(msBiased.quantity(100), ms.quantity(17), msBiased.quantity(83));
		assertSum(msBiased.quantity(BIG_LONG + 1), ms.quantity(1), msBiased.quantity(BIG_LONG));
		assertSum(msBiased.quantity(BIG_LONG + 1), ms.quantity(BIG_LONG), msBiased.quantity(1));
	}

	@Test
	public void testTimestampDiff() {
		assertDiff(ms.quantity(42), msBiased.quantity(59), msBiased.quantity(17));
		assertDiff(ms.quantity(1), msBiased.quantity(BIG_LONG + 1), msBiased.quantity(BIG_LONG));
	}

	@Test
	public void testMillisPlusSeconds() {
		assertSum(ms.quantity(1001), ms.quantity(1), s.quantity(1));
		assertSum(ms.quantity(2003), ms.quantity(3), s.quantity(2));
		assertSum(ms.quantity((BIG_LONG / 1000) * 1000 + 3), ms.quantity(3), s.quantity(BIG_LONG / 1000));
		assertSum(ms.quantity(BIG_LONG * 1000.0), ms.quantity(3), s.quantity(BIG_LONG));
	}

	@Test
	public void testSecondsPlusMillis() {
		assertSum(ms.quantity(1001), s.quantity(1), ms.quantity(1));
		assertSum(ms.quantity(2003), s.quantity(2), ms.quantity(3));
		assertSum(ms.quantity((BIG_LONG / 1000) * 1000 + 3), s.quantity(BIG_LONG / 1000), ms.quantity(3));
		assertSum(ms.quantity(BIG_LONG * 1000.0), s.quantity(BIG_LONG), ms.quantity(3));
	}

	@Test
	public void testMinutesPlusSeconds() {
		assertSum(s.quantity(61), min.quantity(1), s.quantity(1));
		assertSum(s.quantity(123), min.quantity(2), s.quantity(3));
	}

	@Test
	public void testSecondsPlusMinutes() {
		assertSum(s.quantity(61), s.quantity(1), min.quantity(1));
		assertSum(s.quantity(123), s.quantity(3), min.quantity(2));
	}

	@Test
	public void testHoursPlusMinutes() {
		assertSum(min.quantity(61), h.quantity(1), min.quantity(1));
		assertSum(min.quantity(123), h.quantity(2), min.quantity(3));
		assertSum(min.quantity((BIG_LONG / 60) * 60 + 3), min.quantity(3), h.quantity(BIG_LONG / 60));
		assertSum(min.quantity(BIG_LONG * 60.0), min.quantity(3), h.quantity(BIG_LONG));
	}

	@Test
	public void testMinutesPlusHours() {
		assertSum(min.quantity(61), min.quantity(1), h.quantity(1));
		assertSum(min.quantity(123), min.quantity(3), h.quantity(2));
		assertSum(min.quantity((BIG_LONG / 60) * 60 + 3), h.quantity(BIG_LONG / 60), min.quantity(3));
		assertSum(min.quantity(BIG_LONG * 60.0), h.quantity(BIG_LONG), min.quantity(3));
	}

	@Test
	public void testHourAddition() {
		assertSum(h.quantity(BIG_LONG * 2), h.quantity(BIG_LONG), h.quantity(BIG_LONG));
		// Overflowing exact (signed) long representation. Expect double.
		assertSum(h.quantity(BIG_LONG * 4.0), h.quantity(BIG_LONG * 2), h.quantity(BIG_LONG * 2));
	}

	@Test
	public void testMillisPlusMinutes() {
		assertSum(ms.quantity(60001), ms.quantity(1), min.quantity(1));
		assertSum(ms.quantity(120003), ms.quantity(3), min.quantity(2));
	}

	@Test
	public void testMinutesPlusMillis() {
		assertSum(ms.quantity(60001), min.quantity(1), ms.quantity(1));
		assertSum(ms.quantity(120003), min.quantity(2), ms.quantity(3));
	}

	@Test
	public void testDaysPlusNanos() {
		assertSum(ns.quantity(86400000000003L), d.quantity(1), ns.quantity(3));
	}

	@Test
	public void testYearsPlusHours() {
		assertSum(h.quantity(8790), y.quantity(1), h.quantity(24));
	}

	@Test
	public void testYearsPlusDays() {
		// This isn't implemented to be exact yet, although it could be.
//		assertSum(h.quantity(8790), y.quantity(1), d.quantity(1));
		assertSum(d.quantity(366.25), y.quantity(1), d.quantity(1));
	}

	@Test
	public void testYearsPlusPicos() {
		// Cannot fit exactly in a long, but would be nicer if it was expressed in years, perhaps.
//		assertSum(y.quantity(1.0), y.quantity(1), ps.quantity(3));
		assertSum(ps.quantity(3.15576E19), y.quantity(1), ps.quantity(3));
	}
}
