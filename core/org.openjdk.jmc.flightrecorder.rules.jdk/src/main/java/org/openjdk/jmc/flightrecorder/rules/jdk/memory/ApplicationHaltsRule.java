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

import java.text.MessageFormat;
import java.util.Comparator;

import org.openjdk.jmc.common.IDisplayable;
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
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.dataproviders.HaltsProvider;
import org.openjdk.jmc.flightrecorder.rules.jdk.dataproviders.HaltsProvider.ApplicationHaltsInfoHolder;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
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

	public ApplicationHaltsRule() {
		super("ApplicationHalts", Messages.getString(Messages.ApplicationHaltsRule_RULE_NAME), //$NON-NLS-1$
				JfrRuleTopics.JAVA_APPLICATION_TOPIC, APP_HALTS_INFO_LIMIT, APP_HALTS_WARNING_LIMIT, WINDOW_SIZE);
	}

	@Override
	protected Result getResult(IItemCollection items, IPreferenceValueProvider vp) {
		String[] requiredTypes = new String[] {JdkTypeIDs.GC_PAUSE, JdkTypeIDs.VM_OPERATIONS};
		String[] extraTypes = new String[] {JdkTypeIDs.SAFEPOINT_BEGIN};
		EventAvailability eventAvailability = RulesToolkit.getEventAvailability(items, requiredTypes);
		if (!(eventAvailability == EventAvailability.AVAILABLE || eventAvailability == EventAvailability.ENABLED)) {
			return RulesToolkit.getEventAvailabilityResult(this, items, eventAvailability, requiredTypes);
		}
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
		String startTimeString = haltsWindowRatio.right.getStart().displayUsing(IDisplayable.AUTO);
		String durationString = haltsWindowRatio.right.getExtent().displayUsing(IDisplayable.AUTO);
		String longDescription = MessageFormat.format(Messages.getString(Messages.ApplicationHaltsRule_RULE_TEXT_LONG),
				haltsTotalWindowRatio, durationString, startTimeString, nonGcHaltsToTotalRatio,
				haltsRatios.getTotalHaltsRatio(), haltsRatios.getNonGcHaltsToTotalRatio());
		String shortDescription;

		if (score >= 25) {
			shortDescription = Messages.getString(Messages.ApplicationHaltsRule_RULE_TEXT);
			longDescription += "<p>" + Messages.getString(Messages.ApplicationHaltsRule_RULE_TEXT_RECOMMENDATION); //$NON-NLS-1$
		} else {
			shortDescription = Messages.getString(Messages.ApplicationHaltsRule_RULE_TEXT_OK);
		}
		if (extraTypesInfo != null) {
			longDescription += "<p>" + extraTypesInfo; //$NON-NLS-1$
		}
		longDescription = shortDescription + "<p>" + longDescription; //$NON-NLS-1$
		return new Result(this, score, shortDescription, longDescription);
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
