/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.flightrecorder.rules.jdk.io;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.ResultBuilder;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.RequiredEventsBuilder;
import org.owasp.encoder.Encode;

public class FileForceRule implements IRule {

	public static final TypedPreference<IQuantity> FORCE_INFO_LIMIT = new TypedPreference<>("io.file.force.info.limit", //$NON-NLS-1$
			Messages.getString(Messages.FileForceRule_CONFIG_INFO_LIMIT),
			Messages.getString(Messages.FileForceRule_CONFIG_INFO_LIMIT_LONG), UnitLookup.TIMESPAN,
			UnitLookup.MILLISECOND.quantity(50));

	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays
			.<TypedPreference<?>> asList(FORCE_INFO_LIMIT);
	private static final String RESULT_ID = "FileForce"; //$NON-NLS-1$

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.FILE_FORCE, EventAvailability.AVAILABLE).build();

	public static final TypedResult<IQuantity> LONGEST_FORCE_TIME = new TypedResult<>("longestForceTime", //$NON-NLS-1$
			"Longest Force (Time)", "The longest time it took to perform a file force.", UnitLookup.TIMESPAN,
			IQuantity.class);
	public static final TypedResult<String> LONGEST_FORCE_PATH = new TypedResult<>("longestForcePath", //$NON-NLS-1$
			"Longest Force (Path)", "The path of the file force that took the longest time.", UnitLookup.PLAIN_TEXT,
			String.class);
	public static final TypedResult<IQuantity> LONGEST_TOTAL_FORCE = new TypedResult<>("totalForceForLongest", //$NON-NLS-1$ 
			"Total Force (Top File)", "The total duration of all file forced for the file with the longest force.",
			UnitLookup.TIMESPAN, IQuantity.class);
	public static final TypedResult<IQuantity> AVERAGE_FILE_FORCE = new TypedResult<>("averageFileForce", //$NON-NLS-1$
			"Average File Force", "The average duration of all file force.", UnitLookup.TIMESPAN, IQuantity.class);
	public static final TypedResult<IQuantity> TOTAL_FILE_FORCE = new TypedResult<>("totalFileForce", //$NON-NLS-1$
			"Total File Force", "The total duration of all file forced.", UnitLookup.TIMESPAN, IQuantity.class);

	private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays.<TypedResult<?>> asList(
			TypedResult.SCORE, LONGEST_FORCE_PATH, LONGEST_FORCE_TIME, LONGEST_TOTAL_FORCE, AVERAGE_FILE_FORCE,
			TOTAL_FILE_FORCE);

	private IResult getResult(IItemCollection items, IPreferenceValueProvider vp, IResultValueProvider resultProvider) {
		long infoLimit = vp.getPreferenceValue(FORCE_INFO_LIMIT).longValue();

		IItemCollection fileForceEvents = items.apply(JdkFilters.FILE_FORCE);
		IItem longestEvent = fileForceEvents.getAggregate(Aggregators.itemWithMax(JfrAttributes.DURATION));

		IQuantity longestDuration = RulesToolkit.getValue(longestEvent, JfrAttributes.DURATION);
		double score = RulesToolkit.mapExp74(longestDuration.doubleValueIn(UnitLookup.MILLISECOND), infoLimit);
		Severity severity = Severity.get(score);
		if (score >= infoLimit) {
			String longestIOPath = RulesToolkit.getValue(longestEvent, JdkAttributes.IO_PATH);
			String fileName = sanitizeFileName(longestIOPath);
			IQuantity avgDuration = fileForceEvents
					.getAggregate(Aggregators.avg(JdkTypeIDs.FILE_FORCE, JfrAttributes.DURATION));
			IQuantity totalDuration = fileForceEvents
					.getAggregate(Aggregators.sum(JdkTypeIDs.FILE_FORCE, JfrAttributes.DURATION));
			IItemCollection eventsFromLongestIOPath = fileForceEvents
					.apply(ItemFilters.equals(JdkAttributes.IO_PATH, longestIOPath));
			IQuantity totalLongestIOPath = eventsFromLongestIOPath
					.getAggregate(Aggregators.sum(JdkTypeIDs.FILE_FORCE, JfrAttributes.DURATION));
			return ResultBuilder.createFor(this, vp).setSeverity(severity)
					.setSummary(Messages.getString(Messages.FileForceRuleFactory_TEXT_WARN))
					.setExplanation(Messages.getString(Messages.FileForceRuleFactory_TEXT_WARN_LONG))
					.addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(score))
					.addResult(LONGEST_FORCE_TIME, longestDuration).addResult(AVERAGE_FILE_FORCE, avgDuration)
					.addResult(TOTAL_FILE_FORCE, totalDuration).addResult(LONGEST_TOTAL_FORCE, totalLongestIOPath)
					.addResult(LONGEST_FORCE_PATH, fileName).build();
		}
		return ResultBuilder.createFor(this, vp).setSeverity(severity)
				.setSummary(Messages.getString(Messages.FileForceRuleFactory_TEXT_OK))
				.addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(score))
				.addResult(LONGEST_FORCE_TIME, longestDuration).build();
	}

	static String sanitizeFileName(String fileName) {
		if (fileName == null || fileName.isEmpty()) {
			return Encode.forHtml(Messages.getString(Messages.General_UNKNOWN_FILE_NAME));
		}
		return Encode.forHtml(fileName);
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
		return Messages.getString(Messages.FileForceRuleFactory_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.FILE_IO;
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
