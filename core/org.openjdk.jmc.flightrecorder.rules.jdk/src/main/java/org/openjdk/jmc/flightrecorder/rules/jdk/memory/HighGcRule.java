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
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.QuantityRange;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.ResultBuilder;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.jdk.combine.SpanSquare;
import org.openjdk.jmc.flightrecorder.rules.jdk.combine.SpanToolkit;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.RequiredEventsBuilder;

public class HighGcRule implements IRule {

	private static final String RESULT_ID = "HighGc"; //$NON-NLS-1$

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.GC_PAUSE, EventAvailability.ENABLED).build();

	public static final TypedResult<IRange<IQuantity>> PAUSE_CLUSTER = new TypedResult<>("pauseCluster", //$NON-NLS-1$
			"Pause Cluster", "The time in which a cluster of pauses was detected.", UnitLookup.TIMERANGE);
	public static final TypedResult<IQuantity> PAUSE_TIME = new TypedResult<>("pauseTime", "Pause Time Ratio", //$NON-NLS-1$
			"The percent of the cluster spent paused.", UnitLookup.PERCENTAGE, IQuantity.class);

	public static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays.<TypedResult<?>> asList(TypedResult.SCORE,
			PAUSE_CLUSTER, PAUSE_TIME);

	private IResult getResult(
		IItemCollection items, IPreferenceValueProvider valueProvider, IResultValueProvider resultProvider) {
		SpanSquare longestGcCluster = calculateLongestGcCluster(items.apply(JdkFilters.GC_PAUSE));
		if (longestGcCluster != null) {
			long sumPauseNanos = longestGcCluster.mass;
			IRange<IQuantity> cluster = QuantityRange.createWithEnd(
					UnitLookup.EPOCH_NS.quantity(longestGcCluster.start),
					UnitLookup.EPOCH_NS.quantity(longestGcCluster.end));
			long durationNanos = longestGcCluster.end - longestGcCluster.start;
			IQuantity pausePercent = UnitLookup.PERCENT_UNITY.quantity(sumPauseNanos / (double) durationNanos);
			// FIXME: Configuration attribute instead of hard coded 1 second => score 90
			// FIXME: Also consider duration length when calculating score?
			double score = RulesToolkit.mapExp100Y(sumPauseNanos, 1e9, 90);
			// int score = longestGcCluster == null ? 0 : (int) (longestGcCluster.mass / 10000000); // 1 s => 100 p
			String longMessage = Messages.getString(Messages.HighGcRuleFactory_TEXT_INFO_LONG);
			if (!RulesToolkit.isEventsEnabled(items, JdkTypeIDs.ALLOC_INSIDE_TLAB, JdkTypeIDs.ALLOC_OUTSIDE_TLAB)) {
				longMessage = longMessage + "\n" //$NON-NLS-1$
						+ RulesToolkit.getEnabledEventTypesRecommendation(items, JdkTypeIDs.ALLOC_INSIDE_TLAB,
								JdkTypeIDs.ALLOC_OUTSIDE_TLAB);
			}
			return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.get(score))
					.setSummary(Messages.getString(Messages.HighGcRuleFactory_TEXT_INFO)).setExplanation(longMessage)
					.addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(score))
					.addResult(PAUSE_CLUSTER, cluster).addResult(PAUSE_TIME, pausePercent).build();
		}
		return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.OK)
				.setSummary(Messages.getString(Messages.HighGcRuleFactory_TEXT_OK)).build();
	}

	private static SpanSquare calculateLongestGcCluster(IItemCollection items) {
		return SpanToolkit.getMaxDurationCluster(items);
	}

	@Override
	public RunnableFuture<IResult> createEvaluation(
		final IItemCollection items, final IPreferenceValueProvider valueProvider,
		final IResultValueProvider resultProvider) {
		FutureTask<IResult> evaluationTask = new FutureTask<>(new Callable<IResult>() {
			@Override
			public IResult call() throws Exception {
				return getResult(items, valueProvider, resultProvider);
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
		return RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.HighGcRuleFactory_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.HEAP;
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
