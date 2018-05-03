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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openjdk.jmc.common.test.MCTestCase;
import org.openjdk.jmc.common.unit.BinaryPrefix;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.KindOfQuantity;
import org.openjdk.jmc.common.unit.LinearKindOfQuantity;
import org.openjdk.jmc.common.unit.LinearUnit;
import org.openjdk.jmc.common.unit.UnitLookup;

/**
 *
 */
@SuppressWarnings("nls")
public class DeriveUnitTest extends MCTestCase {
	private LinearUnit one;
	private LinearUnit B, kiB, MiB;
	private LinearUnit ms, s, min, h, d, wk, y;

	@Before
	public void setUp() throws Exception {
		one = UnitLookup.NUMBER_UNITY;
		B = UnitLookup.BYTE;
		kiB = UnitLookup.MEMORY.getUnit(BinaryPrefix.KIBI);
		MiB = UnitLookup.MEMORY.getUnit(BinaryPrefix.MEBI);
		LinearKindOfQuantity span = UnitLookup.TIMESPAN;
		ms = UnitLookup.MILLISECOND;
		s = UnitLookup.SECOND;
		min = UnitLookup.MINUTE;
		h = span.getUnit("h");
		d = span.getUnit("d");
		wk = span.getUnit("wk");
		y = span.getUnit("a");
	}

	private void assertDerivedUnit(IUnit expectedUnit, IQuantity quantity) throws Exception {
		KindOfQuantity<?> kind = quantity.getType();
		Assert.assertEquals(expectedUnit, kind.getLargestExactUnit(quantity));
	}

	@Test
	public void testMemoryUnits() throws Exception {
		assertDerivedUnit(kiB, B.quantity(2048));
		assertDerivedUnit(kiB, B.quantity(1024));
		assertDerivedUnit(B, B.quantity(512));
		assertDerivedUnit(B, B.quantity(2050));
		assertDerivedUnit(B, B.quantity(1));
		assertDerivedUnit(B, kiB.quantity(0.5));

		assertDerivedUnit(MiB, kiB.quantity(2048));
		assertDerivedUnit(MiB, kiB.quantity(1024));
		assertDerivedUnit(kiB, kiB.quantity(512));
		assertDerivedUnit(kiB, kiB.quantity(2050));
		assertDerivedUnit(kiB, kiB.quantity(1));
		assertDerivedUnit(kiB, MiB.quantity(0.5));

		assertDerivedUnit(MiB, MiB.quantity(1));

		// FIXME: One could argue that the expected unit should be B here.
		assertDerivedUnit(null, B.quantity(0.5));

		assertDerivedUnit(null, B.quantity(0.1));
	}

	@Test
	public void testNumbers() throws Exception {
		assertDerivedUnit(one, one.quantity(2001));
		assertDerivedUnit(one, one.quantity(1000));
		assertDerivedUnit(one, one.quantity(2000));
		assertDerivedUnit(one, one.quantity(1000000));
		assertDerivedUnit(one, one.quantity(2000000));
		assertDerivedUnit(one, one.quantity(1));

		assertDerivedUnit(null, one.quantity(0.5));

		assertDerivedUnit(null, one.quantity(0.1));
	}

	@Test
	public void testSeconds() throws Exception {
		assertDerivedUnit(min, s.quantity(120));
		assertDerivedUnit(min, s.quantity(60));
		assertDerivedUnit(s, s.quantity(125));
		assertDerivedUnit(s, s.quantity(1));
		assertDerivedUnit(s, s.quantity(1.0));
		assertDerivedUnit(ms, s.quantity(0.5));
	}

	@Test
	public void testMinutes() throws Exception {
		assertDerivedUnit(h, min.quantity(120));
		assertDerivedUnit(h, min.quantity(60));
		assertDerivedUnit(min, min.quantity(125));
		assertDerivedUnit(min, min.quantity(1));
		assertDerivedUnit(min, min.quantity(1.0));
		assertDerivedUnit(s, min.quantity(0.5));
	}

	@Test
	public void testHours() throws Exception {
		assertDerivedUnit(d, h.quantity(48));
		assertDerivedUnit(d, h.quantity(24));
		assertDerivedUnit(h, h.quantity(50));
		assertDerivedUnit(h, h.quantity(1));
		assertDerivedUnit(h, h.quantity(1.0));
		assertDerivedUnit(min, h.quantity(0.5));
	}

	@Test
	public void testDays() throws Exception {
		assertDerivedUnit(wk, d.quantity(49));
		assertDerivedUnit(d, d.quantity(24));
		assertDerivedUnit(d, d.quantity(50));
		assertDerivedUnit(d, d.quantity(1));
		assertDerivedUnit(d, d.quantity(1.0));
		assertDerivedUnit(h, d.quantity(0.5));
	}

	@Test
	public void testWeeks() throws Exception {
		assertDerivedUnit(wk, wk.quantity(52));
		assertDerivedUnit(wk, wk.quantity(24));
		assertDerivedUnit(wk, wk.quantity(50));
		assertDerivedUnit(wk, wk.quantity(1));
		assertDerivedUnit(wk, wk.quantity(1.0));
		assertDerivedUnit(h, wk.quantity(0.5));
	}

	@Test
	public void testYears() throws Exception {
		assertDerivedUnit(y, y.quantity(52));
		assertDerivedUnit(y, y.quantity(24));
		assertDerivedUnit(y, y.quantity(50));
		assertDerivedUnit(y, y.quantity(1));
		assertDerivedUnit(y, y.quantity(1.0));

		assertDerivedUnit(h, y.quantity(0.5));
		assertDerivedUnit(min, y.quantity(0.25));
		assertDerivedUnit(min, y.quantity(0.1));
	}
}
