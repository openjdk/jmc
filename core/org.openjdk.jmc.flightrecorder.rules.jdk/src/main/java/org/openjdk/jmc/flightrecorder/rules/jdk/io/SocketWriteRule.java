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

	public static final TypedResult<IQuantity> LONGEST_WRITE_AMOUNT = new TypedResult<>("longestWriteAmount", //$NON-NLS-1$
			"Longest Write (Amount)", "The amount read for the longest socket write.", UnitLookup.MEMORY,
			IQuantity.class);
	public static final TypedResult<IQuantity> LONGEST_WRITE_TIME = new TypedResult<>("longestWriteTime", //$NON-NLS-1$
			"Longest Write (Time)", "The longest time it took to perform a socket write.", UnitLookup.TIMESPAN,
			IQuantity.class);
	public static final TypedResult<String> LONGEST_WRITE_ADDRESS = new TypedResult<>("longestWriteHost", //$NON-NLS-1$
			"Longest Write (Host)", "The remote host of the socket write that took the longest time.",
			UnitLookup.PLAIN_TEXT, String.class);
	public static final TypedResult<IQuantity> LONGEST_TOTAL_READ = new TypedResult<>("totalWriteForLongest", //$NON-NLS-1$
			"Total Write (Top Host)", "The total duration of all socket writes for the host with the longest write.", //$NON-NLS-1$
			UnitLookup.TIMESPAN, IQuantity.class);
	public static final TypedResult<IQuantity> AVERAGE_SOCKET_READ = new TypedResult<>("averageSocketWrite", //$NON-NLS-1$
			"Average Socket Write", "The average duration of all socket writes.", UnitLookup.TIMESPAN, IQuantity.class);
	public static final TypedResult<IQuantity> TOTAL_SOCKET_READ = new TypedResult<>("totalSocketWrite", //$NON-NLS-1$
			"Total Socket Write", "The total duration of all socket writes.", UnitLookup.TIMESPAN, IQuantity.class);

	private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays.<TypedResult<?>> asList(
			TypedResult.SCORE, LONGEST_WRITE_ADDRESS, LONGEST_WRITE_AMOUNT, LONGEST_WRITE_TIME, LONGEST_TOTAL_READ,
			AVERAGE_SOCKET_READ, TOTAL_SOCKET_READ);

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.SOCKET_WRITE, EventAvailability.AVAILABLE).build();

	@Override
	public RunnableFuture<IResult> createEvaluation(
		final IItemCollection items, final IPreferenceValueProvider vp, final IResultValueProvider rp) {
		FutureTask<IResult> evaluationTask = new FutureTask<>(new Callable<IResult>() {
			@Override
			public IResult call() throws Exception {
				return evaluate(items, vp, rp);
			}
		});
		return evaluationTask;
	}

	private IResult evaluate(IItemCollection items, IPreferenceValueProvider vp, IResultValueProvider rp) {
		IQuantity infoLimit = vp.getPreferenceValue(WRITE_INFO_LIMIT);
		IQuantity warningLimit = vp.getPreferenceValue(WRITE_WARNING_LIMIT);
		IItem longestEvent = items.apply(JdkFilters.NO_RMI_SOCKET_WRITE)
				.getAggregate(Aggregators.itemWithMax(JfrAttributes.DURATION));
		IItemCollection writeItems = items.apply(JdkFilters.NO_RMI_SOCKET_WRITE);
		// We had events, but all got filtered out - say ok, duration 0. Perhaps say "no matching" or something similar.
		if (longestEvent == null) {
			return ResultBuilder.createFor(this, vp).setSeverity(Severity.OK)
					.setSummary(Messages.getString(Messages.SocketWriteRuleFactory_TEXT_NO_EVENTS))
					.setExplanation(Messages.getString(Messages.SocketWriteRuleFactory_TEXT_RMI_NOTE)).build();
		}

		IQuantity maxDuration = RulesToolkit.getValue(longestEvent, JfrAttributes.DURATION);
		double score = RulesToolkit.mapExp100(maxDuration.doubleValueIn(UnitLookup.SECOND),
				infoLimit.doubleValueIn(UnitLookup.SECOND), warningLimit.doubleValueIn(UnitLookup.SECOND));

		Severity severity = Severity.get(score);
		if (severity == Severity.WARNING || severity == Severity.INFO) {
			String address = SocketReadRule
					.sanitizeAddress(RulesToolkit.getValue(longestEvent, JdkAttributes.IO_ADDRESS));
			IQuantity amountWritten = RulesToolkit.getValue(longestEvent, JdkAttributes.IO_SOCKET_BYTES_WRITTEN);
			IQuantity avgDuration = writeItems
					.getAggregate(Aggregators.avg(JdkTypeIDs.SOCKET_WRITE, JfrAttributes.DURATION));
			IQuantity totalDuration = writeItems
					.getAggregate(Aggregators.sum(JdkTypeIDs.SOCKET_WRITE, JfrAttributes.DURATION));
			IItemCollection eventsFromLongestAddress = writeItems
					.apply(ItemFilters.equals(JdkAttributes.IO_ADDRESS, address));
			IQuantity totalLongestIOAddress = eventsFromLongestAddress
					.getAggregate(Aggregators.sum(JdkTypeIDs.SOCKET_WRITE, JfrAttributes.DURATION));
			return ResultBuilder.createFor(this, vp).setSeverity(severity)
					.setSummary(Messages.getString(Messages.SocketWriteRuleFactory_TEXT_WARN))
					.setExplanation(Messages.getString(Messages.SocketWriteRuleFactory_TEXT_WARN_LONG) + " " //$NON-NLS-1$
							+ Messages.getString(Messages.SocketWriteRuleFactory_TEXT_RMI_NOTE))
					.addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(score))
					.addResult(LONGEST_WRITE_ADDRESS, address).addResult(LONGEST_WRITE_AMOUNT, amountWritten)
					.addResult(LONGEST_TOTAL_READ, totalLongestIOAddress).addResult(AVERAGE_SOCKET_READ, avgDuration)
					.addResult(TOTAL_SOCKET_READ, totalDuration).addResult(LONGEST_WRITE_TIME, maxDuration).build();
		}
		return ResultBuilder.createFor(this, vp).setSeverity(severity)
				.setSummary(Messages.getString(Messages.SocketWriteRuleFactory_TEXT_OK))
				.setExplanation(Messages.getString(Messages.SocketWriteRuleFactory_TEXT_RMI_NOTE))
				.addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(score))
				.addResult(LONGEST_WRITE_TIME, maxDuration).build();
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
		return JfrRuleTopics.SOCKET_IO;
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
