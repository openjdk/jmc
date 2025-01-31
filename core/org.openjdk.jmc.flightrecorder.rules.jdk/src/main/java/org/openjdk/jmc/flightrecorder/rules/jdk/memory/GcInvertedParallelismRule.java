/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2023, 2025, Red Hat Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.rules.jdk.memory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
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
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.RequiredEventsBuilder;

/**
 * Rule that checks for "Inverted Parallelism", as described by the Garbagecat GC log analysis tool.
 * Checks if the parallel collection performance is less than serial (single-threaded). See:
 * https://github.com/mgm3746/garbagecat/blob/main/src/main/java/org/eclipselabs/garbagecat/util/jdk/JdkMath.java#L365
 */
public class GcInvertedParallelismRule implements IRule {
	private static final String INVERTED_PARALLELISM_RESULT_ID = "GcInvertedParallelism"; //$NON-NLS-1$
	// Requires the jdk.GCCPUTime event introduced in JDK 20
	// See: JDK-8291753
	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.GC_CONF, EventAvailability.AVAILABLE)
			.addEventType(JdkTypeIDs.GC_CPU_TIME, EventAvailability.AVAILABLE).build();

	public static final TypedResult<IQuantity> GC_CPU_TIME_EVENT_COUNT = new TypedResult<>("gcCpuTimeEventCount", //$NON-NLS-1$
			"GCCPUTime Event Count", "The number of recorded jdk.GCCPUTime events.", UnitLookup.NUMBER,
			IQuantity.class);

	public static final TypedResult<Long> INVERTED_PARALLELISM_COUNT = new TypedResult<>("invertedParallelismCount", //$NON-NLS-1$
			"Inverted Parallelism Count", "The number of detected instances of Inverted Parallelism.",
			UnitLookup.RAW_LONG, Long.class);

	private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays
			.<TypedResult<?>> asList(GC_CPU_TIME_EVENT_COUNT, INVERTED_PARALLELISM_COUNT);

	@Override
	public RunnableFuture<IResult> createEvaluation(
		final IItemCollection items, final IPreferenceValueProvider valueProvider,
		final IResultValueProvider resultProvider) {
		return new FutureTask<>(new Callable<IResult>() {
			@Override
			public IResult call() throws Exception {
				if (items.getAggregate(JdkAggregators.OLD_COLLECTOR)
						.equals(CollectorType.SERIAL_OLD.getCollectorName())) {
					return ResultBuilder.createFor(GcInvertedParallelismRule.this, valueProvider)
							.setSeverity(Severity.IGNORE).build();
				}
				IItemCollection gcCpuTimeItems = items.apply(JdkFilters.GC_CPU_TIME);
				long invertedParallelismCount = 0;
				for (IItemIterable item : gcCpuTimeItems) {
					for (IItem event : item) {
						long timeUser = RulesToolkit.getValue(event, JdkAttributes.GC_TIME_USER)
								.longValueIn(UnitLookup.NANOSECOND);
						long timeSystem = RulesToolkit.getValue(event, JdkAttributes.GC_TIME_SYSTEM)
								.longValueIn(UnitLookup.NANOSECOND);
						long timeReal = RulesToolkit.getValue(event, JdkAttributes.GC_TIME_REAL)
								.longValueIn(UnitLookup.NANOSECOND);
						if (isInvertedParallelism(calcParallelism(timeUser, timeSystem, timeReal))) {
							invertedParallelismCount++;
						}
					}
				}
				if (invertedParallelismCount == 0) {
					return ResultBuilder.createFor(GcInvertedParallelismRule.this, valueProvider)
							.setSeverity(Severity.OK).setSummary("OK").build();
				} else {
					return ResultBuilder.createFor(GcInvertedParallelismRule.this, valueProvider)
							.addResult(INVERTED_PARALLELISM_COUNT, invertedParallelismCount)
							.addResult(GC_CPU_TIME_EVENT_COUNT, gcCpuTimeItems.getAggregate(Aggregators.count()))
							.setSeverity(Severity.WARNING)
							.setSummary(Messages.getString(Messages.GcInvertedParallelism_TEXT_WARN_SHORT))
							.setExplanation(Messages.getString(Messages.GcInvertedParallelism_TEXT_WARN_LONG)).build();
				}
			}
		});
	}

	/**
	 * Calculate parallelism, the ratio of user + sys to wall (real) time.
	 * 
	 * @param timeUser
	 *            The user (non-kernel) time.
	 * @param timeSys
	 *            The sys (kernel) time.
	 * @param timeReal
	 *            The wall (clock) time.
	 * @return Percent user:real time rounded up the the nearest whole number.
	 */
	public static int calcParallelism(final long timeUser, final long timeSys, final long timeReal) {
		if (timeReal == 0) {
			if (timeUser == 0 && timeUser == 0) {
				return 100;
			}
			return Integer.MAX_VALUE;
		} else {
			BigDecimal parallelism = new BigDecimal(timeUser);
			parallelism = parallelism.add(new BigDecimal(timeSys));
			BigDecimal hundred = new BigDecimal("100");
			parallelism = parallelism.multiply(hundred);
			parallelism = parallelism.divide(new BigDecimal(timeReal), 0, RoundingMode.CEILING);
			return parallelism.intValue();
		}
	}

	/**
	 * @param parallelism
	 *            The parallelism percent (ratio or user to wall (real time).
	 * @return True if the parallelism is "inverted", false otherwise. Inverted parallelism is less
	 *         than 100. In other words, the parallel collection performance is less than serial
	 *         (single-threaded).
	 */
	public static boolean isInvertedParallelism(int parallelism) {
		return (parallelism < 100);
	}

	@Override
	public Collection<TypedPreference<?>> getConfigurationAttributes() {
		return Collections.emptyList();
	}

	@Override
	public String getId() {
		return INVERTED_PARALLELISM_RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.GcInvertedParallelism_RULE_NAME);
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
