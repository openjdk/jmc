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
package org.openjdk.jmc.flightrecorder.rules.jdk.memory;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.collection.MapToolkit.IntEntry;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkQueries;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.dataproviders.StacktraceDataProvider;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator.FrameCategorization;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel.Fork;
import org.owasp.encoder.Encode;

public class AllocationByThreadRule implements IRule {
	private static final String THREAD_RESULT_ID = "Allocations.thread"; //$NON-NLS-1$

	private Result getResult(IItemCollection items, IPreferenceValueProvider valueProvider) {
		EventAvailability eventAvailabilityInside = RulesToolkit.getEventAvailability(items,
				JdkTypeIDs.ALLOC_INSIDE_TLAB);
		EventAvailability eventAvailabilityOutside = RulesToolkit.getEventAvailability(items,
				JdkTypeIDs.ALLOC_OUTSIDE_TLAB);
		if (!RulesToolkit.isEventsEnabled(eventAvailabilityInside, eventAvailabilityOutside)) {
			return RulesToolkit.getEventAvailabilityResult(this, items,
					RulesToolkit.getLeastAvailable(eventAvailabilityInside, eventAvailabilityOutside),
					JdkTypeIDs.ALLOC_INSIDE_TLAB, JdkTypeIDs.ALLOC_OUTSIDE_TLAB);
		}
		if (!(eventAvailabilityInside == EventAvailability.AVAILABLE
				|| eventAvailabilityOutside == EventAvailability.AVAILABLE)) {
			return RulesToolkit.getNotApplicableResult(this,
					MessageFormat.format(Messages.getString(Messages.General_RULE_REQUIRES_EVENTS_FROM_ONE_OF_MANY),
							JdkTypeIDs.ALLOC_INSIDE_TLAB + ", " + JdkTypeIDs.ALLOC_OUTSIDE_TLAB)); //$NON-NLS-1$
		}

		List<IntEntry<IMCThread>> entries = RulesToolkit.calculateGroupingScore(items.apply(JdkFilters.ALLOC_ALL),
				JfrAttributes.EVENT_THREAD);
		double balance = RulesToolkit.calculateBalanceScore(entries);
		IntEntry<IMCThread> mostSignificant = entries.get(entries.size() - 1);
		// FIXME: Configuration attribute instead of hard coded 1000 tlabs => relevance 50
		double relevance = RulesToolkit.mapExp100Y(mostSignificant.getValue(), 1000, 50);
		double score = balance * relevance * 0.74; // ceiling at 74;

		IItemFilter significantFilter = ItemFilters.and(JdkFilters.ALLOC_ALL,
				ItemFilters.equals(JfrAttributes.EVENT_THREAD, mostSignificant.getKey()));
		StacktraceModel stacktraceModel = new StacktraceModel(false,
				new FrameSeparator(FrameCategorization.METHOD, false), items.apply(significantFilter));
		Fork rootFork = stacktraceModel.getRootFork();
		String relevantTraceHtmlList = rootFork.getBranchCount() == 0
				? Messages.getString(Messages.General_NO_STACK_TRACE_AVAILABLE)
				: StacktraceDataProvider.getRelevantTraceHtmlList(rootFork.getBranch(0), rootFork.getItemsInFork());
		String message = MessageFormat.format(Messages.getString(Messages.AllocationByThreadRule_TEXT_MESSAGE),
				Encode.forHtml(mostSignificant.getKey().getThreadName()), relevantTraceHtmlList);
		String longMessage = message + "<p>" + Messages.getString(Messages.AllocationRuleFactory_TEXT_THREAD_INFO_LONG); //$NON-NLS-1$
		return new Result(this, score, message, longMessage, JdkQueries.ALLOC_INSIDE_TLAB_BY_THREAD);
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
		return Collections.emptyList();
	}

	@Override
	public String getId() {
		return THREAD_RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.AllocationByThreadRule_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.JAVA_APPLICATION_TOPIC;
	}
}
