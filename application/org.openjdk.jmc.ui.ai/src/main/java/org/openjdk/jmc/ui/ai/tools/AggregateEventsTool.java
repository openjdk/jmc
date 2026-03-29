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

import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.ItemCollectionToolkit;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.ui.ai.IAITool;

public class AggregateEventsTool implements IAITool {

	private static final Pattern TYPE_PATTERN = Pattern.compile("\"eventType\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""); //$NON-NLS-1$
	private static final Pattern FUNC_PATTERN = Pattern.compile("\"function\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""); //$NON-NLS-1$
	private static final Pattern FROM_PATTERN = Pattern.compile("\"fromSeconds\"\\s*:\\s*([\\d.]+)"); //$NON-NLS-1$
	private static final Pattern TO_PATTERN = Pattern.compile("\"toSeconds\"\\s*:\\s*([\\d.]+)"); //$NON-NLS-1$

	@Override
	public String getName() {
		return "aggregate_events"; //$NON-NLS-1$
	}

	@Override
	public String getDescription() {
		return "Computes statistical aggregations on JFR event durations: count, sum, avg, min, max, stddev." //$NON-NLS-1$
				+ " For min and max, also returns ALL attributes of that specific event." //$NON-NLS-1$
				+ " This is the fastest way to find the longest/shortest event and its details." //$NON-NLS-1$
				+ " Use get_event_types first to discover available types."; //$NON-NLS-1$
	}

	@Override
	public String getParameterSchema() {
		return "{\"type\":\"object\",\"properties\":{" //$NON-NLS-1$
				+ "\"eventType\":{\"type\":\"string\",\"description\":\"The JFR event type ID, e.g. jdk.JavaMonitorEnter\"}," //$NON-NLS-1$
				+ "\"function\":{\"type\":\"string\",\"description\":\"Aggregation function: count, sum, avg, min, max, stddev, or all\"," //$NON-NLS-1$
				+ "\"enum\":[\"count\",\"sum\",\"avg\",\"min\",\"max\",\"stddev\",\"all\"]}," //$NON-NLS-1$
				+ "\"fromSeconds\":{\"type\":\"number\",\"description\":\"Start of time range in seconds from recording start\"}," //$NON-NLS-1$
				+ "\"toSeconds\":{\"type\":\"number\",\"description\":\"End of time range in seconds from recording start\"}" //$NON-NLS-1$
				+ "},\"required\":[\"eventType\"]}"; //$NON-NLS-1$
	}

	@Override
	public String execute(String parametersJson) {
		IItemCollection items = JfrContext.getActiveItems();
		if (items == null) {
			return "No flight recording is currently open."; //$NON-NLS-1$
		}

		String eventType = JfrContext.extractString(TYPE_PATTERN, parametersJson);
		if (eventType == null) {
			return "Missing required parameter: eventType"; //$NON-NLS-1$
		}

		String function = JfrContext.extractString(FUNC_PATTERN, parametersJson);
		if (function == null) {
			function = "all"; //$NON-NLS-1$
		}

		String from = JfrContext.extractString(FROM_PATTERN, parametersJson);
		String to = JfrContext.extractString(TO_PATTERN, parametersJson);
		IItemCollection filtered = JfrContext.filterItems(items, eventType, from, to);
		if (!filtered.hasItems()) {
			return "No events found for type: " + eventType; //$NON-NLS-1$
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Aggregation for ").append(eventType).append(":\n"); //$NON-NLS-1$ //$NON-NLS-2$

		boolean all = "all".equals(function); //$NON-NLS-1$

		if (all || "count".equals(function)) { //$NON-NLS-1$
			IQuantity count = filtered.getAggregate(Aggregators.count());
			sb.append("  Count: ").append(format(count)).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (all || "sum".equals(function)) { //$NON-NLS-1$
			IQuantity sum = filtered.getAggregate(Aggregators.sum(JfrAttributes.DURATION));
			sb.append("  Sum(duration): ").append(format(sum)).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (all || "avg".equals(function)) { //$NON-NLS-1$
			IQuantity avg = filtered.getAggregate(Aggregators.avg(JfrAttributes.DURATION));
			sb.append("  Avg(duration): ").append(format(avg)).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (all || "min".equals(function)) { //$NON-NLS-1$
			IQuantity min = filtered.getAggregate((IAggregator<IQuantity, ?>) Aggregators.min(JfrAttributes.DURATION));
			sb.append("  Min(duration): ").append(format(min)).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
			IItem minItem = filtered
					.getAggregate((IAggregator<IItem, ?>) Aggregators.itemWithMin(JfrAttributes.DURATION));
			if (minItem != null) {
				JfrContext.store(eventType + ".min", ItemCollectionToolkit.build(Stream.of(minItem))); //$NON-NLS-1$
				sb.append("  (stored as '").append(eventType).append(".min')\n"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			appendItemDetails(sb, minItem, "  Min event", filtered); //$NON-NLS-1$
		}
		if (all || "max".equals(function)) { //$NON-NLS-1$
			IQuantity max = filtered.getAggregate((IAggregator<IQuantity, ?>) Aggregators.max(JfrAttributes.DURATION));
			sb.append("  Max(duration): ").append(format(max)).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
			IItem maxItem = filtered
					.getAggregate((IAggregator<IItem, ?>) Aggregators.itemWithMax(JfrAttributes.DURATION));
			if (maxItem != null) {
				JfrContext.store(eventType + ".max", ItemCollectionToolkit.build(Stream.of(maxItem))); //$NON-NLS-1$
				sb.append("  (stored as '").append(eventType).append(".max')\n"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			appendItemDetails(sb, maxItem, "  Max event", filtered); //$NON-NLS-1$
		}
		if (all || "stddev".equals(function)) { //$NON-NLS-1$
			IQuantity stddev = filtered.getAggregate(Aggregators.stddev(JfrAttributes.DURATION));
			sb.append("  StdDev(duration): ").append(format(stddev)).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	private void appendItemDetails(StringBuilder sb, IItem item, String label, IItemCollection filtered) {
		if (item == null) {
			return;
		}
		sb.append(label).append(" details:\n"); //$NON-NLS-1$
		// Find the type for this item to get its attributes
		for (IItemIterable iterable : filtered) {
			IType<IItem> type = iterable.getType();
			// Check if this type matches by trying to access an attribute
			IMemberAccessor<IQuantity, IItem> startAccessor = JfrAttributes.START_TIME.getAccessor(type);
			if (startAccessor == null) {
				continue;
			}
			try {
				// Try accessing - will work if item belongs to this type
				startAccessor.getMember(item);
				for (IAttribute<?> attr : type.getAttributes()) {
					IMemberAccessor<?, IItem> accessor = attr.getAccessor(type);
					if (accessor != null) {
						Object value = accessor.getMember(item);
						if (value != null) {
							String display = value instanceof IDisplayable
									? ((IDisplayable) value).displayUsing(IDisplayable.AUTO) : value.toString();
							sb.append("    ").append(attr.getName()).append(": ").append(display).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						}
					}
				}
				return;
			} catch (Exception e) {
				continue; // Wrong type, try next
			}
		}
	}

	private static String format(IQuantity value) {
		return value != null ? value.displayUsing(IDisplayable.AUTO) : "N/A"; //$NON-NLS-1$
	}

}
