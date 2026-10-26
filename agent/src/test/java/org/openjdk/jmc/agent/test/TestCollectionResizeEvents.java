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
package org.openjdk.jmc.agent.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Vector;

import org.junit.Test;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

/**
 * End-to-end test for the collection resize tracking capability. Runs with the agent attached (see
 * the {@code test-collection-tracking-enabled} failsafe execution) and a {@code minsize} of 128. It
 * drives real {@link HashMap} and {@link ArrayList} resizes under an active flight recording and
 * asserts that the expected {@code jdk.jmc.CollectionResize} events were produced.
 */
public class TestCollectionResizeEvents {

	private static final String EVENT_NAME = "jdk.jmc.CollectionResize"; //$NON-NLS-1$
	private static final int MIN_SIZE = 128;
	private static final int ENTRIES = 5000;

	@Test
	public void testCollectionResizeEvents() throws Exception {
		Path recordingFile = Files.createTempFile("collection-resize", ".jfr"); //$NON-NLS-1$ //$NON-NLS-2$

		HashMap<Integer, Integer> map = new HashMap<>();
		ArrayList<Integer> list = new ArrayList<>();
		Vector<Integer> vector = new Vector<>();
		Hashtable<Integer, Integer> hashtable = new Hashtable<>();
		PriorityQueue<Integer> priorityQueue = new PriorityQueue<>();
		ArrayDeque<Integer> arrayDeque = new ArrayDeque<>();

		long mapId = identityId(map);
		long listId = identityId(list);
		long vectorId = identityId(vector);
		long hashtableId = identityId(hashtable);
		long priorityQueueId = identityId(priorityQueue);
		long arrayDequeId = identityId(arrayDeque);

		try (Recording recording = new Recording()) {
			recording.enable(EVENT_NAME);
			recording.start();
			for (int i = 0; i < ENTRIES; i++) {
				map.put(i, i);
				list.add(i);
				vector.add(i);
				hashtable.put(i, i);
				priorityQueue.add(i);
				arrayDeque.add(i);
			}
			recording.stop();
			recording.dump(recordingFile);
		}

		List<RecordedEvent> events = RecordingFile.readAllEvents(recordingFile);
		Files.deleteIfExists(recordingFile);

		assertResized(events, mapId, "java.util.HashMap"); //$NON-NLS-1$
		assertResized(events, listId, "java.util.ArrayList"); //$NON-NLS-1$
		assertResized(events, vectorId, "java.util.Vector"); //$NON-NLS-1$
		assertResized(events, hashtableId, "java.util.Hashtable"); //$NON-NLS-1$
		assertResized(events, priorityQueueId, "java.util.PriorityQueue"); //$NON-NLS-1$
		assertResized(events, arrayDequeId, "java.util.ArrayDeque"); //$NON-NLS-1$

		// HashMap doubles its capacity - verify the before/after values look right.
		for (RecordedEvent event : eventsFor(events, mapId)) {
			long oldCapacity = event.getLong("oldCapacity"); //$NON-NLS-1$
			if (oldCapacity > 0) {
				assertEquals("HashMap should double its capacity", oldCapacity * 2, event.getLong("newCapacity")); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	private static long identityId(Object collection) {
		return System.identityHashCode(collection) & 0xFFFFFFFFL;
	}

	private static void assertResized(List<RecordedEvent> events, long id, String expectedClassName) {
		List<RecordedEvent> collectionEvents = eventsFor(events, id);
		assertFalse("Expected at least one resize event for " + expectedClassName, collectionEvents.isEmpty()); //$NON-NLS-1$
		for (RecordedEvent event : collectionEvents) {
			assertEquals(expectedClassName, event.getClass("collectionClass").getName()); //$NON-NLS-1$
			assertTrue("Event below the configured threshold was emitted for " + expectedClassName, //$NON-NLS-1$
					event.getLong("size") >= MIN_SIZE); //$NON-NLS-1$
			assertTrue("New capacity should be larger than old capacity for " + expectedClassName, //$NON-NLS-1$
					event.getLong("newCapacity") > event.getLong("oldCapacity")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private static List<RecordedEvent> eventsFor(List<RecordedEvent> events, long id) {
		List<RecordedEvent> result = new ArrayList<>();
		for (RecordedEvent event : events) {
			if (EVENT_NAME.equals(event.getEventType().getName()) && event.getLong("id") == id) { //$NON-NLS-1$
				result.add(event);
			}
		}
		return result;
	}
}
