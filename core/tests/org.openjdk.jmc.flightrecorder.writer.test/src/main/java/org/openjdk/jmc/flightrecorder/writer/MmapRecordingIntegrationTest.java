/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import jdk.jfr.Event;
import jdk.jfr.Label;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openjdk.jmc.flightrecorder.writer.api.Recordings;

class MmapRecordingIntegrationTest {
	@Label("Test Event")
	public static class TestEvent extends Event {
		@Label("message")
		public String message;
		@Label("value")
		public int value;
	}

	@Label("Large Event")
	public static class LargeEvent extends Event {
		@Label("payload")
		public String payload;
	}

	@TempDir
	Path tempDir;

	@Test
	void testZeroEventMmapRecording() throws IOException, CouldNotLoadRecordingException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		RecordingImpl recording = (RecordingImpl) Recordings.newRecording(baos,
				settings -> settings.withMmap(512 * 1024).withJdkTypeInitialization());

		recording.close();

		byte[] recordingData = baos.toByteArray();
		assertTrue(recordingData.length > 0, "Empty recording should still produce JFR output");
		assertNotNull(JfrLoaderToolkit.loadEvents(new ByteArrayInputStream(recordingData)),
				"Empty mmap recording should be parseable");
	}

	@Test
	void testWriteAfterClosedThrows() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		RecordingImpl recording = (RecordingImpl) Recordings.newRecording(baos,
				settings -> settings.withMmap(512 * 1024).withJdkTypeInitialization());

		recording.close();

		TestEvent event = new TestEvent();
		assertThrows(IllegalStateException.class, () -> recording.writeEvent(event));
	}

	@Test
	void testBasicMmapRecording() throws IOException, CouldNotLoadRecordingException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		RecordingImpl recording = (RecordingImpl) Recordings.newRecording(baos,
				settings -> settings.withMmap(512 * 1024).withJdkTypeInitialization());

		// Write some events
		for (int i = 0; i < 100; i++) {
			TestEvent event = new TestEvent();
			event.message = "Test " + i;
			event.value = i;
			recording.writeEvent(event);
		}

		recording.close();

		byte[] recordingData = baos.toByteArray();
		assertTrue(recordingData.length > 0, "Recording should contain data");

		// Verify JFR magic bytes
		assertEquals('F', recordingData[0]);
		assertEquals('L', recordingData[1]);
		assertEquals('R', recordingData[2]);
		assertEquals(0, recordingData[3]);

		// Verify the recording is parseable
		assertNotNull(JfrLoaderToolkit.loadEvents(new ByteArrayInputStream(recordingData)),
				"Recording should be parseable");
	}

	@Test
	void testMultiThreadedMmapRecording() throws IOException, InterruptedException, CouldNotLoadRecordingException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		RecordingImpl recording = (RecordingImpl) Recordings.newRecording(baos,
				settings -> settings.withMmap(512 * 1024).withJdkTypeInitialization());

		int numThreads = 4;
		int eventsPerThread = 250; // Total 1000 events
		CountDownLatch latch = new CountDownLatch(numThreads);
		CyclicBarrier startBarrier = new CyclicBarrier(numThreads);
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);

		for (int t = 0; t < numThreads; t++) {
			executor.submit(() -> {
				try {
					startBarrier.await(); // Start all threads simultaneously for better race coverage
					for (int i = 0; i < eventsPerThread; i++) {
						TestEvent event = new TestEvent();
						event.message = "Thread " + Thread.currentThread().getId();
						event.value = i;
						recording.writeEvent(event);
					}
				} catch (BrokenBarrierException e) {
					throw new RuntimeException(e);
				} finally {
					latch.countDown();
				}
			});
		}

		assertTrue(latch.await(10, TimeUnit.SECONDS), "All threads should complete");
		executor.shutdown();
		recording.close();

		byte[] recordingData = baos.toByteArray();
		assertTrue(recordingData.length > 0, "Recording should contain data");

		// Verify the recording is parseable (concurrent writes must not corrupt structure)
		assertNotNull(JfrLoaderToolkit.loadEvents(new ByteArrayInputStream(recordingData)),
				"Recording should be parseable after concurrent writes");
	}

	@Test
	void testLargeEventsMmapRecording() throws IOException, CouldNotLoadRecordingException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		RecordingImpl recording = (RecordingImpl) Recordings.newRecording(baos,
				settings -> settings.withMmap(512 * 1024).withJdkTypeInitialization());

		// Create large events to trigger rotation
		StringBuilder largePayload = new StringBuilder();
		for (int i = 0; i < 1000; i++) {
			largePayload.append("Large payload data segment ").append(i).append(". ");
		}
		String payload = largePayload.toString();

		// Write enough large events to exceed one chunk (512KB)
		for (int i = 0; i < 20; i++) {
			LargeEvent event = new LargeEvent();
			event.payload = payload + " Event " + i;
			recording.writeEvent(event);
		}

		recording.close();

		byte[] recordingData = baos.toByteArray();
		assertTrue(recordingData.length > 512 * 1024, "Recording should be larger than one chunk");

		// Verify chunk rotation did not corrupt the recording
		assertNotNull(JfrLoaderToolkit.loadEvents(new ByteArrayInputStream(recordingData)),
				"Recording should be parseable after chunk rotation");
	}

	@Test
	void testMmapRecordingToFile() throws IOException, CouldNotLoadRecordingException {
		Path outputFile = tempDir.resolve("test-recording.jfr");
		FileOutputStream fos = new FileOutputStream(outputFile.toFile());
		RecordingImpl recording = (RecordingImpl) Recordings.newRecording(fos,
				settings -> settings.withMmap(512 * 1024).withJdkTypeInitialization());

		for (int i = 0; i < 500; i++) {
			TestEvent event = new TestEvent();
			event.message = "File test";
			event.value = i;
			recording.writeEvent(event);
		}

		recording.close();

		assertTrue(Files.exists(outputFile), "Output file should exist");
		assertTrue(Files.size(outputFile) > 0, "Output file should not be empty");

		// Verify file is valid JFR
		byte[] header = new byte[4];
		try (InputStream is = Files.newInputStream(outputFile)) {
			is.read(header);
		}
		assertEquals('F', header[0]);
		assertEquals('L', header[1]);
		assertEquals('R', header[2]);
		assertEquals(0, header[3]);

		// Verify the file is parseable
		assertNotNull(JfrLoaderToolkit.loadEvents(outputFile.toFile()), "Recording file should be parseable");
	}

	@Test
	void testComparisonMmapVsHeap() throws IOException, CouldNotLoadRecordingException {
		int numEvents = 1000;

		// Test with mmap
		ByteArrayOutputStream mmapBaos = new ByteArrayOutputStream();
		RecordingImpl mmapRecording = (RecordingImpl) Recordings.newRecording(mmapBaos,
				settings -> settings.withMmap(512 * 1024).withJdkTypeInitialization());

		for (int i = 0; i < numEvents; i++) {
			TestEvent event = new TestEvent();
			event.message = "Compare test";
			event.value = i;
			mmapRecording.writeEvent(event);
		}
		mmapRecording.close();

		// Test with heap (no withMmap call)
		ByteArrayOutputStream heapBaos = new ByteArrayOutputStream();
		RecordingImpl heapRecording = (RecordingImpl) Recordings.newRecording(heapBaos,
				settings -> settings.withJdkTypeInitialization());

		for (int i = 0; i < numEvents; i++) {
			TestEvent event = new TestEvent();
			event.message = "Compare test";
			event.value = i;
			heapRecording.writeEvent(event);
		}
		heapRecording.close();

		// Both should produce parseable JFR files
		assertNotNull(JfrLoaderToolkit.loadEvents(new ByteArrayInputStream(mmapBaos.toByteArray())),
				"Mmap recording should be parseable");
		assertNotNull(JfrLoaderToolkit.loadEvents(new ByteArrayInputStream(heapBaos.toByteArray())),
				"Heap recording should be parseable");

		// Sizes should be reasonably similar (within 10%)
		double mmapSize = mmapBaos.size();
		double heapSize = heapBaos.size();
		double ratio = mmapSize / heapSize;
		assertTrue(ratio > 0.9 && ratio < 1.1,
				"Mmap and heap recordings should have similar sizes, got ratio: " + ratio);
	}
}
