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
package org.openjdk.jmc.flightrecorder.internal;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.internal.parser.Chunk;
import org.openjdk.jmc.flightrecorder.internal.parser.LoaderContext;
import org.openjdk.jmc.flightrecorder.internal.parser.v0.ChunkLoaderV0;
import org.openjdk.jmc.flightrecorder.internal.parser.v1.ChunkLoaderV1;
import org.openjdk.jmc.flightrecorder.parser.IParserExtension;
import org.openjdk.jmc.flightrecorder.parser.ParserExtensionRegistry;

/**
 * Helper class for loading flight recordings from disk.
 */
public final class FlightRecordingLoader {

	private static final Logger LOGGER = Logger.getLogger(FlightRecordingLoader.class.getName());
	private static final String SINGLE_THREADED_PARSER_PROPERTY_KEY = "org.openjdk.jmc.flightrecorder.parser.singlethreaded"; //$NON-NLS-1$
	private static final int MIN_MEMORY_PER_THREAD = 300 * 1024 * 1024; // Unless the chunks are very big, 300MB of available memory per parallel chunk load should be plenty
	private static final short VERSION_0 = 0; // JDK7 & JDK8
	private static final short VERSION_1 = 1; // JDK9 & JDK10
	private static final short VERSION_2 = 2; // JDK11
	private static final byte[] FLIGHT_RECORDER_MAGIC = {'F', 'L', 'R', '\0'};

	public static EventArray[] loadStream(InputStream stream, boolean hideExperimentals, boolean ignoreTruncatedChunk)
			throws CouldNotLoadRecordingException, IOException {
		return loadStream(stream, ParserExtensionRegistry.getParserExtensions(), hideExperimentals,
				ignoreTruncatedChunk);
	}

	/**
	 * Read events from an input stream of JFR data.
	 *
	 * @param stream
	 *            input stream
	 * @param extensions
	 *            the extensions to use when parsing the data
	 * @param hideExperimentals
	 *            if {@code true}, then events of types marked as experimental will be ignored when
	 *            reading the data
	 * @return an array of EventArrays (one event type per EventArray)
	 */
	public static EventArray[] loadStream(
		InputStream stream, List<? extends IParserExtension> extensions, boolean hideExperimentals,
		boolean ignoreTruncatedChunk) throws CouldNotLoadRecordingException, IOException {
		return readChunks(null, extensions, createChunkSupplier(stream), hideExperimentals, ignoreTruncatedChunk);
	}

	public static IChunkSupplier createChunkSupplier(final InputStream input)
			throws CouldNotLoadRecordingException, IOException {
		return new IChunkSupplier() {

			@Override
			public Chunk getNextChunk(byte[] reusableBuffer) throws CouldNotLoadRecordingException, IOException {
				int value = input.read();
				if (value < 0) {
					return null;
				}
				return createChunkInput(new DataInputStream(input), value, reusableBuffer);
			}
		};
	}

	public static IChunkSupplier createChunkSupplier(final RandomAccessFile input)
			throws CouldNotLoadRecordingException, IOException {
		return new IChunkSupplier() {

			@Override
			public Chunk getNextChunk(byte[] reusableBuffer) throws CouldNotLoadRecordingException, IOException {
				if (input.length() > input.getFilePointer()) {
					return createChunkInput(input, input.readUnsignedByte(), reusableBuffer);
				}
				return null;
			}

		};
	}

	public static IChunkSupplier createChunkSupplier(final RandomAccessFile input, Collection<ChunkInfo> chunks)
			throws CouldNotLoadRecordingException, IOException {
		final LinkedList<ChunkInfo> include = new LinkedList<>(chunks);
		return new IChunkSupplier() {

			@Override
			public Chunk getNextChunk(byte[] reusableBuffer) throws CouldNotLoadRecordingException, IOException {
				if (include.isEmpty()) {
					return null;
				}
				input.seek(include.poll().getChunkPosistion());
				return createChunkInput(input, input.readUnsignedByte(), reusableBuffer);
			}

		};

	}

	private static Chunk createChunkInput(DataInput input, int firstByte, byte[] reusableBuffer)
			throws CouldNotLoadRecordingException, IOException {
		int i = 0;
		while (FLIGHT_RECORDER_MAGIC[i] == firstByte) {
			if (++i == FLIGHT_RECORDER_MAGIC.length) {
				return new Chunk(input, FLIGHT_RECORDER_MAGIC.length, reusableBuffer);
			}
			firstByte = input.readUnsignedByte();
		}
		throw new InvalidJfrFileException();
	}

	public static List<ChunkInfo> readChunkInfo(IChunkSupplier chunkSupplier)
			throws CouldNotLoadRecordingException, IOException {
		long nextChunkPos = 0;
		final List<ChunkInfo> chunks = new ArrayList<>();
		byte[] buffer = new byte[0];
		Chunk nextChunk;
		while ((nextChunk = chunkSupplier.getNextChunk(buffer)) != null) {
			ChunkInfo info = getChunkInfo(nextChunk, nextChunkPos);
			nextChunk.skip(info.getChunkSize());
			buffer = nextChunk.getReusableBuffer();
			nextChunkPos += info.getChunkSize();
			chunks.add(info);
		}
		return chunks;
	}

	private static ChunkInfo getChunkInfo(Chunk nextChunk, long nextChunkPos)
			throws CouldNotLoadRecordingException, IOException {
		switch (nextChunk.getMajorVersion()) {
		case VERSION_0:
			return ChunkLoaderV0.getInfo(nextChunk, nextChunkPos);
		case VERSION_1:
		case VERSION_2:
			return ChunkLoaderV1.getInfo(nextChunk, nextChunkPos);
		default:
			throw new VersionNotSupportedException();
		}
	}

	public static EventArray[] readChunks(
		Runnable monitor, IChunkSupplier chunkSupplier, boolean hideExperimentals, boolean ignoreTruncatedChunk)
			throws CouldNotLoadRecordingException, IOException {
		return readChunks(monitor, ParserExtensionRegistry.getParserExtensions(), chunkSupplier, hideExperimentals,
				ignoreTruncatedChunk);
	}

	public static EventArray[] readChunks(
		Runnable monitor, List<? extends IParserExtension> extensions, IChunkSupplier chunkSupplier,
		boolean hideExperimentals, boolean ignoreTruncatedChunk) throws CouldNotLoadRecordingException, IOException {
		LoaderContext context = new LoaderContext(extensions, hideExperimentals);
		Runtime rt = Runtime.getRuntime();
		long availableMemory = rt.maxMemory() - rt.totalMemory() + rt.freeMemory();
		long maxBuffersCount = Math.min(Math.max(availableMemory / MIN_MEMORY_PER_THREAD, 1),
				rt.availableProcessors() - 1);

		ExecutorService threadPool;
		if (Boolean.getBoolean(SINGLE_THREADED_PARSER_PROPERTY_KEY)) {
			threadPool = Executors.newSingleThreadExecutor();
		} else {
			threadPool = Executors.newCachedThreadPool();
		}

		int chunkCount = 0;
		try {
			ExecutorCompletionService<byte[]> service = new ExecutorCompletionService<>(threadPool);
			byte[] buffer = new byte[0];
			int outstanding = 0;
			Set<Long> loadedChunkTimestamps = new HashSet<>();
			IChunkLoader chunkLoader;
			while ((chunkLoader = createChunkLoader(chunkSupplier, context, buffer, ignoreTruncatedChunk)) != null) {
				Long ts = chunkLoader.getTimestamp();
				if (!loadedChunkTimestamps.contains(ts)) {
					loadedChunkTimestamps.add(ts);
					service.submit(chunkLoader);
					chunkCount++;
					outstanding++;
					// Recover buffer from finished chunk loader for reuse or create a new buffer
					Future<byte[]> available = service.poll();
					if (available != null) {
						buffer = available.get();
						sendProgress(monitor);
						outstanding--;
					} else if (outstanding < maxBuffersCount) {
						buffer = new byte[0];
					} else {
						buffer = service.take().get();
						sendProgress(monitor);
						outstanding--;
					}
				}
			}
			// Wait for all outstanding loaders to complete
			while (outstanding > 0) {
				service.take().get();
				sendProgress(monitor);
				outstanding--;
			}
			if (chunkCount == 0) {
				// Recordings without any chunks are not allowed
				throw new InvalidJfrFileException("No readable chunks in recording"); //$NON-NLS-1$
			}
		} catch (InterruptedException e) {
			throw new CouldNotLoadRecordingException(e);
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof Error) {
				throw ((Error) cause);
			} else if (cause instanceof RuntimeException) {
				throw ((RuntimeException) cause);
			} else if (cause instanceof IOException) {
				throw ((IOException) cause);
			} else if (cause instanceof CouldNotLoadRecordingException) {
				throw ((CouldNotLoadRecordingException) cause);
			} else {
				throw new CouldNotLoadRecordingException(cause);
			}
		} finally {
			threadPool.shutdownNow();
		}
		LOGGER.fine("Loaded JFR with " + chunkCount + " chunks"); //$NON-NLS-1$ //$NON-NLS-2$
		return context.buildEventArrays();
	}

	private static void sendProgress(Runnable listener) {
		if (listener != null) {
			listener.run();
		}
	}

	/**
	 * @param chunkSupplier
	 *            chunk data source
	 * @param context
	 *            loader context that the returned chunk loader will send event data to
	 * @param buffer
	 *            Initial byte array to use for storing chunk data. See
	 *            {@link IChunkSupplier#getNextChunk(byte[])}.
	 * @param ignoreTruncatedChunk
	 *            if true, then any exceptions caused by getting and reading the next chunk will be
	 *            ignored and instead make the method return null
	 * @return a new chunk loader or null if no more data is available from the chunk supplier
	 */
	private static IChunkLoader createChunkLoader(
		IChunkSupplier chunkSupplier, LoaderContext context, byte[] buffer, boolean ignoreTruncatedChunk)
			throws CouldNotLoadRecordingException, IOException {
		try {
			Chunk chunk = chunkSupplier.getNextChunk(buffer);
			if (chunk != null) {
				switch (chunk.getMajorVersion()) {
				case VERSION_0:
					return ChunkLoaderV0.create(chunk, context);
				case VERSION_1:
				case VERSION_2:
					return ChunkLoaderV1.create(chunk, context);
				default:
					throw new VersionNotSupportedException();
				}
			}
		} catch (IOException e) {
			if (ignoreTruncatedChunk) {
				LOGGER.log(Level.INFO, "Ignoring exception while reading chunk", e); //$NON-NLS-1$
			} else {
				throw e;
			}
		}
		return null;
	}
}
