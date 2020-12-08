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
package org.openjdk.jmc.flightrecorder.rules.jdk.latency;

import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER;
import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER_UNITY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IItemQuery;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.item.ItemQueryBuilder;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
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
import org.openjdk.jmc.flightrecorder.rules.jdk.util.ClassEntry;
import org.openjdk.jmc.flightrecorder.rules.jdk.util.ColumnInfo;
import org.openjdk.jmc.flightrecorder.rules.jdk.util.IItemResultSet;
import org.openjdk.jmc.flightrecorder.rules.jdk.util.ItemResultSetException;
import org.openjdk.jmc.flightrecorder.rules.jdk.util.ItemResultSetFactory;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.RequiredEventsBuilder;

/**
 * This rule is making use of the new dedicated biased locking revocation events available in JDK
 * 10/18.3. It will fire whenever a class is excluded from biased lockings, or whenever there have
 * been more than 15 revocations (can be configured) for a particular class.
 */
public final class BiasedLockingRevocationRule implements IRule {

	private static final String RESULT_ID = "biasedLockingRevocation"; //$NON-NLS-1$
	public static final TypedPreference<IQuantity> WARNING_LIMIT = new TypedPreference<>(
			"biasedRevocation.warning.limit", //$NON-NLS-1$
			Messages.getString(Messages.BiasedLockingRevocationRule_CONFIG_WARNING_LIMIT),
			Messages.getString(Messages.BiasedLockingRevocationRule_CONFIG_WARNING_LIMIT_LONG), NUMBER,
			NUMBER_UNITY.quantity(15));

	public static final TypedPreference<IQuantity> MAX_NUMBER_OF_CLASSES_TO_REPORT = new TypedPreference<>(
			"biasedRevocation.classesToReport.limit", //$NON-NLS-1$
			Messages.getString(Messages.General_CONFIG_CLASS_LIMIT),
			Messages.getString(Messages.General_CONFIG_CLASS_LIMIT_LONG), NUMBER, NUMBER_UNITY.quantity(5));

	private static final TypedPreference<String> FILTERED_CLASSES = new TypedPreference<>(
			"biasedRevocation.filtered.classes", //$NON-NLS-1$
			Messages.getString(Messages.BiasedLockingRevocationRule_CONFIG_FILTERED_CLASSES),
			Messages.getString(Messages.BiasedLockingRevocationRule_CONFIG_FILTERED_CLASSES_LONG),
			UnitLookup.PLAIN_TEXT.getPersister(), "java.lang.ref.ReferenceQueue$Lock"); //$NON-NLS-1$

	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays.<TypedPreference<?>> asList(WARNING_LIMIT,
			MAX_NUMBER_OF_CLASSES_TO_REPORT, FILTERED_CLASSES);

	public static final TypedCollectionResult<IMCType> REVOKED_TYPES = new TypedCollectionResult<>("revokedClasses", //$NON-NLS-1$
			"Revoked Classes", "Revoked Classes.", UnitLookup.CLASS, IMCType.class);
	public static final TypedCollectionResult<ClassEntry> REVOCATION_CLASSES = new TypedCollectionResult<>(
			"revocationClasses", "Revocation Classes", "Revocation Classes", ClassEntry.CLASS_ENTRY, ClassEntry.class); //$NON-NLS-1$
	public static final TypedCollectionResult<String> FILTERED_TYPES = new TypedCollectionResult<>("filteredTypes", //$NON-NLS-1$
			"Filtered Types", "Types that were filtered out.", UnitLookup.PLAIN_TEXT, String.class);

	private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays
			.<TypedResult<?>> asList(TypedResult.SCORE, REVOKED_TYPES, REVOCATION_CLASSES, FILTERED_TYPES);

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.BIASED_LOCK_CLASS_REVOCATION, EventAvailability.ENABLED).build();

	private IResult getResult(
		IItemCollection items, IPreferenceValueProvider valueProvider, IResultValueProvider resultProvider) {
		IItemCollection revokationEvents = items.apply(JdkFilters.BIASED_LOCKING_REVOCATIONS); // $NON-NLS-1$
		if (!revokationEvents.hasItems()) {
			return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.OK)
					.setSummary(Messages.getString(Messages.BiasedLockingRevocationPauseRule_TEXT_OK)).build();
		}

		Set<String> filteredTypes = getFilteredTypes(valueProvider.getPreferenceValue(FILTERED_CLASSES));

		IItemCollection revokedClassesEvents = revokationEvents
				.apply(ItemFilters.and(ItemFilters.hasAttribute(JdkAttributes.BIASED_REVOCATION_CLASS),
						ItemFilters.equals(JdkAttributes.BIASED_REVOCATION_DISABLE_BIASING, Boolean.TRUE)));
		Set<IMCType> revokedTypes = filter(filteredTypes, revokedClassesEvents.getAggregate(
				(IAggregator<Set<IMCType>, ?>) Aggregators.distinct(JdkAttributes.BIASED_REVOCATION_CLASS)));

		StringBuilder summary = new StringBuilder();
		StringBuilder explanation = new StringBuilder();

		float totalScore = 0;

		if (!revokedTypes.isEmpty()) {
			totalScore = 25; // Base penalty for having fully revoked types not filtered out.
			totalScore += RulesToolkit.mapExp(revokedTypes.size(), 25, 7, 20); // Up to 25 more points if you have plenty of revoked types.
			summary.append(Messages.getString(Messages.BiasedLockingRevocationRule_TEXT_REVOKED_CLASSES_FOUND));
			summary.append(" "); //$NON-NLS-1$
			explanation
					.append(Messages.getString(Messages.BiasedLockingRevocationRule_TEXT_REVOKED_CLASSES_FOUND_LONG));
		}
		int warningLimit = (int) valueProvider.getPreferenceValue(WARNING_LIMIT).longValue();

		Map<IMCType, ClassEntry> revocationMap = extractRevocations(revokationEvents,
				ItemFilters.or(ItemFilters.type(JdkTypeIDs.BIASED_LOCK_REVOCATION),
						ItemFilters.type(JdkTypeIDs.BIASED_LOCK_SELF_REVOCATION)),
				JdkAttributes.BIASED_REVOCATION_LOCK_CLASS);
		Map<IMCType, ClassEntry> classRevocationMap = extractRevocations(revokationEvents,
				ItemFilters.type(JdkTypeIDs.BIASED_LOCK_CLASS_REVOCATION), JdkAttributes.BIASED_REVOCATION_CLASS);

		List<ClassEntry> revocationClasses = filteredMerge(filteredTypes, revokedTypes, classRevocationMap,
				revocationMap);
		totalScore += calculateRevocationCountScore(revocationClasses);

		Collections.sort(revocationClasses);
		List<ClassEntry> filteredRevocationClasses = new ArrayList<>();
		if (revocationClasses.size() > 0) {
			int maxClasses = (int) valueProvider.getPreferenceValue(MAX_NUMBER_OF_CLASSES_TO_REPORT).longValue();
			summary.append(Messages.getString(Messages.BiasedLockingRevocationRule_TEXT_REVOKE_LIMIT_CLASSES_FOUND));
			explanation.append(
					Messages.getString(Messages.BiasedLockingRevocationRule_TEXT_REVOKE_LIMIT_CLASSES_FOUND_LONG));
			int classLimit = Math.min(revocationClasses.size(), maxClasses);
			for (int i = 0; i < classLimit; i++) {
				ClassEntry classEntry = revocationClasses.get(i);
				filteredRevocationClasses.add(classEntry);
				if (classEntry.getCount().longValue() < warningLimit) {
					break;
				}
			}
		}
		if (totalScore == 0) {
			return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.OK)
					.setSummary(Messages.getString(Messages.BiasedLockingRevocationPauseRule_TEXT_OK)).build();
		} else {
			explanation.append(Messages.getString(Messages.BiasedLockingRevocationRule_TEXT_EPILOGUE));
		}
		return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.get(totalScore))
				.setSummary(summary.toString()).setExplanation(explanation.toString())
				.addResult(REVOKED_TYPES, revokedTypes).addResult(REVOCATION_CLASSES, filteredRevocationClasses)
				.addResult(FILTERED_TYPES, filteredTypes).build();
	}

	private int calculateRevocationCountScore(List<ClassEntry> offendingClasses) {
		int score = 0;
		for (ClassEntry entry : offendingClasses) {
			// Can get maximum the base score for a full revocation if there are plenty of
			// revocation events for a single class.
			score = (int) Math.max(Math.min(entry.getCount().longValue() / 2, 25), score);
		}
		return score;
	}

	/**
	 * @param filteredTypes
	 *            user filtered types
	 * @param revokedTypes
	 *            the types that were revoked during this recording.
	 * @param offendingClassRevocations
	 * @param offendingRevocations
	 * @return
	 */
	private List<ClassEntry> filteredMerge(
		Set<String> filteredTypes, Set<IMCType> revokedTypes, Map<IMCType, ClassEntry> offendingClassRevocations,
		Map<IMCType, ClassEntry> offendingRevocations) {
		Map<IMCType, ClassEntry> merged = new HashMap<>();

		for (Entry<IMCType, ClassEntry> entry : offendingRevocations.entrySet()) {
			putIfNotInFiltered(filteredTypes, revokedTypes, merged, entry);
		}

		// Likely far fewer class revocations
		for (Entry<IMCType, ClassEntry> entry : offendingClassRevocations.entrySet()) {
			ClassEntry mergedEntry = merged.get(entry.getKey());
			if (mergedEntry != null) {
				merged.put(entry.getKey(),
						new ClassEntry(entry.getKey(), entry.getValue().getCount().add(mergedEntry.getCount())));
			} else {
				putIfNotInFiltered(filteredTypes, revokedTypes, merged, entry);
			}
		}
		return new ArrayList<>(merged.values());
	}

	private static void putIfNotInFiltered(
		Set<String> filteredTypes, Set<IMCType> revokedTypes, Map<IMCType, ClassEntry> merged,
		Entry<IMCType, ClassEntry> entry) {
		IMCType type = entry.getKey();
		if (type != null && !filteredTypes.contains(type.getFullName()) && !revokedTypes.contains(type)) {
			merged.put(entry.getKey(), entry.getValue());
		}
	}

	private Map<IMCType, ClassEntry> extractRevocations(
		IItemCollection revokationEvents, IItemFilter filter, IAttribute<IMCType> classAttribute) {
		ItemQueryBuilder itemQueryBuilder = ItemQueryBuilder.fromWhere(filter);
		itemQueryBuilder.groupBy(classAttribute);
		itemQueryBuilder.select(classAttribute);
		itemQueryBuilder.select(Aggregators.count());
		IItemQuery query = itemQueryBuilder.build();

		IItemResultSet resultSet = new ItemResultSetFactory().createResultSet(revokationEvents, query);
		ColumnInfo countColumn = resultSet.getColumnMetadata().get(Aggregators.count().getName());
		ColumnInfo classColumn = resultSet.getColumnMetadata().get(classAttribute.getIdentifier());

		Map<IMCType, ClassEntry> offendingClasses = new HashMap<>();
		while (resultSet.next()) {
			try {
				IQuantity countObject = (IQuantity) resultSet.getValue(countColumn.getColumn());
				IMCType type = (IMCType) resultSet.getValue(classColumn.getColumn());
				if (countObject != null && type != null) {
					offendingClasses.put(type, new ClassEntry(type, countObject));

				}
			} catch (ItemResultSetException e) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING,
						"Unexpected problem looking at biased revocation events.", e); //$NON-NLS-1$
			}
		}
		return offendingClasses;
	}

	private Set<IMCType> filter(Set<String> filteredTypes, Set<IMCType> types) {
		Set<IMCType> result = new HashSet<>();
		for (IMCType type : types) {
			if (!filteredTypes.contains(type.getFullName())) {
				result.add(type);
			}
		}
		return result;
	}

	private static Set<String> getFilteredTypes(String preferenceValue) {
		Set<String> acceptedOptionNames = new HashSet<>();
		if (preferenceValue != null) {
			String[] optionNames = preferenceValue.split("[, ]+"); //$NON-NLS-1$
			for (String optionName : optionNames) {
				acceptedOptionNames.add(optionName);
			}
		}
		return acceptedOptionNames;
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
		return Messages.getString(Messages.BiasedLockingRevocationRule_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.BIASED_LOCKING;
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
