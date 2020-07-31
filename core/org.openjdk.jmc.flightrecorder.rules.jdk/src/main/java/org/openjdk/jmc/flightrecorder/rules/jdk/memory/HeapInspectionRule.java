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

import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER;
import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER_UNITY;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.jdk.JdkQueries;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

public class HeapInspectionRule implements IRule {
	private static final String HEAP_INSPECTION_RESULT_ID = "HeapInspectionGc"; //$NON-NLS-1$

	public static final TypedPreference<IQuantity> HEAP_INSPECTION_LIMIT = new TypedPreference<>(
			"heap.inspection.info.limit", Messages.getString(Messages.HeapInspectionRule_CONFIG_WARNING_LIMIT), //$NON-NLS-1$
			Messages.getString(Messages.HeapInspectionRule_CONFIG_WARNING_LIMIT_LONG), NUMBER,
			NUMBER_UNITY.quantity(1));
	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays
			.<TypedPreference<?>> asList(HEAP_INSPECTION_LIMIT);

	@Override
	public RunnableFuture<Result> evaluate(final IItemCollection items, final IPreferenceValueProvider valueProvider) {
		FutureTask<Result> evaluationTask = new FutureTask<>(new Callable<Result>() {
			@Override
			public Result call() throws Exception {
				EventAvailability eventAvailability = RulesToolkit.getEventAvailability(items,
						JdkTypeIDs.GARBAGE_COLLECTION);
				if (eventAvailability != EventAvailability.AVAILABLE) {
					return RulesToolkit.getEventAvailabilityResult(HeapInspectionRule.this, items, eventAvailability,
							JdkTypeIDs.GARBAGE_COLLECTION);
				}
				GarbageCollectionsInfo aggregate = items.getAggregate(GarbageCollectionsInfo.GC_INFO_AGGREGATOR);
				return getHeapInspectionResult(aggregate.getObjectCountGCs(), items,
						valueProvider.getPreferenceValue(HEAP_INSPECTION_LIMIT));
			}
		});
		return evaluationTask;
	}

	private Result getHeapInspectionResult(int objectCountGCs, IItemCollection items, IQuantity limit) {
		if (objectCountGCs > 0) {
			double score = RulesToolkit.mapExp74(objectCountGCs, limit.longValue());
			String message = MessageFormat.format(Messages.getString(Messages.HeapInspectionGcRuleFactory_TEXT_INFO),
					objectCountGCs);
			String longMessage = message + " " //$NON-NLS-1$
					+ Messages.getString(Messages.HeapInspectionGcRuleFactory_TEXT_INFO_LONG);
			if (RulesToolkit.isEventsEnabled(items, JdkTypeIDs.OBJECT_COUNT)) {
				longMessage += "<p>" + Messages.getString(Messages.HeapInspectionGcRuleFactory_TEXT_INFO_LONG_JFR); //$NON-NLS-1$
			}
			return new Result(this, score, message, longMessage, JdkQueries.GARBAGE_COLLECTION);
		} else {
			return new Result(this, 0, Messages.getString(Messages.HeapInspectionGcRuleFactory_TEXT_OK),
					Messages.getString(Messages.HeapInspectionGcRuleFactory_TEXT_OK_LONG));
		}
	}

	@Override
	public Collection<TypedPreference<?>> getConfigurationAttributes() {
		return CONFIG_ATTRIBUTES;
	}

	@Override
	public String getId() {
		return HEAP_INSPECTION_RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.HeapInspectionGcRuleFactory_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.GARBAGE_COLLECTION;
	}
}
