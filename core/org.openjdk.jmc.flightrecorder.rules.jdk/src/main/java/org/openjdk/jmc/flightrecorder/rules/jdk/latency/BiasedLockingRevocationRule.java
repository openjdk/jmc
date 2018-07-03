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
package org.openjdk.jmc.flightrecorder.rules.jdk.latency;

import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER;
import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER_UNITY;

import java.text.MessageFormat;
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
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.jdk.util.ClassEntry;
import org.openjdk.jmc.flightrecorder.rules.jdk.util.ColumnInfo;
import org.openjdk.jmc.flightrecorder.rules.jdk.util.IItemResultSet;
import org.openjdk.jmc.flightrecorder.rules.jdk.util.ItemResultSetException;
import org.openjdk.jmc.flightrecorder.rules.jdk.util.ItemResultSetFactory;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

/**
 * This rule is making use of the new dedicated biased locking revocation events available in JDK
 * 10/18.3. It will fire whenever a class is excluded from biased lockings, or whenever there have
 * been more than 15 revocations (can be configured) for a particular class.
 */
public final class BiasedLockingRevocationRule implements IRule {
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

	private Result getResult(IItemCollection items, IPreferenceValueProvider valueProvider) {
		EventAvailability eventAvailability = RulesToolkit.getEventAvailability(items,
				JdkTypeIDs.BIASED_LOCK_CLASS_REVOCATION);
		if (eventAvailability == EventAvailability.UNKNOWN || eventAvailability == EventAvailability.DISABLED) {
			return RulesToolkit.getEventAvailabilityResult(this, items, eventAvailability,
					JdkTypeIDs.BIASED_LOCK_CLASS_REVOCATION);
		}

		IItemCollection revokationEvents = items.apply(JdkFilters.BIASED_LOCKING_REVOCATIONS); // $NON-NLS-1$
		if (!revokationEvents.hasItems()) {
			return new Result(this, 0, Messages.getString(Messages.BiasedLockingRevocationPauseRule_TEXT_OK));
		}

		Set<String> filteredTypes = getFilteredTypes(valueProvider.getPreferenceValue(FILTERED_CLASSES));

		IItemCollection revokedClassesEvents = revokationEvents
				.apply(ItemFilters.and(ItemFilters.hasAttribute(JdkAttributes.BIASED_REVOCATION_CLASS),
						ItemFilters.equals(JdkAttributes.BIASED_REVOCATION_DISABLE_BIASING, Boolean.TRUE)));
		Set<IMCType> revokedTypes = filter(filteredTypes,
				revokedClassesEvents.getAggregate(Aggregators.distinct(JdkAttributes.BIASED_REVOCATION_CLASS)));

		StringBuilder shortMessage = new StringBuilder();
		StringBuilder longMessage = new StringBuilder();

		float totalScore = 0;

		if (!revokedTypes.isEmpty()) {
			totalScore = 25; // Base penalty for having fully revoked types not filtered out.
			totalScore += RulesToolkit.mapExp(revokedTypes.size(), 25, 7, 20); // Up to 25 more points if you have plenty of revoked types.
			shortMessage.append(Messages.getString(Messages.BiasedLockingRevocationRule_TEXT_REVOKED_CLASSES_FOUND));
			shortMessage.append(" "); //$NON-NLS-1$
			longMessage
					.append(Messages.getString(Messages.BiasedLockingRevocationRule_TEXT_REVOKED_CLASSES_FOUND_LONG));
			longMessage.append("<p><ul>"); //$NON-NLS-1$
			for (IMCType offender : revokedTypes) {
				longMessage.append("<li>"); //$NON-NLS-1$
				longMessage.append(offender.toString());
				longMessage.append("</li>"); //$NON-NLS-1$
			}
			longMessage.append("</ul></p>"); //$NON-NLS-1$
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

		if (revocationClasses.size() > 0) {
			int maxClasses = (int) valueProvider.getPreferenceValue(MAX_NUMBER_OF_CLASSES_TO_REPORT).longValue();
			shortMessage
					.append(Messages.getString(Messages.BiasedLockingRevocationRule_TEXT_REVOKE_LIMIT_CLASSES_FOUND));
			longMessage.append(MessageFormat.format(
					Messages.getString(Messages.BiasedLockingRevocationRule_TEXT_REVOKE_LIMIT_CLASSES_FOUND_LONG),
					warningLimit));
			longMessage.append("<p><ul>"); //$NON-NLS-1$
			int classLimit = Math.min(revocationClasses.size(), maxClasses);
			for (int i = 0; i < classLimit; i++) {
				ClassEntry classEntry = revocationClasses.get(i);
				if (classEntry.getCount() < warningLimit) {
					break;
				}
				longMessage.append("<li>"); //$NON-NLS-1$
				longMessage.append(classEntry);
				longMessage.append("</li>"); //$NON-NLS-1$
			}
			longMessage.append("</ul></p>"); //$NON-NLS-1$
		}
		if (totalScore == 0) {
			return new Result(this, 0, Messages.getString(Messages.BiasedLockingRevocationRule_TEXT_OK));
		} else {
			longMessage
					.append(MessageFormat.format(Messages.getString(Messages.BiasedLockingRevocationRule_TEXT_EPILOGUE),
							String.valueOf(filteredTypes)));
		}
		return new Result(this, totalScore, shortMessage.toString(), longMessage.toString());
	}

	private int calculateRevocationCountScore(List<ClassEntry> offendingClasses) {
		int score = 0;
		for (ClassEntry entry : offendingClasses) {
			// Can get maximum the base score for a full revocation if there are plenty of
			// revocation events for a single class.
			score = Math.max(Math.min(entry.getCount() / 2, 25), score);
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
						new ClassEntry(entry.getKey(), entry.getValue().getCount() + mergedEntry.getCount()));
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
					offendingClasses.put(type, new ClassEntry(type, (int) countObject.longValue()));

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
		return "biasedLockingRevocation"; //$NON-NLS-1$
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.BiasedLockingRevocationRule_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.BIASED_LOCKING;
	}

}
