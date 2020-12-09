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
package org.openjdk.jmc.flightrecorder.rules.jdk.cpu;

import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER;
import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER_UNITY;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.collection.MapToolkit.IntEntry;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
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

public class ManyRunningProcessesRule implements IRule {
	private static final String MANY_RUNNING_PROCESSES_RESULT_ID = "ManyRunningProcesses"; //$NON-NLS-1$

	public static final TypedPreference<IQuantity> OTHER_PROCESSES_INFO_LIMIT = new TypedPreference<>(
			"other.processes.info.limit", //$NON-NLS-1$
			Messages.getString(Messages.ManyRunningProcessesRule_INFO_LIMIT),
			Messages.getString(Messages.ManyRunningProcessesRule_INFO_LIMIT_LONG), NUMBER, NUMBER_UNITY.quantity(150));

	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays
			.<TypedPreference<?>> asList(OTHER_PROCESSES_INFO_LIMIT);

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.PROCESSES, EventAvailability.AVAILABLE).build();

	public static final TypedResult<IQuantity> COMPETING_PROCESS_COUNT = new TypedResult<>("competingProcessCount", //$NON-NLS-1$
			"Competing Process Count", "The number of other processes running at the same time on the same system.", //$NON-NLS-1$
			UnitLookup.NUMBER, IQuantity.class);
	public static final TypedResult<IQuantity> COMPETING_PROCESS_TIME = new TypedResult<>("competingProcessTime", //$NON-NLS-1$
			"Competing Process Time", "The timestamp when the number of competing processes was at the maximum.", //$NON-NLS-1$
			UnitLookup.TIMESTAMP, IQuantity.class);

	private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays
			.<TypedResult<?>> asList(COMPETING_PROCESS_COUNT, COMPETING_PROCESS_TIME);

	private IResult getResult(IItemCollection items, IPreferenceValueProvider vp, IResultValueProvider rp) {
		// FIXME: Can we really be sure that 'concurrent' events have the exact same timestamp?
		List<IntEntry<IQuantity>> entries = RulesToolkit.calculateGroupingScore(items.apply(JdkFilters.PROCESSES),
				JfrAttributes.END_TIME);
		if (entries.size() > 0) {
			IntEntry<IQuantity> maxNumberProcesses = entries.get(entries.size() - 1);
			double score = RulesToolkit.mapExp74(maxNumberProcesses.getValue(),
					vp.getPreferenceValue(OTHER_PROCESSES_INFO_LIMIT).clampedFloorIn(NUMBER_UNITY));
			return ResultBuilder.createFor(this, vp).setSeverity(Severity.get(score))
					.setSummary(Messages.getString(Messages.ManyRunningProcessesRule_TEXT_INFO))
					.setExplanation(Messages.getString(Messages.ManyRunningProcessesRule_TEXT_INFO_LONG))
					.setSolution(Messages.getString(Messages.ManyRunningProcessesRule_TEXT_RECOMMENDATION))
					.addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(score))
					.addResult(COMPETING_PROCESS_COUNT, UnitLookup.NUMBER_UNITY.quantity(maxNumberProcesses.getValue()))
					.addResult(COMPETING_PROCESS_TIME, maxNumberProcesses.getKey()).build();
		} else {
			return ResultBuilder.createFor(this, vp).setSeverity(Severity.NA).build();
		}
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
		return MANY_RUNNING_PROCESSES_RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.ManyRunningProcessesRule_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.PROCESSES;
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
