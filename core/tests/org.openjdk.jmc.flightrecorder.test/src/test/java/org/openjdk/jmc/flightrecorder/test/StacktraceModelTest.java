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
package org.openjdk.jmc.flightrecorder.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.test.mock.item.MockCollections;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator.FrameCategorization;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel.Branch;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel.Fork;
import org.openjdk.jmc.flightrecorder.test.util.MockStacktraceGenerator;

public class StacktraceModelTest {
	private static final boolean TRUNCATED_FALSE = false;
	private static final boolean TRUNCATED_TRUE = true;
	private static final boolean RECURSIVE_FALSE = false;
	private static final boolean RECURSIVE_TRUE = true;
	private static final boolean THREAD_ROOT_ON_TOP_FALSE = false;
	private static final boolean THREAD_ROOT_ON_TOP_TRUE = true;
	private static final boolean DISTINGUISH_FRAMES_BY_OPTIMIZATION_FALSE = false;
	private static final boolean DISTINGUISH_FRAMES_BY_OPTIMIZATION_TRUE = true;
	private static final FrameCategorization NO_FRAME_CATEGORIZATION = null;

	@Test
	public void testSingleFrame() {
		IItemCollection items = MockCollections
				.getStackTraceCollection(MockStacktraceGenerator.generateTraces(TRUNCATED_FALSE, RECURSIVE_FALSE, 0,
						NO_FRAME_CATEGORIZATION, DISTINGUISH_FRAMES_BY_OPTIMIZATION_FALSE));
		StacktraceModel smModel = new StacktraceModel(THREAD_ROOT_ON_TOP_FALSE,
				new FrameSeparator(FrameCategorization.METHOD, DISTINGUISH_FRAMES_BY_OPTIMIZATION_FALSE), items);
		Fork root = smModel.getRootFork();
		assertEquals(1, root.getBranchCount());
		Branch branch = root.getBranch(0);
		assertEquals(branch.getFirstFrame(), branch.getLastFrame());
	}

	@Test
	public void testTwoFrames() {
		IItemCollection items = MockCollections
				.getStackTraceCollection(MockStacktraceGenerator.generateTraces(TRUNCATED_FALSE, RECURSIVE_FALSE, 1,
						NO_FRAME_CATEGORIZATION, DISTINGUISH_FRAMES_BY_OPTIMIZATION_FALSE));
		StacktraceModel smModel = new StacktraceModel(THREAD_ROOT_ON_TOP_FALSE,
				new FrameSeparator(FrameCategorization.METHOD, DISTINGUISH_FRAMES_BY_OPTIMIZATION_FALSE), items);
		Fork root = smModel.getRootFork();
		assertEquals(1, root.getBranchCount());
		Branch branch = root.getBranch(0);
		assertTrue(branch.hasTail());
		assertEquals(1, branch.getTailFrames().length);
		assertNotEquals(branch.getFirstFrame(), branch.getLastFrame());
	}

	@Test
	public void testManyFrames() {
		IItemCollection items = MockCollections
				.getStackTraceCollection(MockStacktraceGenerator.generateTraces(TRUNCATED_FALSE, RECURSIVE_FALSE, 1000,
						NO_FRAME_CATEGORIZATION, DISTINGUISH_FRAMES_BY_OPTIMIZATION_FALSE));
		StacktraceModel smModel = new StacktraceModel(THREAD_ROOT_ON_TOP_FALSE,
				new FrameSeparator(FrameCategorization.METHOD, DISTINGUISH_FRAMES_BY_OPTIMIZATION_FALSE), items);
		Fork root = smModel.getRootFork();
		assertEquals(1, root.getBranchCount());
		Branch branch = root.getBranch(0);
		assertTrue(branch.hasTail());
		assertEquals(1000, branch.getTailFrames().length);
		// FIXME: Loop through all branches and frames, to make sure nothing breaks

	}

	@Test
	public void testTruncated() {
		IItemCollection items = MockCollections.getStackTraceCollection(MockStacktraceGenerator.generateTraces(
				TRUNCATED_TRUE, RECURSIVE_FALSE, 0, NO_FRAME_CATEGORIZATION, DISTINGUISH_FRAMES_BY_OPTIMIZATION_FALSE));
		StacktraceModel smModel = new StacktraceModel(THREAD_ROOT_ON_TOP_TRUE,
				new FrameSeparator(FrameCategorization.METHOD, DISTINGUISH_FRAMES_BY_OPTIMIZATION_FALSE), items);
		Fork root = smModel.getRootFork();
		assertEquals(1, root.getBranchCount());
		Branch branch = root.getBranch(0);
		assertEquals(StacktraceModel.UNKNOWN_FRAME, branch.getLastFrame().getFrame());
	}

	@Test
	public void testRecursive() {
		IItemCollection items = MockCollections
				.getStackTraceCollection(MockStacktraceGenerator.generateTraces(TRUNCATED_FALSE, RECURSIVE_TRUE, 0,
						FrameCategorization.METHOD, DISTINGUISH_FRAMES_BY_OPTIMIZATION_FALSE));
		StacktraceModel smModel = new StacktraceModel(THREAD_ROOT_ON_TOP_TRUE,
				new FrameSeparator(FrameCategorization.METHOD, DISTINGUISH_FRAMES_BY_OPTIMIZATION_FALSE), items);
		Fork root = smModel.getRootFork();
		assertEquals(1, root.getBranchCount());
		Branch branch = root.getBranch(0);

		assertNotEquals(branch.getFirstFrame(), branch.getLastFrame());
		assertEquals(branch.getFirstFrame().getFrame(), branch.getLastFrame().getFrame());
	}

	@Test
	public void testDifferentBCIDontSeparate() {
		IItemCollection items = MockCollections
				.getStackTraceCollection(MockStacktraceGenerator.generateTraces(TRUNCATED_FALSE, RECURSIVE_FALSE, 0,
						FrameCategorization.BCI, DISTINGUISH_FRAMES_BY_OPTIMIZATION_FALSE));
		StacktraceModel smModel = new StacktraceModel(THREAD_ROOT_ON_TOP_FALSE,
				new FrameSeparator(FrameCategorization.METHOD, DISTINGUISH_FRAMES_BY_OPTIMIZATION_FALSE), items);
		Fork root = smModel.getRootFork();
		assertEquals(1, root.getBranchCount());
	}

	@Test
	public void testDifferentBCISeparate() {
		IItemCollection items = MockCollections
				.getStackTraceCollection(MockStacktraceGenerator.generateTraces(TRUNCATED_FALSE, RECURSIVE_FALSE, 0,
						FrameCategorization.BCI, DISTINGUISH_FRAMES_BY_OPTIMIZATION_FALSE));
		StacktraceModel smModel = new StacktraceModel(THREAD_ROOT_ON_TOP_FALSE,
				new FrameSeparator(FrameCategorization.BCI, DISTINGUISH_FRAMES_BY_OPTIMIZATION_FALSE), items);
		Fork root = smModel.getRootFork();
		assertEquals(2, root.getBranchCount());
	}

	@Test
	public void testDifferentLineNumberSeparate() {
		IItemCollection items = MockCollections
				.getStackTraceCollection(MockStacktraceGenerator.generateTraces(TRUNCATED_FALSE, RECURSIVE_FALSE, 0,
						FrameCategorization.LINE, DISTINGUISH_FRAMES_BY_OPTIMIZATION_FALSE));
		StacktraceModel smModel = new StacktraceModel(THREAD_ROOT_ON_TOP_FALSE,
				new FrameSeparator(FrameCategorization.LINE, DISTINGUISH_FRAMES_BY_OPTIMIZATION_FALSE), items);
		Fork root = smModel.getRootFork();
		assertEquals(2, root.getBranchCount());
	}

	@Test
	public void testDifferentFrameTypeDontSeparate() {
		IItemCollection items = MockCollections.getStackTraceCollection(MockStacktraceGenerator.generateTraces(
				TRUNCATED_FALSE, RECURSIVE_FALSE, 0, NO_FRAME_CATEGORIZATION, DISTINGUISH_FRAMES_BY_OPTIMIZATION_TRUE));
		StacktraceModel smModel = new StacktraceModel(THREAD_ROOT_ON_TOP_FALSE,
				new FrameSeparator(FrameCategorization.LINE, DISTINGUISH_FRAMES_BY_OPTIMIZATION_FALSE), items);
		Fork root = smModel.getRootFork();
		assertEquals(1, root.getBranchCount());
	}

	@Test
	public void testDifferentFrameTypeSeparate() {
		IItemCollection items = MockCollections
				.getStackTraceCollection(MockStacktraceGenerator.generateTraces(TRUNCATED_FALSE, RECURSIVE_FALSE, 0,
						FrameCategorization.LINE, DISTINGUISH_FRAMES_BY_OPTIMIZATION_TRUE));
		StacktraceModel smModel = new StacktraceModel(THREAD_ROOT_ON_TOP_FALSE,
				new FrameSeparator(FrameCategorization.LINE, DISTINGUISH_FRAMES_BY_OPTIMIZATION_TRUE), items);
		Fork root = smModel.getRootFork();
		assertEquals(2, root.getBranchCount());
	}

}
