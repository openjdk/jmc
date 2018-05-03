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
import static org.openjdk.jmc.common.unit.UnitLookup.PERCENT;
import static org.openjdk.jmc.common.unit.UnitLookup.PERCENTAGE;
import static org.openjdk.jmc.common.unit.UnitLookup.SECOND;
import static org.openjdk.jmc.common.unit.UnitLookup.TIMESPAN;
import static org.openjdk.jmc.flightrecorder.rules.jdk.RulePreferences.SHORT_RECORDING_LIMIT;

import java.text.MessageFormat;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.IPredicate;
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
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.Pair;
import org.openjdk.jmc.common.util.StringToolkit;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkQueries;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.AbstractRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.rules.util.SlidingWindowToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.SlidingWindowToolkit.IUnorderedWindowValueFunction;

/**
 */
// FIXME: Could possible be merged into the HighJvmCpuRule
public class FewSampledThreadsRule extends AbstractRule {

	private static final String NEW_PARAGRAPH = "<p>"; //$NON-NLS-1$

	private static final IAggregator<Iterable<? extends GroupEntry<IMCThread, CountConsumer>>, ?> SAMPLES_PER_THREAD = GroupingAggregator
			.build(Messages.getString(Messages.FewSampledThreadsRule_AGGR_SAMPLES_PER_THREAD),
					Messages.getString(Messages.FewSampledThreadsRule_AGGR_SAMPLES_PER_THREAD_DESC),
					JfrAttributes.EVENT_THREAD, Aggregators.count(), new IPredicate<IType<IItem>>() {

						@Override
						public boolean evaluate(IType<IItem> type) {
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

	public FewSampledThreadsRule() {
		super("FewSampledThreads", Messages.getString(Messages.FewSampledThreadsRule_RULE_NAME), //$NON-NLS-1$
				JfrRuleTopics.JAVA_APPLICATION_TOPIC, SAMPLED_THREADS_RATIO_LIMIT, MIN_CPU_RATIO_LIMIT,
				SHORT_RECORDING_LIMIT, CPU_WINDOW_SIZE, MIN_SAMPLE_COUNT, MIN_SAMPLE_COUNT_PER_THREAD);
	}

	@Override
	protected Result getResult(IItemCollection items, IPreferenceValueProvider vp) {

		Result availabilityResult = checkAvailability(items);
		if (availabilityResult != null) {
			return availabilityResult;
		}

		// Do rule calculations
		Result ruleResult = calculateResult(items, vp);

		// Add information about short recordings and extra event types
		String longDescription = ruleResult.getLongDescription();
		longDescription = longDescription != null ? longDescription : ""; //$NON-NLS-1$
		double score = ruleResult.getScore();

		String shortRecordingInfo = RulesToolkit.getShortRecordingInfo(items,
				vp.getPreferenceValue(SHORT_RECORDING_LIMIT));
		if (shortRecordingInfo != null) {
			longDescription += NEW_PARAGRAPH + shortRecordingInfo;
			score = score > 0 ? score / 2 : score;
		}

		String extraTypesInfo = getExtraTypesInfo(items);
		if (extraTypesInfo != null) {
			longDescription += NEW_PARAGRAPH + extraTypesInfo;
		}

		return new Result(this, score, ruleResult.getShortDescription(), longDescription, ruleResult.getItemQuery());
	}

	private Result calculateResult(IItemCollection items, IPreferenceValueProvider vp) {
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
		Result idleResult = getIdleResult(items, minCpuRatio, windowSize, sampledThreads);
		if (idleResult != null) {
			return idleResult;
		}

		// Are there too few samples in the recording?
		// TODO: Could make this more advanced (calculated expected samples etc), and merge with MethodProfilingRule
		IQuantity totalNumberOfSamples = items
				.getAggregate(Aggregators.count(ItemFilters.type(JdkTypeIDs.EXECUTION_SAMPLE)));
		if (totalNumberOfSamples.compareTo(minSampleCount) < 0) {
			return new Result(this, Result.NOT_APPLICABLE,
					MessageFormat.format(Messages.getString(Messages.FewSampledThreadsRule_TEXT_NOT_ENOUGH_SAMPLES),
							totalNumberOfSamples.displayUsing(IDisplayable.AUTO),
							minSampleCount.displayUsing(IDisplayable.AUTO)));
		}

		// Are there more sampled threads than cores?
		long hwThreads = getHardwareThreads(items).longValue();
		if (threadsWithEnoughSamples >= hwThreads) {
			String shortDescription = Messages.getString(Messages.FewSampledThreadsRule_TEXT_OK);
			String longDescription = shortDescription + "<p>" //$NON-NLS-1$
					+ Messages.getString(Messages.FewSampledThreadsRule_TEXT_OK_LONG);
			return new Result(this, 0, shortDescription, longDescription);
		}

		// FIXME: Alter calculation to able to name/describe pref value better...
		double sampledThreadRatio = ((double) threadsWithEnoughSamples) / hwThreads;
		double score = RulesToolkit.mapExp74(1 - sampledThreadRatio, sampledThreadRatioLimit);

		String shortDescription = Messages.getString(Messages.FewSampledThreadsRule_TEXT_INFO);
		String longDescription = shortDescription + "<p>" //$NON-NLS-1$
				+ MessageFormat.format(Messages.getString(Messages.FewSampledThreadsRule_TEXT_INFO_LONG),
						minSampleCountPerThread, threadsWithEnoughSamples, hwThreads);
		return new Result(this, score, shortDescription, longDescription, JdkQueries.EXECUTION_SAMPLE);
	}

	private Result getIdleResult(
		IItemCollection items, IQuantity minCpuRatio, IQuantity windowSize, int sampledThreads) {
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
					.getAggregate(Aggregators.max(JdkAttributes.NUMBER_OF_CORES));
			IQuantity maxSingleThreadedCpu = PERCENT.quantity(100 / cores.doubleValue());

			IQuantity maxCpuForSampledThreads = PERCENT
					.quantity(Math.min(100, maxSingleThreadedCpu.multiply(sampledThreads).doubleValue()));
			IQuantity cpuRatio = PERCENT.quantity(jvmUsage.ratioTo(maxCpuForSampledThreads) * 100);

			if (cpuRatio.compareTo(minCpuRatio) < 0) {
				String shortDescription = MessageFormat.format(
						Messages.getString(Messages.FewSampledThreadsRule_APPLICATION_IDLE), jvmUsage,
						jvmUsageMaxWindow.right.getExtent().displayUsing(IDisplayable.AUTO),
						jvmUsageMaxWindow.right.getStart().displayUsing(IDisplayable.AUTO));
				String longDescription = shortDescription + "<p>" //$NON-NLS-1$
						+ Messages.getString(Messages.FewSampledThreadsRule_APPLICATION_IDLE_LONG);
				return new Result(this, 0, shortDescription, longDescription);
			}
		}
		return null;
	}

	private Result checkAvailability(IItemCollection items) {
		// Check event availability
		EventAvailability eventAvailability = RulesToolkit.getEventAvailability(items, JdkTypeIDs.RECORDING_SETTING,
				JdkTypeIDs.EXECUTION_SAMPLE, JdkTypeIDs.CPU_INFORMATION);
		if (eventAvailability != EventAvailability.AVAILABLE) {
			return RulesToolkit.getEventAvailabilityResult(this, items, eventAvailability, JdkTypeIDs.RECORDING_SETTING,
					JdkTypeIDs.EXECUTION_SAMPLE, JdkTypeIDs.CPU_INFORMATION);
		}
		IItemCollection cpuItems = getCpuItems(items);
		IType<IItem> cpuLoadType = RulesToolkit.getType(cpuItems, JdkTypeIDs.CPU_LOAD);
		if (!cpuLoadType.hasAttribute(JdkAttributes.JVM_USER)) {
			return RulesToolkit.getMissingAttributeResult(this, cpuLoadType, JdkAttributes.JVM_USER);
		}

		IQuantity hwThreadsQ = getHardwareThreads(items);
		if (hwThreadsQ == null) {
			return RulesToolkit.getTooFewEventsResult(this);
		}
		return null;
	}

	private static String getExtraTypesInfo(IItemCollection items) {
		String[] extraTypes = new String[] {JdkTypeIDs.CPU_LOAD};
		EventAvailability extraEventAvailability = RulesToolkit.getEventAvailability(items, extraTypes);
		if (!(extraEventAvailability == EventAvailability.AVAILABLE
				|| extraEventAvailability == EventAvailability.ENABLED)) {
			return MessageFormat.format(Messages.getString(Messages.ApplicationHaltsRule_EXTRA_EVENT_TYPES),
					StringToolkit.join(extraTypes, ", ")); //$NON-NLS-1$
		}
		return null;
	}

	private static IItemCollection getCpuItems(IItemCollection items) {
		return items.apply(JdkFilters.CPU_LOAD);
	}

	private static IQuantity getHardwareThreads(IItemCollection items) {
		return items.apply(ItemFilters.type(JdkTypeIDs.CPU_INFORMATION))
				.getAggregate(Aggregators.max(JdkAttributes.HW_THREADS));
	}
}
