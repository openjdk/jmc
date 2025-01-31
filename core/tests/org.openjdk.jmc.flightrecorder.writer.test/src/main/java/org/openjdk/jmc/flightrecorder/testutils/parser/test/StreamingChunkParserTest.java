/*
 * Copyright (c) 2022, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2022, 2025, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.testutils.parser.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openjdk.jmc.flightrecorder.testutils.parser.ChunkHeader;
import org.openjdk.jmc.flightrecorder.testutils.parser.ChunkParserListener;
import org.openjdk.jmc.flightrecorder.testutils.parser.MetadataEvent;
import org.openjdk.jmc.flightrecorder.testutils.parser.RecordingStream;
import org.openjdk.jmc.flightrecorder.testutils.parser.StreamingChunkParser;
import org.openjdk.jmc.flightrecorder.writer.api.Recording;
import org.openjdk.jmc.flightrecorder.writer.api.Recordings;
import org.openjdk.jmc.flightrecorder.writer.api.Type;
import org.openjdk.jmc.flightrecorder.writer.api.Types;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class StreamingChunkParserTest {
	private StreamingChunkParser instance;
	@Mock
	private ChunkParserListener listener;

	@BeforeEach
	void setup() throws Exception {
		instance = new StreamingChunkParser();
	}

	@Test
	void testNoChunk() throws Exception {
		byte[] data = new byte[0];
		InputStream is = new ByteArrayInputStream(data);

		instance.parse(is, listener);

		ChunkParserListener noInteractions = Mockito.verify(listener, VerificationModeFactory.times(0));
		noInteractions.onRecordingStart();
		noInteractions.onChunkStart(Mockito.anyInt(), Mockito.any(ChunkHeader.class));
		noInteractions.onMetadata(Mockito.any(MetadataEvent.class));
		noInteractions.onEvent(Mockito.anyLong(), Mockito.any(RecordingStream.class), Mockito.anyInt());
		noInteractions.onChunkEnd(Mockito.anyInt(), Mockito.anyBoolean());
		noInteractions.onRecordingEnd();
	}

	@Test
	void testNoMagic() throws Exception {
		byte[] data = new byte[100];
		for (int i = 0; i < 100; i++) {
			data[i] = (byte) i;
		}

		InputStream is = new ByteArrayInputStream(data);

		Assertions.assertThrows(IOException.class, () -> instance.parse(is, listener));

		Mockito.verify(listener, VerificationModeFactory.times(1)).onRecordingStart();
		Mockito.verify(listener, VerificationModeFactory.times(1)).onRecordingEnd();
		Mockito.verify(listener, VerificationModeFactory.times(0)).onChunkStart(Mockito.anyInt(),
				Mockito.any(ChunkHeader.class));
		Mockito.verify(listener, VerificationModeFactory.times(0)).onChunkEnd(Mockito.anyInt(), Mockito.anyBoolean());
		Mockito.verify(listener, VerificationModeFactory.times(0)).onMetadata(Mockito.any(MetadataEvent.class));
		Mockito.verify(listener, VerificationModeFactory.times(0)).onEvent(Mockito.anyLong(),
				Mockito.any(RecordingStream.class), Mockito.anyLong());
	}

	@Test
	void testOnlyMagic() throws Exception {
		InputStream is = new ByteArrayInputStream(ChunkHeader.MAGIC);

		Assertions.assertThrows(IOException.class, () -> instance.parse(is, listener));

		Mockito.verify(listener, VerificationModeFactory.times(1)).onRecordingStart();
		Mockito.verify(listener, VerificationModeFactory.times(1)).onRecordingEnd();
		Mockito.verify(listener, VerificationModeFactory.times(0)).onChunkStart(Mockito.anyInt(),
				Mockito.any(ChunkHeader.class));
		Mockito.verify(listener, VerificationModeFactory.times(0)).onChunkEnd(Mockito.anyInt(), Mockito.anyBoolean());
		Mockito.verify(listener, VerificationModeFactory.times(0)).onMetadata(Mockito.any(MetadataEvent.class));
		Mockito.verify(listener, VerificationModeFactory.times(0)).onEvent(Mockito.anyLong(),
				Mockito.any(RecordingStream.class), Mockito.anyLong());
	}

	@Test
	void testSingleChunkRecording() throws Exception {
		ByteArrayOutputStream recordingStream = new ByteArrayOutputStream();
		long eventTypeId = -1;
		try (Recording recording = Recordings.newRecording(recordingStream)) {
			Type type = recording.registerEventType("datadog.TestEvent", t -> {
				t.addField("value", Types.Builtin.INT);
			});
			eventTypeId = type.getId();
			recording.writeEvent(type.asValue(b -> {
				b.putField("startTime", 10L).putField("eventThread", recording.getType(Types.JDK.THREAD).nullValue())
						.putField("stackTrace", recording.getType(Types.JDK.STACK_TRACE).nullValue())
						.putField("value", 10);
			}));
		}

		assertNotEquals(-1, eventTypeId);

		Mockito.when(listener.onChunkStart(Mockito.anyInt(), Mockito.any(ChunkHeader.class))).thenReturn(true);
		Mockito.when(listener.onMetadata(Mockito.any(MetadataEvent.class))).thenReturn(true);
		Mockito.when(listener.onEvent(Mockito.anyLong(), Mockito.any(RecordingStream.class), Mockito.anyLong()))
				.thenReturn(true);
		Mockito.when(listener.onChunkEnd(Mockito.anyInt(), Mockito.anyBoolean())).thenReturn(true);

		InputStream is = new ByteArrayInputStream(recordingStream.toByteArray());
		instance.parse(is, listener);

		Mockito.verify(listener, VerificationModeFactory.times(1)).onRecordingStart();
		Mockito.verify(listener, VerificationModeFactory.times(1)).onRecordingEnd();
		Mockito.verify(listener, VerificationModeFactory.times(1)).onChunkStart(Mockito.eq(1), Mockito.notNull());
		Mockito.verify(listener, VerificationModeFactory.times(1)).onChunkEnd(Mockito.eq(1), Mockito.eq(false));
		Mockito.verify(listener, VerificationModeFactory.times(1)).onMetadata(Mockito.notNull());
		ArgumentCaptor<Long> capturedSize = ArgumentCaptor.forClass(Long.class);

		Mockito.verify(listener, VerificationModeFactory.times(1)).onEvent(Mockito.eq(eventTypeId), Mockito.notNull(),
				capturedSize.capture());

		assertNotNull(capturedSize.getValue());
		assertTrue(capturedSize.getValue() > 0);
	}

	@ParameterizedTest
	@MethodSource("cancelledParserVerification")
	void testCancellation(
		String cancelAt, int numRecStart, int numChunkStart, int numEvent, int numMetadata, int numChunkEnd,
		int numRecEnd) throws Exception {
		long eventTypeId1 = -1;
		long eventTypeId2 = -1;
		ByteArrayOutputStream recordingStream = new ByteArrayOutputStream();
		try (Recording recording = Recordings.newRecording(recordingStream)) {
			Type type = recording.registerEventType("datadog.TestEvent", t -> {
				t.addField("value", Types.Builtin.INT);
			});
			eventTypeId1 = type.getId();
			recording.writeEvent(type.asValue(b -> {
				b.putField("startTime", 10L).putField("eventThread", recording.getType(Types.JDK.THREAD).nullValue())
						.putField("stackTrace", recording.getType(Types.JDK.STACK_TRACE).nullValue())
						.putField("value", 10);
			}));
			recording.writeEvent(type.asValue(b -> {
				b.putField("startTime", 20L).putField("eventThread", recording.getType(Types.JDK.THREAD).nullValue())
						.putField("stackTrace", recording.getType(Types.JDK.STACK_TRACE).nullValue())
						.putField("value", 20);
			}));
		}
		try (Recording recording = Recordings.newRecording(recordingStream)) {
			Type type = recording.registerEventType("datadog.TestEvent", t -> {
				t.addField("value", Types.Builtin.INT);
			});
			eventTypeId2 = type.getId();
			recording.writeEvent(type.asValue(b -> {
				b.putField("startTime", 30L).putField("eventThread", recording.getType(Types.JDK.THREAD).nullValue())
						.putField("stackTrace", recording.getType(Types.JDK.STACK_TRACE).nullValue())
						.putField("value", 30);
			}));
			recording.writeEvent(type.asValue(b -> {
				b.putField("startTime", 40L).putField("eventThread", recording.getType(Types.JDK.THREAD).nullValue())
						.putField("stackTrace", recording.getType(Types.JDK.STACK_TRACE).nullValue())
						.putField("value", 40);
			}));
		}

		assertNotEquals(-1, eventTypeId1);
		assertNotEquals(-1, eventTypeId2);

		Mockito.lenient().when(listener.onChunkStart(Mockito.anyInt(), Mockito.any(ChunkHeader.class)))
				.thenReturn(!"chunkStart".equals(cancelAt));
		Mockito.lenient().when(listener.onMetadata(Mockito.any(MetadataEvent.class)))
				.thenReturn(!"metadata".equals(cancelAt));
		Mockito.lenient()
				.when(listener.onEvent(Mockito.anyLong(), Mockito.any(RecordingStream.class), Mockito.anyLong()))
				.thenReturn(!"event".equals(cancelAt));
		Mockito.lenient().when(listener.onChunkEnd(Mockito.anyInt(), Mockito.anyBoolean()))
				.thenReturn(!"chunkEnd".equals(cancelAt));

		InputStream is = new ByteArrayInputStream(recordingStream.toByteArray());
		instance.parse(is, listener);

		boolean interruptedChunk = !"chunkEnd".equals(cancelAt);
		Mockito.verify(listener, VerificationModeFactory.times(numRecStart)).onRecordingStart();
		Mockito.verify(listener, VerificationModeFactory.times(numRecEnd)).onRecordingEnd();
		Mockito.verify(listener, VerificationModeFactory.times(numChunkStart)).onChunkStart(Mockito.anyInt(),
				Mockito.notNull());
		Mockito.verify(listener, VerificationModeFactory.times(numChunkEnd)).onChunkEnd(Mockito.anyInt(),
				Mockito.eq(interruptedChunk));
		Mockito.verify(listener, VerificationModeFactory.times(numMetadata)).onMetadata(Mockito.notNull());
		Mockito.verify(listener, VerificationModeFactory.times(numEvent)).onEvent(Mockito.eq(eventTypeId1),
				Mockito.notNull(), Mockito.anyLong());
	}

	private static Stream<Arguments> cancelledParserVerification() {
		return Stream.of(
				//                   cancelAt     rec     chunk   events  meta  endchunk  endrec
				Arguments.arguments("chunkStart", 1, 2, 0, 0, 2, 1), Arguments.arguments("metadata", 1, 2, 4, 2, 2, 1),
				Arguments.arguments("event", 1, 2, 2, 0, 2, 1), Arguments.arguments("chunkEnd", 1, 1, 2, 1, 1, 1));
	}
}
