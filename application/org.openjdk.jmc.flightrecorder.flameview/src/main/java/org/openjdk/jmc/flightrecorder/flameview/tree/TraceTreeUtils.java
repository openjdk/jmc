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
package org.openjdk.jmc.flightrecorder.flameview.tree;

import static org.openjdk.jmc.flightrecorder.flameview.Messages.FLAMEVIEW_SELECT_STACKTRACE_NOT_AVAILABLE;
import static org.openjdk.jmc.flightrecorder.flameview.Messages.FLAMEVIEW_SELECT_ROOT_NODE;
import static org.openjdk.jmc.flightrecorder.flameview.Messages.FLAMEVIEW_SELECT_TITLE_EVENT_MORE_DELIMITER;
import static org.openjdk.jmc.flightrecorder.flameview.Messages.FLAMEVIEW_SELECT_TITLE_EVENT_PATTERN;
import static org.openjdk.jmc.flightrecorder.flameview.Messages.FLAMEVIEW_SELECT_HTML_TABLE_EVENT_PATTERN;
import static org.openjdk.jmc.flightrecorder.flameview.Messages.FLAMEVIEW_SELECT_HTML_TABLE_EVENT_REST_PATTERN;
import static org.openjdk.jmc.flightrecorder.flameview.Messages.FLAMEVIEW_SELECT_HTML_MORE;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.GroupingAggregator;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.Aggregators.CountConsumer;
import org.openjdk.jmc.common.item.GroupingAggregator.GroupEntry;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.FormatToolkit;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.Messages;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel.Branch;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel.Fork;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator.FrameCategorization;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceFrame;

public class TraceTreeUtils {

	public final static String EMPTY_STRING = ""; //$NON-NLS-1$
	public final static int DEFAULT_ROOT_TITLE_MAX_EVENTS = 2;
	public final static int DEFAULT_ROOT_EVENT_MAX = 10;
	public final static String SELECT_EVENT_DELIMITER = createFlameviewMessage(
			FLAMEVIEW_SELECT_TITLE_EVENT_MORE_DELIMITER);
	public final static FrameSeparator DEFAULT_FRAME_SEPARATOR = new FrameSeparator(FrameCategorization.METHOD, false);

	/**
	 * Traces a TraceTree from a {@link StacktraceModel}.
	 * 
	 * @param root
	 *            the root with description
	 * @param model
	 *            the model to trace the tree from
	 * @return the root
	 */
	public static TraceNode createTree(TraceNode root, StacktraceModel model) {
		Fork rootFork = model.getRootFork();
		for (Branch branch : rootFork.getBranches()) {
			addBranch(root, branch);
		}
		return root;
	}

	/**
	 * Root of Traces from the selection {@link IItemCollection}
	 * 
	 * @param items
	 *            the items from the selection
	 * @param branchCount
	 *            branch count from {@link StacktraceModel} model
	 * @return root
	 */
	public static TraceNode createRootWithDescription(IItemCollection items, int branchCount) {

		StringBuilder titleSb = new StringBuilder();
		StringBuilder descSb = new StringBuilder();
		AtomicInteger totalItemsSum = new AtomicInteger(0);

		if (branchCount == 0) {
			titleSb.append(createFlameviewMessage(FLAMEVIEW_SELECT_STACKTRACE_NOT_AVAILABLE));
		} else {
			Map<String, Integer> orderedEventTypeNameWithCount = eventTypeNameWithCountSorted(items, totalItemsSum);
			String selectionText = createFlameviewMessage(FLAMEVIEW_SELECT_ROOT_NODE, totalItemsSum.get(),
					orderedEventTypeNameWithCount.size());
			titleSb.append(selectionText);
			createNodeTitleAndDescription(titleSb, descSb, orderedEventTypeNameWithCount);
		}

		return new TraceNode(titleSb.toString(), 0, descSb.toString());
	}

	/**
	 * Print the tree by the trace node
	 * 
	 * @param node
	 *            trace node
	 * @return tree
	 */
	public static String printTree(TraceNode node) {
		StringBuilder builder = new StringBuilder();
		builder.append("=== Tree Printout ===");
		builder.append(System.lineSeparator());
		printTree(builder, 0, node);
		return builder.toString();
	}

	private static String createFlameviewMessage(String key, Object ... values) {
		if (values == null) {
			return getFlameviewMessage(key);
		} else {
			return MessageFormat.format(getFlameviewMessage(key), values);
		}
	}

	private static String getFlameviewMessage(String key) {
		return org.openjdk.jmc.flightrecorder.flameview.Messages.getString(key);
	}

	private static void addBranch(TraceNode root, Branch branch) {
		StacktraceFrame firstFrame = branch.getFirstFrame();
		TraceNode currentNode = getTraceNodeByStacktraceFrame(firstFrame);
		root.addChild(currentNode);
		for (StacktraceFrame frame : branch.getTailFrames()) {
			TraceNode newNode = getTraceNodeByStacktraceFrame(frame);
			currentNode.addChild(newNode);
			currentNode = newNode;
		}
		addFork(currentNode, branch.getEndFork());
	}

	private static void addFork(TraceNode node, Fork fork) {
		for (Branch branch : fork.getBranches()) {
			addBranch(node, branch);
		}
	}

	private static void printTree(StringBuilder builder, int indentation, TraceNode node) {
		builder.append(String.format("%s%s - %d%n", indent(indentation), node.getName(), node.getValue()));
		for (TraceNode child : node.getChildren()) {
			printTree(builder, indentation + 1, child);
		}
	}

	private static String indent(int indentation) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < indentation; i++) {
			builder.append("   ");
		}
		return builder.toString();
	}

	private static Map<String, Integer> eventTypeNameWithCountSorted(
		IItemCollection items, AtomicInteger totalEventTypeSum) {
		final HashMap<String, Integer> map = new HashMap<>();
		IAggregator<IQuantity, ?> build = GroupingAggregator.build(EMPTY_STRING, EMPTY_STRING, JfrAttributes.EVENT_TYPE,
				Aggregators.count(), new GroupingAggregator.IGroupsFinisher<IQuantity, IType<?>, CountConsumer>() {

					@Override
					public IType<IQuantity> getValueType() {
						return UnitLookup.NUMBER;
					}

					@Override
					public IQuantity getValue(Iterable<? extends GroupEntry<IType<?>, CountConsumer>> groups) {
						for (GroupEntry<IType<?>, CountConsumer> groupEntry : groups) {
							CountConsumer consumer = groupEntry.getConsumer();
							IType<?> key = groupEntry.getKey();
							totalEventTypeSum.addAndGet(consumer.getCount());
							map.put(key.getName(), consumer.getCount());
						}
						return null;
					}
				});
		items.getAggregate(build);
		return RulesToolkit.sortMap(map, false);
	}

	private static void createNodeTitleAndDescription(
		StringBuilder titleSb, StringBuilder descSb, Map<String, Integer> orderedItemCountByType) {

		int i = 0;
		long restEventCount = 0;
		boolean writeTitle = true;
		int maxEventsInTile = orderedItemCountByType.size() > DEFAULT_ROOT_TITLE_MAX_EVENTS
				? DEFAULT_ROOT_TITLE_MAX_EVENTS : orderedItemCountByType.size() - 1;

		for (Map.Entry<String, Integer> e : orderedItemCountByType.entrySet()) {
			if (writeTitle) {
				String eventType = createFlameviewMessage(FLAMEVIEW_SELECT_TITLE_EVENT_PATTERN, e.getKey(),
						String.valueOf(e.getValue()));
				titleSb.append(eventType);
				if (i < maxEventsInTile) {
					titleSb.append(SELECT_EVENT_DELIMITER);
				} else {
					writeTitle = false;
				}
			}
			if (i < DEFAULT_ROOT_EVENT_MAX) {
				String tableEvent = createFlameviewMessage(FLAMEVIEW_SELECT_HTML_TABLE_EVENT_PATTERN,
						String.valueOf(e.getValue()), e.getKey());
				descSb.append(tableEvent);
			} else {
				restEventCount = Long.sum(restEventCount, e.getValue());
			}
			i++;
		}

		if (restEventCount > 0) {
			String restEventCountText = String.valueOf(restEventCount);
			String restItemCountText = String.valueOf(orderedItemCountByType.size() - DEFAULT_ROOT_EVENT_MAX);
			String tableEventRest = createFlameviewMessage(FLAMEVIEW_SELECT_HTML_TABLE_EVENT_REST_PATTERN,
					restEventCountText, restItemCountText);
			descSb.append(tableEventRest);
		}

		if (maxEventsInTile < orderedItemCountByType.size() - 1) {
			titleSb.append(createFlameviewMessage(FLAMEVIEW_SELECT_HTML_MORE)); //$NON-NLS-1$
		}
	}

	private static TraceNode getTraceNodeByStacktraceFrame(StacktraceFrame sFrame) {
		IMCFrame frame = sFrame.getFrame();
		IMCMethod method = frame.getMethod();
		String packageName = FormatToolkit.getPackage(method.getType().getPackage());
		if (frame == StacktraceModel.UNKNOWN_FRAME) {
			return new TraceNode(Messages.getString(Messages.STACKTRACE_UNCLASSIFIABLE_FRAME), sFrame.getItemCount(),
					packageName);
		} else {
			String name = FormatToolkit.getHumanReadable(method, false, false, true, false, true, false);
			return new TraceNode(name, sFrame.getItemCount(), packageName);
		}
	}
}
