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
package org.openjdk.jmc.rjmx.services.jfr.test;

import static org.hamcrest.CoreMatchers.is;
import static org.openjdk.jmc.common.unit.UnitLookup.HOUR;
import static org.openjdk.jmc.common.unit.UnitLookup.MILLISECOND;
import static org.openjdk.jmc.common.unit.UnitLookup.MINUTE;
import static org.openjdk.jmc.common.unit.UnitLookup.NANOSECOND;
import static org.openjdk.jmc.common.unit.UnitLookup.SECOND;
import static org.openjdk.jmc.common.unit.UnitLookup.TIMESPAN;
import static org.openjdk.jmc.flightrecorder.configuration.internal.CommonConstraints.POSITIVE_TIMESPAN;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.openjdk.jmc.common.unit.DecimalPrefix;
import org.openjdk.jmc.common.unit.IDescribedMap;
import org.openjdk.jmc.common.unit.IMutableConstrainedMap;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.configuration.events.IEventTypeID;
import org.openjdk.jmc.flightrecorder.configuration.events.SchemaVersion;
import org.openjdk.jmc.flightrecorder.configuration.internal.CommonConstraints;
import org.openjdk.jmc.flightrecorder.configuration.internal.EventTypeIDV1;
import org.openjdk.jmc.flightrecorder.configuration.internal.EventTypeIDV2;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;

/**
 * Tests for low level response to various event options in offline mode.
 */
@RunWith(Suite.class)
@SuiteClasses({OnlineEventOptionsTest.Common.class, OnlineEventOptionsTest.TestV1.class,
		OnlineEventOptionsTest.TestV2.class})
@SuppressWarnings("nls")
public class OnlineEventOptionsTest {

	public static class Base extends JfrTestCase {
		private final SchemaVersion versionRestriction;
		protected IDescribedMap<EventOptionID> defaults;
		protected IMutableConstrainedMap<EventOptionID> mutable;
		protected IEventTypeID periodicTypeID;
		protected IEventTypeID latencyTypeID;
		protected IEventTypeID unknownTypeID;
		protected IEventTypeID invalidTypeID;

		protected Base(SchemaVersion version) {
			versionRestriction = version;
		}

		@Before
		public void before() throws Exception {
			IFlightRecorderService service = getFlightRecorderService();
			SchemaVersion version = SchemaVersion.fromBeanVersion(service.getVersion());
			defaults = service.getDefaultEventOptions();
			mutable = defaults.emptyWithSameConstraints();

			if (versionRestriction != null) {
				Assume.assumeThat(version, is(versionRestriction));
			}

			IEventTypeID typeV1 = new EventTypeIDV1("http://www.example.org/bar/", "some/short/path");
			IEventTypeID typeV2 = new EventTypeIDV2("some.package.Event");
			if (version == SchemaVersion.V1) {
				periodicTypeID = jvm("java/statistics/threads");
				latencyTypeID = jvm("java/thread_sleep");
				unknownTypeID = typeV1;
				invalidTypeID = typeV2;
			} else {
				periodicTypeID = new EventTypeIDV2("com.oracle.jdk.JavaThreadStatistics");
				latencyTypeID = new EventTypeIDV2("com.oracle.jdk.ThreadSleep");
				unknownTypeID = typeV2;
				invalidTypeID = typeV1;
			}
		}

		protected final EventOptionID periodic(String optionKey) {
			return new EventOptionID(periodicTypeID, optionKey);
		}

		protected final EventOptionID latency(String optionKey) {
			return new EventOptionID(latencyTypeID, optionKey);
		}

		protected final EventOptionID unknown(String optionKey) {
			return new EventOptionID(unknownTypeID, optionKey);
		}

		protected final EventOptionID invalid(String optionKey) {
			return new EventOptionID(invalidTypeID, optionKey);
		}
	}

	public static class Common extends Base {
		public Common() {
			super(null);
		}

		@Test
		public void testAddKnownEnabledTrue() throws QuantityConversionException {
			mutable.put(periodic("enabled"), Boolean.TRUE);
			mutable.put(latency("enabled"), Boolean.FALSE);
			Assert.assertEquals("true", mutable.getPersistableString(periodic("enabled")));
			Assert.assertEquals("false", mutable.getPersistableString(latency("enabled")));
		}

		@Test
		public void testAddKnownEnabledFalse() throws QuantityConversionException {
			mutable.put(periodic("enabled"), Boolean.FALSE);
			mutable.put(latency("enabled"), Boolean.TRUE);
			Assert.assertEquals("false", mutable.getPersistableString(periodic("enabled")));
			Assert.assertEquals("true", mutable.getPersistableString(latency("enabled")));
		}

		@Test
		public void testAddKnownEnabledPersisted() throws QuantityConversionException {
			mutable.putPersistedString(periodic("enabled"), "true");
			mutable.putPersistedString(latency("enabled"), "false");
			Assert.assertEquals(Boolean.TRUE, mutable.get(periodic("enabled")));
			Assert.assertEquals(Boolean.FALSE, mutable.get(latency("enabled")));
		}

		@Test
		public void testAddInvalidTypeEnabled() throws QuantityConversionException {
			try {
				mutable.put(invalid("enabled"), Boolean.FALSE);
				Assert.fail("Expected exception not thrown");
			} catch (IllegalArgumentException expected) {
			}
		}

		@Test
		public void testAddKnownEnabledStringAsObj() throws QuantityConversionException {
			try {
				mutable.put(periodic("enabled"), "true");
				Assert.fail("Expected exception not thrown");
			} catch (ClassCastException expected) {
			}
			try {
				mutable.put(latency("enabled"), "false");
				Assert.fail("Expected exception not thrown");
			} catch (ClassCastException expected) {
			}
		}

		@Test
		public void testAddKnownEnabledBanana() throws QuantityConversionException {
			try {
				mutable.put(periodic("enabled"), "Banana");
				Assert.fail("Expected exception not thrown");
			} catch (ClassCastException expected) {
			}
			try {
				mutable.put(latency("enabled"), "Banana");
				Assert.fail("Expected exception not thrown");
			} catch (ClassCastException expected) {
			}
		}

		@Test
		public void testAddKnownEnabledBananaPersisted() {
			try {
				mutable.putPersistedString(periodic("enabled"), "Banana");
				Assert.fail("Expected exception not thrown");
			} catch (QuantityConversionException expected) {
			}
			try {
				mutable.putPersistedString(latency("enabled"), "Banana");
				Assert.fail("Expected exception not thrown");
			} catch (QuantityConversionException expected) {
			}
		}

		@Test
		public void testAddWrongPeriod1h() throws QuantityConversionException {
			try {
				mutable.put(latency("period"), HOUR.quantity(1));
				Assert.fail("Expected exception not thrown");
			} catch (IllegalArgumentException expected) {
			}
		}

		@Test
		public void testAddWrongPeriod1hWithConstraint() throws QuantityConversionException {
			try {
				mutable.put(latency("period"), CommonConstraints.PERIOD_V1, HOUR.quantity(1));
				Assert.fail("Expected exception not thrown");
			} catch (IllegalArgumentException expected) {
			}
			try {
				mutable.put(latency("period"), CommonConstraints.PERIOD_V2, HOUR.quantity(1));
				Assert.fail("Expected exception not thrown");
			} catch (IllegalArgumentException expected) {
			}
		}

		@Test
		public void testAddKnownPeriod1h() throws QuantityConversionException {
			mutable.put(periodic("period"), HOUR.quantity(1));
			Assert.assertEquals("1 h", mutable.getPersistableString(periodic("period")));
		}

		@Test
		public void testAddKnownPeriodHourAndHalf() throws QuantityConversionException {
			mutable.put(periodic("period"), HOUR.quantity(1.5));
			// Cannot use "1.5 h" as JFR only parses integer numerical quantities.
			Assert.assertEquals("5400 s", mutable.getPersistableString(periodic("period")));
		}

		@Test
		public void testAddKnownPeriod2min() throws QuantityConversionException {
			mutable.put(periodic("period"), MINUTE.quantity(2));
			// Cannot use SI unit "2 min" as JFR cannot parse this.
			Assert.assertEquals("120 s", mutable.getPersistableString(periodic("period")));
			Assert.assertEquals(MINUTE.quantity(2), mutable.get(periodic("period")));
		}

		@Test
		public void testAddKnownPeriod2minPersistedSI() throws QuantityConversionException {
			mutable.putPersistedString(periodic("period"), "2 min");
			Assert.assertEquals("120 s", mutable.getPersistableString(periodic("period")));
			Assert.assertEquals(MINUTE.quantity(2), mutable.get(periodic("period")));
		}

		@Test
		public void testAddKnownPeriod2minPersistedNonSI() throws QuantityConversionException {
			// This non-SI unit (in the meaning "minute") may be received from JFR.
			mutable.putPersistedString(periodic("period"), "2 m");
			Assert.assertEquals("120 s", mutable.getPersistableString(periodic("period")));
			Assert.assertEquals(MINUTE.quantity(2), mutable.get(periodic("period")));
		}

		@Test
		public void testAddWrongThresholdAlmost5s() throws QuantityConversionException {
			try {
				mutable.put(periodic("threshold"), SECOND.quantity(4.711));
				Assert.fail("Expected exception not thrown");
			} catch (IllegalArgumentException expected) {
			}
		}

		@Test
		public void testAddWrongThresholdAlmost5sWithConstraint() throws QuantityConversionException {
			try {
				mutable.put(periodic("threshold"), POSITIVE_TIMESPAN, SECOND.quantity(4.711));
				Assert.fail("Expected exception not thrown");
			} catch (IllegalArgumentException expected) {
			}
		}

		@Test
		public void testAddKnownThresholdAlmost5s() throws QuantityConversionException {
			mutable.put(latency("threshold"), SECOND.quantity(4.711));
			Assert.assertEquals("4711 ms", mutable.getPersistableString(latency("threshold")));
		}

		@Test
		public void testAddKnownThreshold17ms() throws QuantityConversionException {
			mutable.put(latency("threshold"), MILLISECOND.quantity(17));
			Assert.assertEquals("17 ms", mutable.getPersistableString(latency("threshold")));
		}

		@Test
		public void testAddKnownThreshold18ns() throws QuantityConversionException {
			mutable.put(latency("threshold"), NANOSECOND.quantity(18.7));
			// Cannot use "18.7 ns" as JFR only parses integer numerical quantities.
			Assert.assertEquals("18 ns", mutable.getPersistableString(latency("threshold")));
			Assert.assertEquals(NANOSECOND.quantity(18.7), mutable.get(latency("threshold")));
		}

		@Test
		public void testAddKnownThreshold1700ps() throws QuantityConversionException {
			mutable.put(latency("threshold"), TIMESPAN.getUnit(DecimalPrefix.PICO).quantity(1700));
			// Cannot use "1700 ps" as JFR doesn't support smaller units than nanosecond.
			Assert.assertEquals("2 ns", mutable.getPersistableString(latency("threshold")));
		}
	}

	public static class TestV1 extends Base {
		public TestV1() {
			super(SchemaVersion.V1);
		}

		@Test
		public void testAddUnknownPeriod1hFail() throws QuantityConversionException {
			try {
				mutable.put(unknown("period"), HOUR.quantity(1));
				Assert.fail("Expected exception not thrown");
			} catch (IllegalArgumentException expected) {
			}
		}

		@Test
		public void testAddUnknownThresholdAlmost5sFail() throws QuantityConversionException {
			try {
				mutable.put(unknown("threshold"), SECOND.quantity(4.711));
				Assert.fail("Expected exception not thrown");
			} catch (IllegalArgumentException expected) {
			}
		}

		@Test
		public void testAddRandomBananaFail() throws QuantityConversionException {
			try {
				mutable.put(unknown("random"), "Banana");
				Assert.fail("Expected exception not thrown");
			} catch (IllegalArgumentException expected) {
			}
		}

		@Test
		public void testAddRandomBooleanV1() throws QuantityConversionException {
			try {
				mutable.put(unknown("random"), Boolean.TRUE);
				Assert.fail("Expected exception not thrown");
			} catch (IllegalArgumentException expected) {
			}
		}

		@Test
		public void testAddRandomConstrainedBooleanV1() throws QuantityConversionException {
			try {
				mutable.put(unknown("random"), UnitLookup.FLAG.getPersister(), Boolean.TRUE);
				Assert.fail("Expected exception not thrown");
			} catch (IllegalArgumentException expected) {
			}
		}

		@Test
		public void testAddRandomTimespanV1() throws QuantityConversionException {
			try {
				mutable.put(unknown("random"), MILLISECOND.quantity(17));
				Assert.fail("Expected exception not thrown");
			} catch (IllegalArgumentException expected) {
			}
		}

		@Test
		public void testAddRandomConstrainedTimespanV1() throws QuantityConversionException {
			try {
				mutable.put(unknown("random"), CommonConstraints.POSITIVE_TIMESPAN, SECOND.quantity(4.711));
				Assert.fail("Expected exception not thrown");
			} catch (IllegalArgumentException expected) {
			}
		}
	}

	public static class TestV2 extends Base {
		public TestV2() {
			super(SchemaVersion.V2);
		}

		@Test
		public void testAddUnknownPeriod1h() throws QuantityConversionException {
			mutable.put(unknown("period"), HOUR.quantity(1));
			Assert.assertEquals("1 h", mutable.getPersistableString(unknown("period")));
		}

		@Test
		public void testAddUnknownThresholdAlmost5s() throws QuantityConversionException {
			mutable.put(unknown("threshold"), SECOND.quantity(4.711));
			Assert.assertEquals("4711 ms", mutable.getPersistableString(unknown("threshold")));
		}

		@Test
		public void testAddRandomBanana() throws QuantityConversionException {
			mutable.put(unknown("random"), "Banana");
			Assert.assertEquals("Banana", mutable.getPersistableString(unknown("random")));
		}

		@Test
		public void testAddRandomBooleanV2() throws QuantityConversionException {
			try {
				mutable.put(unknown("random"), Boolean.TRUE);
				Assert.fail("Expected exception not thrown");
			} catch (ClassCastException expected) {
			}
		}

		@Test
		public void testAddRandomConstrainedBooleanV2() throws QuantityConversionException {
			mutable.put(unknown("random"), UnitLookup.FLAG.getPersister(), Boolean.TRUE);
			Assert.assertEquals("true", mutable.getPersistableString(unknown("random")));
		}

		@Test
		public void testAddRandomTimespanV2() throws QuantityConversionException {
			try {
				mutable.put(unknown("random"), MILLISECOND.quantity(17));
				Assert.fail("Expected exception not thrown");
			} catch (ClassCastException expected) {
			}
		}

		@Test
		public void testAddRandomConstrainedTimespanV2() throws QuantityConversionException {
			mutable.put(unknown("random"), CommonConstraints.POSITIVE_TIMESPAN, SECOND.quantity(4.711));
			Assert.assertEquals("4711 ms", mutable.getPersistableString(unknown("random")));
		}

		@Test
		public void testAddRandomConstrainedMultipleV2() throws QuantityConversionException {
			mutable.put(unknown("random"), UnitLookup.FLAG.getPersister(), Boolean.TRUE);
			try {
				mutable.put(unknown("random"), CommonConstraints.POSITIVE_TIMESPAN, SECOND.quantity(4.711));
				Assert.fail("Expected exception not thrown");
			} catch (IllegalArgumentException expected) {
			}
		}
	}
}
