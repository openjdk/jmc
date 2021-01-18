/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, 2021, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.stacktrace.graph;

import static org.openjdk.jmc.flightrecorder.JfrAttributes.EVENT_STACKTRACE;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator.FrameCategorization;

/**
 * A model for holding multiple stack traces and their relations to each other as a directed graph.
 * <p>
 * The model is state-less. The model is created and built once, and read only.
 * <p>
 * Instances of this class are thread safe.
 * <p>
 * The typical way of using this class is to first decide on the {@link FrameSeparator} and then
 * create the model.
 * <p>
 * Opening a Java flight Recording and setting up the stack trace model can be done like this:
 *
 * <pre>
 * IItemCollection items = JfrLoaderToolkit.loadEvents(file);
 * IItemCollection filteredItems = items.apply(JdkFilters.EXECUTION_SAMPLE);
 * FrameSeparator frameSeparator = new FrameSeparator(FrameCategorization.METHOD, false);
 * StacktraceGraphModel model = new StacktraceGraphModel(frameSeparator, filteredItems);
 * </pre>
 */
public final class StacktraceGraphModel {
	private final FrameSeparator frameSeparator;
	private final IItemCollection items;
	private final IAttribute<IQuantity> attribute;
	private int totalTraceCount;
	private int totalEdgeCount;
	private int nodeCounter;

	/**
	 * From node id -> Edge
	 */
	private final Map<Integer, Set<Edge>> edges = new HashMap<>(1024);

	/**
	 * Frame -> Node
	 */
	private final Map<AggregatableFrame, Node> nodes = new HashMap<>(1024);

	/**
	 * Constructor.
	 * 
	 * @param frameSeparator
	 *            how to aggregate frames.
	 * @param items
	 *            the items to graph.
	 * @param attribute
	 *            the (optional) attribute to use for calculating the values.
	 */
	public StacktraceGraphModel(FrameSeparator frameSeparator, IItemCollection items, IAttribute<IQuantity> attribute) {
		this.frameSeparator = frameSeparator;
		this.items = items;
		this.attribute = attribute;
		buildModel();
	}

	public Collection<Edge> getEdges() {
		return edges.values().stream().flatMap((c) -> c.stream()).collect(Collectors.toSet());
	}

	public Collection<Node> getNodes() {
		return nodes.values();
	}

	public IAttribute<IQuantity> getAttribute() {
		return attribute;
	}

	public boolean isEmpty() {
		return nodes.isEmpty();
	}

	public IItemCollection getItems() {
		return items;
	}

	/**
	 * @return the total edge count, i.e. the total sum of counts for all edges.
	 */
	public int getTotalEdgeCount() {
		return totalEdgeCount;
	}

	/**
	 * @return the total amount of stack traces use to build this model.
	 */
	public int getTotalTraceCount() {
		return totalTraceCount;
	}

	/**
	 * Searches the nodes for the min count.
	 * 
	 * @return the min count.
	 */
	public int findNodeMinCount() {
		int minCount = Integer.MAX_VALUE;
		for (Node n : getNodes()) {
			minCount = Math.min(n.getCount(), minCount);
		}
		return minCount;
	}

	/**
	 * Searches the nodes for the max count.
	 * 
	 * @return the max count.
	 */
	public int findNodeMaxCount() {
		int maxCount = 0;
		for (Node n : getNodes()) {
			maxCount = Math.max(n.getCount(), maxCount);
		}
		return maxCount;
	}

	/**
	 * Searches the nodes for the min weight.
	 * 
	 * @return the min weight.
	 */
	public double findNodeMinWeight() {
		double minWeight = Double.MAX_VALUE;
		for (Node n : getNodes()) {
			minWeight = Math.min(n.getWeight(), minWeight);
		}
		return minWeight;
	}

	/**
	 * Searches the nodes for the max weight.
	 * 
	 * @return the max weight.
	 */
	public double findNodeMaxWeight() {
		double maxWeight = 0.0d;
		for (Node n : getNodes()) {
			maxWeight = Math.max(n.getWeight(), maxWeight);
		}
		return maxWeight;
	}

	/**
	 * Searches the edges for the min value.
	 * 
	 * @return the min value.
	 */
	public double findEdgeMinValue() {
		double minValue = Double.MAX_VALUE;
		for (Edge e : getEdges()) {
			minValue = Math.min(e.getValue(), minValue);
		}
		return minValue;
	}

	/**
	 * Searches the edges for the max value.
	 * 
	 * @return the max value.
	 */
	public double findEdgeMaxValue() {
		double maxValue = 0.0d;
		for (Edge e : getEdges()) {
			maxValue = Math.max(e.getValue(), maxValue);
		}
		return maxValue;
	}

	/**
	 * Searches the edges for the min count.
	 * 
	 * @return the min count.
	 */
	public int findEdgeMinCount() {
		int minValue = Integer.MAX_VALUE;
		for (Edge e : getEdges()) {
			minValue = Math.min(e.getCount(), minValue);
		}
		return minValue;
	}

	/**
	 * Searches the edges for the max count.
	 * 
	 * @return the max count.
	 */
	public int findEdgeMaxCount() {
		int maxValue = 0;
		for (Edge e : getEdges()) {
			maxValue = Math.max(e.getCount(), maxValue);
		}
		return maxValue;
	}

	@Override
	public String toString() {
		return String.format(
				"=== StackTraceModel ===\nNode Count:%d\nEdge Count:%d\nNodes: %s\nEdges: %s\n========================",
				nodes.size(), edges.size(), nodes.toString(), edges.toString());
	}

	private void buildModel() {
		for (IItemIterable iterable : items) {
			IMemberAccessor<IMCStackTrace, IItem> stacktraceAccessor = getAccessor(iterable, EVENT_STACKTRACE);
			if (stacktraceAccessor == null) {
				continue;
			}
			iterable.forEach((item) -> addItem(item, stacktraceAccessor, getAccessor(iterable, attribute)));
		}
	}

	private static <T> IMemberAccessor<T, IItem> getAccessor(IItemIterable iterable, IAttribute<T> attr) {
		return (attr != null) ? iterable.getType().getAccessor(attr.getKey()) : null;
	}

	private void addItem(
		IItem item, IMemberAccessor<IMCStackTrace, IItem> stackTraceAccessor,
		IMemberAccessor<IQuantity, IItem> quantityAccessor) {
		IMCStackTrace stackTrace = stackTraceAccessor.getMember(item);
		if (stackTrace == null) {
			return;
		}
		List<? extends IMCFrame> frames = stackTrace.getFrames();
		if (frames.isEmpty()) {
			return;
		}

		double value = 0;
		if (quantityAccessor != null) {
			value = quantityAccessor.getMember(item).doubleValue();
		}

		// First frame is the frame where things are actually happening, i.e. the method
		// actually responsible for whatever is being tracked (e.g. the method being on
		// CPU, the method triggering the allocation etc) - it is for this node we
		// increment the count...
		IMCFrame firstFrame = frames.get(0);
		Node n = getOrCreateNode(firstFrame);
		totalTraceCount++;
		n.count++;
		n.weight += value;

		// Next go through all frames from the thread root, and up the cumulative counts
		for (int i = frames.size() - 1; i > 0; i--) {
			// Process two frames sliding window, from and to
			IMCFrame currentFrame = frames.get(i);
			IMCFrame nextFrame = frames.get(i - 1);

			Node currentNode = getOrCreateNode(currentFrame);
			Node nextNode = getOrCreateNode(nextFrame);

			currentNode.cumulativeCount++;
			nextNode.cumulativeCount++;
			currentNode.cumulativeWeight += value;
			nextNode.cumulativeWeight += value;
			Edge e = getOrCreateLink(currentNode, nextNode);
			e.count++;
			totalEdgeCount++;
		}
	}

	private Node getOrCreateNode(IMCFrame frame) {
		AggregatableFrame aframe = new AggregatableFrame(frameSeparator, frame);
		Node n = nodes.get(aframe);
		if (n == null) {
			n = new Node(Integer.valueOf(nodeCounter++), aframe);
			nodes.put(aframe, n);
		}
		return n;
	}

	private Edge getOrCreateLink(Node fromNode, Node toNode) {
		if (!edges.containsKey(fromNode.getNodeId())) {
			Edge edge = new Edge(fromNode, toNode);
			Set<Edge> newEdgeSet = new HashSet<>();
			newEdgeSet.add(edge);
			edges.put(fromNode.getNodeId(), newEdgeSet);
			return edge;
		}
		Set<Edge> toSet = edges.get(fromNode.getNodeId());
		// We assume that we have a reasonable amount of edges from a node - so linear
		// search is ok
		for (Edge edge : toSet) {
			if (edge.getTo().equals(toNode)) {
				return edge;
			}
		}
		Edge edge = new Edge(fromNode, toNode);
		toSet.add(edge);
		return edge;
	}

	public static void main(String[] args) throws IOException, CouldNotLoadRecordingException {
		IItemCollection items = JfrLoaderToolkit.loadEvents(new File(args[0]));
		IItemCollection filteredItems = items.apply(JdkFilters.EXECUTION_SAMPLE);
		FrameSeparator frameSeparator = new FrameSeparator(FrameCategorization.METHOD, false);
		StacktraceGraphModel model = new StacktraceGraphModel(frameSeparator, filteredItems, null);
		System.out.println(GraphModelUtils.printGraph(model));
	}
}
