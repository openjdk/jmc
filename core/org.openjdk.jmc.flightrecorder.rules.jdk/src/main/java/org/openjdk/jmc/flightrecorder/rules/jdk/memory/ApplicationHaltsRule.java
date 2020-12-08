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
package org.openjdk.jmc.flightrecorder.rules.jdk.memory;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.Pair;
import org.openjdk.jmc.common.util.StringToolkit;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.AbstractRule;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.ResultBuilder;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.jdk.dataproviders.HaltsProvider;
import org.openjdk.jmc.flightrecorder.rules.jdk.dataproviders.HaltsProvider.ApplicationHaltsInfoHolder;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.RequiredEventsBuilder;
import org.openjdk.jmc.flightrecorder.rules.util.SlidingWindowToolkit;

public class ApplicationHaltsRule extends AbstractRule {

	public static final TypedPreference<IQuantity> APP_HALTS_INFO_LIMIT = new TypedPreference<>("app.halts.info.limit", //$NON-NLS-1$
			Messages.getString(Messages.ApplicationHaltsRule_HALTS_INFO_LIMIT),
			Messages.getString(Messages.ApplicationHaltsRule_HALTS_INFO_LIMIT_DESC), UnitLookup.PERCENTAGE,
			UnitLookup.PERCENT.quantity(5));
	public static final TypedPreference<IQuantity> APP_HALTS_WARNING_LIMIT = new TypedPreference<>(
			"app.halts.warning.limit", Messages.getString(Messages.ApplicationHaltsRule_HALTS_WARNING_LIMIT), //$NON-NLS-1$
			Messages.getString(Messages.ApplicationHaltsRule_HALTS_WARNING_LIMIT_DESC), UnitLookup.PERCENTAGE,
			UnitLookup.PERCENT.quantity(10));
	public static final TypedPreference<IQuantity> WINDOW_SIZE = new TypedPreference<>("app.halts.window.size", //$NON-NLS-1$
			Messages.getString(Messages.ApplicationHaltsRule_HALTS_WINDOW_SIZE),
			Messages.getString(Messages.ApplicationHaltsRule_HALTS_WINDOW_SIZE_DESC), UnitLookup.TIMESPAN,
			UnitLookup.SECOND.quantity(60));

	private static final Collection<TypedPreference<?>> CONFIGURATION_ATTRIBUTES = Arrays
			.<TypedPreference<?>> asList(APP_HALTS_INFO_LIMIT, APP_HALTS_WARNING_LIMIT, WINDOW_SIZE);

	public static final TypedResult<IRange<IQuantity>> HALTS_WINDOW = new TypedResult<>("applicationsHaltsWindow", //$NON-NLS-1$
			"Halts Window", "The window during which the most application halts were detected.", UnitLookup.TIMERANGE);
	public static final TypedResult<IQuantity> HALTS_RATIO = new TypedResult<>("applicationHaltsRatio", "Halts Ratio", //$NON-NLS-1$
			"The percent of time spent halted.", UnitLookup.PERCENTAGE, IQuantity.class);
	public static final TypedResult<IQuantity> TOTAL_HALTS_RATIO = new TypedResult<>("totalApplicationHaltsRatio", //$NON-NLS-1$
			"Halts Ratio", "The percent of time spent halted during the entire recording.", UnitLookup.PERCENTAGE,
			IQuantity.class);
	public static final TypedResult<IQuantity> NON_GC_HALTS_RATIO = new TypedResult<>("nonGcApplicationHaltsRatio", //$NON-NLS-1$
			"Non-GC Halts Ratio", "The percent of time spent halted on activities other than garbage collection.",
			UnitLookup.PERCENTAGE, IQuantity.class);
	public static final TypedResult<IQuantity> TOTAL_NON_GC_HALTS_RATIO = new TypedResult<>(
			"totalNonGcApplicationHaltsRatio", //$NON-NLS-1$
			"Non-GC Halts Ratio",
			"The percent of time spent halted on activities other than garbage collection during the entire recording.",
			UnitLookup.PERCENTAGE, IQuantity.class);

	private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays.<TypedResult<?>> asList(
			TypedResult.SCORE, TOTAL_HALTS_RATIO, TOTAL_NON_GC_HALTS_RATIO, HALTS_RATIO, HALTS_WINDOW,
			NON_GC_HALTS_RATIO);

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.GC_PAUSE, EventAvailability.ENABLED)
			.addEventType(JdkTypeIDs.VM_OPERATIONS, EventAvailability.ENABLED).build();

	public ApplicationHaltsRule() {
		super("ApplicationHalts", Messages.getString(Messages.ApplicationHaltsRule_RULE_NAME), //$NON-NLS-1$
				JfrRuleTopics.JAVA_APPLICATION, CONFIGURATION_ATTRIBUTES, RESULT_ATTRIBUTES, REQUIRED_EVENTS);
	}

	@Override
	protected IResult getResult(IItemCollection items, IPreferenceValueProvider vp, IResultValueProvider rp) {
		String[] extraTypes = new String[] {JdkTypeIDs.SAFEPOINT_BEGIN};
		String extraTypesInfo = null;
		EventAvailability extraEventAvailability = RulesToolkit.getEventAvailability(items, extraTypes);
		if (!(extraEventAvailability == EventAvailability.AVAILABLE
				|| extraEventAvailability == EventAvailability.ENABLED)) {
			extraTypesInfo = MessageFormat.format(Messages.getString(Messages.ApplicationHaltsRule_EXTRA_EVENT_TYPES),
					StringToolkit.join(extraTypes, ", ")); //$NON-NLS-1$
		}

		IQuantity infoLimit = vp.getPreferenceValue(APP_HALTS_INFO_LIMIT);
		IQuantity warningLimit = vp.getPreferenceValue(APP_HALTS_WARNING_LIMIT);
		IQuantity windowSize = vp.getPreferenceValue(WINDOW_SIZE);

		ApplicationHaltsInfoHolder haltsRatios = HaltsProvider.calculateApplicationHaltsRatio(items);

		Pair<ApplicationHaltsInfoHolder, IRange<IQuantity>> haltsWindowRatio = SlidingWindowToolkit
				.slidingWindowUnorderedMinMaxValue(items, windowSize, evaluationTask,
						HaltsProvider.applicationHaltsRatioFunction(), applicationHaltsComparator(), true, true);
		IQuantity haltsTotalWindowRatio = haltsWindowRatio.left.getTotalHaltsRatio();
		IQuantity nonGcHaltsToTotalRatio = haltsWindowRatio.left.getNonGcHaltsToTotalRatio();

		double score = RulesToolkit.mapExp100(haltsTotalWindowRatio.doubleValue(), infoLimit.doubleValue(),
				warningLimit.doubleValue());
		String longDescription = Messages.getString(Messages.ApplicationHaltsRule_RULE_TEXT_LONG);
		String shortDescription;

		if (score >= 25) {
			shortDescription = Messages.getString(Messages.ApplicationHaltsRule_RULE_TEXT);
			longDescription += "\n" + Messages.getString(Messages.ApplicationHaltsRule_RULE_TEXT_RECOMMENDATION); //$NON-NLS-1$
		} else {
			shortDescription = Messages.getString(Messages.ApplicationHaltsRule_RULE_TEXT_OK);
		}
		if (extraTypesInfo != null) {
			longDescription += "\n" + extraTypesInfo; //$NON-NLS-1$
		}
		return ResultBuilder.createFor(this, vp).setSeverity(Severity.get(score)).setSummary(shortDescription)
				.setExplanation(longDescription).addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(score))
				.addResult(HALTS_RATIO, haltsTotalWindowRatio).addResult(HALTS_WINDOW, haltsWindowRatio.right)
				.addResult(TOTAL_NON_GC_HALTS_RATIO, haltsRatios.getNonGcHaltsToTotalRatio())
				.addResult(TOTAL_HALTS_RATIO, haltsRatios.getTotalHaltsRatio())
				.addResult(NON_GC_HALTS_RATIO, nonGcHaltsToTotalRatio).build();
	}

	private static Comparator<ApplicationHaltsInfoHolder> applicationHaltsComparator() {
		return new Comparator<HaltsProvider.ApplicationHaltsInfoHolder>() {

			@Override
			public int compare(ApplicationHaltsInfoHolder o1, ApplicationHaltsInfoHolder o2) {
				// NOTE: Make sure this uses the same halts value as is used for score
				return o1.getTotalHaltsRatio().compareTo(o2.getTotalHaltsRatio());
			}
		};
	}

}
