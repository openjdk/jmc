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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.Map;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.common.unit.IDescribedMap;
import org.openjdk.jmc.flightrecorder.configuration.ConfigurationToolkit;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.configuration.events.IEventConfiguration;
import org.openjdk.jmc.flightrecorder.configuration.events.IEventTypeID;
import org.openjdk.jmc.flightrecorder.configuration.events.SchemaVersion;
import org.openjdk.jmc.flightrecorder.configuration.internal.EventTypeIDV2;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.XMLModel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfiguration;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfigurationModel;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.rjmx.ConnectionException;
import org.openjdk.jmc.rjmx.ServiceNotAvailableException;
import org.openjdk.jmc.rjmx.services.jfr.FlightRecorderException;
import org.openjdk.jmc.rjmx.services.jfr.IEventTypeInfo;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;
import org.openjdk.jmc.rjmx.test.RjmxTestCase;

@SuppressWarnings("nls")
public class EventConfigurationModelTest extends RjmxTestCase {

	private IFlightRecorderService service;
	private SchemaVersion version;

	protected IFlightRecorderService getFlightRecorderService()
			throws ConnectionException, ServiceNotAvailableException {
		return getConnectionHandle().getServiceOrThrow(IFlightRecorderService.class);
	}

	protected static IEventConfiguration loadConfig(String jfcName) throws Exception {
		InputStream in = EventConfigurationModelTest.class.getResourceAsStream(jfcName);
		XMLModel model = EventConfiguration.createModel(in);
		return new EventConfiguration(model);
	}

	@Before
	public void setUp() throws Exception {
		assumeHotSpot7u12OrLater(getConnectionHandle());
		service = getFlightRecorderService();
		version = SchemaVersion.fromBeanVersion(service.getVersion());
	}

	@Test
	public void testPushServerInfoToXmlModelCategoriesFromJFC() throws Exception {
		Assume.assumeTrue(version.equals(SchemaVersion.V2));
		EventConfiguration config = getEventConfig("categories.jfc", false);

		IEventTypeID threadAllocationStatistics = new EventTypeIDV2(JdkTypeIDs.THREAD_ALLOCATION_STATISTICS);
		assertCategory(new String[] {"Our Special Java Category", "Thread"}, config, threadAllocationStatistics);

		IEventTypeID monitorWait = new EventTypeIDV2(JdkTypeIDs.MONITOR_WAIT);
		assertCategory(new String[] {"Our Special Java Category"}, config, monitorWait);
	}

	// FIXME: These tests should have V1 counterparts.

	@Test
	public void testPushServerInfoToXmlModelConfigValueUnchanged() throws Exception {
		Assume.assumeTrue(version.equals(SchemaVersion.V2));
		EventConfiguration config = getEventConfig("categories.jfc", false);
		assertOptionValue(config, "27 ms", JdkTypeIDs.MONITOR_WAIT, "threshold");
	}

	@Test
	public void testPushServerInfoToXmlModelWithOverrideConfigValueUnchanged() throws Exception {
		Assume.assumeTrue(version.equals(SchemaVersion.V2));
		EventConfiguration config = getEventConfig("categories.jfc", true);
		assertOptionValue(config, "27 ms", JdkTypeIDs.MONITOR_WAIT, "threshold");
	}

	@Test
	public void testPushServerInfoToXmlModelDefaultValueUnchanged() throws Exception {
		Assume.assumeTrue(version.equals(SchemaVersion.V2));
		EventConfiguration config = getEventConfig("categories.jfc", false);
		assertOptionValue(config, "0 ns", JdkTypeIDs.THREAD_SLEEP, "threshold");
	}

	@Test
	public void testPushServerInfoToXmlModelWithOverrideDefaultValueUnchanged() throws Exception {
		Assume.assumeTrue(version.equals(SchemaVersion.V2));
		EventConfiguration config = getEventConfig("categories.jfc", true);
		assertOptionValue(config, "0 ns", JdkTypeIDs.THREAD_SLEEP, "threshold");
	}

	private void assertOptionValue(EventConfiguration config, String valueString, String typeId, String optionKey) {
		IDescribedMap<EventOptionID> options = service.getDefaultEventOptions();
		IConstrainedMap<EventOptionID> eventOptions = config.getEventOptions(options.emptyWithSameConstraints());
		EventOptionID optionID = new EventOptionID(ConfigurationToolkit.createEventTypeID(typeId), optionKey);
		assertEquals(valueString, eventOptions.getPersistableString(optionID));
	}

	@Test
	public void testPushServerInfoToXmlModelServerCategoriesUnchanged() throws Exception {
		Assume.assumeTrue(version.equals(SchemaVersion.V2));

		getEventConfig("categories.jfc", false);
		Map<? extends IEventTypeID, ? extends IEventTypeInfo> serverInfos = service.getEventTypeInfoMapByID();

		IEventTypeID classloadingStatistics = new EventTypeIDV2(JdkTypeIDs.CLASS_LOAD_STATISTICS);
		assertArrayEquals(
				classloadingStatistics.getFullKey() + "did not get the expected category provided from server ",
				new String[] {"Java Application", "Statistics"},
				serverInfos.get(classloadingStatistics).getHierarchicalCategory());

		IEventTypeID activeRecording = new EventTypeIDV2(JdkTypeIDs.RECORDINGS);
		assertArrayEquals(activeRecording.getFullKey() + "did not get the expected category provided from server ",
				new String[] {"Flight Recorder"}, serverInfos.get(activeRecording).getHierarchicalCategory());
	}

	@Test
	public void testPushServerInfoToXmlModelNames() throws Exception {
		Assume.assumeTrue(version.equals(SchemaVersion.V2));

		EventConfiguration config = getEventConfig("descriptions.jfc", false);

		IEventTypeID threadAllocationStatistics = new EventTypeIDV2(JdkTypeIDs.THREAD_ALLOCATION_STATISTICS);
		assertEquals("Did not get the expected label for " + threadAllocationStatistics.getFullKey(),
				"Thread Allocation Statistics Special Label", config.getEventLabel(threadAllocationStatistics));
		IEventTypeID activeRecording = new EventTypeIDV2(JdkTypeIDs.RECORDINGS);
		assertEquals("Did not get the expected label for " + activeRecording.getFullKey(), "Flight Recording",
				config.getEventLabel(activeRecording));
	}

	@Test
	public void testPushServerInfoToXmlModelOptionInfo() throws Exception {
		Assume.assumeTrue(version.equals(SchemaVersion.V2));

		EventConfiguration config = getEventConfig("descriptions.jfc", false);

		assertOptionMetadata(config, JdkTypeIDs.THREAD_SLEEP, "stackTrace", "Stack Trace", "Record stack traces",
				"jdk.jfr.Flag");
		assertOptionMetadata(config, JdkTypeIDs.THREAD_SLEEP, "threshold", "Threshold",
				"Record event with duration above or equal to threshold", "jdk.jfr.Timespan");
	}

	@Test
	public void testPushServerInfoWithOverride() throws Exception {
		Assume.assumeTrue(version.equals(SchemaVersion.V2));

		EventConfiguration config = getEventConfig("descriptions.jfc", true);

		IEventTypeID threadAllocationStatistics = new EventTypeIDV2(JdkTypeIDs.THREAD_ALLOCATION_STATISTICS);
		assertCategory(new String[] {"Java Application", "Statistics"}, config, threadAllocationStatistics);

		IEventTypeID monitorWait = new EventTypeIDV2(JdkTypeIDs.MONITOR_WAIT);
		assertCategory(new String[] {"Java Application"}, config, monitorWait);
	}

	private EventConfiguration getEventConfig(String configFile, boolean override)
			throws Exception, FlightRecorderException {
		EventConfiguration config = (EventConfiguration) loadConfig(configFile);
		EventConfigurationModel model = EventConfigurationModel.pushServerMetadataToLocalConfiguration(config,
				service.getDefaultEventOptions(), service.getEventTypeInfoMapByID(), override);
		return ((EventConfiguration) model.getConfiguration());
	}

	private void assertCategory(String[] expectedCategories, EventConfiguration config, IEventTypeID eventTypeID) {
		assertArrayEquals("Did not get the expected category for " + eventTypeID.getFullKey(), expectedCategories,
				config.getEventCategory(eventTypeID));
	}

	private void assertOptionMetadata(
		EventConfiguration config, String typeID, String option, String expectedName, String expectedDescription,
		String expectedContentType) {
		IEventTypeID eventID = new EventTypeIDV2(typeID);
		String optionLabel = config.getConfigOptionLabel(new EventOptionID(eventID, option));
		String optionDescription = config.getConfigOptionDescription(new EventOptionID(eventID, option));
		String optionContentType = config.getConfigOptionContentType(new EventOptionID(eventID, option));
		String optionKey = eventID.getFullKey() + "#" + option;
		assertEquals("Did not get the expected option label for " + optionKey, expectedName, optionLabel);
		assertEquals("Did not get the expected option description for " + optionKey, expectedDescription,
				optionDescription);
		assertEquals("Did not get the expected option content type for " + optionKey, expectedContentType,
				optionContentType);
	}

	// TODO: Test without the push
	// TODO: Test event descriptions
	// TODO: Test that nothing happens in the V1 case
}
