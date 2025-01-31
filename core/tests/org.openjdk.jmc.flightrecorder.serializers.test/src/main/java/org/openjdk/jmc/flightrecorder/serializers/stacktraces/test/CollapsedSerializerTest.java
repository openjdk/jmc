/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, 2025, Datadog, Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.flightrecorder.serializers.stacktraces.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.serializers.json.test.FlameGraphJsonSerializerTest;
import org.openjdk.jmc.flightrecorder.serializers.stacktraces.CollapsedSerializer;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;
import org.openjdk.jmc.flightrecorder.test.util.RecordingToolkit;
import org.openjdk.jmc.test.TestToolkit;

public class CollapsedSerializerTest {
	@Test
	public void testSerializeKnownRecording() throws IOException, CouldNotLoadRecordingException {
		IItemCollection collection = RecordingToolkit.getFlightRecording(
				TestToolkit.getNamedResource(FlameGraphJsonSerializerTest.class, "recordings", "hotmethods.jfr"));
		assertNotNull(collection);
		String collapsed = CollapsedSerializer.toCollapsed(new StacktraceTreeModel(collection,
				new FrameSeparator(FrameSeparator.FrameCategorization.METHOD, false)));
		String[] lines = collapsed.split("\n");
		assertEquals(15, lines.length);
		assertEquals(
				"Thread.run();Worker.run();HolderOfUniqueValues.initialize(int);LinkedList.add(Object);LinkedList.linkLast(Object) 2068",
				lines[0]);
		assertEquals("Thread.run();Worker.run();HolderOfUniqueValues.initialize(int);Integer.valueOf(int) 1536",
				lines[1]);
		assertEquals(
				"HotMethods.main(String[]);BufferedInputStream.read();BufferedInputStream.fill();FileInputStream.read(byte[], int, int);FileInputStream.readBytes(byte[], int, int) 4906",
				lines[14]);
	}

}
