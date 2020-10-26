package org.openjdk.jmc.flightrecorder.rules.jdk.next;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.rules.DependsOn;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.IRule2;
import org.openjdk.jmc.flightrecorder.rules.ResultBuilder;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

@DependsOn(value = NoDependencyRule.class, severity = Severity.OK)
public class OneDependencyRule implements IRule2 {

	@Override
	public String getId() {
		return "OneDependency";
	}

	@Override
	public String getTopic() {
		return "next";
	}

	@Override
	public String getName() {
		return "One Dependency Rule";
	}

	@Override
	public Map<String, EventAvailability> getRequiredEvents() {
		return Collections.emptyMap();
	}

	@Override
	public RunnableFuture<IResult> createEvaluation(
		final IItemCollection items, final IPreferenceValueProvider preferenceValueProvider,
		final IResultValueProvider dependencyResults) {
		FutureTask<IResult> futureTask = new FutureTask<IResult>(new Callable<IResult>() {
			@Override
			public IResult call() throws Exception {
				// Here we look up an earlier result
				IQuantity resultValue = dependencyResults.getResultValue(NoDependencyRule.FOUND_BYTES);
				if (resultValue != null) {
					return ResultBuilder.createFor(OneDependencyRule.this, preferenceValueProvider).setSeverity(Severity.INFO)
							.setSummary("We found {foundBytes} earlier!")
							.setExplanation(MessageFormat.format("A dependency for this rule found '{'{0}'}'",
									NoDependencyRule.FOUND_BYTES.getIdentifier()))
							.setSolution("Fix the earlier issue, it might be important").build();
				} else {
					return ResultBuilder.createFor(OneDependencyRule.this, preferenceValueProvider).setSeverity(Severity.OK)
							.setSummary("We didn't find any bytes earlier!")
							.setExplanation("A dependency for this rule did not find anything").build();
				}
			}
		});
		return futureTask;
	}

	@Override
	public Collection<TypedPreference<?>> getConfigurationAttributes() {
		return Collections.emptySet();
	}

	@Override
	public Collection<TypedResult<?>> getResults() {
		return Collections.emptySet();
	}

}
