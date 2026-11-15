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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.openjdk.jmc.common.item.IItemCollection;

/**
 * Owns named selections for one recording or analysis session. Names are case-sensitive and must
 * contain at least one non-whitespace character; they are otherwise preserved exactly. Replacing a
 * name creates a new opaque ID and invalidates the previous ID.
 * <p>
 * The registry strongly retains its live selections without automatic eviction. Callers should
 * clear or close it when its recording or session ends. Returned selections and item collections
 * are also strong references: callers may keep using them after removal or closure, and those
 * references, including derived filtered collections, can prolong the lifetime of recording data.
 * Closure releases the registry's references, not references held by callers.
 * <p>
 * Registry operations are synchronized. This does not make a supplied selection implementation
 * thread-safe or protect it against mutations by its owner.
 */
public final class NamedSelectionRegistry implements AutoCloseable {

	/**
	 * Immutable selection metadata. Entries do not refer to the selection payload or registry, so
	 * retaining an entry does not retain recording data.
	 */
	public static final class Entry {
		private final String id;
		private final String name;

		private Entry(String id, String name) {
			this.id = id;
			this.name = name;
		}

		/**
		 * @return the opaque ID of this registration, invalidated when it is removed or replaced
		 */
		public String getId() {
			return id;
		}

		/**
		 * @return the exact name under which the selection was registered
		 */
		public String getName() {
			return name;
		}
	}

	private static final class StoredSelection {
		private final Entry entry;
		private final IFlavoredSelection selection;

		private StoredSelection(Entry entry, IFlavoredSelection selection) {
			this.entry = entry;
			this.selection = selection;
		}
	}

	private final Map<String, StoredSelection> byId = new LinkedHashMap<>();
	private final Map<String, String> byName = new HashMap<>();
	private boolean closed;

	/**
	 * Stores a selection, replacing any selection registered under the same name.
	 *
	 * @return metadata for the newly registered selection
	 * @throws IllegalStateException
	 *             if the registry is closed
	 */
	public synchronized Entry put(String name, IFlavoredSelection selection) {
		requireOpen();
		requireName(name);
		Objects.requireNonNull(selection, "selection"); //$NON-NLS-1$
		String previousId = byName.remove(name);
		if (previousId != null) {
			byId.remove(previousId);
		}
		Entry entry = new Entry(UUID.randomUUID().toString(), name);
		byId.put(entry.getId(), new StoredSelection(entry, selection));
		byName.put(name, entry.getId());
		return entry;
	}

	/**
	 * Stores an item-backed selection without copying or serializing its events. Empty collections
	 * are valid selections and remain retrievable.
	 *
	 * @return metadata for the newly registered selection
	 */
	public synchronized Entry put(String name, IItemCollection items) {
		requireOpen();
		requireName(name);
		return put(name, new ItemBackedSelection(Objects.requireNonNull(items, "items"), name)); //$NON-NLS-1$
	}

	/**
	 * Resolves an ID to its live selection. The returned reference can outlive registry removal or
	 * closure and retains the selection's data for as long as the caller retains it.
	 */
	public synchronized Optional<IFlavoredSelection> get(String id) {
		requireOpen();
		StoredSelection stored = byId.get(Objects.requireNonNull(id, "id")); //$NON-NLS-1$
		return stored == null ? Optional.empty() : Optional.of(stored.selection);
	}

	/**
	 * Resolves an exact, case-sensitive name to its live selection.
	 */
	public synchronized Optional<IFlavoredSelection> getByName(String name) {
		requireOpen();
		requireName(name);
		String id = byName.get(name);
		return id == null ? Optional.empty() : Optional.of(byId.get(id).selection);
	}

	/**
	 * Retrieves the original collection of an {@link ItemBackedSelection} by its registry name. No
	 * flavor is chosen or evaluated for selections of other kinds: those selections, like unknown
	 * names, return {@link Optional#empty()}. A stored empty item collection instead returns a
	 * present optional whose collection has no events.
	 * <p>
	 * Resolution captures a strong reference. A concurrent remove, replace or close does not revoke
	 * that reference, and this call may finish after such an operation.
	 */
	public Optional<IItemCollection> getItems(String name) {
		return getByName(name).filter(ItemBackedSelection.class::isInstance).map(ItemBackedSelection.class::cast)
				.map(ItemBackedSelection::getItems);
	}

	/**
	 * Returns an immutable metadata snapshot in registration order. A replacement occupies a new
	 * position at the end of that order. The snapshot remains usable after closure.
	 */
	public synchronized List<Entry> list() {
		requireOpen();
		List<Entry> entries = new ArrayList<>(byId.size());
		for (StoredSelection stored : byId.values()) {
			entries.add(stored.entry);
		}
		return List.copyOf(entries);
	}

	/**
	 * Removes a selection by ID without revoking references already returned to callers.
	 *
	 * @return whether the ID was registered
	 */
	public synchronized boolean remove(String id) {
		requireOpen();
		StoredSelection removed = byId.remove(Objects.requireNonNull(id, "id")); //$NON-NLS-1$
		if (removed == null) {
			return false;
		}
		byName.remove(removed.entry.getName());
		return true;
	}

	/**
	 * Removes a selection by its exact name.
	 *
	 * @return whether the name was registered
	 */
	public synchronized boolean removeByName(String name) {
		requireOpen();
		requireName(name);
		String id = byName.remove(name);
		if (id == null) {
			return false;
		}
		byId.remove(id);
		return true;
	}

	/**
	 * Releases every registered selection, leaving the registry open for further use.
	 */
	public synchronized void clear() {
		requireOpen();
		byId.clear();
		byName.clear();
	}

	/**
	 * Releases every registered selection and rejects subsequent reads and writes. This operation
	 * is idempotent and does not wait for callers that already obtained live selection references.
	 */
	@Override
	public synchronized void close() {
		byId.clear();
		byName.clear();
		closed = true;
	}

	/**
	 * @return whether the registry has been closed
	 */
	public synchronized boolean isClosed() {
		return closed;
	}

	private void requireOpen() {
		if (closed) {
			throw new IllegalStateException("Selection registry is closed"); //$NON-NLS-1$
		}
	}

	private static void requireName(String name) {
		Objects.requireNonNull(name, "name"); //$NON-NLS-1$
		if (name.isBlank()) {
			throw new IllegalArgumentException("Selection name must not be blank"); //$NON-NLS-1$
		}
	}
}
