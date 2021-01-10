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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator.FrameCategorization;
import org.openjdk.jmc.flightrecorder.stacktrace.graph.Edge;
import org.openjdk.jmc.flightrecorder.stacktrace.graph.GraphModelUtils;
import org.openjdk.jmc.flightrecorder.stacktrace.graph.Node;
import org.openjdk.jmc.flightrecorder.stacktrace.graph.StacktraceGraphModel;

/**
 * Converts a {@link StacktraceGraphModel} to DOT format text. This format can, for example, be used
 * by d3-graphviz, to visualize the graphs.
 */
public final class DotGenerator {
	private static final String DEFAULT_NAME = "Unnamed";
	private static final String DEFAULT_SHAPE = "box";
	private static final String DEFAULT_STYLE = "filled";
	private static final String DEFAULT_FILL_COLOR = "#f8f8f8";
	private static final String DEFAULT_TITLE_FONT_SIZE = "16";
	private static final String DEFAULT_FONT_NAME = "helvetica";
	private static final String DEFAULT_NODE_FILL_COLOR = "#e1e1e1";
	private static final String DEFAULT_EDGE_STYLE = "solid";
	private static final String DEFAULT_MIN_EDGE_WEIGHT = "2";
	private static final String DEFAULT_MAX_EDGE_WEIGHT = "40";
	private static final String DEFAULT_NODE_SIZE_ATTRIBUTE = "count";
	private static final String DEFAULT_MIN_NODE_FONT_SIZE = "8";
	private static final String DEFAULT_MAX_NODE_FONT_SIZE = "32";

	public enum ConfigurationKey {
		/**
		 * The name of the graph.
		 */
		Name,
		/**
		 * The style, e.g. filled.
		 */
		Style,
		/**
		 * The fill color, e.g. #f8f8f8.
		 */
		Fillcolor,
		/**
		 * The name of the font to use, e.g. helvetica, or helvetica:italics.
		 */
		Fontname,
		/**
		 * Have title area, true or false.
		 */
		TitleArea,
		/**
		 * Font size of the title area, e.g. 16. Need to be parseable to a number.
		 */
		TitleFontSize,
		/**
		 * Shape of the title area, e.g. box.
		 */
		TitleShape,
		/**
		 * Shape of the nodes, e.g. box.
		 */
		NodeShape,
		/**
		 * The color of the node, e.g. #b22b00.
		 */
		NodeColor,
		/**
		 * The fill color of the node, e.g. #eddbd5.
		 */
		NodeFillColor,
		/**
		 * Font size for the biggest node. This will be the font size used for the node with the
		 * biggest count or weight.
		 */
		MaxNodeFontSize,
		/**
		 * Font size for the biggest node. This will be the font size used for the node with the
		 * smallest count or weight.
		 */
		MinNodeFontSize,
		/**
		 * The attribute to use for node font size. [count|weight]
		 */
		NodeSizeAttribute,
		/**
		 * The attribute to use for the edge weights. [count|weight]
		 */
		EdgeWeightAttribute,
		/**
		 * The style for the edges. [solid|dotted|dashed|bold]
		 */
		EdgeStyle,
		/**
		 * The max edge weight to use for the most traveled path.
		 */
		MaxEdgeWeight,
		/**
		 * The min edge weight to use for the least traveled path.
		 */
		MinEdgeWeight
	}

	private final static class NodeConfigurator {
		private static final String COUNT = DEFAULT_NODE_SIZE_ATTRIBUTE;
		private final String shape;
		private final boolean useCount;
		private final double minCount;
		private final double maxCount;
		private final double minRange;
		private final double maxRange;
		private final int minFontSize;
		private final int maxFontSize;
		private final String color;
		private final String fillColor;

		public NodeConfigurator(StacktraceGraphModel model, Map<ConfigurationKey, String> configuration) {
			useCount = getConf(configuration, ConfigurationKey.NodeSizeAttribute, COUNT).equals(COUNT);
			minCount = model.findNodeMinCount();
			maxCount = model.findNodeMaxCount();

			if (useCount) {
				maxRange = maxCount;
				minRange = minCount;
			} else {
				maxRange = model.findNodeMaxWeight();
				minRange = model.findNodeMinWeight();
			}
			maxFontSize = Integer
					.parseInt(getConf(configuration, ConfigurationKey.MaxNodeFontSize, DEFAULT_MAX_NODE_FONT_SIZE));
			minFontSize = Integer
					.parseInt(getConf(configuration, ConfigurationKey.MinNodeFontSize, DEFAULT_MIN_NODE_FONT_SIZE));
			shape = getConf(configuration, ConfigurationKey.NodeShape, DEFAULT_SHAPE);
			color = getConf(configuration, ConfigurationKey.NodeColor, "#b22b00");
			fillColor = getConf(configuration, ConfigurationKey.NodeFillColor, "#eddbd5");
		}

		public int getFontSize(Node node) {
			double value = useCount ? node.getCount() : node.getWeight();
			double fraction = (value - minRange) / (maxRange - minRange);
			return (int) Math.round((maxFontSize - minFontSize) * fraction + minFontSize);
		}
	}

	private final static class EdgeConfigurator {
		private final boolean useCount;
		private final double minWeight;
		private final double maxWeight;
		private final int minCount;
		private final int maxCount;
		private final double minRange;
		private final double maxRange;
		private final String style;

		public EdgeConfigurator(StacktraceGraphModel model, Map<ConfigurationKey, String> configuration) {
			useCount = getConf(configuration, ConfigurationKey.NodeSizeAttribute, DEFAULT_NODE_SIZE_ATTRIBUTE)
					.equals(DEFAULT_NODE_SIZE_ATTRIBUTE);
			minCount = model.findEdgeMinCount();
			maxCount = model.findEdgeMaxCount();

			if (useCount) {
				minRange = minCount;
				maxRange = maxCount;
			} else {
				minRange = model.findEdgeMinValue();
				maxRange = model.findEdgeMaxValue();
			}

			minWeight = Integer
					.parseInt(getConf(configuration, ConfigurationKey.MinEdgeWeight, DEFAULT_MIN_EDGE_WEIGHT));
			maxWeight = Integer
					.parseInt(getConf(configuration, ConfigurationKey.MaxEdgeWeight, DEFAULT_MAX_EDGE_WEIGHT));
			style = getConf(configuration, ConfigurationKey.EdgeStyle, DEFAULT_EDGE_STYLE);
		}

		public String generateTooltip(Edge e) {
			return e.getFrom().getFrame().getHumanReadableSeparatorSensitiveString() + " -> "
					+ e.getTo().getFrame().getHumanReadableSeparatorSensitiveString() + " (" + getPercentage(e) + " %)";
		}

		private String getPercentage(Edge e) {
			double val = 0;
			if (useCount) {
				val = ((double) e.getCount()) / maxCount;
			} else {
				val = e.getValue() / maxRange;
			}
			return String.format("%.3f", val);
		}

		/**
		 * This is the weight for the edge, not the edge value.
		 */
		public int getWeight(Edge edge) {
			double value = useCount ? edge.getCount() : edge.getValue();
			double fraction = (value - minRange) / (maxRange - minRange);
			return (int) Math.round((maxWeight - minWeight) * fraction + minWeight);
		}

		public boolean isMax(Edge edge) {
			if (useCount) {
				return edge.getCount() == maxCount;
			} else {
				return edge.getValue() == maxRange;
			}
		}

		public String getColor(Edge edge) {
			// if weight == 0, then have as gray as possible,
			// if weight == MAX_WEIGHT, keep it red.
			// TODO Auto-generated method stub
			int color = 0xb2 << 16;
			double value = useCount ? edge.getCount() : edge.getValue();
			double fraction = (value - minRange) / (maxRange - minRange);
			int colorval = (int) ((1 - fraction) * 0xb2);
			color = color | (colorval << 8) | colorval;
			return "#" + Integer.toHexString(color);
		}
	}

	/**
	 * Renders a {@link StacktraceGraphModel} in DOT format.
	 */
	public static String toDot(
		StacktraceGraphModel model, int maxNodesRendered, Map<ConfigurationKey, String> configuration) {
		StringBuilder builder = new StringBuilder(2048);
		String graphName = getConf(configuration, ConfigurationKey.Name, DEFAULT_NAME);
		builder.append(String.format("digraph \"%s\" {%n", graphName));
		int nodeCount = model.getNodes().size();
		if (nodeCount > maxNodesRendered) {
			String message = String.format("Too many nodes in current selection%n(max: %d, actual: %d)",
					maxNodesRendered, nodeCount);
			emitMessage(builder, message, configuration);
			builder.append("}");
			return builder.toString();
		} else if (nodeCount == 0) {
			emitEmptyMessage(builder, "No graph data in current selection", configuration);
			builder.append("}");
			return builder.toString();
		}
		createDefaultNodeSettingsEntry(builder, configuration);
		if (Boolean.valueOf(getConf(configuration, ConfigurationKey.TitleArea, "false"))) {
			createSubgraphNode(builder, graphName, configuration, model);
		}

		// Convert Nodes
		NodeConfigurator nodeConfigurator = new NodeConfigurator(model, configuration);
		model.getNodes().forEach((node) -> emitNode(builder, model, nodeConfigurator, node));

		// Convert Edges
		EdgeConfigurator edgeConfigurator = new EdgeConfigurator(model, configuration);
		model.getEdges().forEach((edge) -> emitEdge(builder, model, edgeConfigurator, edge));

		builder.append("}");
		return builder.toString();
	}

	private static void createDefaultNodeSettingsEntry(
		StringBuilder builder, Map<ConfigurationKey, String> configuration) {
		builder.append("node [style=");
		builder.append(getConf(configuration, ConfigurationKey.Style, DEFAULT_STYLE));
		builder.append(" fillcolor=\"");
		builder.append(getConf(configuration, ConfigurationKey.Fillcolor, DEFAULT_FILL_COLOR));
		builder.append("\" fontname=\"");
		builder.append(getConf(configuration, ConfigurationKey.Fontname, DEFAULT_FONT_NAME));
		builder.append("\"]\n");
	}

	private static void emitEdge(
		StringBuilder builder, StacktraceGraphModel model, EdgeConfigurator edgeConfigurator, Edge edge) {
		builder.append("N");
		builder.append(edge.getFrom().getNodeId());
		builder.append(" -> N");
		builder.append(edge.getTo().getNodeId());
		builder.append(" [label=\"");
		if (edgeConfigurator.useCount) {
			builder.append(edge.getCount());
		} else {
			builder.append(edge.getValue());
		}
		builder.append("\"");
		int weight = edgeConfigurator.getWeight(edge);
		if (weight >= 2) {
			builder.append(" weight=");
			builder.append(weight);
		}
		builder.append(edgeConfigurator.isMax(edge) ? " penwidth=2 " : " ");
		builder.append("color=\"");
		builder.append(edgeConfigurator.getColor(edge));
		builder.append("\" tooltip=\"");
		String tooltip = edgeConfigurator.generateTooltip(edge);
		builder.append(tooltip);
		builder.append("\" labeltooltip=\"");
		builder.append(tooltip);
		builder.append("\" style=\"");
		builder.append(edgeConfigurator.style);
		builder.append("\"]\n");
	}

	private static void emitNode(
		StringBuilder builder, StacktraceGraphModel model, NodeConfigurator configurator, Node node) {
		String percentOfSamples = String.format("%.3f %%", node.getCount() * 100.0 / model.getTotalTraceCount());
		builder.append("N");
		builder.append(node.getNodeId());
		builder.append(" [label=\"");
		builder.append(node.getFrame().getHumanReadableSeparatorSensitiveString());
		builder.append("\\nSamples: ");
		builder.append(node.getCount());
		builder.append(" (");
		builder.append(percentOfSamples);
		builder.append(")\" id=\"node");
		builder.append(node.getNodeId());
		builder.append("\" fontsize=");
		builder.append(configurator.getFontSize(node));
		builder.append(" shape=");
		builder.append(configurator.shape);
		builder.append(" tooltip=\"");
		builder.append(node.getFrame().getHumanReadableSeparatorSensitiveString());
		builder.append(" (");
		builder.append(percentOfSamples);
		builder.append(" %)\" color=\"");
		builder.append(configurator.color);
		builder.append("\" fillcolor=\"");
		builder.append(configurator.fillColor);
		builder.append("\"]\n");
	}

	private static void emitMessage(
		StringBuilder builder, String message, Map<ConfigurationKey, String> configuration) {
		String shape = getConf(configuration, ConfigurationKey.NodeShape, DEFAULT_SHAPE);
		String color = getConf(configuration, ConfigurationKey.NodeColor, "#b22b00");
		String fillColor = getConf(configuration, ConfigurationKey.NodeFillColor, "#eddbd5");
		builder.append("message [label=\"");
		builder.append(message);
		builder.append("\" id=\"message\"");
		builder.append(" shape=");
		builder.append(shape);
		builder.append(" fontsize=5");
		builder.append(" color=\"");
		builder.append(color);
		builder.append("\" fillcolor=\"");
		builder.append(fillColor);
		builder.append("\"]\n");
	}

	private static void emitEmptyMessage(
		StringBuilder builder, String message, Map<ConfigurationKey, String> configuration) {
		builder.append("label= <<font color='gray' point-size='1'>");
		builder.append(message);
		builder.append("</font>>");
	}

	private static void createSubgraphNode(
		StringBuilder builder, String graphName, Map<ConfigurationKey, String> configuration,
		StacktraceGraphModel model) {
		builder.append("subgraph cluster_L { ");
		builder.append("\"");
		builder.append(graphName);
		builder.append("\" [shape=");
		builder.append(getConf(configuration, ConfigurationKey.TitleShape, DEFAULT_SHAPE));
		builder.append(" fontsize=");
		builder.append(getConf(configuration, ConfigurationKey.TitleFontSize, DEFAULT_TITLE_FONT_SIZE));
		builder.append(" label=\"");
		builder.append(graphName);
		builder.append("\\nTypes: ");
		builder.append(GraphModelUtils.getTypeNames(model.getItems()));
		builder.append("\\lTotal samples = ");
		builder.append(model.getTotalTraceCount());
		builder.append("\\lTotal edge count = ");
		builder.append(model.getTotalEdgeCount());
		builder.append("\\l\" tooltip=\"");
		builder.append(graphName);
		builder.append("\"] }\n");
	}

	/**
	 * @return an example configuration for the dot files, using the defaults.
	 */
	public static Map<ConfigurationKey, String> getDefaultConfiguration() {
		Map<ConfigurationKey, String> configuration = new HashMap<>();
		configuration.put(ConfigurationKey.Name, DEFAULT_NAME);
		configuration.put(ConfigurationKey.Fillcolor, DEFAULT_FILL_COLOR);
		configuration.put(ConfigurationKey.NodeFillColor, DEFAULT_NODE_FILL_COLOR);
		configuration.put(ConfigurationKey.Style, DEFAULT_STYLE);
		configuration.put(ConfigurationKey.TitleShape, DEFAULT_SHAPE);
		configuration.put(ConfigurationKey.TitleFontSize, DEFAULT_TITLE_FONT_SIZE);
		configuration.put(ConfigurationKey.NodeShape, DEFAULT_SHAPE);
		configuration.put(ConfigurationKey.NodeSizeAttribute, DEFAULT_NODE_SIZE_ATTRIBUTE);
		configuration.put(ConfigurationKey.MaxNodeFontSize, DEFAULT_MAX_NODE_FONT_SIZE);
		configuration.put(ConfigurationKey.MinNodeFontSize, DEFAULT_MIN_NODE_FONT_SIZE);
		configuration.put(ConfigurationKey.MaxEdgeWeight, DEFAULT_MAX_EDGE_WEIGHT);
		configuration.put(ConfigurationKey.MinEdgeWeight, DEFAULT_MIN_EDGE_WEIGHT);
		configuration.put(ConfigurationKey.EdgeStyle, DEFAULT_EDGE_STYLE);
		configuration.put(ConfigurationKey.Fontname, DEFAULT_FONT_NAME);
		return configuration;
	}

	private static String getConf(
		Map<ConfigurationKey, String> configuration, ConfigurationKey key, String defaultValue) {
		String value = configuration.get(key);
		return value == null ? defaultValue : value;
	}

	/**
	 * Generates a dot file for the CPU profiling events available in the recording.
	 * 
	 * @param args
	 * @throws IOException
	 * @throws CouldNotLoadRecordingException
	 */
	public static void main(String[] args) throws IOException, CouldNotLoadRecordingException {
		File jfrFile = new File(args[0]);
		IItemCollection items = JfrLoaderToolkit.loadEvents(jfrFile);
		IItemCollection filteredItems = items.apply(JdkFilters.EXECUTION_SAMPLE);
		FrameSeparator frameSeparator = new FrameSeparator(FrameCategorization.METHOD, false);
		StacktraceGraphModel model = new StacktraceGraphModel(frameSeparator, filteredItems, null);
		Map<ConfigurationKey, String> configuration = getDefaultConfiguration();
		configuration.put(ConfigurationKey.Name, jfrFile.getName());
		System.out.println(toDot(model, 1000, configuration));
	}
}
