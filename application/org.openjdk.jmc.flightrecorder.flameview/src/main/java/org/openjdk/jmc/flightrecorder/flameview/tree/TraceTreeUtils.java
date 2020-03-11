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

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.util.FormatToolkit;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.Messages;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel.Branch;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel.Fork;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator.FrameCategorization;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceFrame;

public class TraceTreeUtils {
	private static final Comparator<Map.Entry<String, Long>> COMPARATOR_SORT_EVENT_COUNT = 
			Map.Entry.comparingByValue(Comparator.reverseOrder());
	public final static String DEFAULT_ROOT_NAME = "__root";
	public final static String DEFAULT_ROOT_PACKAGE_NAME = "";
	public final static int DEFAULT_ROOT_TITLE_MAX_EVENTS = 2;
	public final static int DEFAULT_ROOT_EVENT_MAX = 10;
	public final static FrameSeparator DEFAULT_FRAME_SEPARATOR = new FrameSeparator(FrameCategorization.METHOD, false);
	

	/**
	 * Traces a TraceTree from a {@link StacktraceModel}.
	 *
	 * @param model the model to trace the tree from.
	 * @return the root.
	 */
	public static TraceNode createTree(Map<IType<IItem>, Long> itemCountByType, StacktraceModel model) {		
		Fork rootFork = model.getRootFork();
		TraceNode root = getRootTraceNode(itemCountByType, rootFork);
		for (Branch branch : rootFork.getBranches()) {
			addBranch(root, branch);
		}
		return root;
	}

	/**
	 * Traces a tree of stack frames from an {@link IItemCollection}.
	 *
	 * @param items the events to aggregate the traces from.
	 * @return the root of the resulting tree.
	 */
	public static TraceNode createTree(IItemCollection items, FrameSeparator frameSeparator, boolean threadRootAtTop) {	
			
		Map<IType<IItem>, Long> itemCountByType = StreamSupport.stream(items.spliterator(), false)
				.collect(Collectors.toMap(IItemIterable::getType, is -> is.getItemCount(), Long::sum));
		return createTree(itemCountByType, new StacktraceModel(threadRootAtTop, frameSeparator, items));
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

	public static String printTree(TraceNode node) {
		StringBuilder builder = new StringBuilder();
		builder.append("=== Tree Printout ===");
		builder.append(System.lineSeparator());
		printTree(builder, 0, node);
		return builder.toString();
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

	private static TraceNode getRootTraceNode(Map<IType<IItem>, Long> itemCountByType, Fork rootFork) {

		StringBuilder titleSb = new StringBuilder().append("Selection: ");
		StringBuilder descSb = new StringBuilder();
		int maxBranches = rootFork.getBranchCount();
		long totalItemsSum = 0L;
		
		if (maxBranches == 0) {
			titleSb.append("No Events");
		} else {
			Map<String, Long> eventsOccurences = new HashMap<>();
			for (int i = 0; i < maxBranches; i++) {
				Branch b = rootFork.getBranch(i);
				for(int j=0; j < b.getFirstFrame().getItems().size(); j++) {
					IType<?> itemType = b.getFirstFrame().getItems().get(j).getType();
					String itemName = itemType.getName();
					if(!eventsOccurences.containsKey(itemName)) {
						Long itemCount = itemCountByType.get(itemType);
						totalItemsSum += itemCount;
						eventsOccurences.put(itemName, itemCount);
					}
				}
			}

			titleSb.append(totalItemsSum).append(" events of ").append(eventsOccurences.size()).append(" types: ");
			
			
			int maxEventsInTile = eventsOccurences.size() > DEFAULT_ROOT_TITLE_MAX_EVENTS ?  
					DEFAULT_ROOT_TITLE_MAX_EVENTS : eventsOccurences.size() - 1;
			Map<String, Long> sortedEventsOccurences = eventsOccurences.entrySet().stream()
					.sorted(COMPARATOR_SORT_EVENT_COUNT)
					.collect(Collectors
							.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
			
			boolean writeTitle = true;
			int i=0;
			long restEventCount = 0;
			for (Map.Entry<String, Long> e : sortedEventsOccurences.entrySet()) {
				if(writeTitle) {
					titleSb.append(e.getKey());
					titleSb.append("[").append(e.getValue()).append("]");
					if(i < maxEventsInTile) {
						titleSb.append(", ");
					}
					if(i == maxEventsInTile) {
						writeTitle = false;
					}
				}
				if(i < DEFAULT_ROOT_EVENT_MAX ) {
					descSb.append(e.getValue()).append(":").append(e.getKey()).append("|");
				} else {
					restEventCount =  Long.sum(restEventCount, e.getValue());
				}
				i++;
			}
			if(restEventCount > 0) {
				descSb.append(restEventCount).append(":").append("others... (")
				.append(sortedEventsOccurences.size() - DEFAULT_ROOT_EVENT_MAX).append(" types)").append("|");
			}
			
					
			if(eventsOccurences.size() > DEFAULT_ROOT_TITLE_MAX_EVENTS) {
				titleSb.append("...");
			}	
		}
		
		return new TraceNode(titleSb.toString(), Math.toIntExact(totalItemsSum), descSb.toString());
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
