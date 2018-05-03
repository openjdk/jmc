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
package org.openjdk.jmc.flightrecorder.rules.jdk.cpu;

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
import org.openjdk.jmc.common.collection.MapToolkit.IntEntry;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

public class ManyRunningProcessesRule implements IRule {
	private static final String MANY_RUNNING_PROCESSES_RESULT_ID = "ManyRunningProcesses"; //$NON-NLS-1$

	public static final TypedPreference<IQuantity> OTHER_PROCESSES_INFO_LIMIT = new TypedPreference<>(
			"other.processes.info.limit", //$NON-NLS-1$
			Messages.getString(Messages.ManyRunningProcessesRule_INFO_LIMIT),
			Messages.getString(Messages.ManyRunningProcessesRule_INFO_LIMIT_LONG), NUMBER, NUMBER_UNITY.quantity(150));

	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays
			.<TypedPreference<?>> asList(OTHER_PROCESSES_INFO_LIMIT);

	private Result getResult(IItemCollection items, IPreferenceValueProvider vp) {
		EventAvailability eventAvailability = RulesToolkit.getEventAvailability(items, JdkTypeIDs.PROCESSES);
		if (eventAvailability != EventAvailability.AVAILABLE) {
			return RulesToolkit.getEventAvailabilityResult(this, items, eventAvailability, JdkTypeIDs.PROCESSES);
		}
		// FIXME: Can we really be sure that 'concurrent' events have the exact same timestamp?
		List<IntEntry<IQuantity>> entries = RulesToolkit.calculateGroupingScore(items.apply(JdkFilters.PROCESSES),
				JfrAttributes.END_TIME);
		IntEntry<IQuantity> maxNumberProcesses = entries.get(entries.size() - 1);
		double score = RulesToolkit.mapExp74(maxNumberProcesses.getValue(),
				vp.getPreferenceValue(OTHER_PROCESSES_INFO_LIMIT).clampedFloorIn(NUMBER_UNITY));
		String shortMessage = MessageFormat.format(Messages.getString(Messages.ManyRunningProcessesRule_TEXT_INFO),
				maxNumberProcesses.getValue(), maxNumberProcesses.getKey().displayUsing(IDisplayable.AUTO));
		String longMessage = MessageFormat.format(Messages.getString(Messages.ManyRunningProcessesRule_TEXT_INFO_LONG),
				maxNumberProcesses.getValue(), maxNumberProcesses.getKey().displayUsing(IDisplayable.AUTO));
		if (score >= 25) {
			longMessage = longMessage + " " + Messages.getString(Messages.ManyRunningProcessesRule_TEXT_RECOMMENDATION); //$NON-NLS-1$
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
		return JfrRuleTopics.PROCESSES_TOPIC;
	}
}
