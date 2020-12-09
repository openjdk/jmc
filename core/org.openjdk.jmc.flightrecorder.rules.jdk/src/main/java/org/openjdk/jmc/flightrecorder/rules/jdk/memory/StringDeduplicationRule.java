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

import static org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability.AVAILABLE;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAggregator;
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

/**
 * Intent of this rule is to find out if it would be worth enabling string deduplication. String
 * deduplication is available together with the G1 GC, see
 * <a href="https://openjdk.java.net/jeps/192">JEP 192</a>. Rule looks at how much memory is used by
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

	private static final Collection<TypedPreference<?>> CONFIGURATION_ATTRIBUTES = Arrays.<TypedPreference<?>> asList(
			STRING_ARRAY_ALLOCATION_FRAMES, STRING_ARRAY_ALLOCATION_RATIO_AND_HEAP_USAGE_LIMIT,
			STRING_ARRAY_LIVESET_RATIO_AND_HEAP_USAGE_LIMIT);

	public static final TypedResult<IQuantity> HEAP_USAGE = new TypedResult<>("heapUsage", "Heap Usage Ratio", //$NON-NLS-1$
			"The percentage of the heap used.", UnitLookup.PERCENTAGE, IQuantity.class);
	public static final TypedResult<IQuantity> STRING_HEAP_RATIO = new TypedResult<>("stringHeapRatio", "String Usage", //$NON-NLS-1$
			"The percent of the heap used for String objects.", UnitLookup.PERCENTAGE, IQuantity.class);
	public static final TypedResult<String> INTERNAL_STRING_TYPE = new TypedResult<>("stringType", //$NON-NLS-1$
			"Internal String Type", "The internal type used to represent Strings.", UnitLookup.PLAIN_TEXT,
			String.class);

	private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays
			.<TypedResult<?>> asList(TypedResult.SCORE, HEAP_USAGE, STRING_HEAP_RATIO, INTERNAL_STRING_TYPE);

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.VM_INFO, EventAvailability.AVAILABLE)
			.addEventType(JdkTypeIDs.ALLOC_INSIDE_TLAB, EventAvailability.ENABLED)
			.addEventType(JdkTypeIDs.OBJECT_COUNT, EventAvailability.ENABLED)
			.addEventType(JdkTypeIDs.GC_DETAILED_OBJECT_COUNT_AFTER_GC, EventAvailability.ENABLED)
			.addEventType(JdkTypeIDs.ALLOC_OUTSIDE_TLAB, EventAvailability.ENABLED)
			.addEventType(JdkTypeIDs.HEAP_SUMMARY, EventAvailability.AVAILABLE).build();

	public StringDeduplicationRule() {
		super("StringDeduplication", Messages.getString(Messages.StringDeduplicationRule_RULE_NAME), //$NON-NLS-1$
				JfrRuleTopics.HEAP, CONFIGURATION_ATTRIBUTES, RESULT_ATTRIBUTES, REQUIRED_EVENTS);
	}

	@Override
	protected IResult getResult(IItemCollection items, IPreferenceValueProvider vp, IResultValueProvider rp) {
		JavaVersion javaVersion = RulesToolkit.getJavaVersion(items);
		if (javaVersion == null) {
			return ResultBuilder.createFor(this, vp).setSeverity(Severity.NA)
					.setSummary(Messages.getString(Messages.General_TEXT_COULD_NOT_DETERMINE_JAVA_VERSION)).build();
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
			return ResultBuilder.createFor(this, vp).setSeverity(Severity.OK)
					.setSummary(Messages
							.getString(Messages.StringDeduplicationRule_RESULT_USE_STRING_DEDUPLICATION_ENABLED))
					.build();
		}

		EventAvailability objectCountAvail = RulesToolkit.getEventAvailability(items, JdkTypeIDs.OBJECT_COUNT);
		EventAvailability objectCountAfterGcAvail = RulesToolkit.getEventAvailability(items,
				JdkTypeIDs.GC_DETAILED_OBJECT_COUNT_AFTER_GC);
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
		IQuantity heapUsedRatio = null;
		if (maxHeapSize != null) {
			IQuantity avgHeapUsed = items.getAggregate(JdkAggregators.AVG_HEAP_USED_AFTER_GC);
			heapUsedRatio = UnitLookup.PERCENT_UNITY.quantity(avgHeapUsed.ratioTo(maxHeapSize));
			heapInfo = Messages.getString(Messages.StringDeduplicationRule_RESULT_HEAP_USAGE);
		}

		Boolean useG1GC = items.getAggregate(JdkAggregators.USE_G1_GC);
		Boolean useShenandoahGC = items.getAggregate(JdkAggregators.USE_SHENANDOAH_GC);
		String extraCompatInfo = ""; //$NON-NLS-1$
		if (!(Boolean.TRUE.equals(useG1GC)) && !(Boolean.TRUE.equals(useShenandoahGC))) {
			extraCompatInfo += "<p>" + Messages.getString(Messages.StringDeduplicationRule_RESULT_GC_LONG); //$NON-NLS-1$
		}

		// Calculate string internal array ratios depending on available event types
		if (objectCountAvail == AVAILABLE || objectCountAfterGcAvail == AVAILABLE) {
			String objectCountEventType = (objectCountAvail == AVAILABLE) ? JdkTypeIDs.OBJECT_COUNT
					: JdkTypeIDs.GC_DETAILED_OBJECT_COUNT_AFTER_GC;

			return getLiveSetRatioResult(items, stringInternalArrayType, stringInternalArrayTypeFilter,
					averageStringSize, stringLivesetRatioAndHeapUsageLimit, objectCountEventType, heapInfo,
					heapUsedRatio, extraCompatInfo, vp);
		} else {
			return getAllocationRatioResult(items, stringInternalArrayType, stringInternalArrayTypeFilter,
					stringAllocationRatioAndHeapUsageLimit, allocationFramesString, heapInfo, heapUsedRatio,
					extraCompatInfo, vp);
		}
		// TODO: Check free physical memory?
	}

	private IResult getLiveSetRatioResult(
		IItemCollection items, String stringInternalArrayType, IItemFilter stringInternalArrayTypeFilter,
		IQuantity averageStringSize, IQuantity stringLivesetRatioAndHeapUsageLimit, String objectCountEventType,
		String heapInfo, IQuantity heapUsedRatio, String extraGcInfo, IPreferenceValueProvider vp) {

		IItemCollection objectCountItems = items.apply(ItemFilters.type(objectCountEventType));

		double stringMaxRatio = 0;

		// Check the string internal array ratio for each set of ObjectCount events = each gc.
		Set<IQuantity> gcIds = objectCountItems
				.getAggregate((IAggregator<Set<IQuantity>, ?>) Aggregators.distinct(JdkAttributes.GC_ID));
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
		String description = Messages.getString(Messages.StringDeduplicationRule_RESULT_STRING_ARRAY_LIVESET_RATIO)
				+ NEW_LINE + heapInfo;
		double scoreBase = stringMaxRatio + (stringMaxRatio * heapUsedRatio.longValue());
		double score = RulesToolkit.mapExp74(scoreBase, stringLivesetRatioAndHeapUsageLimit.doubleValue());

		String recommendation;
		if (stringMaxRatio > stringLivesetRatioAndHeapUsageLimit.doubleValue()) {
			recommendation = Messages.getString(Messages.StringDeduplicationRule_RESULT_RECOMMEND_STRING_DEDUPLICATION);
		} else {
			recommendation = Messages
					.getString(Messages.StringDeduplicationRule_RESULT_DONT_RECOMMEND_STRING_DEDUPLICATION);
		}

		String shortMessage = description + " " + recommendation; //$NON-NLS-1$
		String longMessage = Messages.getString(Messages.StringDeduplicationRule_RESULT_LONG_DESCRIPTION) + extraGcInfo;
		return ResultBuilder.createFor(this, vp).setSeverity(Severity.get(score)).setSummary(shortMessage)
				.setExplanation(longMessage).addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(score))
				.addResult(HEAP_USAGE, heapUsedRatio)
				.addResult(STRING_HEAP_RATIO, UnitLookup.PERCENT_UNITY.quantity(stringMaxRatio))
				.addResult(INTERNAL_STRING_TYPE, stringInternalArrayType).build();
	}

	private IResult getAllocationRatioResult(
		IItemCollection items, String stringInternalArrayType, IItemFilter stringInternalArrayTypeFilter,
		IQuantity stringAllocationRatioLimit, String allocationFramesString, String heapInfo, IQuantity heapUsedRatio,
		String extraGcInfo, IPreferenceValueProvider vp) {
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
			return ResultBuilder.createFor(this, vp).setSeverity(Severity.NA)
					.setSummary(Messages.getString(Messages.StringDeduplicationRule_RESULT_NO_ALLOC_ITEMS)).build();
			// FIXME: Check if the stacktrace attribute is enabled
		}
		double stringAllocationRatioBasedOnStacktrace = stringInternalArraySizeBasedOnStacktrace.ratioTo(totalSize);
		double scoreBase = stringAllocationRatioBasedOnStacktrace
				+ (stringAllocationRatioBasedOnStacktrace * heapUsedRatio.longValue());
		double score = RulesToolkit.mapExp74(scoreBase, stringAllocationRatioLimit.doubleValue());
		String description = Messages.getString(Messages.StringDeduplicationRule_RESULT_STRING_ARRAY_ALLOCATION_RATIO)
				+ NEW_LINE + heapInfo;

		String recommendation;
		if (stringAllocationRatioBasedOnStacktrace > stringAllocationRatioLimit.doubleValue()) {
			recommendation = Messages.getString(Messages.StringDeduplicationRule_RESULT_RECOMMEND_STRING_DEDUPLICATION);
		} else {
			recommendation = Messages
					.getString(Messages.StringDeduplicationRule_RESULT_DONT_RECOMMEND_STRING_DEDUPLICATION);
		}

		String shortMessage = description + " " + recommendation; //$NON-NLS-1$
		String longMessage = Messages.getString(Messages.StringDeduplicationRule_RESULT_LONG_DESCRIPTION) + extraGcInfo;
		return ResultBuilder.createFor(this, vp).setSeverity(Severity.get(score)).setSummary(shortMessage)
				.setExplanation(longMessage).addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(score))
				.addResult(HEAP_USAGE, heapUsedRatio)
				.addResult(STRING_HEAP_RATIO, UnitLookup.PERCENT_UNITY.quantity(stringAllocationRatioBasedOnStacktrace))
				.addResult(INTERNAL_STRING_TYPE, stringInternalArrayType).build();
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
