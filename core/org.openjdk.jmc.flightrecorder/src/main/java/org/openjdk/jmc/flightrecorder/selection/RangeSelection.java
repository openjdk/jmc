/*
 * Copyright (c) 2018, 2026, Oracle and/or its affiliates. All rights reserved.
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

import java.text.MessageFormat;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemCollectionToolkit;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.item.RangeMatchPolicy;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.selection.messages.internal.Messages;

/**
 * Selection of events matched by a range-valued attribute using a specified
 * {@link RangeMatchPolicy}. When the selection range is absent, all supplied events remain
 * selected. The selected-range aspect uses {@link RangeMatchPolicy#CONTAINED_IN_CLOSED} when
 * evaluated against an event collection.
 */
public class RangeSelection extends FlavoredSelectionBase {
	private final IItemCollection selectedItems;
	private final IAttribute<IRange<IQuantity>> rangeAttribute;
	private final IRange<IQuantity> range;
	private final IItemFilter rangeFilter;

	public RangeSelection(String name, IItemCollection sourceItems, IRange<IQuantity> selectionRange,
			RangeMatchPolicy matchPolicy, IAttribute<IRange<IQuantity>> rangeAttribute) {
		super(name);
		this.range = selectionRange;
		this.rangeAttribute = rangeAttribute;
		if (selectionRange != null) {
			this.rangeFilter = ItemFilters.matchRange(matchPolicy, rangeAttribute, selectionRange);
			/*
			 * The source collection includes events outside the selected range, so filter it here.
			 */
			this.selectedItems = sourceItems.apply(rangeFilter);
		} else {
			this.rangeFilter = null;
			// Without a range filter, all supplied events remain selected.
			this.selectedItems = sourceItems;
		}
	}

	/*
	 * This is the same implementation as PointRangeSelection, could break out to abstract super
	 * class, but waiting with that to make review easier.
	 */
	@Override
	public Stream<IItemStreamFlavor> getFlavors(
		IItemFilter dstFilter, IItemCollection items, List<IAttribute<?>> dstAttributes) {
		IItemCollection dstItems = ItemCollectionToolkit.filterIfNotNull(items, dstFilter);
		boolean itemsApplicableAtDestination = ItemCollectionToolkit.filterIfNotNull(selectedItems, dstFilter)
				.hasItems();
		Builder<IItemStreamFlavor> builder = Stream.builder();
		Predicate<IAttribute<?>> includeAttributes = a -> true;

		IItemStreamFlavor selectedItemsFlavor = IItemStreamFlavor
				.build(MessageFormat.format(Messages.getString(Messages.FLAVOR_SELECTED_EVENTS),
						ItemCollectionToolkit.getDescription(selectedItems)), selectedItems);

		if (rangeFilter != null) {
			IPropertyFlavor selectedRangeFlavor = buildRange(
					MessageFormat.format(Messages.getString(Messages.FLAVOR_SELECTED_RANGE),
							IPropertyFlavor.getIntervalDescription(rangeAttribute, range)),
					items);

			if (!itemsApplicableAtDestination) {
				// prio1: Selected range
				builder.accept(selectedRangeFlavor);
			}
			if (selectedItems.hasItems()) {
				// prio1/2: All selected items
				builder.accept(selectedItemsFlavor);
				selectedItemsFlavor = null;
			}
			if (itemsApplicableAtDestination) {
				// prio2: Selected range
				builder.accept(selectedRangeFlavor);
			}

			// Skip the selected attribute and the start/end times when selecting by lifetime.
			includeAttributes = a -> !a.equals(rangeAttribute) && !(rangeAttribute.equals(JfrAttributes.LIFETIME)
					&& (a.equals(JfrAttributes.START_TIME) || a.equals(JfrAttributes.END_TIME)));

		} else if (selectedItems.hasItems()) {
			if (itemsApplicableAtDestination) {
				// prio1/2: All selected items
				builder.accept(selectedItemsFlavor);
				selectedItemsFlavor = null;
			}
		}

		// prio3: Destination items with properties shared with the selected items
		SelectionAspectToolkit
				.calculatePersistableFilterFlavors(selectedItems, dstItems, items, dstAttributes, includeAttributes)
				.forEach(builder::accept);
		if (selectedItemsFlavor != null) {
			builder.accept(selectedItemsFlavor);
		}
		return builder.build();
	}

	private IPropertyFlavor buildRange(String name, IItemCollection dstItems) {
		return IPropertyFlavor.buildRange(name, rangeAttribute, range, dstItems);
	}
}
