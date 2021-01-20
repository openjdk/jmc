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
package org.openjdk.jmc.flightrecorder.rules.jdk.compilation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.common.version.JavaVersion;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
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
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.RequiredEventsBuilder;

public class CodeCacheRule implements IRule {

	public static final String CODE_CACHE_RESULT_ID = "CodeCache"; //$NON-NLS-1$

	private static final String NON_PROFILED_NAME = "CodeHeap 'non-profiled nmethods'"; //$NON-NLS-1$
	private static final String PROFILED_NAME = "CodeHeap 'profiled nmethods'"; //$NON-NLS-1$
	private static final String NON_NMETHODS_NAME = "CodeHeap 'non-nmethods'"; //$NON-NLS-1$

	public static final TypedPreference<IQuantity> CODE_CACHE_SIZE_INFO_PERCENT = new TypedPreference<>(
			"codeCache.size.info.limit", Messages.getString(Messages.CodeCacheRuleFactory_SIZE_INFO_LIMIT), //$NON-NLS-1$
			Messages.getString(Messages.CodeCacheRuleFactory_SIZE_INFO_LIMIT_DESC), UnitLookup.PERCENTAGE,
			UnitLookup.PERCENT.quantity(50));
	public static final TypedPreference<IQuantity> CODE_CACHE_SIZE_WARN_PERCENT = new TypedPreference<>(
			"codeCache.size.warn.limit", Messages.getString(Messages.CodeCacheRuleFactory_SIZE_WARN_LIMIT), //$NON-NLS-1$
			Messages.getString(Messages.CodeCacheRuleFactory_SIZE_WARN_LIMIT_DESC), UnitLookup.PERCENTAGE,
			UnitLookup.PERCENT.quantity(80));
	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays
			.<TypedPreference<?>> asList(CODE_CACHE_SIZE_INFO_PERCENT, CODE_CACHE_SIZE_WARN_PERCENT);

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.VM_INFO, EventAvailability.AVAILABLE)
			.addEventType(JdkTypeIDs.CODE_CACHE_CONFIG, EventAvailability.AVAILABLE)
			.addEventType(JdkTypeIDs.CODE_CACHE_STATISTICS, EventAvailability.AVAILABLE)
			.addEventType(JdkTypeIDs.CODE_CACHE_FULL, EventAvailability.AVAILABLE).build();

	private static class CodeHeapData implements Comparable<CodeHeapData>, IDisplayable {
		private String name;
		private IQuantity ratio;

		CodeHeapData(String name, IQuantity ratio) {
			this.name = name;
			this.ratio = ratio;
		}

		public IQuantity getRatio() {
			return ratio;
		}

		@Override
		public String toString() {
			return name + "(" + ratio.displayUsing(IDisplayable.AUTO) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}

		@Override
		public int compareTo(CodeHeapData o) {
			return ratio.compareTo(o.getRatio());
		}

		@Override
		public int hashCode() {
			return name.hashCode() << ratio.hashCode();
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof CodeHeapData) {
				CodeHeapData other = (CodeHeapData) o;
				return ratio.compareTo(other.ratio) == 0 && name.equals(other.name);
			}
			return false;
		}

		@Override
		public String displayUsing(String formatHint) {
			return name + "(" + ratio.displayUsing(formatHint) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public static final ContentType<CodeHeapData> CODE_HEAP = UnitLookup.createSyntheticContentType("codeHeapData"); //$NON-NLS-1$

	public static final TypedResult<IQuantity> CODE_CACHE_FREE_RATIO = new TypedResult<>("codeCacheFreeRatio", //$NON-NLS-1$
			"Code Cache Free Ratio", "The percentage of the code cache that was free.", UnitLookup.PERCENTAGE, //$NON-NLS-1$//$NON-NLS-2$
			IQuantity.class);
	public static final TypedCollectionResult<CodeHeapData> CODE_HEAPS = new TypedCollectionResult<>("codeHeaps", //$NON-NLS-1$
			"Code Heaps", "The code heaps in the JVM.", CODE_HEAP, CodeHeapData.class); //$NON-NLS-1$ //$NON-NLS-2$

	private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays
			.<TypedResult<?>> asList(TypedResult.SCORE, CODE_CACHE_FREE_RATIO);

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

	private IResult getResult(
		IItemCollection items, IPreferenceValueProvider valueProvider, IResultValueProvider resultProvider) {
		// Check if this is an early unsupported recording
		IItemCollection ccItems = items.apply(JdkFilters.CODE_CACHE_CONFIGURATION);
		IType<IItem> ccType = RulesToolkit.getType(ccItems, JdkTypeIDs.CODE_CACHE_CONFIG);
		IQuantity ccFullCount = items.getAggregate(JdkAggregators.CODE_CACHE_FULL_COUNT);
		if (ccFullCount != null && ccFullCount.doubleValue() > 0) {
			return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.WARNING)
					.setSummary(Messages.getString(Messages.CodeCacheRuleFactory_TEXT_WARN))
					.setExplanation(Messages.getString(Messages.CodeCacheRuleFactory_TEXT_WARN_LONG))
					.setSolution(Messages.getString(Messages.CodeCacheRuleFactory_BLOG_REFERENCE)).build();
		}
		IQuantity infoPreferenceValue = valueProvider.getPreferenceValue(CODE_CACHE_SIZE_INFO_PERCENT);
		IQuantity warningPreferenceValue = valueProvider.getPreferenceValue(CODE_CACHE_SIZE_WARN_PERCENT);
		double allocationRatioScore = 0;
		String longDescription = null;
		ResultBuilder builder = ResultBuilder.createFor(this, valueProvider);
		if (hasSegmentedCodeCache(items)) {
			if (!ccType.hasAttribute(JdkAttributes.PROFILED_SIZE)) {
				return RulesToolkit.getMissingAttributeResult(this, ccType, JdkAttributes.PROFILED_SIZE, valueProvider);
			}
			IQuantity profiledAggregate = items.getAggregate(
					(IAggregator<IQuantity, ?>) Aggregators.filter(Aggregators.min(JdkAttributes.UNALLOCATED),
							ItemFilters.matches(JdkAttributes.CODE_HEAP, PROFILED_NAME)));
			IQuantity profiledRatio = null;
			if (profiledAggregate != null) {
				profiledRatio = UnitLookup.PERCENT_UNITY.quantity(profiledAggregate.ratioTo(
						items.getAggregate((IAggregator<IQuantity, ?>) Aggregators.min(JdkAttributes.PROFILED_SIZE))));
			} else {
				profiledRatio = UnitLookup.PERCENT_UNITY.quantity(1.0);
			}
			IQuantity nonProfiledAggregate = items.getAggregate(
					(IAggregator<IQuantity, ?>) Aggregators.filter(Aggregators.min(JdkAttributes.UNALLOCATED),
							ItemFilters.matches(JdkAttributes.CODE_HEAP, NON_PROFILED_NAME)));
			IQuantity nonProfiledRatio = null;
			if (nonProfiledAggregate != null) {
				nonProfiledRatio = UnitLookup.PERCENT_UNITY.quantity(nonProfiledAggregate.ratioTo(items
						.getAggregate((IAggregator<IQuantity, ?>) Aggregators.min(JdkAttributes.NON_PROFILED_SIZE))));
			} else {
				nonProfiledRatio = UnitLookup.PERCENT_UNITY.quantity(1.0);
			}
			IQuantity nonNMethodsRatio = UnitLookup.PERCENT_UNITY.quantity(items
					.getAggregate(
							(IAggregator<IQuantity, ?>) Aggregators.filter(Aggregators.min(JdkAttributes.UNALLOCATED),
									ItemFilters.matches(JdkAttributes.CODE_HEAP, NON_NMETHODS_NAME)))
					.ratioTo(items.getAggregate(
							(IAggregator<IQuantity, ?>) Aggregators.min(JdkAttributes.NON_NMETHOD_SIZE))));
			List<CodeHeapData> heaps = new ArrayList<>();
			addIfHalfFull(profiledRatio, heaps, PROFILED_NAME);
			addIfHalfFull(nonProfiledRatio, heaps, NON_PROFILED_NAME);
			addIfHalfFull(nonNMethodsRatio, heaps, NON_NMETHODS_NAME);
			IQuantity worstRatio;
			Collections.sort(heaps);
			builder.addResult(CODE_HEAPS, heaps);
			if (heaps.size() > 0) {
				if (heaps.size() > 1) {
					builder.setSummary(
							Messages.getString(Messages.CodeCacheRuleFactory_WARN_SEGMENTED_HEAPS_SHORT_DESCRIPTION));
				} else {
					builder.setSummary(
							Messages.getString(Messages.CodeCacheRuleFactory_WARN_SEGMENTED_HEAP_SHORT_DESCRIPTION));
				}
				longDescription = Messages.getString(Messages.CodeCacheRuleFactory_WARN_LONG_DESCRIPTION) + "\n" //$NON-NLS-1$
						+ Messages.getString(Messages.CodeCacheRuleFactory_DEFAULT_LONG_DESCRIPTION) + "\n" //$NON-NLS-1$
						+ Messages.getString(Messages.CodeCacheRuleFactory_BLOG_REFERENCE);
				builder.setExplanation(longDescription);
				worstRatio = heaps.get(0).getRatio();
			} else {
				/*
				 * FIXME: JMC-5606 - If we end up in this block, then descriptions will not be set.
				 * Either set some reasonable result descriptions or change the code so that we
				 * don't get null descriptions when we create the result.
				 */
				List<IQuantity> ratios = Arrays.asList(profiledRatio, nonProfiledRatio, nonNMethodsRatio);
				Collections.sort(ratios);
				worstRatio = ratios.get(0);
			}
			allocationRatioScore = RulesToolkit.mapExp100(100 - worstRatio.doubleValueIn(UnitLookup.PERCENT),
					infoPreferenceValue.doubleValueIn(UnitLookup.PERCENT),
					warningPreferenceValue.doubleValueIn(UnitLookup.PERCENT));
		} else {
			if (!ccType.hasAttribute(JdkAttributes.RESERVED_SIZE)) {
				return RulesToolkit.getMissingAttributeResult(this, ccType, JdkAttributes.RESERVED_SIZE, valueProvider);
			}
			IQuantity codeCacheReserved = items.getAggregate((IAggregator<IQuantity, ?>) Aggregators
					.min(JdkTypeIDs.CODE_CACHE_CONFIG, JdkAttributes.RESERVED_SIZE));
			IQuantity unallocated = items.getAggregate((IAggregator<IQuantity, ?>) Aggregators
					.min(JdkTypeIDs.CODE_CACHE_STATISTICS, JdkAttributes.UNALLOCATED));
			IQuantity unallocatedCodeCachePercent = UnitLookup.PERCENT_UNITY
					.quantity(unallocated.ratioTo(codeCacheReserved));
			allocationRatioScore = RulesToolkit.mapExp100(
					100 - unallocatedCodeCachePercent.doubleValueIn(UnitLookup.PERCENT),
					infoPreferenceValue.doubleValueIn(UnitLookup.PERCENT),
					warningPreferenceValue.doubleValueIn(UnitLookup.PERCENT));
			builder.setSummary(Messages.getString(Messages.CodeCacheRuleFactory_JDK8_TEXT_WARN))
					.addResult(CODE_CACHE_FREE_RATIO, unallocatedCodeCachePercent);
			longDescription = Messages.getString(Messages.CodeCacheRuleFactory_DEFAULT_LONG_DESCRIPTION) + "\n" //$NON-NLS-1$
					+ Messages.getString(Messages.CodeCacheRuleFactory_BLOG_REFERENCE);
			builder.setExplanation(longDescription);
		}
		if (allocationRatioScore >= 25) {
			// FIXME: Include configured value of code cache size in long description
			return builder.setSeverity(Severity.get(allocationRatioScore))
					.addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(allocationRatioScore)).build();
		}
		// FIXME: Show calculated free value also in ok text
		return builder.setSeverity(Severity.OK).setSummary(Messages.getString(Messages.CodeCacheRuleFactory_TEXT_OK))
				.addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(allocationRatioScore)).build();
	}

	private boolean hasSegmentedCodeCache(IItemCollection items) {
		JavaVersion version = RulesToolkit.getJavaVersion(items);
		if (version != null && version.getMajorVersion() >= 9) {
			return items.apply(ItemFilters.matches(JdkAttributes.FLAG_NAME, "SegmentedCodeCache")) //$NON-NLS-1$
					.getAggregate(Aggregators.and(JdkTypeIDs.BOOLEAN_FLAG, JdkAttributes.FLAG_VALUE_BOOLEAN));
		}
		return false;
	}

	private void addIfHalfFull(IQuantity ratioUnallocated, List<CodeHeapData> heaps, String name) {
		if (ratioUnallocated.compareTo(UnitLookup.PERCENT_UNITY.quantity(0.5)) < 0) {
			heaps.add(new CodeHeapData(name, ratioUnallocated));
		}
	}

	@Override
	public Collection<TypedPreference<?>> getConfigurationAttributes() {
		return CONFIG_ATTRIBUTES;
	}

	@Override
	public String getId() {
		return CODE_CACHE_RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.CodeCacheRuleFactory_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.CODE_CACHE;
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
