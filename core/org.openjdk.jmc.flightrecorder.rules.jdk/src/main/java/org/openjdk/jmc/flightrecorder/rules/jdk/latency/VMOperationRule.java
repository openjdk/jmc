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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.IDisplayable;
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
import org.openjdk.jmc.flightrecorder.jdk.JdkQueries;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

public class VMOperationRule implements IRule {

	private static final String RESULT_ID = "VMOperations"; //$NON-NLS-1$
	private static final double MAX_SECONDS_BETWEEN_EVENTS = 0.01;

	public static final TypedPreference<IQuantity> WARNING_LIMIT = new TypedPreference<>("vm.vmoperation.warning.limit", //$NON-NLS-1$
			Messages.getString(Messages.VMOperationRule_CONFIG_WARNING_LIMIT),
			Messages.getString(Messages.VMOperationRule_CONFIG_WARNING_LIMIT_LONG), UnitLookup.TIMESPAN,
			UnitLookup.MILLISECOND.quantity(2000));

	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays.<TypedPreference<?>> asList(WARNING_LIMIT);

	@Override
	public RunnableFuture<Result> evaluate(final IItemCollection items, final IPreferenceValueProvider vp) {
		FutureTask<Result> evaluationTask = new FutureTask<>(new Callable<Result>() {
			@Override
			public Result call() throws Exception {
				return evaluate(items, vp.getPreferenceValue(WARNING_LIMIT));
			}
		});
		return evaluationTask;
	}

	private Result evaluate(IItemCollection items, IQuantity warningLimit) {
		EventAvailability eventAvailability = RulesToolkit.getEventAvailability(items, JdkTypeIDs.VM_OPERATIONS);
		if (eventAvailability == EventAvailability.UNKNOWN || eventAvailability == EventAvailability.DISABLED) {
			return RulesToolkit.getEventAvailabilityResult(this, items, eventAvailability, JdkTypeIDs.VM_OPERATIONS);
		}

		IQuantity infoLimit = warningLimit.multiply(0.5);

		Pair<IItem, IQuantity> longestEventInfo = findLongestEventInfo(
				items.apply(JdkFilters.VM_OPERATIONS_BLOCKING_OR_SAFEPOINT));
		IItem startingEvent = longestEventInfo.left;
		if (startingEvent == null) {
			String zeroDuration = UnitLookup.SECOND.quantity(0).displayUsing(IDisplayable.AUTO);
			return new Result(this, 0,
					MessageFormat.format(Messages.getString(Messages.VMOperationRuleFactory_TEXT_OK), zeroDuration),
					null, JdkQueries.VM_OPERATIONS);
		}
		String timeStr = getStartTime(startingEvent).displayUsing(IDisplayable.AUTO);
		IQuantity longestDuration = longestEventInfo.right;
		String peakDuration = longestDuration.displayUsing(IDisplayable.AUTO);
		String operation = getOperation(startingEvent);
		IMCThread caller = getCaller(startingEvent);
		double score = RulesToolkit.mapExp100(longestDuration.doubleValueIn(UnitLookup.SECOND),
				infoLimit.doubleValueIn(UnitLookup.SECOND), warningLimit.doubleValueIn(UnitLookup.SECOND));

		boolean isCombinedDuration = getDuration(startingEvent).compareTo(longestDuration) != 0;
		longestDuration = null;
		startingEvent = null;
		if (Severity.get(score) == Severity.WARNING || Severity.get(score) == Severity.INFO) {
			String longMessage = isCombinedDuration ? Messages.VMOperationRuleFactory_TEXT_WARN_LONG_COMBINED_DURATION
					: Messages.VMOperationRuleFactory_TEXT_WARN_LONG;
			String shortMessage = isCombinedDuration ? Messages.VMOperationRuleFactory_TEXT_WARN_COMBINED_DURATION
					: Messages.VMOperationRuleFactory_TEXT_WARN;
			return new Result(this, score, MessageFormat.format(Messages.getString(shortMessage), peakDuration),
					MessageFormat.format(Messages.getString(longMessage), peakDuration, operation, caller, timeStr),
					JdkQueries.VM_OPERATIONS_BLOCKING);
		}
		String shortMessage = isCombinedDuration ? Messages.VMOperationRuleFactory_TEXT_OK_COMBINED_DURATION
				: Messages.VMOperationRuleFactory_TEXT_OK;
		return new Result(this, score, MessageFormat.format(Messages.getString(shortMessage), peakDuration), null,
				JdkQueries.FILE_READ);
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
}
