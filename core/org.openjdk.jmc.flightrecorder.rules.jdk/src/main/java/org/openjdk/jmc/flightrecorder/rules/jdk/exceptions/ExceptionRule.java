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
package org.openjdk.jmc.flightrecorder.rules.jdk.exceptions;

import static org.openjdk.jmc.common.unit.UnitLookup.EPOCH_NS;
import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER;
import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER_UNITY;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.KindOfQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkQueries;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.combine.SpanSquare;
import org.openjdk.jmc.flightrecorder.rules.jdk.combine.SpanToolkit;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

public class ExceptionRule implements IRule {

	private static final String RESULT_ID = "Exceptions"; //$NON-NLS-1$

	public static final TypedPreference<IQuantity> EXCEPTIONS_INFO_LIMIT = new TypedPreference<>("exception.info.limit", //$NON-NLS-1$
			Messages.getString(Messages.ExceptionRule_CONFIG_INFO_LIMIT),
			Messages.getString(Messages.ExceptionRule_CONFIG_INFO_LIMIT_LONG), NUMBER, NUMBER_UNITY.quantity(5000));
	public static final TypedPreference<IQuantity> EXCEPTIONS_WARNING_LIMIT = new TypedPreference<>(
			"exception.warning.limit", //$NON-NLS-1$
			Messages.getString(Messages.ExceptionRule_CONFIG_WARN_LIMIT),
			Messages.getString(Messages.ExceptionRule_CONFIG_WARN_LIMIT_LONG), NUMBER, NUMBER_UNITY.quantity(10000));

	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays
			.<TypedPreference<?>> asList(EXCEPTIONS_INFO_LIMIT, EXCEPTIONS_WARNING_LIMIT);

	private Result getResult(IItemCollection items, IPreferenceValueProvider vp) {
		EventAvailability eventAvailability = RulesToolkit.getEventAvailability(items,
				JdkTypeIDs.THROWABLES_STATISTICS);
		if (eventAvailability != EventAvailability.AVAILABLE) {
			return RulesToolkit.getEventAvailabilityResult(this, items, eventAvailability,
					JdkTypeIDs.THROWABLES_STATISTICS);
		}

		long infoLimit = vp.getPreferenceValue(EXCEPTIONS_INFO_LIMIT).clampedLongValueIn(NUMBER_UNITY);
		long warningLimit = vp.getPreferenceValue(EXCEPTIONS_WARNING_LIMIT).clampedLongValueIn(NUMBER_UNITY);

		// FIXME: Check EXCEPTIONS_THROWN event as well, and if so, consider adding an exclude list like for the error rule.
		SpanSquare maxExceptionPeriod = SpanToolkit.getMaxCountCluster(items.apply(JdkFilters.THROWABLES_STATISTICS),
				JdkAttributes.EXCEPTION_THROWABLES_COUNT, JfrAttributes.EVENT_TIMESTAMP);
		if (maxExceptionPeriod != null) {
			double duration = (maxExceptionPeriod.end - maxExceptionPeriod.start) / 1000000000.0;
			double exPerSec = maxExceptionPeriod.mass / duration;
			double score = RulesToolkit.mapExp100(exPerSec, infoLimit, warningLimit);

			String startTime = KindOfQuantity.format(maxExceptionPeriod.start, EPOCH_NS);
			String durationStr = KindOfQuantity.format(duration, UnitLookup.SECOND);
			String exPerSecStr = KindOfQuantity.format(exPerSec, UnitLookup.NUMBER_UNITY);

			String message = MessageFormat.format(Messages.getString(Messages.ExceptionRule_TEXT_MESSAGE), durationStr,
					startTime, exPerSecStr);
			String longMessage = null;
			if (score >= 25) {
				longMessage = message + "<p>" + Messages.getString(Messages.ExceptionRule_TEXT_INFO_LONG); //$NON-NLS-1$
				// FIXME: List most common exception if events are available
			}
			return new Result(this, score, message, longMessage, JdkQueries.THROWABLES_STATISTICS);
		}
		return RulesToolkit.getTooFewEventsResult(this);
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
		return RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.ExceptionRule_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.EXCEPTIONS_TOPIC;
	}
}
