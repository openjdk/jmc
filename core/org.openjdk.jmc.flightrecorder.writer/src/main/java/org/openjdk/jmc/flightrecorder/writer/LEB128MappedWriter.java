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
package org.openjdk.jmc.flightrecorder.writer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

/**
 * Memory-mapped file writer with fixed-size buffer and support for LEB128 encoded integer types.
 * This implementation uses a memory-mapped file for off-heap storage with bounded memory usage.
 */
final class LEB128MappedWriter extends AbstractLEB128Writer {
	private final FileChannel channel;
	private MappedByteBuffer buffer;
	private final Path mmapFile;
	private final int capacity;
	private int position;

	LEB128MappedWriter(Path file, int capacity) throws IOException {
		this.mmapFile = file;
		this.capacity = capacity;
		this.position = 0;

		// Create file and map it
		this.channel = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.READ,
				StandardOpenOption.WRITE);
		this.buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, capacity);
	}

	@Override
	public void reset() {
		position = 0;
		buffer.position(0);
	}

	/**
	 * Check if the buffer can fit the specified number of bytes.
	 *
	 * @param bytes
	 *            the number of bytes to check
	 * @return true if the bytes can fit, false otherwise
	 */
	boolean canFit(int bytes) {
		return position + bytes <= capacity;
	}

	/**
	 * Flush pending writes to disk.
	 */
	void force() {
		if (buffer != null) {
			buffer.force();
		}
	}

	/**
	 * Get the current data size (number of bytes written).
	 *
	 * @return the current position/size
	 */
	int getDataSize() {
		return position;
	}

	/**
	 * Copy current data to output stream.
	 *
	 * @param out
	 *            the output stream to copy to
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	void copyTo(OutputStream out) throws IOException {
		byte[] data = new byte[position];
		buffer.position(0);
		buffer.get(data);
		out.write(data);
	}

	/**
	 * Get the path to the memory-mapped file.
	 *
	 * @return the file path
	 */
	Path getFilePath() {
		return mmapFile;
	}

	@Override
	public int position() {
		return position;
	}

	@Override
	public int capacity() {
		return capacity;
	}

	@Override
	public long writeFloat(long offset, float data) {
		int off = (int) offset;
		buffer.putFloat(off, data);
		position = Math.max(position, off + 4);
		return off + 4;
	}

	@Override
	public long writeDouble(long offset, double data) {
		int off = (int) offset;
		buffer.putDouble(off, data);
		position = Math.max(position, off + 8);
		return off + 8;
	}

	@Override
	public long writeByte(long offset, byte data) {
		int off = (int) offset;
		buffer.put(off, data);
		position = Math.max(position, off + 1);
		return off + 1;
	}

	@Override
	public long writeBytes(long offset, byte ... data) {
		if (data == null) {
			return offset;
		}
		int off = (int) offset;
		buffer.position(off);
		buffer.put(data);
		position = Math.max(position, off + data.length);
		return off + data.length;
	}

	@Override
	public long writeShortRaw(long offset, short data) {
		int off = (int) offset;
		buffer.putShort(off, data);
		position = Math.max(position, off + 2);
		return off + 2;
	}

	@Override
	public long writeIntRaw(long offset, int data) {
		int off = (int) offset;
		buffer.putInt(off, data);
		position = Math.max(position, off + 4);
		return off + 4;
	}

	@Override
	public long writeLongRaw(long offset, long data) {
		int off = (int) offset;
		buffer.putLong(off, data);
		position = Math.max(position, off + 8);
		return off + 8;
	}

	@Override
	public void export(Consumer<ByteBuffer> consumer) {
		// Create a read-only view of the data written so far
		ByteBuffer view = buffer.asReadOnlyBuffer();
		view.position(0);
		view.limit(position);
		consumer.accept(view);
	}

	/**
	 * Export the current data as a byte array.
	 *
	 * @return byte array containing the data
	 */
	byte[] exportBytes() {
		byte[] data = new byte[position];
		buffer.position(0);
		buffer.get(data);
		return data;
	}

	/**
	 * Close the writer and release resources. Note: The backing file is NOT deleted - caller is
	 * responsible for cleanup.
	 *
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	void close() throws IOException {
		if (buffer != null) {
			force();
			// Help GC by clearing reference
			buffer = null;
		}
		if (channel != null && channel.isOpen()) {
			channel.close();
		}
	}
}
