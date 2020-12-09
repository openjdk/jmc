/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.rules.jdk.general;

import static org.openjdk.jmc.common.item.Attribute.attr;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.ResultBuilder;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.RequiredEventsBuilder;

public class FastTimeRule implements IRule {

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.TIME_CONVERSION, EventAvailability.AVAILABLE).build();

	@Override
	public String getId() {
		return "FastTimeRule"; //$NON-NLS-1$
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.JVM_INFORMATION;
	}

	@Override
	public String getName() {
		return "Fast Time Conversion Rule";
	}

	@Override
	public Map<String, EventAvailability> getRequiredEvents() {
		return REQUIRED_EVENTS;
	}

	private IResult getResult(
		IItemCollection items, IPreferenceValueProvider preferences, IResultValueProvider results) {
		// Check time conversion error
		IItemCollection timeConversionItems = items.apply(JdkFilters.TIME_CONVERSION);
		IAggregator<IQuantity, ?> max = Aggregators.max(attr("fastTimeConversionAdjustments", null, //$NON-NLS-1$
				UnitLookup.NUMBER));
		IQuantity conversionFactor = timeConversionItems.getAggregate(max);
		Boolean fastTimeEnabled = timeConversionItems
				.getAggregate(Aggregators.and(JdkTypeIDs.TIME_CONVERSION, attr("fastTimeEnabled", null, //$NON-NLS-1$
						UnitLookup.FLAG)));
		if (conversionFactor != null && fastTimeEnabled) {
			if (conversionFactor.longValue() != 0) {
				return ResultBuilder.createFor(this, preferences).setSeverity(Severity.WARNING)
						.setSummary(Messages.getString(Messages.FasttimeRule_TEXT_WARN))
						.setExplanation(Messages.getString(Messages.FasttimeRule_TEXT_WARN_LONG)).build();
			}
		}
		return ResultBuilder.createFor(this, preferences).setSeverity(Severity.OK)
				.setSummary(Messages.getString(Messages.FlightRecordingSupportRule_TEXT_OK)).build();
	}

	@Override
	public RunnableFuture<IResult> createEvaluation(
		final IItemCollection items, final IPreferenceValueProvider preferenceValueProvider,
		final IResultValueProvider dependencyResults) {
		return new FutureTask<>(new Callable<IResult>() {
			@Override
			public IResult call() throws Exception {
				return getResult(items, preferenceValueProvider, dependencyResults);
			}
		});
	}

	@Override
	public Collection<TypedPreference<?>> getConfigurationAttributes() {
		return Collections.emptyList();
	}

	@Override
	public Collection<TypedResult<?>> getResults() {
		return Collections.emptyList();
	}

}
