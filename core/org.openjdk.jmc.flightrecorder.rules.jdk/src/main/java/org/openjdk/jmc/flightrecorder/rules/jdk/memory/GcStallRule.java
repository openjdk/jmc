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

import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER_UNITY;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkQueries;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

public class GcStallRule implements IRule {
	private static final String GC_STALL_RESULT_ID = "GcStall"; //$NON-NLS-1$

	@Override
	public RunnableFuture<Result> evaluate(final IItemCollection items, final IPreferenceValueProvider valueProvider) {
		FutureTask<Result> evaluationTask = new FutureTask<>(new Callable<Result>() {
			@Override
			public Result call() throws Exception {
				EventAvailability eventAvailability = RulesToolkit.getEventAvailability(items,
						JdkTypeIDs.GARBAGE_COLLECTION);
				if (eventAvailability != EventAvailability.AVAILABLE) {
					return RulesToolkit.getEventAvailabilityResult(GcStallRule.this, items, eventAvailability,
							JdkTypeIDs.GARBAGE_COLLECTION);
				}
				GarbageCollectionsInfo aggregate = items.getAggregate(GarbageCollectionsInfo.GC_INFO_AGGREGATOR);
				return getGcStallResult(aggregate.foundNonRequestedSerialOldGc(), items);
			}
		});
		return evaluationTask;
	}

	private Result getGcStallResult(boolean nonRequestedSerialOldGc, IItemCollection items) {
		if (nonRequestedSerialOldGc) {
			CollectorType oldCollectorType = CollectorType.getOldCollectorType(items);
			if (oldCollectorType == CollectorType.CMS) {
				return new Result(this, 100, Messages.getString(Messages.SerialOldRuleFactory_TEXT_WARN_CMS),
						Messages.getString(Messages.SerialOldRuleFactory_TEXT_WARN_CMS_LONG));
			} else if (oldCollectorType == CollectorType.G1_OLD) {
				return new Result(this, 100, Messages.getString(Messages.SerialOldRuleFactory_TEXT_WARN_G1),
						Messages.getString(Messages.SerialOldRuleFactory_TEXT_WARN_G1_LONG));
			}
		}
		IQuantity c = items.getAggregate(Aggregators.count(null, null, JdkFilters.CONCURRENT_MODE_FAILURE));
		if (c != null && c.clampedLongValueIn(NUMBER_UNITY) > 0) {
			return new Result(this, 100, Messages.getString(Messages.ConcurrentFailedRuleFactory_TEXT_WARN),
					Messages.getString(Messages.ConcurrentFailedRuleFactory_TEXT_WARN_LONG),
					JdkQueries.CONCURRENT_MODE_FAILURE);
		}

		EventAvailability eventAvailability = RulesToolkit.getEventAvailability(items,
				JdkTypeIDs.CONCURRENT_MODE_FAILURE, JdkTypeIDs.GC_CONF, JdkTypeIDs.GARBAGE_COLLECTION);
		if (eventAvailability != EventAvailability.AVAILABLE) {
			return RulesToolkit.getEventAvailabilityResult(this, items, eventAvailability,
					JdkTypeIDs.CONCURRENT_MODE_FAILURE, JdkTypeIDs.GC_CONF, JdkTypeIDs.GARBAGE_COLLECTION);
		}
		return new Result(this, 0, Messages.getString(Messages.GcStallRule_TEXT_OK));
	}

	@Override
	public Collection<TypedPreference<?>> getConfigurationAttributes() {
		return Collections.emptyList();
	}

	@Override
	public String getId() {
		return GC_STALL_RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.GcStallRule_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.GARBAGE_COLLECTION_TOPIC;
	}
}
