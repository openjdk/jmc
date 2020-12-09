/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.rules.jdk.dataproviders;

import java.util.ArrayList;
import java.util.List;

import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceFrame;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel.Branch;

/**
 * Helper class for dealing with stack traces.
 */
public class StacktraceDataProvider {
	private static final int MAX_TAIL_FRAMES = 5;

	/**
	 * Generates an HTML representation of a given stack trace branch as an unordered list.
	 *
	 * @param branch
	 *            the branch to represent as HTML
	 * @param rootItems
	 *            the total amount of frames in the root branch
	 * @return an HTML representation of a stack trace
	 */
	public static List<IMCMethod> getRelevantTraceList(Branch branch, int rootItems) {
		double threshold = rootItems / 5d;
		List<IMCMethod> frames = new ArrayList<>();
		if (branch.getFirstFrame().getItemCount() >= threshold) {
			IMCMethod firstMethod = branch.getFirstFrame().getFrame().getMethod();
			frames.add(firstMethod);
			StacktraceFrame[] tailFrames = branch.getTailFrames();
			for (int i = 0; i < tailFrames.length && i < MAX_TAIL_FRAMES; i++) {
				StacktraceFrame stacktraceFrame = tailFrames[i];
				IMCMethod method = stacktraceFrame.getFrame().getMethod();
				frames.add(method);
			}
		}
		return frames;
	}
}
