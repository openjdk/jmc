/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.writer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.item.Attribute;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.writer.api.Recording;
import org.openjdk.jmc.flightrecorder.writer.api.Recordings;
import org.openjdk.jmc.flightrecorder.writer.api.Type;
import org.openjdk.jmc.flightrecorder.writer.api.TypedField;
import org.openjdk.jmc.flightrecorder.writer.api.TypedValue;
import org.openjdk.jmc.flightrecorder.writer.api.Types;

class ChunkComplexTest {
	private Recording recording;
	private Path jfrPath;

	public static final String EVENT_NAME = "sample event";
	public static final String EVENT_MSG = "Hello world";

	@BeforeEach
	void setup() throws Exception {
		jfrPath = Files.createTempFile("jfr-writer-test-", ".jfr");
		recording = Recordings.newRecording(jfrPath);
	}

	@AfterEach
	void teardown() throws Exception {
		recording.close();
		Files.deleteIfExists(jfrPath);
	}

	@Test
	void writeEvent() throws Exception {
		Types types = recording.getTypes();

		TypedField nameField = types.fieldBuilder("name", Types.Builtin.STRING).build();

		Type customSimpleType = recording.registerType("com.datadog.types.Simple", b -> {
			b.addField("message", TypesImpl.Builtin.STRING);
		});

		TypedField messageField = types.fieldBuilder("message", customSimpleType).build();

		Type eventType = recording.registerEventType("dd.SampleEvent", eventTypeBuilder -> {
			eventTypeBuilder.addFields(nameField, messageField);
		});

		TypedValue eventValue = eventType.asValue(access -> {
			access.putField("startTime", System.nanoTime()).putField("name", EVENT_NAME).putField("message", EVENT_MSG)
					.putField("eventThread", threadAccess -> {
						threadAccess.putField("osName", "Java AWT-0").putField("osThreadId", 41953L)
								.putField("javaName", "AWT-0").putField("javaThreadId", 11L)
								.putField("group", groupAcess -> {
									groupAcess.putField("name", "Main AWT Group");
								});
					}).putField("stackTrace", builder -> {
						//noinspection unchecked
						builder.putField("truncated", false).putFields("frames", frame1 -> {
							frame1.putField("type", "Interpreted").putField("method", method -> {
								method.putField("type", classType -> {
									classType.putField("name", "com.datadoghq.test.Main").putField("package", pkg -> {
										pkg.putField("name", "com.datadoghq.test");
									}).putField("modifiers", Modifier.PUBLIC | Modifier.FINAL);
								}).putField("name", "main").putField("descriptor", "([Ljava/lang/String;)V")
										.putField("modifiers", Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL);
							});
						}, frame2 -> {
							frame2.putField("type", "JIT compiled").putField("method", method -> {
								method.putField("type", classType -> {
									classType.putField("name", "com.datadoghq.test.Main").putField("package", pkg -> {
										pkg.putField("name", "com.datadoghq.test");
									}).putField("modifiers", Modifier.PUBLIC | Modifier.FINAL);
								}).putField("name", "doit").putField("descriptor", "(Ljava/lang/String;)V")
										.putField("modifiers", Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL);
							});
						});
					});
		});

		recording.writeEvent(eventValue).rotateChunk().writeEvent(eventValue).close();

		// sanity check to make sure the recording is loaded
		IItemCollection events = JfrLoaderToolkit.loadEvents(jfrPath.toFile());

		IAttribute<String> nameAttr = Attribute.attr("name", "name", UnitLookup.PLAIN_TEXT);
		IAttribute<String> msgAttr = Attribute.attr("message", "message", UnitLookup.PLAIN_TEXT);
		assertTrue(events.hasItems());
		int[] eventCount = new int[] {0};
		events.forEach(iitem -> {
			IMemberAccessor<IMCStackTrace, IItem> stackTraceAccessor = JfrAttributes.EVENT_STACKTRACE
					.getAccessor(iitem.getType());
			IMemberAccessor<String, IItem> nameAcessor = nameAttr.getAccessor(iitem.getType());
			IMemberAccessor<String, IItem> msgAcessor = msgAttr.getAccessor(iitem.getType());
			IMemberAccessor<IMCThread, IItem> eventThreadAccessor = JfrAttributes.EVENT_THREAD
					.getAccessor(iitem.getType());
			IMemberAccessor<IQuantity, IItem> startTimeAccessor = JfrAttributes.START_TIME.getAccessor(iitem.getType());
			iitem.forEach(item -> {
				eventCount[0]++;
				assertEquals(EVENT_NAME, nameAcessor.getMember(item));
				assertEquals(EVENT_MSG, msgAcessor.getMember(item));
				assertNotNull(eventThreadAccessor.getMember(item));
				IMCStackTrace stackTrace = stackTraceAccessor.getMember(item);
				assertEquals(2, stackTrace.getFrames().size());

				System.out.println(startTimeAccessor.getMember(item).interactiveFormat());
			});
		});
		assertEquals(2, eventCount[0]);
	}
}
