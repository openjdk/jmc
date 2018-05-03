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
package org.openjdk.jmc.flightrecorder.test;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.test.io.IOResourceSet;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.test.util.PrintoutsToolkit;
import org.openjdk.jmc.flightrecorder.test.util.RecordingToolkit;

/**
 * Regression test that verifies that the output from the flight recorder parser hasn't changed. The
 * output is verified against a printout from a previous checked in version of the parser.
 */
@SuppressWarnings("nls")
public class RecordingTest {
	/**
	 * Test that verifies that a the printout from the parser hasn't changed.
	 */

	@Test
	public void testRecordings() throws IOException, CouldNotLoadRecordingException {
		for (IOResourceSet resourceSet : PrintoutsToolkit.getTestResources()) {
			IItemCollection items = RecordingToolkit.getFlightRecording(resourceSet);
			List<String> parsedEvents = PrintoutsToolkit.getEventsAsStrings(items);
			try {
				List<String> expectedEvents = PrintoutsToolkit.getEventsFromPrintout(resourceSet);

				Assert.assertEquals(expectedEvents.size(), parsedEvents.size());

				for (int i = 0; i < expectedEvents.size(); i++) {
					Assert.assertEquals(resourceSet.getResource(0).getName() + ": events did not match expected",
							expectedEvents.get(i), parsedEvents.get(i));
				}
			} catch (Exception e) {
				Assert.fail(resourceSet.getResource(0).getName() + ": Could not read baseline file: " + e.getMessage());
			}
		}
	}
}
