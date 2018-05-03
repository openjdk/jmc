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
package org.openjdk.jmc.flightrecorder.rules.jdk.combine;

import static org.openjdk.jmc.common.unit.UnitLookup.EPOCH_NS;
import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER_UNITY;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.flightrecorder.JfrAttributes;

/**
 * Toolkit for calculating combined span values. Creates spans which consist of at least a start
 * time, an end time and a value. Spans are then combined together (see {@link Combinable} and
 * {@link Combiner}) to form the largest time span where the combined value is still deemed to be
 * over some threshold. Threshold can be either a defined limit or a calculated density.
 * <p>
 * Currently, it's up to the caller of this toolkit to call the methods with a subset of items that
 * have only one item iterable, which means that events should not overlap in time.
 */
public class SpanToolkit {
	// FIXME: Consider letting the spans handle quantities, but will increase allocation?

	/**
	 * Calculates the largest count cluster.
	 *
	 * @param items
	 *            the item collection
	 * @param countAttribute
	 *            the attribute to get count value from, is assumed to be an accumulative value, for
	 *            example exception statistics
	 * @param timestampAttribute
	 *            the attribute to get a time stamp from
	 * @return The maximum count span
	 */
	public static SpanSquare getMaxCountCluster(
		IItemCollection items, IAttribute<IQuantity> countAttribute, IAttribute<IQuantity> timestampAttribute) {

		Iterator<? extends IItemIterable> iiIterator = items.iterator();
		// FIXME: Handle multiple IItemIterable
		if (iiIterator.hasNext()) {
			IItemIterable ii = iiIterator.next();
			Iterator<? extends IItem> itemIt = ii.iterator();
			if (itemIt.hasNext()) {
				IType<IItem> type = ii.getType();
				IMemberAccessor<IQuantity, IItem> countAccessor = countAttribute.getAccessor(type);
				IMemberAccessor<IQuantity, IItem> timeAccessor = timestampAttribute.getAccessor(type);

				List<SpanSquare> spans = new ArrayList<>();
				IItem first = itemIt.next();
				long lastCount = countAccessor.getMember(first).clampedLongValueIn(NUMBER_UNITY);
				long lastTimestamp = timeAccessor.getMember(first).clampedLongValueIn(EPOCH_NS);
				while (itemIt.hasNext()) {
					IItem item = itemIt.next();
					long count = countAccessor.getMember(item).clampedLongValueIn(NUMBER_UNITY);
					long time = timeAccessor.getMember(item).clampedLongValueIn(EPOCH_NS);
					spans.add(new SpanSquare(lastTimestamp, time, count - lastCount));
					lastCount = count;
					lastTimestamp = time;
				}
				return SpanSquare.getMax(spans.toArray(new SpanSquare[spans.size()]));
			}
		}
		return null;
	}

	/**
	 * Calculates the largest duration cluster.
	 *
	 * @param items
	 *            the item collection
	 * @return The maximum duration span
	 */
	public static SpanSquare getMaxDurationCluster(IItemCollection items) {
		Iterator<? extends IItemIterable> iiIterator = items.iterator();
		// FIXME: Handle multiple IItemIterable
		if (iiIterator.hasNext()) {
			IItemIterable ii = iiIterator.next();
			Iterator<? extends IItem> itemIt = ii.iterator();
			IType<IItem> type = ii.getType();
			IMemberAccessor<IQuantity, IItem> startTime = JfrAttributes.START_TIME.getAccessor(type);
			IMemberAccessor<IQuantity, IItem> endTime = JfrAttributes.END_TIME.getAccessor(type);

			List<SpanSquare> span = new ArrayList<>();
			while (itemIt.hasNext()) {
				IItem item = itemIt.next();
				long st = startTime.getMember(item).clampedLongValueIn(EPOCH_NS);
				long et = endTime.getMember(item).clampedLongValueIn(EPOCH_NS);
				span.add(new SpanSquare(st, et));
			}
			return SpanSquare.getMax(span.toArray(new SpanSquare[span.size()]));
		}
		return null;
	}

	/**
	 * Calculates the longest span where the combined value still is above the limit.
	 *
	 * @param items
	 *            the item collection
	 * @param valueAttribute
	 *            the value attribute
	 * @param endTimeAttribute
	 *            the end time attribute
	 * @param limit
	 *            the min limit
	 * @return a span
	 */
	public static SpanLimit getMaxSpanLimit(
		IItemCollection items, IAttribute<IQuantity> valueAttribute, IAttribute<IQuantity> endTimeAttribute,
		double limit) {
		SpanLimit max = null;
		List<SpanLimit> periods = new ArrayList<>();
		boolean first = true;
		double lastCount = 0;
		long lastTimestamp = 0;
		Iterator<IItemIterable> itemsIterator = items.iterator();
		// FIXME: Handle multiple IItemIterable
		if (itemsIterator.hasNext()) {
			IItemIterable itemIterable = itemsIterator.next();
			Iterator<? extends IItem> itemIterator = itemIterable.iterator();
			IMemberAccessor<IQuantity, IItem> valueAccessor = valueAttribute.getAccessor(itemIterable.getType());
			IMemberAccessor<IQuantity, IItem> endTimeAccessor = endTimeAttribute.getAccessor(itemIterable.getType());
			while (itemIterator.hasNext()) {
				IItem item = itemIterator.next();
				IQuantity value = valueAccessor.getMember(item);
				if (value != null) {
					double count = value.doubleValue();
					long time = endTimeAccessor.getMember(item).clampedLongValueIn(EPOCH_NS);
					if (first) {
						lastCount = count;
						lastTimestamp = time;
						first = false;
					} else {
						periods.add(new SpanLimit(lastTimestamp, time, (count + lastCount) / 2, limit));
						lastCount = count;
						lastTimestamp = time;
					}
				}
			}
			// FIXME: Should we use max span or max value, or get all the spans over the limit?
//			int periodCount = Combiner.combine(periods);
			if (periods.size() > 0) {
				max = SpanLimit.getMaxSpan(periods.toArray(new SpanLimit[0]));
			}
		}
		return max;
	}
}
