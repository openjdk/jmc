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

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.flightrecorder.ui.ItemCollectionToolkit;
import org.openjdk.jmc.flightrecorder.ui.JfrPropertySheet;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;

public class ItemBackedSelection extends FlavoredSelectionBase {

	private final IItemCollection selectedItems;

	public ItemBackedSelection(IItemCollection selectedItems, String name) {
		super(name);
		this.selectedItems = selectedItems;
	}

	@Override
	public Stream<IItemStreamFlavor> getFlavors(
		IItemFilter filter, IItemCollection items, List<IAttribute<?>> dstAttributes) {
		boolean itemsApplicableOnPage = ItemCollectionToolkit.filterIfNotNull(selectedItems, filter).hasItems();

		Builder<IItemStreamFlavor> builder = Stream.builder();

		// prio1/2: All selected items, first if they can be shown on the destination page, last otherwise
		IItemStreamFlavor selectedEventsFlavor = IItemStreamFlavor.build(MessageFormat.format(
				Messages.FLAVOR_SELECTED_EVENTS, ItemCollectionToolkit.getDescription(selectedItems)), selectedItems);

		if (itemsApplicableOnPage) {
			builder.accept(selectedEventsFlavor);
		}

		// prio2/1: Destination items with properties shared with the selected items
		IItemCollection dstItems = ItemCollectionToolkit.filterIfNotNull(items, filter);
		JfrPropertySheet.calculatePersistableFilterFlavors(selectedItems, dstItems, items, dstAttributes)
				.forEach(builder::accept);

		if (!itemsApplicableOnPage) {
			builder.accept(selectedEventsFlavor);
		}

		return builder.build();
	}
}
