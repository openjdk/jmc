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
package org.openjdk.jmc.flightrecorder.rules.jdk.general;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.ResultBuilder;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedCollectionResult;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

public class StackDepthSettingRule implements IRule {

	private static final IQuantity DEFAULT_STACK_DEPTH_SETTING = UnitLookup.NUMBER_UNITY.quantity(64);

	public static class StackDepthTruncationData implements IDisplayable {

		private final IQuantity percentTruncated;
		private final String type;

		public StackDepthTruncationData(String type, IQuantity percentTruncated) {
			this.type = type;
			this.percentTruncated = percentTruncated;
		}

		public IQuantity getPercentTruncated() {
			return percentTruncated;
		}

		public String getType() {
			return type;
		}

		@Override
		public String displayUsing(String formatHint) {
			return MessageFormat.format(Messages.getString(Messages.StackdepthSettingRule_TYPE_LIST_TEMPLATE), type,
					percentTruncated.displayUsing(formatHint));
		}

	}

	public static final ContentType<StackDepthTruncationData> TRUNCATION_DATA = UnitLookup
			.createSyntheticContentType("truncationData"); //$NON-NLS-1$

	public static final TypedCollectionResult<StackDepthTruncationData> TRUNCATED_TRACES = new TypedCollectionResult<>(
			"truncatedTraces", "Truncated Traces", "The types that had truncated stacktraces.", TRUNCATION_DATA, //$NON-NLS-1$
			StackDepthTruncationData.class);
	public static final TypedResult<IQuantity> STACK_DEPTH = new TypedResult<>("stackdepth", "Stackdepth", //$NON-NLS-1$
			"The maximum stack depth before the trace is truncated.", UnitLookup.NUMBER, IQuantity.class);
	public static final TypedResult<IQuantity> TRUNCATION_RATIO = new TypedResult<>("truncationRatio", //$NON-NLS-1$
			"Truncation Ratio", "The percentage of stacktraces that were truncated.", UnitLookup.PERCENTAGE,
			IQuantity.class);

	private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays
			.<TypedResult<?>> asList(TypedResult.SCORE, TRUNCATED_TRACES, STACK_DEPTH, TRUNCATION_RATIO);

	private static final String STACKDEPTH_SETTING_RESULT_ID = "StackdepthSetting"; //$NON-NLS-1$

	private IResult getResult(
		IItemCollection items, IPreferenceValueProvider valueProvider, IResultValueProvider resultProvider) {
		IItemFilter stackTracesFilter = ItemFilters.hasAttribute(JfrAttributes.EVENT_STACKTRACE);
		Map<String, Long> truncatedTracesByType = new HashMap<>();
		Map<String, Long> tracesByType = new HashMap<>();
		long truncatedTraces = 0L;
		long totalTraces = 0L;
		for (IItemIterable itemIterable : items.apply(stackTracesFilter)) {
			IMemberAccessor<IMCStackTrace, IItem> stacktraceAccessor = JfrAttributes.EVENT_STACKTRACE
					.getAccessor(itemIterable.getType());
			for (IItem item : itemIterable) {
				String typeIdentifier = itemIterable.getType().getName();
				IMCStackTrace stacktrace = stacktraceAccessor.getMember(item);
				totalTraces++;
				Long tracesForType = tracesByType.containsKey(typeIdentifier) ? tracesByType.get(typeIdentifier) : 0L;
				tracesByType.put(typeIdentifier, tracesForType + 1);
				if (stacktrace != null && stacktrace.getTruncationState().isTruncated()) {
					truncatedTraces++;
					Long truncatedTracesForType = truncatedTracesByType.containsKey(typeIdentifier)
							? truncatedTracesByType.get(typeIdentifier) : 0L;
					truncatedTracesByType.put(typeIdentifier, truncatedTracesForType + 1);
				}
			}
		}
		if (totalTraces == 0L) {
			return RulesToolkit.getNotApplicableResult(this, valueProvider,
					Messages.getString(Messages.StackdepthSettingRule_TEXT_NA));
		}
		if (truncatedTraces > 0) {
			List<String> typesWithTruncatedTraces = new ArrayList<>(truncatedTracesByType.keySet());
			Collections.sort(typesWithTruncatedTraces);
			//TODO: Model this data better with e.g. a EventTruncationData class or something similar
			List<StackDepthTruncationData> truncationData = new ArrayList<>();
			for (String type : typesWithTruncatedTraces) {
				Long value = truncatedTracesByType.get(type);
				IQuantity percentTruncated = UnitLookup.PERCENT_UNITY
						.quantity((double) value / (double) tracesByType.get(type));
				truncationData.add(new StackDepthTruncationData(type, percentTruncated));
			}

			double truncatedTracesRatio = truncatedTraces / (double) totalTraces;
			String stackDepthValue = RulesToolkit.getFlightRecorderOptions(items).get("stackdepth"); //$NON-NLS-1$
			String explanation = Messages.getString(Messages.StackdepthSettingRule_TEXT_INFO_LONG);
			double score = RulesToolkit.mapExp100Y(truncatedTracesRatio, 0.01, 25);
			IQuantity stackDepthSetting = stackDepthValue == null ? DEFAULT_STACK_DEPTH_SETTING
					: UnitLookup.NUMBER_UNITY.quantity(Long.parseLong(stackDepthValue));
			return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.get(score))
					.setSummary(Messages.getString(Messages.StackdepthSettingRule_TEXT_INFO))
					.setExplanation(explanation).addResult(STACK_DEPTH, stackDepthSetting)
					.addResult(TRUNCATED_TRACES, truncationData)
					.addResult(TRUNCATION_RATIO, UnitLookup.PERCENT_UNITY.quantity(truncatedTracesRatio)).build();
		}
		return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.OK)
				.setSummary(Messages.getString(Messages.StackdepthSettingRule_TEXT_OK)).build();
	}

	@Override
	public RunnableFuture<IResult> createEvaluation(
		final IItemCollection items, final IPreferenceValueProvider valueProvider,
		final IResultValueProvider resultProvider) {
		FutureTask<IResult> evaluationTask = new FutureTask<>(new Callable<IResult>() {
			@Override
			public IResult call() throws Exception {
				return getResult(items, valueProvider, resultProvider);
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
		return STACKDEPTH_SETTING_RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.StackdepthSettingRule_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.JVM_INFORMATION;
	}

	@Override
	public Map<String, EventAvailability> getRequiredEvents() {
		return Collections.emptyMap();
	}

	@Override
	public Collection<TypedResult<?>> getResults() {
		return RESULT_ATTRIBUTES;
	}
}
