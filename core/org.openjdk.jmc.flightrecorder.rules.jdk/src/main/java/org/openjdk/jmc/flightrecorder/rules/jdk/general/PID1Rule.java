package org.openjdk.jmc.flightrecorder.rules.jdk.general;

import static org.openjdk.jmc.common.unit.UnitLookup.PLAIN_TEXT;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.logging.Logger;

import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
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

public class PID1Rule implements IRule {
	private static final String PID1Rule_RESULT_ID = "PID1Rule";

	private static final Map<String, RulesToolkit.EventAvailability> REQUIRED_EVENTS = RulesToolkit.RequiredEventsBuilder
			.create().addEventType(JdkTypeIDs.PROCESS_START, RulesToolkit.EventAvailability.AVAILABLE).build();

	private static final TypedCollectionResult<String> PID1 = new TypedCollectionResult<>("PID1SystemProperties", //$NON-NLS-1$
			"PID 1", "Process runs with PID 1.", PLAIN_TEXT, String.class);

	private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = List.of(PID1);

	private static final TypedPreference<String> EXCLUDED_STRINGS_REGEXP = new TypedPreference<>(
			"pid1properties.string.exclude.regexp", //$NON-NLS-1$
			Messages.getString(Messages.PID1Rule_CONFIG_EXCLUDED_STRINGS),
			Messages.getString(Messages.PID1Rule_CONFIG_EXCLUDED_STRINGS_LONG), PLAIN_TEXT.getPersister(), "TODO"); //$NON-NLS-1$

	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays.asList(EXCLUDED_STRINGS_REGEXP);

	@Override
	public String getId() {
		return PID1Rule_RESULT_ID;
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.PROCESSES;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.PID1Rule_RULE_NAME);
	}

	@Override
	public Map<String, RulesToolkit.EventAvailability> getRequiredEvents() {
		return REQUIRED_EVENTS;
	}

	@Override
	public RunnableFuture<IResult> createEvaluation(
		IItemCollection items, IPreferenceValueProvider valueProvider, IResultValueProvider resultProvider) {
		return new FutureTask<>(() -> getResult(items, valueProvider, resultProvider));
	}

	private IResult getResult(
		IItemCollection items, IPreferenceValueProvider valueProvider, IResultValueProvider resultProvider) {
		IItemCollection vmInfoEvents = items.apply(ItemFilters.type(JdkTypeIDs.VM_INFO));
		IAggregator<Set<String>, ?> distinct = Aggregators.distinct(JdkAttributes.PID);
		Set<String> aggregate = vmInfoEvents.getAggregate(distinct);

		if (aggregate.isEmpty()) {
			return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.IGNORE)
					.setSummary("There is no PID information available.").build();
		}

		if (aggregate.size() > 1) {
			// log that it contains more than 1 element. Why does it contain more?
			Logger.getLogger(this.getClass().getName()).warning("There is more than one PID information available. This should not be possible.");
			return RulesToolkit.getNotApplicableResult(this, valueProvider,
					"There is more than one PID information available. This should not be possible.");
		}

		String pid = aggregate.iterator().next();
		if ("1".equals(pid)) {
			return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.WARNING).setSummary("PID is 1.")
					.setExplanation(
							"No Java process should run with PID 1 as it fulfills a special handling in Unix-like systems.")
					.setSolution("TODO").build();
		}

		return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.OK)
				.setSummary(Messages.PID1Rule_TEXT_OK).build();
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
