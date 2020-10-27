package org.openjdk.jmc.flightrecorder.rules.jdk.general;

import static org.openjdk.jmc.common.item.Attribute.attr;

import java.util.Collection;
import java.util.Collections;
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
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.RequiredEventsBuilder;

public class FastTimeRule implements IRule {

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.TIME_CONVERSION, EventAvailability.AVAILABLE).build();

	@Override
	public String getId() {
		return "FastTimeRule"; //$NON-NLS-1$
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.JVM_INFORMATION_TOPIC;
	}

	@Override
	public String getName() {
		return "Fast Time Conversion Rule";
	}

	@Override
	public Map<String, EventAvailability> getRequiredEvents() {
		return REQUIRED_EVENTS;
	}

	private IResult getResult(
		IItemCollection items, IPreferenceValueProvider preferences, IResultValueProvider results) {
		// Check time conversion error
		IItemCollection timeConversionItems = items.apply(JdkFilters.TIME_CONVERSION);
		IQuantity conversionFactor = timeConversionItems
				.getAggregate(Aggregators.max(attr("fastTimeConversionAdjustments", null, //$NON-NLS-1$
						UnitLookup.NUMBER)));
		Boolean fastTimeEnabled = timeConversionItems
				.getAggregate(Aggregators.and(JdkTypeIDs.TIME_CONVERSION, attr("fastTimeEnabled", null, //$NON-NLS-1$
						UnitLookup.FLAG)));
		if (conversionFactor != null && fastTimeEnabled) {
			if (conversionFactor.longValue() != 0) {
				return ResultBuilder.createFor(this, preferences).setSeverity(Severity.WARNING)
						.setSummary(Messages.getString(Messages.FasttimeRule_TEXT_WARN))
						.setExplanation(Messages.getString(Messages.FasttimeRule_TEXT_WARN_LONG)).build();
			}
		}
		return ResultBuilder.createFor(this, preferences).setSeverity(Severity.OK)
				.setSummary(Messages.getString(Messages.FlightRecordingSupportRule_TEXT_OK)).build();
	}

	@Override
	public RunnableFuture<IResult> createEvaluation(
		final IItemCollection items, final IPreferenceValueProvider preferenceValueProvider,
		final IResultValueProvider dependencyResults) {
		return new FutureTask<>(new Callable<IResult>() {
			@Override
			public IResult call() throws Exception {
				return getResult(items, preferenceValueProvider, dependencyResults);
			}
		});
	}

	@Override
	public Collection<TypedPreference<?>> getConfigurationAttributes() {
		return Collections.emptyList();
	}

	@Override
	public Collection<TypedResult<?>> getResults() {
		return Collections.emptyList();
	}

}
