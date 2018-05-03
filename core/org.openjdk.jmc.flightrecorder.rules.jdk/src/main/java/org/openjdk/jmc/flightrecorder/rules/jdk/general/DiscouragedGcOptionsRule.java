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

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.BinaryPrefix;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.memory.CollectorType;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

public class DiscouragedGcOptionsRule implements IRule {
	private static final IQuantity LARGE_HEAP = UnitLookup.MEMORY.getUnit(BinaryPrefix.GIBI).quantity(4);
	private static final IQuantity HW_THREADS_FOR_MULTI_CPU = UnitLookup.NUMBER_UNITY.quantity(4);
	private static final IQuantity ONE = UnitLookup.NUMBER_UNITY.quantity(1);

	private static final String GC_OPTIONS_RESULT_ID = "GcOptions"; //$NON-NLS-1$

	private Result getResult(IItemCollection items, IPreferenceValueProvider valueProvider) {
		EventAvailability eventAvailability = RulesToolkit.getEventAvailability(items, JdkTypeIDs.CPU_INFORMATION);
		if (eventAvailability == EventAvailability.UNAVAILABLE || eventAvailability == EventAvailability.DISABLED) {
			return RulesToolkit.getEventAvailabilityResult(this, items, eventAvailability, JdkTypeIDs.CPU_INFORMATION);
		}

		IQuantity parallelGCThreads = items.getAggregate(JdkAggregators.PARALLEL_GC_THREAD_COUNT_MAX);
		IQuantity minHwThreads = items.getAggregate(JdkAggregators.MIN_HW_THREADS);
		CollectorType oc = CollectorType.getOldCollectorType(items);
		if (parallelGCThreads != null && minHwThreads != null && oc != null) {
			if (oc == CollectorType.SERIAL_OLD) {
				IQuantity maxHeapSize = items.getAggregate(JdkAggregators.HEAP_CONF_MAX_SIZE);
				if (minHwThreads.compareTo(HW_THREADS_FOR_MULTI_CPU) >= 0 && maxHeapSize != null
						&& maxHeapSize.compareTo(LARGE_HEAP) > 0) {
					return new Result(this, 50, Messages.getString(Messages.SerialGcOnMultiCpuRuleFactory_TEXT_INFO),
							Messages.getString(Messages.SerialGcOnMultiCpuRuleFactory_TEXT_INFO_LONG));
				}
			} else if (minHwThreads.compareTo(ONE) == 0 && oc == CollectorType.PARALLEL_OLD) {
				return new Result(this, 50, Messages.getString(Messages.ParallelOnSingleCpuRuleFactory_TEXT_INFO),
						Messages.getString(Messages.ParallelOnSingleCpuRuleFactory_TEXT_INFO_LONG));
			} else if (parallelGCThreads.compareTo(minHwThreads) > 0) {
				String message = MessageFormat.format(
						Messages.getString(Messages.NumberOfGcThreadsRuleFactory_TEXT_INFO), parallelGCThreads,
						minHwThreads.displayUsing(IDisplayable.AUTO));
				String longMessage = message + " " //$NON-NLS-1$
						+ Messages.getString(Messages.NumberOfGcThreadsRuleFactory_TEXT_INFO_LONG);
				return new Result(this, 50, message, longMessage);
			} else if (parallelGCThreads.compareTo(ONE) == 0
					&& (oc == CollectorType.PARALLEL_OLD || oc == CollectorType.G1_OLD)) {
				return new Result(this, 50, Messages.getString(Messages.ParGcFewThreadsRuleFactory_TEXT_INFO),
						Messages.getString(Messages.ParGcFewThreadsRuleFactory_TEXT_INFO_LONG));
			}
			return new Result(this, 0, Messages.getString(Messages.DiscouragedGcOptionsRule_TEXT_OK));
		}
		return RulesToolkit.getTooFewEventsResult(this);
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
		return JfrRuleTopics.JVM_INFORMATION_TOPIC;
	}
}
