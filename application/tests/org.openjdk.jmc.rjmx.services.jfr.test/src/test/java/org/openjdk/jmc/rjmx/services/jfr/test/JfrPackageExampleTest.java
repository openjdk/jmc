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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.common.unit.IDescribedMap;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder;
import org.openjdk.jmc.rjmx.ConnectionDescriptorBuilder;
import org.openjdk.jmc.rjmx.IConnectionDescriptor;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.IServerHandle;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;
import org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor;
import org.openjdk.jmc.rjmx.test.RjmxTestCase;

/**
 * This test suite is supposed to test the example code that we ship with the documentation for the
 * org.openjdk.jmc.rjmx.services.jfr bundle. That is, for each code example included in
 * org.openjdk.jmc.rjmx.services.jfr/src/org/openjdk/jmc/rjmx/services/jfr/package-info.java, there
 * should be a test method in here with a verbatim copy of that code.
 */
// NOTE: If you change the verbatim test YOU MUST update the corresponding package-info.java.
@SuppressWarnings("nls")
public class JfrPackageExampleTest extends RjmxTestCase {

	@Test
	public void testPackageExample1FunctionalityVerbatim() throws Exception {
		IConnectionDescriptor descriptor = new ConnectionDescriptorBuilder().hostName("localhost").port(0).build(); //$NON-NLS-1$
		IServerHandle serverHandle = IServerHandle.create(descriptor);
		IConnectionHandle handle = serverHandle.connect("Get JFR recording info"); //$NON-NLS-1$
		try {
			IFlightRecorderService jfr = handle.getServiceOrThrow(IFlightRecorderService.class);
			for (IRecordingDescriptor desc : jfr.getAvailableRecordings()) {
				System.out.println(desc.getName());
			}
		} finally {
			IOToolkit.closeSilently(handle);
		}
	}

	@Test
	public void testPackageExample2FunctionalityVerbatim() throws Exception {
		IConnectionDescriptor descriptor = new ConnectionDescriptorBuilder().hostName("localhost").port(0).build(); //$NON-NLS-1$
		IServerHandle serverHandle = IServerHandle.create(descriptor);
		IConnectionHandle handle = serverHandle.connect("Start time bound flight recording");
		try {
			IFlightRecorderService jfr = handle.getServiceOrThrow(IFlightRecorderService.class);

			long duration = 5000;
			IDescribedMap<EventOptionID> defaultEventOptions = jfr.getDefaultEventOptions();
			IConstrainedMap<String> recordingOptions = new RecordingOptionsBuilder(jfr).name("MyRecording")
					.duration(duration).build();
			IRecordingDescriptor recording = jfr.start(recordingOptions, defaultEventOptions);
			Thread.sleep(duration);
			while (recording.getState() != IRecordingDescriptor.RecordingState.STOPPED) {
				Thread.sleep(1000);
				recording = jfr.getUpdatedRecordingDescription(recording);
			}
			InputStream is = jfr.openStream(recording, true);
			writeStreamToFile(is);
		} finally {
			IOToolkit.closeSilently(handle);
		}
	}

	private void writeStreamToFile(InputStream in) throws IOException {
		// Just read and count bytes in the test...
		int count = 0;
		try {
			byte[] buf = new byte[1024 * 1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				count += len;
			}
		} finally {
			in.close();
		}
		assertTrue(count > 0);
	}
}
