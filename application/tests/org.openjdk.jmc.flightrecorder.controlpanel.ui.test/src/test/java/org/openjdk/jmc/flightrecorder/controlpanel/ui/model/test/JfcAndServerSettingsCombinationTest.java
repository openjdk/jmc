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
package org.openjdk.jmc.flightrecorder.controlpanel.ui.model.test;

import static org.junit.Assume.assumeTrue;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.configuration.events.SchemaVersion;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfiguration;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.rjmx.ConnectionException;
import org.openjdk.jmc.rjmx.ServiceNotAvailableException;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;

/**
 * Test that checks how JFC event settings combine with event settings from the server.
 */
@SuppressWarnings("nls")
public class JfcAndServerSettingsCombinationTest extends JfrControlTestCase {
	IFlightRecorderService service;
	private SchemaVersion version;

	private IConstrainedMap<EventOptionID> combineConfig(String comparisonType) throws Exception {
		EventConfiguration config = (EventConfiguration) loadConfig(
				comparisonType + "_" + version.attributeValue() + ".jfc");
		return config.getEventOptions(service.getDefaultEventOptions().emptyWithSameConstraints());
	}

	@Before
	public void beforeTest() throws ConnectionException, ServiceNotAvailableException {
		assumeHotSpot7u12OrLater(getConnectionHandle());
		service = getFlightRecorderService();
		version = SchemaVersion.fromBeanVersion(service.getVersion());
	}

	private static void assertContains(EventOptionID expectedOption, IConstrainedMap<EventOptionID> actualMap) {
		Assert.assertNotNull("Expected event option " + expectedOption + " to be included in the combined options.",
				actualMap.get(expectedOption));
	}

	private static void assertNotContains(EventOptionID unexpectedOption, IConstrainedMap<EventOptionID> actualMap) {
		Assert.assertNull(
				"Did not expect event option " + unexpectedOption + " to be included in the combined options.",
				actualMap.get(unexpectedOption));
	}

	private static void assertOptionEquals(
		EventOptionID expectedOption, Object expectedValue, IConstrainedMap<EventOptionID> actualMap) {
		Assert.assertEquals("Unexpected value for event option " + expectedOption + " in the combined options.",
				expectedValue, actualMap.get(expectedOption));
	}

	@Test
	public void testSame() throws Exception {
		IConstrainedMap<EventOptionID> cfg = combineConfig("same");

		switch (version) {
		case V1:
			assertOptionEquals(jvm("java/thread_end", "enabled"), Boolean.TRUE, cfg);
			break;
		case V2:
			assertOptionEquals(v2(JdkTypeIDs.JAVA_THREAD_END, "enabled"), Boolean.TRUE, cfg);
			break;
		}
	}

	@Test
	public void testMoreInJfc() throws Exception {
		IConstrainedMap<EventOptionID> cfg = combineConfig("more");
		switch (version) {
		case V1:
			assertOptionEquals(jdk("java/exception_throw", "enabled"), Boolean.FALSE, cfg);
			assertNotContains(jvm("com/oracle/jmc/extra_event_for_testing", "enabled"), cfg);
			break;
		case V2:
			assertOptionEquals(v2("org.openjdk.jmc.ExtraEventForTesting", "enabled"), Boolean.TRUE, cfg);
			assertContains(v2(JdkTypeIDs.THREAD_ALLOCATION_STATISTICS, "enabled"), cfg);
			break;
		}
	}

	@Test
	public void testLessInJfc() throws Exception {
		IConstrainedMap<EventOptionID> cfg = combineConfig("less");
		switch (version) {
		case V1:
			assertOptionEquals(jdk("java/exception_throw", "enabled"), Boolean.FALSE, cfg);
			assertNotContains(jfr_info("recordings/recording", "enabled"), cfg);
			break;
		case V2:
			assertOptionEquals(v2(JdkTypeIDs.EXCEPTIONS_THROWN, "enabled"), Boolean.FALSE, cfg);
			assertNotContains(v2(JdkTypeIDs.RECORDINGS, "enabled"), cfg);
			break;
		}
	}

	@Test
	public void testDiff() throws Exception {
		IConstrainedMap<EventOptionID> cfg = combineConfig("diff");
		switch (version) {
		case V1:
			assertNotContains(jvm("java/statistics/threads", "enabled"), cfg);
			assertOptionEquals(jvm("java/statistics/thread_allocation", "enabled"), Boolean.TRUE, cfg);
			assertNotContains(jvm("java/statistics/class_loading", "enabledButWrongForTest"), cfg);
			assertNotContains(jvm("java/statistics/thread_allocation", "extraTestOption"), cfg);
			break;
		case V2:
			assertNotContains(v2(JdkTypeIDs.THREAD_STATISTICS, "enabled"), cfg);
			assertContains(v2(JdkTypeIDs.THREAD_ALLOCATION_STATISTICS, "enabled"), cfg);
			assertNotContains(v2(JdkTypeIDs.CLASS_LOAD_STATISTICS, "enabledButWrongForTest"), cfg);
			assertNotContains(v2(JdkTypeIDs.THREAD_ALLOCATION_STATISTICS, "extraTestOption"), cfg);
			break;
		}
	}

	@Test
	public void testCustomInJfc() throws Exception {
		assumeTrue(SchemaVersion.V2.equals(version));
		IConstrainedMap<EventOptionID> cfg = combineConfig("custom");
		assertContains(v2("org.openjdk.jmc.smx.Transaction", "enabled"), cfg);
		assertContains(v2("org.openjdk.jmc.smx.Transaction", "stackTrace"), cfg);
		assertContains(v2("org.openjdk.jmc.smx.Transaction", "threshold"), cfg);
		assertContains(v2("org.openjdk.jmc.smx.Transaction", "filterWithDifferentContentTypeButSameKeyAndLabel"), cfg);
		assertContains(v2("org.openjdk.jmc.smx.Transaction", "textFilterWithSameKeyButDifferentLabel"), cfg);
		assertContains(v2("org.openjdk.jmc.smx.Transaction", "timeSpanFilterWithDifferentKeyButSameLabel"), cfg);
	}
}
