package org.openjdk.jmc.flightrecorder.rules.jdk.next;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.IRule2;
import org.openjdk.jmc.flightrecorder.rules.ResultBuilder;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

public class NoDependencyRule implements IRule2 {

	public static TypedResult<IQuantity> FOUND_BYTES = new TypedResult<IQuantity>("foundBytes", "Found Bytes",
			"Some bytes we found", UnitLookup.MEMORY, IQuantity.class);
	private static Collection<TypedResult<?>> RESULTS = Arrays.<TypedResult<?>> asList(FOUND_BYTES);

	@Override
	public String getId() {
		return "noDeps";
	}

	@Override
	public String getTopic() {
		return "next";
	}

	@Override
	public String getName() {
		return "No Dependencies";
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
				// Here we do some calculations
				IQuantity bytes = UnitLookup.GIBIBYTE.quantity(2);
				return ResultBuilder.createFor(NoDependencyRule.this).setSeverity(Severity.OK)
						.setSummary("No dependencies for this rule").setExplanation("There are bytes in the heap!!!")
						.setSolution("Don't allocate things!").addResult(FOUND_BYTES, bytes).build();
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
		return RESULTS;
	}

}
