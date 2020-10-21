package org.openjdk.jmc.flightrecorder.rules.jdk.general;

import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER;
import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER_UNITY;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.IRule2;
import org.openjdk.jmc.flightrecorder.rules.ResultBuilder;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

public class BufferLostRule implements IRule2 {

	private static final String BUFFER_LOST_RESULT_ID = "BufferLost"; //$NON-NLS-1$

	public static final TypedPreference<IQuantity> WARNING_LIMIT = new TypedPreference<>("bufferlost.warning.limit", //$NON-NLS-1$
			Messages.getString(Messages.BufferLostRuleFactory_CONFIG_WARN_LIMIT),
			Messages.getString(Messages.BufferLostRuleFactory_CONFIG_WARN_LIMIT_LONG), NUMBER,
			NUMBER_UNITY.quantity(1));
	
	public static final TypedResult<IQuantity> DROPPED_COUNT = new TypedResult<>("droppedCount", Messages.getString(Messages.BufferLostRuleFactory_RESULT_DROPPED_COUNT_NAME), Messages.getString(Messages.BufferLostRuleFactory_RESULT_DROPPED_COUNT_DESCRIPTION), UnitLookup.NUMBER, IQuantity.class); //$NON-NLS-1$
	public static final TypedResult<IQuantity> DROPPED_SIZE = new TypedResult<>("droppedSize", Messages.getString(Messages.BufferLostRuleFactory_RESULT_DROPPED_SIZE_NAME), Messages.getString(Messages.BufferLostRuleFactory_RESULT_DROPPED_SIZE_DESCRIPTION), UnitLookup.MEMORY, IQuantity.class); //$NON-NLS-1$
	public static final TypedResult<IQuantity> FIRST_DROPPED_BUFFER_TIME = new TypedResult<>("firstDroppedBufferTime", Messages.getString(Messages.BufferLostRuleFactory_RESULT_FIRST_DROPPED_BUFFER_TIME_NAME), Messages.getString(Messages.BufferLostRuleFactory_RESULT_FIRST_DROPPED_BUFFER_TIME_DESCRIPTION), UnitLookup.TIMESTAMP, IQuantity.class); //$NON-NLS-1$
	
	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays.<TypedPreference<?>> asList(WARNING_LIMIT);
	private static final List<TypedResult<?>> RESULT_ATTRIBUTES = Arrays.<TypedResult<?>> asList(TypedResult.SCORE, DROPPED_COUNT, DROPPED_SIZE, FIRST_DROPPED_BUFFER_TIME);
	
	@Override
	public String getId() {
		return BUFFER_LOST_RESULT_ID;
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.RECORDING_TOPIC;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.BufferLostRuleFactory_RULE_NAME);
	}

	/*
	 * We don't believe JFR_DATA_LOST can be turned off, and recordings do not seem to have
	 * enablement information on them, so no point in checking if it's enabled.
	 */
	@Override
	public Map<String, EventAvailability> getRequiredEvents() {
		return Collections.emptyMap();
	}
	
	private IResult getResult(IItemCollection items, IPreferenceValueProvider valueProvider, IResultValueProvider resultValueProvider) {
		IQuantity limit = valueProvider.getPreferenceValue(WARNING_LIMIT);

		IItemCollection filtered = items.apply(JdkFilters.JFR_DATA_LOST);
		IQuantity startTime = filtered.getAggregate(JdkAggregators.first(JfrAttributes.START_TIME));

		if (startTime != null) {
			IQuantity droppedCount = filtered.getAggregate(JdkAggregators.JFR_DATA_LOST_COUNT);
			IQuantity droppedSize = filtered.getAggregate(JdkAggregators.FLR_DATA_LOST_SIZE);
			double rawScore = calculateScore(limit, droppedCount);
			IQuantity score = UnitLookup.NUMBER_UNITY.quantity(rawScore);
			return ResultBuilder.createFor(this, valueProvider)
					.setSeverity(Severity.get(rawScore))
					.addResult(TypedResult.SCORE, score)
					.addResult(DROPPED_COUNT, droppedCount)
					.addResult(DROPPED_SIZE, droppedSize)
					.addResult(FIRST_DROPPED_BUFFER_TIME, startTime)
					.setSummary(Messages.getString(Messages.BufferLostRuleFactory_RESULT_SUMMARY))
					.setExplanation(Messages.getString(Messages.BufferLostRuleFactory_RESULT_EXPLANATION))
					.setSolution(Messages.getString(Messages.BufferLostRuleFactory_RESULT_SOLUTION))
					.build();
		}
		return ResultBuilder.createFor(this, valueProvider)
				.setSeverity(Severity.OK)
				.setSummary(Messages.getString(Messages.BufferLostRuleFactory_RESULT_SUMMARY_OK))
				.build();
	}
	

	private double calculateScore(IQuantity limit, IQuantity droppedCount) {
		return RulesToolkit.mapExp100(limit.clampedLongValueIn(NUMBER_UNITY), droppedCount.longValue());
	}

	@Override
	public RunnableFuture<IResult> createEvaluation(final IItemCollection items,
			final IPreferenceValueProvider preferenceValueProvider, final IResultValueProvider dependencyResults) {
		FutureTask<IResult> evaluationTask = new FutureTask<>(new Callable<IResult>() {
			@Override
			public IResult call() throws Exception {
				return getResult(items, preferenceValueProvider, dependencyResults);
			}
		});
		return evaluationTask;
	}

	@Override
	public Collection<TypedPreference<?>> getConfigurationAttributes() {
		return CONFIG_ATTRIBUTES;
	}

	@Override
	public Collection<TypedResult<?>> getResults() {
		return RESULT_ATTRIBUTES;
	}

}
