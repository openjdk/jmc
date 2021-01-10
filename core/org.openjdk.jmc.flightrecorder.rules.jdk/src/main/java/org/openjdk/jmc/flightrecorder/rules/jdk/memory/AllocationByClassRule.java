/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.collection.MapToolkit.IntEntry;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemFilters;
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
import org.openjdk.jmc.flightrecorder.rules.jdk.dataproviders.StacktraceDataProvider;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.RequiredEventsBuilder;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator.FrameCategorization;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel.Fork;

public class AllocationByClassRule implements IRule {
	private static final String CLASS_RESULT_ID = "Allocations.class"; //$NON-NLS-1$

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.ALLOC_INSIDE_TLAB, EventAvailability.ENABLED)
			.addEventType(JdkTypeIDs.ALLOC_OUTSIDE_TLAB, EventAvailability.ENABLED).build();

	public static final TypedResult<IMCType> MOST_ALLOCATED_TYPE = new TypedResult<>("mostAllocatedType", //$NON-NLS-1$
			"Most Allocated Type", "The most allocated type.", UnitLookup.CLASS, IMCType.class);
	public static final TypedCollectionResult<IMCMethod> ALLOCATION_FRAMES = new TypedCollectionResult<>(
			"allocationFrames", "Allocation Frames", //$NON-NLS-1$
			"The most interesting frames leading to the most commonly allocated type.", UnitLookup.METHOD,
			IMCMethod.class);

	private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays
			.<TypedResult<?>> asList(TypedResult.SCORE, MOST_ALLOCATED_TYPE, ALLOCATION_FRAMES);

	private IResult getResult(
		IItemCollection items, IPreferenceValueProvider valueProvider, IResultValueProvider resultProvider) {
		List<IntEntry<IMCType>> entries = RulesToolkit.calculateGroupingScore(items.apply(JdkFilters.ALLOC_ALL),
				JdkAttributes.ALLOCATION_CLASS);
		if (entries.size() > 1) {
			double balance = RulesToolkit.calculateBalanceScore(entries);
			IntEntry<IMCType> mostSignificant = entries.get(entries.size() - 1);
			// FIXME: Configuration attribute instead of hard coded 1000 tlabs => relevance 50
			double relevance = RulesToolkit.mapExp100Y(mostSignificant.getValue(), 1000, 50);
			double score = balance * relevance * 0.74; // ceiling at 74;

			IItemFilter significantFilter = ItemFilters.and(JdkFilters.ALLOC_ALL,
					ItemFilters.equals(JdkAttributes.ALLOCATION_CLASS, mostSignificant.getKey()));
			StacktraceModel stacktraceModel = new StacktraceModel(false,
					new FrameSeparator(FrameCategorization.METHOD, false), items.apply(significantFilter));
			Fork rootFork = stacktraceModel.getRootFork();
			if (rootFork.getBranchCount() > 0) {
				List<IMCMethod> relevantFramesList = StacktraceDataProvider.getRelevantTraceList(rootFork.getBranch(0),
						rootFork.getItemsInFork());
				return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.get(score))
						.setSummary(Messages.getString(Messages.AllocationByClassRule_TEXT_MESSAGE))
						.setExplanation(Messages.getString(Messages.AllocationRuleFactory_TEXT_CLASS_INFO_LONG))
						.addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(score))
						.addResult(ALLOCATION_FRAMES, relevantFramesList)
						.addResult(MOST_ALLOCATED_TYPE, mostSignificant.getKey()).build();
			}
		}
		return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.NA).build();
	}

	@Override
	public RunnableFuture<IResult> createEvaluation(
		final IItemCollection items, final IPreferenceValueProvider valueProvider,
		final IResultValueProvider resultProvider) {
		FutureTask<IResult> evaluationTask = new FutureTask<>(new Callable<IResult>() {
			@Override
			public IResult call() throws Exception {
				return getResult(items, valueProvider, resultProvider);
			}
		});
		return evaluationTask;
	}

	@Override
	public Collection<TypedPreference<?>> getConfigurationAttributes() {
		return Collections.emptyList();
	}

	@Override
	public String getId() {
		return CLASS_RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.AllocationByClassRule_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.HEAP;
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
