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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ThreadMmapManagerTest {
	@TempDir
	Path tempDir;

	private ThreadMmapManager manager;
	private Path mmapDir;

	@BeforeEach
	void setup() throws IOException {
		mmapDir = tempDir.resolve("mmap-test");
		manager = new ThreadMmapManager(mmapDir, 1024 * 1024); // 1MB chunks
	}

	@AfterEach
	void cleanup() throws IOException {
		if (manager != null) {
			manager.cleanup();
		}
	}

	@Test
	void testGetActiveWriter() throws IOException {
		long threadId = Thread.currentThread().getId();
		LEB128MappedWriter writer = manager.getActiveWriter(threadId);

		assertNotNull(writer);
		assertEquals(1024 * 1024, writer.capacity());
	}

	@Test
	void testGetActiveWriterTwice() throws IOException {
		long threadId = Thread.currentThread().getId();
		LEB128MappedWriter writer1 = manager.getActiveWriter(threadId);
		LEB128MappedWriter writer2 = manager.getActiveWriter(threadId);

		// Should return the same writer instance
		assertEquals(writer1, writer2);
	}

	@Test
	void testMultipleThreads() throws IOException {
		long thread1 = 100L;
		long thread2 = 200L;

		LEB128MappedWriter writer1 = manager.getActiveWriter(thread1);
		LEB128MappedWriter writer2 = manager.getActiveWriter(thread2);

		assertNotNull(writer1);
		assertNotNull(writer2);
		// Different threads get different writers
		assertTrue(writer1 != writer2);
	}

	@Test
	void testRotateChunk() throws IOException, InterruptedException {
		long threadId = Thread.currentThread().getId();
		LEB128MappedWriter writer = manager.getActiveWriter(threadId);

		// Write some data
		writer.writeByte((byte) 1);
		writer.writeByte((byte) 2);
		writer.writeByte((byte) 3);

		// Rotate
		manager.rotateChunk(threadId);

		// Give background flush time to complete
		Thread.sleep(100);

		// Should have flushed chunks
		List<Path> flushed = manager.getFlushedChunks();
		assertFalse(flushed.isEmpty());
	}

	@Test
	void testRotateChunkSwapsBuffers() throws IOException {
		long threadId = Thread.currentThread().getId();
		LEB128MappedWriter writer1 = manager.getActiveWriter(threadId);

		// Write to first buffer
		writer1.writeByte((byte) 1);

		// Rotate - should swap to second buffer
		manager.rotateChunk(threadId);

		// Get active writer again (should be the other buffer)
		LEB128MappedWriter writer2 = manager.getActiveWriter(threadId);

		// Should still work (different buffer)
		writer2.writeByte((byte) 2);
		assertEquals(1, writer2.position());
	}

	@Test
	void testMultipleRotations() throws IOException, InterruptedException {
		long threadId = Thread.currentThread().getId();
		LEB128MappedWriter writer = manager.getActiveWriter(threadId);

		// First rotation
		writer.writeByte((byte) 1);
		manager.rotateChunk(threadId);

		// Second rotation
		writer = manager.getActiveWriter(threadId);
		writer.writeByte((byte) 2);
		manager.rotateChunk(threadId);

		// Third rotation
		writer = manager.getActiveWriter(threadId);
		writer.writeByte((byte) 3);
		manager.rotateChunk(threadId);

		// Give background flushes time to complete
		Thread.sleep(200);

		// Should have multiple flushed chunks
		List<Path> flushed = manager.getFlushedChunks();
		assertTrue(flushed.size() >= 3);
	}

	@Test
	void testFinalFlush() throws IOException {
		long threadId = Thread.currentThread().getId();
		LEB128MappedWriter writer = manager.getActiveWriter(threadId);

		// Write data without rotating
		writer.writeByte((byte) 42);

		// Final flush should flush active buffer
		manager.finalFlush();

		List<Path> flushed = manager.getFlushedChunks();
		assertFalse(flushed.isEmpty());
	}

	@Test
	void testFinalFlushEmptyBuffer() throws IOException {
		long threadId = Thread.currentThread().getId();
		manager.getActiveWriter(threadId);

		// Don't write anything
		manager.finalFlush();

		// Should not create chunk file for empty buffer
		List<Path> flushed = manager.getFlushedChunks();
		assertTrue(flushed.isEmpty());
	}

	@Test
	void testFinalFlushMultipleThreads() throws IOException {
		long thread1 = 100L;
		long thread2 = 200L;

		LEB128MappedWriter writer1 = manager.getActiveWriter(thread1);
		LEB128MappedWriter writer2 = manager.getActiveWriter(thread2);

		writer1.writeByte((byte) 1);
		writer2.writeByte((byte) 2);

		manager.finalFlush();

		List<Path> flushed = manager.getFlushedChunks();
		assertEquals(2, flushed.size());
	}

	@Test
	void testCleanup() throws IOException {
		long threadId = Thread.currentThread().getId();
		LEB128MappedWriter writer = manager.getActiveWriter(threadId);

		writer.writeByte((byte) 1);
		manager.rotateChunk(threadId);

		// Wait for flush
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		List<Path> flushed = manager.getFlushedChunks();
		assertFalse(flushed.isEmpty());

		// Cleanup should delete files
		manager.cleanup();

		// Files should be deleted
		for (Path path : flushed) {
			assertFalse(Files.exists(path));
		}
	}

	@Test
	void testFlushedChunkFilesExist() throws IOException, InterruptedException {
		long threadId = Thread.currentThread().getId();
		LEB128MappedWriter writer = manager.getActiveWriter(threadId);

		writer.writeByte((byte) 42);
		manager.rotateChunk(threadId);

		// Wait for background flush
		Thread.sleep(200);

		List<Path> flushed = manager.getFlushedChunks();
		assertFalse(flushed.isEmpty());

		// Check files actually exist
		for (Path path : flushed) {
			assertTrue(Files.exists(path), "Flushed chunk file should exist: " + path);
			assertTrue(Files.size(path) > 0, "Flushed chunk file should not be empty");
		}
	}

	@Test
	void testSequenceNumbering() throws IOException, InterruptedException {
		long threadId = Thread.currentThread().getId();

		// Perform multiple rotations
		for (int i = 0; i < 3; i++) {
			LEB128MappedWriter writer = manager.getActiveWriter(threadId);
			writer.writeByte((byte) i);
			manager.rotateChunk(threadId);
		}

		Thread.sleep(300);

		List<Path> flushed = manager.getFlushedChunks();
		assertEquals(3, flushed.size());

		// Files should have sequence numbers in names
		for (Path path : flushed) {
			assertTrue(path.getFileName().toString().contains("chunk-" + threadId));
		}
	}

	@Test
	void testConcurrentAccess() throws Exception {
		Thread thread1 = new Thread(() -> {
			try {
				long threadId = Thread.currentThread().getId();
				LEB128MappedWriter writer = manager.getActiveWriter(threadId);
				for (int i = 0; i < 10; i++) {
					writer.writeByte((byte) i);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});

		Thread thread2 = new Thread(() -> {
			try {
				long threadId = Thread.currentThread().getId();
				LEB128MappedWriter writer = manager.getActiveWriter(threadId);
				for (int i = 0; i < 10; i++) {
					writer.writeByte((byte) (i + 10));
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});

		thread1.start();
		thread2.start();
		thread1.join();
		thread2.join();

		manager.finalFlush();

		// Both threads should have produced data
		List<Path> flushed = manager.getFlushedChunks();
		assertEquals(2, flushed.size());
	}
}
