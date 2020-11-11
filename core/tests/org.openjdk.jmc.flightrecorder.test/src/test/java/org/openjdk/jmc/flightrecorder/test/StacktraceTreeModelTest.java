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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.HashMap;
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

    private static final FrameSeparator separator = new FrameSeparator(FrameSeparator.FrameCategorization.METHOD, false);

    @Test
    public void testTreeModelWithAttribute() {
        StacktraceTreeModel treeModel = new StacktraceTreeModel(separator, testRecording, JdkAttributes.ALLOCATION_SIZE);

        // check number of branches from root
        Set<Integer> rootNodeChildIds = treeModel.getChildrenLookup().get(null);
        assertNotNull(rootNodeChildIds);
        assertEquals(3, rootNodeChildIds.size());

        // get leaf nodes
        Map<String, Double> leafValues = getLeafNodeValues(treeModel);

        assertEquals(leafValues.size(), 3);
        Map<String, Double> expected = new HashMap<>();
        expected.put("Arrays.copyOfRange(char[], int, int)", 104.00);
        expected.put("TimerThread.mainLoop()", 112.00);
        expected.put("AbstractCollection.toArray()", 24.00);
        assertEquals(expected, leafValues);
    }

    @Test
    public void testTreeModelWithoutAttribute() {
        StacktraceTreeModel treeModel = new StacktraceTreeModel(separator, testRecording, null);
        System.out.println(treeModel.toJSON());

        // check number of branches from root
        Set<Integer> rootNodeChildIds = treeModel.getChildrenLookup().get(null);
        assertNotNull(rootNodeChildIds);
        assertEquals(6, rootNodeChildIds.size());

        // get leaf nodes
        Map<String, Double> leafValues = getLeafNodeValues(treeModel);

        Map<String, Double> expected = new HashMap<>();
        expected.put("TimerThread.mainLoop()", 1.0);
        expected.put("AbstractCollection.toArray()", 1.0);
        expected.put("Buffer.checkIndex(int)", 1.0);
        expected.put("Object.wait(long)", 98.0);
        expected.put("ObjectOutputStream$BlockDataOutputStream.writeUTF(String)", 1.0);
        expected.put("SocketInputStream.read(byte[], int, int, int)", 9.0);

        assertEquals(expected, leafValues);
    }

    @Test
    public void testTreeModelToJSON() {
        StacktraceTreeModel treeModel = new StacktraceTreeModel(separator, testRecording, null);
        // FIXME: this is a very weak assertion, but we don't have a JSON library to parse the output
        assertNotNull(treeModel.toJSON());
    }

    public Map<String, Double> getLeafNodeValues(StacktraceTreeModel treeModel) {
        Map<Integer, Node> nodesById = treeModel.getNodes();
        Map<Integer, Set<Integer>> childrenLookup = treeModel.getChildrenLookup();
        Set<Integer> rootNodeChildIds = childrenLookup.get(null);
        Map<String, Double> leafValues = new HashMap<>();
        for (Integer nodeId : rootNodeChildIds) {
            Set<Integer> childIds = childrenLookup.get(nodeId);
            while (childIds.size() > 0) {
                // we have simple branches where each node has a single child
                assertEquals(childIds.size(), 1);
                nodeId = childIds.iterator().next();
                childIds = childrenLookup.get(nodeId);
            }
            Node node = nodesById.get(nodeId);
            leafValues.put(node.getFrame().getHumanReadableShortString(), node.getWeight());
        }
        return leafValues;
    }
}
