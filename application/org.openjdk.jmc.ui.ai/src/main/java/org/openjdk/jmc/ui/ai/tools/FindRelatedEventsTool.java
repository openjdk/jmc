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

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.ui.ai.IAITool;

/**
 * Finds related events using contextual attributes, time overlap, and thread correlation. Supports
 * two modes:
 * <ul>
 * <li><b>concurrent</b> - Find events that overlapped in time with the reference events</li>
 * <li><b>contained</b> - Find events fully contained within the time span of reference events</li>
 * </ul>
 * For finding events by shared attribute value (e.g. gcId=57), use get_event_table with
 * filterAttribute/filterValue instead.
 */
public class FindRelatedEventsTool implements IAITool {

	private static final int MAX_RESULTS = 200;
	private static final Pattern TYPE_PATTERN = Pattern.compile("\"eventType\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""); //$NON-NLS-1$
	private static final Pattern MODE_PATTERN = Pattern.compile("\"mode\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""); //$NON-NLS-1$
	private static final Pattern SAME_THREADS_PATTERN = Pattern.compile("\"sameThreads\"\\s*:\\s*true"); //$NON-NLS-1$
	private static final Pattern LIMIT_PATTERN = Pattern.compile("\"limit\"\\s*:\\s*(\\d+)"); //$NON-NLS-1$
	private static final Pattern FROM_PATTERN = Pattern.compile("\"fromSeconds\"\\s*:\\s*([\\d.]+)"); //$NON-NLS-1$
	private static final Pattern TO_PATTERN = Pattern.compile("\"toSeconds\"\\s*:\\s*([\\d.]+)"); //$NON-NLS-1$
	private static final Pattern FILTER_ATTR_PATTERN = Pattern
			.compile("\"filterAttribute\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""); //$NON-NLS-1$
	private static final Pattern FILTER_VALUE_PATTERN = Pattern
			.compile("\"filterValue\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""); //$NON-NLS-1$

	@Override
	public String getName() {
		return "find_related_events"; //$NON-NLS-1$
	}

	@Override
	public String getDescription() {
		return "Finds events concurrent with or contained within reference events, for root cause analysis." //$NON-NLS-1$
				+ " IMPORTANT: Use filterAttribute/filterValue or fromSeconds/toSeconds to scope the reference" //$NON-NLS-1$
				+ " events to a SPECIFIC instance. Without filters, ALL events of eventType are used as reference," //$NON-NLS-1$
				+ " which gives useless results if the type has many events spanning the whole recording." //$NON-NLS-1$
				+ " Example: to find what happened during a specific servlet request, use" //$NON-NLS-1$
				+ " filterAttribute=ECID, filterValue=<the-ecid> to scope to that one request." //$NON-NLS-1$
				+ " 'concurrent' finds all OTHER events that overlapped in time." //$NON-NLS-1$
				+ " 'contained' finds events fully within the time span." //$NON-NLS-1$
				+ " sameThreads=true restricts to same threads as reference events." //$NON-NLS-1$
				+ " Alternatively, use 'reference' to specify a previously stored result set name" //$NON-NLS-1$
				+ " (e.g. from aggregate_events or get_event_table with storeAs)."; //$NON-NLS-1$
	}

	@Override
	public String getParameterSchema() {
		return "{\"type\":\"object\",\"properties\":{" //$NON-NLS-1$
				+ "\"reference\":{\"type\":\"string\",\"description\":\"Name of a stored result set to use as reference events (e.g. Servlet_Invocation.max)\"}," //$NON-NLS-1$
				+ "\"eventType\":{\"type\":\"string\",\"description\":\"Reference event type ID (alternative to reference)\"}," //$NON-NLS-1$
				+ "\"filterAttribute\":{\"type\":\"string\",\"description\":\"Scope reference events by attribute (e.g. ECID, gcId)\"}," //$NON-NLS-1$
				+ "\"filterValue\":{\"type\":\"string\",\"description\":\"Value the filter attribute must match\"}," //$NON-NLS-1$
				+ "\"fromSeconds\":{\"type\":\"number\",\"description\":\"Scope reference events from this time (seconds from recording start)\"}," //$NON-NLS-1$
				+ "\"toSeconds\":{\"type\":\"number\",\"description\":\"Scope reference events to this time\"}," //$NON-NLS-1$
				+ "\"mode\":{\"type\":\"string\",\"description\":\"Search mode\"," //$NON-NLS-1$
				+ "\"enum\":[\"concurrent\",\"contained\"]}," //$NON-NLS-1$
				+ "\"sameThreads\":{\"type\":\"boolean\",\"description\":\"Restrict to same threads as reference events (default true)\"}," //$NON-NLS-1$
				+ "\"storeAs\":{\"type\":\"string\",\"description\":\"Store the found concurrent/contained events under this name\"}," //$NON-NLS-1$
				+ "\"limit\":{\"type\":\"integer\",\"description\":\"Max events to return (default 100)\"}" //$NON-NLS-1$
				+ "},\"required\":[\"eventType\",\"mode\"]}"; //$NON-NLS-1$
	}

	@Override
	public String execute(String parametersJson) {
		IItemCollection items = JfrContext.getActiveItems();
		if (items == null) {
			return "No flight recording is currently open."; //$NON-NLS-1$
		}

		// Check for stored reference first
		String reference = JfrContext.extractString(JfrContext.REFERENCE_PATTERN, parametersJson);

		String eventType = JfrContext.extractString(TYPE_PATTERN, parametersJson);
		if (reference == null && eventType == null) {
			return "Missing parameter: either 'reference' or 'eventType' is required."; //$NON-NLS-1$
		}

		String mode = JfrContext.extractString(MODE_PATTERN, parametersJson);
		if (mode == null) {
			return "Missing required parameter: mode"; //$NON-NLS-1$
		}

		int limit = JfrContext.extractInt(LIMIT_PATTERN, parametersJson, 100);
		if (limit > MAX_RESULTS) {
			limit = MAX_RESULTS;
		}

		switch (mode) {
		case "concurrent": //$NON-NLS-1$
			return findConcurrentOrContained(items, eventType, parametersJson, limit, false);
		case "contained": //$NON-NLS-1$
			return findConcurrentOrContained(items, eventType, parametersJson, limit, true);
		default:
			return "Unknown mode: " + mode + ". Use 'concurrent' or 'contained'."; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private String findConcurrentOrContained(
		IItemCollection items, String eventType, String json, int limit, boolean contained) {

		boolean sameThreads = SAME_THREADS_PATTERN.matcher(json).find() || !json.contains("\"sameThreads\""); //$NON-NLS-1$

		// Get reference events
		String reference = JfrContext.extractString(JfrContext.REFERENCE_PATTERN, json);
		IItemCollection refEvents;
		String refDescription;

		if (reference != null) {
			refEvents = JfrContext.getStored(reference);
			if (refEvents == null) {
				return "No stored result set named '" + reference + "'. Available: " + JfrContext.getStoredNames(); //$NON-NLS-1$ //$NON-NLS-2$
			}
			refDescription = reference;
		} else {
			String from = JfrContext.extractString(FROM_PATTERN, json);
			String to = JfrContext.extractString(TO_PATTERN, json);
			refEvents = JfrContext.filterItems(items, eventType, from, to);

			String filterAttr = JfrContext.extractString(FILTER_ATTR_PATTERN, json);
			String filterValue = JfrContext.extractString(FILTER_VALUE_PATTERN, json);
			if (filterAttr != null && filterValue != null) {
				refEvents = filterByAttribute(refEvents, filterAttr, filterValue);
			}
			refDescription = eventType;
		}

		if (!refEvents.hasItems()) {
			return "No reference events found for: " + refDescription; //$NON-NLS-1$
		}

		// Collect time ranges and threads from reference events
		IQuantity earliest = null;
		IQuantity latest = null;
		Set<IMCThread> threads = new HashSet<>();

		for (IItemIterable iterable : refEvents) {
			IType<IItem> type = iterable.getType();
			IMemberAccessor<IQuantity, IItem> startAccessor = JfrAttributes.START_TIME.getAccessor(type);
			IMemberAccessor<IQuantity, IItem> endAccessor = JfrAttributes.END_TIME.getAccessor(type);
			IMemberAccessor<IMCThread, IItem> threadAccessor = JfrAttributes.EVENT_THREAD.getAccessor(type);

			for (IItem item : iterable) {
				IQuantity start = startAccessor != null ? startAccessor.getMember(item) : null;
				IQuantity end = endAccessor != null ? endAccessor.getMember(item) : null;
				IMCThread thread = threadAccessor != null ? threadAccessor.getMember(item) : null;

				if (start != null && (earliest == null || start.compareTo(earliest) < 0)) {
					earliest = start;
				}
				if (end != null && (latest == null || end.compareTo(latest) > 0)) {
					latest = end;
				}
				if (thread != null) {
					threads.add(thread);
				}
			}
		}

		if (earliest == null || latest == null) {
			return "Could not determine time range from reference events."; //$NON-NLS-1$
		}

		// Build filter: time overlap + optional thread filter
		IItemFilter timeFilter;
		if (contained) {
			// Events fully within the reference time span
			timeFilter = ItemFilters.and(ItemFilters.moreOrEqual(JfrAttributes.START_TIME, earliest),
					ItemFilters.lessOrEqual(JfrAttributes.END_TIME, latest));
		} else {
			// Events overlapping the reference time span
			timeFilter = ItemFilters.and(ItemFilters.lessOrEqual(JfrAttributes.START_TIME, latest),
					ItemFilters.moreOrEqual(JfrAttributes.END_TIME, earliest));
		}

		IItemFilter filter = timeFilter;
		if (sameThreads && !threads.isEmpty()) {
			filter = ItemFilters.and(filter, ItemFilters.memberOf(JfrAttributes.EVENT_THREAD, threads));
		}

		// Exclude the reference event type if we filtered by type (not by stored reference)
		if (eventType != null && reference == null) {
			filter = ItemFilters.and(filter, ItemFilters.not(ItemFilters.type(eventType)));
		}

		IItemCollection result = items.apply(filter);

		// Store the result if requested
		String storeAs = JfrContext.extractString(JfrContext.STORE_AS_PATTERN, json);
		if (storeAs != null) {
			// Store reference events + concurrent/contained events together
			final IItemCollection refForStore = refEvents;
			IItemCollection combined = org.openjdk.jmc.common.item.ItemCollectionToolkit
					.merge(() -> java.util.stream.Stream.of(refForStore, result));
			JfrContext.store(storeAs, combined);
		}

		String modeLabel = contained ? "Contained" : "Concurrent"; //$NON-NLS-1$ //$NON-NLS-2$
		StringBuilder sb = new StringBuilder();
		sb.append(modeLabel).append(" events"); //$NON-NLS-1$
		if (sameThreads) {
			sb.append(" on same threads"); //$NON-NLS-1$
		}
		sb.append(" during ").append(refDescription).append(":\n"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("Time range: ").append(earliest.displayUsing(IDisplayable.AUTO)); //$NON-NLS-1$
		sb.append(" - ").append(latest.displayUsing(IDisplayable.AUTO)).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("Threads: ").append(threads.size()).append("\n\n"); //$NON-NLS-1$ //$NON-NLS-2$

		// Group by type for summary
		sb.append("Event type summary:\n"); //$NON-NLS-1$
		for (IItemIterable iterable : result) {
			long count = iterable.getItemCount();
			if (count > 0) {
				sb.append("  ").append(iterable.getType().getIdentifier()); //$NON-NLS-1$
				sb.append(": ").append(count).append(" events\n"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		// Show individual events up to limit
		sb.append("\nEvents:\n"); //$NON-NLS-1$
		int count = 0;
		for (IItemIterable iterable : result) {
			IType<IItem> type = iterable.getType();
			for (IItem item : iterable) {
				if (count >= limit) {
					sb.append("... (truncated at ").append(limit).append(")\n"); //$NON-NLS-1$ //$NON-NLS-2$
					return sb.toString();
				}
				appendEventSummary(sb, item, type, count + 1);
				count++;
			}
		}
		if (count == 0) {
			sb.append("No matching events found.\n"); //$NON-NLS-1$
		}
		return sb.toString();
	}

	private IItemCollection filterByAttribute(IItemCollection items, String attrId, String value) {
		java.util.List<IItem> matching = new java.util.ArrayList<>();
		for (IItemIterable iterable : items) {
			IType<IItem> type = iterable.getType();
			IMemberAccessor<?, IItem> accessor = null;
			for (org.openjdk.jmc.common.item.IAttribute<?> attr : type.getAttributes()) {
				if (attr.getIdentifier().equals(attrId)) {
					accessor = attr.getAccessor(type);
					break;
				}
			}
			if (accessor == null) {
				continue;
			}
			for (IItem item : iterable) {
				Object val = accessor.getMember(item);
				if (val != null) {
					String display = val instanceof IDisplayable ? ((IDisplayable) val).displayUsing(IDisplayable.AUTO)
							: val.toString();
					if (value.equals(display)) {
						matching.add(item);
					}
				}
			}
		}
		if (matching.isEmpty()) {
			return org.openjdk.jmc.common.item.ItemCollectionToolkit.build(java.util.stream.Stream.empty());
		}
		return org.openjdk.jmc.common.item.ItemCollectionToolkit.build(matching.stream());
	}

	private void appendEventSummary(StringBuilder sb, IItem item, IType<IItem> type, int index) {
		IMemberAccessor<IQuantity, IItem> startAccessor = JfrAttributes.START_TIME.getAccessor(type);
		IMemberAccessor<IQuantity, IItem> durationAccessor = JfrAttributes.DURATION.getAccessor(type);
		IMemberAccessor<IMCThread, IItem> threadAccessor = JfrAttributes.EVENT_THREAD.getAccessor(type);

		sb.append("#").append(index).append(" ").append(type.getIdentifier()); //$NON-NLS-1$ //$NON-NLS-2$
		IQuantity start = startAccessor != null ? startAccessor.getMember(item) : null;
		if (start != null) {
			sb.append(" @").append(start.displayUsing(IDisplayable.AUTO)); //$NON-NLS-1$
		}
		IQuantity duration = durationAccessor != null ? durationAccessor.getMember(item) : null;
		if (duration != null) {
			sb.append(" dur=").append(duration.displayUsing(IDisplayable.AUTO)); //$NON-NLS-1$
		}
		IMCThread thread = threadAccessor != null ? threadAccessor.getMember(item) : null;
		if (thread != null) {
			sb.append(" thread=").append(thread.getThreadName()); //$NON-NLS-1$
		}
		sb.append("\n"); //$NON-NLS-1$
	}

}
