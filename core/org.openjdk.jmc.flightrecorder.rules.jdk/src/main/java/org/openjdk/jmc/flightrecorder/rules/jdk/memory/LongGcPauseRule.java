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

import static org.openjdk.jmc.common.unit.UnitLookup.MILLISECOND;
import static org.openjdk.jmc.common.unit.UnitLookup.TIMESPAN;

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
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

public class LongGcPauseRule implements IRule {

	private static final String RESULT_ID = "LongGcPause"; //$NON-NLS-1$

	public static final TypedPreference<IQuantity> GC_PAUSE_INFO_LIMIT = new TypedPreference<>("gc.pause.info.limit", //$NON-NLS-1$
			Messages.getString(Messages.LongGcPauseRule_CONFIG_INFO_LIMIT),
			Messages.getString(Messages.LongGcPauseRule_CONFIG_INFO_LIMIT_LONG), TIMESPAN,
			UnitLookup.MILLISECOND.quantity(1000));

	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays
			.<TypedPreference<?>> asList(GC_PAUSE_INFO_LIMIT);

	private Result getResult(IItemCollection items, IPreferenceValueProvider vp) {
		EventAvailability eventAvailability = RulesToolkit.getEventAvailability(items, JdkTypeIDs.GC_PAUSE,
				JdkTypeIDs.GC_CONF, JdkTypeIDs.HEAP_CONF, JdkTypeIDs.GC_PAUSE_L1);
		if (eventAvailability == EventAvailability.DISABLED || eventAvailability == EventAvailability.UNKNOWN) {
			return RulesToolkit.getEventAvailabilityResult(this, items, EventAvailability.DISABLED, JdkTypeIDs.GC_PAUSE,
					JdkTypeIDs.GC_CONF, JdkTypeIDs.HEAP_CONF, JdkTypeIDs.GC_PAUSE_L1);
		}
		IQuantity maxPause = items.getAggregate(JdkAggregators.LONGEST_GC_PAUSE);
		if (maxPause != null) {
			String message = MessageFormat.format(Messages.getString(Messages.LongGcPauseRuleFactory_TEXT_INFO),
					maxPause.displayUsing(IDisplayable.AUTO));
			double gcPauseScore = RulesToolkit.mapExp74(maxPause.doubleValueIn(MILLISECOND),
					vp.getPreferenceValue(GC_PAUSE_INFO_LIMIT).doubleValueIn(MILLISECOND));
			String longMessage = message;
			if (gcPauseScore >= 25) {
				longMessage = appendMessage(longMessage, getLivesetMessage(items));
				longMessage = appendMessage(longMessage, getSemiRefsMessage(items));
				longMessage = appendMessage(longMessage, getCollectorMessage(items));
			}
			return new Result(this, gcPauseScore, message, longMessage);
		}
		return new Result(this, 0, Messages.getString(Messages.LongGcPauseRuleFactory_TEXT_OK));
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

	private static String appendMessage(String message, String newMessage) {
		if (message == null) {
			return newMessage;
		} else if (newMessage == null) {
			return message;
		} else {
			return message + " " + newMessage; //$NON-NLS-1$
		}
	}

	private static String getCollectorMessage(IItemCollection items) {
		CollectorType oldCollectorType = CollectorType.getOldCollectorType(items);
		if (oldCollectorType != CollectorType.G1_OLD) {
			return Messages.getString(Messages.LongGcPauseRuleFactory_TEXT_INFO_G1);
		}
		return null;
	}

	private static String getLivesetMessage(IItemCollection items) {
		IQuantity liveSet = items.getAggregate(JdkAggregators.AVG_HEAP_USED_AFTER_GC);
		IQuantity maxMx = items.getAggregate(JdkAggregators.HEAP_CONF_MAX_SIZE);
		// If liveset is low ( < 50% ), suggest lowering mx.
		if (liveSet != null && maxMx != null) {
			int live = (int) (liveSet.ratioTo(maxMx) * 100);
			if (live < 50) {
				return MessageFormat.format(Messages.getString(Messages.LongGcPauseRuleFactory_TEXT_INFO_MX),
						liveSet.displayUsing(IDisplayable.AUTO), maxMx.displayUsing(IDisplayable.AUTO));
			}
		}
		return null;
	}

	private static String getSemiRefsMessage(IItemCollection items) {
		IQuantity aggregate = items
				.getAggregate(Aggregators.filter(Aggregators.max(JdkTypeIDs.GC_PAUSE_L1, JfrAttributes.DURATION),
						ItemFilters.equals(JdkAttributes.GC_PHASE_NAME, "References"))); //$NON-NLS-1$
		if (aggregate == null) {
			return null;
		}
		int max = aggregate.compareTo(UnitLookup.MILLISECOND.quantity(50));
		if (max >= 50) { // if semirefs part of a gc takes longer than 50ms then we should inform the user
			return Messages.getString(Messages.LongGcPauseRuleFactory_TEXT_INFO_REFERENCES);
		}
		return null;
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
		return Messages.getString(Messages.LongGcPauseRuleFactory_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.GARBAGE_COLLECTION_TOPIC;
	}
}
