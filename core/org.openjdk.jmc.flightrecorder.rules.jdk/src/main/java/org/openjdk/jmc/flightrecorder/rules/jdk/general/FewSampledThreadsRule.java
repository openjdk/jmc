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

import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER;
import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER_UNITY;
import static org.openjdk.jmc.common.unit.UnitLookup.PERCENT;
import static org.openjdk.jmc.common.unit.UnitLookup.PERCENTAGE;
import static org.openjdk.jmc.common.unit.UnitLookup.SECOND;
import static org.openjdk.jmc.common.unit.UnitLookup.TIMESPAN;
import static org.openjdk.jmc.flightrecorder.rules.jdk.RulePreferences.SHORT_RECORDING_LIMIT;

import java.util.Arrays;
import java.util.function.Predicate;

import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.Aggregators.CountConsumer;
import org.openjdk.jmc.common.item.GroupingAggregator;
import org.openjdk.jmc.common.item.GroupingAggregator.GroupEntry;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.Pair;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.AbstractRule;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.ResultBuilder;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.RequiredEventsBuilder;
import org.openjdk.jmc.flightrecorder.rules.util.SlidingWindowToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.SlidingWindowToolkit.IUnorderedWindowValueFunction;

/**
 */
// FIXME: Could possible be merged into the HighJvmCpuRule
public class FewSampledThreadsRule extends AbstractRule {

	private static final IAggregator<Iterable<? extends GroupEntry<IMCThread, CountConsumer>>, ?> SAMPLES_PER_THREAD = GroupingAggregator
			.build(Messages.getString(Messages.FewSampledThreadsRule_AGGR_SAMPLES_PER_THREAD),
					Messages.getString(Messages.FewSampledThreadsRule_AGGR_SAMPLES_PER_THREAD_DESC),
					JfrAttributes.EVENT_THREAD, Aggregators.count(), new Predicate<IType<IItem>>() {

						@Override
						public boolean test(IType<IItem> type) {
							return type.getIdentifier().equals(JdkTypeIDs.EXECUTION_SAMPLE);
						}
					});

	public static final TypedPreference<IQuantity> SAMPLED_THREADS_RATIO_LIMIT = new TypedPreference<>(
			"sampled.threads.ratio.limit", //$NON-NLS-1$
			Messages.getString(Messages.FewSampledThreadsRule_SAMPLED_THREADS_RATIO_WARNING_LIMIT),
			Messages.getString(Messages.FewSampledThreadsRule_SAMPLED_THREADS_RATIO_WARNING_LIMIT_LONG), NUMBER,
			NUMBER_UNITY.quantity(0.25));

	public static final TypedPreference<IQuantity> MIN_CPU_RATIO_LIMIT = new TypedPreference<>("min.cpu.per.core.limit", //$NON-NLS-1$
			Messages.getString(Messages.FewSampledThreadsRule_MIN_CPU_RATIO),
			Messages.getString(Messages.FewSampledThreadsRule_MIN_CPU_RATIO_LONG), PERCENTAGE, PERCENT.quantity(10));

	public static final TypedPreference<IQuantity> CPU_WINDOW_SIZE = new TypedPreference<>("cpu.window.size", //$NON-NLS-1$
			Messages.getString(Messages.FewSampledThreadsRule_CPU_WINDOW_SIZE),
			Messages.getString(Messages.FewSampledThreadsRule_CPU_WINDOW_SIZE_LONG), TIMESPAN, SECOND.quantity(10));

	public static final TypedPreference<IQuantity> MIN_SAMPLE_COUNT = new TypedPreference<>("min.sample.count", //$NON-NLS-1$
			Messages.getString(Messages.FewSampledThreadsRule_MIN_SAMPLE_COUNT),
			Messages.getString(Messages.FewSampledThreadsRule_MIN_SAMPLE_COUNT_LONG), NUMBER,
			NUMBER_UNITY.quantity(20));

	public static final TypedPreference<IQuantity> MIN_SAMPLE_COUNT_PER_THREAD = new TypedPreference<>(
			"min.sample.count.per.thread", //$NON-NLS-1$
			Messages.getString(Messages.FewSampledThreadsRule_MIN_SAMPLE_COUNT_PER_THREAD),
			Messages.getString(Messages.FewSampledThreadsRule_MIN_SAMPLE_COUNT_PER_THREAD_LONG), NUMBER,
			NUMBER_UNITY.quantity(4));

	public static final TypedResult<IQuantity> HW_THREADS = new TypedResult<>("hwThreads", "Hardware Threads", //$NON-NLS-1$
			"The number of hardware threads available.", UnitLookup.NUMBER, IQuantity.class);
	public static final TypedResult<IQuantity> THREADS_WITH_ENOUGH_SAMPLES = new TypedResult<>(
			"threadsWithEnoughSamples", "Threads With Enough Samples", "The number of threads that had enough samples.", //$NON-NLS-1$
			UnitLookup.NUMBER, IQuantity.class);
	public static final TypedResult<IRange<IQuantity>> MAX_WINDOW = new TypedResult<>("maxWindow", "Max Window", //$NON-NLS-1$
			"The window where the maximum JVM CPU usage was detected.", UnitLookup.TIMERANGE);
	public static final TypedResult<IQuantity> JVM_USAGE = new TypedResult<>("jvmUsage", "JVM CPU Usage", //$NON-NLS-1$
			"The amount of CPU used by the JVM.", UnitLookup.PERCENTAGE, IQuantity.class);

	public FewSampledThreadsRule() {
		super("FewSampledThreads", Messages.getString(Messages.FewSampledThreadsRule_RULE_NAME), //$NON-NLS-1$
				JfrRuleTopics.JAVA_APPLICATION,
				Arrays.<TypedPreference<?>> asList(SAMPLED_THREADS_RATIO_LIMIT, MIN_CPU_RATIO_LIMIT,
						SHORT_RECORDING_LIMIT, CPU_WINDOW_SIZE, MIN_SAMPLE_COUNT, MIN_SAMPLE_COUNT_PER_THREAD),
				Arrays.<TypedResult<?>> asList(TypedResult.SCORE, HW_THREADS, THREADS_WITH_ENOUGH_SAMPLES, MAX_WINDOW,
						JVM_USAGE),
				RequiredEventsBuilder.create().addEventType(JdkTypeIDs.RECORDING_SETTING, EventAvailability.AVAILABLE)
						.addEventType(JdkTypeIDs.EXECUTION_SAMPLE, EventAvailability.AVAILABLE)
						.addEventType(JdkTypeIDs.CPU_INFORMATION, EventAvailability.AVAILABLE).build());
	}

	public static final TypedResult<IQuantity> TOTAL_SAMPLES = new TypedResult<>("totalSamples", "Total Samples", //$NON-NLS-1$
			"The total number of execution samples.", UnitLookup.NUMBER, IQuantity.class);

	@Override
	protected IResult getResult(
		IItemCollection items, IPreferenceValueProvider vp, IResultValueProvider resultProvider) {
		double sampledThreadRatioLimit = vp.getPreferenceValue(SAMPLED_THREADS_RATIO_LIMIT).doubleValueIn(NUMBER_UNITY);
		IQuantity minCpuRatio = vp.getPreferenceValue(MIN_CPU_RATIO_LIMIT);
		IQuantity windowSize = vp.getPreferenceValue(CPU_WINDOW_SIZE);
		IQuantity minSampleCountPerThread = vp.getPreferenceValue(MIN_SAMPLE_COUNT_PER_THREAD);
		IQuantity minSampleCount = vp.getPreferenceValue(MIN_SAMPLE_COUNT);

		// How many threads were sampled
		Iterable<? extends GroupEntry<IMCThread, CountConsumer>> samplesPerThread = items
				.getAggregate(SAMPLES_PER_THREAD);
		int threadsWithEnoughSamples = 0;
		int sampledThreads = 0;

		for (GroupEntry<IMCThread, CountConsumer> ge : samplesPerThread) {
			sampledThreads++;
			if (ge.getConsumer().getCount() >= minSampleCountPerThread.doubleValue()) {
				threadsWithEnoughSamples++;
			}
		}

		// Was the application using CPU, or was it just idling
		IResult idleResult = getIdleResult(items, minCpuRatio, windowSize, sampledThreads, vp);
		if (idleResult != null) {
			return idleResult;
		}

		// Are there too few samples in the recording?
		// TODO: Could make this more advanced (calculated expected samples etc), and merge with MethodProfilingRule
		IQuantity totalNumberOfSamples = items
				.getAggregate(Aggregators.count(ItemFilters.type(JdkTypeIDs.EXECUTION_SAMPLE)));
		if (totalNumberOfSamples.compareTo(minSampleCount) < 0) {
			return ResultBuilder.createFor(this, vp).setSeverity(Severity.NA)
					.addResult(TOTAL_SAMPLES, totalNumberOfSamples)
					.setSummary(Messages.getString(Messages.FewSampledThreadsRule_TEXT_NOT_ENOUGH_SAMPLES)).build();
		}

		// Are there more sampled threads than cores?
		IQuantity hwThreads = getHardwareThreads(items);
		if (threadsWithEnoughSamples >= hwThreads.longValue()) {
			return ResultBuilder.createFor(this, vp).setSeverity(Severity.OK)
					.setSummary(Messages.getString(Messages.FewSampledThreadsRule_TEXT_OK))
					.setExplanation(Messages.getString(Messages.FewSampledThreadsRule_TEXT_OK_LONG)).build();
		}

		// FIXME: Alter calculation to able to name/describe pref value better...
		double sampledThreadRatio = ((double) threadsWithEnoughSamples) / hwThreads.longValue();
		double score = RulesToolkit.mapExp74(1 - sampledThreadRatio, sampledThreadRatioLimit);
		return ResultBuilder.createFor(this, vp).setSeverity(Severity.get(score))
				.addResult(THREADS_WITH_ENOUGH_SAMPLES, UnitLookup.NUMBER_UNITY.quantity(threadsWithEnoughSamples))
				.addResult(HW_THREADS, hwThreads)
				.setSummary(Messages.getString(Messages.FewSampledThreadsRule_TEXT_INFO))
				.setExplanation(Messages.getString(Messages.FewSampledThreadsRule_TEXT_INFO_LONG)).build();
	}

	private IResult getIdleResult(
		IItemCollection items, IQuantity minCpuRatio, IQuantity windowSize, int sampledThreads,
		IPreferenceValueProvider vp) {
		IItemCollection cpuItems = getCpuItems(items);
		Pair<IQuantity, IRange<IQuantity>> jvmUsageMaxWindow = SlidingWindowToolkit.slidingWindowUnorderedMinMaxValue(
				cpuItems, windowSize, evaluationTask, new IUnorderedWindowValueFunction<IQuantity>() {

					@Override
					public IQuantity getValue(IItemCollection items, IQuantity startTime, IQuantity endTime) {
						return items.getAggregate(JdkAggregators.AVG_JVM_TOTAL_CPU);
					}
				}, true, false);
		if (jvmUsageMaxWindow != null) {
			IQuantity jvmUsage = jvmUsageMaxWindow.left;

			// How much cpu could max be used if this is a single threaded application (not counting the JVM threads though...)
			IQuantity cores = items.apply(ItemFilters.type(JdkTypeIDs.CPU_INFORMATION))
					.getAggregate((IAggregator<IQuantity, ?>) Aggregators.max(JdkAttributes.NUMBER_OF_CORES));
			IQuantity maxSingleThreadedCpu = PERCENT.quantity(100 / cores.doubleValue());

			IQuantity maxCpuForSampledThreads = PERCENT
					.quantity(Math.min(100, maxSingleThreadedCpu.multiply(sampledThreads).doubleValue()));
			IQuantity cpuRatio = PERCENT.quantity(jvmUsage.ratioTo(maxCpuForSampledThreads) * 100);

			if (cpuRatio.compareTo(minCpuRatio) < 0) {
				return ResultBuilder.createFor(this, vp).setSeverity(Severity.OK)
						.addResult(MAX_WINDOW, jvmUsageMaxWindow.right).addResult(JVM_USAGE, jvmUsage)
						.setSummary(Messages.getString(Messages.FewSampledThreadsRule_APPLICATION_IDLE))
						.setExplanation(Messages.getString(Messages.FewSampledThreadsRule_APPLICATION_IDLE_LONG))
						.build();
			}
		}
		return null;
	}

	private static IItemCollection getCpuItems(IItemCollection items) {
		return items.apply(JdkFilters.CPU_LOAD);
	}

	private static IQuantity getHardwareThreads(IItemCollection items) {
		return items.apply(ItemFilters.type(JdkTypeIDs.CPU_INFORMATION))
				.getAggregate((IAggregator<IQuantity, ?>) Aggregators.max(JdkAttributes.HW_THREADS));
	}
}
