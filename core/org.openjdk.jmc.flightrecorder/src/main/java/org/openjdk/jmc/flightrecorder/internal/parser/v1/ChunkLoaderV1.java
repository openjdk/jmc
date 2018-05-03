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
package org.openjdk.jmc.flightrecorder.internal.parser.v1;

import java.io.IOException;
import java.util.List;

import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.internal.ChunkInfo;
import org.openjdk.jmc.flightrecorder.internal.IChunkLoader;
import org.openjdk.jmc.flightrecorder.internal.InvalidJfrFileException;
import org.openjdk.jmc.flightrecorder.internal.parser.Chunk;
import org.openjdk.jmc.flightrecorder.internal.parser.LoaderContext;
import org.openjdk.jmc.flightrecorder.internal.parser.v1.ChunkMetadata.ClassElement;
import org.openjdk.jmc.flightrecorder.internal.util.ParserToolkit;

public class ChunkLoaderV1 implements IChunkLoader {

	private final static long CONSTANT_POOL_EVENT_TYPE = 1;

	private final ChunkStructure header;
	private final byte[] data;
	private final LoaderContext context;

	public ChunkLoaderV1(ChunkStructure header, byte[] data, LoaderContext context) {
		this.header = header;
		this.data = data;
		this.context = context;
	}

	@Override
	public byte[] call() throws Exception {
		SeekableInputStream input = SeekableInputStream.build(data, header.isIntegersCompressed());

		// Read metadata
		input.seek(header.getMetadataOffset());
		List<ClassElement> classes = ChunkMetadata.readMetadata(input).metadata.classes;
		TypeManager manager = new TypeManager(classes, context, header);

		// Read constants
		long constantPoolOffset = 0;
		// An initial constantPoolOffset of 0 indicates no constant pools.
		long delta = header.getConstantPoolOffset();
		while (delta != 0) {
			constantPoolOffset += delta;
			input.seek(constantPoolOffset);
			delta = readConstantPoolEvent(input, manager);
		}
		manager.resolveConstants();

		// Read events
		long index = header.getBodyStartOffset();
		while (true) {
			input.seek(index);
			int size = input.readInt();
			long type = input.readLong();
			if (type == ChunkMetadata.METADATA_EVENT_TYPE) {
				return data;
			} else if (type != CONSTANT_POOL_EVENT_TYPE) {
				manager.readEvent(type, input);
			}
			index += size;
		}
	}

	private static long readConstantPoolEvent(IDataInput input, TypeManager manager)
			throws IOException, InvalidJfrFileException {
		input.readInt(); // size
		ParserToolkit.assertValue(input.readLong(), CONSTANT_POOL_EVENT_TYPE); // type;
		input.readLong(); // start
		input.readLong(); // duration
		long delta = input.readLong();
		input.readBoolean(); // flush
		int poolCount = input.readInt();
		for (int i = 0; i < poolCount; i++) {
			long classId = input.readLong();
			int constantCount = input.readInt();
			manager.readConstants(classId, input, constantCount);
		}
		return delta;
	}

	public static IChunkLoader create(Chunk input, LoaderContext context)
			throws IOException, CouldNotLoadRecordingException {
		ChunkStructure header = new ChunkStructure(input);
		byte[] data = input.fill(header.getChunkSize());
		return new ChunkLoaderV1(header, data, context);
	}

	public static ChunkInfo getInfo(Chunk input, long position) throws IOException, CouldNotLoadRecordingException {
		ChunkStructure header = new ChunkStructure(input);
		return new ChunkInfo(position, header.getChunkSize(), header.getChunkRange());
	}

	@Override
	public long getTimestamp() {
		return header.getStartTimeNanos();
	}
}
