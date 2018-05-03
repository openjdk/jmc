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
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.QuantityRange;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.ui.ItemCollectionToolkit;
import org.openjdk.jmc.flightrecorder.ui.JfrPropertySheet;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;

public class ChartSelection extends FlavoredSelectionBase {
	private final IItemCollection selectedRowsItems;
	private final IAttribute<IQuantity> xAttribute;
	private final IItemFilter rangeFilter;
	private final IRange<IQuantity> range;

	public ChartSelection(String name, IItemCollection selectedItems, IQuantity selectionStart, IQuantity selectionEnd,
			IAttribute<IQuantity> xAttribute) {
		super(name);
		this.xAttribute = xAttribute;
		if (selectionStart != null && selectionEnd != null) {
			this.range = QuantityRange.createWithEnd(selectionStart, selectionEnd);
			this.rangeFilter = ItemFilters.interval(xAttribute, range.getStart(), true, range.getEnd(), true);
			/*
			 * selectedRowItems parameter is the full item collection of the selected row(s), not
			 * the items inside the range, so need to filter here.
			 */
			this.selectedRowsItems = selectedItems.apply(rangeFilter);
		} else {
			this.range = null;
			this.rangeFilter = null;
			// If the range filter isn't present, the selection will be the entire row
			this.selectedRowsItems = selectedItems;
		}
	}

	/*
	 * This is the same implementation as RangedChartSelection, could break out to abstract super
	 * class, but waiting with that to make review easier.
	 */
	@Override
	public Stream<IItemStreamFlavor> getFlavors(
		IItemFilter dstFilter, IItemCollection items, List<IAttribute<?>> dstAttributes) {
		IItemCollection dstItems = ItemCollectionToolkit.filterIfNotNull(items, dstFilter);
		boolean itemsApplicableOnPage = ItemCollectionToolkit.filterIfNotNull(selectedRowsItems, dstFilter).hasItems();
		Builder<IItemStreamFlavor> builder = Stream.builder();
		Predicate<IAttribute<?>> includeAttributes = a -> true;

		IItemStreamFlavor selectedItemsFlavor = IItemStreamFlavor.build(MessageFormat
				.format(Messages.FLAVOR_SELECTED_EVENTS, ItemCollectionToolkit.getDescription(selectedRowsItems)),
				selectedRowsItems);

		if (rangeFilter != null) {
			IPropertyFlavor selectedRangeFlavor = buildRange(MessageFormat.format(Messages.FLAVOR_SELECTED_RANGE,
					IPropertyFlavor.getIntervalDescription(xAttribute, range)), items);

			if (!itemsApplicableOnPage) {
				// prio1: Selected range
				builder.accept(selectedRangeFlavor);
			}
			if (selectedRowsItems.hasItems()) {
				// prio1/2: All selected items
				builder.accept(selectedItemsFlavor);
				selectedItemsFlavor = null;
			}
			if (itemsApplicableOnPage) {
				// prio2: Selected range
				builder.accept(selectedRangeFlavor);
			}

			// Skip the x attribute, plus skip start time and end time if the x attribute is life time (more likely in the ranged chart case)
			includeAttributes = a -> !a.equals(xAttribute) && !(xAttribute.equals(JfrAttributes.LIFETIME)
					&& (a.equals(JfrAttributes.START_TIME) || a.equals(JfrAttributes.END_TIME)));

		} else if (selectedRowsItems.hasItems()) {
			if (itemsApplicableOnPage) {
				// prio1/2: All selected items
				builder.accept(selectedItemsFlavor);
				selectedItemsFlavor = null;
			}
		}

		// prio3: Destination items with properties shared with the selected items
		JfrPropertySheet
				.calculatePersistableFilterFlavors(selectedRowsItems, dstItems, items, dstAttributes, includeAttributes)
				.forEach(builder::accept);
		if (selectedItemsFlavor != null) {
			builder.accept(selectedItemsFlavor);
		}
		return builder.build();
	}

	private IPropertyFlavor buildRange(String name, IItemCollection items) {
		return IPropertyFlavor.buildPointRange(name, xAttribute, range, items);
	}
}
