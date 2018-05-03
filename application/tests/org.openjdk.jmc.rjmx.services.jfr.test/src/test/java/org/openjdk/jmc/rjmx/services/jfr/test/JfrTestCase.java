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

import static org.junit.Assert.assertEquals;
import static org.openjdk.jmc.common.unit.UnitLookup.SECOND;

import java.util.Random;

import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.configuration.events.IEventTypeID;
import org.openjdk.jmc.flightrecorder.configuration.internal.EventTypeIDV1;
import org.openjdk.jmc.flightrecorder.configuration.internal.EventTypeIDV2;
import org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder;
import org.openjdk.jmc.rjmx.ConnectionException;
import org.openjdk.jmc.rjmx.ServiceNotAvailableException;
import org.openjdk.jmc.rjmx.services.jfr.FlightRecorderException;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;
import org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor;
import org.openjdk.jmc.rjmx.test.RjmxTestCase;

@SuppressWarnings("nls")
public abstract class JfrTestCase extends RjmxTestCase {
	public static void assertSame(IQuantity expected, IQuantity actual) {
		if (expected.compareTo(actual) == 0) {
			return;
		}
		failNotEquals(null, expected, actual);
	}

	protected IFlightRecorderService getFlightRecorderService()
			throws ConnectionException, ServiceNotAvailableException {
		return getConnectionHandle().getServiceOrThrow(IFlightRecorderService.class);
	}

	// We can only rely on that id = 0 if we run against a VM with the default recording enabled
	protected IRecordingDescriptor getDefaultRecording()
			throws FlightRecorderException, ConnectionException, ServiceNotAvailableException {
		for (IRecordingDescriptor availableRecording : getFlightRecorderService().getAvailableRecordings()) {
			if (availableRecording.getId().longValue() == 0L) {
				return availableRecording;
			}
		}
		return null;
	}

	protected IRecordingDescriptor getContinuousRecording()
			throws FlightRecorderException, ConnectionException, ServiceNotAvailableException {
		for (IRecordingDescriptor availableRecording : getFlightRecorderService().getAvailableRecordings()) {
			if (availableRecording.isContinuous()) {
				return availableRecording;
			}
		}
		return null;
	}

	protected IRecordingDescriptor startContinuousRecording() throws Exception {
		Random rnd = new Random();
		String name = "test_recording_" + rnd.nextInt() % 4711; //$NON-NLS-1$
		IFlightRecorderService service = getFlightRecorderService();
		IConstrainedMap<String> recordingOptions = new RecordingOptionsBuilder(service).name(name).duration(0L).build();
		IRecordingDescriptor recording = service.start(recordingOptions, service.getDefaultEventOptions());
		System.out.println("Started " + recording.getName()); //$NON-NLS-1$
		IConstrainedMap<String> options = service.getRecordingOptions(recording);
		Object durationOption = options.get(RecordingOptionsBuilder.KEY_DURATION);
		// FIXME: It seems duration may be null for continuous recordings on Java 9 and later. Check with specification.
//		assertNotNull(durationOption);
		if (durationOption != null) {
			assertSame(SECOND.quantity(0), (IQuantity) durationOption);
		}
		assertEquals(IRecordingDescriptor.RecordingState.RUNNING, recording.getState());

		assertEquals(name, recording.getName());
		return recording;
	}

	protected void stopRecording(IRecordingDescriptor recording) throws Exception {
		IFlightRecorderService service = getFlightRecorderService();
		service.stop(recording);
		for (int stopCount = 0; stopCount < 15; stopCount += 1) {
			recording = service.getUpdatedRecordingDescription(recording);
			if (!recording.getState().equals(IRecordingDescriptor.RecordingState.STOPPING)) {
				break;
			}
			Thread.sleep(1000);
		}
		assertEquals(IRecordingDescriptor.RecordingState.STOPPED, recording.getState());
		System.out.println("Stopped " + recording.getName()); //$NON-NLS-1$
		service.close(recording);
		recording = service.getUpdatedRecordingDescription(recording);
		assertNull(recording);
	}

	protected static EventOptionID jvm(String path, String option) {
		return new EventOptionID(jvm(path), option);
	}

	protected static EventOptionID jdk(String path, String option) {
		return new EventOptionID(jdk(path), option);
	}

	protected static EventOptionID jfr_info(String path, String option) {
		return new EventOptionID(jfr_info(path), option);
	}

	protected static EventOptionID v2(String name, String option) {
		return new EventOptionID(v2(name), option);
	}

	protected static IEventTypeID jvm(String path) {
		return new EventTypeIDV1("http://www.oracle.com/hotspot/jvm/", path);
	}

	protected static IEventTypeID jdk(String path) {
		return new EventTypeIDV1("http://www.oracle.com/hotspot/jdk/", path);
	}

	protected static IEventTypeID jfr_info(String path) {
		return new EventTypeIDV1("http://www.oracle.com/hotspot/jfr-info/", path);
	}

	protected static IEventTypeID v2(String name) {
		return new EventTypeIDV2(name);
	}

}
