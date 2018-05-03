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
package org.openjdk.jmc.flightrecorder.rules.jdk.cpu;

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
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

// FIXME: This rule seems to be a precondition for other rules (Method profiling rules). Remove?
public class HighJvmCpuRule implements IRule {

	private static final int MAX_SAMPLED_THREADS = 5;

	private static final String RESULT_ID = "HighJvmCpu"; //$NON-NLS-1$

	public static final TypedPreference<IQuantity> MISSING_SAMPLE_LIMIT = new TypedPreference<>("missing.sample.limit", //$NON-NLS-1$
			Messages.getString(Messages.HighJvmCpuRule_CONFIG_SAMPLE_LIMIT),
			Messages.getString(Messages.HighJvmCpuRule_CONFIG_SAMPLE_LIMIT_LONG), NUMBER, NUMBER_UNITY.quantity(0.5));

	public static final TypedPreference<IQuantity> MINIMUM_CPU_LOAD_PERIOD = new TypedPreference<>("minimum.cpu.period", //$NON-NLS-1$
			Messages.getString(Messages.HighJvmCpuRule_CONFIG_MIN_CPULOAD),
			Messages.getString(Messages.HighJvmCpuRule_CONFIG_MIN_CPU_LIMIT_LONG), UnitLookup.TIMESPAN,
			UnitLookup.SECOND.quantity(10));

	public static final TypedPreference<IQuantity> JVM_CPU_INFO_LIMIT = new TypedPreference<>("jvm.cpu.info.limit", //$NON-NLS-1$
			Messages.getString(Messages.HighJvmCpuRule_CONFIG_CPU_INFO_LIMIT),
			Messages.getString(Messages.HighJvmCpuRule_CONFIG_CPU_INFO_LIMIT_LONG), NUMBER, NUMBER_UNITY.quantity(80));

	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays
			.<TypedPreference<?>> asList(JVM_CPU_INFO_LIMIT, MINIMUM_CPU_LOAD_PERIOD, MISSING_SAMPLE_LIMIT);

	private static final IAggregator<IQuantity, ?> MAX_ENDTIME = Aggregators.max(
			Messages.getString(Messages.HighJvmCpuRule_AGGR_MAX_ENDTIME), null, JdkTypeIDs.CPU_LOAD,
			JfrAttributes.END_TIME);

	private static final IAggregator<IQuantity, ?> MIN_ENDTIME = Aggregators.min(
			Messages.getString(Messages.HighJvmCpuRule_AGGR_MIN_ENDTIME), null, JdkTypeIDs.CPU_LOAD,
			JfrAttributes.END_TIME);

	// FIXME: The implementation seems to assume that all quantities have the same unit
	private Result getResult(IItemCollection items, IPreferenceValueProvider vp) {
		EventAvailability eventAvailability = RulesToolkit.getEventAvailability(items, JdkTypeIDs.CPU_LOAD);
		if (eventAvailability != EventAvailability.AVAILABLE) {
			return RulesToolkit.getEventAvailabilityResult(this, items, eventAvailability, JdkTypeIDs.CPU_LOAD);
		}

		String periodNotBelow = RulesToolkit.getPeriodIfGreaterThan(items, vp.getPreferenceValue(MINIMUM_CPU_LOAD_PERIOD),
				JdkTypeIDs.CPU_LOAD);
		if (periodNotBelow != null) {
			// FIXME: Should the score be hard-coded to 50 here?
			return new Result(this, 50, Messages.getString(Messages.HighJvmCpuRule_LONG_CPU_LOAD_PERIOD), MessageFormat
					.format(Messages.getString(Messages.HighJvmCpuRule_LONG_CPU_LOAD_PERIOD_LONG), periodNotBelow));
		}

		// This is for returning a helpful result if old recordings are encountered
		IItemCollection cpuItems = items.apply(JdkFilters.CPU_LOAD);
		IType<IItem> cpuLoadType = RulesToolkit.getType(cpuItems, JdkTypeIDs.CPU_LOAD);
		if (!cpuLoadType.hasAttribute(JdkAttributes.JVM_USER)) {
			return RulesToolkit.getMissingAttributeResult(this, cpuLoadType, JdkAttributes.JVM_USER);
		}

		// FIXME: Just looking at the overall average is not enough (it is recording length dependent)
		IQuantity jvmUsage = cpuItems.getAggregate(JdkAggregators.AVG_JVM_USER_CPU);
		IQuantity profilingSamples = items.getAggregate(JdkAggregators.EXECUTION_SAMPLE_COUNT);
		IQuantity maxPeriodProfiling = RulesToolkit.getSettingMaxPeriod(items, JdkTypeIDs.EXECUTION_SAMPLE);
		if (profilingSamples != null && profilingSamples.clampedLongValueIn(UnitLookup.NUMBER_UNITY) > 0
				&& maxPeriodProfiling != null) {
			// FIXME: Should check how many threads were actually sampled.
			double maximumSamplesPerSecond = UnitLookup.SECOND.quantity(1).ratioTo(maxPeriodProfiling)
					* HighJvmCpuRule.MAX_SAMPLED_THREADS;
			// FIXME: Will checking the cpuItems be correct enough to represent recording length?
			// FIXME: Dependence on recording length is bad
			IQuantity first = cpuItems.getAggregate(MIN_ENDTIME);
			IQuantity last = cpuItems.getAggregate(MAX_ENDTIME);
			double lengthInSeconds = last == null ? 0 : last.subtract(first).doubleValueIn(UnitLookup.SECOND);
			double samples = profilingSamples.doubleValue();
			double samplesPerSecond = samples / lengthInSeconds;
			double lackingSamplesRatio = 1 - (samplesPerSecond / maximumSamplesPerSecond);
			double jvmVal = jvmUsage.doubleValue();
			double lackingSamplesTimesCpu = lackingSamplesRatio * jvmVal;
			// FIXME: Should we check the entire recording, or time windows?
			double missingSampleLimit = vp.getPreferenceValue(MISSING_SAMPLE_LIMIT).doubleValue();
			if (lackingSamplesTimesCpu >= missingSampleLimit) {
				double missingSamplesScore = RulesToolkit.mapExp74(lackingSamplesTimesCpu, missingSampleLimit);
				String shortMessage = Messages.getString(Messages.HighJvmCpuRule_FEW_SAMPLES);
				String longMessage = shortMessage + " " + Messages.getString(Messages.HighJvmCpuRule_FEW_SAMPLES_LONG); //$NON-NLS-1$
				return new Result(this, missingSamplesScore, shortMessage, longMessage);
			}
		}
		long infoLimit = vp.getPreferenceValue(JVM_CPU_INFO_LIMIT).longValue();
		double jvmUsageScore = RulesToolkit.mapExp74(jvmUsage.doubleValueIn(UnitLookup.PERCENT), infoLimit);
		if (jvmUsageScore >= infoLimit) {
			// FIXME: This case (or similar) should be replaced with evaluating the method profiling rule
			return new Result(this, jvmUsageScore, Messages.getString(Messages.HighJvmCpuRule_TEXT_WARN));
		}
		return new Result(this, jvmUsageScore, Messages.getString(Messages.HighJvmCpuRule_TEXT_OK));
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
		return Messages.getString(Messages.HighJvmCpuRule_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.JAVA_APPLICATION_TOPIC;
	}
}
