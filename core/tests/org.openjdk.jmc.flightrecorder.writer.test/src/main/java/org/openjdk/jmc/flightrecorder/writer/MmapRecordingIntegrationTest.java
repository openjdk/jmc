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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jdk.jfr.Event;
import jdk.jfr.Label;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openjdk.jmc.common.item.Attribute;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.writer.api.Recordings;

@SuppressWarnings("restriction")
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

		IItemCollection events = JfrLoaderToolkit.loadEvents(new ByteArrayInputStream(recordingData));
		assertNotNull(events, "Empty mmap recording should be parseable");
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
		int eventCount = 100;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		RecordingImpl recording = (RecordingImpl) Recordings.newRecording(baos,
				settings -> settings.withMmap(512 * 1024).withJdkTypeInitialization());

		for (int i = 0; i < eventCount; i++) {
			TestEvent event = new TestEvent();
			event.message = "Test " + i;
			event.value = i;
			recording.writeEvent(event);
		}

		recording.close();

		IItemCollection events = JfrLoaderToolkit.loadEvents(new ByteArrayInputStream(baos.toByteArray()));
		assertNotNull(events, "Recording should be parseable");

		IAttribute<String> messageAttr = Attribute.attr("message", "message", UnitLookup.PLAIN_TEXT);
		IAttribute<IQuantity> valueAttr = Attribute.attr("value", "value", UnitLookup.NUMBER);

		int parsedCount = 0;
		Set<Integer> seenValues = new HashSet<>();
		for (IItemIterable lane : events) {
			IType<IItem> type = lane.getType();
			if (!type.getIdentifier().equals("TestEvent")) {
				continue;
			}
			var messageAccessor = messageAttr.getAccessor(type);
			var valueAccessor = valueAttr.getAccessor(type);
			assertNotNull(messageAccessor, "message accessor should exist");
			assertNotNull(valueAccessor, "value accessor should exist");

			for (IItem item : lane) {
				String message = messageAccessor.getMember(item);
				int value = (int) valueAccessor.getMember(item).longValue();
				assertEquals("Test " + value, message, "message should match value");
				seenValues.add(value);
				parsedCount++;
			}
		}
		assertEquals(eventCount, parsedCount, "All events should be present");
		for (int i = 0; i < eventCount; i++) {
			assertTrue(seenValues.contains(i), "Missing event with value " + i);
		}
	}

	@Test
	void testMultiThreadedMmapRecording() throws IOException, InterruptedException, CouldNotLoadRecordingException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		RecordingImpl recording = (RecordingImpl) Recordings.newRecording(baos,
				settings -> settings.withMmap(512 * 1024).withJdkTypeInitialization());

		int numThreads = 4;
		int eventsPerThread = 250;
		int totalEvents = numThreads * eventsPerThread;
		CountDownLatch latch = new CountDownLatch(numThreads);
		CyclicBarrier startBarrier = new CyclicBarrier(numThreads);
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);

		for (int t = 0; t < numThreads; t++) {
			executor.submit(() -> {
				try {
					startBarrier.await();
					for (int i = 0; i < eventsPerThread; i++) {
						TestEvent event = new TestEvent();
						event.message = "Thread " + Thread.currentThread().getId();
						event.value = i;
						recording.writeEvent(event);
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					latch.countDown();
				}
			});
		}

		assertTrue(latch.await(10, TimeUnit.SECONDS), "All threads should complete");
		executor.shutdown();
		recording.close();

		IItemCollection events = JfrLoaderToolkit.loadEvents(new ByteArrayInputStream(baos.toByteArray()));
		assertNotNull(events, "Recording should be parseable after concurrent writes");

		IAttribute<IQuantity> valueAttr = Attribute.attr("value", "value", UnitLookup.NUMBER);

		int parsedCount = 0;
		for (IItemIterable lane : events) {
			IType<IItem> type = lane.getType();
			if (!type.getIdentifier().equals("TestEvent")) {
				continue;
			}
			var valueAccessor = valueAttr.getAccessor(type);
			assertNotNull(valueAccessor, "value accessor should exist");

			for (IItem item : lane) {
				int value = (int) valueAccessor.getMember(item).longValue();
				assertTrue(value >= 0 && value < eventsPerThread,
						"value should be in range [0, " + eventsPerThread + "), got " + value);
				parsedCount++;
			}
		}
		assertEquals(totalEvents, parsedCount, "All " + totalEvents + " events should be present, got " + parsedCount);
	}

	@Test
	void testLargeEventsMmapRecording() throws IOException, CouldNotLoadRecordingException {
		int eventCount = 20;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		RecordingImpl recording = (RecordingImpl) Recordings.newRecording(baos,
				settings -> settings.withMmap(512 * 1024).withJdkTypeInitialization());

		StringBuilder largePayload = new StringBuilder();
		for (int i = 0; i < 1000; i++) {
			largePayload.append("Large payload data segment ").append(i).append(". ");
		}
		String payload = largePayload.toString();

		for (int i = 0; i < eventCount; i++) {
			LargeEvent event = new LargeEvent();
			event.payload = payload + " Event " + i;
			recording.writeEvent(event);
		}

		recording.close();

		byte[] recordingData = baos.toByteArray();
		assertTrue(recordingData.length > 512 * 1024, "Recording should be larger than one chunk");

		IItemCollection events = JfrLoaderToolkit.loadEvents(new ByteArrayInputStream(recordingData));
		assertNotNull(events, "Recording should be parseable after chunk rotation");

		IAttribute<String> payloadAttr = Attribute.attr("payload", "payload", UnitLookup.PLAIN_TEXT);

		int parsedCount = 0;
		Set<String> seenSuffixes = new HashSet<>();
		for (IItemIterable lane : events) {
			IType<IItem> type = lane.getType();
			if (!type.getIdentifier().equals("LargeEvent")) {
				continue;
			}
			var payloadAccessor = payloadAttr.getAccessor(type);
			assertNotNull(payloadAccessor, "payload accessor should exist");

			for (IItem item : lane) {
				String p = payloadAccessor.getMember(item);
				assertNotNull(p, "payload should not be null");
				assertTrue(p.startsWith("Large payload data segment 0."), "payload should start with expected prefix");
				// Extract the " Event N" suffix
				int idx = p.lastIndexOf(" Event ");
				assertTrue(idx > 0, "payload should contain ' Event ' suffix");
				seenSuffixes.add(p.substring(idx));
				parsedCount++;
			}
		}
		assertEquals(eventCount, parsedCount, "All large events should be present");
		for (int i = 0; i < eventCount; i++) {
			assertTrue(seenSuffixes.contains(" Event " + i), "Missing large event " + i);
		}
	}

	@Test
	void testMmapRecordingToFile() throws IOException, CouldNotLoadRecordingException {
		int eventCount = 500;
		Path outputFile = tempDir.resolve("test-recording.jfr");
		FileOutputStream fos = new FileOutputStream(outputFile.toFile());
		RecordingImpl recording = (RecordingImpl) Recordings.newRecording(fos,
				settings -> settings.withMmap(512 * 1024).withJdkTypeInitialization());

		for (int i = 0; i < eventCount; i++) {
			TestEvent event = new TestEvent();
			event.message = "File test";
			event.value = i;
			recording.writeEvent(event);
		}

		recording.close();

		assertTrue(Files.exists(outputFile), "Output file should exist");
		assertTrue(Files.size(outputFile) > 0, "Output file should not be empty");

		IItemCollection events = JfrLoaderToolkit.loadEvents(outputFile.toFile());
		assertNotNull(events, "Recording file should be parseable");

		IAttribute<String> messageAttr = Attribute.attr("message", "message", UnitLookup.PLAIN_TEXT);
		IAttribute<IQuantity> valueAttr = Attribute.attr("value", "value", UnitLookup.NUMBER);

		int parsedCount = 0;
		for (IItemIterable lane : events) {
			IType<IItem> type = lane.getType();
			if (!type.getIdentifier().equals("TestEvent")) {
				continue;
			}
			var messageAccessor = messageAttr.getAccessor(type);
			var valueAccessor = valueAttr.getAccessor(type);

			for (IItem item : lane) {
				assertEquals("File test", messageAccessor.getMember(item), "message should be 'File test'");
				int value = (int) valueAccessor.getMember(item).longValue();
				assertTrue(value >= 0 && value < eventCount, "value should be in range [0, " + eventCount + ")");
				parsedCount++;
			}
		}
		assertEquals(eventCount, parsedCount, "All events should be present in the file");
	}

	@Test
	void testComparisonMmapVsHeap() throws IOException, CouldNotLoadRecordingException {
		int numEvents = 1000;

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

		IAttribute<String> messageAttr = Attribute.attr("message", "message", UnitLookup.PLAIN_TEXT);
		IAttribute<IQuantity> valueAttr = Attribute.attr("value", "value", UnitLookup.NUMBER);

		// Verify both recordings have the same event content
		int mmapCount = countAndVerifyEvents(mmapBaos.toByteArray(), messageAttr, valueAttr, numEvents);
		int heapCount = countAndVerifyEvents(heapBaos.toByteArray(), messageAttr, valueAttr, numEvents);
		assertEquals(numEvents, mmapCount, "Mmap recording should have all events");
		assertEquals(numEvents, heapCount, "Heap recording should have all events");

		// Sizes should be reasonably similar (within 10%)
		double ratio = (double) mmapBaos.size() / heapBaos.size();
		assertTrue(ratio > 0.9 && ratio < 1.1,
				"Mmap and heap recordings should have similar sizes, got ratio: " + ratio);
	}

	private int countAndVerifyEvents(
		byte[] recordingData, IAttribute<String> messageAttr, IAttribute<IQuantity> valueAttr, int expectedMax)
			throws IOException, CouldNotLoadRecordingException {
		IItemCollection events = JfrLoaderToolkit.loadEvents(new ByteArrayInputStream(recordingData));
		int count = 0;
		Set<Integer> seenValues = new HashSet<>();
		for (IItemIterable lane : events) {
			IType<IItem> type = lane.getType();
			if (!type.getIdentifier().equals("TestEvent")) {
				continue;
			}
			var messageAccessor = messageAttr.getAccessor(type);
			var valueAccessor = valueAttr.getAccessor(type);

			for (IItem item : lane) {
				assertEquals("Compare test", messageAccessor.getMember(item));
				int value = (int) valueAccessor.getMember(item).longValue();
				assertTrue(value >= 0 && value < expectedMax);
				seenValues.add(value);
				count++;
			}
		}
		assertEquals(expectedMax, seenValues.size(), "All distinct values should be present");
		return count;
	}
}
