/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package $packageName$;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.ResultBuilder;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.RequiredEventsBuilder;

/**
 * Simple JFR rule example. The rule will check the environment variable named JFR_RULE_TEST (by default) and 
 * try to parse the contents as a double to use as the score returned by the rule.
 * 
 * <p>The rule illustrates how to:
 *   <ul>
 *     <li>Create a rule result</li>
 *     <li>Use preferences in a rule</li>
 *     <li>How to filter an event collection (IItemCollection)</li>
 *     <li>How to calculate an aggregate on an event collection</li>
 *   </ul>
 *   
 * <p>Most commonly used aggregates are specified either in the Aggregators class (most commonly used numerical aggregates, such as sum, avg, stddev), 
 * or the JfrAggregators class.
 */
@SuppressWarnings("nls")
public class $className$ implements IRule {
	private static final TypedPreference<String> PREFERENCE_ENVIRONMENT_VARIABLE_NAME = 
		new TypedPreference<>("environmentVariable", "Environment Variable", "The name of the environment variable containing the floating point score", UnitLookup.PLAIN_TEXT.getPersister(), "JFR_RULE_TEST");
	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = 
		Arrays.<TypedPreference<?>> asList(PREFERENCE_ENVIRONMENT_VARIABLE_NAME);
	
	public static final TypedResult<String> VARIABLE_NAME = new TypedResult<>("variableName",
			"Variable", "A description", UnitLookup.PLAIN_TEXT, String.class);
	
	private static final List<TypedResult<?>> RESULT_ATTRIBUTES = Arrays.<TypedResult<?>> asList(TypedResult.SCORE, VARIABLE_NAME);
	
	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.ENVIRONMENT_VARIABLE, EventAvailability.AVAILABLE).build();

	private IResult getResult(IItemCollection items, IPreferenceValueProvider valueProvider, IResultValueProvider resultProvider) {
		String variableName = valueProvider.getPreferenceValue(PREFERENCE_ENVIRONMENT_VARIABLE_NAME);
		String environmentVariableValue = getEnvironmentVariable(variableName, items);

		if (environmentVariableValue == null) {
			return ResultBuilder.createFor(this, valueProvider)
					.setSeverity(Severity.WARNING)
					.setSummary("Could not find the environment variable named {variableName}.")
					.addResult(VARIABLE_NAME, variableName)
					.addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(100))
					.build();
		}

		try {
			double score = Double.parseDouble(environmentVariableValue);
			return ResultBuilder.createFor(this, valueProvider)
					.setSeverity(Severity.get(score))
					.setSummary("The result from parsing the information in the environment variable named {variableName} was {core}.")
					.addResult(VARIABLE_NAME, variableName)
					.addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(score))
					.build();
		} catch (NumberFormatException e) {
			return ResultBuilder.createFor(this, valueProvider)
					.setSeverity(Severity.WARNING)
					.setSummary("Could not parse the value for the environment variable named {variableName}.")
					.addResult(VARIABLE_NAME, variableName)
					.addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(100))
					.build();
		}
	}

	@Override
	public RunnableFuture<IResult> createEvaluation(final IItemCollection items, final IPreferenceValueProvider valueProvider, final IResultValueProvider resultProvider) {
		FutureTask<IResult> evaluationTask = new FutureTask<>(new Callable<IResult>() {
			@Override
			public IResult call() throws Exception {
				return getResult(items, valueProvider, resultProvider);
			}
		});
		return evaluationTask;
	}

	private String getEnvironmentVariable(String variableName, IItemCollection items) {
		IItemCollection envItems = items.apply(ItemFilters.and(JdkFilters.ENVIRONMENT_VARIABLE, ItemFilters.equals(JdkAttributes.ENVIRONMENT_KEY, variableName)));
		IAggregator<String, ?> firstAggregator = JdkAggregators.first(JdkAttributes.ENVIRONMENT_VALUE);
		return envItems.getAggregate(firstAggregator);
	}

	@Override
	public Collection<TypedPreference<?>> getConfigurationAttributes() {
		return CONFIG_ATTRIBUTES;
	}
	
	@Override
	public Collection<TypedResult<?>> getResults() {
		return RESULT_ATTRIBUTES;
	}
	
	@Override
	public Map<String, EventAvailability> getRequiredEvents() {
		return REQUIRED_EVENTS;
	}

	@Override
	public String getId() {
		return "$packageName$.$className$";
	}

	@Override
	public String getName() {
		return "$ruleName$";
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.ENVIRONMENT_VARIABLES;
	}
}
