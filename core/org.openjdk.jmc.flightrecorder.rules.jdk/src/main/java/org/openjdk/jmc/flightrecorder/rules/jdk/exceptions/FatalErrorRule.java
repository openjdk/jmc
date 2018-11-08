package org.openjdk.jmc.flightrecorder.rules.jdk.exceptions;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

public class FatalErrorRule implements IRule {
	
	private static final String RESULT_ID = "Fatal Errors"; //$NON-NLS-1$
	
	private static final String ERROR_REASON = "VM Error"; //$NON-NLS-1$
	private static final String INFO_REASON = "No remaining non-daemon Java threads"; //$NON-NLS-1$
	
	private FutureTask<Result> evaluationTask;

	@Override
	public RunnableFuture<Result> evaluate(final IItemCollection items, final IPreferenceValueProvider valueProvider) {
		evaluationTask = new FutureTask<>(new Callable<Result>() {
			@Override
			public Result call() throws Exception {
				return getResult(items, valueProvider);
			}
		});
		return evaluationTask;
	}
	
	private Result getResult(IItemCollection items, IPreferenceValueProvider valueProvider) {
		EventAvailability eventAvailability = RulesToolkit.getEventAvailability(items, JdkTypeIDs.VM_SHUTDOWN);
		if (eventAvailability != EventAvailability.AVAILABLE) {
			return RulesToolkit.getEventAvailabilityResult(this, items, eventAvailability, JdkTypeIDs.VM_SHUTDOWN);
		}
		
		IItemFilter shutdownFilter = ItemFilters.type(JdkTypeIDs.VM_SHUTDOWN);
		IItemCollection shutdownItems = items.apply(shutdownFilter);
		
		if (shutdownItems.hasItems()) {
			// Check the type of VM Shutdown, if it was a VM Error we should report it.
			if (shutdownItems.apply(ItemFilters.contains(JdkAttributes.SHUTDOWN_REASON, ERROR_REASON)).hasItems()) {
				String message = Messages.getString(Messages.FatalErrorRule_TEXT_WARN);
				return new Result(this, 100, message);
			} else if (shutdownItems.apply(ItemFilters.contains(JdkAttributes.SHUTDOWN_REASON, INFO_REASON)).hasItems()) {
				String message = Messages.getString(Messages.FatalErrorRule_TEXT_INFO);
				return new Result(this, 25, message);
			}
		}
		
		return new Result(this, 0, Messages.getString(Messages.FatalErrorRule_TEXT_OK));
	}

	@Override
	public Collection<TypedPreference<?>> getConfigurationAttributes() {
		return Collections.emptyList();
	}

	@Override
	public String getId() {
		return RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.FatalErrorRule_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.JVM_INFORMATION_TOPIC;
	}
}
