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
package org.openjdk.jmc.flightrecorder.rules.jdk.cpu;

import static org.openjdk.jmc.common.unit.UnitLookup.EPOCH_NS;

import java.text.MessageFormat;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.KindOfQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.AbstractRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.combine.SpanLimit;
import org.openjdk.jmc.flightrecorder.rules.jdk.combine.SpanToolkit;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

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

	public CompareCpuRule() {
		super("CompareCpu", Messages.getString(Messages.CompareCpuRule_RULE_NAME), JfrRuleTopics.PROCESSES_TOPIC, //$NON-NLS-1$
				OTHER_CPU_INFO_LIMIT, OTHER_CPU_WARNING_LIMIT);
	}

	@Override
	protected Result getResult(IItemCollection items, IPreferenceValueProvider vp) {
		EventAvailability eventAvailability = RulesToolkit.getEventAvailability(items, JdkTypeIDs.CPU_LOAD);
		if (eventAvailability != EventAvailability.AVAILABLE) {
			return RulesToolkit.getEventAvailabilityResult(this, items, eventAvailability, JdkTypeIDs.CPU_LOAD);
		}

		double warningLimit = vp.getPreferenceValue(OTHER_CPU_WARNING_LIMIT).doubleValue() / 100;
		double infoLimit = vp.getPreferenceValue(OTHER_CPU_INFO_LIMIT).doubleValue() / 100;

		IItemCollection cpuItems = items.apply(JdkFilters.CPU_LOAD);
		IType<IItem> cpuLoadType = RulesToolkit.getType(cpuItems, JdkTypeIDs.CPU_LOAD);
		if (!cpuLoadType.hasAttribute(JdkAttributes.JVM_TOTAL)) {
			return RulesToolkit.getMissingAttributeResult(this, cpuLoadType, JdkAttributes.JVM_TOTAL);
		}
		// FIXME: Could consider using the infoLimit for the span instead?
		SpanLimit max = SpanToolkit.getMaxSpanLimit(cpuItems, JdkAttributes.OTHER_CPU, JfrAttributes.END_TIME,
				warningLimit);

		if (max == null) {
			return RulesToolkit.getNotApplicableResult(this,
					Messages.getString(Messages.CompareCpuRule_TEXT_TOO_FEW_SAMPLES));
		}

		double score = RulesToolkit.mapExp100(max.value, infoLimit, warningLimit);

		String startTime = KindOfQuantity.format(max.start, EPOCH_NS);
		String duration = KindOfQuantity.format(max.end - max.start, UnitLookup.NANOSECOND);
		String otherCpuMaxValueString = UnitLookup.PERCENT.quantity(Math.round(max.value * 100))
				.displayUsing(IDisplayable.AUTO);
		String message = MessageFormat.format(Messages.getString(Messages.CompareCpuRule_TEXT_MESSAGE), duration,
				startTime, otherCpuMaxValueString);
		String longMessage = null;
		if (score >= 25) {
			longMessage = message + "<p>" + Messages.getString(Messages.CompareCpuRule_TEXT_INFO_LONG); //$NON-NLS-1$
		}
		return new Result(this, score, message, longMessage, null);
	}
}
