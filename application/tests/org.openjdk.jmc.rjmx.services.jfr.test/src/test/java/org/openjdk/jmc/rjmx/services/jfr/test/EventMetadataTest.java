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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.openjdk.jmc.common.unit.UnitLookup.MILLISECOND;
import static org.openjdk.jmc.flightrecorder.configuration.internal.KnownEventOptions.KEY_ENABLED;
import static org.openjdk.jmc.flightrecorder.configuration.internal.KnownEventOptions.KEY_PERIOD;
import static org.openjdk.jmc.flightrecorder.configuration.internal.KnownEventOptions.KEY_STACKTRACE;
import static org.openjdk.jmc.flightrecorder.configuration.internal.KnownEventOptions.KEY_THRESHOLD;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.common.unit.IMutableConstrainedMap;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.configuration.events.IEventTypeID;
import org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder;
import org.openjdk.jmc.rjmx.services.jfr.IEventTypeInfo;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;
import org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor;
import org.openjdk.jmc.rjmx.services.jfr.internal.EventTypeMetadataV1;
import org.openjdk.jmc.rjmx.services.jfr.internal.EventTypeMetadataV2;

@SuppressWarnings("nls")
public class EventMetadataTest extends JfrTestCase {
	private static class EventSelection {
		EventOptionID enabledOptionID;
		Object enabled;
		EventOptionID stackTraceOptionID;
		Object stackTrace;
		EventOptionID thresholdOptionID;
		Object threshold;
		EventOptionID requestPeriodOptionID;
		Object requestPeriod;
	}

	@Test
	public void testGetAvailableEventTypes() throws Exception {
		assumeHotSpot7u12OrLater(getConnectionHandle());
		IFlightRecorderService service = getFlightRecorderService();
		Collection<? extends IEventTypeInfo> settings = service.getAvailableEventTypes();
		assertNotNull(settings);
		assertTrue(settings.size() >= 5);
		for (IEventTypeInfo typeInfo : settings) {
			Number id;
			String path;
			// Slightly convoluted due to attempting to reduce IEventTypeInfo to a minimum.
			if (typeInfo instanceof EventTypeMetadataV1) {
				EventTypeMetadataV1 typeMeta = (EventTypeMetadataV1) typeInfo;
				id = typeMeta.getId();
				path = typeMeta.getEventTypeID().getRelativeKey();
			} else {
				EventTypeMetadataV2 typeMeta = (EventTypeMetadataV2) typeInfo;
				id = typeMeta.getId();
				path = typeMeta.getEventTypeID().getRelativeKey();
			}
			assertTrue("Got funky id: " + id, id.longValue() > 0);
			assertNotNull(path);
			assertTrue("Got funky path: " + path, path.length() > 0);
		}
	}

	// FIXME: Change to default event options, as "current" isn't actually available in Java 9 and later
	@Test
	public void testGetCurrentEventSettings() throws Exception {
		assumeHotSpot7u12OrLater(getConnectionHandle());
		IFlightRecorderService service = getFlightRecorderService();
		IConstrainedMap<EventOptionID> settings = service.getCurrentEventTypeSettings();
		validateAllSettings(settings);
	}

	@Test
	public void testGetEventSettingsForRecording() throws Exception {
		assumeHotSpot7u12OrLater(getConnectionHandle());
		IFlightRecorderService service = getFlightRecorderService();
		IRecordingDescriptor recording = startContinuousRecording();
		IConstrainedMap<EventOptionID> settings = service.getEventSettings(getContinuousRecording());
		validateAllSettings(settings);
		stopRecording(recording);
	}

	@Test
	public void testStartRecordingWithModifiedOptions() throws Exception {
		assumeHotSpot7u12OrLater(getConnectionHandle());
		IFlightRecorderService service = getFlightRecorderService();
		IRecordingDescriptor oldRecording = startContinuousRecording();
		IConstrainedMap<EventOptionID> settings = service.getEventSettings(getContinuousRecording());
		EventSelection selection = getEventSelection(settings);
		assertNotNull("enabled option id was null", selection.enabledOptionID);
		assertNotNull("stackTrace option id was null", selection.stackTraceOptionID);
		assertNotNull("threshold option id was null", selection.thresholdOptionID);
		assertNotNull("period option id was null", selection.requestPeriodOptionID);

		// Now tweak the settings for the threshold and request period events...
		IMutableConstrainedMap<EventOptionID> newSettings = settings.mutableCopy();
		newSettings.put(selection.thresholdOptionID, MILLISECOND.quantity(0xAFFEL));
		newSettings.put(selection.requestPeriodOptionID, MILLISECOND.quantity(0xABBAL));

		// Start the new recording...
		RecordingOptionsBuilder roBuilder = new RecordingOptionsBuilder(service).duration(100000);
		IRecordingDescriptor newRecording = service.start(roBuilder.build(), newSettings);

		// Get and validate the new options
		settings = service.getEventSettings(newRecording);
		Object newThreshold = settings.get(selection.thresholdOptionID);
		Object newRequest = settings.get(selection.requestPeriodOptionID);
		assertNotEquals(selection.threshold, newThreshold);
		assertNotEquals(selection.requestPeriod, newRequest);
		assertSame(MILLISECOND.quantity(0xAFFEL), (IQuantity) newThreshold);
		assertSame(MILLISECOND.quantity(0xABBAL), (IQuantity) newRequest);

		// Kill the new recording...
		stopRecording(newRecording);
		// Kill the old recording...
		stopRecording(oldRecording);
	}

	// FIXME: Should also do a testUpdateSettingsOnDefault test method
	@Test
	public void testUpdateSettingsOnContinuous() throws Exception {
		assumeHotSpot7u12OrLater(getConnectionHandle());
		// Check current settings...
		IFlightRecorderService service = getFlightRecorderService();
		IRecordingDescriptor recording = startContinuousRecording();
		IConstrainedMap<EventOptionID> settings = service.getEventSettings(getContinuousRecording());
		EventSelection selection = getEventSelection(settings);
		assertNotNull("enabled option id was null", selection.enabledOptionID);
		assertNotNull("stackTrace option id was null", selection.stackTraceOptionID);
		assertNotNull("threshold option id was null", selection.thresholdOptionID);
		assertNotNull("period option id was null", selection.requestPeriodOptionID);

		// First attempt to invert the enablement on the event supporting enablement...
		IMutableConstrainedMap<EventOptionID> newSettings = settings.mutableCopy();
		newSettings.put(selection.enabledOptionID, invert((Boolean) selection.enabled));
		newSettings.put(selection.stackTraceOptionID, invert((Boolean) selection.stackTrace));
		service.updateEventOptions(getContinuousRecording(), newSettings);

		// Get the updated ones...
		settings = service.getEventSettings(getContinuousRecording());
		Object newEnabled = settings.get(selection.enabledOptionID);
		Object newStacktrace = settings.get(selection.stackTraceOptionID);
		assertNotEquals(selection.enabled, newEnabled);
		assertNotEquals(selection.stackTrace, newStacktrace);
		stopRecording(recording);
	}

	private void assertNotEquals(Object value, Object value2) {
		boolean equal = (value == null) ? (value2 == null) : value.equals(value2);
		assertTrue("expected not equal: <" + value + "> was equal to <" + value2 + '>', !equal);
	}

	private Boolean invert(Boolean value) {
		return Boolean.valueOf(!value.booleanValue());
	}

	private void validateAllSettings(IConstrainedMap<EventOptionID> settings) {
		int max = Integer.MIN_VALUE;
		int min = Integer.MAX_VALUE;
		boolean foundEnabled = false;
		boolean foundRequestPeriod = false;
		boolean foundStacktrace = false;
		boolean foundThreshold = false;

		Map<IEventTypeID, Integer> optionCount = new HashMap<>();

		for (EventOptionID optionID : settings.keySet()) {

			/*
			 * The old style test iterated over event types, but mostly tested options. We retrieve
			 * the event type here to be able to do the tests that actually were on the event type
			 * level. Thus, those tests will be done more than once.
			 */
			IEventTypeID eventTypeID = optionID.getEventTypeID();
			int length = optionCount.merge(eventTypeID, 1, (a, b) -> a + b);

			String optionKey = optionID.getOptionKey();
			if (KEY_ENABLED.equals(optionKey)) {
				foundEnabled = true;
			}
			if (KEY_PERIOD.equals(optionKey)) {
				foundRequestPeriod = true;
			}
			if (KEY_STACKTRACE.equals(optionKey)) {
				foundStacktrace = true;
			}
			if (KEY_THRESHOLD.equals(optionKey)) {
				foundThreshold = true;
			}
			max = Math.max(max, length);
			min = Math.min(min, length);
			// FIXME: The integer ID used in the protocol is no longer exposed to clients and cannot be tested here. Test low level protocol like this somewhere else?
//			assertTrue("Got funky id: " + setting.getMetadata().getId(), setting.getMetadata().getId().longValue() > 0);
			assertTrue("Got funky path: " + eventTypeID.getRelativeKey(), eventTypeID.getRelativeKey().length() > 0);
		}
		// FIXME: This has potential to break in Flight Recorder on Java 9 and later
		assertTrue("Strange, have more options been added in a new release perhaps? Found an event with " + max
				+ " configurable options!", max <= 4);
		assertTrue("Strange, have the options changed in a later release? Found an event with no configurable options!",
				min != 0);
		assertTrue("Found no events with enablement information!", foundEnabled);
		assertTrue("Found no events with stack trace information!", foundStacktrace);
		assertTrue("Found no events with threshold information!", foundThreshold);
		assertTrue("Found no events with request period information!", foundRequestPeriod);
	}

	private EventSelection getEventSelection(IConstrainedMap<EventOptionID> settings) {
		EventSelection sel = new EventSelection();
		for (EventOptionID optionID : settings.keySet()) {
			Object value = settings.get(optionID);
			String optionKey = optionID.getOptionKey();
			if (KEY_ENABLED.equals(optionKey)) {
				sel.enabledOptionID = optionID;
				sel.enabled = value;
			}
			if (KEY_PERIOD.equals(optionKey)) {
				sel.requestPeriodOptionID = optionID;
				sel.requestPeriod = value;
			}
			if (KEY_STACKTRACE.equals(optionKey)) {
				sel.stackTraceOptionID = optionID;
				sel.stackTrace = value;
			}
			if (KEY_THRESHOLD.equals(optionKey)) {
				sel.thresholdOptionID = optionID;
				sel.threshold = value;
			}
		}
		return sel;
	}
}
