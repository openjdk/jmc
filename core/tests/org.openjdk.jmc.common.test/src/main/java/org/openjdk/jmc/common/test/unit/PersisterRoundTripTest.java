/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.jmc.common.unit.IPersister;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.test.MCTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

@SuppressWarnings("nls")
public class PersisterRoundTripTest extends MCTestCase {

	private static final String MALFORMED = "not_a_number";

	private void assertNumberRoundTrip(IPersister<Number> persister, Number value) throws QuantityConversionException {
		String persisted = persister.persistableString(value);
		assertNotNull(persisted);
		Number restored = persister.parsePersisted(persisted);
		if (value instanceof Long || value instanceof Integer) {
			assertEquals(value.longValue(), restored.longValue());
		} else {
			assertEquals(value.doubleValue(), restored.doubleValue(), 0.0001);
		}
	}

	private void assertLongRoundTrip(IPersister<Long> persister, Long value) throws QuantityConversionException {
		String persisted = persister.persistableString(value);
		assertNotNull(persisted);
		Long restored = persister.parsePersisted(persisted);
		assertEquals(value, restored);
	}

	@Test
	public void testRawNumber() throws QuantityConversionException {
		IPersister<Number> persister = UnitLookup.RAW_NUMBER.getPersister();
		assertNotNull(persister);
		assertNumberRoundTrip(persister, 0);
		assertNumberRoundTrip(persister, 42);
		assertNumberRoundTrip(persister, -100);
		assertNumberRoundTrip(persister, Long.MAX_VALUE);
		assertNumberRoundTrip(persister, 3.14);
	}

	@Test
	public void testRawNumberParseError() {
		IPersister<Number> persister = UnitLookup.RAW_NUMBER.getPersister();
		assertNotNull(persister);
		assertThrows(QuantityConversionException.class, () -> persister.parsePersisted(MALFORMED));
		assertThrows(QuantityConversionException.class, () -> persister.parseInteractive(MALFORMED));
	}

	@Test
	public void testRawLong() throws QuantityConversionException {
		IPersister<Long> persister = UnitLookup.RAW_LONG.getPersister();
		assertNotNull(persister);
		assertLongRoundTrip(persister, 0L);
		assertLongRoundTrip(persister, 42L);
		assertLongRoundTrip(persister, -100L);
		assertLongRoundTrip(persister, Long.MAX_VALUE);
		assertLongRoundTrip(persister, Long.MIN_VALUE);
	}

	@Test
	public void testRawLongParseError() {
		IPersister<Long> persister = UnitLookup.RAW_LONG.getPersister();
		assertNotNull(persister);
		assertThrows(QuantityConversionException.class, () -> persister.parsePersisted(MALFORMED));
		assertThrows(QuantityConversionException.class, () -> persister.parseInteractive(MALFORMED));
	}

	@Test
	public void testCount() throws QuantityConversionException {
		IPersister<Long> persister = UnitLookup.COUNT.getPersister();
		assertNotNull(persister);
		assertLongRoundTrip(persister, 0L);
		assertLongRoundTrip(persister, 1L);
		assertLongRoundTrip(persister, 1000L);
		assertLongRoundTrip(persister, 1000000L);
	}

	@Test
	public void testCountParseError() {
		IPersister<Long> persister = UnitLookup.COUNT.getPersister();
		assertNotNull(persister);
		assertThrows(QuantityConversionException.class, () -> persister.parsePersisted(MALFORMED));
		assertThrows(QuantityConversionException.class, () -> persister.parseInteractive(MALFORMED));
	}

	@Test
	public void testIndex() throws QuantityConversionException {
		IPersister<Long> persister = UnitLookup.INDEX.getPersister();
		assertNotNull(persister);
		assertLongRoundTrip(persister, 0L);
		assertLongRoundTrip(persister, 1L);
		assertLongRoundTrip(persister, 100L);
		assertLongRoundTrip(persister, 999999L);
	}

	@Test
	public void testIndexParseError() {
		IPersister<Long> persister = UnitLookup.INDEX.getPersister();
		assertNotNull(persister);
		assertThrows(QuantityConversionException.class, () -> persister.parsePersisted(MALFORMED));
		assertThrows(QuantityConversionException.class, () -> persister.parseInteractive(MALFORMED));
	}

	@Test
	public void testIdentifier() throws QuantityConversionException {
		IPersister<Long> persister = UnitLookup.IDENTIFIER.getPersister();
		assertNotNull(persister);
		assertLongRoundTrip(persister, 1L);
		assertLongRoundTrip(persister, 12345L);
		assertLongRoundTrip(persister, 987654321L);
	}

	@Test
	public void testIdentifierParseError() {
		IPersister<Long> persister = UnitLookup.IDENTIFIER.getPersister();
		assertNotNull(persister);
		assertThrows(QuantityConversionException.class, () -> persister.parsePersisted(MALFORMED));
		assertThrows(QuantityConversionException.class, () -> persister.parseInteractive(MALFORMED));
	}
}
