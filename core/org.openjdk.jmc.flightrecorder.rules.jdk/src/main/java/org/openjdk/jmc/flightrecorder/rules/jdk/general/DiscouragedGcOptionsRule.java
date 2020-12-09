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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.BinaryPrefix;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.ResultBuilder;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.jdk.memory.CollectorType;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.RequiredEventsBuilder;

public class DiscouragedGcOptionsRule implements IRule {
	private static final IQuantity LARGE_HEAP = UnitLookup.MEMORY.getUnit(BinaryPrefix.GIBI).quantity(4);
	private static final IQuantity HW_THREADS_FOR_MULTI_CPU = UnitLookup.NUMBER_UNITY.quantity(4);
	private static final IQuantity ONE = UnitLookup.NUMBER_UNITY.quantity(1);

	private static final String GC_OPTIONS_RESULT_ID = "GcOptions"; //$NON-NLS-1$

	public static final TypedResult<IQuantity> HARDWARE_THREADS = new TypedResult<>("hwThreads", //$NON-NLS-1$
			JdkAggregators.MIN_HW_THREADS, UnitLookup.NUMBER, IQuantity.class);
	public static final TypedResult<IQuantity> PARALLEL_GC_THREADS = new TypedResult<>("parallelGcThreads", //$NON-NLS-1$
			JdkAggregators.PARALLEL_GC_THREAD_COUNT_MAX, UnitLookup.NUMBER, IQuantity.class);

	private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays.<TypedResult<?>> asList(HARDWARE_THREADS,
			PARALLEL_GC_THREADS);

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.CPU_INFORMATION, EventAvailability.AVAILABLE).build();

	private IResult getResult(
		IItemCollection items, IPreferenceValueProvider valueProvider, IResultValueProvider resultProvider) {
		IQuantity parallelGCThreads = items.getAggregate(JdkAggregators.PARALLEL_GC_THREAD_COUNT_MAX);
		IQuantity minHwThreads = items.getAggregate(JdkAggregators.MIN_HW_THREADS);
		CollectorType oc = CollectorType.getOldCollectorType(items);
		if (parallelGCThreads != null && minHwThreads != null && oc != null) {
			if (oc == CollectorType.SERIAL_OLD) {
				IQuantity maxHeapSize = items.getAggregate(JdkAggregators.HEAP_CONF_MAX_SIZE);
				if (minHwThreads.compareTo(HW_THREADS_FOR_MULTI_CPU) >= 0 && maxHeapSize != null
						&& maxHeapSize.compareTo(LARGE_HEAP) > 0) {
					return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.INFO)
							.setSummary(Messages.getString(Messages.SerialGcOnMultiCpuRuleFactory_TEXT_INFO))
							.setExplanation(Messages.getString(Messages.SerialGcOnMultiCpuRuleFactory_TEXT_INFO_LONG))
							.build();
				}
			} else if (minHwThreads.compareTo(ONE) == 0 && oc == CollectorType.PARALLEL_OLD) {
				return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.INFO)
						.setSummary(Messages.getString(Messages.ParallelOnSingleCpuRuleFactory_TEXT_INFO))
						.setExplanation(Messages.getString(Messages.ParallelOnSingleCpuRuleFactory_TEXT_INFO_LONG))
						.build();
			} else if (parallelGCThreads.compareTo(minHwThreads) > 0) {
				return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.INFO)
						.addResult(HARDWARE_THREADS, minHwThreads).addResult(PARALLEL_GC_THREADS, parallelGCThreads)
						.setSummary(Messages.getString(Messages.NumberOfGcThreadsRuleFactory_TEXT_INFO))
						.setExplanation(Messages.getString(Messages.NumberOfGcThreadsRuleFactory_TEXT_INFO_LONG))
						.build();
			} else if (parallelGCThreads.compareTo(ONE) == 0
					&& (oc == CollectorType.PARALLEL_OLD || oc == CollectorType.G1_OLD)) {
				return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.INFO)
						.setSummary(Messages.getString(Messages.ParGcFewThreadsRuleFactory_TEXT_INFO))
						.setExplanation(Messages.getString(Messages.ParGcFewThreadsRuleFactory_TEXT_INFO_LONG)).build();
			}
			return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.OK)
					.setSummary(Messages.getString(Messages.DiscouragedGcOptionsRule_TEXT_OK)).build();
		}
		return RulesToolkit.getTooFewEventsResult(this, valueProvider);
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
		return GC_OPTIONS_RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.DiscouragedGcOptionsRule_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.JVM_INFORMATION;
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
