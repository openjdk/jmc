/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, Red Hat Inc. All rights reserved.
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

package org.openjdk.jmc.flightrecorder.ui.common;

import org.HdrHistogram.ConcurrentHistogram;
import org.HdrHistogram.Histogram;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemConsumer;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.Pair;
import org.openjdk.jmc.flightrecorder.JfrAttributes;

/**
 * An HdrHistogram containing durations of Flight Recorder events.
 *
 * @see org.HdrHistogram
 * @see JfrAttributes#DURATION
 */
public class DurationHdrHistogram {

	// Precision of values stored in the HdrHistogram, in terms of significant digits
	private static final int SIGNIFICANT_DIGITS = 3;

	private final Histogram histogram;

	public DurationHdrHistogram() {
		// Use ConcurrentHistogram, as we may have multiple consumers recording in parallel
		this.histogram = new ConcurrentHistogram(SIGNIFICANT_DIGITS);
	}

	/**
	 * Consumer responsible for recording duration quantities in the HdrHistogram.
	 */
	static class DurationItemConsumer implements IItemConsumer<DurationItemConsumer> {

		private final DurationHdrHistogram durationHist;
		private final IMemberAccessor<IQuantity, IItem> accessor;

		public DurationItemConsumer(DurationHdrHistogram durationHist, IMemberAccessor<IQuantity, IItem> accessor) {
			this.durationHist = durationHist;
			this.accessor = accessor;
		}

		@Override
		public void consume(IItem item) {
			IQuantity quantity = accessor.getMember(item);
			durationHist.histogram.recordValue(quantity.clampedLongValueIn(UnitLookup.NANOSECOND));
		}

		@Override
		public DurationItemConsumer merge(DurationItemConsumer other) {
			// No-op, all consumers should be backed by the same histogram
			return this;
		}

	}

	/**
	 * Computes the duration at a given percentile for values stored in the histogram.
	 * @param percentile - the percentile, as a {@link UnitLookup#NUMBER}
	 * @return the computed duration, as a {@link UnitLookup#TIMESPAN}
	 */
	public IQuantity getDurationAtPercentile(IQuantity percentile) {
		long rawValue = histogram.getValueAtPercentile(percentile.doubleValue());
		IQuantity duration = UnitLookup.NANOSECOND.quantity(rawValue);
		return duration;
	}

	/**
	 * Computes the duration at a given percentile for values stored
	 * in the histogram, and number of values at or above that duration.
	 * @param percentile - the percentile, as a {@link UnitLookup#NUMBER}
	 * @return a pair with the computed duration as a {@link UnitLookup#TIMESPAN},
	 * 	       and item count as a {@link UnitLookup#NUMBER}, in that order
	 */
	public Pair<IQuantity, IQuantity> getDurationAndCountAtPercentile(IQuantity percentile) {
		long rawValue = histogram.getValueAtPercentile(percentile.doubleValue());
		IQuantity duration = UnitLookup.NANOSECOND.quantity(rawValue);
		long rawCount = histogram.getCountBetweenValues(rawValue, histogram.getMaxValue());
		IQuantity count = UnitLookup.NUMBER_UNITY.quantity(rawCount);
		return new Pair<>(duration, count);
	}

	/**
	 * @return whether this histogram is empty
	 */
	public boolean isEmpty() {
		return getTotalCount() == 0L;
	}

	/**
	 * @return the total number of items present in the histogram
	 */
	public long getTotalCount() {
		return histogram.getTotalCount();
	}

	/**
	 * Gets the lowest value considered equivalent by this histogram,
	 * subject to its configured precision. This is effectively a lower
	 * bound for the "bucket" the specified value would fall under.
	 * @see Histogram#lowestEquivalentValue(long)
	 * @param duration - the specified duration quantity
	 * @return the lowest duration equivalent to the supplied argument
	 */
	public IQuantity getLowestEquivalentDuration(IQuantity duration) {
		long rawValue = duration.clampedLongValueIn(UnitLookup.NANOSECOND);
		long lowestEquivalent = histogram.lowestEquivalentValue(rawValue);
		return UnitLookup.NANOSECOND.quantity(lowestEquivalent);
	}

	/**
	 * Resets this histogram to its initial state, deleting all values from it.
	 */
	public void reset() {
		histogram.reset();
	}

}
