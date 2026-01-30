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

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages double-buffered memory-mapped files per thread with background flushing. Each thread gets
 * two fixed-size mmap buffers (active and inactive) for lock-free writes. When the active buffer
 * fills, buffers are swapped and the inactive buffer is flushed to disk in the background.
 */
final class ThreadMmapManager {
	private final Path tempDir;
	private final int chunkSize;
	private final ConcurrentHashMap<Long, ThreadBufferState> threadStates;
	private final ExecutorService flushExecutor;
	private final ConcurrentLinkedQueue<Path> flushedChunks;

	ThreadMmapManager(Path tempDir, int chunkSize) throws IOException {
		this.tempDir = tempDir;
		this.chunkSize = chunkSize;
		this.threadStates = new ConcurrentHashMap<>();
		// Create thread pool with daemon threads so they don't prevent JVM shutdown
		this.flushExecutor = Executors.newFixedThreadPool(Math.min(4, Runtime.getRuntime().availableProcessors()),
				r -> {
					Thread t = new Thread(r);
					t.setDaemon(true);
					return t;
				});
		this.flushedChunks = new ConcurrentLinkedQueue<>();

		// Ensure temp directory exists
		if (!Files.exists(tempDir)) {
			Files.createDirectories(tempDir);
		}
	}

	/**
	 * Get the active writer for the specified thread. Creates double-buffered mmap files on first
	 * access.
	 *
	 * @param threadId
	 *            the thread ID
	 * @return the active LEB128MappedWriter for this thread
	 * @throws IOException
	 *             if mmap file creation fails
	 */
	LEB128MappedWriter getActiveWriter(long threadId) throws IOException {
		ThreadBufferState state = threadStates.computeIfAbsent(threadId, id -> {
			try {
				return createThreadBuffers(id);
			} catch (IOException e) {
				throw new RuntimeException("Failed to create thread buffers for thread " + id, e);
			}
		});
		return state.getActiveWriter();
	}

	/**
	 * Rotate chunk for the specified thread: swap active/inactive buffers and flush the old active
	 * buffer to disk in the background.
	 *
	 * @param threadId
	 *            the thread ID
	 * @throws IOException
	 *             if rotation fails
	 */
	void rotateChunk(long threadId) throws IOException {
		ThreadBufferState state = threadStates.get(threadId);
		if (state == null) {
			return;
		}

		// Swap buffers - get old active for flushing
		LEB128MappedWriter oldActive = state.swapBuffers();

		// Flush old active to disk in background
		int sequence = state.nextSequence();
		Path chunkFile = tempDir.resolve("chunk-" + threadId + "-" + sequence + ".dat");

		flushExecutor.submit(() -> {
			try {
				oldActive.force();
				// Copy to persistent file
				try (FileOutputStream fos = new FileOutputStream(chunkFile.toFile())) {
					oldActive.copyTo(fos);
				}
				flushedChunks.add(chunkFile);
				// Reset buffer for reuse
				oldActive.reset();
			} catch (IOException e) {
				throw new RuntimeException("Failed to flush chunk for thread " + threadId, e);
			}
		});
	}

	/**
	 * Get all flushed chunk files for finalization.
	 *
	 * @return list of flushed chunk file paths
	 */
	List<Path> getFlushedChunks() {
		return new ArrayList<>(flushedChunks);
	}

	/**
	 * Final flush: force flush any active buffers before close.
	 *
	 * @throws IOException
	 *             if flush fails
	 */
	void finalFlush() throws IOException {
		for (ThreadBufferState state : threadStates.values()) {
			LEB128MappedWriter active = state.getActiveWriter();
			if (active.getDataSize() > 0) {
				// Flush final active buffer
				long threadId = state.threadId;
				int sequence = state.nextSequence();
				Path chunkFile = tempDir.resolve("chunk-" + threadId + "-" + sequence + ".dat");

				active.force();
				try (FileOutputStream fos = new FileOutputStream(chunkFile.toFile())) {
					active.copyTo(fos);
				}
				flushedChunks.add(chunkFile);
			}
		}

		// Wait for background flushes to complete
		flushExecutor.shutdown();
		try {
			if (!flushExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
				flushExecutor.shutdownNow();
			}
		} catch (InterruptedException e) {
			flushExecutor.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Cleanup: close all mmap files and delete temporary files.
	 *
	 * @throws IOException
	 *             if cleanup fails
	 */
	void cleanup() throws IOException {
		// Close all mmap files
		for (ThreadBufferState state : threadStates.values()) {
			state.close();
		}

		// Delete chunk files
		for (Path chunk : flushedChunks) {
			Files.deleteIfExists(chunk);
		}

		// Delete buffer files and temp directory
		if (Files.exists(tempDir)) {
			Files.walk(tempDir).sorted((a, b) -> b.compareTo(a)) // Reverse order for depth-first deletion
					.forEach(path -> {
						try {
							Files.deleteIfExists(path);
						} catch (IOException e) {
							// Best effort
						}
					});
		}
	}

	private ThreadBufferState createThreadBuffers(long threadId) throws IOException {
		Path buffer0 = tempDir.resolve("thread-" + threadId + "-buffer-0.mmap");
		Path buffer1 = tempDir.resolve("thread-" + threadId + "-buffer-1.mmap");
		return new ThreadBufferState(threadId, new LEB128MappedWriter(buffer0, chunkSize),
				new LEB128MappedWriter(buffer1, chunkSize));
	}

	/**
	 * Per-thread state managing double-buffered mmap files.
	 */
	static final class ThreadBufferState {
		final long threadId;
		private final LEB128MappedWriter buffer0;
		private final LEB128MappedWriter buffer1;
		private volatile boolean activeIsBuffer0 = true;
		private final AtomicInteger sequence = new AtomicInteger(0);

		ThreadBufferState(long threadId, LEB128MappedWriter buffer0, LEB128MappedWriter buffer1) {
			this.threadId = threadId;
			this.buffer0 = buffer0;
			this.buffer1 = buffer1;
		}

		LEB128MappedWriter getActiveWriter() {
			return activeIsBuffer0 ? buffer0 : buffer1;
		}

		LEB128MappedWriter getInactiveWriter() {
			return activeIsBuffer0 ? buffer1 : buffer0;
		}

		/**
		 * Swap active/inactive buffers, returning the old active buffer for flushing.
		 *
		 * @return the old active buffer
		 */
		synchronized LEB128MappedWriter swapBuffers() {
			LEB128MappedWriter oldActive = getActiveWriter();
			activeIsBuffer0 = !activeIsBuffer0;
			return oldActive;
		}

		int nextSequence() {
			return sequence.getAndIncrement();
		}

		void close() throws IOException {
			buffer0.close();
			buffer1.close();
		}
	}
}
