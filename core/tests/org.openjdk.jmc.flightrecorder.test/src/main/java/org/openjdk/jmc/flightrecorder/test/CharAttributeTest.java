/*
 * Copyright (c) 2022, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2022, 2025, Red Hat, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.test.util.PrintoutsToolkit;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Name;
import jdk.jfr.Recording;

/**
 * Test for making sure events with {@code char} attributes can be parsed.
 */
public class CharAttributeTest {

	@Test
	public void shouldParseEventWithCharAttribute()
			throws IOException, CouldNotLoadRecordingException, URISyntaxException {
		File recordingFile = new File(
				CharAttributeTest.class.getClassLoader().getResource("recordings/char_attribute.jfr").toURI());
		IItemCollection items = JfrLoaderToolkit.loadEvents(Arrays.asList(recordingFile))
				.apply(ItemFilters.type("jmc.CharTestEvent"));

		List<String> eventsAsStrings = PrintoutsToolkit.getEventsAsStrings(items);
		Assert.assertEquals(1, eventsAsStrings.size());
		Assert.assertTrue("Actual events: " + eventsAsStrings,
				eventsAsStrings.get(0).contains("<someChar name=\"someChar\">X</someChar>"));
	}

	/**
	 * Run to create/updated the test recording, if needed.
	 */
	public static void main(String[] args) throws Exception {
		try (Recording recording = new Recording()) {

			recording.enable("jmc.CharTestEvent");
			Path destination = Paths
					.get(CharAttributeTest.class.getProtectionDomain().getCodeSource().getLocation().toURI())
					.getParent().getParent().resolve("src").resolve("test").resolve("resources").resolve("recordings")
					.resolve("char_attribute.jfr");
			recording.setDestination(destination);

			recording.start();

			CharTestEvent event = new CharTestEvent();
			event.someChar = 'X';
			event.commit();

			recording.stop();
		}
	}

	@Name("jmc.CharTestEvent")
	@Category("Testing")
	public static class CharTestEvent extends Event {

		public char someChar;
	}
}
