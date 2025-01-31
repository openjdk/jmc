/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, 2025, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.serializers.stacktraces;

import org.openjdk.jmc.flightrecorder.stacktrace.tree.AggregatableFrame;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts a {@link StacktraceTreeModel} to a collapsed format that can be used as input for the
 * flamegraph perl script from https://github.com/brendangregg/FlameGraph repository.
 */
public class CollapsedSerializer {

	/**
	 * Serializes a {@link StacktraceTreeModel} to collasped format.
	 *
	 * @param model
	 *            the {@link StacktraceTreeModel} to serialize to collapsed format.
	 * @return a String containing the serialized model.
	 */
	public static String toCollapsed(StacktraceTreeModel model) {
		StringBuilder sb = new StringBuilder();
		List<String> lines = new ArrayList<>();
		toCollapsed(sb, lines, model, model.getRoot());
		return String.join("\n", lines);
	}

	private static void toCollapsed(StringBuilder sb, List<String> lines, StacktraceTreeModel model, Node node) {
		if (!node.isRoot()) {
			appendFrame(sb, node.getFrame(), node.getCumulativeWeight());
		}
		if (node.getChildren().isEmpty()) {
			lines.add(sb.toString() + " " + (int) node.getCumulativeWeight());
			return;
		}
		for (Node child : node.getChildren()) {
			toCollapsed(new StringBuilder(sb), lines, model, child);
		}
	}

	private static void appendFrame(StringBuilder sb, AggregatableFrame frame, double value) {
		if (sb.length() > 0) {
			sb.append(";");
		}
		sb.append(frame.getHumanReadableShortString());
	}
}
