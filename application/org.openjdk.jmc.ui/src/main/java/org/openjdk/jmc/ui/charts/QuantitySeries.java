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
package org.openjdk.jmc.ui.charts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.ItemToolkit;
import org.openjdk.jmc.common.unit.IQuantity;

public class QuantitySeries<T> {

	// FIXME: Move and rename
	public static ISpanSeries<IItem> max(
		IItemCollection items, IAttribute<IQuantity> xAttribute, IAttribute<IQuantity> yAttribute) {
		return new ISpanSeries<IItem>() {

			@Override
			public XYQuantities<IItem[]> getQuantities(SubdividedQuantityRange xBucketRange) {
				SubdividedQuantityRange xRange = xBucketRange.copyWithPixelSubdividers();
				IQuantity[] values = new IQuantity[xRange.getNumSubdividers()];
				IItem[] itemsArray = new IItem[xRange.getNumSubdividers()];
				for (IItemIterable next : items) {
					IMemberAccessor<IQuantity, IItem> xValueAccessor = xAttribute.getAccessor(next.getType());
					IMemberAccessor<IQuantity, ? super IItem> yValueAccessor = yAttribute.getAccessor(next.getType());
					for (IItem item : next) {
						int xPos = xRange.getFloorSubdivider(xValueAccessor.getMember(item));
						if (xPos < values.length) {
							xPos = xPos < 0 ? 0 : xPos;
							IQuantity value = yValueAccessor.getMember(item);
							if (value != null) {
								IQuantity currentValue = values[xPos];
								if ((currentValue == null) || value.compareTo(currentValue) > 0) {
									values[xPos] = value;
									itemsArray[xPos] = item;
								}
							}
						}
					}
				}
				return XYQuantities.create(itemsArray, Arrays.asList(values), xRange);
			}

			@Override
			public IQuantity getStartX(IItem item) {
				return xAttribute.getAccessor(ItemToolkit.getItemType(item)).getMember(item);
			}
		};
	}

	public static IQuantitySeries<?> all(
		Iterator<? extends IItem> items, IMemberAccessor<? extends IQuantity, IItem> xValueAccessor,
		IMemberAccessor<? extends IQuantity, IItem> yValueAccessor) {
		List<IQuantity> xValues = new ArrayList<>(100);
		List<IQuantity> yValues = new ArrayList<>(100);
		while (items.hasNext()) {
			IItem item = items.next();
			xValues.add(xValueAccessor.getMember(item));
			yValues.add(yValueAccessor.getMember(item));
		}
		return all(xValues, yValues);
	}

	public static IQuantitySeries<?> all(final List<IQuantity> xValues, final List<IQuantity> yValues) {
		return all(xValues, yValues, null);
	}

	public static <T> IQuantitySeries<T> all(
		final List<IQuantity> xValues, final List<IQuantity> yValues, final T payload) {
		return new IQuantitySeries<T>() {

			@Override
			public XYQuantities<T> getQuantities(SubdividedQuantityRange xBucketRange) {
				int from = Collections.binarySearch(xValues, xBucketRange.getStart());
				if (from < 0) {
					// On inexact match
					from = Math.max((-from - 1) - 1, 0); // use index before returned value
				}
				int to = Collections.binarySearch(xValues, xBucketRange.getEnd());
				if (to < 0) {
					// On inexact match
					to = Math.min((-to - 1), xValues.size() - 1); // set to as last included index
				}
				return XYQuantities.create(payload, xValues.subList(from, to + 1), yValues.subList(from, to + 1),
						xBucketRange);
			}
		};
	}
}
