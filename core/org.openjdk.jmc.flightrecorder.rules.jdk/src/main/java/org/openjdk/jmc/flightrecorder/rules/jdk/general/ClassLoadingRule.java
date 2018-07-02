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
package org.openjdk.jmc.flightrecorder.rules.jdk.general;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkQueries;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
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

	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays
			.<TypedPreference<?>> asList(MAX_DURATION_LIMIT, RATIO_OF_TOTAL_LIMIT);

	private Result getResult(IItemCollection items, IPreferenceValueProvider valueProvider) {
		EventAvailability eventAvailability = RulesToolkit.getEventAvailability(items, JdkTypeIDs.CLASS_LOAD);
		if (eventAvailability == EventAvailability.UNKNOWN || eventAvailability == EventAvailability.DISABLED) {
			return RulesToolkit.getEventAvailabilityResult(this, items, eventAvailability, JdkTypeIDs.CLASS_LOAD);
		}

		IQuantity maxDurationLimit = valueProvider.getPreferenceValue(MAX_DURATION_LIMIT);
		IQuantity ratioOfTotalLimit = valueProvider.getPreferenceValue(RATIO_OF_TOTAL_LIMIT);

		IItemCollection events = items.apply(JdkFilters.CLASS_LOAD);

		IQuantity startTime = events.getAggregate(JdkAggregators.FIRST_ITEM_START);
		IQuantity endTime = events.getAggregate(JdkAggregators.LAST_ITEM_END);
		if (startTime != null && endTime != null) {
			IQuantity totalTime = endTime.subtract(startTime);
			IQuantity max = events.getAggregate(Aggregators.max(JfrAttributes.DURATION));
			IQuantity sum = events.getAggregate(Aggregators.sum(JfrAttributes.DURATION));
			// FIXME: Consider using a score function instead of set value.
			if ((max.compareTo(maxDurationLimit) > 0) || (sum.ratioTo(totalTime) > ratioOfTotalLimit.doubleValue())) {
				String totalTimeString = sum.displayUsing(IDisplayable.AUTO);
				String maxTimeString = max.displayUsing(IDisplayable.AUTO);
				String loadCountString = events.getAggregate(Aggregators.count()).displayUsing(IDisplayable.AUTO);
				return new Result(this, 50,
						MessageFormat.format(Messages.getString(Messages.ClassLoadingRuleFactory_TEXT_INFO),
								totalTimeString, loadCountString),
						MessageFormat.format(Messages.getString(Messages.ClassLoadingRuleFactory_TEXT_INFO_LONG),
								totalTimeString, loadCountString, maxTimeString),
						JdkQueries.CLASS_LOAD);
			}
		}
		return new Result(this, 0, Messages.getString(Messages.ClassLoadingRuleFactory_RULE_TEXT_OK));
	}

	@Override
	public RunnableFuture<Result> evaluate(final IItemCollection items, final IPreferenceValueProvider valueProvider) {
		FutureTask<Result> evaluationTask = new FutureTask<>(new Callable<Result>() {
			@Override
			public Result call() throws Exception {
				return getResult(items, valueProvider);
			}
		});
		return evaluationTask;
	}

	@Override
	public Collection<TypedPreference<?>> getConfigurationAttributes() {
		return CONFIG_ATTRIBUTES;
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
		return JfrRuleTopics.CLASS_LOADING_TOPIC;
	}

}
