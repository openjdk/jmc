/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2023, 2025, Red Hat Inc. All rights reserved.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.item.PersistableItemFilter.Kind;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
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

/**
 * Rule that looks at the G1 GC time and compares it to the max pause time.
 */
public class GcPauseTargetRule implements IRule {

	private static final String GC_PAUSE_TARGET_RESULT_ID = "GcPauseTargetRule"; //$NON-NLS-1$

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.GC_G1MMU, EventAvailability.AVAILABLE).build();

	public static final TypedResult<IQuantity> EXCEEDED_PERCENT = new TypedResult<>("gcExceededPercent", //$NON-NLS-1$
			"Pause target exceeded percent",
			"The percentage of G1MMU events where the gcTime exceeded the pauseTarget.", UnitLookup.PERCENTAGE,
			IQuantity.class);

	public static final TypedResult<IQuantity> G1MMU_TOTAL = new TypedResult<>("g1mmuTotal", "Total G1MMU Events", //$NON-NLS-1$
			"The number of G1MMU events that occured during the recording.", UnitLookup.NUMBER, IQuantity.class);

	public static final TypedResult<IQuantity> G1MMU_EXCEEDED = new TypedResult<>("g1mmuExceeded", //$NON-NLS-1$
			"Exceeded pause targets", "The number of G1MMU events where the gcTime exceeded the pauseTarget.",
			UnitLookup.NUMBER, IQuantity.class);

	public static final TypedResult<IQuantity> PAUSE_TARGET = new TypedResult<>("pauseTarget", "Pause target", //$NON-NLS-1$
			"Max time allowed to be spent on GC during last time slice.", UnitLookup.NUMBER, IQuantity.class);

	public final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays.<TypedResult<?>> asList(EXCEEDED_PERCENT,
			G1MMU_TOTAL, G1MMU_EXCEEDED, PAUSE_TARGET);

	@Override
	public RunnableFuture<IResult> createEvaluation(
		final IItemCollection items, final IPreferenceValueProvider preferenceValueProvider,
		final IResultValueProvider dependencyResults) {
		FutureTask<IResult> evaluationTask = new FutureTask<>(new Callable<IResult>() {
			@Override
			public IResult call() throws Exception {
				IQuantity pauseTarget = items
						.getAggregate((IAggregator<IQuantity, ?>) Aggregators.max(JdkAttributes.GC_PAUSE_TARGET));
				IQuantity maxGcTime = items
						.getAggregate((IAggregator<IQuantity, ?>) Aggregators.max(JdkAttributes.GC_TIME));
				if (maxGcTime.compareTo(pauseTarget) < 1) {
					return ResultBuilder.createFor(GcPauseTargetRule.this, preferenceValueProvider)
							.setSeverity(Severity.OK).setSummary(Messages.getString(Messages.GcPauseTargetRule_TEXT_OK))
							.addResult(PAUSE_TARGET, pauseTarget).build();
				} else {
					IItemCollection g1mmuItems = items.apply(JdkFilters.GC_G1MMU);
					IItemFilter filter = ItemFilters.buildComparisonFilter(Kind.MORE, JdkAttributes.GC_TIME,
							pauseTarget);
					IQuantity g1mmuTotal = g1mmuItems.getAggregate(Aggregators.count());
					IQuantity g1mmuExceeded = g1mmuItems.apply(filter).getAggregate(Aggregators.count());
					IQuantity exceededPercent = RulesToolkit.toRatioPercent(g1mmuExceeded, g1mmuTotal);
					return ResultBuilder.createFor(GcPauseTargetRule.this, preferenceValueProvider)
							.setSeverity(Severity.WARNING)
							.setSummary(Messages.getString(Messages.GcPauseTargetRule_TEXT_WARN_SHORT))
							.setExplanation(Messages.getString(Messages.GcPauseTargetRule_TEXT_WARN_LONG))
							.addResult(PAUSE_TARGET, pauseTarget).addResult(EXCEEDED_PERCENT, exceededPercent)
							.addResult(G1MMU_TOTAL, g1mmuTotal).addResult(G1MMU_EXCEEDED, g1mmuExceeded).build();
				}
			}
		});
		return evaluationTask;
	}

	@Override
	public Collection<TypedPreference<?>> getConfigurationAttributes() {
		return Collections.emptyList();
	}

	@Override
	public String getId() {
		return GC_PAUSE_TARGET_RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.GcPauseTargetRule_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.GARBAGE_COLLECTION;
	}

	@Override
	public Map<String, EventAvailability> getRequiredEvents() {
		return REQUIRED_EVENTS;
	}

	@Override
	public Collection<TypedResult<?>> getResults() {
		return RESULT_ATTRIBUTES;
	}

}
