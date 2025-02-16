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
package org.openjdk.jmc.flightrecorder.testutils.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Streaming, almost zero-allocation, JFR chunk parser implementation. <br>
 * This is an MVP of a chunk parser allowing to stream the JFR events efficiently. The parser
 * notifies its listeners as the data becomes available. Because of this it is possible for the
 * metadata events to come 'out-of-band' (although not very probable) and it is up to the caller to
 * deal with that eventuality. <br>
 * This class is not thread-safe and is intended to be used from a single thread only.
 */
public final class StreamingChunkParser {
	private static final Logger log = LoggerFactory.getLogger(StreamingChunkParser.class);

	/**
	 * Parse the given JFR recording stream.<br>
	 * The parser will process the recording stream and call the provided listener in this order:
	 * <ol>
	 * <li>listener.onRecordingStart()
	 * <li>listener.onChunkStart()
	 * <li>listener.onEvent() | listener.onMetadata()
	 * <li>listener.onChunkEnd()
	 * <li>listener.onRecordingEnd()
	 * </ol>
	 *
	 * @param inputStream
	 *            the JFR recording stream it will be closed when the parsing is over
	 * @param listener
	 *            the parser listener
	 * @throws IOException
	 */
	public void parse(InputStream inputStream, ChunkParserListener listener) throws IOException {
		try (RecordingStream stream = new RecordingStream(inputStream)) {
			parse(stream, listener);
		}
	}

	private void parse(RecordingStream stream, ChunkParserListener listener) throws IOException {
		if (stream.available() == 0) {
			return;
		}
		try {
			listener.onRecordingStart();
			int chunkCounter = 1;
			while (stream.available() > 0) {
				long chunkStartPos = stream.position();
				ChunkHeader header = new ChunkHeader(stream);
				if (!listener.onChunkStart(chunkCounter, header)) {
					log.debug("'onChunkStart' returned false. Skipping metadata and events for chunk {}", chunkCounter);
					stream.skip(header.size - (stream.position() - chunkStartPos));
					listener.onChunkEnd(chunkCounter, true);
					continue;
				}
				long chunkEndPos = chunkStartPos + (int) header.size;
				while (stream.position() < chunkEndPos) {
					long eventStartPos = stream.position();
					stream.mark(20); // max 2 varints ahead
					int eventSize = (int) stream.readVarint();
					if (eventSize > 0) {
						long eventType = stream.readVarint();
						if (eventType == 0) {
							// metadata
							stream.reset(); // roll-back the stream to the event start
							MetadataEvent m = new MetadataEvent(stream);
							if (!listener.onMetadata(m)) {
								log.debug("'onMetadata' returned false. Skipping events for chunk {}", chunkCounter);
								stream.skip(header.size - (stream.position() - chunkStartPos));
								listener.onChunkEnd(chunkCounter, true);
							}
						} else if (eventType == 1) {
							// checkpoint event; skip for now
							stream.skip(eventSize - (stream.position() - eventStartPos));
						} else {
							long currentPos = stream.position();
							if (!listener.onEvent(eventType, stream, eventSize - (currentPos - eventStartPos))) {
								log.debug("'onEvent({}, stream)' returned false. Skipping the rest of the chunk {}",
										eventType, chunkCounter);
								// skip the rest of the chunk
								stream.skip(header.size - (stream.position() - chunkStartPos));
								listener.onChunkEnd(chunkCounter, true);
								continue;
							}
							// always skip any unconsumed event data to get the stream into consistent state
							stream.skip(eventSize - (stream.position() - eventStartPos));
						}
					}
				}
				if (!listener.onChunkEnd(chunkCounter, false)) {
					return;
				}
				chunkCounter++;
			}
		} finally {
			listener.onRecordingEnd();
		}
	}
}
