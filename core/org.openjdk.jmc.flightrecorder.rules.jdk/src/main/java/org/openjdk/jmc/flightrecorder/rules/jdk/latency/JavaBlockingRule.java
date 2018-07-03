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
package org.openjdk.jmc.flightrecorder.rules.jdk.latency;

import static org.openjdk.jmc.common.unit.UnitLookup.PLAIN_TEXT;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.collection.MapToolkit.IntEntry;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.GroupingAggregator;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.dataproviders.MethodProfilingDataProvider;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.owasp.encoder.Encode;

public class JavaBlockingRule implements IRule {

	public static final TypedPreference<String> EXCLUDED_THREADS_REGEXP = new TypedPreference<>("thread.exclude.regexp", //$NON-NLS-1$
			Messages.getString(Messages.JavaBlockingRule_CONFIG_EXCLUDED_THREADS),
			Messages.getString(Messages.JavaBlockingRule_CONFIG_EXCLUDED_THREADS_LONG), PLAIN_TEXT.getPersister(),
			"(.*weblogic\\.socket\\.Muxer.*)"); //$NON-NLS-1$

	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays
			.<TypedPreference<?>> asList(EXCLUDED_THREADS_REGEXP);

	public static final IAggregator<IQuantity, ?> MONITOR_BALANCE_BY_INSTANCE = GroupingAggregator.build(
			Messages.getString(Messages.JavaBlockingRule_AGGR_BALANCE_BY_INSTANCE), null, JdkAttributes.MONITOR_ADDRESS,
			JdkAggregators.TOTAL_BLOCKED_TIME, MethodProfilingDataProvider.topFrameBalanceFunction);

	public static final IAggregator<IQuantity, ?> MONITOR_BALANCE_BY_THREAD = GroupingAggregator.build(
			Messages.getString(Messages.JavaBlockingRule_AGGR_BALANCE_BY_THREAD), null, JfrAttributes.EVENT_THREAD,
			JdkAggregators.TOTAL_BLOCKED_TIME, MethodProfilingDataProvider.topFrameBalanceFunction);

	private static final String RESULT_ID = "JavaBlocking"; //$NON-NLS-1$

	private Result getResult(IItemCollection items, IPreferenceValueProvider valueProvider) {
		String threadExcludeRegexp = valueProvider.getPreferenceValue(EXCLUDED_THREADS_REGEXP);
		items = items.apply(ItemFilters.not(ItemFilters.matches(JdkAttributes.EVENT_THREAD_NAME, threadExcludeRegexp)));

		EventAvailability eventAvailability = RulesToolkit.getEventAvailability(items, JdkTypeIDs.MONITOR_ENTER);
		if (eventAvailability != EventAvailability.AVAILABLE) {
			return RulesToolkit.getEventAvailabilityResult(this, items, eventAvailability, JdkTypeIDs.MONITOR_ENTER);
		}

		IQuantity startTime = items.getAggregate(JdkAggregators.FIRST_ITEM_START);
		IQuantity endTime = items.getAggregate(JdkAggregators.LAST_ITEM_END);
		IQuantity recordingTime = endTime.subtract(startTime);

		IQuantity byInstance = items.getAggregate(MONITOR_BALANCE_BY_INSTANCE);

		IQuantity byThread = items.getAggregate(MONITOR_BALANCE_BY_THREAD);
		if (byInstance == null || byThread == null) {
			return new Result(this, 0, Messages.getString(Messages.JavaBlockingRule_TEXT_OK));
		}
		IQuantity totalWait = items.getAggregate(Aggregators.sum(JdkTypeIDs.MONITOR_ENTER, JfrAttributes.DURATION));
		IQuantity waitRatio = UnitLookup.NUMBER_UNITY.quantity(totalWait.ratioTo(recordingTime));

		String excludeText = ""; //$NON-NLS-1$
		double balanceScore = 1;

		if (!threadExcludeRegexp.isEmpty()) {
			excludeText = "<p>" + MessageFormat.format( //$NON-NLS-1$
					Messages.getString(Messages.JavaBlockingRule_TEXT_EXCLUDED_THREADS), threadExcludeRegexp);
		}

		double weightedValue = RulesToolkit.mapExp100(waitRatio.doubleValue() * balanceScore, 1);
		if (weightedValue < 25) {
			String shortMessage = Messages.getString(Messages.JavaBlockingRule_TEXT_MESSAGE);
			String longMessage = shortMessage + excludeText;
			return new Result(this, weightedValue, shortMessage, longMessage);
		}

		// Significant blocking detected - do more calculations
		String mostBlockingText;
		if (byThread.compareTo(byInstance) > 0) {
			List<IntEntry<IMCThread>> groupedByThread = RulesToolkit.calculateGroupingScore(items,
					JfrAttributes.EVENT_THREAD);
			IntEntry<IMCThread> mostBlockedThread = groupedByThread.get(groupedByThread.size() - 1);

			mostBlockingText = MessageFormat.format(
					Messages.getString(Messages.JavaBlockingRule_TEXT_MOST_BLOCKED_THREAD),
					Encode.forHtml(mostBlockedThread.getKey().getThreadName()), mostBlockedThread.getValue());
		} else {
			List<IntEntry<IMCType>> groupedByClass = RulesToolkit.calculateGroupingScore(
					items.apply(ItemFilters.type(JdkTypeIDs.MONITOR_ENTER)), JdkAttributes.MONITOR_CLASS);
			IntEntry<IMCType> mostBlockingClass = groupedByClass.get(groupedByClass.size() - 1);

			mostBlockingText = MessageFormat.format(
					Messages.getString(Messages.JavaBlockingRule_TEXT_MOST_BLOCKED_CLASS),
					Encode.forHtml(mostBlockingClass.getKey().getTypeName()), mostBlockingClass.getValue());
		}
		String shortMessage = MessageFormat.format(Messages.getString(Messages.JavaBlockingRule_TEXT_INFO),
				totalWait.displayUsing(IDisplayable.AUTO));
		String longMessage = shortMessage + " " + mostBlockingText + excludeText; //$NON-NLS-1$
		return new Result(this, weightedValue, shortMessage, longMessage);
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
		return Messages.getString(Messages.JavaBlocking_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.LOCK_INSTANCES_TOPIC;
	}
}
