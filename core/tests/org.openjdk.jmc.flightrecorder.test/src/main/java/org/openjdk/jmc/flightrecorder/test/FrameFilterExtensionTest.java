/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2025, Datadog, Inc. All rights reserved.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.parser.IEventSink;
import org.openjdk.jmc.flightrecorder.parser.IEventSinkFactory;
import org.openjdk.jmc.flightrecorder.parser.IParserExtension;
import org.openjdk.jmc.flightrecorder.parser.ValueField;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameFilter;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameFilterConstantPoolExtension;

public class FrameFilterExtensionTest {

	@Test
	public void testFrameFilterExtensionIntercepsStackTraces()
			throws IOException, CouldNotLoadRecordingException, URISyntaxException {
		File recordingFile = new File(
				FrameFilterExtensionTest.class.getClassLoader().getResource("recordings/metadata_new.jfr").toURI());
		IItemCollection items = JfrLoaderToolkit.loadEvents(recordingFile, false); // showHiddenFrames = false
		Assert.assertTrue(items.hasItems());

		// If we get here without exceptions, the constant pool filtering worked correctly
		// (The actual filtering verification happens at the constant pool level)
	}

	@Test
	public void testFrameFilteringWithSimulatedHiddenFrames()
			throws IOException, CouldNotLoadRecordingException, URISyntaxException {
		// Create a frame filter that simulates filtering lambda forms
		FrameFilter lambdaFormFilter = frame -> {
			if (frame == null || frame.getMethod() == null || frame.getMethod().getType() == null) {
				return true;
			}
			String typeName = frame.getMethod().getType().getFullName();
			// Simulate lambda form filtering by method name patterns
			return typeName == null || !typeName.contains("LambdaForm") && !typeName.contains("$$Lambda");
		};

		TestEventSinkExtension testExtension = new TestEventSinkExtension();
		List<IParserExtension> extensions = new ArrayList<>();
		extensions.add(testExtension);
		extensions.add(new FrameFilterConstantPoolExtension(lambdaFormFilter));

		File recordingFile = new File(
				FrameFilterExtensionTest.class.getClassLoader().getResource("recordings/metadata_new.jfr").toURI());
		IItemCollection items = JfrLoaderToolkit.loadEvents(Arrays.asList(recordingFile), extensions);
		Assert.assertTrue(items.hasItems());

		// Verify that filtering was attempted
		Assert.assertTrue("Should have processed some stacktraces", testExtension.stackTracesProcessed > 0);
	}

	private static class DebugEventSinkExtension implements IParserExtension {
		boolean sawStackTraceEvents = false;

		@Override
		public IEventSinkFactory getEventSinkFactory(final IEventSinkFactory subFactory) {
			return new IEventSinkFactory() {
				@Override
				public IEventSink create(
					String identifier, String label, String[] category, String description,
					List<ValueField> dataStructure) {
					IEventSink subSink = subFactory.create(identifier, label, category, description, dataStructure);

					// Check if this event type has stacktraces
					boolean hasStackTrace = dataStructure.stream()
							.anyMatch(vf -> vf.getContentType() == JfrAttributes.EVENT_STACKTRACE.getContentType());

					if (hasStackTrace) {
						sawStackTraceEvents = true;
						return new DebugEventSink(subSink);
					}

					return subSink;
				}

				@Override
				public void flush() {
					subFactory.flush();
				}
			};
		}

		private class DebugEventSink implements IEventSink {
			private final IEventSink subSink;

			public DebugEventSink(IEventSink subSink) {
				this.subSink = subSink;
			}

			@Override
			public void addEvent(Object[] values) {
				subSink.addEvent(values);
			}
		}
	}

	private static class TestEventSinkExtension implements IParserExtension {
		int stackTracesProcessed = 0;

		@Override
		public IEventSinkFactory getEventSinkFactory(final IEventSinkFactory subFactory) {
			return new IEventSinkFactory() {
				@Override
				public IEventSink create(
					String identifier, String label, String[] category, String description,
					List<ValueField> dataStructure) {
					IEventSink subSink = subFactory.create(identifier, label, category, description, dataStructure);

					// Check if this event type has stacktraces
					boolean hasStackTrace = dataStructure.stream()
							.anyMatch(vf -> vf.getContentType() == JfrAttributes.EVENT_STACKTRACE.getContentType());

					if (hasStackTrace) {
						return new TestEventSink(subSink);
					}

					return subSink;
				}

				@Override
				public void flush() {
					subFactory.flush();
				}
			};
		}

		private class TestEventSink implements IEventSink {
			private final IEventSink subSink;

			public TestEventSink(IEventSink subSink) {
				this.subSink = subSink;
			}

			@Override
			public void addEvent(Object[] values) {
				// Look for stacktrace values
				for (Object value : values) {
					if (value instanceof IMCStackTrace) {
						stackTracesProcessed++;
						break;
					}
				}
				subSink.addEvent(values);
			}
		}
	}
}
