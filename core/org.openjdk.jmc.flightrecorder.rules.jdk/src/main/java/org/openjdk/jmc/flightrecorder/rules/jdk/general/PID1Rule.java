package org.openjdk.jmc.flightrecorder.rules.jdk.general;

import java.util.Collection;
import java.util.Collections;
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
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;

public class PID1Rule implements IRule {
	private static final String PID1Rule_RESULT_ID = "PID1Rule";

	private static final Map<String, RulesToolkit.EventAvailability> REQUIRED_EVENTS = RulesToolkit.RequiredEventsBuilder
			.create().addEventType(JdkTypeIDs.VM_INFO, RulesToolkit.EventAvailability.AVAILABLE).build();

	@Override
	public String getId() {
		return PID1Rule_RESULT_ID;
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.JVM_INFORMATION;
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
		return new FutureTask<>(() -> getResult(items, valueProvider));
	}

	private IResult getResult(IItemCollection items, IPreferenceValueProvider valueProvider) {
		IItemCollection vmInfoEvents = items.apply(ItemFilters.type(JdkTypeIDs.VM_INFO));
		IAggregator<Set<String>, ?> distinct = Aggregators.distinct(JdkAttributes.PID);
		Set<String> aggregate = vmInfoEvents.getAggregate(distinct);
		if (aggregate == null || aggregate.isEmpty()) {
			return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.IGNORE)
					.setSummary(Messages.getString(Messages.PID1Rule_NO_PID)).build();
		}

		if (aggregate.size() > 1) {
			String warningMessage = "There is more than one PID information available. This should not be possible."; //$NON-NLS-1$
			Logger.getLogger(this.getClass().getName()).warning(warningMessage);
			return RulesToolkit.getNotApplicableResult(this, valueProvider, warningMessage);
		}

		String pid = aggregate.iterator().next();
		if ("1".equals(pid)) {
			return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.WARNING)
					.setSummary(Messages.getString(Messages.PID1Rule_TEXT_INFO))
					.setExplanation(Messages.getString(Messages.PID1Rule_TEXT_INFO_LONG)).build();
		}

		return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.OK)
				.setSummary(Messages.PID1Rule_TEXT_OK).build();
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
