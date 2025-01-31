/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, 2025, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.serializers.json;

import static org.openjdk.jmc.flightrecorder.serializers.internal.Messages.FLAMEGRAPH_SELECT_HTML_MORE;
import static org.openjdk.jmc.flightrecorder.serializers.internal.Messages.FLAMEGRAPH_SELECT_HTML_TABLE_EVENT_PATTERN;
import static org.openjdk.jmc.flightrecorder.serializers.internal.Messages.FLAMEGRAPH_SELECT_HTML_TABLE_EVENT_REST_PATTERN;
import static org.openjdk.jmc.flightrecorder.serializers.internal.Messages.FLAMEGRAPH_SELECT_ROOT_NODE;
import static org.openjdk.jmc.flightrecorder.serializers.internal.Messages.FLAMEGRAPH_SELECT_ROOT_NODE_EVENT;
import static org.openjdk.jmc.flightrecorder.serializers.internal.Messages.FLAMEGRAPH_SELECT_ROOT_NODE_EVENTS;
import static org.openjdk.jmc.flightrecorder.serializers.internal.Messages.FLAMEGRAPH_SELECT_ROOT_NODE_TYPE;
import static org.openjdk.jmc.flightrecorder.serializers.internal.Messages.FLAMEGRAPH_SELECT_ROOT_NODE_TYPES;
import static org.openjdk.jmc.flightrecorder.serializers.internal.Messages.FLAMEGRAPH_SELECT_STACKTRACE_NOT_AVAILABLE;
import static org.openjdk.jmc.flightrecorder.serializers.internal.Messages.FLAMEGRAPH_SELECT_TITLE_EVENT_MORE_DELIMITER;
import static org.openjdk.jmc.flightrecorder.serializers.internal.Messages.FLAMEGRAPH_SELECT_TITLE_EVENT_PATTERN;
import static org.openjdk.jmc.flightrecorder.stacktrace.Messages.STACKTRACE_UNCLASSIFIABLE_FRAME;
import static org.openjdk.jmc.flightrecorder.stacktrace.Messages.STACKTRACE_UNCLASSIFIABLE_FRAME_DESC;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.serializers.internal.Messages;
import org.openjdk.jmc.common.util.FormatToolkit;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator.FrameCategorization;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.AggregatableFrame;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;

/**
 * Produces a tree model suitable for rendering flame graphs.
 */
public class FlameGraphJsonSerializer {
	private static final String UNCLASSIFIABLE_FRAME = Messages.getString(STACKTRACE_UNCLASSIFIABLE_FRAME);
	private static final String UNCLASSIFIABLE_FRAME_DESC = Messages.getString(STACKTRACE_UNCLASSIFIABLE_FRAME_DESC);
	private final static int MAX_TYPES_IN_ROOT_TITLE = 2;
	private final static int MAX_TYPES_IN_ROOT_DESCRIPTION = 10;

	/**
	 * Serializes a {@link StacktraceTreeModel} to JSON.
	 * 
	 * @param model
	 *            the {@link StacktraceTreeModel} to serialize to JSON.
	 * @return a String containing the serialized model.
	 */
	public static String toJson(StacktraceTreeModel model) {
		return toJson(model, model.getRoot());
	}

	private static String toJson(StacktraceTreeModel model, Node node) {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		if (node.isRoot()) {
			sb.append(createRootNodeJson(model));
		} else {
			sb.append(createNodeJsonProps(node.getFrame(), node.getCumulativeWeight()));
		}

		sb.append(", ").append(addQuotes("c")).append(": [ ");
		boolean first = true;
		for (Node child : node.getChildren()) {
			if (!first) {
				sb.append(",");
			}
			sb.append(toJson(model, child));
			first = false;
		}
		sb.append("]").append("}");
		return sb.toString();
	}

	private static String createNodeJsonProps(AggregatableFrame frame, double value) {
		StringBuilder sb = new StringBuilder();
		if (frame.getType().equals(IMCFrame.Type.UNKNOWN) && frame.getHumanReadableShortString().equals(".()")) {
			// TODO: find recording with truncated stacks and add unit test for this case
			sb.append(addQuotes("n")).append(": ").append(addQuotes(UNCLASSIFIABLE_FRAME));
			sb.append(",");
			sb.append(addQuotes("d")).append(": ").append(addQuotes(UNCLASSIFIABLE_FRAME_DESC));
			sb.append(",");
			sb.append(addQuotes("p")).append(": ").append(addQuotes(""));
			sb.append(",");
		} else {
			String frameName = frame.getHumanReadableShortString();
			String packageName = FormatToolkit.getPackage(frame.getMethod().getType().getPackage());
			sb.append(addQuotes("n")).append(": ").append(addQuotes(frameName));
			sb.append(",");
			sb.append(addQuotes("p")).append(": ").append(addQuotes(packageName));
			sb.append(",");
		}
		sb.append(addQuotes("v")).append(": ").append(String.valueOf((int) value));
		return sb.toString();
	}

	private static String createJsonProps(String frameName, String description, double value) {
		StringBuilder sb = new StringBuilder();
		sb.append(addQuotes("n")).append(": ").append(addQuotes(frameName));
		sb.append(",");
		sb.append(addQuotes("p")).append(": ").append(addQuotes(""));
		sb.append(",");
		sb.append(addQuotes("d")).append(": ").append(addQuotes(description));
		sb.append(",");
		sb.append(addQuotes("v")).append(": ").append(String.valueOf((int) value));
		return sb.toString();
	}

	private static String addQuotes(String str) {
		return String.format("\"%s\"", str);
	}

	private static Map<String, Long> countEventsByType(IItemCollection items) {
		final HashMap<String, Long> eventCountByType = new HashMap<>();
		for (IItemIterable eventIterable : items) {
			if (eventIterable.getItemCount() == 0) {
				continue;
			}
			String typeName = eventIterable.getType().getName();
			long newValue = eventCountByType.getOrDefault(typeName, 0L) + eventIterable.getItemCount();
			eventCountByType.put(typeName, newValue);
		}
		// sort the map in ascending order of values
		return eventCountByType.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}

	private static String createRootNodeJson(StacktraceTreeModel model) {
		Map<String, Long> eventCountsByType = countEventsByType(model.getItems());
		String rootTitle = createRootNodeTitle(model, eventCountsByType);
		String rootDescription = createRootNodeDescription(eventCountsByType);
		double rootValue;
		if (model.getAttribute() == null) {
			rootValue = eventValueSum(eventCountsByType);
		} else {
			IQuantity aggregate = model.getItems().getAggregate(Aggregators.sum(model.getAttribute()));
			if (aggregate == null) {
				rootValue = eventValueSum(eventCountsByType);
			} else {
				rootValue = aggregate.doubleValue();
				if (model.getAttribute().getContentType() == UnitLookup.MEMORY) {
					rootValue = rootValue / 1024;
				}
			}
		}
		return createJsonProps(rootTitle, rootDescription, rootValue);
	}

	private static double eventValueSum(Map<String, Long> eventCountsByType) {
		return eventCountsByType.values().stream().mapToLong(Long::longValue).sum();
	}

	private static String createRootNodeTitle(StacktraceTreeModel model, Map<String, Long> eventCountsByType) {
		int eventsInTitle = Math.min(eventCountsByType.size(), MAX_TYPES_IN_ROOT_TITLE);
		long totalEvents = eventCountsByType.values().stream().mapToLong(Long::longValue).sum();
		if (totalEvents == 0) {
			return Messages.getString(FLAMEGRAPH_SELECT_STACKTRACE_NOT_AVAILABLE);
		}
		StringBuilder title = new StringBuilder(createRootNodeTitlePrefix(totalEvents, eventCountsByType.size()));
		int i = 0;
		for (Map.Entry<String, Long> entry : eventCountsByType.entrySet()) {
			String eventType = Messages.getFormattedMessage(FLAMEGRAPH_SELECT_TITLE_EVENT_PATTERN, entry.getKey(),
					String.valueOf(entry.getValue()));
			title.append(eventType);
			if (i < eventsInTitle) {
				title.append(Messages.getFormattedMessage(FLAMEGRAPH_SELECT_TITLE_EVENT_MORE_DELIMITER));
			} else {
				break;
			}
			i++;
		}
		if (eventCountsByType.size() > MAX_TYPES_IN_ROOT_TITLE) {
			title.append(Messages.getFormattedMessage(FLAMEGRAPH_SELECT_HTML_MORE));
		}
		return title.toString();
	}

	private static String createRootNodeTitlePrefix(long events, int types) {
		String eventText = Messages.getFormattedMessage(
				events > 1 ? FLAMEGRAPH_SELECT_ROOT_NODE_EVENTS : FLAMEGRAPH_SELECT_ROOT_NODE_EVENT);
		String typeText = Messages
				.getFormattedMessage(types > 1 ? FLAMEGRAPH_SELECT_ROOT_NODE_TYPES : FLAMEGRAPH_SELECT_ROOT_NODE_TYPE);
		return Messages.getFormattedMessage(FLAMEGRAPH_SELECT_ROOT_NODE, String.valueOf(events), eventText,
				String.valueOf(types), typeText);
	}

	private static String createRootNodeDescription(Map<String, Long> eventCountsByType) {
		StringBuilder description = new StringBuilder();
		int i = 0;
		long remainingEvents = 0;
		for (Map.Entry<String, Long> entry : eventCountsByType.entrySet()) {
			if (i < MAX_TYPES_IN_ROOT_DESCRIPTION) {
				description.append(Messages.getFormattedMessage(FLAMEGRAPH_SELECT_HTML_TABLE_EVENT_PATTERN,
						String.valueOf(entry.getValue()), entry.getKey()));
			} else {
				remainingEvents = Long.sum(remainingEvents, entry.getValue());
			}
			i++;
		}

		if (remainingEvents > 0) {
			int remainingTypes = eventCountsByType.size() - MAX_TYPES_IN_ROOT_DESCRIPTION;
			description.append(Messages.getFormattedMessage(FLAMEGRAPH_SELECT_HTML_TABLE_EVENT_REST_PATTERN,
					String.valueOf(remainingEvents), String.valueOf(remainingTypes)));
		}
		return description.toString();
	}

	/**
	 * Generates a JSON file for the execution sample (CPU profiling) events available in the
	 * recording.
	 * <p>
	 * TODO: This could easily be made highly configurable to allow the user to configure which
	 * event type to filter for, what attribute to use for weight, and what frame separator to use.
	 * 
	 * @param args
	 *            takes one argument - the file name of the JFR file to serialize into JSON.
	 * @throws IOException
	 * @throws CouldNotLoadRecordingException
	 */
	public static void main(String[] args) throws IOException, CouldNotLoadRecordingException {
		if (args.length != 1) {
			System.out.println("Usage: FlameGraphJsonSerializer <filename>\n");
			System.out.println(
					"Serializes the execution sample events a JFR file into a JSON file, suitable for visualizing with flame graph library.");
			System.exit(2);
		}
		File jfrFile = new File(args[0]);
		IItemCollection items = JfrLoaderToolkit.loadEvents(jfrFile);
		IItemCollection filteredItems = items.apply(JdkFilters.EXECUTION_SAMPLE);
		FrameSeparator frameSeparator = new FrameSeparator(FrameCategorization.METHOD, false);
		StacktraceTreeModel model = new StacktraceTreeModel(filteredItems, frameSeparator);
		System.out.println(toJson(model));
	}
}
