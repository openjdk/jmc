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
package org.openjdk.jmc.flightrecorder.rules.jdk.memory;

import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER;
import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER_UNITY;
import static org.openjdk.jmc.common.unit.UnitLookup.PERCENT;
import static org.openjdk.jmc.common.unit.UnitLookup.PERCENTAGE;
import static org.openjdk.jmc.common.unit.UnitLookup.PERCENT_UNITY;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.collection.MapToolkit.IntEntry;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.BinaryPrefix;
import org.openjdk.jmc.common.unit.DecimalPrefix;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkQueries;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.memleak.ReferenceTreeModel;
import org.openjdk.jmc.flightrecorder.memleak.ReferenceTreeObject;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

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
	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays
			.<TypedPreference<?>> asList(CLASSES_LOADED_PERCENT, RELEVANCE_THRESHOLD);

	private Result getResult(IItemCollection items, IPreferenceValueProvider valueProvider) {
		EventAvailability eventAvailability = RulesToolkit.getEventAvailability(items, JdkTypeIDs.HEAP_SUMMARY);
		if (eventAvailability == EventAvailability.UNKNOWN || eventAvailability == EventAvailability.DISABLED) {
			return RulesToolkit.getEventAvailabilityResult(this, items, eventAvailability, JdkTypeIDs.HEAP_SUMMARY);
		}

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
				return RulesToolkit.getTooFewEventsResult(this);
			}
			IQuantity postWarmupHeapSize = items
					.apply(ItemFilters.and(JdkFilters.HEAP_SUMMARY_AFTER_GC,
							ItemFilters.moreOrEqual(JfrAttributes.START_TIME, postWarmupTime)))
					.getAggregate(JdkAggregators.first(JdkAttributes.HEAP_USED));
			if (postWarmupHeapSize == null) {
				return RulesToolkit.getTooFewEventsResult(this);
			}
			double relativeIncreasePerSecond = liveSetIncreasePerSecond.ratioTo(postWarmupHeapSize);
			score = RulesToolkit.mapExp100(relativeIncreasePerSecond, PERCENT_OF_HEAP_INCREASE_PER_SECOND);
		}
		// If we have Old Object Sample events we can attempt to find suitable memory leak class candidates
		// otherwise we just return the basic increasing live set score
		EventAvailability ea = RulesToolkit.getEventAvailability(items, JdkTypeIDs.OLD_OBJECT_SAMPLE);
		// FIXME: Should construct an message using memoryIncrease, not use a hard limit
		if (score >= 25 && (ea == EventAvailability.DISABLED || ea == EventAvailability.UNKNOWN)) {
			IQuantity timeAfterJVMStart = items.getAggregate(JdkAggregators.FIRST_ITEM_START).subtract(items.getAggregate(JdkAggregators.JVM_START_TIME));
			String shortMessage = MessageFormat.format(
					Messages.getString(Messages.IncreasingLiveSetRuleFactory_TEXT_INFO),
					liveSetIncreasePerSecond.displayUsing(IDisplayable.AUTO));
			String longMessage = shortMessage + "<p>" //$NON-NLS-1$
					+ MessageFormat.format(Messages.getString(Messages.IncreasingLiveSetRuleFactory_TEXT_INFO_LONG),
							timeAfterJVMStart.displayUsing(IDisplayable.AUTO));
			return new Result(this, score, shortMessage, longMessage, JdkQueries.HEAP_SUMMARY_AFTER_GC);
		} else if (score < 25) {
			return new Result(this, score, Messages.getString(Messages.IncreasingLiveSetRule_TEXT_OK));
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
			String shortDescription = MessageFormat.format(Messages.IncreasingLiveSetRuleFactory_TEXT_INFO,
					liveSetIncreasePerSecond.displayUsing(IDisplayable.AUTO))
					+ (calculateBalanceScore >= 25
							? Messages.getString(Messages.IncreasingLiveSetRule_TEXT_INFO_UNBALANCED)
							: Messages.getString(Messages.IncreasingLiveSetRule_TEXT_INFO_BALANCED));
			return new Result(this, Math.min(calculateBalanceScore, 25), // because we already know that there is a leak.
					shortDescription, Messages.getString(Messages.IncreasingLiveSetRule_TEXT_INFO_LONG));
		}

		List<ReferenceTreeObject> leakCandidates = tree.getLeakCandidates(
				valueProvider.getPreferenceValue(RELEVANCE_THRESHOLD).doubleValueIn(UnitLookup.NUMBER_UNITY));
		if (leakCandidates.size() > 0) {
			StringBuilder descriptionBuilder = new StringBuilder();
			descriptionBuilder
					.append(MessageFormat.format(Messages.getString(Messages.IncreasingLiveSetRuleFactory_TEXT_INFO),
							liveSetIncreasePerSecond.displayUsing(IDisplayable.AUTO)));
			descriptionBuilder.append("<br/>"); //$NON-NLS-1$
			descriptionBuilder.append(MessageFormat
					.format(Messages.getString(Messages.IncreasingLiveSetRule_LEAK_CANDIDATES), leakCandidates.size()));
			descriptionBuilder.append("<ul>"); //$NON-NLS-1$
			int objectFormat = ReferenceTreeObject.FORMAT_PACKAGE | ReferenceTreeObject.FORMAT_FIELD
					| ReferenceTreeObject.FORMAT_ARRAY_INFO;
			for (ReferenceTreeObject candidate : leakCandidates) {
				descriptionBuilder.append("<li>"); //$NON-NLS-1$
				descriptionBuilder.append(candidate.toString(objectFormat));
				descriptionBuilder.append("<br/>"); //$NON-NLS-1$
				descriptionBuilder.append(Messages.getString(Messages.IncreasingLiveSetRule_CANDIDATE_REFERRED_BY));
				descriptionBuilder.append("<ul>"); //$NON-NLS-1$
				ReferenceTreeObject chainObject = candidate.getParent();
				for (int i = 0; i < 10 && chainObject != null; i++) {
					descriptionBuilder.append("<li>"); //$NON-NLS-1$
					descriptionBuilder.append(chainObject.toString(objectFormat));
					if (chainObject.getParent() == null) { // aborting the loop because we have found the root
						descriptionBuilder.append(" ("); //$NON-NLS-1$
						descriptionBuilder.append(chainObject.getRootDescription());
						descriptionBuilder.append(")</li>"); //$NON-NLS-1$
						break;
					}
					descriptionBuilder.append("</li>"); //$NON-NLS-1$
					chainObject = chainObject.getParent();
				}
				if (chainObject != null && chainObject.getParent() != null) { // we never iterated to the object
					while (chainObject.getParent() != null) {
						chainObject = chainObject.getParent();
					}
					descriptionBuilder.append("<li>"); //$NON-NLS-1$
					descriptionBuilder.append(Messages.getString(Messages.IncreasingLiveSetRule_ELLIPSIS));
					descriptionBuilder.append("</li><li>"); //$NON-NLS-1$
					descriptionBuilder.append(chainObject.toString(objectFormat));
					descriptionBuilder.append(" ("); //$NON-NLS-1$
					descriptionBuilder.append(chainObject.getRootDescription());
					descriptionBuilder.append(")</li>"); //$NON-NLS-1$
				}
				descriptionBuilder.append("</ul>"); //$NON-NLS-1$
				descriptionBuilder.append("</li>"); //$NON-NLS-1$
			}
			descriptionBuilder.append("</ul>"); //$NON-NLS-1$
			return new Result(this, score, descriptionBuilder.toString());
		}
		String description = ""; //$NON-NLS-1$
		if (score >= 25) {
			description = MessageFormat.format(Messages.getString(Messages.IncreasingLiveSetRuleFactory_TEXT_INFO),
					liveSetIncreasePerSecond.displayUsing(IDisplayable.AUTO)) + "</br>"; //$NON-NLS-1$
		}
		return new Result(this, score,
				description + MessageFormat.format(
						Messages.getString(Messages.IncreasingLiveSetRule_TEXT_INFO_NO_CANDIDATES),
						postWarmupTime.displayUsing(IDisplayable.AUTO)),
				null, JdkQueries.HEAP_SUMMARY_AFTER_GC);
	}

	private IQuantity getPostWarmupTime(IItemCollection items, IQuantity classesLoadedPercent) {
		IItemCollection classLoadItems = items.apply(JdkFilters.CLASS_LOAD_STATISTICS);
		IQuantity maxLoadedClasses = classLoadItems
				.getAggregate(Aggregators.max(JdkAttributes.CLASSLOADER_LOADED_COUNT));
		if (maxLoadedClasses == null) {
			return null;
		}
		double doubleValue = classesLoadedPercent.doubleValueIn(PERCENT_UNITY);
		IQuantity loadedClassesLimit = maxLoadedClasses.multiply(doubleValue);
		return classLoadItems.apply(ItemFilters.more(JdkAttributes.CLASSLOADER_LOADED_COUNT, loadedClassesLimit))
				.getAggregate(Aggregators.min(JfrAttributes.START_TIME));
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
		return Messages.getString(Messages.IncreasingLiveSetRuleFactory_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.MEMORY_LEAK_TOPIC;
	}
}
