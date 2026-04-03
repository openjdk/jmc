/*
 * Copyright (c) 2018, 2026, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.common.test.unit;

import org.junit.Test;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.item.PersistableItemFilter;
import org.openjdk.jmc.common.unit.IPersister;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.StateToolkit;
import org.openjdk.jmc.test.MCTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for persister round-trip functionality, ensuring that values can be correctly persisted and
 * restored for primitive types like RAW_NUMBER, RAW_LONG, COUNT, INDEX, and IDENTIFIER. This helps
 * prevent regressions when these types are used in filterable attributes.
 */
@SuppressWarnings("nls")
public class PersisterRoundTripTest extends MCTestCase {

	/**
	 * Helper method to test round-trip persistence for a given persister and value.
	 */
	private <T> void assertRoundTrip(IPersister<T> persister, T value) throws QuantityConversionException {
		String persisted = persister.persistableString(value);
		assertNotNull("Persisted string should not be null", persisted);

		T restored = persister.parsePersisted(persisted);
		assertEquals("Round-trip should preserve value", value, restored);
	}

	/**
	 * Helper method to test round-trip persistence for Number types, comparing numeric values.
	 */
	private void assertNumberRoundTrip(IPersister<Number> persister, Number value) throws QuantityConversionException {
		String persisted = persister.persistableString(value);
		assertNotNull("Persisted string should not be null", persisted);

		Number restored = persister.parsePersisted(persisted);
		assertEquals("Round-trip should preserve numeric value", value.doubleValue(), restored.doubleValue(), 0.0001);
	}

	/**
	 * Helper method to test filter persistence for a given attribute and value.
	 */
	private <T> void assertFilterRoundTrip(IAttribute<T> attribute, T value) throws Exception {
		IItemFilter filter = ItemFilters.equals(attribute, value);
		String document = StateToolkit.toXMLString((PersistableItemFilter) filter);

		IItemFilter restoredFilter = PersistableItemFilter.readFrom(StateToolkit.fromXMLString(document));
		assertNotNull("Restored filter should not be null", restoredFilter);
	}

	@Test
	public void testRawNumberPersister() throws QuantityConversionException {
		IPersister<Number> persister = UnitLookup.RAW_NUMBER.getPersister();

		// Test various number types - note that the persister may normalize types
		assertNumberRoundTrip(persister, Integer.valueOf(42));
		assertNumberRoundTrip(persister, Integer.valueOf(-100));
		assertNumberRoundTrip(persister, Integer.valueOf(0));
		assertNumberRoundTrip(persister, Long.valueOf(9223372036854775807L));
		assertNumberRoundTrip(persister, Double.valueOf(3.14159));
		assertNumberRoundTrip(persister, Float.valueOf(2.71828f));
	}

	@Test
	public void testRawLongPersister() throws QuantityConversionException {
		IPersister<Long> persister = UnitLookup.RAW_LONG.getPersister();

		// Test various long values
		assertRoundTrip(persister, Long.valueOf(0L));
		assertRoundTrip(persister, Long.valueOf(42L));
		assertRoundTrip(persister, Long.valueOf(-100L));
		assertRoundTrip(persister, Long.valueOf(Long.MAX_VALUE));
		assertRoundTrip(persister, Long.valueOf(Long.MIN_VALUE));
	}

	@Test
	public void testCountPersister() throws QuantityConversionException {
		IPersister<Number> persister = UnitLookup.COUNT.getPersister();

		// Test count values
		assertNumberRoundTrip(persister, Integer.valueOf(0));
		assertNumberRoundTrip(persister, Integer.valueOf(1));
		assertNumberRoundTrip(persister, Integer.valueOf(1000));
		assertNumberRoundTrip(persister, Long.valueOf(1000000L));
	}

	@Test
	public void testIndexPersister() throws QuantityConversionException {
		IPersister<Number> persister = UnitLookup.INDEX.getPersister();

		// Test index values
		assertNumberRoundTrip(persister, Integer.valueOf(0));
		assertNumberRoundTrip(persister, Integer.valueOf(1));
		assertNumberRoundTrip(persister, Integer.valueOf(100));
		assertNumberRoundTrip(persister, Long.valueOf(999999L));
	}

	@Test
	public void testIdentifierPersister() throws QuantityConversionException {
		IPersister<Number> persister = UnitLookup.IDENTIFIER.getPersister();

		// Test identifier values
		assertNumberRoundTrip(persister, Integer.valueOf(1));
		assertNumberRoundTrip(persister, Integer.valueOf(12345));
		assertNumberRoundTrip(persister, Long.valueOf(987654321L));
	}
}
