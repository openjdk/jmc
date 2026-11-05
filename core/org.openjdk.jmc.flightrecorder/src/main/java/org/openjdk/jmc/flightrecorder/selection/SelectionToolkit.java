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
package org.openjdk.jmc.flightrecorder.selection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemCollectionToolkit;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.item.ItemIterableToolkit;

/**
 * Set operations on selections from the same parsed recording.
 * <p>
 * Two {@link FilterBackedSelection}s with the same source are combined by composing their filters.
 * The result is another filter-backed view over that source; it is created in constant time and
 * retains no events of its own, only the source it filters.
 * <p>
 * Other inputs are combined by event identity, not value equality, and repeated references are
 * included only once. Collections from independently parsed recordings do not share event
 * identities, even when their values match. Such results hold references to the selected events
 * without copying the events: creating them costs a pass over both inputs, they retain a list of
 * references but not their inputs, and they evaluate as fast as any other collection. Results are
 * grouped by event type; order within each type follows first encounter in the inputs.
 */
public final class SelectionToolkit {

	private SelectionToolkit() {
	}

	/**
	 * Returns the events present in both selections.
	 */
	public static ItemBackedSelection intersect(ItemBackedSelection a, ItemBackedSelection b, String name) {
		IItemCollection source = commonSource(a, b);
		return source != null ? new FilterBackedSelection(source, ItemFilters.and(filterOf(a), filterOf(b)), name)
				: new ItemBackedSelection(intersect(a.getItems(), b.getItems()), name);
	}

	/**
	 * Returns the events present in either selection.
	 */
	public static ItemBackedSelection union(ItemBackedSelection a, ItemBackedSelection b, String name) {
		IItemCollection source = commonSource(a, b);
		return source != null ? new FilterBackedSelection(source, ItemFilters.or(filterOf(a), filterOf(b)), name)
				: new ItemBackedSelection(union(a.getItems(), b.getItems()), name);
	}

	/**
	 * Returns the events in the first selection that are absent from the second selection.
	 */
	public static ItemBackedSelection subtract(ItemBackedSelection a, ItemBackedSelection b, String name) {
		IItemCollection source = commonSource(a, b);
		return source != null
				? new FilterBackedSelection(source, ItemFilters.and(filterOf(a), ItemFilters.not(filterOf(b))), name)
				: new ItemBackedSelection(subtract(a.getItems(), b.getItems()), name);
	}

	/**
	 * Returns the events present in both inputs.
	 */
	public static IItemCollection intersect(IItemCollection a, IItemCollection b) {
		// Removing matches also drops repeated references in the first input.
		Set<IItem> unmatched = identities(b);
		return select(a, unmatched::remove);
	}

	/**
	 * Returns the events present in either input, encountering the first input before the second.
	 */
	public static IItemCollection union(IItemCollection a, IItemCollection b) {
		Set<IItem> seen = newIdentitySet();
		Map<IType<IItem>, List<IItem>> byType = new LinkedHashMap<>();
		collect(a, seen::add, byType);
		collect(b, seen::add, byType);
		return build(byType);
	}

	/**
	 * Returns the events in the first input that are absent from the second input.
	 */
	public static IItemCollection subtract(IItemCollection a, IItemCollection b) {
		// Adding kept events also drops repeated references in the first input.
		Set<IItem> excluded = identities(b);
		return select(a, excluded::add);
	}

	private static IItemCollection commonSource(ItemBackedSelection a, ItemBackedSelection b) {
		Objects.requireNonNull(a, "a"); //$NON-NLS-1$
		Objects.requireNonNull(b, "b"); //$NON-NLS-1$
		if (a instanceof FilterBackedSelection && b instanceof FilterBackedSelection) {
			IItemCollection source = ((FilterBackedSelection) a).getSource();
			return source == ((FilterBackedSelection) b).getSource() ? source : null;
		}
		return null;
	}

	private static IItemFilter filterOf(ItemBackedSelection selection) {
		return ((FilterBackedSelection) selection).getFilter();
	}

	private static IItemCollection select(IItemCollection items, Predicate<IItem> keep) {
		Map<IType<IItem>, List<IItem>> byType = new LinkedHashMap<>();
		collect(items, keep, byType);
		return build(byType);
	}

	private static void collect(IItemCollection items, Predicate<IItem> keep, Map<IType<IItem>, List<IItem>> byType) {
		Objects.requireNonNull(items, "items"); //$NON-NLS-1$
		for (IItemIterable iterable : items) {
			List<IItem> kept = null;
			for (IItem item : iterable) {
				if (keep.test(item)) {
					if (kept == null) {
						kept = byType.computeIfAbsent(iterable.getType(), type -> new ArrayList<>());
					}
					kept.add(item);
				}
			}
		}
	}

	private static IItemCollection build(Map<IType<IItem>, List<IItem>> byType) {
		List<IItemIterable> iterables = new ArrayList<>(byType.size());
		byType.forEach((type, items) -> iterables.add(ItemIterableToolkit.build(items::stream, type)));
		return ItemCollectionToolkit.build(iterables::stream);
	}

	private static Set<IItem> identities(IItemCollection items) {
		Objects.requireNonNull(items, "items"); //$NON-NLS-1$
		Set<IItem> result = newIdentitySet();
		for (IItemIterable iterable : items) {
			for (IItem item : iterable) {
				result.add(item);
			}
		}
		return result;
	}

	private static Set<IItem> newIdentitySet() {
		return Collections.newSetFromMap(new IdentityHashMap<>());
	}

}
