package org.openjdk.jmc.flightrecorder.rules.jdk.general;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
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

public class ClassLoadingRule implements IRule {

	private static final String RESULT_ID = "ClassLoading"; //$NON-NLS-1$

	public static final TypedPreference<IQuantity> MAX_DURATION_LIMIT = new TypedPreference<>(
			"classloading.duration.max.limit", //$NON-NLS-1$
			Messages.getString(Messages.ClassLoadingRule_CONFIG_DURATION_LIMIT),
			Messages.getString(Messages.ClassLoadingRule_CONFIG_DURATION_LIMIT_LONG), UnitLookup.TIMESPAN,
			UnitLookup.MILLISECOND.quantity(1000L));
	public static final TypedPreference<IQuantity> RATIO_OF_TOTAL_LIMIT = new TypedPreference<>(
			"classloading.ratio-to-total.limit", //$NON-NLS-1$
			Messages.getString(Messages.ClassLoadingRule_CONFIG_RATIO_LIMIT),
			Messages.getString(Messages.ClassLoadingRule_CONFIG_RATIO_LIMIT_LONG), UnitLookup.NUMBER,
			UnitLookup.NUMBER_UNITY.quantity(0.10));

	public static final TypedResult<IQuantity> LONGEST_CLASS_LOAD = new TypedResult<>("longestClassLoad", //$NON-NLS-1$
			Messages.getString(Messages.ClassLoadingRule_RESULT_LONGEST_LOAD_NAME),
			Messages.getString(Messages.ClassLoadingRule_RESULT_LONGEST_LOAD_DESCRIPTION), UnitLookup.TIMESPAN,
			IQuantity.class);
	public static final TypedResult<IQuantity> TOTAL_CLASS_LOAD_TIME = new TypedResult<>("totalClassLoadTime", //$NON-NLS-1$
			Messages.getString(Messages.ClassLoadingRule_RESULT_TOTAL_LOAD_TIME_NAME),
			Messages.getString(Messages.ClassLoadingRule_RESULT_TOTAL_LOAD_TIME_DESCRIPTION), UnitLookup.TIMESPAN,
			IQuantity.class);
	public static final TypedResult<IQuantity> TOTAL_CLASS_LOAD_COUNT = new TypedResult<>("totalClassLoadCount", //$NON-NLS-1$
			Messages.getString(Messages.ClassLoadingRule_RESULT_TOTAL_LOAD_COUNT_NAME),
			Messages.getString(Messages.ClassLoadingRule_RESULT_TOTAL_LOAD_COUNT_DESCRIPTION), UnitLookup.TIMESPAN,
			IQuantity.class);

	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays
			.<TypedPreference<?>> asList(MAX_DURATION_LIMIT, RATIO_OF_TOTAL_LIMIT);
	private static final List<TypedResult<?>> RESULT_ATTRIBUTES = Arrays.<TypedResult<?>> asList(TypedResult.SCORE,
			LONGEST_CLASS_LOAD, TOTAL_CLASS_LOAD_COUNT, TOTAL_CLASS_LOAD_TIME);
	private static final Map<String, EventAvailability> REQUIRED_EVENTS;

	static {
		REQUIRED_EVENTS = new HashMap<>();
		REQUIRED_EVENTS.put(JdkTypeIDs.CLASS_LOAD, EventAvailability.ENABLED);
	}

	@Override
	public String getId() {
		return RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.ClassLoadingRuleFactory_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.CLASS_LOADING_TOPIC;
	}

	@Override
	public Map<String, EventAvailability> getRequiredEvents() {
		return REQUIRED_EVENTS;
	}

	private IResult getResult(
		IItemCollection items, IPreferenceValueProvider valueProvider, IResultValueProvider dependencyResults) {
		IQuantity maxDurationLimit = valueProvider.getPreferenceValue(MAX_DURATION_LIMIT);
		IQuantity ratioOfTotalLimit = valueProvider.getPreferenceValue(RATIO_OF_TOTAL_LIMIT);

		IItemCollection events = items.apply(JdkFilters.CLASS_LOAD);

		IQuantity startTime = RulesToolkit.getEarliestStartTime(events);
		IQuantity endTime = RulesToolkit.getLatestEndTime(events);
		if (startTime != null && endTime != null) {
			IQuantity totalTime = endTime.subtract(startTime);
			IQuantity longestTime = events.getAggregate(Aggregators.max(JfrAttributes.DURATION));
			IQuantity sumTimeLoadedClasses = events.getAggregate(Aggregators.sum(JfrAttributes.DURATION));
			if ((longestTime.compareTo(maxDurationLimit) > 0)
					|| (sumTimeLoadedClasses.ratioTo(totalTime) > ratioOfTotalLimit.doubleValue())) {
				IQuantity totalLoadedClasses = events.getAggregate(Aggregators.count());
				return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.INFO)
						.addResult(LONGEST_CLASS_LOAD, longestTime)
						.addResult(TOTAL_CLASS_LOAD_COUNT, totalLoadedClasses)
						.addResult(TOTAL_CLASS_LOAD_TIME, sumTimeLoadedClasses)
						.setSummary(Messages.getString(Messages.ClassLoadingRule_RESULT_SUMMARY))
						.setExplanation(Messages.getString(Messages.ClassLoadingRule_RESULT_EXPLANATION)).build();
			}
		}
		return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.OK)
				.setSummary(Messages.getString(Messages.ClassLoadingRuleFactory_RULE_TEXT_OK)).build();
	}

	@Override
	public RunnableFuture<IResult> createEvaluation(
		final IItemCollection items, final IPreferenceValueProvider preferenceValueProvider,
		final IResultValueProvider dependencyResults) {
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
