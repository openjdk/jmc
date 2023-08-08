/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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

import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER;
import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER_UNITY;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.DependsOn;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.ResultBuilder;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.RequiredEventsBuilder;

@DependsOn(value = GarbageCollectionInfoRule.class)
public class HeapInspectionRule implements IRule {
	private static final String HEAP_INSPECTION_RESULT_ID = "HeapInspectionGc"; //$NON-NLS-1$

	public static final TypedPreference<IQuantity> HEAP_INSPECTION_LIMIT = new TypedPreference<>(
			"heap.inspection.info.limit", Messages.getString(Messages.HeapInspectionRule_CONFIG_WARNING_LIMIT), //$NON-NLS-1$
			Messages.getString(Messages.HeapInspectionRule_CONFIG_WARNING_LIMIT_LONG), NUMBER,
			NUMBER_UNITY.quantity(1));
	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays
			.<TypedPreference<?>> asList(HEAP_INSPECTION_LIMIT);

	public static final TypedResult<IQuantity> OBJECT_COUNT_GCS = new TypedResult<>("objectCountGCs", //$NON-NLS-1$
			"Object Count GCs", "The number of heap inspection GCs.", UnitLookup.NUMBER, IQuantity.class);

	private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays
			.<TypedResult<?>> asList(TypedResult.SCORE, OBJECT_COUNT_GCS);

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.GARBAGE_COLLECTION, EventAvailability.AVAILABLE).build();

	@Override
	public RunnableFuture<IResult> createEvaluation(
		final IItemCollection items, final IPreferenceValueProvider valueProvider,
		final IResultValueProvider resultProvider) {
		FutureTask<IResult> evaluationTask = new FutureTask<>(new Callable<IResult>() {
			@Override
			public IResult call() throws Exception {
				return getHeapInspectionResult(items, valueProvider, resultProvider);
			}
		});
		return evaluationTask;
	}

	private IResult getHeapInspectionResult(
		IItemCollection items, IPreferenceValueProvider vp, IResultValueProvider rp) {
		IQuantity limit = vp.getPreferenceValue(HEAP_INSPECTION_LIMIT);
		GarbageCollectionsInfo aggregate = rp.getResultValue(GarbageCollectionInfoRule.GC_INFO);
		if (aggregate.getObjectCountGCs() > 0) {
			double score = RulesToolkit.mapExp74(aggregate.getObjectCountGCs(), limit.longValue());
			String longMessage = Messages.getString(Messages.HeapInspectionGcRuleFactory_TEXT_INFO_LONG);
			if (RulesToolkit.isEventsEnabled(items, JdkTypeIDs.OBJECT_COUNT)) {
				longMessage += "\n" + Messages.getString(Messages.HeapInspectionGcRuleFactory_TEXT_INFO_LONG_JFR); //$NON-NLS-1$
			}
			return ResultBuilder.createFor(this, vp).setSeverity(Severity.get(score))
					.setSummary(Messages.getString(Messages.HeapInspectionGcRuleFactory_TEXT_INFO))
					.setExplanation(longMessage).addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(score))
					.addResult(OBJECT_COUNT_GCS, UnitLookup.NUMBER_UNITY.quantity(aggregate.getObjectCountGCs()))
					.build();
		} else {
			return ResultBuilder.createFor(this, vp).setSeverity(Severity.OK)
					.setSummary(Messages.getString(Messages.HeapInspectionGcRuleFactory_TEXT_OK))
					.setExplanation(Messages.getString(Messages.HeapInspectionGcRuleFactory_TEXT_OK_LONG)).build();
		}
	}

	@Override
	public Collection<TypedPreference<?>> getConfigurationAttributes() {
		return CONFIG_ATTRIBUTES;
	}

	@Override
	public String getId() {
		return HEAP_INSPECTION_RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.HeapInspectionGcRuleFactory_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.GARBAGE_COLLECTION;
	}

	@Override
	public Map<String, EventAvailability> getRequiredEvents() {
		return REQUIRED_EVENTS;
	}

	@Override
	public Collection<TypedResult<?>> getResults() {
		return RESULT_ATTRIBUTES;
	}
}
