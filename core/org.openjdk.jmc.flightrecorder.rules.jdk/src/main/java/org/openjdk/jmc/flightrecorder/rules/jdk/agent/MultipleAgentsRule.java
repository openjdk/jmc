/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2024, 2025, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.rules.jdk.agent;

import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER;
import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER_UNITY;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.QuantitiesToolkit;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.ResultBuilder;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.RequiredEventsBuilder;

public class MultipleAgentsRule implements IRule {
	private static final String MULTIPLE_AGENTS_RESULT_ID = "MultipleAgents"; //$NON-NLS-1$

	public static final TypedPreference<IQuantity> JAVA_WARNING_LIMIT = new TypedPreference<>(
			"agents.muliple.java.warning.limit", //$NON-NLS-1$
			Messages.getString(Messages.MultipleAgentsRule_JAVA_WARNING_LIMIT),
			Messages.getString(Messages.MultipleAgentsRule_JAVA_WARNING_LIMIT_LONG), NUMBER, NUMBER_UNITY.quantity(1));

	public static final TypedPreference<IQuantity> NATIVE_WARNING_LIMIT = new TypedPreference<>(
			"agents.muliple.native.warning.limit", //$NON-NLS-1$
			Messages.getString(Messages.MultipleAgentsRule_NATIVE_WARNING_LIMIT),
			Messages.getString(Messages.MultipleAgentsRule_NATIVE_WARNING_LIMIT_LONG), NUMBER,
			NUMBER_UNITY.quantity(1));

	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays
			.<TypedPreference<?>> asList(JAVA_WARNING_LIMIT, NATIVE_WARNING_LIMIT);

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.JAVA_AGENT, EventAvailability.ENABLED)
			.addEventType(JdkTypeIDs.NATIVE_AGENT, EventAvailability.ENABLED).build();

	public static final TypedResult<IQuantity> JAVA_AGENT_COUNT = new TypedResult<>("javaAgentCount", //$NON-NLS-1$
			"Java Agent Count", "The number of active Java Agents.", //$NON-NLS-1$
			UnitLookup.NUMBER, IQuantity.class);
	public static final TypedResult<IQuantity> NATIVE_AGENT_COUNT = new TypedResult<>("nativeAgentCount", //$NON-NLS-1$
			"Native Agent Count", "The number of active Native Agents.", //$NON-NLS-1$
			UnitLookup.TIMESTAMP, IQuantity.class);
	public static final TypedResult<IQuantity> TOTAL_AGENT_COUNT = new TypedResult<>("totalAgentCount", //$NON-NLS-1$
			"Total Agent Count", "The total number of active Agents.", //$NON-NLS-1$
			UnitLookup.TIMESTAMP, IQuantity.class);

	private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays
			.<TypedResult<?>> asList(TypedResult.SCORE, JAVA_AGENT_COUNT, NATIVE_AGENT_COUNT, TOTAL_AGENT_COUNT);

	private IResult getResult(
		IItemCollection items, IPreferenceValueProvider valueProvider, IResultValueProvider resultProvider) {
		IQuantity javaCountQuantity = QuantitiesToolkit
				.nullSafe(items.apply(ItemFilters.type(JdkTypeIDs.JAVA_AGENT)).getAggregate(Aggregators.count()));
		IQuantity nativeCountQuantity = QuantitiesToolkit
				.nullSafe(items.apply(ItemFilters.type(JdkTypeIDs.NATIVE_AGENT)).getAggregate(Aggregators.count()));
		IQuantity totalCountQuantity = javaCountQuantity.add(nativeCountQuantity);

		long javaWarningLimit = valueProvider.getPreferenceValue(JAVA_WARNING_LIMIT).clampedFloorIn(NUMBER_UNITY);
		long nativeWarningLimit = valueProvider.getPreferenceValue(JAVA_WARNING_LIMIT).clampedFloorIn(NUMBER_UNITY);
		long javaCount = javaCountQuantity.longValue();
		long nativeCount = nativeCountQuantity.longValue();

		if (javaCount > javaWarningLimit || nativeCount > nativeWarningLimit) {
			return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.WARNING)
					.setSummary(Messages.getString(Messages.MultipleAgentsRule_TEXT_SUMMARY))
					.setExplanation(Messages.getString(Messages.MultipleAgentsRule_TEXT_EXPLANATION))
					.setSolution(Messages.getString(Messages.MultipleAgentsRule_TEXT_SOLUTION))
					.addResult(JAVA_AGENT_COUNT, javaCountQuantity).addResult(NATIVE_AGENT_COUNT, nativeCountQuantity)
					.addResult(TOTAL_AGENT_COUNT, totalCountQuantity)
					.addResult(TypedResult.SCORE, NUMBER_UNITY.quantity(55)).build();
		}
		return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.OK)
				.setSummary(Messages.getString(Messages.MultipleAgentsRule_TEXT_OK))
				.addResult(JAVA_AGENT_COUNT, javaCountQuantity).addResult(NATIVE_AGENT_COUNT, nativeCountQuantity)
				.addResult(TOTAL_AGENT_COUNT, totalCountQuantity).build();
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
		return CONFIG_ATTRIBUTES;
	}

	@Override
	public String getId() {
		return MULTIPLE_AGENTS_RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.MultipleAgentsRule_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.AGENT_INFORMATION;
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
