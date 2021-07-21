/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

public class LowOnPhysicalMemoryRule implements IRule {

	private static final String RESULT_ID = "LowOnPhysicalMemory"; //$NON-NLS-1$

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.OS_MEMORY_SUMMARY, EventAvailability.ENABLED).build();

	public static final TypedResult<IQuantity> USED_MEMORY_RATIO = new TypedResult<>("usedMemoryRatio", //$NON-NLS-1$
			"Used Memory Ratio", "The percentage of used physical memory.", UnitLookup.PERCENTAGE, IQuantity.class);
	public static final TypedResult<IQuantity> TOTAL_MEMORY = new TypedResult<>("totalMemory", //$NON-NLS-1$
			JdkAggregators.MIN_TOTAL_MEMORY, UnitLookup.MEMORY, IQuantity.class);
	public static final TypedResult<IQuantity> MAX_USED_MEMORY = new TypedResult<>("maxUsedMemory", //$NON-NLS-1$
			JdkAggregators.MAX_USED_MEMORY, UnitLookup.MEMORY, IQuantity.class);

	private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays
			.<TypedResult<?>> asList(TypedResult.SCORE, USED_MEMORY_RATIO, TOTAL_MEMORY, MAX_USED_MEMORY);

	private IResult getResult(
		IItemCollection items, IPreferenceValueProvider valueProvider, IResultValueProvider resultProvider) {
		IQuantity total = items.getAggregate(JdkAggregators.MIN_TOTAL_MEMORY);
		IQuantity used = items.getAggregate(JdkAggregators.MAX_USED_MEMORY);

		if (used != null) {
			// FIXME: Configuration attribute for warning limit and maybe safe usage
			double safeUsage = 0.85;
			double warningLimit = 0.95;
			double usage = used.ratioTo(total);
			// FIXME: Check that calculation gives a reasonable value
			// double unsafeUse = (used - (total * safeUsage)) / (total * (1 - safeUsage));
			double score = RulesToolkit.mapExp100(usage - safeUsage, 0, warningLimit - safeUsage);
			if (score > 0) {
				return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.get(score))
						.setSummary(Messages.getString(Messages.LowOnPhysicalMemoryFactory_TEXT_INFO))
						.setExplanation(Messages.getString(Messages.LowOnPhysicalMemoryFactory_TEXT_INFO_LONG))
						.addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(score))
						.addResult(USED_MEMORY_RATIO, UnitLookup.PERCENT_UNITY.quantity(usage))
						.addResult(TOTAL_MEMORY, total).addResult(MAX_USED_MEMORY, used).build();
			}
		}
		return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.OK)
				.setSummary(Messages.getString(Messages.LowOnPhysicalMemoryFactory_TEXT_OK)).build();
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
		return Messages.getString(Messages.LowOnPhysicalMemoryFactory_RULE_NAME);
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
