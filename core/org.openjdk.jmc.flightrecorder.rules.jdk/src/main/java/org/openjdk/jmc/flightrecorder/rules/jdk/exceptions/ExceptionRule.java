/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.rules.jdk.exceptions;

import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER;
import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER_UNITY;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.collection.MapToolkit.IntEntry;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.QuantityRange;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkQueries;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.ResultBuilder;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.jdk.combine.SpanSquare;
import org.openjdk.jmc.flightrecorder.rules.jdk.combine.SpanToolkit;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.RequiredEventsBuilder;

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

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.THROWABLES_STATISTICS, EventAvailability.AVAILABLE).build();

	public static final TypedResult<IQuantity> EXCEPTION_RATE = new TypedResult<>("exceptionsRate", "Exception Rate", //$NON-NLS-1$
			"The rate of exceptions thrown per minute.", UnitLookup.NUMBER, IQuantity.class);
	public static final TypedResult<IRange<IQuantity>> EXCEPTION_WINDOW = new TypedResult<>("exceptionsWindow", //$NON-NLS-1$
			"Exception Window", "The window during which the highest exception rate was detected.",
			UnitLookup.TIMERANGE);
	public static final TypedResult<IMCType> MOST_COMMON_EXCEPTION = new TypedResult<>("mostCommonException", //$NON-NLS-1$
			"Most Common Exception", "The most common exception thrown.", UnitLookup.CLASS, IMCType.class);
	public static final TypedResult<String> MOST_COMMON_EXCEPTION_MESSAGE = new TypedResult<>(
			"mostCommonExceptionMessage", //$NON-NLS-1$
			"Most Common Exception Message", "The most common exception message.", UnitLookup.PLAIN_TEXT, String.class);
	public static final TypedResult<String> MOST_COMMON_EXCEPTION_STACKTRACE = new TypedResult<>(
			"mostCommonExceptionStacktrace", "Most Common Exception Stacktrace", //$NON-NLS-1$
			"The most common exception stacktrace frames.", UnitLookup.PLAIN_TEXT, String.class);

	private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays.<TypedResult<?>> asList(
			TypedResult.SCORE, EXCEPTION_RATE, EXCEPTION_WINDOW, MOST_COMMON_EXCEPTION, MOST_COMMON_EXCEPTION_MESSAGE,
			MOST_COMMON_EXCEPTION_STACKTRACE);

	private IResult getResult(IItemCollection items, IPreferenceValueProvider vp, IResultValueProvider rp) {
		long infoLimit = vp.getPreferenceValue(EXCEPTIONS_INFO_LIMIT).clampedLongValueIn(NUMBER_UNITY);
		long warningLimit = vp.getPreferenceValue(EXCEPTIONS_WARNING_LIMIT).clampedLongValueIn(NUMBER_UNITY);

		// FIXME: Check EXCEPTIONS_THROWN event as well, and if so, consider adding an exclude list like for the error rule.
		SpanSquare maxExceptionPeriod = SpanToolkit.getMaxCountCluster(items.apply(JdkFilters.THROWABLES_STATISTICS),
				JdkAttributes.EXCEPTION_THROWABLES_COUNT, JfrAttributes.EVENT_TIMESTAMP);
		if (maxExceptionPeriod != null) {
			double duration = (maxExceptionPeriod.end - maxExceptionPeriod.start) / 1000000000.0;
			double exPerSec = maxExceptionPeriod.mass / duration;
			double score = RulesToolkit.mapExp100(exPerSec, infoLimit, warningLimit);
			IRange<IQuantity> window = QuantityRange.createWithEnd(
					UnitLookup.EPOCH_NS.quantity(maxExceptionPeriod.start),
					UnitLookup.EPOCH_NS.quantity(maxExceptionPeriod.end));
			ResultBuilder resultBuilder = ResultBuilder.createFor(this, vp);
			String explanation = Messages.getString(Messages.ExceptionRule_TEXT_INFO_LONG);
			EventAvailability exceptionsThrownEventAvailability = RulesToolkit.getEventAvailability(items,
					JdkTypeIDs.EXCEPTIONS_THROWN);
			if (exceptionsThrownEventAvailability == EventAvailability.AVAILABLE) {
				IItemCollection exceptionItems = items.apply(JdkFilters.EXCEPTIONS);
				List<IntEntry<IMCType>> exceptionGrouping = RulesToolkit.calculateGroupingScore(exceptionItems,
						JdkAttributes.EXCEPTION_THROWNCLASS);
				IMCType mostCommonException = exceptionGrouping.get(exceptionGrouping.size() - 1).getKey();
				explanation = Messages.getString(Messages.ExceptionRule_TEXT_MOST_COMMON_EXCEPTION) + explanation;
				resultBuilder.addResult(MOST_COMMON_EXCEPTION, mostCommonException);
				if (mostCommonException != null) {
					IItemCollection mostCommonExceptionItems = exceptionItems
							.apply(ItemFilters.equals(JdkAttributes.EXCEPTION_THROWNCLASS, mostCommonException));
					IItemCollection itemsWithMessage = mostCommonExceptionItems
							.apply(ItemFilters.notEquals(JdkAttributes.EXCEPTION_MESSAGE, null));
					if (itemsWithMessage.hasItems()) {
						List<IntEntry<String>> mostCommonExceptionMessageGrouping = RulesToolkit
								.calculateGroupingScore(itemsWithMessage, JdkAttributes.EXCEPTION_MESSAGE);
						String mostCommonExceptionMessage = mostCommonExceptionMessageGrouping
								.get(mostCommonExceptionMessageGrouping.size() - 1).getKey();
						explanation += "\n"
								+ Messages.getString(Messages.ExceptionRule_TEXT_MOST_COMMON_EXCEPTION_MESSAGE);
						resultBuilder.addResult(MOST_COMMON_EXCEPTION_MESSAGE, mostCommonExceptionMessage);
					}
					IItemCollection itemsWithStackTrace = mostCommonExceptionItems
							.apply(ItemFilters.notEquals(JfrAttributes.EVENT_STACKTRACE, null));
					if (itemsWithStackTrace.hasItems()) {
						String mostCommonExceptionstackTraceFrames = RulesToolkit
								.getTopNFramesInMostCommonTrace(itemsWithStackTrace, 10);
						explanation += "\n"
								+ Messages.getString(Messages.ExceptionRule_TEXT_MOST_COMMON_EXCEPTION_STACKTRACE);
						resultBuilder.addResult(MOST_COMMON_EXCEPTION_STACKTRACE, mostCommonExceptionstackTraceFrames);
					}
				}
			}
			return resultBuilder.setSeverity(Severity.get(score))
					.setSummary(Messages.getString(Messages.ExceptionRule_TEXT_MESSAGE)).setExplanation(explanation)
					.addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(score))
					.addResult(EXCEPTION_RATE, UnitLookup.NUMBER_UNITY.quantity(exPerSec))
					.addResult(TypedResult.ITEM_QUERY, JdkQueries.THROWABLES_STATISTICS)
					.addResult(EXCEPTION_WINDOW, window).build();
		}
		return RulesToolkit.getTooFewEventsResult(this, vp);
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
		return RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.ExceptionRule_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.EXCEPTIONS;
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
