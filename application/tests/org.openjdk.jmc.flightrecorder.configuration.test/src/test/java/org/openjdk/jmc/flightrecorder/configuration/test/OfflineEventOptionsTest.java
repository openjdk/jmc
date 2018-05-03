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
package org.openjdk.jmc.flightrecorder.configuration.test;

import static org.openjdk.jmc.common.unit.UnitLookup.HOUR;
import static org.openjdk.jmc.common.unit.UnitLookup.MILLISECOND;
import static org.openjdk.jmc.common.unit.UnitLookup.MINUTE;
import static org.openjdk.jmc.common.unit.UnitLookup.NANOSECOND;
import static org.openjdk.jmc.common.unit.UnitLookup.SECOND;
import static org.openjdk.jmc.common.unit.UnitLookup.TIMESPAN;
import static org.openjdk.jmc.flightrecorder.configuration.internal.CommonConstraints.BEGIN_CHUNK_MAGIC_INSTANCE;
import static org.openjdk.jmc.flightrecorder.configuration.internal.CommonConstraints.END_CHUNK_MAGIC_INSTANCE;
import static org.openjdk.jmc.flightrecorder.configuration.internal.CommonConstraints.EVERY_CHUNK_MAGIC_INSTANCE;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.openjdk.jmc.common.unit.DecimalPrefix;
import org.openjdk.jmc.common.unit.IDescribedMap;
import org.openjdk.jmc.common.unit.IMutableConstrainedMap;
import org.openjdk.jmc.common.unit.LinearUnit;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.QuantityConversionException.Problem;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.configuration.ConfigurationToolkit;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.configuration.events.IEventTypeID;
import org.openjdk.jmc.flightrecorder.configuration.events.SchemaVersion;
import org.openjdk.jmc.flightrecorder.configuration.internal.CommonConstraints;
import org.openjdk.jmc.flightrecorder.configuration.internal.EventTypeIDV1;
import org.openjdk.jmc.flightrecorder.configuration.internal.EventTypeIDV2;

/**
 * Tests for low level response to various event options in offline mode.
 */
@RunWith(Suite.class)
@SuiteClasses({OfflineEventOptionsTest.TestV1.class, OfflineEventOptionsTest.TestV2.class})
@SuppressWarnings("nls")
public class OfflineEventOptionsTest {
	private static LinearUnit PICOSECOND = TIMESPAN.getUnit(DecimalPrefix.PICO);

	public static class Common {
		protected final IDescribedMap<EventOptionID> defaults;
		protected final IMutableConstrainedMap<EventOptionID> mutable;
		protected final IEventTypeID validTypeID;
		protected final IEventTypeID invalidTypeID;

		protected Common(SchemaVersion version) {
			defaults = ConfigurationToolkit.getEventOptions(version);
			mutable = defaults.emptyWithSameConstraints();
			IEventTypeID typeV1 = new EventTypeIDV1("http://www.example.org/bar/", "some/short/path");
			IEventTypeID typeV2 = new EventTypeIDV2("some.package.Event");
			if (version == SchemaVersion.V1) {
				validTypeID = typeV1;
				invalidTypeID = typeV2;
			} else {
				validTypeID = typeV2;
				invalidTypeID = typeV1;
			}
		}

		protected final EventOptionID option(String optionKey) {
			return new EventOptionID(validTypeID, optionKey);
		}

		protected final EventOptionID optionInvalid(String optionKey) {
			return new EventOptionID(invalidTypeID, optionKey);
		}

		@Test
		public void testAddEnabledTrue() throws QuantityConversionException {
			mutable.put(option("enabled"), Boolean.TRUE);
			Assert.assertEquals("true", mutable.getPersistableString(option("enabled")));
		}

		@Test
		public void testAddEnabledFalse() throws QuantityConversionException {
			mutable.put(option("enabled"), Boolean.FALSE);
			Assert.assertEquals("false", mutable.getPersistableString(option("enabled")));
		}

		@Test
		public void testAddEnabledTruePersisted() throws QuantityConversionException {
			mutable.putPersistedString(option("enabled"), "true");
			Assert.assertEquals(Boolean.TRUE, mutable.get(option("enabled")));
		}

		@Test
		public void testAddEnabledNull() throws QuantityConversionException {
			try {
				mutable.put(option("enabled"), null);
				Assert.fail("Expected exception not thrown");
			} catch (NullPointerException expected) {
			}
		}

		@Test
		public void testAddEnabledNullPersisted() throws QuantityConversionException {
			try {
				mutable.putPersistedString(option("enabled"), null);
				Assert.fail("Expected exception not thrown");
			} catch (NullPointerException expected) {
			}
		}

		@Test
		public void testAddInvalidTypeEnabled() throws QuantityConversionException {
			try {
				mutable.put(optionInvalid("enabled"), Boolean.FALSE);
				Assert.fail("Expected exception not thrown");
			} catch (IllegalArgumentException expected) {
			}
		}

		@Test
		public void testAddEnabledTrueStringAsObj() throws QuantityConversionException {
			try {
				mutable.put(option("enabled"), "true");
				Assert.fail("Expected exception not thrown");
			} catch (ClassCastException expected) {
			}
		}

		@Test
		public void testAddEnabledBanana() throws QuantityConversionException {
			try {
				mutable.put(option("enabled"), "Banana");
				Assert.fail("Expected exception not thrown");
			} catch (ClassCastException expected) {
			}
		}

		@Test
		public void testAddEnabledBananaPersisted() {
			try {
				mutable.putPersistedString(option("enabled"), "Banana");
				Assert.fail("Expected exception not thrown");
			} catch (QuantityConversionException expected) {
				Assert.assertEquals(Problem.UNPARSEABLE, expected.getProblem());
			}
		}

		@Test
		public void testAddStacktraceTrue() throws QuantityConversionException {
			mutable.put(option("stackTrace"), Boolean.TRUE);
			Assert.assertEquals("true", mutable.getPersistableString(option("stackTrace")));
		}

		@Test
		public void testAddStacktraceNull() throws QuantityConversionException {
			try {
				mutable.put(option("stackTrace"), null);
				Assert.fail("Expected exception not thrown");
			} catch (NullPointerException expected) {
			}
		}

		@Test
		public void testAddPeriod1h() throws QuantityConversionException {
			mutable.put(option("period"), HOUR.quantity(1));
			Assert.assertEquals("1 h", mutable.getPersistableString(option("period")));
		}

		@Test
		public void testAddPeriodHourAndHalf() throws QuantityConversionException {
			mutable.put(option("period"), HOUR.quantity(1.5));
			// Cannot use "1.5 h" as JFR only parses integer numerical quantities.
			Assert.assertEquals("5400 s", mutable.getPersistableString(option("period")));
		}

		@Test
		public void testAddPeriod2min() throws QuantityConversionException {
			mutable.put(option("period"), MINUTE.quantity(2));
			// Cannot use SI unit "2 min" as JFR cannot parse this.
			Assert.assertEquals("120 s", mutable.getPersistableString(option("period")));
			Assert.assertEquals(MINUTE.quantity(2), mutable.get(option("period")));
		}

		@Test
		public void testAddPeriod2minPersistedSI() throws QuantityConversionException {
			mutable.putPersistedString(option("period"), "2 min");
			Assert.assertEquals("120 s", mutable.getPersistableString(option("period")));
			Assert.assertEquals(MINUTE.quantity(2), mutable.get(option("period")));
		}

		@Test
		public void testAddPeriod2minPersistedNonSI() throws QuantityConversionException {
			// This non-SI unit (in the meaning "minute") may be received from JFR.
			mutable.putPersistedString(option("period"), "2 m");
			Assert.assertEquals("120 s", mutable.getPersistableString(option("period")));
			Assert.assertEquals(MINUTE.quantity(2), mutable.get(option("period")));
		}

		@Test
		public void testAddPeriodEveryChunkPersisted() throws QuantityConversionException {
			mutable.putPersistedString(option("period"), "everyChunk");
			Assert.assertEquals("everyChunk", mutable.getPersistableString(option("period")));
			Assert.assertSame(EVERY_CHUNK_MAGIC_INSTANCE, mutable.get(option("period")));
		}

		@Test
		public void testAddPeriodEveryChunkMagic() throws QuantityConversionException {
			mutable.put(option("period"), EVERY_CHUNK_MAGIC_INSTANCE);
			Assert.assertEquals("everyChunk", mutable.getPersistableString(option("period")));
			Assert.assertSame(EVERY_CHUNK_MAGIC_INSTANCE, mutable.get(option("period")));
		}

		@Test
		public void testAddPeriod999ps() {
			try {
				mutable.put(option("period"), PICOSECOND.quantity(999));
				Assert.fail("Expected exception not thrown");
			} catch (QuantityConversionException expected) {
				Assert.assertEquals(Problem.TOO_LOW, expected.getProblem());
			}
		}

		@Test
		public void testAddPeriodZeroNanosPersisted() {
			try {
				mutable.putPersistedString(option("period"), "0 ns");
				Assert.fail("Expected exception not thrown");
			} catch (QuantityConversionException expected) {
				Assert.assertEquals(Problem.TOO_LOW, expected.getProblem());
			}
		}

		@Test
		public void testAddPeriodNull() throws QuantityConversionException {
			try {
				mutable.put(option("period"), null);
				Assert.fail("Expected exception not thrown");
			} catch (NullPointerException expected) {
			}
		}

		@Test
		public void testAddPeriodNullPersisted() throws QuantityConversionException {
			try {
				mutable.putPersistedString(option("period"), null);
				Assert.fail("Expected exception not thrown");
			} catch (NullPointerException expected) {
			}
		}

		@Test
		public void testAddThresholdAlmost5s() throws QuantityConversionException {
			mutable.put(option("threshold"), SECOND.quantity(4.711));
			Assert.assertEquals("4711 ms", mutable.getPersistableString(option("threshold")));
		}

		@Test
		public void testAddThreshold17ms() throws QuantityConversionException {
			mutable.put(option("threshold"), MILLISECOND.quantity(17));
			Assert.assertEquals("17 ms", mutable.getPersistableString(option("threshold")));
		}

		@Test
		public void testAddThreshold18ns() throws QuantityConversionException {
			mutable.put(option("threshold"), NANOSECOND.quantity(18.7));
			// Cannot use "18.7 ns" as JFR only parses integer numerical quantities.
			Assert.assertEquals("18 ns", mutable.getPersistableString(option("threshold")));
			Assert.assertEquals(NANOSECOND.quantity(18.7), mutable.get(option("threshold")));
		}

		@Test
		public void testAddThreshold1700ps() throws QuantityConversionException {
			mutable.put(option("threshold"), PICOSECOND.quantity(1700));
			// Cannot use "1700 ps" as JFR doesn't support smaller units than nanosecond.
			Assert.assertEquals("2 ns", mutable.getPersistableString(option("threshold")));
		}

		@Test
		public void testAddThresholdNull() throws QuantityConversionException {
			try {
				mutable.put(option("threshold"), null);
				Assert.fail("Expected exception not thrown");
			} catch (NullPointerException expected) {
			}
		}

		@Test
		public void testAddThresholdNullPersisted() throws QuantityConversionException {
			try {
				mutable.putPersistedString(option("threshold"), null);
				Assert.fail("Expected exception not thrown");
			} catch (NullPointerException expected) {
			}
		}

	}

	public static class TestV1 extends Common {
		public TestV1() {
			super(SchemaVersion.V1);
		}

		@Test
		public void testAddRandomBananaFail() throws QuantityConversionException {
			try {
				mutable.put(option("random"), "Banana");
				Assert.fail("Expected exception not thrown");
			} catch (IllegalArgumentException expected) {
			}
		}

		@Test
		public void testAddRandomBooleanV1() throws QuantityConversionException {
			try {
				mutable.put(option("random"), Boolean.TRUE);
				Assert.fail("Expected exception not thrown");
			} catch (IllegalArgumentException expected) {
			}
		}

		@Test
		public void testAddRandomConstrainedBooleanV1() throws QuantityConversionException {
			try {
				mutable.put(option("random"), UnitLookup.FLAG.getPersister(), Boolean.TRUE);
				Assert.fail("Expected exception not thrown");
			} catch (IllegalArgumentException expected) {
			}
		}

		@Test
		public void testAddRandomTimespanV1() throws QuantityConversionException {
			try {
				mutable.put(option("random"), MILLISECOND.quantity(17));
				Assert.fail("Expected exception not thrown");
			} catch (IllegalArgumentException expected) {
			}
		}

		@Test
		public void testAddRandomConstrainedTimespanV1() throws QuantityConversionException {
			try {
				mutable.put(option("random"), CommonConstraints.POSITIVE_TIMESPAN, SECOND.quantity(4.711));
				Assert.fail("Expected exception not thrown");
			} catch (IllegalArgumentException expected) {
			}
		}
	}

	public static class TestV2 extends Common {
		public TestV2() {
			super(SchemaVersion.V2);
		}

		@Test
		public void testAddRandomBanana() throws QuantityConversionException {
			mutable.put(option("random"), "Banana");
			Assert.assertEquals("Banana", mutable.getPersistableString(option("random")));
		}

		@Test
		public void testAddRandomBooleanV2() throws QuantityConversionException {
			try {
				mutable.put(option("random"), Boolean.TRUE);
				Assert.fail("Expected exception not thrown");
			} catch (ClassCastException expected) {
			}
		}

		@Test
		public void testAddRandomConstrainedBooleanV2() throws QuantityConversionException {
			mutable.put(option("random"), UnitLookup.FLAG.getPersister(), Boolean.TRUE);
			Assert.assertEquals("true", mutable.getPersistableString(option("random")));
		}

		@Test
		public void testAddRandomTimespanV2() throws QuantityConversionException {
			try {
				mutable.put(option("random"), MILLISECOND.quantity(17));
				Assert.fail("Expected exception not thrown");
			} catch (ClassCastException expected) {
			}
		}

		@Test
		public void testAddRandomConstrainedTimespanV2() throws QuantityConversionException {
			mutable.put(option("random"), CommonConstraints.POSITIVE_TIMESPAN, SECOND.quantity(4.711));
			Assert.assertEquals("4711 ms", mutable.getPersistableString(option("random")));
		}

		@Test
		public void testAddRandomConstrainedMultipleV2() throws QuantityConversionException {
			mutable.put(option("random"), UnitLookup.FLAG.getPersister(), Boolean.TRUE);
			try {
				mutable.put(option("random"), CommonConstraints.POSITIVE_TIMESPAN, SECOND.quantity(4.711));
				Assert.fail("Expected exception not thrown");
			} catch (IllegalArgumentException expected) {
			}
		}

		@Test
		public void testAddPeriodBeginChunkPersisted() throws QuantityConversionException {
			mutable.putPersistedString(option("period"), "beginChunk");
			Assert.assertEquals("beginChunk", mutable.getPersistableString(option("period")));
			Assert.assertSame(BEGIN_CHUNK_MAGIC_INSTANCE, mutable.get(option("period")));
		}

		@Test
		public void testAddPeriodBeginChunkMagic() throws QuantityConversionException {
			mutable.put(option("period"), BEGIN_CHUNK_MAGIC_INSTANCE);
			Assert.assertEquals("beginChunk", mutable.getPersistableString(option("period")));
			Assert.assertSame(BEGIN_CHUNK_MAGIC_INSTANCE, mutable.get(option("period")));
		}

		@Test
		public void testAddPeriodEndChunkPersisted() throws QuantityConversionException {
			mutable.putPersistedString(option("period"), "endChunk");
			Assert.assertEquals("endChunk", mutable.getPersistableString(option("period")));
			Assert.assertSame(END_CHUNK_MAGIC_INSTANCE, mutable.get(option("period")));
		}

		@Test
		public void testAddPeriodEndChunkMagic() throws QuantityConversionException {
			mutable.put(option("period"), END_CHUNK_MAGIC_INSTANCE);
			Assert.assertEquals("endChunk", mutable.getPersistableString(option("period")));
			Assert.assertSame(END_CHUNK_MAGIC_INSTANCE, mutable.get(option("period")));
		}
	}
}
