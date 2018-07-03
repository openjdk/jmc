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
package org.openjdk.jmc.flightrecorder.rules.jdk.memory;

import static org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability.AVAILABLE;

import java.text.MessageFormat;
import java.util.Set;

import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.common.version.JavaVersion;
import org.openjdk.jmc.common.version.JavaVersionSupport;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.AbstractRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

/**
 * Intent of this rule is to find out if it would be worth enabling string deduplication. String
 * deduplication is available together with the G1 GC, see
 * <a href="http://openjdk.java.net/jeps/192">JEP 192</a>. Rule looks at how much memory is used by
 * the char/byte arrays used internally in strings, it also check the heap usage.
 * <p>
 * The rule can only give guidance, it is up to the user to measure changes memory consumption after
 * enabling string deduplication. It can still be that the amount of memory used to hold metadata of
 * the deduplicated strings is more than the saved heap space.
 * <p>
 * Rule uses the ObjectCount event if available, otherwise the TLAB events. Uncertainties in the
 * calculations comes both from the statistical issues that the AllocationInNewTLAB event provides,
 * and also from the fact that it is hard to know which of the char/byte arrays that actually are
 * the internal string arrays. Looking at the allocation stack trace can help a bit, but allocations
 * can be missed. If using the ObjectCount event, the statistics are better, but it is impossible to
 * know which arrays to count.
 */
public class StringDeduplicationRule extends AbstractRule {

	private static final String NEW_LINE = "\n"; //$NON-NLS-1$
	private static final IItemFilter STRING_FILTER = ItemFilters.equals(JdkAttributes.OBJECT_CLASS_FULLNAME,
			"java.lang.String"); //$NON-NLS-1$

	// FIXME: These are not all possibilities where the final string internal array is created, so can only use this as a heuristic
	// Known frames where arrays which will be the internal value in strings are allocated
	private static final TypedPreference<String> STRING_ARRAY_ALLOCATION_FRAMES = new TypedPreference<>(
			"string.array.allocation.frames", //$NON-NLS-1$
			Messages.getString(Messages.StringDeduplicationRule_STRING_ARRAY_ALLOCATION_FRAMES),
			Messages.getString(Messages.StringDeduplicationRule_STRING_ARRAY_ALLOCATION_FRAMES_DESC),
			UnitLookup.PLAIN_TEXT.getPersister(),
			"java.lang.String.<init>, java.lang.StringBuffer.toString, java.lang.StringBuffer.toString, " //$NON-NLS-1$
					+ "java.lang.StringUTF16.newString, java.lang.StringLatin1.newString, " //$NON-NLS-1$
					+ "java.lang.StringUTF16.toBytes, java.lang.StringBuilder.toBytes"); //$NON-NLS-1$
	private static final TypedPreference<IQuantity> STRING_ARRAY_LIVESET_RATIO_AND_HEAP_USAGE_LIMIT = new TypedPreference<>(
			"string.array.liveset.ratio.and.heap.usage.limit", //$NON-NLS-1$
			Messages.getString(Messages.StringDeduplicationRule_STRING_ARRAY_LIVESET_RATIO_AND_HEAP_USAGE_LIMIT),
			Messages.getString(Messages.StringDeduplicationRule_STRING_ARRAY_LIVESET_RATIO_AND_HEAP_USAGE_LIMIT_DESC),
			UnitLookup.PERCENTAGE, UnitLookup.PERCENT.quantity(50));
	private static final TypedPreference<IQuantity> STRING_ARRAY_ALLOCATION_RATIO_AND_HEAP_USAGE_LIMIT = new TypedPreference<>(
			"string.array.allocation.ratio.and.heap.usage.limit", //$NON-NLS-1$
			Messages.getString(Messages.StringDeduplicationRule_STRING_ARRAY_ALLOCATION_RATIO_AND_HEAP_USAGE_LIMIT),
			Messages.getString(
					Messages.StringDeduplicationRule_STRING_ARRAY_ALLOCATION_RATIO_AND_HEAP_USAGE_LIMIT_DESC),
			UnitLookup.PERCENTAGE, UnitLookup.PERCENT.quantity(50));
	// FIXME: Does it make more sense to have individual liveset/allocation ratio limit and heap usage limits?
	// FIXME: Add a physical memory limit

	public StringDeduplicationRule() {
		super("StringDeduplication", Messages.getString(Messages.StringDeduplicationRule_RULE_NAME), //$NON-NLS-1$
				JfrRuleTopics.HEAP_TOPIC, STRING_ARRAY_LIVESET_RATIO_AND_HEAP_USAGE_LIMIT,
				STRING_ARRAY_ALLOCATION_RATIO_AND_HEAP_USAGE_LIMIT, STRING_ARRAY_ALLOCATION_FRAMES);
	}

	@Override
	protected Result getResult(IItemCollection items, IPreferenceValueProvider vp) {
		JavaVersion javaVersion = RulesToolkit.getJavaVersion(items);
		if (javaVersion == null) {
			return RulesToolkit.getNotApplicableResult(this,
					Messages.getString(Messages.General_TEXT_COULD_NOT_DETERMINE_JAVA_VERSION));
		}

		String stringInternalArrayType = "byte[]"; //$NON-NLS-1$
		IQuantity averageStringSize = UnitLookup.BYTE.quantity(50);

		if (!javaVersion.isGreaterOrEqualThan(JavaVersionSupport.STRING_IS_BYTE_ARRAY)) {
			stringInternalArrayType = "char[]"; //$NON-NLS-1$

			Boolean compactStrings = items.getAggregate(JdkAggregators.COMPACT_STRINGS);
			if (Boolean.FALSE.equals(compactStrings)) {
				averageStringSize = UnitLookup.BYTE.quantity(100);
			}
		}
		IItemFilter stringInternalArrayTypeFilter = ItemFilters.equals(JdkAttributes.OBJECT_CLASS_FULLNAME,
				stringInternalArrayType);

		Boolean useStringDeduplication = items.getAggregate(JdkAggregators.USE_STRING_DEDUPLICATION);
		if (Boolean.TRUE.equals(useStringDeduplication)) {
			return new Result(this, 0,
					Messages.getString(Messages.StringDeduplicationRule_RESULT_USE_STRING_DEDUPLICATION_ENABLED));
		}

		EventAvailability heapSummaryAvailable = RulesToolkit.getEventAvailability(items, JdkTypeIDs.HEAP_SUMMARY);
		if (heapSummaryAvailable != AVAILABLE) {
			return RulesToolkit.getEventAvailabilityResult(this, items, heapSummaryAvailable, JdkTypeIDs.HEAP_SUMMARY);
		}

		EventAvailability objectCountAvail = RulesToolkit.getEventAvailability(items, JdkTypeIDs.OBJECT_COUNT);
		EventAvailability objectCountAfterGcAvail = RulesToolkit.getEventAvailability(items,
				JdkTypeIDs.GC_DETAILED_OBJECT_COUNT_AFTER_GC);
		EventAvailability allocationAvail = RulesToolkit.getEventAvailability(items,
				JdkTypeIDs.ALLOC_INSIDE_TLAB /* ,ALLOC_OUTSIDE_TLAB */);
		if (objectCountAvail != AVAILABLE && objectCountAfterGcAvail != AVAILABLE && allocationAvail != AVAILABLE) {
			return RulesToolkit.getRuleRequiresAtLeastOneEventTypeResult(this, JdkTypeIDs.OBJECT_COUNT,
					JdkTypeIDs.GC_DETAILED_OBJECT_COUNT_AFTER_GC, JdkTypeIDs.ALLOC_INSIDE_TLAB,
					JdkTypeIDs.ALLOC_OUTSIDE_TLAB);
		}
		// FIXME: Add info about rule preferring object count event, and wanting heap conf or flags...

		IQuantity stringLivesetRatioAndHeapUsageLimit = vp
				.getPreferenceValue(STRING_ARRAY_LIVESET_RATIO_AND_HEAP_USAGE_LIMIT);
		IQuantity stringAllocationRatioAndHeapUsageLimit = vp
				.getPreferenceValue(STRING_ARRAY_ALLOCATION_RATIO_AND_HEAP_USAGE_LIMIT);
		String allocationFramesString = vp.getPreferenceValue(STRING_ARRAY_ALLOCATION_FRAMES);

		// Calculate heap usage
		IQuantity maxHeapSizeConf = items.getAggregate(JdkAggregators.HEAP_CONF_MAX_SIZE);
		IQuantity maxHeapSizeFlag = UnitLookup.BYTE
				.quantity(items.getAggregate(JdkAggregators.LARGEST_MAX_HEAP_SIZE_FROM_FLAG).longValue());
		IQuantity maxHeapSize = maxHeapSizeConf != null ? maxHeapSizeConf : maxHeapSizeFlag;

		String heapInfo = MessageFormat.format(
				Messages.getString(Messages.StringDeduplicationRule_RESULT_NO_MAX_HEAP_INFO), JdkTypeIDs.HEAP_CONF,
				JdkTypeIDs.ULONG_FLAG);
		double heapUsedRatio = -1;
		if (maxHeapSize != null) {
			IQuantity avgHeapUsed = items.getAggregate(JdkAggregators.AVG_HEAP_USED_AFTER_GC);
			heapUsedRatio = avgHeapUsed.ratioTo(maxHeapSize) * 100;
			heapInfo = MessageFormat.format(Messages.getString(Messages.StringDeduplicationRule_RESULT_HEAP_USAGE),
					Math.round(heapUsedRatio));
		}

		Boolean useG1GC = items.getAggregate(JdkAggregators.USE_G1_GC);
		String extraCompatInfo = ""; //$NON-NLS-1$
		if (!Boolean.TRUE.equals(useG1GC)) {
			extraCompatInfo += "<p>" + Messages.getString(Messages.StringDeduplicationRule_RESULT_NON_G1_LONG); //$NON-NLS-1$
		}
		if (!javaVersion.isGreaterOrEqualThan(JavaVersionSupport.STRING_DEDUPLICATION_SUPPORTED)) {
			extraCompatInfo += "<p>" + Messages.getString(Messages.StringDeduplicationRule_RESULT_PRE_8_20); //$NON-NLS-1$
		}

		// Calculate string internal array ratios depending on available event types
		if (objectCountAvail == AVAILABLE || objectCountAfterGcAvail == AVAILABLE) {
			String objectCountEventType = (objectCountAvail == AVAILABLE) ? JdkTypeIDs.OBJECT_COUNT
					: JdkTypeIDs.GC_DETAILED_OBJECT_COUNT_AFTER_GC;

			return getLivesetRatioResult(items, stringInternalArrayType, stringInternalArrayTypeFilter,
					averageStringSize, stringLivesetRatioAndHeapUsageLimit, objectCountEventType, heapInfo,
					heapUsedRatio, extraCompatInfo);
		} else {
			return getAllocationRatioResult(items, stringInternalArrayType, stringInternalArrayTypeFilter,
					stringAllocationRatioAndHeapUsageLimit, allocationFramesString, heapInfo, heapUsedRatio,
					extraCompatInfo);
		}
		// TODO: Check free physical memory?
	}

	private Result getLivesetRatioResult(
		IItemCollection items, String stringInternalArrayType, IItemFilter stringInternalArrayTypeFilter,
		IQuantity averageStringSize, IQuantity stringLivesetRatioAndHeapUsageLimit, String objectCountEventType,
		String heapInfo, double heapUsedRatio, String extraGcInfo) {

		IItemCollection objectCountItems = items.apply(ItemFilters.type(objectCountEventType));

		double stringMaxRatio = 0;

		// Check the string internal array ratio for each set of ObjectCount events = each gc.
		Set<IQuantity> gcIds = objectCountItems.getAggregate(Aggregators.distinct(JdkAttributes.GC_ID));
		for (IQuantity gcId : gcIds) {
			IItemCollection livesetForGc = objectCountItems.apply(ItemFilters.equals(JdkAttributes.GC_ID, gcId));
			IItemCollection stringObjectCountItems = livesetForGc.apply(STRING_FILTER);
			IQuantity stringCount = stringObjectCountItems.getAggregate(Aggregators.sum(JdkAttributes.COUNT));
			if (stringCount != null && stringCount.longValue() > 0) {
				IItemCollection stringInternalArrayObjectCountItems = livesetForGc.apply(stringInternalArrayTypeFilter);

				// FIXME: Compare to total liveset, committed heap or max heap?
				IQuantity totalLivesetSize = livesetForGc.getAggregate(Aggregators.sum(JdkAttributes.HEAP_TOTAL));
				IQuantity arraySize = stringInternalArrayObjectCountItems
						.getAggregate(Aggregators.sum(JdkAttributes.HEAP_TOTAL));

				IQuantity stringInternalArrayCount = stringInternalArrayObjectCountItems
						.getAggregate(Aggregators.sum(JdkAttributes.COUNT));
				if (stringCount.compareTo(stringInternalArrayCount) == 0) {
					// All arrays likely belong to strings
					IQuantity stringInternalArraySize = arraySize;
					double newRatio = stringInternalArraySize.ratioTo(totalLivesetSize) * 100;
					if (newRatio > stringMaxRatio) {
						stringMaxRatio = newRatio;
					}
				} else {
					// Only part of the arrays belong to strings, this value is likely to be too large, especially for byte arrays.
					// FIXME: Which average size estimation method to use?
//					IQuantity averageArraySize = arraySize.multiply(stringCount.ratioTo(stringInternalArrayCount));
					IQuantity averageStringInternalArraySize = averageStringSize.multiply(stringCount.longValue());
					double newMaxRatio = averageStringInternalArraySize.ratioTo(totalLivesetSize) * 100;
					if (newMaxRatio > stringMaxRatio) {
						stringMaxRatio = newMaxRatio;
					}
				}
			}
		}
		String description = MessageFormat.format(
				Messages.getString(Messages.StringDeduplicationRule_RESULT_STRING_ARRAY_LIVESET_RATIO),
				Math.round(stringMaxRatio), stringInternalArrayType) + NEW_LINE + heapInfo;
		double scoreBase = stringMaxRatio + (stringMaxRatio * heapUsedRatio / 100);
		double score = RulesToolkit.mapExp74(scoreBase, stringLivesetRatioAndHeapUsageLimit.doubleValue());

		String recommendation;
		if (stringMaxRatio > stringLivesetRatioAndHeapUsageLimit.doubleValue()) {
			recommendation = Messages.getString(Messages.StringDeduplicationRule_RESULT_RECOMMEND_STRING_DEDUPLICATION);
		} else {
			recommendation = Messages
					.getString(Messages.StringDeduplicationRule_RESULT_DONT_RECOMMEND_STRING_DEDUPLICATION);
		}

		String shortMessage = description + " " + recommendation; //$NON-NLS-1$
		String longMessage = shortMessage + "<p>" //$NON-NLS-1$
				+ Messages.getString(Messages.StringDeduplicationRule_RESULT_LONG_DESCRIPTION) + extraGcInfo;
		return new Result(this, score, shortMessage, longMessage);
	}

	private Result getAllocationRatioResult(
		IItemCollection items, String stringInternalArrayType, IItemFilter stringInternalArrayTypeFilter,
		IQuantity stringAllocationRatioLimit, String allocationFramesString, String heapInfo, double heapUsedRatio,
		String extraGcInfo) {
		// TODO: Calculate in time windows?

		// Find the char/byte array allocations coming from string creation, compare to total allocation
		IItemCollection allocItems = items.apply(JdkFilters.ALLOC_ALL);
		IQuantity totalSize = allocItems.getAggregate(JdkAggregators.ALLOCATION_TOTAL);

		IItemCollection arrayAllocItems = allocItems.apply(stringInternalArrayTypeFilter);
		// FIXME: Check if there were any allocations

		IItemFilter allocationFrameFilter = getAllocationFramesFilter(allocationFramesString);
		// TODO: Improve rule performance by using StacktraceModel instead of filtering each individual item?
		IItemCollection stringInternalArrayAllocItems = arrayAllocItems.apply(allocationFrameFilter);
		// FIXME: This is a min value, can we calculate a higher value, which is not all the char/byte arrays?
		IQuantity stringInternalArraySizeBasedOnStacktrace = stringInternalArrayAllocItems
				.getAggregate(JdkAggregators.ALLOCATION_TOTAL);
		if (stringInternalArraySizeBasedOnStacktrace == null) {
			// FIXME: Check if the stacktrace attribute is enabled
			return new Result(this, Result.NOT_APPLICABLE,
					Messages.getString(Messages.StringDeduplicationRule_RESULT_NO_ALLOC_ITEMS));
		}
		double stringAllocationRatioBasedOnStacktrace = stringInternalArraySizeBasedOnStacktrace.ratioTo(totalSize)
				* 100;
		double scoreBase = stringAllocationRatioBasedOnStacktrace
				+ (stringAllocationRatioBasedOnStacktrace * heapUsedRatio / 100);
		double score = RulesToolkit.mapExp74(scoreBase, stringAllocationRatioLimit.doubleValue());
		String description = MessageFormat.format(
				Messages.getString(Messages.StringDeduplicationRule_RESULT_STRING_ARRAY_ALLOCATION_RATIO),
				Math.round(stringAllocationRatioBasedOnStacktrace), stringInternalArrayType) + NEW_LINE + heapInfo;

		String recommendation;
		if (stringAllocationRatioBasedOnStacktrace > stringAllocationRatioLimit.doubleValue()) {
			recommendation = Messages.getString(Messages.StringDeduplicationRule_RESULT_RECOMMEND_STRING_DEDUPLICATION);
		} else {
			recommendation = Messages
					.getString(Messages.StringDeduplicationRule_RESULT_DONT_RECOMMEND_STRING_DEDUPLICATION);
		}

		String shortMessage = description + " " + recommendation; //$NON-NLS-1$
		String longMessage = shortMessage + "<p>" //$NON-NLS-1$
				+ Messages.getString(Messages.StringDeduplicationRule_RESULT_LONG_DESCRIPTION) + extraGcInfo;
		return new Result(this, score, shortMessage, longMessage);
	}

	private IItemFilter getAllocationFramesFilter(String allocationFramesString) {
		if (allocationFramesString.replace(',', ' ').trim().isEmpty()) {
			return ItemFilters.none();
		}
		String[] allocationFrames = allocationFramesString.split(","); //$NON-NLS-1$
		IItemFilter[] frameFilters = new IItemFilter[allocationFrames.length];
		for (int i = 0; i < frameFilters.length; i++) {
			frameFilters[i] = ItemFilters.contains(JdkAttributes.STACK_TRACE_STRING, allocationFrames[i].trim());
		}
		// FIXME: Return something else if there are no frames
		return ItemFilters.or(frameFilters);
	}
}
