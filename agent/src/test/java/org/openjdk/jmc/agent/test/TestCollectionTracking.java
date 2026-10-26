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

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.openjdk.jmc.agent.TransformDescriptor;
import org.openjdk.jmc.agent.TransformRegistry;
import org.openjdk.jmc.agent.collections.CollectionTrackingSettings;
import org.openjdk.jmc.agent.collections.CollectionTransformDescriptor;
import org.openjdk.jmc.agent.impl.DefaultTransformRegistry;

/**
 * Tests parsing of the {@code <collectiontracking>} capability element and the injection of the
 * fixed HashMap/ArrayList instrumentation descriptors into the registry.
 */
public class TestCollectionTracking {

	private static final String HASHMAP = "java/util/HashMap"; //$NON-NLS-1$
	private static final String ARRAYLIST = "java/util/ArrayList"; //$NON-NLS-1$

	@Test
	public void testEnabledWithMinSize() throws Exception {
		TransformRegistry registry = fromXml("<jfragent><collectiontracking minsize=\"64\"/></jfragent>"); //$NON-NLS-1$

		CollectionTrackingSettings settings = registry.getCollectionTrackingSettings();
		assertTrue("Collection tracking should be enabled", settings.isEnabled()); //$NON-NLS-1$
		assertEquals(64, settings.getMinSize());

		assertSingleCollectionDescriptor(registry, HASHMAP);
		assertSingleCollectionDescriptor(registry, ARRAYLIST);
	}

	@Test
	public void testDefaultMinSize() throws Exception {
		TransformRegistry registry = fromXml("<jfragent><collectiontracking/></jfragent>"); //$NON-NLS-1$

		CollectionTrackingSettings settings = registry.getCollectionTrackingSettings();
		assertTrue(settings.isEnabled());
		assertEquals(CollectionTrackingSettings.DEFAULT_MIN_SIZE, settings.getMinSize());
	}

	@Test
	public void testMinSizeClampsOverflowToSuppress() throws Exception {
		// A minsize larger than an int is valid per the schema (nonNegativeInteger) but must not fall
		// back to the noisy default; it clamps to Integer.MAX_VALUE (effectively suppressing events).
		TransformRegistry registry = fromXml("<jfragent><collectiontracking minsize=\"10000000000\"/></jfragent>"); //$NON-NLS-1$
		assertEquals(Integer.MAX_VALUE, registry.getCollectionTrackingSettings().getMinSize());
	}

	@Test
	public void testAbsent() throws Exception {
		TransformRegistry registry = fromXml("<jfragent></jfragent>"); //$NON-NLS-1$

		assertFalse("Collection tracking should be disabled when the element is absent", //$NON-NLS-1$
				registry.getCollectionTrackingSettings().isEnabled());
		assertTrue("No HashMap transforms expected when disabled", //$NON-NLS-1$
				registry.getTransformData(HASHMAP).isEmpty());
		assertTrue("No ArrayList transforms expected when disabled", //$NON-NLS-1$
				registry.getTransformData(ARRAYLIST).isEmpty());
	}

	@Test
	public void testDisableCollectionTrackingKeepsUserProbes() throws Exception {
		TransformRegistry registry = fromXml("<jfragent><collectiontracking/><events>" //$NON-NLS-1$
				+ arrayListProbe("One") + "</events></jfragent>"); //$NON-NLS-1$
		assertEquals("Expected the collection descriptor and the user probe", 2, //$NON-NLS-1$
				registry.getTransformData(ARRAYLIST).size());

		registry.disableCollectionTracking();

		List<TransformDescriptor> descriptors = registry.getTransformData(ARRAYLIST);
		assertEquals("The user's JFR probe must survive disabling collection tracking", 1, descriptors.size()); //$NON-NLS-1$
		assertFalse(descriptors.get(0) instanceof CollectionTransformDescriptor);
		assertTrue("Classes with only collection descriptors should be dropped", //$NON-NLS-1$
				registry.getTransformData(HASHMAP).isEmpty());
	}

	@Test
	public void testModifyReplacesProbesOnTrackedClasses() throws Exception {
		TransformRegistry registry = fromXml("<jfragent></jfragent>"); //$NON-NLS-1$
		// <collectiontracking> deliberately precedes <events> in document order.
		registry.modify("<jfragent><collectiontracking/><events>" + arrayListProbe("One") //$NON-NLS-1$ //$NON-NLS-2$
				+ "</events></jfragent>"); //$NON-NLS-1$
		Set<String> modified = registry.modify("<jfragent><collectiontracking/><events>" + arrayListProbe("Two") //$NON-NLS-1$ //$NON-NLS-2$
				+ "</events></jfragent>"); //$NON-NLS-1$

		List<TransformDescriptor> descriptors = registry.getTransformData(ARRAYLIST);
		assertEquals("Expected the collection descriptor and the replacing probe only", 2, descriptors.size()); //$NON-NLS-1$
		for (TransformDescriptor descriptor : descriptors) {
			if (!(descriptor instanceof CollectionTransformDescriptor)) {
				assertEquals("The old probe should have been replaced", "test.ArrayListProbeTwo", //$NON-NLS-1$ //$NON-NLS-2$
						descriptor.getId());
			}
		}
		assertTrue("The probe-carrying class must be retransformed", modified.contains(ARRAYLIST)); //$NON-NLS-1$
	}

	@Test
	public void testModifyRemovesStaleProbesOnTrackedClasses() throws Exception {
		TransformRegistry registry = fromXml("<jfragent></jfragent>"); //$NON-NLS-1$
		registry.modify("<jfragent><collectiontracking/><events>" + arrayListProbe("One") //$NON-NLS-1$ //$NON-NLS-2$
				+ "</events></jfragent>"); //$NON-NLS-1$
		// The new configuration keeps tracking but no longer declares the probe.
		Set<String> modified = registry.modify("<jfragent><collectiontracking/></jfragent>"); //$NON-NLS-1$

		assertSingleCollectionDescriptor(registry, ARRAYLIST);
		assertTrue("The class must be retransformed to unweave the removed probe", modified.contains(ARRAYLIST)); //$NON-NLS-1$
	}

	@Test
	public void testModifyUnchangedTrackingDoesNotRetransformCollections() throws Exception {
		TransformRegistry registry = fromXml("<jfragent></jfragent>"); //$NON-NLS-1$
		Set<String> first = registry.modify("<jfragent><collectiontracking/></jfragent>"); //$NON-NLS-1$
		assertTrue("Enabling tracking must retransform the collection classes", first.contains(HASHMAP)); //$NON-NLS-1$
		// Without this, the descriptors stay pending and an identical re-push deliberately retries
		// the (never-executed) weave by re-marking the classes as modified.
		simulateWeave(registry);

		Set<String> second = registry.modify("<jfragent><collectiontracking/></jfragent>"); //$NON-NLS-1$
		assertTrue("Unchanged collection tracking must not retransform the hot JDK collection classes", //$NON-NLS-1$
				second.isEmpty());
		assertSingleCollectionDescriptor(registry, HASHMAP);
		assertTrue(registry.getCollectionTrackingSettings().isEnabled());
	}

	@Test
	public void testModifyOmittingTrackingLeavesTrackingUnchanged() throws Exception {
		TransformRegistry registry = fromXml("<jfragent><collectiontracking/></jfragent>"); //$NON-NLS-1$
		simulateWeave(registry);
		Set<String> modified = registry.modify("<jfragent><events>" + arrayListProbe("Three") //$NON-NLS-1$ //$NON-NLS-2$
				+ "</events></jfragent>"); //$NON-NLS-1$

		assertTrue("An omitted collectiontracking element must not disable the capability", //$NON-NLS-1$
				registry.getCollectionTrackingSettings().isEnabled());
		assertSingleCollectionDescriptor(registry, HASHMAP);
		assertEquals("Expected the collection descriptor and the declared probe", 2, //$NON-NLS-1$
				registry.getTransformData(ARRAYLIST).size());
		assertFalse("Untouched tracking must not retransform the collection classes", modified.contains(HASHMAP)); //$NON-NLS-1$
	}

	@Test
	public void testModifyDisablingTrackingRemovesCollectionDescriptors() throws Exception {
		TransformRegistry registry = fromXml("<jfragent><collectiontracking/></jfragent>"); //$NON-NLS-1$
		registry.modify("<jfragent><collectiontracking enabled=\"false\"/><events>" + arrayListProbe("Three") //$NON-NLS-1$ //$NON-NLS-2$
				+ "</events></jfragent>"); //$NON-NLS-1$

		assertFalse("Explicit enabled=\"false\" must disable the capability", //$NON-NLS-1$
				registry.getCollectionTrackingSettings().isEnabled());
		assertTrue("Collection descriptors should be gone", registry.getTransformData(HASHMAP).isEmpty()); //$NON-NLS-1$
		List<TransformDescriptor> descriptors = registry.getTransformData(ARRAYLIST);
		assertEquals("Only the declared probe should remain on the collection class", 1, descriptors.size()); //$NON-NLS-1$
		assertFalse(descriptors.get(0) instanceof CollectionTransformDescriptor);
	}

	@Test
	public void testStartupExplicitlyDisabled() throws Exception {
		TransformRegistry registry = fromXml("<jfragent><collectiontracking enabled=\"false\"/></jfragent>"); //$NON-NLS-1$

		assertFalse(registry.getCollectionTrackingSettings().isEnabled());
		assertTrue(registry.getTransformData(HASHMAP).isEmpty());
	}

	@Test
	public void testReadBackRendersEffectiveTrackingState() throws Exception {
		TransformRegistry registry = fromXml("<jfragent></jfragent>"); //$NON-NLS-1$
		assertTrue("Disabled state must be rendered explicitly", //$NON-NLS-1$
				registry.getCurrentConfiguration().contains("enabled=\"false\"")); //$NON-NLS-1$

		registry.modify("<jfragent><collectiontracking/></jfragent>"); //$NON-NLS-1$
		// A push omitting the element keeps tracking; the read-back must still state it.
		registry.modify("<jfragent><events>" + arrayListProbe("Four") + "</events></jfragent>"); //$NON-NLS-1$ //$NON-NLS-2$
		String readBack = registry.getCurrentConfiguration();
		assertTrue("Enabled state must be rendered explicitly", readBack.contains("enabled=\"true\"")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("The effective minsize must be rendered explicitly", //$NON-NLS-1$
				readBack.contains("minsize=\"" + registry.getCollectionTrackingSettings().getMinSize() + "\"")); //$NON-NLS-1$ //$NON-NLS-2$

		// The rendered read-back must be schema-valid and stable under a re-push round trip.
		DefaultTransformRegistry.validateProbeDefinition(readBack);
		simulateWeave(registry);
		Set<String> modified = registry.modify(readBack);
		assertFalse("Re-pushing the read-back must not retransform the collection classes", //$NON-NLS-1$
				modified.contains(HASHMAP));
		assertTrue(registry.getCollectionTrackingSettings().isEnabled());
	}

	/**
	 * Clears the pending flag on all registered descriptors, as the Transformer would after
	 * successfully weaving each class - these registry-only tests never run actual retransforms.
	 */
	private static void simulateWeave(TransformRegistry registry) {
		for (String className : registry.getClassNames()) {
			for (TransformDescriptor descriptor : registry.getTransformData(className)) {
				descriptor.setPendingTransforms(false);
			}
		}
	}

	private static String arrayListProbe(String suffix) {
		return "<event id=\"test.ArrayListProbe" + suffix + "\"><label>ArrayListProbe" + suffix //$NON-NLS-1$ //$NON-NLS-2$
				+ "</label><class>java.util.ArrayList</class><method><name>trimToSize</name>" //$NON-NLS-1$
				+ "<descriptor>()V</descriptor></method></event>"; //$NON-NLS-1$
	}

	private static void assertSingleCollectionDescriptor(TransformRegistry registry, String className) {
		List<TransformDescriptor> descriptors = registry.getTransformData(className);
		assertEquals("Expected exactly one transform for " + className, 1, descriptors.size()); //$NON-NLS-1$
		assertTrue("Expected a CollectionTransformDescriptor for " + className, //$NON-NLS-1$
				descriptors.get(0) instanceof CollectionTransformDescriptor);
	}

	private static TransformRegistry fromXml(String xml) throws Exception {
		return DefaultTransformRegistry.from(new ByteArrayInputStream(xml.getBytes()));
	}
}
