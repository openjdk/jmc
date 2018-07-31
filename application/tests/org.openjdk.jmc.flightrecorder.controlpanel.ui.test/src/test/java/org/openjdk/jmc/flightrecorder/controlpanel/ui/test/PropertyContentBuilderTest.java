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
package org.openjdk.jmc.flightrecorder.controlpanel.ui.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.openjdk.jmc.common.util.StringToolkit;
import org.openjdk.jmc.flightrecorder.configuration.ConfigurationToolkit;
import org.openjdk.jmc.flightrecorder.configuration.events.IEventTypeID;
import org.openjdk.jmc.flightrecorder.configuration.events.SchemaVersion;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfiguration;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfigurationModel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.test.JfrControlTestCase;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.recordingconfiguration.EventConfigurationPart;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.recordingconfiguration.EventConfigurationPart.PropertyKey;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.recordingconfiguration.PathElement;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.recordingconfiguration.PathElement.PathElementKind;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.recordingconfiguration.Property;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.recordingconfiguration.PropertyContainer.EventNode;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.recordingconfiguration.PropertyContainer.FolderNode;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.recordingconfiguration.PropertyContentBuilder;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;

@SuppressWarnings("nls")
public class PropertyContentBuilderTest extends JfrControlTestCase {

	private IFlightRecorderService service;
	private SchemaVersion version;

	@Before
	public void setUp() throws Exception {
		assumeHotSpot7u12OrLater(getConnectionHandle());
		service = getFlightRecorderService();
		version = SchemaVersion.fromBeanVersion(service.getVersion());
	}

	@Test
	public void testCategoryRootsSame() throws Exception {
		List<? extends PathElement> propertyRoots = buildPropertyContent("same", true, true);
		assertNodes(Arrays.asList("Java Application", "IN_BOTH", "Java Virtual Machine", "IN_BOTH", "Operating System",
				"IN_BOTH", "Flight Recorder", "IN_BOTH"), propertyRoots, "root");
	}

	@Test
	public void testMore() throws Exception {
		List<? extends PathElement> propertyRoots = buildPropertyContent("more", true, true);
		switch (version) {
		case V1:
			assertNodes(Arrays.asList("Java Application", "IN_BOTH", "Java Virtual Machine", "IN_BOTH",
					"Operating System", "IN_BOTH", "Flight Recorder", "IN_BOTH", "Com", "IN_CONFIGURATION"),
					propertyRoots, "root");
			break;
		case V2:
			assertNodes(Arrays.asList("Java Application", "IN_BOTH", "Java Virtual Machine", "IN_BOTH",
					"Operating System", "IN_BOTH", "Flight Recorder", "IN_BOTH", "com", "IN_CONFIGURATION"),
					propertyRoots, "root");
			break;
		}
	}

	@Test
	public void testLessBeforeServerMetadataPush() throws Exception {
		List<? extends PathElement> propertyRoots = buildPropertyContent("less", false, true);
		assertNodes(Arrays.asList("Java Application", "IN_BOTH", "Java Virtual Machine", "IN_BOTH", "Operating System",
				"IN_BOTH", "Flight Recorder", "IN_SERVER"), propertyRoots, "root");
	}

	@Test
	public void testLessAfterServerMetadataPush() throws Exception {
		List<? extends PathElement> propertyRoots = buildPropertyContent("less", true, true);
		assertNodes(Arrays.asList("Java Application", "IN_BOTH", "Java Virtual Machine", "IN_BOTH", "Operating System",
				"IN_BOTH", "Flight Recorder", "IN_BOTH"), propertyRoots, "root");
	}

	@Test
	public void testDiffWithPush() throws Exception {
		List<? extends PathElement> propertyRoots = buildPropertyContent("diff", true, true);
		FolderNode javaApplication = (FolderNode) propertyRoots.get(0);
		IEventTypeID threadAllocationID = null;
		IEventTypeID classLoadingStatisticsID = null;
		assertEquals("Java Application", javaApplication.getName());
		FolderNode javaStatistics = javaApplication.getFolder("Statistics", PathElementKind.IN_BOTH);

		switch (version) {
		case V1:
			threadAllocationID = jvm("java/statistics/thread_allocation");
			classLoadingStatisticsID = jvm("java/statistics/class_loading");
			break;
		case V2:
			threadAllocationID = v2(JdkTypeIDs.THREAD_ALLOCATION_STATISTICS);
			classLoadingStatisticsID = v2(JdkTypeIDs.CLASS_LOAD_STATISTICS);
			break;
		}

		// FIXME: Should we include extraTestOption and enabledButWrongForTest here, with either IN_CONFIGURATION style, or some new disabled/error style?
		EventNode threadAllocation = javaStatistics.getEvent(threadAllocationID, PathElementKind.IN_BOTH);
		assertOptions(threadAllocation, Arrays.asList("Enabled", "IN_BOTH", "Period", "IN_BOTH")); // "extraTestOption", "IN_CONFIGURATION"

		EventNode classLoadingStatistics = javaStatistics.getEvent(classLoadingStatisticsID, PathElementKind.IN_BOTH);
		assertOptions(classLoadingStatistics, Arrays.asList("Period", "IN_BOTH", "Enabled", "IN_BOTH")); // "enabledButWrongForTest", "IN_CONFIGURATION"
	}

	@Test
	public void testDiffNoPush() throws Exception {
		List<? extends PathElement> propertyRoots = buildPropertyContent("diff", false, true);
		FolderNode javaApplication = (FolderNode) propertyRoots.get(0);
		IEventTypeID threadAllocationID = null;
		IEventTypeID classLoadingStatisticsID = null;
		assertEquals("Java Application", javaApplication.getName());
		FolderNode javaStatistics = javaApplication.getFolder("Statistics", PathElementKind.IN_BOTH);

		switch (version) {
		case V1:
			threadAllocationID = jvm("java/statistics/thread_allocation");
			classLoadingStatisticsID = jvm("java/statistics/class_loading");
			break;
		case V2:
			threadAllocationID = v2(JdkTypeIDs.THREAD_ALLOCATION_STATISTICS);
			classLoadingStatisticsID = v2(JdkTypeIDs.CLASS_LOAD_STATISTICS);
			break;
		}

		// FIXME: Should we include extraTestOption and enabledButWrongForTest here, with either IN_CONFIGURATION style, or some new disabled/error style?
		EventNode threadAllocation = javaStatistics.getEvent(threadAllocationID, PathElementKind.IN_BOTH);
		assertOptions(threadAllocation, Arrays.asList("Enabled", "IN_BOTH", "Period", "IN_BOTH")); // "extraTestOption", "IN_CONFIGURATION"

		EventNode classLoadingStatistics = javaStatistics.getEvent(classLoadingStatisticsID, PathElementKind.IN_BOTH);
		assertOptions(classLoadingStatistics, Arrays.asList("Period", "IN_BOTH", "Enabled", "IN_SERVER")); // "enabledButWrongForTest", "IN_CONFIGURATION"
	}

	@Test
	public void testCategoryOffline() throws Exception {
		assumeTrue(SchemaVersion.V2.equals(version));
		List<? extends PathElement> propertyRoots = buildPropertyContent("custom", false, false);
		assertNodes(Arrays.asList("SMX", "IN_CONFIGURATION", "com", "IN_CONFIGURATION"), propertyRoots, "root");
	}

	@Test
	public void testCustomSettingsLabelsOffline() throws Exception {
		assumeTrue(SchemaVersion.V2.equals(version));
		List<? extends PathElement> propertyRoots = buildPropertyContent("custom", false, false);
		FolderNode smxCategory = (FolderNode) propertyRoots.get(0);
		assertEquals("SMX", smxCategory.getName());
		EventNode smxTransaction = smxCategory.getEvent(v2("org.openjdk.jmc.smx.Transaction"),
				PathElementKind.IN_CONFIGURATION);
		assertOptions(smxTransaction,
				Arrays.asList("Stack Trace", "IN_CONFIGURATION", "LabelForTextFilterWithSameKeyButDifferentLabel",
						"IN_CONFIGURATION", "LabelForTimeSpanFilterWithDifferentKeyButSameLabel", "IN_CONFIGURATION",
						"Threshold", "IN_CONFIGURATION", "Enabled", "IN_CONFIGURATION",
						"LabelForFilterWithDifferentContentTypeButSameKeyAndLabel", "IN_CONFIGURATION"));
	}

	@Test
	public void testCustomSettingsLabelDefaultsOffline() throws Exception {
		assumeTrue(SchemaVersion.V2.equals(version));
		List<? extends PathElement> propertyRoots = buildPropertyContent("custom_no_labels", false, false);
		FolderNode smxCategory = (FolderNode) propertyRoots.get(0);
		assertEquals("SMX", smxCategory.getName());
		EventNode smxTransaction = smxCategory.getEvent(v2("org.openjdk.jmc.smx.Transaction"),
				PathElementKind.IN_CONFIGURATION);
		assertOptions(smxTransaction,
				Arrays.asList("Stack Trace", "IN_CONFIGURATION", "textFilterWithSameKeyButDifferentLabel",
						"IN_CONFIGURATION", "timeSpanFilterWithDifferentKeyButSameLabel", "IN_CONFIGURATION",
						"Threshold", "IN_CONFIGURATION", "Enabled", "IN_CONFIGURATION",
						"filterWithDifferentContentTypeButSameKeyAndLabel", "IN_CONFIGURATION"));
	}

	@Test
	public void testOptionKeysFromFolderNode() throws Exception {
		EventConfigurationModel configModel = buildUiModel("same", true, true);

		List<? extends PathElement> propertyRoots = PropertyContentBuilder.build(configModel);
		FolderNode javaApplicationNode = (FolderNode) propertyRoots.get(0);
		assertEquals("Expected first folder node to be ", "Java Application", javaApplicationNode.getName());
		Collection<PropertyKey> optionKeys = EventConfigurationPart.findProperties(javaApplicationNode).keySet();
		List<String> props = optionKeys.stream().map(p -> p.getLabel()).collect(Collectors.toList());
		assertArrayEquals("Options from the " + javaApplicationNode.getName() + " sub tree does not match expected",
				new String[] {"Enabled", "Period", "Stack Trace", "Threshold"}, props.toArray());
	}

	@Test
	public void testCustomTwoEventsWithSameOptionIdDifferentContentTypes() throws Exception {
		assumeTrue(SchemaVersion.V2.equals(version));
		EventConfigurationModel configModel = buildUiModel("custom", false, false);
		FolderNode smxCategory = (FolderNode) PropertyContentBuilder.build(configModel).get(0);
		assertEquals("SMX", smxCategory.getName());

		Collection<PropertyKey> keyProperties = EventConfigurationPart.findProperties(smxCategory).keySet();
		assertArrayEqualsWithMoreInfo("Distinct option labels from the SMX substree does not match expected",
				new String[] {"Enabled", "LabelForFilterWithDifferentContentTypeButSameKeyAndLabel",
						"LabelForFilterWithDifferentContentTypeButSameKeyAndLabel",
						"LabelForTextFilterWithSameKeyButDifferentLabel",
						"LabelForTextFilterWithSameKeyButDifferentLabel2",
						"LabelForTimeSpanFilterWithDifferentKeyButSameLabel", "Stack Trace", "Threshold"},
				keyProperties.stream().map(p -> p.getLabel()).collect(Collectors.toList()).toArray());

		Collection<Set<Property>> properties = EventConfigurationPart.findProperties(smxCategory).values();
		assertNodes("Actual options from the SMX substree does not match expected",
				Arrays.asList("Enabled", "IN_CONFIGURATION", "Enabled", "IN_CONFIGURATION",
						"LabelForFilterWithDifferentContentTypeButSameKeyAndLabel", "IN_CONFIGURATION",
						"LabelForFilterWithDifferentContentTypeButSameKeyAndLabel", "IN_CONFIGURATION",
						"LabelForTextFilterWithSameKeyButDifferentLabel", "IN_CONFIGURATION",
						"LabelForTextFilterWithSameKeyButDifferentLabel2", "IN_CONFIGURATION",
						"LabelForTimeSpanFilterWithDifferentKeyButSameLabel", "IN_CONFIGURATION",
						"LabelForTimeSpanFilterWithDifferentKeyButSameLabel", "IN_CONFIGURATION", "Stack Trace",
						"IN_CONFIGURATION", "Stack Trace", "IN_CONFIGURATION", "Threshold", "IN_CONFIGURATION",
						"Threshold", "IN_CONFIGURATION"),
				properties.stream().flatMap(s -> s.stream()).collect(Collectors.toList()));

		EventNode smxTransaction = smxCategory.getEvent(v2("org.openjdk.jmc.smx.Transaction"),
				PathElementKind.IN_CONFIGURATION);
		properties = EventConfigurationPart.findProperties(smxTransaction).values();
		assertEquals("Number of options from the " + smxTransaction.getName() + " event does not match expected", 6,
				properties.stream().flatMap(s -> s.stream()).count());
		assertNodes("Options from the " + smxTransaction.getName() + " event does not match expected",
				Arrays.asList("Enabled", "IN_CONFIGURATION", "LabelForFilterWithDifferentContentTypeButSameKeyAndLabel",
						"IN_CONFIGURATION", "LabelForTextFilterWithSameKeyButDifferentLabel", "IN_CONFIGURATION",
						"LabelForTimeSpanFilterWithDifferentKeyButSameLabel", "IN_CONFIGURATION", "Stack Trace",
						"IN_CONFIGURATION", "Threshold", "IN_CONFIGURATION"),
				properties.stream().flatMap(s -> s.stream()).collect(Collectors.toList()));
	}

	// TODO: Add more tests: Other categories, labels, descriptions, option label and descriptions, content types.

	// TODO: Test the offline cases as well?

	private List<? extends PathElement> buildPropertyContent(
		String comparisonType, boolean pushServerMetadata, boolean online) throws Exception {
		return PropertyContentBuilder.build(buildUiModel(comparisonType, pushServerMetadata, online));
	}

	private EventConfigurationModel buildUiModel(String comparisonType, boolean pushServerMetadata, boolean online)
			throws Exception {
		EventConfiguration config = (EventConfiguration) loadConfig(
				comparisonType + "_" + version.attributeValue() + ".jfc");
		EventConfigurationModel model;
		if (online) {
			model = EventConfigurationModel.create(config, service.getDefaultEventOptions(),
					service.getEventTypeInfoMapByID());
		} else {
			model = EventConfigurationModel.create(config, ConfigurationToolkit.getEventOptions(SchemaVersion.V2),
					Collections.emptyMap());
		}
		if (pushServerMetadata) {
			model.pushServerMetadataToLocalConfiguration(false);
		}
		return model;
	}

	private void assertOptions(EventNode event, List<String> expected) {
		assertNodes(expected, event.getChildren(), event.getName());
	}

	private void assertNodes(
		List<String> expected, Collection<? extends PathElement> actualNodes, String nodeIdentifier) {
		assertNodes("Option tree nodes for " + nodeIdentifier + " did not match the expected", expected, actualNodes);

	}

	private void assertNodes(String message, List<String> expected, Collection<? extends PathElement> actualNodes) {
		Map<String, String> expectedMap = new HashMap<>();
		for (Iterator<String> iterator = expected.iterator(); iterator.hasNext();) {
			String expectedNode = iterator.next();
			expectedMap.put(expectedNode, iterator.next());
		}
		Set<String> expectedNodesNames = expectedMap.keySet();
		Set<String> actualNodesNames = actualNodes.stream().map(p -> p.getName()).collect(Collectors.toSet());
		assertTrue(
				"Node names differ from expected: " + StringToolkit.join(actualNodesNames, ",") + " != "
						+ StringToolkit.join(expectedNodesNames, ","),
				actualNodesNames.containsAll(expectedNodesNames) && expectedNodesNames.containsAll(actualNodesNames));
		for (PathElement pathElement : actualNodes) {
			String nodeName = pathElement.getName();
			assertEquals("Wrong path element kind for '" + nodeName + "',", expectedMap.get(nodeName),
					pathElement.getKind().toString());
			// FIXME: Check the node type (folder, event, property)?
		}
	}
}
