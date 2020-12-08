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

public class FileReadRule implements IRule {

	public static final TypedPreference<IQuantity> READ_WARNING_LIMIT = new TypedPreference<>(
			"io.file.read.warning.limit", //$NON-NLS-1$
			Messages.getString(Messages.FileReadRule_CONFIG_WARNING_LIMIT),
			Messages.getString(Messages.FileReadRule_CONFIG_WARNING_LIMIT_LONG), UnitLookup.TIMESPAN,
			UnitLookup.MILLISECOND.quantity(4000));

	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays
			.<TypedPreference<?>> asList(READ_WARNING_LIMIT);
	private static final String RESULT_ID = "FileRead"; //$NON-NLS-1$

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.FILE_READ, EventAvailability.ENABLED).build();

	public static final TypedResult<IQuantity> LONGEST_READ_AMOUNT = new TypedResult<>("longestReadAmount", //$NON-NLS-1$
			"Longest Read (Amount)", "The amount read for the longest file read.", UnitLookup.MEMORY, IQuantity.class);
	public static final TypedResult<IQuantity> LONGEST_READ_TIME = new TypedResult<>("longestReadTime", //$NON-NLS-1$
			"Longest Read (Time)", "The longest time it took to perform a file read.", UnitLookup.TIMESPAN,
			IQuantity.class);
	public static final TypedResult<String> LONGEST_READ_PATH = new TypedResult<>("longestReadPath", //$NON-NLS-1$
			"Longest Read (Path)", "The path of the file read that took the longest time.", UnitLookup.PLAIN_TEXT,
			String.class);
	public static final TypedResult<IQuantity> LONGEST_TOTAL_READ = new TypedResult<>("totalReadForLongest", //$NON-NLS-1$ 
			"Total Read (Top File)", "The total duration of all file reads for the file with the longest read.",
			UnitLookup.TIMESPAN, IQuantity.class);
	public static final TypedResult<IQuantity> AVERAGE_FILE_READ = new TypedResult<>("averageFileRead", //$NON-NLS-1$
			"Average File Read", "The average duration of all file reads.", UnitLookup.TIMESPAN, IQuantity.class);
	public static final TypedResult<IQuantity> TOTAL_FILE_READ = new TypedResult<>("totalFileRead", "Total File Read", //$NON-NLS-1$
			"The total duration of all file reads.", UnitLookup.TIMESPAN, IQuantity.class);

	private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays.<TypedResult<?>> asList(
			TypedResult.SCORE, LONGEST_READ_AMOUNT, LONGEST_READ_PATH, LONGEST_READ_TIME, LONGEST_TOTAL_READ,
			AVERAGE_FILE_READ, TOTAL_FILE_READ);

	private IResult getResult(IItemCollection items, IPreferenceValueProvider vp, IResultValueProvider resultProvider) {
		IQuantity warningLimit = vp.getPreferenceValue(READ_WARNING_LIMIT);
		IQuantity infoLimit = warningLimit.multiply(0.5);

		IItemCollection fileReadEvents = items.apply(JdkFilters.FILE_READ);
		IItem longestEvent = fileReadEvents.getAggregate(Aggregators.itemWithMax(JfrAttributes.DURATION));

		// Aggregate of all file read events - if null, then we had no events
		if (longestEvent == null) {
			return ResultBuilder.createFor(this, vp).setSeverity(Severity.OK)
					.setSummary(Messages.getString(Messages.FileReadRuleFactory_TEXT_NO_EVENTS)).build();
		}
		IQuantity longestDuration = RulesToolkit.getValue(longestEvent, JfrAttributes.DURATION);
		double score = RulesToolkit.mapExp100(longestDuration.doubleValueIn(UnitLookup.SECOND),
				infoLimit.doubleValueIn(UnitLookup.SECOND), warningLimit.doubleValueIn(UnitLookup.SECOND));

		Severity severity = Severity.get(score);
		if (severity == Severity.WARNING || severity == Severity.INFO) {
			String longestIOPath = RulesToolkit.getValue(longestEvent, JdkAttributes.IO_PATH);
			String fileName = sanitizeFileName(longestIOPath);
			IQuantity amountRead = RulesToolkit.getValue(longestEvent, JdkAttributes.IO_FILE_BYTES_READ);
			IQuantity avgDuration = fileReadEvents
					.getAggregate(Aggregators.avg(JdkTypeIDs.FILE_READ, JfrAttributes.DURATION));
			IQuantity totalDuration = fileReadEvents
					.getAggregate(Aggregators.sum(JdkTypeIDs.FILE_READ, JfrAttributes.DURATION));
			IItemCollection eventsFromLongestIOPath = fileReadEvents
					.apply(ItemFilters.equals(JdkAttributes.IO_PATH, longestIOPath));
			IQuantity totalLongestIOPath = eventsFromLongestIOPath
					.getAggregate(Aggregators.sum(JdkTypeIDs.FILE_READ, JfrAttributes.DURATION));
			return ResultBuilder.createFor(this, vp).setSeverity(severity)
					.setSummary(Messages.getString(Messages.FileReadRuleFactory_TEXT_WARN))
					.setExplanation(Messages.getString(Messages.FileReadRuleFactory_TEXT_WARN_LONG))
					.addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(score))
					.addResult(LONGEST_READ_AMOUNT, amountRead).addResult(LONGEST_READ_TIME, longestDuration)
					.addResult(AVERAGE_FILE_READ, avgDuration).addResult(TOTAL_FILE_READ, totalDuration)
					.addResult(LONGEST_TOTAL_READ, totalLongestIOPath).addResult(LONGEST_READ_PATH, fileName).build();
		}
		return ResultBuilder.createFor(this, vp).setSeverity(severity)
				.setSummary(Messages.getString(Messages.FileReadRuleFactory_TEXT_OK))
				.addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(score))
				.addResult(LONGEST_READ_TIME, longestDuration).build();
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
		return Messages.getString(Messages.FileReadRuleFactory_RULE_NAME);
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
