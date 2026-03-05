/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.flamegraph.views;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.openjdk.jmc.common.item.IItemCollection;

class EventTypeFilterAction extends Action implements IMenuCreator {
	static final String EVENT_TYPE_FILTER_ID = "EventTypeFilter"; //$NON-NLS-1$

	private Menu menu;
	private final Map<String, String> typeIdToName;
	private final Set<String> selectedTypeIds;
	private final Runnable onSelectionChanged;

	static Map<String, String> extractEventTypes(IItemCollection items) {
		var types = new TreeMap<String, String>();
		for (var iterable : items) {
			if (iterable.getItemCount() > 0) {
				types.put(iterable.getType().getIdentifier(), iterable.getType().getName());
			}
		}
		return types;
	}

	EventTypeFilterAction(Map<String, String> typeIdToName, Set<String> selectedTypeIds, Runnable onSelectionChanged) {
		super(computeText(selectedTypeIds.size(), typeIdToName.size()), IAction.AS_DROP_DOWN_MENU);
		setId(EVENT_TYPE_FILTER_ID);
		this.typeIdToName = typeIdToName;
		this.selectedTypeIds = selectedTypeIds;
		this.onSelectionChanged = onSelectionChanged;
		setMenuCreator(this);
	}

	private static String computeText(int selected, int total) {
		if (total == 0 || selected == total) {
			return "All Types"; //$NON-NLS-1$
		}
		return selected + "/" + total + " Types"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public void dispose() {
		if (menu != null) {
			menu.dispose();
			menu = null;
		}
	}

	@Override
	public Menu getMenu(Control parent) {
		if (menu == null) {
			menu = new Menu(parent);
			populate();
		}
		return menu;
	}

	@Override
	public Menu getMenu(Menu parent) {
		if (menu == null) {
			menu = new Menu(parent);
			populate();
		}
		return menu;
	}

	private void populate() {
		var sortedEntries = new ArrayList<>(typeIdToName.entrySet());
		sortedEntries.sort(Map.Entry.comparingByValue());
		for (var entry : sortedEntries) {
			var aci = new ActionContributionItem(new ToggleTypeAction(entry.getKey(), entry.getValue()));
			aci.fill(menu, -1);
		}
	}

	private class ToggleTypeAction extends Action {
		private final String typeId;

		ToggleTypeAction(String typeId, String displayName) {
			super(displayName, IAction.AS_CHECK_BOX);
			this.typeId = typeId;
			setChecked(selectedTypeIds.contains(typeId));
		}

		@Override
		public void run() {
			if (isChecked()) {
				selectedTypeIds.add(typeId);
			} else {
				if (selectedTypeIds.size() > 1) {
					selectedTypeIds.remove(typeId);
				} else {
					setChecked(true);
					return;
				}
			}
			onSelectionChanged.run();
		}
	}
}
