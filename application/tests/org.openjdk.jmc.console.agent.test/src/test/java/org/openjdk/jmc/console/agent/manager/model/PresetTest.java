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
package org.openjdk.jmc.console.agent.manager.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openjdk.jmc.console.agent.manager.model.IPreset.CollectionTrackingMode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@SuppressWarnings("nls")
public class PresetTest {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void testEnabledSettingsRoundTrip() throws Exception {
		IPreset preset = presetWithTracking("enabled=\"true\" minsize=\"1024\"");
		preset.setClassPrefix("CollectionProbe");
		preset.setAllowToString(true);
		preset.setAllowConverter(true);
		IEvent event = preset.createEvent();
		event.setClazz("example.Application");
		event.setMethodName("run");
		event.setMethodDescriptor("()V");
		preset.addEvent(event);

		IPreset restored = roundTrip(preset);
		assertTracking(restored.serialize(), "true", "1024");
		assertEquals("CollectionProbe", restored.getClassPrefix());
		assertTrue(restored.getAllowToString());
		assertTrue(restored.getAllowConverter());
		assertEquals(1, restored.getEvents().length);
		IEvent restoredEvent = restored.getEvents()[0];
		assertEquals(event.getId(), restoredEvent.getId());
		assertEquals("example.Application", restoredEvent.getClazz());
		assertEquals("run", restoredEvent.getMethodName());
		assertEquals("()V", restoredEvent.getMethodDescriptor());
	}

	@Test
	public void testDisabledSettingsRoundTrip() throws Exception {
		IPreset preset = presetWithTracking("enabled=\"false\" minsize=\"256\"");
		assertTracking(roundTrip(preset).serialize(), "false", "256");
	}

	@Test
	public void testOmittedAttributesRemainOmitted() throws Exception {
		assertTracking(roundTrip(presetWithTracking("")).serialize(), null, null);
		assertTracking(roundTrip(presetWithTracking("enabled=\"true\"")).serialize(), "true", null);
		assertTracking(roundTrip(presetWithTracking("minsize=\"512\"")).serialize(), null, "512");
	}

	@Test
	public void testAbsentTrackingClearsPreviousState() throws Exception {
		IPreset preset = new PresetRepository().createPreset();
		preset.deserialize("<jfragent/>");
		assertFalse(preset.serialize().contains("collectiontracking"));
		preset.deserialize("<jfragent><collectiontracking minsize=\"512\"/></jfragent>");
		preset.deserialize("<jfragent/>");
		assertFalse(roundTrip(preset).serialize().contains("collectiontracking"));
	}

	@Test
	public void testCollectionTrackingDisabledWhenAbsentOrExplicitlyDisabled() throws Exception {
		IPreset preset = new PresetRepository().createPreset();
		assertFalse(preset.isCollectionTrackingEnabled());
		preset.deserialize("<jfragent/>");
		assertFalse(preset.isCollectionTrackingEnabled());
		for (String attributes : new String[] {"enabled=\"false\"", "enabled=\"0\"", "enabled=\"  false  \"",
				"enabled=\"  0  \" minsize=\"1024\""}) {
			assertFalse(attributes, presetWithTracking(attributes).isCollectionTrackingEnabled());
		}
	}

	@Test
	public void testCollectionTrackingEnabledWithoutProbeEvents() throws Exception {
		for (String attributes : new String[] {"", "minsize=\"1024\"", "enabled=\"true\"", "enabled=\"1\"",
				"enabled=\"  true  \"", "enabled=\"  1  \""}) {
			IPreset preset = presetWithTracking(attributes);
			assertEquals(0, preset.getEvents().length);
			assertTrue(attributes, preset.isCollectionTrackingEnabled());
		}
	}

	@Test
	public void testCollectionTrackingEnabledStateSurvivesCopiesAndRoundTrip() throws Exception {
		IPreset enabled = presetWithTracking("minsize=\"1024\"");
		assertTrue(roundTrip(enabled).isCollectionTrackingEnabled());
		assertTrue(enabled.createWorkingCopy().isCollectionTrackingEnabled());
		assertTrue(enabled.createDuplicate().isCollectionTrackingEnabled());

		IPreset disabled = presetWithTracking("enabled=\"0\"");
		assertFalse(roundTrip(disabled).isCollectionTrackingEnabled());
		assertFalse(disabled.createWorkingCopy().isCollectionTrackingEnabled());
		assertFalse(disabled.createDuplicate().isCollectionTrackingEnabled());
	}

	@Test
	public void testCollectionTrackingStateCanBeEnabledDisabledOrUnchanged() throws Exception {
		IPreset preset = new PresetRepository().createPreset();
		assertEquals(CollectionTrackingMode.UNCHANGED, preset.getCollectionTrackingMode());
		preset.setCollectionTrackingMode(CollectionTrackingMode.ENABLED);
		assertEquals(CollectionTrackingMode.ENABLED, preset.getCollectionTrackingMode());
		assertTracking(preset.serialize(), "true", null);
		preset.setCollectionTrackingMinSize(BigInteger.valueOf(512));
		preset.setCollectionTrackingMode(CollectionTrackingMode.DISABLED);
		assertEquals(CollectionTrackingMode.DISABLED, preset.getCollectionTrackingMode());
		assertTracking(preset.serialize(), "false", "512");
		preset.setCollectionTrackingMode(CollectionTrackingMode.ENABLED);
		assertTracking(preset.serialize(), "true", "512");
		preset.setCollectionTrackingMode(CollectionTrackingMode.UNCHANGED);
		assertEquals(CollectionTrackingMode.UNCHANGED, preset.getCollectionTrackingMode());
		assertNull(preset.getCollectionTrackingMinSize());
		assertFalse(preset.isCollectionTrackingEnabled());
		assertFalse(roundTrip(preset).serialize().contains("collectiontracking"));
	}

	@Test
	public void testMinimumSizeCanCreateTrackingAndBeCleared() throws Exception {
		IPreset preset = new PresetRepository().createPreset();
		preset.setCollectionTrackingMinSize(null);
		assertEquals(CollectionTrackingMode.UNCHANGED, preset.getCollectionTrackingMode());
		assertNull(preset.getCollectionTrackingMinSize());
		preset.setCollectionTrackingMinSize(BigInteger.ZERO);
		assertEquals(CollectionTrackingMode.ENABLED, preset.getCollectionTrackingMode());
		assertEquals(BigInteger.ZERO, preset.getCollectionTrackingMinSize());
		assertTracking(preset.serialize(), null, "0");
		BigInteger largeMinSize = new BigInteger("1000000000000000000000000000000");
		preset.setCollectionTrackingMinSize(largeMinSize);
		assertEquals(largeMinSize, preset.getCollectionTrackingMinSize());
		assertTracking(preset.serialize(), null, largeMinSize.toString());
		preset.setCollectionTrackingMinSize(null);
		assertNull(preset.getCollectionTrackingMinSize());
		assertTracking(preset.serialize(), null, null);
		preset.setCollectionTrackingMode(CollectionTrackingMode.DISABLED);
		preset.setCollectionTrackingMinSize(BigInteger.valueOf(128));
		assertTracking(preset.serialize(), "false", "128");
		preset.setCollectionTrackingMinSize(null);
		assertTracking(preset.serialize(), "false", null);
	}

	@Test
	public void testNegativeMinimumSizeLeavesStateUnchanged() throws Exception {
		IPreset preset = presetWithTracking("enabled=\"0\" minsize=\"512\"");
		String before = preset.serialize();
		assertThrows(IllegalArgumentException.class, () -> preset.setCollectionTrackingMinSize(BigInteger.valueOf(-1)));
		assertEquals(before, preset.serialize());
		IPreset absent = new PresetRepository().createPreset();
		assertThrows(IllegalArgumentException.class, () -> absent.setCollectionTrackingMinSize(BigInteger.valueOf(-1)));
		assertEquals(CollectionTrackingMode.UNCHANGED, absent.getCollectionTrackingMode());
		assertNull(absent.getCollectionTrackingMinSize());
	}

	@Test
	public void testCollectionTrackingEditsSurviveCopiesAndRoundTrip() throws Exception {
		IPreset preset = new PresetRepository().createPreset();
		BigInteger minSize = new BigInteger("1000000000000000000000000000000");
		preset.setCollectionTrackingMode(CollectionTrackingMode.DISABLED);
		preset.setCollectionTrackingMinSize(minSize);
		IPreset workingCopy = preset.createWorkingCopy();
		for (IPreset restored : new IPreset[] {workingCopy, preset.createDuplicate(), roundTrip(preset)}) {
			assertEquals(CollectionTrackingMode.DISABLED, restored.getCollectionTrackingMode());
			assertEquals(minSize, restored.getCollectionTrackingMinSize());
			assertTracking(restored.serialize(), "false", minSize.toString());
		}
		workingCopy.setCollectionTrackingMode(CollectionTrackingMode.ENABLED);
		workingCopy.setCollectionTrackingMinSize(BigInteger.ZERO);
		assertTracking(roundTrip(workingCopy).serialize(), "true", "0");
		assertTracking(preset.serialize(), "false", minSize.toString());
	}

	@Test
	public void testReadingSettingsPreservesImportedAttributeValues() throws Exception {
		IPreset preset = presetWithTracking("enabled=\"  1  \" minsize=\" +000512 \"");
		assertEquals(CollectionTrackingMode.ENABLED, preset.getCollectionTrackingMode());
		assertEquals(BigInteger.valueOf(512), preset.getCollectionTrackingMinSize());
		assertTracking(preset.serialize(), "  1  ", " +000512 ");
		preset.setCollectionTrackingMode(CollectionTrackingMode.DISABLED);
		assertTracking(preset.serialize(), "false", " +000512 ");
		preset.setCollectionTrackingMinSize(BigInteger.valueOf(512));
		assertTracking(preset.serialize(), "false", "512");
	}

	@Test
	public void testLargeMinimumSizeRoundTrip() throws Exception {
		String minSize = "1000000000000000000000000000000";
		IPreset preset = presetWithTracking("enabled=\"1\" minsize=\"" + minSize + "\"");
		assertTracking(roundTrip(preset).serialize(), "1", minSize);
	}

	@Test
	public void testWorkingCopyAndDuplicatePreserveTracking() throws Exception {
		IPreset preset = presetWithTracking("enabled=\"true\" minsize=\"2048\"");
		assertTracking(preset.createWorkingCopy().serialize(), "true", "2048");
		assertTracking(preset.createDuplicate().serialize(), "true", "2048");
	}

	@Test
	public void testBuiltDocumentDoesNotExposeStoredTracking() throws Exception {
		IPreset preset = presetWithTracking("enabled=\"true\" minsize=\"2048\"");
		Element tracking = trackingElement(preset.buildDocument());
		tracking.setAttribute("enabled", "false");
		tracking.setAttribute("minsize", "1");
		assertTracking(preset.serialize(), "true", "2048");
	}

	@Test
	public void testImportAndExportPreserveTracking() throws Exception {
		MemoryStorageDelegate storage = new MemoryStorageDelegate();
		PresetRepository repository = new PresetRepository() {
			@Override
			public void addPreset(IPreset preset) throws IOException {
				preset.setStorageDelegate(storage);
				super.addPreset(preset);
			}
		};
		File source = temporaryFolder.newFile("source.xml");
		Files.writeString(source.toPath(), "<jfragent><collectiontracking enabled=\"false\"/></jfragent>");
		repository.importPreset(source);
		assertTracking(storage.contents, "false", null);
		File exported = temporaryFolder.newFile("exported.xml");
		repository.exportPreset(repository.listPresets()[0], exported);
		assertTracking(Files.readString(exported.toPath()), "false", null);
	}

	private static IPreset presetWithTracking(String attributes) throws Exception {
		IPreset preset = new PresetRepository().createPreset();
		preset.deserialize("<jfragent><collectiontracking " + attributes + "/></jfragent>");
		return preset;
	}

	private static IPreset roundTrip(IPreset preset) throws Exception {
		IPreset result = new PresetRepository().createPreset();
		result.deserialize(preset.serialize());
		return result;
	}

	private static void assertTracking(String xml, String enabled, String minSize) throws Exception {
		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		Element tracking = trackingElement(document);
		assertEquals(enabled != null, tracking.hasAttribute("enabled"));
		assertEquals(minSize != null, tracking.hasAttribute("minsize"));
		if (enabled != null) {
			assertEquals(enabled, tracking.getAttribute("enabled"));
		}
		if (minSize != null) {
			assertEquals(minSize, tracking.getAttribute("minsize"));
		}
	}

	private static Element trackingElement(Document document) {
		assertEquals(1, document.getElementsByTagName("collectiontracking").getLength());
		return (Element) document.getElementsByTagName("collectiontracking").item(0);
	}

	private static class MemoryStorageDelegate implements IPresetStorageDelegate {
		private String name;
		private String contents = "";

		@Override
		public String getName() {
			return name;
		}

		@Override
		public InputStream getContents() {
			return new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8));
		}

		@Override
		public boolean save(String fileName, String fileContent) {
			name = fileName;
			contents = fileContent;
			return true;
		}

		@Override
		public boolean delete() {
			contents = "";
			return true;
		}
	}
}
