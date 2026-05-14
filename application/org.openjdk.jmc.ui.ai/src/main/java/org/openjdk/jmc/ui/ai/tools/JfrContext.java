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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCPackage;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameFilter;
import org.openjdk.jmc.flightrecorder.stacktrace.StackTraceFrameFilter;
import org.openjdk.jmc.flightrecorder.stacktrace.StackTraceFrameFilter.MatchMode;
import org.openjdk.jmc.flightrecorder.ui.DataPageDescriptor;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.JfrEditor;
import org.openjdk.jmc.flightrecorder.ui.RuleManager;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStore;

/**
 * Utility for accessing JFR data from the active editor.
 */
public final class JfrContext {

	private static final Map<String, IItemCollection> storedCollections = new ConcurrentHashMap<>();

	private JfrContext() {
	}

	/**
	 * Stores a named IItemCollection that can be referenced by subsequent tool calls.
	 */
	public static void store(String name, IItemCollection items) {
		storedCollections.put(name, items);
	}

	/**
	 * Retrieves a previously stored IItemCollection by name.
	 */
	public static IItemCollection getStored(String name) {
		return storedCollections.get(name);
	}

	/**
	 * Lists all stored collection names.
	 */
	public static List<String> getStoredNames() {
		return List.copyOf(storedCollections.keySet());
	}

	/**
	 * Clears all stored collections (e.g. when recording changes).
	 */
	public static void clearStored() {
		storedCollections.clear();
	}

	public static final Pattern STORE_AS_PATTERN = Pattern.compile("\"storeAs\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""); //$NON-NLS-1$
	public static final Pattern REFERENCE_PATTERN = Pattern.compile("\"reference\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""); //$NON-NLS-1$
	public static final Pattern INCLUDE_FRAMES_PATTERN = Pattern //
			.compile("\"includeFrames\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""); //$NON-NLS-1$
	public static final Pattern EXCLUDE_FRAMES_PATTERN = Pattern //
			.compile("\"excludeFrames\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""); //$NON-NLS-1$

	public static IItemCollection getActiveItems() {
		IEditorPart editor = getActiveEditor();
		if (editor != null) {
			IItemCollection items = editor.getAdapter(IItemCollection.class);
			if (items != null) {
				return items;
			}
		}
		return null;
	}

	public static RuleManager getActiveRuleManager() {
		IEditorPart editor = getActiveEditor();
		if (editor instanceof JfrEditor) {
			return ((JfrEditor) editor).getRuleManager();
		}
		return null;
	}

	/**
	 * Builds a combined filter from event type and time range (seconds from recording start).
	 */
	public static IItemCollection filterItems(
		IItemCollection items, String eventType, String fromSeconds, String toSeconds) {
		return filterItems(items, eventType, fromSeconds, toSeconds, null, null);
	}

	/**
	 * Builds a combined filter from event type, time range, and stack trace frame inclusion /
	 * exclusion lists. Frame lists are comma-separated strings; each token is interpreted as a
	 * fully qualified class name <em>or</em> a package prefix (matched at name boundary). An event
	 * is kept when its stack trace contains a frame matching any include token <em>and</em>
	 * contains no frame matching any exclude token. Events without a stack trace are excluded by
	 * any non-empty include list and are unaffected by exclude lists.
	 */
	public static IItemCollection filterItems(
		IItemCollection items, String eventType, String fromSeconds, String toSeconds, String includeFrames,
		String excludeFrames) {
		IItemFilter typeFilter = eventType != null ? ItemFilters.type(eventType) : null;
		IItemFilter timeFilter = buildTimeFilter(fromSeconds, toSeconds);
		IItemFilter frameFilter = buildFrameFilter(includeFrames, excludeFrames);

		List<IItemFilter> filters = new ArrayList<>(3);
		if (typeFilter != null) {
			filters.add(typeFilter);
		}
		if (timeFilter != null) {
			filters.add(timeFilter);
		}
		if (frameFilter != null) {
			filters.add(frameFilter);
		}
		if (filters.isEmpty()) {
			return items;
		}
		if (filters.size() == 1) {
			return items.apply(filters.get(0));
		}
		return items.apply(ItemFilters.and(filters.toArray(new IItemFilter[0])));
	}

	/**
	 * Builds a stack trace frame filter from comma-separated include/exclude lists. Each token may
	 * be either a fully qualified class name or a package prefix; both are tested against each
	 * frame and OR-combined within a list. Returns {@code null} if both lists are empty.
	 */
	static IItemFilter buildFrameFilter(String includeFrames, String excludeFrames) {
		IItemFilter inc = framePredicateOf(includeFrames, MatchMode.ANY);
		IItemFilter exc = framePredicateOf(excludeFrames, MatchMode.NONE);
		if (inc == null) {
			return exc;
		}
		if (exc == null) {
			return inc;
		}
		return ItemFilters.and(inc, exc);
	}

	private static IItemFilter framePredicateOf(String csv, MatchMode mode) {
		if (csv == null || csv.trim().isEmpty()) {
			return null;
		}
		List<FrameFilter> tokens = new ArrayList<>();
		for (String raw : csv.split(",")) { //$NON-NLS-1$
			String token = raw.trim();
			if (!token.isEmpty()) {
				tokens.add(classOrPackagePredicate(token));
			}
		}
		if (tokens.isEmpty()) {
			return null;
		}
		FrameFilter combined = tokens.size() == 1 ? tokens.get(0) : anyOf(tokens);
		return new StackTraceFrameFilter(mode, combined);
	}

	/**
	 * Matches a frame whose declaring class has the given fully qualified name, or whose declaring
	 * package equals the name (or a sub-package, matched at a name boundary). This is the
	 * "class-or-package" token grammar used by the AI tools' comma-separated frame filters.
	 */
	private static FrameFilter classOrPackagePredicate(String name) {
		String packagePrefix = name + "."; //$NON-NLS-1$
		return frame -> {
			if (frame == null) {
				return false;
			}
			IMCMethod method = frame.getMethod();
			if (method == null) {
				return false;
			}
			IMCType type = method.getType();
			if (type == null) {
				return false;
			}
			if (name.equals(type.getFullName())) {
				return true;
			}
			IMCPackage pkg = type.getPackage();
			if (pkg == null) {
				return false;
			}
			String pname = pkg.getName();
			return pname != null && (name.equals(pname) || pname.startsWith(packagePrefix));
		};
	}

	private static FrameFilter anyOf(List<FrameFilter> predicates) {
		return frame -> {
			for (FrameFilter p : predicates) {
				if (p.shouldInclude(frame)) {
					return true;
				}
			}
			return false;
		};
	}

	private static IItemFilter buildTimeFilter(String fromSeconds, String toSeconds) {
		if (fromSeconds == null && toSeconds == null) {
			return null;
		}
		IEditorPart editor = getActiveEditor();
		if (!(editor instanceof JfrEditor)) {
			return null;
		}
		JfrEditor jfrEditor = (JfrEditor) editor;
		IQuantity recStart = jfrEditor.getRecordingRange().getStart();

		try {
			IItemFilter lower = null;
			IItemFilter upper = null;
			if (fromSeconds != null) {
				double from = Double.parseDouble(fromSeconds);
				IQuantity start = recStart.add(UnitLookup.SECOND.quantity(from));
				lower = ItemFilters.moreOrEqual(JfrAttributes.END_TIME, start);
			}
			if (toSeconds != null) {
				double to = Double.parseDouble(toSeconds);
				IQuantity end = recStart.add(UnitLookup.SECOND.quantity(to));
				upper = ItemFilters.lessOrEqual(JfrAttributes.START_TIME, end);
			}
			if (lower != null && upper != null) {
				return ItemFilters.and(lower, upper);
			}
			return lower != null ? lower : upper;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static List<DataPageDescriptor> getAllPages() {
		return FlightRecorderUI.getDefault().getPageManager().getAllPages().collect(Collectors.toList());
	}

	public static boolean navigateToPage(String pageId) {
		IEditorPart editor = getActiveEditor();
		if (!(editor instanceof JfrEditor)) {
			return false;
		}
		JfrEditor jfrEditor = (JfrEditor) editor;
		Display display = PlatformUI.getWorkbench().getDisplay();

		AtomicReference<Boolean> result = new AtomicReference<>(false);
		Runnable navigate = () -> {
			for (DataPageDescriptor page : getAllPages()) {
				if (page.getName().equalsIgnoreCase(pageId)) {
					jfrEditor.navigateTo(page);
					result.set(true);
					return;
				}
			}
		};
		if (display.getThread() == Thread.currentThread()) {
			navigate.run();
		} else {
			display.syncExec(navigate);
		}
		return result.get();
	}

	public static SelectionStore getActiveSelectionStore() {
		IEditorPart editor = getActiveEditor();
		if (editor instanceof JfrEditor) {
			return ((JfrEditor) editor).getSelectionStore();
		}
		return null;
	}

	public static String extractString(Pattern pattern, String json) {
		Matcher m = pattern.matcher(json);
		return m.find() ? m.group(1) : null;
	}

	public static int extractInt(Pattern pattern, String json, int defaultValue) {
		Matcher m = pattern.matcher(json);
		return m.find() ? Integer.parseInt(m.group(1)) : defaultValue;
	}

	private static IEditorPart getActiveEditor() {
		Display display = PlatformUI.getWorkbench().getDisplay();
		if (display.getThread() == Thread.currentThread()) {
			return getActiveEditorOnUIThread();
		}
		AtomicReference<IEditorPart> ref = new AtomicReference<>();
		display.syncExec(() -> ref.set(getActiveEditorOnUIThread()));
		return ref.get();
	}

	private static IEditorPart getActiveEditorOnUIThread() {
		try {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			return page != null ? page.getActiveEditor() : null;
		} catch (Exception e) {
			return null;
		}
	}
}
