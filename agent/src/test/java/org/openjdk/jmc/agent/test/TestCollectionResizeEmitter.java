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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openjdk.jmc.agent.TransformRegistry;
import org.openjdk.jmc.agent.collections.CollectionResizeEmitter;
import org.openjdk.jmc.agent.collections.CollectionResizeEvent;
import org.openjdk.jmc.agent.impl.DefaultTransformRegistry;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

/**
 * Exercises threshold updates with a reflective bridge stub, without defining or replacing the
 * bootstrap bridge used by other tests.
 */
@SuppressWarnings("nls")
public class TestCollectionResizeEmitter {

	private static final int ORIGINAL_MIN_SIZE = 1024;
	private static long bridgeMinSize;
	private static int bridgeUpdateCount;
	private static int emitterMinSizeDuringBridgeUpdate;
	private static RuntimeException bridgeFailure;

	private Field minSizeField;
	private Field initializedField;
	private Field bridgeSetterField;
	private int savedMinSize;
	private boolean savedInitialized;
	private Method savedBridgeSetter;

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Before
	public void installBridgeStub() throws Exception {
		minSizeField = emitterField("minSize");
		initializedField = emitterField("initialized");
		bridgeSetterField = emitterField("bridgeSetMinSize");
		synchronized (CollectionResizeEmitter.class) {
			savedMinSize = minSizeField.getInt(null);
			savedInitialized = initializedField.getBoolean(null);
			savedBridgeSetter = (Method) bridgeSetterField.get(null);
			minSizeField.setInt(null, ORIGINAL_MIN_SIZE);
			initializedField.setBoolean(null, true);
			bridgeSetterField.set(null, TestCollectionResizeEmitter.class.getMethod("setBridgeMinSize", long.class));
		}
		bridgeMinSize = ORIGINAL_MIN_SIZE;
		bridgeUpdateCount = 0;
		emitterMinSizeDuringBridgeUpdate = -1;
		bridgeFailure = null;
	}

	@After
	public void restoreEmitterState() throws Exception {
		synchronized (CollectionResizeEmitter.class) {
			minSizeField.setInt(null, savedMinSize);
			initializedField.setBoolean(null, savedInitialized);
			bridgeSetterField.set(null, savedBridgeSetter);
		}
		bridgeFailure = null;
	}

	@Test
	public void testFailedDecreasePreservesThresholdAndEventFiltering() throws Exception {
		bridgeFailure = new IllegalStateException("Simulated bridge update failure");
		ReflectiveOperationException failure = assertThrows(ReflectiveOperationException.class,
				() -> CollectionResizeEmitter.setMinSize(128));
		assertSame(bridgeFailure, failure.getCause());
		assertEquals(ORIGINAL_MIN_SIZE, bridgeMinSize);
		assertEquals(ORIGINAL_MIN_SIZE, CollectionResizeEmitter.getMinSize());
		assertTrue(CollectionResizeEmitter.isInstalled());

		Path recordingFile = temporaryFolder.newFile("failed-retune.jfr").toPath();
		Object collection = new Object();
		try (Recording recording = new Recording()) {
			recording.enable(CollectionResizeEvent.class);
			recording.start();
			CollectionResizeEmitter.onResize(collection, 512, new Object[16], new Object[32]);
			CollectionResizeEmitter.onResize(collection, ORIGINAL_MIN_SIZE, new Object[32], new Object[64]);
			recording.stop();
			recording.dump(recordingFile);
		}
		long collectionId = System.identityHashCode(collection) & 0xFFFFFFFFL;
		List<RecordedEvent> events = RecordingFile.readAllEvents(recordingFile).stream()
				.filter(event -> "jdk.jmc.CollectionResize".equals(event.getEventType().getName())
						&& event.getLong("id") == collectionId)
				.toList();
		assertEquals(1, events.size());
		assertEquals(ORIGINAL_MIN_SIZE, events.get(0).getLong("size"));
	}

	@Test
	public void testNoGrowthDoesNotRetainReentrancyGuard() throws Exception {
		ThreadLocal<?> guard = (ThreadLocal<?>) emitterField("IN_EMIT").get(null);
		guard.remove();
		try {
			Path recordingFile = temporaryFolder.newFile("no-growth.jfr").toPath();
			Object collection = new Object();
			Object[] backing = new Object[ORIGINAL_MIN_SIZE];
			Object[] largerBacking = new Object[ORIGINAL_MIN_SIZE * 2];
			try (Recording recording = new Recording()) {
				recording.enable(CollectionResizeEvent.class);
				recording.start();
				assertTrue("The recording must enable the emitter's JFR path",
						new CollectionResizeEvent().shouldCommit());
				CollectionResizeEmitter.onResize(collection, ORIGINAL_MIN_SIZE, backing, backing);
				assertFalse("The same backing array must not retain the guard", hasThreadLocalEntry(guard));
				CollectionResizeEmitter.onResize(collection, ORIGINAL_MIN_SIZE, largerBacking, backing);
				assertFalse("Shrinking must not retain the guard", hasThreadLocalEntry(guard));
				CollectionResizeEmitter.onResize(collection, ORIGINAL_MIN_SIZE, null, null);
				assertFalse("Null backing arrays must not retain the guard", hasThreadLocalEntry(guard));
				CollectionResizeEmitter.onResize(collection, ORIGINAL_MIN_SIZE, backing, largerBacking);
				assertFalse("Emitting a growth event must remove the guard", hasThreadLocalEntry(guard));
				recording.stop();
				recording.dump(recordingFile);
			}
			long collectionId = System.identityHashCode(collection) & 0xFFFFFFFFL;
			List<RecordedEvent> events = RecordingFile.readAllEvents(recordingFile).stream()
					.filter(event -> "jdk.jmc.CollectionResize".equals(event.getEventType().getName())
							&& event.getLong("id") == collectionId)
					.toList();
			assertEquals(1, events.size());
			assertEquals(ORIGINAL_MIN_SIZE, events.get(0).getLong("oldCapacity"));
			assertEquals(ORIGINAL_MIN_SIZE * 2, events.get(0).getLong("newCapacity"));
		} finally {
			guard.remove();
		}
	}

	@Test
	public void testSuccessfulRetunesPublishEmitterThresholdAfterBridgeUpdate() throws Exception {
		CollectionResizeEmitter.installOrRetune(Integer.valueOf(128));
		assertEquals(ORIGINAL_MIN_SIZE, emitterMinSizeDuringBridgeUpdate);
		assertEquals(128, bridgeMinSize);
		assertEquals(128, CollectionResizeEmitter.getMinSize());

		CollectionResizeEmitter.setMinSize(2048);
		assertEquals(128, emitterMinSizeDuringBridgeUpdate);
		assertEquals(2048, bridgeMinSize);
		assertEquals(2048, CollectionResizeEmitter.getMinSize());
		assertEquals(2, bridgeUpdateCount);
	}

	@Test
	public void testOmittedMinimumSizeDoesNotRetuneInstalledEmitter() throws Exception {
		bridgeFailure = new IllegalStateException("The bridge must not be called without an explicit threshold");
		CollectionResizeEmitter.installOrRetune(null);
		assertEquals(0, bridgeUpdateCount);
		assertEquals(ORIGINAL_MIN_SIZE, bridgeMinSize);
		assertEquals(ORIGINAL_MIN_SIZE, CollectionResizeEmitter.getMinSize());
	}

	@Test
	public void testMissingBridgeRejectsThresholdUpdate() throws Exception {
		bridgeSetterField.set(null, null);
		assertThrows(IllegalStateException.class, () -> CollectionResizeEmitter.setMinSize(128));
		assertEquals(ORIGINAL_MIN_SIZE, CollectionResizeEmitter.getMinSize());
		assertEquals(ORIGINAL_MIN_SIZE, bridgeMinSize);
		assertEquals(0, bridgeUpdateCount);
	}

	@Test
	public void testFailedRetuneKeepsRegistrySettingsAndReadBack() throws Exception {
		TransformRegistry registry = fromXml("<jfragent><collectiontracking minsize=\"1024\"/></jfragent>");
		bridgeFailure = new IllegalStateException("Simulated bridge update failure");
		registry.modify("<jfragent><collectiontracking minsize=\"128\"/></jfragent>");
		assertTrue(registry.getCollectionTrackingSettings().isEnabled());
		assertEquals(ORIGINAL_MIN_SIZE, registry.getCollectionTrackingSettings().getMinSize());
		assertTrue(registry.getCurrentConfiguration().contains("minsize=\"1024\""));
		assertEquals(ORIGINAL_MIN_SIZE, bridgeMinSize);
	}

	@Test
	public void testFailedReenableKeepsRegistryDisabled() throws Exception {
		TransformRegistry registry = fromXml("<jfragent/>");
		bridgeFailure = new IllegalStateException("Simulated bridge update failure");
		registry.modify("<jfragent><collectiontracking minsize=\"128\"/></jfragent>");
		assertFalse(registry.getCollectionTrackingSettings().isEnabled());
		assertTrue(registry.getTransformData("java/util/HashMap").isEmpty());
		assertTrue(registry.getTransformData("java/util/ArrayList").isEmpty());
		assertTrue(registry.getCurrentConfiguration().contains("enabled=\"false\""));
		assertEquals(ORIGINAL_MIN_SIZE, CollectionResizeEmitter.getMinSize());
		assertEquals(ORIGINAL_MIN_SIZE, bridgeMinSize);
	}

	public static void setBridgeMinSize(long newMinSize) {
		bridgeUpdateCount++;
		emitterMinSizeDuringBridgeUpdate = CollectionResizeEmitter.getMinSize();
		if (bridgeFailure != null) {
			throw bridgeFailure;
		}
		bridgeMinSize = newMinSize;
	}

	private static Field emitterField(String name) throws Exception {
		Field field = CollectionResizeEmitter.class.getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}

	private static boolean hasThreadLocalEntry(ThreadLocal<?> key) throws Exception {
		Field threadLocals = Thread.class.getDeclaredField("threadLocals");
		threadLocals.setAccessible(true);
		Object map = threadLocals.get(Thread.currentThread());
		if (map == null) {
			return false;
		}
		Field table = map.getClass().getDeclaredField("table");
		table.setAccessible(true);
		for (Object entry : (Object[]) table.get(map)) {
			if (entry != null && ((Reference<?>) entry).get() == key) {
				return true;
			}
		}
		return false;
	}

	private static TransformRegistry fromXml(String xml) throws Exception {
		return DefaultTransformRegistry.from(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
	}
}
