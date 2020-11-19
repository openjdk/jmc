/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, Red Hat Inc. All rights reserved.
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

package org.openjdk.jmc.flightrecorder.flameview.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.test.io.IOResourceSet;
import org.openjdk.jmc.common.util.StringToolkit;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.flameview.FlameGraphJSONMarshaller;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;
import org.openjdk.jmc.flightrecorder.test.util.RecordingToolkit;
import org.openjdk.jmc.flightrecorder.test.util.StacktraceTestToolkit;

public class FlameGraphJSONMarshallerTest {

	private static IItemCollection testRecording;

	@BeforeClass
	public static void beforeAll() throws IOException, CouldNotLoadRecordingException {
		IOResourceSet[] testResources = StacktraceTestToolkit.getTestResources();
		IOResourceSet resourceSet = testResources[0];
		testRecording = RecordingToolkit.getFlightRecording(resourceSet);
	}

	private static final FrameSeparator separator = new FrameSeparator(FrameSeparator.FrameCategorization.METHOD,
			false);

	@Test
	public void testRenderedJSONWithAttribute() throws Exception {
		StacktraceTreeModel model = new StacktraceTreeModel(testRecording, separator, true,
				JdkAttributes.ALLOCATION_SIZE);
		String flameGraphJSON = FlameGraphJSONMarshaller.toJSON(model);

		String expectedJSON = readResource("/flamegraph-attribute.json");
		assertEquals(expectedJSON, flameGraphJSON);
	}

	@Test
	public void testRenderedJSONWithCounts() throws Exception {
		StacktraceTreeModel model = new StacktraceTreeModel(testRecording, separator);
		String flameGraphJSON = FlameGraphJSONMarshaller.toJSON(model);

		String expectedJSON = readResource("/flamegraph-counts.json");
		assertEquals(expectedJSON, flameGraphJSON);
	}

	private String readResource(String resourcePath) throws IOException {
		try (InputStream is = FlameGraphJSONMarshallerTest.class.getResourceAsStream(resourcePath)) {
			if (is == null) {
				throw new IllegalArgumentException(resourcePath + " not found");
			}
			return StringToolkit.readString(is);
		}
	}

}
