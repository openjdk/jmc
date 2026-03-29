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
package org.openjdk.jmc.ui.ai.tools;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator.FrameCategorization;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.AggregatableFrame;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;
import org.openjdk.jmc.ui.ai.IAITool;

public class GetStackTraceTool implements IAITool {

	private static final int MAX_NODES = 30;
	private static final Pattern TYPE_PATTERN = Pattern.compile("\"eventType\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""); //$NON-NLS-1$
	private static final Pattern ATTR_PATTERN = Pattern.compile("\"attribute\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""); //$NON-NLS-1$
	private static final Pattern LIMIT_PATTERN = Pattern.compile("\"limit\"\\s*:\\s*(\\d+)"); //$NON-NLS-1$
	private static final Pattern FROM_PATTERN = Pattern.compile("\"fromSeconds\"\\s*:\\s*([\\d.]+)"); //$NON-NLS-1$
	private static final Pattern TO_PATTERN = Pattern.compile("\"toSeconds\"\\s*:\\s*([\\d.]+)"); //$NON-NLS-1$

	@Override
	public String getName() {
		return "get_stacktrace"; //$NON-NLS-1$
	}

	@Override
	public String getDescription() {
		return "Gets the aggregated stack trace tree (flame graph data) for events in the recording." //$NON-NLS-1$
				+ " Shows the hottest methods and their call chains." //$NON-NLS-1$
				+ " The attribute parameter controls what the width represents:" //$NON-NLS-1$
				+ " count (sample count), duration (total time), allocationSize (bytes allocated)," //$NON-NLS-1$
				+ " or size (I/O bytes). Defaults to execution samples weighted by count." //$NON-NLS-1$
				+ " For allocation profiling, prefer jdk.ObjectAllocationSample (JDK 16+)." //$NON-NLS-1$
				+ " Only fall back to jdk.ObjectAllocationInNewTLAB/OutsideTLAB for older JDKs."; //$NON-NLS-1$
	}

	@Override
	public String getParameterSchema() {
		return "{\"type\":\"object\",\"properties\":{" //$NON-NLS-1$
				+ "\"eventType\":{\"type\":\"string\",\"description\":\"JFR event type ID (default: jdk.ExecutionSample)\"}," //$NON-NLS-1$
				+ "\"attribute\":{\"type\":\"string\"," //$NON-NLS-1$
				+ "\"description\":\"Attribute for flame graph cell width: count, duration, allocationSize, or size (default: count)\"," //$NON-NLS-1$
				+ "\"enum\":[\"count\",\"duration\",\"allocationSize\",\"size\"]}," //$NON-NLS-1$
				+ "\"fromSeconds\":{\"type\":\"number\",\"description\":\"Start of time range in seconds from recording start\"}," //$NON-NLS-1$
				+ "\"toSeconds\":{\"type\":\"number\",\"description\":\"End of time range in seconds from recording start\"}," //$NON-NLS-1$
				+ "\"limit\":{\"type\":\"integer\",\"description\":\"Max nodes to return (default 20)\"}" //$NON-NLS-1$
				+ "}}"; //$NON-NLS-1$
	}

	@Override
	public String execute(String parametersJson) {
		IItemCollection items = JfrContext.getActiveItems();
		if (items == null) {
			return "No flight recording is currently open."; //$NON-NLS-1$
		}

		String eventType = JfrContext.extractString(TYPE_PATTERN, parametersJson);
		if (eventType == null) {
			eventType = "jdk.ExecutionSample"; //$NON-NLS-1$
		}

		int limit = JfrContext.extractInt(LIMIT_PATTERN, parametersJson, 20);
		if (limit > MAX_NODES) {
			limit = MAX_NODES;
		}

		String attrName = JfrContext.extractString(ATTR_PATTERN, parametersJson);
		IAttribute<IQuantity> attribute = resolveAttribute(attrName);
		String weightLabel = attrName != null ? attrName : "count"; //$NON-NLS-1$

		String from = JfrContext.extractString(FROM_PATTERN, parametersJson);
		String to = JfrContext.extractString(TO_PATTERN, parametersJson);
		IItemCollection filtered = JfrContext.filterItems(items, eventType, from, to);
		if (!filtered.hasItems()) {
			return "No events found for type: " + eventType; //$NON-NLS-1$
		}

		FrameSeparator separator = new FrameSeparator(FrameCategorization.METHOD, false);
		StacktraceTreeModel tree = attribute != null ? new StacktraceTreeModel(filtered, separator, false, attribute)
				: new StacktraceTreeModel(filtered, separator);
		Node root = tree.getRoot();

		// Collect all nodes and sort by weight (hottest first)
		List<Node> allNodes = new ArrayList<>();
		collectNodes(root, allNodes);
		allNodes.sort(Comparator.comparingDouble(Node::getWeight).reversed());

		StringBuilder sb = new StringBuilder();
		sb.append("Stack trace analysis for ").append(eventType) //$NON-NLS-1$
				.append(" (weighted by ").append(weightLabel).append("):\n"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("Total weight: ").append(String.format("%.0f", root.getCumulativeWeight())).append("\n\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sb.append("Hottest methods (by self weight):\n"); //$NON-NLS-1$

		int count = 0;
		for (Node node : allNodes) {
			if (count >= limit || node.getWeight() <= 0) {
				break;
			}
			String methodName = formatFrame(node.getFrame());
			double pct = root.getCumulativeWeight() > 0 ? (node.getWeight() / root.getCumulativeWeight()) * 100 : 0;
			sb.append(String.format("  %5.1f%% (%4.0f samples) %s%n", pct, node.getWeight(), methodName)); //$NON-NLS-1$

			// Show call chain (parent path)
			StringBuilder chain = new StringBuilder();
			Node parent = node.getParent();
			int depth = 0;
			while (parent != null && !parent.isRoot() && depth < 5) {
				chain.insert(0, "    <- " + formatFrame(parent.getFrame()) + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
				parent = parent.getParent();
				depth++;
			}
			if (chain.length() > 0) {
				sb.append(chain);
			}
			count++;
		}
		return sb.toString();
	}

	private void collectNodes(Node node, List<Node> result) {
		if (!node.isRoot()) {
			result.add(node);
		}
		for (Node child : node.getChildren()) {
			collectNodes(child, result);
		}
	}

	private String formatFrame(AggregatableFrame frame) {
		if (frame == null) {
			return "<unknown>"; //$NON-NLS-1$
		}
		IMCMethod method = frame.getMethod();
		if (method == null) {
			return "<unknown>"; //$NON-NLS-1$
		}
		String className = method.getType() != null ? method.getType().getFullName() : "?"; //$NON-NLS-1$
		String methodName = method.getMethodName() != null ? method.getMethodName() : "?"; //$NON-NLS-1$
		return className + "." + methodName + "()"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	private IAttribute<IQuantity> resolveAttribute(String name) {
		if (name == null || "count".equals(name)) { //$NON-NLS-1$
			return null; // count-based (default)
		}
		switch (name) {
		case "duration": //$NON-NLS-1$
			return JfrAttributes.DURATION;
		case "allocationSize": //$NON-NLS-1$
			return JdkAttributes.ALLOCATION_SIZE;
		case "size": //$NON-NLS-1$
			return JdkAttributes.IO_SIZE;
		default:
			return null;
		}
	}

}
