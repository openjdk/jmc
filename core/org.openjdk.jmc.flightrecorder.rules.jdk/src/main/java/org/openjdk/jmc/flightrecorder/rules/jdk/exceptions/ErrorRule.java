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

import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER;
import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER_UNITY;
import static org.openjdk.jmc.common.unit.UnitLookup.PLAIN_TEXT;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.owasp.encoder.Encode;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.collection.MapToolkit.IntEntry;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.QuantityRange;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.Pair;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkQueries;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.rules.util.SlidingWindowToolkit;

public class ErrorRule implements IRule {

	private static final String RESULT_ID = "Errors"; //$NON-NLS-1$

	public static final TypedPreference<IQuantity> ERROR_INFO_LIMIT = new TypedPreference<>("error.info.limit", //$NON-NLS-1$
			Messages.getString(Messages.ErrorRule_CONFIG_INFO_LIMIT),
			Messages.getString(Messages.ErrorRule_CONFIG_INFO_LIMIT_LONG), NUMBER, NUMBER_UNITY.quantity(30));
	public static final TypedPreference<IQuantity> ERROR_WARNING_LIMIT = new TypedPreference<>("error.warning.limit", //$NON-NLS-1$
			Messages.getString(Messages.ErrorRule_CONFIG_WARN_LIMIT),
			Messages.getString(Messages.ErrorRule_CONFIG_WARN_LIMIT_LONG), NUMBER, NUMBER_UNITY.quantity(60));
	public static final TypedPreference<String> EXCLUDED_ERRORS_REGEXP = new TypedPreference<>("error.exclude.regexp", //$NON-NLS-1$
			Messages.getString(Messages.ErrorRule_CONFIG_EXCLUDED_ERRORS),
			Messages.getString(Messages.ErrorRule_CONFIG_EXCLUDED_ERRORS_LONG), PLAIN_TEXT.getPersister(),
			"(com.sun.el.parser.ELParser\\$LookaheadSuccess)"); //$NON-NLS-1$
	public static final TypedPreference<IQuantity> ERROR_WINDOW_SIZE = new TypedPreference<>("error.window.size", //$NON-NLS-1$
			Messages.getString(Messages.ErrorRule_CONFIG_WINDOW_SIZE),
			Messages.getString(Messages.ErrorRule_CONFIG_WINDOW_SIZE_LONG), UnitLookup.TIMESPAN,
			UnitLookup.SECOND.quantity(60));

	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays.<TypedPreference<?>> asList(
			ERROR_INFO_LIMIT, ERROR_WARNING_LIMIT, EXCLUDED_ERRORS_REGEXP, ERROR_WINDOW_SIZE);

	private FutureTask<Result> evaluationTask;

	private Result getResult(IItemCollection items, IPreferenceValueProvider vp) {
		EventAvailability eventAvailability = RulesToolkit.getEventAvailability(items, JdkTypeIDs.ERRORS_THROWN);
		if (eventAvailability != EventAvailability.AVAILABLE) {
			return RulesToolkit.getEventAvailabilityResult(this, items, eventAvailability, JdkTypeIDs.ERRORS_THROWN);
		}

		long warnLimit = vp.getPreferenceValue(ERROR_WARNING_LIMIT).clampedLongValueIn(NUMBER_UNITY);
		long infoLimit = vp.getPreferenceValue(ERROR_INFO_LIMIT).clampedLongValueIn(NUMBER_UNITY);
		String errorExcludeRegexp = vp.getPreferenceValue(EXCLUDED_ERRORS_REGEXP).trim();

		IItemCollection errorItems = items;
		IQuantity excludedErrors = null;
		if (!errorExcludeRegexp.isEmpty()) {
			IItemFilter matchesExclude = ItemFilters.matches(JdkAttributes.EXCEPTION_THROWNCLASS_NAME,
					errorExcludeRegexp);
			IItemFilter errorsExcludingExclude = ItemFilters.and(ItemFilters.type(JdkTypeIDs.ERRORS_THROWN),
					ItemFilters.not(matchesExclude));
			errorItems = errorItems.apply(errorsExcludingExclude);
			excludedErrors = items.getAggregate(
					Aggregators.filter(Aggregators.count(), ItemFilters.and(ItemFilters.type(JdkTypeIDs.ERRORS_THROWN),
							ItemFilters.matches(JdkAttributes.EXCEPTION_THROWNCLASS_NAME, errorExcludeRegexp))));
		}
		IQuantity errorCount = errorItems.getAggregate(JdkAggregators.ERROR_COUNT);
		if (errorCount != null && errorCount.doubleValue() > 0) {
			final List<Pair<IQuantity, IRange<IQuantity>>> errorsList = new ArrayList<>();
			IQuantity windowSize = vp.getPreferenceValue(ERROR_WINDOW_SIZE);
			IQuantity slideSize = windowSize.getUnit().quantity(windowSize.ratioTo(windowSize.getUnit().quantity(2)));
			SlidingWindowToolkit.slidingWindowUnordered(new SlidingWindowToolkit.IUnorderedWindowVisitor() {
				@Override
				public void visitWindow(IItemCollection items, IQuantity startTime, IQuantity endTime) {
					IRange<IQuantity> timeRange = QuantityRange.createWithEnd(startTime, endTime);
					Double errors = items.getAggregate(Aggregators.count()).doubleValue();
					if (errors > 0) {
						IQuantity errorsPerMinute = UnitLookup.NUMBER_UNITY
								.quantity(errors / timeRange.getExtent().doubleValueIn(UnitLookup.MINUTE));
						errorsList.add(new Pair<>(errorsPerMinute, timeRange));
					}
				}

				@Override
				public boolean shouldContinue() {
					return !evaluationTask.isCancelled();
				}
			}, errorItems, windowSize, slideSize);
			Pair<IQuantity, IRange<IQuantity>> maxErrorsPerMinute = Collections.max(errorsList,
					new Comparator<Pair<IQuantity, IRange<IQuantity>>>() {
						@Override
						public int compare(
							Pair<IQuantity, IRange<IQuantity>> o1, Pair<IQuantity, IRange<IQuantity>> o2) {
							return o1.left.compareTo(o2.left);
						}
					});
			List<IntEntry<IMCType>> errorGrouping = RulesToolkit.calculateGroupingScore(errorItems,
					JdkAttributes.EXCEPTION_THROWNCLASS);
			String mostCommonError = Encode.forHtml(errorGrouping.get(errorGrouping.size() - 1).getKey().getFullName());
			int errorsThrown = errorGrouping.get(errorGrouping.size() - 1).getValue();
			double score = RulesToolkit.mapExp100(maxErrorsPerMinute.left.doubleValue(), infoLimit, warnLimit);
			String shortMessage = MessageFormat.format(Messages.getString(Messages.ErrorRule_TEXT_WARN),
					maxErrorsPerMinute.left.displayUsing(IDisplayable.AUTO),
					maxErrorsPerMinute.right.displayUsing(IDisplayable.AUTO));
			String longMessage = MessageFormat.format(Messages.getString(Messages.ErrorRule_TEXT_WARN_LONG),
					maxErrorsPerMinute.left.displayUsing(IDisplayable.AUTO),
					maxErrorsPerMinute.right.displayUsing(IDisplayable.AUTO), errorCount, mostCommonError,
					errorsThrown);
			// FIXME: List some frames of the most common stack trace
			if (excludedErrors != null && excludedErrors.longValue() > 0) {
				longMessage += " " + MessageFormat.format( //$NON-NLS-1$
						Messages.getString(Messages.ErrorRule_TEXT_WARN_EXCLUDED_INFO), errorExcludeRegexp,
						excludedErrors);
			}
			return new Result(this, score, shortMessage, longMessage, JdkQueries.ERRORS);
		}
		return new Result(this, 0, Messages.getString(Messages.ErrorRule_TEXT_OK));
	}

	@Override
	public RunnableFuture<Result> evaluate(final IItemCollection items, final IPreferenceValueProvider valueProvider) {
		evaluationTask = new FutureTask<>(new Callable<Result>() {
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
		return Messages.getString(Messages.ErrorRule_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.EXCEPTIONS_TOPIC;
	}
}
