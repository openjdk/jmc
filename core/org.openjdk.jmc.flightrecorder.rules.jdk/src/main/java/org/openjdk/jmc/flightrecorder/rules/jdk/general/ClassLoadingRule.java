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
package org.openjdk.jmc.flightrecorder.rules.jdk.general;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkQueries;
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

public class ClassLoadingRule implements IRule {

	private static final String RESULT_ID = "ClassLoading"; //$NON-NLS-1$

	public static final TypedPreference<IQuantity> MAX_DURATION_LIMIT = new TypedPreference<>(
			"classloading.duration.max.limit", //$NON-NLS-1$
			Messages.getString(Messages.ClassLoadingRule_CONFIG_DURATION_LIMIT),
			Messages.getString(Messages.ClassLoadingRule_CONFIG_DURATION_LIMIT_LONG), UnitLookup.TIMESPAN,
			UnitLookup.MILLISECOND.quantity(1000L));
	public static final TypedPreference<IQuantity> RATIO_OF_TOTAL_LIMIT = new TypedPreference<>(
			"classloading.ratio-to-total.limit", //$NON-NLS-1$
			Messages.getString(Messages.ClassLoadingRule_CONFIG_RATIO_LIMIT),
			Messages.getString(Messages.ClassLoadingRule_CONFIG_RATIO_LIMIT_LONG), UnitLookup.NUMBER,
			UnitLookup.NUMBER_UNITY.quantity(0.10));

	public static final TypedResult<IQuantity> LONGEST_CLASS_LOAD = new TypedResult<>("longestClassLoad", //$NON-NLS-1$
			Messages.getString(Messages.ClassLoadingRule_RESULT_LONGEST_LOAD_NAME),
			Messages.getString(Messages.ClassLoadingRule_RESULT_LONGEST_LOAD_DESCRIPTION), UnitLookup.TIMESPAN,
			IQuantity.class);
	public static final TypedResult<IQuantity> TOTAL_CLASS_LOAD_TIME = new TypedResult<>("totalClassLoadTime", //$NON-NLS-1$
			Messages.getString(Messages.ClassLoadingRule_RESULT_TOTAL_LOAD_TIME_NAME),
			Messages.getString(Messages.ClassLoadingRule_RESULT_TOTAL_LOAD_TIME_DESCRIPTION), UnitLookup.TIMESPAN,
			IQuantity.class);
	public static final TypedResult<IQuantity> TOTAL_CLASS_LOAD_COUNT = new TypedResult<>("totalClassLoadCount", //$NON-NLS-1$
			Messages.getString(Messages.ClassLoadingRule_RESULT_TOTAL_LOAD_COUNT_NAME),
			Messages.getString(Messages.ClassLoadingRule_RESULT_TOTAL_LOAD_COUNT_DESCRIPTION), UnitLookup.TIMESPAN,
			IQuantity.class);

	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays
			.<TypedPreference<?>> asList(MAX_DURATION_LIMIT, RATIO_OF_TOTAL_LIMIT);
	private static final List<TypedResult<?>> RESULT_ATTRIBUTES = Arrays.<TypedResult<?>> asList(TypedResult.SCORE,
			LONGEST_CLASS_LOAD, TOTAL_CLASS_LOAD_COUNT, TOTAL_CLASS_LOAD_TIME);
	private static final Map<String, EventAvailability> REQUIRED_EVENTS;

	static {
		REQUIRED_EVENTS = new HashMap<>();
		REQUIRED_EVENTS.put(JdkTypeIDs.CLASS_LOAD, EventAvailability.ENABLED);
	}

	@Override
	public String getId() {
		return RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.ClassLoadingRuleFactory_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.CLASS_LOADING;
	}

	@Override
	public Map<String, EventAvailability> getRequiredEvents() {
		return REQUIRED_EVENTS;
	}

	private IResult getResult(
		IItemCollection items, IPreferenceValueProvider valueProvider, IResultValueProvider dependencyResults) {
		IQuantity maxDurationLimit = valueProvider.getPreferenceValue(MAX_DURATION_LIMIT);
		IQuantity ratioOfTotalLimit = valueProvider.getPreferenceValue(RATIO_OF_TOTAL_LIMIT);

		IItemCollection events = items.apply(JdkFilters.CLASS_LOAD);

		IQuantity startTime = RulesToolkit.getEarliestStartTime(events);
		IQuantity endTime = RulesToolkit.getLatestEndTime(events);
		if (startTime != null && endTime != null) {
			IQuantity totalTime = endTime.subtract(startTime);
			IAggregator<IQuantity, ?> max = Aggregators.max(JfrAttributes.DURATION);
			IQuantity longestTime = events.getAggregate(max);
			IQuantity sumTimeLoadedClasses = events.getAggregate(Aggregators.sum(JfrAttributes.DURATION));
			if ((longestTime.compareTo(maxDurationLimit) > 0)
					|| (sumTimeLoadedClasses.ratioTo(totalTime) > ratioOfTotalLimit.doubleValue())) {
				IQuantity totalLoadedClasses = events.getAggregate(Aggregators.count());
				return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.INFO)
						.addResult(LONGEST_CLASS_LOAD, longestTime)
						.addResult(TOTAL_CLASS_LOAD_COUNT, totalLoadedClasses)
						.addResult(TOTAL_CLASS_LOAD_TIME, sumTimeLoadedClasses)
						.addResult(TypedResult.ITEM_QUERY, JdkQueries.CLASS_LOAD)
						.setSummary(Messages.getString(Messages.ClassLoadingRule_RESULT_SUMMARY))
						.setExplanation(Messages.getString(Messages.ClassLoadingRule_RESULT_EXPLANATION)).build();
			}
		}
		return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.OK)
				.setSummary(Messages.getString(Messages.ClassLoadingRuleFactory_RULE_TEXT_OK)).build();
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
