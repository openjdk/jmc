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

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

public class TlabAllocationRatioRule implements IRule {

	private static final String RESULT_ID = "TlabAllocationRatio"; //$NON-NLS-1$

	private Result getResult(IItemCollection items, IPreferenceValueProvider valueProvider) {

		EventAvailability eventAvailability = RulesToolkit.getEventAvailability(items, JdkTypeIDs.ALLOC_INSIDE_TLAB,
				JdkTypeIDs.ALLOC_OUTSIDE_TLAB);
		if (eventAvailability == EventAvailability.DISABLED || eventAvailability == EventAvailability.UNKNOWN) {
			return RulesToolkit.getEventAvailabilityResult(this, items, eventAvailability, JdkTypeIDs.ALLOC_INSIDE_TLAB,
					JdkTypeIDs.ALLOC_OUTSIDE_TLAB);
		}

		IQuantity insideSum = items.getAggregate(JdkAggregators.ALLOC_INSIDE_TLAB_SUM);
		IQuantity outsideSum = items.getAggregate(JdkAggregators.ALLOC_OUTSIDE_TLAB_SUM);

		if (outsideSum == null) {
			return new Result(this, 0, Messages.getString(Messages.TlabAllocationRatioRuleFactory_TEXT_OK_NO_OUTSIDE));
		} else if (insideSum == null) {
			String shortMessage = Messages.getString(Messages.TlabAllocationRatioRuleFactory_TEXT_INFO_ONLY_OUTSIDE);
			String longMessage = Messages.getString(Messages.TlabAllocationRatioRuleFactory_TEXT_INFO_ONLY_OUTSIDE_LONG)
					+ "<p>" //$NON-NLS-1$
					+ Messages.getString(Messages.TlabAllocationRatioRuleFactory_TEXT_RECOMMEND_LESS_ALLOCATION);
			return new Result(this, 100, shortMessage, longMessage);
		}

		IQuantity totalSum = insideSum.add(outsideSum);

		double rawRatio = outsideSum.ratioTo(totalSum);
		// FIXME: Configuration attribute instead of hard coded 0.2 for info limit
		double score = RulesToolkit.mapExp74(rawRatio, 0.2);
		String shortMessage = MessageFormat.format(
				Messages.getString(Messages.TlabAllocationRatioRuleFactory_TEXT_INFO),
				UnitLookup.PERCENT_UNITY.quantity(rawRatio).displayUsing(IDisplayable.AUTO));
		String longMessage = shortMessage;
		if (score >= 25) {
			longMessage = longMessage + "<p>" //$NON-NLS-1$
					+ Messages.getString(Messages.TlabAllocationRatioRuleFactory_TEXT_RECOMMEND_LESS_ALLOCATION);
		}
		return new Result(this, score, shortMessage, longMessage);
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
		return Messages.getString(Messages.TlabAllocationRatioRuleFactory_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.TLAB_TOPIC;
	}

}
