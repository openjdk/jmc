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
package org.openjdk.jmc.flightrecorder.rules.jdk.memory;

import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER;
import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER_UNITY;
import static org.openjdk.jmc.common.unit.UnitLookup.PERCENT;
import static org.openjdk.jmc.common.unit.UnitLookup.PERCENTAGE;
import static org.openjdk.jmc.common.unit.UnitLookup.PERCENT_UNITY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.collection.MapToolkit.IntEntry;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.BinaryPrefix;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.memleak.ReferenceTreeModel;
import org.openjdk.jmc.flightrecorder.memleak.ReferenceTreeObject;
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

public class IncreasingLiveSetRule implements IRule {

	/**
	 * Defines the relative amount of live set increase per second that corresponds to a rule score
	 * of 75.
	 */
	private static final double PERCENT_OF_HEAP_INCREASE_PER_SECOND = 0.01;

	private static final String RESULT_ID = "IncreasingLiveSet"; //$NON-NLS-1$

	public static final TypedPreference<IQuantity> CLASSES_LOADED_PERCENT = new TypedPreference<>(
			"memleak.classload.percent", Messages.getString(Messages.IncreasingLiveSetRule_LOADED_CLASSES_PERCENT), //$NON-NLS-1$
			Messages.getString(Messages.IncreasingLiveSetRule_LOADED_CLASSES_PERCENT_DESC), PERCENTAGE,
			PERCENT.quantity(90));
	public static final TypedPreference<IQuantity> RELEVANCE_THRESHOLD = new TypedPreference<>(
			"memleak.reference.tree.depth", Messages.getString(Messages.IncreasingLiveSetRule_RELEVANCE_THRESHOLD), //$NON-NLS-1$
			Messages.getString(Messages.IncreasingLiveSetRule_RELEVANCE_THRESHOLD_DESC), NUMBER,
			NUMBER_UNITY.quantity(0.5d));
	public static final TypedPreference<IQuantity> YOUNG_COLLECTION_THRESHOLD = new TypedPreference<>(
			"memleak.young.collections", Messages.getString(Messages.IncreasingLiveSetRule_YOUNG_COLLECTION_THRESHOLD), //$NON-NLS-1$
			Messages.getString(Messages.IncreasingLiveSetRule_YOUNG_COLLECTION_THRESHOLD_DESC), NUMBER,
			NUMBER_UNITY.quantity(4));
	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays
			.<TypedPreference<?>> asList(CLASSES_LOADED_PERCENT, RELEVANCE_THRESHOLD, YOUNG_COLLECTION_THRESHOLD);

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.HEAP_SUMMARY, EventAvailability.ENABLED).build();

	public static final ContentType<ReferenceTreeObject> REFERENCE_TREE_OBJECT = UnitLookup
			.createSyntheticContentType("referenceTreeObject"); //$NON-NLS-1$

	public static final TypedResult<IQuantity> LIVESET_INCREASE = new TypedResult<>("livesetIncrease", //$NON-NLS-1$
			"Liveset Increase", "The speed of the liveset increase per second.", UnitLookup.MEMORY, IQuantity.class);
	public static final TypedResult<IQuantity> TIME_AFTER_JVM_START = new TypedResult<>("timeAfterJvmStart", //$NON-NLS-1$
			"Time After JVM Start", "The time since the JVM was started.", UnitLookup.TIMESPAN, IQuantity.class);
	public static final TypedResult<IQuantity> LEAK_CANDIDATE_COUNT = new TypedResult<>("leakCandidateCount", //$NON-NLS-1$
			"Leak Candidate Count", "The number of leak candidates detected.", UnitLookup.NUMBER, IQuantity.class);
	public static final TypedResult<ReferenceTreeObject> LEAK_CANDIDATE = new TypedResult<>("leakCandidate", //$NON-NLS-1$
			"Leak Candidate", "The main leak candidate detected.", REFERENCE_TREE_OBJECT, ReferenceTreeObject.class);
	public static final TypedCollectionResult<ReferenceTreeObject> REFERENCE_CHAIN = new TypedCollectionResult<>(
			"referenceChain", "Reference Chain", "The objects keeping the main leak candidate alive.", //$NON-NLS-1$
			REFERENCE_TREE_OBJECT, ReferenceTreeObject.class);
	public static final TypedResult<IQuantity> POST_WARMUP_TIME = new TypedResult<>("postWarmupTime", //$NON-NLS-1$
			"Post Warmup Time",
			"The time after which the rule assumes that long lived objects aren't supposed to be allocated.",
			UnitLookup.TIMESTAMP, IQuantity.class);

	private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays.<TypedResult<?>> asList(
			TypedResult.SCORE, LIVESET_INCREASE, TIME_AFTER_JVM_START, LEAK_CANDIDATE_COUNT, LEAK_CANDIDATE,
			REFERENCE_CHAIN, POST_WARMUP_TIME);

	private IResult getResult(
		IItemCollection items, IPreferenceValueProvider valueProvider, IResultValueProvider resultProvider) {
		IQuantity postWarmupTime = getPostWarmupTime(items, valueProvider.getPreferenceValue(CLASSES_LOADED_PERCENT));
		Iterator<? extends IItemIterable> allAfterItems = items.apply(JdkFilters.HEAP_SUMMARY_AFTER_GC).iterator();
		double score = 0;
		IQuantity liveSetIncreasePerSecond = UnitLookup.MEMORY.getUnit(BinaryPrefix.MEBI).quantity(0);
		if (allAfterItems.hasNext()) {
			// FIXME: Handle multiple IItemIterable
			IItemIterable afterItems = allAfterItems.next();
			IMemberAccessor<IQuantity, IItem> timeAccessor = JfrAttributes.END_TIME.getAccessor(afterItems.getType());
			IMemberAccessor<IQuantity, IItem> memAccessor = JdkAttributes.HEAP_USED.getAccessor(afterItems.getType());

			liveSetIncreasePerSecond = UnitLookup.MEMORY.getUnit(BinaryPrefix.MEBI)
					.quantity(RulesToolkit.leastSquareMemory(afterItems.iterator(), timeAccessor, memAccessor));

			if (postWarmupTime == null) {
				return RulesToolkit.getTooFewEventsResult(this, valueProvider);
			}
			IQuantity postWarmupHeapSize = items
					.apply(ItemFilters.and(JdkFilters.HEAP_SUMMARY_AFTER_GC,
							ItemFilters.moreOrEqual(JfrAttributes.START_TIME, postWarmupTime)))
					.getAggregate((IAggregator<IQuantity, ?>) JdkAggregators.first(JdkAttributes.HEAP_USED));
			if (postWarmupHeapSize == null) {
				return RulesToolkit.getTooFewEventsResult(this, valueProvider);
			}
			double relativeIncreasePerSecond = liveSetIncreasePerSecond.ratioTo(postWarmupHeapSize);
			score = RulesToolkit.mapExp100(relativeIncreasePerSecond, PERCENT_OF_HEAP_INCREASE_PER_SECOND);
		}

		IQuantity youngCollections = items
				.getAggregate(Aggregators.count(ItemFilters.type(JdkTypeIDs.GC_COLLECTOR_YOUNG_GARBAGE_COLLECTION)));
		IQuantity oldCollections = items.getAggregate(Aggregators.count(JdkFilters.OLD_GARBAGE_COLLECTION));
		if (oldCollections.longValue() == 0) {
			// If there are no old collections we cannot accurately determine whether or not there is a leak
			// but a stable increase in live set over a recording is still interesting, since it could force a full GC eventually.
			if (youngCollections.longValue() <= valueProvider.getPreferenceValue(YOUNG_COLLECTION_THRESHOLD)
					.longValue()) {
				// If we have too few collections at all we shouldn't even try to guess at the live set
				return RulesToolkit.getTooFewEventsResult(this, valueProvider);
			}
			score = Math.min(score, 74);
		}
		// If we have Old Object Sample events we can attempt to find suitable memory leak class candidates
		// otherwise we just return the basic increasing live set score
		EventAvailability ea = RulesToolkit.getEventAvailability(items, JdkTypeIDs.OLD_OBJECT_SAMPLE);
		// FIXME: Should construct an message using memoryIncrease, not use a hard limit
		IQuantity timeAfterJVMStart = RulesToolkit.getEarliestStartTime(items)
				.subtract(items.getAggregate(JdkAggregators.JVM_START_TIME));
		if (ea == EventAvailability.DISABLED || ea == EventAvailability.UNKNOWN) {
			if (score >= 25) {
				return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.get(score))
						.setSummary(Messages.getString(Messages.IncreasingLiveSetRuleFactory_TEXT_INFO))
						.setExplanation(Messages.getString(Messages.IncreasingLiveSetRuleFactory_TEXT_INFO_LONG))
						.addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(score))
						.addResult(LIVESET_INCREASE, liveSetIncreasePerSecond)
						.addResult(TIME_AFTER_JVM_START, timeAfterJVMStart).build();
			} else {
				return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.OK)
						.setSummary(Messages.getString(Messages.IncreasingLiveSetRule_TEXT_OK)).build();
			}
		}

		// step 1. extract events from after the estimated warmup period
		IItemCollection oldObjectItems = items.apply(ItemFilters.and(ItemFilters.type(JdkTypeIDs.OLD_OBJECT_SAMPLE),
				ItemFilters.more(JfrAttributes.START_TIME, postWarmupTime)));

		ReferenceTreeModel tree = ReferenceTreeModel.buildReferenceTree(oldObjectItems);

		// step 2. perform a balance calculation on the old object sample events aggregated by class count
		boolean anyReferrerChains = false;
		for (ReferenceTreeObject referenceTreeObject : tree.getLeakObjects()) {
			if (referenceTreeObject.getParent() != null) {
				anyReferrerChains = true;
				break;
			}
		}
		if (!anyReferrerChains) {
			List<IntEntry<IMCType>> calculateGroupingScore = RulesToolkit.calculateGroupingScore(oldObjectItems,
					JdkAttributes.OLD_OBJECT_CLASS);
			double calculateBalanceScore = RulesToolkit.calculateBalanceScore(calculateGroupingScore);
			String summary = Messages.getString(Messages.IncreasingLiveSetRuleFactory_TEXT_INFO)
					+ (calculateBalanceScore >= 25
							? Messages.getString(Messages.IncreasingLiveSetRule_TEXT_INFO_UNBALANCED)
							: Messages.getString(Messages.IncreasingLiveSetRule_TEXT_INFO_BALANCED));
			return ResultBuilder.createFor(this, valueProvider)
					.setSeverity(Severity.get(Math.min(calculateBalanceScore, 25))) // At least INFO, because we already know that there is a leak.
					.setSummary(summary)
					.setExplanation(Messages.getString(Messages.IncreasingLiveSetRule_TEXT_INFO_LONG))
					.addResult(LIVESET_INCREASE, liveSetIncreasePerSecond)
					.addResult(TIME_AFTER_JVM_START, timeAfterJVMStart)
					.addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(calculateBalanceScore)).build();
		}

		List<ReferenceTreeObject> leakCandidates = tree.getLeakCandidates(
				valueProvider.getPreferenceValue(RELEVANCE_THRESHOLD).doubleValueIn(UnitLookup.NUMBER_UNITY));
		if (leakCandidates.size() > 0) {
			return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.get(score))
					.setSummary(Messages.getString(Messages.IncreasingLiveSetRuleFactory_TEXT_INFO))
					.setExplanation(Messages.getString(Messages.IncreasingLiveSetRule_LEAK_CANDIDATES))
					.addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(score))
					.addResult(TIME_AFTER_JVM_START, timeAfterJVMStart)
					.addResult(LIVESET_INCREASE, liveSetIncreasePerSecond)
					.addResult(LEAK_CANDIDATE_COUNT, UnitLookup.NUMBER_UNITY.quantity(leakCandidates.size()))
					.addResult(LEAK_CANDIDATE, leakCandidates.get(0))
					.addResult(REFERENCE_CHAIN, getReferenceChain(leakCandidates.get(0))).build();
		}
		return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.get(score))
				.setSummary(Messages.getString(Messages.IncreasingLiveSetRuleFactory_TEXT_INFO))
				.setExplanation(Messages.getString(Messages.IncreasingLiveSetRule_TEXT_INFO_NO_CANDIDATES))
				.addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(score))
				.addResult(LIVESET_INCREASE, liveSetIncreasePerSecond)
				.addResult(TIME_AFTER_JVM_START, timeAfterJVMStart).addResult(POST_WARMUP_TIME, postWarmupTime).build();
	}

	private List<ReferenceTreeObject> getReferenceChain(ReferenceTreeObject candidate) {
		ReferenceTreeObject chainObject = candidate.getParent();
		List<ReferenceTreeObject> referenceChain = new ArrayList<>();
		for (int i = 0; i < 10 && chainObject != null; i++) {
			referenceChain.add(chainObject);
			chainObject = chainObject.getParent();
		}
		return referenceChain;
	}

	private IQuantity getPostWarmupTime(IItemCollection items, IQuantity classesLoadedPercent) {
		IItemCollection classLoadItems = items.apply(JdkFilters.CLASS_LOAD_STATISTICS);
		IQuantity maxLoadedClasses = classLoadItems
				.getAggregate((IAggregator<IQuantity, ?>) Aggregators.max(JdkAttributes.CLASSLOADER_LOADED_COUNT));
		if (maxLoadedClasses == null) {
			return null;
		}
		double doubleValue = classesLoadedPercent.doubleValueIn(PERCENT_UNITY);
		IQuantity loadedClassesLimit = maxLoadedClasses.multiply(doubleValue);
		return classLoadItems.apply(ItemFilters.more(JdkAttributes.CLASSLOADER_LOADED_COUNT, loadedClassesLimit))
				.getAggregate((IAggregator<IQuantity, ?>) Aggregators.min(JfrAttributes.START_TIME));
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
		return CONFIG_ATTRIBUTES;
	}

	@Override
	public String getId() {
		return RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.IncreasingLiveSetRuleFactory_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.MEMORY_LEAK;
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
