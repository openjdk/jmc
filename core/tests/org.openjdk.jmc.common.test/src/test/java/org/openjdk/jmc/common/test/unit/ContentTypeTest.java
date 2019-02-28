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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.openjdk.jmc.common.IDisplayable.AUTO;
import static org.openjdk.jmc.common.IDisplayable.EXACT;
import static org.openjdk.jmc.common.IDisplayable.VERBOSE;
import static org.openjdk.jmc.common.unit.DecimalPrefix.MICRO;
import static org.openjdk.jmc.common.unit.DecimalPrefix.NONE;
import static org.openjdk.jmc.common.unit.UnitLookup.EPOCH_MS;
import static org.openjdk.jmc.common.unit.UnitLookup.EPOCH_NS;
import static org.openjdk.jmc.common.unit.UnitLookup.MEMORY;
import static org.openjdk.jmc.common.unit.UnitLookup.TIMESPAN;
import static org.openjdk.jmc.common.unit.UnitLookup.TIMESTAMP;

import org.junit.Test;
import org.openjdk.jmc.common.test.MCTestCase;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.ITypedQuantity;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.LinearKindOfQuantity;
import org.openjdk.jmc.common.unit.LinearUnit;
import org.openjdk.jmc.common.unit.QuantityRange;
import org.openjdk.jmc.common.unit.UnitLookup;

@SuppressWarnings("nls")
public class ContentTypeTest extends MCTestCase {

	static public void assertContains(String expectedSubStr, String actual) {
		if (!actual.contains(expectedSubStr)) {
			fail("expected to contain:<" + expectedSubStr + "> did not:<" + actual + ">");
		}
	}

	static public void assertNotContain(String unexpectedSubStr, String actual) {
		if (actual.contains(unexpectedSubStr)) {
			fail("didn't expect to contain:<" + unexpectedSubStr + "> did:<" + actual + ">");
		}
	}

	@Test
	public void testTimestampInteractive() throws Exception {
		for (IUnit unit : TIMESTAMP.getAllUnits()) {
			// Doesn't work with ms, as the era designator (B.C.E) typically isn't in the used pattern.
//			IQuantity quantity = unit.quantity(-4937981208836185383L);
			IQuantity quantity = unit.quantity(-49379812088L);
			AdHocQuantityTest.assertInteractiveRoundtrip(quantity);
			quantity = unit.quantity(4937981208836185L);
			AdHocQuantityTest.assertInteractiveRoundtrip(quantity);
		}
	}

	@Test
	public void testTimestampMilliRangeFormat() throws Exception {
		IRange<IQuantity> range = QuantityRange.createWithEnd(EPOCH_MS.quantity(4200000000123L),
				EPOCH_MS.quantity(4200000000999L));
		assertContains("999", range.displayUsing(AUTO));
		assertContains("999", range.displayUsing(EXACT));
		assertContains("999", range.displayUsing(VERBOSE));
	}

	@Test
	public void testTimestampNanoRangeFormat() throws Exception {
		IRange<IQuantity> range = QuantityRange.createWithEnd(EPOCH_NS.quantity(4200000000123000000L),
				EPOCH_NS.quantity(4200000000999999999L));
		assertContains("999", range.displayUsing(AUTO));
		assertContains("999", range.displayUsing(EXACT));
		assertContains("999", range.displayUsing(VERBOSE));
	}

	@Test
	public void testTimestampPersisted() throws Exception {
		for (IUnit unit : TIMESTAMP.getAllUnits()) {
			IQuantity quantity = unit.quantity(-4937981208836185383L);
			AdHocQuantityTest.assertPersistedRoundtrip(quantity);
			quantity = unit.quantity(4937981208836185383L);
			AdHocQuantityTest.assertPersistedRoundtrip(quantity);
		}
	}

	@Test
	public void testTimeSpanUnits() {
		for (IUnit unit : UnitLookup.TIMESPAN.getAllUnits()) {
			IQuantity quantity = unit.quantity(17);
			assertTrue(quantity.toString().contains("17"));
		}
	}

	@Test
	public void testTimeSpanFormatAlmostWeeks() {
		LinearUnit days = TIMESPAN.getUnit("d");
		IQuantity almost2wk = days.quantity(13.5);
		String dispStr = almost2wk.displayUsing(AUTO);
		assertContains("2", dispStr);
		assertNotContain("14", dispStr);
		assertNotContain("13", dispStr);
		assertNotContain("12", dispStr);
	}

	@Test
	public void testTimeSpanFormatAlmostWeeksNeg() {
		LinearUnit days = TIMESPAN.getUnit("d");
		IQuantity almost2wk = days.quantity(-13.5);
		String dispStr = almost2wk.displayUsing(AUTO);
		assertContains("2", dispStr);
		assertNotContain("14", dispStr);
		assertNotContain("13", dispStr);
		assertNotContain("12", dispStr);
	}

	@Test
	public void testTimeSpanFormatFewMinutes() {
		LinearUnit seconds = TIMESPAN.getUnit(NONE);
		IQuantity fewMinutes = seconds.quantity(100);
		String dispStr = fewMinutes.displayUsing(AUTO);
		assertContains("1", dispStr);
		assertContains("40", dispStr);
		assertNotContain("20", dispStr);
	}

	@Test
	public void testTimeSpanFormatFewMinutesNeg() {
		LinearUnit seconds = TIMESPAN.getUnit(NONE);
		IQuantity fewMinutes = seconds.quantity(-100);
		String dispStr = fewMinutes.displayUsing(AUTO);
		assertContains("1", dispStr);
		assertContains("40", dispStr);
		assertNotContain("20", dispStr);
	}

	@Test
	public void testTimeSpanZeroDays() {
		LinearUnit days = TIMESPAN.getUnit("d");
		IQuantity noTime = days.quantity(0);
		String dispStr = noTime.displayUsing(AUTO);
		assertContains("0", dispStr);
	}

	@Test
	public void testTimeSpanZeroDaysNeg() {
		LinearUnit days = TIMESPAN.getUnit("d");
		IQuantity noTime = days.quantity(-0);
		String dispStr = noTime.displayUsing(AUTO);
		assertContains("0", dispStr);
	}

	@Test
	public void testMicrosecondMu() {
		// FIXME: This should probably return the real microsecond unit instead of null.
		assertNull(UnitLookup.getUnitOrNull("timespan:\u03bcs"));
	}

	@Test
	public void testMicrosecondU() {
		IUnit us = TIMESPAN.getUnit(MICRO);
		assertEquals(us, UnitLookup.getUnitOrNull("timespan:us"));
	}

	@Test
	public void testPercentagePercent() {
		IUnit percent = UnitLookup.getUnitOrNull("percentage:%");
		assertNotNull(percent);
	}

	@Test
	public void testPercentageUnity() {
		IUnit unity = UnitLookup.getUnitOrNull("percentage:");
		assertNotNull(unity);
	}

	@Test
	public void testMemoryUnits() throws Exception {
		LinearUnit MiB = MEMORY.getUnit("MiB");
		assertNotNull(MiB);
		LinearUnit GiB = MEMORY.getUnit("GiB");
		assertNotNull(GiB);
		IQuantity limit32bit = GiB.quantity(4);
		assertEquals(4096, limit32bit.longValueIn(MiB));
	}

	@Test
	public void testMemoryUnitsGenerally() throws Exception {
		LinearUnit MiB = (LinearUnit) UnitLookup.getUnitOrNull("memory:MiB");
		assertNotNull(MiB);
		LinearUnit GiB = (LinearUnit) UnitLookup.getUnitOrNull("memory:GiB");
		assertNotNull(GiB);
		IQuantity limit32bit = GiB.quantity(4);
		assertEquals(4096, limit32bit.longValueIn(MiB));
	}

	@Test
	public void testBadMemoryUnits() throws Exception {
		assertNull(MEMORY.getUnit("MB"));
		assertNull(MEMORY.getUnit("GB"));
		assertNull(MEMORY.getUnit("KB"));
		assertNull(MEMORY.getUnit("kB"));
	}

	@Test
	public void testBadMemoryUnitsGenerally() throws Exception {
		assertNull(UnitLookup.getUnitOrNull("memory:MB"));
		assertNull(UnitLookup.getUnitOrNull("memory:GB"));
		assertNull(UnitLookup.getUnitOrNull("memory:KB"));
		assertNull(UnitLookup.getUnitOrNull("memory:kB"));
	}

	@Test
	public void testCustomUnitConversion() throws Exception {
		LinearKindOfQuantity memory = MEMORY;
		ITypedQuantity<LinearUnit> unitBase = memory.parsePersisted("14075211111991929 TiB");
		LinearUnit customUnit = memory.makeCustomUnit(unitBase);
		IQuantity quantity = customUnit.quantity(-690087618782632812L);
		IQuantity wellKnown = memory.parsePersisted(quantity.persistableString());
		AdHocQuantityTest.assertNearlySame(wellKnown, quantity);
		AdHocQuantityTest.assertNearlySame(quantity, wellKnown);
	}

}
