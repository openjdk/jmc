/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.rules.jdk.memory;

import static org.openjdk.jmc.common.unit.UnitLookup.SECOND;
import static org.openjdk.jmc.common.unit.UnitLookup.TIMESPAN;
import static org.openjdk.jmc.flightrecorder.rules.jdk.RulePreferences.SHORT_RECORDING_LIMIT;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.QuantityRange;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.Pair;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.AbstractRule;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.ResultBuilder;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.RequiredEventsBuilder;
import org.openjdk.jmc.flightrecorder.rules.util.SlidingWindowToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.SlidingWindowToolkit.IUnorderedWindowVisitor;

/**
 * Rule that checks how much memory was freed per second, and compares that to the liveset.
 * Calculations are done in time windows. Rule is designed to only require the GCHeapSummary event.
 */
public class GcFreedRatioRule extends AbstractRule {

	private static final String NEW_PARAGRAPH = "\n"; //$NON-NLS-1$
	private static final String SPACE = " "; //$NON-NLS-1$

	private static final TypedPreference<IQuantity> GC_FREED_PER_SECOND_TO_LIVESET_RATIO_INFO_LIMIT = new TypedPreference<>(
			"gc.freed.per.second.to.liveset.ratio.info.limit", //$NON-NLS-1$
			Messages.getString(Messages.GcFreedRatioRule_GC_FREED_RATIO_INFO_LIMIT),
			Messages.getString(Messages.GcFreedRatioRule_GC_FREED_RATIO_INFO_LIMIT_DESC), UnitLookup.PERCENTAGE,
			UnitLookup.PERCENT_UNITY.quantity(10));
	public static final TypedPreference<IQuantity> WINDOW_SIZE = new TypedPreference<>(
			"gc.freed.per.second.to.liveset.ratio.window.size", //$NON-NLS-1$
			Messages.getString(Messages.GcFreedRatioRule_WINDOW_SIZE),
			Messages.getString(Messages.GcFreedRatioRule_WINDOW_SIZE_DESC), TIMESPAN, SECOND.quantity(10));
	public static final TypedPreference<IQuantity> FEW_GCS_LIMIT = new TypedPreference<>("few.gcs.limit", //$NON-NLS-1$
			Messages.getString(Messages.GcFreedRatioRule_FEW_GCS_LIMIT),
			Messages.getString(Messages.GcFreedRatioRule_FEW_GCS_LIMIT_DESC), UnitLookup.NUMBER,
			UnitLookup.NUMBER_UNITY.quantity(10));

	private static final Collection<TypedPreference<?>> CONFIGURATION_ATTRIBUTES = Arrays
			.<TypedPreference<?>> asList(GC_FREED_PER_SECOND_TO_LIVESET_RATIO_INFO_LIMIT, WINDOW_SIZE, FEW_GCS_LIMIT);

	public static final TypedResult<IQuantity> HEAP_SUMMARY_EVENTS = new TypedResult<>("heapSummarys", //$NON-NLS-1$
			"Heap Summary Events", "Heap Summary Events", UnitLookup.NUMBER, IQuantity.class);
	public static final TypedResult<IQuantity> GC_FREED_RATIO = new TypedResult<>("gcFreedRatio", "GC Freed Ratio", //$NON-NLS-1$
			"The ratio of memory freed by gc and liveset.", UnitLookup.PERCENTAGE, IQuantity.class);
	public static final TypedResult<IQuantity> GC_FREED_PER_SECOND = new TypedResult<>("gcFreedPerSecond", //$NON-NLS-1$
			"GC Freed Per Second", "The amount of memory freed per second.", UnitLookup.MEMORY, IQuantity.class);
	public static final TypedResult<IRange<IQuantity>> GC_WINDOW = new TypedResult<>("gcWindoow", "Window", //$NON-NLS-1$
			"The window where the most amount of memory freed occurred.", UnitLookup.TIMERANGE);
	public static final TypedResult<IQuantity> AVERAGE_LIVESET = new TypedResult<>("averageLiveset", "Average Liveset", //$NON-NLS-1$
			"The average amount of live memory.", UnitLookup.MEMORY, IQuantity.class);

	private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays.<TypedResult<?>> asList(
			TypedResult.SCORE, HEAP_SUMMARY_EVENTS, GC_FREED_PER_SECOND, GC_FREED_RATIO, AVERAGE_LIVESET, GC_WINDOW);

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.HEAP_SUMMARY, EventAvailability.ENABLED)
			.addEventType(JdkTypeIDs.ALLOC_INSIDE_TLAB, EventAvailability.ENABLED)
			.addEventType(JdkTypeIDs.ALLOC_OUTSIDE_TLAB, EventAvailability.ENABLED).build();

	public GcFreedRatioRule() {
		super("GcFreedRatio", Messages.getString(Messages.GcFreedRatioRule_RULE_NAME), JfrRuleTopics.HEAP, //$NON-NLS-1$
				CONFIGURATION_ATTRIBUTES, RESULT_ATTRIBUTES, REQUIRED_EVENTS);
	}

	@Override
	protected IResult getResult(IItemCollection items, IPreferenceValueProvider vp, IResultValueProvider rp) {
		double infoLimit = vp.getPreferenceValue(GC_FREED_PER_SECOND_TO_LIVESET_RATIO_INFO_LIMIT).doubleValue();
		IQuantity windowSize = vp.getPreferenceValue(WINDOW_SIZE);
		IQuantity slideSize = windowSize.getUnit().quantity(windowSize.ratioTo(windowSize.getUnit().quantity(2)));
		IQuantity shortRecordingLimit = vp.getPreferenceValue(SHORT_RECORDING_LIMIT);
		IQuantity fewGcsLimit = vp.getPreferenceValue(FEW_GCS_LIMIT);

		IQuantity heapSummaryCount = items.getAggregate(Aggregators.count(ItemFilters.type(JdkTypeIDs.HEAP_SUMMARY)));
		if (heapSummaryCount.compareTo(fewGcsLimit) < 0) {
			return ResultBuilder.createFor(this, vp).setSeverity(Severity.OK)
					.setSummary(Messages.getString(Messages.GcFreedRatioRule_RESULT_FEW_GCS))
					.addResult(HEAP_SUMMARY_EVENTS, heapSummaryCount).build();
		}

		// Do the rule calculations
		GcInfoHolder maxFreedGcInfo = getMaxFreedWindow(items, windowSize, slideSize);
		double freedRatio = maxFreedGcInfo.freedPerSecondToLivesetRatio.doubleValueIn(UnitLookup.PERCENT_UNITY);
		double score = RulesToolkit.mapExp74(freedRatio, infoLimit);
		// FIXME: Check if range is null
		String longDescription = Messages.getString(Messages.GcFreedRatioRule_RESULT_LONG_DESCRIPTION);

		String shortDescription = Messages.getString(Messages.GcFreedRatioRule_RESULT_SHORT_DESCRIPTION);
		if (score < Severity.INFO.getLimit()) {
			shortDescription += SPACE + Messages.getString(Messages.GcFreedRatioRule_RESULT_OK);
			longDescription += SPACE + Messages.getString(Messages.GcFreedRatioRule_RESULT_OK);
		} else {
			shortDescription += SPACE + Messages.getString(Messages.GcFreedRatioRule_RESULT_NOT_OK);
			longDescription += SPACE + Messages.getString(Messages.GcFreedRatioRule_RESULT_NOT_OK);
			longDescription += NEW_PARAGRAPH + Messages.getString(Messages.GcFreedRatioRule_RESULT_MORE_INFO);
		}

		String shortRecordingInfo = RulesToolkit.getShortRecordingInfo(items, shortRecordingLimit);
		if (shortRecordingInfo != null) {
			longDescription += NEW_PARAGRAPH + shortRecordingInfo;
			// Halving score for short recordings
			score = score > 0 ? score / 2 : score;
		}
		return ResultBuilder.createFor(this, vp).setSeverity(Severity.get(score)).setSummary(shortDescription)
				.setExplanation(longDescription).addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(score))
				.addResult(GC_FREED_RATIO, maxFreedGcInfo.freedPerSecondToLivesetRatio)
				.addResult(HEAP_SUMMARY_EVENTS, heapSummaryCount).addResult(GC_WINDOW, maxFreedGcInfo.range)
				.addResult(AVERAGE_LIVESET, maxFreedGcInfo.averageLiveset)
				.addResult(GC_FREED_PER_SECOND, maxFreedGcInfo.freedPerSecond).build();
	}

	private GcInfoHolder getMaxFreedWindow(final IItemCollection allItems, IQuantity windowSize, IQuantity slideSize) {
		final GcInfoHolder maxFreedGcInfo = new GcInfoHolder();
		maxFreedGcInfo.freedPerSecondToLivesetRatio = UnitLookup.PERCENT.quantity(0);
		maxFreedGcInfo.freedPerSecond = UnitLookup.BYTE.quantity(0);
		maxFreedGcInfo.averageLiveset = UnitLookup.BYTE.quantity(0);
		maxFreedGcInfo.range = QuantityRange.createWithEnd(UnitLookup.EPOCH_MS.quantity(0),
				UnitLookup.EPOCH_MS.quantity(0));

		// FIXME: Check which of heapSummarySlide and normal sliding window that seems to give the best result

		SlidingWindowToolkit.slidingWindowUnordered(new IUnorderedWindowVisitor() {

			@Override
			public void visitWindow(IItemCollection windowItems, IQuantity startTime, IQuantity endTime) {
				Pair<IItemCollection, IRange<IQuantity>> windowRangePair = getWindowWithPairedHeapSummaryEvents(
						windowItems, startTime, endTime);
				windowItems = windowRangePair.left;
				IQuantity beforeGc = windowItems.getAggregate(JdkAggregators.SUM_HEAP_USED_BEFORE_GC);
				IQuantity afterGc = windowItems.getAggregate(JdkAggregators.SUM_HEAP_USED_AFTER_GC);
				IQuantity averageLiveset = windowItems.getAggregate(JdkAggregators.AVG_HEAP_USED_AFTER_GC);
				if (beforeGc == null || afterGc == null || averageLiveset == null) {
					return;
				}
				IQuantity totalFreed = beforeGc.subtract(afterGc);
				IRange<IQuantity> range = windowRangePair.right;

				double recordingLengthInSeconds = range.getExtent().in(UnitLookup.SECOND).doubleValue();
				IQuantity freedPerSecond = totalFreed.multiply(1 / recordingLengthInSeconds);
				IQuantity freedPerSecondToLivesetRatio = UnitLookup.PERCENT_UNITY
						.quantity(freedPerSecond.ratioTo(averageLiveset));
				if (freedPerSecondToLivesetRatio.compareTo(maxFreedGcInfo.freedPerSecondToLivesetRatio) > 0) {
					maxFreedGcInfo.freedPerSecondToLivesetRatio = freedPerSecondToLivesetRatio;
					maxFreedGcInfo.freedPerSecond = freedPerSecond;
					maxFreedGcInfo.averageLiveset = averageLiveset;
					maxFreedGcInfo.range = range;
				}
			}

			/**
			 * Fixes the item collection by including the potential orphan 'before' event in the
			 * beginning and 'after' event in the end, and after that removing any non-paired events
			 * in the whole item collection.
			 */
			private Pair<IItemCollection, IRange<IQuantity>> getWindowWithPairedHeapSummaryEvents(
				IItemCollection windowItems, IQuantity startTime, IQuantity endTime) {
				IQuantity newStartTime = null;
				IQuantity newEndTime = null;
				IItemCollection heapSummaryWindowItems = windowItems.apply(JdkFilters.HEAP_SUMMARY);
				IItemCollection heapSummaryAllItems = allItems.apply(JdkFilters.HEAP_SUMMARY);
				IQuantity lowestGcId = heapSummaryWindowItems
						.getAggregate((IAggregator<IQuantity, ?>) Aggregators.min(JdkAttributes.GC_ID));
				IItemCollection lowestGcIdWindowItems = heapSummaryWindowItems
						.apply(ItemFilters.equals(JdkAttributes.GC_ID, lowestGcId));
				IItemCollection lowestGcIdAllItems = heapSummaryAllItems
						.apply(ItemFilters.equals(JdkAttributes.GC_ID, lowestGcId));
				IItemCollection lowestGcIdBeforeWindowItems = lowestGcIdWindowItems
						.apply(JdkFilters.HEAP_SUMMARY_BEFORE_GC);
				IItemCollection lowestGcIdAfterWindowItems = lowestGcIdWindowItems
						.apply(JdkFilters.HEAP_SUMMARY_AFTER_GC);
				IItemCollection lowestGcIdBeforeAllItems = lowestGcIdAllItems.apply(JdkFilters.HEAP_SUMMARY_BEFORE_GC);
				// If the beginning of the window is between a 'before' and an 'after' event.
				if (lowestGcIdAfterWindowItems.hasItems() && !lowestGcIdBeforeWindowItems.hasItems()) {
					if (lowestGcIdBeforeAllItems.hasItems()) {
						newStartTime = RulesToolkit.getEarliestEndTime(lowestGcIdBeforeAllItems);
					}
				}
				IQuantity highestGcId = heapSummaryWindowItems
						.getAggregate((IAggregator<IQuantity, ?>) Aggregators.max(JdkAttributes.GC_ID));
				IItemCollection highestGcIdWindowItems = heapSummaryWindowItems
						.apply(ItemFilters.equals(JdkAttributes.GC_ID, highestGcId));
				IItemCollection highestGcIdAllItems = heapSummaryAllItems
						.apply(ItemFilters.equals(JdkAttributes.GC_ID, highestGcId));
				IItemCollection highestGcIdBeforeWindowItems = highestGcIdWindowItems
						.apply(JdkFilters.HEAP_SUMMARY_BEFORE_GC);
				IItemCollection highestGcIdAfterWindowItems = lowestGcIdWindowItems
						.apply(JdkFilters.HEAP_SUMMARY_AFTER_GC);
				IItemCollection highestGcIdAfterAllItems = highestGcIdAllItems.apply(JdkFilters.HEAP_SUMMARY_BEFORE_GC);
				if (highestGcIdBeforeWindowItems.hasItems() && !highestGcIdAfterWindowItems.hasItems()) {
					if (highestGcIdAfterAllItems.hasItems()) {
						newEndTime = RulesToolkit.getEarliestStartTime(highestGcIdAfterAllItems);
					}
				}

				if (newStartTime != null || newEndTime != null) {
					if (newStartTime != null) {
						startTime = newStartTime;
					}
					if (newEndTime != null) {
						endTime = newEndTime;
					}
					windowItems = allItems
							.apply(ItemFilters.interval(JfrAttributes.END_TIME, startTime, false, endTime, false));
				}

				// Filter out those that don't have matching before/after pairs
				Set<IQuantity> gcIds = windowItems.apply(JdkFilters.HEAP_SUMMARY)
						.getAggregate((IAggregator<Set<IQuantity>, ?>) Aggregators.distinct(JdkAttributes.GC_ID));
				for (Iterator<IQuantity> iterator = gcIds.iterator(); iterator.hasNext();) {
					IQuantity gcId = iterator.next();
					IItemCollection gcItems = windowItems.apply(ItemFilters.equals(JdkAttributes.GC_ID, gcId));
					if (!(gcItems.apply(JdkFilters.AFTER_GC).hasItems()
							&& gcItems.apply(JdkFilters.BEFORE_GC).hasItems())) {
						iterator.remove();
					}
				}
				return new Pair<>(windowItems.apply(ItemFilters.memberOf(JdkAttributes.GC_ID, gcIds)),
						QuantityRange.createWithEnd(startTime, endTime));
			}

			@Override
			public boolean shouldContinue() {
				return !evaluationTask.isCancelled();
			}

		}, allItems, windowSize, slideSize);
		return maxFreedGcInfo;
	}

	private static class GcInfoHolder {
		protected IRange<IQuantity> range;
		protected IQuantity freedPerSecond;
		protected IQuantity averageLiveset;
		protected IQuantity freedPerSecondToLivesetRatio;
	}
}
