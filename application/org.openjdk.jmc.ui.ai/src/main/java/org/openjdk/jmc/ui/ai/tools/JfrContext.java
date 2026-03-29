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

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.ui.DataPageDescriptor;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.JfrEditor;
import org.openjdk.jmc.flightrecorder.ui.RuleManager;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStore;

/**
 * Utility for accessing JFR data from the active editor.
 */
public final class JfrContext {

	private JfrContext() {
	}

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
		IItemFilter typeFilter = eventType != null ? ItemFilters.type(eventType) : null;
		IItemFilter timeFilter = buildTimeFilter(fromSeconds, toSeconds);

		if (typeFilter != null && timeFilter != null) {
			return items.apply(ItemFilters.and(typeFilter, timeFilter));
		} else if (typeFilter != null) {
			return items.apply(typeFilter);
		} else if (timeFilter != null) {
			return items.apply(timeFilter);
		}
		return items;
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
