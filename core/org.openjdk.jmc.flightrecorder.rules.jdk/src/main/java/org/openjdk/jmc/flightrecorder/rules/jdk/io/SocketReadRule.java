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
import org.openjdk.jmc.common.item.IType;
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

public class SocketReadRule implements IRule {

	private static final String RESULT_ID = "SocketRead"; //$NON-NLS-1$

	public static final TypedPreference<IQuantity> READ_INFO_LIMIT = new TypedPreference<>("io.socket.read.info.limit", //$NON-NLS-1$
			Messages.getString(Messages.SocketReadRule_CONFIG_INFO_LIMIT),
			Messages.getString(Messages.SocketReadRule_CONFIG_INFO_LIMIT_LONG), UnitLookup.TIMESPAN,
			UnitLookup.MILLISECOND.quantity(275));
	public static final TypedPreference<IQuantity> READ_WARNING_LIMIT = new TypedPreference<>(
			"io.socket.read.warning.limit", //$NON-NLS-1$
			Messages.getString(Messages.SocketReadRule_CONFIG_WARNING_LIMIT),
			Messages.getString(Messages.SocketReadRule_CONFIG_WARNING_LIMIT_LONG), UnitLookup.TIMESPAN,
			UnitLookup.MILLISECOND.quantity(2000));

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.SOCKET_READ, EventAvailability.AVAILABLE).build();

	public static final TypedResult<IQuantity> LONGEST_READ_AMOUNT = new TypedResult<>("longestReadAmount", //$NON-NLS-1$
			"Longest Read (Amount)", "The amount read for the longest socket read.", UnitLookup.MEMORY,
			IQuantity.class);
	public static final TypedResult<IQuantity> LONGEST_READ_TIME = new TypedResult<>("longestReadTime", //$NON-NLS-1$
			"Longest Read (Time)", "The longest time it took to perform a socket read.", UnitLookup.TIMESPAN,
			IQuantity.class);
	public static final TypedResult<String> LONGEST_READ_ADDRESS = new TypedResult<>("longestReadHost", //$NON-NLS-1$
			"Longest Read (Host)", "The remote host of the socket read that took the longest time.",
			UnitLookup.PLAIN_TEXT, String.class);
	public static final TypedResult<IQuantity> LONGEST_TOTAL_READ = new TypedResult<>("totalReadForLongest", //$NON-NLS-1$
			"Total Read (Top Host)", "The total duration of all socket reads for the host with the longest read.",
			UnitLookup.TIMESPAN, IQuantity.class);
	public static final TypedResult<IQuantity> AVERAGE_SOCKET_READ = new TypedResult<>("averageSocketRead", //$NON-NLS-1$
			"Average Socket Read", "The average duration of all socket reads.", UnitLookup.TIMESPAN, IQuantity.class);
	public static final TypedResult<IQuantity> TOTAL_SOCKET_READ = new TypedResult<>("totalSocketRead", //$NON-NLS-1$
			"Total Socket Read", "The total duration of all socket reads.", UnitLookup.TIMESPAN, IQuantity.class);

	private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays.<TypedResult<?>> asList(
			TypedResult.SCORE, LONGEST_READ_ADDRESS, LONGEST_READ_AMOUNT, LONGEST_READ_TIME, LONGEST_TOTAL_READ,
			AVERAGE_SOCKET_READ, TOTAL_SOCKET_READ);

	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays
			.<TypedPreference<?>> asList(READ_INFO_LIMIT, READ_WARNING_LIMIT);

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
		IQuantity infoLimit = vp.getPreferenceValue(READ_INFO_LIMIT);
		IQuantity warningLimit = vp.getPreferenceValue(READ_WARNING_LIMIT);
		// Check if this is an early unsupported recording
		IItemCollection readItems = items.apply(JdkFilters.NO_RMI_SOCKET_READ);
		IType<IItem> readType = RulesToolkit.getType(readItems, JdkTypeIDs.SOCKET_READ);
		if (!readType.hasAttribute(JdkAttributes.IO_ADDRESS)) {
			return RulesToolkit.getMissingAttributeResult(this, readType, JdkAttributes.IO_ADDRESS, vp);
		}

		IItem longestEvent = readItems.getAggregate(Aggregators.itemWithMax(JfrAttributes.DURATION));
		// Had events, but all got filtered out - say ok, duration 0. We could possibly say "no matching" or something similar.
		if (longestEvent == null) {
			return ResultBuilder.createFor(this, vp).setSeverity(Severity.OK)
					.setSummary(Messages.SocketReadRuleFactory_TEXT_NO_EVENTS)
					.setExplanation(Messages.getString(Messages.SocketReadRuleFactory_TEXT_RMI_NOTE)).build();
		}

		IQuantity maxDuration = RulesToolkit.getValue(longestEvent, JfrAttributes.DURATION);
		double score = RulesToolkit.mapExp100(maxDuration.doubleValueIn(UnitLookup.SECOND),
				infoLimit.doubleValueIn(UnitLookup.SECOND), warningLimit.doubleValueIn(UnitLookup.SECOND));

		Severity severity = Severity.get(score);
		if (severity == Severity.WARNING || severity == Severity.INFO) {
			String address = RulesToolkit.getValue(longestEvent, JdkAttributes.IO_ADDRESS);
			if (address == null || address.isEmpty()) {
				address = Messages.getString(Messages.General_UNKNOWN_ADDRESS);
			}
			IQuantity amountRead = RulesToolkit.getValue(longestEvent, JdkAttributes.IO_SOCKET_BYTES_READ);
			IQuantity avgDuration = readItems
					.getAggregate(Aggregators.avg(JdkTypeIDs.SOCKET_READ, JfrAttributes.DURATION));
			IQuantity totalDuration = readItems
					.getAggregate(Aggregators.sum(JdkTypeIDs.SOCKET_READ, JfrAttributes.DURATION));
			IItemCollection eventsFromLongestAddress = readItems
					.apply(ItemFilters.equals(JdkAttributes.IO_ADDRESS, address));
			IQuantity totalLongestIOAddress = eventsFromLongestAddress
					.getAggregate(Aggregators.sum(JdkTypeIDs.SOCKET_READ, JfrAttributes.DURATION));
			return ResultBuilder.createFor(this, vp).setSeverity(severity)
					.setSummary(Messages.getString(Messages.SocketReadRuleFactory_TEXT_WARN))
					.setExplanation(Messages.getString(Messages.SocketReadRuleFactory_TEXT_WARN_LONG) + " " //$NON-NLS-1$
							+ Messages.getString(Messages.SocketReadRuleFactory_TEXT_RMI_NOTE))
					.addResult(LONGEST_READ_ADDRESS, address).addResult(LONGEST_READ_AMOUNT, amountRead)
					.addResult(LONGEST_TOTAL_READ, totalLongestIOAddress).addResult(AVERAGE_SOCKET_READ, avgDuration)
					.addResult(TOTAL_SOCKET_READ, totalDuration).addResult(LONGEST_READ_TIME, maxDuration).build();
		}
		return ResultBuilder.createFor(this, vp).setSeverity(severity)
				.setSummary(Messages.getString(Messages.SocketReadRuleFactory_TEXT_OK))
				.setExplanation(Messages.getString(Messages.SocketReadRuleFactory_TEXT_RMI_NOTE))
				.addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(score))
				.addResult(LONGEST_READ_TIME, maxDuration).build();
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
		return Messages.getString(Messages.SocketReadRuleFactory_RULE_NAME);
	}

	protected static String sanitizeAddress(String address) {
		if (address == null || address.isEmpty()) {
			Messages.getString(Messages.General_UNKNOWN_ADDRESS);
		}
		return address;
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
