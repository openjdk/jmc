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
package org.openjdk.jmc.flightrecorder.rules.jdk.exceptions;

import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER;
import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER_UNITY;
import static org.openjdk.jmc.common.unit.UnitLookup.PLAIN_TEXT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.collection.MapToolkit.IntEntry;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAggregator;
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
import org.openjdk.jmc.flightrecorder.rules.AbstractRule;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.ResultBuilder;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.RequiredEventsBuilder;
import org.openjdk.jmc.flightrecorder.rules.util.SlidingWindowToolkit;

public class ErrorRule extends AbstractRule {

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

	public static final TypedResult<IRange<IQuantity>> ERROR_WINDOW = new TypedResult<>("errorWindow", "Error Window", //$NON-NLS-1$
			"The window during which the rule detected the most errors.", UnitLookup.TIMERANGE);
	public static final TypedResult<IQuantity> ERROR_RATE = new TypedResult<>("errorRate", "Error Rate", //$NON-NLS-1$
			"The rate of errors created.", UnitLookup.NUMBER, IQuantity.class);
	public static final TypedResult<IQuantity> ERROR_COUNT = new TypedResult<>("errorCount", "Error Count", //$NON-NLS-1$
			"The total amount of errors created.", UnitLookup.NUMBER, IQuantity.class);
	public static final TypedResult<IMCType> MOST_COMMON_ERROR = new TypedResult<>("mostCommonError", //$NON-NLS-1$
			"Most Common Error", "The most common error thrown.", UnitLookup.CLASS, IMCType.class);
	public static final TypedResult<IQuantity> MOST_COMMON_ERROR_COUNT = new TypedResult<>("mostCommonErrorCount", //$NON-NLS-1$
			"Most Common Error Count", "The number of times the most common error type was thrown.", UnitLookup.NUMBER,
			IQuantity.class);
	public static final TypedResult<IQuantity> EXCLUDED_ERRORS = new TypedResult<>("excludedErrors", "Excluded Errors", //$NON-NLS-1$
			"The number of errors excluded from the rule evaluation.", UnitLookup.NUMBER, IQuantity.class);

	private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays.<TypedResult<?>> asList(
			TypedResult.SCORE, ERROR_COUNT, EXCLUDED_ERRORS, ERROR_RATE, ERROR_WINDOW, MOST_COMMON_ERROR,
			MOST_COMMON_ERROR_COUNT);

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.ERRORS_THROWN, EventAvailability.AVAILABLE).build();

	@Override
	protected IResult getResult(IItemCollection items, IPreferenceValueProvider vp, IResultValueProvider rp) {
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

			excludedErrors = items.getAggregate((IAggregator<IQuantity, ?>) Aggregators.filter(Aggregators.count(),
					ItemFilters.and(ItemFilters.type(JdkTypeIDs.ERRORS_THROWN),
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
			IMCType mostCommonError = errorGrouping.get(errorGrouping.size() - 1).getKey();
			int errorsThrown = errorGrouping.get(errorGrouping.size() - 1).getValue();
			double score = RulesToolkit.mapExp100(maxErrorsPerMinute.left.doubleValue(), infoLimit, warnLimit);
			String longMessage = Messages.getString(Messages.ErrorRule_TEXT_WARN_LONG);
			// FIXME: List some frames of the most common stack trace
			if (excludedErrors != null && excludedErrors.longValue() > 0) {
				longMessage += " " + Messages.getString(Messages.ErrorRule_TEXT_WARN_EXCLUDED_INFO); //$NON-NLS-1$
			}
			return ResultBuilder.createFor(this, vp).setSeverity(Severity.get(score))
					.setSummary(Messages.getString(Messages.ErrorRule_TEXT_WARN)).setExplanation(longMessage)
					.addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(score))
					.addResult(ERROR_COUNT, errorCount).addResult(ERROR_WINDOW, maxErrorsPerMinute.right)
					.addResult(ERROR_RATE, maxErrorsPerMinute.left).addResult(MOST_COMMON_ERROR, mostCommonError)
					.addResult(EXCLUDED_ERRORS, excludedErrors).addResult(TypedResult.ITEM_QUERY, JdkQueries.ERRORS)
					.addResult(MOST_COMMON_ERROR_COUNT, UnitLookup.NUMBER_UNITY.quantity(errorsThrown)).build();
		}
		return ResultBuilder.createFor(this, vp).setSeverity(Severity.OK)
				.setSummary(Messages.getString(Messages.ErrorRule_TEXT_OK)).build();
	}

	public ErrorRule() {
		super(RESULT_ID, Messages.getString(Messages.ErrorRule_RULE_NAME), JfrRuleTopics.EXCEPTIONS, CONFIG_ATTRIBUTES,
				RESULT_ATTRIBUTES, REQUIRED_EVENTS);
	}
}
