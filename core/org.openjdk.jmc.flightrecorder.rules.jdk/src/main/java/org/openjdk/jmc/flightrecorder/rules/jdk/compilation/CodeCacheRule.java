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
package org.openjdk.jmc.flightrecorder.rules.jdk.compilation;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.StringToolkit;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.common.version.JavaVersion;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkQueries;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

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

	private static class CodeHeapData implements Comparable<CodeHeapData> {
		private String name;
		private IQuantity ratio;

		CodeHeapData(String name, IQuantity ratio) {
			this.name = name;
			this.ratio = ratio;
		}

		IQuantity getRatio() {
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

	private Result getResult(IItemCollection items, IPreferenceValueProvider valueProvider) {
		EventAvailability eventAvailability = RulesToolkit.getEventAvailability(items, JdkTypeIDs.CODE_CACHE_FULL,
				JdkTypeIDs.VM_INFO, JdkTypeIDs.CODE_CACHE_STATISTICS, JdkTypeIDs.CODE_CACHE_CONFIG);
		if (eventAvailability != EventAvailability.ENABLED && eventAvailability != EventAvailability.AVAILABLE) {
			return RulesToolkit.getEventAvailabilityResult(this, items, eventAvailability, JdkTypeIDs.CODE_CACHE_FULL,
					JdkTypeIDs.VM_INFO, JdkTypeIDs.CODE_CACHE_STATISTICS, JdkTypeIDs.CODE_CACHE_CONFIG);
		}
		eventAvailability = RulesToolkit.getEventAvailability(items, JdkTypeIDs.CODE_CACHE_CONFIG);
		if (eventAvailability != EventAvailability.AVAILABLE) {
			return RulesToolkit.getEventAvailabilityResult(this, items, eventAvailability,
					JdkTypeIDs.CODE_CACHE_CONFIG);
		}

		// Check if this is an early unsupported recording
		IItemCollection ccItems = items.apply(JdkFilters.CODE_CACHE_CONFIGURATION);
		IType<IItem> ccType = RulesToolkit.getType(ccItems, JdkTypeIDs.CODE_CACHE_CONFIG);
		IQuantity ccFullCount = items.getAggregate(JdkAggregators.CODE_CACHE_FULL_COUNT);
		if (ccFullCount != null && ccFullCount.doubleValue() > 0) {
			String shortDescription = Messages.getString(Messages.CodeCacheRuleFactory_TEXT_WARN);
			String longDescription = shortDescription + "<p>" //$NON-NLS-1$
					+ Messages.getString(Messages.CodeCacheRuleFactory_TEXT_WARN_LONG) + "<p>" //$NON-NLS-1$
					+ Messages.getString(Messages.CodeCacheRuleFactory_BLOG_REFERENCE);
			return new Result(this, 100, shortDescription, longDescription, JdkQueries.CODE_CACHE_FULL);
		}
		IQuantity infoPreferenceValue = valueProvider.getPreferenceValue(CODE_CACHE_SIZE_INFO_PERCENT);
		IQuantity warningPreferenceValue = valueProvider.getPreferenceValue(CODE_CACHE_SIZE_WARN_PERCENT);
		double allocationRatioScore = 0;
		String shortDescription = null;
		String longDescription = null;
		if (hasSegmentedCodeCache(items)) {
			if (!ccType.hasAttribute(JdkAttributes.PROFILED_SIZE)) {
				return RulesToolkit.getMissingAttributeResult(this, ccType, JdkAttributes.PROFILED_SIZE);
			}
			IQuantity profiledAggregate = items
					.getAggregate(Aggregators.filter(Aggregators.min(JdkAttributes.UNALLOCATED),
							ItemFilters.matches(JdkAttributes.CODE_HEAP, PROFILED_NAME)));
			IQuantity profiledRatio = null;
			if (profiledAggregate != null) {
				profiledRatio = UnitLookup.PERCENT_UNITY.quantity(
						profiledAggregate.ratioTo(items.getAggregate(Aggregators.min(JdkAttributes.PROFILED_SIZE))));
			} else {
				profiledRatio = UnitLookup.PERCENT_UNITY.quantity(1.0);
			}
			IQuantity nonProfiledAggregate = items
					.getAggregate(Aggregators.filter(Aggregators.min(JdkAttributes.UNALLOCATED),
							ItemFilters.matches(JdkAttributes.CODE_HEAP, NON_PROFILED_NAME)));
			IQuantity nonProfiledRatio = null;
			if (nonProfiledAggregate != null) {
				nonProfiledRatio = UnitLookup.PERCENT_UNITY.quantity(nonProfiledAggregate
						.ratioTo(items.getAggregate(Aggregators.min(JdkAttributes.NON_PROFILED_SIZE))));
			} else {
				nonProfiledRatio = UnitLookup.PERCENT_UNITY.quantity(1.0);
			}

			IQuantity nonNMethodsRatio = UnitLookup.PERCENT_UNITY.quantity(items
					.getAggregate(Aggregators.filter(Aggregators.min(JdkAttributes.UNALLOCATED),
							ItemFilters.matches(JdkAttributes.CODE_HEAP, NON_NMETHODS_NAME)))
					.ratioTo(items.getAggregate(Aggregators.min(JdkAttributes.NON_NMETHOD_SIZE))));
			List<CodeHeapData> heaps = new ArrayList<>();
			addIfHalfFull(profiledRatio, heaps, PROFILED_NAME);
			addIfHalfFull(nonProfiledRatio, heaps, NON_PROFILED_NAME);
			addIfHalfFull(nonNMethodsRatio, heaps, NON_NMETHODS_NAME);
			IQuantity worstRatio;
			Collections.sort(heaps);
			if (heaps.size() > 0) {
				if (heaps.size() > 1) {
					shortDescription = MessageFormat.format(
							Messages.getString(Messages.CodeCacheRuleFactory_WARN_SEGMENTED_HEAPS_SHORT_DESCRIPTION),
							StringToolkit.join(heaps, ",")); //$NON-NLS-1$
				} else {
					shortDescription = MessageFormat.format(
							Messages.getString(Messages.CodeCacheRuleFactory_WARN_SEGMENTED_HEAP_SHORT_DESCRIPTION),
							heaps.get(0));
				}
				longDescription = shortDescription + " " //$NON-NLS-1$
						+ Messages.getString(Messages.CodeCacheRuleFactory_WARN_LONG_DESCRIPTION) + "<p>" //$NON-NLS-1$
						+ Messages.getString(Messages.CodeCacheRuleFactory_DEFAULT_LONG_DESCRIPTION) + "<p>" //$NON-NLS-1$
						+ Messages.getString(Messages.CodeCacheRuleFactory_BLOG_REFERENCE);
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
				return RulesToolkit.getMissingAttributeResult(this, ccType, JdkAttributes.RESERVED_SIZE);
			}
			IQuantity codeCacheReserved = items
					.getAggregate(Aggregators.min(JdkTypeIDs.CODE_CACHE_CONFIG, JdkAttributes.RESERVED_SIZE));
			IQuantity unallocated = items
					.getAggregate(Aggregators.min(JdkTypeIDs.CODE_CACHE_STATISTICS, JdkAttributes.UNALLOCATED));
			IQuantity unallocatedCodeCachePercent = UnitLookup.PERCENT_UNITY
					.quantity(unallocated.ratioTo(codeCacheReserved));
			allocationRatioScore = RulesToolkit.mapExp100(
					100 - unallocatedCodeCachePercent.doubleValueIn(UnitLookup.PERCENT),
					infoPreferenceValue.doubleValueIn(UnitLookup.PERCENT),
					warningPreferenceValue.doubleValueIn(UnitLookup.PERCENT));
			shortDescription = MessageFormat.format(Messages.getString(Messages.CodeCacheRuleFactory_JDK8_TEXT_WARN),
					unallocatedCodeCachePercent.displayUsing(IDisplayable.AUTO));
			longDescription = shortDescription + "<p>" //$NON-NLS-1$
					+ Messages.getString(Messages.CodeCacheRuleFactory_DEFAULT_LONG_DESCRIPTION) + "<p>" //$NON-NLS-1$
					+ Messages.getString(Messages.CodeCacheRuleFactory_BLOG_REFERENCE);
		}
		if (allocationRatioScore >= 25) {
			// FIXME: Include configured value of code cache size in long description
			return new Result(this, allocationRatioScore, shortDescription, longDescription);
		}
		// FIXME: Show calculated free value also in ok text
		return new Result(this, allocationRatioScore, Messages.getString(Messages.CodeCacheRuleFactory_TEXT_OK));
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
		return JfrRuleTopics.CODE_CACHE_TOPIC;
	}
}
