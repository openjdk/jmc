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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.IPredicate;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.util.StringToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;

/**
 * Returns all events that intersects with the provided ranges, per thread.
 */
// FIXME: Not really related to item trees. Should be moved somewhere else.
public class TimeRangeThreadFilter implements IItemFilter {

	private Map<IMCThread, Range> rangeMap;

	private class TimeRangePredicate implements IPredicate<IItem> {
		@Override
		public boolean evaluate(IItem o) {
			IMCThread thread = RulesToolkit.getThread(o);

			if (!rangeMap.containsKey(thread)) {
				return false;
			}

			if (intersects(o, rangeMap.get(thread))) {
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
	 * Create an item filter based on a ranges per thread.
	 *
	 * @param rangeMap
	 *            Time ranges that items must intersect for each thread. Items from threads not in
	 *            this map will be ignored.
	 */
	public TimeRangeThreadFilter(Map<IMCThread, Range> rangeMap) {
		this.rangeMap = rangeMap;
	}

	/**
	 * Create an item filter based on a ranges per thread.
	 *
	 * @param thread
	 *            thread that items must belong to
	 * @param range
	 *            time ranges that items must intersect
	 */
	public TimeRangeThreadFilter(IMCThread thread, Range range) {
		this.rangeMap = new HashMap<>();
		rangeMap.put(thread, range);
	}

	@Override
	public IPredicate<IItem> getPredicate(IType<IItem> type) {
		return new TimeRangePredicate();
	}

	@Override
	public String toString() {
		return "TimeRangeThreadFilter " + toString(rangeMap); //$NON-NLS-1$
	}

	private String toString(Map<IMCThread, Range> rangeMap) {
		StringBuilder builder = new StringBuilder();
		builder.append("["); //$NON-NLS-1$
		List<String> texts = new ArrayList<>();

		for (Map.Entry<IMCThread, Range> entry : rangeMap.entrySet()) {
			texts.add(String.format("Thread: %s = Range: %s", entry.getKey().getThreadName(), //$NON-NLS-1$
					String.valueOf(entry.getValue())));
		}
		builder.append(StringToolkit.join(texts, ", ")); //$NON-NLS-1$
		builder.append("]"); //$NON-NLS-1$
		return builder.toString();
	}

	/**
	 * Get the range map.
	 *
	 * @return time ranges that items must intersect for each thread
	 */
	public Map<IMCThread, Range> getRangeMap() {
		return rangeMap;
	}
}
