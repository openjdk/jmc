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

import static org.openjdk.jmc.common.unit.BinaryPrefix.KIBI;
import static org.openjdk.jmc.common.unit.BinaryPrefix.MEBI;
import static org.openjdk.jmc.common.unit.BinaryPrefix.NOBI;
import static org.openjdk.jmc.common.unit.BinaryPrefix.TEBI;
import static org.openjdk.jmc.common.unit.BinaryPrefix.YOBI;
import static org.openjdk.jmc.common.unit.DecimalPrefix.CENTI;
import static org.openjdk.jmc.common.unit.DecimalPrefix.KILO;
import static org.openjdk.jmc.common.unit.DecimalPrefix.MICRO;
import static org.openjdk.jmc.common.unit.DecimalPrefix.MILLI;
import static org.openjdk.jmc.common.unit.DecimalPrefix.NONE;
import static org.openjdk.jmc.common.unit.DecimalPrefix.YOCTO;
import static org.openjdk.jmc.common.unit.DecimalPrefix.YOTTA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.DecimalFormatSymbols;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.openjdk.jmc.common.test.MCTestCase;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.KindOfQuantity;
import org.openjdk.jmc.common.unit.LinearKindOfQuantity;
import org.openjdk.jmc.common.unit.LinearUnit;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;

/**
 * Simple, ad-hoc testing of quantities. Start testing new constructs here, to show their usability
 * when the units are known at compile time. Broad testing of constructs for all kinds of quantities
 * and units can then be added to {@link KindOfQuantityTest}. UnitLookup is generally not used here.
 */
@SuppressWarnings("nls")
public class AdHocQuantityTest extends MCTestCase {
	private char decimalSep;
	private LinearKindOfQuantity length;
	private LinearUnit um;
	private LinearUnit mm;
	private LinearUnit cm;
	private LinearUnit m;
	private LinearUnit km;
	private IQuantity a4width;
	private IQuantity a4length;

	@Before
	public void setUp() throws Exception {
		// NOTE: Delayed to here so that the tests could be run in multiple Locales.
		decimalSep = DecimalFormatSymbols.getInstance().getDecimalSeparator();
		// Only needed when this suite is run on its own.
		@SuppressWarnings("unused")
		Object classInitWorkaround = UnitLookup.RAW_NUMBER;
		length = new LinearKindOfQuantity("length", "m", YOCTO, YOTTA);
		um = length.getUnit(MICRO);
		mm = length.getUnit(MILLI);
		cm = length.getUnit(CENTI);
		m = length.getUnit(NONE);
		km = length.getUnit(KILO);
		a4width = mm.quantity(211);
		a4length = cm.quantity(29.7);
	}

	@Test
	public void testComparision() {
		assertGreaterThan(a4width, a4length);
	}

	@Test
	public void testDecimalUnitConversion() throws Exception {
		assertEquals(297, a4length.longValueIn(mm, 300));
		assertEquals(297.0, a4length.doubleValueIn(mm), 0);
		assertEquals(0.297, a4length.doubleValueIn(m), 0);
		assertEquals(0.000297, a4length.doubleValueIn(km), 0);
		assertEquals(21.1, a4width.doubleValueIn(cm), 0);
		assertEquals(211, a4width.longValueIn(mm, 211));
	}

	@Test
	public void testDecimalUnitConversionProblem() {
		try {
			assertEquals(211, a4width.longValueIn(mm, 210));
			fail("Expected exception not thrown!");
		} catch (QuantityConversionException qce) {
			assertEquals(QuantityConversionException.Problem.TOO_HIGH, qce.getProblem());
			assertEquals("210 mm", qce.getPersistablePrototype());
		}
	}

	@Test
	public void testDecimalUnitConversionProblemNeg() throws Exception {
		IQuantity negA4width = mm.quantity(-211);
		assertEquals(-211, negA4width.longValueIn(mm, 210));
		try {
			assertEquals(-211, negA4width.longValueIn(mm, 209));
			fail("Expected exception not thrown!");
		} catch (QuantityConversionException qce) {
			assertEquals(QuantityConversionException.Problem.TOO_LOW, qce.getProblem());
			assertEquals("-210 mm", qce.getPersistablePrototype());
		}
	}

	@Test
	public void testDecimalUnitConversionProblemDouble() {
		try {
			assertEquals(297, a4length.longValueIn(mm, 296));
			fail("Expected exception not thrown!");
		} catch (QuantityConversionException qce) {
			assertEquals(QuantityConversionException.Problem.TOO_HIGH, qce.getProblem());
			assertEquals("296 mm", qce.getPersistablePrototype());
		}
	}

	@Test
	public void testDecimalUnitConversionProblemNegDouble() throws Exception {
		IQuantity negA4length = cm.quantity(-29.7);
		assertEquals(-297, negA4length.longValueIn(mm, 296));
		try {
			assertEquals(-297, negA4length.longValueIn(mm, 295));
			fail("Expected exception not thrown!");
		} catch (QuantityConversionException qce) {
			assertEquals(QuantityConversionException.Problem.TOO_LOW, qce.getProblem());
			assertEquals("-296 mm", qce.getPersistablePrototype());
		}
	}

	@Test
	public void testBinaryUnitConversion() throws Exception {
		LinearKindOfQuantity info = new LinearKindOfQuantity("information", "B", NOBI, YOBI);
		LinearUnit octet = info.getUnit(NOBI);
		LinearUnit bit = info.makeUnit("b", octet.quantity(1.0 / 8), "bit");
		LinearUnit kiB = info.getUnit(KIBI);
		LinearUnit MiB = info.getUnit(MEBI);
		LinearUnit TiB = info.getUnit(TEBI);
		IQuantity onekB = kiB.quantity(1);
		IQuantity oneMB = MiB.quantity(1);
		IQuantity oneTB = TiB.quantity(1);
		assertEquals(1024, onekB.longValueIn(octet));
		assertEquals(1024 * 1024, oneMB.longValueIn(octet, Integer.MAX_VALUE));
		assertEquals(1L << 43, oneTB.longValueIn(bit));
	}

	@Test
	public void testImperialUnitConversion() {
		LinearUnit inch = length.makeUnit("in", um.quantity(25400), "inch");
		LinearUnit foot = length.makeUnit("ft", inch.quantity(12), "foot");
		LinearUnit yard = length.makeUnit("yd", foot.quantity(3), "yard");
		assertEquals(30.48, foot.quantity(1).doubleValueIn(cm), 0.00001);
		assertEquals(304.8, foot.quantity(1).doubleValueIn(mm), 0.0001);
		assertEquals(0.9144, yard.quantity(1).doubleValueIn(m), 0.00000001);
		assertEquals(36.0, yard.quantity(1).doubleValueIn(inch), 0.0001);
	}

	@Test
	public void testPersisted() throws Exception {
		IQuantity len = length.parsePersisted("1.2019 m");
		assertEquals(1, len.longValue());
		assertEquals(1201, len.longValueIn(mm));
		IQuantity mm100 = length.parsePersisted("100 mm");
		assertEquals(100, mm100.longValue());
	}

	@Test
	public void testPersistedUnparseable() throws Exception {
		try {
			length.parsePersisted("120.0.4 cm");
			fail("Expected exception not thrown!");
		} catch (QuantityConversionException qce) {
			assertEquals(QuantityConversionException.Problem.UNPARSEABLE, qce.getProblem());
		}
	}

	@Test
	public void testPersistedNoUnit() throws Exception {
		try {
			length.parsePersisted("1200.4");
			fail("Expected exception not thrown!");
		} catch (QuantityConversionException qce) {
			assertEquals(QuantityConversionException.Problem.NO_UNIT, qce.getProblem());
		}
	}

	@Test
	public void testPersistedUnknownUnit() throws Exception {
		try {
			length.parsePersisted("1200.4 a");
			fail("Expected exception not thrown!");
		} catch (QuantityConversionException qce) {
			assertEquals(QuantityConversionException.Problem.UNKNOWN_UNIT, qce.getProblem());
		}
	}

	public static void assertPersistedRoundtrip(IQuantity quantity) throws QuantityConversionException {
		KindOfQuantity<?> kind = quantity.getUnit().getContentType();
		String storedText = quantity.persistableString();
		IQuantity newQuantity = kind.parsePersisted(storedText);
		assertEquals(quantity, newQuantity);
	}

	@Test
	public void testPersistedLong() throws Exception {
		assertPersistedRoundtrip(a4width);
		// Test a large number of quantities, but keep test deterministic.
		Random rnd = new Random(17);
		for (int i = 0; i < 1000; i++) {
			IQuantity length = um.quantity(rnd.nextLong());
			assertPersistedRoundtrip(length);
		}
	}

	@Test
	public void testPersistedDouble() throws Exception {
		assertPersistedRoundtrip(a4length);
		// Test a large number of quantities, but keep test deterministic.
		Random rnd = new Random(17);
		for (int i = 0; i < 1000; i++) {
			IQuantity length = um.quantity(rnd.nextDouble());
			assertPersistedRoundtrip(length);
		}
	}

	@Test
	public void testInteractive() throws Exception {
		IQuantity len = length.parseInteractive("1" + decimalSep + "2019 m");
		assertEquals(1, len.longValue());
		assertEquals(1201, len.longValueIn(mm));
		IQuantity mm100 = length.parseInteractive("100 mm");
		assertEquals(100, mm100.longValue());
	}

	@Test
	public void testInteractiveHexadecimal() throws Exception {
		IQuantity address = UnitLookup.ADDRESS.parseInteractive("0x47");
		assertEquals(0x47, address.longValue());
		address = UnitLookup.ADDRESS.parseInteractive("0xaf");
		assertEquals(0xAF, address.longValue());
		address = UnitLookup.ADDRESS.parseInteractive("0xAF");
		assertEquals(0xAF, address.longValue());

		boolean gotException = false;
		try {
			address = UnitLookup.ADDRESS.parseInteractive("0xgg");
		} catch (QuantityConversionException e) {
			gotException = true;
		}
		assertTrue("Managed to parse bad hex number without exception!", gotException);
	}

	@Test
	public void testInteractiveHexadecimalBytes() throws Exception {
		IQuantity mem = UnitLookup.MEMORY.parseInteractive("0x47 KiB");
		assertEquals(0x47, mem.longValue());
		assertEquals(72704, mem.in(UnitLookup.BYTE).longValue());
		mem = UnitLookup.MEMORY.parseInteractive("0xaf KiB");
		assertEquals(0xAF, mem.longValue());
		mem = UnitLookup.MEMORY.parseInteractive("0xAF KiB");
		assertEquals(0xAF, mem.longValue());

		boolean gotException = false;
		try {
			mem = UnitLookup.MEMORY.parseInteractive("0xgg KiB");
		} catch (QuantityConversionException e) {
			gotException = true;
		}
		assertTrue("Managed to parse bad hex number without exception!", gotException);

		gotException = false;
		try {
			mem = UnitLookup.MEMORY.parseInteractive("0xaf");
		} catch (QuantityConversionException e) {
			gotException = true;
		}
		assertTrue("Managed to parse hex number without unit without exception!", gotException);
	}

	@Test
	public void testInteractiveUnparseable() throws Exception {
		try {
			length.parseInteractive("120" + decimalSep + "0" + decimalSep + "4 cm");
			fail("Expected exception not thrown!");
		} catch (QuantityConversionException qce) {
			assertEquals(QuantityConversionException.Problem.UNPARSEABLE, qce.getProblem());
		}
	}

	@Test
	public void testInteractiveNoUnit() throws Exception {
		try {
			length.parseInteractive("1200" + decimalSep + "4");
			fail("Expected exception not thrown!");
		} catch (QuantityConversionException qce) {
			assertEquals(QuantityConversionException.Problem.NO_UNIT, qce.getProblem());
		}
	}

	@Test
	public void testInteractiveUnknownUnit() throws Exception {
		try {
			length.parseInteractive("1200" + decimalSep + "4 a");
			fail("Expected exception not thrown!");
		} catch (QuantityConversionException qce) {
			assertEquals(QuantityConversionException.Problem.UNKNOWN_UNIT, qce.getProblem());
		}
	}

	public static void assertNearlySame(IQuantity expected, IQuantity actual) {
		assertNearlySame(null, expected, actual);
	}

	public static void assertNearlySame(String message, IQuantity expected, IQuantity actual) {
		if (expected.compareTo(actual) == 0) {
			return;
		}
		double expectedNum = expected.doubleValueIn(actual.getUnit());
		double actualNum = actual.doubleValue();
		double ulp = Math.ulp(expectedNum);
		if (!(Math.abs(expectedNum - actualNum) <= Math.scalb(ulp, 2))) {
			failNotEquals(message, expected, actual);
		}
	}

	public static void assertInteractiveRoundtrip(IQuantity quantity) throws QuantityConversionException {
		KindOfQuantity<?> kind = quantity.getUnit().getContentType();
		String uiText = quantity.interactiveFormat();
		IQuantity newQuantity = kind.parseInteractive(uiText);
		assertEquals(quantity, newQuantity);
	}

	@Test
	public void testInteractiveLong() throws Exception {
		assertInteractiveRoundtrip(a4width);
		// Test a large number of quantities, but keep test deterministic.
		Random rnd = new Random(17);
		for (int i = 0; i < 1000; i++) {
			IQuantity length = um.quantity(rnd.nextLong());
			assertInteractiveRoundtrip(length);
		}
	}

	@Test
	public void testInteractiveDouble() throws Exception {
		assertInteractiveRoundtrip(a4length);
		// Test a large number of quantities, but keep test deterministic.
		Random rnd = new Random(17);
		for (int i = 0; i < 1000; i++) {
			IQuantity length = um.quantity(rnd.nextDouble());
			assertInteractiveRoundtrip(length);
		}
	}
}
