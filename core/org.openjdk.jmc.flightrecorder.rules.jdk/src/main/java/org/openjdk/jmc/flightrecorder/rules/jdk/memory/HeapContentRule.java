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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.GroupingAggregator;
import org.openjdk.jmc.common.item.GroupingAggregator.IQuantityListFinisher;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkQueries;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

public class HeapContentRule implements IRule {

	private static final IAggregator<IQuantity, ?> HEAP_CONTENT_SCORE_AGGREGATOR = GroupingAggregator.build(
			Messages.getString(Messages.HeapContentRule_AGGR_CLASS_SCORE), null, JdkAttributes.OBJECT_CLASS,
			JdkAggregators.OBJECT_COUNT_MAX_SIZE, new IQuantityListFinisher<IQuantity>() {

				@Override
				public IType<IQuantity> getValueType() {
					return UnitLookup.NUMBER;
				}

				@Override
				public IQuantity getValue(List<IQuantity> t, IQuantity u) {
					double classScore = 0;
					for (int i = 0; i < t.size(); i++) {
						classScore += t.get(i).doubleValue() / u.doubleValue() / (t.size() - i);
					}
					return UnitLookup.NUMBER_UNITY.quantity(classScore);
				}

			});

	private static final String RESULT_ID = "HeapContent"; //$NON-NLS-1$

	private Result getResult(IItemCollection items, IPreferenceValueProvider valueProvider) {
		EventAvailability eventAvailability = RulesToolkit.getEventAvailability(items, JdkTypeIDs.OBJECT_COUNT);
		if (eventAvailability != EventAvailability.AVAILABLE) {
			return RulesToolkit.getEventAvailabilityResult(this, items, eventAvailability, JdkTypeIDs.OBJECT_COUNT);
		}

		IQuantity aggregate = items.getAggregate(HEAP_CONTENT_SCORE_AGGREGATOR);
		// FIXME: Configuration attribute instead of hard coded 0.75 warning limit
		double score = aggregate == null ? 0 : RulesToolkit.mapExp100(aggregate.doubleValue(), 0.75);
		// FIXME: Construct a more informative message, not use a hard limit. Include a description of the aggregate.
		// As the samples come at different points in time, it's not very obvious what this aggregate represents.
		if (score >= 25) {
			String shortMessage = Messages.getString(Messages.HeapContentRuleFactory_TEXT_INFO);
			String longMessage = shortMessage + " " //$NON-NLS-1$
					+ Messages.getString(Messages.HeapContentRuleFactory_TEXT_INFO_LONG);
			return new Result(this, score, shortMessage, longMessage, JdkQueries.OBJECT_COUNT);
		} else {
			return new Result(this, score, Messages.getString(Messages.HeapContentRuleFactory_TEXT_OK));
		}
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
		return Collections.emptyList();
	}

	@Override
	public String getId() {
		return RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.HeapContentRuleFactory_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.HEAP_TOPIC;
	}
}
