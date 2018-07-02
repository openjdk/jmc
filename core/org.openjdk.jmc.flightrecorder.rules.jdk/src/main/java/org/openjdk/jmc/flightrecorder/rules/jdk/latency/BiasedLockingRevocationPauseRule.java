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
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

public final class BiasedLockingRevocationPauseRule implements IRule {

	public static final TypedPreference<IQuantity> INFO_LIMIT = new TypedPreference<>(
			"vm.biasedrevocationpause.info.limit", //$NON-NLS-1$
			Messages.getString(Messages.BiasedLockingRevocationPauseRule_CONFIG_WARNING_LIMIT),
			Messages.getString(Messages.BiasedLockingRevocationPauseRule_CONFIG_WARNING_LIMIT_LONG),
			UnitLookup.TIMESPAN, UnitLookup.MILLISECOND.quantity(100));

	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays.<TypedPreference<?>> asList(INFO_LIMIT);

	private Result getResult(IItemCollection items, IPreferenceValueProvider valueProvider) {
		EventAvailability eventAvailability = RulesToolkit.getEventAvailability(items, JdkTypeIDs.VM_OPERATIONS);
		if (eventAvailability == EventAvailability.UNKNOWN || eventAvailability == EventAvailability.DISABLED) {
			return RulesToolkit.getEventAvailabilityResult(this, items, eventAvailability, JdkTypeIDs.VM_OPERATIONS);
		}

		IItemCollection revocationEvents = items.apply(
				ItemFilters.and(JdkFilters.VM_OPERATIONS, ItemFilters.matches(JdkAttributes.OPERATION, "RevokeBias"))); //$NON-NLS-1$
		if (!revocationEvents.hasItems()) {
			return new Result(this, 0, Messages.getString(Messages.BiasedLockingRevocationPauseRule_TEXT_OK));
		}
		IQuantity timeSpentRevoking = revocationEvents.hasItems()
				? revocationEvents.getAggregate(Aggregators.sum(JfrAttributes.DURATION))
				: UnitLookup.MILLISECOND.quantity(0);
		double mappedDuration = RulesToolkit.mapExp100Y(timeSpentRevoking.doubleValueIn(UnitLookup.MILLISECOND),
				valueProvider.getPreferenceValue(INFO_LIMIT).doubleValueIn(UnitLookup.MILLISECOND), 25);
		String shortMessage = MessageFormat.format(
				Messages.getString(Messages.BiasedLockingRevocationPauseRule_TEXT_MESSAGE),
				timeSpentRevoking.displayUsing(IDisplayable.AUTO));
		String longMessage = shortMessage;
		if (mappedDuration >= 25) {
			longMessage = longMessage + "<p>" //$NON-NLS-1$
					+ Messages.getString(Messages.BiasedLockingRevocationPauseRule_TEXT_INFO_LONG);
		}
		return new Result(this, mappedDuration, shortMessage, longMessage);
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
		return "biasedLockingRevocationPause"; //$NON-NLS-1$
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.BiasedLockingRevocationPauseRule_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.VM_OPERATIONS_TOPIC;
	}

}
