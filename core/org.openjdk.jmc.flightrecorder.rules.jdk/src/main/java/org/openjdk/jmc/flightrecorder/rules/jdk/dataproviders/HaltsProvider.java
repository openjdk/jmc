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
package org.openjdk.jmc.flightrecorder.rules.jdk.dataproviders;

import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.QuantityRange;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.rules.tree.ITreeNode;
import org.openjdk.jmc.flightrecorder.rules.tree.ItemTreeBuilder;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.SlidingWindowToolkit.IUnorderedWindowValueFunction;

/**
 * Helper class with useful methods for calculating various data about JVM halts (GC pauses, thread
 * dumps and so on).
 */
public class HaltsProvider {
	/**
	 * Calculates the ratio of GC pauses to the total time.
	 *
	 * @param items
	 *            items to do calculation on
	 * @return GC/total ratio in percent
	 */
	public static IQuantity calculateGcPauseRatio(IItemCollection items) {
		IRange<IQuantity> itemRange = createRange(items);
		return calculateHaltsRatio(items, items.getAggregate(JdkAggregators.TOTAL_GC_PAUSE), itemRange.getStart(),
				itemRange.getEnd());
	}

	/**
	 * Calculates the ratio of GC pauses to the total time.
	 *
	 * @param items
	 *            items to do calculation on
	 * @param startTime
	 *            start of timespan to do calculation in
	 * @param endTime
	 *            end of timespan to do calculation in
	 * @return GC/total ratio in percent
	 */
	public static IQuantity calculateGcHaltsRatio(IItemCollection items, IQuantity startTime, IQuantity endTime) {
		return calculateHaltsRatio(items, calculatePauseSum(items, JdkFilters.GC_PAUSE, startTime, endTime), startTime,
				endTime);
	}

	/**
	 * Calculates the ratio of all application pauses to the total time.
	 *
	 * @param items
	 *            items to do calculation on
	 * @return pauses/total ratio in percent
	 */
	public static ApplicationHaltsInfoHolder calculateApplicationHaltsRatio(IItemCollection items) {
		IRange<IQuantity> itemRange = createRange(items);
		return calculateHaltsRatiosWithEncapsulationTree(items, itemRange.getStart(), itemRange.getEnd());
	}

	private static ApplicationHaltsInfoHolder calculateHaltsRatiosWithEncapsulationTree(
		IItemCollection items, IQuantity startTime, IQuantity endTime) {
		IItemFilter haltsFilter = JdkFilters.APPLICATION_PAUSES;
		IItemFilter gcFilter = JdkFilters.GC_PAUSE;
		IItemFilter safepointFilter = JdkFilters.SAFE_POINTS;

		IQuantity haltsDuration = calculatePauseSumWithEncapsulationTree(items, haltsFilter, startTime, endTime);
		IQuantity gcDuration = calculatePauseSum(items, gcFilter, startTime, endTime);
		IQuantity safepointDuration = calculatePauseSum(items, safepointFilter, startTime, endTime);
		IQuantity haltsRatio = calculateHaltsRatio(items, haltsDuration, startTime, endTime);
		IQuantity gcRatio = calculateHaltsRatio(items, gcDuration, startTime, endTime);
		IQuantity safepointRatio = calculateHaltsRatio(items, safepointDuration, startTime, endTime);
		return new ApplicationHaltsInfoHolder(gcRatio, safepointRatio, haltsRatio);
	}

	private static IQuantity calculateHaltsRatio(
		IItemCollection items, IQuantity pauseTime, IQuantity startTime, IQuantity endTime) {
		// FIXME: Use the item range instead of the time range, in case the sliding window logic slides outside the item range. Or fix the sliding window logic.
		IRange<IQuantity> range = QuantityRange.createWithEnd(startTime, endTime);
		IQuantity totalTime = range.getExtent();
		if (pauseTime != null) {
			return RulesToolkit.toRatioPercent(pauseTime, totalTime);
		}
		return UnitLookup.PERCENT.quantity(0);
	}

	private static IQuantity calculatePauseSum(
		IItemCollection items, IItemFilter pauseFilter, IQuantity startTime, IQuantity endTime) {
		IItemCollection pauses = items.apply(pauseFilter);
		IQuantity pauseTime = UnitLookup.NANOSECOND.quantity(0);
		for (IItemIterable ii : pauses) {
			for (IItem item : ii) {
				pauseTime = pauseTime.add(RulesToolkit.getDurationInWindow(startTime, endTime, item));
			}
		}
		return pauseTime;
	}

	private static IQuantity calculatePauseSumWithEncapsulationTree(
		IItemCollection items, IItemFilter haltsFilter, IQuantity startTime, IQuantity endTime) {
		IItemCollection filteredCollection = items.apply(haltsFilter);
		ITreeNode<IItem> root = ItemTreeBuilder.buildEncapsulationTree(filteredCollection, false, true);
		IQuantity totalDuration = UnitLookup.NANOSECOND.quantity(0);
		for (ITreeNode<IItem> child : root.getChildren()) {
			IQuantity childDuration = RulesToolkit.getDurationInWindow(startTime, endTime, child.getValue());
			totalDuration = totalDuration.add(childDuration);
		}
		return totalDuration;
	}

	private static IRange<IQuantity> createRange(IItemCollection items) {
		IQuantity start = items.getAggregate(JdkAggregators.FIRST_ITEM_START);
		IQuantity end = items.getAggregate(JdkAggregators.LAST_ITEM_END);
		return start != null && end != null ? QuantityRange.createWithEnd(start, end) : null;
	}

	/**
	 * @return function to use when calculating GC halts using sliding windows
	 */
	public static IUnorderedWindowValueFunction<IQuantity> gcHaltsRatioFunction() {
		return new IUnorderedWindowValueFunction<IQuantity>() {

			@Override
			public IQuantity getValue(IItemCollection items, IQuantity startTime, IQuantity endTime) {
				return calculateGcHaltsRatio(items, startTime, endTime);
			}
		};
	}

	/**
	 * @return function to use when calculating application halts using sliding windows
	 */
	public static IUnorderedWindowValueFunction<ApplicationHaltsInfoHolder> applicationHaltsRatioFunction() {
		return new IUnorderedWindowValueFunction<ApplicationHaltsInfoHolder>() {

			@Override
			public ApplicationHaltsInfoHolder getValue(IItemCollection items, IQuantity startTime, IQuantity endTime) {
				return calculateHaltsRatiosWithEncapsulationTree(items, startTime, endTime);
			}
		};
	}

	public static class ApplicationHaltsInfoHolder {
		private final IQuantity gcPauseRatio;
		private final IQuantity safePointRatio;
		private final IQuantity totalHaltsRatio;
		private final IQuantity nonGcHaltsRatio;
		private final IQuantity nonGcHaltsToTotalRatio;

		ApplicationHaltsInfoHolder(IQuantity gcPauseRatio, IQuantity safePointRatio, IQuantity totalHaltsRatio) {
			this.gcPauseRatio = gcPauseRatio;
			this.safePointRatio = safePointRatio;
			this.totalHaltsRatio = totalHaltsRatio;
			this.nonGcHaltsRatio = totalHaltsRatio.subtract(gcPauseRatio);
			this.nonGcHaltsToTotalRatio = totalHaltsRatio.doubleValue() == 0 ? UnitLookup.PERCENT.quantity(0)
					: RulesToolkit.toRatioPercent(nonGcHaltsRatio, totalHaltsRatio);
		}

		public IQuantity getGcPauseRatio() {
			return gcPauseRatio;
		}

		public IQuantity getSafePointRatio() {
			return safePointRatio;
		}

		public IQuantity getTotalHaltsRatio() {
			return totalHaltsRatio;
		}

		public IQuantity getNonGcHaltsRatio() {
			return nonGcHaltsRatio;
		}

		public IQuantity getNonGcHaltsToTotalRatio() {
			return nonGcHaltsToTotalRatio;
		}
	}
}
