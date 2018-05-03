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

import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER;
import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER_UNITY;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.GroupingAggregator;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

public class ContextSwitchRule implements IRule {

	public static final TypedPreference<IQuantity> CONTEXT_SWITCH_WARNING_LIMIT = new TypedPreference<>(
			"contextswitch.warning.limit", //$NON-NLS-1$
			Messages.getString(Messages.ContextSwitchRule_CONFIG_WARNING_LIMIT),
			Messages.getString(Messages.ContextSwitchRule_CONFIG_WARNING_LIMIT_LONG), NUMBER,
			NUMBER_UNITY.quantity(10000));

	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays
			.<TypedPreference<?>> asList(CONTEXT_SWITCH_WARNING_LIMIT);

	private static final String RESULT_ID = "ContextSwitch"; //$NON-NLS-1$

	private static final IAggregator<IQuantity, ?> MAX_BLOCKS = GroupingAggregator.buildMax(
			Messages.getString(Messages.ContextSwitchRule_AGGR_MAX_BLOCKS), null, JdkAttributes.MONITOR_ADDRESS,
			JdkAggregators.TOTAL_BLOCKED_COUNT);

	@Override
	public RunnableFuture<Result> evaluate(final IItemCollection items, final IPreferenceValueProvider vp) {
		FutureTask<Result> evaluationTask = new FutureTask<>(new Callable<Result>() {
			@Override
			public Result call() throws Exception {
				return evaluate(items, vp.getPreferenceValue(CONTEXT_SWITCH_WARNING_LIMIT)
						.clampedLongValueIn(UnitLookup.NUMBER_UNITY));
			}
		});
		return evaluationTask;
	}

	private Result evaluate(IItemCollection items, long switchRateLimit) {
		EventAvailability eventAvailability = RulesToolkit.getEventAvailability(items, JdkTypeIDs.CONTEXT_SWITCH_RATE);
		if (eventAvailability != EventAvailability.AVAILABLE) {
			return RulesToolkit.getEventAvailabilityResult(this, items, eventAvailability,
					JdkTypeIDs.CONTEXT_SWITCH_RATE);
		}

		long switchRate = calculateSwitchRate(items);
		if (switchRate == -1) {
			return RulesToolkit.getTooFewEventsResult(this);
		}
		long mostBlocks = (switchRate > switchRateLimit) ? getMostBlocks(items) : 0;
		// FIXME: Configuration attribute for the warning and info limits
		int warningLimit = 100;
		int infoLimit = warningLimit / 2;
		double score = RulesToolkit.mapExp100(mostBlocks, infoLimit, warningLimit);
		String text, longText;
		if (score < 25) {
			text = Messages.getString(Messages.ContextSwitchRuleFactory_TEXT_OK);
			longText = null;
		} else {
			text = Messages.getString(Messages.ContextSwitchRuleFactory_TEXT_INFO);
			longText = Messages.getString(Messages.ContextSwitchRuleFactory_TEXT_INFO_LONG);
		}
		return new Result(this, score, text, longText);
	}

	private static long calculateSwitchRate(IItemCollection switchItems) {
		IQuantity aggregate = switchItems
				.getAggregate(Aggregators.avg(JdkTypeIDs.CONTEXT_SWITCH_RATE, JdkAttributes.OS_SWITCH_RATE));
		return aggregate == null ? -1 : aggregate.longValue();
	}

	private static long getMostBlocks(IItemCollection items) {
		IQuantity aggregate = items.getAggregate(MAX_BLOCKS);
		return aggregate == null ? 0 : aggregate.longValue();
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
		return Messages.getString(Messages.ContextSwitchRuleFactory_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.LOCK_INSTANCES_TOPIC;
	}
}
