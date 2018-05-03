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
package org.openjdk.jmc.flightrecorder.internal.parser.v0;

import java.io.IOException;

import org.openjdk.jmc.common.unit.QuantityRange;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.internal.ChunkInfo;
import org.openjdk.jmc.flightrecorder.internal.IChunkLoader;
import org.openjdk.jmc.flightrecorder.internal.parser.Chunk;
import org.openjdk.jmc.flightrecorder.internal.parser.LoaderContext;

public class ChunkLoaderV0 implements IChunkLoader {
	private final ChunkStructure structure;
	private final byte[] data;
	private final LoaderContext context;
	private final ChunkMetadata metadata;

	private ChunkLoaderV0(ChunkStructure structure, byte[] data, LoaderContext context)
			throws CouldNotLoadRecordingException {
		this.structure = structure;
		this.data = data;
		this.context = context;
		// Read metadata
		metadata = new ChunkMetadata(data, structure.getMetadataOffset());
	}

	@Override
	public byte[] call() throws Exception {
		// Read constants
		ReaderFactory readerFactory = new ReaderFactory(metadata, data, context);

		// Read events
		EventParserManager eventParser = new EventParserManager(readerFactory, context, metadata.getProducers());
		int nextEventIndex = structure.getBodyStartOffset();
		while (nextEventIndex < structure.getMetadataOffset()) {
			Offset offset = new Offset(data, nextEventIndex);
			nextEventIndex = offset.getEnd();
			int eventTypeId = NumberReaders.readInt(data, offset);
			if (eventTypeId == EventParserManager.METADATA_EVENT_TYPE_INDEX
					|| eventTypeId == EventParserManager.CHECK_POINT_EVENT_TYPE_INDEX) {
				// Metadata event || Checkpoint event
			} else {
				// Data event
				eventParser.loadEvent(data, offset, eventTypeId);
			}
		}
		return data;
	}

	public static IChunkLoader create(Chunk input, LoaderContext context)
			throws IOException, CouldNotLoadRecordingException {
		ChunkStructure structure = new ChunkStructure(input);
		byte[] buffer = input.fill(structure.getChunkSize());
		return new ChunkLoaderV0(structure, buffer, context);
	}

	public static ChunkInfo getInfo(Chunk input, long position) throws IOException, CouldNotLoadRecordingException {
		ChunkStructure structure = new ChunkStructure(input);
		byte[] buffer = input.fill(structure.getChunkSize());
		ChunkMetadata metadata = new ChunkMetadata(buffer, structure.getMetadataOffset());
		return new ChunkInfo(position, structure.getChunkSize(),
				QuantityRange.createWithEnd(metadata.getStartTime(), metadata.getEndTime()));
	}

	@Override
	public long getTimestamp() {
		return metadata.getStartTime().longValue();
	}
}
