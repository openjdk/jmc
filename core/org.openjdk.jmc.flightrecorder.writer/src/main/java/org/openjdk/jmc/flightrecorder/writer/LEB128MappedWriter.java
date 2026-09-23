/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, Datadog, Inc. All rights reserved.
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.function.Consumer;

/**
 * Memory-mapped file writer with fixed-size buffer and support for LEB128 encoded integer types.
 * This implementation uses a memory-mapped file for off-heap storage with bounded memory usage.
 */
final class LEB128MappedWriter extends AbstractLEB128Writer {
	private static final int COPY_SLICE_SIZE = 64 * 1024;

	private final FileChannel channel;
	private MappedByteBuffer buffer;
	private final Path mmapFile;
	private final int capacity;
	// volatile so a cross-thread flush (finalFlush()) sees a fully published buffer: every write
	// method publishes its buffer.put(...) via this field, and a reader that observes the new
	// value is guaranteed to see the bytes written before it.
	private volatile int position;
	private volatile boolean closed;
	// only touched by the single flush task owning this buffer (see ThreadBufferState#swapBuffers)
	// and by finalFlush() once that executor has drained, so it needs no synchronization
	private byte[] copySlice;

	LEB128MappedWriter(Path file, int capacity) throws IOException {
		this.mmapFile = file;
		this.capacity = capacity;
		this.position = 0;

		this.channel = openOwnerOnly(file);
		try {
			this.buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, capacity);
		} catch (IOException | OutOfMemoryError e) {
			channel.close();
			throw e;
		}
	}

	/**
	 * Creates the backing file with owner-only permissions where the file system supports it.
	 * {@linkplain Files#createFile} also fails when any file or symlink already exists at the path,
	 * so a pre-planted symlink cannot redirect the writes.
	 */
	private static FileChannel openOwnerOnly(Path file) throws IOException {
		try {
			Files.createFile(file, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
		} catch (UnsupportedOperationException e) {
			// file system without POSIX permissions
			Files.createFile(file);
		}
		return FileChannel.open(file, StandardOpenOption.READ, StandardOpenOption.WRITE);
	}

	private void ensureOpen() {
		if (closed || buffer == null) {
			throw new IllegalStateException("writer is closed: " + mmapFile);
		}
	}

	@Override
	public void reset() {
		ensureOpen();
		position = 0;
		buffer.position(0);
	}

	boolean canFit(int bytes) {
		return position + bytes <= capacity;
	}

	void force() {
		if (buffer != null) {
			buffer.force();
		}
	}

	int getDataSize() {
		return position;
	}

	void copyTo(OutputStream out) throws IOException {
		ensureOpen();
		if (position == 0) {
			return;
		}
		// stream the mapped data in bounded slices instead of copying the whole chunk on-heap
		if (copySlice == null) {
			copySlice = new byte[Math.min(COPY_SLICE_SIZE, capacity)];
		}
		for (int off = 0; off < position;) {
			int len = Math.min(copySlice.length, position - off);
			buffer.get(off, copySlice, 0, len);
			out.write(copySlice, 0, len);
			off += len;
		}
	}

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
		ensureOpen();
		int off = (int) offset;
		buffer.putFloat(off, data);
		position = Math.max(position, off + 4);
		return off + 4;
	}

	@Override
	public long writeDouble(long offset, double data) {
		ensureOpen();
		int off = (int) offset;
		buffer.putDouble(off, data);
		position = Math.max(position, off + 8);
		return off + 8;
	}

	@Override
	public long writeByte(long offset, byte data) {
		ensureOpen();
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
		ensureOpen();
		int off = (int) offset;
		buffer.put(off, data, 0, data.length);
		position = Math.max(position, off + data.length);
		return off + data.length;
	}

	@Override
	public long writeShortRaw(long offset, short data) {
		ensureOpen();
		int off = (int) offset;
		buffer.putShort(off, data);
		position = Math.max(position, off + 2);
		return off + 2;
	}

	@Override
	public long writeIntRaw(long offset, int data) {
		ensureOpen();
		int off = (int) offset;
		buffer.putInt(off, data);
		position = Math.max(position, off + 4);
		return off + 4;
	}

	@Override
	public long writeLongRaw(long offset, long data) {
		ensureOpen();
		int off = (int) offset;
		buffer.putLong(off, data);
		position = Math.max(position, off + 8);
		return off + 8;
	}

	@Override
	public void export(Consumer<ByteBuffer> consumer) {
		ensureOpen();
		ByteBuffer view = buffer.asReadOnlyBuffer();
		view.position(0);
		view.limit(position);
		consumer.accept(view);
	}

	byte[] exportBytes() {
		ensureOpen();
		byte[] data = new byte[position];
		buffer.get(0, data, 0, position);
		return data;
	}

	// backing file is NOT deleted here - caller owns cleanup
	void close() throws IOException {
		closed = true;
		if (buffer != null) {
			force();
			buffer = null;
		}
		if (channel != null && channel.isOpen()) {
			channel.close();
		}
	}
}
