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

import static org.openjdk.jmc.common.unit.UnitLookup.MILLISECOND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.util.Random;

import org.junit.Test;

import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;
import org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor;

public class JfrControlTest extends JfrTestCase {
	@Test
	public void testGetAvailableRecordings() throws Exception {
		assumeHotSpot7u12OrLater(getConnectionHandle());

		IFlightRecorderService service = getFlightRecorderService();
		assertNotNull(service);
		assertNotNull(service.getAvailableRecordings());
	}

	@Test
	public void testGetContinuousRecording() throws Exception {
		assumeHotSpot7u12OrLater(getConnectionHandle());
		IRecordingDescriptor recording = startContinuousRecording();
		assertNotNull(getContinuousRecording());
		stopRecording(recording);

	}

	/**
	 * Also tests stop, close and getUpdatedRecordingDescriptor...
	 *
	 * @throws Exception
	 */
	@Test
	public void testStartContinuousRecording() throws Exception {
		assumeHotSpot7u12OrLater(getConnectionHandle());

		IRecordingDescriptor recording = startContinuousRecording();
		stopRecording(recording);
	}

	/**
	 * Also tests stop, close and getUpdatedRecordingDescriptor...
	 *
	 * @throws Exception
	 */
	@Test
	public void testStartTimedRecording() throws Exception {
		assumeHotSpot7u12OrLater(getConnectionHandle());

		IQuantity duration = MILLISECOND.quantity(5000);
		Random rnd = new Random();
		String name = "test_recording_" + rnd.nextInt() % 4711; //$NON-NLS-1$
		IFlightRecorderService service = getFlightRecorderService();
		IConstrainedMap<String> recordingOptions = new RecordingOptionsBuilder(service).name(name).duration(duration)
				.build();
		IRecordingDescriptor recording = service.start(recordingOptions, null);
		IConstrainedMap<String> options = service.getRecordingOptions(recording);
		Object durationOption = options.get(RecordingOptionsBuilder.KEY_DURATION);
		assertNotNull(durationOption);
		assertSame(duration, (IQuantity) durationOption);
		assertEquals(IRecordingDescriptor.RecordingState.RUNNING, recording.getState());
		System.out.println("Started " + recording.getName()); //$NON-NLS-1$
		assertEquals(name, recording.getName());

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

	@Test
	public void testGetEventTypeSettings() throws Exception {
		assumeHotSpot7u12OrLater(getConnectionHandle());

		IFlightRecorderService service = getFlightRecorderService();
		IConstrainedMap<EventOptionID> settings = service.getDefaultEventOptions();
		assertNotNull(settings);
	}

	@Test
	public void testDumpContinuous() throws Exception {
		assumeHotSpot7u12OrLater(getConnectionHandle());

		byte[] bytes = new byte[4096];
		int read = 0;
		IFlightRecorderService service = getFlightRecorderService();
		IRecordingDescriptor recording = startContinuousRecording();
		IRecordingDescriptor descriptor = getContinuousRecording();
		InputStream stream = service.openStream(descriptor, false);
		int lastRead = -1;
		while ((lastRead = stream.read(bytes)) != -1) {
			read += lastRead;
		}
		assertMin("Should have read something!", 1, read); //$NON-NLS-1$
		stream.close();
		stopRecording(recording);
	}
}
