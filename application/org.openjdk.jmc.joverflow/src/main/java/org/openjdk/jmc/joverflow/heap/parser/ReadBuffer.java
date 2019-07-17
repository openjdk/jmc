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
package org.openjdk.jmc.joverflow.heap.parser;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import org.openjdk.jmc.common.io.IOToolkit;

/**
 * Abstract superclass for positionable read only buffer classes. A concrete implementation may use
 * a mmapped file, a random-access file, a backing array in JVM memory, etc.
 */
public abstract class ReadBuffer {

	/**
	 * An instance of a concrete subclass of this class serves three purposes:
	 * <ul>
	 * <li>Encapsulates information about the dump source (the API supports a file vs. a byte[]
	 * array, the latter supposed to be used in tests)</li>
	 * <li>Provides a create() method that allows for delayed creation of a ReadBuffer instance of a
	 * type associated with a Factory type (e.g. a CachedReadBufferFactory creates an instance of
	 * CachedReadBuffer)</li>
	 * <li>Contains additional information specified at construction time, for example preferred
	 * size for the CachedReadBuffer to be created.</li>
	 * </ul>
	 */
	public static abstract class Factory {

		abstract String getFileName();

		abstract byte[] getFileImageBytes();

		/**
		 * This method is called internally by HprofReader after the heap dump file has been parsed,
		 * and we are about to start random access operations with it. supplementalInfo can be any
		 * object; what it is exactly is an internal contract between HprofReader and Factory
		 * subclasses.
		 */
		abstract public ReadBuffer create(Object supplementalInfo) throws IOException;
	}

	/**
	 * This factory creates an instance of CachedReadBuffer, which is our own HPROF file cache
	 * implementation that uses memory only in the JVM heap.
	 */
	public static class CachedReadBufferFactory extends Factory {
		private final String fileName;
		private final int preferredCacheSize;

		/**
		 * If preferredSize is greater than zero, it will be used as a disk cache size without
		 * further checks. Otherwise, an appropriate cache size will be calculated based on the
		 * available JVM heap size and other factors.
		 */
		public CachedReadBufferFactory(String fileName, int preferredCacheSize) {
			this.fileName = fileName;
			this.preferredCacheSize = preferredCacheSize;
		}

		@Override
		String getFileName() {
			return fileName;
		}

		@Override
		byte[] getFileImageBytes() {
			return null;
		}

		@Override
		public ReadBuffer create(Object supplementalInfo) throws IOException {
			RandomAccessFile file = new RandomAccessFile(fileName, "r");
			try {
				return CachedReadBuffer.createInstance(file, preferredCacheSize);
			} catch (IOException e) {
				IOToolkit.closeSilently(file);
				throw e;
			}
		}
	}

	/**
	 * This factory creates an instance of MappedBuffer or MappedReadMultiBuffer, which uses mmap()
	 * internally, which uses memory outside the JVM heap.
	 * <p>
	 * Note that if the file given to this object is over 2GB, an instance of MappedReadMultiBuffer
	 * is created, which critically depends on data in a long[] array passed to it via the create()
	 * method of this factory. This array specifies the borders of 2GB-or-so "segments" within the
	 * HPROF file, and it can only be generated by HprofReader.
	 */
	public static class MmappedBufferFactory extends Factory {
		private final String fileName;

		@Override
		String getFileName() {
			return fileName;
		}

		@Override
		byte[] getFileImageBytes() {
			return null;
		}

		public MmappedBufferFactory(String fileName) {
			this.fileName = fileName;
		}

		@Override
		public ReadBuffer create(Object supplementalInfo) throws IOException {
			long mappedBBEndOfs[] = (long[]) supplementalInfo;
			int maxSingleMappedBufSize = Integer.MAX_VALUE;
			RandomAccessFile file = new RandomAccessFile(fileName, "r");
			FileChannel ch = file.getChannel();
			try {
				long size = ch.size();

				if (size <= maxSingleMappedBufSize) {
					// Use a single backing MappedByteBuffer
					MappedByteBuffer buf = ch.map(FileChannel.MapMode.READ_ONLY, 0, size);
					return new MappedReadBuffer(buf);
				} else if (mappedBBEndOfs != null) {
					// Use multiple backing MappedByteBuffers
					// Actually, it looks like the internal implementation of MappedByteBuffer supports
					// long file size and offsets. However, there is no public API for it...
					MappedByteBuffer[] bufs = new MappedByteBuffer[mappedBBEndOfs.length];
					long startOfs = 0;
					for (int i = 0; i < mappedBBEndOfs.length; i++) {
						bufs[i] = ch.map(FileChannel.MapMode.READ_ONLY, startOfs, mappedBBEndOfs[i] - startOfs + 1);
						startOfs = mappedBBEndOfs[i] + 1;
					}
					ch.close();
					file.close();
					return new MappedReadMultiBuffer(bufs, mappedBBEndOfs, maxSingleMappedBufSize);
				}
			} finally {
				IOToolkit.closeSilently(ch);
				IOToolkit.closeSilently(file);

			}

			return new FileReadBuffer(file);
		}
	}

	/**
	 * This factory creates an instance of ByteArrayBuffer, which uses file contents that have
	 * already been read directly into memory.
	 */
	public static class ByteArrayBufferFactory extends Factory {
		private final byte[] fileImageBytes;

		public ByteArrayBufferFactory(byte[] fileImageBytes) {
			this.fileImageBytes = fileImageBytes;
		}

		@Override
		String getFileName() {
			return null;
		}

		@Override
		byte[] getFileImageBytes() {
			return fileImageBytes;
		}

		@Override
		public ReadBuffer create(Object supplementalInfo) {
			return new ByteArrayReadBuffer(fileImageBytes);
		}
	}

	// Read methods, that return only byte array and numeric primitive types.

	public abstract void get(long pos, byte[] buf) throws IOException;

	public abstract void get(long pos, byte[] buf, int num) throws IOException;

	public abstract int getInt(long pos) throws IOException;

	public abstract long getLong(long pos) throws IOException;

	public abstract void close();
}
