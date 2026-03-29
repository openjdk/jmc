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
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemCollectionToolkit;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.flightrecorder.ui.selection.IItemStreamFlavor;
import org.openjdk.jmc.flightrecorder.ui.selection.ItemBackedSelection;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStore;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStore.SelectionStoreEntry;
import org.openjdk.jmc.ui.ai.IAITool;

/**
 * Tool for creating and activating stored selections in the Flight Recorder editor. Selections
 * filter and highlight events across all pages - the AI can use this to visually point the user to
 * interesting events.
 */
public class ManageSelectionTool implements IAITool {

	private static final Pattern ACTION_PATTERN = Pattern.compile("\"action\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""); //$NON-NLS-1$
	private static final Pattern NAME_PATTERN = Pattern.compile("\"name\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""); //$NON-NLS-1$
	private static final Pattern TYPE_PATTERN = Pattern.compile("\"eventType\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""); //$NON-NLS-1$
	private static final Pattern FILTER_ATTR_PATTERN = Pattern
			.compile("\"filterAttribute\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""); //$NON-NLS-1$
	private static final Pattern FILTER_VALUE_PATTERN = Pattern
			.compile("\"filterValue\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""); //$NON-NLS-1$
	private static final Pattern FROM_PATTERN = Pattern.compile("\"fromSeconds\"\\s*:\\s*([\\d.]+)"); //$NON-NLS-1$
	private static final Pattern TO_PATTERN = Pattern.compile("\"toSeconds\"\\s*:\\s*([\\d.]+)"); //$NON-NLS-1$

	@Override
	public String getName() {
		return "manage_selection"; //$NON-NLS-1$
	}

	@Override
	public String getDescription() {
		return "Manages stored selections in the Flight Recorder editor." //$NON-NLS-1$
				+ " Selections highlight and filter events across all pages." //$NON-NLS-1$
				+ " Actions: 'create' creates a new selection from event type, time range," //$NON-NLS-1$
				+ " and/or attribute value filter (e.g. filterAttribute=gcId, filterValue=57" //$NON-NLS-1$
				+ " to select all events for a specific GC across all event types)." //$NON-NLS-1$
				+ " 'activate' sets a selection as current (by name)." //$NON-NLS-1$
				+ " 'clear' removes the current selection (resets to no filter)." //$NON-NLS-1$
				+ " IMPORTANT: Always clear the selection before starting a new recording-wide analysis." //$NON-NLS-1$
				+ " 'list' shows all stored selections."; //$NON-NLS-1$
	}

	@Override
	public String getParameterSchema() {
		return "{\"type\":\"object\",\"properties\":{" //$NON-NLS-1$
				+ "\"action\":{\"type\":\"string\",\"enum\":[\"create\",\"activate\",\"clear\",\"list\"]," //$NON-NLS-1$
				+ "\"description\":\"The action to perform\"}," //$NON-NLS-1$
				+ "\"name\":{\"type\":\"string\",\"description\":\"Selection name (for create/activate)\"}," //$NON-NLS-1$
				+ "\"eventType\":{\"type\":\"string\",\"description\":\"Event type filter (for create)\"}," //$NON-NLS-1$
				+ "\"filterAttribute\":{\"type\":\"string\",\"description\":\"Attribute ID to filter on, e.g. gcId (for create)\"}," //$NON-NLS-1$
				+ "\"filterValue\":{\"type\":\"string\",\"description\":\"Value the attribute must match, e.g. 57 (for create)\"}," //$NON-NLS-1$
				+ "\"fromSeconds\":{\"type\":\"number\",\"description\":\"Start of time range in seconds from recording start (for create)\"}," //$NON-NLS-1$
				+ "\"toSeconds\":{\"type\":\"number\",\"description\":\"End of time range in seconds from recording start (for create)\"}" //$NON-NLS-1$
				+ "},\"required\":[\"action\"]}"; //$NON-NLS-1$
	}

	@Override
	public String execute(String parametersJson) {
		String action = JfrContext.extractString(ACTION_PATTERN, parametersJson);
		if (action == null) {
			return "Missing required parameter: action"; //$NON-NLS-1$
		}

		switch (action) {
		case "create": //$NON-NLS-1$
			return createSelection(parametersJson);
		case "activate": //$NON-NLS-1$
			return activateSelection(parametersJson);
		case "clear": //$NON-NLS-1$
			return clearSelection();
		case "list": //$NON-NLS-1$
			return listSelections();
		default:
			return "Unknown action: " + action; //$NON-NLS-1$
		}
	}

	private String createSelection(String json) {
		IItemCollection items = JfrContext.getActiveItems();
		if (items == null) {
			return "No flight recording is currently open."; //$NON-NLS-1$
		}

		SelectionStore store = JfrContext.getActiveSelectionStore();
		if (store == null) {
			return "No selection store available."; //$NON-NLS-1$
		}

		String eventType = JfrContext.extractString(TYPE_PATTERN, json);
		String filterAttr = JfrContext.extractString(FILTER_ATTR_PATTERN, json);
		String filterValue = JfrContext.extractString(FILTER_VALUE_PATTERN, json);
		String from = JfrContext.extractString(FROM_PATTERN, json);
		String to = JfrContext.extractString(TO_PATTERN, json);

		String name = JfrContext.extractString(NAME_PATTERN, json);
		if (name == null) {
			name = buildSelectionName(eventType, filterAttr, filterValue, from, to);
		}

		// Apply type and time range filter first
		IItemCollection filtered = JfrContext.filterItems(items, eventType, from, to);

		// Apply attribute value filter if specified
		if (filterAttr != null && filterValue != null) {
			filtered = filterByAttribute(filtered, filterAttr, filterValue);
		}

		if (!filtered.hasItems()) {
			return "No events match the specified criteria."; //$NON-NLS-1$
		}

		NamedItemSelection selection = new NamedItemSelection(filtered, name);
		runOnUIThread(() -> store.addAndSetAsCurrentSelection(selection));

		// Auto-navigate: if events span a short time and no dedicated page, go to Java Application (thread timeline)
		String navigatedTo = autoNavigate(filtered, eventType);
		StringBuilder result = new StringBuilder();
		result.append("Created and activated selection '").append(name).append("'."); //$NON-NLS-1$ //$NON-NLS-2$
		if (navigatedTo != null) {
			result.append(" Navigated to ").append(navigatedTo).append(" page."); //$NON-NLS-1$ //$NON-NLS-2$
			result.append(" Tip: enable 'Show Concurrent' and 'Same Threads' in the Aspect selector,"); //$NON-NLS-1$
			result.append(" then click 'Set' to zoom the time range to these events."); //$NON-NLS-1$
		}
		return result.toString();
	}

	private IItemCollection filterByAttribute(IItemCollection items, String attrId, String value) {
		List<IItemCollection> matching = new ArrayList<>();
		for (IItemIterable iterable : items) {
			IType<IItem> type = iterable.getType();
			IMemberAccessor<?, IItem> accessor = findAccessor(type, attrId);
			if (accessor == null) {
				continue;
			}
			List<IItem> matchingItems = new ArrayList<>();
			for (IItem item : iterable) {
				Object val = accessor.getMember(item);
				if (val != null && matchesValue(val, value)) {
					matchingItems.add(item);
				}
			}
			if (!matchingItems.isEmpty()) {
				matching.add(ItemCollectionToolkit.build(matchingItems.stream()));
			}
		}
		if (matching.isEmpty()) {
			return ItemCollectionToolkit.build(java.util.stream.Stream.empty());
		}
		return ItemCollectionToolkit.merge(() -> matching.stream());
	}

	private IMemberAccessor<?, IItem> findAccessor(IType<IItem> type, String attrId) {
		for (IAttribute<?> attr : type.getAttributes()) {
			if (attr.getIdentifier().equals(attrId)) {
				return attr.getAccessor(type);
			}
		}
		return null;
	}

	private String buildSelectionName(String eventType, String filterAttr, String filterValue, String from, String to) {
		StringBuilder sb = new StringBuilder();
		if (filterAttr != null && filterValue != null) {
			sb.append(filterAttr).append("=").append(filterValue); //$NON-NLS-1$
		}
		if (eventType != null) {
			if (sb.length() > 0) {
				sb.append(", "); //$NON-NLS-1$
			}
			// Use short name: jdk.GarbageCollection -> GarbageCollection
			int dot = eventType.lastIndexOf('.');
			sb.append(dot >= 0 ? eventType.substring(dot + 1) : eventType);
		}
		if (from != null || to != null) {
			if (sb.length() > 0) {
				sb.append(", "); //$NON-NLS-1$
			}
			sb.append(from != null ? from + "s" : "start"); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append("-"); //$NON-NLS-1$
			sb.append(to != null ? to + "s" : "end"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return sb.length() > 0 ? sb.toString() : "AI Selection"; //$NON-NLS-1$
	}

	private boolean matchesValue(Object value, String expected) {
		String display = value instanceof IDisplayable ? ((IDisplayable) value).displayUsing(IDisplayable.AUTO)
				: value.toString();
		return expected.equals(display);
	}

	private String clearSelection() {
		SelectionStore store = JfrContext.getActiveSelectionStore();
		if (store == null) {
			return "No selection store available."; //$NON-NLS-1$
		}
		runOnUIThread(store::clearSelection);
		return "Selection cleared. All pages now show unfiltered data."; //$NON-NLS-1$
	}

	private String activateSelection(String json) {
		SelectionStore store = JfrContext.getActiveSelectionStore();
		if (store == null) {
			return "No selection store available."; //$NON-NLS-1$
		}

		String name = JfrContext.extractString(NAME_PATTERN, json);
		if (name == null) {
			return "Missing parameter: name"; //$NON-NLS-1$
		}

		String matchName = name;
		java.util.Optional<SelectionStoreEntry> entry = store.getSelections()
				.filter(e -> e.getName().equalsIgnoreCase(matchName)).findFirst();

		if (entry.isPresent()) {
			runOnUIThread(() -> store.addAndSetAsCurrentSelection(entry.get().getSelection()));
			return "Activated selection: " + name; //$NON-NLS-1$
		}
		return "Selection not found: " + name + "\n" + listSelections(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private String listSelections() {
		SelectionStore store = JfrContext.getActiveSelectionStore();
		if (store == null) {
			return "No selection store available."; //$NON-NLS-1$
		}

		String entries = store.getSelections().map(e -> "  " + e.getName()) //$NON-NLS-1$
				.collect(Collectors.joining("\n")); //$NON-NLS-1$
		if (entries.isEmpty()) {
			return "No stored selections."; //$NON-NLS-1$
		}

		SelectionStoreEntry current = store.getCurrentSelection();
		String currentName = current != null ? current.getName() : "none"; //$NON-NLS-1$
		return "Stored selections (current: " + currentName + "):\n" + entries; //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static final long MAX_TIMELINE_SPAN_MS = 120_000; // 2 minutes
	private static final String THREAD_TIMELINE_PAGE = "Threads"; //$NON-NLS-1$

	private String autoNavigate(IItemCollection filtered, String eventType) {
		// Check if events span a short enough period for the thread timeline to be useful
		IQuantity earliest = null;
		IQuantity latest = null;
		for (IItemIterable iterable : filtered) {
			IType<IItem> type = iterable.getType();
			IMemberAccessor<IQuantity, IItem> startAccessor = JfrAttributes.START_TIME.getAccessor(type);
			IMemberAccessor<IQuantity, IItem> endAccessor = JfrAttributes.END_TIME.getAccessor(type);
			for (IItem item : iterable) {
				IQuantity start = startAccessor != null ? startAccessor.getMember(item) : null;
				IQuantity end = endAccessor != null ? endAccessor.getMember(item) : null;
				if (start != null && (earliest == null || start.compareTo(earliest) < 0)) {
					earliest = start;
				}
				if (end != null && (latest == null || end.compareTo(latest) > 0)) {
					latest = end;
				}
			}
		}

		// If events span more than 2 minutes, the thread timeline won't be very useful
		if (earliest == null || latest == null) {
			return null;
		}
		long spanMs = latest.subtract(earliest).clampedLongValueIn(org.openjdk.jmc.common.unit.UnitLookup.MILLISECOND);
		if (spanMs > MAX_TIMELINE_SPAN_MS) {
			return null;
		}

		// Navigate to thread timeline - the selection will highlight the events there
		if (JfrContext.navigateToPage(THREAD_TIMELINE_PAGE)) {
			return THREAD_TIMELINE_PAGE;
		}
		return null;
	}

	private static void runOnUIThread(Runnable runnable) {
		Display display = PlatformUI.getWorkbench().getDisplay();
		if (display.getThread() == Thread.currentThread()) {
			runnable.run();
		} else {
			display.syncExec(runnable);
		}
	}

	/**
	 * Custom selection that uses the descriptive name in the flavor/aspect display, instead of the
	 * generic "Selected events (N events of M types)" format.
	 */
	private static class NamedItemSelection extends ItemBackedSelection {
		private final String flavorName;

		NamedItemSelection(IItemCollection items, String name) {
			super(items, name);
			this.flavorName = name;
		}

		@Override
		public java.util.stream.Stream<IItemStreamFlavor> getFlavors(
			IItemFilter filter, IItemCollection items, java.util.List<IAttribute<?>> dstAttributes) {

			IItemCollection selectedItems = getItems();
			boolean applicable = ItemCollectionToolkit.filterIfNotNull(selectedItems, filter).hasItems();

			java.util.stream.Stream.Builder<IItemStreamFlavor> builder = java.util.stream.Stream.builder();
			IItemStreamFlavor namedFlavor = IItemStreamFlavor.build(flavorName, selectedItems);

			if (applicable) {
				builder.accept(namedFlavor);
			}

			IItemCollection dstItems = ItemCollectionToolkit.filterIfNotNull(items, filter);
			org.openjdk.jmc.flightrecorder.ui.JfrPropertySheet
					.calculatePersistableFilterFlavors(selectedItems, dstItems, items, dstAttributes)
					.forEach(builder::accept);

			if (!applicable) {
				builder.accept(namedFlavor);
			}
			return builder.build();
		}
	}
}
