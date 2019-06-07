/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, Red Hat Inc. All rights reserved.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.Aggregators.MergingAggregator;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemConsumer;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

public class FullGcRule implements IRule {
	private static final String FULL_GC_RESULT_ID = "FullGc"; //$NON-NLS-1$

	@Override
	public RunnableFuture<Result> evaluate(final IItemCollection items, final IPreferenceValueProvider valueProvider) {
		return new FutureTask<>(new Callable<Result>() {
			@Override
			public Result call() throws Exception {
				final CollectorType collectorType = CollectorType.getOldCollectorType(items);
				if (!(CollectorType.CMS.equals(collectorType) || CollectorType.G1_OLD.equals(collectorType))) {
					return RulesToolkit.getNotApplicableResult(
							FullGcRule.this,
							Messages.getString(Messages.FullGcRule_OTHER_COLLECTOR_IN_USE)
							);
				}

				final String[] eventTypes;
				if (CollectorType.CMS.equals(collectorType)) {
					eventTypes = new String[] { JdkTypeIDs.GC_COLLECTOR_OLD_GARBAGE_COLLECTION };
				} else {
					eventTypes = G1Aggregator.EVENT_TYPES;
				}
				if (!hasAvailableEvents(items, eventTypes)) {
					return RulesToolkit.getEventAvailabilityResult(
							FullGcRule.this,
							items,
							RulesToolkit.getEventAvailability(items, eventTypes),
							eventTypes
							);
				}

				final int fullGCs;
				if (CollectorType.CMS.equals(collectorType)) {
					final IQuantity c = items.getAggregate(Aggregators.count(null, null, JdkFilters.OLD_GARBAGE_COLLECTION));
					fullGCs = c == null ? 0 : c.clampedIntFloorIn(NUMBER_UNITY);
				} else {
					fullGCs = items.getAggregate(new G1Aggregator()).fullGCs;
				}

				if (fullGCs > 0) {
					return new Result(
							FullGcRule.this, 100,
							Messages.getString(Messages.FullGcRule_FULL_GC_OCCURRED_TITLE),
							Messages.getString(Messages.FullGcRule_FULL_GC_OCCURRED_DESC)
							);
				} else {
					return new Result(
							FullGcRule.this,
							0,
							Messages.getString(Messages.FullGcRule_NO_FULL_GC_OCCURRED)
							);
				}
			}
		});
	}

	private boolean hasAvailableEvents(final IItemCollection items, final String[] eventTypes) {
		return RulesToolkit.getEventAvailability(items, eventTypes) == EventAvailability.AVAILABLE;
	}

	@Override
	public Collection<TypedPreference<?>> getConfigurationAttributes() {
		return Collections.emptyList();
	}

	@Override
	public String getId() {
		return FULL_GC_RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.FullGcRule_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.GARBAGE_COLLECTION_TOPIC;
	}

	private static class G1Aggregator extends MergingAggregator<G1FullGCInfo, G1FullGCInfo> {
		static final String[] EVENT_TYPES = new String[] { JdkTypeIDs.GARBAGE_COLLECTION };

		G1Aggregator() {
			super(null, null, UnitLookup.UNKNOWN);
		}

		@Override
		public final boolean acceptType(final IType<IItem> type) {
			return Arrays.asList(EVENT_TYPES).contains(type.getIdentifier());
		}

		@Override
		public G1FullGCInfo newItemConsumer(final IType<IItem> type) {
			return new G1FullGCInfo(JdkAttributes.GC_NAME.getAccessor(type));
		}

		@Override
		public G1FullGCInfo getValue(final G1FullGCInfo consumer) {
			return consumer == null ? new G1FullGCInfo(null) : consumer;
		}
	}

	private static class G1FullGCInfo implements IItemConsumer<G1FullGCInfo> {
		private final IMemberAccessor<String, IItem> accessor;

		G1FullGCInfo(final IMemberAccessor<String, IItem> accessor) {
			this.accessor = accessor;
		}

		int fullGCs = 0;

		@Override
		public G1FullGCInfo merge(final G1FullGCInfo other) {
			this.fullGCs += other.fullGCs;
			return this;
		}

		@Override
		public void consume(final IItem item) {
			if (this.accessor == null) {
				return;
			}
			final String gcName = this.accessor.getMember(item);
			if (gcName.equals(CollectorType.G1_FULL.getCollectorName())) {
				this.fullGCs++;
			}
		}
	}

}
