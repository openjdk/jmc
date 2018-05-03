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

import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER;
import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER_UNITY;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkQueries;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;

public class BufferLostRule implements IRule {
	private static final String BUFFER_LOST_RESULT_ID = "BufferLost"; //$NON-NLS-1$

	public static final TypedPreference<IQuantity> WARNING_LIMIT = new TypedPreference<>("bufferlost.warning.limit", //$NON-NLS-1$
			Messages.getString(Messages.BufferLostRuleFactory_CONFIG_WARN_LIMIT),
			Messages.getString(Messages.BufferLostRuleFactory_CONFIG_WARN_LIMIT_LONG), NUMBER,
			NUMBER_UNITY.quantity(1));

	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays.<TypedPreference<?>> asList(WARNING_LIMIT);

	private Result getResult(IItemCollection items, IPreferenceValueProvider valueProvider) {
		IQuantity limit = valueProvider.getPreferenceValue(WARNING_LIMIT);

		/*
		 * We don't believe JFR_DATA_LOST can be turned off, and recordings do not seem to have
		 * enablement information on them, so no point in checking if it's enabled.
		 */

		IItemCollection filtered = items.apply(JdkFilters.JFR_DATA_LOST);
		IQuantity startTime = filtered.getAggregate(JdkAggregators.first(JfrAttributes.START_TIME));

		if (startTime != null) {
			IQuantity droppedCount = filtered.getAggregate(JdkAggregators.JFR_DATA_LOST_COUNT);
			IQuantity droppedSize = filtered.getAggregate(JdkAggregators.FLR_DATA_LOST_SIZE);

			String shortDescription = MessageFormat.format(Messages.BufferLostRuleFactory_TEXT_INFO,
					droppedCount.displayUsing(IDisplayable.AUTO), droppedSize.displayUsing(IDisplayable.AUTO));
			String longDescription = MessageFormat.format(
					Messages.getString(Messages.BufferLostRuleFactory_TEXT_INFO_LONG),
					droppedCount.displayUsing(IDisplayable.AUTO), droppedSize.displayUsing(IDisplayable.AUTO),
					startTime.displayUsing(IDisplayable.AUTO));
			return new Result(this, calculateScore(limit, droppedCount), shortDescription, longDescription,
					JdkQueries.JFR_DATA_LOST);
		}
		return new Result(this, 0, Messages.getString(Messages.BufferLostRuleFactory_RULE_TEXT_OK));
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

	private double calculateScore(IQuantity limit, IQuantity droppedCount) {
		return RulesToolkit.mapExp100(limit.clampedLongValueIn(NUMBER_UNITY), droppedCount.longValue());
	}

	@Override
	public Collection<TypedPreference<?>> getConfigurationAttributes() {
		return CONFIG_ATTRIBUTES;
	}

	@Override
	public String getId() {
		return BUFFER_LOST_RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.BufferLostRuleFactory_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.RECORDING_TOPIC;
	}
}
