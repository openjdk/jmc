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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.collection.SimpleArray;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
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
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceFrame;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel.Branch;

/**
 * Rule that checks how much of the total allocation is caused by possible primitive to object
 * conversion. Looks at top frames and searches for java.lang.BoxType.valueOf.
 */
// FIXME: Rename class (and message constants) from autoboxing to something more generic?
public class AutoBoxingRule extends AbstractRule {

	private static final String VALUE_OF_METHOD_NAME = "valueOf"; //$NON-NLS-1$
	private static final String SHORT = "java.lang.Short"; //$NON-NLS-1$
	private static final String LONG = "java.lang.Long"; //$NON-NLS-1$
	private static final String INTEGER = "java.lang.Integer"; //$NON-NLS-1$
	private static final String FLOAT = "java.lang.Float"; //$NON-NLS-1$
	private static final String DOUBLE = "java.lang.Double"; //$NON-NLS-1$
	private static final String CHARACTER = "java.lang.Character"; //$NON-NLS-1$
	private static final String BYTE = "java.lang.Byte"; //$NON-NLS-1$
	private static final String BOOLEAN = "java.lang.Boolean"; //$NON-NLS-1$

	private static final Predicate<IMCMethod> IS_AUTOBOXED_PREDICATE = new Predicate<IMCMethod>() {
		@Override
		public boolean test(IMCMethod method) {
			String type = method.getType().getFullName();
			if (VALUE_OF_METHOD_NAME.equals(method.getMethodName())) {
				if (BYTE.equals(type)) {
					return true;
				} else if (CHARACTER.equals(type)) {
					return true;
				} else if (DOUBLE.equals(type)) {
					return true;
				} else if (FLOAT.equals(type)) {
					return true;
				} else if (INTEGER.equals(type)) {
					return true;
				} else if (LONG.equals(type)) {
					return true;
				} else if (SHORT.equals(type)) {
					return true;
				} else if (BOOLEAN.equals(type)) {
					return true;
				}
			}
			return false;
		}
	};

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

	private static final Collection<TypedPreference<?>> CONFIGURATION_ATTRIBUTES = Arrays
			.<TypedPreference<?>> asList(AUTOBOXING_RATIO_INFO_LIMIT, AUTOBOXING_RATIO_WARNING_LIMIT);

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.ALLOC_INSIDE_TLAB, EventAvailability.ENABLED)
			.addEventType(JdkTypeIDs.ALLOC_OUTSIDE_TLAB, EventAvailability.AVAILABLE).build();

	public static final TypedResult<IMCType> LARGEST_ALLOCATED_TYPE = new TypedResult<>("largestAllocatedType", //$NON-NLS-1$
			"Largest Allocated Type", "The type allocated the most.", UnitLookup.CLASS, IMCType.class);
	public static final TypedResult<IMCFrame> SECOND_FRAME_MOST_ALLOCATED = new TypedResult<>(
			"secondFrameMostAllocated", "Most Common Call Site", "The most common frame calling into a boxing method.", //$NON-NLS-1$
			UnitLookup.STACKTRACE_FRAME, IMCFrame.class);
	public static final TypedResult<IQuantity> BOXED_ALLOCATION_SIZE = new TypedResult<>("boxedAllocationSize", //$NON-NLS-1$
			"Boxed Allocation Size", "The size of all allocations caused by boxing.", UnitLookup.MEMORY,
			IQuantity.class);
	public static final TypedResult<IQuantity> LARGEST_ALLOCATED_BY_TYPE = new TypedResult<>("largestAllocatedByType", //$NON-NLS-1$
			"Allocation by Type", "The amount allocated by boxing the most boxed type.", UnitLookup.MEMORY,
			IQuantity.class);
	public static final TypedResult<IQuantity> BOXED_ALLOCATION_RATIO = new TypedResult<>("boxedAllocationRatio", //$NON-NLS-1$
			"Boxed Allocation Ratio", "The percentage of all allocations caused by boxing.", UnitLookup.PERCENTAGE,
			IQuantity.class);

	private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays.<TypedResult<?>> asList(
			TypedResult.SCORE, LARGEST_ALLOCATED_TYPE, LARGEST_ALLOCATED_BY_TYPE, SECOND_FRAME_MOST_ALLOCATED,
			BOXED_ALLOCATION_SIZE, BOXED_ALLOCATION_RATIO);

	public AutoBoxingRule() {
		super("PrimitiveToObjectConversion", Messages.getString(Messages.AutoboxingRule_RULE_NAME), //$NON-NLS-1$
				JfrRuleTopics.HEAP, CONFIGURATION_ATTRIBUTES, RESULT_ATTRIBUTES, REQUIRED_EVENTS);
	}

	@Override
	protected IResult getResult(IItemCollection items, IPreferenceValueProvider vp, IResultValueProvider rp) {
		double autoboxingRatioInfoLimit = vp.getPreferenceValue(AUTOBOXING_RATIO_INFO_LIMIT).doubleValue();
		double autoboxingRatioWarningLimit = vp.getPreferenceValue(AUTOBOXING_RATIO_WARNING_LIMIT).doubleValue();

		// FIXME: Should add a dependency on a rule checking allocation pressure later, but keeping the rule very simplistic as a first step.
		IItemCollection allocationItems = items.apply(JdkFilters.ALLOC_ALL);
		FrameSeparator sep = new FrameSeparator(FrameSeparator.FrameCategorization.LINE, false);
		StacktraceModel model = new StacktraceModel(false, sep, allocationItems);
		Map<IMCType, IQuantity> allocationSizeByType = new HashMap<>();
		IQuantity sizeOfAllBoxedAllocations = UnitLookup.BYTE.quantity(0);
		IQuantity largestAllocatedByType = UnitLookup.BYTE.quantity(0);
		IMCType largestAllocatedType = null;
		IMCFrame secondFrameFromMostAllocated = null;
		for (StacktraceFrame stacktraceFrame : model.getRootFork().getFirstFrames()) {
			IMCMethod method = stacktraceFrame.getFrame().getMethod();
			if (IS_AUTOBOXED_PREDICATE.test(method)) {
				SimpleArray<IItem> itemArray = stacktraceFrame.getItems();
				IQuantity total = UnitLookup.BYTE.quantity(0);
				for (IItem item : itemArray) {
					total = total.add(RulesToolkit.getValue(item, JdkAttributes.TOTAL_ALLOCATION_SIZE));
				}
				sizeOfAllBoxedAllocations = sizeOfAllBoxedAllocations.add(total);
				if (total.compareTo(largestAllocatedByType) > 0) {
					largestAllocatedByType = total;
					largestAllocatedType = method.getType();
					StacktraceFrame secondFrame = null;
					Branch firstBranch = stacktraceFrame.getBranch();
					if (firstBranch.getTailFrames().length > 0) {
						secondFrame = firstBranch.getTailFrames()[0];
					} else if (firstBranch.getEndFork().getBranchCount() > 0) {
						secondFrame = firstBranch.getEndFork().getBranch(0).getFirstFrame();
					}
					secondFrameFromMostAllocated = secondFrame.getFrame();
				}
				allocationSizeByType.put(method.getType(), total);
			}
		}
		if (allocationSizeByType.size() == 0) {
			return ResultBuilder.createFor(this, vp).setSeverity(Severity.OK)
					.setSummary(Messages.getString(Messages.AutoboxingRule_RESULT_NO_AUTOBOXING)).build();
		}
		IQuantity totalAllocationSize = allocationItems.getAggregate(JdkAggregators.ALLOCATION_TOTAL);
		double possibleAutoboxingRatio = sizeOfAllBoxedAllocations.ratioTo(totalAllocationSize);

		double score = RulesToolkit.mapExp100(possibleAutoboxingRatio * 100, autoboxingRatioInfoLimit,
				autoboxingRatioWarningLimit);

		// Compute information about top autoboxing type
		String mostAllocatedTypeInfo = ""; //$NON-NLS-1$
		String mostAllocatedTypeInfoLong = ""; //$NON-NLS-1$
		if (largestAllocatedType != null) {
			mostAllocatedTypeInfo = " " + Messages.getString(Messages.AutoboxingRule_RESULT_MOST_AUTOBOXED_TYPE); //$NON-NLS-1$
			mostAllocatedTypeInfoLong = "\n" //$NON-NLS-1$
					+ Messages.getString(Messages.AutoboxingRule_RESULT_MOST_AUTOBOXED_TYPE_LONG);
		}

		String shortIntro = Messages.getString(Messages.AutoboxingRule_RESULT_AUTOBOXING_RATIO);
		String shortMessage = shortIntro + mostAllocatedTypeInfo;
		String longMessage = mostAllocatedTypeInfoLong + "\n" //$NON-NLS-1$
				+ Messages.getString(Messages.AutoboxingRule_RESULT_LONG);
		return ResultBuilder.createFor(this, vp).setSeverity(Severity.get(score)).setSummary(shortMessage)
				.setExplanation(longMessage).addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(score))
				.addResult(LARGEST_ALLOCATED_BY_TYPE, largestAllocatedByType)
				.addResult(LARGEST_ALLOCATED_TYPE, largestAllocatedType)
				.addResult(SECOND_FRAME_MOST_ALLOCATED, secondFrameFromMostAllocated)
				.addResult(BOXED_ALLOCATION_RATIO, UnitLookup.PERCENT_UNITY.quantity(possibleAutoboxingRatio))
				.addResult(BOXED_ALLOCATION_SIZE, totalAllocationSize).build();
	}
}
