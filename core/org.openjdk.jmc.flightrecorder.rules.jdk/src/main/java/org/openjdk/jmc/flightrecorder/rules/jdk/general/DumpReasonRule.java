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

import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.item.ItemQueryBuilder;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

public class DumpReasonRule implements IRule {
	private static final String DUMP_REASON_RESULT_ID = "DumpReason"; //$NON-NLS-1$

	public static final TypedPreference<IQuantity> CRASH_SCORE = new TypedPreference<>("crash.score", //$NON-NLS-1$
			Messages.getString(Messages.DumpReasonRule_CRASH_SCORE),
			Messages.getString(Messages.DumpReasonRule_CRASH_SCORE_LONG), NUMBER, NUMBER_UNITY.quantity(100));
	public static final TypedPreference<IQuantity> COREDUMP_SCORE = new TypedPreference<>("coredump.score", //$NON-NLS-1$
			Messages.getString(Messages.DumpReasonRule_COREDUMP_SCORE),
			Messages.getString(Messages.DumpReasonRule_COREDUMP_SCORE_LONG), NUMBER, NUMBER_UNITY.quantity(90));
	public static final TypedPreference<IQuantity> OOM_SCORE = new TypedPreference<>("oom.score", //$NON-NLS-1$
			Messages.getString(Messages.DumpReasonRule_OOM_SCORE),
			Messages.getString(Messages.DumpReasonRule_OOM_SCORE_LONG), NUMBER, NUMBER_UNITY.quantity(80));

	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays.<TypedPreference<?>> asList(CRASH_SCORE,
			COREDUMP_SCORE, OOM_SCORE);

	private Result getResult(IItemCollection items, IPreferenceValueProvider valueProvider) {
		String eventType = JdkTypeIDs.DUMP_REASON;
		EventAvailability eventAvailability = RulesToolkit.getEventAvailability(items, eventType);
		if (eventAvailability == EventAvailability.AVAILABLE) {

			IQuantity crashScore = valueProvider.getPreferenceValue(CRASH_SCORE);
			IQuantity coredumpScore = valueProvider.getPreferenceValue(COREDUMP_SCORE);
			IQuantity oomScore = valueProvider.getPreferenceValue(OOM_SCORE);

			IItemFilter itemFilter = ItemFilters.type(eventType);
			IItemCollection filtered = items.apply(itemFilter);

			// FIXME: Will hopefully include "exceptional" boolean in the future
			String reasons = filtered
					.getAggregate(Aggregators.distinctAsString(JdkTypeIDs.DUMP_REASON, JdkAttributes.DUMP_REASON));
			double score;
			String longDescription;
			String shortDescription = Messages.getString(Messages.DumpReasonRule_TEXT_INFO);
			String reasonsLower = reasons != null ? reasons.toLowerCase() : ""; //$NON-NLS-1$
			if (reasonsLower.contains("crash")) { //$NON-NLS-1$
				score = crashScore.doubleValue();
				longDescription = Messages.getString(Messages.DumpReasonRule_TEXT_LONG_CRASH);
			} else if (reasonsLower.contains("core dump")) { //$NON-NLS-1$
				score = coredumpScore.doubleValue();
				longDescription = Messages.getString(Messages.DumpReasonRule_TEXT_LONG_COREDUMP);
			} else if (reasonsLower.contains("out of memory")) { //$NON-NLS-1$
				score = oomScore.doubleValue();
				longDescription = Messages.getString(Messages.DumpReasonRule_TEXT_LONG_OOM);
			} else {
				// FIXME: When all recordings have DumpReasons, we will be more sure of if an unknown reason is good or bad.
				score = 10;
				shortDescription = Messages.getString(Messages.DumpReasonRule_TEXT_INFO_UNKNOWN);
				longDescription = MessageFormat.format(Messages.getString(Messages.DumpReasonRule_TEXT_LONG_UNKNOWN),
						reasons);
			}
			return new Result(this, score, shortDescription, longDescription,
					ItemQueryBuilder.fromWhere(itemFilter).build());
		}
		if (eventAvailability == EventAvailability.DISABLED) {
			return RulesToolkit.getEventAvailabilityResult(this, items, eventAvailability, eventType);
		}

		return new Result(this, 0, Messages.getString(Messages.DumpReasonRule_TEXT_OK));
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
		return DUMP_REASON_RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.DumpReason_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.RECORDING_TOPIC;
	}
}
