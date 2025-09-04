/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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

import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER;
import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER_UNITY;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.ResultBuilder;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.RequiredEventsBuilder;

public class ZGCAllocationStallRule implements IRule {

	private static final String ZGC_RESULT_ID = "ZGCAllocationStallRule"; //$NON-NLS-1$

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.ZGC_ALLOCATION_STALL, EventAvailability.AVAILABLE).build();

	public static final TypedResult<IQuantity> ZGC_ALLOCATION_STALL_EVENTS = new TypedResult<>(
			"zgcAllocationStallCount", //$NON-NLS-1$
			JdkAggregators.ZGC_ALLOCATION_STALL_COUNT, UnitLookup.NUMBER, IQuantity.class);

	public static final TypedResult<IQuantity> ZGC_ALLOCATION_STALL_LONGEST_DURATION = new TypedResult<>(
			"zgcAllocationStallLongestDuration", //$NON-NLS-1$
			JdkAggregators.ZGC_ALLOCATION_STALL_COUNT, UnitLookup.NUMBER, IQuantity.class);

	public static final TypedResult<IQuantity> ZGC_ALLOCATION_STALL_TOTAL_DURATION = new TypedResult<>(
			"zgcAllocationStallTotalDuration", //$NON-NLS-1$
			JdkAggregators.ZGC_ALLOCATION_STALL_COUNT, UnitLookup.NUMBER, IQuantity.class);

	public static final TypedResult<IQuantity> ZGC_ALLOCATION_STALL_PER_MINUTE = new TypedResult<>(
			"zgcAllocationStallPerMinute", //$NON-NLS-1$
			Messages.getString(Messages.ZGCAllocationStallRule_RATE),
			Messages.getString(Messages.ZGCAllocationStallRule_RATE_LONG), UnitLookup.NUMBER, IQuantity.class);
	public static final TypedPreference<IQuantity> ALLOCATION_STALL_INFO_LIMIT = new TypedPreference<>(
			"allocation.stall.rate.info.limit", //$NON-NLS-1$
			Messages.getString(Messages.ZGCAllocationStallRule_CONFIG_INFO_LIMIT),
			Messages.getString(Messages.ZGCAllocationStallRule_CONFIG_INFO_LIMIT_LONG), NUMBER,
			NUMBER_UNITY.quantity(10));

	public static final TypedPreference<IQuantity> ALLOCATION_STALL_WARNING_LIMIT = new TypedPreference<>(
			"allocation.stall.rate.warning.limit", //$NON-NLS-1$
			Messages.getString(Messages.ZGCAllocationStallRule_CONFIG_WARN_LIMIT),
			Messages.getString(Messages.ZGCAllocationStallRule_CONFIG_WARN_LIMIT_LONG), NUMBER,
			NUMBER_UNITY.quantity(100));

	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays
			.<TypedPreference<?>> asList(ALLOCATION_STALL_INFO_LIMIT, ALLOCATION_STALL_WARNING_LIMIT);

	private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays.<TypedResult<?>> asList(
			TypedResult.SCORE, ZGC_ALLOCATION_STALL_EVENTS, ZGC_ALLOCATION_STALL_LONGEST_DURATION,
			ZGC_ALLOCATION_STALL_TOTAL_DURATION, ZGC_ALLOCATION_STALL_PER_MINUTE);

	@Override
	public String getId() {
		return ZGC_RESULT_ID;
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.GARBAGE_COLLECTION;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.ZGCAllocationStall_RULE_NAME);
	}

	@Override
	public Map<String, EventAvailability> getRequiredEvents() {
		return REQUIRED_EVENTS;
	}

	private IResult getResult(
		IItemCollection items, IPreferenceValueProvider valueProvider, IResultValueProvider resultProvider) {
		long infoLimit = valueProvider.getPreferenceValue(ALLOCATION_STALL_INFO_LIMIT).clampedLongValueIn(NUMBER_UNITY);
		long warningLimit = valueProvider.getPreferenceValue(ALLOCATION_STALL_WARNING_LIMIT)
				.clampedLongValueIn(NUMBER_UNITY);

		IQuantity zgcAllocationStallCount = items.getAggregate(JdkAggregators.ZGC_ALLOCATION_STALL_COUNT);
		IQuantity zgcAllocationStallTotalDuration = items.getAggregate(JdkAggregators.TOTAL_ZGC_ALLOCATION_STALL);
		IQuantity zgcAllocationStallLongestDuration = items.getAggregate(JdkAggregators.LONGEST_ZGC_ALLOCATION_STALL);
		if (zgcAllocationStallCount != null && zgcAllocationStallCount.doubleValue() > 0) {

			//Calculate time after JVM Start
			IQuantity timeAfterJVMStart = RulesToolkit.getEarliestStartTime(items)
					.subtract(items.getAggregate(JdkAggregators.JVM_START_TIME));

			//Calculate Stall Per minute
			IQuantity stallPerMinute = UnitLookup.NUMBER_UNITY
					.quantity(zgcAllocationStallTotalDuration.doubleValueIn(UnitLookup.MINUTE)
							/ timeAfterJVMStart.doubleValueIn(UnitLookup.MINUTE));

			double score = RulesToolkit.mapExp100(stallPerMinute.clampedLongValueIn(UnitLookup.NUMBER_UNITY), infoLimit,
					warningLimit);

			return ResultBuilder.createFor(ZGCAllocationStallRule.this, valueProvider).setSeverity(Severity.get(score))
					.setSummary(Messages.getString(Messages.ZgcAllocationStall_TEXT_INFO)
							.concat(Messages.getString(Messages.ZgcAllocationStall_TEXT_WARN)))
					.addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(score))
					.addResult(ZGC_ALLOCATION_STALL_EVENTS, zgcAllocationStallCount)
					.addResult(ZGC_ALLOCATION_STALL_TOTAL_DURATION, zgcAllocationStallTotalDuration)
					.addResult(ZGC_ALLOCATION_STALL_LONGEST_DURATION, zgcAllocationStallLongestDuration)
					.addResult(ZGC_ALLOCATION_STALL_PER_MINUTE, stallPerMinute).build();

		}
		return ResultBuilder.createFor(ZGCAllocationStallRule.this, valueProvider).setSeverity(Severity.OK)
				.setSummary(Messages.getString(Messages.ZgcAllocationStall_TEXT_INFO)
						.concat(Messages.getString(Messages.ZgcAllocationStall_TEXT_OK)))
				.build();
	}

	@Override
	public RunnableFuture<IResult> createEvaluation(
		final IItemCollection items, final IPreferenceValueProvider preferenceValueProvider,
		final IResultValueProvider dependencyResults) {
		FutureTask<IResult> evaluationTask = new FutureTask<>(new Callable<IResult>() {
			@Override
			public IResult call() throws Exception {
				return getResult(items, preferenceValueProvider, dependencyResults);
			}
		});
		return evaluationTask;
	}

	@Override
	public Collection<TypedPreference<?>> getConfigurationAttributes() {
		return CONFIG_ATTRIBUTES;
	}

	@Override
	public Collection<TypedResult<?>> getResults() {
		return RESULT_ATTRIBUTES;
	}

}
