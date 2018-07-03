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
package org.openjdk.jmc.flightrecorder.rules.tree;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.util.StringToolkit;
import org.openjdk.jmc.flightrecorder.rules.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.tree.traversal.BFTreeVisitor;
import org.openjdk.jmc.flightrecorder.rules.tree.traversal.LayerBreakdownGenerator;
import org.openjdk.jmc.flightrecorder.rules.tree.traversal.LayerBreakdownVisitor.LayerBreakdown;
import org.openjdk.jmc.flightrecorder.rules.tree.traversal.LayerBreakdownVisitor.LayerEntry;
import org.openjdk.jmc.flightrecorder.rules.tree.traversal.LongestDurationIterator;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;

/**
 * Toolkit for working with item trees.
 */
public final class ItemTreeToolkit {

	/**
	 * @return A String representation of the tree. Useful for debugging.
	 */
	public static String printTree(ITreeNode<IItem> node) {
		final StringBuilder builder = new StringBuilder();
		node.accept(new BFTreeVisitor<IItem>() {
			@Override
			protected void processPayload(IItem value, int level) {
				builder.append(String.format("%02d:%s", level, String.valueOf(value))); //$NON-NLS-1$
			}
		});
		return builder.toString();
	}

	static boolean hasDuration(IItem item) {
		return RulesToolkit.getDuration(item).longValue() != 0;
	}

	/**
	 * Renders a report of the longest encapsulating event chain.
	 * <p>
	 * If we have A -&gt; (B-&gt;D, C-&gt;E) and B has longer duration than C, the report will be A
	 * =&gt; B =&gt; D.
	 *
	 * @param report
	 *            the report to write to
	 * @param root
	 *            the root node to start from
	 */
	public static void appendLongestBreakdown(StringBuilder report, final ITreeNode<IItem> root) {
		Iterator<IItem> itemIterator = new LongestDurationIterator(root);
		report.append(Messages.getString(Messages.ItemTreeToolkit_BREAKDOWN_HEADER_MAX_DURATION_EVENT_CHAIN));
		IQuantity topLevelDuration = null;

		List<String> reports = new ArrayList<>();
		while (itemIterator.hasNext()) {
			IItem next = itemIterator.next();
			IQuantity duration = RulesToolkit.getDuration(next);
			if (topLevelDuration == null) {
				topLevelDuration = duration;
			}
			reports.add(String.format("&nbsp;&nbsp;%s, %s (%s)", next.getType().getName(), toString(duration), //$NON-NLS-1$
					RulesToolkit.toRatioPercentString(duration, topLevelDuration)));
		}
		report.append(StringToolkit.join(reports, " =><br>")); //$NON-NLS-1$
	}

	/**
	 * Reports all encapsulation layers.
	 * <p>
	 * If A -&gt; (B-&gt;D, C-&gt;E), the report will show:
	 * <ul>
	 * <li>Layer 1: A (100 %)
	 * <li>Layer 2: B (X %), C (Y %)
	 * <li>Layer 3: D (U %), E (V %)
	 * </ul>
	 *
	 * @param report
	 *            the report to write to
	 * @param root
	 *            the root node to start the analysis from
	 * @param maxDepth
	 *            maximum number of layers to analyze
	 */
	public static void appendLayeredBreakdown(StringBuilder report, ITreeNode<IItem> root, int maxDepth) {
		LayerBreakdownGenerator layerBreakdown = new LayerBreakdownGenerator(root);
		List<LayerBreakdown> layers = layerBreakdown.getLayers();
		if (layers.isEmpty()) {
			return;
		}

		IQuantity firstLayerDuration = null;
		report.append(Messages.getString(Messages.ItemTreeToolkit_BREAKDOWN_HEADER_LAYERS));
		report.append("<ul>"); //$NON-NLS-1$

		for (int i = 0; i < layers.size() && i <= maxDepth; i++) {
			LayerBreakdown breakdown = layers.get(i);
			IQuantity layerDuration = breakdown.getDuration();
			if (layerDuration == null) {
				continue;
			} else if (firstLayerDuration == null) {
				firstLayerDuration = layerDuration;
			}
			report.append("<li>"); //$NON-NLS-1$
			report.append(MessageFormat.format(Messages.getString(Messages.ItemTreeToolkit_BREAKDOWN_LAYER_CAPTION),
					breakdown.getLayer()));
			appendLayerBreakdown(report, firstLayerDuration, breakdown);
			report.append("</li>"); //$NON-NLS-1$
			report.append("<br>"); //$NON-NLS-1$
		}
		report.append("</ul>"); //$NON-NLS-1$
	}

	private static void appendLayerBreakdown(
		StringBuilder report, IQuantity firstLayerDuration, LayerBreakdown breakdown) {
		List<String> reportEntries = new LinkedList<>();
		for (LayerEntry entry : breakdown.getLayerEntries()) {
			reportEntries.add(String.format("%s  %s (%s)", //$NON-NLS-1$
					RulesToolkit.toRatioPercentString(entry.getDuration(), firstLayerDuration),
					entry.getType().getName(), toString(entry.getDuration())));
		}
		report.append(StringToolkit.join(reportEntries, "<br>")); //$NON-NLS-1$
	}

	private static Object toString(IQuantity duration) {
		return duration.displayUsing(IDisplayable.AUTO);
	}

	/**
	 * Returns the number of ancestors the node has.
	 *
	 * @param node
	 *            the node for which to calculate the depth
	 * @return the depth of the node from the root
	 */
	public static int getDepth(ITreeNode<?> node) {
		int depth = 0;
		ITreeNode<?> parent = node.getParent();
		while (parent != null) {
			parent = parent.getParent();
			depth++;
		}
		return depth;
	}

}
