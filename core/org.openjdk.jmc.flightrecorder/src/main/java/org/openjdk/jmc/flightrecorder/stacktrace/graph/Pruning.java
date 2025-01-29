/*
 * Copyright (c) 2019, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, 2025, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.stacktrace.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openjdk.jmc.flightrecorder.stacktrace.graph.Node.NodeWrapper;

// implementation based on https://github.com/google/pprof/blob/83db2b799d1f74c40857232cb5eb4c60379fe6c2/internal/report/report.go#L124
public class Pruning {
	public static StacktraceGraphModel prune(StacktraceGraphModel model, int maxNodeCount, boolean trimLowFrequency) {
		// first phase: cutoff
		long totalValue = model.getNodes().stream().mapToLong(node -> node.count).sum();
		if (trimLowFrequency) {
			double nodeFraction = 0.005;
			long nodeCutoff = Math.round(totalValue * nodeFraction);
			if (nodeCutoff > 0) {
				model = discardLowFrequencyNodes(model, nodeCutoff);
			}
		}
		// second phase: entropy
		Map<Integer, Long> nodeScores = new HashMap<>();
		for (Node node : model.getNodes()) {
			long score = entropyScore(node);
			nodeScores.put(node.getNodeId(), score);
		}
		// sort
		List<Node> sortedNodes = new ArrayList<Node>(model.getNodes());
		sortedNodes.sort((n1, n2) -> {
			long score1 = nodeScores.get(n1.getNodeId());
			long score2 = nodeScores.get(n2.getNodeId());
			return -Long.compare(score1, score2);
		});
		if (trimLowFrequency) {
			double edgeFraction = 0.001;
			long edgeCutoff = Math.round(totalValue * edgeFraction);
			trimLowFrequencyEdges(sortedNodes, edgeCutoff);
		}
		// selectTopNodes
		return selectTopNode(model, sortedNodes, maxNodeCount);
	}

	private static StacktraceGraphModel discardLowFrequencyNodes(StacktraceGraphModel model, long nodeCutoff) {
		Set<AggregatableFrame> cutNodes = new HashSet<>(model.getNodes().size());
		for (Node node : model.getNodes()) {
			if (node.cumulativeWeight < nodeCutoff) {
				continue;
			}
			cutNodes.add(node.getFrame());
		}
		return new StacktraceGraphModel(model, cutNodes);
	}

	private static StacktraceGraphModel selectTopNode(
		StacktraceGraphModel model, Collection<Node> sortedNodes, int maxCount) {
		Set<AggregatableFrame> cutNodes = new HashSet<>(model.getNodes().size());
		int count = 0;
		for (Node node : sortedNodes) {
			cutNodes.add(node.getFrame());
			count++;
			if (count >= maxCount) {
				break;
			}
		}
		return new StacktraceGraphModel(model, cutNodes);
	}

	private static int trimLowFrequencyEdges(Collection<Node> sortedNode, long edgeCutoff) {
		int droppedEdges = 0;
		for (Node node : sortedNode) {

			for (Map.Entry<NodeWrapper, Edge> entry : new HashSet<>(node.getIn().entrySet())) {
				if (entry.getValue().value < edgeCutoff) {
					node.getIn().remove(entry.getKey());
					entry.getKey().node.getOut().remove(new NodeWrapper(node.getNodeId(), node));
					droppedEdges++;
				}
			}
		}
		return droppedEdges;
	}

	private static long entropyScore(Node node) {
		double score = 0;
		if (node.getIn().isEmpty()) {
			score++;
		} else {
			score += edgeEntropyScore(node, node.getIn().values(), 0);
		}
		if (node.getOut().isEmpty()) {
			score++;
		} else {
			score += edgeEntropyScore(node, node.getOut().values(), node.weight);
		}
		return Math.round(score * node.cumulativeWeight + node.weight);
	}

	private static double edgeEntropyScore(Node node, Collection<Edge> edges, double self) {
		double score = 0;
		double total = self;
		for (Edge edge : edges) {
			if (edge.getValue() > 0) {
				total += Math.abs(edge.getValue());
			}
		}
		if (total > 0) {
			for (Edge edge : edges) {
				double frac = Math.abs(edge.getValue()) / total;
				score += -frac * Math.log(frac);
			}
			if (self > 0) {
				double frac = Math.abs(self) / total;
				score += -frac * Math.log(frac);
			}
		}
		return score;
	}

}
