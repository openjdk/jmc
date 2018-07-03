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
package org.openjdk.jmc.flightrecorder.rules.jdk.general;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.Aggregators.CountConsumer;
import org.openjdk.jmc.common.item.GroupingAggregator;
import org.openjdk.jmc.common.item.GroupingAggregator.GroupEntry;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.owasp.encoder.Encode;

public class StackDepthSettingRule implements IRule {
	private static final int DEFAULT_STACK_DEPTH = 64;
	private static final String STACKDEPTH_SETTING_RESULT_ID = "StackdepthSetting"; //$NON-NLS-1$

	private Result getResult(IItemCollection items, IPreferenceValueProvider valueProvider) {
		IItemFilter truncatedTracesFilter = ItemFilters.equals(JdkAttributes.STACK_TRACE_TRUNCATED, true);
		IQuantity numberOfTruncatedTraces = items.getAggregate(Aggregators.count(truncatedTracesFilter));
		IQuantity numberOfTraces = items
				.getAggregate(Aggregators.count(ItemFilters.hasAttribute(JfrAttributes.EVENT_STACKTRACE)));
		if (numberOfTraces == null) {
			return RulesToolkit.getNotApplicableResult(this,
					Messages.getString(Messages.StackdepthSettingRule_TEXT_NA));
		}
		if (numberOfTruncatedTraces.longValue() > 0) {
			IItemCollection truncatedTraces = items.apply(truncatedTracesFilter);
			Map<String, Integer> truncatedTraceCounts = getTraceCount(truncatedTraces);
			Set<String> eventTypes = new HashSet<>();
			for (IType<?> type : truncatedTraces.getAggregate(Aggregators.distinct(JfrAttributes.EVENT_TYPE))) {
				eventTypes.add(type.getIdentifier());
			}
			Map<String, Integer> allTraceCounts = getTraceCount(items.apply(ItemFilters.type(eventTypes)));
			StringBuilder listBuilder = new StringBuilder();
			for (Entry<String, Integer> entry : truncatedTraceCounts.entrySet()) {
				listBuilder.append("<li>"); //$NON-NLS-1$
				IQuantity percentTruncated = UnitLookup.PERCENT_UNITY
						.quantity((double) entry.getValue() / (double) allTraceCounts.get(entry.getKey()));
				listBuilder.append(
						MessageFormat.format(Messages.getString(Messages.StackdepthSettingRule_TYPE_LIST_TEMPLATE),
								Encode.forHtml(entry.getKey()), percentTruncated.displayUsing(IDisplayable.AUTO)));
				listBuilder.append("</li>"); //$NON-NLS-1$
			}

			double truncatedTracesRatio = numberOfTruncatedTraces.ratioTo(numberOfTraces);
			String shortMessage = Messages.getString(Messages.StackdepthSettingRule_TEXT_INFO);
			String stackDepthValue = RulesToolkit.getFlightRecorderOptions(items).get("stackdepth"); //$NON-NLS-1$
			String longMessage = shortMessage + "<p>" //$NON-NLS-1$
					+ MessageFormat.format(Messages.getString(Messages.StackdepthSettingRule_TEXT_INFO_LONG),
							stackDepthValue == null ? DEFAULT_STACK_DEPTH : Encode.forHtml(stackDepthValue),
							stackDepthValue == null
									? Messages.getString(Messages.StackdepthSettingRule_TEXT_INFO_LONG_DEFAULT) + " " //$NON-NLS-1$
									: "", //$NON-NLS-1$
							UnitLookup.PERCENT_UNITY.quantity(truncatedTracesRatio).displayUsing(IDisplayable.AUTO),
							listBuilder.toString());
			return new Result(this, RulesToolkit.mapExp100Y(truncatedTracesRatio, 0.01, 25), shortMessage, longMessage);
		}
		return new Result(this, 0, Messages.getString(Messages.StackdepthSettingRule_TEXT_OK));
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

	private Map<String, Integer> getTraceCount(IItemCollection items) {
		final HashMap<String, Integer> map = new HashMap<>();
		IAggregator<IQuantity, ?> build = GroupingAggregator.build("", "", JfrAttributes.EVENT_TYPE, //$NON-NLS-1$ //$NON-NLS-2$
				Aggregators.count(), new GroupingAggregator.IGroupsFinisher<IQuantity, IType<?>, CountConsumer>() {

					@Override
					public IType<IQuantity> getValueType() {
						return UnitLookup.NUMBER;
					}

					@Override
					public IQuantity getValue(Iterable<? extends GroupEntry<IType<?>, CountConsumer>> groups) {
						for (GroupEntry<IType<?>, CountConsumer> groupEntry : groups) {
							CountConsumer consumer = groupEntry.getConsumer();
							IType<?> key = groupEntry.getKey();
							map.put(key.getName(), consumer.getCount());
						}
						return null;
					}
				});
		items.getAggregate(build);
		return RulesToolkit.sortMap(map, false);
	}

	@Override
	public Collection<TypedPreference<?>> getConfigurationAttributes() {
		return Collections.emptyList();
	}

	@Override
	public String getId() {
		return STACKDEPTH_SETTING_RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.StackdepthSettingRule_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.JVM_INFORMATION_TOPIC;
	}
}
