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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.openjdk.jmc.common.unit.UnitLookup.MILLISECOND;
import static org.openjdk.jmc.common.unit.UnitLookup.SECOND;

import java.util.Date;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;
import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.common.unit.IMutableConstrainedMap;
import org.openjdk.jmc.common.unit.IOptionDescriptor;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.configuration.internal.ValidationToolkit;
import org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;
import org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor;

@SuppressWarnings("nls")
public class RecordingOptionsTest extends JfrTestCase {

	@Test
	public void testGetAvailableOptions() throws Exception {
		assumeHotSpot7u12OrLater(getConnectionHandle());

		IFlightRecorderService service = getFlightRecorderService();
		Map<String, IOptionDescriptor<?>> data = service.getAvailableRecordingOptions();
		assertNotNull(data);
	}

	@Test
	public void testGetOptionsFromRecording() throws Exception {
		assumeHotSpot7u12OrLater(getConnectionHandle());

		IFlightRecorderService service = getFlightRecorderService();
		IRecordingDescriptor recording = startContinuousRecording();
		IConstrainedMap<String> data = service.getRecordingOptions(getContinuousRecording());
		assertNotNull(data);
		stopRecording(recording);
	}

	@Test
	public void testValidateOption() throws Exception {
		RecordingOptionsBuilder builder = new RecordingOptionsBuilder(getFlightRecorderService());
		builder.addByKey(RecordingOptionsBuilder.KEY_DURATION, "20s"); //$NON-NLS-1$
		IConstrainedMap<String> options = builder.build();
		try {
			ValidationToolkit.validate(options);
		} catch (Exception e) {
			throw e;
		}
	}

	@Test
	public void testValidateInvalidOption() throws Exception {
		IFlightRecorderService service = getFlightRecorderService();
		IMutableConstrainedMap<String> options = service.getDefaultRecordingOptions().emptyWithSameConstraints();
		try {
			// Not allowed to put string representations directly in options map anymore.
			options.put(RecordingOptionsBuilder.KEY_DURATION, "20s"); //$NON-NLS-1$
			fail("Expected exception to be thrown for invalid options");
		} catch (Exception expected) {
		}
		ValidationToolkit.validate(options);
	}

	@Test
	public void testStringOptions() throws Exception {
		RecordingOptionsBuilder builder = new RecordingOptionsBuilder(getFlightRecorderService());
		builder.addByKey(RecordingOptionsBuilder.KEY_DURATION, "20s"); //$NON-NLS-1$
		IConstrainedMap<String> map = builder.build();
		assertSame(SECOND.quantity(20), (IQuantity) map.get(RecordingOptionsBuilder.KEY_DURATION));
	}

	@Test
	public void testCreateOptionsMapFromStrings() throws Exception {
		Properties props = new Properties();
		IFlightRecorderService service = getFlightRecorderService();
		RecordingOptionsBuilder builder = new RecordingOptionsBuilder(service);
		boolean hasCompressed = "1.0".equals(service.getVersion());
		props.put(RecordingOptionsBuilder.KEY_DURATION, "45 ms"); //$NON-NLS-1$
		Date myDate = new Date();
		if (hasCompressed) {
			props.put(RecordingOptionsBuilder.KEY_DESTINATION_FILE, "myfile"); //$NON-NLS-1$
			props.put(RecordingOptionsBuilder.KEY_DESTINATION_COMPRESSED, "true"); //$NON-NLS-1$
			// FIXME: Do we really want to support parsing Date.toString() format?
//			props.put(RecordingOptionsBuilder.KEY_START_TIME, myDate.toString());
			props.put(RecordingOptionsBuilder.KEY_START_TIME, Long.toString(myDate.getTime()));
		}
		IConstrainedMap<String> options = builder.fromProperties(props).build();
		assertSame(MILLISECOND.quantity(45), (IQuantity) options.get(RecordingOptionsBuilder.KEY_DURATION));
		if (hasCompressed) {
			assertEquals("myfile", options.get(RecordingOptionsBuilder.KEY_DESTINATION_FILE)); //$NON-NLS-1$
			assertEquals(Boolean.TRUE, options.get(RecordingOptionsBuilder.KEY_DESTINATION_COMPRESSED));
			assertSame(UnitLookup.fromDate(myDate), (IQuantity) options.get(RecordingOptionsBuilder.KEY_START_TIME));
		}
	}
}
