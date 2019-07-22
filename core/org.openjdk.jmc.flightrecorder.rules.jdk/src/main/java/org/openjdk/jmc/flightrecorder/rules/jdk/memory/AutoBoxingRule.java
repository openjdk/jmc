/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.util.HashMap;
import java.util.Map;
import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.IPredicate;
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
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceFormatToolkit;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceFrame;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel.Branch;

/**
 * Rule that checks how much of the total allocation is caused by possible primitive to object
 * conversion. Looks at top frames and searches for java.lang.BoxType.valueOf.
 */
// FIXME: Rename class (and message constants) from autoboxing to something more generic?
public class AutoBoxingRule extends AbstractRule {
	
	private static final String VALUE_OF_METHOD_NAME = "valueOf";
	private static final String SHORT = "java.lang.Short";
	private static final String LONG = "java.lang.Long";
	private static final String INTEGER = "java.lang.Integer";
	private static final String FLOAT = "java.lang.Float";
	private static final String DOUBLE = "java.lang.Double";
	private static final String CHARACTER = "java.lang.Character";
	private static final String BYTE = "java.lang.Byte";
	private static final String BOOLEAN = "java.lang.Boolean";
	
	private static final IPredicate<IMCMethod> IS_AUTOBOXED_PREDICATE = new IPredicate<IMCMethod>() {
		@Override
		public boolean evaluate(IMCMethod method) {
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
		FrameSeparator sep = new FrameSeparator(FrameSeparator.FrameCategorization.LINE, false);
		StacktraceModel model = new StacktraceModel(false, sep, allocationItems);
		Map<IMCType, IQuantity> allocationSizeByType = new HashMap<>();
		IQuantity sizeOfAllBoxedAllocations = UnitLookup.BYTE.quantity(0);
		IQuantity largestAllocatedByType = UnitLookup.BYTE.quantity(0);
		IMCType largestAllocatedType = null;
		String secondFrameFromMostAllocated = "";
		for (StacktraceFrame stacktraceFrame : model.getRootFork().getFirstFrames()) {
			IMCMethod method = stacktraceFrame.getFrame().getMethod();
			if (IS_AUTOBOXED_PREDICATE.evaluate(method)) {
				SimpleArray<IItem> itemArray = stacktraceFrame.getItems();
				IQuantity total = UnitLookup.BYTE.quantity(0);
				for (IItem item : itemArray) {
					total = total.add(RulesToolkit.getValue(item, JdkAttributes.TLAB_SIZE));
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
					secondFrameFromMostAllocated = StacktraceFormatToolkit.formatFrame(secondFrame.getFrame(), sep, false, false, true, true, true, false);
				}
				allocationSizeByType.put(method.getType(), total);
			}
		}
		if (allocationSizeByType.size() == 0) {
			return new Result(this, 0, Messages.getString(Messages.AutoboxingRule_RESULT_NO_AUTOBOXING));
		}
		IQuantity totalAllocationSize = allocationItems.getAggregate(JdkAggregators.ALLOCATION_TOTAL);
		double possibleAutoboxingRatio = sizeOfAllBoxedAllocations.ratioTo(totalAllocationSize) * 100;

		double score = RulesToolkit.mapExp100(possibleAutoboxingRatio, autoboxingRatioInfoLimit,
				autoboxingRatioWarningLimit);

		// Compute information about top autoboxing type
		String mostAllocatedTypeInfo = ""; //$NON-NLS-1$
		String mostAllocatedTypeInfoLong = ""; //$NON-NLS-1$
		if (largestAllocatedType != null) {
			String fullName = largestAllocatedType.getFullName();
			mostAllocatedTypeInfo = " " + MessageFormat //$NON-NLS-1$
					.format(Messages.getString(Messages.AutoboxingRule_RESULT_MOST_AUTOBOXED_TYPE), fullName);
			mostAllocatedTypeInfoLong = "<p>" //$NON-NLS-1$
					+ MessageFormat.format(Messages.getString(Messages.AutoboxingRule_RESULT_MOST_AUTOBOXED_TYPE_LONG),
							fullName, largestAllocatedByType.displayUsing(IDisplayable.AUTO), secondFrameFromMostAllocated);
		}

		String shortIntro = MessageFormat.format(Messages.getString(Messages.AutoboxingRule_RESULT_AUTOBOXING_RATIO),
				Math.round(possibleAutoboxingRatio), sizeOfAllBoxedAllocations.displayUsing(IDisplayable.AUTO));
		String shortMessage = shortIntro + mostAllocatedTypeInfo;
		String longMessage = shortIntro + mostAllocatedTypeInfoLong + "<p>" //$NON-NLS-1$
				+ Messages.getString(Messages.AutoboxingRule_RESULT_LONG);
		return new Result(this, score, shortMessage, longMessage);
	}
}
