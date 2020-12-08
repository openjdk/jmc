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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
 * This rule looks at the loaded classes to try to figure out if multiple classes with the same name
 * has been loaded. Note that this rule can get fairly expensive if you have load events with many
 * (thousands) of unique classes.
 */
// FIXME: This rule could perhaps be improved by doing a linear regression of the metaspace usage the higher k, the higher score.
public class ClassLeakingRule implements IRule {

	private static final String RESULT_ID = "ClassLeak"; //$NON-NLS-1$
	private static final String COUNT_AGGREGATOR_ID = "count"; //$NON-NLS-1$

	public static final TypedPreference<IQuantity> WARNING_LIMIT = new TypedPreference<>("classLeaking.warning.limit", //$NON-NLS-1$
			Messages.getString(Messages.ClassLeakingRule_CONFIG_WARNING_LIMIT),
			Messages.getString(Messages.ClassLeakingRule_CONFIG_WARNING_LIMIT_LONG), NUMBER, NUMBER_UNITY.quantity(25));

	public static final TypedPreference<IQuantity> MAX_NUMBER_OF_CLASSES_TO_REPORT = new TypedPreference<>(
			"classLeaking.classesToReport.limit", //$NON-NLS-1$
			Messages.getString(Messages.General_CONFIG_CLASS_LIMIT),
			Messages.getString(Messages.General_CONFIG_CLASS_LIMIT_LONG), NUMBER, NUMBER_UNITY.quantity(5));

	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays.<TypedPreference<?>> asList(WARNING_LIMIT,
			MAX_NUMBER_OF_CLASSES_TO_REPORT);

	public static final TypedCollectionResult<ClassEntry> LOADED_CLASSES = new TypedCollectionResult<>("loadedClasses", //$NON-NLS-1$
			Messages.getString(Messages.ClassLeakingRule_RESULT_LOADED_CLASSES_NAME),
			Messages.getString(Messages.ClassLeakingRule_RESULT_LOADED_CLASSES_DESCRIPTION), ClassEntry.CLASS_ENTRY,
			ClassEntry.class);
	public static final TypedResult<IMCType> MOST_LOADED_CLASS = new TypedResult<>("mostLoadedClass", //$NON-NLS-1$
			Messages.getString(Messages.ClassLeakingRule_RESULT_MOST_LOADED_CLASS_NAME),
			Messages.getString(Messages.ClassLeakingRule_RESULT_MOST_LOADED_CLASS_DESCRIPTION), UnitLookup.CLASS,
			IMCType.class);
	public static final TypedResult<IQuantity> MOST_LOADED_CLASS_TIMES = new TypedResult<>("mostLoadedClassTimes", //$NON-NLS-1$
			Messages.getString(Messages.ClassLeakingRule_RESULT_MOST_LOADED_CLASS_LOADS_NAME),
			Messages.getString(Messages.ClassLeakingRule_RESULT_MOST_LOADED_CLASS_LOADS_DESCRIPTION), UnitLookup.NUMBER,
			IQuantity.class);

	private static final List<TypedResult<?>> RESULT_ATTRIBUTES = Arrays.<TypedResult<?>> asList(TypedResult.SCORE,
			LOADED_CLASSES, MOST_LOADED_CLASS, MOST_LOADED_CLASS_TIMES);

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.CLASS_LOAD, EventAvailability.ENABLED)
			.addEventType(JdkTypeIDs.CLASS_UNLOAD, EventAvailability.ENABLED).build();

	@Override
	public String getId() {
		return RESULT_ID;
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.CLASS_LOADING;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.ClassLeakingRule_NAME);
	}

	@Override
	public Map<String, EventAvailability> getRequiredEvents() {
		return REQUIRED_EVENTS;
	}

	private IResult getResult(
		IItemCollection items, IPreferenceValueProvider valueProvider, IResultValueProvider dependencyResults) {
		int warningLimit = (int) valueProvider.getPreferenceValue(WARNING_LIMIT).longValue();

		ItemQueryBuilder queryLoad = ItemQueryBuilder.fromWhere(JdkFilters.CLASS_LOAD);
		queryLoad.groupBy(JdkAttributes.CLASS_LOADED);
		queryLoad.select(JdkAttributes.CLASS_LOADED);
		queryLoad.select(Aggregators.count(COUNT_AGGREGATOR_ID, "classesLoaded")); //$NON-NLS-1$
		Map<String, ClassEntry> entriesLoad = extractClassEntriesFromQuery(items, queryLoad.build());

		ItemQueryBuilder queryUnload = ItemQueryBuilder.fromWhere(ItemFilters.and(JdkFilters.CLASS_UNLOAD,
				createClassAttributeFilter(JdkAttributes.CLASS_UNLOADED, entriesLoad)));
		queryUnload.groupBy(JdkAttributes.CLASS_UNLOADED);
		queryUnload.select(JdkAttributes.CLASS_UNLOADED);
		queryUnload.select(Aggregators.count(COUNT_AGGREGATOR_ID, "classesUnloaded")); //$NON-NLS-1$
		Map<String, ClassEntry> entriesUnload = extractClassEntriesFromQuery(items, queryUnload.build());
		Map<String, ClassEntry> diff = diff(entriesLoad, entriesUnload);
		List<ClassEntry> entries = new ArrayList<>(diff.values());

		if (entries.size() > 0) {
			int classLimit = Math.min(
					(int) valueProvider.getPreferenceValue(MAX_NUMBER_OF_CLASSES_TO_REPORT).longValue(),
					entries.size());
			long maxCount = 0;
			Collections.sort(entries);
			Collection<ClassEntry> entriesOverLimit = new ArrayList<>();
			for (int i = 0; i < classLimit; i++) {
				ClassEntry entry = entries.get(i);
				entriesOverLimit.add(entry);
				maxCount = Math.max(entry.getCount().longValue(), maxCount);
			}
			double maxScore = RulesToolkit.mapExp100(maxCount, warningLimit) * 0.75;
			ClassEntry worst = entries.get(0);
			return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.get(maxScore))
					.setSummary(Messages.getString(Messages.ClassLeakingRule_RESULT_SUMMARY))
					.setExplanation(Messages.getString(Messages.ClassLeakingRule_RESULT_EXPLANATION))
					.addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(maxScore))
					.addResult(LOADED_CLASSES, entriesOverLimit).addResult(MOST_LOADED_CLASS, worst.getType())
					.addResult(MOST_LOADED_CLASS_TIMES, worst.getCount()).build();
		}
		return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.OK)
				.setSummary(Messages.getString(Messages.ClassLeakingRule_TEXT_OK)).build();
	}

	private static IItemFilter createClassAttributeFilter(
		IAttribute<IMCType> attribute, Map<String, ClassEntry> entries) {
		List<IItemFilter> allowedClasses = new ArrayList<>();
		for (ClassEntry entry : entries.values()) {
			allowedClasses.add(ItemFilters.equals(attribute, entry.getType()));
		}
		return ItemFilters.or(allowedClasses.toArray(new IItemFilter[0]));
	}

	private Map<String, ClassEntry> diff(Map<String, ClassEntry> entriesLoad, Map<String, ClassEntry> entriesUnload) {
		// Found no corresponding unloads, so short cutting this...
		if (entriesUnload.isEmpty()) {
			return entriesLoad;
		}
		Map<String, ClassEntry> diffMap = new HashMap<>(entriesLoad.size());
		for (Entry<String, ClassEntry> mapEntryLoad : entriesLoad.entrySet()) {
			ClassEntry classEntryUnload = entriesUnload.get(mapEntryLoad.getKey());
			if (classEntryUnload != null) {
				diffMap.put(mapEntryLoad.getKey(), new ClassEntry(mapEntryLoad.getValue().getType(),
						mapEntryLoad.getValue().getCount().subtract(classEntryUnload.getCount())));
			} else {
				diffMap.put(mapEntryLoad.getKey(), mapEntryLoad.getValue());
			}
		}
		return diffMap;
	}

	private Map<String, ClassEntry> extractClassEntriesFromQuery(IItemCollection items, IItemQuery query) {
		Map<String, ClassEntry> entries = new HashMap<>();
		IItemResultSet resultSet = new ItemResultSetFactory().createResultSet(items, query);
		ColumnInfo countColumn = resultSet.getColumnMetadata().get(COUNT_AGGREGATOR_ID); // $NON-NLS-1$
		ColumnInfo classColumn = resultSet.getColumnMetadata().get(query.getGroupBy().getIdentifier());

		while (resultSet.next()) {
			IQuantity countObject;
			try {
				countObject = (IQuantity) resultSet.getValue(countColumn.getColumn());
				if (countObject != null) {
					IMCType type = (IMCType) resultSet.getValue(classColumn.getColumn());
					if (type != null) {
						ClassEntry entry = new ClassEntry(type, countObject);
						entries.put(entry.getType().getFullName(), entry);
					}
				}
			} catch (ItemResultSetException e) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Failed to extract class entries from query!", //$NON-NLS-1$
						e);
			}
		}
		return entries;
	}

	@Override
	public RunnableFuture<IResult> createEvaluation(
		final IItemCollection items, final IPreferenceValueProvider preferenceValueProvider,
		final IResultValueProvider dependencyResults) {
		FutureTask<IResult> evaluationTask = new FutureTask<>(new Callable<IResult>() {
			@Override
			public IResult call() throws Exception {
				return getResult(items, preferenceValueProvider, dependencyResults);
			}
		});
		return evaluationTask;
	}

	@Override
	public Collection<TypedPreference<?>> getConfigurationAttributes() {
		return CONFIG_ATTRIBUTES;
	}

	@Override
	public Collection<TypedResult<?>> getResults() {
		return RESULT_ATTRIBUTES;
	}

}
