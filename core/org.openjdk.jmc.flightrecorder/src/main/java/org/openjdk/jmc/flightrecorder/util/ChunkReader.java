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
package org.openjdk.jmc.flightrecorder.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.internal.util.DataInputToolkit;

/**
 * Provides an efficient means to read JFR data, chunk by chunk. The actual method employed will
 * depend on whether the JFR file is available as a stream or as a file, and whether or not the data
 * is compressed or not.
 * <p>
 * Each chunk will be self-contained and parsable, for example by wrapping it in a
 * {@link ByteArrayInputStream} and using the {@link JfrLoaderToolkit}.
 */
public final class ChunkReader {
	private static final byte[] JFR_MAGIC_BYTES = new byte[] {'F', 'L', 'R', 0};
	private static final int[] JFR_MAGIC = new int[] {'F', 'L', 'R', 0};
	private static final int ZIP_MAGIC[] = new int[] {31, 139};
	private static final int GZ_MAGIC[] = new int[] {31, 139};
	// For JDK 8 this is the size of the magic + version and offset to the meta data event.
	// For JDK 9 and later, this it the part of the header right up to, and including, the chunk size.
	private static final int HEADER_SIZE = DataInputToolkit.INTEGER_SIZE + 2 * DataInputToolkit.SHORT_SIZE
			+ DataInputToolkit.LONG_SIZE;

	/**
	 * Chunk iterator for an uncompressed JFR file. Efficiently reads a JFR file, chunk by chunk,
	 * into memory as byte arrays by memory mapping the JFR file, finding the chunk boundaries with
	 * a minimum of parsing, and then block-transferring the byte arrays. The transfers will be done
	 * on {@link Iterator#next()}, and the resulting byte array will only be reachable for as long
	 * as it is referenced. The JFR file must not be zip or gzip compressed.
	 * <p>
	 * Note that {@link Iterator#next()} can throw {@link IllegalArgumentException} if it encounters
	 * a corrupted chunk.
	 */
	private static class ChunkIterator implements Iterator<byte[]> {
		int lastChunkOffset;
		private RandomAccessFile file;
		private final FileChannel channel;
		private final MappedByteBuffer buffer;

		private ChunkIterator(File jfrFile) throws IOException {
			try {
				file = new RandomAccessFile(jfrFile, "r"); //$NON-NLS-1$
				channel = file.getChannel();
				buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
				if (!bufferHasMagic(JFR_MAGIC)) {
					if (bufferHasMagic(GZ_MAGIC) || bufferHasMagic(ZIP_MAGIC)) {
						throw new IOException(
								"Cannot use the ChunkIterators with gzipped JMC files. Please use unzipped recordings."); //$NON-NLS-1$
					} else {
						throw new IOException("The provided file (" + String.valueOf(jfrFile) + ") is not a JFR file!"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			} catch (Exception e) {
				if (file != null) {
					file.close();
				}
				throw e;
			}
		}

		@Override
		public boolean hasNext() {
			boolean hasNext = checkHasMore();
			if (!hasNext) {
				try {
					channel.close();
					file.close();
				} catch (IOException e) {
					// Shouldn't happen.
					e.printStackTrace();
				}
			}
			return hasNext;
		}

		private boolean checkHasMore() {
			return lastChunkOffset < buffer.limit();
		}

		@Override
		public byte[] next() {
			if (!checkHasMore()) {
				throw new NoSuchElementException();
			}
			if (!bufferHasMagic(JFR_MAGIC)) {
				lastChunkOffset = buffer.limit() + 1;
				throw new IllegalArgumentException("Corrupted chunk encountered! Aborting!"); //$NON-NLS-1$
			}

			int index = lastChunkOffset + JFR_MAGIC.length;
			short versionMSB = buffer.getShort(index);
			// short versionLSB = buffer.getShort(index + SHORT_SIZE);
			index += 2 * DataInputToolkit.SHORT_SIZE;
			int size = 0;

			if (versionMSB >= 1) {
				// We have a JDK 9+ recording - chunk size can be directly read from header
				size = (int) buffer.getLong(index);
				index = lastChunkOffset + size;
			} else {
				// Got a pre JDK 9 recording. Need to find the metadata event index, read and
				// add the size of the metadata event to find the chunk boundary
				index = lastChunkOffset + (int) buffer.getLong(index);
				// Reading the metadata event size
				int lastEventSize = buffer.getInt(index);
				index += lastEventSize;
				size = index - lastChunkOffset;
			}
			// Read the chunk and return it
			byte[] result = new byte[size];
			buffer.position(lastChunkOffset);
			buffer.get(result, 0, result.length);
			lastChunkOffset = index;
			return result;
		}

		private boolean bufferHasMagic(int[] magicBytes) {
			for (int i = 0; i < magicBytes.length; i++) {
				if (buffer.get(lastChunkOffset + i) != magicBytes[i]) {
					return false;
				}
			}
			return true;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Cannot remove chunks"); //$NON-NLS-1$
		}
	}

	private enum StreamState {
		NEXT_CHUNK, JFR_CHECKED, ERROR
	}

	/**
	 * Iterator reading JFR chunks from a stream.
	 */
	private static class StreamChunkIterator implements Iterator<byte[]> {
		private final DataInputStream inputStream;
		private StreamState streamState = StreamState.NEXT_CHUNK;
		private Throwable lastError = null;

		public StreamChunkIterator(InputStream inputStream) {
			this.inputStream = getDataStream(inputStream);
		}

		private DataInputStream getDataStream(InputStream is) {
			if (is.markSupported()) {
				return new DataInputStream(is);
			}
			return new DataInputStream(new BufferedInputStream(is));
		}

		@Override
		public boolean hasNext() {
			boolean hasNext = false;
			if (streamState == StreamState.NEXT_CHUNK) {
				hasNext = validateJFRMagic();
			} else if (streamState == StreamState.JFR_CHECKED) {
				hasNext = true;
			}
			if (!hasNext) {
				IOToolkit.closeSilently(inputStream);
			}
			return hasNext;
		}

		private boolean validateJFRMagic() {
			try {
				if (IOToolkit.hasMagic(inputStream, JFR_MAGIC)) {
					streamState = StreamState.JFR_CHECKED;
					return true;
				} else {
					streamState = StreamState.ERROR;
					lastError = new Exception(
							"Next chunk has no JFR magic. It is either no JFR file at all or corrupt."); //$NON-NLS-1$
					return false;
				}
			} catch (IOException e) {
				streamState = StreamState.ERROR;
				lastError = e;
				return false;
			}
		}

		@Override
		public byte[] next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			switch (streamState) {
			case ERROR:
				throw new IllegalArgumentException(lastError);
			case NEXT_CHUNK:
				if (!validateJFRMagic()) {
					throw new IllegalArgumentException(lastError);
				}
				// Fall through
			case JFR_CHECKED:
				try {
					return retrieveNextChunk();
				} catch (IOException e) {
					lastError = e;
					throw new IllegalArgumentException(e);
				}
			default:
				throw new IllegalArgumentException("Unknown stream state"); //$NON-NLS-1$
			}
		}

		private byte[] retrieveNextChunk() throws IOException {
			byte[] chunkHeader = new byte[HEADER_SIZE];
			// Copy in the magic
			System.arraycopy(JFR_MAGIC_BYTES, 0, chunkHeader, 0, JFR_MAGIC_BYTES.length);
			// Read rest of chunk header
			readBytesFromStream(chunkHeader, JFR_MAGIC_BYTES.length, HEADER_SIZE - JFR_MAGIC_BYTES.length);
			short majorVersion = DataInputToolkit.readShort(chunkHeader, JFR_MAGIC_BYTES.length);
			byte[] chunkTotal = null;
			if (majorVersion >= 1) {
				// JDK 9+ recording
				long fullSize = DataInputToolkit.readLong(chunkHeader, HEADER_SIZE - DataInputToolkit.LONG_SIZE);
				int readSize = (int) fullSize - HEADER_SIZE;
				chunkTotal = new byte[(int) fullSize];
				System.arraycopy(chunkHeader, 0, chunkTotal, 0, chunkHeader.length);
				readBytesFromStream(chunkTotal, HEADER_SIZE, readSize);
			} else {
				long metadataIndex = DataInputToolkit.readLong(chunkHeader, HEADER_SIZE - DataInputToolkit.LONG_SIZE);
				int eventReadSize = (int) (metadataIndex - HEADER_SIZE + DataInputToolkit.INTEGER_SIZE);
				byte[] chunkEvents = new byte[eventReadSize];
				readBytesFromStream(chunkEvents, 0, chunkEvents.length);
				int metadataEventSize = DataInputToolkit.readInt(chunkEvents,
						eventReadSize - DataInputToolkit.INTEGER_SIZE) - DataInputToolkit.INTEGER_SIZE;
				byte[] chunkMetadata = new byte[metadataEventSize];
				readBytesFromStream(chunkMetadata, 0, chunkMetadata.length);

				chunkTotal = new byte[chunkHeader.length + chunkEvents.length + chunkMetadata.length];
				System.arraycopy(chunkHeader, 0, chunkTotal, 0, chunkHeader.length);
				System.arraycopy(chunkEvents, 0, chunkTotal, chunkHeader.length, chunkEvents.length);
				System.arraycopy(chunkMetadata, 0, chunkTotal, chunkHeader.length + chunkEvents.length,
						chunkMetadata.length);
			}
			streamState = StreamState.NEXT_CHUNK;
			return chunkTotal;
		}

		private void readBytesFromStream(byte[] bytes, int offset, int count) throws IOException {
			int totalRead = 0;
			while (totalRead < count) {
				int read = inputStream.read(bytes, offset + totalRead, count - totalRead);
				if (read == -1) {
					throw new IOException("Unexpected end of data."); //$NON-NLS-1$
				}
				totalRead += read;
			}
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Cannot remove chunks"); //$NON-NLS-1$
		}
	}

	/**
	 * Reads a JFR file, chunk by chunk.
	 * <p>
	 * Each chunk will be self contained and parsable, for example by wrapping it in a
	 * {@link ByteArrayInputStream}. Note that {@link Iterator#next()} can throw
	 * {@link IllegalArgumentException} if it encounters a corrupted chunk.
	 *
	 * @param jfrFile
	 *            the file to read binary data from
	 * @return returns an iterator over byte arrays, where each byte array is a self containing jfr
	 *         chunk
	 */
	public static Iterator<byte[]> readChunks(File jfrFile) throws IOException {
		// We fall back to using a StreamChunkIterator if the file is compressed.
		if (IOToolkit.isCompressedFile(jfrFile)) {
			return new StreamChunkIterator(IOToolkit.openUncompressedStream(jfrFile));
		}
		return new ChunkIterator(jfrFile);
	}

	/**
	 * Reads a JFR file, chunk by chunk, from a stream.
	 * <p>
	 * Each chunk will be self contained and parsable, for example by wrapping it in a
	 * {@link ByteArrayInputStream}. Note that {@link Iterator#next()} can throw
	 * {@link IllegalArgumentException} if it encounters a corrupted chunk.
	 *
	 * @param jfrStream
	 *            the stream to read binary data from
	 * @return returns an iterator over byte arrays, where each byte array is a self containing JFR
	 *         chunk
	 */
	public static Iterator<byte[]> readChunks(InputStream jfrStream) throws IOException {
		return new StreamChunkIterator(IOToolkit.openUncompressedStream(jfrStream));
	}

	/**
	 * Program for listing the number of chunks in a recording.
	 *
	 * @param args
	 *            takes one argument, which must be the path to a recording
	 * @throws IOException
	 *             if there was a problem reading the file
	 */
	public static void main(String[] args) throws IOException {
		long nanoStart = System.nanoTime();
		int chunkCount = 0, byteCount = 0;

		if (args.length != 1) {
			System.out.println("Usage: ChunkReader <file>"); //$NON-NLS-1$
			System.exit(2);
		}
		File file = new File(args[0]);
		if (!file.exists()) {
			System.out.println("The file " + file.getAbsolutePath() + " does not exist. Exiting..."); //$NON-NLS-1$ //$NON-NLS-2$
			System.exit(3);
		}
		Iterator<byte[]> iter = readChunks(file);
		while (iter.hasNext()) {
			byte[] bytes = iter.next();
			chunkCount += 1;
			byteCount += bytes.length;
			System.out.println("Chunk #" + chunkCount + " size: " + bytes.length); //$NON-NLS-1$ //$NON-NLS-2$
		}
		double duration = (System.nanoTime() - nanoStart) / 1_000_000d;

		System.out.println("Chunks: " + chunkCount + " Byte count: " + byteCount + " Time taken: " + duration + " ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}
}
