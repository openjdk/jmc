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
package org.openjdk.jmc.flightrecorder.rules.jdk.general;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.ResultBuilder;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedCollectionResult;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.jdk.dataproviders.JvmInternalsDataProvider;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

public class DuplicateFlagsRule implements IRule {

	public static class DuplicateFlags implements IDisplayable {
		private final List<String> duplicates;

		private DuplicateFlags(List<String> duplicates) {
			this.duplicates = duplicates;
		}

		@Override
		public String displayUsing(String formatHint) {
			StringBuilder sb = new StringBuilder();
			for (String d : duplicates) {
				sb.append(d);
				sb.append(',');
			}
			sb.deleteCharAt(sb.length() - 1);
			return sb.toString();
		}
	}

	private static final String RESULT_ID = "DuplicateFlags"; //$NON-NLS-1$

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = new HashMap<>();

	private static final ContentType<DuplicateFlags> DUPLICATE_FLAGS = UnitLookup
			.createSyntheticContentType("duplicateFlags"); //$NON-NLS-1$

	public static final TypedCollectionResult<DuplicateFlags> DUPLICATED_FLAGS = new TypedCollectionResult<>(
			"duplicateFlags", //$NON-NLS-1$
			Messages.getString(Messages.DuplicateFlagsRule_RESULT_DUPLICATED_FLAGS_NAME),
			Messages.getString(Messages.DuplicateFlagsRule_RESULT_DUPLICATED_FLAGS_DESCRIPTION), DUPLICATE_FLAGS,
			DuplicateFlags.class);
	public static final TypedResult<IQuantity> TOTAL_DUPLICATED_FLAGS = new TypedResult<>("totalDuplicatedFlags", //$NON-NLS-1$
			Messages.getString(Messages.DuplicateFlagsRule_RESULT_DUPLICATED_FLAGS_COUNT_NAME),
			Messages.getString(Messages.DuplicateFlagsRule_RESULT_DUPLICATED_FLAGS_COUNT_DESCRIPTION),
			UnitLookup.NUMBER, IQuantity.class);

	private static final List<TypedResult<?>> RESULT_ATTRIBUTES = Arrays.<TypedResult<?>> asList(DUPLICATED_FLAGS,
			TOTAL_DUPLICATED_FLAGS);

	static {
		REQUIRED_EVENTS.put(JdkTypeIDs.VM_INFO, EventAvailability.AVAILABLE);
	}

	@Override
	public String getId() {
		return RESULT_ID;
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.JVM_INFORMATION;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.DuplicateFlagsRuleFactory_RULE_NAME);
	}

	@Override
	public Map<String, EventAvailability> getRequiredEvents() {
		return REQUIRED_EVENTS;
	}

	@Override
	public RunnableFuture<IResult> createEvaluation(
		final IItemCollection items, final IPreferenceValueProvider preferenceValueProvider,
		final IResultValueProvider dependencyResults) {
		FutureTask<IResult> evaluationTask = new FutureTask<>(new Callable<IResult>() {
			@Override
			public IResult call() throws Exception {
				return getResult(items, preferenceValueProvider, dependencyResults);
			}
		});
		return evaluationTask;
	}

	private IResult getResult(IItemCollection items, IPreferenceValueProvider vp, IResultValueProvider rp) {
		IItemCollection jvmInfoItems = items.apply(JdkFilters.VM_INFO);

		// FIXME: Should we check if there are different jvm args in different chunks?
		IAggregator<Set<String>, ?> argumentAggregator = Aggregators.distinct(JdkAttributes.JVM_ARGUMENTS);
		Set<String> args = jvmInfoItems.getAggregate(argumentAggregator);
		if (args != null && !args.isEmpty()) {
			List<DuplicateFlags> duplicateFlags = new ArrayList<>();
			for (List<String> dupe : JvmInternalsDataProvider.checkDuplicates(args.iterator().next())) {
				duplicateFlags.add(new DuplicateFlags(dupe));
			}
			if (!JvmInternalsDataProvider.checkDuplicates(args.iterator().next()).isEmpty()) {
				return ResultBuilder.createFor(this, vp).addResult(DUPLICATED_FLAGS, duplicateFlags)
						.addResult(TOTAL_DUPLICATED_FLAGS, UnitLookup.NUMBER_UNITY.quantity(duplicateFlags.size()))
						.setSeverity(Severity.INFO)
						.setSummary(Messages.getString(Messages.DuplicateFlagsRule_RESULT_SUMMARY))
						.setExplanation(Messages.getString(Messages.DuplicateFlagsRule_RESULT_EXPLANATION))
						.setSolution(Messages.getString(Messages.DuplicateFlagsRule_RESULT_SOLUTION)).build();
			}
		}
		return ResultBuilder.createFor(this, vp).setSeverity(Severity.OK)
				.setSummary(Messages.getString(Messages.DuplicateFlagsRule_RESULT_SUMMARY_OK)).build();
	}

	@Override
	public Collection<TypedPreference<?>> getConfigurationAttributes() {
		return Collections.emptyList();
	}

	@Override
	public Collection<TypedResult<?>> getResults() {
		return RESULT_ATTRIBUTES;
	}

}
