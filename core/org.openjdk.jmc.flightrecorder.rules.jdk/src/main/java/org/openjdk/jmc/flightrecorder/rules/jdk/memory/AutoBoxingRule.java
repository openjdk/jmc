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

import java.text.MessageFormat;
import java.util.Set;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
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
 * Rule that checks how much of the total allocation is caused by possible primitive to object
 * conversion. Looks at top frames and searches for java.lang.BoxType.valueOf.
 */
// FIXME: Rename class (and message constants) from autoboxing to something more generic?
public class AutoBoxingRule extends AbstractRule {

	private static final IItemFilter BOXED_TYPES_FILTER = ItemFilters.or(
			ItemFilters.contains(JdkAttributes.OBJECT_CLASS_FULLNAME, "java.lang.Boolean"), //$NON-NLS-1$
			ItemFilters.contains(JdkAttributes.OBJECT_CLASS_FULLNAME, "java.lang.Byte"), //$NON-NLS-1$
			ItemFilters.contains(JdkAttributes.OBJECT_CLASS_FULLNAME, "java.lang.Character"), //$NON-NLS-1$
			ItemFilters.contains(JdkAttributes.OBJECT_CLASS_FULLNAME, "java.lang.Double"), //$NON-NLS-1$
			ItemFilters.contains(JdkAttributes.OBJECT_CLASS_FULLNAME, "java.lang.Float"), //$NON-NLS-1$
			ItemFilters.contains(JdkAttributes.OBJECT_CLASS_FULLNAME, "java.lang.Integer"), //$NON-NLS-1$
			ItemFilters.contains(JdkAttributes.OBJECT_CLASS_FULLNAME, "java.lang.Long"), //$NON-NLS-1$
			ItemFilters.contains(JdkAttributes.OBJECT_CLASS_FULLNAME, "java.lang.Short")); //$NON-NLS-1$
	private static final IItemFilter AUTOBOXING_TOP_FRAME_FILTER = ItemFilters.or(
			ItemFilters.contains(JdkAttributes.STACK_TRACE_TOP_METHOD_STRING, "java.lang.Boolean.valueOf"), //$NON-NLS-1$
			ItemFilters.contains(JdkAttributes.STACK_TRACE_TOP_METHOD_STRING, "java.lang.Byte.valueOf"), //$NON-NLS-1$
			ItemFilters.contains(JdkAttributes.STACK_TRACE_TOP_METHOD_STRING, "java.lang.Character.valueOf"), //$NON-NLS-1$
			ItemFilters.contains(JdkAttributes.STACK_TRACE_TOP_METHOD_STRING, "java.lang.Double.valueOf"), //$NON-NLS-1$
			ItemFilters.contains(JdkAttributes.STACK_TRACE_TOP_METHOD_STRING, "java.lang.Float.valueOf"), //$NON-NLS-1$
			ItemFilters.contains(JdkAttributes.STACK_TRACE_TOP_METHOD_STRING, "java.lang.Integer.valueOf"), //$NON-NLS-1$
			ItemFilters.contains(JdkAttributes.STACK_TRACE_TOP_METHOD_STRING, "java.lang.Long.valueOf"), //$NON-NLS-1$
			ItemFilters.contains(JdkAttributes.STACK_TRACE_TOP_METHOD_STRING, "java.lang.Short.valueOf")); //$NON-NLS-1$

	private static final TypedPreference<IQuantity> AUTOBOXING_RATIO_INFO_LIMIT = new TypedPreference<>(
			"autoboxing.ratio.info.limit", //$NON-NLS-1$
			Messages.getString(Messages.AutoboxingRule_AUTOBOXING_RATIO_INFO_LIMIT),
			Messages.getString(Messages.AutoboxingRule_AUTOBOXING_RATIO_INFO_LIMIT_DESC), UnitLookup.PERCENTAGE,
			UnitLookup.PERCENT.quantity(20));
	private static final TypedPreference<IQuantity> AUTOBOXING_RATIO_WARNING_LIMIT = new TypedPreference<>(
			"autoboxing.ratio.warning.limit", //$NON-NLS-1$
			Messages.getString(Messages.AutoboxingRule_AUTOBOXING_RATIO_WARNING_LIMIT),
			Messages.getString(Messages.AutoboxingRule_AUTOBOXING_RATIO_WARNING_LIMIT_DESC), UnitLookup.PERCENTAGE,
			UnitLookup.PERCENT.quantity(80));

	public AutoBoxingRule() {
		super("PrimitiveToObjectConversion", Messages.getString(Messages.AutoboxingRule_RULE_NAME), //$NON-NLS-1$
				JfrRuleTopics.HEAP_TOPIC, AUTOBOXING_RATIO_INFO_LIMIT, AUTOBOXING_RATIO_WARNING_LIMIT);
	}

	@Override
	protected Result getResult(IItemCollection items, IPreferenceValueProvider vp) {
		EventAvailability allocAvail = RulesToolkit.getEventAvailability(items, JdkTypeIDs.ALLOC_INSIDE_TLAB,
				JdkTypeIDs.ALLOC_OUTSIDE_TLAB);
		if (allocAvail != EventAvailability.AVAILABLE) {
			return RulesToolkit.getEventAvailabilityResult(this, items, allocAvail, JdkTypeIDs.ALLOC_INSIDE_TLAB,
					JdkTypeIDs.ALLOC_OUTSIDE_TLAB);
		}

		double autoboxingRatioInfoLimit = vp.getPreferenceValue(AUTOBOXING_RATIO_INFO_LIMIT).doubleValue();
		double autoboxingRatioWarningLimit = vp.getPreferenceValue(AUTOBOXING_RATIO_WARNING_LIMIT).doubleValue();

		// FIXME: Should add a check for allocation pressure later, but keeping the rule very simplistic as a first step.

		IItemCollection allocationItems = items.apply(JdkFilters.ALLOC_ALL);
		// FIXME: Consider using StacktraceModel, since this only looks at top frame it might be cheaper?
		// However, we need to be able to convert between individual items and IItemCollection
		IItemCollection boxedTypesItems = allocationItems.apply(BOXED_TYPES_FILTER);
		IItemCollection autoboxingStacktraceItems = boxedTypesItems.apply(AUTOBOXING_TOP_FRAME_FILTER);

		IQuantity possibleAutoboxingSize = autoboxingStacktraceItems.getAggregate(JdkAggregators.ALLOCATION_TOTAL);
		if (possibleAutoboxingSize == null) {
			return new Result(this, 0, Messages.getString(Messages.AutoboxingRule_RESULT_NO_AUTOBOXING));
		}
		IQuantity totalAllocationSize = allocationItems.getAggregate(JdkAggregators.ALLOCATION_TOTAL);
		double possibleAutoboxingRatio = possibleAutoboxingSize.ratioTo(totalAllocationSize) * 100;

		double score = RulesToolkit.mapExp100(possibleAutoboxingRatio, autoboxingRatioInfoLimit,
				autoboxingRatioWarningLimit);

		// Compute information about top autoboxing type
		String mostAllocatedTypeInfo = ""; //$NON-NLS-1$
		String mostAllocatedTypeInfoLong = ""; //$NON-NLS-1$
		// FIXME: Would like an aggregator that calculates all this at once, should be possible to use the GroupingAggregator with the ALLOCATION_TOTAL aggregator
		Set<String> autoboxedTypes = autoboxingStacktraceItems
				.getAggregate(Aggregators.distinct(JdkAttributes.OBJECT_CLASS_FULLNAME));
		String mostAllocatedType = null;
		IItemCollection mostAutoboxedTypeItems = null;
		IQuantity mostAllocated = UnitLookup.BYTE.quantity(0);
		for (String type : autoboxedTypes) {
			IItemCollection autoboxedTypeItems = autoboxingStacktraceItems
					.apply(ItemFilters.equals(JdkAttributes.OBJECT_CLASS_FULLNAME, type));
			IQuantity allocated = autoboxedTypeItems.getAggregate(JdkAggregators.ALLOCATION_TOTAL);
			if (allocated.compareTo(mostAllocated) > 0) {
				mostAllocatedType = type;
				mostAllocated = allocated;
				mostAutoboxedTypeItems = autoboxedTypeItems;
			}
		}
		if (mostAllocatedType != null) {
			String secondFrame = RulesToolkit.getSecondFrameInMostCommonTrace(mostAutoboxedTypeItems);
			mostAllocatedTypeInfo = " " + MessageFormat //$NON-NLS-1$
					.format(Messages.getString(Messages.AutoboxingRule_RESULT_MOST_AUTOBOXED_TYPE), mostAllocatedType);
			mostAllocatedTypeInfoLong = "<p>" //$NON-NLS-1$
					+ MessageFormat.format(Messages.getString(Messages.AutoboxingRule_RESULT_MOST_AUTOBOXED_TYPE_LONG),
							mostAllocatedType, mostAllocated.displayUsing(IDisplayable.AUTO), secondFrame);
		}

		String shortIntro = MessageFormat.format(Messages.getString(Messages.AutoboxingRule_RESULT_AUTOBOXING_RATIO),
				Math.round(possibleAutoboxingRatio), possibleAutoboxingSize.displayUsing(IDisplayable.AUTO));
		String shortMessage = shortIntro + mostAllocatedTypeInfo;
		String longMessage = shortIntro + mostAllocatedTypeInfoLong + "<p>" //$NON-NLS-1$
				+ Messages.getString(Messages.AutoboxingRule_RESULT_LONG);
		return new Result(this, score, shortMessage, longMessage);
	}
}
