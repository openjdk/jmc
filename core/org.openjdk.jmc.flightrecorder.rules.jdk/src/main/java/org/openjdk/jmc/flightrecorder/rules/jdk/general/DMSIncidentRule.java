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
package org.openjdk.jmc.flightrecorder.rules.jdk.general;

import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER;
import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER_UNITY;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IItemQuery;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.item.ItemQueryBuilder;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

public class DMSIncidentRule implements IRule {

	private static final String RESULT_ID = "DMSIncident"; //$NON-NLS-1$
	private static final String DMS_PATH = "http://www.oracle.com/dms/dfw/dms/dfw/DFW_Incident/DFW_Incident_state"; //$NON-NLS-1$
	private static final IItemFilter FILTER = ItemFilters.type(DMS_PATH);
	public static final IAggregator<IQuantity, ?> INCIDENTS_COUNT = Aggregators.count(
			Messages.getString(Messages.DMSIncidentRule_AGGR_INCIDENTS_COUNT),
			Messages.getString(Messages.DMSIncidentRule_AGGR_INCIDENTS_COUNT_DESC), FILTER);

	private static final TypedPreference<IQuantity> WARNING_LIMIT = new TypedPreference<>("dmsincident.warning.limit", //$NON-NLS-1$
			Messages.getString(Messages.DMSIncidentRule_CONFIG_WARNING_LIMIT),
			Messages.getString(Messages.DMSIncidentRule_CONFIG_WARNING_LIMIT_LONG), NUMBER, NUMBER_UNITY.quantity(1));
	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays.<TypedPreference<?>> asList(WARNING_LIMIT);

	private Result getResult(IItemCollection items, IPreferenceValueProvider valueProvider) {
		EventAvailability eventAvailability = RulesToolkit.getEventAvailability(items, DMS_PATH);

		// Not getting any is good, but only if the event was not unavailable or disabled
		if (eventAvailability == EventAvailability.UNKNOWN || eventAvailability == EventAvailability.DISABLED) {
			return RulesToolkit.getEventAvailabilityResult(this, items, eventAvailability, DMS_PATH);
		}

		IQuantity limit = valueProvider.getPreferenceValue(WARNING_LIMIT);
		IQuantity incidents = items.getAggregate(INCIDENTS_COUNT);
		if (incidents != null && incidents.compareTo(limit) >= 0) {
			double score = RulesToolkit.mapExp100(incidents.doubleValue(), limit.doubleValueIn(incidents.getUnit()));
			IItemQuery query = ItemQueryBuilder.fromWhere(FILTER).build();
			return new Result(this, score, Messages.getString(Messages.DMSIncidentRuleFactory_TEXT_WARN),
					MessageFormat.format(Messages.getString(Messages.DMSIncidentRuleFactory_TEXT_WARN_LONG), incidents),
					query);
		}
		return new Result(this, 0, Messages.getString(Messages.DMSIncidentRuleFactory_TEXT_OK));
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
		return Messages.getString(Messages.DMSIncidentRuleFactory_RULE_NAME);
	}

	@Override
	public String getTopic() {
		// FIXME: Create constant for path
		return "DMS"; //$NON-NLS-1$
	}

}
