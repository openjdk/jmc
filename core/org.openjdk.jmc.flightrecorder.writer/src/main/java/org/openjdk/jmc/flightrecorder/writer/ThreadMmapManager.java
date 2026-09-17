/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2025, 2026, Datadog, Inc. All rights reserved.
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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Manages double-buffered memory-mapped files per thread with background flushing. Each thread gets
 * two fixed-size mmap buffers (active and inactive). Normal event writes go to the per-thread
 * active buffer without cross-thread locking; only buffer rotation (swap) is synchronized per
 * thread state. When the active buffer fills, buffers are swapped and the inactive buffer is
 * flushed to disk in the background.
 *
 * Temp files are only removed by {@linkplain #cleanup()}; a recording abandoned without a close()
 * leaves its temp files behind. There is no stale-file sweep because another live recording may own
 * adjacent {@code jfr-writer-mmap-*} directories.
 */
final class ThreadMmapManager {
	private static final Logger LOGGER = Logger.getLogger(ThreadMmapManager.class.getName());

	private final Path tempDir;
	private final int chunkSize;
	private final ConcurrentHashMap<Long, ThreadBufferState> threadStates;
	private final ExecutorService flushExecutor;
	private final ConcurrentLinkedQueue<Future<?>> flushFutures;
	private final ConcurrentLinkedQueue<ChunkRef> flushedChunks;

	ThreadMmapManager(Path tempDir, int chunkSize) throws IOException {
		this.tempDir = tempDir;
		this.chunkSize = chunkSize;
		if (!Files.exists(tempDir)) {
			Files.createDirectories(tempDir);
		}

		this.threadStates = new ConcurrentHashMap<>();
		// fixed pool for I/O-bound flush tasks, decoupled from CPU cores since concurrent disk
		// writes have diminishing returns beyond a small number of threads. Also bounds flush
		// throughput: writer threads block in swapBuffers() on the pending flush when rotating
		// faster than the pool can drain.
		this.flushExecutor = Executors.newFixedThreadPool(2, r -> {
			Thread t = new Thread(r);
			t.setDaemon(true);
			return t;
		});
		this.flushFutures = new ConcurrentLinkedQueue<>();
		this.flushedChunks = new ConcurrentLinkedQueue<>();
	}

	/**
	 * Get the active writer for the specified thread, creating the double-buffered mmap files on
	 * first access.
	 */
	LEB128MappedWriter getActiveWriter(long threadId) throws IOException {
		ThreadBufferState state = threadStates.computeIfAbsent(threadId, id -> {
			try {
				return createThreadBuffers(id);
			} catch (IOException e) {
				throw new UncheckedIOException("Failed to create thread buffers for thread " + id, e);
			}
		});
		return state.getActiveWriter();
	}

	/**
	 * Swap active/inactive buffers for the thread and flush the old active buffer in the
	 * background.
	 */
	void rotateChunk(long threadId) throws IOException {
		ThreadBufferState state = threadStates.get(threadId);
		if (state == null) {
			throw new IllegalStateException("No buffer state for thread " + threadId);
		}

		LEB128MappedWriter oldActive = state.swapBuffers();
		ChunkRef chunkRef = nextChunkRef(threadId, state);

		flushFutures.removeIf(Future::isDone);

		Future<?> flushFuture = flushExecutor.submit(() -> {
			try {
				flushToFile(oldActive, chunkRef);
				oldActive.reset();
			} catch (IOException e) {
				throw new UncheckedIOException("Failed to flush chunk for thread " + threadId, e);
			}
		});
		flushFutures.add(flushFuture);
		state.setPendingFlush(flushFuture);
	}

	private ChunkRef nextChunkRef(long threadId, ThreadBufferState state) {
		int sequence = state.nextSequence();
		return new ChunkRef(threadId, sequence, tempDir.resolve("chunk-" + threadId + "-" + sequence + ".dat"));
	}

	private void flushToFile(LEB128MappedWriter writer, ChunkRef chunk) throws IOException {
		writer.force();
		try (FileOutputStream fos = new FileOutputStream(chunk.file().toFile())) {
			writer.copyTo(fos);
		}
		flushedChunks.add(chunk);
	}

	/**
	 * @return the flushed chunk files ordered by (threadId, sequence), i.e. the per-thread order in
	 *         which the chunks were filled, not the order in which background flushes completed
	 */
	List<Path> getFlushedChunks() {
		List<ChunkRef> refs = new ArrayList<>(flushedChunks);
		refs.sort(Comparator.naturalOrder());
		List<Path> paths = new ArrayList<>(refs.size());
		for (ChunkRef ref : refs) {
			paths.add(ref.file());
		}
		return paths;
	}

	Path getTempDir() {
		return tempDir;
	}

	/** Force-flushes any active buffers still holding data before close. */
	void finalFlush() throws IOException {
		// wait for outstanding background flushes first: chunks flushed from the active buffers
		// below must be appended to flushedChunks AFTER any earlier rotation of the same thread
		flushExecutor.shutdown();
		try {
			if (!flushExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
				flushExecutor.shutdownNow();
			}
		} catch (InterruptedException e) {
			flushExecutor.shutdownNow();
			Thread.currentThread().interrupt();
		}

		// collect all background flush failures before throwing, rather than losing all but the first
		IOException first = null;
		int failureCount = 0;
		for (Future<?> future : flushFutures) {
			try {
				future.get();
			} catch (ExecutionException e) {
				failureCount++;
				Throwable cause = e.getCause();
				if (first == null) {
					first = cause instanceof IOException ? (IOException) cause
							: new IOException("Background chunk flush failed", cause);
				} else {
					first.addSuppressed(cause);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException("Interrupted while checking background flush results", e);
			} catch (CancellationException e) {
				// task never ran, or was cut off by shutdownNow() after the await timeout
				failureCount++;
				if (first == null) {
					first = new IOException("Background chunk flush was cancelled before completion", e);
				} else {
					first.addSuppressed(e);
				}
			}
		}
		if (first != null) {
			throw new IOException(failureCount + " background chunk flush(es) failed", first);
		}

		// with the background flushes drained, the active buffers hold the last data of their
		// threads; flush them synchronously so their chunks land at the tail of the sequence
		for (ThreadBufferState state : threadStates.values()) {
			LEB128MappedWriter active = state.getActiveWriter();
			if (active.getDataSize() > 0) {
				flushToFile(active, nextChunkRef(state.threadId, state));
			}
		}
	}

	void cleanup() throws IOException {
		for (ThreadBufferState state : threadStates.values()) {
			state.close();
		}

		// best-effort: on Windows a live mapping keeps the file locked until the buffer is GC-ed,
		// so a delete can fail even though the recording was written successfully
		for (ChunkRef chunk : flushedChunks) {
			try {
				Files.deleteIfExists(chunk.file());
			} catch (IOException e) {
				LOGGER.log(Level.FINE, "Failed to delete mmap chunk file " + chunk.file(), e);
			}
		}

		deleteRecursively(tempDir);
	}

	/** Deletes the directory tree rooted at {@code dir}, best-effort. */
	static void deleteRecursively(Path dir) {
		if (!Files.exists(dir)) {
			return;
		}
		try {
			// reverse order so directory entries are deleted after their contents
			List<Path> paths = Files.walk(dir).sorted((a, b) -> b.compareTo(a)).collect(Collectors.toList());
			for (Path path : paths) {
				try {
					Files.deleteIfExists(path);
				} catch (IOException e) {
					LOGGER.log(Level.FINE, "Failed to delete mmap file " + path, e);
				}
			}
		} catch (IOException e) {
			LOGGER.log(Level.FINE, "Failed to walk mmap directory " + dir, e);
		}
	}

	private ThreadBufferState createThreadBuffers(long threadId) throws IOException {
		Path buffer0Path = tempDir.resolve("thread-" + threadId + "-buffer-0.mmap");
		Path buffer1Path = tempDir.resolve("thread-" + threadId + "-buffer-1.mmap");
		LEB128MappedWriter buffer0 = new LEB128MappedWriter(buffer0Path, chunkSize);
		try {
			LEB128MappedWriter buffer1 = new LEB128MappedWriter(buffer1Path, chunkSize);
			return new ThreadBufferState(threadId, buffer0, buffer1);
		} catch (IOException e) {
			buffer0.close();
			throw e;
		}
	}

	/**
	 * Identifies a flushed chunk file; ordering by (threadId, sequence) reproduces the per-thread
	 * write order regardless of the order in which background flushes completed.
	 */
	private static record ChunkRef(long threadId, int sequence, Path file) implements Comparable<ChunkRef> {

	@Override
	public int compareTo(ChunkRef other) {
		int cmp = Long.compare(threadId, other.threadId);
		return cmp != 0 ? cmp : Integer.compare(sequence, other.sequence);
	}
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
	private volatile Future<?> pendingFlush;

	ThreadBufferState(long threadId, LEB128MappedWriter buffer0, LEB128MappedWriter buffer1) {
		this.threadId = threadId;
		this.buffer0 = buffer0;
		this.buffer1 = buffer1;
	}

	LEB128MappedWriter getActiveWriter() {
		return activeIsBuffer0 ? buffer0 : buffer1;
	}

	/**
	 * Swaps active/inactive buffers and returns the old active buffer for flushing. Waits for any
	 * pending flush on the inactive buffer first, since that buffer is about to become active and
	 * must not still be written to by the flush task.
	 */
	synchronized LEB128MappedWriter swapBuffers() throws IOException {
		Future<?> pending = pendingFlush;
		if (pending != null) {
			try {
				pending.get();
			} catch (ExecutionException e) {
				Throwable cause = e.getCause();
				throw cause instanceof IOException ? (IOException) cause
						: new IOException("Pending buffer flush failed", cause);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException("Interrupted while waiting for pending buffer flush", e);
			}
		}
		LEB128MappedWriter oldActive = getActiveWriter();
		activeIsBuffer0 = !activeIsBuffer0;
		return oldActive;
	}

	void setPendingFlush(Future<?> future) {
		this.pendingFlush = future;
	}

	int nextSequence() {
		return sequence.getAndIncrement();
	}

	void close() throws IOException {
		buffer0.close();
		buffer1.close();
	}
}}
