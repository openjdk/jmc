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
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.ItemCollectionToolkit;

/**
 * Set operations on selections from the same parsed recording. Events are compared by reference
 * identity, not value equality, and repeated references are included only once. Collections from
 * independently parsed recordings do not share event identities, even when their values match.
 * <p>
 * Results materialize references to the selected events without copying the events. They can be
 * filtered and aggregated using the ordinary item APIs. Materialization avoids retaining excluded
 * events through source collections or membership predicates, but retained events can still refer
 * to shared recording structures. Results are grouped by event type; order within each type follows
 * first encounter in the inputs.
 */
public final class SelectionToolkit {

	private SelectionToolkit() {
	}

	/**
	 * Returns the events present in both inputs.
	 */
	public static IItemCollection intersect(IItemCollection a, IItemCollection b) {
		return select(a, identities(b), true);
	}

	/**
	 * Returns the events present in either input, encountering the first input before the second.
	 */
	public static IItemCollection union(IItemCollection a, IItemCollection b) {
		Set<IItem> seen = newIdentitySet();
		List<IItem> result = new ArrayList<>();
		appendUnique(a, seen, result);
		appendUnique(b, seen, result);
		return ItemCollectionToolkit.build(result.stream());
	}

	/**
	 * Returns the events in the first input that are absent from the second input.
	 */
	public static IItemCollection subtract(IItemCollection a, IItemCollection b) {
		return select(a, identities(b), false);
	}

	private static IItemCollection select(IItemCollection items, Set<IItem> membership, boolean included) {
		Objects.requireNonNull(items, "items"); //$NON-NLS-1$
		Set<IItem> seen = newIdentitySet();
		List<IItem> result = new ArrayList<>();
		for (IItemIterable iterable : items) {
			for (IItem item : iterable) {
				if (membership.contains(item) == included && seen.add(item)) {
					result.add(item);
				}
			}
		}
		return ItemCollectionToolkit.build(result.stream());
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

	private static void appendUnique(IItemCollection items, Set<IItem> seen, List<IItem> result) {
		Objects.requireNonNull(items, "items"); //$NON-NLS-1$
		for (IItemIterable iterable : items) {
			for (IItem item : iterable) {
				if (seen.add(item)) {
					result.add(item);
				}
			}
		}
	}

	private static Set<IItem> newIdentitySet() {
		return Collections.newSetFromMap(new IdentityHashMap<>());
	}
}
