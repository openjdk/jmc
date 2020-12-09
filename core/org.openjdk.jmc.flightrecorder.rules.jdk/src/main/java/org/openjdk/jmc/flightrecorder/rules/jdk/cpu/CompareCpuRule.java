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
package org.openjdk.jmc.flightrecorder.rules.jdk.cpu;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.QuantityRange;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.AbstractRule;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.ResultBuilder;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.jdk.combine.SpanLimit;
import org.openjdk.jmc.flightrecorder.rules.jdk.combine.SpanToolkit;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.RequiredEventsBuilder;

public class CompareCpuRule extends AbstractRule {
	public static final TypedPreference<IQuantity> OTHER_CPU_WARNING_LIMIT = new TypedPreference<>(
			"other.cpu.warning.limit", //$NON-NLS-1$
			Messages.getString(Messages.CompareCpuRule_WARNING_LIMIT),
			Messages.getString(Messages.CompareCpuRule_WARNING_LIMIT_LONG), UnitLookup.PERCENTAGE,
			UnitLookup.PERCENT.quantity(40));
	public static final TypedPreference<IQuantity> OTHER_CPU_INFO_LIMIT = new TypedPreference<>("other.cpu.info.limit", //$NON-NLS-1$
			Messages.getString(Messages.CompareCpuRule_INFO_LIMIT),
			Messages.getString(Messages.CompareCpuRule_INFO_LIMIT_LONG), UnitLookup.PERCENTAGE,
			UnitLookup.PERCENT.quantity(20));

	private static final Collection<TypedPreference<?>> CONFIGURATION_ATTRIBUTES = Arrays
			.<TypedPreference<?>> asList(OTHER_CPU_INFO_LIMIT, OTHER_CPU_WARNING_LIMIT);

	public static final TypedResult<IQuantity> AVERAGE_CPU_LOAD = new TypedResult<>("avgCpuLoad", "Average CPU Load", //$NON-NLS-1$
			"The average CPU load detected.", UnitLookup.PERCENTAGE, IQuantity.class);
	public static final TypedResult<IRange<IQuantity>> AVERAGE_CPU_LOAD_WINDOW = new TypedResult<>("avgCpuLoadWindow", //$NON-NLS-1$
			"Average CPU Load Window", "The window during which the high CPU load was detected.", UnitLookup.TIMERANGE);

	private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays
			.<TypedResult<?>> asList(TypedResult.SCORE, AVERAGE_CPU_LOAD, AVERAGE_CPU_LOAD_WINDOW);

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.CPU_LOAD, EventAvailability.AVAILABLE).build();

	public CompareCpuRule() {
		super("CompareCpu", Messages.getString(Messages.CompareCpuRule_RULE_NAME), JfrRuleTopics.PROCESSES, //$NON-NLS-1$
				CONFIGURATION_ATTRIBUTES, RESULT_ATTRIBUTES, REQUIRED_EVENTS);
	}

	@Override
	protected IResult getResult(IItemCollection items, IPreferenceValueProvider vp, IResultValueProvider rp) {
		double warningLimit = vp.getPreferenceValue(OTHER_CPU_WARNING_LIMIT).doubleValue() / 100;
		double infoLimit = vp.getPreferenceValue(OTHER_CPU_INFO_LIMIT).doubleValue() / 100;

		IItemCollection cpuItems = items.apply(JdkFilters.CPU_LOAD);
		IType<IItem> cpuLoadType = RulesToolkit.getType(cpuItems, JdkTypeIDs.CPU_LOAD);
		if (!cpuLoadType.hasAttribute(JdkAttributes.JVM_TOTAL)) {
			return RulesToolkit.getMissingAttributeResult(this, cpuLoadType, JdkAttributes.JVM_TOTAL, vp);
		}
		// FIXME: Could consider using the infoLimit for the span instead?
		SpanLimit maxOtherCpu = SpanToolkit.getMaxSpanLimit(cpuItems, JdkAttributes.OTHER_CPU, JfrAttributes.END_TIME,
				warningLimit);
		SpanLimit maxOtherCpuRatio = SpanToolkit.getMaxSpanLimit(cpuItems, JdkAttributes.OTHER_CPU_RATIO,
				JfrAttributes.END_TIME, warningLimit);

		if (maxOtherCpu == null || maxOtherCpuRatio == null) {
			return ResultBuilder.createFor(this, vp).setSeverity(Severity.NA)
					.setSummary(Messages.getString(Messages.CompareCpuRule_TEXT_TOO_FEW_SAMPLES)).build();
		}

		double score = RulesToolkit.mapExp100(maxOtherCpuRatio.value, infoLimit, warningLimit);

		IRange<IQuantity> cpuLoadWindow = QuantityRange.createWithEnd(UnitLookup.EPOCH_NS.quantity(maxOtherCpu.start),
				UnitLookup.EPOCH_NS.quantity(maxOtherCpu.end));
		IQuantity otherCpuMaxValue = UnitLookup.PERCENT.quantity(Math.round(maxOtherCpu.value * 100));
		return ResultBuilder.createFor(this, vp).setSeverity(Severity.get(score))
				.setSummary(Messages.getString(Messages.CompareCpuRule_TEXT_MESSAGE))
				.setExplanation(Messages.getString(Messages.CompareCpuRule_TEXT_INFO_LONG))
				.addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(score))
				.addResult(AVERAGE_CPU_LOAD, otherCpuMaxValue).addResult(AVERAGE_CPU_LOAD_WINDOW, cpuLoadWindow)
				.build();
	}
}
