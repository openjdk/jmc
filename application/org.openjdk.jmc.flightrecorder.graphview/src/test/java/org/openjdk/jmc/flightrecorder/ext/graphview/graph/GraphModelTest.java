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
package org.openjdk.jmc.flightrecorder.ext.graphview.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.stacktrace.graph.GraphModelUtils;
import org.openjdk.jmc.flightrecorder.stacktrace.graph.StacktraceGraphModel;

final class GraphModelTest {

	@Test
	void testBuildGraph() throws IOException, CouldNotLoadRecordingException {
		IItemCollection events = JfrLoaderToolkit
				.loadEvents(GraphModelTest.class.getResourceAsStream("hotmethods.jfr"));
		assertTrue(events.hasItems());
		IItemCollection executionSamples = events.apply(JdkFilters.EXECUTION_SAMPLE);
		assertTrue(executionSamples.hasItems());
		assertEquals(executionSamples.getAggregate(Aggregators.count()).longValue(), 24526);
		StacktraceGraphModel model = new StacktraceGraphModel(GraphModelUtils.DEFAULT_FRAME_SEPARATOR, executionSamples,
				null);
		assertFalse("No nodes!", model.getNodes().isEmpty());
		assertFalse("No edges!", model.getEdges().isEmpty());
	}

	public static void main(String[] args) throws IOException, CouldNotLoadRecordingException {
		new GraphModelTest().testBuildGraph();
	}
}
