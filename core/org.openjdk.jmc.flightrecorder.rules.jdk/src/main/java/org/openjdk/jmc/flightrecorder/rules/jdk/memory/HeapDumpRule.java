package org.openjdk.jmc.flightrecorder.rules.jdk.memory;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

public class HeapDumpRule implements IRule {
	private static final String HEAP_DUMP_RESULT_ID = "HeapDump"; //$NON-NLS-1$

	protected Result getResult(IItemCollection items, IPreferenceValueProvider valueProvider) {
		EventAvailability eventAvailability = RulesToolkit.getEventAvailability(items, JdkTypeIDs.HEAP_DUMP);
		if (eventAvailability == EventAvailability.DISABLED) {
			return RulesToolkit.getEventAvailabilityResult(this, items, eventAvailability, JdkTypeIDs.HEAP_DUMP);
		}
		IItemCollection heapDumpEvents = items.apply(ItemFilters.type(JdkTypeIDs.HEAP_DUMP));
		if (!heapDumpEvents.hasItems()) {
			return new Result(this, 0, Messages.getString(Messages.HeapDumpRule_TEXT_OK));
		}
		IQuantity heapDumpCount = heapDumpEvents.getAggregate(Aggregators.count());
		String message = Messages.getString(Messages.HeapDumpRule_TEXT_INFO);
		return new Result(this, 50, MessageFormat.format(message, heapDumpCount.clampedLongValueIn(UnitLookup.NUMBER_UNITY)));
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

	@Override
	public Collection<TypedPreference<?>> getConfigurationAttributes() {
		return Collections.emptyList();
	}

	@Override
	public String getId() {
		return HEAP_DUMP_RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.HeapDumpRule_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.HEAP;
	}

}
