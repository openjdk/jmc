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
package org.openjdk.jmc.flightrecorder.rules.tree;

import org.openjdk.jmc.common.IPredicate;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;

/**
 * Returns all events that intersects with the provided range.
 */
// FIXME: Not really related to item trees. Should be moved somewhere else.
public class TimeRangeFilter implements IItemFilter {

	private Range range;

	private class TimeRangePredicate implements IPredicate<IItem> {
		@Override
		public boolean evaluate(IItem o) {
			if (intersects(o, range)) {
				return true;
			}
			return false;
		}

		private boolean intersects(IItem o, Range range) {
			IQuantity startTime = RulesToolkit.getStartTime(o);
			IQuantity endTime = RulesToolkit.getStartTime(o);

			if (range.isInside(startTime) || range.isInside(endTime)) {
				return true;
			}
			if (range.isBefore(startTime) && range.isAfter(endTime)) {
				return true;
			}
			return false;
		}
	}

	/**
	 * Create an item filter based on a range.
	 *
	 * @param range
	 *            time range that items must intersect
	 */
	public TimeRangeFilter(Range range) {
		this.range = range;
	}

	@Override
	public IPredicate<IItem> getPredicate(IType<IItem> type) {
		return new TimeRangePredicate();
	}

	@Override
	public String toString() {
		return "TimeRangeFilter " + toString(range); //$NON-NLS-1$
	}

	private String toString(Range range) {
		StringBuilder builder = new StringBuilder();
		builder.append("["); //$NON-NLS-1$
		builder.append(String.format("Range: %s", range)); //$NON-NLS-1$
		builder.append("]"); //$NON-NLS-1$
		return builder.toString();
	}
}
