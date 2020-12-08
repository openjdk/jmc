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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.Pair;
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

public class VMOperationRule implements IRule {

	private static final String RESULT_ID = "VMOperations"; //$NON-NLS-1$
	private static final double MAX_SECONDS_BETWEEN_EVENTS = 0.01;

	public static final TypedPreference<IQuantity> WARNING_LIMIT = new TypedPreference<>("vm.vmoperation.warning.limit", //$NON-NLS-1$
			Messages.getString(Messages.VMOperationRule_CONFIG_WARNING_LIMIT),
			Messages.getString(Messages.VMOperationRule_CONFIG_WARNING_LIMIT_LONG), UnitLookup.TIMESPAN,
			UnitLookup.MILLISECOND.quantity(2000));

	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays.<TypedPreference<?>> asList(WARNING_LIMIT);

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.VM_OPERATIONS, EventAvailability.ENABLED).build();

	public static final TypedResult<IQuantity> LONGEST_OPERATION_DURATION = new TypedResult<>(
			"longestOperationDuration", "Longest Operation Duration", //$NON-NLS-1$
			"The duration of the longest running VM operation.", UnitLookup.TIMESPAN, IQuantity.class);
	public static final TypedResult<String> LONGEST_OPERATION = new TypedResult<>("longestOperation", //$NON-NLS-1$
			"Longest Operation", "The type of vm operation that took longest to complete.", UnitLookup.PLAIN_TEXT,
			String.class);
	public static final TypedResult<IMCThread> LONGEST_OPERATION_CALLER = new TypedResult<>("longestOperationCaller", //$NON-NLS-1$
			"Longest Operation Caller", "The thread that initiated the vm operation took the longest to complete.",
			UnitLookup.THREAD, IMCThread.class);
	public static final TypedResult<IQuantity> LONGEST_OPERATION_START_TIME = new TypedResult<>(
			"longestOperationStartTime", "Longest Operation Start", //$NON-NLS-1$
			"The time the vm operation that took the longest to complete was started.", UnitLookup.TIMESTAMP,
			IQuantity.class);

	private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays.<TypedResult<?>> asList(
			TypedResult.SCORE, LONGEST_OPERATION_DURATION, LONGEST_OPERATION, LONGEST_OPERATION_CALLER,
			LONGEST_OPERATION_START_TIME);

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
		IQuantity warningLimit = vp.getPreferenceValue(WARNING_LIMIT);
		IQuantity infoLimit = warningLimit.multiply(0.5);

		Pair<IItem, IQuantity> longestEventInfo = findLongestEventInfo(
				items.apply(JdkFilters.VM_OPERATIONS_BLOCKING_OR_SAFEPOINT));
		IItem startingEvent = longestEventInfo.left;
		if (startingEvent == null) {
			return ResultBuilder.createFor(this, vp).setSeverity(Severity.OK)
					.setSummary(Messages.getString(Messages.VMOperationRuleFactory_TEXT_OK))
					.addResult(LONGEST_OPERATION_DURATION, UnitLookup.SECOND.quantity(0)).build();
		}
		IQuantity longestOperationStart = getStartTime(startingEvent);
		IQuantity longestDuration = longestEventInfo.right;
		String operation = getOperation(startingEvent);
		IMCThread caller = getCaller(startingEvent);
		double score = RulesToolkit.mapExp100(longestDuration.doubleValueIn(UnitLookup.SECOND),
				infoLimit.doubleValueIn(UnitLookup.SECOND), warningLimit.doubleValueIn(UnitLookup.SECOND));

		boolean isCombinedDuration = getDuration(startingEvent).compareTo(longestDuration) != 0;
		if (Severity.get(score) == Severity.WARNING || Severity.get(score) == Severity.INFO) {
			String longMessage = isCombinedDuration
					? Messages.getString(Messages.VMOperationRuleFactory_TEXT_WARN_LONG_COMBINED_DURATION)
					: Messages.getString(Messages.VMOperationRuleFactory_TEXT_WARN_LONG);
			String shortMessage = isCombinedDuration
					? Messages.getString(Messages.VMOperationRuleFactory_TEXT_WARN_COMBINED_DURATION)
					: Messages.getString(Messages.VMOperationRuleFactory_TEXT_WARN);
			return ResultBuilder.createFor(this, vp).setSeverity(Severity.get(score)).setSummary(shortMessage)
					.setExplanation(longMessage).addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(score))
					.addResult(LONGEST_OPERATION, operation).addResult(LONGEST_OPERATION_CALLER, caller)
					.addResult(LONGEST_OPERATION_DURATION, longestDuration)
					.addResult(LONGEST_OPERATION_START_TIME, longestOperationStart).build();
		}
		String shortMessage = isCombinedDuration
				? Messages.getString(Messages.VMOperationRuleFactory_TEXT_OK_COMBINED_DURATION)
				: Messages.getString(Messages.VMOperationRuleFactory_TEXT_OK);
		return ResultBuilder.createFor(this, vp).setSeverity(Severity.get(score))
				.addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(score)).setSummary(shortMessage)
				.addResult(LONGEST_OPERATION, operation).addResult(LONGEST_OPERATION_DURATION, longestDuration).build();
	}

	private Pair<IItem, IQuantity> findLongestEventInfo(IItemCollection items) {
		IItem startingEvent = null;
		IQuantity longestDuration = null;
		IItem curStartingEvent = null;
		IQuantity prevEndTime = null;
		IQuantity curCombinedDur = null;

		List<IItem> sortedEvents = sortEventsByStartTime(items);
		for (IItem event : sortedEvents) {
			if (curStartingEvent == null) {
				curStartingEvent = event;
				curCombinedDur = getDuration(event);
			} else {
				IQuantity startTime = getStartTime(event);
				IQuantity duration = getDuration(event);
				double timeBetweenEvents = startTime.subtract(prevEndTime).doubleValueIn(UnitLookup.SECOND);
				if (getOperation(curStartingEvent).equals(getOperation(event))
						&& Objects.equals(getCaller(curStartingEvent), getCaller(event))
						&& timeBetweenEvents <= MAX_SECONDS_BETWEEN_EVENTS) {
					curCombinedDur = curCombinedDur.add(duration);
				} else {
					curCombinedDur = duration;
					curStartingEvent = event;
				}
			}

			if (longestDuration == null || longestDuration.compareTo(curCombinedDur) < 0) {
				longestDuration = curCombinedDur;
				startingEvent = curStartingEvent;
			}
			prevEndTime = getEndTime(event);
		}
		return new Pair<IItem, IQuantity>(startingEvent, longestDuration);
	}

	private List<IItem> sortEventsByStartTime(IItemCollection items) {
		List<IItem> sortedEvents = new ArrayList<>();
		for (IItemIterable iter : items) {
			for (IItem event : iter) {
				sortedEvents.add(event);
			}
		}
		Collections.sort(sortedEvents, new Comparator<IItem>() {
			@Override
			public int compare(IItem e1, IItem e2) {
				return getStartTime(e1).compareTo(getStartTime(e2));
			}
		});
		return sortedEvents;
	}

	private IQuantity getStartTime(IItem event) {
		return RulesToolkit.getValue(event, JfrAttributes.START_TIME);
	}

	private IQuantity getEndTime(IItem event) {
		return RulesToolkit.getValue(event, JfrAttributes.END_TIME);
	}

	private IQuantity getDuration(IItem event) {
		return RulesToolkit.getValue(event, JfrAttributes.DURATION);
	}

	private IMCThread getCaller(IItem event) {
		return RulesToolkit.getValue(event, JdkAttributes.CALLER);
	}

	private String getOperation(IItem event) {
		return RulesToolkit.getValue(event, JdkAttributes.OPERATION);
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
		return Messages.getString(Messages.VMOperations_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.VM_OPERATIONS;
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
