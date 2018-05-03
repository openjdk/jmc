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

import java.text.MessageFormat;
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
 * This rule looks at the loaded classes to try to figure out if multiple classes with the same name
 * has been loaded. Note that this rule can get fairly expensive if you have load events with many
 * (thousands) of unique classes.
 */
// FIXME: This rule could perhaps be improved by doing a linear regression of the metaspace usage the higher k, the higher score.
public final class ClassLeakingRule implements IRule {
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

	private Result getResult(IItemCollection items, IPreferenceValueProvider valueProvider) {
		EventAvailability eventAvailability = RulesToolkit.getEventAvailability(items, JdkTypeIDs.CLASS_LOAD,
				JdkTypeIDs.CLASS_UNLOAD);
		if (eventAvailability == EventAvailability.UNAVAILABLE || eventAvailability == EventAvailability.DISABLED) {
			return RulesToolkit.getEventAvailabilityResult(this, items, eventAvailability, JdkTypeIDs.CLASS_LOAD,
					JdkTypeIDs.CLASS_UNLOAD);
		}
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
			StringBuilder longText = new StringBuilder();
			int classLimit = Math.min(
					(int) valueProvider.getPreferenceValue(MAX_NUMBER_OF_CLASSES_TO_REPORT).longValue(),
					entries.size());
			longText.append(MessageFormat.format(Messages.getString(Messages.ClassLeakingRule_TEXT_WARN_LONG),
					String.valueOf(classLimit)));

			int maxCount = 0;
			Collections.sort(entries);
			longText.append("<p><ul>"); //$NON-NLS-1$
			for (int i = 0; i < classLimit; i++) {
				ClassEntry entry = entries.get(i);
				longText.append("<li>"); //$NON-NLS-1$
				longText.append(entry);
				longText.append("</li>"); //$NON-NLS-1$
				maxCount = Math.max(entry.getCount(), maxCount);
			}
			longText.append("</ul></p>"); //$NON-NLS-1$
			double maxScore = RulesToolkit.mapExp100(maxCount, warningLimit) * 0.75;
			ClassEntry worst = entries.get(0);
			return new Result(this, maxScore,
					MessageFormat.format(Messages.getString(Messages.ClassLeakingRule_TEXT_WARN),
							worst.getType().getFullName(), worst.getCount()),
					longText.toString());
		}
		return new Result(this, 0, Messages.getString(Messages.ClassLeakingRule_TEXT_OK));
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
						mapEntryLoad.getValue().getCount() - classEntryUnload.getCount()));
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
					int count = (int) countObject.longValue();
					IMCType type = (IMCType) resultSet.getValue(classColumn.getColumn());
					if (type != null) {
						ClassEntry entry = new ClassEntry(type, count);
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
		return Messages.getString(Messages.ClassLeakingRule_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.CLASS_LOADING_TOPIC;
	}

}
