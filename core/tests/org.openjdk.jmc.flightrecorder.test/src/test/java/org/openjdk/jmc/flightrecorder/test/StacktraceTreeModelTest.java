/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, 2020, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.test;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.test.io.IOResourceSet;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;
import org.openjdk.jmc.flightrecorder.test.util.RecordingToolkit;
import org.openjdk.jmc.flightrecorder.test.util.StacktraceTestToolkit;

public class StacktraceTreeModelTest {

	private static IItemCollection testRecording;

	@BeforeClass
	public static void beforeAll() throws IOException, CouldNotLoadRecordingException {
		IOResourceSet[] testResources = StacktraceTestToolkit.getTestResources();
		IOResourceSet resourceSet = testResources[0];
		testRecording = RecordingToolkit.getFlightRecording(resourceSet);
	}

	private static final FrameSeparator separator = new FrameSeparator(FrameSeparator.FrameCategorization.METHOD,
			false);

	@Test
	public void testTreeModelWithAttributeThreadRootAtTop() {
		StacktraceTreeModel treeModel = new StacktraceTreeModel(testRecording, separator, true,
				JdkAttributes.ALLOCATION_SIZE);

		// check number of branches from root
		Set<Integer> rootNodeChildIds = treeModel.getChildrenLookup().get(null);
		assertNotNull(rootNodeChildIds);
		assertEquals(3, rootNodeChildIds.size());

		// get leaf nodes
		Map<String, List<Double>> leafValues = getLeafNodeValues(treeModel);

		assertEquals(leafValues.size(), 3);
		Map<String, List<Double>> expected = new HashMap<>();
		expected.put("Arrays.copyOfRange(char[], int, int)", asList(104.00));
		expected.put("TimerThread.mainLoop()", asList(112.00));
		expected.put("AbstractCollection.toArray()", asList(24.00));
		assertEquals(expected, leafValues);
	}

	@Test
	public void testTreeModelWithAttributeThreadRootAtBottom() {
		StacktraceTreeModel treeModel = new StacktraceTreeModel(testRecording, separator, false,
				JdkAttributes.ALLOCATION_SIZE);

		// check number of branches from root
		Set<Integer> rootNodeChildIds = treeModel.getChildrenLookup().get(null);
		assertNotNull(rootNodeChildIds);
		assertEquals(3, rootNodeChildIds.size());

		// get leaf nodes
		Map<String, List<Double>> leafValues = getLeafNodeValues(treeModel);

		assertEquals(leafValues.size(), 3);
		Map<String, List<Double>> expected = new HashMap<>();
		expected.put("JFRImpl.onNewChunk()", asList(24.0));
		expected.put("TimerThread.run()", asList(112.00));
		expected.put("Thread.run()", asList(104.0));
		assertEquals(expected, leafValues);
	}

	@Test
	public void testTreeModelWithoutAttributeThreadRootAtTop() {
		StacktraceTreeModel treeModel = new StacktraceTreeModel(testRecording, separator);

		// check number of branches from root
		Set<Integer> rootNodeChildIds = treeModel.getChildrenLookup().get(null);
		assertNotNull(rootNodeChildIds);
		assertEquals(6, rootNodeChildIds.size());

		// get leaf nodes
		Map<String, List<Double>> leafValues = getLeafNodeValues(treeModel);

		Map<String, List<Double>> expected = new HashMap<>();
		expected.put("AbstractCollection.toArray()", asList(1.0));
		expected.put("Buffer.checkIndex(int)", asList(1.0));
		expected.put("Object.wait(long)", asList(10.0, 98.0));
		expected.put("ObjectOutputStream$BlockDataOutputStream.writeUTF(String)", asList(1.0));
		expected.put("SocketInputStream.read(byte[], int, int, int)", asList(9.0));
		expected.put("Arrays.copyOfRange(char[], int, int)", asList(1.0));
		assertEquals(expected, leafValues);
	}

	@Test
	public void testTreeModelWithoutAttributeThreadRootAtBottom() {
		StacktraceTreeModel treeModel = new StacktraceTreeModel(testRecording, separator, false);

		// check number of branches from root
		Set<Integer> rootNodeChildIds = treeModel.getChildrenLookup().get(null);
		assertNotNull(rootNodeChildIds);
		assertEquals(7, rootNodeChildIds.size());

		// get leaf nodes
		Map<String, List<Double>> leafValues = getLeafNodeValues(treeModel);

		Map<String, List<Double>> expected = new HashMap<>();
		expected.put("TimerThread.run()", asList(1.0, 10.0));
		expected.put("JFRImpl.onNewChunk()", asList(1.0));
		expected.put("OGLRenderQueue$QueueFlusher.run()", asList(98.0));
		expected.put("InstantEvent.commit()", asList(1.0));
		expected.put("Thread.run()", asList(1.0, 9.0));
		expected.put("ArrayList.writeObject(ObjectOutputStream)", asList(1.0));
		assertEquals(expected, leafValues);
	}

	public Map<String, List<Double>> getLeafNodeValues(StacktraceTreeModel treeModel) {
		Map<Integer, Node> nodesById = treeModel.getNodes();
		Map<Integer, Set<Integer>> childrenLookup = treeModel.getChildrenLookup();
		Map<String, List<Double>> leafValues = new HashMap<>();
		// we get the leaves by checking the lookup and selecting nodes with no children
		for (Integer nodeId : childrenLookup.keySet()) {
			if (childrenLookup.get(nodeId).isEmpty()) {
				Node node = nodesById.get(nodeId);
				String key = node.getFrame().getHumanReadableShortString();
				leafValues.computeIfAbsent(key, k -> new ArrayList<>());
				leafValues.get(key).add(node.getWeight());
				leafValues.get(key).sort(Comparator.naturalOrder());
			}
		}
		return leafValues;
	}
}
