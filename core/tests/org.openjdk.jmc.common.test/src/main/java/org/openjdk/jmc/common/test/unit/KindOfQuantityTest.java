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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.test.MCTestCase;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.ITypedQuantity;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.KindOfQuantity;
import org.openjdk.jmc.common.unit.LinearKindOfQuantity;
import org.openjdk.jmc.common.unit.LinearUnit;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;

@RunWith(Parameterized.class)
@SuppressWarnings("nls")
public class KindOfQuantityTest extends MCTestCase {
	@Parameter
	public KindOfQuantity<?> kindOfQuantity;
	@Parameter(value = 1)
	public String name;

	@Parameters(name = "{1}")
	public static Collection<Object[]> data() {
		ArrayList<Object[]> units = new ArrayList<>();
		for (ContentType<?> type : UnitLookup.getAllContentTypes()) {
			if (type instanceof KindOfQuantity) {
				Object[] array = new Object[2];
				array[0] = type;
				array[1] = KindOfQuantityTest.class.getSimpleName() + ": " + type.getIdentifier();
				units.add(array);
			}
		}
		return units;
	}

	protected static IQuantity randomQuantity(IUnit unit, Random rnd) {
		return rnd.nextBoolean() ? unit.quantity(rnd.nextDouble()) : unit.quantity(rnd.nextLong());
	}

	/**
	 * Same as {@link #randomQuantity(IUnit, Random)}, just more strictly typed.
	 */
	protected static ITypedQuantity<LinearUnit> randomQuantity(LinearUnit unit, Random rnd) {
		return rnd.nextBoolean() ? unit.quantity(rnd.nextDouble()) : unit.quantity(rnd.nextLong());
	}

	protected static ITypedQuantity<LinearUnit> randomPositiveQuantity(LinearUnit unit, Random rnd) {
		return rnd.nextBoolean() ? unit.quantity(Math.abs(rnd.nextDouble())) : unit.quantity(Math.abs(rnd.nextLong()));
	}

	protected static ITypedQuantity<LinearUnit> randomCustomQuantity(LinearUnit unit, Random rnd) {
		ITypedQuantity<LinearUnit> unitQuantity = randomPositiveQuantity(unit, rnd);
		LinearUnit customUnit = unit.getContentType().makeCustomUnit(unitQuantity);
		return randomQuantity(customUnit, rnd);
	}

	@RunWith(Parameterized.class)
	public static class UnitConversionTest extends MCTestCase {
		@Parameter
		public IUnit fromUnit;
		@Parameter(value = 1)
		public IUnit toUnit;
		@Parameter(value = 2)
		public String name;

		@Parameters(name = "{2}")
		public static Collection<Object[]> data() {
			ArrayList<Object[]> units = new ArrayList<>();
			for (ContentType<?> type : UnitLookup.getAllContentTypes()) {
				// FIXME: This should be tested on all kinds of quantities, not just linear, once the rest can be stored using doubles.
				if (type instanceof LinearKindOfQuantity) {
					KindOfQuantity<?> kind = (KindOfQuantity<?>) type;
					for (IUnit unit : kind.getAllUnits()) {
						for (IUnit toUnit : kind.getAllUnits()) {
							Object[] array = new Object[3];
							array[0] = unit;
							array[1] = toUnit;
							array[2] = UnitConversionTest.class.getSimpleName() + " (" + type.getIdentifier() + "): "
									+ unit.getIdentifier() + " -> " + toUnit.getIdentifier();
							units.add(array);
						}
					}
				}
			}
			return units;
		}

		@Test
		public void testRoundTripConversionDouble() {
			IQuantity fromQuantity = fromUnit.quantity(17.0);
			IQuantity toQuantity = fromQuantity.in(toUnit);
			assertEquals(fromQuantity.doubleValue(), toQuantity.in(fromUnit).doubleValue(), 0.000001);
		}
	}

	@RunWith(Parameterized.class)
	public static class ScalarUnitTest extends MCTestCase {
		@Parameter
		public IUnit unit;
		@Parameter(value = 1)
		public String name;

		@Parameters(name = "{1}")
		public static Collection<Object[]> data() {
			ArrayList<Object[]> units = new ArrayList<>();
			for (ContentType<?> type : UnitLookup.getAllContentTypes()) {
				// FIXME: Many tests in this suite could be tested on all kinds of quantities, not just linear, as it is today. Other tests could work if tweaked slightly.
				if (type instanceof LinearKindOfQuantity) {
					KindOfQuantity<?> kind = (KindOfQuantity<?>) type;
					for (IUnit unit : kind.getAllUnits()) {
						Object[] array = new Object[2];
						array[0] = unit;
						array[1] = ScalarUnitTest.class.getSimpleName() + " (" + type.getIdentifier() + "): "
								+ unit.getIdentifier();
						units.add(array);
					}
				}
			}
			return units;
		}

		@Test
		public void testDefaultFormat() {
			// Test a large number of quantities, but keep test deterministic.
			Random rnd = new Random(17);
			for (int i = 0; i < 1000; i++) {
				IQuantity quantity = randomQuantity(unit, rnd);
				try {
					assertNotNull(KindOfQuantity.format(quantity.doubleValue(), unit));
				} catch (NullPointerException e) {
					throw new NullPointerException("When formatting " + quantity);
				}
			}
		}

		private static void assertNotEquals(Object unexpected, Object actual) {
			if (unexpected.equals(actual)) {
				fail("Unexpectedly equal: " + unexpected + " and " + actual);
			}
		}

		private static void assertAllSpacesAreNonBreaking(String displayStr) {
			boolean hasBreaking = displayStr.contains(" ");
			if (hasBreaking) {
				fail("Breaking spaces in : " + displayStr.replace('\u00a0', '_'));
			}
		}

		private static void assertAllSpacesAreBreaking(String displayStr) {
			boolean hasNonBreaking = displayStr.contains("\u00a0");
			if (hasNonBreaking) {
				fail("Non-breaking spaces (shown as '_') in : " + displayStr.replace('\u00a0', '_'));
			}
		}

		@Test
		public void testUnitSymbol() {
			assertAllSpacesAreNonBreaking(unit.getLocalizedSymbol());
		}

		@Test
		public void testCommonFormats() {
			// Test a large number of quantities, but keep test deterministic.
			Random rnd = new Random(17);
			for (int i = 0; i < 1000; i++) {
				IQuantity quantity = randomQuantity(unit, rnd);
				String auto = quantity.displayUsing(IDisplayable.AUTO);
				String exact = quantity.displayUsing(IDisplayable.EXACT);
				String verbose = quantity.displayUsing(IDisplayable.VERBOSE);
				assertAllSpacesAreNonBreaking(auto);
				assertAllSpacesAreNonBreaking(exact);
				if (exact.length() == verbose.length()) {
					// This does not hold for custom units.
					assertAllSpacesAreNonBreaking(verbose);
				}
				// These should of course not be strict, but since it holds now, it is nice
				// to be informed of when it no longer holds.
				if (auto.length() > exact.length()) {
					fail("auto (" + auto + ").len <= exact (" + exact + ").len");
				}
				if (exact.length() > verbose.length()) {
					fail("exact (" + exact + ").len <= verbose (" + verbose + ").len");
				}
			}
		}

		@Test
		public void testInteractiveSpaces() {
			// Test a large number of quantities, but keep test deterministic.
			Random rnd = new Random(17);
			for (int i = 0; i < 1000; i++) {
				IQuantity quantity = randomQuantity(unit, rnd);
				String interactive = quantity.interactiveFormat();
				assertAllSpacesAreBreaking(interactive);
			}
		}

		@Test
		public void testPersistedSpaces() {
			// Test a large number of quantities, but keep test deterministic.
			Random rnd = new Random(17);
			for (int i = 0; i < 1000; i++) {
				IQuantity quantity = randomQuantity(unit, rnd);
				String persisted = quantity.persistableString();
				assertAllSpacesAreBreaking(persisted);
			}
		}

		@Test
		public void testCustomUnitSymbols() {
			Assume.assumeTrue("Custom units currently only supported for LinearUnit:s", unit instanceof LinearUnit);
			LinearUnit unit = (LinearUnit) this.unit;
			// Test a large number of quantities, but keep test deterministic.
			Random rnd = new Random(17);
			for (int i = 0; i < 1000; i++) {
				IQuantity quantity = randomCustomQuantity(unit, rnd);
				assertAllSpacesAreNonBreaking(quantity.getUnit().getLocalizedSymbol());
			}
		}

		@Test
		public void testCustomCommonFormats() {
			Assume.assumeTrue("Custom units currently only supported for LinearUnit:s", unit instanceof LinearUnit);
			LinearUnit unit = (LinearUnit) this.unit;
			// Test a large number of quantities, but keep test deterministic.
			Random rnd = new Random(17);
			for (int i = 0; i < 1000; i++) {
				IQuantity quantity = randomCustomQuantity(unit, rnd);
				String auto = quantity.displayUsing(IDisplayable.AUTO);
				String exact = quantity.displayUsing(IDisplayable.EXACT);
				String verbose = quantity.displayUsing(IDisplayable.VERBOSE);
				assertAllSpacesAreNonBreaking(auto);
				assertAllSpacesAreNonBreaking(exact);
				if (exact.length() == verbose.length()) {
					// This does not hold for custom units.
					assertAllSpacesAreNonBreaking(verbose);
				}
				// These should of course not be strict, but since it holds now, it is nice
				// to be informed of when it no longer holds.
				assertTrue("auto <= exact", auto.length() <= exact.length());
				assertTrue("exact <= verbose", exact.length() <= verbose.length());
			}
		}

		@Test
		public void testCustomInteractiveSpaces() {
			Assume.assumeTrue("Custom units currently only supported for LinearUnit:s", unit instanceof LinearUnit);
			LinearUnit unit = (LinearUnit) this.unit;
			// Test a large number of quantities, but keep test deterministic.
			Random rnd = new Random(17);
			for (int i = 0; i < 1000; i++) {
				ITypedQuantity<?> quantity = randomCustomQuantity(unit, rnd);
				String interactive = quantity.interactiveFormat();
				String customInteractive = quantity.interactiveFormat(true);
				assertAllSpacesAreBreaking(interactive);
				assertAllSpacesAreBreaking(customInteractive);
				assertNotEquals(interactive, customInteractive);
			}
		}

		@Test
		public void testCustomPersistedSpaces() {
			Assume.assumeTrue("Custom units currently only supported for LinearUnit:s", unit instanceof LinearUnit);
			LinearUnit unit = (LinearUnit) this.unit;
			// Test a large number of quantities, but keep test deterministic.
			Random rnd = new Random(17);
			for (int i = 0; i < 1000; i++) {
				IQuantity quantity = randomCustomQuantity(unit, rnd);
				String persisted = quantity.persistableString();
				assertAllSpacesAreBreaking(persisted);
			}
		}

		@Test
		public void testCustomInteractiveRoundtrip() throws Exception {
			Assume.assumeTrue("Custom units currently only supported for LinearUnit:s", unit instanceof LinearUnit);
			LinearUnit unit = (LinearUnit) this.unit;
			// Test a large number of quantities, but keep test deterministic.
			Random rnd = new Random(17);
			for (int i = 0; i < 1000; i++) {
				ITypedQuantity<LinearUnit> quantity = randomCustomQuantity(unit, rnd);
				String interactive = quantity.interactiveFormat();
				String customInteractive = quantity.interactiveFormat(true);
				assertNotEquals(interactive, customInteractive);
				IQuantity wellKnownQuantity = unit.getContentType().parseInteractive(interactive);
				IQuantity customQuantity = quantity.getUnit().customParseInteractive(customInteractive);
				assertNotEquals(wellKnownQuantity, customQuantity);
				// This should work both ways. Didn't for a while.
				AdHocQuantityTest.assertNearlySame(customQuantity, wellKnownQuantity);
				AdHocQuantityTest.assertNearlySame(wellKnownQuantity, customQuantity);
				try {
					IQuantity brokenQuantity = unit.getContentType().parseInteractive(customInteractive);
					fail("Didn't fail to parse '" + interactive + "' (into " + brokenQuantity + ')');
				} catch (QuantityConversionException expected) {
					assertEquals(QuantityConversionException.Problem.UNKNOWN_UNIT, expected.getProblem());
				}
			}
		}

		@Test
		public void testCustomPersistedRoundtrip() throws Exception {
			Assume.assumeTrue("Custom units currently only supported for LinearUnit:s", unit instanceof LinearUnit);
			LinearUnit unit = (LinearUnit) this.unit;
			// Test a large number of quantities, but keep test deterministic.
			Random rnd = new Random(17);
			for (int i = 0; i < 1000; i++) {
				// Cannot handle negative custom units yet.
				ITypedQuantity<LinearUnit> wellKnownQuantity = randomPositiveQuantity(unit, rnd);
				String wellKnownPersisted = wellKnownQuantity.persistableString();
				LinearUnit customUnit = unit.getContentType().makeCustomUnit(wellKnownQuantity);
				IQuantity customQuantity = customUnit.quantity(1);
				String customPersisted = customQuantity.persistableString();
				assertAllSpacesAreBreaking(customPersisted);
				assertAllSpacesAreBreaking(wellKnownPersisted);
				assertEquals(wellKnownPersisted, customPersisted);

				customQuantity = customUnit.quantity(17);
				customPersisted = customQuantity.persistableString();
				assertAllSpacesAreBreaking(customPersisted);
				IQuantity roundtripped = unit.getContentType().parsePersisted(customPersisted);
				assertNotEquals(customQuantity, roundtripped);
				// This should work both ways. Didn't for a while.
				AdHocQuantityTest.assertNearlySame(customQuantity, roundtripped);
				AdHocQuantityTest.assertNearlySame(roundtripped, customQuantity);
			}
		}

		@Test
		public void testPersistedLongRoundtrip() throws Exception {
			// Test a large number of quantities, but keep test deterministic.
			Random rnd = new Random(17);
			for (int i = 0; i < 1000; i++) {
				IQuantity quantity = unit.quantity(rnd.nextLong());
				AdHocQuantityTest.assertPersistedRoundtrip(quantity);
			}
		}

		@Test
		public void testPersistedDoubleRoundtrip() throws Exception {
			// Test a large number of quantities, but keep test deterministic.
			Random rnd = new Random(17);
			for (int i = 0; i < 1000; i++) {
				IQuantity quantity = unit.quantity(rnd.nextDouble());
				AdHocQuantityTest.assertPersistedRoundtrip(quantity);
			}
		}

		@Test
		public void testInteractiveLongRoundtrip() throws Exception {
			// Test a large number of quantities, but keep test deterministic.
			Random rnd = new Random(17);
			for (int i = 0; i < 1000; i++) {
				IQuantity quantity = unit.quantity(rnd.nextLong());
				AdHocQuantityTest.assertInteractiveRoundtrip(quantity);
			}
		}

		@Test
		public void testInteractiveDoubleRoundtrip() throws Exception {
			// Test a large number of quantities, but keep test deterministic.
			Random rnd = new Random(17);
			for (int i = 0; i < 1000; i++) {
				IQuantity quantity = unit.quantity(rnd.nextDouble());
				AdHocQuantityTest.assertInteractiveRoundtrip(quantity);
			}
		}
	}

	@Test
	@SuppressWarnings("unlikely-arg-type")
	public void testAllUnitsSupersetOfCommon() {
		if (!kindOfQuantity.getAllUnits().containsAll(kindOfQuantity.getCommonUnits())) {
			Set<IUnit> missing = new HashSet<IUnit>(kindOfQuantity.getCommonUnits());
			missing.removeAll(kindOfQuantity.getAllUnits());
			fail("Common units not in 'all': " + missing);
		}
	}

	@Test
	public void testUnitUniqueness() {
		Collection<? extends IUnit> units = kindOfQuantity.getAllUnits();
		Map<String, IUnit> strToUnit = new HashMap<>();
		for (IUnit unit : units) {
			assertNull("Unit " + unit + " ID collides with other unit", strToUnit.put(unit.getIdentifier(), unit));
			if (!unit.getIdentifier().equals(unit.getLocalizedSymbol())) {
				assertNull("Unit " + unit + " symbol collides with other unit",
						strToUnit.put(unit.getLocalizedDescription(), unit));
			}
		}
	}

	@Test
	public void testUnitLookupByIdentifier() {
		Collection<? extends IUnit> units = kindOfQuantity.getAllUnits();
		for (IUnit unit : units) {
			String id = UnitLookup.getUnitIdentifier(unit);
			IUnit backUnit = UnitLookup.getUnitOrNull(id);
			assertEquals(unit, backUnit);
		}
	}
}
