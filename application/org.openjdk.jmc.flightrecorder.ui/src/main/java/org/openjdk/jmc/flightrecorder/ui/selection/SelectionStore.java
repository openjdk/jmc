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
package org.openjdk.jmc.flightrecorder.ui.selection;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;

/**
 * Store with all selections that should be kept. Used to transfer selections between pages (similar
 * to a clipboard).
 */
public class SelectionStore {

	public interface SelectionStoreListener {
		void selectionActive(boolean active);

		void selectionAdded(SelectionStoreEntry selection);
	}

	public static class SelectionStoreEntry {
		private final int index;
		private IFlavoredSelection selection;

		private SelectionStoreEntry(int index, IFlavoredSelection selection) {
			this.index = index;
			this.selection = selection;
		}

		public String getName() {
			return index + ": " + selection.getName(); //$NON-NLS-1$
		}

		public IFlavoredSelection getSelection() {
			return selection;
		}
	}

	private final LinkedList<SelectionStoreEntry> selections = new LinkedList<>();
	private int nextIndex = 1;
	private SelectionStoreEntry current;
	private boolean currentActive = false;
	// FIXME: Currently only one listener at a time to avoid bothering with unregistering. Consider allowing multiple listeners.
	private SelectionStoreListener listener;

	private static final IFlavoredSelection NO_SELECTION = new IFlavoredSelection() {
		@Override
		public boolean isEmpty() {
			return true;
		}

		@Override
		public String getName() {
			return Messages.SELECTION_STORE_NO_SELECTION;
		}

		@Override
		public Stream<IItemStreamFlavor> getFlavors(
			IItemFilter filter, IItemCollection items, List<IAttribute<?>> ignored) {
			return Stream.of(IItemStreamFlavor.build(getName(), isEmpty(), items));
		}
	};

	private static final SelectionStoreEntry NO_SELECTION_ENTRY = new SelectionStoreEntry(0, NO_SELECTION) {
		@Override
		public String getName() {
			return NO_SELECTION.getName();
		};
	};

	public SelectionStore() {
	}

	private int getNextIndex() {
		return nextIndex++;
	}

	/**
	 * @return All selections in the store
	 */
	public Stream<SelectionStoreEntry> getSelections() {
		// FIXME: If we decide to use a separate enable button in the flavor selector, then it is not necessary to use the special NO_SELECTION entry
		return Stream.concat(Stream.of(NO_SELECTION_ENTRY), selections.stream());
	}

	/**
	 * The current selection is the the last one set by {@link #setCurrent(IFlavoredSelection)} or
	 * {@link #addAndSetAsCurrentSelection(IFlavoredSelection)}. If none has ever been selected
	 * explicitly then it is the last one added by {@link #addSelection(IFlavoredSelection)}.
	 * <p>
	 * Note that you also need to check with {@link #isCurrentActive()} whether to use the current
	 * selection or not.
	 *
	 * @return The current selection or null if none is available
	 */
	public SelectionStoreEntry getCurrentSelection() {
		if (current != null) {
			return current;
		} else if (selections.size() > 0) {
			return selections.getFirst();
		}
		return null;
	}

	public boolean isCurrentActive() {
		return currentActive;
	}

	public void setCurrentActive(boolean currentActive) {
		this.currentActive = currentActive;
		fireSelectionActive(currentActive);
	}

	/**
	 * Set the currently active selection. Adds the selection to the store if it is not already
	 * present.
	 *
	 * @param selection
	 */
	public void setCurrent(IFlavoredSelection selection) {
		SelectionStoreEntry entry = null;
		if (selection == NO_SELECTION) {
			entry = NO_SELECTION_ENTRY;
		} else if (selection != null) {
			entry = findEntry(selection);
			if (entry == null) {
				entry = addSelectionInternal(selection, false);
			}
		}
		current = entry;
	}

	private SelectionStoreEntry findEntry(IFlavoredSelection selection) {
		for (SelectionStoreEntry entry : selections) {
			if (entry.getSelection() == selection) {
				return entry;
			}
		}
		return null;
	}

	/**
	 * Add a selection to the store.
	 *
	 * @param selection
	 */
	public void addSelection(IFlavoredSelection selection) {
		addSelectionInternal(selection, true);
	}

	private SelectionStoreEntry addSelectionInternal(IFlavoredSelection selection, boolean keepCurrent) {
		SelectionStoreEntry entry = new SelectionStoreEntry(getNextIndex(), selection);
		selections.push(entry);
		// FIXME: Should we listen for changes to this preference, and shrink the store if the size has decreased?
		long storeSize = Math.max(1, FlightRecorderUI.getDefault().getSelectionStoreSize().longValue());

		boolean currentRemoved = false;
		while (selections.size() > storeSize) {
			SelectionStoreEntry removed = selections.pollLast();
			if (removed == current) {
				currentRemoved = true;
			}
		}
		if (currentRemoved) {
			if (keepCurrent) {
				// Give the current entry a bonus place last on the list
				selections.addLast(current);
			} else {
				current = null;
			}
		}
		fireSelectionAdded(entry);
		return entry;
	}

	/**
	 * Add another selection to the store and set is as active
	 *
	 * @param selection
	 */
	public void addAndSetAsCurrentSelection(IFlavoredSelection selection) {
		setCurrent(selection);
		setCurrentActive(true);
	}

	public void setListener(SelectionStoreListener listener) {
		this.listener = listener;
	}

	private void fireSelectionAdded(SelectionStoreEntry selection) {
		if (listener != null) {
			listener.selectionAdded(selection);
		}
	}

	private void fireSelectionActive(boolean active) {
		if (listener != null) {
			listener.selectionActive(active);
		}
	}
}
