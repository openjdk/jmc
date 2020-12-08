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

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.Pair;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.AbstractRule;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.ResultBuilder;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.jdk.dataproviders.HaltsProvider;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.RequiredEventsBuilder;
import org.openjdk.jmc.flightrecorder.rules.util.SlidingWindowToolkit;

public class GcPauseRatioRule extends AbstractRule {

	private static final TypedPreference<IQuantity> INFO_LIMIT = new TypedPreference<>("gc.pauseratio.info.limit", //$NON-NLS-1$
			Messages.getString(Messages.GcPauseRatioRule_INFO_LIMIT),
			Messages.getString(Messages.GcPauseRatioRule_INFO_LIMIT_DESC), UnitLookup.PERCENTAGE,
			UnitLookup.PERCENT.quantity(5));
	private static final TypedPreference<IQuantity> WARNING_LIMIT = new TypedPreference<>("gc.pauseratio.warning.limit", //$NON-NLS-1$
			Messages.getString(Messages.GcPauseRatioRule_WARNING_LIMIT),
			Messages.getString(Messages.GcPauseRatioRule_WARNING_LIMIT_DESC), UnitLookup.PERCENTAGE,
			UnitLookup.PERCENT.quantity(10));
	private static final TypedPreference<IQuantity> WINDOW_SIZE = new TypedPreference<>("gc.pauseratio.window.size", //$NON-NLS-1$
			Messages.getString(Messages.GcPauseRatioRule_WINDOW_SIZE),
			Messages.getString(Messages.GcPauseRatioRule_WINDOW_SIZE_DESC), UnitLookup.TIMESPAN,
			UnitLookup.SECOND.quantity(60));

	private static final Collection<TypedPreference<?>> CONFIGURATION_ATTRIBUTES = Arrays
			.<TypedPreference<?>> asList(INFO_LIMIT, WARNING_LIMIT, WINDOW_SIZE);

	public static final TypedResult<IQuantity> GC_PAUSE_RATIO = new TypedResult<>("gcPauseRatio", "GC Pause Ratio", //$NON-NLS-1$
			"The percent of time spent in GC.", UnitLookup.PERCENTAGE, IQuantity.class);
	public static final TypedResult<IRange<IQuantity>> WINDOW = new TypedResult<>("gcPauseWindow", "GC Pause Window", //$NON-NLS-1$
			"The window reported for the gc pause ratio.", UnitLookup.TIMERANGE);
	public static final TypedResult<IQuantity> GC_PAUSE_RATIO_WINDOW = new TypedResult<>("gcPauseRatioWindow", //$NON-NLS-1$
			"GC Pause Ratio (Window)", "The gc pause ratio in the reported window.", UnitLookup.PERCENTAGE,
			IQuantity.class);

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.GC_PAUSE, EventAvailability.ENABLED).build();

	private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays
			.<TypedResult<?>> asList(TypedResult.SCORE, GC_PAUSE_RATIO, GC_PAUSE_RATIO_WINDOW, WINDOW);

	public GcPauseRatioRule() {
		super("GcPauseRatio", Messages.getString(Messages.GcPauseRatioRule_RULE_NAME), //$NON-NLS-1$
				JfrRuleTopics.GARBAGE_COLLECTION, CONFIGURATION_ATTRIBUTES, RESULT_ATTRIBUTES, REQUIRED_EVENTS);
	}

	@Override
	protected IResult getResult(IItemCollection items, IPreferenceValueProvider vp, IResultValueProvider rp) {
		IQuantity infoLimit = vp.getPreferenceValue(INFO_LIMIT);
		IQuantity warningLimit = vp.getPreferenceValue(WARNING_LIMIT);
		IQuantity windowSize = vp.getPreferenceValue(WINDOW_SIZE);

		IQuantity haltsTotalRatio = HaltsProvider.calculateGcPauseRatio(items);

		Pair<IQuantity, IRange<IQuantity>> haltsWindowRatio = SlidingWindowToolkit.slidingWindowUnorderedMinMaxValue(
				items, windowSize, evaluationTask, HaltsProvider.gcHaltsRatioFunction(), true, true);

		double score = RulesToolkit.mapExp100(haltsWindowRatio.left.doubleValue(), infoLimit.doubleValue(),
				warningLimit.doubleValue());
		String shortDescription = score >= 25 ? Messages.getString(Messages.GcPauseRatioRule_RULE_TEXT)
				: Messages.getString(Messages.GcPauseRatioRule_RULE_TEXT_OK);
		return ResultBuilder.createFor(this, vp).setSeverity(Severity.get(score)).setSummary(shortDescription)
				.setExplanation(Messages.getString(Messages.GcPauseRatioRule_RULE_TEXT_LONG))
				.setSolution(Messages.getString(Messages.GcPauseRatioRule_RULE_TEXT_RECOMMENDATION))
				.addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(score))
				.addResult(GC_PAUSE_RATIO, haltsTotalRatio).addResult(WINDOW, haltsWindowRatio.right)
				.addResult(GC_PAUSE_RATIO_WINDOW, haltsWindowRatio.left).build();
	}
}
