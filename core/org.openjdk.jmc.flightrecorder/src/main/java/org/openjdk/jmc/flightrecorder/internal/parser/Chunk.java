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
package org.openjdk.jmc.flightrecorder.internal.parser;

import java.io.DataInput;
import java.io.IOException;
import java.util.Arrays;

import org.openjdk.jmc.flightrecorder.internal.InvalidJfrFileException;
import org.openjdk.jmc.flightrecorder.internal.util.DataInputToolkit;

/**
 * Class for handling data belonging to a single chunk.
 */
public class Chunk {
	private final DataInput input;
	private final short majorVersion;
	private final short minorVersion;
	private int position;
	private byte[] data;

	/**
	 * @param input
	 *            input to read chunk data from
	 * @param offset
	 *            initial position
	 * @param reusableBuffer
	 *            a byte array for holding read chunk data
	 */
	public Chunk(DataInput input, int offset, byte[] reusableBuffer) throws IOException, InvalidJfrFileException {
		this.input = input;
		this.data = reusableBuffer;
		position = offset;
		byte[] buffer = fill(offset + 2 * DataInputToolkit.SHORT_SIZE);
		majorVersion = DataInputToolkit.readShort(buffer, offset);
		minorVersion = DataInputToolkit.readShort(buffer, offset + DataInputToolkit.SHORT_SIZE);
	}

	public short getMajorVersion() {
		return majorVersion;
	}

	public short getMinorVersion() {
		return minorVersion;
	}

	public int getPosition() {
		return position;
	}

	/**
	 * Copy data from the input source to the chunk buffer. Note that this may replace the buffer if
	 * it is not large enough.
	 *
	 * @param upToPosition
	 *            position to fill buffer to
	 * @return the current buffer for the chunk data
	 */
	public byte[] fill(long upToPosition) throws IOException, InvalidJfrFileException {
		int fillUpTo = getArrayPosition(upToPosition);
		if (data.length < fillUpTo) {
			data = Arrays.copyOf(data, (int) (fillUpTo * 1.2));
		}
		if (fillUpTo > position) {
			input.readFully(data, position, fillUpTo - position);
			position = fillUpTo;
		}
		return data;
	}

	/**
	 * Skip reading data from the input source up to a specified position. Note that the skipped
	 * data can not be read later from the same input source.
	 *
	 * @param upToPosition
	 *            chunk relative position
	 */
	public void skip(long upToPosition) throws IOException, InvalidJfrFileException {
		int skipUpTo = getArrayPosition(upToPosition);
		if (skipUpTo > position) {
			int skipped = input.skipBytes(skipUpTo - position);
			position += skipped;
		}
	}

	/**
	 * Return the current data buffer. Note that the returned array is only guaranteed to contain
	 * data up to the read position when this method is called. If the buffer is filled more later,
	 * then that data may end up in a new array.
	 *
	 * @return the current chunk data buffer
	 */
	public byte[] getReusableBuffer() {
		return data;
	}

	private static int getArrayPosition(long pos) throws InvalidJfrFileException {
		if (pos > Integer.MAX_VALUE) {
			throw new InvalidJfrFileException();
		} else {
			return (int) pos;
		}
	}

}
