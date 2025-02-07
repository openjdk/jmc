/*
 * Copyright (c) 2022, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2022, 2025, Datadog, Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
import static org.openjdk.jmc.common.unit.UnitLookup.PLAIN_TEXT;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.ResultBuilder;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedCollectionResult;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.RequiredEventsBuilder;

public class FinalizersRunRule implements IRule {

	private static final String FINALIZERS_RUN_RESULT_ID = "FinalizersRun"; //$NON-NLS-1$

	private static final TypedResult<IQuantity> FINALIZERS_RUN_COUNT = new TypedResult<>("finalizersRunCount", //$NON-NLS-1$
			Messages.getString(Messages.FinalizersRunRule_RESULT_FINALIZERS_RUN_COUNT),
			Messages.getString(Messages.FinalizersRunRule_RESULT_FINALIZERS_RUN_COUNT_DESC), NUMBER, IQuantity.class);
	private static final TypedCollectionResult<String> FINALIZERS_RUN_CLASSES = new TypedCollectionResult<>(
			"finalizersRunClasses", Messages.getString(Messages.FinalizersRunRule_RESULT_FINALIZERS_RUN_CLASSES), //$NON-NLS-1$
			Messages.getString(Messages.FinalizersRunRule_RESULT_FINALIZERS_RUN_CLASSES_DESC), PLAIN_TEXT,
			String.class);
	private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays
			.<TypedResult<?>> asList(FINALIZERS_RUN_COUNT, FINALIZERS_RUN_CLASSES);

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.FINALIZER_STATISTICS, EventAvailability.AVAILABLE).build();

	private static final TypedPreference<String> FINALIZABLE_CLASSES_INCLUDE_REGEXP = new TypedPreference<>(
			"finalizable.classes.include.regexp", //$NON-NLS-1$
			Messages.getString(Messages.FinalizersRunRule_CONFIG_FINALIZABLE_CLASSES_INCLUDE_REGEXP),
			Messages.getString(Messages.FinalizersRunRule_CONFIG_FINALIZABLE_CLASSES_INCLUDE_REGEXP_DESC),
			PLAIN_TEXT.getPersister(),
			// Exclude a number of common standard library prefixes.
			"^(?!java\\.|javax\\.|sun\\.|com\\.sun\\.|jdk\\.|scala\\.|kotlin\\.|kotlinx\\.|groovy\\.|closure\\.).*$"); //$NON-NLS-1$
	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays
			.<TypedPreference<?>> asList(FINALIZABLE_CLASSES_INCLUDE_REGEXP);

	@Override
	public RunnableFuture<IResult> createEvaluation(
		IItemCollection items, IPreferenceValueProvider valueProvider, IResultValueProvider resultProvider) {
		return new FutureTask<>(() -> getResult(items, valueProvider));
	}

	@Override
	public Collection<TypedPreference<?>> getConfigurationAttributes() {
		return CONFIG_ATTRIBUTES;
	}

	@Override
	public Collection<TypedResult<?>> getResults() {
		return RESULT_ATTRIBUTES;
	}

	@Override
	public String getId() {
		return FINALIZERS_RUN_RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.FinalizersRunRule_RULE_NAME);
	}

	@Override
	public Map<String, EventAvailability> getRequiredEvents() {
		return REQUIRED_EVENTS;
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.JAVA_APPLICATION;
	}

	private IResult getResult(IItemCollection items, IPreferenceValueProvider valueProvider) {
		String classesIncludeRegex = valueProvider.getPreferenceValue(FINALIZABLE_CLASSES_INCLUDE_REGEXP);
		IItemFilter finalizerStatisticsEventsFilter = ItemFilters.and(ItemFilters.type(JdkTypeIDs.FINALIZER_STATISTICS),
				ItemFilters.matches(JdkAttributes.FINALIZABLE_CLASS_NAME, classesIncludeRegex));
		IItemCollection finalizerStatisticsEvents = items.apply(finalizerStatisticsEventsFilter);

		long totalCount = 0;
		Set<String> finalizableClasses = new HashSet<String>();
		for (IItemIterable eventIterable : finalizerStatisticsEvents) {
			IMemberAccessor<IMCType, IItem> finalizableClassAccessor = JdkAttributes.FINALIZABLE_CLASS
					.getAccessor(eventIterable.getType());
			IMemberAccessor<IQuantity, IItem> totalFinalizersRunAccessor = JdkAttributes.TOTAL_FINALIZERS_RUN
					.getAccessor(eventIterable.getType());
			for (IItem event : eventIterable) {
				IMCType finalizableClass = finalizableClassAccessor.getMember(event);
				IQuantity totalFinalizersRun = totalFinalizersRunAccessor.getMember(event);
				if (finalizableClass != null && totalFinalizersRun != null) {
					long countforEvent = totalFinalizersRun.clampedLongValueIn(NUMBER_UNITY);
					if (countforEvent > 0) {
						totalCount += countforEvent;
						finalizableClasses.add(finalizableClass.getFullName());
					}
				}
			}
		}

		if (totalCount <= 0) {
			return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.OK)
					.setSummary(Messages.getString(Messages.FinalizersRunRule_SUMMARY_OK)).build();
		}

		return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.INFO)
				.setSummary(Messages.getString(Messages.FinalizersRunRule_SUMMARY_WARN))
				.setExplanation(Messages.getString(Messages.FinalizersRunRule_EXPLANATION))
				.setSolution(Messages.getString(Messages.FinalizersRunRule_SOLUTION))
				.addResult(FINALIZERS_RUN_COUNT, NUMBER_UNITY.quantity(totalCount))
				.addResult(FINALIZERS_RUN_CLASSES, finalizableClasses).build();
	}
}
