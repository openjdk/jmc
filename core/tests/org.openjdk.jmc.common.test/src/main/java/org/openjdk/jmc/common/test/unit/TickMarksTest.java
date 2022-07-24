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

import org.junit.Before;
import org.junit.Test;
import org.openjdk.jmc.common.test.MCTestCase;
import org.openjdk.jmc.common.unit.BinaryPrefix;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.ITypedQuantity;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.KindOfQuantity;
import org.openjdk.jmc.common.unit.LinearKindOfQuantity;
import org.openjdk.jmc.common.unit.LinearUnit;
import org.openjdk.jmc.common.unit.UnitLookup;

@SuppressWarnings("nls")
public class TickMarksTest extends MCTestCase {
	private LinearUnit one;
	private LinearUnit B, kiB;
	private LinearUnit s, min, h, d, wk, y;

	@Before
	public void setUp() throws Exception {
		one = UnitLookup.NUMBER_UNITY;
		B = UnitLookup.BYTE;
		kiB = UnitLookup.MEMORY.getUnit(BinaryPrefix.KIBI);
		LinearKindOfQuantity span = UnitLookup.TIMESPAN;
		s = UnitLookup.SECOND;
		min = UnitLookup.MINUTE;
		h = span.getUnit("h");
		d = span.getUnit("d");
		wk = span.getUnit("wk");
		y = span.getUnit("a");
	}

	private void assertTicks(
		ITypedQuantity<LinearUnit> expectedSpacing, IQuantity expectedAlignedStart, IQuantity start, IQuantity end,
		int maxTicks) throws Exception {
		KindOfQuantity<?> kind = start.getType();
		IRange<IQuantity> firstBucket = kind.getFirstBucket(start, end, maxTicks);
		IQuantity first = firstBucket.getStart();
		IQuantity diff = firstBucket.getExtent();
		AdHocQuantityTest.assertNearlySame("Spacing", expectedSpacing, diff);
		AdHocQuantityTest.assertNearlySame("Start", expectedAlignedStart, first);
// System.out.println("Expected start: " + expectedAlignedStart + ", Actual: " + first);
// System.out.println("Expected space: " + expectedSpacing + ", Actual: " + diff);
	}

	private void assertTicks(
		ITypedQuantity<LinearUnit> expectedSpacing, Number expectedAlignedStart, Number start, Number end, IUnit unit,
		int maxTicks) throws Exception {
		assertTicks(expectedSpacing, unit.quantity(expectedAlignedStart), unit.quantity(start), unit.quantity(end),
				maxTicks);
	}

	private void assertTicks(
		Number expectedSpacing, Number expectedAlignedStart, LinearUnit expectedUnit, Number start, Number end,
		LinearUnit unit, int maxTicks) throws Exception {
		assertTicks(expectedUnit.quantity(expectedSpacing), expectedUnit.quantity(expectedAlignedStart),
				unit.quantity(start), unit.quantity(end), maxTicks);
	}

	private void assertTicks(
		Number expectedSpacing, Number expectedAlignedStart, Number start, Number end, LinearUnit unit, int maxTicks)
			throws Exception {
		assertTicks(expectedSpacing, expectedAlignedStart, unit, start, end, unit, maxTicks);
	}

	@Test
	public void testMemoryTicks() throws Exception {
		assertTicks(2, 4 - 2, kiB, 2675, 15000, B, 10);
		assertTicks(2, 4 - 2, 2.67, 15, kiB, 10);
	}

	@Test
	public void testNumberTicks() throws Exception {
		assertTicks(0.5, 1, 1, 5, one, 10);

		assertTicks(2, 4 - 2, 2.67, 15, one, 10);
		assertTicks(2, 4 - 2, 2.67, 22, one, 10);
		assertTicks(2, 4 - 2, 2.67, 22.67, one, 10);
		assertTicks(2.5, 5 - 2.5, 2.67, 22.68, one, 10);
		assertTicks(2.5, 5 - 2.5, 2.67, 23, one, 10);
	}

	@Test
	public void testSecondsTicks() throws Exception {
		assertTicks(0.5, 1, 1, 5, s, 10);

		assertTicks(2, 4 - 2, 2.67, 15, s, 10);
		assertTicks(2, 4 - 2, 2.67, 22, s, 10);
		assertTicks(2, 4 - 2, 2.67, 22.67, s, 10);
		assertTicks(4, 4 - 4, 2.67, 22.68, s, 10);
		assertTicks(4, 4 - 4, 2.67, 23, s, 10);
	}

	@Test
	public void testMinutesTicks() throws Exception {
		assertTicks(s.quantity(30), 1, 1, 5, min, 10);

		assertTicks(2, 4 - 2, 2.67, 15, min, 10);
		assertTicks(2, 4 - 2, 2.67, 22.67, min, 10);
		assertTicks(4, 4 - 4, 2.67, 22.68, min, 10);
		assertTicks(4, 4 - 4, 2.67, 23, min, 10);
	}

	@Test
	public void testHoursTicks() throws Exception {
		assertTicks(min.quantity(30), 1, 1, 5, h, 10);

		assertTicks(2, 4 - 2, 2.67, 15, h, 10);
		assertTicks(2, 4 - 2, 2.67, 22.67, h, 10);
		assertTicks(4, 4 - 4, 2.67, 22.68, h, 10);
		assertTicks(4, 4 - 4, 2.67, 23, h, 10);
	}

	@Test
	public void testDaysTicks() throws Exception {
		assertTicks(h.quantity(12), 1, 1, 5, d, 10);

		assertTicks(2, 4 - 2, 2.67, 15, d, 10);
		assertTicks(2, 4 - 2, 2.67, 22.67, d, 10);
		assertTicks(4, 4 - 4, 2.67, 22.68, d, 10);
		assertTicks(4, 4 - 4, 2.67, 23, d, 10);
	}

	@Test
	public void testWeeksTicks() throws Exception {
		// Is this what a user would want?
		assertTicks(4, 8 - 4, d, 1, 5, wk, 10);

		assertTicks(2, 4 - 2, 2.67, 15, wk, 10);
		assertTicks(2, 4 - 2, 2.67, 22.67, wk, 10);
		assertTicks(4, 4 - 4, 2.67, 22.68, wk, 10);
		assertTicks(4, 4 - 4, 2.67, 23, wk, 10);
	}

	@Test
	public void testYearsTicks() throws Exception {
		// Are these what a user would want?
		assertTicks(0.25, 1, 1, 2.5, y, 10);
		assertTicks(0.5, 1, 1, 5, y, 10);

		assertTicks(2, 4 - 2, 2.67, 15, y, 10);
		assertTicks(2, 4 - 2, 2.67, 22.67, y, 10);
		assertTicks(2.5, 5 - 2.5, 2.67, 22.68, y, 10);
		assertTicks(2.5, 5 - 2.5, 2.67, 23, y, 10);
	}
}
