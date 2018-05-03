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
package org.openjdk.jmc.flightrecorder.rules.jdk.io;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.owasp.encoder.Encode;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkQueries;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

public class FileReadRule implements IRule {

	public static final TypedPreference<IQuantity> READ_WARNING_LIMIT = new TypedPreference<>(
			"io.file.read.warning.limit", //$NON-NLS-1$
			Messages.getString(Messages.FileReadRule_CONFIG_WARNING_LIMIT),
			Messages.getString(Messages.FileReadRule_CONFIG_WARNING_LIMIT_LONG), UnitLookup.TIMESPAN,
			UnitLookup.MILLISECOND.quantity(4000));

	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays
			.<TypedPreference<?>> asList(READ_WARNING_LIMIT);
	private static final String RESULT_ID = "FileRead"; //$NON-NLS-1$

	private Result getResult(IItemCollection items, IPreferenceValueProvider vp) {
		EventAvailability eventAvailability = RulesToolkit.getEventAvailability(items, JdkTypeIDs.FILE_READ);
		if (eventAvailability == EventAvailability.UNAVAILABLE || eventAvailability == EventAvailability.DISABLED) {
			return RulesToolkit.getEventAvailabilityResult(this, items, eventAvailability, JdkTypeIDs.FILE_READ);
		}

		IQuantity warningLimit = vp.getPreferenceValue(READ_WARNING_LIMIT);
		IQuantity infoLimit = warningLimit.multiply(0.5);

		IItem longestEvent = items.apply(JdkFilters.FILE_READ)
				.getAggregate(Aggregators.itemWithMax(JfrAttributes.DURATION));

		// Aggregate of all file read events - if null, then we had no events
		if (longestEvent == null) {
			return new Result(this, 0, Messages.getString(Messages.FileReadRuleFactory_TEXT_NO_EVENTS), null,
					JdkQueries.FILE_READ);
		}
		IQuantity longestDuration = RulesToolkit.getValue(longestEvent, JfrAttributes.DURATION);
		String peakDuration = longestDuration.displayUsing(IDisplayable.AUTO);
		double score = RulesToolkit.mapExp100(longestDuration.doubleValueIn(UnitLookup.SECOND),
				infoLimit.doubleValueIn(UnitLookup.SECOND), warningLimit.doubleValueIn(UnitLookup.SECOND));

		if (Severity.get(score) == Severity.WARNING || Severity.get(score) == Severity.INFO) {
			String fileName = sanitizeFileName(RulesToolkit.getValue(longestEvent, JdkAttributes.IO_PATH));
			String amountRead = RulesToolkit.getValue(longestEvent, JdkAttributes.IO_FILE_BYTES_READ)
					.displayUsing(IDisplayable.AUTO);
			return new Result(this, score,
					MessageFormat.format(Messages.getString(Messages.FileReadRuleFactory_TEXT_WARN), peakDuration),
					MessageFormat.format(Messages.getString(Messages.FileReadRuleFactory_TEXT_WARN_LONG), peakDuration,
							fileName, amountRead),
					JdkQueries.FILE_READ);
		}
		return new Result(this, score,
				MessageFormat.format(Messages.getString(Messages.FileReadRuleFactory_TEXT_OK), peakDuration), null,
				JdkQueries.FILE_READ);
	}

	static String sanitizeFileName(String fileName) {
		if (fileName == null || fileName.isEmpty()) {
			return Encode.forHtml(Messages.getString(Messages.General_UNKNOWN_FILE_NAME));
		}
		return Encode.forHtml(fileName);
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
		return Messages.getString(Messages.FileReadRuleFactory_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.FILE_IO_TOPIC;
	}
}
