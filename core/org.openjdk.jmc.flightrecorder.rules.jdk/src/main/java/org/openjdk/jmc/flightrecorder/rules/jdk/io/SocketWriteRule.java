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

public class SocketWriteRule implements IRule {

	private static final String RESULT_ID = "SocketWrite"; //$NON-NLS-1$

	public static final TypedPreference<IQuantity> WRITE_INFO_LIMIT = new TypedPreference<>(
			"io.socket.write.info.limit", //$NON-NLS-1$
			Messages.getString(Messages.SocketWriteRule_CONFIG_INFO_LIMIT),
			Messages.getString(Messages.SocketWriteRule_CONFIG_INFO_LIMIT_LONG), UnitLookup.TIMESPAN,
			UnitLookup.MILLISECOND.quantity(275));
	public static final TypedPreference<IQuantity> WRITE_WARNING_LIMIT = new TypedPreference<>(
			"io.socket.write.warning.limit", //$NON-NLS-1$
			Messages.getString(Messages.SocketWriteRule_CONFIG_WARNING_LIMIT),
			Messages.getString(Messages.SocketWriteRule_CONFIG_WARNING_LIMIT_LONG), UnitLookup.TIMESPAN,
			UnitLookup.MILLISECOND.quantity(2000));

	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays
			.<TypedPreference<?>> asList(WRITE_INFO_LIMIT, WRITE_WARNING_LIMIT);

	@Override
	public RunnableFuture<Result> evaluate(final IItemCollection items, final IPreferenceValueProvider vp) {
		FutureTask<Result> evaluationTask = new FutureTask<>(new Callable<Result>() {
			@Override
			public Result call() throws Exception {
				return evaluate(items, vp.getPreferenceValue(WRITE_INFO_LIMIT),
						vp.getPreferenceValue(WRITE_WARNING_LIMIT));
			}
		});
		return evaluationTask;
	}

	private Result evaluate(IItemCollection items, IQuantity infoLimit, IQuantity warningLimit) {
		EventAvailability eventAvailability = RulesToolkit.getEventAvailability(items, JdkTypeIDs.SOCKET_WRITE);
		if (eventAvailability != EventAvailability.AVAILABLE) {
			return RulesToolkit.getEventAvailabilityResult(this, items, eventAvailability, JdkTypeIDs.SOCKET_WRITE);
		}

		IItem longestEvent = items.apply(JdkFilters.NO_RMI_SOCKET_WRITE)
				.getAggregate(Aggregators.itemWithMax(JfrAttributes.DURATION));
		// We had events, but all got filtered out - say ok, duration 0. Perhaps say "no matching" or something similar.
		if (longestEvent == null) {
			String shortMessage = Messages.getString(Messages.SocketWriteRuleFactory_TEXT_NO_EVENTS);
			String longMessage = shortMessage + "<p>" //$NON-NLS-1$
					+ Messages.getString(Messages.SocketWriteRuleFactory_TEXT_RMI_NOTE);
			return new Result(this, 0, shortMessage, longMessage, JdkQueries.NO_RMI_SOCKET_WRITE);
		}

		IQuantity maxDuration = RulesToolkit.getValue(longestEvent, JfrAttributes.DURATION);
		String peakDuration = maxDuration.displayUsing(IDisplayable.AUTO);
		double score = RulesToolkit.mapExp100(maxDuration.doubleValueIn(UnitLookup.SECOND),
				infoLimit.doubleValueIn(UnitLookup.SECOND), warningLimit.doubleValueIn(UnitLookup.SECOND));

		if (Severity.get(score) == Severity.WARNING || Severity.get(score) == Severity.INFO) {
			String address = SocketReadRule
					.sanitizeAddress(RulesToolkit.getValue(longestEvent, JdkAttributes.IO_ADDRESS));
			String amountWritten = RulesToolkit.getValue(longestEvent, JdkAttributes.IO_SOCKET_BYTES_WRITTEN)
					.displayUsing(IDisplayable.AUTO);
			String shortMessage = MessageFormat.format(Messages.getString(Messages.SocketWriteRuleFactory_TEXT_WARN),
					peakDuration);
			String longMessage = MessageFormat.format(
					Messages.getString(Messages.SocketWriteRuleFactory_TEXT_WARN_LONG), peakDuration, address,
					amountWritten) + " " + Messages.getString(Messages.SocketWriteRuleFactory_TEXT_RMI_NOTE); //$NON-NLS-1$
			return new Result(this, score, shortMessage, longMessage);
		}
		String shortMessage = MessageFormat.format(Messages.getString(Messages.SocketWriteRuleFactory_TEXT_OK),
				peakDuration);
		String longMessage = shortMessage + "<p>" + Messages.getString(Messages.SocketWriteRuleFactory_TEXT_RMI_NOTE); //$NON-NLS-1$
		return new Result(this, score, shortMessage, longMessage, JdkQueries.NO_RMI_SOCKET_WRITE);
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
		return Messages.getString(Messages.SocketWriteRuleFactory_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.SOCKET_IO_TOPIC;
	}
}
